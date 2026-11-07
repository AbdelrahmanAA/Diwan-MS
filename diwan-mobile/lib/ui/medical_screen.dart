import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../platform/contact_picker.dart';
import '../platform/reminders.dart';
import '../state/session.dart';
import 'format.dart';
import 'medical_suggestions.dart';
import 'widgets.dart';

enum _Kind { text, date, dateTime, integer, bloodType, severity, phone, times, toggle }

/// One field of a medical record category, named as in diwan-medical's MedicalRecordRequest.
class _Field {
  const _Field(this.key, this.label, {this.kind = _Kind.text, this.suggestions, this.presets, this.medicineSearch = false});

  final String key;
  final String Function(AppLocalizations) label;
  final _Kind kind;
  final List<String> Function(bool arabic)? suggestions; // typed-ahead while the field is free text
  final List<String> Function(AppLocalizations)? presets; // one tap fills the field
  final bool medicineSearch; // offers the list of medicines registered in Saudi Arabia
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
const _severities = ['MILD', 'MODERATE', 'SEVERE', 'LIFE_THREATENING'];
const _relations = ['SPOUSE', 'CHILD', 'PARENT', 'SIBLING', 'OTHER'];

String _severityLabel(AppLocalizations t, String? code) => switch (code) {
      'MILD' => t.sevMild,
      'MODERATE' => t.sevModerate,
      'SEVERE' => t.sevSevere,
      'LIFE_THREATENING' => t.sevLifeThreatening,
      _ => code ?? '-',
    };

String _relationLabel(AppLocalizations t, String code) => switch (code) {
      'SELF' => t.relSelf,
      'SPOUSE' => t.relSpouse,
      'CHILD' => t.relChild,
      'PARENT' => t.relParent,
      'SIBLING' => t.relSibling,
      _ => t.relOther,
    };

String _personName(AppLocalizations t, MedicalProfile p) => p.name ?? (p.self ? t.relSelf : '-');

List<String> _familyPresets(AppLocalizations t) => [t.relFather, t.relMother, t.relBrother, t.relSister, t.relGrandparent];

final List<_Category> _categories = [
  _Category('medications', (t) => t.medMedications, Icons.medication_outlined, [
    _Field('name', (t) => t.name, suggestions: medicineSuggestions, medicineSearch: true),
    _Field('dosage', (t) => t.fDosage),
    _Field('frequency', (t) => t.fFrequency,
        presets: (t) => [t.freqOnce, t.freqTwice, t.freqThree, t.freqFour, t.freqAsNeeded]),
    _Field('startDate', (t) => t.fStartDate, kind: _Kind.date),
    _Field('endDate', (t) => t.fEndDate, kind: _Kind.date),
    _Field('times', (t) => t.fDoseTimes, kind: _Kind.times),
    _Field('reminders', (t) => t.fReminders, kind: _Kind.toggle),
  ]),
  _Category('appointments', (t) => t.medAppointments, Icons.event_outlined, [
    _Field('appointmentWith', (t) => t.fAppointmentWith),
    _Field('appointmentAt', (t) => t.fAppointmentAt, kind: _Kind.dateTime),
    _Field('location', (t) => t.fLocation),
  ]),
  _Category('allergies', (t) => t.medAllergies, Icons.coronavirus_outlined, [
    _Field('allergen', (t) => t.fAllergen, suggestions: allergenSuggestions),
    _Field('severity', (t) => t.fSeverity, kind: _Kind.severity),
    _Field('reaction', (t) => t.fReaction),
  ]),
  _Category('bloodType', (t) => t.medBloodType, Icons.bloodtype_outlined, [
    _Field('bloodType', (t) => t.medBloodType, kind: _Kind.bloodType),
  ]),
  _Category('chronicDiseases', (t) => t.medChronicDiseases, Icons.monitor_heart_outlined, [
    _Field('diseaseName', (t) => t.fDisease, suggestions: conditionSuggestions),
    _Field('diagnosedAt', (t) => t.fDiagnosedAt, kind: _Kind.date),
    _Field('currentTreatment', (t) => t.fTreatment),
  ]),
  _Category('surgeries', (t) => t.medSurgeries, Icons.local_hospital_outlined, [
    _Field('surgeryName', (t) => t.fSurgery),
    _Field('surgeryDate', (t) => t.date, kind: _Kind.date),
    _Field('hospital', (t) => t.fHospital),
    _Field('surgeon', (t) => t.fSurgeon),
  ]),
  _Category('labResults', (t) => t.medLabResults, Icons.science_outlined, [
    _Field('testName', (t) => t.fTest),
    _Field('result', (t) => t.fResult),
    _Field('unit', (t) => t.fUnit),
    _Field('referenceRange', (t) => t.fReferenceRange),
    _Field('testDate', (t) => t.date, kind: _Kind.date),
  ]),
  _Category('vaccines', (t) => t.medVaccines, Icons.vaccines_outlined, [
    _Field('vaccineName', (t) => t.fVaccine, suggestions: vaccineSuggestions),
    _Field('doseNumber', (t) => t.fDose, kind: _Kind.integer),
    _Field('vaccinationDate', (t) => t.date, kind: _Kind.date),
    _Field('nextDoseDate', (t) => t.fNextDose, kind: _Kind.date),
  ]),
  _Category('familyHistory', (t) => t.medFamilyHistory, Icons.family_restroom_outlined, [
    _Field('condition', (t) => t.fCondition, suggestions: conditionSuggestions),
    _Field('relation', (t) => t.fRelation, presets: _familyPresets),
  ]),
  _Category('emergencyContacts', (t) => t.medEmergencyContacts, Icons.contact_phone_outlined, [
    _Field('contactName', (t) => t.name),
    _Field('phone', (t) => t.fPhone, kind: _Kind.phone),
    _Field('contactRelation', (t) => t.fRelation, presets: _familyPresets),
  ]),
];

/// [perDay] dose times evenly over 24 hours from the first one ("HH:mm"), e.g. 08:00 three times: 08:00, 16:00, 00:00.
@visibleForTesting
List<String> spreadDoseTimes(int firstMinute, int perDay) => [
      for (var i = 0; i < perDay; i++)
        () {
          final m = (firstMinute + i * 24 * 60 ~/ perDay) % (24 * 60);
          return '${(m ~/ 60).toString().padLeft(2, '0')}:${(m % 60).toString().padLeft(2, '0')}';
        }(),
    ];

_Category _categoryOf(String code) => _categories.firstWhere((c) => c.code == code);

/// What the home page shows: the people of the account and the selected person.
class _Home {
  _Home(this.people, this.overview, {this.doses = const [], this.savedAt});

