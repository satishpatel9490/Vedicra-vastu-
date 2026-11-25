package com.example.model

import android.view.Surface
import kotlinx.serialization.Serializable
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

@Serializable
enum class VastuChakraType(
    val displayName: String,
    val shortName: String,
    val subtitle: String
) {
    SHAKTI_16_32(
        displayName = "Shakti Chakra (16 Zones)",
        shortName = "Shakti Chakra",
        subtitle = "16 Zones • 32 Padas • 0° North strictly between N4 & N5"
    ),
    DEVTA_45_MANDALA(
        displayName = "45 Devta Mandala",
        shortName = "45 Devta",
        subtitle = "Brahma + 12 Inner Devas + 32 Padas + Body Organs & Gunas"
    ),
    PANCH_TATVA_CYCLE(
        displayName = "Panch Tatva Cycle",
        shortName = "Panch Tatva",
        subtitle = "5 Elements • Clockwise Support & 5-Point Oppose Star"
    ),
    MAIN_DOOR_32(
        displayName = "Main Door Chakra (✓/✗)",
        shortName = "Main Door ✓/✗",
        subtitle = "32 Radial Padas • Auspicious ✓ & Inauspicious ✗ Entrances"
    )
}

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
    val element: VastuElement,
    val padaNumber45: Int = 14,
    val directionGroup: String = "North",
    val entranceEffect: String = ""
) {
    val midDegree: Float
        get() = (startDegree + endDegree) / 2f

    val signSymbol: String
        get() = if (isAuspicious) "+" else "-"
}

data class VastuInnerDevta(
    val devtaNumber: Int,
    val name: String,
    val ringLayer: Int, // 1 = Brahma center, 2 = 4 cardinal inner, 3 = 8 corner intermediate
    val startDegree: Float,
    val sweepDegree: Float,
    val fillColorArgb: Long
) {
    val midDegree: Float
        get() = startDegree + sweepDegree / 2f
}

