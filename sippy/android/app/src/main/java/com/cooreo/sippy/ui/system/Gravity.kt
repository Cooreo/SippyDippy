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

/**
 * Two-axis tilt for parallax (round 8, B11): the backdrop and the pinned friends
 * shift by a few dp against the phone's lean, and nothing else moves.
 *
 * Two [MutableFloatState]s rather than values, for the same reason as the orb's
 * tilt: they are read inside `graphicsLayer` lambdas, so a sensor sample costs a
 * layer update and never a recomposition.
 */
class TiltParallax internal constructor(
    val x: MutableFloatState,
    val y: MutableFloatState,
)

/** The pure part: one low-pass step, and the degrees it is allowed to reach. */
object TiltFilter {
    /** ~0.06 per sample: a lean settles in about a second, a jolt does not show. */
    const val RESPONSE = 0.06f
    const val MAX_DEGREES = 5f

    fun next(current: Float, target: Float): Float = current + (target - current) * RESPONSE

    fun degrees(axisFraction: Float): Float = axisFraction.coerceIn(-1f, 1f) * MAX_DEGREES
}

/**
 * Registers the gravity sensor while [enabled] and nowhere else: off under Reduce
 * effects, and unregistered with the composition.
 */
@Composable
fun rememberTiltParallax(enabled: Boolean): TiltParallax {
    val context = LocalContext.current
    val x = remember { mutableFloatStateOf(0f) }
    val y = remember { mutableFloatStateOf(0f) }
    DisposableEffect(enabled) {
        if (!enabled) {
            x.floatValue = 0f
            y.floatValue = 0f
            return@DisposableEffect onDispose { }
        }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (manager == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val roll = TiltFilter.degrees(event.values[0] / SensorManager.GRAVITY_EARTH)
                val pitch = TiltFilter.degrees(event.values[1] / SensorManager.GRAVITY_EARTH)
                x.floatValue = TiltFilter.next(x.floatValue, roll)
                y.floatValue = TiltFilter.next(y.floatValue, pitch)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            manager.unregisterListener(listener)
            x.floatValue = 0f
            y.floatValue = 0f
        }
    }
    return remember(x, y) { TiltParallax(x, y) }
}