  final List<MedicalProfile> people;
  final MedicalOverview overview;
  final List<MedicalDose> doses; // answered today
  final DateTime? savedAt; // set when this is the copy kept on the phone
}

/// Replaces the phone's reminders with the server's plan. Failing is not fatal: the last plan stays scheduled.
Future<void> syncMedicalReminders(Session session) async {
  try {
    final plan = await session.api.medicalReminders();
    await Reminders.instance.sync(plan, lookupAppLocalizations(session.locale));
  } catch (_) {}
}

/// Medical history (diwan-medical): one person at a time, the essentials first.
class MedicalScreen extends StatefulWidget {
  const MedicalScreen({super.key});

  @override
  State<MedicalScreen> createState() => _MedicalScreenState();
}

class _MedicalScreenState extends State<MedicalScreen> {
  int? _personId;
  Key _key = UniqueKey();

  void _refresh() => setState(() => _key = UniqueKey());

  /// The emergency card has to work without a connection, so the last overview of each person stays on the phone.
  Future<_Home> _load() async {
    final session = context.read<Session>();
    try {
      final people = await session.api.medicalProfiles();
      final selected = people.where((p) => p.id == _personId).firstOrNull ?? people.first;
      final overview = await session.api.medicalOverview(profileId: selected.id);
      final doses = await session.api.medicalDoses(profileId: selected.id, day: DateTime.now());
      await session.saveMedicalCard(overview);
      unawaited(syncMedicalReminders(session));
      return _Home(people, overview, doses: doses);
    } catch (_) {
      final saved = await session.savedMedicalCards();
      if (saved.isEmpty) rethrow;
      final card = saved.where((c) => c.overview.profile.id == _personId).firstOrNull ?? saved.first;
      return _Home([for (final c in saved) c.overview.profile], card.overview, savedAt: card.savedAt);
    }
  }

  Future<void> _openPerson([MedicalProfile? person]) async {
    final changed = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => _PersonForm(person: person)));
    if (changed == true) _refresh();
  }

  Future<void> _push(Widget page) async {
    await Navigator.push(context, MaterialPageRoute(builder: (_) => page));
    _refresh();
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(t.featureMedical)),
      body: Loader<_Home>(
        key: _key,
        load: _load,
        builder: (context, home, reload) {
          final o = home.overview;
          final offline = home.savedAt != null;
          return RefreshIndicator(
            onRefresh: reload,
            child: ListView(padding: const EdgeInsets.only(bottom: 32), children: [
              if (offline)
                MaterialBanner(
                  content: Text(t.offlineCopy(Fmt(context).dateTime(home.savedAt))),
                  actions: [TextButton(onPressed: reload, child: Text(t.retry))],
                ),
              _PeopleBar(
                people: home.people,
                selected: o.profile,
                onSelect: (p) => setState(() {
                  _personId = p.id;
                  _key = UniqueKey();
                }),
                onAdd: offline ? null : _openPerson,
                onEdit: offline ? null : _openPerson,
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                child: FilledButton.icon(
                  style: FilledButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.errorContainer,
                    foregroundColor: Theme.of(context).colorScheme.onErrorContainer,
                  ),
                  icon: const Icon(Icons.emergency_outlined),
                  label: Text(t.emergencyCard),
                  onPressed: () => Navigator.push(context,
                      MaterialPageRoute(builder: (_) => EmergencyCardScreen(overview: o, savedAt: home.savedAt))),
                ),
              ),
              if (!offline) _TodaysDoses(overview: o, doses: home.doses),
              _Essentials(overview: o),
              if (!offline) ...[
                const Divider(),
                ListTile(
                  leading: const Icon(Icons.history),
                  title: Text(t.medicalTimeline),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => _push(_TimelineScreen(person: o.profile)),
                ),
                for (final c in _categories)
                  ListTile(
                    leading: Icon(c.icon),
                    title: Text(c.title(t)),
                    trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                      Text('${o.counts[c.code] ?? 0}'),
                      const Icon(Icons.chevron_right),
                    ]),
                    onTap: () => _push(_CategoryScreen(category: c, person: o.profile)),
                  ),
              ],
              Padding(
                padding: const EdgeInsets.all(16),
                child: Text(t.medicalDisclaimer, style: Theme.of(context).textTheme.bodySmall),
              ),
            ]),
          );
        },
      ),
    );
  }
}

