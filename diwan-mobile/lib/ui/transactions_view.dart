import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'money_screen.dart';
import 'positions_view.dart';
import 'transaction_filter.dart';
import 'widgets.dart';

/// The transactions list with search, filters (period, category, bank, card/account, source, amount) and
/// grouping (day, month, category, merchant, bank, card/account). The totals are those of what is shown.
class TransactionsView extends StatefulWidget {
  const TransactionsView({super.key});

  static const groupByKey = 'diwan.tx.groupBy';

  @override
  State<TransactionsView> createState() => _TransactionsViewState();
}

class _TransactionsViewState extends State<TransactionsView> {
  TxFilter _filter = TxFilter.none;
  TxGroupBy _groupBy = TxGroupBy.none;
  Key _key = UniqueKey();
  final _search = TextEditingController();
  List<MoneyTransaction> _all = const [];

  @override
  void initState() {
    super.initState();
    _restoreGroupBy();
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<void> _restoreGroupBy() async {
    try {
      final name = (await SharedPreferences.getInstance()).getString(TransactionsView.groupByKey);
      final g = TxGroupBy.values.where((v) => v.name == name).firstOrNull;
      if (g != null && mounted) setState(() => _groupBy = g);
    } catch (_) {
      // a remembered choice is only a convenience
    }
  }

  Future<void> _setGroupBy(TxGroupBy g) async {
    setState(() => _groupBy = g);
    try {
      await (await SharedPreferences.getInstance()).setString(TransactionsView.groupByKey, g.name);
    } catch (_) {}
  }

  Future<void> _open([MoneyTransaction? tx]) async {
    final changed = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => MoneyTransactionForm(transaction: tx)));
    if (changed == true) setState(() => _key = UniqueKey());
  }

  Future<void> _openFilters() async {
    final f = await showModalBottomSheet<TxFilter>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      showDragHandle: true,
      builder: (_) => _FilterSheet(all: _all, initial: _filter),
    );
    if (f != null) setState(() => _filter = f);
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add),
        label: Text(t.newTransaction),
        onPressed: _open,
      ),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 8, 4, 0),
          child: Row(children: [
            Expanded(
              child: TextField(
                controller: _search,
                textInputAction: TextInputAction.search,
                decoration: InputDecoration(
                  isDense: true,
                  hintText: t.searchTransactions,
                  prefixIcon: const Icon(Icons.search),
                  border: const OutlineInputBorder(),
                  suffixIcon: _filter.query.isEmpty
                      ? null
                      : IconButton(
                          tooltip: t.clearAll,
                          icon: const Icon(Icons.close),
                          onPressed: () => setState(() {
                            _search.clear();
                            _filter = _filter.copyWith(query: '');
                          }),
                        ),
                ),
                onChanged: (v) => setState(() => _filter = _filter.copyWith(query: v)),
              ),
            ),
            IconButton(
              tooltip: t.filters,
              onPressed: _openFilters,
              icon: Badge(
                isLabelVisible: _filter.sheetCount > 0,
                label: Text('${_filter.sheetCount}'),
                child: const Icon(Icons.filter_list),
              ),
            ),
            PopupMenuButton<TxGroupBy>(
              tooltip: t.groupBy,
              icon: const Icon(Icons.workspaces_outlined),
              initialValue: _groupBy,
              onSelected: _setGroupBy,
              itemBuilder: (_) => [
                PopupMenuItem(enabled: false, child: Text(t.groupBy, style: Theme.of(context).textTheme.labelLarge)),
                for (final g in TxGroupBy.values)
                  CheckedPopupMenuItem(value: g, checked: g == _groupBy, child: Text(_groupByLabel(t, g))),
              ],
            ),
          ]),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
          child: SegmentedButton<String?>(
            segments: [
              ButtonSegment(value: null, label: Text(t.all)),
              ButtonSegment(value: 'income', label: Text(t.incomeType)),
              ButtonSegment(value: 'expense', label: Text(t.expenseType)),
              ButtonSegment(value: 'transfer', label: Text(t.transferType)),
            ],
            selected: {_filter.type},
            onSelectionChanged: (s) => setState(() => _filter = _filter.copyWith(type: s.first)),
          ),
        ),
        if (_filter.sheetCount > 0) _ActiveFilters(all: _all, filter: _filter, onChanged: (f) => setState(() => _filter = f)),
        Expanded(
          child: Loader<List<MoneyTransaction>>(
            key: _key,
            load: () async => _all = await api.moneyTransactions(),
            builder: (context, all, reload) => _list(context, all),
          ),
        ),
      ]),
    );
  }

  Widget _list(BuildContext context, List<MoneyTransaction> all) {
    final t = AppLocalizations.of(context);
    final shown = _filter.apply(all, DateTime.now());
    final groups = groupTransactions(shown, _groupBy);
    final totals = totalsOf(shown);
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 96),
      children: [
        if (all.isEmpty)
          _Empty(icon: Icons.receipt_long_outlined, text: t.noMoneyTransactions)
        else if (shown.isEmpty) ...[
          _Empty(icon: Icons.filter_alt_off_outlined, text: t.noMatchingTransactions),
          Center(
            child: TextButton(
              onPressed: () => setState(() {
                _search.clear();
                _filter = TxFilter.none;
              }),
              child: Text(t.clearAll),
            ),
          ),
        ] else ...[
          for (final e in totals.entries) _TotalsCard(currency: e.key, totals: e.value),
          Padding(
            padding: const EdgeInsets.fromLTRB(4, 4, 4, 4),
            child: Text(t.txCount(shown.length), style: Theme.of(context).textTheme.bodySmall),
          ),
          for (final g in groups) ..._group(context, g),
        ],
      ],
    );
  }

  List<Widget> _group(BuildContext context, TxGroup g) {
    final tiles = [for (final tx in g.items) MoneyTile(tx: tx, onTap: () => _open(tx))];
    switch (_groupBy) {
      case TxGroupBy.none:
        return tiles;
      case TxGroupBy.day:
      case TxGroupBy.month:
        return [_DateHeader(group: g, monthly: _groupBy == TxGroupBy.month), ...tiles];
      default:
        return [
          Card(
            clipBehavior: Clip.antiAlias,
            child: ExpansionTile(
              key: PageStorageKey('${_groupBy.name}:${g.key}'),
              leading: CircleAvatar(child: Icon(_groupIcon(_groupBy, g.key))),
              title: Row(children: [
                Expanded(child: Text(groupLabel(context, _groupBy, g), maxLines: 1, overflow: TextOverflow.ellipsis)),
                _GroupTotals(totals: g.totals),
              ]),
              subtitle: Text(AppLocalizations.of(context).txCount(g.items.length)),
              children: tiles,
            ),
          ),
        ];
    }
  }
}

