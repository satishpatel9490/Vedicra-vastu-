package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VastuChakraSpec
import com.example.model.VastuChakraType
import com.example.model.VastuCompassMath
import com.example.model.VastuPada
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Horizontal chip bar to switch between all clean Vedicra Vastu Chakra diagrams
 * on the same plan, compass, map, camera, or layout screen.
 */
@Composable
fun ChakraTypeSelectorRow(
    selectedType: VastuChakraType,
    onSelectType: (VastuChakraType) -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String = "chakra_selector"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VastuChakraType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onSelectType(type) },
                label = {
                    Text(
                        text = type.shortName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Medium
                    )
                },
                modifier = Modifier.testTag("${tagPrefix}_${type.name.lowercase()}")
            )
        }
    }
}

/**
 * Reusable exact numeric degree input + fine nudge (-1°/+1°/-0.5°/+0.5°) + slider
 * so users can manually tilt or rotate any chakra or grid to exact degrees.
 */
@Composable
fun ManualDegreeInputControl(
    degrees: Float,
    onDegreesChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Manual Tilt / Degree (0°–360°)",
    showSlider: Boolean = true,
    darkSurface: Boolean = false,
    tagPrefix: String = "manual_degree"
) {
    val norm = VastuCompassMath.normalizeDegrees(degrees)
    var textValue by remember {
        mutableStateOf(String.format(Locale.US, "%.1f", norm))
    }

    LaunchedEffect(norm) {
        val currentParsed = VastuCompassMath.parseManualDegrees(textValue)
        if (currentParsed == null || abs(currentParsed - norm) > 0.05f) {
            textValue = String.format(Locale.US, "%.1f", norm)
        }
    }

    fun applyTypedDegree() {
        val parsed = VastuCompassMath.parseManualDegrees(textValue)
        if (parsed != null) {
            onDegreesChange(parsed)
            textValue = String.format(Locale.US, "%.1f", parsed)
        }
    }

    val primaryText = if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurface
    val mutedText = if (darkSurface) Color(0xFFD0DBD5) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input
                    val parsed = VastuCompassMath.parseManualDegrees(input)
                    if (parsed != null && !input.endsWith(".") && !input.endsWith("-")) {
                        onDegreesChange(parsed)
                    }
                },
                label = { Text(label, maxLines = 1) },
                suffix = { Text("°") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { applyTypedDegree() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_input")
            )

            Button(
                onClick = { applyTypedDegree() },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("${tagPrefix}_apply_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Set", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Fine degree nudge buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                -5f to "-5°",
                -1f to "-1°",
                -0.5f to "-0.5°",
                0.5f to "+0.5°",
                1f to "+1°",
                5f to "+5°"
            ).forEach { (delta, text) ->
                Surface(
                    onClick = {
                        val next = VastuCompassMath.normalizeDegrees(norm + delta)
                        onDegreesChange(next)
                        textValue = String.format(Locale.US, "%.1f", next)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (darkSurface) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (darkSurface) Color(0x66D4AF37) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText
                        )
                    }
                }
            }
        }

        if (showSlider) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val next = VastuCompassMath.normalizeDegrees(norm - 15f)
                        onDegreesChange(next)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.RotateLeft, contentDescription = "Rotate -15°", modifier = Modifier.size(16.dp))
                }
                Slider(
                    value = norm,
                    onValueChange = { onDegreesChange(VastuCompassMath.normalizeDegrees(it)) },
                    valueRange = 0f..360f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${tagPrefix}_slider")
                )
                OutlinedButton(
                    onClick = {
                        val next = VastuCompassMath.normalizeDegrees(norm + 15f)
                        onDegreesChange(next)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.RotateRight, contentDescription = "Rotate +15°", modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = "Current: ${String.format(Locale.US, "%.1f", norm)}° • ${VastuCompassMath.zoneForDegree(norm).code} (${VastuCompassMath.padaForDegree(norm).code})",
                style = MaterialTheme.typography.labelSmall,
                color = mutedText
            )
        }
    }
}

/**
 * Unified Compose DrawScope dispatcher rendering any of the 5 clean Vedicra Vastu Chakras.
 */
