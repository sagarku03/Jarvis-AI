package com.jarvis.ai.service

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.widget.Toast

class DeviceActionsManager(private val context: Context) {

    fun openApp(appName: String): String {
        val pm: PackageManager = context.packageManager
        val cleanName = appName.trim().lowercase()

        // Known package mapping shortcuts
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

        // Search installed apps by label
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

    fun makeCall(phoneNumber: String): String {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phoneNumber.trim()}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Calling $phoneNumber."
        } catch (e: Exception) {
            return "Failed to place call: ${e.message}"
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String): String {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "Jarvis Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Alarm set for %02d:%02d.".format(hour, minute)
        } catch (e: Exception) {
            return "Could not set alarm: ${e.message}"
        }
    }

    fun searchWeb(query: String): String {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Searching web for $query."
        } catch (e: Exception) {
            return "Could not open web search: ${e.message}"
        }
    }

    fun sendWhatsApp(phoneNumber: String?, message: String): String {
        try {
            val uri = if (!phoneNumber.isNullOrBlank()) {
                val cleanNumber = phoneNumber.replace("+", "").replace(" ", "")
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Opening WhatsApp with your message."
        } catch (e: Exception) {
            return "WhatsApp is not available: ${e.message}"
        }
    }
}
