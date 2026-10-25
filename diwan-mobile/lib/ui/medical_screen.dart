import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

enum _Kind { text, date, integer, bloodType }

/// One field of a medical record category, named as in diwan-medical's MedicalRecordRequest.
class _Field {
  const _Field(this.key, this.label, [this.kind = _Kind.text]);

  final String key;
  final String Function(AppLocalizations) label;
  final _Kind kind;
}

/// A record category: its code on the server, title, icon and fields (the first one is required and is the
/// record's title in the list).
class _Category {
  const _Category(this.code, this.title, this.icon, this.fields);

  final String code;
  final String Function(AppLocalizations) title;
  final IconData icon;
  final List<_Field> fields;
}

const _bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

final List<_Category> _categories = [
  _Category('medications', (t) => t.medMedications, Icons.medication_outlined, [
    _Field('name', (t) => t.name),
    _Field('dosage', (t) => t.fDosage),
    _Field('frequency', (t) => t.fFrequency),
    _Field('startDate', (t) => t.fStartDate, _Kind.date),
    _Field('endDate', (t) => t.fEndDate, _Kind.date),
  ]),
  _Category('allergies', (t) => t.medAllergies, Icons.coronavirus_outlined, [
    _Field('allergen', (t) => t.fAllergen),
    _Field('severity', (t) => t.fSeverity),
    _Field('reaction', (t) => t.fReaction),
  ]),
  _Category('bloodType', (t) => t.medBloodType, Icons.bloodtype_outlined, [
    _Field('bloodType', (t) => t.medBloodType, _Kind.bloodType),
  ]),
  _Category('surgeries', (t) => t.medSurgeries, Icons.local_hospital_outlined, [
    _Field('surgeryName', (t) => t.fSurgery),
    _Field('surgeryDate', (t) => t.date, _Kind.date),
    _Field('hospital', (t) => t.fHospital),
    _Field('surgeon', (t) => t.fSurgeon),
  ]),
  _Category('chronicDiseases', (t) => t.medChronicDiseases, Icons.monitor_heart_outlined, [
    _Field('diseaseName', (t) => t.fDisease),
    _Field('diagnosedAt', (t) => t.fDiagnosedAt, _Kind.date),
    _Field('currentTreatment', (t) => t.fTreatment),
  ]),
  _Category('labResults', (t) => t.medLabResults, Icons.science_outlined, [
    _Field('testName', (t) => t.fTest),
    _Field('result', (t) => t.fResult),
    _Field('unit', (t) => t.fUnit),
    _Field('referenceRange', (t) => t.fReferenceRange),
    _Field('testDate', (t) => t.date, _Kind.date),
  ]),
  _Category('vaccines', (t) => t.medVaccines, Icons.vaccines_outlined, [
    _Field('vaccineName', (t) => t.fVaccine),
    _Field('doseNumber', (t) => t.fDose, _Kind.integer),
    _Field('vaccinationDate', (t) => t.date, _Kind.date),
    _Field('nextDoseDate', (t) => t.fNextDose, _Kind.date),
  ]),
  _Category('familyHistory', (t) => t.medFamilyHistory, Icons.family_restroom_outlined, [
    _Field('condition', (t) => t.fCondition),
    _Field('relation', (t) => t.fRelation),
  ]),
];

/// Medical history (diwan-medical): one tab per record category.
class MedicalScreen extends StatelessWidget {
  const MedicalScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return DefaultTabController(
      length: _categories.length,
      child: Scaffold(
        appBar: AppBar(
          title: Text(t.featureMedical),
          bottom: TabBar(
            isScrollable: true,
            tabAlignment: TabAlignment.start,
            tabs: [for (final c in _categories) Tab(icon: Icon(c.icon), text: c.title(t))],
          ),
        ),
        body: TabBarView(children: [for (final c in _categories) _CategoryList(category: c)]),
      ),
    );
  }
}

class _CategoryList extends StatefulWidget {
  const _CategoryList({required this.category});

  final _Category category;

  @override
  State<_CategoryList> createState() => _CategoryListState();
}

class _CategoryListState extends State<_CategoryList> with AutomaticKeepAliveClientMixin {
  Key _key = UniqueKey();

  @override
  bool get wantKeepAlive => true;

