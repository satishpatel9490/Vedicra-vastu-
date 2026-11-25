package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.example.model.AnnotationType
import com.example.model.EraseBrushStroke
import com.example.model.EraseStrokePoint
import com.example.model.GridOverlayStyle
import com.example.model.LayoutAnnotation
import com.example.model.VastuCompassMath
import com.example.model.VastuConstants
import com.example.pdf.PdfShareHelper
import com.example.sensor.rememberVastuCompassState
import com.example.ui.components.ChakraTypeSelectorRow
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.ManualDegreeInputControl
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VedicraTextField
import com.example.ui.components.drawChakraByTypeCompose
import com.example.ui.viewmodel.WorkspaceEvent
import com.example.ui.viewmodel.WorkspaceViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import java.util.Locale

private val workspaceColors = listOf(
    0xFF1B4D3E to "Forest",
    0xFFB5892E to "Gold",
    0xFFC62828 to "Alert Red",
    0xFF1565C0 to "Blue"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LayoutWorkspaceScreen(
    viewModel: WorkspaceViewModel,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val media by viewModel.media.collectAsStateWithLifecycle()
    val bitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val meta by viewModel.workspaceMeta.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedAnnotationId.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var panMode by remember { mutableStateOf(false) }
    var cleanCropMode by remember { mutableStateOf(false) }
    var activeColorHex by remember { mutableLongStateOf(0xFF1B4D3E) }
    var activeEraseColorHex by remember { mutableLongStateOf(0xFFFFFFFF) }
    var eraseBrushRadiusNorm by remember { mutableFloatStateOf(0.028f) }
    var activeStrokePoints by remember { mutableStateOf<List<EraseStrokePoint>>(emptyList()) }

    var showDirPickerDialog by remember { mutableStateOf(false) }
    var showRoomPickerDialog by remember { mutableStateOf(false) }
    var showTextPromptType by remember { mutableStateOf<AnnotationType?>(null) }
    var editingAnnotation by remember { mutableStateOf<LayoutAnnotation?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val liveCompass by rememberVastuCompassState(enabled = meta.showGridOverlay)
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WorkspaceEvent.Message -> onShowMessage(event.text)
                is WorkspaceEvent.ExportReady -> {
                    PdfShareHelper.shareImage(context, event.file, "Annotated Vastu Site Layout")
                }
            }
        }
    }

    val selectedAnnotation = remember(meta.annotations, selectedId) {
        meta.annotations.firstOrNull { it.id == selectedId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SITE GRID & LAYOUT WORKSPACE", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Original photo stays untouched",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.FitScreen, contentDescription = "Fit to Screen")
                    }
                    IconButton(
                        onClick = { viewModel.rotateImage90() },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Image 90°")
                    }
                    IconButton(
                        onClick = { viewModel.undo() },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.saveAnnotatedVersion(shareAfterSave = true) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Export Annotated Image")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 6.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (meta.showGridOverlay) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChakraTypeSelectorRow(
                                selectedType = meta.chakraType,
                                onSelectType = { viewModel.setChakraType(it) },
                                tagPrefix = "workspace_chakra"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GridOverlayStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = meta.overlayStyle == style,
                                        onClick = { viewModel.setOverlayStyle(style) },
                                        label = { Text(style.displayName, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("workspace_style_${style.name.lowercase()}")
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = {
                                        val aligned = VastuCompassMath.normalizeDegrees(-liveCompass.azimuthDeg)
                                        viewModel.setGridRotation(aligned)
                                        onShowMessage("Chakra North aligned to live compass (${String.format(Locale.US, "%.1f", liveCompass.azimuthDeg)}°)")
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("workspace_align_compass_button")
                                ) {
                                    Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Sync North", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            ManualDegreeInputControl(
                                degrees = meta.gridRotationDegrees,
                                onDegreesChange = { viewModel.setGridRotation(it) },
                                label = "Manual Grid / Chakra Tilt",
                                showSlider = true,
                                tagPrefix = "workspace_tilt_degree"
                            )
                        }
                    }

                    if (cleanCropMode) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Clean Branding / Erase & Crop Edges",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { viewModel.clearEraseStrokes() }) {
                                            Text("Clear Brush", style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(onClick = { viewModel.resetCrop() }) {
                                            Text("Reset Crop", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Mask:", style = MaterialTheme.typography.labelSmall)
                                    listOf(
                                        0xFFFFFFFFL to "White",
                                        0xFFFAF6EEL to "Paper",
                                        0xFF1F2421L to "Dark"
                                    ).forEach { (hex, title) ->
                                        FilterChip(
                                            selected = activeEraseColorHex == hex,
                                            onClick = { activeEraseColorHex = hex },
                                            label = { Text(title, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    listOf(0.015f to "S", 0.028f to "M", 0.050f to "L").forEach { (r, sz) ->
                                        FilterChip(
                                            selected = kotlin.math.abs(eraseBrushRadiusNorm - r) < 0.004f,
                                            onClick = { eraseBrushRadiusNorm = r },
                                            label = { Text(sz, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Crop L/R", style = MaterialTheme.typography.labelSmall)
                                    Slider(
                                        value = meta.cropLeft,
                                        onValueChange = { viewModel.updateCropRect(it, meta.cropTop, meta.cropRight, meta.cropBottom) },
                                        valueRange = 0f..0.45f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Slider(
                                        value = meta.cropRight,
                                        onValueChange = { viewModel.updateCropRect(meta.cropLeft, meta.cropTop, it, meta.cropBottom) },
                                        valueRange = 0.55f..1f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Crop T/B", style = MaterialTheme.typography.labelSmall)
                                    Slider(
                                        value = meta.cropTop,
                                        onValueChange = { viewModel.updateCropRect(meta.cropLeft, it, meta.cropRight, meta.cropBottom) },
                                        valueRange = 0f..0.45f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Slider(
                                        value = meta.cropBottom,
                                        onValueChange = { viewModel.updateCropRect(meta.cropLeft, meta.cropTop, meta.cropRight, it) },
                                        valueRange = 0.55f..1f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    if (selectedAnnotation != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Selected: ${selectedAnnotation.label.ifBlank { selectedAnnotation.type.name }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedAnnotation.type == AnnotationType.NORTH_ARROW) {
                                    IconButton(
                                        onClick = {
                                            viewModel.updateAnnotation(
                                                selectedAnnotation.copy(
                                                    rotationDegrees = (selectedAnnotation.rotationDegrees + 15f) % 360f
                                                ),
                                                recordUndo = true
                                            )
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate North Arrow")
                                    }
                                }
                                IconButton(onClick = { editingAnnotation = selectedAnnotation }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Selected")
                                }
                                IconButton(onClick = { viewModel.deleteSelectedAnnotation() }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete Selected",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // Color Palette + Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            workspaceColors.forEach { (hex, label) ->
                                Surface(
                                    onClick = { activeColorHex = hex },
                                    shape = CircleShape,
                                    color = Color(hex),
                                    border = if (activeColorHex == hex) {
                                        BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface)
                                    } else {
                                        BorderStroke(1.dp, Color.White)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    if (activeColorHex == hex) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = label,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = cleanCropMode,
                                onClick = {
                                    cleanCropMode = !cleanCropMode
                                    if (cleanCropMode) panMode = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Clean/Crop", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("workspace_clean_crop_chip")
                            )
                            FilterChip(
                                selected = panMode,
                                onClick = {
                                    panMode = !panMode
                                    if (panMode) cleanCropMode = false
                                },
                                leadingIcon = { Icon(Icons.Filled.PanTool, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text(if (panMode) "Zoom" else "Pan", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = meta.showGridOverlay,
                                onClick = { viewModel.toggleGridOverlay() },
                                leadingIcon = { Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Grid", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Tool Palette Strip
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            OutlinedButton(
                                onClick = { showDirPickerDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("workspace_add_direction_button")
                            ) {
                                Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("N..NW")
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = {
                                    viewModel.addAnnotation(
                                        LayoutAnnotation(
                                            type = AnnotationType.NORTH_ARROW,
                                            label = "North",
                                            colorHex = activeColorHex
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.CompassCalibration, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("North Arrow")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { showTextPromptType = AnnotationType.DEGREE_LABEL }) {
                                Text("Degrees")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { showRoomPickerDialog = true }) {
                                Icon(Icons.Filled.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Room")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { showTextPromptType = AnnotationType.ZONE_LABEL }) {
                                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Zone")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { showTextPromptType = AnnotationType.TEXT_NOTE }) {
                                Icon(Icons.Filled.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Text")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { showTextPromptType = AnnotationType.AREA_NOTE }) {
                                Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Area Note")
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = {
                                    viewModel.addAnnotation(
                                        LayoutAnnotation(
                                            type = AnnotationType.ARROW,
                                            label = "Arrow",
                                            x = 0.35f,
                                            y = 0.5f,
                                            endX = 0.65f,
                                            endY = 0.5f,
                                            colorHex = activeColorHex
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Arrow")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { showTextPromptType = AnnotationType.PIN_MARKER }) {
                                Icon(Icons.Filled.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Marker")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                        PrimaryDarkGreenButton(
                            text = if (isSaving) "Saving Version..." else "Save Annotated Version",
                            icon = Icons.Filled.Save,
                            enabled = !isSaving,
                            onClick = { viewModel.saveAnnotatedVersion(shareAfterSave = false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("workspace_save_version_button")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val bmp = bitmap
        if (bmp == null || media == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1D2220))
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                val maxW = constraints.maxWidth.toFloat()
                val maxH = constraints.maxHeight.toFloat()
                val imgAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                val boxAspect = maxW / maxH.coerceAtLeast(1f)
                val fittedW: Float
                val fittedH: Float
                if (imgAspect > boxAspect) {
                    fittedW = maxW
                    fittedH = maxW / imgAspect
                } else {
                    fittedH = maxH
                    fittedW = maxH * imgAspect
                }

                val boxDensity = androidx.compose.ui.platform.LocalDensity.current
                val widthDp = with(boxDensity) { fittedW.toDp() }
                val heightDp = with(boxDensity) { fittedH.toDp() }

                Box(
                    modifier = Modifier
                        .size(widthDp, heightDp)
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = panOffset.x,
                            translationY = panOffset.y
                        )
                        .let { mod ->
                            if (panMode) {
                                mod.pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val nextScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                        zoomScale = nextScale
                                        panOffset = if (nextScale > 1f) panOffset + pan else Offset.Zero
                                    }
                                }
                            } else {
                                mod
                            }
                        }
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Site Plan Workspace",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .let { canvasMod ->
                                when {
                                    panMode -> canvasMod
                                    cleanCropMode -> canvasMod.pointerInput(activeEraseColorHex, eraseBrushRadiusNorm) {
                                        detectDragGestures(
                                            onDragStart = { start ->
                                                val nx = (start.x / size.width).coerceIn(0f, 1f)
                                                val ny = (start.y / size.height).coerceIn(0f, 1f)
                                                activeStrokePoints = listOf(EraseStrokePoint(nx, ny))
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                                val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                                activeStrokePoints = activeStrokePoints + EraseStrokePoint(nx, ny)
                                            },
                                            onDragEnd = {
                                                if (activeStrokePoints.isNotEmpty()) {
                                                    viewModel.addEraseStroke(
                                                        EraseBrushStroke(
                                                            points = activeStrokePoints,
                                                            radiusNorm = eraseBrushRadiusNorm,
                                                            colorHex = activeEraseColorHex
                                                        )
                                                    )
                                                    activeStrokePoints = emptyList()
                                                }
                                            },
                                            onDragCancel = { activeStrokePoints = emptyList() }
                                        )
                                    }
                                    else -> canvasMod
                                        .pointerInput(meta.annotations) {
                                            detectTapGestures { offset ->
                                                val nx = offset.x / size.width
                                                val ny = offset.y / size.height
                                                val hit = meta.annotations.lastOrNull { ann ->
                                                    hypot(ann.x - nx, ann.y - ny) < 0.08f ||
                                                        (ann.type == AnnotationType.ARROW && hypot(ann.endX - nx, ann.endY - ny) < 0.08f)
                                                }
                                                viewModel.selectAnnotation(hit?.id)
                                            }
                                        }
                                        .pointerInput(meta.annotations, selectedId) {
                                            var draggingArrowEnd = false
                                            var activeId: String? = null
                                            detectDragGestures(
                                                onDragStart = { start ->
                                                    val nx = start.x / size.width
                                                    val ny = start.y / size.height
                                                    val hit = meta.annotations.lastOrNull { ann ->
                                                        val dStart = hypot(ann.x - nx, ann.y - ny)
                                                        val dEnd = if (ann.type == AnnotationType.ARROW) {
                                                            hypot(ann.endX - nx, ann.endY - ny)
                                                        } else 99f
                                                        dStart < 0.09f || dEnd < 0.09f
                                                    }
                                                    if (hit != null) {
                                                        activeId = hit.id
                                                        viewModel.selectAnnotation(hit.id)
                                                        draggingArrowEnd = hit.type == AnnotationType.ARROW &&
                                                            hypot(hit.endX - nx, hit.endY - ny) < hypot(hit.x - nx, hit.y - ny)
                                                    } else {
                                                        activeId = null
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val targetId = activeId ?: return@detectDragGestures
                                                    val current = meta.annotations.firstOrNull { it.id == targetId }
                                                        ?: return@detectDragGestures
                                                    val dx = dragAmount.x / size.width
                                                    val dy = dragAmount.y / size.height
                                                    val updated = if (draggingArrowEnd) {
                                                        current.copy(
                                                            endX = (current.endX + dx).coerceIn(0.03f, 0.97f),
                                                            endY = (current.endY + dy).coerceIn(0.03f, 0.97f)
                                                        )
                                                    } else {
                                                        val newX = (current.x + dx).coerceIn(0.03f, 0.97f)
                                                        val newY = (current.y + dy).coerceIn(0.03f, 0.97f)
                                                        if (current.type == AnnotationType.ARROW) {
                                                            current.copy(
                                                                x = newX,
                                                                y = newY,
                                                                endX = (current.endX + dx).coerceIn(0.03f, 0.97f),
                                                                endY = (current.endY + dy).coerceIn(0.03f, 0.97f)
                                                            )
                                                        } else {
                                                            current.copy(x = newX, y = newY)
                                                        }
                                                    }
                                                    viewModel.updateAnnotation(updated, recordUndo = false)
                                                }
                                            )
                                        }
                                }
                            }
                    ) {
                        // Draw saved and in-progress Clean / Erase strokes on plan
                        val allStrokes = if (activeStrokePoints.isNotEmpty()) {
                            meta.eraseStrokes + EraseBrushStroke(
                                points = activeStrokePoints,
                                radiusNorm = eraseBrushRadiusNorm,
                                colorHex = activeEraseColorHex
                            )
                        } else meta.eraseStrokes
                        allStrokes.forEach { stroke ->
                            if (stroke.points.isNotEmpty()) {
                                val col = Color(stroke.colorHex)
                                val strokePx = stroke.radiusNorm * min(size.width, size.height) * 2f
                                if (stroke.points.size == 1) {
                                    val pt = stroke.points.first()
                                    drawCircle(col, radius = strokePx / 2f, center = Offset(pt.x * size.width, pt.y * size.height))
                                } else {
                                    val path = Path()
                                    stroke.points.forEachIndexed { idx, pt ->
                                        val px = pt.x * size.width
                                        val py = pt.y * size.height
                                        if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                    }
                                    drawPath(
                                        path = path,
                                        color = col,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                        }

                        // Draw crop preview mask if edges trimmed
                        val hasCrop = meta.cropLeft > 0.005f || meta.cropTop > 0.005f || meta.cropRight < 0.995f || meta.cropBottom < 0.995f
                        if (hasCrop || cleanCropMode) {
                            val cl = meta.cropLeft * size.width
                            val ct = meta.cropTop * size.height
                            val cr = meta.cropRight * size.width
                            val cb = meta.cropBottom * size.height
                            if (cl > 0f) drawRect(Color(0x88000000), Offset.Zero, Size(cl, size.height))
                            if (cr < size.width) drawRect(Color(0x88000000), Offset(cr, 0f), Size(size.width - cr, size.height))
                            if (ct > 0f) drawRect(Color(0x88000000), Offset(cl, 0f), Size(cr - cl, ct))
                            if (cb < size.height) drawRect(Color(0x88000000), Offset(cl, cb), Size(cr - cl, size.height - cb))
                            drawRect(
                                color = Color(0xFFFFD54F),
                                topLeft = Offset(cl, ct),
                                size = Size(cr - cl, cb - ct),
                                style = Stroke(width = 3f)
                            )
                        }

                        if (meta.showGridOverlay) {
                            when (meta.overlayStyle) {
                                GridOverlayStyle.VASTU_CHAKRA_32 -> {
                                    drawChakraByTypeCompose(
                                        chakraType = meta.chakraType,
                                        rotationDegrees = meta.gridRotationDegrees,
                                        textMeasurer = textMeasurer,
                                        transparentFill = true,
                                        showDeityNames = true
                                    )
                                }
                                GridOverlayStyle.CLASSIC_9_GRID -> {
                                    drawComposeVastuGrid(meta.gridRotationDegrees, textMeasurer)
                                }
                                GridOverlayStyle.BOTH -> {
                                    drawComposeVastuGrid(meta.gridRotationDegrees, textMeasurer)
                                    drawChakraByTypeCompose(
                                        chakraType = meta.chakraType,
                                        rotationDegrees = meta.gridRotationDegrees,
                                        textMeasurer = textMeasurer,
                                        transparentFill = true,
                                        showDeityNames = true
                                    )
                                }
                            }
                        }
                        meta.annotations.forEach { ann ->
                            drawComposeAnnotation(
                                ann = ann,
                                isSelected = ann.id == selectedId,
                                textMeasurer = textMeasurer
                            )
                        }
                    }
                }

                // Top Degree & Tilt HUD inside Workspace Canvas
                if (meta.showGridOverlay) {
                    val normTilt = VastuCompassMath.normalizeDegrees(meta.gridRotationDegrees)
                    val topFacingDeg = VastuCompassMath.normalizeDegrees(360f - normTilt)
                    val topZone = VastuCompassMath.zoneForDegree(topFacingDeg)
                    val topPada = VastuCompassMath.padaForDegree(topFacingDeg)
                    val onZeroNorth = VastuCompassMath.isOnNorthBoundary(topFacingDeg)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xD6111815),
                        border = BorderStroke(1.dp, Color(0x99D4AF37)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("workspace_top_tilt_hud")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "North Tilt: ${String.format(Locale.US, "%.1f", normTilt)}°  •  Top: ${String.format(Locale.US, "%.1f", topFacingDeg)}° ${topZone.code}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (onZeroNorth) {
                                    "0° North strictly between N4 Bhallat ✓ & N5 Soma ✓  •  Phone Tilt: ${String.format(Locale.US, "%.1f", liveCompass.tiltFromLevelDeg)}°"
                                } else {
                                    "Pada ${topPada.code} (${topPada.deity}) • Phone Heading ${String.format(Locale.US, "%.1f", liveCompass.azimuthDeg)}° • Tilt ${String.format(Locale.US, "%.1f", liveCompass.tiltFromLevelDeg)}°"
                                },
                                color = Color(0xFFDDE5DF),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDirPickerDialog) {
        AlertDialog(
            onDismissRequest = { showDirPickerDialog = false },
            title = { Text("Add Directional Marker") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VastuConstants.eightDirectionMarkers.forEach { dir ->
                        Button(
                            onClick = {
                                viewModel.addAnnotation(
                                    LayoutAnnotation(
                                        type = AnnotationType.DIRECTION_MARKER,
                                        label = dir,
                                        colorHex = activeColorHex
                                    )
                                )
                                showDirPickerDialog = false
                            }
                        ) { Text(dir) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDirPickerDialog = false }) { Text("Close") }
            }
        )
    }

    if (showRoomPickerDialog) {
        var customRoom by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRoomPickerDialog = false },
            title = { Text("Add Room Label") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VastuConstants.quickRoomLabels.forEach { room ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    viewModel.addAnnotation(
                                        LayoutAnnotation(
                                            type = AnnotationType.ROOM_LABEL,
                                            label = room,
                                            colorHex = activeColorHex
                                        )
                                    )
                                    showRoomPickerDialog = false
                                },
                                label = { Text(room) }
                            )
                        }
                    }
                    VedicraTextField(
                        value = customRoom,
                        onValueChange = { customRoom = it },
                        label = "Or Custom Room Name"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customRoom.isNotBlank()) {
                            viewModel.addAnnotation(
                                LayoutAnnotation(
                                    type = AnnotationType.ROOM_LABEL,
                                    label = customRoom.trim(),
                                    colorHex = activeColorHex
                                )
                            )
                        }
                        showRoomPickerDialog = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showRoomPickerDialog = false }) { Text("Cancel") }
            }
        )
    }

    showTextPromptType?.let { type ->
        var label by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        val title = when (type) {
            AnnotationType.DEGREE_LABEL -> "Add Degree Label"
            AnnotationType.ZONE_LABEL -> "Add Zone Label"
            AnnotationType.AREA_NOTE -> "Add Area Note"
            AnnotationType.PIN_MARKER -> "Add Marker Pin"
            else -> "Add Text Annotation"
        }
        AlertDialog(
            onDismissRequest = { showTextPromptType = null },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    VedicraTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = when (type) {
                            AnnotationType.DEGREE_LABEL -> "Degree (e.g. 45° NE)"
                            AnnotationType.PIN_MARKER -> "Short Pin Tag (e.g. 1, WC)"
                            else -> "Label"
                        }
                    )
                    if (type == AnnotationType.AREA_NOTE || type == AnnotationType.TEXT_NOTE) {
                        VedicraTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = "Detailed Note",
                            singleLine = false,
                            minLines = 2
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalLabel = label.trim().ifBlank {
                            when (type) {
                                AnnotationType.DEGREE_LABEL -> "0° N"
                                AnnotationType.ZONE_LABEL -> "NE Zone"
                                AnnotationType.PIN_MARKER -> "•"
                                else -> "Note"
                            }
                        }
                        viewModel.addAnnotation(
                            LayoutAnnotation(
                                type = type,
                                label = finalLabel,
                                note = note.trim(),
                                colorHex = activeColorHex
                            )
                        )
                        showTextPromptType = null
                    }
                ) { Text("Place on Plan") }
            },
            dismissButton = {
                TextButton(onClick = { showTextPromptType = null }) { Text("Cancel") }
            }
        )
    }

    editingAnnotation?.let { target ->
        var label by remember(target) { mutableStateOf(target.label) }
        var note by remember(target) { mutableStateOf(target.note) }
        AlertDialog(
            onDismissRequest = { editingAnnotation = null },
            title = { Text("Edit Annotation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    VedicraTextField(value = label, onValueChange = { label = it }, label = "Label")
                    VedicraTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Note",
                        singleLine = false,
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAnnotation(target.copy(label = label, note = note), recordUndo = true)
                        editingAnnotation = null
                    }
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { editingAnnotation = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearConfirm) {
        ConfirmActionDialog(
            title = "Clear All Annotations?",
            message = "Remove all markers, labels and grid overlays from this workspace? The original image will remain intact.",
            confirmLabel = "Clear All",
            isDestructive = true,
            onConfirm = {
                showClearConfirm = false
                viewModel.clearAllAnnotations()
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

private fun DrawScope.drawComposeVastuGrid(rotationDeg: Float, textMeasurer: TextMeasurer) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = min(size.width, size.height) * 0.43f

    rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
        val gridHalf = radius * 0.85f
        val left = cx - gridHalf
        val top = cy - gridHalf
        drawRect(
            color = Color(0xAA1B4D3E),
            topLeft = Offset(left, top),
            size = Size(gridHalf * 2f, gridHalf * 2f),
            style = Stroke(width = 2.5f)
        )
        for (i in 1..2) {
            val gx = left + (gridHalf * 2f * i / 3f)
            val gy = top + (gridHalf * 2f * i / 3f)
            drawLine(Color(0x991B4D3E), Offset(gx, top), Offset(gx, top + gridHalf * 2f), strokeWidth = 1.8f)
            drawLine(Color(0x991B4D3E), Offset(left, gy), Offset(left + gridHalf * 2f, gy), strokeWidth = 1.8f)
        }

        drawCircle(
            color = Color(0xCC1B4D3E),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = Color(0xAAB5892E),
            radius = radius * 0.33f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        )

        for (i in 0 until 16) {
            val angleRad = Math.toRadians(i * 22.5 - 90.0)
            val ex = cx + (radius * cos(angleRad)).toFloat()
            val ey = cy + (radius * sin(angleRad)).toFloat()
            drawLine(
                color = if (i % 2 == 0) Color(0xAA1B4D3E) else Color(0x88B5892E),
                start = Offset(cx, cy),
                end = Offset(ex, ey),
                strokeWidth = if (i % 2 == 0) 2.5f else 1.5f
            )
        }

        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        dirs.forEachIndexed { idx, dir ->
            val angleRad = Math.toRadians(idx * 45.0 - 90.0)
            val lx = cx + ((radius + 24f) * cos(angleRad)).toFloat()
            val ly = cy + ((radius + 24f) * sin(angleRad)).toFloat()
            drawCircle(color = Color(0xEEFAF6EE), radius = 18f, center = Offset(lx, ly))
            drawCircle(color = Color(0xFF1B4D3E), radius = 18f, center = Offset(lx, ly), style = Stroke(2f))
            val res = textMeasurer.measure(
                text = dir,
                style = TextStyle(color = Color(0xFF1B4D3E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            drawText(
                textLayoutResult = res,
                topLeft = Offset(lx - res.size.width / 2f, ly - res.size.height / 2f)
            )
        }
    }
}

private fun DrawScope.drawComposeAnnotation(
    ann: LayoutAnnotation,
    isSelected: Boolean,
    textMeasurer: TextMeasurer
) {
    val px = ann.x * size.width
    val py = ann.y * size.height
    val accent = Color(ann.colorHex)

    when (ann.type) {
        AnnotationType.DIRECTION_MARKER,
        AnnotationType.DEGREE_LABEL -> {
            val text = ann.label.ifBlank { "N" }
            val res = textMeasurer.measure(
                text = text,
                style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            val boxW = (res.size.width + 24f).coerceAtLeast(38f)
            val boxH = (res.size.height + 14f).coerceAtLeast(30f)
            val topLeft = Offset(px - boxW / 2f, py - boxH / 2f)
            drawRoundRect(color = accent, topLeft = topLeft, size = Size(boxW, boxH), cornerRadius = CornerRadius(12f, 12f))
            drawRoundRect(
                color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                topLeft = topLeft,
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = if (isSelected) 4f else 2f)
            )
            drawText(res, topLeft = Offset(px - res.size.width / 2f, py - res.size.height / 2f))
        }

        AnnotationType.NORTH_ARROW -> {
            rotate(degrees = ann.rotationDegrees, pivot = Offset(px, py)) {
                val r = 34f
                val path = Path().apply {
                    moveTo(px, py - r)
                    lineTo(px + r * 0.55f, py + r * 0.7f)
                    lineTo(px, py + r * 0.35f)
                    lineTo(px - r * 0.55f, py + r * 0.7f)
                    close()
                }
                drawPath(path, color = accent)
                drawPath(path, color = if (isSelected) Color(0xFFFFD54F) else Color.White, style = Stroke(3f))
                val res = textMeasurer.measure(
                    text = "N",
                    style = TextStyle(color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                )
                drawText(res, topLeft = Offset(px - res.size.width / 2f, py - r - res.size.height - 2f))
            }
        }

        AnnotationType.ROOM_LABEL,
        AnnotationType.ZONE_LABEL,
        AnnotationType.TEXT_NOTE,
        AnnotationType.AREA_NOTE -> {
            val text = if (ann.note.isNotBlank() && ann.label.isNotBlank()) {
                "${ann.label}: ${ann.note}"
            } else ann.label.ifBlank { ann.note.ifBlank { "Note" } }
            val res = textMeasurer.measure(
                text = text,
                style = TextStyle(color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            )
            val boxW = (res.size.width + 22f).coerceAtLeast(48f)
            val boxH = (res.size.height + 14f).coerceAtLeast(28f)
            val topLeft = Offset(px - boxW / 2f, py - boxH / 2f)
            drawRoundRect(color = Color(0xF0FFFDF8), topLeft = topLeft, size = Size(boxW, boxH), cornerRadius = CornerRadius(8f, 8f))
            drawRoundRect(
                color = if (isSelected) Color(0xFFFFD54F) else accent,
                topLeft = topLeft,
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = if (isSelected) 4f else 2f)
            )
            drawText(res, topLeft = Offset(px - res.size.width / 2f, py - res.size.height / 2f))
        }

        AnnotationType.ARROW -> {
            val ex = ann.endX * size.width
            val ey = ann.endY * size.height
            drawLine(
                color = if (isSelected) Color(0xFFFFD54F) else accent,
                start = Offset(px, py),
                end = Offset(ex, ey),
                strokeWidth = 6f
            )
            val angle = atan2((ey - py).toDouble(), (ex - px).toDouble())
            val headLen = 22f
            val p1x = ex - (headLen * cos(angle - Math.PI / 6)).toFloat()
            val p1y = ey - (headLen * sin(angle - Math.PI / 6)).toFloat()
            val p2x = ex - (headLen * cos(angle + Math.PI / 6)).toFloat()
            val p2y = ey - (headLen * sin(angle + Math.PI / 6)).toFloat()
            val head = Path().apply {
                moveTo(ex, ey)
                lineTo(p1x, p1y)
                lineTo(p2x, p2y)
                close()
            }
            drawPath(head, color = accent)
            if (isSelected) {
                drawCircle(Color(0xFFFFD54F), radius = 10f, center = Offset(px, py))
                drawCircle(Color(0xFFFFD54F), radius = 10f, center = Offset(ex, ey))
            }
        }

        AnnotationType.PIN_MARKER -> {
            drawCircle(color = accent, radius = 18f, center = Offset(px, py))
            drawCircle(
                color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                radius = 18f,
                center = Offset(px, py),
                style = Stroke(width = if (isSelected) 4f else 2.5f)
            )
            if (ann.label.isNotBlank()) {
                val res = textMeasurer.measure(
                    text = ann.label.take(3),
                    style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                drawText(res, topLeft = Offset(px - res.size.width / 2f, py - res.size.height / 2f))
            }
        }
    }
}
