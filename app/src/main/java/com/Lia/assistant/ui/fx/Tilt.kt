package com.Lia.assistant.ui.fx

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext

/**
 * Device tilt from the accelerometer, low-pass filtered, each axis in -1..1.
 * Registers only while [enabled] and unregisters when the composable leaves.
 */
@Composable
fun rememberTilt(enabled: Boolean): State<Offset> {
    val ctx = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var fx = 0f
        var fy = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                fx += LOW_PASS * (event.values[0] / GRAVITY - fx)
                fy += LOW_PASS * (event.values[1] / GRAVITY - fy)
                tilt.value = Offset((-fx).coerceIn(-1f, 1f), fy.coerceIn(-1f, 1f))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return tilt
}

private const val LOW_PASS = 0.15f
private const val GRAVITY = 9.81f
