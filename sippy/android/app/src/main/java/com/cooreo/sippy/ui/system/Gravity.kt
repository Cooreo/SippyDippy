package com.cooreo.sippy.ui.system

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * How far the phone is tilted sideways, in degrees (round 8, B5).
 *
 * The gravity vector is low-pass filtered so the water sloshes instead of
 * jittering, and the result is a [MutableFloatState] on purpose: the orb reads it
 * *inside* its draw scope, so a sensor sample invalidates a draw, never a
 * recomposition. The listener is registered only while [enabled] and is removed
 * with the composition, so nothing keeps running in the background.
 */
@Composable
fun rememberGravityTilt(enabled: Boolean): MutableFloatState {
    val context = LocalContext.current
    val tilt = remember { mutableFloatStateOf(0f) }
    DisposableEffect(enabled) {
        if (!enabled) {
            tilt.floatValue = 0f
            return@DisposableEffect onDispose { }
        }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (manager == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val sideways = (event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                val target = sideways * MAX_DEGREES
                // Low-pass: ~0.12 per sample at SENSOR_DELAY_GAME.
                tilt.floatValue += (target - tilt.floatValue) * 0.12f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            manager.unregisterListener(listener)
            tilt.floatValue = 0f
        }
    }
    return tilt
}

/** The most the surface ever leans, so the water stays inside the glass. */
private const val MAX_DEGREES = 6f
