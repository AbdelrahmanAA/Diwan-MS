import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

const _marketTraded = {'STOCK', 'ETF', 'MUTUAL_FUND', 'BOND', 'CRYPTO', 'COMMODITY'};
const _goldKarats = [24, 22, 21, 18];

/// The gold symbols are priced per gram, one per karat.
bool isGold(String? symbol) => symbol != null && RegExp(r'^GOLD-\d+K$').hasMatch(symbol);

/// Add (or edit) an investment. Market-traded types can be linked to a symbol, which fills in country,
/// currency and the live price; everything else is valued from manual valuations. A listed symbol stays in
/// the currency of its market; gold can be held in any currency.
class HoldingForm extends StatefulWidget {
  const HoldingForm({super.key, required this.portfolioId, this.holding, this.baseCurrency});

  final int portfolioId;
  final Holding? holding;
  final String? baseCurrency; // suggested for gold, which has no market currency

  @override
  State<HoldingForm> createState() => _HoldingFormState();
}

class _HoldingFormState extends State<HoldingForm> {
  final _form = GlobalKey<FormState>();
  late String _type = widget.holding?.assetType ?? 'STOCK';
  late String? _country = widget.holding?.countryCode;
  late String? _currency = widget.holding?.currency;
  late String? _symbol = widget.holding?.symbol;
  String? _marketCurrency;
  late final _name = TextEditingController(text: widget.holding?.name);
  late final _notes = TextEditingController(text: widget.holding?.notes);
  late final _address = TextEditingController(text: widget.holding?.attributes['address']?.toString());
  final _quantity = TextEditingController();
  final _price = TextEditingController();
  final _fees = TextEditingController();
  DateTime _date = DateTime.now();
  bool _busy = false;

  bool get _editing => widget.holding != null;
  bool get _isCash => _type == 'CASH';
  bool get _isProperty => _type == 'REAL_ESTATE';
  bool get _isCommodity => _type == 'COMMODITY';
  bool get _currencyLocked => _symbol != null && !isGold(_symbol) && (_marketCurrency != null || _editing);

  Future<void> _pickSymbol() async {
    final match = await showModalBottomSheet<SymbolMatch>(
        context: context, isScrollControlled: true, builder: (_) => const _SymbolSearch());
    if (match == null || !mounted) return;
    await _useSymbol(match.symbol);
  }

