package com.diwan.diwan_mobile

import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine

// FlutterFragmentActivity: required by local_auth for the fingerprint / face prompt
class MainActivity : FlutterFragmentActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        SmsChannel.register(flutterEngine.dartExecutor.binaryMessenger, this)
    }
}
