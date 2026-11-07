import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// A name and phone number chosen in the phone's own contact picker.
class PickedContact {
  const PickedContact(this.name, this.phone);

  final String? name;
  final String phone;
}

/// The system contact picker (Android). It returns only the contact the user picks, so the app needs no
/// permission to read the address book.
class ContactPicker {
  static const _channel = MethodChannel('diwan/contacts');

  @visibleForTesting
  static bool? availableOverride;

  static bool get available => availableOverride ?? (!kIsWeb && Platform.isAndroid);

  /// Null when the user goes back without choosing.
  static Future<PickedContact?> pickPhone() async {
    final r = await _channel.invokeMapMethod<String, String?>('pickPhone');
    final phone = r?['phone'];
    if (phone == null || phone.trim().isEmpty) return null;
    return PickedContact(r?['name'], cleanPhone(phone));
  }

  /// Contacts may hold formatting the server does not accept (invisible direction marks, dots, slashes).
  static String cleanPhone(String raw) =>
      raw.replaceAll(RegExp(r'[^+0-9 ()-]'), '').replaceAll(RegExp(r'\s+'), ' ').trim();
}
