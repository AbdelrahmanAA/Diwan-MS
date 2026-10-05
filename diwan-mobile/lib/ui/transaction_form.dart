import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

const _tradeTypes = {'BUY', 'SELL'};
const _cashTypes = ['DEPOSIT', 'WITHDRAWAL', 'INCOME', 'EXPENSE'];
const _otherTypes = ['BUY', 'SELL', 'DIVIDEND', 'INCOME', 'EXPENSE'];

/// Bottom sheet: buy / sell / dividend / income / expense (deposit / withdrawal for cash).
class TransactionForm extends StatefulWidget {
  const TransactionForm({super.key, required this.holding});

  final Holding holding;

  @override
  State<TransactionForm> createState() => _TransactionFormState();
}

class _TransactionFormState extends State<TransactionForm> {
  final _form = GlobalKey<FormState>();
  late String _type = widget.holding.isCash ? 'DEPOSIT' : 'BUY';
  final _quantity = TextEditingController();
  final _price = TextEditingController();
  final _amount = TextEditingController();
  final _fees = TextEditingController();
  final _note = TextEditingController();
  DateTime _date = DateTime.now();
  bool _busy = false;

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    final trade = _tradeTypes.contains(_type);
    try {
      await context.read<Session>().api.addTransaction(widget.holding.id, {
        'type': _type,
        'tradeDate': Fmt.isoDate(_date),
        if (trade) 'quantity': normalizeNumber(_quantity.text),
        if (trade) 'price': normalizeNumber(_price.text),
        if (!trade) 'amount': normalizeNumber(_amount.text),
        'fees': normalizeNumber(_fees.text) ?? '0',
        'note': _note.text.trim().isEmpty ? null : _note.text.trim(),
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
    final trade = _tradeTypes.contains(_type);
    String? number(String? v) =>
        v == null || v.trim().isEmpty ? t.required : normalizeNumber(v) == null ? t.invalidNumber : null;
    String? optionalNumber(String? v) => v == null || v.trim().isEmpty || normalizeNumber(v) != null ? null : t.invalidNumber;
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: FutureBuilder<Reference>(
        future: session.reference(),
        builder: (context, snap) {
          final ref = snap.data;
          final types = widget.holding.isCash ? _cashTypes : _otherTypes;
          return Form(
            key: _form,
            child: ListView(shrinkWrap: true, padding: const EdgeInsets.all(16), children: [
              Text(t.addTransaction, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 12),
              Wrap(spacing: 8, children: [
                for (final type in types)
                  ChoiceChip(
                    label: Text(ref?.label(ref.transactionTypes, type) ?? type),
                    selected: _type == type,
                    onSelected: (_) => setState(() => _type = type),
                  ),
              ]),
              const SizedBox(height: 12),
              if (trade)
                Row(children: [
                  Expanded(child: TextFormField(controller: _quantity, keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      decoration: InputDecoration(labelText: t.quantity, border: const OutlineInputBorder()), validator: number)),
                  const SizedBox(width: 12),
                  Expanded(child: TextFormField(controller: _price, keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      decoration: InputDecoration(labelText: t.price, suffixText: widget.holding.currency, border: const OutlineInputBorder()),
                      validator: number)),
                ])
              else
                TextFormField(controller: _amount, keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: InputDecoration(labelText: t.amount, suffixText: widget.holding.currency, border: const OutlineInputBorder()),
                    validator: number),
              const SizedBox(height: 12),
              TextFormField(controller: _fees, keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: InputDecoration(labelText: t.fees, border: const OutlineInputBorder()), validator: optionalNumber),
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(t.date),
                subtitle: Text(Fmt(context).date(_date)),
                trailing: const Icon(Icons.calendar_today),
                onTap: () async {
                  final d = await showDatePicker(context: context, initialDate: _date, firstDate: DateTime(1970), lastDate: DateTime.now());
                  if (d != null) setState(() => _date = d);
                },
              ),
              TextFormField(controller: _note, maxLength: 2000,
                  decoration: InputDecoration(labelText: t.note, border: const OutlineInputBorder())),
              const SizedBox(height: 8),
              FilledButton(onPressed: _busy ? null : _save, child: Text(t.save)),
            ]),
          );
        },
      ),
    );
  }
}

/// Bottom sheet: today's value of a property / business / fund.
class ValuationForm extends StatefulWidget {
  const ValuationForm({super.key, required this.holding});

  final Holding holding;

  @override
  State<ValuationForm> createState() => _ValuationFormState();
}

class _ValuationFormState extends State<ValuationForm> {
  final _form = GlobalKey<FormState>();
  final _value = TextEditingController();
  final _note = TextEditingController();
  DateTime _date = DateTime.now();
  bool _busy = false;

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      await context.read<Session>().api.addValuation(widget.holding.id, {
        'valuationDate': Fmt.isoDate(_date),
        'value': normalizeNumber(_value.text),
        'note': _note.text.trim().isEmpty ? null : _note.text.trim(),
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
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Form(
        key: _form,
        child: ListView(shrinkWrap: true, padding: const EdgeInsets.all(16), children: [
          Text(t.addValuation, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          TextFormField(
            controller: _value,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(labelText: t.value, suffixText: widget.holding.currency, border: const OutlineInputBorder()),
            validator: (v) => v == null || v.trim().isEmpty ? t.required : normalizeNumber(v) == null ? t.invalidNumber : null,
          ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(t.date),
            subtitle: Text(Fmt(context).date(_date)),
            trailing: const Icon(Icons.calendar_today),
            onTap: () async {
              final d = await showDatePicker(context: context, initialDate: _date, firstDate: DateTime(1970), lastDate: DateTime.now());
              if (d != null) setState(() => _date = d);
            },
          ),
          TextFormField(controller: _note, maxLength: 2000,
              decoration: InputDecoration(labelText: t.note, border: const OutlineInputBorder())),
          const SizedBox(height: 8),
          FilledButton(onPressed: _busy ? null : _save, child: Text(t.save)),
        ]),
      ),
    );
  }
}
