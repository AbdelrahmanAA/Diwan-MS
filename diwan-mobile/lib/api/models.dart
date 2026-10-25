// Response models of the Diwan API (see diwan-portfolio Responses.java). Amounts are display values computed
// by the server; the app never does money arithmetic of its own.

double? _d(dynamic v) => v == null ? null : (v as num).toDouble();
DateTime? _t(dynamic v) => v == null ? null : DateTime.parse(v as String).toLocal();
List<T> _list<T>(dynamic v, T Function(Map<String, dynamic>) f) =>
    v == null ? <T>[] : (v as List).map((e) => f(e as Map<String, dynamic>)).toList();

class AuthUser {
  AuthUser({required this.token, required this.userId, required this.fullName, required this.email});

  factory AuthUser.fromJson(Map<String, dynamic> j) => AuthUser(
        token: j['token'] as String,
        userId: (j['userId'] as num).toInt(),
        fullName: (j['fullName'] ?? '') as String,
        email: (j['email'] ?? '') as String,
      );

  final String token;
  final int userId;
  final String fullName;
  final String email;
}

class CodeLabel {
  CodeLabel(this.code, this.label);

  factory CodeLabel.fromJson(Map<String, dynamic> j) => CodeLabel(j['code'] as String, j['label'] as String);

  final String code;
  final String label;
}

class Reference {
  Reference.fromJson(Map<String, dynamic> j)
      : assetTypes = _list(j['assetTypes'], CodeLabel.fromJson),
        transactionTypes = _list(j['transactionTypes'], CodeLabel.fromJson),
        alertTypes = _list(j['alertTypes'], CodeLabel.fromJson),
        countries = _list(j['countries'], CodeLabel.fromJson),
        currencies = _list(j['currencies'], CodeLabel.fromJson),
        marketDataProvider = (j['marketDataProvider'] ?? '') as String;

  final List<CodeLabel> assetTypes;
  final List<CodeLabel> transactionTypes;
  final List<CodeLabel> alertTypes;
  final List<CodeLabel> countries;
  final List<CodeLabel> currencies;
  final String marketDataProvider;

  String label(List<CodeLabel> list, String code) =>
      list.firstWhere((c) => c.code == code, orElse: () => CodeLabel(code, code)).label;
}

class Totals {
  Totals.fromJson(Map<String, dynamic> j)
      : marketValue = _d(j['marketValue']) ?? 0,
        costBasis = _d(j['costBasis']) ?? 0,
        unrealizedPnl = _d(j['unrealizedPnl']) ?? 0,
        unrealizedPnlPercent = _d(j['unrealizedPnlPercent']),
        realizedPnl = _d(j['realizedPnl']) ?? 0,
        netIncome = _d(j['netIncome']) ?? 0,
        totalReturn = _d(j['totalReturn']) ?? 0,
        totalReturnPercent = _d(j['totalReturnPercent']),
        dayChange = _d(j['dayChange']) ?? 0,
        dayChangePercent = _d(j['dayChangePercent']);

  final double marketValue;
  final double costBasis;
  final double unrealizedPnl;
  final double? unrealizedPnlPercent;
  final double realizedPnl;
  final double netIncome;
  final double totalReturn;
  final double? totalReturnPercent;
  final double dayChange;
  final double? dayChangePercent;
}

class Portfolio {
  Portfolio.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        name = j['name'] as String,
        description = j['description'] as String?,
        baseCurrency = j['baseCurrency'] as String,
        color = j['color'] as String?,
        icon = j['icon'] as String?;

  final int id;
  final String name;
  final String? description;
  final String baseCurrency;
  final String? color;
  final String? icon;
}

class PortfolioOverview {
  PortfolioOverview.fromJson(Map<String, dynamic> j)
      : portfolio = Portfolio.fromJson(j['portfolio'] as Map<String, dynamic>),
        holdingsCount = (j['holdingsCount'] as num).toInt(),
        totals = Totals.fromJson(j['totals'] as Map<String, dynamic>),
        missingRates = (j['missingRates'] as List? ?? []).cast<String>();

  final Portfolio portfolio;
  final int holdingsCount;
  final Totals totals;
  final List<String> missingRates;
}

class Tag {
  Tag.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        name = j['name'] as String,
        color = j['color'] as String?;

  final int id;
  final String name;
  final String? color;
}

