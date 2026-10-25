import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

// Codes the transactions service stores (lower case); labels are the app's.
const moneyCategories = ['food', 'transport', 'bills', 'health', 'shopping', 'entertainment', 'salary', 'general'];
const moneyCurrencies = ['EGP', 'USD', 'EUR', 'SAR', 'AED', 'GBP'];
const paymentMethods = ['cash', 'card', 'transfer', 'wallet', 'cheque'];
const moneyStatuses = ['completed', 'pending', 'cancelled'];

String categoryLabel(AppLocalizations t, String code) => switch (code.toLowerCase()) {
      'food' => t.catFood,
      'transport' => t.catTransport,
      'bills' => t.catBills,
      'health' => t.catHealth,
      'shopping' => t.catShopping,
      'entertainment' => t.catEntertainment,
      'salary' => t.catSalary,
      'general' => t.catGeneral,
      _ => code,
    };

IconData categoryIcon(String code) => switch (code.toLowerCase()) {
      'food' => Icons.restaurant,
      'transport' => Icons.directions_car_outlined,
      'bills' => Icons.receipt_long_outlined,
      'health' => Icons.local_hospital_outlined,
      'shopping' => Icons.shopping_bag_outlined,
      'entertainment' => Icons.movie_outlined,
      'salary' => Icons.payments_outlined,
      _ => Icons.category_outlined,
    };

String paymentLabel(AppLocalizations t, String code) => switch (code) {
      'cash' => t.payCash,
      'card' => t.payCard,
      'transfer' => t.payTransfer,
      'wallet' => t.payWallet,
      'cheque' => t.payCheque,
      _ => code,
    };

String statusLabel(AppLocalizations t, String code) => switch (code) {
      'completed' => t.statusCompleted,
      'pending' => t.statusPending,
      'cancelled' => t.statusCancelled,
      _ => code,
    };

/// Financial history (diwan-transactions): totals per currency and the list of income/expenses.
class MoneyScreen extends StatefulWidget {
  const MoneyScreen({super.key});

  @override
  State<MoneyScreen> createState() => _MoneyScreenState();
}

class _MoneyScreenState extends State<MoneyScreen> {
  String? _type; // null = all, else income | expense
  Key _key = UniqueKey();

  void _refresh() => setState(() => _key = UniqueKey());

  Future<void> _open([MoneyTransaction? tx]) async {
    final changed = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => MoneyTransactionForm(transaction: tx)));
    if (changed == true) _refresh();
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Scaffold(
      appBar: AppBar(title: Text(t.featureTransactions)),
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add),
        label: Text(t.newTransaction),
        onPressed: _open,
      ),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
          child: SegmentedButton<String?>(
            segments: [
              ButtonSegment(value: null, label: Text(t.all)),
              ButtonSegment(value: 'income', label: Text(t.incomeType)),
              ButtonSegment(value: 'expense', label: Text(t.expenseType)),
            ],
            selected: {_type},
            onSelectionChanged: (s) => setState(() {
              _type = s.first;
              _key = UniqueKey();
            }),
          ),
        ),
        Expanded(
          child: Loader<(List<CurrencyTotals>, List<MoneyTransaction>)>(
            key: _key,
            load: () async {
              final results = await Future.wait([api.moneySummary(), api.moneyTransactions(type: _type)]);
              return (results[0] as List<CurrencyTotals>, results[1] as List<MoneyTransaction>);
            },
            builder: (context, data, reload) {
              final (totals, list) = data;
              return ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.fromLTRB(12, 12, 12, 96),
                children: [
                  for (final c in totals) _TotalsCard(totals: c),
                  if (list.isEmpty)
                    Padding(
                      padding: const EdgeInsets.only(top: 48),
                      child: Column(children: [
                        Icon(Icons.receipt_long_outlined, size: 56, color: Theme.of(context).colorScheme.outline),
                        const SizedBox(height: 12),
                        Text(t.noMoneyTransactions, textAlign: TextAlign.center),
                      ]),
                    ),
                  for (final tx in list) _MoneyTile(tx: tx, onTap: () => _open(tx)),
                ],
              );
            },
          ),
        ),
      ]),
    );
  }
}