class _PeopleBar extends StatelessWidget {
  const _PeopleBar({required this.people, required this.selected, required this.onSelect, this.onAdd, this.onEdit});

  final List<MedicalProfile> people;
  final MedicalProfile selected;
  final ValueChanged<MedicalProfile> onSelect;
  final VoidCallback? onAdd;
  final ValueChanged<MedicalProfile>? onEdit;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return SizedBox(
      height: 56,
      child: ListView(scrollDirection: Axis.horizontal, padding: const EdgeInsets.symmetric(horizontal: 12), children: [
        for (final p in people)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
            child: ChoiceChip(
              label: Text(_personName(t, p)),
              selected: p.id == selected.id,
              onSelected: (_) => p.id == selected.id ? onEdit?.call(p) : onSelect(p),
            ),
          ),
        if (onAdd != null)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
            child: ActionChip(avatar: const Icon(Icons.add, size: 18), label: Text(t.addPerson), onPressed: onAdd),
          ),
      ]),
    );
  }
}

/// The short version of a person's record. Shows what was entered, without any judgement of it.
class _Essentials extends StatelessWidget {
  const _Essentials({required this.overview});

  final MedicalOverview overview;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final o = overview;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Wrap(spacing: 24, runSpacing: 12, children: [
          Metric(label: t.medBloodType, child: Text(o.bloodType ?? '-', textDirection: TextDirection.ltr)),
          if (o.profile.birthDate != null) Metric(label: t.birthDate, child: Text(f.date(o.profile.birthDate))),
        ]),
        const SizedBox(height: 12),
        _Summary(title: t.medAllergies, lines: [
          for (final a in o.allergies)
            [a.text('allergen'), if (a.text('severity') != null) _severityLabel(t, a.text('severity'))].join(' · '),
        ]),
        _Summary(title: t.currentMedications, lines: [
          for (final m in o.currentMedications) [m.text('name'), m.text('dosage'), m.text('frequency')].nonNulls.join(' · '),
        ]),
        _Summary(title: t.medChronicDiseases, lines: [for (final c in o.conditions) c.text('diseaseName') ?? '-']),
        _Summary(title: t.upcoming, lines: [
          for (final u in o.upcoming)
            u.category == 'appointments'
                ? '${u.text('appointmentWith')} · ${f.dateTime(DateTime.tryParse(u.text('appointmentAt') ?? ''))}'
                : '${u.text('vaccineName')} · ${f.date(DateTime.tryParse(u.text('nextDoseDate') ?? ''))}',
        ]),
      ]),
    );
  }
}

/// The doses due today and whether they were taken. Only records what the user says; no judgement.
class _TodaysDoses extends StatefulWidget {
  const _TodaysDoses({required this.overview, required this.doses});

  final MedicalOverview overview;
  final List<MedicalDose> doses;

  @override
  State<_TodaysDoses> createState() => _TodaysDosesState();
}

class _TodaysDosesState extends State<_TodaysDoses> {
  /// Answers given on this screen since the last load, by "medicationId@time"
  final Map<String, String> _answered = {};

  @override
  void didUpdateWidget(_TodaysDoses old) {
    super.didUpdateWidget(old);
    if (old.doses != widget.doses) _answered.clear();
  }

  static String _key(MedicalRecord m, DateTime at) => '${m.id}@${at.toIso8601String()}';

  String? _statusOf(MedicalRecord m, DateTime at) =>
      _answered[_key(m, at)] ??
      widget.doses.where((d) => d.medicationId == m.id && d.scheduledAt == at).firstOrNull?.status;

  Future<void> _mark(MedicalRecord m, DateTime at, String status) async {
    try {
      await context.read<Session>().api.recordDose(m.id, at, status);
      if (mounted) setState(() => _answered[_key(m, at)] = status);
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final now = DateTime.now();
    final slots = [
      for (final m in widget.overview.currentMedications)
        for (final time in m.times)
          (m, DateTime(now.year, now.month, now.day, int.parse(time.split(':')[0]), int.parse(time.split(':')[1]))),
    ]..sort((a, b) => a.$2.compareTo(b.$2));
    if (slots.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(t.todaysDoses, style: Theme.of(context).textTheme.titleSmall),
        for (final (m, at) in slots)
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Text(TimeOfDay.fromDateTime(at).format(context)),
            title: Text([m.text('name'), m.text('dosage')].nonNulls.join(' · ')),
            trailing: Wrap(spacing: 4, children: [
              ChoiceChip(
                label: Text(t.doseTaken),
                selected: _statusOf(m, at) == 'TAKEN',
                onSelected: (_) => _mark(m, at, 'TAKEN'),
              ),
              ChoiceChip(
                label: Text(t.doseSkipped),
                selected: _statusOf(m, at) == 'SKIPPED',
                onSelected: (_) => _mark(m, at, 'SKIPPED'),
              ),
            ]),
          ),
      ]),
    );
  }
}

