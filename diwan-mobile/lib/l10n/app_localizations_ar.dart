// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Arabic (`ar`).
class AppLocalizationsAr extends AppLocalizations {
  AppLocalizationsAr([String locale = 'ar']) : super(locale);

  @override
  String get appTitle => 'ديوان للاستثمار';

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
}