class Holding {
  Holding.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        portfolioId = (j['portfolioId'] as num).toInt(),
        name = j['name'] as String,
        assetType = j['assetType'] as String,
        assetTypeLabel = j['assetTypeLabel'] as String,
        countryCode = j['countryCode'] as String,
        countryLabel = j['countryLabel'] as String,
        currency = j['currency'] as String,
        symbol = j['symbol'] as String?,
        instrumentName = j['instrumentName'] as String?,
        quantity = _d(j['quantity']) ?? 0,
        averageCostLocal = _d(j['averageCostLocal']) ?? 0,
        priceLocal = _d(j['priceLocal']),
        priceSource = j['priceSource'] as String,
        priceAsOf = _t(j['priceAsOf']),
        marketValueLocal = _d(j['marketValueLocal']) ?? 0,
        convertible = j['convertible'] as bool? ?? true,
        marketValue = _d(j['marketValue']),
        costBasis = _d(j['costBasis']),
        unrealizedPnl = _d(j['unrealizedPnl']),
        unrealizedPnlPercent = _d(j['unrealizedPnlPercent']),
        realizedPnl = _d(j['realizedPnl']),
        netIncome = _d(j['netIncome']),
        totalReturn = _d(j['totalReturn']),
        dayChange = _d(j['dayChange']),
        dayChangePercent = _d(j['dayChangePercent']),
        weightPercent = _d(j['weightPercent']),
        notes = j['notes'] as String?,
        attributes = (j['attributes'] as Map<String, dynamic>?) ?? {},
        closed = j['closed'] as bool? ?? false,
        tags = _list(j['tags'], Tag.fromJson);

  final int id;
  final int portfolioId;
  final String name;
  final String assetType;
  final String assetTypeLabel;
  final String countryCode;
  final String countryLabel;
  final String currency;
  final String? symbol;
  final String? instrumentName;
  final double quantity;
  final double averageCostLocal;
  final double? priceLocal;
  final String priceSource;
  final DateTime? priceAsOf;
  final double marketValueLocal;
  final bool convertible;
  final double? marketValue;
  final double? costBasis;
  final double? unrealizedPnl;
  final double? unrealizedPnlPercent;
  final double? realizedPnl;
  final double? netIncome;
  final double? totalReturn;
  final double? dayChange;
  final double? dayChangePercent;
  final double? weightPercent;
  final String? notes;
  final Map<String, dynamic> attributes;
  final bool closed;
  final List<Tag> tags;

  bool get isCash => assetType == 'CASH';
}

class GroupNode {
  GroupNode.fromJson(Map<String, dynamic> j)
      : dimension = j['dimension'] as String,
        key = j['key'] as String,
        label = j['label'] as String,
        weightPercent = _d(j['weightPercent']),
        totals = Totals.fromJson(j['totals'] as Map<String, dynamic>),
        groups = _list(j['groups'], GroupNode.fromJson),
        holdings = _list(j['holdings'], Holding.fromJson);

  final String dimension;
  final String key;
  final String label;
  final double? weightPercent;
  final Totals totals;
  final List<GroupNode> groups;
  final List<Holding> holdings;
}

class Summary {
  Summary.fromJson(Map<String, dynamic> j)
      : portfolio = Portfolio.fromJson(j['portfolio'] as Map<String, dynamic>),
        groupBy = (j['groupBy'] as List).cast<String>(),
        totals = Totals.fromJson(j['totals'] as Map<String, dynamic>),
        groups = _list(j['groups'], GroupNode.fromJson),
        missingRates = (j['missingRates'] as List? ?? []).cast<String>();

  final Portfolio portfolio;
  final List<String> groupBy;
  final Totals totals;
  final List<GroupNode> groups;
  final List<String> missingRates;
}

class TransactionItem {
  TransactionItem.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        type = j['type'] as String,
        typeLabel = j['typeLabel'] as String,
        tradeDate = DateTime.parse(j['tradeDate'] as String),
        quantity = _d(j['quantity']),
        price = _d(j['price']),
        amount = _d(j['amount']),
        fees = _d(j['fees']) ?? 0,
        note = j['note'] as String?;

  final int id;
  final String type;
  final String typeLabel;
  final DateTime tradeDate;
  final double? quantity;
  final double? price;
  final double? amount;
  final double fees;
  final String? note;
}

class ValuationItem {
  ValuationItem.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        valuationDate = DateTime.parse(j['valuationDate'] as String),
        value = _d(j['value']) ?? 0,
        note = j['note'] as String?;

  final int id;
  final DateTime valuationDate;
  final double value;
  final String? note;
}