class _TotalsCard extends StatelessWidget {
  const _TotalsCard({required this.totals});

  final CurrencyTotals totals;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Wrap(spacing: 24, runSpacing: 8, children: [
          Metric(label: t.incomeType, child: Text(f.money(totals.income, totals.currency), style: TextStyle(color: pnlColor(context, 1)))),
          Metric(label: t.expenseType, child: Text(f.money(totals.expense, totals.currency), style: TextStyle(color: pnlColor(context, -1)))),
          Metric(label: t.net, child: PnlText(value: totals.net, currency: totals.currency, style: Theme.of(context).textTheme.titleMedium)),
        ]),
      ),
    );
  }
}

class _MoneyTile extends StatelessWidget {
  const _MoneyTile({required this.tx, required this.onTap});

  final MoneyTransaction tx;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final title = [tx.merchantName, tx.description].firstWhere((s) => s != null && s.trim().isNotEmpty, orElse: () => null) ??
        categoryLabel(t, tx.category);
    final subtitle = [
      categoryLabel(t, tx.category),
      f.date(tx.recordedAt),
      if (tx.bank != null && tx.bank != 'Unknown') tx.bank!,
      if (tx.status != null && tx.status != 'completed') statusLabel(t, tx.status!),
    ].join(' · ');
    return ListTile(
      onTap: onTap,
      leading: CircleAvatar(child: Icon(categoryIcon(tx.category))),
      title: Text(title, maxLines: 1, overflow: TextOverflow.ellipsis),
      subtitle: Text(subtitle),
      trailing: PnlText(value: tx.isIncome ? tx.amount : -tx.amount, currency: tx.currency),
    );
  }
}

/// Add or edit a transaction. The service cannot change type, currency, date or description of a saved
/// transaction, so those fields are read-only when editing.
class MoneyTransactionForm extends StatefulWidget {
  const MoneyTransactionForm({super.key, this.transaction});

  final MoneyTransaction? transaction;

  @override
  State<MoneyTransactionForm> createState() => _MoneyTransactionFormState();
}

class _MoneyTransactionFormState extends State<MoneyTransactionForm> {
  final _form = GlobalKey<FormState>();
  late final MoneyTransaction? _tx = widget.transaction;
  late String _type = _tx?.type.toLowerCase() ?? 'expense';
  late String _category = _tx?.category.toLowerCase() ?? 'general';
  late String _currency = _tx?.currency ?? 'EGP';
  late String _payment = _tx?.paymentMethod ?? 'card';
  late String _status = _tx?.status ?? 'completed';
  late DateTime _date = _tx?.recordedAt ?? DateTime.now();
  late final _amount = TextEditingController(text: _tx == null ? '' : _tx.amount.toString());
  late final _merchant = TextEditingController(text: _tx?.merchantName ?? '');
  late final _bank = TextEditingController(text: _tx?.bank == 'Unknown' ? '' : _tx?.bank ?? '');
  late final _description = TextEditingController(text: _tx?.description ?? '');
  late final _notes = TextEditingController(text: _tx?.notes ?? '');
  bool _busy = false;

  bool get _editing => _tx != null;

