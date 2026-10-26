import 'dart:convert';

import 'package:diwan_mobile/l10n/app_localizations.dart';
import 'package:diwan_mobile/state/session.dart';
import 'package:diwan_mobile/ui/money_screen.dart';
import 'package:diwan_mobile/ui/transactions_view.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

http.Response _json(Object body) =>
    http.Response.bytes(utf8.encode(jsonEncode(body)), 200, headers: {'content-type': 'application/json'});

String _daysAgo(int days, [int hour = 12]) {
  final d = DateTime.now().subtract(Duration(days: days));
  return DateTime(d.year, d.month, d.day, hour).toIso8601String();
}

Map<String, Object?> _tx(int id, String type, String category, num amount, String at,
        {String? merchant, String? card, String? account, String source = 'sms'}) =>
    {
      'id': id, 'type': type, 'category': category, 'amount': amount, 'currency': 'SAR', 'bank': 'Al Rajhi',
      'merchantName': merchant, 'cardRef': card, 'cardNetwork': card == null ? null : 'Visa', 'accountRef': account,
      'source': source, 'status': 'completed', 'recordedAt': at,
    };

var _requests = 0;
final _server = MockClient((req) async {
  if (req.url.path == '/api/transactions') _requests++;
  return switch (req.url.path) {
    '/api/users/login' => _json({
        'success': true,
        'data': {'token': 't', 'userId': 1, 'fullName': 'Sara Ali', 'email': 'sara@diwan.test'},
      }),
    '/api/transactions' => _json({
        'success': true,
        'data': [
          _tx(1, 'expense', 'food', 45, _daysAgo(0, 8), merchant: 'Starbucks', card: '1553'),
          _tx(2, 'expense', 'shopping', 103.51, _daysAgo(1), merchant: 'TASALIMAR', card: '1553'),
          _tx(3, 'expense', 'food', 30, _daysAgo(1, 9), merchant: 'starbucks', card: '5101', account: '2410'),
          _tx(4, 'income', 'salary', 1000, _daysAgo(2), account: '2410'),
          _tx(5, 'expense', 'bills', 300, _daysAgo(400), merchant: 'SEC', source: 'manual'),
        ],
      }),
    _ => _json({}),
  };
});

Future<void> _pump(WidgetTester tester, {Map<String, Object> prefs = const {}}) async {
  tester.view.physicalSize = const Size(1440, 6000);
  addTearDown(tester.view.reset);
  FlutterSecureStorage.setMockInitialValues({});
  SharedPreferences.setMockInitialValues(prefs);
  final session = Session(httpClient: _server)..locale = const Locale('en');
  await session.login('sara@diwan.test', 'secret123');
  await tester.pumpWidget(ChangeNotifierProvider.value(
    value: session,
    child: const MaterialApp(
      locale: Locale('en'),
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: MoneyScreen(),
    ),
  ));
  await tester.pumpAndSettle();
}

Future<void> _groupBy(WidgetTester tester, String label) async {
  await tester.tap(find.byTooltip('Group by'));
  await tester.pumpAndSettle();
  await tester.tap(find.text(label).last);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('search narrows the list and the totals follow it', (tester) async {
    await _pump(tester);
    expect(find.text('5 transactions'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'starb');
    await tester.pumpAndSettle();
    expect(find.text('2 transactions'), findsOneWidget);
    expect(find.text('SAR 75.00'), findsOneWidget); // expense total of the two
    expect(find.text('TASALIMAR'), findsNothing);

    await tester.enterText(find.byType(TextField), 'nothing like this');
    await tester.pumpAndSettle();
    expect(find.text('No transactions match these filters.'), findsOneWidget);
    await tester.tap(find.text('Clear all'));
    await tester.pumpAndSettle();
    expect(find.text('5 transactions'), findsOneWidget);
    expect(_requests, 1); // filtering happens on the phone
  });

  testWidgets('filter sheet: period and card, shown as removable chips', (tester) async {
    await _pump(tester);

    await tester.tap(find.byTooltip('Filters'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('This year'));
    await tester.pumpAndSettle();
    expect(find.text('Show 4 transactions'), findsOneWidget); // the bill is 400 days old
    await tester.tap(find.text('Visa ••1553'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Show 2 transactions'));
    await tester.pumpAndSettle();

    expect(find.text('2 transactions'), findsOneWidget);
    expect(find.widgetWithText(InputChip, 'This year'), findsOneWidget);
    expect(find.widgetWithText(InputChip, 'Visa ••1553'), findsOneWidget);
    expect(find.text('2'), findsOneWidget); // badge on the filter button

    // removing the card chip keeps the period
    await tester.tap(find.descendant(of: find.widgetWithText(InputChip, 'Visa ••1553'), matching: find.byIcon(Icons.clear)));
    await tester.pumpAndSettle();
    expect(find.text('4 transactions'), findsOneWidget);
  });

  testWidgets('group by merchant: one group per merchant, biggest spending first', (tester) async {
    await _pump(tester);
    await _groupBy(tester, 'Merchant');

    final tiles = find.byType(ExpansionTile);
    expect(tiles, findsNWidgets(4)); // SEC, TASALIMAR, Starbucks (both spellings), other
    final titles = tester.widgetList<ExpansionTile>(tiles).map((e) => ((e.title as Row).children.first as Expanded).child);
    expect(titles.map((w) => (w as Text).data), ['SEC', 'TASALIMAR', 'Starbucks', 'Other']);
    expect(find.text('2 transactions'), findsOneWidget); // Starbucks

    await tester.tap(find.text('Starbucks'));
    await tester.pumpAndSettle();
    expect(find.text('starbucks'), findsOneWidget); // its second transaction, now visible
  });

  testWidgets('group by day uses Today / Yesterday and is remembered', (tester) async {
    await _pump(tester);
    await _groupBy(tester, 'Day');
    expect(find.textContaining('Today'), findsOneWidget);
    expect(find.textContaining('Yesterday'), findsOneWidget);
    expect((await SharedPreferences.getInstance()).getString(TransactionsView.groupByKey), 'day');
  });

  testWidgets('a remembered grouping is used on the next visit', (tester) async {
    await _pump(tester, prefs: {TransactionsView.groupByKey: 'category'});
    expect(find.byType(ExpansionTile), findsNWidgets(4)); // bills, shopping, food, salary
    expect(find.text('Bills'), findsOneWidget);
  });
}
