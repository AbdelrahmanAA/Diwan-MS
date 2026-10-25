package com.diwan.diwan_mobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Bank SMS waiting to be uploaded, and the inbox reader. The receiver fires before the SMS app has written the
 * message to the inbox, so it keeps its own copy here (app-private storage) until the Dart sync uploaded it.
 * Only bank senders are kept and OTP messages never are.
 */
object SmsStore {
    // same lists as BankSmsParser.senders / BankSmsParser.isOtp (Dart)
    private val senders = listOf("alrajhi")
    private val otp = Regex("\\botp\\b|verification code|one[- ]time password", RegexOption.IGNORE_CASE)

    private const val PREFS = "diwan_sms"
    private const val PENDING = "pending"
    private const val MAX_PENDING = 500

    fun isBankSender(sender: String?): Boolean {
        val letters = (sender ?: "").lowercase().replace(Regex("[^a-z]"), "")
        return senders.any { letters.contains(it) }
    }

    fun isOtp(body: String) = otp.containsMatchIn(body)

    /** The switch in the app (shared_preferences stores it as "flutter." + key). */
    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            .getBoolean("flutter.diwan.sms.enabled", false)

    @Synchronized
    fun addPending(context: Context, sender: String, body: String, receivedAt: Long) {
        val list = read(context)
        list.put(JSONObject().put("id", UUID.randomUUID().toString()).put("sender", sender)
            .put("body", body).put("receivedAt", receivedAt))
        while (list.length() > MAX_PENDING) list.remove(0)
        write(context, list)
    }

    @Synchronized
    fun pending(context: Context): List<Map<String, Any>> {
        val list = read(context)
        return (0 until list.length()).map { i ->
            val o = list.getJSONObject(i)
            mapOf("id" to o.getString("id"), "sender" to o.getString("sender"),
                "body" to o.getString("body"), "receivedAt" to o.getLong("receivedAt"))
        }
    }

    @Synchronized
    fun clearPending(context: Context, ids: Collection<String>) {
        val list = read(context)
        val kept = JSONArray()
        for (i in 0 until list.length()) {
            val o = list.getJSONObject(i)
            if (o.getString("id") !in ids) kept.put(o)
        }
        write(context, kept)
    }

    @Synchronized
    fun clearAll(context: Context) = write(context, JSONArray())

    /** Bank SMS received since [sinceMillis] (empty without the READ_SMS permission). */
    fun inbox(context: Context, sinceMillis: Long): List<Map<String, Any>> {
        if (context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return emptyList()
        val result = mutableListOf<Map<String, Any>>()
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(sinceMillis.toString()),
            "${Telephony.Sms.DATE} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val sender = c.getString(0) ?: continue
                val body = c.getString(1) ?: continue
                if (!isBankSender(sender) || isOtp(body)) continue
                result.add(mapOf("sender" to sender, "body" to body, "receivedAt" to c.getLong(2)))
            }
        }
        return result
    }

    private fun read(context: Context): JSONArray =
        JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PENDING, "[]"))

    private fun write(context: Context, list: JSONArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(PENDING, list.toString()).apply()
    }
}
