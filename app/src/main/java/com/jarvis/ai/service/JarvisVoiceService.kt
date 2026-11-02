package com.jarvis.ai.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.jarvis.ai.JarvisApp
import com.jarvis.ai.MainActivity
import java.util.Locale

class JarvisVoiceService : Service(), TextToSpeech.OnInitListener {

    private val binder = LocalBinder()
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    inner class LocalBinder : Binder() {
        fun getService(): JarvisVoiceService = this@JarvisVoiceService
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        startForeground(101, createNotification())
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
            isTtsReady = true
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (isTtsReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_speech_id")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, JarvisApp.CHANNEL_ID)
            .setContentTitle("Jarvis AI Active")
            .setContentText("Jarvis is online and ready for voice commands.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
