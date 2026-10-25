# Diwan mobile app (Flutter)

Android and iOS app for the Diwan platform. After sign-in it opens a **dashboard** with one tile per feature
of the gateway's `GET /api/features` (the `app_features` table, so tiles can be added, reordered or switched off
on the server without a new app release). Each tile opens its section by the feature's `screen`:

| `screen` | Section | Service |
|---|---|---|
| `Portfolio` | Investments: portfolios, watchlist, news, alerts (bottom tabs) | `/api/portfolio` |
| `Transactions` | Financial history: income/expense totals per currency, list, add/edit/delete | `/api/transactions` |
| `MedicalHistory` | Medical history: a tab per category (medications, allergies, blood type, surgeries, chronic diseases, lab results, vaccines, family history) | `/api/medical` |
| `SmartHome` | Devices with an on/off switch (the server sends the MQTT command), add/rename/remove | `/api/smart-home/devices` |
| `UpdateUserInfo` | My account: profile, language, fingerprint lock, sign out, deactivate/delete account | `/api/users` |

Tile titles and descriptions come from the app's translations; a tile with a `screen` the app does not know yet
shows the server's texts and a "not available in the app yet" page. The mapping is `featureScreens` in
`lib/ui/dashboard_screen.dart`.

- **Languages:** Arabic and English, chosen on the login screen or in Settings (default: the phone's language).
  Arabic switches the whole UI to right-to-left. Server texts (errors, labels of asset types, countries,
  currencies, alerts) arrive in the selected language through `Accept-Language`; the choice is also saved on the
  server so alerts/notifications use it.
- **Security:** the login token is kept in the platform secure storage (Android Keystore / iOS Keychain), never in
  plain preferences; optional fingerprint/face unlock (Settings); signing out revokes the token at the gateway.
  Only HTTPS is allowed, except the local addresses used for development (`network_security_config.xml`).
- **Investment screens:** portfolios (headline numbers) -> portfolio (grouped view country -> type with a chart, all
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
- The financial-history service cannot change type, currency, date or description of a saved transaction, so
  the app shows those read-only when editing.
