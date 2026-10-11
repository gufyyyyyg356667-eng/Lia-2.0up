package com.Lia.assistant.wake

import android.content.Context

/** Play build: wake-word listening is not included, so it never starts. */
object WakeControl {
    fun start(ctx: Context): Boolean = false
    fun stop(ctx: Context) = Unit
}
