import 'dart:convert';

import 'package:diwan_mobile/l10n/app_localizations.dart';
import 'package:diwan_mobile/api/models.dart';
import 'package:diwan_mobile/l10n/app_localizations.dart' show AppLocalizations;
import 'package:diwan_mobile/platform/contact_picker.dart';
import 'package:diwan_mobile/platform/reminders.dart';
import 'package:diwan_mobile/state/session.dart';
import 'package:diwan_mobile/ui/medical_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

const _me = {'id': 1, 'name': null, 'relation': 'SELF', 'birthDate': null, 'self': true};
const _sara = {'id': 2, 'name': 'Sara', 'relation': 'CHILD', 'birthDate': '2015-03-01', 'self': false};

Map<String, Object?> _overview(Map<String, Object?> person, {String? bloodType, List<Object> allergies = const []}) => {
      'profile': person,
      'bloodType': bloodType,
      'allergies': allergies,
      'currentMedications': [
        {'id': 5, 'category': 'medications', 'name': 'Metformin', 'dosage': '500mg', 'times': ['08:00', '20:00']},
      ],
      'conditions': [],
      'emergencyContacts': [
        {'id': 6, 'category': 'emergencyContacts', 'contactName': 'Omar', 'phone': '+966500000000'},
      ],
      'upcoming': [],
      'counts': {'medications': 1, 'allergies': allergies.length},
    };

class _FakeReminders implements Reminders {
  final synced = <ReminderPlan>[];
  bool permission = true;
  int asked = 0;

  @override
  Future<bool> requestPermission() async {
    asked++;
    return permission;
  }

  @override
  Future<void> sync(ReminderPlan plan, AppLocalizations t) async => synced.add(plan);

  @override
  Future<void> cancelAll() async {}
}

class _Server {
  bool down = false;
  final saved = <Map<String, dynamic>>[];
  final doses = <Map<String, dynamic>>[];
  final gets = <String>[];

  late final client = MockClient((request) async {
    if (down) throw http.ClientException('offline');
    final path = request.url.path;
    if (path.endsWith('/profiles')) return _json([_me, _sara]);
    if (path.endsWith('/overview')) {
      return _json(request.url.queryParameters['profileId'] == '2'
          ? _overview(_sara, bloodType: 'A+')
          : _overview(_me, bloodType: 'O-', allergies: [
              {'id': 7, 'category': 'allergies', 'allergen': 'Penicillin', 'severity': 'LIFE_THREATENING'},
            ]));
    }
    if (path.endsWith('/medicines')) {
      return _json({
        'items': request.url.queryParameters['q'] == 'pan'
            ? [{'trade': 'PANADOL', 'scientific': 'PARACETAMOL', 'strength': '500', 'form': 'Tablet'}]
            : [],
        'source': 'SFDA registered drugs list',
      });
    }
    if (path.endsWith('/reminders')) {
      return _json({
        'medications': [
          {'id': 5, 'profileId': 1, 'self': true, 'name': 'Metformin', 'times': ['08:00']},
        ],
        'appointments': [],
      });
    }
    if (path.endsWith('/doses') && request.method == 'POST') {
      doses.add(jsonDecode(request.body) as Map<String, dynamic>);
      return _json({});
    }
    if (path.endsWith('/doses')) {
      gets.add(request.url.toString());
      return _json(doses);
    }
    if (path.endsWith('/timeline')) {
      return _json({
        'items': [
          {'id': 9, 'category': 'labResults', 'title': 'HbA1c', 'eventDate': '2024-01-15', 'testName': 'HbA1c'},
        ],
        'page': 0, 'size': 50, 'totalItems': 1, 'totalPages': 1,
      });
    }
    if (request.method == 'POST') {
      saved.add(jsonDecode(request.body) as Map<String, dynamic>);
      return _json({});
    }
    return _json([]);
  });

  static http.Response _json(Object body) => http.Response.bytes(utf8.encode(jsonEncode(body)), 200,
      headers: {'content-type': 'application/json; charset=utf-8'});
}

Future<void> _open(WidgetTester tester, _Server server) async {
  tester.view.physicalSize = const Size(900, 2400);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(ChangeNotifierProvider(
    create: (_) => Session(httpClient: server.client),
    child: const MaterialApp(
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: MedicalScreen(),
    ),
  ));
  await tester.pumpAndSettle();
}