class _Summary extends StatelessWidget {
  const _Summary({required this.title, required this.lines});

  final String title;
  final List<String> lines;

  @override
  Widget build(BuildContext context) {
    if (lines.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: Theme.of(context).textTheme.titleSmall),
        const SizedBox(height: 4),
        for (final l in lines) Text(l),
      ]),
    );
  }
}

/// For a doctor or paramedic: large, plain and available without a connection.
class EmergencyCardScreen extends StatelessWidget {
  const EmergencyCardScreen({super.key, required this.overview, this.savedAt});

  final MedicalOverview overview;
  final DateTime? savedAt;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final o = overview;
    final big = Theme.of(context).textTheme.headlineMedium;
    return Scaffold(
      appBar: AppBar(title: Text(t.emergencyCard)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        Text(_personName(t, o.profile), style: big),
        if (o.profile.birthDate != null) Text('${t.birthDate}: ${f.date(o.profile.birthDate)}'),
        const SizedBox(height: 16),
        Text(t.medBloodType, style: Theme.of(context).textTheme.titleMedium),
        Text(o.bloodType ?? '-', style: Theme.of(context).textTheme.displayMedium, textDirection: TextDirection.ltr),
        const Divider(height: 32),
        _CardSection(title: t.medAllergies, empty: t.noneRecorded, lines: [
          for (final a in o.allergies)
            [a.text('allergen'), if (a.text('severity') != null) _severityLabel(t, a.text('severity')), a.text('reaction')]
                .nonNulls.join(' · '),
        ]),
        _CardSection(title: t.currentMedications, empty: t.noneRecorded, lines: [
          for (final m in o.currentMedications) [m.text('name'), m.text('dosage'), m.text('frequency')].nonNulls.join(' · '),
        ]),
        _CardSection(title: t.medChronicDiseases, empty: t.noneRecorded,
            lines: [for (final c in o.conditions) c.text('diseaseName') ?? '-']),
        Text(t.medEmergencyContacts, style: Theme.of(context).textTheme.titleMedium),
        if (o.emergencyContacts.isEmpty) Text(t.noneRecorded),
        for (final c in o.emergencyContacts)
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(c.text('contactName') ?? '-'),
            subtitle: Text([c.text('contactRelation'), c.text('phone')].nonNulls.join(' · '), textDirection: TextDirection.ltr),
            trailing: IconButton(
              tooltip: t.call,
              icon: const Icon(Icons.call),
              onPressed: () => launchUrl(Uri(scheme: 'tel', path: c.text('phone')!.replaceAll(' ', ''))),
            ),
          ),
        if (savedAt != null) ...[
          const SizedBox(height: 16),
          Text(t.offlineCopy(f.dateTime(savedAt)), style: Theme.of(context).textTheme.bodySmall),
        ],
      ]),
    );
  }
}

class _CardSection extends StatelessWidget {
  const _CardSection({required this.title, required this.empty, required this.lines});

  final String title;
  final String empty;
  final List<String> lines;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 4),
        if (lines.isEmpty) Text(empty),
        for (final l in lines) Padding(padding: const EdgeInsets.only(bottom: 2), child: Text(l, style: Theme.of(context).textTheme.bodyLarge)),
      ]),
    );
  }
}

/// A person's history, newest first, with search and a filter per category.
class _TimelineScreen extends StatefulWidget {
  const _TimelineScreen({required this.person});

  final MedicalProfile person;

  @override
  State<_TimelineScreen> createState() => _TimelineScreenState();
}

class _TimelineScreenState extends State<_TimelineScreen> {
  static const _pageSize = 50;

  final _search = TextEditingController();
  final List<MedicalRecord> _items = [];
  String? _category;
  int _page = 0;
  int _total = 0;
  bool _busy = true;
  Object? _error;

