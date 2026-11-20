package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompassAccuracy
import com.example.model.CompassReading
import com.example.model.VastuChakraSpec
import com.example.model.VastuCompassMath
import com.example.model.VastuPada
import com.example.model.VastuZone16
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class CameraCompassMode(val label: String) {
    FULL_CHAKRA("Chakra"),
    SIMPLE_DIAL("Dial"),
    OFF("Off")
}

/**
 * Prominent top HUD displaying live compass azimuth degree, tilt angle (how much tilted),
 * bubble level status, active 16-zone, and active 32-pada (explicitly noting 0° between N4 & N5).
 */
@Composable
fun CompassTopDegreeTiltHud(
    reading: CompassReading,
    isLocked: Boolean,
    onToggleLock: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    titlePrefix: String = "HEADING"
) {
    val normDeg = VastuCompassMath.normalizeDegrees(reading.azimuthDeg)
    val onNorthSplit = VastuCompassMath.isOnNorthBoundary(normDeg)
    val tiltDisplay = if (reading.isUprightCameraMode) {
        abs(reading.rollDeg)
    } else {
        reading.tiltFromLevelDeg
    }
    val levelColor = if (reading.isLevel) Color(0xFF4CAF50) else Color(0xFFFFC107)
    val padaColor = if (reading.pada.isAuspicious) Color(0xFF66BB6A) else Color(0xFFFF8A65)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xCC111815),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0x99D4AF37)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("compass_top_degree_hud")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Explore,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "${String.format("%.1f", normDeg)}° ${reading.zone.code}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (onNorthSplit) {
                                "$titlePrefix • 0° / 360° North (Between N4 & N5)"
                            } else {
                                "$titlePrefix • ${reading.zone.attributeTitle}"
                            },
                            color = Color(0xFFE0E6E2),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tilt readout pill (shows how much phone is tilted)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = levelColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, levelColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ScreenRotation,
                                contentDescription = "Tilt Angle",
                                tint = levelColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Tilt: ${String.format("%.1f", tiltDisplay)}°" +
                                    if (reading.isLevel) " ✓" else "",
                                color = levelColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (onToggleLock != null) {
                        IconButton(
                            onClick = onToggleLock,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (isLocked) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.14f),
                                    shape = CircleShape
                                )
                                .testTag("compass_lock_button")
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = if (isLocked) "Unlock Compass" else "Lock Heading",
                                tint = if (isLocked) Color(0xFF111815) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = padaColor.copy(alpha = 0.18f)
                ) {
                    val mark = if (reading.pada.isAuspicious) "✓ Auspicious" else "✗ Inauspicious"
                    Text(
                        text = if (onNorthSplit) {
                            "N4 Bhallat ✓  |  0°/360°  |  N5 Soma ✓"
                        } else {
                            "Pada ${reading.pada.code} • ${reading.pada.deity} ($mark)"
                        },
                        color = padaColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = if (reading.hasHardwareSensors) {
                        "Pitch ${String.format("%.0f", reading.pitchDeg)}° • ${reading.accuracy.label}"
                    } else {
                        "Manual Mode (${CompassAccuracy.MANUAL.label})"
                    },
                    color = Color(0xFFB5C0BA),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Standalone interactive Compose Canvas rendering the original watermark-free Vedicra Vastu Chakra.
 */
@Composable
fun VastuChakraCanvas(
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
    transparentFill: Boolean = true,
    showDeityNames: Boolean = true,
    showTopLubberNeedle: Boolean = false,
    tiltRollDeg: Float = 0f,
    tiltPitchDeg: Float = 0f
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        drawVastuChakraCompose(
            rotationDegrees = rotationDegrees,
            textMeasurer = textMeasurer,
            transparentFill = transparentFill,
            showDeityNames = showDeityNames,
            showTopLubberNeedle = showTopLubberNeedle,
            tiltRollDeg = tiltRollDeg,
            tiltPitchDeg = tiltPitchDeg
        )
    }
}

/**
 * Compact cardinal compass dial for users preferring minimal screen occlusion during camera capture.
 */
@Composable
fun CompactCardinalDialCanvas(
    azimuthDeg: Float,
    tiltRollDeg: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(size.width, size.height) * 0.43f

        // Outer translucent dial
        drawCircle(
            color = Color(0x44101614),
            radius = radius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFD4AF37),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )

        rotate(degrees = -azimuthDeg, pivot = Offset(cx, cy)) {
            // Tick every 15 degrees
            for (deg in 0 until 360 step 15) {
                val rad = Math.toRadians(deg.toDouble() - 90.0)
                val inner = if (deg % 45 == 0) radius * 0.84f else radius * 0.91f
                drawLine(
                    color = if (deg == 0) Color(0xFFFF5252) else Color.White.copy(alpha = 0.8f),
                    start = Offset(cx + inner * cos(rad).toFloat(), cy + inner * sin(rad).toFloat()),
                    end = Offset(cx + radius * cos(rad).toFloat(), cy + radius * sin(rad).toFloat()),
                    strokeWidth = if (deg % 45 == 0) 3.5f else 1.5f
                )
            }

            // N4 / N5 boundary highlight at 0°
            val northRad = Math.toRadians(-90.0)
            drawLine(
                color = Color(0xFFFF5252),
                start = Offset(cx, cy),
                end = Offset(cx + radius * cos(northRad).toFloat(), cy + radius * sin(northRad).toFloat()),
                strokeWidth = 3f
            )

            // 8 cardinal labels
            val cardinals = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            cardinals.forEachIndexed { i, dir ->
                val ang = i * 45.0 - 90.0
                val rad = Math.toRadians(ang)
                val lr = radius * 0.72f
                val lx = cx + (lr * cos(rad)).toFloat()
                val ly = cy + (lr * sin(rad)).toFloat()
                val res = textMeasurer.measure(
                    text = dir,
                    style = TextStyle(
                        color = if (dir == "N") Color(0xFFFF6E6E) else Color.White,
                        fontSize = if (dir.length == 1) 14.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(res, topLeft = Offset(lx - res.size.width / 2f, ly - res.size.height / 2f))
            }
        }

        // Fixed top lubber triangle
        val pointer = Path().apply {
            moveTo(cx, cy - radius - 14f)
            lineTo(cx - 10f, cy - radius - 28f)
            lineTo(cx + 10f, cy - radius - 28f)
            close()
        }
        drawPath(pointer, Color(0xFFFF5252))
        drawLine(
            color = Color(0xCCFFD54F),
            start = Offset(cx, cy - radius),
            end = Offset(cx, cy),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        )

        // Center bubble level ring
        drawCircle(
            color = if (isLevel) Color(0xFF4CAF50) else Color(0xFFFFC107),
            radius = 18f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.5f)
        )
        val bubbleDx = (tiltRollDeg / 20f).coerceIn(-1f, 1f) * 10f
        drawCircle(
            color = if (isLevel) Color(0xFF69F0AE) else Color(0xFFFFD54F),
            radius = 6f,
            center = Offset(cx + bubbleDx, cy)
        )
    }
}

