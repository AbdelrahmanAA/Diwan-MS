import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'holding_form.dart';
import 'transaction_form.dart';
import 'widgets.dart';

class _HoldingData {
  _HoldingData(this.holding, this.transactions, this.valuations, this.history);

  final Holding holding;
  final List<TransactionItem> transactions;
  final List<ValuationItem> valuations;
  final List<PricePoint> history;
}

/// One investment: value and P/L, price chart, transactions and valuations.
class HoldingScreen extends StatefulWidget {
  const HoldingScreen({super.key, required this.holdingId, required this.currency});

  final int holdingId;
  final String currency; // portfolio base currency

  @override
  State<HoldingScreen> createState() => _HoldingScreenState();
}

class _HoldingScreenState extends State<HoldingScreen> {
  Key _key = UniqueKey();

  void _refresh() => setState(() => _key = UniqueKey());

  Future<_HoldingData> _load() async {
    final api = context.read<Session>().api;
    final h = await api.holding(widget.holdingId);
    final results = await Future.wait([
      api.transactions(h.id),
      h.isCash ? Future.value(<ValuationItem>[]) : api.valuations(h.id),
      h.symbol == null ? Future.value(<PricePoint>[]) : api.history(h.symbol!, 365),
    ]);
    return _HoldingData(h, results[0] as List<TransactionItem>, results[1] as List<ValuationItem>,
        results[2] as List<PricePoint>);
  }

