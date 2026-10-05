import 'package:diwan_mobile/l10n/app_localizations.dart';
import 'package:diwan_mobile/state/session.dart';
import 'package:diwan_mobile/ui/login_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

Widget _app(Locale locale) => ChangeNotifierProvider(
      create: (_) => Session(),
      child: MaterialApp(
        locale: locale,
        supportedLocales: AppLocalizations.supportedLocales,
        localizationsDelegates: const [
          AppLocalizations.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        home: const LoginScreen(),
      ),
    );

void main() {
  testWidgets('login screen in English is left-to-right', (tester) async {
    await tester.pumpWidget(_app(const Locale('en')));
    expect(find.text('Sign in'), findsOneWidget);
    expect(Directionality.of(tester.element(find.text('Sign in'))), TextDirection.ltr);
  });

  testWidgets('login screen in Arabic is right-to-left', (tester) async {
    await tester.pumpWidget(_app(const Locale('ar')));
    expect(find.text('تسجيل الدخول'), findsOneWidget);
    expect(Directionality.of(tester.element(find.text('تسجيل الدخول'))), TextDirection.rtl);
  });

  testWidgets('empty form shows required errors instead of calling the server', (tester) async {
    await tester.pumpWidget(_app(const Locale('en')));
    await tester.tap(find.text('Sign in'));
    await tester.pump();
    expect(find.text('Required'), findsNWidgets(2));
  });
}
