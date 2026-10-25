package com.diwan.diwan_mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** New SMS: keeps bank messages (not OTPs) for upload and starts a sync, also when the app is closed. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION || !SmsStore.isEnabled(context)) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        var kept = false
        // a long SMS arrives in parts
        parts.filterNotNull().groupBy { it.originatingAddress ?: "" }.forEach { (sender, messages) ->
            val body = messages.joinToString("") { it.messageBody ?: "" }
            if (SmsStore.isBankSender(sender) && !SmsStore.isOtp(body)) {
                SmsStore.addPending(context, sender, body, System.currentTimeMillis())
                kept = true
            }
        }
        if (kept) SmsSyncWorker.syncNow(context)
    }
}