fun DrawScope.drawChakraByTypeCompose(
    chakraType: VastuChakraType,
    rotationDegrees: Float,
    textMeasurer: TextMeasurer,
    transparentFill: Boolean = true,
    showDeityNames: Boolean = true,
    showTopLubberNeedle: Boolean = false,
    tiltRollDeg: Float = 0f,
    tiltPitchDeg: Float = 0f
) {
    when (chakraType) {
        VastuChakraType.SHAKTI_16_32 -> drawVastuChakraCompose(
            rotationDegrees = rotationDegrees,
            textMeasurer = textMeasurer,
            transparentFill = transparentFill,
            showDeityNames = showDeityNames,
            showTopLubberNeedle = showTopLubberNeedle,
            tiltRollDeg = tiltRollDeg,
            tiltPitchDeg = tiltPitchDeg
        )
        VastuChakraType.DEVTA_45_MANDALA -> drawDevta45MandalaCompose(
            rotationDegrees = rotationDegrees,
            textMeasurer = textMeasurer,
            transparentFill = transparentFill,
            showTopLubberNeedle = showTopLubberNeedle
        )
        VastuChakraType.PANCH_TATVA_CYCLE -> drawPanchTatvaCycleCompose(
            rotationDegrees = rotationDegrees,
            textMeasurer = textMeasurer,
            transparentFill = transparentFill,
            showTopLubberNeedle = showTopLubberNeedle
        )
        VastuChakraType.MAIN_DOOR_32 -> drawMainDoor32ChakraCompose(
            rotationDegrees = rotationDegrees,
            textMeasurer = textMeasurer,
            transparentFill = transparentFill,
            showTopLubberNeedle = showTopLubberNeedle
        )
        VastuChakraType.PANCH_TATVA_COMPASS -> drawPanchTatvaCompassDialCompose(
            rotationDegrees = rotationDegrees,
            textMeasurer = textMeasurer,
            transparentFill = transparentFill,
            showTopLubberNeedle = showTopLubberNeedle
        )
    }
}

/**
 * 45-Devta Vastu Purusha Mandala Chakra (Clean Vector — Zero Watermarks).
 * Brahma (1) center -> 4 Cardinal Inner Devas (2..5) -> 8 Intermediate Corner Devas (6..13) ->
 * 32 Outer Pada Devas (14..45) -> N1..W8 -> 16 Zone Attributes -> Organ & Tri-Guna band -> 360° ticks.
 */
