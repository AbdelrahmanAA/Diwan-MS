// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Diwan';

  @override
  String get signIn => 'Sign in';

  @override
  String get createAccount => 'Create account';

  @override
  String get fullName => 'Full name';

  @override
  String get email => 'Email';

  @override
  String get password => 'Password';

  @override
  String get passwordHint => 'At least 8 characters';

  @override
  String get noAccount => 'No account yet? Create one';

  @override
  String get haveAccount => 'Already have an account? Sign in';

  @override
  String get logout => 'Sign out';

  @override
  String get portfolios => 'Portfolios';

  @override
  String get watchlist => 'Watchlist';

  @override
  String get news => 'News';

  @override
  String get alerts => 'Alerts';

  @override
  String get settings => 'Settings';

  @override
  String get newPortfolio => 'New portfolio';

  @override
  String get editPortfolio => 'Edit portfolio';

  @override
  String get deletePortfolio => 'Delete portfolio';

  @override
  String deletePortfolioConfirm(String name) {
    return 'Delete \"$name\" and all its investments? This cannot be undone.';
  }

  @override
  String get name => 'Name';

  @override
  String get description => 'Description';

  @override
  String get baseCurrency => 'Base currency';

  @override
  String get save => 'Save';

  @override
  String get cancel => 'Cancel';

  @override
  String get delete => 'Delete';

  @override
  String get retry => 'Try again';

  @override
  String get required => 'Required';

  @override
  String get invalidNumber => 'Enter a valid number';

  @override
  String get networkError =>
      'Cannot reach the server. Check your connection and try again.';

  @override
  String get sessionExpired => 'Your session has ended. Please sign in again.';

  @override
  String get totalValue => 'Total value';

  @override
  String get today => 'Today';

  @override
  String get unrealizedPnl => 'Unrealized P/L';

  @override
  String get realizedPnl => 'Realized P/L';

  @override
  String get totalReturn => 'Total return';

  @override
  String get income => 'Net income';

  @override
  String get costBasis => 'Cost';

  @override
  String holdingsCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count investments',
      one: '1 investment',
      zero: 'No investments',
    );
    return '$_temp0';
  }

  @override
  String get overview => 'Overview';

  @override
  String get holdings => 'Investments';

  @override
  String get allocation => 'Allocation';

  @override
  String get goals => 'Goals';

  @override
  String get byCountry => 'By country';

  @override
  String get byType => 'By type';

  @override
  String get byCurrency => 'By currency';

  @override
  String get addInvestment => 'Add investment';

  @override
  String get editInvestment => 'Edit investment';

  @override
  String get assetType => 'Type';

  @override
  String get country => 'Country';

  @override
  String get currency => 'Currency';

  @override
  String get symbol => 'Market symbol';

  @override
  String get symbolHint => 'e.g. AAPL - search by name or symbol';

  @override
  String get search => 'Search';

  @override
  String get quantity => 'Quantity';

  @override
  String get price => 'Price per unit';

  @override
  String get amount => 'Amount';

  @override
  String get fees => 'Fees';

  @override
  String get date => 'Date';

  @override
  String get note => 'Note';

  @override
  String get notes => 'Notes';

  @override
  String get details => 'Details';

  @override
  String get address => 'Address';

  @override
  String get firstTransaction => 'First transaction (optional)';

  @override
  String get addTransaction => 'Add transaction';

  @override
  String get transactions => 'Transactions';

  @override
  String get valuations => 'Valuations';

  @override
  String get addValuation => 'Add valuation';

  @override
  String get value => 'Value';

  @override
  String get averageCost => 'Average cost';

  @override
  String get marketValue => 'Market value';

  @override
  String get weight => 'Weight';

  @override
  String priceFrom(String source) {
    return 'Price: $source';
  }

  @override
  String get sourceQuote => 'market quote';

  @override
  String get sourceValuation => 'your valuation';

  @override
  String get sourceCost => 'purchase cost';

  @override
  String get sourceCash => 'balance';

  @override
  String get closeHolding => 'Archive';

  @override
  String get reopenHolding => 'Restore';

  @override
  String get deleteHolding => 'Delete investment';

  @override
  String get confirmDelete => 'Delete this? This cannot be undone.';

  @override
  String get targets => 'Targets';

  @override
  String get editTargets => 'Set targets';

  @override
  String get targetsHint =>
      'Percentages must add up to 100. Leave all empty to remove the targets.';

  @override
  String targetsSum(String sum) {
    return 'Total: $sum%';
  }

  @override
  String get current => 'Current';

  @override
  String get target => 'Target';

  @override
  String get drift => 'Drift';

  @override
  String buyMore(String amount) {
    return 'Add $amount';
  }

  @override
  String reduce(String amount) {
    return 'Reduce $amount';
  }

  @override
  String get noTargets =>
      'No targets yet. Set a target allocation to see how far you are from it.';

  @override
  String get newGoal => 'New goal';

  @override
  String get targetAmount => 'Target amount';

  @override
  String get targetDate => 'Target date';

  @override
  String monthlyNeeded(String amount) {
    return '$amount per month';
  }

  @override
  String get goalReached => 'Reached';

  @override
  String get noPortfolios => 'No portfolios yet. Create your first one.';

  @override
  String get noHoldings => 'No investments yet.';

  @override
  String get noNews => 'No news yet for your investments.';

  @override
  String get noAlerts => 'No alerts.';

  @override
  String get noWatchlist => 'Follow a symbol to see its price here.';

  @override
  String get noRules => 'No alert rules.';

  @override
  String get alertRules => 'Alert rules';

  @override
  String get newAlertRule => 'New alert rule';

  @override
  String get threshold => 'Threshold';

  @override
  String get thresholdPercent => 'Threshold (%)';

  @override
  String get markAllRead => 'Mark all as read';

  @override
  String get addSymbol => 'Follow a symbol';

  @override
  String get investment => 'Investment';

  @override
  String get portfolio => 'Portfolio';

  @override
  String get language => 'Language';

  @override
  String get unlockWithBiometrics => 'Unlock with fingerprint / face';

  @override
  String get unlockReason => 'Unlock Diwan';

  @override
  String get unlock => 'Unlock';

  @override
  String get about => 'About';

  @override
  String get fxAttribution =>
      'Exchange rates by ExchangeRate-API (exchangerate-api.com)';

  @override
  String get demoData =>
      'Demo market data: the server has no market data key yet.';

  @override
  String marketData(String provider) {
    return 'Market data: $provider';
  }

  @override
  String missingRates(String currencies) {
    return 'No exchange rate for $currencies; not included in totals.';
  }

  @override
  String get history => 'Price history';

  @override
  String get noHistory => 'Price history builds up day by day.';

  @override
  String get readMore => 'Read article';

  @override
  String get dashboard => 'Home';

  @override
  String hello(String name) {
    return 'Hello, $name';
  }

  @override
  String get featureNotInApp => 'This section is not available in the app yet.';

  @override
  String get featureSmartHome => 'Smart Home';

  @override
  String get featureSmartHomeDesc => 'Your devices: switch them on and off';

  @override
  String get featureMedical => 'Medical History';

  @override
  String get featureMedicalDesc =>
      'Medications, allergies, lab results and more';

  @override
  String get featureTransactions => 'Financial History';

  @override
  String get featureTransactionsDesc => 'Income and expenses';

  @override
  String get featureAccount => 'My Account';

  @override
  String get featureAccountDesc => 'Profile, language and security';

  @override
  String get featurePortfolio => 'Investments';

  @override
  String get featurePortfolioDesc => 'Portfolios, prices, alerts and news';

  @override
  String get all => 'All';

  @override
  String get incomeType => 'Income';

  @override
  String get expenseType => 'Expense';

  @override
  String get net => 'Net';

  @override
  String get newTransaction => 'New transaction';

  @override
  String get editTransaction => 'Edit transaction';

  @override
  String get noMoneyTransactions => 'No transactions yet.';

  @override
  String get category => 'Category';

  @override
  String get merchant => 'Merchant';

  @override
  String get bank => 'Bank';

  @override
  String get paymentMethod => 'Payment method';

  @override
  String get status => 'Status';

  @override
  String get notEditable => 'Cannot be changed after saving';

  @override
  String get catFood => 'Food';

  @override
  String get catTransport => 'Transport';

  @override
  String get catBills => 'Bills';

  @override
  String get catHealth => 'Health';

  @override
  String get catShopping => 'Shopping';

  @override
  String get catEntertainment => 'Entertainment';

  @override
  String get catSalary => 'Salary';

  @override
  String get catGeneral => 'General';

  @override
  String get payCash => 'Cash';

  @override
  String get payCard => 'Card';

  @override
  String get payTransfer => 'Transfer';

  @override
  String get payWallet => 'Wallet';

  @override
  String get payCheque => 'Cheque';

  @override
  String get statusCompleted => 'Completed';

  @override
  String get statusPending => 'Pending';

  @override
  String get statusCancelled => 'Cancelled';

  @override
  String get newRecord => 'New record';

  @override
  String get editRecord => 'Edit record';

  @override
  String get noMedicalRecords => 'No records in this section yet.';

  @override
  String get medMedications => 'Medications';

  @override
  String get medAllergies => 'Allergies';

  @override
  String get medBloodType => 'Blood type';

  @override
  String get medSurgeries => 'Surgeries';

  @override
  String get medChronicDiseases => 'Chronic diseases';

  @override
  String get medLabResults => 'Lab results';

  @override
  String get medVaccines => 'Vaccines';

  @override
  String get medFamilyHistory => 'Family history';

  @override
  String get fDosage => 'Dosage';

  @override
  String get fFrequency => 'Frequency';

  @override
  String get fStartDate => 'Start date';

  @override
  String get fEndDate => 'End date';

  @override
  String get fAllergen => 'Allergen';

  @override
  String get fSeverity => 'Severity';

  @override
  String get fReaction => 'Reaction';

  @override
  String get fSurgery => 'Surgery';

  @override
  String get fHospital => 'Hospital';

  @override
  String get fSurgeon => 'Surgeon';

  @override
  String get fDisease => 'Disease';

  @override
  String get fDiagnosedAt => 'Diagnosed on';

  @override
  String get fTreatment => 'Current treatment';

  @override
  String get fTest => 'Test';

  @override
  String get fResult => 'Result';

  @override
  String get fUnit => 'Unit';

  @override
  String get fReferenceRange => 'Reference range';

  @override
  String get fVaccine => 'Vaccine';

  @override
  String get fDose => 'Dose number';

  @override
  String get fNextDose => 'Next dose';

  @override
  String get fCondition => 'Condition';

  @override
  String get fRelation => 'Relation';

  @override
  String get devices => 'Devices';

  @override
  String get addDevice => 'Add device';

  @override
  String get deviceId => 'Device ID';

  @override
  String get deviceIdHint => 'Printed on the device / its QR code';

  @override
  String get rename => 'Rename';

  @override
  String get remove => 'Remove';

  @override
  String get noDevices => 'No devices yet. Add one with the ID printed on it.';

  @override
  String get deviceOn => 'On';

  @override
  String get deviceOff => 'Off';

  @override
  String get deviceUnknown => 'Unknown';

  @override
  String get alexaHint => 'You can also control these devices with Alexa.';

  @override
  String get account => 'Account';

  @override
  String get deactivateAccount => 'Deactivate account';

  @override
  String get deactivateConfirm =>
      'Deactivate your account? You will be signed out and cannot sign in until it is reactivated.';

  @override
  String get deleteAccount => 'Delete account';

  @override
  String get deleteAccountConfirm =>
      'Delete your account and all your data (investments, transactions, medical records)? This cannot be undone.';

  @override
  String get deactivate => 'Deactivate';

  @override
  String get transferType => 'Transfer';

  @override
  String get catTransfer => 'Transfer';

  @override
  String get catLoan => 'Loan';

  @override
  String get catCash => 'Cash';

  @override
  String get catRefund => 'Refund';

  @override
  String get catProfit => 'Profit';

  @override
  String get fromSms => 'From SMS';

  @override
  String get smsLogging => 'Log bank SMS automatically';

  @override
  String get smsLoggingHint =>
      'Al Rajhi Bank transactions are added to Financial History.';

  @override
  String get smsDisclosureTitle => 'Read your bank SMS?';

  @override
  String get smsDisclosure =>
      'Diwan reads the SMS messages your bank (Al Rajhi) sends you, also while the app is closed, and adds your purchases, transfers, salary and other transactions to Financial History.\n\nOnly the transaction details are sent to your Diwan account: amount, currency, merchant or person, date, the last 4 digits of the card or account, and the balance. The message text, OTP codes and all your other SMS stay on your phone.\n\nYou can turn this off at any time in My Account.';

  @override
  String get continueLabel => 'Continue';

  @override
  String get notNow => 'Not now';

  @override
  String get smsPermissionDenied =>
      'Diwan needs permission to read SMS for this.';

  @override
  String get openSettings => 'Open settings';

  @override
  String get smsHistoryTitle => 'Import earlier messages?';

  @override
  String get smsHistoryNone => 'Only new messages';

  @override
  String get smsHistory3 => 'Last 3 months';

  @override
  String get smsHistory12 => 'Last 12 months';

  @override
  String get smsSyncNow => 'Sync now';

  @override
  String smsLastSync(String date) {
    return 'Last sync: $date';
  }

  @override
  String smsImported(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count transactions added',
      one: '1 transaction added',
      zero: 'No transactions added yet',
    );
    return '$_temp0';
  }

  @override
  String smsUnrecognized(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count bank messages not recognized',
      one: '1 bank message not recognized',
    );
    return '$_temp0';
  }

  @override
  String smsSyncResult(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count new transactions',
      one: '1 new transaction',
      zero: 'No new transactions',
    );
    return '$_temp0';
  }

  @override
  String get cardsAndAccounts => 'Cards & accounts';

  @override
  String get positionsEmpty =>
      'Your cards, accounts and loans appear here once bank SMS are logged. Turn it on in My Account.';

  @override
  String accountTitle(String ref) {
    return 'Account ••$ref';
  }

  @override
  String cardTitle(String network, String ref) {
    return '$network ••$ref';
  }

  @override
  String get loanTitle => 'Loan';

  @override
  String get accounts => 'Accounts';

  @override
  String get cards => 'Cards';

  @override
  String get loans => 'Loans';

  @override
  String balanceEstimated(String date) {
    return 'Estimated from SMS since $date';
  }

  @override
  String get enterBalance => 'Tap to enter the current balance';

  @override
  String monthInOut(String moneyIn, String moneyOut) {
    return 'This month: $moneyIn in, $moneyOut out';
  }

  @override
  String availableAt(String amount, String date) {
    return 'Available $amount · $date';
  }

  @override
  String owedOfLimit(String owed, String limit) {
    return 'Owed $owed of $limit';
  }

  @override
  String get enterLimit => 'Tap to enter the credit limit';

  @override
  String linkedAccount(String ref) {
    return 'Debit card of account ••$ref';
  }

  @override
  String spentThisMonth(String amount) {
    return 'Spent this month: $amount';
  }

  @override
  String lastInstalment(String amount, String date) {
    return 'Last instalment $amount · $date';
  }

  @override
  String get remaining => 'Remaining';

  @override
  String get available => 'Available';

  @override
  String get currentBalance => 'Current balance';

  @override
  String get currentBalanceHint =>
      'As your banking app shows it now; the SMS that follow keep it up to date.';

  @override
  String get creditLimit => 'Credit limit';

  @override
  String get nickname => 'Name (optional)';

  @override
  String get filters => 'Filters';

  @override
  String get clearAll => 'Clear all';

  @override
  String get groupBy => 'Group by';

  @override
  String get groupNone => 'No grouping';

  @override
  String get groupDay => 'Day';

  @override
  String get groupMonth => 'Month';

  @override
  String get groupProduct => 'Card / account';

  @override
  String get period => 'Period';

  @override
  String get periodAll => 'All time';

  @override
  String get periodThisMonth => 'This month';

  @override
  String get periodLastMonth => 'Last month';

  @override
  String get periodLast3Months => 'Last 3 months';

  @override
  String get periodThisYear => 'This year';

  @override
  String get periodCustom => 'Choose dates';

  @override
  String get txSource => 'Source';

  @override
  String get sourceSms => 'Bank SMS';

  @override
  String get sourceManual => 'Entered by me';

  @override
  String get minAmount => 'Min amount';

  @override
  String get maxAmount => 'Max amount';

  @override
  String get card => 'Card';

  @override
  String get otherGroup => 'Other';

  @override
  String get yesterday => 'Yesterday';

  @override
  String get searchTransactions => 'Search merchant, description, notes';

  @override
  String get noMatchingTransactions => 'No transactions match these filters.';

  @override
  String txCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count transactions',
      one: '1 transaction',
    );
    return '$_temp0';
  }

  @override
  String showTransactions(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Show $count transactions',
      one: 'Show 1 transaction',
      zero: 'No transactions',
    );
    return '$_temp0';
  }
}
