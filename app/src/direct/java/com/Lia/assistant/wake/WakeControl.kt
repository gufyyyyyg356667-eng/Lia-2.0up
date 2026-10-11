package com.Lia.assistant.wake

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Starts and stops the always-listening service (direct build). Returns false if it cannot start. */
object WakeControl {
    fun start(ctx: Context): Boolean {
        val micGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!micGranted) return false
        return try {
            ContextCompat.startForegroundService(ctx, Intent(ctx, WakeWordService::class.java))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stop(ctx: Context) {
        ctx.stopService(Intent(ctx, WakeWordService::class.java))
    }
}
