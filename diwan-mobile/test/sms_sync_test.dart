import 'dart:convert';

import 'package:diwan_mobile/api/api_client.dart';
import 'package:diwan_mobile/api/diwan_api.dart';
import 'package:diwan_mobile/sms/sms_sync.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _FakePlatform extends SmsPlatform {
  _FakePlatform(this.inboxMessages, this.pendingMessages);

  final List<RawSms> inboxMessages;
  final List<RawSms> pendingMessages;
  DateTime? inboxSince;
  final cleared = <String>[];
  bool? enabled;

  @override
  Future<List<RawSms>> inbox(DateTime since) async {
    inboxSince = since;
    return inboxMessages;
  }

  @override
  Future<List<RawSms>> pending() async => pendingMessages;

  @override
  Future<void> clearPending(List<String> ids) async => cleared.addAll(ids);

  @override
  Future<void> setEnabled(bool enabled) async => this.enabled = enabled;
}

const _purchase = 'PoS purchase\nCard:1553 ;Visa-Samsung Pay\nAt: TASALIMAR\nAmount:103.51 SAR\n25/10/26 21:18';
const _salary = 'Credit transfer Salary\nAmount:SAR 34965\nTo:2410\n25/10/26 00:23';
final _at = DateTime(2026, 10, 25, 21, 18);

void main() {
  test('uploads each bank transaction once, drops OTPs and other senders, clears the receiver queue', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final uploads = <List<dynamic>>[];
    final api = DiwanApi(ApiClient(
      baseUrl: 'http://gw',
      tokenProvider: () => 't',
      languageProvider: () => 'en',
      httpClient: MockClient((req) async {
        expect(req.url.path, '/api/transactions/import');
        final list = (jsonDecode(req.body) as Map<String, dynamic>)['transactions'] as List;
        uploads.add(list);
        return http.Response(jsonEncode({'success': true, 'imported': list.length, 'duplicates': 0}), 200);
      }),
    ));
    final platform = _FakePlatform(
      [
        RawSms(sender: 'AlRajhiBank', body: _purchase, receivedAt: _at), // also in the queue
        RawSms(sender: 'AlRajhiBank', body: _salary, receivedAt: DateTime(2026, 10, 25, 0, 23)),
        RawSms(sender: 'AlRajhiBank', body: 'OTP Code:7549\nReason:Beneficiary Addition', receivedAt: _at),
        RawSms(sender: 'AlRajhiBank', body: 'Our branches are closed on Friday', receivedAt: _at),
        RawSms(sender: 'Mom', body: 'Amount: 5 SAR\nAt: bakery', receivedAt: _at),
      ],
      [RawSms(sender: 'AlRajhiBank', body: _purchase, receivedAt: _at, pendingId: 'p1')],
    );
    final sync = SmsSync(api: api, prefs: prefs, platform: platform);

    final since = DateTime(2026, 7, 25);
    final result = await sync.enable(since);

    expect(platform.enabled, isTrue);
    expect(platform.inboxSince, since);
    expect(uploads.single.map((t) => t['category']), unorderedEquals(['shopping', 'salary']));
    expect(result.imported, 2);
    expect(result.unrecognized, 1);
    expect(platform.cleared, ['p1']);
    expect(sync.stats['imported'], 2);
    expect(sync.lastSync, isNotNull);

    // the next run reads the inbox again from a day before the last sync
    await sync.run();
    expect(platform.inboxSince!.isBefore(sync.lastSync!), isTrue);

    await sync.disable();
    expect(platform.enabled, isFalse);
    expect(sync.enabled, isFalse);
    expect(await sync.run(), isA<SmsSyncResult>().having((r) => r.imported, 'imported', 0));
  });

  test('a network failure keeps the receiver queue and the last sync time', () async {
    SharedPreferences.setMockInitialValues({SmsSync.enabledKey: true});
    final prefs = await SharedPreferences.getInstance();
    final api = DiwanApi(ApiClient(
      baseUrl: 'http://gw',
      tokenProvider: () => 't',
      languageProvider: () => 'en',
      httpClient: MockClient((_) async => throw http.ClientException('offline')),
    ));
    final platform = _FakePlatform([], [RawSms(sender: 'AlRajhiBank', body: _purchase, receivedAt: _at, pendingId: 'p1')]);
    final sync = SmsSync(api: api, prefs: prefs, platform: platform);

    await expectLater(sync.run(), throwsA(isA<ApiException>().having((e) => e.isNetwork, 'network', isTrue)));
    expect(platform.cleared, isEmpty);
    expect(sync.lastSync, isNull);
  });
}
