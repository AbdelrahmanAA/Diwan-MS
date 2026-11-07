import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'holding_form.dart';
import 'widgets.dart';

class WatchlistTab extends StatelessWidget {
  const WatchlistTab({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: Text(t.watchlist),
          bottom: TabBar(tabs: [Tab(text: t.stocks), Tab(text: t.gold)]),
        ),
        body: const TabBarView(children: [_StocksView(), _GoldView()]),
      ),
    );
  }
}

class _StocksView extends StatefulWidget {
  const _StocksView();

  @override
  State<_StocksView> createState() => _StocksViewState();
}

class _StocksViewState extends State<_StocksView> {
  Key _key = UniqueKey();

  Future<void> _add() async {
    final match = await showModalBottomSheet<SymbolMatch>(
        context: context, isScrollControlled: true, builder: (_) => const SymbolSearch(hideGold: true));
    if (match == null || !mounted) return;
    try {
      await context.read<Session>().api.watch(match.symbol);
      setState(() => _key = UniqueKey());
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Scaffold(
      floatingActionButton: FloatingActionButton(onPressed: _add, tooltip: t.addSymbol, child: const Icon(Icons.add)),
      body: Loader<List<WatchItem>>(
        key: _key,
        load: api.watchlist,
        builder: (context, items, reload) {
          if (items.isEmpty) return EmptyState(icon: Icons.visibility_outlined, text: t.noWatchlist);
          final f = Fmt(context);
          return ListView(children: [
            for (final w in items)
              Dismissible(
                key: ValueKey(w.id),
                direction: DismissDirection.endToStart,
                background: Container(color: Theme.of(context).colorScheme.errorContainer),
                onDismissed: (_) => api.unwatch(w.instrument.symbol).catchError((Object e) {
                  if (context.mounted) showError(context, e);
                }),
                child: ListTile(
                  title: Text(displaySymbol(w.instrument.symbol), textDirection: TextDirection.ltr),
                  subtitle: Text(w.instrument.name ?? ''),
                  trailing: w.instrument.quote == null
                      ? null
                      : Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.end, children: [
                          Text(f.money(w.instrument.quote!.price, w.instrument.currency ?? 'USD'),
                              style: const TextStyle(fontWeight: FontWeight.w600)),
                          Text(f.percent(w.instrument.quote!.changePercent),
                              style: TextStyle(color: pnlColor(context, w.instrument.quote!.changePercent), fontSize: 12)),
                        ]),
                ),
              ),
            const DataCredits(),
          ]);
        },
      ),
    );
  }
}

/// Gold prices for everyone, held or not: the preferred currency until another one is picked.
class _GoldView extends StatefulWidget {
  const _GoldView();

  @override
  State<_GoldView> createState() => _GoldViewState();
}

class _GoldViewState extends State<_GoldView> {
  String? _currency; // null = the preferred currency

  Future<void> _pickCurrency() async {
    final t = AppLocalizations.of(context);
    try {
      final ref = await context.read<Session>().reference();
      if (!mounted) return;
      final picked = await pickFromList(context, t.currency, ref.currencies.map((c) => MapEntry(c.code, c.label)).toList());
      if (picked != null) setState(() => _currency = picked);
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Loader<GoldPrices>(
      key: ValueKey(_currency),
      load: () => api.goldPrices(currency: _currency),
      builder: (context, gold, reload) {
        final f = Fmt(context);
        return RefreshIndicator(
          onRefresh: reload,
          child: ListView(children: [
            ListTile(
              title: Text(t.currency),
              subtitle: Text(gold.currency),
              trailing: const Icon(Icons.arrow_drop_down),
              onTap: _pickCurrency,
            ),
            const Divider(height: 1),
            for (final p in gold.prices)
              ListTile(
                title: Text(p.isOunce ? t.goldOunce : t.goldGram(p.karat)),
                trailing: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.end, children: [
                  Text(f.money(p.price, gold.currency), style: const TextStyle(fontWeight: FontWeight.w600)),
                  Text(f.percent(p.changePercent), style: TextStyle(color: pnlColor(context, p.changePercent), fontSize: 12)),
                ]),
              ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text([t.goldPriceNote, if (gold.time != null) f.dateTime(gold.time!)].join('\n'),
                  style: Theme.of(context).textTheme.bodySmall),
            ),
          ]),
        );
      },
    );
  }
}