String _groupByLabel(AppLocalizations t, TxGroupBy g) => switch (g) {
      TxGroupBy.none => t.groupNone,
      TxGroupBy.day => t.groupDay,
      TxGroupBy.month => t.groupMonth,
      TxGroupBy.category => t.category,
      TxGroupBy.merchant => t.merchant,
      TxGroupBy.bank => t.bank,
      TxGroupBy.product => t.groupProduct,
    };

IconData _groupIcon(TxGroupBy by, Object? key) => switch (by) {
      TxGroupBy.category => categoryIcon((key ?? '') as String),
      TxGroupBy.merchant => Icons.storefront_outlined,
      TxGroupBy.bank => Icons.account_balance_outlined,
      TxGroupBy.product => '$key'.startsWith('card:') ? Icons.credit_card : Icons.account_balance_wallet_outlined,
      _ => Icons.category_outlined,
    };

/// "Visa ••1553" / "Account ••2410" (as on the cards & accounts tab) for a [productKeys] key.
String productLabel(AppLocalizations t, String key, {String? network}) {
  final i = key.indexOf(':');
  final ref = key.substring(i + 1);
  final kind = key.substring(0, i) == 'card' ? (network ?? t.card) : t.account;
  return '$kind ••$ref';
}

String groupLabel(BuildContext context, TxGroupBy by, TxGroup g) {
  final t = AppLocalizations.of(context);
  final key = g.key;
  if (key == null) return t.otherGroup;
  return switch (by) {
    TxGroupBy.category => categoryLabel(t, key as String),
    TxGroupBy.bank => bankLabel(context, key as String),
    TxGroupBy.product => productLabel(t, key as String,
        network: g.items.map((tx) => tx.cardNetwork).whereType<String>().firstOrNull),
    _ => '$key',
  };
}

class _DateHeader extends StatelessWidget {
  const _DateHeader({required this.group, required this.monthly});

