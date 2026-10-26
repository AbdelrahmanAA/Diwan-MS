// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Arabic (`ar`).
class AppLocalizationsAr extends AppLocalizations {
  AppLocalizationsAr([String locale = 'ar']) : super(locale);

  @override
  String get appTitle => 'ديوان';

  @override
  String get signIn => 'تسجيل الدخول';

  @override
  String get createAccount => 'إنشاء حساب';

  @override
  String get fullName => 'الاسم الكامل';

  @override
  String get email => 'البريد الإلكتروني';

  @override
  String get password => 'كلمة المرور';

  @override
  String get passwordHint => '8 أحرف على الأقل';

  @override
  String get noAccount => 'ليس لديك حساب؟ أنشئ حسابًا';

  @override
  String get haveAccount => 'لديك حساب بالفعل؟ سجّل الدخول';

  @override
  String get logout => 'تسجيل الخروج';

  @override
  String get portfolios => 'المحافظ';

  @override
  String get watchlist => 'المتابعة';

  @override
  String get news => 'الأخبار';

  @override
  String get alerts => 'التنبيهات';

  @override
  String get settings => 'الإعدادات';

  @override
  String get newPortfolio => 'محفظة جديدة';

  @override
  String get editPortfolio => 'تعديل المحفظة';

  @override
  String get deletePortfolio => 'حذف المحفظة';

  @override
  String deletePortfolioConfirm(String name) {
    return 'حذف \"$name\" وكل استثماراتها؟ لا يمكن التراجع عن ذلك.';
  }

  @override
  String get name => 'الاسم';

  @override
  String get description => 'الوصف';

  @override
  String get baseCurrency => 'العملة الأساسية';

  @override
  String get save => 'حفظ';

  @override
  String get cancel => 'إلغاء';

  @override
  String get delete => 'حذف';

  @override
  String get retry => 'إعادة المحاولة';

  @override
  String get required => 'مطلوب';

  @override
  String get invalidNumber => 'أدخل رقمًا صحيحًا';

  @override
  String get networkError =>
      'تعذر الاتصال بالخادم. تحقق من اتصالك وحاول مرة أخرى.';

  @override
  String get sessionExpired => 'انتهت جلستك. يرجى تسجيل الدخول مرة أخرى.';

  @override
  String get totalValue => 'القيمة الإجمالية';

  @override
  String get today => 'اليوم';

  @override
  String get unrealizedPnl => 'ربح/خسارة غير محققة';

  @override
  String get realizedPnl => 'ربح/خسارة محققة';

  @override
  String get totalReturn => 'العائد الإجمالي';

  @override
  String get income => 'صافي الدخل';

  @override
  String get costBasis => 'التكلفة';

