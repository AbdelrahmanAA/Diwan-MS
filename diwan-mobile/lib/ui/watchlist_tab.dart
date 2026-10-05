import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

class WatchlistTab extends StatefulWidget {
  const WatchlistTab({super.key});

  @override
  State<WatchlistTab> createState() => _WatchlistTabState();
}

class _WatchlistTabState extends State<WatchlistTab> {
  Key _key = UniqueKey();

  Future<void> _add() async {
    final t = AppLocalizations.of(context);
    final controller = TextEditingController();
    final symbol = await showDialog<String>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text(t.addSymbol),
        content: TextField(
          controller: controller,
          autofocus: true,
          textDirection: TextDirection.ltr,
          textCapitalization: TextCapitalization.characters,
          decoration: InputDecoration(labelText: t.symbol, helperText: t.symbolHint),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c), child: Text(t.cancel)),
          FilledButton(onPressed: () => Navigator.pop(c, controller.text.trim()), child: Text(t.save)),
        ],
      ),
    );
    if (symbol == null || symbol.isEmpty || !mounted) return;
    try {
      await context.read<Session>().api.watch(symbol);
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
      appBar: AppBar(title: Text(t.watchlist)),
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
                  title: Text(w.instrument.symbol, textDirection: TextDirection.ltr),
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
          ]);
        },
      ),
    );
  }
}