/**
 * Draws the complete Original Vedicra Vastu Chakra on a Compose [DrawScope].
 * Zero watermarks or third-party branding.
 * - 360° graduated tick ring + 10° numeric labels
 * - 0° North strictly lies between N4 (348.75°..360°) and N5 (0°..11.25°)
 * - 16 Vastu Zone attribute ring + 16 Zone elemental band
 * - 32 Main-Door Pada ring (N1..W8) with auspicious (✓) / inauspicious (✗) indicators + deity names
 * - Brahmasthan center ring
 */
fun DrawScope.drawVastuChakraCompose(
    rotationDegrees: Float,
    textMeasurer: TextMeasurer,
    transparentFill: Boolean = true,
    showDeityNames: Boolean = true,
    showTopLubberNeedle: Boolean = false,
    tiltRollDeg: Float = 0f,
    tiltPitchDeg: Float = 0f
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = min(size.width, size.height) * 0.45f
    if (radius < 40f) return

    val rTickOuter = radius * 1.00f
    val rTickInner = radius * 0.93f
    val rDegreeLabelMid = radius * 0.875f
    val rAttrOuter = radius * 0.825f
    val rAttrInner = radius * 0.680f
    val rZoneOuter = radius * 0.680f
    val rZoneInner = radius * 0.575f
    val rPadaOuter = radius * 0.575f
    val rPadaInner = radius * 0.445f
    val rDeityOuter = radius * 0.445f
    val rBrahma = radius * 0.135f

    val sectorAlpha = if (transparentFill) 0.54f else 0.92f
    val cardAlpha = if (transparentFill) 0.62f else 0.95f
    val strokeMain = (radius * 0.006f).coerceIn(1.2f, 3f)
    val strokeThin = (radius * 0.003f).coerceIn(0.8f, 1.8f)

    rotate(degrees = rotationDegrees, pivot = Offset(cx, cy)) {
        // Subtle backing circle for legibility over dark camera or busy drawings
        drawCircle(
            color = if (transparentFill) Color(0x36FFFFFF) else Color(0xFFFDFBF6),
            radius = rTickOuter,
            center = Offset(cx, cy)
        )

        // 1. 16 Zones: Attribute Ring + Element-Colored Zone Code Ring
        VastuChakraSpec.zones16.forEach { zone ->
            // Screen degrees: 0° North is at -90° in Compose drawArc
            val startAngle = zone.centerDegree - 11.25f - 90f
            val sweepAngle = 22.5f

            // Outer attribute cell (ivory/translucent card)
            drawRingSector(
                cx = cx,
                cy = cy,
                innerRadius = rAttrInner,
                outerRadius = rAttrOuter,
                startAngleDeg = startAngle,
                sweepAngleDeg = sweepAngle,
                fillColor = Color(0xFFF8F4EA).copy(alpha = cardAlpha),
                borderColor = Color(0xFF28342E).copy(alpha = 0.75f),
                borderWidth = strokeThin
            )

            // Inner 16-zone element-colored band
            drawRingSector(
                cx = cx,
                cy = cy,
                innerRadius = rZoneInner,
                outerRadius = rZoneOuter,
                startAngleDeg = startAngle,
                sweepAngleDeg = sweepAngle,
                fillColor = Color(zone.sectorColorArgb).copy(alpha = sectorAlpha),
                borderColor = Color(0xFF1F2A24).copy(alpha = 0.85f),
                borderWidth = strokeThin
            )

            // Attribute text (only if readable)
            if (radius >= 95f) {
                drawZoneAttributeText(
                    zone = zone,
                    cx = cx,
                    cy = cy,
                    midRadius = (rAttrInner + rAttrOuter) / 2f,
                    chakraRadius = radius,
                    textMeasurer = textMeasurer
                )
            }

            // Zone code text (N, NNE, NE, etc.)
            drawZoneCodeText(
                zone = zone,
                cx = cx,
                cy = cy,
                midRadius = (rZoneInner + rZoneOuter) / 2f,
                chakraRadius = radius,
                transparentFill = transparentFill,
                textMeasurer = textMeasurer
            )
        }

        // 2. 32 Padas Ring (N5..N4 clockwise, 0° boundary strictly between N4 and N5)
        VastuChakraSpec.padas32.forEach { pada ->
            val startAngle = pada.startDegree - 90f
            val sweepAngle = VastuChakraSpec.PADA_SPAN_DEGREES

            val padaFill = if (pada.isAuspicious) {
                Color(0xFF4CAF50).copy(alpha = if (transparentFill) 0.74f else 0.90f)
            } else {
                Color(0xFFF7F5EE).copy(alpha = if (transparentFill) 0.58f else 0.92f)
            }
            drawRingSector(
                cx = cx,
                cy = cy,
                innerRadius = rPadaInner,
                outerRadius = rPadaOuter,
                startAngleDeg = startAngle,
                sweepAngleDeg = sweepAngle,
                fillColor = padaFill,
                borderColor = Color(0xFF1F2A24).copy(alpha = 0.85f),
                borderWidth = strokeThin
            )

            // Radial spoke from Brahmasthan to rPadaInner
            val boundaryRad = Math.toRadians(startAngle.toDouble())
            val isCardinalNorthBoundary = pada.index == 0 // 0° line between N4 and N5
            val isMajorCompassSpoke = pada.index % 4 == 0
            drawLine(
                color = when {
                    isCardinalNorthBoundary -> Color(0xFFD32F2F)
                    isMajorCompassSpoke -> Color(0xFF1B4D3E).copy(alpha = 0.85f)
                    else -> Color(0xFF37474F).copy(alpha = 0.5f)
                },
                start = Offset(
                    cx + rBrahma * cos(boundaryRad).toFloat(),
                    cy + rBrahma * sin(boundaryRad).toFloat()
                ),
                end = Offset(
                    cx + (if (isCardinalNorthBoundary) rTickOuter else rPadaInner) * cos(boundaryRad).toFloat(),
                    cy + (if (isCardinalNorthBoundary) rTickOuter else rPadaInner) * sin(boundaryRad).toFloat()
                ),
                strokeWidth = when {
                    isCardinalNorthBoundary -> strokeMain * 1.4f
                    isMajorCompassSpoke -> strokeMain
                    else -> strokeThin
                }
            )

            // Pada label (e.g. N4, N5)
            drawPadaCodeText(
                pada = pada,
                cx = cx,
                cy = cy,
                midRadius = (rPadaInner + rPadaOuter) / 2f,
                chakraRadius = radius,
                textMeasurer = textMeasurer
            )

            // Radial Deity Name + ✓/✗ inside spoke if requested & large enough
            if (showDeityNames && radius >= 120f) {
                drawPadaDeityRadialText(
                    pada = pada,
                    cx = cx,
                    cy = cy,
                    radialMid = (rDeityOuter + rBrahma * 1.45f) / 2f,
                    chakraRadius = radius,
                    transparentFill = transparentFill,
                    textMeasurer = textMeasurer
                )
            }
        }

        // 3. 360° Degree Tick Scale + 10° Labels
        drawCircle(
            color = Color(0xFF1B4D3E),
            radius = rTickOuter,
            center = Offset(cx, cy),
            style = Stroke(width = strokeMain)
        )
        drawCircle(
            color = Color(0xFF1B4D3E).copy(alpha = 0.7f),
            radius = rAttrOuter,
            center = Offset(cx, cy),
            style = Stroke(width = strokeThin)
        )

        for (deg in 0 until 360) {
            val angleRad = Math.toRadians(deg.toDouble() - 90.0)
            val tickStart = when {
                deg % 10 == 0 -> rTickOuter - (rTickOuter - rTickInner) * 1.15f
                deg % 5 == 0 -> rTickOuter - (rTickOuter - rTickInner) * 0.80f
                else -> rTickOuter - (rTickOuter - rTickInner) * 0.48f
            }
            val tickColor = when {
                deg == 0 -> Color(0xFFD32F2F)
                deg % 90 == 0 -> Color(0xFF1B4D3E)
                deg % 10 == 0 -> Color(0xFF1F2421)
                else -> Color(0xFF455A64).copy(alpha = 0.78f)
            }
            drawLine(
                color = tickColor,
                start = Offset(cx + tickStart * cos(angleRad).toFloat(), cy + tickStart * sin(angleRad).toFloat()),
                end = Offset(cx + rTickOuter * cos(angleRad).toFloat(), cy + rTickOuter * sin(angleRad).toFloat()),
                strokeWidth = when {
                    deg == 0 -> strokeMain * 1.4f
                    deg % 10 == 0 -> strokeMain
                    deg % 5 == 0 -> strokeThin * 1.2f
                    else -> strokeThin * 0.8f
                }
            )

            if (deg % 10 == 0 && radius >= 95f) {
                val tx = cx + (rDegreeLabelMid * cos(angleRad)).toFloat()
                val ty = cy + (rDegreeLabelMid * sin(angleRad)).toFloat()
                val fontSp = (radius * 0.052f).coerceIn(7f, 12f).sp
                val labelText = if (deg == 0) "0°/360°" else "$deg"
                val res = textMeasurer.measure(
                    text = labelText,
                    style = TextStyle(
                        color = if (deg == 0) Color(0xFFD32F2F) else Color(0xFF1A221E),
                        fontSize = if (deg == 0) (fontSp.value * 0.85f).sp else fontSp,
                        fontWeight = if (deg % 90 == 0) FontWeight.ExtraBold else FontWeight.Bold
                    )
                )
                rotate(degrees = deg.toFloat(), pivot = Offset(tx, ty)) {
                    drawText(res, topLeft = Offset(tx - res.size.width / 2f, ty - res.size.height / 2f))
                }
            }
        }

        // 4. Brahmasthan Sacred Center
        drawCircle(
            color = Color(0xE8FAF6EE),
            radius = rBrahma,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFB5892E),
            radius = rBrahma,
            center = Offset(cx, cy),
            style = Stroke(width = strokeMain)
        )
        drawCircle(
            color = Color(0xAA1B4D3E),
            radius = rBrahma * 0.55f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeThin)
        )
    }

    // 5. Fixed Top Lubber Pointer & Bubble Level (Camera / Interactive alignment)
    if (showTopLubberNeedle) {
        val pointerTipY = cy - rTickOuter - 2f
        val tri = Path().apply {
            moveTo(cx, pointerTipY)
            lineTo(cx - 11f, pointerTipY - 18f)
            lineTo(cx + 11f, pointerTipY - 18f)
            close()
        }
        drawPath(tri, Color(0xFFD32F2F))
        drawPath(tri, Color.White, style = Stroke(width = 1.5f))
        drawLine(
            color = Color(0xCCD32F2F),
            start = Offset(cx, cy - rTickOuter),
            end = Offset(cx, cy - rBrahma),
            strokeWidth = 2.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
        )

        // Bubble level inside Brahmasthan
        val maxBubbleShift = rBrahma * 0.45f
        val bx = (tiltRollDeg / 25f).coerceIn(-1f, 1f) * maxBubbleShift
        val by = ((tiltPitchDeg) / 90f).coerceIn(-1f, 1f) * maxBubbleShift * 0.35f
        val isLevel = abs(tiltRollDeg) <= 3.5f
        drawCircle(
            color = if (isLevel) Color(0xFF2E7D32) else Color(0xFFD4AF37),
            radius = (rBrahma * 0.28f).coerceAtLeast(4f),
            center = Offset(cx + bx, cy + by)
        )
    }
}