class AllocationRow {
  AllocationRow.fromJson(Map<String, dynamic> j)
      : key = j['key'] as String,
        label = j['label'] as String,
        value = _d(j['value']) ?? 0,
        currentPercent = _d(j['currentPercent']) ?? 0,
        targetPercent = _d(j['targetPercent']),
        driftPercent = _d(j['driftPercent']),
        suggestedChange = _d(j['suggestedChange']);

  final String key;
  final String label;
  final double value;
  final double currentPercent;
  final double? targetPercent;
  final double? driftPercent;
  final double? suggestedChange;
}

class Allocation {
  Allocation.fromJson(Map<String, dynamic> j)
      : dimension = j['dimension'] as String,
        baseCurrency = j['baseCurrency'] as String,
        totalValue = _d(j['totalValue']) ?? 0,
        hasTargets = j['hasTargets'] as bool,
        maxDriftPercent = _d(j['maxDriftPercent']),
        rows = _list(j['rows'], AllocationRow.fromJson);

  final String dimension;
  final String baseCurrency;
  final double totalValue;
  final bool hasTargets;
  final double? maxDriftPercent;
  final List<AllocationRow> rows;
}

class Goal {
  Goal.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        name = j['name'] as String,
        targetAmount = _d(j['targetAmount']) ?? 0,
        targetDate = j['targetDate'] == null ? null : DateTime.parse(j['targetDate'] as String),
        currency = j['currency'] as String,
        currentValue = _d(j['currentValue']) ?? 0,
        progressPercent = _d(j['progressPercent']) ?? 0,
        remainingAmount = _d(j['remainingAmount']) ?? 0,
        monthsLeft = (j['monthsLeft'] as num?)?.toInt(),
        monthlyNeeded = _d(j['monthlyNeeded']),
        reached = j['reached'] as bool? ?? false;

  final int id;
  final String name;
  final double targetAmount;
  final DateTime? targetDate;
  final String currency;
  final double currentValue;
  final double progressPercent;
  final double remainingAmount;
  final int? monthsLeft;
  final double? monthlyNeeded;
  final bool reached;
}

class Quote {
  Quote.fromJson(Map<String, dynamic> j)
      : price = _d(j['price']) ?? 0,
        change = _d(j['change']),
        changePercent = _d(j['changePercent']),
        time = _t(j['time']);

  final double price;
  final double? change;
  final double? changePercent;
  final DateTime? time;
}

class Instrument {
  Instrument.fromJson(Map<String, dynamic> j)
      : symbol = j['symbol'] as String,
        name = j['name'] as String?,
        exchange = j['exchange'] as String?,
        countryCode = j['countryCode'] as String?,
        currency = j['currency'] as String?,
        assetType = j['assetType'] as String?,
        quote = j['quote'] == null ? null : Quote.fromJson(j['quote'] as Map<String, dynamic>);

  final String symbol;
  final String? name;
  final String? exchange;
  final String? countryCode;
  final String? currency;
  final String? assetType;
  final Quote? quote;
}

class SymbolMatch {
  SymbolMatch.fromJson(Map<String, dynamic> j)
      : symbol = j['symbol'] as String,
        description = (j['description'] ?? '') as String;

  final String symbol;
  final String description;
}

class PricePoint {
  PricePoint.fromJson(Map<String, dynamic> j)
      : date = DateTime.parse(j['date'] as String),
        close = _d(j['close']) ?? 0;

  final DateTime date;
  final double close;
}

class WatchItem {
  WatchItem.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        instrument = Instrument.fromJson(j['instrument'] as Map<String, dynamic>);

  final int id;
  final Instrument instrument;
}

class NewsItem {
  NewsItem.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        symbol = j['symbol'] as String,
        headline = j['headline'] as String,
        summary = j['summary'] as String?,
        source = j['source'] as String?,
        url = j['url'] as String,
        imageUrl = j['imageUrl'] as String?,
        publishedAt = _t(j['publishedAt'])!;

  final int id;
  final String symbol;
  final String headline;
  final String? summary;
  final String? source;
  final String url;
  final String? imageUrl;
  final DateTime publishedAt;
}

class AlertRule {
  AlertRule.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        type = j['type'] as String,
        typeLabel = j['typeLabel'] as String,
        symbol = j['symbol'] as String?,
        holdingId = (j['holdingId'] as num?)?.toInt(),
        portfolioId = (j['portfolioId'] as num?)?.toInt(),
        threshold = _d(j['threshold']) ?? 0,
        active = j['active'] as bool? ?? true,
        lastTriggeredAt = _t(j['lastTriggeredAt']);

