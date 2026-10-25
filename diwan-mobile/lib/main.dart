import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';

import 'l10n/app_localizations.dart';
import 'sms/sms_sync.dart';
import 'state/session.dart';
import 'ui/dashboard_screen.dart';
import 'ui/login_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final session = Session();
  session.load(WidgetsBinding.instance.platformDispatcher.locale);
  runApp(ChangeNotifierProvider.value(value: session, child: const DiwanApp()));
}

/// Started by SmsSyncWorker (Android) in a headless engine: uploads new bank SMS, then reports back.
@pragma('vm:entry-point')
Future<void> smsBackgroundSync() async {
  WidgetsFlutterBinding.ensureInitialized();
  var ok = false;
  try {
    final raw = await const FlutterSecureStorage().read(key: Session.userKey);
    ok = await runSmsSyncInBackground(raw == null ? null : (jsonDecode(raw) as Map<String, dynamic>)['token'] as String?);
  } catch (_) {
    ok = false;
  }
  await const MethodChannel('diwan/sms').invokeMethod('done', {'ok': ok});
}

class DiwanApp extends StatelessWidget {
  const DiwanApp({super.key});

  @override
  Widget build(BuildContext context) {
    final session = context.watch<Session>();
    const seed = Color(0xFF7C3AED);
    return MaterialApp(
      // a new navigator when the user changes: pages opened from the dashboard do not survive a sign-out
      key: ValueKey(session.user?.userId),
      onGenerateTitle: (c) => AppLocalizations.of(c).appTitle,
      debugShowCheckedModeBanner: false,
      locale: session.locale,
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      theme: ThemeData(colorSchemeSeed: seed, useMaterial3: true),
      darkTheme: ThemeData(colorSchemeSeed: seed, brightness: Brightness.dark, useMaterial3: true),
      home: !session.ready
          ? const Scaffold(body: Center(child: CircularProgressIndicator()))
          : !session.signedIn
              ? const LoginScreen()
              : session.locked
                  ? const LockScreen()
                  // a new key per user so nothing of a previous session survives a sign-in
                  : DashboardScreen(key: ValueKey(session.user!.userId)),
    );
  }
}

class LockScreen extends StatelessWidget {
  const LockScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final session = context.read<Session>();
    return Scaffold(
      body: Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const Icon(Icons.lock_outline, size: 64),
          const SizedBox(height: 16),
          FilledButton.icon(
            icon: const Icon(Icons.fingerprint),
            label: Text(t.unlock),
            onPressed: () => session.unlock(t.unlockReason),
          ),
          TextButton(onPressed: session.logout, child: Text(t.logout)),
        ]),
      ),
    );
  }
}
