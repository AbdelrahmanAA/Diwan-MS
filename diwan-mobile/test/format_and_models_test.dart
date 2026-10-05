import 'dart:convert';

import 'package:diwan_mobile/api/api_client.dart';
import 'package:diwan_mobile/api/models.dart';
import 'package:diwan_mobile/ui/format.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

void main() {
  group('normalizeNumber', () {
    test('accepts Western and Arabic-Indic digits', () {
      expect(normalizeNumber('12.5'), '12.5');
      expect(normalizeNumber('١٢٫٥'), '12.5');
      expect(normalizeNumber(' ١٬٠٠٠ '), '1000');
      expect(normalizeNumber('3,75'), '3.75');
    });

    test('rejects garbage', () {
      expect(normalizeNumber('abc'), isNull);
      expect(normalizeNumber(''), isNull);
    });
  });

  test('summary JSON from the API parses into nested groups', () {
    final json = jsonDecode('''
      {"portfolio":{"id":1,"name":"Retirement","baseCurrency":"USD"},
       "groupBy":["COUNTRY","ASSET_TYPE"],
       "totals":{"marketValue":122000.00,"costBasis":102000.00,"unrealizedPnl":20000.00,"unrealizedPnlPercent":19.61,
                 "realizedPnl":0,"netIncome":0,"totalReturn":20000,"totalReturnPercent":19.61,"dayChange":0,"dayChangePercent":0},
       "groups":[{"dimension":"COUNTRY","key":"EG","label":"مصر","weightPercent":100,
                  "totals":{"marketValue":122000},
                  "groups":[{"dimension":"ASSET_TYPE","key":"REAL_ESTATE","label":"عقارات","weightPercent":98.36,
                             "totals":{"marketValue":120000},"groups":[],
                             "holdings":[{"id":4,"portfolioId":1,"name":"شقة","assetType":"REAL_ESTATE","assetTypeLabel":"عقارات",
                                          "countryCode":"EG","countryLabel":"مصر","currency":"EGP","quantity":1,"averageCostLocal":4850000,
                                          "priceLocal":5820000,"priceSource":"VALUATION","marketValueLocal":5820000,"convertible":true,
                                          "marketValue":120000,"unrealizedPnl":20000,"attributes":{"address":"New Cairo"},"tags":[]}]}],
                  "holdings":[]}],
       "missingRates":[]}''') as Map<String, dynamic>;

    final s = Summary.fromJson(json);

    expect(s.totals.marketValue, 122000);
    expect(s.groups.single.label, 'مصر');
    final flat = s.groups.single.groups.single.holdings.single;
    expect(flat.name, 'شقة');
    expect(flat.attributes['address'], 'New Cairo');
    expect(flat.priceSource, 'VALUATION');
  });

  test('API errors carry the server code and localized message', () async {
    final client = ApiClient(
      baseUrl: 'https://api.test',
      tokenProvider: () => 'tok',
      languageProvider: () => 'ar',
      httpClient: MockClient((req) async {
        expect(req.headers['Authorization'], 'Bearer tok');
        expect(req.headers['Accept-Language'], 'ar');
        return http.Response.bytes(
            utf8.encode('{"success":false,"code":"portfolio.not_found","message":"المحفظة غير موجودة"}'), 404,
            headers: {'content-type': 'application/json'});
      }),
    );

    expect(
      () => client.get('/api/portfolio/portfolios/9'),
      throwsA(isA<ApiException>()
          .having((e) => e.status, 'status', 404)
          .having((e) => e.code, 'code', 'portfolio.not_found')
          .having((e) => e.message, 'message', 'المحفظة غير موجودة')),
    );
  });

  test('401 with a token signs the user out', () async {
    var expired = false;
    final client = ApiClient(
      baseUrl: 'https://api.test',
      tokenProvider: () => 'old',
      languageProvider: () => 'en',
      onUnauthorized: () => expired = true,
      httpClient: MockClient((_) async => http.Response('', 401)),
    );
    await expectLater(client.get('/x'), throwsA(isA<ApiException>()));
    expect(expired, isTrue);
  });
}
