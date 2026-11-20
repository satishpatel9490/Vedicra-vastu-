package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.model.CompassAccuracy
import com.example.model.CompassReading
import com.example.model.VastuCompassMath

@Composable
fun rememberVastuCompassState(enabled: Boolean = true): State<CompassReading> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(CompassReading()) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            val hasBoth = accelSensor != null && magnetSensor != null

            if (sensorManager == null || !hasBoth) {
                state.value = state.value.copy(
                    hasHardwareSensors = false,
                    accuracy = CompassAccuracy.MANUAL
                )
                onDispose { }
            } else {
                state.value = state.value.copy(
                    hasHardwareSensors = true,
                    accuracy = CompassAccuracy.MEDIUM
                )

                val gravity = FloatArray(3)
                val geomagnetic = FloatArray(3)
                var hasAccel = false
                var hasMagnet = false
                var currentAccuracy = CompassAccuracy.MEDIUM
                var lastAzimuth: Float? = null

                fun currentDisplayRotation(): Int {
                    return runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            context.display?.rotation ?: Surface.ROTATION_0
                        } else {
                            @Suppress("DEPRECATION")
                            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                            @Suppress("DEPRECATION")
                            wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
                        }
                    }.getOrDefault(Surface.ROTATION_0)
                }

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val alpha = 0.22f
                        when (event.sensor.type) {
                            Sensor.TYPE_ACCELEROMETER -> {
                                if (!hasAccel) {
                                    System.arraycopy(event.values, 0, gravity, 0, 3)
                                    hasAccel = true
                                } else {
                                    gravity[0] += alpha * (event.values[0] - gravity[0])
                                    gravity[1] += alpha * (event.values[1] - gravity[1])
                                    gravity[2] += alpha * (event.values[2] - gravity[2])
                                }
                            }
                            Sensor.TYPE_MAGNETIC_FIELD -> {
                                if (!hasMagnet) {
                                    System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                                    hasMagnet = true
                                } else {
                                    geomagnetic[0] += alpha * (event.values[0] - geomagnetic[0])
                                    geomagnetic[1] += alpha * (event.values[1] - geomagnetic[1])
                                    geomagnetic[2] += alpha * (event.values[2] - geomagnetic[2])
                                }
                            }
                        }
                        if (hasAccel && hasMagnet) {
                            val reading = VastuCompassMath.computeOrientationFromVectors(
                                accel = gravity,
                                magnet = geomagnetic,
                                displayRotation = currentDisplayRotation(),
                                previousAzimuthDeg = lastAzimuth,
                                accuracy = currentAccuracy,
                                hasHardwareSensors = true
                            )
                            lastAzimuth = reading.azimuthDeg
                            state.value = reading
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                            currentAccuracy = when (accuracy) {
                                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
                                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
                                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
                                else -> CompassAccuracy.UNRELIABLE
                            }
                        }
                    }
                }

                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(listener, magnetSensor, SensorManager.SENSOR_DELAY_UI)

                onDispose {
                    sensorManager.unregisterListener(listener)
                }
            }
        }
    }

    return state
}
