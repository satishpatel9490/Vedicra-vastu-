package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.data.local.JsonCodec
import com.example.data.repository.VastuRepository
import com.example.model.CompassAccuracy
import com.example.model.CompassReading
import com.example.model.GridOverlayStyle
import com.example.model.MediaKind
import com.example.model.VastuChakraType
import com.example.model.VastuCompassMath
import com.example.model.WorkspaceMetadata
import com.example.pdf.WorkspaceRenderer
import com.example.sensor.rememberVastuCompassState
import com.example.ui.components.ChakraTypeSelectorRow
import com.example.ui.components.CompassTopDegreeTiltHud
import com.example.ui.components.ManualDegreeInputControl
import com.example.ui.components.VastuChakraCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

private enum class MapLayerStyle(val id: String, val label: String) {
    HYBRID("hybrid", "Google Hybrid"),
    SATELLITE("satellite", "Satellite"),
    STREET("street", "Street")
}

private enum class MapHeadingSource(val label: String) {
    MAP_WALL_ALIGN("Map Wall Align"),
    LIVE_SENSOR("Live Sensor"),
    MANUAL("Manual °")
}

private class MapJsBridge(
    private val onViewportChanged: (Double, Double, Double, Double) -> Unit
) {
    @JavascriptInterface
    fun onMapTransform(lat: Double, lng: Double, zoom: Double, bearingDeg: Double) {
        onViewportChanged(lat, lng, zoom, bearingDeg)
    }
}

/**
 * Formats signed tilt difference between Map True North and the aligned site boundary walls.
 * Positive mapBearingDeg = user rotated map clockwise -> site top axis lies CCW (West of North).
 * Negative mapBearingDeg = user rotated map counter-clockwise -> site top axis lies CW (East of North).
 */