  Future<void> _action(Future<void> Function() call) async {
    try {
      await call();
      _refresh();
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Loader<_HoldingData>(
      key: _key,
      load: _load,
      builder: (context, d, reload) {
        final h = d.holding;
        final f = Fmt(context);
        final ccy = widget.currency;
        final source = switch (h.priceSource) {
          'QUOTE' => t.sourceQuote,
          'VALUATION' => t.sourceValuation,
          'CASH' => t.sourceCash,
          _ => t.sourceCost,
        };
        return Scaffold(
          appBar: AppBar(
            title: Text(h.name),
            actions: [
              PopupMenuButton<String>(
                onSelected: (v) async {
                  if (v == 'edit') {
                    final r = await Navigator.push<Holding>(context,
                        MaterialPageRoute(builder: (_) => HoldingForm(portfolioId: h.portfolioId, holding: h)));
                    if (r != null) _refresh();
                  } else if (v == 'close') {
                    _action(() => api.setClosed(h.id, !h.closed));
                  } else if (v == 'delete' && await confirm(context, t.confirmDelete)) {
                    try {
                      await api.deleteHolding(h.id);
                      if (context.mounted) Navigator.pop(context);
                    } catch (e) {
                      if (context.mounted) showError(context, e);
                    }
                  }
                },
                itemBuilder: (c) => [
                  PopupMenuItem(value: 'edit', child: Text(t.editInvestment)),
                  PopupMenuItem(value: 'close', child: Text(h.closed ? t.reopenHolding : t.closeHolding)),
                  PopupMenuItem(value: 'delete', child: Text(t.deleteHolding)),
                ],
              ),
            ],
          ),
          floatingActionButton: FloatingActionButton.extended(
            icon: const Icon(Icons.add),
            label: Text(t.addTransaction),
            onPressed: () async {
              final ok = await showModalBottomSheet<bool>(
                context: context,
                isScrollControlled: true,
                builder: (_) => TransactionForm(holding: h),
              );
              if (ok == true) _refresh();
            },
          ),
          body: ListView(padding: const EdgeInsets.only(bottom: 96), children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('${h.assetTypeLabel} · ${h.countryLabel}${h.symbol == null ? '' : ' · ${h.symbol}'}',
                    style: Theme.of(context).textTheme.bodyMedium),
                const SizedBox(height: 8),
                Text(f.money(h.marketValue, ccy), style: Theme.of(context).textTheme.headlineMedium),
                if (h.currency != ccy) Text(f.money(h.marketValueLocal, h.currency)),
                const SizedBox(height: 12),
                Wrap(spacing: 24, runSpacing: 12, children: [
                  if (!h.isCash) Metric(label: t.quantity, child: Text(f.number(h.quantity))),
                  if (!h.isCash) Metric(label: t.averageCost, child: Text(f.money(h.averageCostLocal, h.currency))),
                  Metric(label: t.priceFrom(source), child: Text(f.money(h.priceLocal, h.currency))),
                  Metric(label: t.today, child: PnlText(value: h.dayChange, percent: h.dayChangePercent, currency: ccy)),
                  Metric(label: t.unrealizedPnl, child: PnlText(value: h.unrealizedPnl, percent: h.unrealizedPnlPercent, currency: ccy)),
                  Metric(label: t.realizedPnl, child: PnlText(value: h.realizedPnl, currency: ccy)),
                  Metric(label: t.income, child: PnlText(value: h.netIncome, currency: ccy)),
                  if (h.weightPercent != null) Metric(label: t.weight, child: Text(f.percent(h.weightPercent, signed: false))),
                ]),
                if (h.attributes['address'] != null) ...[
                  const SizedBox(height: 12),
                  Metric(label: t.address, child: Text(h.attributes['address'].toString())),
                ],
                if (h.notes != null) ...[
                  const SizedBox(height: 12),
                  Metric(label: t.notes, child: Text(h.notes!)),
                ],
              ]),
            ),
            if (h.symbol != null) ...[
              _SectionTitle(t.history),
              SizedBox(
                height: 180,
                child: d.history.length < 2
                    ? Center(child: Text(t.noHistory))
                    : Padding(padding: const EdgeInsets.fromLTRB(16, 8, 24, 8), child: _PriceChart(points: d.history)),
              ),
            ],
            _SectionTitle(t.transactions),
            for (final tx in d.transactions)
              ListTile(
                title: Text(tx.typeLabel),
                subtitle: Text([
                  f.date(tx.tradeDate),
                  if (tx.quantity != null) '${f.number(tx.quantity)} × ${f.money(tx.price, h.currency)}',
                  if (tx.amount != null) f.money(tx.amount, h.currency),
                  if (tx.note != null) tx.note!,
                ].join(' · ')),
                trailing: IconButton(
                  icon: const Icon(Icons.delete_outline),
                  onPressed: () async {
                    if (await confirm(context, t.confirmDelete)) _action(() => api.deleteTransaction(tx.id));
                  },
                ),
              ),
            if (!h.isCash) ...[
              _SectionTitle(t.valuations, action: IconButton(
                icon: const Icon(Icons.add),
                tooltip: t.addValuation,
                onPressed: () async {
                  final ok = await showModalBottomSheet<bool>(
                      context: context, isScrollControlled: true, builder: (_) => ValuationForm(holding: h));
                  if (ok == true) _refresh();
                },
              )),
              for (final v in d.valuations)
                ListTile(
                  title: Text(f.money(v.value, h.currency)),
                  subtitle: Text([f.date(v.valuationDate), if (v.note != null) v.note!].join(' · ')),
                ),
            ],
          ]),
        );
      },
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.text, {this.action});

  final String text;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 8, 4),
      child: Row(children: [
        Expanded(child: Text(text, style: Theme.of(context).textTheme.titleMedium)),
        ?action,
      ]),
    );
  }
}

class _PriceChart extends StatelessWidget {
  const _PriceChart({required this.points});

  final List<PricePoint> points;

  @override
  Widget build(BuildContext context) {
    final color = Theme.of(context).colorScheme.primary;
    return LineChart(LineChartData(
      gridData: const FlGridData(show: false),
      borderData: FlBorderData(show: false),
      titlesData: const FlTitlesData(
        topTitles: AxisTitles(), rightTitles: AxisTitles(), bottomTitles: AxisTitles(),
        leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 44)),
      ),
      lineBarsData: [
        LineChartBarData(
          spots: [for (var i = 0; i < points.length; i++) FlSpot(i.toDouble(), points[i].close)],
          isCurved: true,
          color: color,
          dotData: const FlDotData(show: false),
          belowBarData: BarAreaData(show: true, color: color.withValues(alpha: 0.12)),
        ),
      ],
    ));
  }
}
