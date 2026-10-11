package com.Lia.assistant.ui.fx

import android.content.Context

/**
 * Play build: no system-wide overlay. SYSTEM_ALERT_WINDOW is not declared in this flavor.
 * The same signature exists in the direct flavor, so shared code compiles against both.
 */
object OverlayEdgeGlowController {
    @Suppress("UNUSED_PARAMETER")
    fun show(ctx: Context) = Unit

    fun hide() = Unit
}