fun formatWallTiltDifferenceLabel(mapBearingDeg: Float): String {
    val norm = VastuCompassMath.normalizeDegrees(-mapBearingDeg)
    val signedDiff = if (norm <= 180f) norm else norm - 360f
    return when {
        abs(signedDiff) < 0.2f -> "0.0° Aligned to True North"
        signedDiff > 0f -> "${String.format(Locale.US, "%.1f", signedDiff)}° CW (East of North)"
        else -> "${String.format(Locale.US, "%.1f", abs(signedDiff))}° CCW (West of North)"
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapCompassAlignScreen(
    reportId: String,
    initialChakraType: VastuChakraType = VastuChakraType.SHAKTI_16_32,
    repository: VastuRepository,
    onBack: () -> Unit,
    onOpenWorkspace: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    val liveCompass by rememberVastuCompassState(enabled = true)
    var headingSource by remember { mutableStateOf(MapHeadingSource.MAP_WALL_ALIGN) }
    var mapBearingDeg by remember { mutableFloatStateOf(0f) }
    var manualDegree by remember { mutableFloatStateOf(0f) }
    var isLocked by remember { mutableStateOf(false) }
    var lockedReading by remember { mutableStateOf<CompassReading?>(null) }
    var selectedChakraType by remember { mutableStateOf(initialChakraType) }
    var showDegreeDrawer by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var layerStyle by remember { mutableStateOf(MapLayerStyle.HYBRID) }
    var mapLat by remember { mutableDoubleStateOf(17.385044) }
    var mapLng by remember { mutableDoubleStateOf(78.486671) }
    var gpsAccuracyMeters by remember { mutableStateOf<Float?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val activeReading = remember(
        liveCompass,
        headingSource,
        mapBearingDeg,
        manualDegree,
        isLocked,
        lockedReading
    ) {
        val base = when (headingSource) {
            MapHeadingSource.MAP_WALL_ALIGN -> {
                val alignedDeg = VastuCompassMath.normalizeDegrees(-mapBearingDeg)
                liveCompass.copy(
                    azimuthDeg = alignedDeg,
                    zone = VastuCompassMath.zoneForDegree(alignedDeg),
                    pada = VastuCompassMath.padaForDegree(alignedDeg),
                    accuracy = CompassAccuracy.HIGH
                )
            }
            MapHeadingSource.LIVE_SENSOR -> {
                if (liveCompass.hasHardwareSensors) {
                    liveCompass
                } else {
                    liveCompass.copy(
                        azimuthDeg = manualDegree,
                        zone = VastuCompassMath.zoneForDegree(manualDegree),
                        pada = VastuCompassMath.padaForDegree(manualDegree),
                        accuracy = CompassAccuracy.MANUAL
                    )
                }
            }
            MapHeadingSource.MANUAL -> {
                liveCompass.copy(
                    azimuthDeg = manualDegree,
                    zone = VastuCompassMath.zoneForDegree(manualDegree),
                    pada = VastuCompassMath.padaForDegree(manualDegree),
                    accuracy = CompassAccuracy.MANUAL
                )
            }
        }
        if (isLocked) lockedReading ?: base else base
    }

    val wallTiltSummary = remember(mapBearingDeg, headingSource, activeReading.azimuthDeg) {
        val effectiveMapBearing = when (headingSource) {
            MapHeadingSource.MAP_WALL_ALIGN -> mapBearingDeg
            else -> -activeReading.azimuthDeg
        }
        formatWallTiltDifferenceLabel(effectiveMapBearing)
    }

    fun setJsMapBearing(deg: Float) {
        mapBearingDeg = deg
        headingSource = MapHeadingSource.MAP_WALL_ALIGN
        webViewRef?.evaluateJavascript("if (window.setMapBearing) window.setMapBearing($deg);", null)
    }

    fun centerMapOn(lat: Double, lng: Double, zoom: Int = 19) {
        mapLat = lat
        mapLng = lng
        webViewRef?.evaluateJavascript("if (window.setMapCenter) window.setMapCenter($lat, $lng, $zoom);", null)
    }

    @SuppressLint("MissingPermission")
    fun requestSingleGpsFix() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val bestLast: Location? = providers.mapNotNull { p ->
            runCatching { lm.getLastKnownLocation(p) }.getOrNull()
        }.minByOrNull { it.accuracy }

        if (bestLast != null) {
            gpsAccuracyMeters = bestLast.accuracy
            centerMapOn(bestLast.latitude, bestLast.longitude, 19)
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                gpsAccuracyMeters = location.accuracy
                centerMapOn(location.latitude, location.longitude, 19)
                runCatching { lm.removeUpdates(this) }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        providers.forEach { p ->
            if (runCatching { lm.isProviderEnabled(p) }.getOrDefault(false)) {
                runCatching { lm.requestLocationUpdates(p, 500L, 1f, listener) }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            requestSingleGpsFix()
        } else {
            onShowMessage("GPS permission denied — search location or pan map manually.")
        }
    }

    val uploadPlanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val degLabel = VastuCompassMath.formatHeadingLabel(activeReading.azimuthDeg)
                val added = repository.importGalleryImages(
                    reportId = reportId,
                    uris = listOf(uri),
                    mediaKind = MediaKind.DRAWING,
                    drawingCategory = "Plot layout drawing",
                    drawingTitle = "Floor Plan (Map Alignment $degLabel)",
                    orientationDirection = degLabel,
                    description = "Aligned near ${String.format(Locale.US, "%.5f", mapLat)}, ${String.format(Locale.US, "%.5f", mapLng)} • Tilt: $wallTiltSummary"
                )
                val first = added.firstOrNull()
                if (first != null) {
                    val meta = WorkspaceMetadata(
                        showGridOverlay = true,
                        gridRotationDegrees = -activeReading.azimuthDeg,
                        overlayStyle = GridOverlayStyle.VASTU_CHAKRA_32,
                        chakraType = selectedChakraType
                    )
                    repository.updateMedia(first.copy(annotationsJson = JsonCodec.encodeWorkspaceMetadata(meta)))
                    onShowMessage("Floor plan imported — opening Chakra Workspace")
                    onOpenWorkspace(first.id)
                }
            }
        }
    }

    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            requestSingleGpsFix()
        }
        onDispose {}
    }

    fun performLocationSearch() {
        val query = searchQuery.trim()
        if (query.isBlank() || isSearching) return
        isSearching = true
        scope.launch {
            val hit = withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val conn = (URL("https://nominatim.openstreetmap.org/search?format=json&limit=1&q=$encoded")
                        .openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("User-Agent", "VedicraVastuSolutionsApp/1.0")
                    }
                    val body = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        val obj = arr.getJSONObject(0)
                        Triple(obj.getDouble("lat"), obj.getDouble("lon"), obj.optString("display_name"))
                    } else null
                }.getOrNull()
            }
            isSearching = false
            if (hit != null) {
                centerMapOn(hit.first, hit.second, 19)
                onShowMessage("Centered on: ${hit.third.take(48)}")
            } else {
                onShowMessage("Location not found or internet unavailable")
            }
        }
    }

    fun captureAndSaveMapAlignment(updateReportPlotDeg: Boolean = false) {
        val wv = webViewRef
        val widthPx = (wv?.width ?: 1080).coerceAtLeast(800)
        val heightPx = (wv?.height ?: 1080).coerceAtLeast(800)
        scope.launch {
            val resultFile = withContext(Dispatchers.Main) {
                val bmp = createBitmap(widthPx, heightPx)
                val canvas = Canvas(bmp)
                canvas.drawColor(AndroidColor.rgb(22, 30, 26))
                wv?.draw(canvas)

                val cx = widthPx / 2f
                val cy = heightPx / 2f

                // Draw North–South Reference Axis Line (Cyan) and East–West Reference Axis Line (Gold)
                val nsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(230, 0, 229, 255)
                    strokeWidth = 3.5f
                    pathEffect = DashPathEffect(floatArrayOf(18f, 10f), 0f)
                }
                val ewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(230, 255, 213, 79)
                    strokeWidth = 3.5f
                    pathEffect = DashPathEffect(floatArrayOf(18f, 10f), 0f)
                }
                canvas.drawLine(cx, 0f, cx, heightPx.toFloat(), nsPaint)
                canvas.drawLine(0f, cy, widthPx.toFloat(), cy, ewPaint)

                val axisBadgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(215, 17, 24, 21)
                    style = Paint.Style.FILL
                }
                val axisTxtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.WHITE
                    textSize = (widthPx * 0.025f).coerceIn(20f, 30f)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                // Axis labels near screen edges
                val edgePad = widthPx * 0.06f
                listOf(
                    Triple("N-S AXIS", cx, edgePad * 1.4f),
                    Triple("S", cx, heightPx - edgePad * 2.4f),
                    Triple("W", edgePad, cy),
                    Triple("E", widthPx - edgePad, cy)
                ).forEach { (lbl, lx, ly) ->
                    val tw = axisTxtPaint.measureText(lbl)
                    canvas.drawRoundRect(
                        lx - tw / 2f - 12f,
                        ly - axisTxtPaint.textSize,
                        lx + tw / 2f + 12f,
                        ly + 10f,
                        10f,
                        10f,
                        axisBadgeBg
                    )
                    canvas.drawText(lbl, lx, ly, axisTxtPaint)
                }

                // Draw active Vastu Chakra rotated by -azimuthDeg
                val chakraSize = min(widthPx, heightPx).toFloat() * 0.90f
                val leftOffset = (widthPx - chakraSize) / 2f
                val topOffset = (heightPx - chakraSize) / 2f
                canvas.save()
                canvas.translate(leftOffset, topOffset)
                WorkspaceRenderer.drawVastuChakraByType(
                    canvas = canvas,
                    width = chakraSize,
                    height = chakraSize,
                    rotationDeg = -activeReading.azimuthDeg,
                    baseUnit = chakraSize / 40f,
                    chakraType = selectedChakraType
                )
                canvas.restore()

                // Stamp tilt angle difference, coordinates, and magnetic field at bottom
                val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(228, 17, 24, 21)
                    style = Paint.Style.FILL
                }
                val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.WHITE
                    textSize = (widthPx * 0.024f).coerceIn(19f, 30f)
                    textAlign = Paint.Align.CENTER
                }
                val barH = txtPaint.textSize * 3.0f
                canvas.drawRect(0f, heightPx - barH, widthPx.toFloat(), heightPx.toFloat(), stampPaint)
                val stamp1 = "Wall Tilt Diff: $wallTiltSummary • Heading ${String.format(Locale.US, "%.1f", activeReading.azimuthDeg)}° ${activeReading.zone.code} (${activeReading.pada.code})"
                val stamp2 = "${selectedChakraType.shortName} • GPS: ${String.format(Locale.US, "%.5f", mapLat)}, ${String.format(Locale.US, "%.5f", mapLng)} • Mag: ${activeReading.magneticFieldMicroTesla.toInt()} μT"
                canvas.drawText(stamp1, cx, heightPx - barH + txtPaint.textSize * 1.2f, txtPaint)
                canvas.drawText(stamp2, cx, heightPx - barH + txtPaint.textSize * 2.45f, txtPaint)

                withContext(Dispatchers.IO) {
                    val outFile = repository.mediaManager.createOriginalImageFile(reportId, "map_align")
                    FileOutputStream(outFile).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    bmp.recycle()
                    outFile
                }
            }

            val headingLabel = VastuCompassMath.formatHeadingLabel(activeReading.azimuthDeg)
            val savedMedia = repository.addCapturedCameraPhoto(
                reportId = reportId,
                file = resultFile,
                mediaKind = MediaKind.DRAWING,
                caption = "Google Map Wall Alignment ($headingLabel)",
                drawingCategory = "Plot layout drawing",
                drawingTitle = "Map & Reference Axis Alignment (${selectedChakraType.shortName})",
                orientationDirection = headingLabel,
                description = "Wall Tilt Diff: $wallTiltSummary | Geo: ${String.format(Locale.US, "%.6f", mapLat)}, ${String.format(Locale.US, "%.6f", mapLng)} | Mag: ${activeReading.magneticFieldMicroTesla.toInt()} μT"
            )
            val meta = WorkspaceMetadata(
                showGridOverlay = false,
                gridRotationDegrees = -activeReading.azimuthDeg,
                chakraType = selectedChakraType
            )
            repository.updateMedia(savedMedia.copy(annotationsJson = JsonCodec.encodeWorkspaceMetadata(meta)))

            if (updateReportPlotDeg) {
                val bundle = repository.getReportBundle(reportId)
                if (bundle != null) {
                    repository.saveReport(bundle.report.copy(plotDegrees = "$headingLabel • $wallTiltSummary"), markAsSaved = false)
                }
            }
            onShowMessage("Map alignment ($wallTiltSummary) captured & saved to Drawings!")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1411))
    ) {
        // 1. Live Google Satellite / Hybrid / Street Map View (2-Finger Rotate CW/CCW + Pinch Zoom + Pan)
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    addJavascriptInterface(
                        MapJsBridge { lat, lng, _, bearingDeg ->
                            post {
                                mapLat = lat
                                mapLng = lng
                                val normBearing = bearingDeg.toFloat()
                                if (abs(normBearing - mapBearingDeg) > 0.05f) {
                                    mapBearingDeg = normBearing
                                    headingSource = MapHeadingSource.MAP_WALL_ALIGN
                                }
                            }
                        },
                        "AndroidBridge"
                    )
                    loadDataWithBaseURL(
                        "https://localhost/",
                        buildLeafletMapHtml(mapLat, mapLng, layerStyle.id),
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_compass_webview")
        )

        // 2. North–South & East–West Reference Axis Lines for aligning site boundary walls
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_reference_axes_canvas")
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 10f))

            // North–South Vertical Reference Axis Line (Cyan)
            drawLine(
                color = Color(0xE600E5FF),
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 3f,
                pathEffect = dash
            )

            // East–West Horizontal Reference Axis Line (Gold)
            drawLine(
                color = Color(0xE6FFD54F),
                start = Offset(0f, cy),
                end = Offset(size.width, cy),
                strokeWidth = 3f,
                pathEffect = dash
            )

            // Alignment perpendicular ticks along axes for crisp wall alignment
            val stepPx = 42.dp.toPx()
            for (k in 1..6) {
                val d = k * stepPx
                drawLine(Color(0xBB00E5FF), Offset(cx - 8f, cy - d), Offset(cx + 8f, cy - d), 2f)
                drawLine(Color(0xBB00E5FF), Offset(cx - 8f, cy + d), Offset(cx + 8f, cy + d), 2f)
                drawLine(Color(0xBBFFD54F), Offset(cx - d, cy - 8f), Offset(cx - d, cy + 8f), 2f)
                drawLine(Color(0xBBFFD54F), Offset(cx + d, cy - 8f), Offset(cx + d, cy + 8f), 2f)
            }

            // Axis labels (N-S / E-W)
            fun drawAxisPill(label: String, x: Float, y: Float, color: Color) {
                val res = textMeasurer.measure(
                    text = label,
                    style = TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                )
                val padX = 8f
                val padY = 4f
                drawRoundRect(
                    color = Color(0xCC111815),
                    topLeft = Offset(x - res.size.width / 2f - padX, y - res.size.height / 2f - padY),
                    size = androidx.compose.ui.geometry.Size(res.size.width + padX * 2f, res.size.height + padY * 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                drawText(res, topLeft = Offset(x - res.size.width / 2f, y - res.size.height / 2f))
            }

            drawAxisPill("W", 22.dp.toPx(), cy, Color(0xFFFFD54F))
            drawAxisPill("E", size.width - 22.dp.toPx(), cy, Color(0xFFFFD54F))
            drawAxisPill("N-S", cx, size.height * 0.34f, Color(0xFF00E5FF))
            drawAxisPill("S", cx, size.height * 0.78f, Color(0xFF00E5FF))
        }

        // 3. Overlaid Vastu Chakra (automatically aligned with Map Wall Tilt OR Live Sensor)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            VastuChakraCanvas(
                rotationDegrees = -activeReading.azimuthDeg,
                chakraType = selectedChakraType,
                transparentFill = true,
                showDeityNames = true,
                showTopLubberNeedle = true,
                tiltRollDeg = activeReading.rollDeg,
                tiltPitchDeg = activeReading.pitchDeg,
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .aspectRatio(1f)
                    .testTag("map_chakra_overlay")
            )
        }

        // 4. Top Controls (Back, Search, Google Layer Switcher, Chakra Selector, Tilt Difference HUD)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xCC111815),
                border = BorderStroke(1.dp, Color(0x88D4AF37))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search city, plot, address...", color = Color(0xFFB5C0BA), maxLines = 1) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { performLocationSearch() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD4AF37),
                                unfocusedBorderColor = Color(0x66FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("map_search_input")
                        )
                        IconButton(
                            onClick = { performLocationSearch() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF1B4D3E), RoundedCornerShape(10.dp))
                                .testTag("map_search_button")
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "Search Location", tint = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MapLayerStyle.entries.forEach { style ->
                            FilterChip(
                                selected = layerStyle == style,
                                onClick = {
                                    layerStyle = style
                                    webViewRef?.evaluateJavascript("if (window.setMapLayer) window.setMapLayer('${style.id}');", null)
                                },
                                label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("map_layer_${style.id}")
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(
                            onClick = {
                                uploadPlanLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, tint = Color(0xFFE6C574), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Upload Plan", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    ChakraTypeSelectorRow(
                        selectedType = selectedChakraType,
                        onSelectType = { selectedChakraType = it },
                        tagPrefix = "map_chakra"
                    )
                }
            }

            CompassTopDegreeTiltHud(
                reading = activeReading,
                isLocked = isLocked,
                titlePrefix = selectedChakraType.shortName.uppercase(Locale.US),
                onToggleLock = {
                    if (!isLocked) lockedReading = activeReading
                    isLocked = !isLocked
                }
            )

            // Prominent Wall Alignment Angle Difference Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xD9111815),
                border = BorderStroke(1.dp, Color(0x9900E5FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_wall_tilt_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wall Tilt Diff: $wallTiltSummary",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "2-finger twist map (CW/CCW) & pinch zoom to align walls with N-S / E-W axes",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD0DBD5),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (abs(mapBearingDeg) > 0.2f) {
                        OutlinedButton(
                            onClick = { setJsMapBearing(0f) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("map_reset_bearing_button")
                        ) {
                            Text("Reset 0°", color = Color(0xFFFFD54F), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (showDegreeDrawer) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xE8111815),
                    border = BorderStroke(1.dp, Color(0x88D4AF37)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MapHeadingSource.entries.forEach { src ->
                                if (src != MapHeadingSource.LIVE_SENSOR || liveCompass.hasHardwareSensors) {
                                    FilterChip(
                                        selected = headingSource == src,
                                        onClick = { headingSource = src },
                                        label = { Text(src.label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                        ManualDegreeInputControl(
                            degrees = activeReading.azimuthDeg,
                            onDegreesChange = { deg ->
                                manualDegree = deg
                                headingSource = MapHeadingSource.MANUAL
                            },
                            label = "Site Wall / Chakra Tilt (°)",
                            showSlider = true,
                            darkSurface = true,
                            tagPrefix = "map_manual_degree"
                        )
                    }
                }
            }
        }

        // 5. Right Side Floating Map Zoom, 2-Finger/Button Rotate, GPS & Manual Controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Surface(
                onClick = { webViewRef?.evaluateJavascript("if (window.zoomInMap) window.zoomInMap();", null) },
                shape = CircleShape,
                color = Color(0xCC111815),
                border = BorderStroke(1.dp, Color(0x88D4AF37)),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, contentDescription = "Zoom In", tint = Color.White)
                }
            }
            Surface(
                onClick = { webViewRef?.evaluateJavascript("if (window.zoomOutMap) window.zoomOutMap();", null) },
                shape = CircleShape,
                color = Color(0xCC111815),
                border = BorderStroke(1.dp, Color(0x88D4AF37)),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", tint = Color.White)
                }
            }
            Surface(
                onClick = { setJsMapBearing(mapBearingDeg - 5f) },
                shape = CircleShape,
                color = Color(0xCC111815),
                border = BorderStroke(1.dp, Color(0x8800E5FF)),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate Map Anticlockwise", tint = Color(0xFF00E5FF))
                }
            }
            Surface(
                onClick = { setJsMapBearing(mapBearingDeg + 5f) },
                shape = CircleShape,
                color = Color(0xCC111815),
                border = BorderStroke(1.dp, Color(0x8800E5FF)),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Map Clockwise", tint = Color(0xFF00E5FF))
                }
            }
            Surface(
                onClick = {
                    if (hasLocationPermission) {
                        requestSingleGpsFix()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                shape = CircleShape,
                color = Color(0xCC1B4D3E),
                border = BorderStroke(1.dp, Color(0xFFD4AF37)),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("map_gps_center_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Center GPS Location", tint = Color.White)
                }
            }
            Surface(
                onClick = { showDegreeDrawer = !showDegreeDrawer },
                shape = CircleShape,
                color = if (showDegreeDrawer) Color(0xFFD4AF37) else Color(0xCC111815),
                border = BorderStroke(1.dp, Color(0x88D4AF37)),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("map_toggle_manual_degree_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Manual & Sensor Mode",
                        tint = if (showDegreeDrawer) Color(0xFF111815) else Color.White
                    )
                }
            }
        }

        // 6. Bottom Coordinates, Magnetic Field & Capture Bar
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            color = Color(0xDF111815),
            border = BorderStroke(1.dp, Color(0x88D4AF37)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Geo Coordinates",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD4AF37)
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.5f", mapLat)}, ${String.format(Locale.US, "%.5f", mapLng)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Mode & Magnetic Field",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD4AF37)
                        )
                        val magText = if (activeReading.magneticFieldMicroTesla > 0f) {
                            "${activeReading.magneticFieldMicroTesla.toInt()} μT"
                        } else {
                            headingSource.label
                        }
                        val accText = gpsAccuracyMeters?.let { " • ±${it.toInt()}m" } ?: ""
                        Text(
                            text = "$magText$accText",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { captureAndSaveMapAlignment(updateReportPlotDeg = true) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B4D3E),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("map_capture_save_button")
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Capture & Save Plot Deg", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { captureAndSaveMapAlignment(updateReportPlotDeg = false) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFFE6C574), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save View", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private fun buildLeafletMapHtml(lat: Double, lng: Double, initialLayer: String): String = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
  html, body {
    margin: 0; padding: 0; width: 100%; height: 100%;
    overflow: hidden; background: #141c18;
    touch-action: none;
  }
  #map-rotator {
    position: absolute;
    left: 50%;
    top: 50%;
    width: 185vmax;
    height: 185vmax;
    transform: translate(-50%, -50%) rotate(0deg);
    transform-origin: center center;
    will-change: transform;
  }
  #map {
    width: 100%;
    height: 100%;
    background: #141c18;
  }
