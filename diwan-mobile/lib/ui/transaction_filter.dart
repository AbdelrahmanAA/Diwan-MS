import '../api/models.dart';

/// Filters and grouping of the transactions list. The app loads all of the user's transactions once and
/// filters them on the phone, so every change is instant and the totals always match what is shown.

enum TxPeriod { all, thisMonth, lastMonth, last3Months, thisYear, custom }

enum TxSource { all, sms, manual }

class TxFilter {
  const TxFilter({
    this.type,
    this.period = TxPeriod.all,
    this.from,
    this.to,
    this.categories = const {},
    this.banks = const {},
    this.products = const {},
    this.source = TxSource.all,
    this.minAmount,
    this.maxAmount,
    this.query = '',
  });

  final String? type; // null = all, else income | expense | transfer
  final TxPeriod period;
  final DateTime? from; // custom period, both days included
  final DateTime? to;
  final Set<String> categories;
  final Set<String> banks;
  final Set<String> products; // productKey()s: 'card:1553', 'account:2410'
  final TxSource source;
  final double? minAmount;
  final double? maxAmount;
  final String query;

  static const none = TxFilter();

  /// Filters chosen in the filter sheet (type and search have their own controls).
  int get sheetCount => [
        period != TxPeriod.all,
        categories.isNotEmpty,
        banks.isNotEmpty,
        products.isNotEmpty,
        source != TxSource.all,
        minAmount != null || maxAmount != null,
      ].where((b) => b).length;

  TxFilter copyWith({
    Object? type = _keep,
    TxPeriod? period,
    Object? from = _keep,
    Object? to = _keep,
    Set<String>? categories,
    Set<String>? banks,
    Set<String>? products,
    TxSource? source,
    Object? minAmount = _keep,
    Object? maxAmount = _keep,
    String? query,
  }) =>
      TxFilter(
        type: identical(type, _keep) ? this.type : type as String?,
        period: period ?? this.period,
        from: identical(from, _keep) ? this.from : from as DateTime?,
        to: identical(to, _keep) ? this.to : to as DateTime?,
        categories: categories ?? this.categories,
        banks: banks ?? this.banks,
        products: products ?? this.products,
        source: source ?? this.source,
        minAmount: identical(minAmount, _keep) ? this.minAmount : minAmount as double?,
        maxAmount: identical(maxAmount, _keep) ? this.maxAmount : maxAmount as double?,
        query: query ?? this.query,
      );

  /// Keeps type and search, clears the rest.
  TxFilter clearSheet() => TxFilter(type: type, query: query);

  /// [start, end) of the period, or null for all time.
  (DateTime, DateTime)? range(DateTime now) {
    final month = DateTime(now.year, now.month);
    return switch (period) {
      TxPeriod.all => null,
      TxPeriod.thisMonth => (month, DateTime(now.year, now.month + 1)),
      TxPeriod.lastMonth => (DateTime(now.year, now.month - 1), month),
      TxPeriod.last3Months => (DateTime(now.year, now.month - 2), DateTime(now.year, now.month + 1)),
      TxPeriod.thisYear => (DateTime(now.year), DateTime(now.year + 1)),
      TxPeriod.custom => from == null || to == null
          ? null
          : (DateTime(from!.year, from!.month, from!.day), DateTime(to!.year, to!.month, to!.day + 1)),
    };
  }

  bool matches(MoneyTransaction tx, DateTime now) {
    if (type != null && tx.type.toLowerCase() != type) return false;
    final r = range(now);
    if (r != null) {
      final at = tx.recordedAt;
      if (at == null || at.isBefore(r.$1) || !at.isBefore(r.$2)) return false;
    }
    if (categories.isNotEmpty && !categories.contains(tx.category.toLowerCase())) return false;
    if (banks.isNotEmpty && !banks.contains(bankOf(tx))) return false;
    if (products.isNotEmpty && !productKeys(tx).any(products.contains)) return false;
    if (source == TxSource.sms && !tx.fromSms) return false;
    if (source == TxSource.manual && tx.fromSms) return false;
    if (minAmount != null && tx.amount < minAmount!) return false;
    if (maxAmount != null && tx.amount > maxAmount!) return false;
    final q = query.trim().toLowerCase();
    if (q.isNotEmpty &&
        ![tx.merchantName, tx.description, tx.notes, tx.bank].any((s) => s != null && s.toLowerCase().contains(q))) {
      return false;
    }
    return true;
  }

