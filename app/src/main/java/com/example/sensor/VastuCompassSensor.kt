package com.example.sensor

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.CompassAccuracy
import com.example.model.CompassReading
import com.example.model.VastuCompassMath
import kotlin.math.sqrt

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberVastuCompassState(enabled: Boolean = true): State<CompassReading> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val initialHasSensors = remember(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sm == null) {
            false
        } else {
            val hasRot = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                sm.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null
            val hasAccelMag = (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null ||
                sm.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) &&
                sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
            hasRot || hasAccelMag
        }
    }

    val state = remember(context) {
        mutableStateOf(
            CompassReading(
                hasHardwareSensors = initialHasSensors,
                accuracy = if (initialHasSensors) CompassAccuracy.MEDIUM else CompassAccuracy.MANUAL
            )
        )
    }

    DisposableEffect(context, lifecycleOwner, enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            val geoRotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            val hasAccelMag = accelSensor != null && magnetSensor != null
            val activeRotSensor = rotationSensor ?: geoRotationSensor
            val hasAnyCompass = activeRotSensor != null || hasAccelMag

            if (sensorManager == null || !hasAnyCompass) {
                state.value = state.value.copy(
                    hasHardwareSensors = false,
                    accuracy = CompassAccuracy.MANUAL
                )
                onDispose { }
            } else {
                state.value = state.value.copy(
                    hasHardwareSensors = true,
                    accuracy = if (state.value.accuracy == CompassAccuracy.MANUAL) CompassAccuracy.MEDIUM else state.value.accuracy
                )

                val gravity = FloatArray(3)
                val geomagnetic = FloatArray(3)
                val rotationMatrix = FloatArray(9)
                var hasAccel = false
                var hasMagnet = false
                var hasRotationVectorEvents = false
                var currentAccuracy = CompassAccuracy.MEDIUM
                var lastAzimuth: Float? = null
                var latestMagnetMicroTesla = 0f

                fun currentDisplayRotation(): Int {
                    val activity = context.findActivity()
                    return runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            (activity ?: context).display.rotation
                        } else {
                            @Suppress("DEPRECATION")
                            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                            @Suppress("DEPRECATION")
                            wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
                        }
                    }.getOrDefault(Surface.ROTATION_0)
                }

                var isRegistered = false

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val alpha = 0.25f
                        when (event.sensor.type) {
                            Sensor.TYPE_ROTATION_VECTOR,
                            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                                val gNormSq = rotationMatrix[6] * rotationMatrix[6] +
                                    rotationMatrix[7] * rotationMatrix[7] +
                                    rotationMatrix[8] * rotationMatrix[8]
                                if (gNormSq > 0.1f) {
                                    hasRotationVectorEvents = true
                                    val reading = VastuCompassMath.computeOrientationFromRotationMatrix(
                                        rotationMatrix = rotationMatrix,
                                        magnetMicroTesla = latestMagnetMicroTesla,
                                        displayRotation = currentDisplayRotation(),
                                        previousAzimuthDeg = lastAzimuth,
                                        accuracy = currentAccuracy,
                                        hasHardwareSensors = true,
                                        smoothingAlpha = 0.28f
                                    )
                                    lastAzimuth = reading.azimuthDeg
                                    state.value = reading
                                }
                            }

                            Sensor.TYPE_GRAVITY,
                            Sensor.TYPE_ACCELEROMETER -> {
                                if (!hasAccel) {
                                    System.arraycopy(event.values, 0, gravity, 0, 3)
                                    hasAccel = true
                                } else {
                                    gravity[0] += alpha * (event.values[0] - gravity[0])
                                    gravity[1] += alpha * (event.values[1] - gravity[1])
                                    gravity[2] += alpha * (event.values[2] - gravity[2])
                                }
                                if (!hasRotationVectorEvents && hasAccel && hasMagnet) {
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

                            Sensor.TYPE_MAGNETIC_FIELD -> {
                                if (!hasMagnet) {
                                    System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                                    hasMagnet = true
                                } else {
                                    geomagnetic[0] += alpha * (event.values[0] - geomagnetic[0])
                                    geomagnetic[1] += alpha * (event.values[1] - geomagnetic[1])
                                    geomagnetic[2] += alpha * (event.values[2] - geomagnetic[2])
                                }
                                latestMagnetMicroTesla = sqrt(
                                    geomagnetic[0] * geomagnetic[0] +
                                        geomagnetic[1] * geomagnetic[1] +
                                        geomagnetic[2] * geomagnetic[2]
                                )
                                if (!hasRotationVectorEvents && hasAccel && hasMagnet) {
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
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD ||
                            sensor?.type == Sensor.TYPE_ROTATION_VECTOR ||
                            sensor?.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
                        ) {
                            currentAccuracy = when (accuracy) {
                                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
                                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
                                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
                                else -> CompassAccuracy.UNRELIABLE
                            }
                        }
                    }
                }

                fun registerSensors() {
                    if (isRegistered) return
                    activeRotSensor?.let {
                        sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
                    }
                    accelSensor?.let {
                        sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
                    }
                    magnetSensor?.let {
                        sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
                    }
                    isRegistered = true
                }

                fun unregisterSensors() {
                    if (!isRegistered) return
                    sensorManager.unregisterListener(listener)
                    isRegistered = false
                }

                registerSensors()

                val lifecycleObserver = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> registerSensors()
                        Lifecycle.Event.ON_PAUSE -> unregisterSensors()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                    unregisterSensors()
                }
            }
        }
    }

    return state
}