  Future<void> _useSymbol(String symbol, {String? name}) async {
    setState(() {
      _symbol = symbol;
      _marketCurrency = null;
    });
    try {
      final i = await context.read<Session>().api.instrument(symbol);
      if (!mounted) return;
      setState(() {
        if (_name.text.trim().isEmpty) _name.text = name ?? i.name ?? i.symbol;
        if (isGold(symbol)) {
          _currency ??= widget.baseCurrency ?? i.currency;
        } else {
          _marketCurrency = i.currency;
          _country = i.countryCode ?? _country;
          _currency = i.currency ?? _currency;
        }
        // The quote is in the symbol's currency: a price in another currency would be wrong
        if (_price.text.isEmpty && i.quote != null && i.currency == _currency) _price.text = i.quote!.price.toString();
      });
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  Future<void> _pick(String title, List<CodeLabel> list, ValueChanged<String> set) async {
    final picked = await pickFromList(context, title, list.map((c) => MapEntry(c.code, c.label)).toList());
    if (picked != null) setState(() => set(picked));
  }

  Map<String, Object?>? _initialTransaction() {
    if (_editing) return null;
    final date = Fmt.isoDate(_date);
    if (_isCash) {
      final amount = normalizeNumber(_quantity.text);
      return amount == null ? null : {'type': 'DEPOSIT', 'tradeDate': date, 'amount': amount};
    }
    final q = normalizeNumber(_quantity.text);
    final p = normalizeNumber(_price.text);
    if (q == null || p == null) return null;
    return {'type': 'BUY', 'tradeDate': date, 'quantity': q, 'price': p, 'fees': normalizeNumber(_fees.text) ?? '0'};
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    final api = context.read<Session>().api;
    final attributes = {...?widget.holding?.attributes};
    if (_isProperty) {
      _address.text.trim().isEmpty ? attributes.remove('address') : attributes['address'] = _address.text.trim();
    }
    final body = <String, Object?>{
      'assetType': _type,
      'name': _name.text.trim(),
      'countryCode': _country,
      'currency': _currency,
      'symbol': _marketTraded.contains(_type) ? _symbol : null,
      'notes': _notes.text.trim().isEmpty ? null : _notes.text.trim(),
      'attributes': attributes,
      'initialTransaction': _initialTransaction(),
    };
    try {
      final h = _editing ? await api.updateHolding(widget.holding!.id, body) : await api.addHolding(widget.portfolioId, body);
      if (mounted) Navigator.pop(context, h);
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
    String? number(String? v) => v == null || v.trim().isEmpty || normalizeNumber(v) != null ? null : t.invalidNumber;
    return Scaffold(
      appBar: AppBar(title: Text(_editing ? t.editInvestment : t.addInvestment)),
      body: FutureBuilder<Reference>(
        future: session.reference(),
        builder: (context, snap) {
          if (snap.hasError) return ErrorView(message: errorText(context, snap.error!), onRetry: () => setState(() {}));
          if (!snap.hasData) return const Center(child: CircularProgressIndicator());
          final ref = snap.data!;
          return Form(
            key: _form,
            child: ListView(padding: const EdgeInsets.all(16), children: [
              DropdownButtonFormField<String>(
                initialValue: _type,
                decoration: InputDecoration(labelText: t.assetType, border: const OutlineInputBorder()),
                items: [for (final a in ref.assetTypes) DropdownMenuItem(value: a.code, child: Text(a.label))],
                onChanged: (v) => setState(() => _type = v!),
              ),
              const SizedBox(height: 12),
              if (_marketTraded.contains(_type)) ...[
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.search),
                  title: Text(_symbol ?? t.symbol),
                  subtitle: Text(t.symbolHint),
                  trailing: _symbol == null ? null : IconButton(icon: const Icon(Icons.clear), onPressed: () => setState(() {
                    _symbol = null;
                    _marketCurrency = null;
                  })),
                  onTap: _pickSymbol,
                ),
                const SizedBox(height: 8),
              ],
              if (_isCommodity) ...[
                Text(t.goldByKarat, style: Theme.of(context).textTheme.titleSmall),
                Wrap(spacing: 8, children: [
                  for (final k in _goldKarats)
                    ChoiceChip(
                      label: Text(t.goldKarat(k)),
                      selected: _symbol == 'GOLD-${k}K',
                      onSelected: (_) => _useSymbol('GOLD-${k}K', name: t.goldKarat(k)),
                    ),
                ]),
                if (isGold(_symbol)) Text(t.goldPriceNote, style: Theme.of(context).textTheme.bodySmall),
                const SizedBox(height: 8),
              ],
              TextFormField(
                controller: _name,
                maxLength: 120,
                decoration: InputDecoration(labelText: t.name, border: const OutlineInputBorder()),
                validator: (v) => v == null || v.trim().isEmpty ? t.required : null,
              ),
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(t.country),
                subtitle: Text(_country == null ? '-' : ref.label(ref.countries, _country!)),
                trailing: const Icon(Icons.arrow_drop_down),
                onTap: () => _pick(t.country, ref.countries, (v) => _country = v),
              ),
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(t.currency),
                subtitle: Text(_currency ?? '-'),
                trailing: _currencyLocked ? const Icon(Icons.lock_outline) : const Icon(Icons.arrow_drop_down),
                onTap: _currencyLocked ? null : () => _pick(t.currency, ref.currencies, (v) => _currency = v),
              ),
              if (_isProperty)
                TextFormField(
                  controller: _address,
                  decoration: InputDecoration(labelText: t.address, border: const OutlineInputBorder()),
                ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _notes,
                maxLines: 3,
                maxLength: 4000,
                decoration: InputDecoration(labelText: t.notes, border: const OutlineInputBorder()),
              ),
              if (!_editing) ...[
                const SizedBox(height: 8),
                Text(t.firstTransaction, style: Theme.of(context).textTheme.titleSmall),
                const SizedBox(height: 8),
                Row(children: [
                  Expanded(
                    child: TextFormField(
                      controller: _quantity,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      decoration: InputDecoration(labelText: _isCash ? t.amount : isGold(_symbol) ? t.grams : t.quantity, border: const OutlineInputBorder()),
                      validator: number,
                    ),
                  ),
                  if (!_isCash) ...[
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextFormField(
                        controller: _price,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: InputDecoration(labelText: isGold(_symbol) ? t.pricePerGram : t.price, border: const OutlineInputBorder()),
                        validator: number,
                      ),
                    ),
                  ],
                ]),
                if (!_isCash) ...[
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _fees,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: InputDecoration(labelText: t.fees, border: const OutlineInputBorder()),
                    validator: number,
                  ),
                ],
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(t.date),
                  subtitle: Text(Fmt(context).date(_date)),
                  trailing: const Icon(Icons.calendar_today),
                  onTap: () async {
                    final d = await showDatePicker(
                        context: context, initialDate: _date, firstDate: DateTime(1970), lastDate: DateTime.now());
                    if (d != null) setState(() => _date = d);
                  },
                ),
              ],
              const SizedBox(height: 16),
              FilledButton(onPressed: _busy ? null : _save, child: Text(t.save)),
            ]),
          );
        },
      ),
    );
  }
}

class _SymbolSearch extends StatefulWidget {
  const _SymbolSearch();

  @override
  State<_SymbolSearch> createState() => _SymbolSearchState();
}

class _SymbolSearchState extends State<_SymbolSearch> {
  Timer? _debounce;
  List<SymbolMatch> _results = [];
  bool _loading = false;
  Object? _error;

  void _onChanged(String q) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () => _search(q));
  }

  Future<void> _search(String q) async {
    if (q.trim().isEmpty) return;
    setState(() { _loading = true; _error = null; });
    try {
      final r = await context.read<Session>().api.search(q.trim());
      if (mounted) setState(() => _results = r);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.85,
      builder: (context, scroll) => Column(children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: TextField(
            autofocus: true,
            textDirection: TextDirection.ltr,
            decoration: InputDecoration(prefixIcon: const Icon(Icons.search), labelText: t.symbol, helperText: t.symbolHint),
            onChanged: _onChanged,
            onSubmitted: _search,
          ),
        ),
        if (_loading) const LinearProgressIndicator(),
        if (_error != null) Padding(padding: const EdgeInsets.all(16), child: Text(errorText(context, _error!))),
        Expanded(
          child: ListView.builder(
            controller: scroll,
            itemCount: _results.length,
            itemBuilder: (c, i) => ListTile(
              title: Text(_results[i].symbol, textDirection: TextDirection.ltr),
              subtitle: Text(_results[i].description),
              onTap: () => Navigator.pop(context, _results[i]),
            ),
          ),
        ),
      ]),
    );
  }
}
