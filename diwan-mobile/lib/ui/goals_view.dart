import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

class GoalsView extends StatefulWidget {
  const GoalsView({super.key, required this.portfolio});

  final Portfolio portfolio;

  @override
  State<GoalsView> createState() => _GoalsViewState();
}

class _GoalsViewState extends State<GoalsView> {
  Key _key = UniqueKey();

  Future<void> _edit([Goal? goal]) async {
    final saved = await showModalBottomSheet<bool>(
        context: context, isScrollControlled: true, builder: (_) => _GoalForm(portfolio: widget.portfolio, goal: goal));
    if (saved == true) setState(() => _key = UniqueKey());
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Loader<List<Goal>>(
      key: _key,
      load: () => api.goals(widget.portfolio.id),
      builder: (context, goals, reload) {
        final f = Fmt(context);
        return ListView(padding: const EdgeInsets.only(bottom: 96), children: [
          for (final g in goals)
            Card(
              margin: const EdgeInsets.fromLTRB(12, 12, 12, 0),
              child: ListTile(
                onTap: () => _edit(g),
                title: Text(g.name),
                subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  const SizedBox(height: 6),
                  LinearProgressIndicator(value: (g.progressPercent / 100).clamp(0, 1)),
                  const SizedBox(height: 6),
                  Text('${f.money(g.currentValue, g.currency)} / ${f.money(g.targetAmount, g.currency)}'
                      '${g.targetDate == null ? '' : ' · ${f.date(g.targetDate)}'}'),
                  if (g.reached)
                    Text(t.goalReached, style: TextStyle(color: pnlColor(context, 1)))
                  else if (g.monthlyNeeded != null)
                    Text(t.monthlyNeeded(f.money(g.monthlyNeeded, g.currency))),
                ]),
                trailing: Text(f.percent(g.progressPercent, signed: false)),
              ),
            ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: OutlinedButton.icon(onPressed: _edit, icon: const Icon(Icons.flag_outlined), label: Text(t.newGoal)),
          ),
        ]);
      },
    );
  }
}

class _GoalForm extends StatefulWidget {
  const _GoalForm({required this.portfolio, this.goal});

  final Portfolio portfolio;
  final Goal? goal;

  @override
  State<_GoalForm> createState() => _GoalFormState();
}

class _GoalFormState extends State<_GoalForm> {
  final _form = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.goal?.name);
  late final _amount = TextEditingController(text: widget.goal?.targetAmount.toStringAsFixed(0));
  late DateTime? _date = widget.goal?.targetDate;
  bool _busy = false;

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      await context.read<Session>().api.saveGoal(widget.portfolio.id, widget.goal?.id, {
        'name': _name.text.trim(),
        'targetAmount': normalizeNumber(_amount.text),
        'targetDate': _date == null ? null : Fmt.isoDate(_date!),
      });
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _delete() async {
    try {
      await context.read<Session>().api.deleteGoal(widget.goal!.id);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Form(
        key: _form,
        child: ListView(shrinkWrap: true, padding: const EdgeInsets.all(16), children: [
          Text(widget.goal == null ? t.newGoal : widget.goal!.name, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          TextFormField(controller: _name, maxLength: 120,
              decoration: InputDecoration(labelText: t.name, border: const OutlineInputBorder()),
              validator: (v) => v == null || v.trim().isEmpty ? t.required : null),
          TextFormField(controller: _amount, keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: InputDecoration(labelText: t.targetAmount, suffixText: widget.portfolio.baseCurrency,
                  border: const OutlineInputBorder()),
              validator: (v) => v == null || v.trim().isEmpty ? t.required : normalizeNumber(v) == null ? t.invalidNumber : null),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(t.targetDate),
            subtitle: Text(Fmt(context).date(_date)),
            trailing: const Icon(Icons.calendar_today),
            onTap: () async {
              final now = DateTime.now();
              final d = await showDatePicker(context: context, initialDate: _date ?? DateTime(now.year + 5),
                  firstDate: now.add(const Duration(days: 1)), lastDate: DateTime(now.year + 60));
              if (d != null) setState(() => _date = d);
            },
          ),
          const SizedBox(height: 8),
          FilledButton(onPressed: _busy ? null : _save, child: Text(t.save)),
          if (widget.goal != null) TextButton(onPressed: _delete, child: Text(t.delete)),
        ]),
      ),
    );
  }
}
