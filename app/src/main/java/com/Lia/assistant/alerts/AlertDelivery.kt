package com.Lia.assistant.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.Lia.assistant.voice.VoiceSessionManager

/** Speaks a message if a voice session is live, otherwise shows it as a notification. */
object AlertDelivery {
    private const val CHANNEL = "lia_alerts"

    fun deliver(ctx: Context, text: String) {
        if (VoiceSessionManager.isActive.value) {
            VoiceSessionManager.announce(text)
            return
        }
        val manager = ctx.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Lia alerts", NotificationManager.IMPORTANCE_DEFAULT))
        val note = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Lia")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(text.hashCode(), note)
        } catch (e: SecurityException) {
            // Notification permission not granted; the alert is simply not shown.
        }
    }
}