</style>
</head>
<body>
<div id="map-rotator"><div id="map"></div></div>
<script>
  var map = L.map('map', {
    zoomControl: false,
    attributionControl: false,
    dragging: false,
    touchZoom: false,
    doubleClickZoom: false,
    scrollWheelZoom: false,
    boxZoom: false,
    keyboard: false,
    zoomSnap: 0.1,
    zoomDelta: 0.25
  }).setView([$lat, $lng], 18);

  var googleHybrid = L.tileLayer('https://mt{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
    subdomains: '0123', maxZoom: 21, maxNativeZoom: 20
  });
  var googleSat = L.tileLayer('https://mt{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', {
    subdomains: '0123', maxZoom: 21, maxNativeZoom: 20
  });
  var googleStreet = L.tileLayer('https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
    subdomains: '0123', maxZoom: 21, maxNativeZoom: 20
  });

  var currentLayer = null;
  window.setMapLayer = function(mode) {
    if (currentLayer) map.removeLayer(currentLayer);
    if (mode === 'satellite') currentLayer = googleSat;
    else if (mode === 'street') currentLayer = googleStreet;
    else currentLayer = googleHybrid;
    currentLayer.addTo(map);
  };
  window.setMapLayer('$initialLayer');

  var mapBearingDeg = 0.0;
  var rotatorEl = document.getElementById('map-rotator');

  function applyBearingTransform() {
    rotatorEl.style.transform = 'translate(-50%, -50%) rotate(' + mapBearingDeg + 'deg)';
  }

  function notifyTransform() {
    var c = map.getCenter();
    if (window.AndroidBridge && window.AndroidBridge.onMapTransform) {
      window.AndroidBridge.onMapTransform(c.lat, c.lng, map.getZoom(), mapBearingDeg);
    }
  }

  window.setMapCenter = function(lat, lng, z) {
    map.setView([lat, lng], z || map.getZoom());
    notifyTransform();
  };
  window.zoomInMap = function() {
    map.setZoom(Math.min(21, map.getZoom() + 0.5));
    notifyTransform();
  };
  window.zoomOutMap = function() {
    map.setZoom(Math.max(2, map.getZoom() - 0.5));
    notifyTransform();
  };
  window.setMapBearing = function(deg) {
    mapBearingDeg = deg;
    applyBearingTransform();
    notifyTransform();
  };

  // Custom unified touch controller: 1-finger rotation-compensated pan + 2-finger twist rotation (CW/CCW) & pinch-zoom
  var gestureMode = 'none';
  var lastSingleX = 0, lastSingleY = 0;
  var prevAngle = 0, prevDist = 0, startZoom = 18;

  function touchDist(t1, t2) {
    return Math.hypot(t2.clientX - t1.clientX, t2.clientY - t1.clientY);
  }
  function touchAngleDeg(t1, t2) {
    return Math.atan2(t2.clientY - t1.clientY, t2.clientX - t1.clientX) * 180.0 / Math.PI;
  }
  function wrapAngleDelta(d) {
    while (d > 180) d -= 360;
    while (d < -180) d += 360;
    return d;
  }

  document.addEventListener('touchstart', function(e) {
    if (e.touches.length === 1) {
      gestureMode = 'pan';
      lastSingleX = e.touches[0].clientX;
      lastSingleY = e.touches[0].clientY;
    } else if (e.touches.length === 2) {
      gestureMode = 'pinch_rotate';
      prevDist = Math.max(10, touchDist(e.touches[0], e.touches[1]));
      prevAngle = touchAngleDeg(e.touches[0], e.touches[1]);
      startZoom = map.getZoom();
    }
  }, { passive: false });

  document.addEventListener('touchmove', function(e) {
    e.preventDefault();
    if (e.touches.length === 1 && gestureMode === 'pan') {
      var sx = e.touches[0].clientX - lastSingleX;
      var sy = e.touches[0].clientY - lastSingleY;
      lastSingleX = e.touches[0].clientX;
      lastSingleY = e.touches[0].clientY;

      var rad = mapBearingDeg * Math.PI / 180.0;
      var lx = sx * Math.cos(rad) + sy * Math.sin(rad);
      var ly = -sx * Math.sin(rad) + sy * Math.cos(rad);
      map.panBy([-lx, -ly], { animate: false });
    } else if (e.touches.length >= 2) {
      var t1 = e.touches[0], t2 = e.touches[1];
      var curDist = Math.max(10, touchDist(t1, t2));
      var curAngle = touchAngleDeg(t1, t2);
      if (gestureMode !== 'pinch_rotate') {
        gestureMode = 'pinch_rotate';
        prevDist = curDist;
        prevAngle = curAngle;
        return;
      }
      var deltaAng = wrapAngleDelta(curAngle - prevAngle);
      mapBearingDeg += deltaAng;
      applyBearingTransform();

      var zoomDelta = Math.log2(curDist / prevDist);
      var nextZoom = Math.min(21, Math.max(2, map.getZoom() + zoomDelta));
      map.setZoom(nextZoom, { animate: false });

      prevDist = curDist;
      prevAngle = curAngle;
      notifyTransform();
    }
  }, { passive: false });

  document.addEventListener('touchend', function(e) {
    if (e.touches.length === 1) {
      gestureMode = 'pan';
      lastSingleX = e.touches[0].clientX;
      lastSingleY = e.touches[0].clientY;
    } else if (e.touches.length === 0) {
      gestureMode = 'none';
    }
    notifyTransform();
  }, { passive: false });

  map.on('moveend', notifyTransform);
</script>
</body>
</html>
""".trimIndent()
