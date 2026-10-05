import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../l10n/app_localizations.dart';
import 'format.dart';

/// The message to show for an error: the server's (already localized) text, or a local one for network errors.
String errorText(BuildContext context, Object error) {
  final t = AppLocalizations.of(context);
  if (error is ApiException) {
    if (error.isNetwork) return t.networkError;
    if (error.isUnauthorized) return t.sessionExpired;
    return error.message;
  }
  return error.toString();
}

void showError(BuildContext context, Object error) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorText(context, error))));
}

/// Loads [future] and shows a spinner, an error with retry, or [builder]'s content. Pull to refresh included.
class Loader<T> extends StatefulWidget {
  const Loader({super.key, required this.load, required this.builder});

  final Future<T> Function() load;
  final Widget Function(BuildContext context, T data, Future<void> Function() reload) builder;

  @override
  State<Loader<T>> createState() => _LoaderState<T>();
}

class _LoaderState<T> extends State<Loader<T>> {
  late Future<T> _future;

  @override
  void initState() {
    super.initState();
    _future = widget.load();
  }

  Future<void> _reload() async {
    final f = widget.load();
    setState(() => _future = f);
    try {
      await f;
    } catch (_) {
      // shown by the FutureBuilder
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<T>(
      future: _future,
      builder: (context, snap) {
        if (snap.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snap.hasError) {
          return ErrorView(message: errorText(context, snap.error!), onRetry: _reload);
        }
        return RefreshIndicator(onRefresh: _reload, child: widget.builder(context, snap.data as T, _reload));
      },
    );
  }
}

class ErrorView extends StatelessWidget {
  const ErrorView({super.key, required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Icon(Icons.cloud_off, size: 48, color: Theme.of(context).colorScheme.error),
          const SizedBox(height: 12),
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 12),
          FilledButton.tonal(onPressed: onRetry, child: Text(AppLocalizations.of(context).retry)),
        ]),
      ),
    );
  }
}

/// A scrollable empty state (so pull-to-refresh still works).
class EmptyState extends StatelessWidget {
  const EmptyState({super.key, required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return ListView(physics: const AlwaysScrollableScrollPhysics(), children: [
      const SizedBox(height: 120),
      Icon(icon, size: 56, color: Theme.of(context).colorScheme.outline),
      const SizedBox(height: 12),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32),
        child: Text(text, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyLarge),
      ),
    ]);
  }
}

/// "+1,234.00 $ (+5.2%)" in green/red.
class PnlText extends StatelessWidget {
  const PnlText({super.key, required this.value, required this.currency, this.percent, this.style});

  final double? value;
  final double? percent;
  final String currency;
  final TextStyle? style;

  @override
  Widget build(BuildContext context) {
    final f = Fmt(context);
    final text = percent == null
        ? f.money(value, currency, signed: true)
        : '${f.money(value, currency, signed: true)} (${f.percent(percent)})';
    return Text(text, style: (style ?? const TextStyle()).copyWith(color: pnlColor(context, value)));
  }
}

/// Label above value, used in summary cards.
class Metric extends StatelessWidget {
  const Metric({super.key, required this.label, required this.child});

  final String label;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(label, style: Theme.of(context).textTheme.labelMedium?.copyWith(
          color: Theme.of(context).colorScheme.onSurfaceVariant)),
      const SizedBox(height: 2),
      child,
    ]);
  }
}

Future<bool> confirm(BuildContext context, String message) async {
  final t = AppLocalizations.of(context);
  return await showDialog<bool>(
        context: context,
        builder: (c) => AlertDialog(
          content: Text(message),
          actions: [
            TextButton(onPressed: () => Navigator.pop(c, false), child: Text(t.cancel)),
            FilledButton(onPressed: () => Navigator.pop(c, true), child: Text(t.delete)),
          ],
        ),
      ) ??
      false;
}

/// Picks one item of a long list (countries, currencies) with a search field.
Future<String?> pickFromList(BuildContext context, String title, List<MapEntry<String, String>> items) {
  return showModalBottomSheet<String>(
    context: context,
    isScrollControlled: true,
    builder: (c) => _PickerSheet(title: title, items: items),
  );
}

class _PickerSheet extends StatefulWidget {
  const _PickerSheet({required this.title, required this.items});

  final String title;
  final List<MapEntry<String, String>> items;

  @override
  State<_PickerSheet> createState() => _PickerSheetState();
}

class _PickerSheetState extends State<_PickerSheet> {
  String _q = '';

  @override
  Widget build(BuildContext context) {
    final q = _q.toLowerCase();
    final shown = widget.items
        .where((e) => q.isEmpty || e.value.toLowerCase().contains(q) || e.key.toLowerCase().contains(q))
        .toList();
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.8,
      builder: (context, scroll) => Column(children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: TextField(
            autofocus: true,
            decoration: InputDecoration(prefixIcon: const Icon(Icons.search), labelText: widget.title),
            onChanged: (v) => setState(() => _q = v),
          ),
        ),
        Expanded(
          child: ListView.builder(
            controller: scroll,
            itemCount: shown.length,
            itemBuilder: (c, i) => ListTile(
              title: Text(shown[i].value),
              trailing: Text(shown[i].key),
              onTap: () => Navigator.pop(context, shown[i].key),
            ),
          ),
        ),
      ]),
    );
  }
}
