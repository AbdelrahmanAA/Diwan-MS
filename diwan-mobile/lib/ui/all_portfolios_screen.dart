import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'holding_screen.dart';
import 'portfolio_screen.dart';
import 'widgets.dart';

/// Every portfolio added together: totals, charts and groups in the user's preferred currency.
class AllPortfoliosScreen extends StatefulWidget {
  const AllPortfoliosScreen({super.key});

  @override
  State<AllPortfoliosScreen> createState() => _AllPortfoliosScreenState();
}

class _AllPortfoliosScreenState extends State<AllPortfoliosScreen> {
  List<String> _groupBy = const ['COUNTRY', 'ASSET_TYPE'];
  Key _key = UniqueKey();

  void _refresh() => setState(() => _key = UniqueKey());

  Future<void> _pickCurrency() async {
    final t = AppLocalizations.of(context);
    final session = context.read<Session>();
    try {
      final ref = await session.reference();
      if (!mounted) return;
      final picked = await pickFromList(context, t.baseCurrency, ref.currencies.map((c) => MapEntry(c.code, c.label)).toList());
      if (picked == null) return;
      await session.api.savePreferences(baseCurrency: picked);
      _refresh();
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  /// A holding is valued in the base currency of its own portfolio, not the combined one.
  Future<void> _openHolding(Summary summary, Holding h) async {
    final share = summary.portfolios.where((s) => s.portfolio.id == h.portfolioId).firstOrNull;
    await Navigator.push(context, MaterialPageRoute(
        builder: (_) => HoldingScreen(holdingId: h.id, currency: share?.portfolio.baseCurrency ?? h.currency)));
    _refresh();
  }

  Future<void> _openPortfolio(Portfolio p) async {
    await Navigator.push(context, MaterialPageRoute(builder: (_) => PortfolioScreen(portfolio: p)));
    _refresh();
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Scaffold(
      appBar: AppBar(
        title: Text(t.allPortfolios),
        actions: [IconButton(icon: const Icon(Icons.currency_exchange), tooltip: t.baseCurrency, onPressed: _pickCurrency)],
      ),
      body: KeyedSubtree(
        key: _key,
        child: Loader<Summary>(
          load: () => api.combinedSummary(groupBy: _groupBy.first == byPortfolioKey ? null : _groupBy),
          builder: (c, s, reload) => SummaryOverview(
            summary: s,
            groupBy: _groupBy,
            onGroupBy: (g) => setState(() { _groupBy = g; _key = UniqueKey(); }),
            onHolding: (h) => _openHolding(s, h),
            onPortfolio: _openPortfolio,
          ),
        ),
      ),
    );
  }
}