  final TxGroup group;
  final bool monthly;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    final d = group.key as DateTime?;
    String title;
    if (d == null) {
      title = t.otherGroup;
    } else if (monthly) {
      title = DateFormat.yMMMM(locale).format(d);
    } else {
      final today = DateUtils.dateOnly(DateTime.now());
      title = d == today
          ? t.today
          : d == today.subtract(const Duration(days: 1))
              ? t.yesterday
              : DateFormat.yMMMEd(locale).format(d);
    }
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 16, 4, 4),
      child: Row(children: [
        Expanded(
          child: Text.rich(TextSpan(children: [
            TextSpan(text: title, style: Theme.of(context).textTheme.titleSmall),
            TextSpan(text: '  ·  ${t.txCount(group.items.length)}', style: Theme.of(context).textTheme.bodySmall),
          ])),
        ),
        _GroupTotals(totals: group.totals),
      ]),
    );
  }
}

/// Net of a group per currency.
class _GroupTotals extends StatelessWidget {
  const _GroupTotals({required this.totals});

  final Map<String, TxTotals> totals;

  @override
  Widget build(BuildContext context) => Column(
        crossAxisAlignment: CrossAxisAlignment.end,
        mainAxisSize: MainAxisSize.min,
        children: [
          for (final e in totals.entries)
            PnlText(value: e.value.net, currency: e.key, style: Theme.of(context).textTheme.labelLarge),
        ],
      );
}

class _Empty extends StatelessWidget {
  const _Empty({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(top: 48, bottom: 8),
        child: Column(children: [
          Icon(icon, size: 56, color: Theme.of(context).colorScheme.outline),
          const SizedBox(height: 12),
          Text(text, textAlign: TextAlign.center),
        ]),
      );
}

class _TotalsCard extends StatelessWidget {
  const _TotalsCard({required this.currency, required this.totals});

  final String currency;
  final TxTotals totals;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Wrap(spacing: 24, runSpacing: 8, children: [
          Metric(label: t.incomeType, child: Text(f.money(totals.income, currency), style: TextStyle(color: pnlColor(context, 1)))),
          Metric(label: t.expenseType, child: Text(f.money(totals.expense, currency), style: TextStyle(color: pnlColor(context, -1)))),
          Metric(label: t.net, child: PnlText(value: totals.net, currency: currency, style: Theme.of(context).textTheme.titleMedium)),
        ]),
      ),
    );
  }
}

class MoneyTile extends StatelessWidget {
  const MoneyTile({super.key, required this.tx, required this.onTap});

  final MoneyTransaction tx;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final title = [tx.merchantName, tx.description].firstWhere((s) => s != null && s.trim().isNotEmpty, orElse: () => null) ??
        categoryLabel(t, tx.category);
    final bank = bankOf(tx);
    final subtitle = [
      categoryLabel(t, tx.category),
      f.date(tx.recordedAt),
      if (bank != null) bankLabel(context, bank),
      if (tx.cardRef != null) '••${tx.cardRef}' else if (tx.accountRef != null) '••${tx.accountRef}',
      if (tx.status != null && tx.status != 'completed') statusLabel(t, tx.status!),
    ].join(' · ');
    return ListTile(
      onTap: onTap,
      leading: CircleAvatar(child: Icon(categoryIcon(tx.category))),
      title: Text(title, maxLines: 1, overflow: TextOverflow.ellipsis),
      subtitle: Text.rich(TextSpan(children: [
        if (tx.fromSms) ...[
          WidgetSpan(
            alignment: PlaceholderAlignment.middle,
            child: Icon(Icons.sms_outlined, size: 14, semanticLabel: t.fromSms),
          ),
          const TextSpan(text: ' '),
        ],
        TextSpan(text: subtitle),
      ])),
      // transfers move money between your own accounts: neither gain nor loss
      trailing: tx.isTransfer
          ? Text(f.money(tx.amount, tx.currency))
          : PnlText(value: tx.isIncome ? tx.amount : -tx.amount, currency: tx.currency),
    );
  }
}

// ── filters ──────────────────────────────────────────────────────────────

String _periodLabel(BuildContext context, TxFilter f) {
  final t = AppLocalizations.of(context);
  return switch (f.period) {
    TxPeriod.all => t.periodAll,
    TxPeriod.thisMonth => t.periodThisMonth,
    TxPeriod.lastMonth => t.periodLastMonth,
    TxPeriod.last3Months => t.periodLast3Months,
    TxPeriod.thisYear => t.periodThisYear,
    TxPeriod.custom => f.from == null || f.to == null
        ? t.periodCustom
        : '${Fmt(context).date(f.from)} – ${Fmt(context).date(f.to)}',
  };
}

/// The values the user's transactions actually have, for the filter choices.
class _Options {
  _Options(List<MoneyTransaction> all) {
    for (final tx in all) {
      categories.add(tx.category.toLowerCase());
      final b = bankOf(tx);
      if (b != null) banks.add(b);
      for (final k in productKeys(tx)) {
        products.add(k);
        if (tx.cardNetwork != null) networks.putIfAbsent(k, () => tx.cardNetwork!);
      }
    }
  }