  static String? _text(TextEditingController c) => c.text.trim().isEmpty ? null : c.text.trim();

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      await context.read<Session>().api.saveMoneyTransaction(_tx?.id, {
        'type': _type,
        'category': _category,
        'amount': normalizeNumber(_amount.text),
        'currency': _currency,
        'merchantName': _text(_merchant),
        'bank': _text(_bank),
        'paymentMethod': _payment,
        'status': _status,
        'description': _text(_description),
        'notes': _text(_notes),
        if (!_editing) 'recordedAt': DateFormat("yyyy-MM-dd'T'HH:mm:ss").format(_date),
      });
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _delete() async {
    final t = AppLocalizations.of(context);
    if (!await confirm(context, t.confirmDelete) || !mounted) return;
    try {
      await context.read<Session>().api.deleteMoneyTransaction(_tx!.id);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final locked = _editing ? t.notEditable : null;
    return Scaffold(
      appBar: AppBar(
        title: Text(_editing ? t.editTransaction : t.newTransaction),
        actions: [
          if (_editing) IconButton(tooltip: t.delete, icon: const Icon(Icons.delete_outline), onPressed: _delete),
        ],
      ),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          SegmentedButton<String>(
            segments: [
              ButtonSegment(value: 'expense', label: Text(t.expenseType), icon: const Icon(Icons.arrow_upward)),
              ButtonSegment(value: 'income', label: Text(t.incomeType), icon: const Icon(Icons.arrow_downward)),
            ],
            selected: {_type},
            onSelectionChanged: _editing ? null : (s) => setState(() => _type = s.first),
          ),
          const SizedBox(height: 16),
          Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Expanded(
              flex: 3,
              child: TextFormField(
                controller: _amount,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(labelText: t.amount),
                validator: (v) {
                  if (v == null || v.trim().isEmpty) return t.required;
                  final n = normalizeNumber(v);
                  return n == null || double.parse(n) <= 0 ? t.invalidNumber : null;
                },
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              flex: 2,
              child: DropdownButtonFormField<String>(
                initialValue: _currency,
                isExpanded: true,
                decoration: InputDecoration(labelText: t.currency, helperText: locked),
                items: {...moneyCurrencies, _currency}.map((c) => DropdownMenuItem(value: c, child: Text(c))).toList(),
                onChanged: _editing ? null : (v) => setState(() => _currency = v!),
              ),
            ),
          ]),
          const SizedBox(height: 16),
          Text(t.category, style: Theme.of(context).textTheme.labelLarge),
          const SizedBox(height: 8),
          Wrap(spacing: 8, runSpacing: 4, children: [
            for (final c in {...moneyCategories, _category})
              ChoiceChip(
                avatar: Icon(categoryIcon(c), size: 18),
                label: Text(categoryLabel(t, c)),
                selected: _category == c,
                onSelected: (_) => setState(() => _category = c),
              ),
          ]),
          const SizedBox(height: 16),
          TextFormField(controller: _merchant, decoration: InputDecoration(labelText: t.merchant)),
          TextFormField(controller: _bank, decoration: InputDecoration(labelText: t.bank)),
          const SizedBox(height: 8),
          Row(children: [
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: _payment,
                isExpanded: true,
                decoration: InputDecoration(labelText: t.paymentMethod),
                items: {...paymentMethods, _payment}
                    .map((c) => DropdownMenuItem(value: c, child: Text(paymentLabel(t, c))))
                    .toList(),
                onChanged: (v) => setState(() => _payment = v!),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: _status,
                isExpanded: true,
                decoration: InputDecoration(labelText: t.status),
                items: {...moneyStatuses, _status}
                    .map((c) => DropdownMenuItem(value: c, child: Text(statusLabel(t, c))))
                    .toList(),
                onChanged: (v) => setState(() => _status = v!),
              ),
            ),
          ]),
          const SizedBox(height: 8),
          ListTile(
            contentPadding: EdgeInsets.zero,
            enabled: !_editing,
            leading: const Icon(Icons.event),
            title: Text(t.date),
            subtitle: Text(_editing ? '${f.date(_date)} · $locked' : f.date(_date)),
            onTap: () async {
              final d = await showDatePicker(
                  context: context, initialDate: _date, firstDate: DateTime(2000), lastDate: DateTime.now());
              if (d != null) setState(() => _date = DateTime(d.year, d.month, d.day, _date.hour, _date.minute));
            },
          ),
          TextFormField(
            controller: _description,
            enabled: !_editing,
            decoration: InputDecoration(labelText: t.description, helperText: locked),
          ),
          TextFormField(controller: _notes, maxLines: 3, minLines: 1, decoration: InputDecoration(labelText: t.notes)),
          const SizedBox(height: 24),
          FilledButton(
            onPressed: _busy ? null : _save,
            child: _busy
                ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : Text(t.save),
          ),
        ]),
      ),
    );
  }
}
