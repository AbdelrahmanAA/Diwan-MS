/// Build-time configuration:
///   flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8087   (Android emulator -> local docker gateway)
///   flutter build apk                                              (default: the SIT server)
class AppConfig {
  static const apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'https://developerxgroup.ddns.net',
  );

  static const requestTimeout = Duration(seconds: 20);
}
