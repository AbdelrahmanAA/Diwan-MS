import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'portfolio_form.dart';
import 'portfolio_screen.dart';
import 'widgets.dart';

class PortfoliosTab extends StatefulWidget {
  const PortfoliosTab({super.key});

  @override
  State<PortfoliosTab> createState() => _PortfoliosTabState();
}

class _PortfoliosTabState extends State<PortfoliosTab> {
  Key _key = UniqueKey();

  void _refresh() => setState(() => _key = UniqueKey());

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Scaffold(
      appBar: AppBar(title: Text(t.portfolios)),
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add),
        label: Text(t.newPortfolio),
        onPressed: () async {
          final created = await Navigator.push<Portfolio>(
              context, MaterialPageRoute(builder: (_) => const PortfolioForm()));
          if (created != null) _refresh();
        },
      ),
      body: Loader<List<PortfolioOverview>>(
        key: _key,
        load: api.portfolios,
        builder: (context, list, reload) {
          if (list.isEmpty) return EmptyState(icon: Icons.pie_chart_outline, text: t.noPortfolios);
          return ListView.builder(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 96),
            itemCount: list.length,
            itemBuilder: (c, i) => _PortfolioCard(
              overview: list[i],
              onTap: () async {
                await Navigator.push(context,
                    MaterialPageRoute(builder: (_) => PortfolioScreen(portfolio: list[i].portfolio)));
                reload();
              },
            ),
          );
        },
      ),
    );
  }
}

class _PortfolioCard extends StatelessWidget {
  const _PortfolioCard({required this.overview, required this.onTap});

  final PortfolioOverview overview;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final p = overview.portfolio;
    final totals = overview.totals;
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              CircleAvatar(
                backgroundColor: parseColor(p.color) ?? Theme.of(context).colorScheme.primaryContainer,
                child: const Icon(Icons.account_balance_wallet_outlined, color: Colors.white),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(p.name, style: Theme.of(context).textTheme.titleMedium),
                  Text(t.holdingsCount(overview.holdingsCount), style: Theme.of(context).textTheme.bodySmall),
                ]),
              ),
              const Icon(Icons.chevron_right),
            ]),
            const SizedBox(height: 12),
            Text(f.money(totals.marketValue, p.baseCurrency), style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 8),
            Wrap(spacing: 24, runSpacing: 8, children: [
              Metric(label: t.today, child: PnlText(value: totals.dayChange, percent: totals.dayChangePercent, currency: p.baseCurrency)),
              Metric(label: t.totalReturn, child: PnlText(value: totals.totalReturn, percent: totals.totalReturnPercent, currency: p.baseCurrency)),
            ]),
            if (overview.missingRates.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(t.missingRates(overview.missingRates.join(', ')),
                  style: TextStyle(color: Theme.of(context).colorScheme.error, fontSize: 12)),
            ],
          ]),
        ),
      ),
    );
  }
}

Color? parseColor(String? hex) {
  if (hex == null || !RegExp(r'^#[0-9a-fA-F]{6}$').hasMatch(hex)) return null;
  return Color(int.parse('FF${hex.substring(1)}', radix: 16));
}
