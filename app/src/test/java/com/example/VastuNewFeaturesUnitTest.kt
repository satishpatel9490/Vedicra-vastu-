package com.example

import com.example.data.local.JsonCodec
import com.example.model.CustomLayoutPlan
import com.example.model.EraseBrushStroke
import com.example.model.EraseStrokePoint
import com.example.model.LayoutItemCategory
import com.example.model.LayoutWallLine
import com.example.model.PlacedLayoutItem
import com.example.model.VastuChakraSpec
import com.example.model.VastuChakraType
import com.example.model.VastuCompassMath
import com.example.model.WorkspaceMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VastuNewFeaturesUnitTest {

    @Test
    fun allFiveChakrasAnd45DevtaMandala_specIsComplete() {
        assertEquals(5, VastuChakraType.entries.size)
        // 13 inner + 32 outer padas = 45 Devtas
        assertEquals(13, VastuChakraSpec.innerDevtas13.size)
        assertEquals(32, VastuChakraSpec.padas32.size)
        assertEquals("Brahma", VastuChakraSpec.innerDevtas13.first().name)
        assertEquals(15, VastuChakraSpec.organGunaSegments.size)

        // All 32 padas have non-blank entrance effects and N1..W8 North-to-West ordering
        assertEquals(32, VastuChakraSpec.padasNorthToWestOrder.size)
        assertEquals("N1", VastuChakraSpec.padasNorthToWestOrder.first().code)
        assertEquals("W8", VastuChakraSpec.padasNorthToWestOrder.last().code)
        VastuChakraSpec.padas32.forEach { pada ->
            assertTrue("Missing entranceEffect for ${pada.code}", pada.entranceEffect.isNotBlank())
            assertTrue(pada.directionGroup in listOf("North", "East", "South", "West"))
        }
        assertTrue(VastuChakraSpec.padas32.first { it.code == "N4" }.entranceEffect.contains("ancestors", ignoreCase = true))
        assertTrue(VastuChakraSpec.padas32.first { it.code == "E3" }.entranceEffect.contains("profit", ignoreCase = true))
        assertTrue(VastuChakraSpec.padas32.first { it.code == "S6" }.entranceEffect.contains("poverty", ignoreCase = true))
        assertTrue(VastuChakraSpec.padas32.first { it.code == "W6" }.entranceEffect.contains("depression", ignoreCase = true))
    }

    @Test
    fun manualDegreeParser_andPlanPositionAnalysis_computeExactZonesAndPadas() {
        assertEquals(45.5f, VastuCompassMath.parseManualDegrees("45.5°")!!, 0.01f)
        assertEquals(345f, VastuCompassMath.parseManualDegrees("-15")!!, 0.01f)
        assertEquals(12f, VastuCompassMath.parseManualDegrees("372 deg")!!, 0.01f)
        assertNull(VastuCompassMath.parseManualDegrees("invalid"))

        // Top-center point (0.5, 0.1) with 0° tilt -> North (N), 0° -> N5 Soma
        val (degTop, zoneTop, padaTop) = VastuCompassMath.analyzePlanPosition(
            xNorm = 0.5f,
            yNorm = 0.1f,
            chakraRotationDeg = 0f
        )
        assertEquals(0f, degTop, 0.5f)
        assertEquals("N", zoneTop.code)
        assertEquals("N5", padaTop.code)

        // Right-center point (0.9, 0.5) is 90° screen; if chakra is tilted +22.5° clockwise,
        // Vastu degree at 90° screen becomes 67.5° -> ENE (E3 Jayant ✓)
        val (degRot, zoneRot, padaRot) = VastuCompassMath.analyzePlanPosition(
            xNorm = 0.9f,
            yNorm = 0.5f,
            chakraRotationDeg = 22.5f
        )
        assertEquals(67.5f, degRot, 0.5f)
        assertEquals("ENE", zoneRot.code)
        assertEquals("E3", padaRot.code)
        assertTrue(padaRot.isAuspicious)
    }

    @Test
    fun workspaceMetadata_backwardCompatibleJsonWithCleanCropAndScaledLayout() {
        // Legacy JSON without new fields deserializes cleanly with defaults
        val legacyJson = """{"showGridOverlay":true,"gridRotationDegrees":18.0}"""
        val decodedLegacy = JsonCodec.decodeWorkspaceMetadata(legacyJson)
        assertTrue(decodedLegacy.showGridOverlay)
        assertEquals(18f, decodedLegacy.gridRotationDegrees, 0.01f)
        assertEquals(VastuChakraType.SHAKTI_16_32, decodedLegacy.chakraType)
        assertEquals(0f, decodedLegacy.cropLeft, 0.001f)
        assertEquals(1f, decodedLegacy.cropRight, 0.001f)
        assertTrue(decodedLegacy.eraseStrokes.isEmpty())
        assertNull(decodedLegacy.customLayoutPlan)

        // Full metadata round-trip
        val rich = WorkspaceMetadata(
            showGridOverlay = true,
            gridRotationDegrees = -24.5f,
            chakraType = VastuChakraType.DEVTA_45_MANDALA,
            cropLeft = 0.1f,
            cropTop = 0.05f,
            cropRight = 0.9f,
            cropBottom = 0.95f,
            eraseStrokes = listOf(
                EraseBrushStroke(
                    points = listOf(EraseStrokePoint(0.2f, 0.3f), EraseStrokePoint(0.4f, 0.3f)),
                    radiusNorm = 0.03f,
                    colorHex = 0xFFFFFFFF
                )
            ),
            customLayoutPlan = CustomLayoutPlan(
                title = "Factory Wing A",
                topLength = 80f,
                bottomLength = 80f,
                leftLength = 50f,
                rightLength = 50f,
                northTiltDeg = 24.5f,
                chakraType = VastuChakraType.PANCH_TATVA_CYCLE,
                wallLines = listOf(LayoutWallLine(x1 = 0.2f, y1 = 0.5f, x2 = 0.8f, y2 = 0.5f)),
                items = listOf(
                    PlacedLayoutItem(category = LayoutItemCategory.MACHINE, label = "Lathe 1", xNorm = 0.7f, yNorm = 0.7f),
                    PlacedLayoutItem(category = LayoutItemCategory.TOILET, label = "Staff WC", xNorm = 0.2f, yNorm = 0.3f)
                )
            )
        )
        val encoded = JsonCodec.encodeWorkspaceMetadata(rich)
        val roundTripped = JsonCodec.decodeWorkspaceMetadata(encoded)
        assertEquals(VastuChakraType.DEVTA_45_MANDALA, roundTripped.chakraType)
        assertEquals(0.1f, roundTripped.cropLeft, 0.001f)
        assertEquals(1, roundTripped.eraseStrokes.size)
        val decodedPlan = roundTripped.customLayoutPlan
        assertNotNull(decodedPlan)
        assertEquals("Factory Wing A", decodedPlan?.title)
        assertEquals(2, decodedPlan?.items?.size)
        assertEquals(1, decodedPlan?.wallLines?.size)
        assertFalse(roundTripped.showGridOverlay.not())
    }
}
