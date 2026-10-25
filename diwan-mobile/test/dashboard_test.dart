import 'dart:convert';

import 'package:diwan_mobile/l10n/app_localizations.dart';
import 'package:diwan_mobile/state/session.dart';
import 'package:diwan_mobile/ui/dashboard_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

// What the gateway's /api/features answers (RouteDataInitializer seed), plus a tile the app does not know.
const _features = [
  {'id': 'smarthome', 'displayName': 'Smart Home', 'icon': '🏠', 'color': '#0EA5E9', 'screen': 'SmartHome', 'path': '/api/smart-home'},
  {'id': 'medical', 'displayName': 'Medical History', 'icon': '🏥', 'color': '#EF4444', 'screen': 'MedicalHistory', 'path': '/api/medical'},
  {'id': 'transactions', 'displayName': 'Financial History', 'icon': '💰', 'color': '#16A34A', 'screen': 'Transactions', 'path': '/api/transactions'},
  {'id': 'users', 'displayName': 'Update User Info', 'icon': '👤', 'color': '#2563EB', 'screen': 'UpdateUserInfo', 'path': '/api/users'},
  {'id': 'portfolio', 'displayName': 'Investments', 'icon': '📈', 'color': '#7C3AED', 'screen': 'Portfolio', 'path': '/api/portfolio'},
  {'id': 'travel', 'displayName': 'Travel', 'icon': '✈️', 'color': '#F59E0B', 'screen': 'Travel', 'description': 'Trips', 'path': '/api/travel'},
];

http.Response _json(Object body) =>
    http.Response.bytes(utf8.encode(jsonEncode(body)), 200, headers: {'content-type': 'application/json'});

final _server = MockClient((req) async => switch (req.url.path) {
      '/api/users/login' => _json({
          'success': true,
          'data': {'token': 't', 'userId': 1, 'fullName': 'Sara Ali', 'email': 'sara@diwan.test'},
        }),
      '/api/features' => _json(_features),
      '/api/smart-home/devices' => _json([
          {'id': 3, 'deviceId': 'lamp1', 'name': 'Kitchen lamp', 'state': 'ON', 'active': true},
        ]),
      '/api/transactions/positions' => _json({
          'success': true,
          'data': [
            {
              'bank': 'Al Rajhi',
              'accounts': [
                {'ref': '2410', 'currency': 'SAR', 'balance': 29255, 'balanceSince': '2026-10-25T00:00:00',
                 'monthIn': 34965, 'monthOut': 18710, 'cards': ['5101']},
                {'ref': '8468', 'currency': 'SAR', 'monthIn': 5500, 'monthOut': 0, 'cards': []},
              ],
              'cards': [
                {'ref': '1553', 'network': 'Visa', 'currency': 'SAR', 'available': 5844.89, 'availableAt': '2026-10-25T22:25:00',
                 'creditLimit': 10000, 'owed': 4155.11, 'monthSpent': 103.51},
                {'ref': '5101', 'network': 'mada', 'currency': 'SAR', 'account': '2410', 'monthSpent': 8000},
              ],
              'loans': [
                {'ref': 'loan', 'currency': 'SAR', 'remaining': 41680, 'remainingAt': '2026-10-25T20:05:00', 'lastInstalment': 5210, 'account': '2410'},
              ],
            },
          ],
        }),
      '/api/transactions/summary' => _json({'success': true, 'data': {'EGP': {'income': 1000, 'expense': 250}}}),
      '/api/transactions' => _json({'success': true, 'count': 0, 'data': []}),
      '/api/medical' => _json([]),
      _ => _json({}),
    });

Future<void> _pumpDashboard(WidgetTester tester, Locale locale) async {
  FlutterSecureStorage.setMockInitialValues({});
  SharedPreferences.setMockInitialValues({});
  final session = Session(httpClient: _server)..locale = locale;
  await session.login('sara@diwan.test', 'secret123');
  await tester.pumpWidget(ChangeNotifierProvider.value(
    value: session,
    child: MaterialApp(
      locale: locale,
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: const DashboardScreen(),
    ),
  ));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('dashboard shows a tile for every feature of the gateway', (tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    addTearDown(tester.view.reset);
    await _pumpDashboard(tester, const Locale('en'));

    expect(find.text('Hello, Sara'), findsOneWidget);
    for (final title in ['Smart Home', 'Medical History', 'Financial History', 'My Account', 'Investments', 'Travel']) {
      expect(find.text(title), findsOneWidget, reason: title);
    }
    expect(find.byType(FeatureTile), findsNWidgets(_features.length));
  });

  testWidgets('tile titles follow the app language', (tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    addTearDown(tester.view.reset);
    await _pumpDashboard(tester, const Locale('ar'));

    expect(find.text('المنزل الذكي'), findsOneWidget);
    expect(find.text('السجل الطبي'), findsOneWidget);
    expect(find.text('Travel'), findsOneWidget); // unknown to the app: the server's text
  });

  testWidgets('tapping a tile opens its page', (tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    addTearDown(tester.view.reset);
    await _pumpDashboard(tester, const Locale('en'));

    await tester.tap(find.text('Smart Home'));
    await tester.pumpAndSettle();
    expect(find.text('Kitchen lamp'), findsOneWidget);
    expect(tester.widget<Switch>(find.byType(Switch)).value, isTrue);
    await tester.pageBack();
    await tester.pumpAndSettle();

    await tester.tap(find.text('Financial History'));
    await tester.pumpAndSettle();
    expect(find.text('No transactions yet.'), findsOneWidget);
    expect(find.text('Net'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();

    await tester.tap(find.text('Medical History'));
    await tester.pumpAndSettle();
    expect(find.text('No records in this section yet.'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();

    await tester.tap(find.text('Travel'));
    await tester.pumpAndSettle();
    expect(find.text('This section is not available in the app yet.'), findsOneWidget);
  });

  testWidgets('cards & accounts tab shows the positions per bank', (tester) async {
    tester.view.physicalSize = const Size(1440, 6000); // every tile on screen
    addTearDown(tester.view.reset);
    await _pumpDashboard(tester, const Locale('en'));

    await tester.tap(find.text('Financial History'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cards & accounts'));
    await tester.pumpAndSettle();

    expect(find.text('Al Rajhi'), findsOneWidget);
    expect(find.text('Account ••2410'), findsOneWidget);
    expect(find.textContaining('Tap to enter the current balance'), findsOneWidget); // 8468
    expect(find.text('Visa ••1553'), findsOneWidget);
    expect(find.textContaining('Owed'), findsOneWidget);
    expect(find.textContaining('Debit card of account ••2410'), findsOneWidget);
    expect(find.text('Loan'), findsOneWidget);

    expect(find.text('SAR\u00A029,255.00'), findsOneWidget); // non-breaking space

    await tester.tap(find.text('Account ••8468'));
    await tester.pumpAndSettle();
    expect(find.text('Current balance'), findsOneWidget);
  });
}