  final categories = <String>{};
  final banks = <String>{};
  final products = <String>{};
  final networks = <String, String>{};
}

/// One deletable chip per active filter, under the search row.
class _ActiveFilters extends StatelessWidget {
  const _ActiveFilters({required this.all, required this.filter, required this.onChanged});

  final List<MoneyTransaction> all;
  final TxFilter filter;
  final ValueChanged<TxFilter> onChanged;

  String _join(Iterable<String> labels, String title) => labels.length <= 2 ? labels.join(', ') : '$title (${labels.length})';

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final options = _Options(all);
    final currency = all.map((tx) => tx.currency).firstOrNull ?? 'SAR';
    String amount() {
      if (filter.minAmount != null && filter.maxAmount != null) {
        return '${f.money(filter.minAmount, currency)} – ${f.money(filter.maxAmount, currency)}';
      }
      return filter.minAmount != null ? '≥ ${f.money(filter.minAmount, currency)}' : '≤ ${f.money(filter.maxAmount, currency)}';
    }

    final chips = [
      if (filter.period != TxPeriod.all)
        (Icons.date_range, _periodLabel(context, filter), () => filter.copyWith(period: TxPeriod.all, from: null, to: null)),
      if (filter.categories.isNotEmpty)
        (Icons.category_outlined, _join(filter.categories.map((c) => categoryLabel(t, c)), t.category),
            () => filter.copyWith(categories: {})),
      if (filter.banks.isNotEmpty)
        (Icons.account_balance_outlined, _join(filter.banks.map((b) => bankLabel(context, b)), t.bank),
            () => filter.copyWith(banks: {})),
      if (filter.products.isNotEmpty)
        (Icons.credit_card, _join(filter.products.map((k) => productLabel(t, k, network: options.networks[k])), t.groupProduct),
            () => filter.copyWith(products: {})),
      if (filter.source != TxSource.all)
        (Icons.sms_outlined, filter.source == TxSource.sms ? t.sourceSms : t.sourceManual,
            () => filter.copyWith(source: TxSource.all)),
      if (filter.minAmount != null || filter.maxAmount != null)
        (Icons.payments_outlined, amount(), () => filter.copyWith(minAmount: null, maxAmount: null)),
    ];
    return SizedBox(
      height: 48,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
        children: [
          for (final (icon, label, clear) in chips)
            Padding(
              padding: const EdgeInsetsDirectional.only(end: 8),
              child: InputChip(
                avatar: Icon(icon, size: 18),
                label: Text(label),
                onDeleted: () => onChanged(clear()),
                onPressed: null,
              ),
            ),
          TextButton(onPressed: () => onChanged(filter.clearSheet()), child: Text(t.clearAll)),
        ],
      ),
    );
  }
}

class _FilterSheet extends StatefulWidget {
  const _FilterSheet({required this.all, required this.initial});

  final List<MoneyTransaction> all;
  final TxFilter initial;

  @override
  State<_FilterSheet> createState() => _FilterSheetState();
}

class _FilterSheetState extends State<_FilterSheet> {
  late TxFilter _f = widget.initial;
  late final _options = _Options(widget.all);
  late final _min = TextEditingController(text: _amountText(widget.initial.minAmount));
  late final _max = TextEditingController(text: _amountText(widget.initial.maxAmount));

  static String _amountText(double? v) => v == null ? '' : (v == v.roundToDouble() ? v.toInt().toString() : '$v');

  static double? _amount(String text) {
    final n = normalizeNumber(text);
    return n == null ? null : double.parse(n);
  }

  @override
  void dispose() {
    _min.dispose();
    _max.dispose();
    super.dispose();
  }

  Set<String> _toggle(Set<String> set, String v) => set.contains(v) ? ({...set}..remove(v)) : {...set, v};

  Future<void> _pickDates() async {
    final now = DateTime.now();
    final r = await showDateRangePicker(
      context: context,
      firstDate: DateTime(2000),
      lastDate: now,
      initialDateRange: _f.from != null && _f.to != null ? DateTimeRange(start: _f.from!, end: _f.to!) : null,
    );
    if (r != null) setState(() => _f = _f.copyWith(period: TxPeriod.custom, from: r.start, to: r.end));
  }

