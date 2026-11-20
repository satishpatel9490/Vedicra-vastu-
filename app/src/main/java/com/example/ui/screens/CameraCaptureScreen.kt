package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.local.MediaFileManager
import com.example.data.local.SiteMediaEntity
import com.example.data.repository.VastuRepository
import com.example.model.MediaKind
import com.example.model.VastuCompassMath
import com.example.sensor.rememberVastuCompassState
import com.example.ui.components.CameraCompassMode
import com.example.ui.components.CompactCardinalDialCanvas
import com.example.ui.components.CompassTopDegreeTiltHud
import com.example.ui.components.VastuChakraCanvas
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

@Composable
fun CameraCaptureScreen(
    reportId: String,
    mediaKind: MediaKind,
    drawingCategory: String,
    drawingTitle: String,
    floorNumber: String,
    orientationDirection: String,
    description: String,
    additionalNotes: String,
    repository: VastuRepository,
    mediaManager: MediaFileManager,
    onDone: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Vedicra Vastu uses your camera directly during site visits to photograph rooms, entrances, and floor plans and link them automatically to the client report.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Camera Access")
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        }
                    ) {
                        Text("App Settings")
                    }
                }
                OutlinedButton(onClick = onDone) {
                    Text("Back to Report")
                }
            }
        }
        return
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var linearZoom by remember { mutableFloatStateOf(0f) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var pendingReviewFile by remember { mutableStateOf<File?>(null) }
    var reviewRotation by remember { mutableIntStateOf(0) }
    var reviewCaption by remember { mutableStateOf("") }
    var capturedHeadingSummary by remember { mutableStateOf("") }
    var capturedOrientationTag by remember { mutableStateOf("") }
    var isCapturing by remember { mutableStateOf(false) }

    var compassMode by remember { mutableStateOf(CameraCompassMode.FULL_CHAKRA) }
    var isHeadingLocked by remember { mutableStateOf(false) }
    var manualAzimuthDeg by remember { mutableFloatStateOf(0f) }

    val liveCompass by rememberVastuCompassState(enabled = compassMode != CameraCompassMode.OFF)
    var lockedReading by remember { mutableStateOf(liveCompass) }
    val activeCompass = remember(liveCompass, isHeadingLocked, manualAzimuthDeg) {
        val base = if (isHeadingLocked) lockedReading else liveCompass
        if (!base.hasHardwareSensors) {
            base.copy(
                azimuthDeg = manualAzimuthDeg,
                zone = VastuCompassMath.zoneForDegree(manualAzimuthDeg),
                pada = VastuCompassMath.padaForDegree(manualAzimuthDeg)
            )
        } else {
            base
        }
    }

    val sessionCaptured = remember { mutableStateListOf<SiteMediaEntity>() }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, lensFacing, flashMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(flashMode)
                    .build()
                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                val cam = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                cam.cameraControl.setLinearZoom(linearZoom)
                boundCamera = cam
                imageCapture = capture
            }
        }
        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    LaunchedEffect(linearZoom, boundCamera) {
        boundCamera?.cameraControl?.setLinearZoom(linearZoom)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (pendingReviewFile == null) {
            // Live Camera Preview
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                linearZoom = (linearZoom + (zoom - 1f) * 0.45f).coerceIn(0f, 1f)
                            }
                        }
                    }
            )

            // Live Digital Vastu Chakra / Compact Compass Overlay
            if (compassMode == CameraCompassMode.FULL_CHAKRA) {
                VastuChakraCanvas(
                    rotationDegrees = -activeCompass.azimuthDeg,
                    transparentFill = true,
                    showDeityNames = true,
                    showTopLubberNeedle = true,
                    tiltRollDeg = activeCompass.rollDeg,
                    tiltPitchDeg = activeCompass.pitchDeg,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(340.dp)
                        .testTag("camera_vastu_chakra_overlay")
                )
            } else if (compassMode == CameraCompassMode.SIMPLE_DIAL) {
                CompactCardinalDialCanvas(
                    azimuthDeg = activeCompass.azimuthDeg,
                    tiltRollDeg = activeCompass.rollDeg,
                    isLevel = activeCompass.isLevel,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(260.dp)
                        .testTag("camera_compact_dial_overlay")
                )
            }

            // Top Controls + Top Degree & Tilt HUD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        IconButton(
                            onClick = onDone,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Return to Report",
                                tint = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = if (mediaKind == MediaKind.DRAWING) {
                                "Drawing: ${drawingTitle.ifBlank { drawingCategory }}"
                            } else {
                                "Site Photography"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = if (compassMode != CameraCompassMode.OFF) {
                                Color(0xCC1B4D3E)
                            } else {
                                Color.Black.copy(alpha = 0.55f)
                            }
                        ) {
                            IconButton(
                                onClick = {
                                    compassMode = when (compassMode) {
                                        CameraCompassMode.FULL_CHAKRA -> CameraCompassMode.SIMPLE_DIAL
                                        CameraCompassMode.SIMPLE_DIAL -> CameraCompassMode.OFF
                                        CameraCompassMode.OFF -> CameraCompassMode.FULL_CHAKRA
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("camera_compass_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Explore,
                                    contentDescription = "Compass Overlay: ${compassMode.label}",
                                    tint = if (compassMode != CameraCompassMode.OFF) Color(0xFFD4AF37) else Color.White
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f)
                        ) {
                            IconButton(
                                onClick = {
                                    flashMode = when (flashMode) {
                                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                                        else -> ImageCapture.FLASH_MODE_AUTO
                                    }
                                    imageCapture?.flashMode = flashMode
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = when (flashMode) {
                                        ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                                        ImageCapture.FLASH_MODE_OFF -> Icons.Filled.FlashOff
                                        else -> Icons.Filled.FlashAuto
                                    },
                                    contentDescription = "Toggle Flash",
                                    tint = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f)
                        ) {
                            IconButton(
                                onClick = {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cameraswitch,
                                    contentDescription = "Switch Front/Rear Camera",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                if (compassMode != CameraCompassMode.OFF) {
                    CompassTopDegreeTiltHud(
                        reading = activeCompass,
                        isLocked = isHeadingLocked,
                        onToggleLock = {
                            if (!isHeadingLocked) {
                                lockedReading = activeCompass
                            }
                            isHeadingLocked = !isHeadingLocked
                        }
                    )
                }
            }

            // Bottom Zoom + Capture + Continuous Strip
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (compassMode != CameraCompassMode.OFF && !activeCompass.hasHardwareSensors) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Compass", color = Color(0xFFD4AF37), style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = manualAzimuthDeg,
                            onValueChange = { manualAzimuthDeg = it },
                            valueRange = 0f..360f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${manualAzimuthDeg.toInt()}°",
                            color = Color(0xFFD4AF37),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Zoom", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = linearZoom,
                        onValueChange = { linearZoom = it },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(1f + linearZoom * 3f).let { String.format("%.1fx", it) }}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (sessionCaptured.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sessionCaptured, key = { it.id }) { captured ->
                            Box {
                                AsyncImage(
                                    model = File(captured.filePath),
                                    contentDescription = "Captured thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .rotate(captured.rotationDegrees.toFloat())
                                )
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deleteMediaItem(captured)
                                            sessionCaptured.remove(captured)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete captured photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sessionCaptured.size} saved",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Surface(
                        onClick = {
                            if (isCapturing) return@Surface
                            val cap = imageCapture ?: return@Surface
                            isCapturing = true
                            val outFile = mediaManager.createOriginalImageFile(
                                reportId = reportId,
                                prefix = if (mediaKind == MediaKind.DRAWING) "cam_drawing" else "cam_photo"
                            )
                            val outputOpts = ImageCapture.OutputFileOptions.Builder(outFile).build()
                            cap.takePicture(
                                outputOpts,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        isCapturing = false
                                        reviewRotation = 0
                                        if (compassMode != CameraCompassMode.OFF) {
                                            val deg = VastuCompassMath.normalizeDegrees(activeCompass.azimuthDeg)
                                            val tilt = if (activeCompass.isUprightCameraMode) {
                                                abs(activeCompass.rollDeg)
                                            } else {
                                                activeCompass.tiltFromLevelDeg
                                            }
                                            capturedOrientationTag = VastuCompassMath.formatHeadingLabel(deg)
                                            capturedHeadingSummary = "Compass: ${String.format("%.1f", deg)}° ${activeCompass.zone.code} " +
                                                "(${activeCompass.pada.code} ${activeCompass.pada.deity}) • Tilt: ${String.format("%.1f", tilt)}°"
                                            reviewCaption = capturedOrientationTag
                                        } else {
                                            capturedOrientationTag = ""
                                            capturedHeadingSummary = ""
                                            reviewCaption = ""
                                        }
                                        pendingReviewFile = outFile
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                        onShowMessage("Capture error: ${exception.message}")
                                    }
                                }
                            )
                        },
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(72.dp)
                            .border(4.dp, Color(0xFFD4AF37), CircleShape)
                            .testTag("camera_shutter_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Take Photograph",
                                tint = Color(0xFF1B4D3E),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B4D3E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Done")
                    }
                }
            }
        } else {
            // Instant Capture Preview & Accept / Retake / Rotate / Caption
            val file = pendingReviewFile!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preview Captured Image",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { reviewRotation = (reviewRotation + 90) % 360 },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RotateRight,
                            contentDescription = "Rotate Photo",
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF121212)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = file,
                        contentDescription = "Captured Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(reviewRotation.toFloat())
                    )
                }

                OutlinedTextField(
                    value = reviewCaption,
                    onValueChange = { reviewCaption = it },
                    label = { Text("Caption / Label (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFD4AF37),
                        unfocusedLabelColor = Color.LightGray,
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            file.delete()
                            pendingReviewFile = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Retake", color = Color.White)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val resolvedOrientation = orientationDirection.ifBlank { capturedOrientationTag }
                                val resolvedNotes = listOfNotNull(
                                    additionalNotes.takeIf { it.isNotBlank() },
                                    capturedHeadingSummary.takeIf { it.isNotBlank() }
                                ).joinToString(" | ")
                                val savedEntity = repository.addCapturedCameraPhoto(
                                    reportId = reportId,
                                    file = file,
                                    mediaKind = mediaKind,
                                    caption = reviewCaption,
                                    drawingCategory = drawingCategory,
                                    drawingTitle = drawingTitle,
                                    floorNumber = floorNumber,
                                    orientationDirection = resolvedOrientation,
                                    description = description,
                                    additionalNotes = resolvedNotes
                                )
                                val finalEntity = if (reviewRotation != 0) {
                                    val rotated = savedEntity.copy(rotationDegrees = reviewRotation)
                                    repository.updateMedia(rotated)
                                    rotated
                                } else savedEntity
                                sessionCaptured.add(finalEntity)
                                pendingReviewFile = null
                                onShowMessage("Photo saved to report")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B4D3E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("accept_captured_photo_button")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Accept & Save")
                    }
                }
            }
        }
    }
}
