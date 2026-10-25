import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:intl/intl.dart';

/// What the parser made of one SMS.
enum SmsOutcome {
  /// A transaction to store.
  transaction,

  /// Deliberately ignored: OTP / verification codes, declined transactions.
  dropped,

  /// From the bank but not a message type we know (counted, never sent).
  unknown,
}

/// A transaction read from a bank SMS, in the shape of diwan-transactions' import request. Only these fields
/// leave the phone; the SMS text itself never does.
class SmsTransaction {
  SmsTransaction({
    required this.sourceRef,
    required this.type,
    required this.category,
    required this.amount,
    required this.currency,
    required this.paymentMethod,
    required this.recordedAt,
    required this.description,
    this.merchantName,
    this.referenceNumber,
    this.notes,
    this.accountRef,
    this.counterAccountRef,
    this.cardRef,
    this.cardNetwork,
    this.balance,
    this.balanceCurrency,
    this.balanceOf,
  });

  final String sourceRef;
  final String type; // expense | income | transfer
  final String category;
  final String amount; // decimal text, sent unchanged
  final String currency;
  final String paymentMethod;
  final DateTime recordedAt;
  final String description;
  final String? merchantName;
  final String? referenceNumber;
  final String? notes;

  /// Own account debited / credited (last 4 digits); the receiving own account of an own transfer.
  final String? accountRef;
  final String? counterAccountRef;

  /// Card used (last 4 digits) and its network (Visa, mada, ...).
  final String? cardRef;
  final String? cardNetwork;

  /// Balance the SMS reported after the transaction: a card's available amount (CARD), an account's
  /// balance (ACCOUNT) or a loan's remaining amount (LOAN).
  final String? balance;
  final String? balanceCurrency;
  final String? balanceOf;

  Map<String, Object?> toJson() => {
        'sourceRef': sourceRef,
        'type': type,
        'category': category,
        'amount': amount,
        'currency': currency,
        'paymentMethod': paymentMethod,
        'status': 'completed',
        'bank': BankSmsParser.bankName,
        'tags': 'sms',
        'recordedAt': DateFormat("yyyy-MM-dd'T'HH:mm:ss").format(recordedAt),
        'description': description,
        'merchantName': merchantName,
        'referenceNumber': referenceNumber,
        'notes': notes,
        'accountRef': accountRef,
        'counterAccountRef': counterAccountRef,
        'cardRef': cardRef,
        'cardNetwork': cardNetwork,
        'balance': balance,
        'balanceCurrency': balanceCurrency,
        'balanceOf': balanceOf,
      };
}

class SmsParseResult {
  SmsParseResult(this.outcome, {this.transaction, this.reason});

  final SmsOutcome outcome;
  final SmsTransaction? transaction;
  final String? reason;
}

/// One message type: the title (first line) it starts with and how it is stored.
class _Rule {
  const _Rule(this.title, this.type, this.category, this.payment);

  final String title; // regexp on the normalized title
  final String? type; // null = drop
  final String category;
  final String payment;
}

/// Reads Al Rajhi Bank transaction SMS (English). Every message is a title line, `Key:Value` lines and a date
/// line; the title decides the meaning, the generic key/value reader extracts the fields. Rules are data so
/// further banks can be added (later: configured by the user / served by the backend).
class BankSmsParser {
  static const bankName = 'Al Rajhi';

  /// Senders whose messages are read (letters only, lower case); keep in sync with SmsStore.kt.
  static const senders = ['alrajhi'];

  static const _rules = [
    _Rule(r'^transaction declined', null, '', ''),
    _Rule(r'^(pos purchase|online purchase)', 'expense', 'shopping', 'card'),
    _Rule(r'^withdrawal:atm', 'expense', 'cash', 'cash'),
    _Rule(r'^deposit:atm', 'income', 'cash', 'cash'),
    _Rule(r'^deposit:', 'income', 'profit', 'transfer'),
    _Rule(r'^(credit card refund|reverse transaction)', 'income', 'refund', 'card'),
    _Rule(r'^credit transfer salary', 'income', 'salary', 'transfer'),
    _Rule(r'^credit transfer', 'income', 'transfer', 'transfer'),
    _Rule(r'^(transfer between your accounts|credit card:payment)', 'transfer', 'transfer', 'transfer'),
    _Rule(r'^debit:.*loan|loan instal', 'expense', 'loan', 'transfer'),
    _Rule(r'^debit.*transfer', 'expense', 'transfer', 'transfer'),
    _Rule(r'^bill payment', 'expense', 'bills', 'transfer'),
  ];

