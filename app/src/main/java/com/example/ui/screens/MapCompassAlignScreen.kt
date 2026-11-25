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
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

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

data class MapPlaceSearchResult(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)

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

internal fun parseCoordinateSearchQuery(query: String): MapPlaceSearchResult? {
    val match = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*[,;\s]\s*(-?\d+(?:\.\d+)?)\s*$""").matchEntire(query)
        ?: return null
    val lat = match.groupValues[1].toDoubleOrNull() ?: return null
    val lng = match.groupValues[2].toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    return MapPlaceSearchResult(
        title = "Coordinates (${String.format(Locale.US, "%.5f", lat)}, ${String.format(Locale.US, "%.5f", lng)})",
        subtitle = "Direct GPS Latitude / Longitude",
        latitude = lat,
        longitude = lng
    )
}

internal fun parsePhotonSearchJson(jsonBody: String): List<MapPlaceSearchResult> {
    val root = runCatching { Json.parseToJsonElement(jsonBody).jsonObject }.getOrNull() ?: return emptyList()
    val features = root["features"]?.jsonArray ?: return emptyList()
    val out = mutableListOf<MapPlaceSearchResult>()
    for (featEl in features) {
        val feat = runCatching { featEl.jsonObject }.getOrNull() ?: continue
        val geom = feat["geometry"]?.jsonObject ?: continue
        val coords = geom["coordinates"]?.jsonArray ?: continue
        if (coords.size < 2) continue
        val lng = coords[0].jsonPrimitive.doubleOrNull ?: continue
        val lat = coords[1].jsonPrimitive.doubleOrNull ?: continue
        val props = feat["properties"]?.jsonObject
        fun propStr(key: String): String = props?.get(key)?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val name = propStr("name").ifBlank {
            listOfNotNull(
                propStr("street").takeIf { it.isNotBlank() },
                propStr("city").takeIf { it.isNotBlank() },
                propStr("state").takeIf { it.isNotBlank() }
            ).firstOrNull().orEmpty()
        }.ifBlank { "Selected Location" }
        val subtitleParts = listOfNotNull(
            propStr("district").takeIf { it.isNotBlank() && it != name },
            propStr("city").takeIf { it.isNotBlank() && it != name },
            propStr("state").takeIf { it.isNotBlank() && it != name },
            propStr("country").takeIf { it.isNotBlank() && it != name }
        ).distinct()
        out += MapPlaceSearchResult(
            title = name,
            subtitle = subtitleParts.joinToString(", ").ifBlank {
                "${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)}"
            },
            latitude = lat,
            longitude = lng
        )
    }
    return out
}

internal fun parseNominatimSearchJson(jsonBody: String): List<MapPlaceSearchResult> {
    val arr = runCatching { Json.parseToJsonElement(jsonBody).jsonArray }.getOrNull() ?: return emptyList()
    val out = mutableListOf<MapPlaceSearchResult>()
    for (el in arr) {
        val obj = runCatching { el.jsonObject }.getOrNull() ?: continue
        val lat = obj["lat"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: continue
        val lng = obj["lon"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: continue
        val displayName = obj["display_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val parts = displayName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val title = parts.firstOrNull() ?: "Location"
        val subtitle = parts.drop(1).take(3).joinToString(", ").ifBlank { displayName }
        out += MapPlaceSearchResult(
            title = title,
            subtitle = subtitle,
            latitude = lat,
            longitude = lng
        )
    }
    return out
}

private suspend fun searchPlacesMultiProvider(
    context: Context,
    query: String
): List<MapPlaceSearchResult> {
    parseCoordinateSearchQuery(query)?.let { return listOf(it) }

    val encoded = URLEncoder.encode(query, "UTF-8")

    // 1. Photon OSM Geocoder (fast, reliable mobile/cloud IP access)
    val photonHits = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("https://photon.komoot.io/api/?q=$encoded&limit=5")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "VedicraVastuApp/1.0")
            }
            val code = conn.responseCode
            val body = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
            conn.disconnect()
            if (body.isNotBlank()) parsePhotonSearchJson(body) else emptyList()
        }.getOrDefault(emptyList())
    }
    if (photonHits.isNotEmpty()) return photonHits

    // 2. Nominatim OSM Geocoder Fallback
    val nominatimHits = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("https://nominatim.openstreetmap.org/search?format=json&limit=5&q=$encoded")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag())
                setRequestProperty("User-Agent", "VedicraVastuSolutionsApp/1.0 (Android)")
            }
            val code = conn.responseCode
            val body = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
            conn.disconnect()
            if (body.isNotBlank()) parseNominatimSearchJson(body) else emptyList()
        }.getOrDefault(emptyList())
    }
    if (nominatimHits.isNotEmpty()) return nominatimHits

    // 3. System Android Geocoder Fallback (Google Play Services Geocoder)
    if (!Geocoder.isPresent()) return emptyList()
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
            runCatching {
                geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                    override fun onGeocode(results: MutableList<Address>) {
                        if (cont.isActive) cont.resume(results)
                    }
                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(emptyList())
                    }
                })
            }.onFailure {
                if (cont.isActive) cont.resume(emptyList())
            }
        }
    } else {
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocationName(query, 5).orEmpty() }.getOrDefault(emptyList())
        }
    }
    return addresses.map { addr ->
        val line = (0..addr.maxAddressLineIndex).mapNotNull { idx ->
            addr.getAddressLine(idx)?.takeIf { it.isNotBlank() }
        }.joinToString(", ")
        val title = addr.featureName?.takeIf { it.isNotBlank() }
            ?: addr.locality?.takeIf { it.isNotBlank() }
            ?: line.ifBlank { query }
        val subtitle = listOfNotNull(
            addr.subLocality,
            addr.locality,
            addr.adminArea,
            addr.countryName
        ).distinct().joinToString(", ").ifBlank { line }
        MapPlaceSearchResult(
            title = title,
            subtitle = subtitle,
            latitude = addr.latitude,
            longitude = addr.longitude
        )
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val liveCompass by rememberVastuCompassState(enabled = true)
    var headingSource by remember { mutableStateOf(MapHeadingSource.MAP_WALL_ALIGN) }
    var mapBearingDeg by remember { mutableFloatStateOf(0f) }
    var manualDegree by remember { mutableFloatStateOf(0f) }
    var isLocked by remember { mutableStateOf(false) }
    var lockedReading by remember { mutableStateOf<CompassReading?>(null) }
    var selectedChakraType by remember { mutableStateOf(initialChakraType) }

    // UI visibility, Full Screen Hide Mode, Chakra Transparency (Light/Dark) & Chakra Size
    var hideAllControls by remember { mutableStateOf(false) }
    var showControlsPanel by remember { mutableStateOf(false) }
    var showTransparencyBar by remember { mutableStateOf(false) }
    var showFullHudCard by remember { mutableStateOf(false) }
    var chakraOpacity by remember { mutableFloatStateOf(0.74f) }
    var chakraDarkContrast by remember { mutableStateOf(false) }
    var chakraScale by remember { mutableFloatStateOf(0.88f) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MapPlaceSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isLocatingGps by remember { mutableStateOf(false) }
    var layerStyle by remember { mutableStateOf(MapLayerStyle.HYBRID) }
    var mapLat by remember { mutableDoubleStateOf(17.385044) }
    var mapLng by remember { mutableDoubleStateOf(78.486671) }
    var gpsMarkerLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var gpsAccuracyMeters by remember { mutableStateOf<Float?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapPageLoaded by remember { mutableStateOf(false) }

    BackHandler(enabled = hideAllControls || showControlsPanel || showTransparencyBar || searchResults.isNotEmpty()) {
        when {
            searchResults.isNotEmpty() -> searchResults = emptyList()
            showTransparencyBar -> showTransparencyBar = false
            showControlsPanel -> showControlsPanel = false
            hideAllControls -> hideAllControls = false
        }
    }

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
        if (isMapPageLoaded) {
            webViewRef?.evaluateJavascript("if (window.setMapBearing) window.setMapBearing($deg);", null)
        }
    }

    fun centerMapOn(lat: Double, lng: Double, zoom: Int = 19, showPinLabel: String? = null) {
        mapLat = lat
        mapLng = lng
        if (isMapPageLoaded) {
            webViewRef?.evaluateJavascript("if (window.setMapCenter) window.setMapCenter($lat, $lng, $zoom);", null)
            if (showPinLabel != null) {
                val escaped = showPinLabel.replace("\\", "\\\\").replace("'", "\\'")
                webViewRef?.evaluateJavascript("if (window.setSearchPin) window.setSearchPin($lat, $lng, '$escaped');", null)
            }
        }
    }

    fun updateGpsMarkerOnMap(location: Location) {
        gpsAccuracyMeters = location.accuracy
        gpsMarkerLatLng = location.latitude to location.longitude
        centerMapOn(location.latitude, location.longitude, 19)
        if (isMapPageLoaded) {
            val acc = location.accuracy.coerceIn(5f, 150f)
            webViewRef?.evaluateJavascript(
                "if (window.setMyLocationMarker) window.setMyLocationMarker(${location.latitude}, ${location.longitude}, $acc);",
                null
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun requestSingleGpsFix(userTriggered: Boolean = false) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm != null && !LocationManagerCompat.isLocationEnabled(lm)) {
            isLocatingGps = false
            if (userTriggered) {
                onShowMessage("Device GPS / Location is turned off. Please enable Location in Quick Settings.")
            }
            return
        }

        isLocatingGps = true
        var receivedFix = false

        // 1. Check cached last-known location across providers first for instant feedback
        if (lm != null) {
            val cachedProviders = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
                add(LocationManager.GPS_PROVIDER)
                add(LocationManager.NETWORK_PROVIDER)
                add(LocationManager.PASSIVE_PROVIDER)
            }
            val bestLast = cachedProviders.mapNotNull { provider ->
                runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            }.minByOrNull { it.accuracy }
            if (bestLast != null) {
                updateGpsMarkerOnMap(bestLast)
            }
        }

        // 2. Request high-accuracy Fused Location first (fastest on phones with Play Services)
        val fusedClient = runCatching { LocationServices.getFusedLocationProviderClient(context) }.getOrNull()
        val fusedCancel = CancellationTokenSource()
        if (fusedClient != null) {
            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && !receivedFix) {
                    updateGpsMarkerOnMap(lastLoc)
                }
            }
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, fusedCancel.token)
                .addOnSuccessListener { freshLoc ->
                    if (freshLoc != null) {
                        receivedFix = true
                        isLocatingGps = false
                        updateGpsMarkerOnMap(freshLoc)
                        if (userTriggered) {
                            onShowMessage("GPS locked (±${freshLoc.accuracy.toInt()}m)")
                        }
                    } else {
                        isLocatingGps = false
                    }
                }
                .addOnFailureListener {
                    isLocatingGps = false
                }
        }

        // 3. Concurrently request LocationManagerCompat current location for devices without Play Services
        if (lm != null) {
            val activeProviders = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
                add(LocationManager.GPS_PROVIDER)
                add(LocationManager.NETWORK_PROVIDER)
            }.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

            val signal = CancellationSignal()
            val executor = ContextCompat.getMainExecutor(context)
            activeProviders.forEach { provider ->
                runCatching {
                    LocationManagerCompat.getCurrentLocation(
                        lm,
                        provider,
                        signal,
                        executor
                    ) { loc ->
                        if (loc != null && !receivedFix) {
                            receivedFix = true
                            isLocatingGps = false
                            updateGpsMarkerOnMap(loc)
                            if (userTriggered) {
                                onShowMessage("GPS locked (±${loc.accuracy.toInt()}m)")
                            }
                        }
                    }
                }
            }
            if (activeProviders.isEmpty() && fusedClient == null) {
                isLocatingGps = false
                if (userTriggered) {
                    onShowMessage("No active GPS provider available — search address or pan manually.")
                }
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
            requestSingleGpsFix(userTriggered = true)
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
            requestSingleGpsFix(userTriggered = false)
        }
        onDispose {}
    }

    fun performLocationSearch() {
        val query = searchQuery.trim()
        if (query.isBlank() || isSearching) return
        focusManager.clearFocus()
        keyboardController?.hide()
        isSearching = true
        scope.launch {
            val hits = searchPlacesMultiProvider(context, query)
            isSearching = false
            if (hits.isNotEmpty()) {
                val first = hits.first()
                centerMapOn(first.latitude, first.longitude, 19, showPinLabel = first.title)
                searchResults = if (hits.size > 1) hits else emptyList()
                onShowMessage("Centered on: ${first.title}")
            } else {
                searchResults = emptyList()
                onShowMessage("Place not found. Check internet or try city/pincode or lat, lng.")
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

                // Draw active Vastu Chakra at user's chosen scale and transparency level
                val chakraSize = min(widthPx, heightPx).toFloat() * chakraScale
                val leftOffset = (widthPx - chakraSize) / 2f
                val topOffset = (heightPx - chakraSize) / 2f
                val alphaInt = (chakraOpacity * 255f).roundToInt().coerceIn(25, 255)
                val layerCount = canvas.saveLayerAlpha(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), alphaInt)
                canvas.translate(leftOffset, topOffset)
                WorkspaceRenderer.drawVastuChakraByType(
                    canvas = canvas,
                    width = chakraSize,
                    height = chakraSize,
                    rotationDeg = -activeReading.azimuthDeg,
                    baseUnit = chakraSize / 40f,
                    chakraType = selectedChakraType
                )
                canvas.restoreToCount(layerCount)

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
        // 1. Edge-to-Edge Full Screen Google Satellite / Hybrid / Street Map View
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.rgb(20, 28, 24))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    settings.loadWithOverviewMode = false
                    settings.useWideViewPort = false
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isMapPageLoaded = true
                            view?.evaluateJavascript(
                                "if (window.setMapLayer) window.setMapLayer('${layerStyle.id}');" +
                                    "if (window.setMapCenter) window.setMapCenter($mapLat, $mapLng, 19);" +
                                    "if (window.setMapBearing) window.setMapBearing($mapBearingDeg);" +
                                    "if (window.refreshMapSize) window.refreshMapSize();",
                                null
                            )
                            gpsMarkerLatLng?.let { (gLat, gLng) ->
                                val acc = (gpsAccuracyMeters ?: 15f).coerceIn(5f, 150f)
                                view?.evaluateJavascript(
                                    "if (window.setMyLocationMarker) window.setMyLocationMarker($gLat, $gLng, $acc);",
                                    null
                                )
                            }
                        }
                    }
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

            drawLine(
                color = Color(0xE600E5FF),
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 3f,
                pathEffect = dash
            )

            drawLine(
                color = Color(0xE6FFD54F),
                start = Offset(0f, cy),
                end = Offset(size.width, cy),
                strokeWidth = 3f,
                pathEffect = dash
            )

            val stepPx = 42.dp.toPx()
            for (k in 1..7) {
                val d = k * stepPx
                drawLine(Color(0xBB00E5FF), Offset(cx - 8f, cy - d), Offset(cx + 8f, cy - d), 2f)
                drawLine(Color(0xBB00E5FF), Offset(cx - 8f, cy + d), Offset(cx + 8f, cy + d), 2f)
                drawLine(Color(0xBBFFD54F), Offset(cx - d, cy - 8f), Offset(cx - d, cy + 8f), 2f)
                drawLine(Color(0xBBFFD54F), Offset(cx + d, cy - 8f), Offset(cx + d, cy + 8f), 2f)
            }

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
            drawAxisPill("N-S", cx, size.height * 0.22f, Color(0xFF00E5FF))
            drawAxisPill("S", cx, size.height * 0.84f, Color(0xFF00E5FF))
        }

        // 3. Resizable & Adjustable-Transparency Vastu Shakti Chakra Overlay
        Box(
            modifier = Modifier.fillMaxSize(),
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
                chakraOpacity = chakraOpacity,
                darkContrastMode = chakraDarkContrast,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = chakraScale
                        scaleY = chakraScale
                    }
                    .testTag("map_chakra_overlay")
            )
        }

        if (hideAllControls) {
            // Pure Full-Screen Mode: All banners/menus/buttons hidden — single floating Show Controls pill
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xB8111815),
                    border = BorderStroke(1.dp, Color(0x88D4AF37))
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", activeReading.azimuthDeg)}° ${activeReading.zone.code}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
                Surface(
                    onClick = { hideAllControls = false },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xE01B4D3E),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37)),
                    modifier = Modifier.testTag("map_show_controls_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "Show Controls",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.FullscreenExit,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Show",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // 4. Compact Top Search Bar + Single-Row Heading/Tilt Strip + Collapsible Tools Drawer
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Slim Top Search & Action Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xD6111815),
                    border = BorderStroke(1.dp, Color(0x88D4AF37))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            placeholder = {
                                Text(
                                    "Search place, city, address...",
                                    color = Color(0xFFB5C0BA),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { performLocationSearch() }),
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        searchResults = emptyList()
                                    }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = Color(0xFFB5C0BA), modifier = Modifier.size(18.dp))
                                    }
                                }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD4AF37),
                                unfocusedBorderColor = Color(0x55FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("map_search_input")
                        )
                        IconButton(
                            onClick = { performLocationSearch() },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF1B4D3E), RoundedCornerShape(10.dp))
                                .testTag("map_search_button")
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = "Search Location", tint = Color.White)
                            }
                        }
                        Surface(
                            onClick = {
                                showControlsPanel = false
                                showTransparencyBar = false
                                searchResults = emptyList()
                                hideAllControls = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x33FFFFFF),
                            border = BorderStroke(1.dp, Color(0x88FFD54F)),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("map_hide_controls_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VisibilityOff,
                                    contentDescription = "Hide Buttons Full Screen",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "Hide",
                                    color = Color(0xFFFFD54F),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Search Suggestions Dropdown Card (when multiple results are returned)
                if (searchResults.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xF0111815),
                        border = BorderStroke(1.dp, Color(0xAAD4AF37)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_search_results_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Matching Places (${searchResults.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFD4AF37),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { searchResults = emptyList() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close results", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            searchResults.forEachIndexed { idx, hit ->
                                if (idx > 0) HorizontalDivider(color = Color(0x33FFFFFF))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            centerMapOn(hit.latitude, hit.longitude, 19, showPinLabel = hit.title)
                                            searchResults = emptyList()
                                            onShowMessage("Centered on: ${hit.title}")
                                        }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = hit.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = hit.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFB5C0BA),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Single Compact Heading + Wall Tilt Strip (tap to expand full HUD)
                Surface(
                    onClick = { showFullHudCard = !showFullHudCard },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xCC111815),
                    border = BorderStroke(1.dp, Color(0x8800E5FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_wall_tilt_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", activeReading.azimuthDeg)}° ${activeReading.zone.code} (${activeReading.pada.code}) • ${selectedChakraType.shortName}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tilt Diff: $wallTiltSummary",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E5FF),
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
                        IconButton(
                            onClick = {
                                if (!isLocked) lockedReading = activeReading
                                isLocked = !isLocked
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .background(
                                    if (isLocked) Color(0xFFD4AF37) else Color(0x22FFFFFF),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = if (isLocked) "Unlock Compass" else "Lock Compass",
                                tint = if (isLocked) Color(0xFF111815) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Icon(
                            imageVector = if (showFullHudCard) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Expand Degree Details",
                            tint = Color(0xFFD0DBD5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showFullHudCard) {
                    CompassTopDegreeTiltHud(
                        reading = activeReading,
                        isLocked = isLocked,
                        titlePrefix = selectedChakraType.shortName.uppercase(Locale.US),
                        onToggleLock = {
                            if (!isLocked) lockedReading = activeReading
                            isLocked = !isLocked
                        }
                    )
                }

                // Collapsible Chakra Size, Transparency (Light/Dark), Layer & Manual Degree Panel
                if (showControlsPanel) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xEB111815),
                        border = BorderStroke(1.dp, Color(0x99D4AF37)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_controls_drawer")
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Map Layer Chips
                            Text(
                                text = "Map View & Chakra Type",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFD4AF37),
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MapLayerStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = layerStyle == style,
                                        onClick = {
                                            layerStyle = style
                                            webViewRef?.evaluateJavascript(
                                                "if (window.setMapLayer) window.setMapLayer('${style.id}');",
                                                null
                                            )
                                        },
                                        label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("map_layer_${style.id}")
                                    )
                                }
                            }

                            ChakraTypeSelectorRow(
                                selectedType = selectedChakraType,
                                onSelectType = { selectedChakraType = it },
                                tagPrefix = "map_chakra"
                            )

                            HorizontalDivider(color = Color(0x33FFFFFF))

                            // Shakti Chakra Transparency Level (Light <-> Dark) & Contrast Style
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Chakra Visibility: ${(chakraOpacity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = !chakraDarkContrast,
                                        onClick = { chakraDarkContrast = false },
                                        label = { Text("Light", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("map_chakra_light_mode")
                                    )
                                    FilterChip(
                                        selected = chakraDarkContrast,
                                        onClick = { chakraDarkContrast = true },
                                        label = { Text("Dark", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("map_chakra_dark_mode")
                                    )
                                }
                            }
                            Slider(
                                value = chakraOpacity,
                                onValueChange = { chakraOpacity = it },
                                valueRange = 0.15f..1.0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("map_chakra_opacity_slider")
                            )

                            // Shakti Chakra Size (Reduce <-> Enlarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Chakra Size: ${(chakraScale * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { chakraScale = (chakraScale - 0.10f).coerceAtLeast(0.40f) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("map_chakra_size_reduce")
                                    ) {
                                        Text("- Smaller", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { chakraScale = (chakraScale + 0.10f).coerceAtMost(1.45f) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("map_chakra_size_enlarge")
                                    ) {
                                        Text("+ Larger", color = Color(0xFFFFD54F), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Slider(
                                value = chakraScale,
                                onValueChange = { chakraScale = it },
                                valueRange = 0.40f..1.45f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("map_chakra_size_slider")
                            )

                            HorizontalDivider(color = Color(0x33FFFFFF))

                            // Heading Source & Manual Tilt
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

            // 5. Left Side Directly Visible Shakti Chakra Controls (Zoom In/Out, Transparency, Light/Dark)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Surface(
                    onClick = { chakraScale = (chakraScale + 0.10f).coerceAtMost(1.45f) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xCC111815),
                    border = BorderStroke(1.dp, Color(0x99FFD54F)),
                    modifier = Modifier
                        .width(62.dp)
                        .testTag("map_chakra_zoom_in_button")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.ZoomIn,
                            contentDescription = "Zoom In Shakti Chakra",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "Chakra +",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    onClick = { chakraScale = (chakraScale - 0.10f).coerceAtLeast(0.40f) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xCC111815),
                    border = BorderStroke(1.dp, Color(0x99FFD54F)),
                    modifier = Modifier
                        .width(62.dp)
                        .testTag("map_chakra_zoom_out_button")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.ZoomOut,
                            contentDescription = "Zoom Out Shakti Chakra",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "Chakra -",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    onClick = { showTransparencyBar = !showTransparencyBar },
                    shape = RoundedCornerShape(14.dp),
                    color = if (showTransparencyBar) Color(0xFFD4AF37) else Color(0xCC111815),
                    border = BorderStroke(1.dp, Color(0xAA00E5FF)),
                    modifier = Modifier
                        .width(62.dp)
                        .testTag("map_chakra_transparency_button")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Opacity,
                            contentDescription = "Shakti Chakra Transparency Level",
                            tint = if (showTransparencyBar) Color(0xFF111815) else Color(0xFF00E5FF),
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "${(chakraOpacity * 100).roundToInt()}% Vis",
                            color = if (showTransparencyBar) Color(0xFF111815) else Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    onClick = { chakraDarkContrast = !chakraDarkContrast },
                    shape = RoundedCornerShape(14.dp),
                    color = if (chakraDarkContrast) Color(0xE01B4D3E) else Color(0xCC111815),
                    border = BorderStroke(1.dp, Color(0x99D4AF37)),
                    modifier = Modifier
                        .width(62.dp)
                        .testTag("map_chakra_light_dark_button")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Brightness6,
                            contentDescription = "Switch Chakra Light or Dark Visibility",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = if (chakraDarkContrast) "Dark" else "Light",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // 6. Right Side Floating Map Zoom, Rotate, GPS My Location & Layer/Tune Menu
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { webViewRef?.evaluateJavascript("if (window.zoomInMap) window.zoomInMap();", null) },
                    shape = CircleShape,
                    color = Color(0xCC111815),
                    border = BorderStroke(1.dp, Color(0x88D4AF37)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Zoom In Map", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Surface(
                    onClick = { webViewRef?.evaluateJavascript("if (window.zoomOutMap) window.zoomOutMap();", null) },
                    shape = CircleShape,
                    color = Color(0xCC111815),
                    border = BorderStroke(1.dp, Color(0x88D4AF37)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Remove, contentDescription = "Zoom Out Map", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Surface(
                    onClick = { setJsMapBearing(mapBearingDeg - 5f) },
                    shape = CircleShape,
                    color = Color(0xCC111815),
                    border = BorderStroke(1.dp, Color(0x8800E5FF)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate Map Anticlockwise", tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                    }
                }
                Surface(
                    onClick = { setJsMapBearing(mapBearingDeg + 5f) },
                    shape = CircleShape,
                    color = Color(0xCC111815),
                    border = BorderStroke(1.dp, Color(0x8800E5FF)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Map Clockwise", tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                    }
                }
                Surface(
                    onClick = {
                        if (hasLocationPermission) {
                            requestSingleGpsFix(userTriggered = true)
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
                        .size(40.dp)
                        .testTag("map_gps_center_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLocatingGps) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Filled.MyLocation, contentDescription = "Center GPS My Location", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Surface(
                    onClick = { showControlsPanel = !showControlsPanel },
                    shape = CircleShape,
                    color = if (showControlsPanel) Color(0xFFD4AF37) else Color(0xCC111815),
                    border = BorderStroke(1.dp, Color(0x88D4AF37)),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("map_toggle_controls_drawer_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = "Map Layers, Chakra Type & Manual Degree",
                            tint = if (showControlsPanel) Color(0xFF111815) else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // 7. Bottom Column: Optional Fine Transparency & Size Slider Card + Slim Geo & Capture Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                if (showTransparencyBar) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xE6111815),
                        border = BorderStroke(1.dp, Color(0x9900E5FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("map_chakra_transparency_slider_card")
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
                                Text(
                                    text = "Shakti Chakra Transparency: ${(chakraOpacity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = !chakraDarkContrast,
                                        onClick = { chakraDarkContrast = false },
                                        label = { Text("Light", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = chakraDarkContrast,
                                        onClick = { chakraDarkContrast = true },
                                        label = { Text("Dark", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            Slider(
                                value = chakraOpacity,
                                onValueChange = { chakraOpacity = it },
                                valueRange = 0.15f..1.0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Chakra Size: ${(chakraScale * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "15% Faint ↔ 100% Solid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB5C0BA)
                                )
                            }
                            Slider(
                                value = chakraScale,
                                onValueChange = { chakraScale = it },
                                valueRange = 0.40f..1.45f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    color = Color(0xD9111815),
                    border = BorderStroke(1.dp, Color(0x88D4AF37)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.5f", mapLat)}, ${String.format(Locale.US, "%.5f", mapLng)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            val magText = if (activeReading.magneticFieldMicroTesla > 0f) {
                                "${activeReading.magneticFieldMicroTesla.toInt()} μT"
                            } else {
                                headingSource.label
                            }
                            val accText = gpsAccuracyMeters?.let { " • GPS ±${it.toInt()}m" } ?: ""
                            Text(
                                text = "$magText$accText",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD4AF37),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { captureAndSaveMapAlignment(updateReportPlotDeg = true) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1B4D3E),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("map_capture_save_button")
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Capture & Save", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { captureAndSaveMapAlignment(updateReportPlotDeg = false) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFFE6C574), modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    uploadPlanLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = "Upload Floor Plan", tint = Color(0xFFE6C574), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Plan", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
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
    position: fixed;
    inset: 0;
    margin: 0; padding: 0;
    width: 100vw; height: 100vh;
    overflow: hidden;
    background: #141c18;
    touch-action: none;
  }
  #map-rotator {
    position: absolute;
    left: 50%;
    top: 50%;
    width: 220vmax;
    height: 220vmax;
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

  window.refreshMapSize = function() {
    map.invalidateSize({ animate: false });
  };
  setTimeout(window.refreshMapSize, 150);
  setTimeout(window.refreshMapSize, 600);
  window.addEventListener('resize', window.refreshMapSize);
  if (typeof ResizeObserver !== 'undefined') {
    new ResizeObserver(window.refreshMapSize).observe(document.getElementById('map-rotator'));
  }

  var myLocationDot = null;
  var myLocationRing = null;
  window.setMyLocationMarker = function(lat, lng, accMeters) {
    if (myLocationDot) map.removeLayer(myLocationDot);
    if (myLocationRing) map.removeLayer(myLocationRing);
    myLocationRing = L.circle([lat, lng], {
      radius: accMeters || 15,
      color: '#1E88E5',
      fillColor: '#42A5F5',
      fillOpacity: 0.22,
      weight: 1.5
    }).addTo(map);
    myLocationDot = L.circleMarker([lat, lng], {
      radius: 7,
      color: '#FFFFFF',
      weight: 2.5,
      fillColor: '#1565C0',
      fillOpacity: 1.0
    }).addTo(map);
  };

  var searchPin = null;
  window.setSearchPin = function(lat, lng, label) {
    if (searchPin) map.removeLayer(searchPin);
    searchPin = L.circleMarker([lat, lng], {
      radius: 7,
      color: '#FFFFFF',
      weight: 2,
      fillColor: '#D4AF37',
      fillOpacity: 0.95
    }).addTo(map);
  };

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
    map.invalidateSize({ animate: false });
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
  var prevAngle = 0, prevDist = 0;

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
