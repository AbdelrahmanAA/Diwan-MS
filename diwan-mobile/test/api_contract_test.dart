// End-to-end check of the app's API client and models against a running gateway. Skipped unless a URL is given:
//   flutter test test/api_contract_test.dart --dart-define=CONTRACT_API=http://localhost:8087
// Registers a throw-away user (uses 1 of the 5 register attempts per minute), exercises the portfolio API in
// Arabic, and deletes the user again.
import 'package:diwan_mobile/api/api_client.dart';
import 'package:diwan_mobile/api/diwan_api.dart';
import 'package:flutter_test/flutter_test.dart';

const _base = String.fromEnvironment('CONTRACT_API');

void main() {
  test('app models parse every portfolio response of the real API', () async {
    String? token;
    final api = DiwanApi(ApiClient(baseUrl: _base, tokenProvider: () => token, languageProvider: () => 'ar'));
    final today = DateTime.now().toIso8601String().substring(0, 10);

    final user = await api.register('Contract Test', 'contract-${DateTime.now().millisecondsSinceEpoch}@diwan.test', 'Passw0rd!x');
    token = user.token;
    try {
      final ref = await api.reference();
      expect(ref.label(ref.assetTypes, 'REAL_ESTATE'), 'عقارات');
      expect(ref.label(ref.countries, 'EG'), 'مصر');

      final p = await api.savePortfolio(null, {'name': 'محفظة الاختبار', 'baseCurrency': 'USD'});
      final stock = await api.addHolding(p.id, {
        'assetType': 'STOCK', 'name': 'Apple', 'symbol': 'AAPL',
        'initialTransaction': {'type': 'BUY', 'tradeDate': today, 'quantity': '3', 'price': '100'},
      });
      expect(stock.countryCode, 'US');
      expect(stock.priceSource, 'QUOTE');
      final flat = await api.addHolding(p.id, {
        'assetType': 'REAL_ESTATE', 'name': 'شقة', 'countryCode': 'EG', 'currency': 'EGP',
        'attributes': {'address': 'القاهرة'},
        'initialTransaction': {'type': 'BUY', 'tradeDate': today, 'quantity': '1', 'price': '2000000'},
      });
      await api.addValuation(flat.id, {'valuationDate': today, 'value': '2500000'});
      await api.addTransaction(stock.id, {'type': 'SELL', 'tradeDate': today, 'quantity': '1', 'price': '120'});

      final summary = await api.summary(p.id, groupBy: ['COUNTRY', 'ASSET_TYPE']);
      final eg = summary.groups.firstWhere((g) => g.key == 'EG');
      expect(eg.label, 'مصر');
      expect(eg.groups.single.holdings.single.attributes['address'], 'القاهرة');
      expect(eg.groups.single.holdings.single.unrealizedPnl, greaterThan(0));

      expect((await api.holding(stock.id)).quantity, 2);
      expect((await api.transactions(stock.id)).first.type, 'SELL');
      expect(await api.valuations(flat.id), hasLength(1));
      expect((await api.portfolios()).single.holdingsCount, 2);

      await api.setTargets(p.id, 'ASSET_TYPE', {'STOCK': '50', 'REAL_ESTATE': '50'});
      final alloc = await api.allocation(p.id, 'ASSET_TYPE');
      expect(alloc.hasTargets, isTrue);
      await api.saveGoal(p.id, null, {'name': 'هدف', 'targetAmount': '1000000'});
      expect((await api.goals(p.id)).single.progressPercent, greaterThan(0));

      expect(await api.search('apple'), isNotEmpty);
      expect((await api.instrument('AAPL')).quote, isNotNull);
      await api.history('AAPL', 30);
      await api.watch('MSFT');
      expect((await api.watchlist()).single.instrument.symbol, 'MSFT');
      await api.createAlertRule({'type': 'LOSS_PERCENT', 'holdingId': stock.id, 'threshold': '10'});
      expect((await api.alertRules()).single.type, 'LOSS_PERCENT');
      await api.alerts();
      await api.unreadAlerts();
      await api.news();

      Object? error;
      try {
        await api.addTransaction(stock.id, {'type': 'SELL', 'tradeDate': today, 'quantity': '99', 'price': '1'});
      } catch (e) {
        error = e;
      }
      expect(error, isA<ApiException>().having((e) => e.code, 'code', 'transaction.exceeds_quantity'));
      expect((error as ApiException).message, contains('تبيع'));

      await api.deletePortfolio(p.id);
    } finally {
      await api.http.delete('/api/users/${user.userId}');
    }
  }, skip: _base.isEmpty ? 'set --dart-define=CONTRACT_API=<gateway url> to run' : false);

  test('dashboard features and the other sections (money, medical, smart home) parse', () async {
    String? token;
    final api = DiwanApi(ApiClient(baseUrl: _base, tokenProvider: () => token, languageProvider: () => 'en'));
    final features = await api.features();
    expect(features.map((f) => f.screen), containsAll(['Portfolio', 'Transactions', 'MedicalHistory', 'SmartHome', 'UpdateUserInfo']));

    final user = await api.register('Contract Test', 'contract2-${DateTime.now().millisecondsSinceEpoch}@diwan.test', 'Passw0rd!x');
    token = user.token;
    var deleted = false;
    try {
      await api.saveMoneyTransaction(null, {
        'type': 'income', 'category': 'salary', 'amount': '1000.50', 'currency': 'EGP', 'recordedAt': '2026-10-01T09:00:00',
      });
      await api.saveMoneyTransaction(null, {'type': 'expense', 'category': 'food', 'amount': '200', 'currency': 'EGP'});
      final all = await api.moneyTransactions();
      expect(all, hasLength(2));
      expect((await api.moneyTransactions(type: 'expense')).single.category, 'food');
      final egp = (await api.moneySummary()).single;
      expect(egp.income, 1000.5);
      expect(egp.net, 800.5);
      final food = all.firstWhere((t) => t.category == 'food');
      await api.saveMoneyTransaction(food.id, {'merchantName': 'Carrefour', 'amount': '250'});
      expect((await api.moneyTransactions(type: 'expense')).single.merchantName, 'Carrefour');
      await api.deleteMoneyTransaction(food.id);

      await api.saveMedicalRecord(null, {'category': 'medications', 'name': 'Aspirin', 'dosage': '100mg', 'startDate': '2026-01-15'});
      await api.saveMedicalRecord(null, {'category': 'bloodType', 'bloodType': 'O+'});
      final meds = await api.medicalRecords(category: 'medications');
      expect(meds.single.text('name'), 'Aspirin');
      expect(meds.single.text('startDate'), '2026-01-15');
      await api.saveMedicalRecord(meds.single.id, {'category': 'medications', 'dosage': '75mg'});
      expect((await api.medicalRecords(category: 'medications')).single.text('dosage'), '75mg');
      await api.deleteMedicalRecord(meds.single.id, 'medications');
      expect(await api.medicalRecords(category: 'medications'), isEmpty);

      await api.addDevice('contract-lamp', 'Lamp');
      final lamp = (await api.devices()).single;
      expect(lamp.state, isNot('ON'));
      await api.renameDevice(lamp.id, 'Kitchen lamp');
      expect((await api.switchDevice(lamp.id, true)).isOn, isTrue);
      expect((await api.devices()).single.name, 'Kitchen lamp');
      await api.removeDevice(lamp.id);
      expect(await api.devices(), isEmpty);

      await api.deleteAccount(user.userId);
      deleted = true;
    } finally {
      if (!deleted) await api.http.delete('/api/users/${user.userId}');
    }
  }, skip: _base.isEmpty ? 'set --dart-define=CONTRACT_API=<gateway url> to run' : false);
}
