import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

class AlertsTab extends StatelessWidget {
  const AlertsTab({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: Text(t.alerts),
          bottom: TabBar(tabs: [Tab(text: t.alerts), Tab(text: t.alertRules)]),
        ),
        body: const TabBarView(children: [_Inbox(), _Rules()]),
      ),
    );
  }
}

class _Inbox extends StatefulWidget {
  const _Inbox();

  @override
  State<_Inbox> createState() => _InboxState();
}

class _InboxState extends State<_Inbox> {
  Key _key = UniqueKey();

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Loader<ResultPage<AlertItem>>(
      key: _key,
      load: () => api.alerts(),
      builder: (context, page, reload) {
        if (page.items.isEmpty) return EmptyState(icon: Icons.notifications_none, text: t.noAlerts);
        final f = Fmt(context);
        return ListView(children: [
          if (page.items.any((a) => a.readAt == null))
            Align(
              alignment: AlignmentDirectional.centerEnd,
              child: TextButton(
                onPressed: () async {
                  await api.markAllRead();
                  setState(() => _key = UniqueKey());
                },
                child: Text(t.markAllRead),
              ),
            ),
          for (final a in page.items)
            ListTile(
              leading: Icon(a.readAt == null ? Icons.notifications_active : Icons.notifications_none,
                  color: a.readAt == null ? Theme.of(context).colorScheme.primary : null),
              title: Text(a.title, style: TextStyle(fontWeight: a.readAt == null ? FontWeight.bold : null)),
              subtitle: Text('${a.message}\n${f.dateTime(a.triggeredAt)}'),
              isThreeLine: true,
            ),
        ]);
      },
    );
  }
}

class _Rules extends StatefulWidget {
  const _Rules();

  @override
  State<_Rules> createState() => _RulesState();
}

class _RulesState extends State<_Rules> {
  Key _key = UniqueKey();

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add_alert),
        label: Text(t.newAlertRule),
        onPressed: () async {
          final ok = await showModalBottomSheet<bool>(
              context: context, isScrollControlled: true, builder: (_) => const _RuleForm());
          if (ok == true) setState(() => _key = UniqueKey());
        },
      ),
      body: Loader<List<AlertRule>>(
        key: _key,
        load: api.alertRules,
        builder: (context, rules, reload) {
          if (rules.isEmpty) return EmptyState(icon: Icons.rule, text: t.noRules);
          final f = Fmt(context);
          return ListView(padding: const EdgeInsets.only(bottom: 96), children: [
            for (final r in rules)
              ListTile(
                leading: Icon(r.active ? Icons.alarm_on : Icons.alarm_off),
                title: Text('${r.typeLabel} ${f.number(r.threshold)}${r.type.contains('PERCENT') || r.type == 'ALLOCATION_DRIFT' ? '%' : ''}'),
                subtitle: Text([
                  if (r.symbol != null) r.symbol!,
                  if (r.lastTriggeredAt != null) f.dateTime(r.lastTriggeredAt),
                ].join(' · ')),
                trailing: IconButton(
                  icon: const Icon(Icons.delete_outline),
                  onPressed: () async {
                    try {
                      await api.deleteAlertRule(r.id);
                      setState(() => _key = UniqueKey());
                    } catch (e) {
                      if (context.mounted) showError(context, e);
                    }
                  },
                ),
              ),
          ]);
        },
      ),
    );
  }
}

/// Price / move alerts on a symbol, take-profit / stop-loss on an investment, drift on a portfolio.
class _RuleForm extends StatefulWidget {
  const _RuleForm();

  @override
  State<_RuleForm> createState() => _RuleFormState();
}

class _RuleFormState extends State<_RuleForm> {
  final _form = GlobalKey<FormState>();
  String _type = 'PRICE_ABOVE';
  final _symbol = TextEditingController();
  final _threshold = TextEditingController();
  int? _holdingId;
  int? _portfolioId;
  bool _busy = false;
  late Future<List<PortfolioOverview>> _portfolios;
  Future<List<Holding>>? _holdings;

