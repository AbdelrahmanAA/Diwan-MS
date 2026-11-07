import 'dart:convert';

import 'package:diwan_mobile/l10n/app_localizations.dart';
import 'package:diwan_mobile/state/session.dart';
import 'package:diwan_mobile/ui/watchlist_tab.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

class _Server {
  final watched = <String>[];
  final goldCurrencies = <String?>[];

  late final client = MockClient((request) async {
    final path = request.url.path;
    if (path.endsWith('/reference')) {
      return _json({
        'currencies': [
          {'code': 'SAR', 'label': 'Saudi Riyal'},
          {'code': 'USD', 'label': 'US Dollar'},
        ],
      });
    }
    if (path.endsWith('/gold-prices')) {
      final currency = request.url.queryParameters['currency'];
      goldCurrencies.add(currency);
      return _json({
        'currency': currency ?? 'SAR',
        'time': '2026-11-07T06:53:06Z',
        'prices': [
          {'karat': 24, 'unit': 'GRAM', 'price': 490.95, 'changePercent': 1.5},
          {'karat': 21, 'unit': 'GRAM', 'price': 429.58, 'changePercent': 1.5},
          {'karat': 24, 'unit': 'OUNCE', 'price': 15270.0, 'changePercent': 1.5},
        ],
      });
    }
    if (path.endsWith('/instruments/search')) {
      return _json(switch (request.url.queryParameters['q']) {
        'aramco' => [{'symbol': '2222.SR', 'description': 'Saudi Aramco'}],
        'gold' => [{'symbol': 'GOLD-24K', 'description': 'Gold 24K (per gram)'}],
        _ => [],
      });
    }
    if (path.endsWith('/watchlist') && request.method == 'POST') {
      watched.add((jsonDecode(request.body) as Map)['symbol'] as String);
    }
    if (path.endsWith('/watchlist')) {
      return _json([
        for (final s in watched)
          {'id': 1, 'instrument': {'symbol': s, 'name': 'Saudi Aramco', 'currency': 'SAR'}},
      ]);
    }
    return _json({});
  });

  static http.Response _json(Object body) =>
      http.Response(jsonEncode(body), 200, headers: {'content-type': 'application/json'});
}

Future<void> _open(WidgetTester tester, _Server server) async {
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
      home: WatchlistTab(),
    ),
  ));
  await tester.pumpAndSettle();
}

Future<void> _search(WidgetTester tester, String text) async {
  await tester.tap(find.byType(FloatingActionButton));
  await tester.pumpAndSettle();
  await tester.enterText(find.byType(TextField), text);
  await tester.pump(const Duration(milliseconds: 500));
  await tester.pumpAndSettle();
}

Future<void> _openGold(WidgetTester tester) async {
  await tester.tap(find.widgetWithText(Tab, 'Gold'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('a Saudi stock is found by name and shown without its suffix', (tester) async {
    final server = _Server();
    await _open(tester, server);
    await _search(tester, 'aramco');
    expect(find.text('2222'), findsOneWidget);
    await tester.tap(find.text('Saudi Aramco'));
    await tester.pumpAndSettle();
    expect(server.watched, ['2222.SR']);
    expect(find.text('2222'), findsOneWidget);
    expect(find.text('2222.SR'), findsNothing);
  });

  testWidgets('a search without results says so', (tester) async {
    await _open(tester, _Server());
    await _search(tester, 'nothing');
    expect(find.textContaining('Nothing found'), findsOneWidget);
  });

  testWidgets('gold is not offered in the stocks search', (tester) async {
    await _open(tester, _Server());
    await _search(tester, 'gold');
    expect(find.textContaining('Gold 24K'), findsNothing);
    expect(find.textContaining('Nothing found'), findsOneWidget);
  });

  testWidgets('the gold tab shows 24K, 21K and the ounce in the preferred currency', (tester) async {
    final server = _Server();
    await _open(tester, server);
    await _openGold(tester);
    expect(server.goldCurrencies, [null]);
    expect(find.text('SAR'), findsOneWidget);
    expect(find.text('Gold 24K, 1 gram'), findsOneWidget);
    expect(find.text('Gold 21K, 1 gram'), findsOneWidget);
    expect(find.text('Gold, 1 ounce'), findsOneWidget);
    expect(find.byType(FloatingActionButton), findsNothing);
  });

  testWidgets('another currency can be picked on the gold tab', (tester) async {
    final server = _Server();
    await _open(tester, server);
    await _openGold(tester);
    await tester.tap(find.text('Currency'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('US Dollar'));
    await tester.pumpAndSettle();
    expect(server.goldCurrencies.last, 'USD');
    expect(find.text('USD'), findsOneWidget);
  });
}