  @override
  String holdingsCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count استثمار',
      many: '$count استثمارًا',
      few: '$count استثمارات',
      two: 'استثماران',
      one: 'استثمار واحد',
      zero: 'لا توجد استثمارات',
    );
    return '$_temp0';
  }

  @override
  String get overview => 'نظرة عامة';

  @override
  String get holdings => 'الاستثمارات';

  @override
  String get allocation => 'التوزيع';

  @override
  String get goals => 'الأهداف';

  @override
  String get byCountry => 'حسب الدولة';

  @override
  String get byType => 'حسب النوع';

  @override
  String get byCurrency => 'حسب العملة';

  @override
  String get addInvestment => 'إضافة استثمار';

  @override
  String get editInvestment => 'تعديل الاستثمار';

  @override
  String get assetType => 'النوع';

  @override
  String get country => 'الدولة';

  @override
  String get currency => 'العملة';

  @override
  String get symbol => 'رمز السوق';

  @override
  String get symbolHint => 'مثل AAPL - ابحث بالاسم أو الرمز';

  @override
  String get search => 'بحث';

  @override
  String get quantity => 'الكمية';

  @override
  String get price => 'سعر الوحدة';

  @override
  String get amount => 'المبلغ';

  @override
  String get fees => 'الرسوم';

  @override
  String get date => 'التاريخ';

  @override
  String get note => 'ملاحظة';

  @override
  String get notes => 'ملاحظات';

  @override
  String get details => 'التفاصيل';

  @override
  String get address => 'العنوان';

  @override
  String get firstTransaction => 'أول عملية (اختياري)';

  @override
  String get addTransaction => 'إضافة عملية';

  @override
  String get transactions => 'العمليات';

  @override
  String get valuations => 'التقييمات';

  @override
  String get addValuation => 'إضافة تقييم';

  @override
  String get value => 'القيمة';

  @override
  String get averageCost => 'متوسط التكلفة';

  @override
  String get marketValue => 'القيمة السوقية';

  @override
  String get weight => 'الوزن';

  @override
  String priceFrom(String source) {
    return 'السعر: $source';
  }

  @override
  String get sourceQuote => 'سعر السوق';

  @override
  String get sourceValuation => 'تقييمك';

  @override
  String get sourceCost => 'تكلفة الشراء';

  @override
  String get sourceCash => 'الرصيد';

  @override
  String get closeHolding => 'أرشفة';

  @override
  String get reopenHolding => 'استعادة';

  @override
  String get deleteHolding => 'حذف الاستثمار';

  @override
  String get confirmDelete => 'هل تريد الحذف؟ لا يمكن التراجع عن ذلك.';

  @override
  String get targets => 'الأهداف المستهدفة';

  @override
  String get editTargets => 'تحديد النسب المستهدفة';

  @override
  String get targetsHint =>
      'يجب أن يكون مجموع النسب 100. اترك الكل فارغًا لإزالة الأهداف.';

  @override
  String targetsSum(String sum) {
    return 'المجموع: $sum%';
  }

  @override
  String get current => 'الحالي';

  @override
  String get target => 'المستهدف';

  @override
  String get drift => 'الانحراف';

  @override
  String buyMore(String amount) {
    return 'أضف $amount';
  }

  @override
  String reduce(String amount) {
    return 'خفّض $amount';
  }

  @override
  String get noTargets =>
      'لا توجد أهداف بعد. حدد توزيعًا مستهدفًا لترى مدى ابتعادك عنه.';

  @override
  String get newGoal => 'هدف جديد';

  @override
  String get targetAmount => 'المبلغ المستهدف';

  @override
  String get targetDate => 'التاريخ المستهدف';

  @override
  String monthlyNeeded(String amount) {
    return '$amount شهريًا';
  }

  @override
  String get goalReached => 'تحقق';

  @override
  String get noPortfolios => 'لا توجد محافظ بعد. أنشئ محفظتك الأولى.';

  @override
  String get noHoldings => 'لا توجد استثمارات بعد.';

  @override
  String get noNews => 'لا توجد أخبار بعد عن استثماراتك.';

  @override
  String get noAlerts => 'لا توجد تنبيهات.';

  @override
  String get noWatchlist => 'تابع رمزًا لترى سعره هنا.';

  @override
  String get noRules => 'لا توجد قواعد تنبيه.';

  @override
  String get alertRules => 'قواعد التنبيه';

  @override
  String get newAlertRule => 'قاعدة تنبيه جديدة';

  @override
  String get threshold => 'الحد';

  @override
  String get thresholdPercent => 'الحد (%)';

  @override
  String get markAllRead => 'تعليم الكل كمقروء';

  @override
  String get addSymbol => 'متابعة رمز';

  @override
  String get investment => 'الاستثمار';

  @override
  String get portfolio => 'المحفظة';

  @override
  String get language => 'اللغة';

  @override
  String get unlockWithBiometrics => 'الفتح بالبصمة / الوجه';

  @override
  String get unlockReason => 'فتح ديوان';

  @override
  String get unlock => 'فتح';

  @override
  String get about => 'حول التطبيق';

  @override
  String get fxAttribution =>
      'أسعار الصرف من ExchangeRate-API (exchangerate-api.com)';

  @override
  String get demoData =>
      'بيانات سوق تجريبية: لا يوجد مفتاح بيانات سوق على الخادم بعد.';

  @override
  String marketData(String provider) {
    return 'بيانات السوق: $provider';
  }

  @override
  String missingRates(String currencies) {
    return 'لا يوجد سعر صرف لـ $currencies؛ غير مضمنة في الإجماليات.';
  }

  @override
  String get history => 'تاريخ السعر';

  @override
  String get noHistory => 'يتكوّن تاريخ السعر يومًا بعد يوم.';

  @override
  String get readMore => 'قراءة المقال';

  @override
  String get dashboard => 'الرئيسية';

  @override
  String hello(String name) {
    return 'أهلاً، $name';
  }

  @override
  String get featureNotInApp => 'هذا القسم غير متاح في التطبيق بعد.';

  @override
  String get featureSmartHome => 'المنزل الذكي';

  @override
  String get featureSmartHomeDesc => 'أجهزتك: شغّلها وأطفئها';

  @override
  String get featureMedical => 'السجل الطبي';

  @override
  String get featureMedicalDesc => 'الأدوية والحساسية ونتائج التحاليل وغيرها';

  @override
  String get featureTransactions => 'السجل المالي';

  @override
  String get featureTransactionsDesc => 'الدخل والمصروفات';

  @override
  String get featureAccount => 'حسابي';

  @override
  String get featureAccountDesc => 'الملف الشخصي واللغة والأمان';

  @override
  String get featurePortfolio => 'الاستثمارات';

  @override
  String get featurePortfolioDesc => 'المحافظ والأسعار والتنبيهات والأخبار';

  @override
  String get all => 'الكل';

  @override
  String get incomeType => 'دخل';

  @override
  String get expenseType => 'مصروف';

  @override
  String get net => 'الصافي';

  @override
  String get newTransaction => 'معاملة جديدة';

  @override
  String get editTransaction => 'تعديل المعاملة';

  @override
  String get noMoneyTransactions => 'لا توجد معاملات بعد.';

  @override
  String get category => 'الفئة';

  @override
  String get merchant => 'التاجر';

  @override
  String get bank => 'البنك';

  @override
  String get paymentMethod => 'طريقة الدفع';

  @override
  String get status => 'الحالة';

  @override
  String get notEditable => 'لا يمكن تغييره بعد الحفظ';

  @override
  String get catFood => 'طعام';

  @override
  String get catTransport => 'مواصلات';

  @override
  String get catBills => 'فواتير';

  @override
  String get catHealth => 'صحة';

  @override
  String get catShopping => 'تسوق';

  @override
  String get catEntertainment => 'ترفيه';

  @override
  String get catSalary => 'راتب';

  @override
  String get catGeneral => 'عام';

  @override
  String get payCash => 'نقداً';

  @override
  String get payCard => 'بطاقة';

  @override
  String get payTransfer => 'تحويل';

  @override
  String get payWallet => 'محفظة إلكترونية';

  @override
  String get payCheque => 'شيك';

  @override
  String get statusCompleted => 'مكتملة';

  @override
  String get statusPending => 'قيد الانتظار';

  @override
  String get statusCancelled => 'ملغاة';

  @override
  String get newRecord => 'سجل جديد';

  @override
  String get editRecord => 'تعديل السجل';

  @override
  String get noMedicalRecords => 'لا توجد سجلات في هذا القسم بعد.';

  @override
  String get medMedications => 'الأدوية';

  @override
  String get medAllergies => 'الحساسية';

  @override
  String get medBloodType => 'فصيلة الدم';

  @override
  String get medSurgeries => 'العمليات الجراحية';

  @override
  String get medChronicDiseases => 'الأمراض المزمنة';

  @override
  String get medLabResults => 'نتائج التحاليل';

  @override
  String get medVaccines => 'التطعيمات';

  @override
  String get medFamilyHistory => 'التاريخ العائلي';

  @override
  String get fDosage => 'الجرعة';

  @override
  String get fFrequency => 'عدد المرات';

  @override
  String get fStartDate => 'تاريخ البدء';

  @override
  String get fEndDate => 'تاريخ الانتهاء';

  @override
  String get fAllergen => 'مسبب الحساسية';

  @override
  String get fSeverity => 'الشدة';

  @override
  String get fReaction => 'رد الفعل';

  @override
  String get fSurgery => 'العملية';

  @override
  String get fHospital => 'المستشفى';

  @override
  String get fSurgeon => 'الجراح';

  @override
  String get fDisease => 'المرض';

  @override
  String get fDiagnosedAt => 'تاريخ التشخيص';

  @override
  String get fTreatment => 'العلاج الحالي';

  @override
  String get fTest => 'التحليل';

  @override
  String get fResult => 'النتيجة';

  @override
  String get fUnit => 'الوحدة';

  @override
  String get fReferenceRange => 'المعدل الطبيعي';

  @override
  String get fVaccine => 'التطعيم';

  @override
  String get fDose => 'رقم الجرعة';

  @override
  String get fNextDose => 'الجرعة التالية';

  @override
  String get fCondition => 'الحالة المرضية';

  @override
  String get fRelation => 'صلة القرابة';

  @override
  String get devices => 'الأجهزة';

  @override
  String get addDevice => 'إضافة جهاز';

  @override
  String get deviceId => 'معرّف الجهاز';

  @override
  String get deviceIdHint => 'مطبوع على الجهاز / رمز QR الخاص به';

  @override
  String get rename => 'إعادة تسمية';

  @override
  String get remove => 'إزالة';

  @override
  String get noDevices =>
      'لا توجد أجهزة بعد. أضف جهازاً بالمعرّف المطبوع عليه.';

  @override
  String get deviceOn => 'يعمل';

  @override
  String get deviceOff => 'مطفأ';

  @override
  String get deviceUnknown => 'غير معروف';

  @override
  String get alexaHint => 'يمكنك أيضاً التحكم في هذه الأجهزة عبر Alexa.';

  @override
  String get account => 'الحساب';

  @override
  String get deactivateAccount => 'إيقاف الحساب';

  @override
  String get deactivateConfirm =>
      'إيقاف حسابك؟ سيتم تسجيل خروجك ولن تتمكن من الدخول حتى يُعاد تفعيله.';

  @override
  String get deleteAccount => 'حذف الحساب';

  @override
  String get deleteAccountConfirm =>
      'حذف حسابك وكل بياناتك (الاستثمارات والمعاملات والسجلات الطبية)؟ لا يمكن التراجع عن ذلك.';

  @override
  String get deactivate => 'إيقاف';

  @override
  String get transferType => 'تحويل';

  @override
  String get catTransfer => 'تحويل';

  @override
  String get catLoan => 'قرض';

  @override
  String get catCash => 'نقد';

  @override
  String get catRefund => 'استرداد';

  @override
  String get catProfit => 'أرباح';

  @override
  String get fromSms => 'من رسالة';

  @override
  String get smsLogging => 'تسجيل رسائل البنك تلقائياً';

  @override
  String get smsLoggingHint => 'تُضاف معاملات مصرف الراجحي إلى السجل المالي.';

  @override
  String get smsDisclosureTitle => 'قراءة رسائل البنك؟';

  @override
  String get smsDisclosure =>
      'يقرأ ديوان الرسائل النصية التي يرسلها لك البنك (الراجحي)، حتى والتطبيق مغلق، ويضيف مشترياتك وتحويلاتك وراتبك وباقي معاملاتك إلى السجل المالي.\n\nتُرسل إلى حسابك في ديوان تفاصيل المعاملة فقط: المبلغ والعملة والتاجر أو الشخص والتاريخ وآخر 4 أرقام من البطاقة أو الحساب والرصيد. أما نص الرسالة ورموز التحقق (OTP) وباقي رسائلك فتبقى على هاتفك.\n\nيمكنك إيقاف ذلك في أي وقت من صفحة حسابي.';

  @override
  String get continueLabel => 'متابعة';

  @override
  String get notNow => 'ليس الآن';

  @override
  String get smsPermissionDenied => 'يحتاج ديوان إذن قراءة الرسائل لذلك.';

  @override
  String get openSettings => 'فتح الإعدادات';

  @override
  String get smsHistoryTitle => 'استيراد الرسائل السابقة؟';

  @override
  String get smsHistoryNone => 'الرسائل الجديدة فقط';

  @override
  String get smsHistory3 => 'آخر 3 أشهر';

  @override
  String get smsHistory12 => 'آخر 12 شهراً';

  @override
  String get smsSyncNow => 'مزامنة الآن';

  @override
  String smsLastSync(String date) {
    return 'آخر مزامنة: $date';
  }

  @override
  String smsImported(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'أُضيفت $count معاملة',
      few: 'أُضيفت $count معاملات',
      two: 'أُضيفت معاملتان',
      one: 'أُضيفت معاملة واحدة',
      zero: 'لم تُضف معاملات بعد',
    );
    return '$_temp0';
  }

  @override
  String smsUnrecognized(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count رسالة بنكية غير معروفة',
      few: '$count رسائل بنكية غير معروفة',
      two: 'رسالتان بنكيتان غير معروفتين',
      one: 'رسالة بنكية واحدة غير معروفة',
    );
    return '$_temp0';
  }

  @override
  String smsSyncResult(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count معاملة جديدة',
      few: '$count معاملات جديدة',
      two: 'معاملتان جديدتان',
      one: 'معاملة جديدة واحدة',
      zero: 'لا توجد معاملات جديدة',
    );
    return '$_temp0';
  }

  @override
  String get cardsAndAccounts => 'البطاقات والحسابات';

  @override
  String get positionsEmpty =>
      'تظهر هنا بطاقاتك وحساباتك وقروضك بعد تسجيل رسائل البنك. فعّل ذلك من صفحة حسابي.';

  @override
  String accountTitle(String ref) {
    return 'حساب ••$ref';
  }

  @override
  String cardTitle(String network, String ref) {
    return '$network ••$ref';
  }

  @override
  String get loanTitle => 'قرض';

  @override
  String get accounts => 'الحسابات';

  @override
  String get cards => 'البطاقات';

  @override
  String get loans => 'القروض';

  @override
  String balanceEstimated(String date) {
    return 'تقدير من الرسائل منذ $date';
  }

  @override
  String get enterBalance => 'اضغط لإدخال الرصيد الحالي';

  @override
  String monthInOut(String moneyIn, String moneyOut) {
    return 'هذا الشهر: $moneyIn وارد، $moneyOut صادر';
  }

  @override
  String availableAt(String amount, String date) {
    return 'المتاح $amount · $date';
  }

  @override
  String owedOfLimit(String owed, String limit) {
    return 'المستحق $owed من $limit';
  }

  @override
  String get enterLimit => 'اضغط لإدخال حد الائتمان';

  @override
  String linkedAccount(String ref) {
    return 'بطاقة خصم للحساب ••$ref';
  }

  @override
  String spentThisMonth(String amount) {
    return 'مصروف هذا الشهر: $amount';
  }

  @override
  String lastInstalment(String amount, String date) {
    return 'آخر قسط $amount · $date';
  }

  @override
  String get remaining => 'المتبقي';

  @override
  String get available => 'المتاح';

  @override
  String get currentBalance => 'الرصيد الحالي';

  @override
  String get currentBalanceHint =>
      'كما يظهر في تطبيق البنك الآن؛ والرسائل التالية تحدّثه.';

  @override
  String get creditLimit => 'حد الائتمان';

  @override
  String get nickname => 'الاسم (اختياري)';

  @override
  String get filters => 'التصفية';

  @override
  String get clearAll => 'مسح الكل';

  @override
  String get groupBy => 'تجميع حسب';

  @override
  String get groupNone => 'بدون تجميع';

  @override
  String get groupDay => 'يوم';

  @override
  String get groupMonth => 'شهر';

  @override
  String get groupProduct => 'البطاقة / الحساب';

  @override
  String get period => 'الفترة';

  @override
  String get periodAll => 'كل الأوقات';

  @override
  String get periodThisMonth => 'هذا الشهر';

  @override
  String get periodLastMonth => 'الشهر الماضي';

  @override
  String get periodLast3Months => 'آخر 3 أشهر';

  @override
  String get periodThisYear => 'هذه السنة';

  @override
  String get periodCustom => 'اختر التواريخ';

  @override
  String get txSource => 'المصدر';

  @override
  String get sourceSms => 'رسائل البنك';

  @override
  String get sourceManual => 'أدخلتها بنفسي';

  @override
  String get minAmount => 'أقل مبلغ';

  @override
  String get maxAmount => 'أعلى مبلغ';

  @override
  String get card => 'بطاقة';

  @override
  String get otherGroup => 'أخرى';

  @override
  String get yesterday => 'أمس';

  @override
  String get searchTransactions => 'ابحث في التاجر أو الوصف أو الملاحظات';

  @override
  String get noMatchingTransactions => 'لا توجد معاملات تطابق التصفية.';

  @override
  String txCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count معاملة',
      few: '$count معاملات',
      two: 'معاملتان',
      one: 'معاملة واحدة',
      zero: 'لا معاملات',
    );
    return '$_temp0';
  }

  @override
  String showTransactions(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'عرض $count معاملة',
      few: 'عرض $count معاملات',
      two: 'عرض معاملتين',
      one: 'عرض معاملة واحدة',
      zero: 'لا معاملات',
    );
    return '$_temp0';
  }
}