  List<MoneyTransaction> apply(List<MoneyTransaction> list, DateTime now) =>
      list.where((tx) => matches(tx, now)).toList();
}

const _keep = Object();

/// The bank of a transaction, null when unknown ("Unknown" is the service's default).
String? bankOf(MoneyTransaction tx) {
  final b = tx.bank?.trim();
  return b == null || b.isEmpty || b == 'Unknown' ? null : b;
}

/// The cards and accounts a transaction touched ("By:5101;mada  From:2410" -> both).
List<String> productKeys(MoneyTransaction tx) => [
      if (tx.cardRef != null) 'card:${tx.cardRef}',
      if (tx.accountRef != null) 'account:${tx.accountRef}',
    ];

// ── grouping ─────────────────────────────────────────────────────────────

enum TxGroupBy { none, day, month, category, merchant, bank, product }

/// Income and expense of one currency; transfers between own accounts count in neither.
class TxTotals {
  double income = 0;
  double expense = 0;
  double get net => income - expense;
}

Map<String, TxTotals> totalsOf(Iterable<MoneyTransaction> list) {
  final out = <String, TxTotals>{};
  for (final tx in list) {
    if (tx.isTransfer) continue;
    final t = out.putIfAbsent(tx.currency, TxTotals.new);
    if (tx.isIncome) {
      t.income += tx.amount;
    } else {
      t.expense += tx.amount;
    }
  }
  return out;
}

class TxGroup {
  TxGroup(this.key, this.items);

  /// What the group is: a date (day/month), a category code, a merchant name, a bank, a productKey, or
  /// null for "other" (no merchant / bank / card).
  final Object? key;
  final List<MoneyTransaction> items;
  late final Map<String, TxTotals> totals = totalsOf(items);

  double get _spent => totals.values.fold(0.0, (s, t) => s + t.expense);
}

/// Groups [list] (newest first) by [by]. Date groups stay newest first; the other groups are ordered by
/// what was spent in them, biggest first, with "other" last.
List<TxGroup> groupTransactions(List<MoneyTransaction> list, TxGroupBy by) {
  if (by == TxGroupBy.none) return [TxGroup(null, list)];
  final groups = <Object?, List<MoneyTransaction>>{};
  final names = <String, String>{}; // merchant: lower case -> first spelling seen
  for (final tx in list) {
    final key = switch (by) {
      TxGroupBy.none => null,
      TxGroupBy.day => tx.recordedAt == null ? null : DateTime(tx.recordedAt!.year, tx.recordedAt!.month, tx.recordedAt!.day),
      TxGroupBy.month => tx.recordedAt == null ? null : DateTime(tx.recordedAt!.year, tx.recordedAt!.month),
      TxGroupBy.category => tx.category.toLowerCase(),
      TxGroupBy.merchant => _merchant(tx, names),
      TxGroupBy.bank => bankOf(tx),
      TxGroupBy.product => productKeys(tx).firstOrNull,
    };
    groups.putIfAbsent(key, () => []).add(tx);
  }
  final result = [for (final e in groups.entries) TxGroup(e.key, e.value)];
  if (by == TxGroupBy.day || by == TxGroupBy.month) {
    result.sort((a, b) {
      if (a.key == null || b.key == null) return a.key == null ? 1 : -1;
      return (b.key as DateTime).compareTo(a.key as DateTime);
    });
  } else {
    result.sort((a, b) {
      if (a.key == null || b.key == null) return a.key == null ? (b.key == null ? 0 : 1) : -1;
      final bySpent = b._spent.compareTo(a._spent);
      return bySpent != 0 ? bySpent : b.items.length.compareTo(a.items.length);
    });
  }
  return result;
}

String? _merchant(MoneyTransaction tx, Map<String, String> names) {
  final m = [tx.merchantName, tx.description].map((s) => s?.trim()).firstWhere((s) => s != null && s.isNotEmpty,
      orElse: () => null);
  if (m == null) return null;
  return names.putIfAbsent(m.toLowerCase(), () => m);
}
