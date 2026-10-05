import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../config.dart';

/// An error answered by the API ({"success":false,"code":...,"message":...}) or a network failure.
/// [message] is already in the user's language when it comes from the server.
class ApiException implements Exception {
  ApiException(this.status, this.message, {this.code});

  final int status;
  final String? code;
  final String message;

  bool get isUnauthorized => status == 401;
  bool get isNetwork => status == 0;

  @override
  String toString() => message;
}

/// Thin JSON-over-HTTP client for the Diwan gateway: adds the bearer token and Accept-Language to every
/// request and turns error bodies into [ApiException]s.
class ApiClient {
  ApiClient({
    required this.tokenProvider,
    required this.languageProvider,
    this.onUnauthorized,
    String? baseUrl,
    http.Client? httpClient,
  })  : baseUrl = baseUrl ?? AppConfig.apiBaseUrl,
        _http = httpClient ?? http.Client();

  final String baseUrl;
  final String? Function() tokenProvider;
  final String Function() languageProvider;
  final void Function()? onUnauthorized;
  final http.Client _http;

  Future<dynamic> get(String path, {Map<String, Object?>? query}) => _send('GET', path, query: query);

  Future<dynamic> post(String path, [Object? body]) => _send('POST', path, body: body);

  Future<dynamic> put(String path, [Object? body]) => _send('PUT', path, body: body);

  Future<dynamic> delete(String path) => _send('DELETE', path);

  Uri _uri(String path, Map<String, Object?>? query) {
    final params = <String, String>{};
    query?.forEach((k, v) {
      if (v != null && v.toString().isNotEmpty) params[k] = v.toString();
    });
    return Uri.parse('$baseUrl$path').replace(queryParameters: params.isEmpty ? null : params);
  }

  Future<dynamic> _send(String method, String path, {Map<String, Object?>? query, Object? body}) async {
    final request = http.Request(method, _uri(path, query));
    request.headers['Accept'] = 'application/json';
    request.headers['Accept-Language'] = languageProvider();
    final token = tokenProvider();
    if (token != null) request.headers['Authorization'] = 'Bearer $token';
    if (body != null) {
      request.headers['Content-Type'] = 'application/json; charset=utf-8';
      request.body = jsonEncode(body);
    }

    http.Response response;
    try {
      response = await http.Response.fromStream(await _http.send(request).timeout(AppConfig.requestTimeout));
    } on TimeoutException {
      throw ApiException(0, 'timeout');
    } on SocketException {
      throw ApiException(0, 'network');
    } on http.ClientException {
      throw ApiException(0, 'network');
    }

    final text = utf8.decode(response.bodyBytes);
    final dynamic json = text.isEmpty ? null : _tryDecode(text);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return json;
    }
    if (response.statusCode == 401 && token != null) {
      onUnauthorized?.call();
    }
    String message = 'HTTP ${response.statusCode}';
    String? code;
    if (json is Map) {
      message = (json['message'] ?? json['error'] ?? message).toString();
      code = json['code']?.toString();
    }
    throw ApiException(response.statusCode, message, code: code);
  }

  static dynamic _tryDecode(String text) {
    try {
      return jsonDecode(text);
    } on FormatException {
      return null;
    }
  }
}
