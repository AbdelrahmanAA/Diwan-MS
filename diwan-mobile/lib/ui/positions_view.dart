import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

const _arabicBankNames = {'Al Rajhi': 'مصرف الراجحي'};

/// Financial History, second tab: per bank the accounts (estimated balance), cards (available / owed) and
/// loans (remaining) as known from the bank SMS, plus what the user entered once (balance, limit, name).
class PositionsView extends StatefulWidget {
  const PositionsView({super.key});

  @override
  State<PositionsView> createState() => _PositionsViewState();
}

class _PositionsViewState extends State<PositionsView> with AutomaticKeepAliveClientMixin {
  Key _key = UniqueKey();

  @override
  bool get wantKeepAlive => true;

  Future<void> _edit(String bank, String kind, String ref,
      {String? label, double? limit, String? currency}) async {
    final saved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _ProductSheet(bank: bank, kind: kind, ref: ref, label: label, limit: limit, currency: currency),
    );
    if (saved == true) setState(() => _key = UniqueKey());
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final t = AppLocalizations.of(context);
    final arabic = Localizations.localeOf(context).languageCode == 'ar';
    return Loader<List<BankPositions>>(
      key: _key,
      load: context.read<Session>().api.positions,
      builder: (context, banks, reload) {
        if (banks.isEmpty) return EmptyState(icon: Icons.account_balance_outlined, text: t.positionsEmpty);
        return ListView(padding: const EdgeInsets.fromLTRB(12, 8, 12, 24), children: [
          for (final b in banks) ...[
            Padding(
              padding: const EdgeInsets.fromLTRB(4, 12, 4, 4),
              child: Text(arabic ? _arabicBankNames[b.bank] ?? b.bank : b.bank,
                  style: Theme.of(context).textTheme.titleLarge),
            ),
            if (b.accounts.isNotEmpty) _Section(title: t.accounts),
            for (final a in b.accounts)
              _AccountTile(
                account: a,
                onTap: () => _edit(b.bank, 'ACCOUNT', a.ref, label: a.label, currency: a.currency),
              ),
            if (b.cards.isNotEmpty) _Section(title: t.cards),
            for (final c in b.cards)
              _CardTile(
                card: c,
                onTap: () => _edit(b.bank, 'CARD', c.ref, label: c.label, limit: c.creditLimit, currency: c.currency),
              ),
            if (b.loans.isNotEmpty) _Section(title: t.loans),
            for (final l in b.loans)
              _LoanTile(loan: l, onTap: () => _edit(b.bank, 'LOAN', l.ref, label: l.label, currency: l.currency)),
          ],
        ]);
      },
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(4, 8, 4, 2),
        child: Text(title,
            style: Theme.of(context).textTheme.labelLarge?.copyWith(color: Theme.of(context).colorScheme.primary)),
      );
}

/// Amount on the trailing side, or a dash when unknown.
Widget _amount(BuildContext context, double? value, String currency) => Text(
      value == null ? '—' : Fmt(context).money(value, currency),
      style: Theme.of(context).textTheme.titleMedium,
    );

class _AccountTile extends StatelessWidget {
  const _AccountTile({required this.account, required this.onTap});

  final AccountPosition account;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final a = account;
    return Card(
      child: ListTile(
        onTap: onTap,
        leading: const CircleAvatar(child: Icon(Icons.account_balance_outlined)),
        title: Text(a.label ?? t.accountTitle(a.ref)),
        subtitle: Text([
          if (a.label != null) t.accountTitle(a.ref),
          a.balance == null ? t.enterBalance : t.balanceEstimated(f.date(a.balanceSince)),
          t.monthInOut(f.money(a.monthIn, a.currency), f.money(a.monthOut, a.currency)),
        ].join('\n')),
        isThreeLine: true,
        trailing: _amount(context, a.balance, a.currency),
      ),
    );
  }
}

class _CardTile extends StatelessWidget {
  const _CardTile({required this.card, required this.onTap});

