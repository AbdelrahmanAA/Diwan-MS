import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

import '../api/models.dart';
import '../l10n/app_localizations.dart';

/// Medication and appointment reminders, scheduled on the phone itself (no push service). Each sync replaces
/// every reminder with the plan from the server, so a change made on another phone arrives at the next sync.
abstract class Reminders {
  /// Swapped in tests, where the notification plugin has no platform side.
  static Reminders instance = LocalReminders();

  /// Asks for notification permission (Android 13+) and exact alarms. False when notifications are off.
  Future<bool> requestPermission();

  Future<void> sync(ReminderPlan plan, AppLocalizations t);

  Future<void> cancelAll();
}

class LocalReminders implements Reminders {
  static const _channel = AndroidNotificationDetails(
    'medical_reminders',
    'Medical reminders',
    channelDescription: 'Medication doses and appointments',
    importance: Importance.high,
    priority: Priority.high,
    // the lock screen shows only that there is a reminder, not the medicine or the doctor
    visibility: NotificationVisibility.private,
  );
  static const _details = NotificationDetails(android: _channel);

  /// The system allows only so many scheduled alarms per app; reminders beyond this are left out.
  static const _maxScheduled = 400;

  final _plugin = FlutterLocalNotificationsPlugin();
  bool _ready = false;
  bool _exact = false;

  Future<void> _init() async {
    if (_ready) return;
    tz_data.initializeTimeZones();
    try {
      tz.setLocalLocation(tz.getLocation((await FlutterTimezone.getLocalTimezone()).identifier));
    } catch (_) {
      tz.setLocalLocation(tz.getLocation('Asia/Riyadh'));
    }
    await _plugin.initialize(
        settings: const InitializationSettings(android: AndroidInitializationSettings('@mipmap/ic_launcher')));
    _ready = true;
  }

  AndroidFlutterLocalNotificationsPlugin? get _android =>
      _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();

  @override
  Future<bool> requestPermission() async {
    await _init();
    final allowed = await _android?.requestNotificationsPermission() ?? true;
    _exact = await _android?.canScheduleExactNotifications() ?? false;
    if (!_exact) _exact = await _android?.requestExactAlarmsPermission() ?? false;
    return allowed;
  }

  @override
  Future<void> sync(ReminderPlan plan, AppLocalizations t) async {
    await _init();
    _exact = await _android?.canScheduleExactNotifications() ?? false;
    await _plugin.cancelAll();
    final now = tz.TZDateTime.now(tz.local);
    var id = 0;
    for (final s in reminderSchedule(plan, t, now)) {
      if (id >= _maxScheduled) break;
      await _plugin.zonedSchedule(
        id: id++,
        scheduledDate: s.at,
        title: s.title,
        body: s.body,
        notificationDetails: _details,
        androidScheduleMode: _exact ? AndroidScheduleMode.exactAllowWhileIdle : AndroidScheduleMode.inexactAllowWhileIdle,
        matchDateTimeComponents: s.daily ? DateTimeComponents.time : null,
      );
    }
  }

  @override
  Future<void> cancelAll() async {
    if (!_ready) return;
    await _plugin.cancelAll();
  }
}

/// One notification to schedule. [daily] repeats it every day at the same time.
class ScheduledReminder {
  ScheduledReminder(this.at, this.title, this.body, {this.daily = false});

  final tz.TZDateTime at;
  final String title;
  final String body;
  final bool daily;
}

/// What to schedule from [plan], from [now] on. A medication that has not started yet or ends soon gets dated
/// reminders for its remaining days instead of a daily repeat, so nothing rings outside its dates.
@visibleForTesting
List<ScheduledReminder> reminderSchedule(ReminderPlan plan, AppLocalizations t, tz.TZDateTime now) {
  const datedDays = 14;
  final out = <ScheduledReminder>[];
  final today = DateTime(now.year, now.month, now.day);
  for (final m in plan.medications) {
    final person = m.self ? null : m.person;
    final title = person == null ? t.reminderDose : t.reminderDoseFor(person);
    final body = [m.name, m.dosage].nonNulls.join(' · ');
    final start = m.startDate != null && m.startDate!.isAfter(today) ? m.startDate! : today;
    final daily = start == today && m.endDate == null;
    for (final time in m.times) {
      final parts = time.split(':');
      final hour = int.parse(parts[0]), minute = int.parse(parts[1]);
      if (daily) {
        var at = tz.TZDateTime(tz.local, now.year, now.month, now.day, hour, minute);
        if (!at.isAfter(now)) at = at.add(const Duration(days: 1));
        out.add(ScheduledReminder(at, title, body, daily: true));
        continue;
      }
      for (var d = 0; d < datedDays; d++) {
        final day = start.add(Duration(days: d));
        if (m.endDate != null && day.isAfter(m.endDate!)) break;
        final at = tz.TZDateTime(tz.local, day.year, day.month, day.day, hour, minute);
        if (at.isAfter(now)) out.add(ScheduledReminder(at, title, body));
      }
    }
  }
  for (final a in plan.appointments) {
    final person = a.self ? null : a.person;
    final when = tz.TZDateTime(tz.local, a.at.year, a.at.month, a.at.day, a.at.hour, a.at.minute);
    final body = [a.withWhom, ?person, a.location].nonNulls.join(' · ');
    for (final before in const [Duration(days: 1), Duration(hours: 2)]) {
      final at = when.subtract(before);
      if (at.isAfter(now)) {
        out.add(ScheduledReminder(at, before.inDays == 1 ? t.reminderAppointmentTomorrow : t.reminderAppointmentSoon, body));
      }
    }
  }
  out.sort((a, b) => a.at.compareTo(b.at));
  return out;
}