  @override
  void initState() {
    super.initState();
    _load(reset: true);
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<void> _load({bool reset = false}) async {
    if (reset) _page = 0;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final result = await context.read<Session>().api.medicalTimeline(
          profileId: widget.person.id, category: _category, q: _search.text.trim(), page: _page, size: _pageSize);
      if (!mounted) return;
      setState(() {
        if (reset) _items.clear();
        _items.addAll(result.items);
        _total = result.totalItems;
        _page++;
      });
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _open(MedicalRecord r) async {
    final changed = await Navigator.push<bool>(context, MaterialPageRoute(
        builder: (_) => _RecordForm(category: _categoryOf(r.category), person: widget.person, record: r)));
    if (changed == true) _load(reset: true);
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    return Scaffold(
      appBar: AppBar(title: Text(t.medicalTimeline)),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
          child: TextField(
            controller: _search,
            textInputAction: TextInputAction.search,
            decoration: InputDecoration(prefixIcon: const Icon(Icons.search), labelText: t.search),
            onSubmitted: (_) => _load(reset: true),
          ),
        ),
        SizedBox(
          height: 52,
          child: ListView(scrollDirection: Axis.horizontal, padding: const EdgeInsets.symmetric(horizontal: 12), children: [
            for (final c in _categories.where((c) => c.code != 'emergencyContacts' && c.code != 'bloodType' && c.code != 'familyHistory'))
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 6),
                child: FilterChip(
                  label: Text(c.title(t)),
                  selected: _category == c.code,
                  onSelected: (on) {
                    _category = on ? c.code : null;
                    _load(reset: true);
                  },
                ),
              ),
          ]),
        ),
        if (_busy) const LinearProgressIndicator(),
        Expanded(
          child: _error != null
              ? ErrorView(message: errorText(context, _error!), onRetry: () => _load(reset: true))
              : !_busy && _items.isEmpty
                  ? EmptyState(icon: Icons.history, text: t.noMedicalRecords)
                  : ListView(children: [
                      for (final r in _items)
                        ListTile(
                          leading: CircleAvatar(child: Icon(_categoryOf(r.category).icon)),
                          title: Text(r.title ?? '-'),
                          subtitle: Text('${_categoryOf(r.category).title(t)} · ${f.date(r.eventDate)}'),
                          onTap: () => _open(r),
                        ),
                      if (_items.length < _total)
                        Padding(
                          padding: const EdgeInsets.all(16),
                          child: OutlinedButton(onPressed: _busy ? null : _load, child: Text(t.loadMore)),
                        ),
                    ]),
        ),
      ]),
    );
  }
}

class _CategoryScreen extends StatefulWidget {
  const _CategoryScreen({required this.category, required this.person});

  final _Category category;
  final MedicalProfile person;

  @override
  State<_CategoryScreen> createState() => _CategoryScreenState();
}

class _CategoryScreenState extends State<_CategoryScreen> {
  Key _key = UniqueKey();

