package com.example.model

import android.hardware.SensorManager
import android.view.Surface
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class VastuElement(
    val label: String,
    val fillColorArgb: Long,
    val softFillColorArgb: Long,
    val textColorArgb: Long
) {
    WATER(
        label = "Water",
        fillColorArgb = 0xFF2B74B8,
        softFillColorArgb = 0xCC3C88CE,
        textColorArgb = 0xFFFFFFFF
    ),
    AIR(
        label = "Air",
        fillColorArgb = 0xFF4F9D4B,
        softFillColorArgb = 0xCC62B15D,
        textColorArgb = 0xFFFFFFFF
    ),
    FIRE(
        label = "Fire",
        fillColorArgb = 0xFFD94832,
        softFillColorArgb = 0xCCE65B45,
        textColorArgb = 0xFFFFFFFF
    ),
    EARTH(
        label = "Earth",
        fillColorArgb = 0xFFEBC834,
        softFillColorArgb = 0xCCF4D654,
        textColorArgb = 0xFF1F2421
    ),
    SPACE(
        label = "Space",
        fillColorArgb = 0xFFF2F4F7,
        softFillColorArgb = 0xCCFAFBFC,
        textColorArgb = 0xFF1F2421
    )
}

data class VastuPada(
    val index: Int,
    val code: String,
    val deity: String,
    val startDegree: Float,
    val endDegree: Float,
    val isAuspicious: Boolean,
    val element: VastuElement
) {
    val midDegree: Float
        get() = (startDegree + endDegree) / 2f
}

data class VastuZone16(
    val index: Int,
    val code: String,
    val fullName: String,
    val attributeTitle: String,
    val centerDegree: Float,
    val startDegree: Float,
    val endDegree: Float,
    val element: VastuElement,
    val sectorColorArgb: Long,
    val sectorTextColorArgb: Long
)

enum class CompassAccuracy(val label: String) {
    HIGH("High"),
    MEDIUM("Good"),
    LOW("Calibrate (Figure-8)"),
    UNRELIABLE("Needs Calibration"),
    MANUAL("Manual")
}

data class CompassReading(
    val azimuthDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val tiltFromLevelDeg: Float = 0f,
    val isLevel: Boolean = true,
    val isUprightCameraMode: Boolean = false,
    val zone: VastuZone16 = VastuChakraSpec.zones16.first(),
    val pada: VastuPada = VastuChakraSpec.padas32.first(),
    val accuracy: CompassAccuracy = CompassAccuracy.MANUAL,
    val hasHardwareSensors: Boolean = false
)

object VastuChakraSpec {
    const val PADA_SPAN_DEGREES = 11.25f
    const val ZONE_SPAN_DEGREES = 22.5f

