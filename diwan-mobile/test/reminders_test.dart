import 'package:diwan_mobile/api/models.dart';
import 'package:diwan_mobile/l10n/app_localizations_en.dart';
import 'package:diwan_mobile/platform/reminders.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

final _t = AppLocalizationsEn();

MedicationReminder _med(String name, List<String> times, {bool self = true, String? person, String? start, String? end}) =>
    MedicationReminder.fromJson({
      'id': 1, 'profileId': 1, 'person': person, 'self': self, 'name': name, 'dosage': '500mg', 'times': times,
      'startDate': start, 'endDate': end,
    });

AppointmentReminder _visit(String at, {bool self = true, String? person}) => AppointmentReminder.fromJson(
    {'id': 2, 'profileId': 1, 'person': person, 'self': self, 'with': 'Dr. Ahmed', 'at': at, 'location': 'KFSH'});

void main() {
  late tz.TZDateTime now;

  setUpAll(() {
    tz_data.initializeTimeZones();
    tz.setLocalLocation(tz.getLocation('Asia/Riyadh'));
    now = tz.TZDateTime(tz.local, 2026, 11, 7, 12, 0);
  });

  test('an ongoing medication repeats daily from its next dose time', () {
    final s = reminderSchedule(ReminderPlan([_med('Metformin', ['08:00', '20:00'])], []), _t, now);

    expect(s.map((r) => r.daily), everyElement(isTrue));
    expect(s.map((r) => '${r.at.day} ${r.at.hour}'), ['7 20', '8 8']);
    expect(s.first.title, 'Time for a dose');
    expect(s.first.body, 'Metformin · 500mg');
  });

  test('a medication with an end date rings only until that date', () {
    final s = reminderSchedule(ReminderPlan([_med('Antibiotic', ['09:00'], end: '2026-11-09')], []), _t, now);

    expect(s.map((r) => r.daily), everyElement(isFalse));
    expect(s.map((r) => r.at.day), [8, 9]);
  });

  test('a medication that starts later does not ring before it starts', () {
    final s = reminderSchedule(ReminderPlan([_med('Later', ['09:00'], start: '2026-11-20')], []), _t, now);

    expect(s.first.at.day, 20);
    expect(s, hasLength(14));
  });

  test('a family member is named in the reminder', () {
    final s = reminderSchedule(ReminderPlan([_med('Syrup', ['18:00'], self: false, person: 'Sara')], []), _t, now);

    expect(s.single.title, 'Dose for Sara');
  });

  test('an appointment is announced the day before and two hours before', () {
    final s = reminderSchedule(ReminderPlan([], [_visit('2026-11-10T10:30')]), _t, now);

    expect(s.map((r) => '${r.at.day} ${r.at.hour}:${r.at.minute}'), ['9 10:30', '10 8:30']);
    expect(s.first.title, 'Appointment tomorrow');
    expect(s.first.body, 'Dr. Ahmed · KFSH');
  });

  test('reminders already past are left out', () {
    final s = reminderSchedule(ReminderPlan([], [_visit('2026-11-07T13:00')]), _t, now);

    expect(s, isEmpty);
  });
}