private fun DrawScope.drawRingSector(
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
        moveTo(
            cx + outerRadius * cos(startRad).toFloat(),
            cy + outerRadius * sin(startRad).toFloat()
        )
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius
            ),
            startAngleDegrees = startAngleDeg,
            sweepAngleDegrees = sweepAngleDeg,
            forceMoveTo = false
        )
        lineTo(
            cx + innerRadius * cos(endRad).toFloat(),
            cy + innerRadius * sin(endRad).toFloat()
        )
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius
            ),
            startAngleDegrees = startAngleDeg + sweepAngleDeg,
            sweepAngleDegrees = -sweepAngleDeg,
            forceMoveTo = false
        )
        close()
    }
    drawPath(path, fillColor)
    drawPath(path, borderColor, style = Stroke(width = borderWidth))
}

private fun DrawScope.drawZoneAttributeText(
    zone: VastuZone16,
    cx: Float,
    cy: Float,
    midRadius: Float,
    chakraRadius: Float,
    textMeasurer: TextMeasurer
) {
    val angleDeg = zone.centerDegree
    val rad = Math.toRadians(angleDeg.toDouble() - 90.0)
    val tx = cx + (midRadius * cos(rad)).toFloat()
    val ty = cy + (midRadius * sin(rad)).toFloat()
    val formatted = zone.attributeTitle.replace(" / ", "\n")
    val fontSp = (chakraRadius * 0.036f).coerceIn(5.5f, 9.5f).sp
    val res = textMeasurer.measure(
        text = formatted,
        style = TextStyle(
            color = Color(0xFF1A221E),
            fontSize = fontSp,
            lineHeight = (fontSp.value * 1.05f).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    )
    // Flip text right-side-up on the bottom half (100°..260°)
    val textRot = if (angleDeg > 100f && angleDeg < 260f) angleDeg + 180f else angleDeg
    rotate(degrees = textRot, pivot = Offset(tx, ty)) {
        drawText(res, topLeft = Offset(tx - res.size.width / 2f, ty - res.size.height / 2f))
    }
}

private fun DrawScope.drawZoneCodeText(
    zone: VastuZone16,
    cx: Float,
    cy: Float,
    midRadius: Float,
    chakraRadius: Float,
    transparentFill: Boolean,
    textMeasurer: TextMeasurer
) {
    val angleDeg = zone.centerDegree
    val rad = Math.toRadians(angleDeg.toDouble() - 90.0)
    val tx = cx + (midRadius * cos(rad)).toFloat()
    val ty = cy + (midRadius * sin(rad)).toFloat()
    val fontSp = (chakraRadius * 0.048f).coerceIn(6.5f, 12f).sp
    val textColor = if (transparentFill && zone.sectorTextColorArgb == 0xFF1F2421) {
        Color(0xFF131915)
    } else {
        Color(zone.sectorTextColorArgb)
    }
    val res = textMeasurer.measure(
        text = zone.code,
        style = TextStyle(
            color = textColor,
            fontSize = fontSp,
            fontWeight = FontWeight.ExtraBold
        )
    )
    val textRot = if (angleDeg > 100f && angleDeg < 260f) angleDeg + 180f else angleDeg
    rotate(degrees = textRot, pivot = Offset(tx, ty)) {
        drawText(res, topLeft = Offset(tx - res.size.width / 2f, ty - res.size.height / 2f))
    }
}

private fun DrawScope.drawPadaCodeText(
    pada: VastuPada,
    cx: Float,
    cy: Float,
    midRadius: Float,
    chakraRadius: Float,
    textMeasurer: TextMeasurer
) {
    val angleDeg = pada.midDegree
    val rad = Math.toRadians(angleDeg.toDouble() - 90.0)
    val tx = cx + (midRadius * cos(rad)).toFloat()
    val ty = cy + (midRadius * sin(rad)).toFloat()
    val fontSp = (chakraRadius * 0.042f).coerceIn(6f, 10.5f).sp
    val res = textMeasurer.measure(
        text = pada.code,
        style = TextStyle(
            color = if (pada.isAuspicious) Color.White else Color(0xFF1F2421),
            fontSize = fontSp,
            fontWeight = FontWeight.Bold
        )
    )
    val textRot = if (angleDeg > 100f && angleDeg < 260f) angleDeg + 180f else angleDeg
    rotate(degrees = textRot, pivot = Offset(tx, ty)) {
        drawText(res, topLeft = Offset(tx - res.size.width / 2f, ty - res.size.height / 2f))
    }
}

private fun DrawScope.drawPadaDeityRadialText(
    pada: VastuPada,
    cx: Float,
    cy: Float,
    radialMid: Float,
    chakraRadius: Float,
    transparentFill: Boolean,
    textMeasurer: TextMeasurer
) {
    val angleDeg = pada.midDegree
    val rad = Math.toRadians(angleDeg.toDouble() - 90.0)
    val tx = cx + (radialMid * cos(rad)).toFloat()
    val ty = cy + (radialMid * sin(rad)).toFloat()
    val fontSp = (chakraRadius * 0.032f).coerceIn(5.5f, 8.5f).sp
    val mark = if (pada.isAuspicious) "✓" else "✗"
    val color = when {
        pada.isAuspicious -> if (transparentFill) Color(0xFF69F0AE) else Color(0xFF1B5E20)
        else -> if (transparentFill) Color(0xFFFFD6D6) else Color(0xFF7B1F1F)
    }
    val res = textMeasurer.measure(
        text = "${pada.deity} $mark",
        style = TextStyle(
            color = color,
            fontSize = fontSp,
            fontWeight = if (pada.isAuspicious) FontWeight.Bold else FontWeight.Medium
        )
    )
    // Align text radially outward/inward neatly readable
    val radialRot = if (angleDeg in 0f..180f) angleDeg - 90f else angleDeg + 90f
    rotate(degrees = radialRot, pivot = Offset(tx, ty)) {
        drawText(res, topLeft = Offset(tx - res.size.width / 2f, ty - res.size.height / 2f))
    }
}