  Future<void> _open([MedicalRecord? record]) async {
    final changed = await Navigator.push<bool>(context, MaterialPageRoute(
        builder: (_) => _RecordForm(category: widget.category, person: widget.person, record: record)));
    if (changed == true) setState(() => _key = UniqueKey());
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final c = widget.category;
    return Scaffold(
      appBar: AppBar(title: Text('${c.title(t)} · ${_personName(t, widget.person)}')),
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add),
        label: Text(t.newRecord),
        onPressed: _open,
      ),
      body: Loader<List<MedicalRecord>>(
        key: _key,
        load: () => context.read<Session>().api.medicalRecords(category: c.code, profileId: widget.person.id),
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
                  if (r.text(field.key) != null && !(field.kind == _Kind.toggle && r.text(field.key) == 'false'))
                    '${field.label(t)}: ${_display(t, f, field, r.text(field.key)!)}',
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

String _display(AppLocalizations t, Fmt f, _Field field, String value) => switch (field.kind) {
      _Kind.date => f.date(DateTime.tryParse(value)),
      _Kind.dateTime => f.dateTime(DateTime.tryParse(value)),
      _Kind.times => value.replaceAll(RegExp(r'[\[\]]'), ''),
      _Kind.toggle => value == 'true' ? '✓' : '-',
      _Kind.severity => _severityLabel(t, value),
      _ => value,
    };

class _RecordForm extends StatefulWidget {
  const _RecordForm({required this.category, required this.person, this.record});

  final _Category category;
  final MedicalProfile person;
  final MedicalRecord? record;

  @override
  State<_RecordForm> createState() => _RecordFormState();
}

class _RecordFormState extends State<_RecordForm> {
  static const _textKinds = {_Kind.text, _Kind.integer, _Kind.phone};

  final _form = GlobalKey<FormState>();
  late final Map<String, TextEditingController> _text = {
    for (final field in widget.category.fields.where((f) => _textKinds.contains(f.kind)))
      field.key: TextEditingController(text: widget.record?.text(field.key) ?? ''),
  };
  late final Map<String, DateTime?> _dates = {
    for (final field in widget.category.fields.where((f) => f.kind == _Kind.date || f.kind == _Kind.dateTime))
      field.key: DateTime.tryParse(widget.record?.text(field.key) ?? ''),
  };
  late final List<String> _times = [...?widget.record?.times];
  late bool _reminders = widget.record?.reminders ?? false;
  late String? _bloodType = widget.record?.text('bloodType');
  late String? _severity = widget.record?.text('severity');
  late final _notes = TextEditingController(text: widget.record?.notes ?? '');
  bool _busy = false;

  bool get _editing => widget.record != null;

  @override
  void dispose() {
    for (final c in _text.values) {
      c.dispose();
    }
    _notes.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    final body = <String, Object?>{'category': widget.category.code, 'profileId': widget.person.id};
    for (final field in widget.category.fields) {
      body[field.key] = switch (field.kind) {
        _Kind.text || _Kind.phone => _text[field.key]!.text.trim().isEmpty ? null : _text[field.key]!.text.trim(),
        _Kind.integer => int.tryParse(normalizeNumber(_text[field.key]!.text) ?? ''),
        _Kind.date => _dates[field.key] == null ? null : Fmt.isoDate(_dates[field.key]!),
        _Kind.dateTime => _dates[field.key]?.toIso8601String().substring(0, 16),
        _Kind.times => _times,
        _Kind.toggle => _reminders && _times.isNotEmpty,
        _Kind.bloodType => _bloodType,
        _Kind.severity => _severity,
      };
    }
    body['notes'] = _notes.text.trim().isEmpty ? null : _notes.text.trim();
    try {
      final session = context.read<Session>();
      await session.api.saveMedicalRecord(widget.record?.id, body);
      unawaited(syncMedicalReminders(session));
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  /// Turning reminders on asks for notification permission once, and says so when it is refused.
  Future<void> _setReminders(bool on) async {
    setState(() => _reminders = on);
    if (!on) return;
    final allowed = await Reminders.instance.requestPermission().catchError((_) => false);
    if (!allowed && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(AppLocalizations.of(context).notificationsOff)));
    }
  }

  Future<void> _addTime() async {
    final picked = await showTimePicker(context: context, initialTime: const TimeOfDay(hour: 8, minute: 0));
    if (picked == null) return;
    final value = '${picked.hour.toString().padLeft(2, '0')}:${picked.minute.toString().padLeft(2, '0')}';
    setState(() {
      if (!_times.contains(value)) _times.add(value);
      _times.sort();
    });
  }

  Future<void> _delete() async {
    final t = AppLocalizations.of(context);
    if (!await confirm(context, t.confirmDelete) || !mounted) return;
    try {
      final session = context.read<Session>();
      await session.api.deleteMedicalRecord(widget.record!.id, widget.category.code);
      unawaited(syncMedicalReminders(session));
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  /// Picks from the medicines registered in Saudi Arabia; fills the name, and the dosage when still empty.
  Future<void> _pickMedicine() async {
    final m = await showModalBottomSheet<Medicine>(
        context: context, isScrollControlled: true, builder: (_) => const _MedicineSearch());
    if (m == null || !mounted) return;
    setState(() {
      _text['name']!.text = m.trade;
      final dosage = _text['dosage'];
      if (dosage != null && dosage.text.trim().isEmpty) dosage.text = [m.strength, m.form].where((s) => s.isNotEmpty).join(' ');
    });
  }

  /// Fills the name (when still empty) and the number from the phone's contact picker.
  Future<void> _pickContact() async {
    try {
      final c = await ContactPicker.pickPhone();
      if (c == null || !mounted) return;
      setState(() {
        final name = _text['contactName'];
        if (name != null && name.text.trim().isEmpty && c.name != null) name.text = c.name!;
        _text['phone']!.text = c.phone;
      });
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  String? _validate(_Field field, String? v) {
    final t = AppLocalizations.of(context);
    final empty = v == null || v.trim().isEmpty;
    final required = field == widget.category.fields.first || field.kind == _Kind.phone;
    if (required && empty) return t.required;
    if (field.kind == _Kind.integer && !empty && int.tryParse(normalizeNumber(v) ?? '') == null) return t.invalidNumber;
    if (field.kind == _Kind.phone && !empty && !RegExp(r'^[+0-9 ()-]{5,20}$').hasMatch(v.trim())) return t.invalidPhone;
    return null;
  }

  Widget _textField(_Field field) {
    final t = AppLocalizations.of(context);
    final controller = _text[field.key]!;
    final keyboard = switch (field.kind) {
      _Kind.integer => TextInputType.number,
      _Kind.phone => TextInputType.phone,
      _ => TextInputType.text,
    };
    final decoration = InputDecoration(labelText: field.label(t));
    final input = field.suggestions == null
        ? TextFormField(
            controller: controller,
            keyboardType: keyboard,
            textDirection: field.kind == _Kind.phone ? TextDirection.ltr : null,
            decoration: decoration,
            validator: (v) => _validate(field, v),
          )
        : _SuggestField(
            controller: controller,
            decoration: decoration,
            options: field.suggestions!(Localizations.localeOf(context).languageCode == 'ar'),
            validator: (v) => _validate(field, v),
          );
    if (field.presets == null) return input;
    final perDay = field.key == 'frequency' ? [1, 2, 3, 4, null] : null; // doses a day of each frequency preset
    final presets = field.presets!(t);
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      input,
      Wrap(spacing: 8, children: [
        for (var i = 0; i < presets.length; i++)
          ActionChip(
            label: Text(presets[i]),
            onPressed: () async {
              setState(() => controller.text = presets[i]);
              final n = perDay?[i];
              if (n != null) await _spreadTimes(n);
            },
          ),
      ]),
    ]);
  }

  /// Asks for the first dose and suggests the others evenly over the day (every 24 / [perDay] hours).
  /// The user can change or remove any of them afterwards.
  Future<void> _spreadTimes(int perDay) async {
    final t = AppLocalizations.of(context);
    final first = _times.isEmpty ? const TimeOfDay(hour: 8, minute: 0) : _timeOf(_times.first);
    final picked = await showTimePicker(context: context, initialTime: first, helpText: t.firstDoseTime);
    if (picked == null || !mounted) return;
    setState(() {
      _times
        ..clear()
        ..addAll(spreadDoseTimes(picked.hour * 60 + picked.minute, perDay))
        ..sort();
    });
  }

  static TimeOfDay _timeOf(String hhmm) =>
      TimeOfDay(hour: int.parse(hhmm.split(':')[0]), minute: int.parse(hhmm.split(':')[1]));

  Future<void> _editTime(String old) async {
    final picked = await showTimePicker(context: context, initialTime: _timeOf(old));
    if (picked == null || !mounted) return;
    final value = '${picked.hour.toString().padLeft(2, '0')}:${picked.minute.toString().padLeft(2, '0')}';
    setState(() {
      _times.remove(old);
      if (!_times.contains(value)) _times.add(value);
      _times.sort();
    });
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
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
          if (widget.category.fields.any((f) => f.medicineSearch))
            Align(
              alignment: AlignmentDirectional.centerStart,
              child: OutlinedButton.icon(
                icon: const Icon(Icons.search),
                label: Text(t.searchMedicines),
                onPressed: _pickMedicine,
              ),
            ),
          if (ContactPicker.available && widget.category.fields.any((f) => f.kind == _Kind.phone))
            Align(
              alignment: AlignmentDirectional.centerStart,
              child: OutlinedButton.icon(
                icon: const Icon(Icons.contacts_outlined),
                label: Text(t.pickFromContacts),
                onPressed: _pickContact,
              ),
            ),
          for (final field in widget.category.fields)
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
              _Kind.severity => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: DropdownButtonFormField<String>(
                    initialValue: _severity,
                    isExpanded: true,
                    decoration: InputDecoration(labelText: field.label(t)),
                    items: [for (final s in _severities) DropdownMenuItem(value: s, child: Text(_severityLabel(t, s)))],
                    onChanged: (v) => setState(() => _severity = v),
                  ),
                ),
              _Kind.times => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: InputDecorator(
                    decoration: InputDecoration(labelText: field.label(t), border: InputBorder.none),
                    child: Wrap(spacing: 8, runSpacing: 4, children: [
                      for (final time in _times)
                        InputChip(
                          label: Text(time, textDirection: TextDirection.ltr),
                          onPressed: () => _editTime(time),
                          onDeleted: () => setState(() => _times.remove(time)),
                        ),
                      if (_times.length < 8)
                        ActionChip(avatar: const Icon(Icons.add_alarm, size: 18), label: Text(t.addTime), onPressed: _addTime),
                    ]),
                  ),
                ),
              _Kind.toggle => SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(field.label(t)),
                  value: _reminders && _times.isNotEmpty,
                  onChanged: _times.isEmpty ? null : _setReminders,
                ),
              _Kind.dateTime => FormField<DateTime>(
                  initialValue: _dates[field.key],
                  validator: (_) => _dates[field.key] == null ? t.required : null,
                  builder: (state) => ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.schedule),
                    title: Text(field.label(t)),
                    subtitle: Text(state.errorText ?? f.dateTime(_dates[field.key]),
                        style: state.errorText == null ? null : TextStyle(color: Theme.of(context).colorScheme.error)),
                    onTap: () async {
                      final current = _dates[field.key] ?? DateTime.now();
                      final day = await showDatePicker(
                          context: context, initialDate: current, firstDate: DateTime(1900), lastDate: DateTime(DateTime.now().year + 5));
                      if (day == null || !context.mounted) return;
                      final time = await showTimePicker(context: context, initialTime: TimeOfDay.fromDateTime(current));
                      if (time == null) return;
                      setState(() => _dates[field.key] = DateTime(day.year, day.month, day.day, time.hour, time.minute));
                      state.didChange(_dates[field.key]);
                    },
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
              _ => _textField(field),
            },
          TextFormField(controller: _notes, maxLines: 3, minLines: 1, maxLength: 4000, decoration: InputDecoration(labelText: t.notes)),
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

/// Search in the medicines registered in Saudi Arabia (SFDA list), by trade or scientific name.
class _MedicineSearch extends StatefulWidget {
  const _MedicineSearch();

  @override
  State<_MedicineSearch> createState() => _MedicineSearchState();
}

class _MedicineSearchState extends State<_MedicineSearch> {
  Timer? _debounce;
  List<Medicine> _results = [];
  String _searched = '';
  bool _loading = false;
  Object? _error;

  void _onChanged(String q) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 350), () => _search(q));
  }

  Future<void> _search(String q) async {
    final query = q.trim();
    if (query.length < 2) {
      setState(() {
        _results = [];
        _searched = '';
      });
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final r = await context.read<Session>().api.searchMedicines(query);
      if (mounted && query == q.trim()) {
        setState(() {
          _results = r;
          _searched = query;
        });
      }
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
            decoration: InputDecoration(prefixIcon: const Icon(Icons.search), labelText: t.searchMedicines, helperText: t.medicineHint),
            onChanged: _onChanged,
            onSubmitted: _search,
          ),
        ),
        if (_loading) const LinearProgressIndicator(),
        if (_error != null) Padding(padding: const EdgeInsets.all(16), child: Text(errorText(context, _error!))),
        if (_error == null && !_loading && _searched.isNotEmpty && _results.isEmpty)
          Padding(padding: const EdgeInsets.all(16), child: Text(t.noMedicineFound)),
        Expanded(
          child: ListView(controller: scroll, children: [
            for (final m in _results)
              ListTile(
                title: Text(m.trade, textDirection: TextDirection.ltr),
                subtitle: Text([m.scientific, m.strength, m.form].where((s) => s.isNotEmpty).join(' · '),
                    textDirection: TextDirection.ltr),
                onTap: () => Navigator.pop(context, m),
              ),
            if (_results.isNotEmpty)
              Padding(
                padding: const EdgeInsets.all(16),
                child: Text(t.medicineSource, style: Theme.of(context).textTheme.bodySmall),
              ),
          ]),
        ),
      ]),
    );
  }
}

