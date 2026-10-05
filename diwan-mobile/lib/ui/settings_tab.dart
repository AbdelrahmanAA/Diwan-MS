import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../l10n/app_localizations.dart';
import '../state/session.dart';

class SettingsTab extends StatelessWidget {
  const SettingsTab({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final session = context.watch<Session>();
    final user = session.user!;
    return Scaffold(
      appBar: AppBar(title: Text(t.settings)),
      body: ListView(children: [
        ListTile(
          leading: const CircleAvatar(child: Icon(Icons.person)),
          title: Text(user.fullName),
          subtitle: Text(user.email, textDirection: TextDirection.ltr),
        ),
        const Divider(),
        ListTile(
          leading: const Icon(Icons.language),
          title: Text(t.language),
          trailing: SegmentedButton<String>(
            segments: const [
              ButtonSegment(value: 'en', label: Text('English')),
              ButtonSegment(value: 'ar', label: Text('العربية')),
            ],
            selected: {session.locale.languageCode},
            onSelectionChanged: (s) => session.setLanguage(s.first),
          ),
        ),
        FutureBuilder<bool>(
          future: session.canUseBiometrics(),
          builder: (c, snap) => snap.data == true
              ? SwitchListTile(
                  secondary: const Icon(Icons.fingerprint),
                  title: Text(t.unlockWithBiometrics),
                  value: session.biometricEnabled,
                  onChanged: (v) => session.setBiometric(v, t.unlockReason),
                )
              : const SizedBox.shrink(),
        ),
        const Divider(),
        FutureBuilder(
          future: session.reference(),
          builder: (c, snap) => ListTile(
            leading: const Icon(Icons.info_outline),
            title: Text(t.about),
            subtitle: Text([
              if (snap.data?.marketDataProvider == 'fake') t.demoData,
              if (snap.data != null && snap.data!.marketDataProvider != 'fake') t.marketData(snap.data!.marketDataProvider),
              t.fxAttribution,
            ].join('\n')),
          ),
        ),
        const Divider(),
        ListTile(
          leading: Icon(Icons.logout, color: Theme.of(context).colorScheme.error),
          title: Text(t.logout, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          onTap: session.logout,
        ),
      ]),
    );
  }
}
