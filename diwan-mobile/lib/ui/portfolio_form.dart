import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'portfolios_tab.dart';
import 'widgets.dart';

const portfolioColors = ['#7C3AED', '#2563EB', '#0EA5E9', '#16A34A', '#CA8A04', '#EA580C', '#DC2626', '#DB2777'];

class PortfolioForm extends StatefulWidget {
  const PortfolioForm({super.key, this.portfolio});

  final Portfolio? portfolio;

  @override
  State<PortfolioForm> createState() => _PortfolioFormState();
}

class _PortfolioFormState extends State<PortfolioForm> {
  final _form = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.portfolio?.name);
  late final _description = TextEditingController(text: widget.portfolio?.description);
  late String _currency = widget.portfolio?.baseCurrency ?? 'USD';
  late String _color = widget.portfolio?.color ?? portfolioColors.first;
  bool _busy = false;

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      final saved = await context.read<Session>().api.savePortfolio(widget.portfolio?.id, {
        'name': _name.text.trim(),
        'description': _description.text.trim().isEmpty ? null : _description.text.trim(),
        'baseCurrency': _currency,
        'color': _color,
      });
      if (mounted) Navigator.pop(context, saved);
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
    return Scaffold(
      appBar: AppBar(title: Text(widget.portfolio == null ? t.newPortfolio : t.editPortfolio)),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          TextFormField(
            controller: _name,
            maxLength: 120,
            decoration: InputDecoration(labelText: t.name, border: const OutlineInputBorder()),
            validator: (v) => v == null || v.trim().isEmpty ? t.required : null,
          ),
          const SizedBox(height: 8),
          TextFormField(
            controller: _description,
            maxLines: 3,
            maxLength: 2000,
            decoration: InputDecoration(labelText: t.description, border: const OutlineInputBorder()),
          ),
          const SizedBox(height: 8),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(t.baseCurrency),
            subtitle: Text(_currency),
            trailing: const Icon(Icons.arrow_drop_down),
            onTap: () async {
              final ref = await session.reference();
              if (!context.mounted) return;
              final picked = await pickFromList(
                  context, t.baseCurrency, ref.currencies.map((c) => MapEntry(c.code, c.label)).toList());
              if (picked != null) setState(() => _currency = picked);
            },
          ),
          const SizedBox(height: 8),
          Wrap(spacing: 12, children: [
            for (final c in portfolioColors)
              GestureDetector(
                onTap: () => setState(() => _color = c),
                child: CircleAvatar(
                  radius: 18,
                  backgroundColor: parseColor(c),
                  child: _color == c ? const Icon(Icons.check, color: Colors.white) : null,
                ),
              ),
          ]),
          const SizedBox(height: 24),
          FilledButton(onPressed: _busy ? null : _save, child: Text(t.save)),
        ]),
      ),
    );
  }
}
