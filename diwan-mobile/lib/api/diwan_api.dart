import 'api_client.dart';
import 'models.dart';

/// Every call the app makes. Amount fields are sent as strings so decimals reach the server unchanged.
class DiwanApi {
  DiwanApi(this.http);

  final ApiClient http;
  static const _p = '/api/portfolio';

  // ---- auth (diwan-users) ----
  Future<AuthUser> login(String email, String password) async {
    final r = await http.post('/api/users/login', {'email': email, 'password': password});
    return AuthUser.fromJson((r as Map<String, dynamic>)['data'] as Map<String, dynamic>);
  }

  Future<AuthUser> register(String fullName, String email, String password) async {
    final r = await http.post('/api/users/register', {'fullName': fullName, 'email': email, 'password': password});
    return AuthUser.fromJson((r as Map<String, dynamic>)['data'] as Map<String, dynamic>);
  }

  Future<void> logout() => http.post('/api/users/logout');

  // ---- reference & preferences ----
  Future<Reference> reference() async => Reference.fromJson(await http.get('$_p/reference') as Map<String, dynamic>);

  Future<void> savePreferences({String? language, String? baseCurrency}) =>
      http.put('$_p/preferences', {'language': language, 'baseCurrency': baseCurrency});

  // ---- portfolios ----
  Future<List<PortfolioOverview>> portfolios() async =>
      (await http.get('$_p/portfolios') as List).map((e) => PortfolioOverview.fromJson(e as Map<String, dynamic>)).toList();

  Future<Portfolio> savePortfolio(int? id, Map<String, Object?> body) async {
    final r = id == null ? await http.post('$_p/portfolios', body) : await http.put('$_p/portfolios/$id', body);
    return Portfolio.fromJson(r as Map<String, dynamic>);
  }

  Future<void> deletePortfolio(int id) => http.delete('$_p/portfolios/$id');

  Future<Summary> summary(int id, {List<String>? groupBy, int? tagId}) async => Summary.fromJson(await http.get(
        '$_p/portfolios/$id/summary',
        query: {'groupBy': groupBy?.join(','), 'tagId': tagId},
      ) as Map<String, dynamic>);

  Future<Allocation> allocation(int id, String dimension) async => Allocation.fromJson(
      await http.get('$_p/portfolios/$id/allocation', query: {'dimension': dimension}) as Map<String, dynamic>);

  Future<void> setTargets(int id, String dimension, Map<String, String> percentByKey) => http.put(
        '$_p/portfolios/$id/targets',
        {
          'dimension': dimension,
          'targets': percentByKey.entries.map((e) => {'key': e.key, 'percent': e.value}).toList(),
        },
      );

  Future<List<Goal>> goals(int portfolioId) async =>
      (await http.get('$_p/portfolios/$portfolioId/goals') as List).map((e) => Goal.fromJson(e as Map<String, dynamic>)).toList();

  Future<void> saveGoal(int portfolioId, int? goalId, Map<String, Object?> body) =>
      goalId == null ? http.post('$_p/portfolios/$portfolioId/goals', body) : http.put('$_p/goals/$goalId', body);

  Future<void> deleteGoal(int goalId) => http.delete('$_p/goals/$goalId');

  // ---- holdings ----
  Future<Holding> addHolding(int portfolioId, Map<String, Object?> body) async =>
      Holding.fromJson(await http.post('$_p/portfolios/$portfolioId/holdings', body) as Map<String, dynamic>);

  Future<Holding> updateHolding(int id, Map<String, Object?> body) async =>
      Holding.fromJson(await http.put('$_p/holdings/$id', body) as Map<String, dynamic>);

  Future<Holding> holding(int id) async => Holding.fromJson(await http.get('$_p/holdings/$id') as Map<String, dynamic>);

  Future<void> deleteHolding(int id) => http.delete('$_p/holdings/$id');

  Future<void> setClosed(int id, bool closed) => http.post('$_p/holdings/$id/${closed ? 'close' : 'reopen'}');

  Future<List<TransactionItem>> transactions(int holdingId) async => (await http.get('$_p/holdings/$holdingId/transactions') as List)
      .map((e) => TransactionItem.fromJson(e as Map<String, dynamic>))
      .toList();

  Future<void> addTransaction(int holdingId, Map<String, Object?> body) => http.post('$_p/holdings/$holdingId/transactions', body);

  Future<void> deleteTransaction(int id) => http.delete('$_p/transactions/$id');

  Future<List<ValuationItem>> valuations(int holdingId) async => (await http.get('$_p/holdings/$holdingId/valuations') as List)
      .map((e) => ValuationItem.fromJson(e as Map<String, dynamic>))
      .toList();

  Future<void> addValuation(int holdingId, Map<String, Object?> body) => http.post('$_p/holdings/$holdingId/valuations', body);

  // ---- market ----
  Future<List<SymbolMatch>> search(String q) async => (await http.get('$_p/instruments/search', query: {'q': q}) as List)
      .map((e) => SymbolMatch.fromJson(e as Map<String, dynamic>))
      .toList();

  Future<Instrument> instrument(String symbol) async =>
      Instrument.fromJson(await http.get('$_p/instruments/${Uri.encodeComponent(symbol)}') as Map<String, dynamic>);

  Future<List<PricePoint>> history(String symbol, int days) async =>
      (await http.get('$_p/instruments/${Uri.encodeComponent(symbol)}/history', query: {'days': days}) as List)
          .map((e) => PricePoint.fromJson(e as Map<String, dynamic>))
          .toList();

  Future<List<WatchItem>> watchlist() async =>
      (await http.get('$_p/watchlist') as List).map((e) => WatchItem.fromJson(e as Map<String, dynamic>)).toList();

  Future<void> watch(String symbol) => http.post('$_p/watchlist', {'symbol': symbol});

  Future<void> unwatch(String symbol) => http.delete('$_p/watchlist/${Uri.encodeComponent(symbol)}');

  Future<ResultPage<NewsItem>> news({int? portfolioId, String? symbol, int page = 0}) async => ResultPage.fromJson(
      await http.get('$_p/news', query: {'portfolioId': portfolioId, 'symbol': symbol, 'page': page, 'size': 20})
          as Map<String, dynamic>,
      NewsItem.fromJson);

  // ---- alerts ----
  Future<List<AlertRule>> alertRules() async =>
      (await http.get('$_p/alerts/rules') as List).map((e) => AlertRule.fromJson(e as Map<String, dynamic>)).toList();

  Future<void> createAlertRule(Map<String, Object?> body) => http.post('$_p/alerts/rules', body);

  Future<void> deleteAlertRule(int id) => http.delete('$_p/alerts/rules/$id');

  Future<ResultPage<AlertItem>> alerts({int page = 0}) async => ResultPage.fromJson(
      await http.get('$_p/alerts', query: {'page': page, 'size': 30}) as Map<String, dynamic>, AlertItem.fromJson);

  Future<int> unreadAlerts() async => ((await http.get('$_p/alerts/unread-count') as Map)['unread'] as num).toInt();

  Future<void> markAllRead() => http.post('$_p/alerts/read-all');
}