  static final _otp = RegExp(r'\botp\b|verification code|one[- ]time password', caseSensitive: false);
  static final _dateLine =
      RegExp(r'^(?:date\s*:\s*)?(\d{1,2})/(\d{1,2})/(\d{2})\s+(\d{1,2}):(\d{2})$', caseSensitive: false);
  static final _account = RegExp(r'^\d{4}$');
  static final _money = RegExp(r'([A-Z]{2,3})?\s*(\d[\d,]*(?:\.\d+)?)\s*([A-Z]{2,3})?');

  static String _letters(String s) => s.toLowerCase().replaceAll(RegExp(r'[^a-z]'), '');

  static bool isBankSender(String sender) => senders.any(_letters(sender).contains);

  static bool isOtp(String body) => _otp.hasMatch(body);

  /// Stable fingerprint of a message: the same SMS read by the receiver and from the inbox gives the same value.
  static String fingerprint(String sender, String body) =>
      sha256.convert(utf8.encode('${_letters(sender)}\n${_normalizeBody(body)}')).toString();

  static String _normalizeBody(String body) =>
      body.replaceAll('\r\n', '\n').split('\n').map((l) => l.trim()).where((l) => l.isNotEmpty).join('\n');

  SmsParseResult parse(String sender, String body, DateTime receivedAt) {
    final lines = _normalizeBody(body).split('\n');
    if (lines.isEmpty || lines.first.isEmpty) return SmsParseResult(SmsOutcome.unknown, reason: 'empty');
    if (isOtp(body)) return SmsParseResult(SmsOutcome.dropped, reason: 'otp');

    final title = lines.first.toLowerCase().replaceAll(RegExp(r'\s*:\s*'), ':').replaceAll(RegExp(r'\s+'), ' ');
    final rule = _rules.cast<_Rule?>().firstWhere((r) => RegExp(r!.title).hasMatch(title), orElse: () => null);
    if (rule == null) return SmsParseResult(SmsOutcome.unknown, reason: 'type');
    if (rule.type == null) return SmsParseResult(SmsOutcome.dropped, reason: 'declined');

    // key/value lines in order (keys repeat: "To:MOHAMED HASSAN" then "To:2368")
    final fields = <MapEntry<String, String>>[];
    final labels = <String>[];
    RegExpMatch? date;
    for (final line in lines.skip(1)) {
      final d = _dateLine.firstMatch(line);
      if (d != null) {
        date = d;
        continue;
      }
      final i = line.indexOf(RegExp(r'[:~]'));
      if (i <= 0) continue;
      labels.add(line.substring(0, i).trim());
      fields.add(MapEntry(line.substring(0, i).trim().toLowerCase(), line.substring(i + 1).trim()));
    }
    final used = <int>{};
    int? find(bool Function(String key, String value) test) {
      for (var i = 0; i < fields.length; i++) {
        if (!used.contains(i) && test(fields[i].key, fields[i].value)) return i;
      }
      return null;
    }

    String? take(int? i) {
      if (i == null) return null;
      used.add(i);
      return fields[i].value;
    }

    // amount: what was charged in the account currency first
    ({String amount, String currency})? money;
    final total = find((k, _) => k == 'total due amount');
    final amountIndex = find((k, _) => k == 'amount' || k == 'instalment');
    if (total != null) {
      money = _parseMoney(take(total)!);
    } else if (amountIndex != null) {
      final value = fields[amountIndex].value;
      final inner = RegExp(r'\(([^)]*)\)').firstMatch(value);
      money = _parseMoney(inner?.group(1) ?? value) ?? _parseMoney(value);
      if (inner == null) used.add(amountIndex); // "57USD(214.05 SAR)" stays in the notes
    }
    if (money == null) return SmsParseResult(SmsOutcome.unknown, reason: 'amount');

    // counterparty: shop, else the other person of a transfer, else the biller
    final isAccount = _account.hasMatch;
    final merchant = take(find((k, _) => k == 'at' || k == 'place')) ??
        take(find((k, v) => k == (rule.type == 'income' ? 'from' : 'to') && !isAccount(v))) ??
        take(find((k, _) => k == 'service'));
    final reference = take(find((k, _) => k == 'bill' || k == 'reference' || k == 'ref'));

    var payment = rule.payment;
    final card = find((k, _) => k == 'card' || k == 'by');
    if (payment == 'card' && card != null && fields[card].value.toLowerCase().contains('pay')) payment = 'wallet';

    // positions: which own account / card, and the balance after the transaction
    String? value(bool Function(String key, String value) test) =>
        fields.where((f) => test(f.key, f.value)).map((f) => f.value).firstOrNull;
    final cardValue = value((k, _) => k == 'card' || k == 'by');
    final cardRef = cardValue == null ? null : RegExp(r'\d{4}').firstMatch(cardValue)?.group(0);
    final cardNetwork = cardValue
        ?.replaceAll(RegExp(r'\d'), '')
        .split(RegExp(r'[;\-]'))
        .map((s) => s.trim())
        .firstWhere((s) => s.isNotEmpty, orElse: () => '');
    final from = value((k, v) => k == 'from' && isAccount(v));
    final to = value((k, v) => k == 'to' && isAccount(v));
    final accountRef = rule.type == 'income' ? to : from;
    final counterAccountRef = rule.type == 'transfer' ? to : null;
    final remaining = value((k, _) => k == 'remaining amount');
    final balanceText = remaining ?? value((k, _) => k == 'balance');
    final balance = balanceText == null ? null : _parseMoney(balanceText);
    final balanceOf = balance == null
        ? null
        : remaining != null
            ? 'LOAN'
            : cardRef != null
                ? 'CARD'
                : accountRef != null
                    ? 'ACCOUNT'
                    : null;

    final notes = [
      for (var i = 0; i < fields.length; i++)
        if (!used.contains(i)) '${labels[i]}: ${fields[i].value.replaceAll(RegExp(r'\s*;\s*'), ' ')}',
    ].join(' · ');

    return SmsParseResult(
      SmsOutcome.transaction,
      transaction: SmsTransaction(
        sourceRef: fingerprint(sender, body),
        type: rule.type!,
        category: rule.category,
        amount: money.amount,
        currency: money.currency,
        paymentMethod: payment,
        recordedAt: _resolveDate(date, receivedAt),
        description: lines.first,
        merchantName: merchant,
        referenceNumber: reference,
        notes: notes.isEmpty ? null : notes,
        accountRef: accountRef,
        counterAccountRef: counterAccountRef,
        cardRef: cardRef,
        cardNetwork: cardNetwork == null || cardNetwork.isEmpty ? null : cardNetwork,
        balance: balanceOf == null ? null : balance!.amount,
        balanceCurrency: balanceOf == null ? null : balance!.currency,
        balanceOf: balanceOf,
      ),
    );
  }

