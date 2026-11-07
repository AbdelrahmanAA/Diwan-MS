import 'dart:convert';

import 'package:diwan_mobile/l10n/app_localizations.dart';
import 'package:diwan_mobile/state/session.dart';
import 'package:diwan_mobile/ui/alerts_tab.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

class _Server {
  final created = <Map<String, dynamic>>[];

  late final client = MockClient((request) async {
    final path = request.url.path;
    if (path.endsWith('/reference')) {
      return _json({
        'alertTypes': [
          {'code': 'PRICE_ABOVE', 'label': 'Price above'},
        ],
      });
    }
    if (path.endsWith('/preferences')) return _json({'language': 'en', 'baseCurrency': 'SAR'});
    if (path.endsWith('/instruments/search')) {
      return _json(request.url.queryParameters['q'] == 'gold'
          ? [{'symbol': 'GOLD-24K', 'description': 'Gold 24K (per gram)'}]
          : [{'symbol': '2222.SR', 'description': 'Saudi Aramco'}]);
    }
    if (path.endsWith('/alerts/rules') && request.method == 'POST') {
      created.add(jsonDecode(request.body) as Map<String, dynamic>);
      return _json({});
    }
    if (path.endsWith('/alerts/rules')) {
      return _json([
        for (final r in created)
          {'id': 1, 'type': 'PRICE_ABOVE', 'typeLabel': 'Price above', 'symbol': r['symbol'], 'threshold': 30, 'currency': 'SAR'},
      ]);
    }
    if (path.endsWith('/alerts')) return _json({'items': [], 'page': 0, 'size': 20, 'totalItems': 0, 'totalPages': 0});
    return _json([]);
  });

  static http.Response _json(Object body) =>
      http.Response(jsonEncode(body), 200, headers: {'content-type': 'application/json'});
}

Future<void> _openForm(WidgetTester tester, _Server server) async {
  tester.view.physicalSize = const Size(900, 1600);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(ChangeNotifierProvider(
    create: (_) => Session(httpClient: server.client),
    child: const MaterialApp(
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: AlertsTab(),
    ),
  ));
  await tester.pumpAndSettle();
  await tester.tap(find.widgetWithText(Tab, 'Alert rules'));
  await tester.pumpAndSettle();
  await tester.tap(find.byType(FloatingActionButton));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('a Saudi stock is picked from the search and saved with its full symbol', (tester) async {
    final server = _Server();
    await _openForm(tester, server);

    await tester.tap(find.byIcon(Icons.search));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, 'aramco');
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Saudi Aramco'));
    await tester.pumpAndSettle();
    expect(find.text('2222 · Saudi Aramco'), findsOneWidget);

    await tester.enterText(find.byType(TextFormField).last, '30');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();
    expect(server.created.single['symbol'], '2222.SR');
    expect(find.textContaining('2222.SR'), findsNothing);
  });

  testWidgets('a gold price rule is asked per gram in the preferred currency', (tester) async {
    await _openForm(tester, _Server());
    await tester.tap(find.byIcon(Icons.search));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, 'gold');
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Gold 24K (per gram)'));
    await tester.pumpAndSettle();
    expect(find.text('Price per gram'), findsOneWidget);
    expect(find.text('SAR'), findsOneWidget);
  });

  testWidgets('a price rule needs a symbol', (tester) async {
    final server = _Server();
    await _openForm(tester, server);
    await tester.enterText(find.byType(TextFormField).last, '30');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();
    expect(find.text('Required'), findsOneWidget);
    expect(server.created, isEmpty);
  });
}
