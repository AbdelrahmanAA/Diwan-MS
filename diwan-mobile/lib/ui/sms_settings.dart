import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:provider/provider.dart';

import '../l10n/app_localizations.dart';
import '../sms/sms_sync.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

/// My Account: turn bank SMS logging on/off (Android only), with the disclosure Google Play requires before the
/// SMS permission is requested, and the sync status.
class SmsSettings extends StatefulWidget {
  const SmsSettings({super.key});

  @override
  State<SmsSettings> createState() => _SmsSettingsState();
}

class _SmsSettingsState extends State<SmsSettings> {
  bool _busy = false;

  Future<void> _sync(Future<SmsSyncResult> Function() run) async {
    final t = AppLocalizations.of(context);
    setState(() => _busy = true);
    try {
      final r = await run();
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(t.smsSyncResult(r.imported))));
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _enable(SmsSync sms) async {
    final t = AppLocalizations.of(context);
    final agreed = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        icon: const Icon(Icons.sms_outlined),
        title: Text(t.smsDisclosureTitle),
        content: SingleChildScrollView(child: Text(t.smsDisclosure)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: Text(t.notNow)),
          FilledButton(onPressed: () => Navigator.pop(c, true), child: Text(t.continueLabel)),
        ],
      ),
    );
    if (agreed != true || !mounted) return;

    final status = await Permission.sms.request();
    if (!mounted) return;
    if (!status.isGranted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(t.smsPermissionDenied),
        action: status.isPermanentlyDenied ? SnackBarAction(label: t.openSettings, onPressed: openAppSettings) : null,
      ));
      return;
    }

    final months = await showDialog<int>(
      context: context,
      builder: (c) => SimpleDialog(
        title: Text(t.smsHistoryTitle),
        children: [
          for (final (m, label) in [(0, t.smsHistoryNone), (3, t.smsHistory3), (12, t.smsHistory12)])
            SimpleDialogOption(onPressed: () => Navigator.pop(c, m), child: Text(label)),
        ],
      ),
    );
    if (months == null || !mounted) return;
    final now = DateTime.now();
    await _sync(() => sms.enable(months == 0 ? now : DateTime(now.year, now.month - months, now.day)));
  }

  Future<void> _disable(SmsSync sms) async {
    setState(() => _busy = true);
    try {
      await sms.disable();
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final sms = context.watch<Session>().sms;
    if (sms == null || !SmsSync.supported) return const SizedBox.shrink();
    final t = AppLocalizations.of(context);
    final f = Fmt(context);
    final stats = sms.stats;
    final unrecognized = stats['unrecognized'] ?? 0;
    return Column(children: [
      SwitchListTile(
        secondary: const Icon(Icons.sms_outlined),
        title: Text(t.smsLogging),
        subtitle: Text(t.smsLoggingHint),
        value: sms.enabled,
        onChanged: _busy ? null : (on) => on ? _enable(sms) : _disable(sms),
      ),
      if (sms.enabled)
        ListTile(
          leading: const SizedBox(width: 24),
          title: Text(t.smsImported(stats['imported'] ?? 0)),
          subtitle: Text([
            if (sms.lastSync != null) t.smsLastSync(f.dateTime(sms.lastSync)),
            if (unrecognized > 0) t.smsUnrecognized(unrecognized),
          ].join('\n')),
          trailing: _busy
              ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
              : TextButton(onPressed: () => _sync(sms.run), child: Text(t.smsSyncNow)),
        ),
    ]);
  }
}
