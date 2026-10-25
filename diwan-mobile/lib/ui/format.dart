import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

/// Number, money and date formatting in the app's language (Arabic gets Arabic digits and RTL-aware symbols).
class Fmt {
  Fmt(BuildContext context) : locale = Localizations.localeOf(context).toLanguageTag();

  final String locale;

  String money(double? value, String currency, {bool signed = false}) {
    if (value == null) return '-';
    var f = NumberFormat.simpleCurrency(locale: locale, name: currency);
    // currencies without a sign get a word ("Riyal29,255.00"): use the code with a space instead
    if (RegExp(r'^[A-Za-z]+$').hasMatch(f.currencySymbol)) {
      f = NumberFormat.currency(locale: locale, name: currency, symbol: '$currency\u00A0', decimalDigits: f.decimalDigits);
    }
    final text = f.format(value.abs());
    if (value < 0) return '-$text';
    return signed && value > 0 ? '+$text' : text;
  }

  String compactMoney(double value, String currency) =>
      NumberFormat.compactSimpleCurrency(locale: locale, name: currency).format(value);

  String number(double? value, {int maxDecimals = 4}) {
    if (value == null) return '-';
    final f = NumberFormat.decimalPattern(locale)..maximumFractionDigits = maxDecimals;
    return f.format(value);
  }

  String percent(double? value, {bool signed = true}) {
    if (value == null) return '-';
    final f = NumberFormat.decimalPattern(locale)
      ..minimumFractionDigits = 2
      ..maximumFractionDigits = 2;
    final sign = signed && value > 0 ? '+' : '';
    return '$sign${f.format(value)}%';
  }

  String date(DateTime? d) => d == null ? '-' : DateFormat.yMMMd(locale).format(d);

  String dateTime(DateTime? d) => d == null ? '-' : DateFormat.yMMMd(locale).add_jm().format(d);

  static String isoDate(DateTime d) => DateFormat('yyyy-MM-dd').format(d);
}

/// Green for gains, red for losses.
Color pnlColor(BuildContext context, double? value) {
  if (value == null || value == 0) return Theme.of(context).colorScheme.onSurfaceVariant;
  return value > 0 ? const Color(0xFF15803D) : const Color(0xFFB91C1C);
}

/// Parses a number typed in either Western or Arabic-Indic digits ("١٢٫٥" -> "12.5"); null if invalid.
String? normalizeNumber(String input) {
  const arabic = '٠١٢٣٤٥٦٧٨٩';
  final b = StringBuffer();
  for (final ch in input.trim().split('')) {
    final i = arabic.indexOf(ch);
    if (i >= 0) {
      b.write(i);
    } else if (ch == '٫' || ch == ',') {
      b.write('.');
    } else if (ch == '٬' || ch == ' ') {
      continue;
    } else {
      b.write(ch);
    }
  }
  final s = b.toString();
  return double.tryParse(s) == null ? null : s;
}