    /**
     * 32 Main-Door Entrance Padas clockwise starting at 0.0° Magnetic North.
     * Magnetic 0° lies strictly on the boundary between N4 (348.75°..360.0°) and N5 (0.0°..11.25°).
     */
    val padas32: List<VastuPada> = listOf(
        // North (0° starts N5; 360° ends N4)
        VastuPada(0, "N5", "Soma", 0.00f, 11.25f, true, VastuElement.WATER),
        VastuPada(1, "N6", "Bhujag", 11.25f, 22.50f, false, VastuElement.WATER),
        VastuPada(2, "N7", "Aditi", 22.50f, 33.75f, false, VastuElement.WATER),
        VastuPada(3, "N8", "Diti", 33.75f, 45.00f, false, VastuElement.WATER),

        // East (45°..135°)
        VastuPada(4, "E1", "Shikhi", 45.00f, 56.25f, false, VastuElement.AIR),
        VastuPada(5, "E2", "Parjanya", 56.25f, 67.50f, false, VastuElement.AIR),
        VastuPada(6, "E3", "Jayant", 67.50f, 78.75f, true, VastuElement.AIR),
        VastuPada(7, "E4", "Indra", 78.75f, 90.00f, true, VastuElement.AIR),
        VastuPada(8, "E5", "Surya", 90.00f, 101.25f, false, VastuElement.AIR),
        VastuPada(9, "E6", "Satya", 101.25f, 112.50f, false, VastuElement.AIR),
        VastuPada(10, "E7", "Bhrisha", 112.50f, 123.75f, false, VastuElement.AIR),
        VastuPada(11, "E8", "Akash", 123.75f, 135.00f, false, VastuElement.FIRE),

        // South (135°..225°)
        VastuPada(12, "S1", "Anil", 135.00f, 146.25f, false, VastuElement.FIRE),
        VastuPada(13, "S2", "Pusha", 146.25f, 157.50f, false, VastuElement.FIRE),
        VastuPada(14, "S3", "Vitatha", 157.50f, 168.75f, true, VastuElement.FIRE),
        VastuPada(15, "S4", "Grihakshat", 168.75f, 180.00f, true, VastuElement.FIRE),
        VastuPada(16, "S5", "Yama", 180.00f, 191.25f, false, VastuElement.FIRE),
        VastuPada(17, "S6", "Gandharv", 191.25f, 202.50f, false, VastuElement.EARTH),
        VastuPada(18, "S7", "Bhringraj", 202.50f, 213.75f, false, VastuElement.EARTH),
        VastuPada(19, "S8", "Mrigha", 213.75f, 225.00f, false, VastuElement.EARTH),

        // West (225°..315°)
        VastuPada(20, "W1", "Pitra", 225.00f, 236.25f, false, VastuElement.EARTH),
        VastuPada(21, "W2", "Dauwarik", 236.25f, 247.50f, false, VastuElement.EARTH),
        VastuPada(22, "W3", "Sugreev", 247.50f, 258.75f, true, VastuElement.SPACE),
        VastuPada(23, "W4", "Puspdant", 258.75f, 270.00f, true, VastuElement.SPACE),
        VastuPada(24, "W5", "Varun", 270.00f, 281.25f, true, VastuElement.SPACE),
        VastuPada(25, "W6", "Asur", 281.25f, 292.50f, false, VastuElement.SPACE),
        VastuPada(26, "W7", "Shosha", 292.50f, 303.75f, false, VastuElement.SPACE),
        VastuPada(27, "W8", "Papyashma", 303.75f, 315.00f, false, VastuElement.SPACE),

        // North-West back to North 0° (315°..360°)
        VastuPada(28, "N1", "Roga", 315.00f, 326.25f, false, VastuElement.WATER),
        VastuPada(29, "N2", "Naga", 326.25f, 337.50f, false, VastuElement.WATER),
        VastuPada(30, "N3", "Mukhya", 337.50f, 348.75f, true, VastuElement.WATER),
        VastuPada(31, "N4", "Bhallat", 348.75f, 360.00f, true, VastuElement.WATER)
    )

    /**
     * 16 Vastu Zones (22.5° each).
     * North spans 348.75°..11.25° (centered at 0° between N4 and N5).
     */
    val zones16: List<VastuZone16> = listOf(
        VastuZone16(0, "N", "N (North)", "New Clients / Orders", 0.0f, 348.75f, 11.25f, VastuElement.WATER, 0xFF3478B8, 0xFFFFFFFF),
        VastuZone16(1, "NNE", "NNE (North-Northeast)", "Health / Solutions", 22.5f, 11.25f, 33.75f, VastuElement.WATER, 0xFF3478B8, 0xFFFFFFFF),
        VastuZone16(2, "NE", "NE (Northeast / Ishanya)", "Ideas / Vision", 45.0f, 33.75f, 56.25f, VastuElement.WATER, 0xFFF3E6AB, 0xFF1F2421),
        VastuZone16(3, "ENE", "ENE (East-Northeast)", "Joy / Creativity", 67.5f, 56.25f, 78.75f, VastuElement.AIR, 0xFF59A553, 0xFFFFFFFF),
        VastuZone16(4, "E", "E (East)", "Society / Marketing", 90.0f, 78.75f, 101.25f, VastuElement.AIR, 0xFF59A553, 0xFFFFFFFF),
        VastuZone16(5, "ESE", "ESE (East-Southeast)", "Analysis / Churning", 112.5f, 101.25f, 123.75f, VastuElement.AIR, 0xFF59A553, 0xFFFFFFFF),
        VastuZone16(6, "SE", "SE (Southeast / Agneya)", "Liquid Cash / Security", 135.0f, 123.75f, 146.25f, VastuElement.FIRE, 0xFFE04D36, 0xFFFFFFFF),
        VastuZone16(7, "SSE", "SSE (South-Southeast)", "Confidence", 157.5f, 146.25f, 168.75f, VastuElement.FIRE, 0xFFE04D36, 0xFFFFFFFF),
        VastuZone16(8, "S", "S (South)", "Fame / Branding", 180.0f, 168.75f, 191.25f, VastuElement.FIRE, 0xFFE04D36, 0xFFFFFFFF),
        VastuZone16(9, "SSW", "SSW (South-Southwest)", "Let Go / Expenses", 202.5f, 191.25f, 213.75f, VastuElement.EARTH, 0xFFF1D442, 0xFF1F2421),
        VastuZone16(10, "SW", "SW (Southwest / Nairutya)", "Relations / Skills", 225.0f, 213.75f, 236.25f, VastuElement.EARTH, 0xFFF1D442, 0xFF1F2421),
        VastuZone16(11, "WSW", "WSW (West-Southwest)", "Knowledge / Savings", 247.5f, 236.25f, 258.75f, VastuElement.SPACE, 0xFFB8DFA8, 0xFF1F2421),
        VastuZone16(12, "W", "W (West)", "Gains / Fulfillment", 270.0f, 258.75f, 281.25f, VastuElement.SPACE, 0xFFF4F6F9, 0xFF1F2421),
        VastuZone16(13, "WNW", "WNW (West-Northwest)", "Depression / Detox", 292.5f, 281.25f, 303.75f, VastuElement.SPACE, 0xFFF4F6F9, 0xFF1F2421),
        VastuZone16(14, "NW", "NW (Northwest / Vayavya)", "Banks / Support", 315.0f, 303.75f, 326.25f, VastuElement.SPACE, 0xFFF5EBAD, 0xFF1F2421),
        VastuZone16(15, "NNW", "NNW (North-Northwest)", "Sex / Attraction", 337.5f, 326.25f, 348.75f, VastuElement.WATER, 0xFF3478B8, 0xFFFFFFFF)
    )
}

