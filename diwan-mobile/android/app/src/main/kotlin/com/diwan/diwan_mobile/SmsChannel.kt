package com.diwan.diwan_mobile

import android.content.Context
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.Executors

/** `diwan/sms`: what lib/sms/sms_sync.dart (SmsPlatform) calls, in the app and in the background engine. */
object SmsChannel {
    private val io = Executors.newSingleThreadExecutor()

    fun register(messenger: BinaryMessenger, context: Context, onDone: ((Boolean) -> Unit)? = null) {
        val app = context.applicationContext
        MethodChannel(messenger, "diwan/sms").setMethodCallHandler { call, result ->
            when (call.method) {
                "inbox" -> {
                    val since = (call.argument<Number>("since") ?: 0).toLong()
                    // the content provider query can take a while for a year of messages
                    io.execute {
                        val list = try { SmsStore.inbox(app, since) } catch (e: Exception) { null }
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            if (list == null) result.error("inbox", "Cannot read SMS", null) else result.success(list)
                        }
                    }
                }
                "pending" -> result.success(SmsStore.pending(app))
                "clearPending" -> {
                    SmsStore.clearPending(app, call.argument<List<String>>("ids") ?: emptyList())
                    result.success(null)
                }
                "setEnabled" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    if (!enabled) SmsStore.clearAll(app)
                    SmsSyncWorker.schedule(app, enabled)
                    result.success(null)
                }
                "done" -> {
                    result.success(null)
                    onDone?.invoke(call.argument<Boolean>("ok") ?: false)
                }
                else -> result.notImplemented()
            }
        }
    }
}
