package com.example.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withRotation
import androidx.core.graphics.withSave
import com.example.model.AnnotationType
import com.example.model.GridOverlayStyle
import com.example.model.LayoutAnnotation
import com.example.model.VastuChakraSpec
import com.example.model.VastuChakraType
import com.example.model.VastuCompassMath
import com.example.model.WorkspaceMetadata
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object WorkspaceRenderer {

    fun renderAnnotatedBitmap(
        sourceBitmap: Bitmap,
        metadata: WorkspaceMetadata
    ): Bitmap {
        val cLeft = metadata.cropLeft.coerceIn(0f, 0.9f)
        val cTop = metadata.cropTop.coerceIn(0f, 0.9f)
        val cRight = metadata.cropRight.coerceIn(cLeft + 0.08f, 1f)
        val cBottom = metadata.cropBottom.coerceIn(cTop + 0.08f, 1f)

        val srcW = sourceBitmap.width
        val srcH = sourceBitmap.height
        val workingBitmap = if (cLeft > 0.005f || cTop > 0.005f || cRight < 0.995f || cBottom < 0.995f) {
            val cropX = (cLeft * srcW).toInt().coerceIn(0, (srcW - 2).coerceAtLeast(0))
            val cropY = (cTop * srcH).toInt().coerceIn(0, (srcH - 2).coerceAtLeast(0))
            val cropW = ((cRight - cLeft) * srcW).toInt().coerceIn(2, srcW - cropX)
            val cropH = ((cBottom - cTop) * srcH).toInt().coerceIn(2, srcH - cropY)
            Bitmap.createBitmap(sourceBitmap, cropX, cropY, cropW, cropH)
        } else {
            sourceBitmap
        }

        val out = createBitmap(workingBitmap.width, workingBitmap.height)
        val canvas = Canvas(out)
        canvas.drawBitmap(workingBitmap, 0f, 0f, null)
        if (workingBitmap !== sourceBitmap) {
            workingBitmap.recycle()
        }

        val w = out.width.toFloat()
        val h = out.height.toFloat()
        val baseUnit = min(w, h) / 40f

        // Paint clean/erase brush strokes (remapped into cropped space if cropped)
        if (metadata.eraseStrokes.isNotEmpty()) {
            val spanX = (cRight - cLeft).coerceAtLeast(0.08f)
            val spanY = (cBottom - cTop).coerceAtLeast(0.08f)
            val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            metadata.eraseStrokes.forEach { stroke ->
                if (stroke.points.isNotEmpty()) {
                    val col = stroke.colorHex.toInt()
                    val strokePx = stroke.radiusNorm * min(w, h) * 2f
                    erasePaint.color = col
                    erasePaint.strokeWidth = strokePx
                    dotPaint.color = col
                    if (stroke.points.size == 1) {
                        val pt = stroke.points.first()
                        val px = ((pt.x - cLeft) / spanX) * w
                        val py = ((pt.y - cTop) / spanY) * h
                        canvas.drawCircle(px, py, strokePx / 2f, dotPaint)
                    } else {
                        val path = Path()
                        stroke.points.forEachIndexed { i, pt ->
                            val px = ((pt.x - cLeft) / spanX) * w
                            val py = ((pt.y - cTop) / spanY) * h
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        canvas.drawPath(path, erasePaint)
                    }
                }
            }
        }

        if (metadata.showGridOverlay) {
            when (metadata.overlayStyle) {
                GridOverlayStyle.VASTU_CHAKRA_32 -> {
                    drawVastuChakraByType(canvas, w, h, metadata.gridRotationDegrees, baseUnit, metadata.chakraType)
                }
                GridOverlayStyle.CLASSIC_9_GRID -> {
                    drawVastuGridOverlay(canvas, w, h, metadata.gridRotationDegrees, baseUnit)
                }
                GridOverlayStyle.BOTH -> {
                    drawVastuGridOverlay(canvas, w, h, metadata.gridRotationDegrees, baseUnit)
                    drawVastuChakraByType(canvas, w, h, metadata.gridRotationDegrees, baseUnit, metadata.chakraType)
                }
            }
            drawTopChakraTiltBadge(canvas, w, metadata.gridRotationDegrees, baseUnit, metadata.chakraType)
        }

        metadata.annotations.forEach { annotation ->
            drawAnnotation(canvas, w, h, annotation, baseUnit)
        }
        return out
    }

    fun drawVastuChakraByType(
        canvas: Canvas,
        width: Float,
        height: Float,
        rotationDeg: Float,
        baseUnit: Float,
        chakraType: VastuChakraType = VastuChakraType.SHAKTI_16_32
    ) {
        when (chakraType) {
            VastuChakraType.SHAKTI_16_32 -> drawVastuChakraOverlay(canvas, width, height, rotationDeg, baseUnit)
            VastuChakraType.DEVTA_45_MANDALA -> drawDevta45MandalaBitmap(canvas, width, height, rotationDeg)
            VastuChakraType.PANCH_TATVA_CYCLE -> drawPanchTatvaCycleBitmap(canvas, width, height, rotationDeg)
            VastuChakraType.MAIN_DOOR_32 -> drawMainDoor32Bitmap(canvas, width, height, rotationDeg)
        }
    }

    /**
     * Draws a crisp top-center North tilt & degree badge onto exported annotated layouts.
     */
    private fun drawTopChakraTiltBadge(
        canvas: Canvas,
        width: Float,
        gridRotationDeg: Float,
        baseUnit: Float,
        chakraType: VastuChakraType = VastuChakraType.SHAKTI_16_32
    ) {
        val topFacingDeg = VastuCompassMath.normalizeDegrees(360f - gridRotationDeg)
        val normTilt = VastuCompassMath.normalizeDegrees(gridRotationDeg)
        val topZone = VastuCompassMath.zoneForDegree(topFacingDeg)
        val topPada = VastuCompassMath.padaForDegree(topFacingDeg)
        val label = "${chakraType.shortName} • Tilt ${String.format(Locale.US, "%.1f", normTilt)}° • Top ${String.format(Locale.US, "%.1f", topFacingDeg)}° ${topZone.code} (${topPada.code})"

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (baseUnit * 0.72f).coerceAtLeast(15f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textW = textPaint.measureText(label)
        val padX = baseUnit * 0.9f
        val boxW = (textW + padX * 2f).coerceAtMost(width * 0.92f)
        val boxH = baseUnit * 1.55f
        val cx = width / 2f
        val topY = baseUnit * 0.55f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 17, 24, 21)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(212, 175, 55)
            style = Paint.Style.STROKE
            strokeWidth = (baseUnit * 0.08f).coerceAtLeast(1.5f)
        }
        val rect = RectF(cx - boxW / 2f, topY, cx + boxW / 2f, topY + boxH)
        canvas.drawRoundRect(rect, boxH * 0.45f, boxH * 0.45f, bgPaint)
        canvas.drawRoundRect(rect, boxH * 0.45f, boxH * 0.45f, borderPaint)
        val yOffset = -(textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, cx, topY + boxH / 2f + yOffset, textPaint)
    }

    /**
     * Original watermark-free 32-Pada & 16-Zone Vedicra Vastu Chakra renderer for exported bitmaps & PDFs.
     * 0° Magnetic North strictly lies between N4 and N5.
     */
    fun drawVastuChakraOverlay(
        canvas: Canvas,
        width: Float,
        height: Float,
        rotationDeg: Float,
        baseUnit: Float
    ) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.45f
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

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val strokeMain = (radius * 0.006f).coerceIn(1.5f, 4f)
        val strokeThin = (radius * 0.003f).coerceIn(1.0f, 2.2f)

        canvas.withRotation(rotationDeg, cx, cy) {
            // Subtle circular scrim
            fillPaint.color = Color.argb(68, 255, 255, 255)
            drawCircle(cx, cy, rTickOuter, fillPaint)

            // 1. 16 Zones (Attribute outer band + element-colored zone code band)
            VastuChakraSpec.zones16.forEach { zone ->
                val startAngle = zone.centerDegree - 11.25f - 90f
                val sweepAngle = 22.5f

                drawAndroidRingSector(
                    canvas = this, cx = cx, cy = cy,
                    innerRadius = rAttrInner, outerRadius = rAttrOuter,
                    startAngleDeg = startAngle, sweepAngleDeg = sweepAngle,
                    fillColor = Color.argb(175, 248, 244, 234),
                    borderColor = Color.argb(200, 40, 52, 46),
                    borderWidth = strokeThin,
                    fillPaint = fillPaint, strokePaint = strokePaint
                )

                val zc = zone.sectorColorArgb.toInt()
                val zoneAlphaColor = Color.argb(175, Color.red(zc), Color.green(zc), Color.blue(zc))
                drawAndroidRingSector(
                    canvas = this, cx = cx, cy = cy,
                    innerRadius = rZoneInner, outerRadius = rZoneOuter,
                    startAngleDeg = startAngle, sweepAngleDeg = sweepAngle,
                    fillColor = zoneAlphaColor,
                    borderColor = Color.argb(220, 31, 42, 36),
                    borderWidth = strokeThin,
                    fillPaint = fillPaint, strokePaint = strokePaint
                )

                // Attribute text (2 lines)
                val attrMidR = (rAttrInner + rAttrOuter) / 2f
                val rad = Math.toRadians(zone.centerDegree.toDouble() - 90.0)
                val ax = cx + (attrMidR * cos(rad)).toFloat()
                val ay = cy + (attrMidR * sin(rad)).toFloat()
                val attrLines = zone.attributeTitle.split(" / ")
                textPaint.color = Color.rgb(26, 34, 30)
                textPaint.textSize = (radius * 0.033f).coerceAtLeast(10f)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val textRot = if (zone.centerDegree > 100f && zone.centerDegree < 260f) zone.centerDegree + 180f else zone.centerDegree
                withRotation(textRot, ax, ay) {
                    if (attrLines.size == 2) {
                        val lh = textPaint.textSize * 1.08f
                        drawText(attrLines[0], ax, ay - lh * 0.2f, textPaint)
                        drawText(attrLines[1], ax, ay + lh * 0.78f, textPaint)
                    } else {
                        val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
                        drawText(zone.attributeTitle, ax, ay + yOff, textPaint)
                    }
                }

                // Zone code text
                val zMidR = (rZoneInner + rZoneOuter) / 2f
                val zx = cx + (zMidR * cos(rad)).toFloat()
                val zy = cy + (zMidR * sin(rad)).toFloat()
                textPaint.color = zone.sectorTextColorArgb.toInt()
                textPaint.textSize = (radius * 0.047f).coerceAtLeast(13f)
                withRotation(textRot, zx, zy) {
                    val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
                    drawText(zone.code, zx, zy + yOff, textPaint)
                }
            }

        // 2. 32 Padas Ring (0° strictly between N4 and N5)
        VastuChakraSpec.padas32.forEach { pada ->
            val startAngle = pada.startDegree - 90f
            val sweepAngle = VastuChakraSpec.PADA_SPAN_DEGREES
            val pFill = if (pada.isAuspicious) {
                Color.argb(200, 76, 175, 80)
            } else {
                Color.argb(175, 247, 245, 238)
            }
            drawAndroidRingSector(
                canvas = canvas, cx = cx, cy = cy,
                innerRadius = rPadaInner, outerRadius = rPadaOuter,
                startAngleDeg = startAngle, sweepAngleDeg = sweepAngle,
                fillColor = pFill,
                borderColor = Color.argb(215, 31, 42, 36),
                borderWidth = strokeThin,
                fillPaint = fillPaint, strokePaint = strokePaint
            )

            // Radial spoke
            val bRad = Math.toRadians(startAngle.toDouble())
            val isNorthBoundary = pada.index == 0
            val isMajorSpoke = pada.index % 4 == 0
            strokePaint.color = when {
                isNorthBoundary -> Color.rgb(211, 47, 47)
                isMajorSpoke -> Color.argb(215, 27, 77, 62)
                else -> Color.argb(135, 55, 71, 79)
            }
            strokePaint.strokeWidth = when {
                isNorthBoundary -> strokeMain * 1.4f
                isMajorSpoke -> strokeMain
                else -> strokeThin
            }
            val spokeOuter = if (isNorthBoundary) rTickOuter else rPadaInner
            canvas.drawLine(
                cx + rBrahma * cos(bRad).toFloat(),
                cy + rBrahma * sin(bRad).toFloat(),
                cx + spokeOuter * cos(bRad).toFloat(),
                cy + spokeOuter * sin(bRad).toFloat(),
                strokePaint
            )

            // Pada code
            val pMidR = (rPadaInner + rPadaOuter) / 2f
            val mRad = Math.toRadians(pada.midDegree.toDouble() - 90.0)
            val px = cx + (pMidR * cos(mRad)).toFloat()
            val py = cy + (pMidR * sin(mRad)).toFloat()
            textPaint.color = if (pada.isAuspicious) Color.WHITE else Color.rgb(31, 36, 33)
            textPaint.textSize = (radius * 0.041f).coerceAtLeast(11f)
            val textRot = if (pada.midDegree > 100f && pada.midDegree < 260f) pada.midDegree + 180f else pada.midDegree
            withRotation(textRot, px, py) {
                val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
                drawText(pada.code, px, py + yOff, textPaint)
            }

            // Radial Deity Name + ✓/✗ inside spoke
            val deityR = (rDeityOuter + rBrahma * 1.45f) / 2f
            val dx = cx + (deityR * cos(mRad)).toFloat()
            val dy = cy + (deityR * sin(mRad)).toFloat()
            textPaint.color = if (pada.isAuspicious) Color.rgb(27, 94, 32) else Color.rgb(123, 31, 31)
            textPaint.textSize = (radius * 0.031f).coerceAtLeast(9f)
            val mark = if (pada.isAuspicious) "✓" else "✗"
            val radialRot = if (pada.midDegree in 0f..180f) pada.midDegree - 90f else pada.midDegree + 90f
            withRotation(radialRot, dx, dy) {
                val dyOff = -(textPaint.descent() + textPaint.ascent()) / 2f
                drawText("${pada.deity} $mark", dx, dy + dyOff, textPaint)
            }
        }

        // 3. 360° Degree Ticks + 10° Numeric Ring
        strokePaint.color = Color.rgb(27, 77, 62)
        strokePaint.strokeWidth = strokeMain
        canvas.drawCircle(cx, cy, rTickOuter, strokePaint)
        strokePaint.color = Color.argb(180, 27, 77, 62)
        strokePaint.strokeWidth = strokeThin
        canvas.drawCircle(cx, cy, rAttrOuter, strokePaint)

        for (deg in 0 until 360) {
            val angleRad = Math.toRadians(deg.toDouble() - 90.0)
            val tickStart = when {
                deg % 10 == 0 -> rTickOuter - (rTickOuter - rTickInner) * 1.15f
                deg % 5 == 0 -> rTickOuter - (rTickOuter - rTickInner) * 0.80f
                else -> rTickOuter - (rTickOuter - rTickInner) * 0.48f
            }
            strokePaint.color = when {
                deg == 0 -> Color.rgb(211, 47, 47)
                deg % 90 == 0 -> Color.rgb(27, 77, 62)
                deg % 10 == 0 -> Color.rgb(31, 36, 33)
                else -> Color.argb(195, 69, 90, 100)
            }
            strokePaint.strokeWidth = when {
                deg == 0 -> strokeMain * 1.4f
                deg % 10 == 0 -> strokeMain
                deg % 5 == 0 -> strokeThin * 1.2f
                else -> strokeThin * 0.8f
            }
            canvas.drawLine(
                cx + tickStart * cos(angleRad).toFloat(),
                cy + tickStart * sin(angleRad).toFloat(),
                cx + rTickOuter * cos(angleRad).toFloat(),
                cy + rTickOuter * sin(angleRad).toFloat(),
                strokePaint
            )

            if (deg % 10 == 0) {
                val tx = cx + (rDegreeLabelMid * cos(angleRad)).toFloat()
                val ty = cy + (rDegreeLabelMid * sin(angleRad)).toFloat()
                textPaint.color = if (deg == 0) Color.rgb(211, 47, 47) else Color.rgb(26, 34, 30)
                textPaint.textSize = (radius * 0.048f).coerceAtLeast(11f)
                val labelText = if (deg == 0) "0°" else "$deg"
                withRotation(deg.toFloat(), tx, ty) {
                    val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
                    drawText(labelText, tx, ty + yOff, textPaint)
                }
            }
        }

        // 4. Brahmasthan Center Ring
        fillPaint.color = Color.argb(225, 250, 246, 238)
        drawCircle(cx, cy, rBrahma, fillPaint)
        strokePaint.color = Color.rgb(181, 137, 46)
        strokePaint.strokeWidth = strokeMain
        drawCircle(cx, cy, rBrahma, strokePaint)
        strokePaint.color = Color.argb(175, 27, 77, 62)
        strokePaint.strokeWidth = strokeThin
        drawCircle(cx, cy, rBrahma * 0.55f, strokePaint)
        }
    }

    private fun drawDevta45MandalaBitmap(
        canvas: Canvas,
        width: Float,
        height: Float,
        rotationDeg: Float
    ) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.455f
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

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val strokeThin = (radius * 0.003f).coerceIn(1f, 2f)
        val strokeMain = (radius * 0.006f).coerceIn(1.5f, 3.5f)

        canvas.withRotation(rotationDeg, cx, cy) {
            fillPaint.color = Color.argb(90, 255, 255, 255)
            drawCircle(cx, cy, rTickOuter, fillPaint)

            VastuChakraSpec.zones16.forEach { zone ->
                val startAngle = zone.centerDegree - 11.25f - 90f
                drawAndroidRingSector(
                    this, cx, cy, rZoneInner, rZoneOuter, startAngle, 22.5f,
                    Color.argb(200, 249, 246, 238), Color.rgb(31, 42, 36), strokeThin, fillPaint, strokePaint
                )
                val zc = zone.sectorColorArgb.toInt()
                drawAndroidRingSector(
                    this, cx, cy, rAttrInner, rAttrOuter, startAngle, 22.5f,
                    Color.argb(190, Color.red(zc), Color.green(zc), Color.blue(zc)), Color.rgb(31, 42, 36), strokeThin, fillPaint, strokePaint
                )
                drawBitmapRadialLabel(this, zone.code, zone.centerDegree, (rZoneInner + rZoneOuter) / 2f, cx, cy, (radius * 0.036f).coerceAtLeast(10f), Color.rgb(26, 34, 30), textPaint)
                drawBitmapRadialLabel(this, zone.attributeTitle.substringBefore(" /"), zone.centerDegree, (rAttrInner + rAttrOuter) / 2f, cx, cy, (radius * 0.025f).coerceAtLeast(8f), zone.sectorTextColorArgb.toInt(), textPaint)
            }

            VastuChakraSpec.organGunaSegments.forEach { seg ->
                val span = if (seg.endDegree >= seg.startDegree) seg.endDegree - seg.startDegree else 360f - seg.startDegree + seg.endDegree
                drawAndroidRingSector(
                    this, cx, cy, rOrganInner, rOrganOuter, seg.startDegree - 90f, span,
                    if (seg.isGunaBand) Color.argb(205, 220, 227, 223) else Color.argb(205, 243, 246, 244),
                    Color.rgb(55, 71, 79), strokeThin, fillPaint, strokePaint
                )
                drawBitmapRadialLabel(this, seg.label, seg.midDegree, (rOrganInner + rOrganOuter) / 2f, cx, cy, (radius * 0.023f).coerceAtLeast(8f), if (seg.isGunaBand) Color.rgb(27, 77, 62) else Color.rgb(38, 50, 56), textPaint)
            }

            VastuChakraSpec.padas32.forEach { pada ->
                val startAngle = pada.startDegree - 90f
                drawAndroidRingSector(
                    this, cx, cy, rPadaCodeInner, rPadaCodeOuter, startAngle, VastuChakraSpec.PADA_SPAN_DEGREES,
                    if (pada.isAuspicious) Color.argb(215, 76, 175, 80) else Color.argb(210, 255, 255, 255),
                    Color.rgb(31, 42, 36), strokeThin, fillPaint, strokePaint
                )
                val outerColor = when {
                    pada.isAuspicious -> Color.argb(210, 45, 156, 90)
                    pada.index % 3 == 0 -> Color.argb(210, 245, 216, 54)
                    pada.index % 3 == 1 -> Color.argb(210, 227, 53, 45)
                    else -> Color.argb(210, 31, 122, 192)
                }
                drawAndroidRingSector(
                    this, cx, cy, rDevta32Inner, rDevta32Outer, startAngle, VastuChakraSpec.PADA_SPAN_DEGREES,
                    outerColor, Color.rgb(31, 42, 36), strokeThin, fillPaint, strokePaint
                )
                drawBitmapRadialLabel(this, "${pada.code}${pada.signSymbol}", pada.midDegree, (rPadaCodeInner + rPadaCodeOuter) / 2f, cx, cy, (radius * 0.027f).coerceAtLeast(8f), if (pada.isAuspicious) Color.WHITE else Color.rgb(31, 36, 33), textPaint)
                val rad = Math.toRadians(pada.midDegree.toDouble() - 90.0)
                val midR = (rDevta32Inner + rDevta32Outer) / 2f
                val tx = cx + (midR * cos(rad)).toFloat()
                val ty = cy + (midR * sin(rad)).toFloat()
                textPaint.color = if (!pada.isAuspicious && pada.index % 3 == 0) Color.rgb(20, 24, 21) else Color.WHITE
                textPaint.textSize = (radius * 0.026f).coerceAtLeast(8f)
                val radialRot = if (pada.midDegree in 0f..180f) pada.midDegree - 90f else pada.midDegree + 90f
                withRotation(radialRot, tx, ty) {
                    val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
                    drawText(pada.deity, tx, ty + yOff, textPaint)
                }
            }

            VastuChakraSpec.innerDevtas13.filter { it.ringLayer >= 2 }.forEach { devta ->
                val dc = devta.fillColorArgb.toInt()
                drawAndroidRingSector(
                    this, cx, cy, rBrahma, rInnerRingOuter, devta.startDegree - 90f, devta.sweepDegree,
                    Color.argb(215, Color.red(dc), Color.green(dc), Color.blue(dc)),
                    Color.rgb(31, 42, 36), strokeThin, fillPaint, strokePaint
                )
                val tCol = if (devta.fillColorArgb == 0xFFF5DE3A) Color.rgb(20, 24, 21) else Color.WHITE
                drawBitmapRadialLabel(this, "${devta.devtaNumber} ${devta.name.replace("\n", " ")}", devta.midDegree, (rBrahma + rInnerRingOuter) / 2f, cx, cy, (radius * 0.025f).coerceAtLeast(8f), tCol, textPaint)
            }

            fillPaint.color = Color.argb(235, 247, 229, 92)
            drawCircle(cx, cy, rBrahma, fillPaint)
            strokePaint.color = Color.rgb(31, 42, 36)
            strokePaint.strokeWidth = strokeMain
            drawCircle(cx, cy, rBrahma, strokePaint)
            textPaint.color = Color.rgb(26, 34, 30)
            textPaint.textSize = (radius * 0.036f).coerceAtLeast(11f)
            drawText("1 BRAHMA", cx, cy - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)

            drawBitmapOuter360Scale(this, cx, cy, rTickOuter, rZoneOuter, rDegMid, radius, strokeMain, strokeThin, strokePaint, textPaint)
        }
    }

    private fun drawPanchTatvaCycleBitmap(
        canvas: Canvas,
        width: Float,
        height: Float,
        rotationDeg: Float
    ) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.45f
        if (radius < 40f) return

        val rOuterAttr = radius * 0.98f
        val rAttrInner = radius * 0.80f
        val rPadaOuter = radius * 0.80f
        val rPadaInner = radius * 0.70f
        val rElementOuter = radius * 0.70f
        val rElementInner = radius * 0.56f
        val rSupportCircle = radius * 0.47f
        val rOpposePentagram = radius * 0.34f

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val strokeThin = (radius * 0.003f).coerceIn(1f, 2f)
        val strokeMain = (radius * 0.006f).coerceIn(2f, 4f)

        canvas.withRotation(rotationDeg, cx, cy) {
            fillPaint.color = Color.argb(95, 250, 249, 245)
            drawCircle(cx, cy, rOuterAttr, fillPaint)

            VastuChakraSpec.zones16.forEach { zone ->
                val startAngle = zone.centerDegree - 11.25f - 90f
                val zc = zone.sectorColorArgb.toInt()
                drawAndroidRingSector(
                    this, cx, cy, rAttrInner, rOuterAttr, startAngle, 22.5f,
                    Color.argb(145, Color.red(zc), Color.green(zc), Color.blue(zc)), Color.rgb(31, 42, 36), strokeThin, fillPaint, strokePaint
                )
                drawBitmapRadialLabel(this, zone.code, zone.centerDegree, (rAttrInner + rOuterAttr) / 2f, cx, cy, (radius * 0.032f).coerceAtLeast(10f), Color.rgb(20, 24, 21), textPaint)
            }

            VastuChakraSpec.padas32.forEach { pada ->
                drawAndroidRingSector(
                    this, cx, cy, rPadaInner, rPadaOuter, pada.startDegree - 90f, VastuChakraSpec.PADA_SPAN_DEGREES,
                    if (pada.isAuspicious) Color.argb(215, 127, 209, 76) else Color.argb(205, 243, 246, 244),
                    Color.rgb(31, 42, 36), strokeThin, fillPaint, strokePaint
                )
                drawBitmapRadialLabel(this, pada.code, pada.midDegree, (rPadaInner + rPadaOuter) / 2f, cx, cy, (radius * 0.030f).coerceAtLeast(9f), Color.rgb(20, 24, 21), textPaint)
            }

            val elementSlices = listOf(
                Triple("WATER", 315f to 78.75f, Color.argb(215, 79, 163, 207)),
                Triple("AIR", 33.75f to 90f, Color.argb(215, 122, 193, 67)),
                Triple("FIRE", 123.75f to 67.5f, Color.argb(215, 217, 135, 110)),
                Triple("EARTH", 191.25f to 56.25f, Color.argb(215, 236, 226, 56)),
                Triple("SPACE", 247.5f to 67.5f, Color.argb(215, 239, 242, 245))
            )
            elementSlices.forEach { (name, degs, col) ->
                drawAndroidRingSector(
                    this, cx, cy, rElementInner, rElementOuter, degs.first - 90f, degs.second,
                    col, Color.rgb(31, 42, 36), strokeThin, fillPaint, strokePaint
                )
                drawBitmapRadialLabel(this, name, degs.first + degs.second / 2f, (rElementInner + rElementOuter) / 2f, cx, cy, (radius * 0.042f).coerceAtLeast(12f), Color.rgb(20, 24, 21), textPaint)
            }

            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeMain * 1.4f
                color = Color.rgb(76, 175, 80)
                strokeCap = Paint.Cap.ROUND
            }
            val supportRect = RectF(cx - rSupportCircle, cy - rSupportCircle, cx + rSupportCircle, cy + rSupportCircle)
            listOf(0f, 72f, 144f, 216f, 288f).forEach { startDeg ->
                drawArc(supportRect, startDeg + 12f - 90f, 48f, false, arcPaint)
            }

            val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeMain * 1.2f
                color = Color.rgb(224, 159, 31)
                strokeCap = Paint.Cap.ROUND
            }
            val starOrder = listOf(0f, 144f, 288f, 72f, 216f)
            for (i in starOrder.indices) {
                val a1 = Math.toRadians(starOrder[i].toDouble() - 90.0)
                val a2 = Math.toRadians(starOrder[(i + 1) % starOrder.size].toDouble() - 90.0)
                drawLine(
                    cx + rOpposePentagram * cos(a1).toFloat(),
                    cy + rOpposePentagram * sin(a1).toFloat(),
                    cx + rOpposePentagram * cos(a2).toFloat(),
                    cy + rOpposePentagram * sin(a2).toFloat(),
                    starPaint
                )
            }
            textPaint.color = Color.rgb(181, 110, 0)
            textPaint.textSize = (radius * 0.03f).coerceAtLeast(10f)
            drawText("OPPOSE", cx, cy + radius * 0.06f, textPaint)
        }
    }

    private fun drawMainDoor32Bitmap(
        canvas: Canvas,
        width: Float,
        height: Float,
        rotationDeg: Float
    ) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.44f
        if (radius < 40f) return

        val rMarkRing = radius * 0.96f
        val rDeityRim = radius * 0.83f
        val rColorBandOuter = radius * 0.75f
        val rColorBandInner = radius * 0.61f
        val rCodeInner = radius * 0.45f
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val strokeThin = (radius * 0.003f).coerceIn(1f, 2f)

        canvas.withRotation(rotationDeg, cx, cy) {
            fillPaint.color = Color.argb(95, 255, 255, 255)
            drawCircle(cx, cy, rColorBandOuter, fillPaint)

            VastuChakraSpec.padas32.forEach { pada ->
                val startAngle = pada.startDegree - 90f
                val bandColor = when {
                    pada.isAuspicious -> Color.argb(215, 76, 175, 80)
                    pada.index % 4 == 0 -> Color.argb(200, 31, 101, 179)
                    pada.index % 4 == 1 -> Color.argb(200, 95, 176, 75)
                    pada.index % 4 == 2 -> Color.argb(200, 243, 226, 58)
                    else -> Color.argb(200, 223, 92, 86)
                }
                drawAndroidRingSector(
                    this, cx, cy, rColorBandInner, rColorBandOuter, startAngle, VastuChakraSpec.PADA_SPAN_DEGREES,
                    bandColor, Color.rgb(31, 42, 36), strokeThin, fillPaint, strokePaint
                )
                drawAndroidRingSector(
                    this, cx, cy, rCodeInner, rColorBandInner, startAngle, VastuChakraSpec.PADA_SPAN_DEGREES,
                    Color.argb(215, 250, 248, 242), Color.rgb(31, 42, 36), strokeThin, fillPaint, strokePaint
                )
                val bRad = Math.toRadians(startAngle.toDouble())
                strokePaint.color = if (pada.index == 0) Color.rgb(211, 47, 47) else Color.argb(165, 55, 71, 79)
                strokePaint.strokeWidth = if (pada.index == 0) 3f else strokeThin
                drawLine(cx, cy, cx + rCodeInner * cos(bRad).toFloat(), cy + rCodeInner * sin(bRad).toFloat(), strokePaint)

                drawBitmapRadialLabel(this, pada.code, pada.midDegree, (rCodeInner + rColorBandInner) / 2f, cx, cy, (radius * 0.040f).coerceAtLeast(11f), Color.rgb(20, 24, 21), textPaint)
                drawBitmapRadialLabel(this, pada.deity, pada.midDegree, rDeityRim, cx, cy, (radius * 0.030f).coerceAtLeast(9f), Color.rgb(31, 36, 33), textPaint)
                val mRad = Math.toRadians(pada.midDegree.toDouble() - 90.0)
                textPaint.color = if (pada.isAuspicious) Color.rgb(46, 125, 50) else Color.rgb(211, 47, 47)
                textPaint.textSize = (radius * 0.052f).coerceAtLeast(14f)
                drawText(if (pada.isAuspicious) "✓" else "✗", cx + rMarkRing * cos(mRad).toFloat(), cy + rMarkRing * sin(mRad).toFloat() - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
            }
            fillPaint.color = Color.rgb(27, 77, 62)
            drawCircle(cx, cy, radius * 0.05f, fillPaint)
        }
    }


    private fun drawBitmapRadialLabel(
        canvas: Canvas,
        text: String,
        angleDeg: Float,
        midRadius: Float,
        cx: Float,
        cy: Float,
        textSizePx: Float,
        colorInt: Int,
        textPaint: Paint
    ) {
        val normAngle = VastuCompassMath.normalizeDegrees(angleDeg)
        val rad = Math.toRadians(normAngle.toDouble() - 90.0)
        val tx = cx + (midRadius * cos(rad)).toFloat()
        val ty = cy + (midRadius * sin(rad)).toFloat()
        textPaint.color = colorInt
        textPaint.textSize = textSizePx
        val textRot = if (normAngle > 100f && normAngle < 260f) normAngle + 180f else normAngle
        canvas.withRotation(textRot, tx, ty) {
            val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
            drawText(text, tx, ty + yOff, textPaint)
        }
    }

    private fun drawBitmapOuter360Scale(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        rTickOuter: Float,
        rTickInner: Float,
        rDegMid: Float,
        radius: Float,
        strokeMain: Float,
        strokeThin: Float,
        strokePaint: Paint,
        textPaint: Paint
    ) {
        strokePaint.color = Color.rgb(27, 77, 62)
        strokePaint.strokeWidth = strokeMain
        canvas.drawCircle(cx, cy, rTickOuter, strokePaint)
        for (deg in 0 until 360 step 2) {
            val angleRad = Math.toRadians(deg.toDouble() - 90.0)
            val len = if (deg % 10 == 0) (rTickOuter - rTickInner) * 0.7f else (rTickOuter - rTickInner) * 0.35f
            strokePaint.color = if (deg == 0) Color.rgb(211, 47, 47) else Color.rgb(38, 50, 56)
            strokePaint.strokeWidth = if (deg % 10 == 0) strokeThin * 1.4f else strokeThin * 0.8f
            canvas.drawLine(
                cx + (rTickOuter - len) * cos(angleRad).toFloat(),
                cy + (rTickOuter - len) * sin(angleRad).toFloat(),
                cx + rTickOuter * cos(angleRad).toFloat(),
                cy + rTickOuter * sin(angleRad).toFloat(),
                strokePaint
            )
            if (deg % 10 == 0) {
                val tx = cx + (rDegMid * cos(angleRad)).toFloat()
                val ty = cy + (rDegMid * sin(angleRad)).toFloat()
                textPaint.color = if (deg == 0) Color.rgb(211, 47, 47) else Color.rgb(26, 34, 30)
                textPaint.textSize = (radius * 0.036f).coerceAtLeast(10f)
                val lbl = if (deg == 0) "0°" else "$deg"
                canvas.withRotation(deg.toFloat(), tx, ty) {
                    val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
                    drawText(lbl, tx, ty + yOff, textPaint)
                }
            }
        }
    }

    private fun drawAndroidRingSector(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        innerRadius: Float,
        outerRadius: Float,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        fillPaint: Paint,
        strokePaint: Paint
    ) {
        val startRad = Math.toRadians(startAngleDeg.toDouble())
        val endRad = Math.toRadians((startAngleDeg + sweepAngleDeg).toDouble())
        val outerRect = RectF(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius)
        val innerRect = RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius)
        val path = Path().apply {
            moveTo(
                cx + outerRadius * cos(startRad).toFloat(),
                cy + outerRadius * sin(startRad).toFloat()
            )
            arcTo(outerRect, startAngleDeg, sweepAngleDeg, false)
            lineTo(
                cx + innerRadius * cos(endRad).toFloat(),
                cy + innerRadius * sin(endRad).toFloat()
            )
            arcTo(innerRect, startAngleDeg + sweepAngleDeg, -sweepAngleDeg, false)
            close()
        }
        fillPaint.color = fillColor
        canvas.drawPath(path, fillPaint)
        strokePaint.color = borderColor
        strokePaint.strokeWidth = borderWidth
        canvas.drawPath(path, strokePaint)
    }

    fun drawVastuGridOverlay(
        canvas: Canvas,
        width: Float,
        height: Float,
        rotationDeg: Float,
        baseUnit: Float
    ) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.44f

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (baseUnit * 0.14f).coerceAtLeast(2f)
            color = Color.argb(180, 27, 77, 62)
        }
        val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (baseUnit * 0.1f).coerceAtLeast(1.5f)
            color = Color.argb(150, 181, 137, 46)
            pathEffect = DashPathEffect(floatArrayOf(baseUnit * 0.6f, baseUnit * 0.4f), 0f)
        }
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (baseUnit * 0.08f).coerceAtLeast(1.2f)
            color = Color.argb(120, 27, 77, 62)
        }
        val dirLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(27, 77, 62)
            textSize = (baseUnit * 1.1f).coerceAtLeast(18f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dirBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 250, 246, 238)
            style = Paint.Style.FILL
        }

        canvas.withRotation(rotationDeg, cx, cy) {
            // 3x3 Vastu grid box around center
            val gridHalf = radius * 0.85f
            val left = cx - gridHalf
            val top = cy - gridHalf
            val right = cx + gridHalf
            val bottom = cy + gridHalf
            drawRect(left, top, right, bottom, boxPaint)
            for (i in 1..2) {
                val gx = left + (gridHalf * 2f * i / 3f)
                val gy = top + (gridHalf * 2f * i / 3f)
                drawLine(gx, top, gx, bottom, boxPaint)
                drawLine(left, gy, right, gy, boxPaint)
            }

            // Outer compass ring & 16-zone radial spokes
            drawCircle(cx, cy, radius, circlePaint)
            drawCircle(cx, cy, radius * 0.33f, dashedPaint)

            for (i in 0 until 16) {
                val angleRad = Math.toRadians((i * 22.5 - 90.0))
                val ex = cx + (radius * cos(angleRad)).toFloat()
                val ey = cy + (radius * sin(angleRad)).toFloat()
                drawLine(cx, cy, ex, ey, if (i % 2 == 0) circlePaint else dashedPaint)
            }

            val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            dirs.forEachIndexed { idx, label ->
                val angleRad = Math.toRadians((idx * 45.0 - 90.0))
                val lx = cx + ((radius + baseUnit * 1.4f) * cos(angleRad)).toFloat()
                val ly = cy + ((radius + baseUnit * 1.4f) * sin(angleRad)).toFloat()
                val pillR = baseUnit * 1.15f
                drawCircle(lx, ly, pillR, dirBgPaint)
                drawCircle(lx, ly, pillR, circlePaint)
                val yOffset = -(dirLabelPaint.descent() + dirLabelPaint.ascent()) / 2f
                drawText(label, lx, ly + yOffset, dirLabelPaint)
            }
        }
    }

    private fun drawAnnotation(
        canvas: Canvas,
        width: Float,
        height: Float,
        ann: LayoutAnnotation,
        baseUnit: Float
    ) {
        val px = ann.x * width
        val py = ann.y * height
        val accentColor = ann.colorHex.toInt()

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (baseUnit * 1.05f * ann.scale).coerceAtLeast(18f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = (baseUnit * 0.14f).coerceAtLeast(2f)
        }

        when (ann.type) {
            AnnotationType.DIRECTION_MARKER,
            AnnotationType.DEGREE_LABEL -> {
                val label = ann.label.ifBlank { "N" }
                val textW = textPaint.measureText(label)
                val padX = baseUnit * 0.7f
                val padY = baseUnit * 0.5f
                val halfW = (textW / 2f + padX).coerceAtLeast(baseUnit * 1.2f)
                val halfH = baseUnit * 0.85f + padY * 0.2f
                val rect = RectF(px - halfW, py - halfH, px + halfW, py + halfH)
                canvas.drawRoundRect(rect, baseUnit * 0.5f, baseUnit * 0.5f, bgPaint)
                canvas.drawRoundRect(rect, baseUnit * 0.5f, baseUnit * 0.5f, borderPaint)
                val yOffset = -(textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(label, px, py + yOffset, textPaint)
            }

            AnnotationType.NORTH_ARROW -> {
                canvas.withSave {
                    translate(px, py)
                    rotate(ann.rotationDegrees)
                    val r = baseUnit * 2.1f * ann.scale
                    val path = Path().apply {
                        moveTo(0f, -r)
                        lineTo(r * 0.55f, r * 0.7f)
                        lineTo(0f, r * 0.35f)
                        lineTo(-r * 0.55f, r * 0.7f)
                        close()
                    }
                    drawPath(path, bgPaint)
                    drawPath(path, borderPaint)
                    val nPaint = Paint(textPaint).apply {
                        textSize = (baseUnit * 0.95f * ann.scale).coerceAtLeast(16f)
                    }
                    drawText("N", 0f, -r - baseUnit * 0.35f, nPaint.apply {
                        color = accentColor
                    })
                }
            }

            AnnotationType.ROOM_LABEL,
            AnnotationType.ZONE_LABEL,
            AnnotationType.TEXT_NOTE,
            AnnotationType.AREA_NOTE -> {
                val displayText = if (ann.note.isNotBlank() && ann.label.isNotBlank()) {
                    "${ann.label}: ${ann.note}"
                } else {
                    ann.label.ifBlank { ann.note.ifBlank { "Note" } }
                }
                val labelPaint = Paint(textPaint).apply {
                    textSize = (baseUnit * 0.95f * ann.scale).coerceAtLeast(16f)
                }
                val textW = labelPaint.measureText(displayText)
                val padX = baseUnit * 0.7f
                val halfW = (textW / 2f + padX).coerceAtLeast(baseUnit * 1.6f)
                val halfH = baseUnit * 0.85f
                val rect = RectF(px - halfW, py - halfH, px + halfW, py + halfH)
                val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(235, 255, 253, 248)
                    style = Paint.Style.FILL
                }
                val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    style = Paint.Style.STROKE
                    strokeWidth = (baseUnit * 0.14f).coerceAtLeast(2f)
                }
                canvas.drawRoundRect(rect, baseUnit * 0.35f, baseUnit * 0.35f, cardBg)
                canvas.drawRoundRect(rect, baseUnit * 0.35f, baseUnit * 0.35f, cardBorder)
                val yOffset = -(labelPaint.descent() + labelPaint.ascent()) / 2f
                labelPaint.color = accentColor
                canvas.drawText(displayText, px, py + yOffset, labelPaint)
            }

            AnnotationType.ARROW -> {
                val ex = ann.endX * width
                val ey = ann.endY * height
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    strokeWidth = (baseUnit * 0.28f * ann.scale).coerceAtLeast(4f)
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                canvas.drawLine(px, py, ex, ey, linePaint)
                val angle = atan2((ey - py).toDouble(), (ex - px).toDouble())
                val headLen = baseUnit * 1.3f * ann.scale
                val p1x = ex - (headLen * cos(angle - Math.PI / 6)).toFloat()
                val p1y = ey - (headLen * sin(angle - Math.PI / 6)).toFloat()
                val p2x = ex - (headLen * cos(angle + Math.PI / 6)).toFloat()
                val p2y = ey - (headLen * sin(angle + Math.PI / 6)).toFloat()
                val headPath = Path().apply {
                    moveTo(ex, ey)
                    lineTo(p1x, p1y)
                    lineTo(p2x, p2y)
                    close()
                }
                canvas.drawPath(headPath, bgPaint)
            }

            AnnotationType.PIN_MARKER -> {
                val r = baseUnit * 0.9f * ann.scale
                canvas.drawCircle(px, py, r, bgPaint)
                canvas.drawCircle(px, py, r, borderPaint)
                if (ann.label.isNotBlank()) {
                    val yOffset = -(textPaint.descent() + textPaint.ascent()) / 2f
                    canvas.drawText(ann.label.take(3), px, py + yOffset, textPaint.apply {
                        textSize = (baseUnit * 0.75f * ann.scale).coerceAtLeast(13f)
                    })
                }
            }
        }
    }
}