void main() {
  late _FakeReminders reminders;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    reminders = _FakeReminders();
    Reminders.instance = reminders;
  });

  testWidgets('the overview shows the essentials of the account holder', (tester) async {
    await _open(tester, _Server());
    expect(find.text('O-'), findsOneWidget);
    expect(find.text('Penicillin · Life-threatening'), findsOneWidget);
    expect(find.text('Metformin · 500mg'), findsWidgets); // today's doses and current medications
    expect(find.textContaining('not a medical device'), findsOneWidget);
  });

  testWidgets('choosing a family member switches to their records', (tester) async {
    await _open(tester, _Server());
    await tester.tap(find.widgetWithText(ChoiceChip, 'Sara'));
    await tester.pumpAndSettle();
    expect(find.text('A+'), findsOneWidget);
    expect(find.text('O-'), findsNothing);
  });

  testWidgets('the emergency card shows what a paramedic needs', (tester) async {
    await _open(tester, _Server());
    await tester.tap(find.text('Emergency card'));
    await tester.pumpAndSettle();
    expect(find.text('O-'), findsOneWidget);
    expect(find.textContaining('Penicillin'), findsOneWidget);
    expect(find.text('Omar'), findsOneWidget);
    expect(find.byIcon(Icons.call), findsOneWidget);
  });

  testWidgets('without a connection the saved card is still there', (tester) async {
    final server = _Server();
    await _open(tester, server);
    server.down = true;
    await tester.pumpWidget(const SizedBox());
    await _open(tester, server);

    expect(find.textContaining('Saved copy'), findsOneWidget);
    await tester.tap(find.text('Emergency card'));
    await tester.pumpAndSettle();
    expect(find.text('O-'), findsOneWidget);
    expect(find.text('Omar'), findsOneWidget);
  });

  testWidgets('a new allergy is saved for the person on screen with a severity code', (tester) async {
    final server = _Server();
    await _open(tester, server);
    await tester.tap(find.widgetWithText(ChoiceChip, 'Sara'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Allergies').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextFormField).first, 'Peanuts');
    await tester.pumpAndSettle();
    await tester.tap(find.byType(DropdownButtonFormField<String>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Severe').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    expect(server.saved.single['category'], 'allergies');
    expect(server.saved.single['profileId'], 2);
    expect(server.saved.single['severity'], 'SEVERE');
    expect(server.saved.single['allergen'], 'Peanuts');
  });

  testWidgets('a contact needs a valid phone number', (tester) async {
    final server = _Server();
    await _open(tester, server);
    await tester.tap(find.text('Emergency contacts'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField).at(0), 'Omar');
    await tester.enterText(find.byType(TextFormField).at(1), 'call me');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();
    expect(find.text('Enter a valid phone number'), findsOneWidget);
    expect(server.saved, isEmpty);
  });

  testWidgets('a contact can be chosen from the phone and is saved with a clean number', (tester) async {
    ContactPicker.availableOverride = true;
    addTearDown(() => ContactPicker.availableOverride = null);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('diwan/contacts'),
        (call) async => {'name': 'Omar Ali', 'phone': '\u202A+966 50-123.4567\u202C'});
    addTearDown(() => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(const MethodChannel('diwan/contacts'), null));
    final server = _Server();
    await _open(tester, server);
    await tester.tap(find.text('Emergency contacts'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Choose from contacts'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    expect(server.saved.single['contactName'], 'Omar Ali');
    expect(server.saved.single['phone'], '+966 50-1234567');
  });

  testWidgets('without a picker on the device the button is not shown', (tester) async {
    ContactPicker.availableOverride = false;
    addTearDown(() => ContactPicker.availableOverride = null);
    await _open(tester, _Server());
    await tester.tap(find.text('Emergency contacts'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();
    expect(find.text('Choose from contacts'), findsNothing);
  });

  testWidgets('opening the page schedules the reminders of the plan', (tester) async {
    await _open(tester, _Server());
    expect(reminders.synced, isNotEmpty);
    expect(reminders.synced.last.medications.single.name, 'Metformin');
  });

  testWidgets("today's doses can be marked as taken", (tester) async {
    final server = _Server();
    await _open(tester, server);
    expect(find.text("Today's doses"), findsOneWidget);
    await tester.tap(find.widgetWithText(ChoiceChip, 'Taken').first);
    await tester.pumpAndSettle();

    final now = DateTime.now();
    expect(server.doses.single['medicationId'], 5);
    expect(server.doses.single['status'], 'TAKEN');
    expect(server.doses.single['scheduledAt'], '${now.year.toString().padLeft(4, '0')}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}T08:00');
    expect(tester.widget<ChoiceChip>(find.widgetWithText(ChoiceChip, 'Taken').first).selected, isTrue);
  });

  testWidgets('dose times and reminders are saved with a medication', (tester) async {
    final server = _Server();
    await _open(tester, server);
    await tester.tap(find.text('Medications'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField).first, 'Vitamin D');
    await tester.pumpAndSettle();

    expect(tester.widget<SwitchListTile>(find.byType(SwitchListTile)).onChanged, isNull); // no time yet
    await tester.tap(find.text('Add a time'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(SwitchListTile));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    expect(reminders.asked, 1);
    expect(server.saved.single['times'], ['08:00']);
    expect(server.saved.single['reminders'], true);
  });

  testWidgets('a medicine is picked from the Saudi list and fills name and dosage', (tester) async {
    final server = _Server();
    await _open(tester, server);
    await tester.tap(find.text('Medications'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Search Saudi registered medicines'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, 'pan');
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pumpAndSettle();
    expect(find.textContaining('SFDA'), findsOneWidget);
    await tester.tap(find.text('PANADOL'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    expect(server.saved.single['name'], 'PANADOL');
    expect(server.saved.single['dosage'], '500 Tablet');
  });

  testWidgets('a medicine not on the list says so and can still be typed', (tester) async {
    await _open(tester, _Server());
    await tester.tap(find.text('Medications'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Search Saudi registered medicines'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, 'zzz');
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pumpAndSettle();
    expect(find.textContaining('Not on the list'), findsOneWidget);
  });

  test('dose times are spread evenly over the day', () {
    expect(spreadDoseTimes(8 * 60, 1), ['08:00']);
    expect(spreadDoseTimes(8 * 60, 2), ['08:00', '20:00']);
    expect(spreadDoseTimes(8 * 60, 3), ['08:00', '16:00', '00:00']);
    expect(spreadDoseTimes(7 * 60 + 30, 4), ['07:30', '13:30', '19:30', '01:30']);
  });

  testWidgets('choosing three times a day suggests a dose every 8 hours', (tester) async {
    final server = _Server();
    await _open(tester, server);
    await tester.tap(find.text('Medications'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField).first, 'Antibiotic');
    await tester.tap(find.text('Three times a day'));
    await tester.pumpAndSettle();
    expect(find.text('Time of the first dose'), findsOneWidget);
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
    expect(find.text('16:00'), findsOneWidget);
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    expect(server.saved.single['frequency'], 'Three times a day');
    expect(server.saved.single['times'], ['00:00', '08:00', '16:00']);
  });

  testWidgets('a suggested time can be changed by tapping it', (tester) async {
    await _open(tester, _Server());
    await tester.tap(find.text('Medications'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Twice a day'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('20:00'));
    await tester.pumpAndSettle();
    expect(find.byType(TimePickerDialog), findsOneWidget);
  });

  testWidgets('refused notifications are explained', (tester) async {
    reminders.permission = false;
    await _open(tester, _Server());
    await tester.tap(find.text('Medications'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Add a time'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(SwitchListTile));
    await tester.pumpAndSettle();
    expect(find.textContaining('Notifications are off'), findsOneWidget);
  });

  testWidgets('an appointment needs a date and time', (tester) async {
    final server = _Server();
    await _open(tester, server);
    await tester.tap(find.text('Appointments'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FloatingActionButton));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField).first, 'Dr. Ahmed');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();
    expect(find.text('Required'), findsOneWidget);
    expect(server.saved, isEmpty);
  });

  testWidgets('the history lists records', (tester) async {
    await _open(tester, _Server());
    await tester.tap(find.text('Medical history'));
    await tester.pumpAndSettle();
    expect(find.text('HbA1c'), findsOneWidget);
  });
}
