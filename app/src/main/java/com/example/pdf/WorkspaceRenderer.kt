package com.example.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.AnnotationType
import com.example.model.GridOverlayStyle
import com.example.model.LayoutAnnotation
import com.example.model.VastuChakraSpec
import com.example.model.VastuCompassMath
import com.example.model.WorkspaceMetadata
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object WorkspaceRenderer {

    fun renderAnnotatedBitmap(
        sourceBitmap: Bitmap,
        metadata: WorkspaceMetadata
    ): Bitmap {
        val out = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(out)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

        val w = out.width.toFloat()
        val h = out.height.toFloat()
        val baseUnit = min(w, h) / 40f

        if (metadata.showGridOverlay) {
            when (metadata.overlayStyle) {
                GridOverlayStyle.VASTU_CHAKRA_32 -> {
                    drawVastuChakraOverlay(canvas, w, h, metadata.gridRotationDegrees, baseUnit)
                }
                GridOverlayStyle.CLASSIC_9_GRID -> {
                    drawVastuGridOverlay(canvas, w, h, metadata.gridRotationDegrees, baseUnit)
                }
                GridOverlayStyle.BOTH -> {
                    drawVastuGridOverlay(canvas, w, h, metadata.gridRotationDegrees, baseUnit)
                    drawVastuChakraOverlay(canvas, w, h, metadata.gridRotationDegrees, baseUnit)
                }
            }
            drawTopChakraTiltBadge(canvas, w, metadata.gridRotationDegrees, baseUnit)
        }

        metadata.annotations.forEach { annotation ->
            drawAnnotation(canvas, w, h, annotation, baseUnit)
        }
        return out
    }

    /**
     * Draws a crisp top-center North tilt & degree badge onto exported annotated layouts.
     */
    private fun drawTopChakraTiltBadge(
        canvas: Canvas,
        width: Float,
        gridRotationDeg: Float,
        baseUnit: Float
    ) {
        val topFacingDeg = VastuCompassMath.normalizeDegrees(360f - gridRotationDeg)
        val normTilt = VastuCompassMath.normalizeDegrees(gridRotationDeg)
        val topZone = VastuCompassMath.zoneForDegree(topFacingDeg)
        val topPada = VastuCompassMath.padaForDegree(topFacingDeg)
        val label = "North Tilt: ${String.format("%.1f", normTilt)}°  •  Top Faces ${String.format("%.1f", topFacingDeg)}° ${topZone.code} (${topPada.code})"

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

        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)

        // Subtle circular scrim
        fillPaint.color = Color.argb(68, 255, 255, 255)
        canvas.drawCircle(cx, cy, rTickOuter, fillPaint)

        // 1. 16 Zones (Attribute outer band + element-colored zone code band)
        VastuChakraSpec.zones16.forEach { zone ->
            val startAngle = zone.centerDegree - 11.25f - 90f
            val sweepAngle = 22.5f

            drawAndroidRingSector(
                canvas = canvas, cx = cx, cy = cy,
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
                canvas = canvas, cx = cx, cy = cy,
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
            canvas.save()
            canvas.rotate(textRot, ax, ay)
            if (attrLines.size == 2) {
                val lh = textPaint.textSize * 1.08f
                canvas.drawText(attrLines[0], ax, ay - lh * 0.2f, textPaint)
                canvas.drawText(attrLines[1], ax, ay + lh * 0.78f, textPaint)
            } else {
                val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(zone.attributeTitle, ax, ay + yOff, textPaint)
            }
            canvas.restore()

            // Zone code text
            val zMidR = (rZoneInner + rZoneOuter) / 2f
            val zx = cx + (zMidR * cos(rad)).toFloat()
            val zy = cy + (zMidR * sin(rad)).toFloat()
            textPaint.color = zone.sectorTextColorArgb.toInt()
            textPaint.textSize = (radius * 0.047f).coerceAtLeast(13f)
            canvas.save()
            canvas.rotate(textRot, zx, zy)
            val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(zone.code, zx, zy + yOff, textPaint)
            canvas.restore()
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
            canvas.save()
            canvas.rotate(textRot, px, py)
            val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(pada.code, px, py + yOff, textPaint)
            canvas.restore()

            // Radial Deity Name + ✓/✗ inside spoke
            val deityR = (rDeityOuter + rBrahma * 1.45f) / 2f
            val dx = cx + (deityR * cos(mRad)).toFloat()
            val dy = cy + (deityR * sin(mRad)).toFloat()
            textPaint.color = if (pada.isAuspicious) Color.rgb(27, 94, 32) else Color.rgb(123, 31, 31)
            textPaint.textSize = (radius * 0.031f).coerceAtLeast(9f)
            val mark = if (pada.isAuspicious) "✓" else "✗"
            val radialRot = if (pada.midDegree in 0f..180f) pada.midDegree - 90f else pada.midDegree + 90f
            canvas.save()
            canvas.rotate(radialRot, dx, dy)
            val dyOff = -(textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText("${pada.deity} $mark", dx, dy + dyOff, textPaint)
            canvas.restore()
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
                canvas.save()
                canvas.rotate(deg.toFloat(), tx, ty)
                val yOff = -(textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(labelText, tx, ty + yOff, textPaint)
                canvas.restore()
            }
        }

        // 4. Brahmasthan Center Ring
        fillPaint.color = Color.argb(225, 250, 246, 238)
        canvas.drawCircle(cx, cy, rBrahma, fillPaint)
        strokePaint.color = Color.rgb(181, 137, 46)
        strokePaint.strokeWidth = strokeMain
        canvas.drawCircle(cx, cy, rBrahma, strokePaint)
        strokePaint.color = Color.argb(175, 27, 77, 62)
        strokePaint.strokeWidth = strokeThin
        canvas.drawCircle(cx, cy, rBrahma * 0.55f, strokePaint)

        canvas.restore()
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

        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)

        // 3x3 Vastu grid box around center
        val gridHalf = radius * 0.85f
        val left = cx - gridHalf
        val top = cy - gridHalf
        val right = cx + gridHalf
        val bottom = cy + gridHalf
        canvas.drawRect(left, top, right, bottom, boxPaint)
        for (i in 1..2) {
            val gx = left + (gridHalf * 2f * i / 3f)
            val gy = top + (gridHalf * 2f * i / 3f)
            canvas.drawLine(gx, top, gx, bottom, boxPaint)
            canvas.drawLine(left, gy, right, gy, boxPaint)
        }

        // Outer compass ring & 16-zone radial spokes
        canvas.drawCircle(cx, cy, radius, circlePaint)
        canvas.drawCircle(cx, cy, radius * 0.33f, dashedPaint)

        for (i in 0 until 16) {
            val angleRad = Math.toRadians((i * 22.5 - 90.0))
            val ex = cx + (radius * cos(angleRad)).toFloat()
            val ey = cy + (radius * sin(angleRad)).toFloat()
            canvas.drawLine(cx, cy, ex, ey, if (i % 2 == 0) circlePaint else dashedPaint)
        }

        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        dirs.forEachIndexed { idx, label ->
            val angleRad = Math.toRadians((idx * 45.0 - 90.0))
            val lx = cx + ((radius + baseUnit * 1.4f) * cos(angleRad)).toFloat()
            val ly = cy + ((radius + baseUnit * 1.4f) * sin(angleRad)).toFloat()
            val pillR = baseUnit * 1.15f
            canvas.drawCircle(lx, ly, pillR, dirBgPaint)
            canvas.drawCircle(lx, ly, pillR, circlePaint)
            val yOffset = -(dirLabelPaint.descent() + dirLabelPaint.ascent()) / 2f
            canvas.drawText(label, lx, ly + yOffset, dirLabelPaint)
        }

        canvas.restore()
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
                canvas.save()
                canvas.translate(px, py)
                canvas.rotate(ann.rotationDegrees)
                val r = baseUnit * 2.1f * ann.scale
                val path = Path().apply {
                    moveTo(0f, -r)
                    lineTo(r * 0.55f, r * 0.7f)
                    lineTo(0f, r * 0.35f)
                    lineTo(-r * 0.55f, r * 0.7f)
                    close()
                }
                canvas.drawPath(path, bgPaint)
                canvas.drawPath(path, borderPaint)
                val nPaint = Paint(textPaint).apply {
                    textSize = (baseUnit * 0.95f * ann.scale).coerceAtLeast(16f)
                }
                canvas.drawText("N", 0f, -r - baseUnit * 0.35f, nPaint.apply {
                    color = accentColor
                })
                canvas.restore()
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
