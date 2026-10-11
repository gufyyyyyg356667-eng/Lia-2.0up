package com.Lia.assistant.ui.fx

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import com.Lia.assistant.voice.VoiceSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * System-wide edge glow for the direct build. It shows over other apps while a voice session is live.
 * Does nothing unless the user has granted "Display over other apps".
 */
object OverlayEdgeGlowController {
    private var view: EdgeView? = null
    private var manager: WindowManager? = null
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun show(ctx: Context) {
        if (view != null) return
        if (!Settings.canDrawOverlays(ctx)) return
        val wm = ctx.getSystemService(WindowManager::class.java) ?: return
        val glow = EdgeView(ctx)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        runCatching { wm.addView(glow, params) }.onFailure { return }
        view = glow
        manager = wm
        job = scope.launch {
            VoiceSessionManager.state.collect { state -> glow.setColor(edgeGlowArgb(state)) }
        }
    }

    fun hide() {
        job?.cancel()
        job = null
        val v = view ?: return
        runCatching { manager?.removeView(v) }
        view = null
        manager = null
    }
}

/** Plain View that paints four edge gradients. Kept free of Compose so it works in a system window. */
private class EdgeView(ctx: Context) : View(ctx) {
    private var color: Int? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val band = 26 * resources.displayMetrics.density

    fun setColor(argb: Long?) {
        color = argb?.toInt()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val c = color ?: return
        val strong = (c and 0x00FFFFFF) or (0xB3 shl 24)
        val faded = c and 0x00FFFFFF
        val w = width.toFloat()
        val h = height.toFloat()

        paint.shader = LinearGradient(0f, 0f, 0f, band, strong, faded, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, band, paint)
        paint.shader = LinearGradient(0f, h, 0f, h - band, strong, faded, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, h - band, w, h, paint)
        paint.shader = LinearGradient(0f, 0f, band, 0f, strong, faded, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, band, h, paint)
        paint.shader = LinearGradient(w, 0f, w - band, 0f, strong, faded, Shader.TileMode.CLAMP)
        canvas.drawRect(w - band, 0f, w, h, paint)
    }
}
