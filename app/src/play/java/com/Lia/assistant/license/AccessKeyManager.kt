package com.Lia.assistant.license

import android.content.Context

/** Play build: no licence gate. Same signatures as the direct build, always allowed. */
object AccessKeyManager {
    fun isActive(): Boolean = true
    fun refusalMessage(): String = ""
    fun refreshInfo(ctx: Context) = Unit
    suspend fun liveCheck(ctx: Context, force: Boolean = false) = Unit
}
