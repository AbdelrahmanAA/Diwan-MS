import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../api/api_client.dart';
import '../api/diwan_api.dart';
import 'bank_sms_parser.dart';

/// An SMS as the Android side hands it over (from the receiver's queue or the inbox).
class RawSms {
  RawSms({required this.sender, required this.body, required this.receivedAt, this.pendingId});

  factory RawSms.fromMap(Map<Object?, Object?> m) => RawSms(
        sender: m['sender'] as String? ?? '',
        body: m['body'] as String? ?? '',
        receivedAt: DateTime.fromMillisecondsSinceEpoch((m['receivedAt'] as num).toInt()),
        pendingId: m['id'] as String?,
      );

  final String sender;
  final String body;
  final DateTime receivedAt;

  /// Set for messages caught by the receiver; removed from its queue once uploaded.
  final String? pendingId;
}

/// Android side of SMS detection (MainActivity / SmsSyncWorker, channel `diwan/sms`).
class SmsPlatform {
  const SmsPlatform();

  static const _channel = MethodChannel('diwan/sms');

  Future<List<RawSms>> inbox(DateTime since) async =>
      ((await _channel.invokeListMethod<Object?>('inbox', {'since': since.millisecondsSinceEpoch})) ?? [])
          .map((e) => RawSms.fromMap(e as Map<Object?, Object?>))
          .toList();

  Future<List<RawSms>> pending() async => ((await _channel.invokeListMethod<Object?>('pending')) ?? [])
      .map((e) => RawSms.fromMap(e as Map<Object?, Object?>))
      .toList();

  Future<void> clearPending(List<String> ids) => _channel.invokeMethod('clearPending', {'ids': ids});

  /// Schedules (or cancels) the periodic background sync and the receiver's queue.
  Future<void> setEnabled(bool enabled) => _channel.invokeMethod('setEnabled', {'enabled': enabled});
}

class SmsSyncResult {
  SmsSyncResult({this.imported = 0, this.duplicates = 0, this.unrecognized = 0});

  final int imported;
  final int duplicates;
  final int unrecognized;
}

/// Reads new bank SMS, turns them into transactions and uploads them to diwan-transactions. Runs in the app
/// (start, resume, "sync now") and in the background worker; the server skips messages it already has, so
/// overlapping runs and re-reads are harmless.
class SmsSync {
  SmsSync({required this.api, required this.prefs, this.platform = const SmsPlatform(), BankSmsParser? parser})
      : parser = parser ?? BankSmsParser();

  // read by the Android receiver too (FlutterSharedPreferences, "flutter." prefix)
  static const enabledKey = 'diwan.sms.enabled';
  static const lastSyncKey = 'diwan.sms.lastSync';
  static const statsKey = 'diwan.sms.stats';
  static const _overlap = Duration(days: 1);
  static const _batch = 200;

  static bool get supported => Platform.isAndroid;

  final DiwanApi api;
  final SharedPreferences prefs;
  final SmsPlatform platform;
  final BankSmsParser parser;

  bool get enabled => prefs.getBool(enabledKey) ?? false;

  DateTime? get lastSync {
    final ms = prefs.getInt(lastSyncKey);
    return ms == null ? null : DateTime.fromMillisecondsSinceEpoch(ms);
  }

  /// Totals since SMS logging was turned on: imported, duplicates, unrecognized.
  Map<String, int> get stats {
    final raw = prefs.getString(statsKey);
    return raw == null ? {} : (jsonDecode(raw) as Map<String, dynamic>).map((k, v) => MapEntry(k, (v as num).toInt()));
  }

  /// Turns logging on; the first run imports the inbox from [since] (now = only new messages).
  Future<SmsSyncResult> enable(DateTime since) async {
    await prefs.setBool(enabledKey, true);
    await prefs.remove(statsKey);
    await prefs.setInt(lastSyncKey, since.millisecondsSinceEpoch);
    await platform.setEnabled(true);
    return run(since: since);
  }

  Future<void> disable() async {
    await prefs.setBool(enabledKey, false);
    await prefs.remove(lastSyncKey);
    await prefs.remove(statsKey);
    await platform.setEnabled(false);
  }

  Future<SmsSyncResult> run({DateTime? since}) async {
    if (!enabled) return SmsSyncResult();
    final started = DateTime.now();
    final from = since ?? lastSync?.subtract(_overlap) ?? started.subtract(_overlap);

    final messages = [...await platform.pending(), ...await platform.inbox(from)];
    final transactions = <String, Map<String, Object?>>{};
    final seen = <String>{}; // the receiver's queue and the inbox overlap: each SMS once
    var unrecognized = 0;
    for (final sms in messages) {
      if (!BankSmsParser.isBankSender(sms.sender) || !seen.add(BankSmsParser.fingerprint(sms.sender, sms.body))) continue;
      final result = parser.parse(sms.sender, sms.body, sms.receivedAt);
      switch (result.outcome) {
        case SmsOutcome.transaction:
          transactions[result.transaction!.sourceRef] = result.transaction!.toJson();
        case SmsOutcome.unknown:
          unrecognized++;
        case SmsOutcome.dropped:
          break;
      }
    }

    var imported = 0, duplicates = 0;
    final all = transactions.values.toList();
    for (var i = 0; i < all.length; i += _batch) {
      final r = await api.importTransactions(all.sublist(i, i + _batch > all.length ? all.length : i + _batch));
      imported += r.imported;
      duplicates += r.duplicates;
    }

    final ids = [for (final m in messages) if (m.pendingId != null) m.pendingId!];
    if (ids.isNotEmpty) await platform.clearPending(ids);
    await prefs.setInt(lastSyncKey, started.millisecondsSinceEpoch);
    final s = stats;
    await prefs.setString(statsKey, jsonEncode({
      'imported': (s['imported'] ?? 0) + imported,
      'duplicates': (s['duplicates'] ?? 0) + duplicates,
      // messages are re-read for a day after a sync, so this is the latest run's count, not a total
      'unrecognized': unrecognized,
    }));
    return SmsSyncResult(imported: imported, duplicates: duplicates, unrecognized: unrecognized);
  }
}

/// Background run (started by SmsSyncWorker with no UI): signed-in user's token from secure storage.
/// Returns false when it should be retried later (no network).
Future<bool> runSmsSyncInBackground(String? token) async {
  if (token == null) return true;
  final prefs = await SharedPreferences.getInstance();
  await prefs.reload(); // the app may have changed the settings since this isolate's cache was filled
  final sync = SmsSync(
    api: DiwanApi(ApiClient(tokenProvider: () => token, languageProvider: () => 'en')),
    prefs: prefs,
  );
  try {
    await sync.run();
    return true;
  } on ApiException catch (e) {
    return !e.isNetwork && e.status < 500; // 401 etc.: retrying will not help
  }
}