  Widget _section(String title, Widget child) => Padding(
        padding: const EdgeInsets.only(top: 16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(title, style: Theme.of(context).textTheme.titleSmall),
          const SizedBox(height: 8),
          child,
        ]),
      );

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final count = _f.apply(widget.all, DateTime.now()).length;
    final categories = _options.categories.toList()..sort((a, b) => categoryLabel(t, a).compareTo(categoryLabel(t, b)));
    final products = _options.products.toList()..sort();
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.85,
      maxChildSize: 0.95,
      builder: (context, scroll) => Column(children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(children: [
            Expanded(child: Text(t.filters, style: Theme.of(context).textTheme.titleLarge)),
            TextButton(
              onPressed: () => setState(() {
                _f = _f.clearSheet();
                _min.clear();
                _max.clear();
              }),
              child: Text(t.clearAll),
            ),
          ]),
        ),
        Expanded(
          child: ListView(controller: scroll, padding: const EdgeInsets.fromLTRB(16, 0, 16, 16), children: [
            _section(
              t.period,
              Wrap(spacing: 8, runSpacing: 4, children: [
                for (final p in TxPeriod.values.where((p) => p != TxPeriod.custom))
                  ChoiceChip(
                    label: Text(_periodLabel(context, TxFilter(period: p))),
                    selected: _f.period == p,
                    onSelected: (_) => setState(() => _f = _f.copyWith(period: p, from: null, to: null)),
                  ),
                ChoiceChip(
                  avatar: const Icon(Icons.date_range, size: 18),
                  label: Text(_periodLabel(context, _f.period == TxPeriod.custom ? _f : const TxFilter(period: TxPeriod.custom))),
                  selected: _f.period == TxPeriod.custom,
                  onSelected: (_) => _pickDates(),
                ),
              ]),
            ),
            if (categories.isNotEmpty)
              _section(
                t.category,
                Wrap(spacing: 8, runSpacing: 4, children: [
                  for (final c in categories)
                    FilterChip(
                      avatar: Icon(categoryIcon(c), size: 18),
                      label: Text(categoryLabel(t, c)),
                      selected: _f.categories.contains(c),
                      onSelected: (_) => setState(() => _f = _f.copyWith(categories: _toggle(_f.categories, c))),
                    ),
                ]),
              ),
            if (_options.banks.isNotEmpty)
              _section(
                t.bank,
                Wrap(spacing: 8, runSpacing: 4, children: [
                  for (final b in _options.banks.toList()..sort())
                    FilterChip(
                      label: Text(bankLabel(context, b)),
                      selected: _f.banks.contains(b),
                      onSelected: (_) => setState(() => _f = _f.copyWith(banks: _toggle(_f.banks, b))),
                    ),
                ]),
              ),
            if (products.isNotEmpty)
              _section(
                t.groupProduct,
                Wrap(spacing: 8, runSpacing: 4, children: [
                  for (final k in products)
                    FilterChip(
                      avatar: Icon(k.startsWith('card:') ? Icons.credit_card : Icons.account_balance_wallet_outlined, size: 18),
                      label: Text(productLabel(t, k, network: _options.networks[k])),
                      selected: _f.products.contains(k),
                      onSelected: (_) => setState(() => _f = _f.copyWith(products: _toggle(_f.products, k))),
                    ),
                ]),
              ),
            _section(
              t.txSource,
              SegmentedButton<TxSource>(
                segments: [
                  ButtonSegment(value: TxSource.all, label: Text(t.all)),
                  ButtonSegment(value: TxSource.sms, label: Text(t.sourceSms), icon: const Icon(Icons.sms_outlined)),
                  ButtonSegment(value: TxSource.manual, label: Text(t.sourceManual)),
                ],
                selected: {_f.source},
                onSelectionChanged: (s) => setState(() => _f = _f.copyWith(source: s.first)),
              ),
            ),
            _section(
              t.amount,
              Row(children: [
                Expanded(
                  child: TextField(
                    controller: _min,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: InputDecoration(labelText: t.minAmount, border: const OutlineInputBorder(), isDense: true),
                    onChanged: (v) => setState(() => _f = _f.copyWith(minAmount: _amount(v))),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextField(
                    controller: _max,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: InputDecoration(labelText: t.maxAmount, border: const OutlineInputBorder(), isDense: true),
                    onChanged: (v) => setState(() => _f = _f.copyWith(maxAmount: _amount(v))),
                  ),
                ),
              ]),
            ),
          ]),
        ),
        SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
            child: SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => Navigator.pop(context, _f),
                child: Text(t.showTransactions(count)),
              ),
            ),
          ),
        ),
      ]),
    );
  }
}