  final int id;
  final String type;
  final String typeLabel;
  final String? symbol;
  final int? holdingId;
  final int? portfolioId;
  final double threshold;
  final bool active;
  final DateTime? lastTriggeredAt;
}

class AlertItem {
  AlertItem.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        type = j['type'] as String,
        title = j['title'] as String,
        message = j['message'] as String,
        triggeredAt = _t(j['triggeredAt'])!,
        readAt = _t(j['readAt']);

  final int id;
  final String type;
  final String title;
  final String message;
  final DateTime triggeredAt;
  final DateTime? readAt;
}

class ResultPage<T> {
  ResultPage(this.items, this.totalItems, this.totalPages);

  factory ResultPage.fromJson(Map<String, dynamic> j, T Function(Map<String, dynamic>) f) =>
      ResultPage(_list(j['items'], f), (j['totalItems'] as num).toInt(), (j['totalPages'] as num).toInt());

  final List<T> items;
  final int totalItems;
  final int totalPages;
}

// ---- dashboard (gateway /api/features) ----

/// A dashboard tile. [screen] tells the app which page to open; [color] is "#RRGGBB".
class Feature {
  Feature({required this.id, required this.displayName, this.icon, this.color, this.description, this.screen, this.path});

  factory Feature.fromJson(Map<String, dynamic> j) => Feature(
        id: j['id'] as String,
        displayName: (j['displayName'] ?? j['id']) as String,
        icon: j['icon'] as String?,
        color: j['color'] as String?,
        description: j['description'] as String?,
        screen: j['screen'] as String?,
        path: j['path'] as String?,
      );

  final String id;
  final String displayName;
  final String? icon;
  final String? color;
  final String? description;
  final String? screen;
  final String? path;
}

// ---- financial transactions (diwan-transactions) ----

class MoneyTransaction {
  MoneyTransaction.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        type = (j['type'] ?? 'expense') as String,
        category = (j['category'] ?? '') as String,
        amount = _d(j['amount']) ?? 0,
        currency = (j['currency'] ?? '') as String,
        bank = j['bank'] as String?,
        merchantName = j['merchantName'] as String?,
        paymentMethod = j['paymentMethod'] as String?,
        status = j['status'] as String?,
        description = j['description'] as String?,
        notes = j['notes'] as String?,
        source = (j['source'] ?? 'manual') as String,
        recordedAt = _t(j['recordedAt']);

  final int id;
  final String source; // manual | sms
  final String type; // expense | income | transfer
  final String category;
  final double amount;
  final String currency;
  final String? bank;
  final String? merchantName;
  final String? paymentMethod;
  final String? status;
  final String? description;
  final String? notes;
  final DateTime? recordedAt;

  bool get isIncome => type.toLowerCase() == 'income';
  bool get isTransfer => type.toLowerCase() == 'transfer';
  bool get fromSms => source == 'sms';
}

/// Income and expense totals of one currency.
class CurrencyTotals {
  CurrencyTotals(this.currency, this.income, this.expense);

  final String currency;
  final double income;
  final double expense;

  double get net => income - expense;

  /// {"EGP":{"income":100,"expense":40}, ...}
  static List<CurrencyTotals> listFromJson(Map<String, dynamic> j) => j.entries.map((e) {
        final m = e.value as Map<String, dynamic>;
        return CurrencyTotals(e.key, _d(m['income']) ?? 0, _d(m['expense']) ?? 0);
      }).toList()
        ..sort((a, b) => a.currency.compareTo(b.currency));
}

// ---- medical records (diwan-medical) ----

/// One record of any category; the category decides which [fields] are used (see medical_screen.dart).
class MedicalRecord {
  MedicalRecord.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        category = j['category'] as String,
        notes = j['notes'] as String?,
        recordedAt = _t(j['recordedAt'] ?? j['createdAt']),
        fields = j;

  final int id;
  final String category;
  final String? notes;
  final DateTime? recordedAt;
  final Map<String, dynamic> fields;

  String? text(String key) {
    final v = fields[key];
    return v == null || v.toString().isEmpty ? null : v.toString();
  }
}

// ---- smart home (diwan-smarthome) ----

class SmartDevice {
  SmartDevice.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        deviceId = (j['deviceId'] ?? '') as String,
        name = (j['name'] ?? '') as String,
        state = (j['state'] ?? 'UNKNOWN') as String;

  final int id;
  final String deviceId;
  final String name;
  final String state; // ON | OFF | UNKNOWN

  bool get isOn => state == 'ON';
}