data class VastuOrganGunaSegment(
    val label: String,
    val startDegree: Float,
    val endDegree: Float,
    val isGunaBand: Boolean = false
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
    val magneticFieldMicroTesla: Float = 0f,
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
        VastuPada(
            index = 0, code = "N5", deity = "Soma", startDegree = 0.00f, endDegree = 11.25f,
            isAuspicious = true, element = VastuElement.WATER, padaNumber45 = 42, directionGroup = "North",
            entranceEffect = "Religious, calm and non-aggressive mindset; supports peaceful spiritual and steady household growth."
        ),
        VastuPada(
            index = 1, code = "N6", deity = "Bhujag", startDegree = 11.25f, endDegree = 22.50f,
            isAuspicious = false, element = VastuElement.WATER, padaNumber45 = 43, directionGroup = "North",
            entranceEffect = "Unacceptable behaviour in society and people usually avoid listening to residents."
        ),
        VastuPada(
            index = 2, code = "N7", deity = "Aditi", startDegree = 22.50f, endDegree = 33.75f,
            isAuspicious = false, element = VastuElement.WATER, padaNumber45 = 44, directionGroup = "North",
            entranceEffect = "Girls in family tend to go beyond traditional family culture."
        ),
        VastuPada(
            index = 3, code = "N8", deity = "Diti", startDegree = 33.75f, endDegree = 45.00f,
            isAuspicious = false, element = VastuElement.WATER, padaNumber45 = 45, directionGroup = "North",
            entranceEffect = "Gives high bank balance and strong financial accumulation."
        ),

        // East (45°..135°)
        VastuPada(
            index = 4, code = "E1", deity = "Shikhi", startDegree = 45.00f, endDegree = 56.25f,
            isAuspicious = false, element = VastuElement.AIR, padaNumber45 = 14, directionGroup = "East",
            entranceEffect = "Causes fire hazards, accidents and unexpected losses."
        ),
        VastuPada(
            index = 5, code = "E2", deity = "Parjanya", startDegree = 56.25f, endDegree = 67.50f,
            isAuspicious = false, element = VastuElement.AIR, padaNumber45 = 15, directionGroup = "East",
            entranceEffect = "Wasteful expenditure and number of girls are more in family."
        ),
        VastuPada(
            index = 6, code = "E3", deity = "Jayant", startDegree = 67.50f, endDegree = 78.75f,
            isAuspicious = true, element = VastuElement.AIR, padaNumber45 = 16, directionGroup = "East",
            entranceEffect = "Auspicious entrance that brings money, business profit and consistent success."
        ),
        VastuPada(
            index = 7, code = "E4", deity = "Indra", startDegree = 78.75f, endDegree = 90.00f,
            isAuspicious = true, element = VastuElement.AIR, padaNumber45 = 17, directionGroup = "East",
            entranceEffect = "Builds strong connections and growth with powerful people in government departments."
        ),
        VastuPada(
            index = 8, code = "E5", deity = "Surya", startDegree = 90.00f, endDegree = 101.25f,
            isAuspicious = false, element = VastuElement.AIR, padaNumber45 = 18, directionGroup = "East",
            entranceEffect = "Creates extreme short temperament and quick anger among residents."
        ),
        VastuPada(
            index = 9, code = "E6", deity = "Satya", startDegree = 101.25f, endDegree = 112.50f,
            isAuspicious = false, element = VastuElement.AIR, padaNumber45 = 19, directionGroup = "East",
            entranceEffect = "Non-commitments and unreliability of residents."
        ),
        VastuPada(
            index = 10, code = "E7", deity = "Bhrisha", startDegree = 112.50f, endDegree = 123.75f,
            isAuspicious = false, element = VastuElement.AIR, padaNumber45 = 20, directionGroup = "East",
            entranceEffect = "Insensitive towards problems and discomfort of others."
        ),
        VastuPada(
            index = 11, code = "E8", deity = "Akash", startDegree = 123.75f, endDegree = 135.00f,
            isAuspicious = false, element = VastuElement.FIRE, padaNumber45 = 21, directionGroup = "East",
            entranceEffect = "Results in accidents, burglary and unexpected financial losses."
        ),

        // South (135°..225°)
        VastuPada(
            index = 12, code = "S1", deity = "Anil", startDegree = 135.00f, endDegree = 146.25f,
            isAuspicious = false, element = VastuElement.FIRE, padaNumber45 = 22, directionGroup = "South",
            entranceEffect = "Affects boys in house negatively; actions and thoughts go beyond parents' expectations."
        ),
        VastuPada(
            index = 13, code = "S2", deity = "Pusha", startDegree = 146.25f, endDegree = 157.50f,
            isAuspicious = false, element = VastuElement.FIRE, padaNumber45 = 23, directionGroup = "South",
            entranceEffect = "Increases tendency to work for others; supportive for professionals working in MNCs and corporates."
        ),
        VastuPada(
            index = 14, code = "S3", deity = "Vitatha", startDegree = 157.50f, endDegree = 168.75f,
            isAuspicious = true, element = VastuElement.FIRE, padaNumber45 = 24, directionGroup = "South",
            entranceEffect = "Brings immense prosperity and makes residents smart enough to get their work done."
        ),
        VastuPada(
            index = 15, code = "S4", deity = "Grihakshat", startDegree = 168.75f, endDegree = 180.00f,
            isAuspicious = true, element = VastuElement.FIRE, padaNumber45 = 25, directionGroup = "South",
            entranceEffect = "Industries are highly successful and the family is blessed with more boys in house."
        ),
        VastuPada(
            index = 16, code = "S5", deity = "Yama", startDegree = 180.00f, endDegree = 191.25f,
            isAuspicious = false, element = VastuElement.FIRE, padaNumber45 = 26, directionGroup = "South",
            entranceEffect = "People live with high debts and feel they are not making good use of their skills."
        ),
        VastuPada(
            index = 17, code = "S6", deity = "Gandharv", startDegree = 191.25f, endDegree = 202.50f,
            isAuspicious = false, element = VastuElement.EARTH, padaNumber45 = 27, directionGroup = "South",
            entranceEffect = "Door brings poverty and severe financial drain in the house."
        ),
        VastuPada(
            index = 18, code = "S7", deity = "Bhringraj", startDegree = 202.50f, endDegree = 213.75f,
            isAuspicious = false, element = VastuElement.EARTH, padaNumber45 = 28, directionGroup = "South",
            entranceEffect = "Professional and relationship efforts go in waste; people feel disconnected from others."
        ),
        VastuPada(
            index = 19, code = "S8", deity = "Mrigha", startDegree = 213.75f, endDegree = 225.00f,
            isAuspicious = false, element = VastuElement.EARTH, padaNumber45 = 29, directionGroup = "South",
            entranceEffect = "Extreme rude and attitude behaviours disconnecting from rest of the world, leading to financial and relationship hardships."
        ),

        // West (225°..315°)
        VastuPada(
            index = 20, code = "W1", deity = "Pitra", startDegree = 225.00f, endDegree = 236.25f,
            isAuspicious = false, element = VastuElement.EARTH, padaNumber45 = 30, directionGroup = "West",
            entranceEffect = "Negativity of entrance affects financial position and overall life span."
        ),
        VastuPada(
            index = 21, code = "W2", deity = "Dauwarik", startDegree = 236.25f, endDegree = 247.50f,
            isAuspicious = false, element = VastuElement.EARTH, padaNumber45 = 31, directionGroup = "West",
            entranceEffect = "Unstable career, feeling insecure in life, and lack of clarity and vision."
        ),
        VastuPada(
            index = 22, code = "W3", deity = "Sugreev", startDegree = 247.50f, endDegree = 258.75f,
            isAuspicious = true, element = VastuElement.SPACE, padaNumber45 = 32, directionGroup = "West",
            entranceEffect = "Amazing growth and prosperity across work and family life."
        ),
        VastuPada(
            index = 23, code = "W4", deity = "Puspdant", startDegree = 258.75f, endDegree = 270.00f,
            isAuspicious = true, element = VastuElement.SPACE, padaNumber45 = 33, directionGroup = "West",
            entranceEffect = "Balanced and smooth life — neither excessive profits nor heavy losses."
        ),
        VastuPada(
            index = 24, code = "W5", deity = "Varun", startDegree = 270.00f, endDegree = 281.25f,
            isAuspicious = true, element = VastuElement.SPACE, padaNumber45 = 34, directionGroup = "West",
            entranceEffect = "Makes one perfectionist and overambitious which can lead to problems in life."
        ),
        VastuPada(
            index = 25, code = "W6", deity = "Asur", startDegree = 281.25f, endDegree = 292.50f,
            isAuspicious = false, element = VastuElement.SPACE, padaNumber45 = 35, directionGroup = "West",
            entranceEffect = "Brings mental depression and emotional heaviness."
        ),
        VastuPada(
            index = 26, code = "W7", deity = "Shosha", startDegree = 292.50f, endDegree = 303.75f,
            isAuspicious = false, element = VastuElement.SPACE, padaNumber45 = 36, directionGroup = "West",
            entranceEffect = "Induces addiction tendencies (drugs/alcohol) and loss of general happiness."
        ),
        VastuPada(
            index = 27, code = "W8", deity = "Papyashma", startDegree = 303.75f, endDegree = 315.00f,
            isAuspicious = false, element = VastuElement.SPACE, padaNumber45 = 37, directionGroup = "West",
            entranceEffect = "Encourages unfair and unlawful means for personal benefit."
        ),

        // North-West back to North 0° (315°..360°)
        VastuPada(
            index = 28, code = "N1", deity = "Roga", startDegree = 315.00f, endDegree = 326.25f,
            isAuspicious = false, element = VastuElement.WATER, padaNumber45 = 38, directionGroup = "North",
            entranceEffect = "Residents in house are affected by bad intentions and ill will of other people."
        ),
        VastuPada(
            index = 29, code = "N2", deity = "Naga", startDegree = 326.25f, endDegree = 337.50f,
            isAuspicious = false, element = VastuElement.WATER, padaNumber45 = 39, directionGroup = "North",
            entranceEffect = "Residents constantly feel that other people are jealous of them."
        ),
        VastuPada(
            index = 30, code = "N3", deity = "Mukhya", startDegree = 337.50f, endDegree = 348.75f,
            isAuspicious = true, element = VastuElement.WATER, padaNumber45 = 40, directionGroup = "North",
            entranceEffect = "Auspicious entrance bringing continuous inflow of lot of money."
        ),
        VastuPada(
            index = 31, code = "N4", deity = "Bhallat", startDegree = 348.75f, endDegree = 360.00f,
            isAuspicious = true, element = VastuElement.WATER, padaNumber45 = 41, directionGroup = "North",
            entranceEffect = "Gain from ancestors' property and steady earning of money."
        )
    )

    val padasNorthToWestOrder: List<VastuPada> = listOf("N", "E", "S", "W").flatMap { dirPrefix ->
        (1..8).map { num -> padas32.first { it.code == "$dirPrefix$num" } }
    }

    /**
     * Inner 13 Devtas of the 45-Devta Mandala (Brahma #1 + 4 cardinal inner #2..#5 + 8 corner #6..#13).
     */
    val innerDevtas13: List<VastuInnerDevta> = listOf(
        VastuInnerDevta(1, "Brahma", 1, 0f, 360f, 0xFFF7E55C),
        // Layer 2: 4 Cardinal Inner Devtas (each spans 45° centered at 0°, 90°, 180°, 270°)
        VastuInnerDevta(2, "Bhudhar", 2, 337.5f, 45f, 0xFF1F7AC0),
        VastuInnerDevta(3, "Aryama", 2, 67.5f, 45f, 0xFFF5DE3A),
        VastuInnerDevta(4, "Viviswan", 2, 157.5f, 45f, 0xFFE3352D),
        VastuInnerDevta(5, "Mitra", 2, 247.5f, 45f, 0xFFE3352D),
        // Layer 3: 8 Intermediate Corner Devtas (each spans 22.5°)
        VastuInnerDevta(6, "Aapaha", 3, 22.5f, 22.5f, 0xFFE3352D),
        VastuInnerDevta(7, "Aapaha\nVatsa", 3, 45.0f, 22.5f, 0xFFE3352D),
        VastuInnerDevta(8, "Savita", 3, 112.5f, 22.5f, 0xFF1F7AC0),
        VastuInnerDevta(9, "Savitra", 3, 135.0f, 22.5f, 0xFFE3352D),
        VastuInnerDevta(10, "Indra", 3, 202.5f, 22.5f, 0xFF1F7AC0),
        VastuInnerDevta(11, "Jaya", 3, 225.0f, 22.5f, 0xFFE3352D),
        VastuInnerDevta(12, "Rudra", 3, 292.5f, 22.5f, 0xFF1F7AC0),
        VastuInnerDevta(13, "Rajya\nKshma", 3, 315.0f, 22.5f, 0xFF1F7AC0)
    )

    /**
     * Body organs & Tri-Guna band paired around the 45-Devta Mandala chart.
     */
    val organGunaSegments: List<VastuOrganGunaSegment> = listOf(
        VastuOrganGunaSegment("Bladder", 337.5f, 360f),
        VastuOrganGunaSegment("Kidney", 0f, 22.5f),
        VastuOrganGunaSegment("TAMAS", 22.5f, 33.75f, isGunaBand = true),
        VastuOrganGunaSegment("Pericardium", 33.75f, 56.25f),
        VastuOrganGunaSegment("Circulation", 56.25f, 90f),
        VastuOrganGunaSegment("Liver", 90f, 118f),
        VastuOrganGunaSegment("Gall Bladder", 118f, 140f),
        VastuOrganGunaSegment("RAJAS", 140f, 152f, isGunaBand = true),
        VastuOrganGunaSegment("Spleen", 152f, 180f),
        VastuOrganGunaSegment("Stomach", 180f, 213.75f),
        VastuOrganGunaSegment("Heart", 213.75f, 236.25f),
        VastuOrganGunaSegment("Lung", 236.25f, 258.75f),
        VastuOrganGunaSegment("SATTAV", 258.75f, 275f, isGunaBand = true),
        VastuOrganGunaSegment("Colon", 275f, 303.75f),
        VastuOrganGunaSegment("Small Intestine", 303.75f, 337.5f)
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

    fun parseManualDegrees(raw: String): Float? {
        val cleaned = raw.trim()
            .replace("°", "")
            .replace(',', '.')
        if (cleaned.isEmpty()) return null
        val match = Regex("[-+]?\\d+(?:\\.\\d+)?").find(cleaned) ?: return null
        val parsed = match.value.toFloatOrNull() ?: return null
        return normalizeDegrees(parsed)
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
     * Given a normalized point (xNorm, yNorm) in a layout/plan relative to the chakra center
     * (centerXNorm, centerYNorm) and the chakra's screen rotation in degrees (`chakraRotationDeg`,
     * where positive rotates clockwise so 0° North sits at `chakraRotationDeg`), returns the
     * exact Vastu degrees, 16-Zone, and 32-Pada at that position.
     */
    fun analyzePlanPosition(
        xNorm: Float,
        yNorm: Float,
        centerXNorm: Float = 0.5f,
        centerYNorm: Float = 0.5f,
        chakraRotationDeg: Float = 0f
    ): Triple<Float, VastuZone16, VastuPada> {
        val dx = xNorm - centerXNorm
        val dy = yNorm - centerYNorm
        // Screen angle clockwise from top (12 o'clock = 0°)
        val screenClockDeg = normalizeDegrees(Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble())).toFloat())
        val vastuDeg = normalizeDegrees(screenClockDeg - chakraRotationDeg)
        return Triple(vastuDeg, zoneForDegree(vastuDeg), padaForDegree(vastuDeg))
    }

    /**
     * Computes magnetic heading, physical tilt angles & raw geomagnetic field strength (uT)
     * from raw accelerometer (m/s^2) and magnetometer (uT) vectors.
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

        val magnetMicroTesla = sqrt(magnet[0] * magnet[0] + magnet[1] * magnet[1] + magnet[2] * magnet[2])

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

        val rotationMatrix = computeRotationMatrix(accel, magnet)
        return if (rotationMatrix != null) {
            computeOrientationFromRotationMatrix(
                rotationMatrix = rotationMatrix,
                pitchDeg = pitchDeg,
                rollDeg = rollDeg,
                tiltFromFlatDeg = tiltFromFlatDeg,
                uprightCameraMode = uprightCameraMode,
                horizonRollDeg = horizonRollDeg,
                isLevel = isLevel,
                magnetMicroTesla = magnetMicroTesla,
                displayRotation = displayRotation,
                previousAzimuthDeg = previousAzimuthDeg,
                accuracy = accuracy,
                hasHardwareSensors = hasHardwareSensors,
                smoothingAlpha = smoothingAlpha
            )
        } else {
            val fallbackAzimuth = previousAzimuthDeg ?: 0f
            CompassReading(
                azimuthDeg = fallbackAzimuth,
                pitchDeg = pitchDeg,
                rollDeg = if (uprightCameraMode) horizonRollDeg else rollDeg,
                tiltFromLevelDeg = tiltFromFlatDeg,
                isLevel = isLevel,
                isUprightCameraMode = uprightCameraMode,
                magneticFieldMicroTesla = magnetMicroTesla,
                zone = zoneForDegree(fallbackAzimuth),
                pada = padaForDegree(fallbackAzimuth),
                accuracy = accuracy,
                hasHardwareSensors = hasHardwareSensors
            )
        }
    }

    /**
     * Pure-Kotlin equivalent of Android SensorManager.getRotationMatrix(R, null, gravity, geomagnetic).
     */
    fun computeRotationMatrix(accel: FloatArray, magnet: FloatArray): FloatArray? {
        if (accel.size < 3 || magnet.size < 3) return null
        val ax = accel[0]
        val ay = accel[1]
        val az = accel[2]
        val normSqA = ax * ax + ay * ay + az * az
        if (normSqA < 0.01f) return null

        val ex = magnet[0]
        val ey = magnet[1]
        val ez = magnet[2]
        var hx = ey * az - ez * ay
        var hy = ez * ax - ex * az
        var hz = ex * ay - ey * ax
        val normH = sqrt(hx * hx + hy * hy + hz * hz)
        if (normH < 0.1f) return null

        val invH = 1f / normH
        hx *= invH
        hy *= invH
        hz *= invH

        val invA = 1f / sqrt(normSqA)
        val nax = ax * invA
        val nay = ay * invA
        val naz = az * invA

        val mx = nay * hz - naz * hy
        val my = naz * hx - nax * hz
        val mz = nax * hy - nay * hx

        return floatArrayOf(
            hx, hy, hz,
            mx, my, mz,
            nax, nay, naz
        )
    }

    /**
     * Pure-Kotlin axis remapping for flat and upright camera compass orientations.
     * Axis encoding: 1=X, 2=Y, 3=Z, negative = negated axis.
     */
    private fun remapMatrixAxes(inR: FloatArray, newXAxis: Int, newYAxis: Int): FloatArray {
        fun axisSignAndIndex(axis: Int): Pair<Float, Int> =
            if (axis < 0) -1f to ((-axis) - 1) else 1f to (axis - 1)

        val (sx, ix) = axisSignAndIndex(newXAxis)
        val (sy, iy) = axisSignAndIndex(newYAxis)
        val out = FloatArray(9)
        for (row in 0..2) {
            val base = row * 3
            out[base] = sx * inR[base + ix]
            out[base + 1] = sy * inR[base + iy]
            // 3rd column is unused for azimuth atan2(R[1], R[4]), keep normalized orthogonality if needed
            val iz = 3 - ix - iy
            out[base + 2] = inR[base + iz]
        }
        return out
    }

    /**
     * Computes CompassReading directly from a 3x3 rotation matrix (e.g. from TYPE_ROTATION_VECTOR
     * or computeRotationMatrix) with proper flat/upright and display-rotation remapping.
     */
    fun computeOrientationFromRotationMatrix(
        rotationMatrix: FloatArray,
        pitchDeg: Float? = null,
        rollDeg: Float? = null,
        tiltFromFlatDeg: Float? = null,
        uprightCameraMode: Boolean? = null,
        horizonRollDeg: Float? = null,
        isLevel: Boolean? = null,
        magnetMicroTesla: Float = 0f,
        displayRotation: Int = Surface.ROTATION_0,
        previousAzimuthDeg: Float? = null,
        accuracy: CompassAccuracy = CompassAccuracy.HIGH,
        hasHardwareSensors: Boolean = true,
        smoothingAlpha: Float = 0.25f
    ): CompassReading {
        // Gravity vector in device coordinates corresponds to 3rd row of R (elements [6], [7], [8])
        val gx = rotationMatrix[6].coerceIn(-1f, 1f)
        val gy = rotationMatrix[7].coerceIn(-1f, 1f)
        val gz = rotationMatrix[8].coerceIn(-1f, 1f)

        val calcPitch = pitchDeg ?: Math.toDegrees(asin(gy.toDouble())).toFloat()
        val calcRoll = rollDeg ?: Math.toDegrees(atan2(gx.toDouble(), gz.toDouble())).toFloat()
        val calcTiltFlat = tiltFromFlatDeg ?: Math.toDegrees(acos(gz.toDouble())).toFloat()
        val calcUpright = uprightCameraMode ?: (abs(calcPitch) > 50f)
        val calcHorizonRoll = horizonRollDeg ?: Math.toDegrees(atan2(gx.toDouble(), hypot(gy.toDouble(), gz.toDouble()))).toFloat()
        val calcIsLevel = isLevel ?: ((if (calcUpright) abs(calcHorizonRoll) else calcTiltFlat) <= 3.5f)

        // Axis indices: 1=X, 2=Y, 3=Z (negative = minus axis)
        val remapped = if (calcUpright) {
            when (displayRotation) {
                Surface.ROTATION_90 -> remapMatrixAxes(rotationMatrix, -2, -3)
                Surface.ROTATION_270 -> remapMatrixAxes(rotationMatrix, 2, -3)
                else -> remapMatrixAxes(rotationMatrix, 1, -3)
            }
        } else {
            when (displayRotation) {
                Surface.ROTATION_90 -> remapMatrixAxes(rotationMatrix, 2, -1)
                Surface.ROTATION_180 -> remapMatrixAxes(rotationMatrix, -1, -2)
                Surface.ROTATION_270 -> remapMatrixAxes(rotationMatrix, -2, 1)
                else -> rotationMatrix
            }
        }

        val rawAzimuth = normalizeDegrees(
            Math.toDegrees(atan2(remapped[1].toDouble(), remapped[4].toDouble())).toFloat()
        )

        val smoothedAzimuth = if (previousAzimuthDeg != null) {
            smoothAzimuth(previousAzimuthDeg, rawAzimuth, smoothingAlpha)
        } else {
            rawAzimuth
        }

        return CompassReading(
            azimuthDeg = smoothedAzimuth,
            pitchDeg = calcPitch,
            rollDeg = if (calcUpright) calcHorizonRoll else calcRoll,
            tiltFromLevelDeg = calcTiltFlat,
            isLevel = calcIsLevel,
            isUprightCameraMode = calcUpright,
            magneticFieldMicroTesla = magnetMicroTesla,
            zone = zoneForDegree(smoothedAzimuth),
            pada = padaForDegree(smoothedAzimuth),
            accuracy = accuracy,
            hasHardwareSensors = hasHardwareSensors
        )
    }
}
