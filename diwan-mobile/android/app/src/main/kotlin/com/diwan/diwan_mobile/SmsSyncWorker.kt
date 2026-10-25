package com.diwan.diwan_mobile

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Uploads new bank SMS in the background: starts a headless Flutter engine running `smsBackgroundSync`
 * (lib/main.dart), which reads the queue and the inbox and calls the API, then reports `done` on the
 * `diwan/sms` channel. Retried with backoff while there is no network.
 */
class SmsSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        if (!SmsStore.isEnabled(applicationContext)) return Result.success()
        val latch = CountDownLatch(1)
        var ok = false
        var engine: FlutterEngine? = null
        val main = Handler(Looper.getMainLooper())
        main.post {
            val loader = FlutterInjector.instance().flutterLoader()
            loader.startInitialization(applicationContext)
            loader.ensureInitializationCompleteAsync(applicationContext, null, main) {
                val e = FlutterEngine(applicationContext)
                engine = e
                SmsChannel.register(e.dartExecutor.binaryMessenger, applicationContext) { success ->
                    ok = success
                    latch.countDown()
                }
                e.dartExecutor.executeDartEntrypoint(
                    DartExecutor.DartEntrypoint(loader.findAppBundlePath(), "smsBackgroundSync"))
            }
        }
        val finished = latch.await(8, TimeUnit.MINUTES)
        main.post { engine?.destroy() }
        return if (finished && ok) Result.success() else Result.retry()
    }

    companion object {
        private const val NOW = "diwan-sms-sync"
        private const val PERIODIC = "diwan-sms-sync-periodic"

        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SmsSyncWorker>()
                .setConstraints(online)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        /** Safety net for messages the receiver missed (e.g. after the app was force-stopped). */
        fun schedule(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(PERIODIC)
                wm.cancelUniqueWork(NOW)
                return
            }
            val request = PeriodicWorkRequestBuilder<SmsSyncWorker>(6, TimeUnit.HOURS).setConstraints(online).build()
            wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
