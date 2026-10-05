import 'dart:convert';

import 'package:flutter/widgets.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:local_auth/local_auth.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../api/api_client.dart';
import '../api/diwan_api.dart';
import '../api/models.dart';

/// Who is signed in, which language the app uses, and whether the app is locked behind the fingerprint.
/// The token lives in the platform's secure storage (Keychain / Android Keystore), never in plain prefs.
class Session extends ChangeNotifier {
  Session({FlutterSecureStorage? storage, LocalAuthentication? localAuth})
      : _storage = storage ?? const FlutterSecureStorage(),
        _localAuth = localAuth ?? LocalAuthentication() {
    api = DiwanApi(ApiClient(
      tokenProvider: () => _user?.token,
      languageProvider: () => locale.languageCode,
      onUnauthorized: _expired,
    ));
  }

  static const _userKey = 'diwan.user';
  static const _langKey = 'diwan.language';
  static const _biometricKey = 'diwan.biometric';

  final FlutterSecureStorage _storage;
  final LocalAuthentication _localAuth;
  late final DiwanApi api;

  AuthUser? _user;
  Locale locale = const Locale('en');
  bool biometricEnabled = false;
  bool locked = false;
  bool ready = false;
  Reference? _reference;
  String? _referenceLanguage;

  AuthUser? get user => _user;
  bool get signedIn => _user != null;

  /// Restores language, user and lock state at app start.
  Future<void> load(Locale deviceLocale) async {
    final prefs = await SharedPreferences.getInstance();
    final lang = prefs.getString(_langKey) ?? (deviceLocale.languageCode == 'ar' ? 'ar' : 'en');
    locale = Locale(lang);
    biometricEnabled = prefs.getBool(_biometricKey) ?? false;
    try {
      final raw = await _storage.read(key: _userKey);
      if (raw != null) {
        _user = AuthUser.fromJson(jsonDecode(raw) as Map<String, dynamic>);
        locked = biometricEnabled;
      }
    } catch (_) {
      await _storage.delete(key: _userKey); // unreadable after a reinstall / key change: sign in again
    }
    ready = true;
    notifyListeners();
  }

  Future<void> login(String email, String password) => _signedIn(api.login(email.trim(), password));

  Future<void> register(String name, String email, String password) =>
      _signedIn(api.register(name.trim(), email.trim(), password));

  Future<void> _signedIn(Future<AuthUser> call) async {
    final user = await call;
    await _storage.write(key: _userKey, value: jsonEncode({
      'token': user.token, 'userId': user.userId, 'fullName': user.fullName, 'email': user.email,
    }));
    _user = user;
    locked = false;
    notifyListeners();
    _syncLanguage();
  }

  Future<void> logout() async {
    try {
      await api.logout(); // revokes the token at the gateway
    } catch (_) {
      // signing out locally must work offline too
    }
    await _clear();
  }

  void _expired() {
    if (_user != null) _clear();
  }

  Future<void> _clear() async {
    await _storage.delete(key: _userKey);
    _user = null;
    _reference = null;
    locked = false;
    notifyListeners();
  }

  Future<void> setLanguage(String languageCode) async {
    locale = Locale(languageCode);
    _reference = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_langKey, languageCode);
    notifyListeners();
    _syncLanguage();
  }

  /// Alerts and push notifications are written in the language saved on the server.
  void _syncLanguage() {
    if (_user != null) {
      api.savePreferences(language: locale.languageCode).catchError((_) {});
    }
  }

  /// Reference data (types, countries, currencies with labels) in the current language, cached.
  Future<Reference> reference() async {
    if (_reference == null || _referenceLanguage != locale.languageCode) {
      _reference = await api.reference();
      _referenceLanguage = locale.languageCode;
    }
    return _reference!;
  }

  Future<bool> canUseBiometrics() async {
    try {
      return await _localAuth.isDeviceSupported() && await _localAuth.canCheckBiometrics;
    } catch (_) {
      return false;
    }
  }

  Future<void> setBiometric(bool enabled, String reason) async {
    if (enabled && !await _authenticate(reason)) return;
    biometricEnabled = enabled;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_biometricKey, enabled);
    notifyListeners();
  }

  Future<void> unlock(String reason) async {
    if (await _authenticate(reason)) {
      locked = false;
      notifyListeners();
    }
  }

  Future<bool> _authenticate(String reason) async {
    try {
      return await _localAuth.authenticate(localizedReason: reason);
    } catch (_) {
      return false;
    }
  }
}