fun DrawScope.drawDevta45MandalaCompose(
    rotationDegrees: Float,
    textMeasurer: TextMeasurer,
    transparentFill: Boolean,
    showTopLubberNeedle: Boolean
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = min(size.width, size.height) * 0.455f
    if (radius < 40f) return

    val rTickOuter = radius * 1.00f
    val rDegMid = radius * 0.90f
    val rZoneOuter = radius * 0.855f
    val rZoneInner = radius * 0.775f
    val rOrganOuter = radius * 0.775f
    val rOrganInner = radius * 0.715f
    val rAttrOuter = radius * 0.715f
    val rAttrInner = radius * 0.615f
    val rPadaCodeOuter = radius * 0.615f
    val rPadaCodeInner = radius * 0.535f
    val rDevta32Outer = radius * 0.535f
    val rDevta32Inner = radius * 0.365f
    val rInnerRingOuter = radius * 0.365f
    val rBrahma = radius * 0.175f

    val fillAlpha = if (transparentFill) 0.68f else 0.94f
    val strokeThin = (radius * 0.003f).coerceIn(0.8f, 1.8f)
    val strokeMain = (radius * 0.006f).coerceIn(1.3f, 2.8f)

    rotate(degrees = rotationDegrees, pivot = Offset(cx, cy)) {
        drawCircle(
            color = if (transparentFill) Color(0x38FFFFFF) else Color(0xFFFDFBF6),
            radius = rTickOuter,
            center = Offset(cx, cy)
        )

        // 1. 16 Zones (Zone Code + Attribute Ring)
        VastuChakraSpec.zones16.forEach { zone ->
            val startAngle = zone.centerDegree - 11.25f - 90f
            drawRingSectorArc(
                cx, cy, rZoneInner, rZoneOuter, startAngle, 22.5f,
                fillColor = Color(0xFFF9F6EE).copy(alpha = fillAlpha),
                borderColor = Color(0xFF1F2A24), borderWidth = strokeThin
            )
            drawRingSectorArc(
                cx, cy, rAttrInner, rAttrOuter, startAngle, 22.5f,
                fillColor = Color(zone.sectorColorArgb).copy(alpha = fillAlpha),
                borderColor = Color(0xFF1F2A24), borderWidth = strokeThin
            )
            drawRadialBannerText(
                text = zone.code,
                angleDeg = zone.centerDegree,
                midRadius = (rZoneInner + rZoneOuter) / 2f,
                cx = cx, cy = cy,
                fontSp = (radius * 0.040f).coerceIn(5.5f, 10.5f),
                color = Color(0xFF1A221E),
                textMeasurer = textMeasurer
            )
            if (radius >= 95f) {
                drawRadialBannerText(
                    text = zone.attributeTitle.replace(" / ", "\n"),
                    angleDeg = zone.centerDegree,
                    midRadius = (rAttrInner + rAttrOuter) / 2f,
                    cx = cx, cy = cy,
                    fontSp = (radius * 0.026f).coerceIn(4.8f, 7.8f),
                    color = Color(zone.sectorTextColorArgb),
                    textMeasurer = textMeasurer
                )
            }
        }

        // 2. Organ & Tri-Guna Band
        VastuChakraSpec.organGunaSegments.forEach { seg ->
            val span = if (seg.endDegree >= seg.startDegree) seg.endDegree - seg.startDegree else 360f - seg.startDegree + seg.endDegree
            val startAngle = seg.startDegree - 90f
            drawRingSectorArc(
                cx, cy, rOrganInner, rOrganOuter, startAngle, span,
                fillColor = if (seg.isGunaBand) Color(0xFFDCE3DF).copy(alpha = fillAlpha) else Color(0xFFF3F6F4).copy(alpha = fillAlpha),
                borderColor = Color(0xFF37474F), borderWidth = strokeThin
            )
            if (radius >= 100f) {
                drawRadialBannerText(
                    text = seg.label,
                    angleDeg = seg.midDegree,
                    midRadius = (rOrganInner + rOrganOuter) / 2f,
                    cx = cx, cy = cy,
                    fontSp = (radius * 0.025f).coerceIn(4.5f, 7.2f),
                    color = if (seg.isGunaBand) Color(0xFF1B4D3E) else Color(0xFF263238),
                    textMeasurer = textMeasurer
                )
            }
        }

        // 3. 32 Pada Codes (N5..N4) + 32 Outer Devtas (#14..#45 with +/-)
        VastuChakraSpec.padas32.forEach { pada ->
            val startAngle = pada.startDegree - 90f
            val codeFill = if (pada.isAuspicious) {
                Color(0xFF4CAF50).copy(alpha = fillAlpha)
            } else {
                Color(0xFFFFFFFF).copy(alpha = fillAlpha)
            }
            drawRingSectorArc(
                cx, cy, rPadaCodeInner, rPadaCodeOuter, startAngle, VastuChakraSpec.PADA_SPAN_DEGREES,
                fillColor = codeFill, borderColor = Color(0xFF1F2A24), borderWidth = strokeThin
            )

            // 32 Outer Devta color rotation inspired by classic 45-Devta chart
            val outerColor = when {
                pada.isAuspicious -> Color(0xFF2D9C5A)
                pada.index % 3 == 0 -> Color(0xFFF5D836)
                pada.index % 3 == 1 -> Color(0xFFE3352D)
                else -> Color(0xFF1F7AC0)
            }.copy(alpha = fillAlpha)
            drawRingSectorArc(
                cx, cy, rDevta32Inner, rDevta32Outer, startAngle, VastuChakraSpec.PADA_SPAN_DEGREES,
                fillColor = outerColor, borderColor = Color(0xFF1F2A24), borderWidth = strokeThin
            )

            drawRadialBannerText(
                text = "${pada.code}\n${pada.signSymbol}",
                angleDeg = pada.midDegree,
                midRadius = (rPadaCodeInner + rPadaCodeOuter) / 2f,
                cx = cx, cy = cy,
                fontSp = (radius * 0.028f).coerceIn(5f, 8.2f),
                color = if (pada.isAuspicious) Color.White else Color(0xFF1F2421),
                textMeasurer = textMeasurer
            )

            if (radius >= 110f) {
                val rad = Math.toRadians(pada.midDegree.toDouble() - 90.0)
                val midR = (rDevta32Inner + rDevta32Outer) / 2f
                val tx = cx + (midR * cos(rad)).toFloat()
                val ty = cy + (midR * sin(rad)).toFloat()
                val textColor = if (outerColor.red > 0.7f && outerColor.green > 0.7f) Color(0xFF111815) else Color.White
                val res = textMeasurer.measure(
                    text = pada.deity,
                    style = TextStyle(
                        color = textColor,
                        fontSize = (radius * 0.027f).coerceIn(5f, 7.6f).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                val radialRot = if (pada.midDegree in 0f..180f) pada.midDegree - 90f else pada.midDegree + 90f
                rotate(degrees = radialRot, pivot = Offset(tx, ty)) {
                    drawText(res, topLeft = Offset(tx - res.size.width / 2f, ty - res.size.height / 2f))
                }
            }
        }

        // 4. Inner 12 Devtas (Layers 2 & 3)
        VastuChakraSpec.innerDevtas13.filter { it.ringLayer >= 2 }.forEach { devta ->
            drawRingSectorArc(
                cx = cx,
                cy = cy,
                innerRadius = rBrahma,
                outerRadius = rInnerRingOuter,
                startAngleDeg = devta.startDegree - 90f,
                sweepAngleDeg = devta.sweepDegree,
                fillColor = Color(devta.fillColorArgb).copy(alpha = fillAlpha),
                borderColor = Color(0xFF1F2A24),
                borderWidth = strokeThin
            )
            if (radius >= 95f) {
                val midR = (rBrahma + rInnerRingOuter) / 2f
                val fontSp = if (devta.ringLayer == 2) (radius * 0.036f).coerceIn(5.5f, 9.5f) else (radius * 0.025f).coerceIn(4.8f, 7.2f)
                val textColor = if (devta.fillColorArgb == 0xFFF5DE3A) Color(0xFF111815) else Color.White
                drawRadialBannerText(
                    text = "${devta.devtaNumber}\n${devta.name}",
                    angleDeg = devta.midDegree,
                    midRadius = midR,
                    cx = cx, cy = cy,
                    fontSp = fontSp,
                    color = textColor,
                    textMeasurer = textMeasurer
                )
            }
        }

        // 5. Center 1 Brahma (Gold/Yellow)
        drawCircle(color = Color(0xFFF7E55C).copy(alpha = fillAlpha), radius = rBrahma, center = Offset(cx, cy))
        drawCircle(color = Color(0xFF1F2A24), radius = rBrahma, center = Offset(cx, cy), style = Stroke(strokeMain))
        val brahmaRes = textMeasurer.measure(
            text = "1 BRAHMA",
            style = TextStyle(
                color = Color(0xFF1A221E),
                fontSize = (radius * 0.036f).coerceIn(6f, 10f).sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
        drawText(brahmaRes, topLeft = Offset(cx - brahmaRes.size.width / 2f, cy - brahmaRes.size.height / 2f))

        // 6. Outer 360° Degree Ticks
        drawOuter360Scale(cx, cy, rTickOuter, rZoneOuter, rDegMid, radius, strokeMain, strokeThin, textMeasurer)
    }

    if (showTopLubberNeedle) {
        drawTopLubberMarker(cx, cy, rTickOuter, rBrahma)
    }
}

/**
 * Clean Panch Tatva Cycle Chakra (Support Green Outer Arrows + Oppose 5-Point Star).
 */
fun DrawScope.drawPanchTatvaCycleCompose(
    rotationDegrees: Float,
    textMeasurer: TextMeasurer,
    transparentFill: Boolean,
    showTopLubberNeedle: Boolean
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = min(size.width, size.height) * 0.45f
    if (radius < 40f) return

    val rOuterAttr = radius * 0.98f
    val rAttrInner = radius * 0.80f
    val rPadaOuter = radius * 0.80f
    val rPadaInner = radius * 0.70f
    val rElementOuter = radius * 0.70f
    val rElementInner = radius * 0.56f
    val rSupportCircle = radius * 0.47f
    val rOpposePentagram = radius * 0.34f

    val fillAlpha = if (transparentFill) 0.72f else 0.95f
    val strokeThin = (radius * 0.003f).coerceIn(0.8f, 1.8f)
    val strokeMain = (radius * 0.006f).coerceIn(1.5f, 3.0f)

    rotate(degrees = rotationDegrees, pivot = Offset(cx, cy)) {
        drawCircle(
            color = if (transparentFill) Color(0x38FFFFFF) else Color(0xFFFAF9F5),
            radius = rOuterAttr,
            center = Offset(cx, cy)
        )

        // 1. 16 Zones Attribute Rim
        VastuChakraSpec.zones16.forEach { zone ->
            val startAngle = zone.centerDegree - 11.25f - 90f
            drawRingSectorArc(
                cx, cy, rAttrInner, rOuterAttr, startAngle, 22.5f,
                fillColor = Color(zone.sectorColorArgb).copy(alpha = fillAlpha * 0.55f),
                borderColor = Color(0xFF1F2A24), borderWidth = strokeThin
            )
            if (radius >= 90f) {
                drawRadialBannerText(
                    text = "${zone.code}\n${zone.attributeTitle.substringBefore(" /")}",
                    angleDeg = zone.centerDegree,
                    midRadius = (rAttrInner + rOuterAttr) / 2f,
                    cx = cx, cy = cy,
                    fontSp = (radius * 0.032f).coerceIn(5.2f, 8.8f),
                    color = Color(0xFF111815),
                    textMeasurer = textMeasurer
                )
            }
        }

        // 2. 32 Padas Strip
        VastuChakraSpec.padas32.forEach { pada ->
            val startAngle = pada.startDegree - 90f
            drawRingSectorArc(
                cx, cy, rPadaInner, rPadaOuter, startAngle, VastuChakraSpec.PADA_SPAN_DEGREES,
                fillColor = if (pada.isAuspicious) Color(0xFF7FD14C).copy(alpha = fillAlpha) else Color(0xFFF3F6F4).copy(alpha = fillAlpha),
                borderColor = Color(0xFF1F2A24), borderWidth = strokeThin
            )
            drawRadialBannerText(
                text = pada.code,
                angleDeg = pada.midDegree,
                midRadius = (rPadaInner + rPadaOuter) / 2f,
                cx = cx, cy = cy,
                fontSp = (radius * 0.030f).coerceIn(5.2f, 8.5f),
                color = Color(0xFF111815),
                textMeasurer = textMeasurer
            )
        }

        // 3. 5 Panch Tatva Element Arcs (matching Water -> Air -> Fire -> Earth -> Space)
        data class TatvaArc(val name: String, val startDeg: Float, val sweepDeg: Float, val color: Color)
        val arcs = listOf(
            TatvaArc("WATER", 315f, 78.75f, Color(0xFF4FA3CF)),
            TatvaArc("AIR", 33.75f, 90f, Color(0xFF7AC143)),
            TatvaArc("FIRE", 123.75f, 67.5f, Color(0xFFD9876E)),
            TatvaArc("EARTH", 191.25f, 56.25f, Color(0xFFECE238)),
            TatvaArc("SPACE", 247.5f, 67.5f, Color(0xFFEFF2F5))
        )
        arcs.forEach { arc ->
            drawRingSectorArc(
                cx, cy, rElementInner, rElementOuter, arc.startDeg - 90f, arc.sweepDeg,
                fillColor = arc.color.copy(alpha = fillAlpha),
                borderColor = Color(0xFF1F2A24), borderWidth = strokeThin
            )
            val midDeg = arc.startDeg + arc.sweepDeg / 2f
            drawRadialBannerText(
                text = arc.name,
                angleDeg = midDeg,
                midRadius = (rElementInner + rElementOuter) / 2f,
                cx = cx, cy = cy,
                fontSp = (radius * 0.044f).coerceIn(6.5f, 11.5f),
                color = Color(0xFF111815),
                textMeasurer = textMeasurer
            )
        }

        // 4. Inner Support Green Cycle Arcs (Clockwise)
        val supportVerticesDeg = listOf(0f, 72f, 144f, 216f, 288f)
        supportVerticesDeg.forEach { startDeg ->
            val arcStart = startDeg + 12f - 90f
            val arcSweep = 48f
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = arcStart,
                sweepAngle = arcSweep,
                useCenter = false,
                topLeft = Offset(cx - rSupportCircle, cy - rSupportCircle),
                size = androidx.compose.ui.geometry.Size(rSupportCircle * 2f, rSupportCircle * 2f),
                style = Stroke(width = strokeMain * 1.3f, cap = StrokeCap.Round)
            )
            // Arrow head at end of arc
            val tipDeg = startDeg + 60f - 90f
            val tipRad = Math.toRadians(tipDeg.toDouble())
            val tipX = cx + rSupportCircle * cos(tipRad).toFloat()
            val tipY = cy + rSupportCircle * sin(tipRad).toFloat()
            val tangentAngle = tipRad + Math.PI / 2.0
            drawArrowHead(tipX, tipY, tangentAngle, (radius * 0.045f).coerceAtLeast(8f), Color(0xFF4CAF50))
            // "Support" tiny label
            val lblDeg = startDeg + 36f
            drawRadialBannerText(
                text = "Support",
                angleDeg = lblDeg,
                midRadius = rSupportCircle - radius * 0.055f,
                cx = cx, cy = cy,
                fontSp = (radius * 0.028f).coerceIn(5.2f, 8.2f),
                color = Color(0xFF2E7D32),
                textMeasurer = textMeasurer
            )
        }

        // 5. Golden 5-Point Oppose Star Arrows (0° -> 144° -> 288° -> 72° -> 216° -> 0°)
        val starOrder = listOf(0f, 144f, 288f, 72f, 216f)
        for (i in starOrder.indices) {
            val a1 = Math.toRadians(starOrder[i].toDouble() - 90.0)
            val a2 = Math.toRadians(starOrder[(i + 1) % starOrder.size].toDouble() - 90.0)
            val p1 = Offset(cx + rOpposePentagram * cos(a1).toFloat(), cy + rOpposePentagram * sin(a1).toFloat())
            val p2 = Offset(cx + rOpposePentagram * cos(a2).toFloat(), cy + rOpposePentagram * sin(a2).toFloat())
            // Shorten end slightly for clean arrowhead
            val ang = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
            drawLine(
                color = Color(0xFFE09F1F),
                start = p1,
                end = p2,
                strokeWidth = strokeMain * 1.15f,
                cap = StrokeCap.Round
            )
            drawArrowHead(p2.x, p2.y, ang, (radius * 0.040f).coerceAtLeast(7f), Color(0xFFE09F1F))
        }
        drawCircle(color = Color(0xFFE09F1F), radius = radius * 0.025f, center = Offset(cx, cy))
        val oppRes = textMeasurer.measure(
            text = "OPPOSE",
            style = TextStyle(color = Color(0xFFB56E00), fontSize = (radius * 0.026f).coerceIn(5f, 8f).sp, fontWeight = FontWeight.Bold)
        )
        drawText(oppRes, topLeft = Offset(cx - oppRes.size.width / 2f, cy + radius * 0.04f))
    }

    if (showTopLubberNeedle) {
        drawTopLubberMarker(cx, cy, rOuterAttr, rOpposePentagram * 0.25f)
    }
}

/**
 * Clean 32-Pada Main Door Auspicious (✓) / Inauspicious (✗) Chakra.
 */
fun DrawScope.drawMainDoor32ChakraCompose(
    rotationDegrees: Float,
    textMeasurer: TextMeasurer,
    transparentFill: Boolean,
    showTopLubberNeedle: Boolean
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = min(size.width, size.height) * 0.44f
    if (radius < 40f) return

    val rMarkRing = radius * 0.96f
    val rDeityRim = radius * 0.83f
    val rColorBandOuter = radius * 0.75f
    val rColorBandInner = radius * 0.61f
    val rCodeInner = radius * 0.45f
    val rCenterDot = radius * 0.06f

    val alpha = if (transparentFill) 0.72f else 0.94f
    val strokeThin = (radius * 0.003f).coerceIn(0.8f, 1.8f)

    rotate(degrees = rotationDegrees, pivot = Offset(cx, cy)) {
        drawCircle(
            color = if (transparentFill) Color(0x36FFFFFF) else Color(0xFFFFFFFF),
            radius = rColorBandOuter,
            center = Offset(cx, cy)
        )

        VastuChakraSpec.padas32.forEach { pada ->
            val startAngle = pada.startDegree - 90f
            val bandColor = when (pada.index % 4) {
                0 -> Color(0xFF1F65B3)
                1 -> Color(0xFF5FB04B)
                2 -> Color(0xFFF3E23A)
                else -> Color(0xFFDF5C56)
            }.let {
                if (pada.isAuspicious) Color(0xFF4CAF50) else it
            }.copy(alpha = alpha)

            drawRingSectorArc(
                cx, cy, rColorBandInner, rColorBandOuter, startAngle, VastuChakraSpec.PADA_SPAN_DEGREES,
                fillColor = bandColor, borderColor = Color(0xFF1F2A24), borderWidth = strokeThin
            )
            drawRingSectorArc(
                cx, cy, rCodeInner, rColorBandInner, startAngle, VastuChakraSpec.PADA_SPAN_DEGREES,
                fillColor = Color(0xFFFAF8F2).copy(alpha = alpha), borderColor = Color(0xFF1F2A24), borderWidth = strokeThin
            )

            // Radial spoke right to center
            val bRad = Math.toRadians(startAngle.toDouble())
            drawLine(
                color = if (pada.index == 0) Color(0xFFD32F2F) else Color(0xFF37474F).copy(alpha = 0.7f),
                start = Offset(cx, cy),
                end = Offset(cx + rCodeInner * cos(bRad).toFloat(), cy + rCodeInner * sin(bRad).toFloat()),
                strokeWidth = if (pada.index == 0) 2.5f else strokeThin
            )

            // Pada Code (N1..W8)
            drawRadialBannerText(
                text = pada.code,
                angleDeg = pada.midDegree,
                midRadius = (rCodeInner + rColorBandInner) / 2f,
                cx = cx, cy = cy,
                fontSp = (radius * 0.042f).coerceIn(6f, 10.5f),
                color = Color(0xFF111815),
                textMeasurer = textMeasurer
            )

            // Deity Name outside rim
            if (radius >= 95f) {
                drawRadialBannerText(
                    text = pada.deity,
                    angleDeg = pada.midDegree,
                    midRadius = rDeityRim,
                    cx = cx, cy = cy,
                    fontSp = (radius * 0.030f).coerceIn(5.2f, 8.2f),
                    color = if (transparentFill) Color(0xFFFAFAFA) else Color(0xFF1F2421),
                    textMeasurer = textMeasurer
                )
            }

            // Green ✓ or Red ✗ outer badge
            val mRad = Math.toRadians(pada.midDegree.toDouble() - 90.0)
            val mx = cx + (rMarkRing * cos(mRad)).toFloat()
            val my = cy + (rMarkRing * sin(mRad)).toFloat()
            val markRes = textMeasurer.measure(
                text = if (pada.isAuspicious) "✓" else "✗",
                style = TextStyle(
                    color = if (pada.isAuspicious) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                    fontSize = (radius * 0.055f).coerceIn(8f, 14f).sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            drawText(markRes, topLeft = Offset(mx - markRes.size.width / 2f, my - markRes.size.height / 2f))
        }

        drawCircle(color = Color(0xFF1B4D3E), radius = rCenterDot, center = Offset(cx, cy))
        drawCircle(color = Color(0xFFD4AF37), radius = rCenterDot * 0.45f, center = Offset(cx, cy))
    }

    if (showTopLubberNeedle) {
        drawTopLubberMarker(cx, cy, rColorBandOuter, rCenterDot)
    }
}

/**
 * Clean Panch Tatva Compass Dial (Ether center + Water/Air/Fire/Earth gradient ring + needle).
 */
fun DrawScope.drawPanchTatvaCompassDialCompose(
    rotationDegrees: Float,
    textMeasurer: TextMeasurer,
    transparentFill: Boolean,
    showTopLubberNeedle: Boolean
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = min(size.width, size.height) * 0.45f
    if (radius < 40f) return

    val rOuter = radius * 0.98f
    val rRingOuter = radius * 0.87f
    val rRingInner = radius * 0.69f
    val rCenter = radius * 0.19f
    val ringMid = (rRingInner + rRingOuter) / 2f
    val ringWidth = rRingOuter - rRingInner

    rotate(degrees = rotationDegrees, pivot = Offset(cx, cy)) {
        // Gradient Sweep Ring for Water (N) -> Air (E) -> Fire (S) -> Earth (W)
        val sweepBrush = Brush.sweepGradient(
            0.00f to Color(0xFF5EB37E), // East-ish in screen angle (0 rad = +X = East)
            0.16f to Color(0xFFD94A3E), // Fire (SE/S)
            0.35f to Color(0xFFEAD438), // Earth (SW)
            0.52f to Color(0xFFDFE4E8), // Air/Space (W/NW)
            0.75f to Color(0xFF3E83C4), // Water (N)
            1.00f to Color(0xFF5EB37E),
            center = Offset(cx, cy)
        )
        drawCircle(
            brush = sweepBrush,
            radius = ringMid,
            center = Offset(cx, cy),
            style = Stroke(width = ringWidth),
            alpha = if (transparentFill) 0.78f else 0.95f
        )
        drawCircle(color = Color(0xFF1F2421), radius = rRingOuter, center = Offset(cx, cy), style = Stroke(2f))
        drawCircle(color = Color(0xFF1F2421), radius = rRingInner, center = Offset(cx, cy), style = Stroke(2f))

        // 5 Element Labels around ring
        listOf(
            35f to "Water",
            128f to "Fire",
            222f to "Earth",
            310f to "Air"
        ).forEach { (deg, name) ->
            drawRadialBannerText(
                text = name,
                angleDeg = deg,
                midRadius = ringMid,
                cx = cx, cy = cy,
                fontSp = (radius * 0.056f).coerceIn(8f, 13f),
                color = Color(0xFF111815),
                textMeasurer = textMeasurer
            )
        }

        // Cardinal N, E, S, W ticks + labels
        listOf(0f to "N", 90f to "E", 180f to "S", 270f to "W").forEach { (deg, d) ->
            drawRadialBannerText(
                text = d,
                angleDeg = deg,
                midRadius = ringMid,
                cx = cx, cy = cy,
                fontSp = (radius * 0.070f).coerceIn(10f, 16f),
                color = if (d == "N") Color(0xFFD32F2F) else Color(0xFF111815),
                textMeasurer = textMeasurer
            )
        }

        // 360° Fine outer scale
        drawOuter360Scale(cx, cy, rOuter, rRingOuter, (rOuter + rRingOuter) / 2f, radius, 2f, 1f, textMeasurer)

        // 4-point Compass Rose Needle (Red North tip)
        val needleR = rRingInner * 0.88f
        val baseHalf = rCenter * 0.34f
        // North (Red)
        val northNeedle = Path().apply {
            moveTo(cx, cy - needleR)
            lineTo(cx + baseHalf, cy)
            lineTo(cx - baseHalf, cy)
            close()
        }
        drawPath(northNeedle, Color(0xFFD32F2F))
        // East, South, West (Deep Navy)
        listOf(90f, 180f, 270f).forEach { rotDeg ->
            rotate(degrees = rotDeg, pivot = Offset(cx, cy)) {
                val p = Path().apply {
                    moveTo(cx, cy - needleR * 0.88f)
                    lineTo(cx + baseHalf, cy)
                    lineTo(cx - baseHalf, cy)
                    close()
                }
                drawPath(p, Color(0xFF1A3A54))
            }
        }

        // Ether Center Hub
        drawCircle(color = Color(0xEEFDFBF6), radius = rCenter, center = Offset(cx, cy))
        drawCircle(color = Color(0xFF1F2A24), radius = rCenter, center = Offset(cx, cy), style = Stroke(2f))
        val etherRes = textMeasurer.measure(
            text = "Ether",
            style = TextStyle(color = Color(0xFF111815), fontSize = (radius * 0.052f).coerceIn(8f, 13f).sp, fontWeight = FontWeight.Bold)
        )
        drawText(etherRes, topLeft = Offset(cx - etherRes.size.width / 2f, cy - etherRes.size.height / 2f))
    }

    if (showTopLubberNeedle) {
        drawTopLubberMarker(cx, cy, rOuter, rCenter)
    }
}

private fun DrawScope.drawRingSectorArc(
    cx: Float,
    cy: Float,
    innerRadius: Float,
    outerRadius: Float,
    startAngleDeg: Float,
    sweepAngleDeg: Float,
    fillColor: Color,
    borderColor: Color,
    borderWidth: Float
) {
    val startRad = Math.toRadians(startAngleDeg.toDouble())
    val endRad = Math.toRadians((startAngleDeg + sweepAngleDeg).toDouble())
    val path = Path().apply {
        moveTo(cx + outerRadius * cos(startRad).toFloat(), cy + outerRadius * sin(startRad).toFloat())
        arcTo(
            rect = Rect(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius),
            startAngleDegrees = startAngleDeg,
            sweepAngleDegrees = sweepAngleDeg,
            forceMoveTo = false
        )
        lineTo(cx + innerRadius * cos(endRad).toFloat(), cy + innerRadius * sin(endRad).toFloat())
        arcTo(
            rect = Rect(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius),
            startAngleDegrees = startAngleDeg + sweepAngleDeg,
            sweepAngleDegrees = -sweepAngleDeg,
            forceMoveTo = false
        )
        close()
    }
    drawPath(path, fillColor)
    drawPath(path, borderColor, style = Stroke(width = borderWidth))
}

private fun DrawScope.drawRadialBannerText(
    text: String,
    angleDeg: Float,
    midRadius: Float,
    cx: Float,
    cy: Float,
    fontSp: Float,
    color: Color,
    textMeasurer: TextMeasurer
) {
    val normAngle = VastuCompassMath.normalizeDegrees(angleDeg)
    val rad = Math.toRadians(normAngle.toDouble() - 90.0)
    val tx = cx + (midRadius * cos(rad)).toFloat()
    val ty = cy + (midRadius * sin(rad)).toFloat()
    val res = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = fontSp.sp,
            lineHeight = (fontSp * 1.05f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )
    val textRot = if (normAngle > 100f && normAngle < 260f) normAngle + 180f else normAngle
    rotate(degrees = textRot, pivot = Offset(tx, ty)) {
        drawText(res, topLeft = Offset(tx - res.size.width / 2f, ty - res.size.height / 2f))
    }
}

private fun DrawScope.drawOuter360Scale(
    cx: Float,
    cy: Float,
    rTickOuter: Float,
    rTickInner: Float,
    rDegMid: Float,
    chakraRadius: Float,
    strokeMain: Float,
    strokeThin: Float,
    textMeasurer: TextMeasurer
) {
    drawCircle(color = Color(0xFF1B4D3E), radius = rTickOuter, center = Offset(cx, cy), style = Stroke(strokeMain))
    for (deg in 0 until 360 step 2) {
        val angleRad = Math.toRadians(deg.toDouble() - 90.0)
        val len = when {
            deg % 10 == 0 -> (rTickOuter - rTickInner) * 0.72f
            else -> (rTickOuter - rTickInner) * 0.36f
        }
        drawLine(
            color = if (deg == 0) Color(0xFFD32F2F) else Color(0xFF263238),
            start = Offset(cx + (rTickOuter - len) * cos(angleRad).toFloat(), cy + (rTickOuter - len) * sin(angleRad).toFloat()),
            end = Offset(cx + rTickOuter * cos(angleRad).toFloat(), cy + rTickOuter * sin(angleRad).toFloat()),
            strokeWidth = if (deg % 10 == 0) strokeThin * 1.3f else strokeThin * 0.7f
        )
        if (deg % 10 == 0 && chakraRadius >= 95f) {
            val tx = cx + (rDegMid * cos(angleRad)).toFloat()
            val ty = cy + (rDegMid * sin(angleRad)).toFloat()
            val res = textMeasurer.measure(
                text = if (deg == 0) "0°" else "$deg",
                style = TextStyle(
                    color = if (deg == 0) Color(0xFFD32F2F) else Color(0xFF1A221E),
                    fontSize = (chakraRadius * 0.036f).coerceIn(5.5f, 9.5f).sp,
                    fontWeight = FontWeight.Bold
                )
            )
            rotate(degrees = deg.toFloat(), pivot = Offset(tx, ty)) {
                drawText(res, topLeft = Offset(tx - res.size.width / 2f, ty - res.size.height / 2f))
            }
        }
    }
}

private fun DrawScope.drawArrowHead(tipX: Float, tipY: Float, angleRad: Double, headLen: Float, color: Color) {
    val p1x = tipX - (headLen * cos(angleRad - Math.PI / 6.0)).toFloat()
    val p1y = tipY - (headLen * sin(angleRad - Math.PI / 6.0)).toFloat()
    val p2x = tipX - (headLen * cos(angleRad + Math.PI / 6.0)).toFloat()
    val p2y = tipY - (headLen * sin(angleRad + Math.PI / 6.0)).toFloat()
    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(p1x, p1y)
        lineTo(p2x, p2y)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawTopLubberMarker(cx: Float, cy: Float, rOuter: Float, rInner: Float) {
    val pointerTipY = cy - rOuter - 2f
    val tri = Path().apply {
        moveTo(cx, pointerTipY)
        lineTo(cx - 10f, pointerTipY - 16f)
        lineTo(cx + 10f, pointerTipY - 16f)
        close()
    }
    drawPath(tri, Color(0xFFD32F2F))
    drawPath(tri, Color.White, style = Stroke(width = 1.4f))
    drawLine(
        color = Color(0xCCD32F2F),
        start = Offset(cx, cy - rOuter),
        end = Offset(cx, cy - rInner),
        strokeWidth = 2.0f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 6f))
    )
}