/// Free text with a list of common values while typing; anything else can still be entered.
class _SuggestField extends StatefulWidget {
  const _SuggestField({required this.controller, required this.decoration, required this.options, required this.validator});

  final TextEditingController controller;
  final InputDecoration decoration;
  final List<String> options;
  final FormFieldValidator<String> validator;

  @override
  State<_SuggestField> createState() => _SuggestFieldState();
}

class _SuggestFieldState extends State<_SuggestField> {
  final _focus = FocusNode();

  @override
  void dispose() {
    _focus.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return RawAutocomplete<String>(
      textEditingController: widget.controller,
      focusNode: _focus,
      optionsBuilder: (value) {
        final q = value.text.trim().toLowerCase();
        return q.isEmpty ? const <String>[] : widget.options.where((o) => o.toLowerCase().contains(q));
      },
      fieldViewBuilder: (context, controller, focus, onSubmit) => TextFormField(
        controller: controller,
        focusNode: focus,
        decoration: widget.decoration,
        validator: widget.validator,
      ),
      optionsViewBuilder: (context, onSelected, options) => Align(
        alignment: AlignmentDirectional.topStart,
        child: Material(
          elevation: 4,
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxHeight: 220, maxWidth: 320),
            child: ListView(shrinkWrap: true, padding: EdgeInsets.zero, children: [
              for (final o in options) ListTile(dense: true, title: Text(o), onTap: () => onSelected(o)),
            ]),
          ),
        ),
      ),
    );
  }
}