object VastuCompassMath {

    fun normalizeDegrees(deg: Float): Float {
        if (deg.isNaN() || deg.isInfinite()) return 0f
        val rem = deg % 360f
        return if (rem < 0f) rem + 360f else rem
    }

    fun padaForDegree(deg: Float): VastuPada {
        val d = normalizeDegrees(deg)
        val idx = ((d / VastuChakraSpec.PADA_SPAN_DEGREES).toInt()).coerceIn(0, 31)
        return VastuChakraSpec.padas32[idx]
    }

    fun zoneForDegree(deg: Float): VastuZone16 {
        val d = normalizeDegrees(deg)
        val shifted = normalizeDegrees(d + VastuChakraSpec.PADA_SPAN_DEGREES)
        val idx = ((shifted / VastuChakraSpec.ZONE_SPAN_DEGREES).toInt()).coerceIn(0, 15)
        return VastuChakraSpec.zones16[idx]
    }

    fun isOnNorthBoundary(deg: Float, toleranceDeg: Float = 0.75f): Boolean {
        val d = normalizeDegrees(deg)
        return d <= toleranceDeg || d >= (360f - toleranceDeg)
    }

    fun smoothAzimuth(prevDeg: Float, nextDeg: Float, alpha: Float = 0.2f): Float {
        val pRad = Math.toRadians(normalizeDegrees(prevDeg).toDouble())
        val nRad = Math.toRadians(normalizeDegrees(nextDeg).toDouble())
        val a = alpha.coerceIn(0.01f, 1.0f).toDouble()
        val sinAvg = (1.0 - a) * sin(pRad) + a * sin(nRad)
        val cosAvg = (1.0 - a) * cos(pRad) + a * cos(nRad)
        return normalizeDegrees(Math.toDegrees(atan2(sinAvg, cosAvg)).toFloat())
    }

    fun formatHeadingLabel(deg: Float): String {
        val norm = normalizeDegrees(deg)
        val zone = zoneForDegree(norm)
        val pada = padaForDegree(norm)
        val rounded = (norm * 10f).roundToInt() / 10f
        return if (isOnNorthBoundary(norm)) {
            "0.0° / 360° N (Between N4 & N5)"
        } else {
            "${String.format(Locale.US, "%.1f", rounded)}° ${zone.code} (${pada.code} ${pada.deity})"
        }
    }

    fun entranceDirectionForDegree(deg: Float): String {
        val d = normalizeDegrees(deg)
        return when {
            d >= 337.5f || d < 22.5f -> "North"
            d < 67.5f -> "Northeast"
            d < 112.5f -> "East"
            d < 157.5f -> "Southeast"
            d < 202.5f -> "South"
            d < 247.5f -> "Southwest"
            d < 292.5f -> "West"
            else -> "Northwest"
        }
    }

