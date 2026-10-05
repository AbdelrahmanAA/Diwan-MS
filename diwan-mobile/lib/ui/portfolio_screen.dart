import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'goals_view.dart';
import 'holding_form.dart';
import 'holding_screen.dart';
import 'portfolio_form.dart';
import 'targets_view.dart';
import 'widgets.dart';

/// One portfolio: grouped overview (country -> type by default), all investments, allocation vs target, goals.
class PortfolioScreen extends StatefulWidget {
  const PortfolioScreen({super.key, required this.portfolio});

  final Portfolio portfolio;

  @override
  State<PortfolioScreen> createState() => _PortfolioScreenState();
}

class _PortfolioScreenState extends State<PortfolioScreen> {
  late Portfolio _portfolio = widget.portfolio;
  List<String> _groupBy = const ['COUNTRY', 'ASSET_TYPE'];
  Key _key = UniqueKey();

  void _refresh() => setState(() => _key = UniqueKey());

  Future<void> _openHolding(Holding h) async {
    await Navigator.push(context, MaterialPageRoute(builder: (_) => HoldingScreen(holdingId: h.id, currency: _portfolio.baseCurrency)));
    _refresh();
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return DefaultTabController(
      length: 4,
      child: Scaffold(
        appBar: AppBar(
          title: Text(_portfolio.name),
          actions: [
            PopupMenuButton<String>(
              onSelected: (v) async {
                if (v == 'edit') {
                  final p = await Navigator.push<Portfolio>(
                      context, MaterialPageRoute(builder: (_) => PortfolioForm(portfolio: _portfolio)));
                  if (p != null) setState(() { _portfolio = p; _key = UniqueKey(); });
                } else if (v == 'delete') {
                  if (await confirm(context, t.deletePortfolioConfirm(_portfolio.name))) {
                    try {
                      await api.deletePortfolio(_portfolio.id);
                      if (context.mounted) Navigator.pop(context);
                    } catch (e) {
                      if (context.mounted) showError(context, e);
                    }
                  }
                }
              },
              itemBuilder: (c) => [
                PopupMenuItem(value: 'edit', child: Text(t.editPortfolio)),
                PopupMenuItem(value: 'delete', child: Text(t.deletePortfolio)),
              ],
            ),
          ],
          bottom: TabBar(isScrollable: true, tabs: [
            Tab(text: t.overview),
            Tab(text: t.holdings),
            Tab(text: t.allocation),
            Tab(text: t.goals),
          ]),
        ),
        floatingActionButton: FloatingActionButton.extended(
          icon: const Icon(Icons.add),
          label: Text(t.addInvestment),
          onPressed: () async {
            final added = await Navigator.push<Holding>(
                context, MaterialPageRoute(builder: (_) => HoldingForm(portfolioId: _portfolio.id)));
            if (added != null) _refresh();
          },
        ),
        body: KeyedSubtree(
          key: _key,
          child: TabBarView(children: [
            Loader<Summary>(
              load: () => api.summary(_portfolio.id, groupBy: _groupBy),
              builder: (c, s, reload) => _Overview(
                summary: s,
                groupBy: _groupBy,
                onGroupBy: (g) => setState(() { _groupBy = g; _key = UniqueKey(); }),
                onHolding: _openHolding,
              ),
            ),
            Loader<Summary>(
              load: () => api.summary(_portfolio.id, groupBy: const ['ASSET_TYPE']),
              builder: (c, s, reload) {
                final all = [for (final g in s.groups) ...g.holdings];
                if (all.isEmpty) return EmptyState(icon: Icons.inventory_2_outlined, text: t.noHoldings);
                all.sort((a, b) => (b.marketValue ?? 0).compareTo(a.marketValue ?? 0));
                return ListView(padding: const EdgeInsets.only(bottom: 96), children: [
                  for (final h in all) HoldingTile(holding: h, currency: s.portfolio.baseCurrency, onTap: () => _openHolding(h)),
                ]);
              },
            ),
            TargetsView(portfolio: _portfolio),
            GoalsView(portfolio: _portfolio),
          ]),
        ),
      ),
    );
  }
}

class _Overview extends StatelessWidget {
  const _Overview({required this.summary, required this.groupBy, required this.onGroupBy, required this.onHolding});

