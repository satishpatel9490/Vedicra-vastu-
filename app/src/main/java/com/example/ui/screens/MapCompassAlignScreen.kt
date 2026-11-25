package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import kotlin.math.min

private enum class MapLayerStyle(val id: String, val label: String) {
    SATELLITE("satellite", "Satellite"),
    HYBRID("hybrid", "Hybrid"),
    STREET("street", "Street")
}

private class MapJsBridge(
    private val onViewportChanged: (Double, Double, Int) -> Unit
) {
    @JavascriptInterface
    fun onMapMove(lat: Double, lng: Double, zoom: Int) {
        onViewportChanged(lat, lng, zoom)
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

    val liveCompass by rememberVastuCompassState(enabled = true)
    var useManualDegree by remember(liveCompass.hasHardwareSensors) {
        mutableStateOf(!liveCompass.hasHardwareSensors)
    }
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

    val activeReading = remember(liveCompass, useManualDegree, manualDegree, isLocked, lockedReading) {
        val base = if (useManualDegree || !liveCompass.hasHardwareSensors) {
            liveCompass.copy(
                azimuthDeg = manualDegree,
                zone = VastuCompassMath.zoneForDegree(manualDegree),
                pada = VastuCompassMath.padaForDegree(manualDegree),
                accuracy = if (liveCompass.hasHardwareSensors) liveCompass.accuracy else CompassAccuracy.MANUAL
            )
        } else {
            liveCompass
        }
        if (isLocked) lockedReading ?: base else base
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
                    description = "Aligned near ${String.format(Locale.US, "%.5f", mapLat)}, ${String.format(Locale.US, "%.5f", mapLng)}"
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

                // Draw centered crosshair hairline
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(180, 255, 82, 82)
                    strokeWidth = 2.5f
                }
                canvas.drawLine(0f, heightPx / 2f, widthPx.toFloat(), heightPx / 2f, linePaint)
                canvas.drawLine(widthPx / 2f, 0f, widthPx / 2f, heightPx.toFloat(), linePaint)

                // Draw active Vastu Chakra rotated by -azimuthDeg
                val chakraSize = min(widthPx, heightPx).toFloat() * 0.92f
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

                // Stamp coordinates, degree tilt, magnetic field at bottom
                val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(220, 17, 24, 21)
                    style = Paint.Style.FILL
                }
                val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.WHITE
                    textSize = (widthPx * 0.026f).coerceIn(20f, 32f)
                    textAlign = Paint.Align.CENTER
                }
                val barH = txtPaint.textSize * 2.8f
                canvas.drawRect(0f, heightPx - barH, widthPx.toFloat(), heightPx.toFloat(), stampPaint)
                val stamp1 = "Tilt: ${String.format(Locale.US, "%.1f", activeReading.azimuthDeg)}° ${activeReading.zone.code} (${activeReading.pada.code}) • ${selectedChakraType.shortName}"
                val stamp2 = "GPS: ${String.format(Locale.US, "%.5f", mapLat)}, ${String.format(Locale.US, "%.5f", mapLng)} • Mag: ${activeReading.magneticFieldMicroTesla.toInt()} μT"
                canvas.drawText(stamp1, widthPx / 2f, heightPx - barH + txtPaint.textSize * 1.15f, txtPaint)
                canvas.drawText(stamp2, widthPx / 2f, heightPx - barH + txtPaint.textSize * 2.35f, txtPaint)

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
                caption = "Satellite Map Alignment ($headingLabel)",
                drawingCategory = "Plot layout drawing",
                drawingTitle = "Map & Compass Alignment (${selectedChakraType.shortName})",
                orientationDirection = headingLabel,
                description = "Geo: ${String.format(Locale.US, "%.6f", mapLat)}, ${String.format(Locale.US, "%.6f", mapLng)} | Mag Field: ${activeReading.magneticFieldMicroTesla.toInt()} μT | Tilt: ${String.format(Locale.US, "%.1f", activeReading.azimuthDeg)}°"
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
                    repository.saveReport(bundle.report.copy(plotDegrees = headingLabel), markAsSaved = false)
                }
            }
            onShowMessage("Map chakra alignment captured & saved to Drawings!")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1411))
    ) {
        // 1. Live Leaflet Satellite / Hybrid / Street Map View
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    addJavascriptInterface(
                        MapJsBridge { lat, lng, _ ->
                            mapLat = lat
                            mapLng = lng
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

        // 2. Centered Crosshair + Active Vastu Chakra Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color(0xCCFF5252),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0xCCFF5252),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 2f
            )
        }

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
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f)
                    .testTag("map_chakra_overlay")
            )
        }

        // 3. Top Controls (Back, Search, Layer Switcher, Chakra Selector, Degree HUD)
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

            if (showDegreeDrawer) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xDE111815),
                    border = BorderStroke(1.dp, Color(0x88D4AF37)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (liveCompass.hasHardwareSensors) {
                                FilterChip(
                                    selected = !useManualDegree,
                                    onClick = { useManualDegree = false },
                                    label = { Text("Live Sensor") }
                                )
                            }
                            FilterChip(
                                selected = useManualDegree || !liveCompass.hasHardwareSensors,
                                onClick = { useManualDegree = true },
                                label = { Text("Manual Tilt Degree") }
                            )
                        }
                        ManualDegreeInputControl(
                            degrees = if (useManualDegree || !liveCompass.hasHardwareSensors) manualDegree else activeReading.azimuthDeg,
                            onDegreesChange = {
                                useManualDegree = true
                                manualDegree = it
                            },
                            label = "Site North / Chakra Tilt (°)",
                            showSlider = true,
                            darkSurface = true,
                            tagPrefix = "map_manual_degree"
                        )
                    }
                }
            }
        }

        // 4. Right Side Floating Map Zoom & GPS Controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { webViewRef?.evaluateJavascript("if (window.zoomInMap) window.zoomInMap();", null) },
                shape = CircleShape,
                color = Color(0xCC111815),
                border = BorderStroke(1.dp, Color(0x88D4AF37)),
                modifier = Modifier.size(44.dp)
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
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", tint = Color.White)
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
                    .size(44.dp)
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
                    .size(44.dp)
                    .testTag("map_toggle_manual_degree_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Manual Tilt Input",
                        tint = if (showDegreeDrawer) Color(0xFF111815) else Color.White
                    )
                }
            }
        }

        // 5. Bottom Coordinates, Magnetic Field & Capture Bar
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
                            text = "Magnetic Field",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD4AF37)
                        )
                        val magText = if (activeReading.magneticFieldMicroTesla > 0f) {
                            "${activeReading.magneticFieldMicroTesla.toInt()} μT"
                        } else {
                            "Manual"
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
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=yes" />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
  html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #141c18; }
</style>
</head>
<body>
<div id="map"></div>
<script>
  var map = L.map('map', { zoomControl: false, attributionControl: false }).setView([$lat, $lng], 18);
  var satLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', { maxZoom: 20 });
  var refLabels = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}', { maxZoom: 20 });
  var osmLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 });
  var hybridGroup = L.layerGroup([satLayer, refLabels]);

  var currentLayer = null;
  window.setMapLayer = function(mode) {
    if (currentLayer) map.removeLayer(currentLayer);
    if (mode === 'satellite') currentLayer = satLayer;
    else if (mode === 'street') currentLayer = osmLayer;
    else currentLayer = hybridGroup;
    currentLayer.addTo(map);
  };
  window.setMapLayer('$initialLayer');

  window.setMapCenter = function(lat, lng, z) {
    map.setView([lat, lng], z || map.getZoom());
  };
  window.zoomInMap = function() { map.zoomIn(); };
  window.zoomOutMap = function() { map.zoomOut(); };

  map.on('moveend', function() {
    var c = map.getCenter();
    if (window.AndroidBridge) {
      window.AndroidBridge.onMapMove(c.lat, c.lng, map.getZoom());
    }
  });
</script>
</body>
</html>
""".trimIndent()
