import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'widgets.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _form = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _register = false;
  bool _busy = false;

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    final session = context.read<Session>();
    try {
      if (_register) {
        await session.register(_name.text, _email.text, _password.text);
      } else {
        await session.login(_email.text, _password.text);
      }
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final session = context.watch<Session>();
    String? required(String? v) => v == null || v.trim().isEmpty ? t.required : null;
    return Scaffold(
      appBar: AppBar(actions: [
        TextButton(
          onPressed: () => session.setLanguage(session.locale.languageCode == 'ar' ? 'en' : 'ar'),
          child: Text(session.locale.languageCode == 'ar' ? 'English' : 'العربية'),
        ),
      ]),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _form,
                child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                  Icon(Icons.account_balance, size: 64, color: Theme.of(context).colorScheme.primary),
                  const SizedBox(height: 12),
                  Text(t.appTitle, textAlign: TextAlign.center, style: Theme.of(context).textTheme.headlineSmall),
                  const SizedBox(height: 32),
                  if (_register) ...[
                    TextFormField(
                      controller: _name,
                      decoration: InputDecoration(labelText: t.fullName, border: const OutlineInputBorder()),
                      textInputAction: TextInputAction.next,
                      validator: required,
                    ),
                    const SizedBox(height: 12),
                  ],
                  TextFormField(
                    controller: _email,
                    decoration: InputDecoration(labelText: t.email, border: const OutlineInputBorder()),
                    keyboardType: TextInputType.emailAddress,
                    textDirection: TextDirection.ltr,
                    autofillHints: const [AutofillHints.email],
                    textInputAction: TextInputAction.next,
                    validator: required,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _password,
                    obscureText: true,
                    decoration: InputDecoration(
                      labelText: t.password,
                      helperText: _register ? t.passwordHint : null,
                      border: const OutlineInputBorder(),
                    ),
                    autofillHints: const [AutofillHints.password],
                    onFieldSubmitted: (_) => _submit(),
                    validator: (v) => required(v) ?? (_register && v!.length < 8 ? t.passwordHint : null),
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: _busy ? null : _submit,
                    child: _busy
                        ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                        : Text(_register ? t.createAccount : t.signIn),
                  ),
                  TextButton(
                    onPressed: () => setState(() => _register = !_register),
                    child: Text(_register ? t.haveAccount : t.noAccount),
                  ),
                ]),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