  final Summary summary;
  final List<String> groupBy;
  final ValueChanged<List<String>> onGroupBy;
  final ValueChanged<Holding> onHolding;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final ccy = summary.portfolio.baseCurrency;
    final totals = summary.totals;
    return ListView(padding: const EdgeInsets.only(bottom: 96), children: [
      Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(t.totalValue, style: Theme.of(context).textTheme.labelLarge),
          Text(f.money(totals.marketValue, ccy), style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 8),
          Wrap(spacing: 24, runSpacing: 12, children: [
            Metric(label: t.today, child: PnlText(value: totals.dayChange, percent: totals.dayChangePercent, currency: ccy)),
            Metric(label: t.unrealizedPnl, child: PnlText(value: totals.unrealizedPnl, percent: totals.unrealizedPnlPercent, currency: ccy)),
            Metric(label: t.realizedPnl, child: PnlText(value: totals.realizedPnl, currency: ccy)),
            Metric(label: t.income, child: PnlText(value: totals.netIncome, currency: ccy)),
            Metric(label: t.totalReturn, child: PnlText(value: totals.totalReturn, percent: totals.totalReturnPercent, currency: ccy)),
          ]),
          if (summary.missingRates.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(t.missingRates(summary.missingRates.join(', ')),
                  style: TextStyle(color: Theme.of(context).colorScheme.error)),
            ),
        ]),
      ),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: SegmentedButton<String>(
          segments: [
            ButtonSegment(value: 'COUNTRY', label: Text(t.byCountry)),
            ButtonSegment(value: 'ASSET_TYPE', label: Text(t.byType)),
            ButtonSegment(value: 'CURRENCY', label: Text(t.byCurrency)),
          ],
          selected: {groupBy.first},
          onSelectionChanged: (s) {
            final first = s.first;
            onGroupBy(first == 'COUNTRY' ? ['COUNTRY', 'ASSET_TYPE'] : first == 'ASSET_TYPE' ? ['ASSET_TYPE', 'COUNTRY'] : ['CURRENCY', 'ASSET_TYPE']);
          },
        ),
      ),
      if (summary.groups.isEmpty)
        Padding(padding: const EdgeInsets.all(32), child: Text(t.noHoldings, textAlign: TextAlign.center))
      else ...[
        SizedBox(height: 200, child: _AllocationPie(groups: summary.groups)),
        for (final g in summary.groups) _GroupCard(group: g, currency: ccy, onHolding: onHolding),
      ],
    ]);
  }
}

const _palette = [
  Color(0xFF7C3AED), Color(0xFF2563EB), Color(0xFF0EA5E9), Color(0xFF16A34A),
  Color(0xFFCA8A04), Color(0xFFEA580C), Color(0xFFDC2626), Color(0xFFDB2777), Color(0xFF64748B),
];

class _AllocationPie extends StatelessWidget {
  const _AllocationPie({required this.groups});

  final List<GroupNode> groups;

  @override
  Widget build(BuildContext context) {
    final shown = groups.where((g) => (g.weightPercent ?? 0) > 0).toList();
    return PieChart(PieChartData(
      sectionsSpace: 2,
      centerSpaceRadius: 48,
      sections: [
        for (var i = 0; i < shown.length; i++)
          PieChartSectionData(
            value: shown[i].weightPercent,
            color: _palette[i % _palette.length],
            radius: 44,
            title: (shown[i].weightPercent ?? 0) >= 6 ? shown[i].key : '',
            titleStyle: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12),
          ),
      ],
    ));
  }
}

class _GroupCard extends StatelessWidget {
  const _GroupCard({required this.group, required this.currency, required this.onHolding});

  final GroupNode group;
  final String currency;
  final ValueChanged<Holding> onHolding;

  @override
  Widget build(BuildContext context) {
    final f = Fmt(context);
    return Card(
      margin: const EdgeInsets.fromLTRB(12, 8, 12, 0),
      clipBehavior: Clip.antiAlias,
      child: ExpansionTile(
        initiallyExpanded: true,
        title: Text(group.label, style: Theme.of(context).textTheme.titleMedium),
        subtitle: Text('${f.money(group.totals.marketValue, currency)} · ${f.percent(group.weightPercent, signed: false)}'),
        trailing: PnlText(value: group.totals.totalReturn, currency: currency),
        children: [
          for (final sub in group.groups)
            ExpansionTile(
              tilePadding: const EdgeInsetsDirectional.only(start: 32, end: 16),
              title: Text(sub.label),
              subtitle: Text('${f.money(sub.totals.marketValue, currency)} · ${f.percent(sub.weightPercent, signed: false)}'),
              children: [
                for (final h in sub.holdings) HoldingTile(holding: h, currency: currency, onTap: () => onHolding(h), indent: true),
              ],
            ),
          for (final h in group.holdings) HoldingTile(holding: h, currency: currency, onTap: () => onHolding(h)),
        ],
      ),
    );
  }
}

class HoldingTile extends StatelessWidget {
  const HoldingTile({super.key, required this.holding, required this.currency, required this.onTap, this.indent = false});

  final Holding holding;
  final String currency;
  final VoidCallback onTap;
  final bool indent;

  @override
  Widget build(BuildContext context) {
    final f = Fmt(context);
    final h = holding;
    final subtitle = [
      if (h.symbol != null) h.symbol!,
      if (!h.isCash) '${f.number(h.quantity)} × ${f.money(h.priceLocal, h.currency)}',
      if (h.isCash) f.money(h.marketValueLocal, h.currency),
    ].join(' · ');
    return ListTile(
      contentPadding: EdgeInsetsDirectional.only(start: indent ? 48 : 16, end: 16),
      title: Text(h.name, maxLines: 1, overflow: TextOverflow.ellipsis),
      subtitle: Text(subtitle, maxLines: 1, overflow: TextOverflow.ellipsis),
      trailing: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.end, children: [
        Text(f.money(h.marketValue, currency), style: const TextStyle(fontWeight: FontWeight.w600)),
        PnlText(value: h.unrealizedPnl, percent: h.unrealizedPnlPercent, currency: currency, style: const TextStyle(fontSize: 12)),
      ]),
      onTap: onTap,
    );
  }
}
