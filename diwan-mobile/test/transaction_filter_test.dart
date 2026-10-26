import 'package:diwan_mobile/api/models.dart';
import 'package:diwan_mobile/ui/transaction_filter.dart';
import 'package:flutter_test/flutter_test.dart';

MoneyTransaction tx(int id, String type, String category, double amount, String at,
        {String currency = 'SAR', String? merchant, String bank = 'Al Rajhi', String? card, String? account, String source = 'sms', String? notes}) =>
    MoneyTransaction.fromJson({
      'id': id,
      'type': type,
      'category': category,
      'amount': amount,
      'currency': currency,
      'merchantName': merchant,
      'bank': bank,
      'cardRef': card,
      'cardNetwork': card == null ? null : 'Visa',
      'accountRef': account,
      'source': source,
      'notes': notes,
      'recordedAt': at,
    });

final now = DateTime(2026, 10, 26, 9);

// newest first, like the service returns them
final list = [
  tx(1, 'expense', 'food', 45, '2026-10-26T08:10:00', merchant: 'Starbucks', card: '1553'),
  tx(2, 'transfer', 'transfer', 5500, '2026-10-25T22:26:00', account: '2410'),
  tx(3, 'expense', 'shopping', 103.51, '2026-10-25T21:18:00', merchant: 'TASALIMAR', card: '1553'),
  tx(4, 'expense', 'food', 30, '2026-10-24T13:00:00', merchant: 'starbucks', card: '5101', account: '2410'),
  tx(5, 'income', 'salary', 34965, '2026-10-01T00:23:00', account: '2410'),
  tx(6, 'expense', 'bills', 300, '2026-09-15T10:00:00', bank: 'Unknown', source: 'manual', notes: 'electricity'),
  tx(7, 'expense', 'shopping', 20, '2026-09-02T10:00:00', currency: 'USD', merchant: 'Amazon', card: '1553'),
  tx(8, 'income', 'salary', 30000, '2025-12-25T00:20:00', account: '2410'),
];

List<int> ids(TxFilter f) => f.apply(list, now).map((t) => t.id).toList();

void main() {
  group('filter', () {
    test('no filter keeps everything', () => expect(ids(TxFilter.none), [1, 2, 3, 4, 5, 6, 7, 8]));

    test('type', () => expect(ids(const TxFilter(type: 'income')), [5, 8]));

    test('periods', () {
      expect(ids(const TxFilter(period: TxPeriod.thisMonth)), [1, 2, 3, 4, 5]);
      expect(ids(const TxFilter(period: TxPeriod.lastMonth)), [6, 7]);
      expect(ids(const TxFilter(period: TxPeriod.last3Months)), [1, 2, 3, 4, 5, 6, 7]);
      expect(ids(const TxFilter(period: TxPeriod.thisYear)), [1, 2, 3, 4, 5, 6, 7]);
    });

    test('custom period includes both days', () {
      final f = TxFilter(period: TxPeriod.custom, from: DateTime(2026, 10, 24), to: DateTime(2026, 10, 25));
      expect(ids(f), [2, 3, 4]);
    });

    test('last month in January is December', () {
      final f = const TxFilter(period: TxPeriod.lastMonth);
      expect(f.apply(list, DateTime(2026, 1, 10)).map((t) => t.id), [8]);
    });

    test('categories, banks, cards and accounts', () {
      expect(ids(const TxFilter(categories: {'food', 'bills'})), [1, 4, 6]);
      expect(ids(const TxFilter(banks: {'Al Rajhi'})), [1, 2, 3, 4, 5, 7, 8]); // "Unknown" is no bank
      expect(ids(const TxFilter(products: {'card:1553'})), [1, 3, 7]);
      expect(ids(const TxFilter(products: {'account:2410'})), [2, 4, 5, 8]); // 4 was paid by its mada card
    });

    test('source, amount and search', () {
      expect(ids(const TxFilter(source: TxSource.manual)), [6]);
      expect(ids(const TxFilter(source: TxSource.sms)).length, 7);
      expect(ids(const TxFilter(minAmount: 100, maxAmount: 5500)), [2, 3, 6]);
      expect(ids(const TxFilter(query: 'STARB')), [1, 4]);
      expect(ids(const TxFilter(query: 'electric')), [6]); // notes
    });

    test('filters combine', () {
      expect(ids(const TxFilter(type: 'expense', period: TxPeriod.thisMonth, products: {'card:1553'})), [1, 3]);
    });

    test('clearSheet keeps type and search; sheetCount counts the rest', () {
      const f = TxFilter(type: 'expense', query: 'x', period: TxPeriod.thisYear, categories: {'food'}, minAmount: 5);
      expect(f.sheetCount, 3);
      final cleared = f.clearSheet();
      expect((cleared.type, cleared.query, cleared.sheetCount), ('expense', 'x', 0));
    });

    test('copyWith can set a value back to null', () {
      const f = TxFilter(type: 'income', minAmount: 5);
      expect(f.copyWith(type: null).type, isNull);
      expect(f.copyWith(minAmount: null).minAmount, isNull);
      expect(f.copyWith(query: 'a').type, 'income');
    });
  });

  group('grouping', () {
    test('totals per currency leave transfers out', () {
      final t = totalsOf(list);
      expect(t['SAR']!.income, 64965);
      expect(t['SAR']!.expense, closeTo(478.51, 1e-9));
      expect(t['USD']!.expense, 20);
    });

    test('by day, newest first', () {
      final g = groupTransactions(list, TxGroupBy.day);
      expect(g.first.key, DateTime(2026, 10, 26));
      expect(g[1].key, DateTime(2026, 10, 25));
      expect(g[1].items.map((t) => t.id), [2, 3]);
      expect(g.last.key, DateTime(2025, 12, 25));
    });

    test('by month', () {
      final g = groupTransactions(list, TxGroupBy.month);
      expect(g.map((x) => x.key), [DateTime(2026, 10), DateTime(2026, 9), DateTime(2025, 12)]);
      expect(g.first.totals['SAR']!.net, closeTo(34965 - 178.51, 1e-9));
    });

    test('by category: biggest spending first', () {
      final g = groupTransactions(list, TxGroupBy.category);
      expect(g.map((x) => x.key), ['bills', 'shopping', 'food', 'salary', 'transfer']);
    });

    test('by merchant: case does not split a merchant; no merchant is "other", last', () {
      final g = groupTransactions(list, TxGroupBy.merchant);
      expect(g.first.key, 'TASALIMAR');
      final starbucks = g.firstWhere((x) => x.key == 'Starbucks');
      expect(starbucks.items.map((t) => t.id), [1, 4]);
      expect(g.last.key, isNull);
      expect(g.last.items.map((t) => t.id), [2, 5, 6, 8]);
    });

    test('by card or account: the card wins when an SMS names both', () {
      final g = groupTransactions(list, TxGroupBy.product);
      final keys = g.map((x) => x.key).toList();
      expect(keys, containsAll(['card:1553', 'card:5101', 'account:2410', null]));
      expect(g.firstWhere((x) => x.key == 'card:5101').items.map((t) => t.id), [4]);
      expect(keys.last, isNull);
    });

    test('by bank', () {
      final g = groupTransactions(list, TxGroupBy.bank);
      expect(g.map((x) => x.key), ['Al Rajhi', null]);
    });

    test('none is one group of everything', () {
      final g = groupTransactions(list, TxGroupBy.none);
      expect(g.single.items.length, list.length);
    });
  });
}
