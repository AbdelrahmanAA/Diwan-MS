import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_ar.dart';
import 'app_localizations_en.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('ar'),
    Locale('en'),
  ];

  /// No description provided for @appTitle.
  ///
  /// In en, this message translates to:
  /// **'Diwan Investments'**
  String get appTitle;

  /// No description provided for @signIn.
  ///
  /// In en, this message translates to:
  /// **'Sign in'**
  String get signIn;

  /// No description provided for @createAccount.
  ///
  /// In en, this message translates to:
  /// **'Create account'**
  String get createAccount;

  /// No description provided for @fullName.
  ///
  /// In en, this message translates to:
  /// **'Full name'**
  String get fullName;

  /// No description provided for @email.
  ///
  /// In en, this message translates to:
  /// **'Email'**
  String get email;

  /// No description provided for @password.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get password;

  /// No description provided for @passwordHint.
  ///
  /// In en, this message translates to:
  /// **'At least 8 characters'**
  String get passwordHint;

  /// No description provided for @noAccount.
  ///
  /// In en, this message translates to:
  /// **'No account yet? Create one'**
  String get noAccount;

  /// No description provided for @haveAccount.
  ///
  /// In en, this message translates to:
  /// **'Already have an account? Sign in'**
  String get haveAccount;

  /// No description provided for @logout.
  ///
  /// In en, this message translates to:
  /// **'Sign out'**
  String get logout;

  /// No description provided for @portfolios.
  ///
  /// In en, this message translates to:
  /// **'Portfolios'**
  String get portfolios;

  /// No description provided for @watchlist.
  ///
  /// In en, this message translates to:
  /// **'Watchlist'**
  String get watchlist;

  /// No description provided for @news.
  ///
  /// In en, this message translates to:
  /// **'News'**
  String get news;

  /// No description provided for @alerts.
  ///
  /// In en, this message translates to:
  /// **'Alerts'**
  String get alerts;

  /// No description provided for @settings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settings;

  /// No description provided for @newPortfolio.
  ///
  /// In en, this message translates to:
  /// **'New portfolio'**
  String get newPortfolio;

  /// No description provided for @editPortfolio.
  ///
  /// In en, this message translates to:
  /// **'Edit portfolio'**
  String get editPortfolio;

  /// No description provided for @deletePortfolio.
  ///
  /// In en, this message translates to:
  /// **'Delete portfolio'**
  String get deletePortfolio;

  /// No description provided for @deletePortfolioConfirm.
  ///
  /// In en, this message translates to:
  /// **'Delete \"{name}\" and all its investments? This cannot be undone.'**
  String deletePortfolioConfirm(String name);

  /// No description provided for @name.
  ///
  /// In en, this message translates to:
  /// **'Name'**
  String get name;

  /// No description provided for @description.
  ///
  /// In en, this message translates to:
  /// **'Description'**
  String get description;

  /// No description provided for @baseCurrency.
  ///
  /// In en, this message translates to:
  /// **'Base currency'**
  String get baseCurrency;

  /// No description provided for @save.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get save;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @delete.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get delete;

  /// No description provided for @retry.
  ///
  /// In en, this message translates to:
  /// **'Try again'**
  String get retry;

  /// No description provided for @required.
  ///
  /// In en, this message translates to:
  /// **'Required'**
  String get required;

  /// No description provided for @invalidNumber.
  ///
  /// In en, this message translates to:
  /// **'Enter a valid number'**
  String get invalidNumber;

  /// No description provided for @networkError.
  ///
  /// In en, this message translates to:
  /// **'Cannot reach the server. Check your connection and try again.'**
  String get networkError;

  /// No description provided for @sessionExpired.
  ///
  /// In en, this message translates to:
  /// **'Your session has ended. Please sign in again.'**
  String get sessionExpired;

  /// No description provided for @totalValue.
  ///
  /// In en, this message translates to:
  /// **'Total value'**
  String get totalValue;

  /// No description provided for @today.
  ///
  /// In en, this message translates to:
  /// **'Today'**
  String get today;

  /// No description provided for @unrealizedPnl.
  ///
  /// In en, this message translates to:
  /// **'Unrealized P/L'**
  String get unrealizedPnl;

  /// No description provided for @realizedPnl.
  ///
  /// In en, this message translates to:
  /// **'Realized P/L'**
  String get realizedPnl;

  /// No description provided for @totalReturn.
  ///
  /// In en, this message translates to:
  /// **'Total return'**
  String get totalReturn;

  /// No description provided for @income.
  ///
  /// In en, this message translates to:
  /// **'Net income'**
  String get income;

  /// No description provided for @costBasis.
  ///
  /// In en, this message translates to:
  /// **'Cost'**
  String get costBasis;

  /// No description provided for @holdingsCount.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =0{No investments} =1{1 investment} other{{count} investments}}'**
  String holdingsCount(int count);

  /// No description provided for @overview.
  ///
  /// In en, this message translates to:
  /// **'Overview'**
  String get overview;

  /// No description provided for @holdings.
  ///
  /// In en, this message translates to:
  /// **'Investments'**
  String get holdings;

  /// No description provided for @allocation.
  ///
  /// In en, this message translates to:
  /// **'Allocation'**
  String get allocation;

  /// No description provided for @goals.
  ///
  /// In en, this message translates to:
  /// **'Goals'**
  String get goals;

  /// No description provided for @byCountry.
  ///
  /// In en, this message translates to:
  /// **'By country'**
  String get byCountry;

  /// No description provided for @byType.
  ///
  /// In en, this message translates to:
  /// **'By type'**
  String get byType;

  /// No description provided for @byCurrency.
  ///
  /// In en, this message translates to:
  /// **'By currency'**
  String get byCurrency;

  /// No description provided for @addInvestment.
  ///
  /// In en, this message translates to:
  /// **'Add investment'**
  String get addInvestment;

  /// No description provided for @editInvestment.
  ///
  /// In en, this message translates to:
  /// **'Edit investment'**
  String get editInvestment;

  /// No description provided for @assetType.
  ///
  /// In en, this message translates to:
  /// **'Type'**
  String get assetType;

  /// No description provided for @country.
  ///
  /// In en, this message translates to:
  /// **'Country'**
  String get country;

  /// No description provided for @currency.
  ///
  /// In en, this message translates to:
  /// **'Currency'**
  String get currency;

  /// No description provided for @symbol.
  ///
  /// In en, this message translates to:
  /// **'Market symbol'**
  String get symbol;

  /// No description provided for @symbolHint.
  ///
  /// In en, this message translates to:
  /// **'e.g. AAPL - search by name or symbol'**
  String get symbolHint;

  /// No description provided for @search.
  ///
  /// In en, this message translates to:
  /// **'Search'**
  String get search;

  /// No description provided for @quantity.
  ///
  /// In en, this message translates to:
  /// **'Quantity'**
  String get quantity;

  /// No description provided for @price.
  ///
  /// In en, this message translates to:
  /// **'Price per unit'**
  String get price;

  /// No description provided for @amount.
  ///
  /// In en, this message translates to:
  /// **'Amount'**
  String get amount;

  /// No description provided for @fees.
  ///
  /// In en, this message translates to:
  /// **'Fees'**
  String get fees;

  /// No description provided for @date.
  ///
  /// In en, this message translates to:
  /// **'Date'**
  String get date;

  /// No description provided for @note.
  ///
  /// In en, this message translates to:
  /// **'Note'**
  String get note;

  /// No description provided for @notes.
  ///
  /// In en, this message translates to:
  /// **'Notes'**
  String get notes;

  /// No description provided for @details.
  ///
  /// In en, this message translates to:
  /// **'Details'**
  String get details;

  /// No description provided for @address.
  ///
  /// In en, this message translates to:
  /// **'Address'**
  String get address;

  /// No description provided for @firstTransaction.
  ///
  /// In en, this message translates to:
  /// **'First transaction (optional)'**
  String get firstTransaction;

  /// No description provided for @addTransaction.
  ///
  /// In en, this message translates to:
  /// **'Add transaction'**
  String get addTransaction;

  /// No description provided for @transactions.
  ///
  /// In en, this message translates to:
  /// **'Transactions'**
  String get transactions;

  /// No description provided for @valuations.
  ///
  /// In en, this message translates to:
  /// **'Valuations'**
  String get valuations;

  /// No description provided for @addValuation.
  ///
  /// In en, this message translates to:
  /// **'Add valuation'**
  String get addValuation;

  /// No description provided for @value.
  ///
  /// In en, this message translates to:
  /// **'Value'**
  String get value;

  /// No description provided for @averageCost.
  ///
  /// In en, this message translates to:
  /// **'Average cost'**
  String get averageCost;

  /// No description provided for @marketValue.
  ///
  /// In en, this message translates to:
  /// **'Market value'**
  String get marketValue;

  /// No description provided for @weight.
  ///
  /// In en, this message translates to:
  /// **'Weight'**
  String get weight;

  /// No description provided for @priceFrom.
  ///
  /// In en, this message translates to:
  /// **'Price: {source}'**
  String priceFrom(String source);

  /// No description provided for @sourceQuote.
  ///
  /// In en, this message translates to:
  /// **'market quote'**
  String get sourceQuote;

  /// No description provided for @sourceValuation.
  ///
  /// In en, this message translates to:
  /// **'your valuation'**
  String get sourceValuation;

  /// No description provided for @sourceCost.
  ///
  /// In en, this message translates to:
  /// **'purchase cost'**
  String get sourceCost;

  /// No description provided for @sourceCash.
  ///
  /// In en, this message translates to:
  /// **'balance'**
  String get sourceCash;

  /// No description provided for @closeHolding.
  ///
  /// In en, this message translates to:
  /// **'Archive'**
  String get closeHolding;

  /// No description provided for @reopenHolding.
  ///
  /// In en, this message translates to:
  /// **'Restore'**
  String get reopenHolding;

  /// No description provided for @deleteHolding.
  ///
  /// In en, this message translates to:
  /// **'Delete investment'**
  String get deleteHolding;

  /// No description provided for @confirmDelete.
  ///
  /// In en, this message translates to:
  /// **'Delete this? This cannot be undone.'**
  String get confirmDelete;

  /// No description provided for @targets.
  ///
  /// In en, this message translates to:
  /// **'Targets'**
  String get targets;

  /// No description provided for @editTargets.
  ///
  /// In en, this message translates to:
  /// **'Set targets'**
  String get editTargets;

  /// No description provided for @targetsHint.
  ///
  /// In en, this message translates to:
  /// **'Percentages must add up to 100. Leave all empty to remove the targets.'**
  String get targetsHint;

  /// No description provided for @targetsSum.
  ///
  /// In en, this message translates to:
  /// **'Total: {sum}%'**
  String targetsSum(String sum);

  /// No description provided for @current.
  ///
  /// In en, this message translates to:
  /// **'Current'**
  String get current;

  /// No description provided for @target.
  ///
  /// In en, this message translates to:
  /// **'Target'**
  String get target;

  /// No description provided for @drift.
  ///
  /// In en, this message translates to:
  /// **'Drift'**
  String get drift;

  /// No description provided for @buyMore.
  ///
  /// In en, this message translates to:
  /// **'Add {amount}'**
  String buyMore(String amount);

  /// No description provided for @reduce.
  ///
  /// In en, this message translates to:
  /// **'Reduce {amount}'**
  String reduce(String amount);

  /// No description provided for @noTargets.
  ///
  /// In en, this message translates to:
  /// **'No targets yet. Set a target allocation to see how far you are from it.'**
  String get noTargets;

  /// No description provided for @newGoal.
  ///
  /// In en, this message translates to:
  /// **'New goal'**
  String get newGoal;

  /// No description provided for @targetAmount.
  ///
  /// In en, this message translates to:
  /// **'Target amount'**
  String get targetAmount;

  /// No description provided for @targetDate.
  ///
  /// In en, this message translates to:
  /// **'Target date'**
  String get targetDate;

  /// No description provided for @monthlyNeeded.
  ///
  /// In en, this message translates to:
  /// **'{amount} per month'**
  String monthlyNeeded(String amount);

  /// No description provided for @goalReached.
  ///
  /// In en, this message translates to:
  /// **'Reached'**
  String get goalReached;

  /// No description provided for @noPortfolios.
  ///
  /// In en, this message translates to:
  /// **'No portfolios yet. Create your first one.'**
  String get noPortfolios;

  /// No description provided for @noHoldings.
  ///
  /// In en, this message translates to:
  /// **'No investments yet.'**
  String get noHoldings;

  /// No description provided for @noNews.
  ///
  /// In en, this message translates to:
  /// **'No news yet for your investments.'**
  String get noNews;

  /// No description provided for @noAlerts.
  ///
  /// In en, this message translates to:
  /// **'No alerts.'**
  String get noAlerts;

  /// No description provided for @noWatchlist.
  ///
  /// In en, this message translates to:
  /// **'Follow a symbol to see its price here.'**
  String get noWatchlist;

  /// No description provided for @noRules.
  ///
  /// In en, this message translates to:
  /// **'No alert rules.'**
  String get noRules;

  /// No description provided for @alertRules.
  ///
  /// In en, this message translates to:
  /// **'Alert rules'**
  String get alertRules;

  /// No description provided for @newAlertRule.
  ///
  /// In en, this message translates to:
  /// **'New alert rule'**
  String get newAlertRule;

  /// No description provided for @threshold.
  ///
  /// In en, this message translates to:
  /// **'Threshold'**
  String get threshold;

  /// No description provided for @thresholdPercent.
  ///
  /// In en, this message translates to:
  /// **'Threshold (%)'**
  String get thresholdPercent;

  /// No description provided for @markAllRead.
  ///
  /// In en, this message translates to:
  /// **'Mark all as read'**
  String get markAllRead;

  /// No description provided for @addSymbol.
  ///
  /// In en, this message translates to:
  /// **'Follow a symbol'**
  String get addSymbol;

  /// No description provided for @investment.
  ///
  /// In en, this message translates to:
  /// **'Investment'**
  String get investment;

  /// No description provided for @portfolio.
  ///
  /// In en, this message translates to:
  /// **'Portfolio'**
  String get portfolio;

  /// No description provided for @language.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get language;

  /// No description provided for @unlockWithBiometrics.
  ///
  /// In en, this message translates to:
  /// **'Unlock with fingerprint / face'**
  String get unlockWithBiometrics;

  /// No description provided for @unlockReason.
  ///
  /// In en, this message translates to:
  /// **'Unlock Diwan'**
  String get unlockReason;

  /// No description provided for @unlock.
  ///
  /// In en, this message translates to:
  /// **'Unlock'**
  String get unlock;

  /// No description provided for @about.
  ///
  /// In en, this message translates to:
  /// **'About'**
  String get about;

  /// No description provided for @fxAttribution.
  ///
  /// In en, this message translates to:
  /// **'Exchange rates by ExchangeRate-API (exchangerate-api.com)'**
  String get fxAttribution;

  /// No description provided for @demoData.
  ///
  /// In en, this message translates to:
  /// **'Demo market data: the server has no market data key yet.'**
  String get demoData;

  /// No description provided for @marketData.
  ///
  /// In en, this message translates to:
  /// **'Market data: {provider}'**
  String marketData(String provider);

  /// No description provided for @missingRates.
  ///
  /// In en, this message translates to:
  /// **'No exchange rate for {currencies}; not included in totals.'**
  String missingRates(String currencies);

  /// No description provided for @history.
  ///
  /// In en, this message translates to:
  /// **'Price history'**
  String get history;

  /// No description provided for @noHistory.
  ///
  /// In en, this message translates to:
  /// **'Price history builds up day by day.'**
  String get noHistory;

  /// No description provided for @readMore.
  ///
  /// In en, this message translates to:
  /// **'Read article'**
  String get readMore;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['ar', 'en'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'ar':
      return AppLocalizationsAr();
    case 'en':
      return AppLocalizationsEn();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
