package com.example.model

import kotlinx.serialization.Serializable
import java.util.UUID

enum class AppThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SAGE_GREEN("Sage Green"),
    IVORY_GOLD("Premium Ivory & Gold")
}

enum class PdfThemeOption(val displayName: String) {
    CLASSIC_IVORY_GOLD("Classic Ivory & Gold"),
    SAGE_BOTANICAL("Sage Botanical"),
    MINIMALIST_SLATE("Minimalist Slate"),
    HIGH_CONTRAST_PRINT("High-Contrast Print")
}

enum class FontSizeOption(val displayName: String, val pdfScale: Float) {
    SMALL("Small", 0.9f),
    STANDARD("Standard", 1.0f),
    LARGE("Large", 1.15f)
}

enum class MediaKind {
    PHOTO,
    DRAWING
}

enum class AnnotationType {
    DIRECTION_MARKER,
    NORTH_ARROW,
    DEGREE_LABEL,
    ROOM_LABEL,
    ZONE_LABEL,
    TEXT_NOTE,
    AREA_NOTE,
    ARROW,
    PIN_MARKER
}

@Serializable
data class EntranceDetail(
    val id: String = UUID.randomUUID().toString(),
    val direction: String = "North",
    val label: String = "",
    val degrees: String = "",
    val notes: String = ""
)

@Serializable
data class ClientProblemItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCustom: Boolean = false
)

@Serializable
data class LayoutAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val type: AnnotationType,
    val label: String = "",
    val note: String = "",
    // Normalized coordinates [0f..1f] relative to the rotated image dimensions
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val endX: Float = 0.65f,
    val endY: Float = 0.5f,
    val rotationDegrees: Float = 0f,
    val colorHex: Long = 0xFF1B4D3E,
    val scale: Float = 1f
)

@Serializable
enum class GridOverlayStyle(val displayName: String) {
    VASTU_CHAKRA_32("Chakra Only"),
    CLASSIC_9_GRID("9-Grid Only"),
    BOTH("Chakra + 9-Grid")
}

@Serializable
data class EraseStrokePoint(
    val x: Float,
    val y: Float
)

@Serializable
data class EraseBrushStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<EraseStrokePoint> = emptyList(),
    val radiusNorm: Float = 0.028f,
    val colorHex: Long = 0xFFFFFFFF
)

@Serializable
enum class LayoutItemCategory(
    val label: String,
    val defaultColorHex: Long,
    val defaultWidthNorm: Float,
    val defaultHeightNorm: Float
) {
    ROOM("Room", 0xFF1B4D3E, 0.22f, 0.18f),
    OFFICE("Office", 0xFF1565C0, 0.20f, 0.16f),
    LABOUR("Labour Area", 0xFF6D4C41, 0.22f, 0.15f),
    MACHINE("Machine", 0xFFC62828, 0.18f, 0.14f),
    RAW_MATERIAL("Raw Material", 0xFFE65100, 0.20f, 0.15f),
    TOILET("Toilet / WC", 0xFF6A1B9A, 0.13f, 0.12f),
    ENTRANCE("Entrance", 0xFF2E7D32, 0.14f, 0.08f),
    WATER_TANK("Water Tank", 0xFF0277BD, 0.14f, 0.12f),
    SEPTIC_TANK("Septic Tank", 0xFF455A64, 0.15f, 0.12f),
    STAIRCASE("Staircase", 0xFF8D6E63, 0.16f, 0.14f),
    CASH_COUNTER("Cash / Locker", 0xFFB5892E, 0.14f, 0.11f),
    KITCHEN("Kitchen / Pantry", 0xFFD84315, 0.18f, 0.14f)
}

@Serializable
data class PlacedLayoutItem(
    val id: String = UUID.randomUUID().toString(),
    val category: LayoutItemCategory = LayoutItemCategory.ROOM,
    val label: String = "Room",
    val xNorm: Float = 0.5f,
    val yNorm: Float = 0.5f,
    val widthNorm: Float = 0.20f,
    val heightNorm: Float = 0.16f,
    val rotationDeg: Float = 0f,
    val colorHex: Long = 0xFF1B4D3E
)

