package com.jarvis.ai.service

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import com.jarvis.ai.ui.TheForgeActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceActionsManager(private val context: Context) {

    fun openApp(appName: String): String {
        val pm: PackageManager = context.packageManager
        val cleanName = appName.trim().lowercase()

        val commonPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "chrome" to "com.android.chrome",
            "camera" to "com.android.camera",
            "settings" to "com.android.settings",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm",
            "spotify" to "com.spotify.music",
            "telegram" to "org.telegram.messenger",
            "instagram" to "com.instagram.android"
        )

        val targetPackage = commonPackages[cleanName]
        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "Opening $appName."
            }
        }

        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(cleanName) || cleanName.contains(label)) {
                val intent = pm.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return "Opening ${pm.getApplicationLabel(app)}."
                }
            }
        }

        return "Could not find $appName installed on this phone."
    }

    fun callContact(contactQuery: String): String {
        val query = contactQuery.trim()
        val digitsOnly = query.replace("[^0-9+]".toRegex(), "")

        if (digitsOnly.length >= 7) {
            return placeCall(digitsOnly)
        }

        // Search contact by name in ContactsContract
        val phone = findPhoneNumberByName(query)
        if (phone != null) {
            return placeCall(phone)
        }

        return "Contact '$query' not found in your phone's address book."
    }

    private fun placeCall(phoneNumber: String): String {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Calling $phoneNumber."
        } catch (e: SecurityException) {
            // Fallback to dialer if CALL_PHONE permission not yet granted
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return "Dialer opened for $phoneNumber."
        } catch (e: Exception) {
            return "Failed to place call: ${e.message}"
        }
    }

    fun messageContact(contact: String, message: String, app: String = "sms"): String {
        val phone = findPhoneNumberByName(contact) ?: contact.replace("[^0-9+]".toRegex(), "")
        return if (app.equals("whatsapp", ignoreCase = true)) {
            val cleanNumber = phone.replace("+", "").replace(" ", "")
            val url = if (cleanNumber.isNotBlank()) {
                "https://wa.me/$cleanNumber?text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening WhatsApp with message ready."
        } else {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "SMS composer opened for $contact."
        }
    }

    fun searchContacts(query: String?, limit: Int = 10): String {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            if (!query.isNullOrBlank()) "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?" else null,
            if (!query.isNullOrBlank()) arrayOf("%$query%") else null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        val results = JSONArray()
        cursor?.use {
            var count = 0
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext() && count < limit) {
                val name = it.getString(nameIdx)
                val number = it.getString(numIdx)
                results.put(JSONObject().apply {
                    put("name", name)
                    put("number", number)
                })
                count++
            }
        }

        return if (results.length() > 0) {
            "Found contacts: $results"
        } else {
            "No contacts found."
        }
    }

    private fun findPhoneNumberByName(nameQuery: String): String? {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$nameQuery%"),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return it.getString(numIdx)
            }
        }
        return null
    }

    fun buildWebsite(prompt: String): String {
        val intent = Intent(context, TheForgeActivity::class.java).apply {
            putExtra("website_prompt", prompt)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "The Forge 3D Reactor activated. Building your website now."
    }

    fun controlDevice(what: String, state: String): String {
        val isTurnOn = state.equals("on", ignoreCase = true) || state.equals("enable", ignoreCase = true)
        val target = what.trim().lowercase()

        return when {
            target.contains("flashlight") || target.contains("torch") -> {
                try {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                        cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }
                    if (cameraId != null) {
                        cameraManager.setTorchMode(cameraId, isTurnOn)
                        "Flashlight turned ${if (isTurnOn) "on" else "off"}."
                    } else {
                        "This phone does not have an available flashlight."
                    }
                } catch (e: Exception) {
                    "Failed to toggle flashlight: ${e.message}"
                }
            }
            target.contains("wifi") || target.contains("wlan") -> {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Wi-Fi settings panel opened."
            }
            target.contains("bluetooth") -> {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Bluetooth settings panel opened."
            }
            else -> "Unsupported hardware target: $what"
        }
    }

    fun controlMedia(action: String): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val keycode = when (action.lowercase()) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keycode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keycode))
        return "Media command sent: $action."
    }

    fun searchYouTube(query: String): String {
        return try {
            val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching YouTube for '$query'."
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            "Opened YouTube web search for '$query'."
        }
    }

    fun searchSpotify(query: String): String {
        return try {
            val uri = Uri.parse("spotify:search:${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.spotify.music")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening Spotify for '$query'."
        } catch (e: Exception) {
            "Spotify app is not installed."
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "Jarvis Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Alarm set for %02d:%02d.".format(hour, minute)
        } catch (e: Exception) {
            "Could not set alarm: ${e.message}"
        }
    }

    fun setTimer(seconds: Int, message: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "Jarvis Timer" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Timer set for $seconds seconds."
        } catch (e: Exception) {
            "Could not set timer: ${e.message}"
        }
    }

    fun getPhoneStatus(): String {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volPct = (currentVol * 100 / maxVol)

        return "Phone Status: Battery $batteryPct%, Media Volume $volPct%."
    }

    fun setVolume(level: Int): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (level.coerceIn(0, 100) * maxVol) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return "Volume adjusted to $level%."
    }

    fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' hh:mm a", Locale.getDefault())
        return "Current date and time: ${sdf.format(Date())}."
    }

    fun searchWeb(query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching web for '$query'."
        } catch (e: Exception) {
            "Could not open web search: ${e.message}"
        }
    }

    fun navigateTo(destination: String): String {
        return try {
            val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Navigating to '$destination'."
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(destination)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            "Opened Maps for '$destination'."
        }
    }

    fun openCamera(): String {
        val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Camera opened."
    }

    fun startSketchDrawing(): String {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Choose an image to turn into an animated pencil sketch."
    }
}