  final CardPosition card;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final c = card;
    final name = t.cardTitle(c.network ?? '', c.ref).trim();
    final lines = [
      if (c.label != null) name,
      if (c.available != null) t.availableAt(f.money(c.available, c.currency), f.date(c.availableAt)),
      // a card with a balance in its SMS is a credit card: what is owed needs its limit
      if (c.available != null)
        c.owed == null ? t.enterLimit : t.owedOfLimit(f.money(c.owed, c.currency), f.money(c.creditLimit, c.currency)),
      if (c.account != null) t.linkedAccount(c.account!),
      t.spentThisMonth(f.money(c.monthSpent, c.currency)),
    ];
    return Card(
      child: ListTile(
        onTap: onTap,
        leading: const CircleAvatar(child: Icon(Icons.credit_card)),
        title: Text(c.label ?? name),
        subtitle: Text(lines.join('\n')),
        isThreeLine: lines.length > 1,
        // what is owed when the limit is known, else what is available
        trailing: c.owed != null
            ? Text(f.money(c.owed, c.currency),
                style: Theme.of(context).textTheme.titleMedium?.copyWith(color: pnlColor(context, -1)))
            : _amount(context, c.available, c.currency),
      ),
    );
  }
}

class _LoanTile extends StatelessWidget {
  const _LoanTile({required this.loan, required this.onTap});

  final LoanPosition loan;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final l = loan;
    return Card(
      child: ListTile(
        onTap: onTap,
        leading: const CircleAvatar(child: Icon(Icons.request_quote_outlined)),
        title: Text(l.label ?? t.loanTitle),
        subtitle: Text([
          '${t.remaining} · ${f.date(l.remainingAt)}',
          if (l.lastInstalment != null) t.lastInstalment(f.money(l.lastInstalment, l.currency), f.date(l.remainingAt)),
        ].join('\n')),
        isThreeLine: l.lastInstalment != null,
        trailing: Text(f.money(l.remaining, l.currency),
            style: Theme.of(context).textTheme.titleMedium?.copyWith(color: pnlColor(context, -1))),
      ),
    );
  }
}

/// Name for any product; the current balance for an account, the credit limit for a card.
class _ProductSheet extends StatefulWidget {
  const _ProductSheet({required this.bank, required this.kind, required this.ref, this.label, this.limit, this.currency});

  final String bank;
  final String kind;
  final String ref;
  final String? label;
  final double? limit;
  final String? currency;

  @override
  State<_ProductSheet> createState() => _ProductSheetState();
}

class _ProductSheetState extends State<_ProductSheet> {
  final _form = GlobalKey<FormState>();
  late final _label = TextEditingController(text: widget.label ?? '');
  final _balance = TextEditingController();
  late final _limit = TextEditingController(text: widget.limit?.toString() ?? '');
  bool _busy = false;

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      await context.read<Session>().api.saveBankProduct(
            widget.bank,
            widget.kind,
            widget.ref,
            label: _label.text.trim().isEmpty ? null : _label.text.trim(),
            balance: widget.kind == 'ACCOUNT' ? normalizeNumber(_balance.text) : null,
            creditLimit: widget.kind == 'CARD' ? normalizeNumber(_limit.text) : null,
            currency: widget.currency,
          );
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
    String? number(String? v) => v == null || v.trim().isEmpty || normalizeNumber(v) != null ? null : t.invalidNumber;
    final title = switch (widget.kind) {
      'ACCOUNT' => t.accountTitle(widget.ref),
      'CARD' => t.cardTitle('', widget.ref).trim(),
      _ => t.loanTitle,
    };
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Form(
        key: _form,
        child: ListView(shrinkWrap: true, padding: const EdgeInsets.all(16), children: [
          Text(title, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          TextFormField(controller: _label, decoration: InputDecoration(labelText: t.nickname)),
          if (widget.kind == 'ACCOUNT')
            TextFormField(
              controller: _balance,
              autofocus: true,
              keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
              decoration: InputDecoration(
                  labelText: t.currentBalance, helperText: t.currentBalanceHint, helperMaxLines: 3, suffixText: widget.currency),
              validator: number,
            ),
          if (widget.kind == 'CARD')
            TextFormField(
              controller: _limit,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: InputDecoration(labelText: t.creditLimit, suffixText: widget.currency),
              validator: number,
            ),
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