@Serializable
data class LayoutWallLine(
    val id: String = UUID.randomUUID().toString(),
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val label: String = ""
)

@Serializable
data class CustomLayoutPlan(
    val title: String = "Site Vastu Layout Plan",
    val unit: String = "ft",
    val topLength: Float = 60f,
    val bottomLength: Float = 60f,
    val leftLength: Float = 40f,
    val rightLength: Float = 40f,
    val northSideLength: String = "60",
    val eastSideLength: String = "40",
    val southSideLength: String = "60",
    val westSideLength: String = "40",
    val northTiltDeg: Float = 0f,
    val showChakraOverlay: Boolean = true,
    val show9Grid: Boolean = false,
    val chakraType: VastuChakraType = VastuChakraType.SHAKTI_16_32,
    val wallLines: List<LayoutWallLine> = emptyList(),
    val items: List<PlacedLayoutItem> = emptyList()
)

@Serializable
data class WorkspaceMetadata(
    val showGridOverlay: Boolean = false,
    val gridRotationDegrees: Float = 0f,
    val overlayStyle: GridOverlayStyle = GridOverlayStyle.VASTU_CHAKRA_32,
    val chakraType: VastuChakraType = VastuChakraType.SHAKTI_16_32,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val eraseStrokes: List<EraseBrushStroke> = emptyList(),
    val customLayoutPlan: CustomLayoutPlan? = null,
    val annotations: List<LayoutAnnotation> = emptyList()
)

data class AppSettings(
    val companyName: String = "VEDICRA VASTU SOLUTIONS",
    val companyLogoPath: String = "",
    val consultantName: String = "S. SATISH PATEL",
    val contactNumber: String = "9000505678",
    val pdfTheme: PdfThemeOption = PdfThemeOption.CLASSIC_IVORY_GOLD,
    val defaultFontSize: FontSizeOption = FontSizeOption.STANDARD,
    val appTheme: AppThemeMode = AppThemeMode.IVORY_GOLD
)

object VastuConstants {
    val propertyTypes = listOf(
        "House",
        "Apartment",
        "Shop",
        "Office",
        "Commercial building",
        "Industry",
        "Factory",
        "Warehouse",
        "Plot",
        "Agricultural land",
        "Other"
    )

    val entranceDirections = listOf(
        "North",
        "Northeast",
        "East",
        "Southeast",
        "South",
        "Southwest",
        "West",
        "Northwest",
        "Other"
    )

    val eightDirectionMarkers = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    val sixteenVastuZones = listOf(
        "N (North)",
        "NNE (North-Northeast)",
        "NE (Northeast / Ishanya)",
        "ENE (East-Northeast)",
        "E (East)",
        "ESE (East-Southeast)",
        "SE (Southeast / Agneya)",
        "SSE (South-Southeast)",
        "S (South)",
        "SSW (South-Southwest)",
        "SW (Southwest / Nairutya)",
        "WSW (West-Southwest)",
        "W (West)",
        "WNW (West-Northwest)",
        "NW (Northwest / Vayavya)",
        "NNW (North-Northwest)",
        "Brahmasthan (Center)"
    )

    val swarOptions = listOf(
        "Surya / Pingala Nadi (Right)",
        "Chandra / Ida Nadi (Left)",
        "Sushumna Nadi"
    )

    val landEnergyGunas = listOf("Rajas", "Sattva", "Tamas")

    val predefinedProblems = listOf(
        "Financial problems",
        "Relationship problems",
        "Family disputes",
        "Major health problems",
        "Minor health problems",
        "Customer-related problems",
        "Sales problems",
        "Money recovery problems",
        "Outstanding payments",
        "Labour problems",
        "Fights between labourers",
        "Machinery breakdown",
        "Machinery not working to full potential",
        "Savings problems",
        "Marketing problems",
        "Profit and gains problems",
        "Lack of support",
        "Lack of attraction",
        "Children's education problems",
        "Name and fame problems",
        "Power and confidence problems",
        "Business growth problems",
        "Employee management problems",
        "Production problems",
        "Business expansion problems",
        "Legal problems",
        "Property-related problems",
        "Career problems",
        "Other problems"
    )

