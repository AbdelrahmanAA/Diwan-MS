# Diwan mobile app (Flutter)

Android and iOS app for the Diwan platform. The first version covers sign-in/registration and the investment
portfolio service (`/api/portfolio` through the gateway); transactions, medical and smart-home sections can be
added as further tabs.

- **Languages:** Arabic and English, chosen on the login screen or in Settings (default: the phone's language).
  Arabic switches the whole UI to right-to-left. Server texts (errors, labels of asset types, countries,
  currencies, alerts) arrive in the selected language through `Accept-Language`; the choice is also saved on the
  server so alerts/notifications use it.
- **Security:** the login token is kept in the platform secure storage (Android Keystore / iOS Keychain), never in
  plain preferences; optional fingerprint/face unlock (Settings); signing out revokes the token at the gateway.
  Only HTTPS is allowed, except the local addresses used for development (`network_security_config.xml`).
- **Screens:** portfolios (headline numbers) -> portfolio (grouped view country -> type with a chart, all
  investments, allocation vs targets with rebalancing suggestions, goals) -> investment (value, P/L, price
  history, transactions, valuations); watchlist; news about your investments; alerts (inbox + rules); settings.

## Requirements

Flutter 3.47+ (installed on this machine in `C:\Users\abdel\dev\flutter`), the Android SDK, and for iOS a Mac
with Xcode and an Apple developer account.

## Run and build

```
flutter pub get
flutter gen-l10n                                   # after changing lib/l10n/*.arb
flutter analyze && flutter test

# against the local docker stack (gateway on :8087) from the Android emulator
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8087

# release APK against the SIT server (https://developerxgroup.ddns.net)
flutter build apk --release
# -> build/app/outputs/flutter-apk/app-release.apk
```

The release build is currently signed with the debug key so it can be installed directly. Before publishing to
Google Play, create an upload keystore and fill in `android/key.properties` (see `android/app/build.gradle.kts`).

## Structure

| Path | What |
|---|---|
| `lib/config.dart` | `API_BASE_URL` (dart-define) |
| `lib/api/` | HTTP client (token, language, error mapping), response models, all API calls |
| `lib/state/session.dart` | signed-in user, language, fingerprint lock, cached reference data |
| `lib/l10n/app_*.arb` | UI texts (English template + Arabic) |
| `lib/ui/` | screens and shared widgets; `format.dart` formats money/dates per language and parses Arabic digits |

## Next

- Push notifications for alerts (needs a Firebase project; alerts are already published to Kafka
  `portfolio.alerts` with title/message in the user's language).
- More sections of Diwan (transactions, medical, smart home) as tabs.
