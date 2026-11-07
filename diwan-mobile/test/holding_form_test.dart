import 'dart:convert';

import 'package:diwan_mobile/l10n/app_localizations.dart';
import 'package:diwan_mobile/state/session.dart';
import 'package:diwan_mobile/ui/holding_form.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

const _reference = {
  'assetTypes': [
    {'code': 'STOCK', 'label': 'Stocks'},
    {'code': 'COMMODITY', 'label': 'Gold and commodities'},
  ],
  'currencies': [
    {'code': 'SAR', 'label': 'Saudi Riyal'},
    {'code': 'USD', 'label': 'US Dollar'},
  ],
};

/// A gold wallet in SAR. [priceInCurrency] is null when the server has no exchange rate.
class _Server {
  _Server(this.priceInCurrency);

  final double? priceInCurrency;
  final saved = <Map<String, dynamic>>[];
  final askedCurrencies = <String?>[];

  late final client = MockClient((request) async {
    final path = request.url.path;
    if (path.endsWith('/reference')) return _json(_reference);
    if (path.contains('/instruments/')) {
      askedCurrencies.add(request.url.queryParameters['currency']);
      final prices = {'GOLD-24K': priceInCurrency, 'GOLD-18K': priceInCurrency == null ? null : priceInCurrency! * 0.75};
      return _json({
        'symbol': path.split('/').last,
        'currency': 'USD',
        'quote': {'price': 130.9211},
        'priceInCurrency': prices[path.split('/').last],
      });
    }
    saved.add(jsonDecode(request.body) as Map<String, dynamic>);
    return http.Response('{}', 500);
  });

  static http.Response _json(Object body) =>
      http.Response(jsonEncode(body), 200, headers: {'content-type': 'application/json'});
}

Future<void> _openGoldForm(WidgetTester tester, _Server server) async {
  tester.view.physicalSize = const Size(900, 3000);
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
      home: HoldingForm(portfolioId: 1, baseCurrency: 'SAR'),
    ),
  ));
  await tester.pumpAndSettle();
  await tester.tap(find.byType(DropdownButtonFormField<String>));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Gold and commodities').last);
  await tester.pumpAndSettle();
}

Finder _chip(String label) => find.widgetWithText(ChoiceChip, label);

Finder _field(String label) => find.widgetWithText(TextFormField, label);

String _text(WidgetTester tester, String label) => tester.widget<TextFormField>(_field(label)).controller!.text;

void main() {
  testWidgets('gold price is suggested in the currency of the wallet', (tester) async {
    final server = _Server(490.9541);
    await _openGoldForm(tester, server);
    await tester.tap(_chip('Gold 24K'));
    await tester.pumpAndSettle();
    expect(server.askedCurrencies, ['SAR']);
    expect(_text(tester, 'Price per gram'), '490.9541');
  });

  testWidgets('another karat replaces the suggested price but not a typed one', (tester) async {
    await _openGoldForm(tester, _Server(400));
    await tester.tap(_chip('Gold 24K'));
    await tester.pumpAndSettle();
    await tester.tap(_chip('Gold 18K'));
    await tester.pumpAndSettle();
    expect(_text(tester, 'Price per gram'), '300.0');

    await tester.enterText(_field('Price per gram'), '350');
    await tester.tap(_chip('Gold 24K'));
    await tester.pumpAndSettle();
    expect(_text(tester, 'Price per gram'), '350');
  });

  testWidgets('grams without a price are refused instead of saving nothing', (tester) async {
    final server = _Server(null);
    await _openGoldForm(tester, server);
    await tester.tap(_chip('Gold 24K'));
    await tester.pumpAndSettle();
    expect(_text(tester, 'Price per gram'), isEmpty);

    await tester.enterText(_field('Grams'), '20');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();
    expect(find.text('Required'), findsOneWidget);
    expect(server.saved, isEmpty);
  });
}