/// Add or change a person. The account holder always stays; a family member can be removed with their records.
class _PersonForm extends StatefulWidget {
  const _PersonForm({this.person});

  final MedicalProfile? person;

  @override
  State<_PersonForm> createState() => _PersonFormState();
}

class _PersonFormState extends State<_PersonForm> {
  final _form = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.person?.name ?? '');
  late String _relation = widget.person?.relation ?? 'CHILD';
  late DateTime? _birthDate = widget.person?.birthDate;
  bool _busy = false;

  bool get _self => widget.person?.self ?? false;

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      await context.read<Session>().api.saveMedicalProfile(widget.person?.id, {
        'name': _name.text.trim(),
        'relation': _relation,
        'birthDate': _birthDate == null ? null : Fmt.isoDate(_birthDate!),
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
    if (!await confirm(context, t.confirmDeletePerson) || !mounted) return;
    final session = context.read<Session>();
    try {
      await session.api.deleteMedicalProfile(widget.person!.id);
      await session.forgetMedicalCard(widget.person!.id);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.person == null ? t.addPerson : t.editPerson),
        actions: [
          if (widget.person != null && !_self) IconButton(tooltip: t.delete, icon: const Icon(Icons.delete_outline), onPressed: _delete),
        ],
      ),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          TextFormField(
            controller: _name,
            maxLength: 120,
            decoration: InputDecoration(labelText: t.name),
            validator: (v) => v == null || v.trim().isEmpty ? t.required : null,
          ),
          if (!_self)
            DropdownButtonFormField<String>(
              initialValue: _relation,
              isExpanded: true,
              decoration: InputDecoration(labelText: t.fRelation),
              items: [for (final r in _relations) DropdownMenuItem(value: r, child: Text(_relationLabel(t, r)))],
              onChanged: (v) => setState(() => _relation = v!),
            ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.cake_outlined),
            title: Text(t.birthDate),
            subtitle: Text(f.date(_birthDate)),
            trailing: _birthDate == null ? null : IconButton(icon: const Icon(Icons.clear), onPressed: () => setState(() => _birthDate = null)),
            onTap: () async {
              final d = await showDatePicker(
                  context: context, initialDate: _birthDate ?? DateTime(2000), firstDate: DateTime(1900), lastDate: DateTime.now());
              if (d != null) setState(() => _birthDate = d);
            },
          ),
          const SizedBox(height: 24),
          FilledButton(onPressed: _busy ? null : _save, child: Text(t.save)),
        ]),
      ),
    );
  }
}
