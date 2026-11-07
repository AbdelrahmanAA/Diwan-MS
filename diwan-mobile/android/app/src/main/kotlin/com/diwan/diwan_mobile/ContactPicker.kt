package com.diwan.diwan_mobile

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel

/**
 * Lets the user pick one phone number with the system contact picker. Android grants access to the picked
 * entry only, so the app needs no contacts permission and never sees the rest of the address book.
 * Must be created while the activity is being constructed (it registers an activity result launcher).
 */
class ContactPicker(private val activity: ComponentActivity) {

    private var pending: MethodChannel.Result? = null

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val result = pending ?: return@registerForActivityResult
        pending = null
        val uri = r.data?.data
        if (r.resultCode != Activity.RESULT_OK || uri == null) {
            result.success(null) // the user went back without choosing
            return@registerForActivityResult
        }
        try {
            activity.contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null).use { c ->
                result.success(if (c != null && c.moveToFirst()) mapOf("name" to c.getString(0), "phone" to c.getString(1)) else null)
            }
        } catch (e: Exception) {
            result.error("read_failed", e.message, null)
        }
    }

    fun register(messenger: BinaryMessenger) {
        MethodChannel(messenger, "diwan/contacts").setMethodCallHandler { call, result ->
            when (call.method) {
                "pickPhone" -> {
                    if (pending != null) {
                        result.error("busy", "A contact is already being picked", null)
                    } else {
                        pending = result
                        launcher.launch(Intent(Intent.ACTION_PICK, Phone.CONTENT_URI))
                    }
                }
                else -> result.notImplemented()
            }
        }
    }
}