  @override
  void initState() {
    super.initState();
    _portfolios = context.read<Session>().api.portfolios();
  }

  bool get _needsSymbol => const {'PRICE_ABOVE', 'PRICE_BELOW', 'DAILY_MOVE_PERCENT'}.contains(_type);
  bool get _needsHolding => const {'GAIN_PERCENT', 'LOSS_PERCENT'}.contains(_type);
  bool get _needsPortfolio => _type == 'ALLOCATION_DRIFT';

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      await context.read<Session>().api.createAlertRule({
        'type': _type,
        if (_needsSymbol) 'symbol': _symbol.text.trim(),
        if (_needsHolding) 'holdingId': _holdingId,
        if (_needsPortfolio || _needsHolding) 'portfolioId': _portfolioId,
        'threshold': normalizeNumber(_threshold.text),
      });
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
    final session = context.read<Session>();
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: FutureBuilder<Reference>(
        future: session.reference(),
        builder: (context, snap) {
          final ref = snap.data;
          return Form(
            key: _form,
            child: ListView(shrinkWrap: true, padding: const EdgeInsets.all(16), children: [
              Text(t.newAlertRule, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _type,
                isExpanded: true,
                items: [
                  for (final a in ref?.alertTypes ?? <CodeLabel>[]) DropdownMenuItem(value: a.code, child: Text(a.label)),
                ],
                onChanged: (v) => setState(() => _type = v!),
              ),
              const SizedBox(height: 12),
              if (_needsSymbol)
                TextFormField(
                  controller: _symbol,
                  textDirection: TextDirection.ltr,
                  textCapitalization: TextCapitalization.characters,
                  decoration: InputDecoration(labelText: t.symbol, border: const OutlineInputBorder()),
                  validator: (v) => v == null || v.trim().isEmpty ? t.required : null,
                ),
              if (_needsHolding || _needsPortfolio)
                FutureBuilder<List<PortfolioOverview>>(
                  future: _portfolios,
                  builder: (c, s) => DropdownButtonFormField<int>(
                    initialValue: _portfolioId,
                    isExpanded: true,
                    decoration: InputDecoration(labelText: t.portfolio, border: const OutlineInputBorder()),
                    items: [
                      for (final p in s.data ?? <PortfolioOverview>[])
                        DropdownMenuItem(value: p.portfolio.id, child: Text(p.portfolio.name)),
                    ],
                    validator: (v) => v == null ? t.required : null,
                    onChanged: (v) => setState(() {
                      _portfolioId = v;
                      _holdingId = null;
                      _holdings = session.api.summary(v!, groupBy: const ['ASSET_TYPE'])
                          .then((s) => [for (final g in s.groups) ...g.holdings]);
                    }),
                  ),
                ),
              if (_needsHolding && _holdings != null) ...[
                const SizedBox(height: 12),
                FutureBuilder<List<Holding>>(
                  future: _holdings,
                  builder: (c, s) => DropdownButtonFormField<int>(
                    initialValue: _holdingId,
                    isExpanded: true,
                    decoration: InputDecoration(labelText: t.investment, border: const OutlineInputBorder()),
                    items: [
                      for (final h in s.data ?? <Holding>[]) DropdownMenuItem(value: h.id, child: Text(h.name)),
                    ],
                    validator: (v) => v == null ? t.required : null,
                    onChanged: (v) => setState(() => _holdingId = v),
                  ),
                ),
              ],
              const SizedBox(height: 12),
              TextFormField(
                controller: _threshold,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                    labelText: _needsSymbol && _type != 'DAILY_MOVE_PERCENT' ? t.threshold : t.thresholdPercent,
                    border: const OutlineInputBorder()),
                validator: (v) => v == null || v.trim().isEmpty ? t.required : normalizeNumber(v) == null ? t.invalidNumber : null,
              ),
              const SizedBox(height: 16),
              FilledButton(onPressed: _busy ? null : _save, child: Text(t.save)),
            ]),
          );
        },
      ),
    );
  }
}