    /**
     * Computes magnetic heading & physical tilt angles from raw accelerometer (m/s^2) and
     * magnetometer (uT) vectors. Handles both flat tabletop floor-plan alignment and upright
     * camera framing smoothly without gimbal lock.
     */
    fun computeOrientationFromVectors(
        accel: FloatArray,
        magnet: FloatArray,
        displayRotation: Int = Surface.ROTATION_0,
        previousAzimuthDeg: Float? = null,
        accuracy: CompassAccuracy = CompassAccuracy.HIGH,
        hasHardwareSensors: Boolean = true,
        smoothingAlpha: Float = 0.22f
    ): CompassReading {
        val gMag = sqrt(accel[0] * accel[0] + accel[1] * accel[1] + accel[2] * accel[2]).coerceAtLeast(0.001f)
        val gx = accel[0] / gMag
        val gy = accel[1] / gMag
        val gz = accel[2] / gMag

        // Pitch: 0° when lying flat face-up, +90° when vertical upright
        val pitchRad = asin(gy.coerceIn(-1f, 1f).toDouble())
        val pitchDeg = Math.toDegrees(pitchRad).toFloat()

        // Roll: left/right bank relative to gravity
        val rollRad = atan2(gx.toDouble(), gz.toDouble())
        val rollDeg = Math.toDegrees(rollRad).toFloat()

        // Total tilt away from flat horizontal plane (0° = perfectly flat on floor plan)
        val tiltFromFlatDeg = Math.toDegrees(acos(gz.coerceIn(-1f, 1f).toDouble())).toFloat()

        val uprightCameraMode = abs(pitchDeg) > 50f

        // Level criterion: either flat on plan (tiltFromFlat <= 3.5°) or upright horizon level (|roll| <= 3.5°)
        val horizonRollDeg = Math.toDegrees(atan2(gx.toDouble(), hypot(gy.toDouble(), gz.toDouble()))).toFloat()
        val effectiveTiltForBadge = if (uprightCameraMode) abs(horizonRollDeg) else tiltFromFlatDeg
        val isLevel = effectiveTiltForBadge <= 3.5f

        val rotationMatrix = FloatArray(9)
        val inclinationMatrix = FloatArray(9)
        val success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accel, magnet)

        val rawAzimuth = if (success) {
            val remapped = FloatArray(9)
            if (uprightCameraMode) {
                // Camera aiming mode: align azimuth along the back camera (-Z) optical axis
                when (displayRotation) {
                    Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_Z,
                        SensorManager.AXIS_MINUS_X,
                        remapped
                    )
                    Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_MINUS_Z,
                        SensorManager.AXIS_X,
                        remapped
                    )
                    else -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remapped
                    )
                }
            } else {
                when (displayRotation) {
                    Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_Y,
                        SensorManager.AXIS_MINUS_X,
                        remapped
                    )
                    Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_MINUS_X,
                        SensorManager.AXIS_MINUS_Y,
                        remapped
                    )
                    Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_MINUS_Y,
                        SensorManager.AXIS_X,
                        remapped
                    )
                    else -> {
                        System.arraycopy(rotationMatrix, 0, remapped, 0, 9)
                    }
                }
            }
            val orientation = FloatArray(3)
            SensorManager.getOrientation(remapped, orientation)
            normalizeDegrees(Math.toDegrees(orientation[0].toDouble()).toFloat())
        } else {
            previousAzimuthDeg ?: 0f
        }

        val smoothedAzimuth = if (previousAzimuthDeg != null) {
            smoothAzimuth(previousAzimuthDeg, rawAzimuth, smoothingAlpha)
        } else {
            rawAzimuth
        }

        return CompassReading(
            azimuthDeg = smoothedAzimuth,
            pitchDeg = pitchDeg,
            rollDeg = if (uprightCameraMode) horizonRollDeg else rollDeg,
            tiltFromLevelDeg = tiltFromFlatDeg,
            isLevel = isLevel,
            isUprightCameraMode = uprightCameraMode,
            zone = zoneForDegree(smoothedAzimuth),
            pada = padaForDegree(smoothedAzimuth),
            accuracy = accuracy,
            hasHardwareSensors = hasHardwareSensors
        )
    }
}