  Future<void> _open([MedicalRecord? record]) async {
    final changed = await Navigator.push<bool>(
        context, MaterialPageRoute(builder: (_) => _RecordForm(category: widget.category, record: record)));
    if (changed == true) setState(() => _key = UniqueKey());
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final c = widget.category;
    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        heroTag: c.code,
        icon: const Icon(Icons.add),
        label: Text(t.newRecord),
        onPressed: _open,
      ),
      body: Loader<List<MedicalRecord>>(
        key: _key,
        load: () => context.read<Session>().api.medicalRecords(category: c.code),
        builder: (context, list, reload) {
          if (list.isEmpty) return EmptyState(icon: c.icon, text: t.noMedicalRecords);
          return ListView.separated(
            padding: const EdgeInsets.only(bottom: 96),
            itemCount: list.length,
            separatorBuilder: (_, _) => const Divider(height: 1),
            itemBuilder: (context, i) {
              final r = list[i];
              final details = [
                for (final field in c.fields.skip(1))
                  if (r.text(field.key) != null) '${field.label(t)}: ${_display(f, field, r.text(field.key)!)}',
              ];
              return ListTile(
                leading: CircleAvatar(child: Icon(c.icon)),
                title: Text(r.text(c.fields.first.key) ?? '-'),
                subtitle: details.isEmpty && r.notes == null
                    ? null
                    : Text([...details, if (r.notes != null && r.notes!.isNotEmpty) r.notes!].join('\n')),
                isThreeLine: details.length + (r.notes == null ? 0 : 1) > 1,
                onTap: () => _open(r),
              );
            },
          );
        },
      ),
    );
  }
}

String _display(Fmt f, _Field field, String value) =>
    field.kind == _Kind.date ? f.date(DateTime.tryParse(value)) : value;

class _RecordForm extends StatefulWidget {
  const _RecordForm({required this.category, this.record});

  final _Category category;
  final MedicalRecord? record;

  @override
  State<_RecordForm> createState() => _RecordFormState();
}

class _RecordFormState extends State<_RecordForm> {
  final _form = GlobalKey<FormState>();
  late final Map<String, TextEditingController> _text = {
    for (final field in widget.category.fields.where((f) => f.kind == _Kind.text || f.kind == _Kind.integer))
      field.key: TextEditingController(text: widget.record?.text(field.key) ?? ''),
  };
  late final Map<String, DateTime?> _dates = {
    for (final field in widget.category.fields.where((f) => f.kind == _Kind.date))
      field.key: DateTime.tryParse(widget.record?.text(field.key) ?? ''),
  };
  late String? _bloodType = widget.record?.text('bloodType');
  late final _notes = TextEditingController(text: widget.record?.notes ?? '');
  bool _busy = false;

  bool get _editing => widget.record != null;

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    final body = <String, Object?>{'category': widget.category.code};
    for (final field in widget.category.fields) {
      body[field.key] = switch (field.kind) {
        _Kind.text => _text[field.key]!.text.trim().isEmpty ? null : _text[field.key]!.text.trim(),
        _Kind.integer => int.tryParse(normalizeNumber(_text[field.key]!.text) ?? ''),
        _Kind.date => _dates[field.key] == null ? null : Fmt.isoDate(_dates[field.key]!),
        _Kind.bloodType => _bloodType,
      };
    }
    body['notes'] = _notes.text.trim().isEmpty ? null : _notes.text.trim();
    try {
      await context.read<Session>().api.saveMedicalRecord(widget.record?.id, body);
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
      await context.read<Session>().api.deleteMedicalRecord(widget.record!.id, widget.category.code);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final fields = widget.category.fields;
    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.category.title(t)} · ${_editing ? t.editRecord : t.newRecord}'),
        actions: [
          if (_editing) IconButton(tooltip: t.delete, icon: const Icon(Icons.delete_outline), onPressed: _delete),
        ],
      ),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          for (final field in fields)
            switch (field.kind) {
              _Kind.bloodType => Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: FormField<String>(
                    initialValue: _bloodType,
                    validator: (_) => _bloodType == null ? t.required : null,
                    builder: (state) => InputDecorator(
                      decoration: InputDecoration(labelText: field.label(t), errorText: state.errorText, border: InputBorder.none),
                      child: Wrap(spacing: 8, runSpacing: 4, children: [
                        for (final b in {..._bloodTypes, ?_bloodType})
                          ChoiceChip(
                            label: Text(b, textDirection: TextDirection.ltr),
                            selected: _bloodType == b,
                            onSelected: (_) => setState(() => _bloodType = b),
                          ),
                      ]),
                    ),
                  ),
                ),
              _Kind.date => ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.event),
                  title: Text(field.label(t)),
                  subtitle: Text(f.date(_dates[field.key])),
                  trailing: _dates[field.key] == null
                      ? null
                      : IconButton(icon: const Icon(Icons.clear), onPressed: () => setState(() => _dates[field.key] = null)),
                  onTap: () async {
                    final d = await showDatePicker(
                        context: context,
                        initialDate: _dates[field.key] ?? DateTime.now(),
                        firstDate: DateTime(1900),
                        lastDate: DateTime(DateTime.now().year + 10));
                    if (d != null) setState(() => _dates[field.key] = d);
                  },
                ),
              _ => TextFormField(
                  controller: _text[field.key],
                  keyboardType: field.kind == _Kind.integer ? TextInputType.number : TextInputType.text,
                  decoration: InputDecoration(labelText: field.label(t)),
                  validator: (v) {
                    final empty = v == null || v.trim().isEmpty;
                    if (field == fields.first && empty) return t.required;
                    if (field.kind == _Kind.integer && !empty && int.tryParse(normalizeNumber(v) ?? '') == null) {
                      return t.invalidNumber;
                    }
                    return null;
                  },
                ),
            },
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