  static ({String amount, String currency})? _parseMoney(String value) {
    for (final m in _money.allMatches(value.toUpperCase())) {
      final amount = m.group(2)!.replaceAll(',', '');
      if (double.tryParse(amount) == null) continue;
      var currency = m.group(1) ?? m.group(3) ?? 'SAR';
      if (currency == 'SR') currency = 'SAR';
      return (amount: amount, currency: currency);
    }
    return null;
  }

  /// The bank writes dates as d/m/yy and as yy/mm/dd (both seen in the same day's messages), so the reading
  /// closest to the time the SMS arrived wins; if neither is within 3 days, the arrival time is used.
  static DateTime _resolveDate(RegExpMatch? m, DateTime receivedAt) {
    if (m == null) return receivedAt;
    final a = int.parse(m.group(1)!), b = int.parse(m.group(2)!), c = int.parse(m.group(3)!);
    final h = int.parse(m.group(4)!), min = int.parse(m.group(5)!);
    DateTime? valid(int year, int month, int day) {
      final d = DateTime(year, month, day, h, min);
      return d.month == month && d.day == day ? d : null;
    }

    final candidates = [valid(2000 + c, b, a), valid(2000 + a, b, c)].whereType<DateTime>().toList()
      ..sort((x, y) => x.difference(receivedAt).abs().compareTo(y.difference(receivedAt).abs()));
    if (candidates.isEmpty || candidates.first.difference(receivedAt).abs() > const Duration(days: 3)) return receivedAt;
    return candidates.first;
  }
}
