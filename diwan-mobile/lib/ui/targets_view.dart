import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

/// Current allocation against the customer's targets, with what to add or reduce to get back on target.
class TargetsView extends StatefulWidget {
  const TargetsView({super.key, required this.portfolio});

  final Portfolio portfolio;

  @override
  State<TargetsView> createState() => _TargetsViewState();
}

class _TargetsViewState extends State<TargetsView> {
  String _dimension = 'ASSET_TYPE';
  Key _key = UniqueKey();

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Column(children: [
      Padding(
        padding: const EdgeInsets.all(12),
        child: SegmentedButton<String>(
          segments: [
            ButtonSegment(value: 'ASSET_TYPE', label: Text(t.byType)),
            ButtonSegment(value: 'COUNTRY', label: Text(t.byCountry)),
          ],
          selected: {_dimension},
          onSelectionChanged: (s) => setState(() { _dimension = s.first; _key = UniqueKey(); }),
        ),
      ),
      Expanded(
        child: Loader<Allocation>(
          key: _key,
          load: () => api.allocation(widget.portfolio.id, _dimension),
          builder: (context, a, reload) {
            final f = Fmt(context);
            return ListView(padding: const EdgeInsets.only(bottom: 96), children: [
              if (!a.hasTargets) Padding(padding: const EdgeInsets.all(16), child: Text(t.noTargets)),
              for (final r in a.rows)
                ListTile(
                  title: Text(r.label),
                  subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    const SizedBox(height: 4),
                    LinearProgressIndicator(value: (r.currentPercent / 100).clamp(0, 1)),
                    const SizedBox(height: 4),
                    Text([
                      '${t.current}: ${f.percent(r.currentPercent, signed: false)}',
                      if (r.targetPercent != null) '${t.target}: ${f.percent(r.targetPercent, signed: false)}',
                      if (r.driftPercent != null) '${t.drift}: ${f.percent(r.driftPercent)}',
                    ].join('  ·  ')),
                  ]),
                  trailing: r.suggestedChange == null || r.suggestedChange!.abs() < 0.01
                      ? Text(f.money(r.value, a.baseCurrency))
                      : Text(
                          r.suggestedChange! > 0
                              ? t.buyMore(f.money(r.suggestedChange, a.baseCurrency))
                              : t.reduce(f.money(-r.suggestedChange!, a.baseCurrency)),
                          style: TextStyle(color: pnlColor(context, r.suggestedChange)),
                        ),
                ),
              Padding(
                padding: const EdgeInsets.all(16),
                child: OutlinedButton.icon(
                  icon: const Icon(Icons.tune),
                  label: Text(t.editTargets),
                  onPressed: () async {
                    final saved = await showModalBottomSheet<bool>(
                      context: context,
                      isScrollControlled: true,
                      builder: (_) => _TargetsEditor(portfolio: widget.portfolio, dimension: _dimension, rows: a.rows),
                    );
                    if (saved == true) setState(() => _key = UniqueKey());
                  },
                ),
              ),
            ]);
          },
        ),
      ),
    ]);
  }
}

class _TargetsEditor extends StatefulWidget {
  const _TargetsEditor({required this.portfolio, required this.dimension, required this.rows});

  final Portfolio portfolio;
  final String dimension;
  final List<AllocationRow> rows;

  @override
  State<_TargetsEditor> createState() => _TargetsEditorState();
}

class _TargetsEditorState extends State<_TargetsEditor> {
  final Map<String, TextEditingController> _fields = {};
  final Map<String, String> _labels = {};
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    for (final r in widget.rows) {
      _labels[r.key] = r.label;
      _fields[r.key] = TextEditingController(text: r.targetPercent == null ? '' : _plain(r.targetPercent!));
    }
  }

  static String _plain(double v) => v == v.roundToDouble() ? v.toStringAsFixed(0) : v.toString();

  double get _sum => _fields.values.fold(0, (s, c) => s + (double.tryParse(normalizeNumber(c.text) ?? '') ?? 0));

  Future<void> _addKey() async {
    final ref = await context.read<Session>().reference();
    if (!mounted) return;
    final list = widget.dimension == 'ASSET_TYPE' ? ref.assetTypes : ref.countries;
    final picked = await pickFromList(context, AppLocalizations.of(context).target,
        list.where((c) => !_fields.containsKey(c.code)).map((c) => MapEntry(c.code, c.label)).toList());
    if (picked != null) {
      setState(() {
        _labels[picked] = ref.label(list, picked);
        _fields[picked] = TextEditingController();
      });
    }
  }

  Future<void> _save() async {
    setState(() => _busy = true);
    final targets = <String, String>{};
    _fields.forEach((k, c) {
      final v = normalizeNumber(c.text);
      if (v != null && double.parse(v) > 0) targets[k] = v;
    });
    try {
      await context.read<Session>().api.setTargets(widget.portfolio.id, widget.dimension, targets);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final sum = _sum;
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: ListView(shrinkWrap: true, padding: const EdgeInsets.all(16), children: [
        Text(t.editTargets, style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 4),
        Text(t.targetsHint),
        const SizedBox(height: 12),
        for (final k in _fields.keys)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: TextField(
              controller: _fields[k],
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: InputDecoration(labelText: _labels[k], suffixText: '%', border: const OutlineInputBorder()),
              onChanged: (_) => setState(() {}),
            ),
          ),
        TextButton.icon(onPressed: _addKey, icon: const Icon(Icons.add), label: Text(t.target)),
        Text(t.targetsSum(Fmt(context).number(sum, maxDecimals: 2)),
            style: TextStyle(color: (sum - 100).abs() < 0.01 || sum == 0 ? null : Theme.of(context).colorScheme.error)),
        const SizedBox(height: 12),
        FilledButton(onPressed: _busy ? null : _save, child: Text(t.save)),
      ]),
    );
  }
}