    val problemCategories = listOf(
        "Financial & Business",
        "Operations, Labour & Machinery",
        "Health & Wellbeing",
        "Relationships & Family",
        "Career, Fame, Legal & Property",
        "Other / Custom"
    )

    val problemToCategoryMap: Map<String, String> = mapOf(
        "Financial problems" to "Financial & Business",
        "Customer-related problems" to "Financial & Business",
        "Sales problems" to "Financial & Business",
        "Money recovery problems" to "Financial & Business",
        "Outstanding payments" to "Financial & Business",
        "Savings problems" to "Financial & Business",
        "Marketing problems" to "Financial & Business",
        "Profit and gains problems" to "Financial & Business",
        "Business growth problems" to "Financial & Business",
        "Business expansion problems" to "Financial & Business",
        "Lack of attraction" to "Financial & Business",

        "Labour problems" to "Operations, Labour & Machinery",
        "Fights between labourers" to "Operations, Labour & Machinery",
        "Machinery breakdown" to "Operations, Labour & Machinery",
        "Machinery not working to full potential" to "Operations, Labour & Machinery",
        "Employee management problems" to "Operations, Labour & Machinery",
        "Production problems" to "Operations, Labour & Machinery",

        "Major health problems" to "Health & Wellbeing",
        "Minor health problems" to "Health & Wellbeing",

        "Relationship problems" to "Relationships & Family",
        "Family disputes" to "Relationships & Family",
        "Lack of support" to "Relationships & Family",
        "Children's education problems" to "Relationships & Family",

        "Name and fame problems" to "Career, Fame, Legal & Property",
        "Power and confidence problems" to "Career, Fame, Legal & Property",
        "Legal problems" to "Career, Fame, Legal & Property",
        "Property-related problems" to "Career, Fame, Legal & Property",
        "Career problems" to "Career, Fame, Legal & Property",

        "Other problems" to "Other / Custom"
    )

    fun categoryForProblem(title: String): String {
        val exact = problemToCategoryMap[title.trim()]
        if (exact != null) return exact
        val lower = title.lowercase()
        return when {
            listOf("financ", "money", "payment", "saving", "sale", "profit", "market", "business", "customer", "cash").any { it in lower } ->
                "Financial & Business"
            listOf("labour", "labor", "machin", "product", "employ", "staff", "factory", "worker").any { it in lower } ->
                "Operations, Labour & Machinery"
            listOf("health", "ill", "pain", "stress", "sleep", "medical").any { it in lower } ->
                "Health & Wellbeing"
            listOf("relation", "family", "marriage", "dispute", "child", "education", "support").any { it in lower } ->
                "Relationships & Family"
            listOf("career", "job", "fame", "legal", "court", "property", "confidence", "power").any { it in lower } ->
                "Career, Fame, Legal & Property"
            else -> "Other / Custom"
        }
    }

    val remedyPriorities = listOf("High", "Medium", "Low")

    val waitingPeriods = listOf(
        "7 days",
        "15 days",
        "21 days",
        "30 days",
        "45 days",
        "60 days",
        "90 days",
        "3–6 months",
        "6–12 months",
        "Custom period"
    )

    val drawingCategories = listOf(
        "Plot layout drawing",
        "Building floor plan",
        "Ground floor layout",
        "First floor layout",
        "Second floor layout",
        "House room layout",
        "Shop layout",
        "Office layout",
        "Factory layout",
        "Machinery placement layout",
        "Electrical layout",
        "Other drawings"
    )

    val quickRoomLabels = listOf(
        "Main Entrance",
        "Living Room",
        "Kitchen",
        "Master Bedroom",
        "Puja Room",
        "Toilet / Bath",
        "Staircase",
        "Cash Counter",
        "Office Cabin",
        "Machinery Area",
        "Water Tank",
        "Septic Tank"
    )

    val plotShapes = listOf(
        "Square",
        "Rectangle",
        "L-Shaped",
        "Irregular",
        "Triangular",
        "Gaumukhi",
        "Shermukhi",
        "Extended Corner",
        "Cut Corner",
        "Other"
    )
}
