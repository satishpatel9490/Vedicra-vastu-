package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withRotation
import com.example.data.local.JsonCodec
import com.example.data.repository.VastuRepository
import com.example.model.CustomLayoutPlan
import com.example.model.GridOverlayStyle
import com.example.model.LayoutItemCategory
import com.example.model.LayoutWallLine
import com.example.model.MediaKind
import com.example.model.PlacedLayoutItem
import com.example.model.VastuChakraType
import com.example.model.VastuCompassMath
import com.example.model.WorkspaceMetadata
import com.example.pdf.WorkspaceRenderer
import com.example.ui.components.ChakraTypeSelectorRow
import com.example.ui.components.ManualDegreeInputControl
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VastuChakraCanvas
import com.example.ui.components.VedicraTextField
import com.example.ui.theme.LocalVedicraColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class BuilderToolMode {
    PLACE_MOVE,
    DRAW_WALL
}

private data class ScaledPlotPolygon(
    val tlX: Float, val tlY: Float,
    val trX: Float, val trY: Float,
    val brX: Float, val brY: Float,
    val blX: Float, val blY: Float
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LayoutBuilderScreen(
    reportId: String,
    mediaId: String? = null,
    repository: VastuRepository,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val brandColors = LocalVedicraColors.current
    val textMeasurer = rememberTextMeasurer()

    var plan by remember { mutableStateOf(CustomLayoutPlan()) }
    var topStr by remember { mutableStateOf("60") }
    var bottomStr by remember { mutableStateOf("60") }
    var leftStr by remember { mutableStateOf("40") }
    var rightStr by remember { mutableStateOf("40") }
    var toolMode by remember { mutableStateOf(BuilderToolMode.PLACE_MOVE) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var dimensionsExpanded by remember { mutableStateOf(true) }
    var showEditItemDialog by remember { mutableStateOf<PlacedLayoutItem?>(null) }
    var activeWallStart by remember { mutableStateOf<Offset?>(null) }
    var activeWallEnd by remember { mutableStateOf<Offset?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Load existing scaled layout OR pre-fill tilt from report.plotDegrees
    LaunchedEffect(reportId, mediaId) {
        if (!mediaId.isNullOrBlank()) {
            val media = repository.getMediaById(mediaId)
            if (media != null) {
                val meta = JsonCodec.decodeWorkspaceMetadata(media.annotationsJson)
                val existingPlan = meta.customLayoutPlan
                if (existingPlan != null) {
                    plan = existingPlan
                    topStr = trimFloat(existingPlan.topLength)
                    bottomStr = trimFloat(existingPlan.bottomLength)
                    leftStr = trimFloat(existingPlan.leftLength)
                    rightStr = trimFloat(existingPlan.rightLength)
                    return@LaunchedEffect
                }
            }
        }
        val bundle = repository.getReportBundle(reportId)
        val parsedPlotDeg = bundle?.report?.plotDegrees?.let { VastuCompassMath.parseManualDegrees(it) }
        if (parsedPlotDeg != null) {
            plan = plan.copy(northTiltDeg = parsedPlotDeg)
        }
    }

    val selectedItem = remember(plan.items, selectedItemId) {
        plan.items.firstOrNull { it.id == selectedItemId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Scaled Site Layout Builder", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${plan.title} • ${trimFloat(plan.topLength)}×${trimFloat(plan.leftLength)} ${plan.unit} • Tilt ${String.format(Locale.US, "%.1f", plan.northTiltDeg)}°",
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
                        enabled = !isSaving,
                        onClick = {
                            isSaving = true
                            scope.launch {
                                saveScaledLayoutToReport(
                                    reportId = reportId,
                                    existingMediaId = mediaId,
                                    plan = plan,
                                    repository = repository
                                )
                                isSaving = false
                                onShowMessage("Scaled Layout & Chakra saved to Report Drawings!")
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("layout_builder_save_top")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Layout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Dimensions & North Degree Card (Collapsible)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, brandColors.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Plot Dimensions (Top, Bottom, Left, Right & N/E/S/W)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Top ${topStr}${plan.unit} • Right ${rightStr}${plan.unit} • Bottom ${bottomStr}${plan.unit} • Left ${leftStr}${plan.unit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { dimensionsExpanded = !dimensionsExpanded }) {
                            Icon(
                                imageVector = if (dimensionsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (dimensionsExpanded) "Collapse Dimensions" else "Expand Dimensions"
                            )
                        }
                    }

                    if (dimensionsExpanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VedicraTextField(
                                value = plan.title,
                                onValueChange = { plan = plan.copy(title = it) },
                                label = "Layout Title",
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = plan.unit == "ft",
                                onClick = { plan = plan.copy(unit = "ft") },
                                label = { Text("ft") }
                            )
                            FilterChip(
                                selected = plan.unit == "m",
                                onClick = { plan = plan.copy(unit = "m") },
                                label = { Text("m") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VedicraTextField(
                                value = topStr,
                                onValueChange = { v ->
                                    topStr = v
                                    v.toFloatOrNull()?.takeIf { it > 0f }?.let { len ->
                                        plan = plan.copy(topLength = len, northSideLength = v)
                                    }
                                },
                                label = "Top / North (${plan.unit})",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                                testTag = "layout_top_length_input"
                            )
                            VedicraTextField(
                                value = bottomStr,
                                onValueChange = { v ->
                                    bottomStr = v
                                    v.toFloatOrNull()?.takeIf { it > 0f }?.let { len ->
                                        plan = plan.copy(bottomLength = len, southSideLength = v)
                                    }
                                },
                                label = "Bottom / South (${plan.unit})",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                                testTag = "layout_bottom_length_input"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VedicraTextField(
                                value = leftStr,
                                onValueChange = { v ->
                                    leftStr = v
                                    v.toFloatOrNull()?.takeIf { it > 0f }?.let { len ->
                                        plan = plan.copy(leftLength = len, westSideLength = v)
                                    }
                                },
                                label = "Left / West (${plan.unit})",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                                testTag = "layout_left_length_input"
                            )
                            VedicraTextField(
                                value = rightStr,
                                onValueChange = { v ->
                                    rightStr = v
                                    v.toFloatOrNull()?.takeIf { it > 0f }?.let { len ->
                                        plan = plan.copy(rightLength = len, eastSideLength = v)
                                    }
                                },
                                label = "Right / East (${plan.unit})",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                                testTag = "layout_right_length_input"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VedicraTextField(
                                value = plan.northSideLength,
                                onValueChange = { plan = plan.copy(northSideLength = it) },
                                label = "N Dim",
                                modifier = Modifier.weight(1f)
                            )
                            VedicraTextField(
                                value = plan.eastSideLength,
                                onValueChange = { plan = plan.copy(eastSideLength = it) },
                                label = "E Dim",
                                modifier = Modifier.weight(1f)
                            )
                            VedicraTextField(
                                value = plan.southSideLength,
                                onValueChange = { plan = plan.copy(southSideLength = it) },
                                label = "S Dim",
                                modifier = Modifier.weight(1f)
                            )
                            VedicraTextField(
                                value = plan.westSideLength,
                                onValueChange = { plan = plan.copy(westSideLength = it) },
                                label = "W Dim",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        ManualDegreeInputControl(
                            degrees = plan.northTiltDeg,
                            onDegreesChange = { plan = plan.copy(northTiltDeg = it) },
                            label = "North Degree Tilt on Plot",
                            showSlider = true,
                            tagPrefix = "layout_builder_degree"
                        )
                    }
                }
            }

            // 2. Chakra Overlay & Tool Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = toolMode == BuilderToolMode.PLACE_MOVE,
                    onClick = { toolMode = BuilderToolMode.PLACE_MOVE },
                    leadingIcon = { Icon(Icons.Filled.OpenWith, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Place / Move") }
                )
                FilterChip(
                    selected = toolMode == BuilderToolMode.DRAW_WALL,
                    onClick = { toolMode = BuilderToolMode.DRAW_WALL },
                    leadingIcon = { Icon(Icons.Filled.Draw, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Draw Walls") },
                    modifier = Modifier.testTag("layout_draw_walls_mode_chip")
                )
                FilterChip(
                    selected = plan.showChakraOverlay,
                    onClick = { plan = plan.copy(showChakraOverlay = !plan.showChakraOverlay) },
                    leadingIcon = { Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Chakra Overlay") },
                    modifier = Modifier.testTag("layout_toggle_chakra_chip")
                )
                FilterChip(
                    selected = plan.show9Grid,
                    onClick = { plan = plan.copy(show9Grid = !plan.show9Grid) },
                    leadingIcon = { Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("9-Grid") }
                )
                if (plan.wallLines.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            plan = plan.copy(wallLines = plan.wallLines.dropLast(1))
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Wall", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Undo Wall", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (plan.showChakraOverlay) {
                ChakraTypeSelectorRow(
                    selectedType = plan.chakraType,
                    onSelectType = { plan = plan.copy(chakraType = it) },
                    tagPrefix = "layout_builder_chakra"
                )
            }

            // 3. Interactive Scaled Layout Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFFAF8F2), RoundedCornerShape(14.dp))
            ) {
                val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(toolMode, plan.items, canvasW, canvasH) {
                            detectTapGestures { tap ->
                                val xn = (tap.x / canvasW).coerceIn(0f, 1f)
                                val yn = (tap.y / canvasH).coerceIn(0f, 1f)
                                val hit = plan.items.lastOrNull { item ->
                                    abs(item.xNorm - xn) <= item.widthNorm / 2f + 0.03f &&
                                        abs(item.yNorm - yn) <= item.heightNorm / 2f + 0.03f
                                }
                                selectedItemId = hit?.id
                            }
                        }
                        .pointerInput(toolMode, selectedItemId, canvasW, canvasH) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val xn = (offset.x / canvasW).coerceIn(0.05f, 0.95f)
                                    val yn = (offset.y / canvasH).coerceIn(0.05f, 0.95f)
                                    if (toolMode == BuilderToolMode.DRAW_WALL) {
                                        activeWallStart = Offset(xn, yn)
                                        activeWallEnd = Offset(xn, yn)
                                    } else {
                                        val hit = plan.items.lastOrNull { item ->
                                            abs(item.xNorm - xn) <= item.widthNorm / 2f + 0.04f &&
                                                abs(item.yNorm - yn) <= item.heightNorm / 2f + 0.04f
                                        }
                                        if (hit != null) {
                                            selectedItemId = hit.id
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (toolMode == BuilderToolMode.DRAW_WALL) {
                                        val curr = activeWallEnd ?: return@detectDragGestures
                                        activeWallEnd = Offset(
                                            (curr.x + dragAmount.x / canvasW).coerceIn(0.06f, 0.94f),
                                            (curr.y + dragAmount.y / canvasH).coerceIn(0.06f, 0.94f)
                                        )
                                    } else {
                                        val targetId = selectedItemId ?: return@detectDragGestures
                                        val dx = dragAmount.x / canvasW
                                        val dy = dragAmount.y / canvasH
                                        plan = plan.copy(
                                            items = plan.items.map { item ->
                                                if (item.id == targetId) {
                                                    item.copy(
                                                        xNorm = (item.xNorm + dx).coerceIn(0.08f, 0.92f),
                                                        yNorm = (item.yNorm + dy).coerceIn(0.08f, 0.92f)
                                                    )
                                                } else item
                                            }
                                        )
                                    }
                                },
                                onDragEnd = {
                                    if (toolMode == BuilderToolMode.DRAW_WALL) {
                                        val s = activeWallStart
                                        val e = activeWallEnd
                                        if (s != null && e != null && kotlin.math.hypot((e.x - s.x).toDouble(), (e.y - s.y).toDouble()) > 0.03) {
                                            plan = plan.copy(
                                                wallLines = plan.wallLines + LayoutWallLine(
                                                    x1 = s.x,
                                                    y1 = s.y,
                                                    x2 = e.x,
                                                    y2 = e.y
                                                )
                                            )
                                        }
                                        activeWallStart = null
                                        activeWallEnd = null
                                    }
                                }
                            )
                        }
                ) {
                    // Blueprint Plot, Walls, Items & 9-Grid Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Blueprint subtle architectural grid
                        val step = w / 20f
                        for (i in 0..20) {
                            val p = i * step
                            val isMajor = i % 5 == 0
                            val col = if (isMajor) Color(0xFFDDE3DF) else Color(0xFFEDF1EE)
                            drawLine(col, Offset(p, 0f), Offset(p, h), strokeWidth = if (isMajor) 1.4f else 0.8f)
                            drawLine(col, Offset(0f, p), Offset(w, p), strokeWidth = if (isMajor) 1.4f else 0.8f)
                        }

                        // Scaled Plot Polygon
                        val poly = computePlotPolygon(plan, w, h)
                        val plotPath = Path().apply {
                            moveTo(poly.tlX, poly.tlY)
                            lineTo(poly.trX, poly.trY)
                            lineTo(poly.brX, poly.brY)
                            lineTo(poly.blX, poly.blY)
                            close()
                        }
                        drawPath(plotPath, Color(0xFFFDFBF4))
                        drawPath(plotPath, Color(0xFF1B4D3E), style = Stroke(width = 3.5f))

                        // Dimension Callouts on 4 sides (Top/Bottom/Left/Right + N/E/S/W)
                        val dimStyle = TextStyle(color = Color(0xFF1B4D3E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val topLbl = "TOP (N): ${trimFloat(plan.topLength)} ${plan.unit}"
                        val botLbl = "BOTTOM (S): ${trimFloat(plan.bottomLength)} ${plan.unit}"
                        val leftLbl = "L (W): ${trimFloat(plan.leftLength)} ${plan.unit}"
                        val rightLbl = "R (E): ${trimFloat(plan.rightLength)} ${plan.unit}"
                        val topRes = textMeasurer.measure(topLbl, dimStyle)
                        drawText(topRes, topLeft = Offset((poly.tlX + poly.trX) / 2f - topRes.size.width / 2f, poly.tlY - topRes.size.height - 4f))
                        val botRes = textMeasurer.measure(botLbl, dimStyle)
                        drawText(botRes, topLeft = Offset((poly.blX + poly.brX) / 2f - botRes.size.width / 2f, poly.blY + 4f))
                        val leftRes = textMeasurer.measure(leftLbl, dimStyle)
                        drawText(leftRes, topLeft = Offset(max(4f, poly.tlX - leftRes.size.width / 2f), (poly.tlY + poly.blY) / 2f - leftRes.size.height / 2f))
                        val rightRes = textMeasurer.measure(rightLbl, dimStyle)
                        drawText(rightRes, topLeft = Offset(min(w - rightRes.size.width - 4f, poly.trX - rightRes.size.width / 2f), (poly.trY + poly.brY) / 2f - rightRes.size.height / 2f))

                        // Optional 9-Grid inside plot center
                        if (plan.show9Grid) {
                            val gxMin = min(poly.tlX, poly.blX)
                            val gxMax = max(poly.trX, poly.brX)
                            val gyMin = min(poly.tlY, poly.trY)
                            val gyMax = max(poly.blY, poly.brY)
                            val gw = gxMax - gxMin
                            val gh = gyMax - gyMin
                            for (i in 1..2) {
                                drawLine(Color(0x88B5892E), Offset(gxMin + gw * i / 3f, gyMin), Offset(gxMin + gw * i / 3f, gyMax), strokeWidth = 1.6f)
                                drawLine(Color(0x88B5892E), Offset(gxMin, gyMin + gh * i / 3f), Offset(gxMax, gyMin + gh * i / 3f), strokeWidth = 1.6f)
                            }
                        }

                        // Drawn Partition Wall Lines
                        plan.wallLines.forEach { wall ->
                            drawLine(
                                color = Color(0xFF263238),
                                start = Offset(wall.x1 * w, wall.y1 * h),
                                end = Offset(wall.x2 * w, wall.y2 * h),
                                strokeWidth = 4.5f,
                                cap = StrokeCap.Round
                            )
                        }
                        if (activeWallStart != null && activeWallEnd != null) {
                            drawLine(
                                color = Color(0xFFD32F2F),
                                start = Offset(activeWallStart!!.x * w, activeWallStart!!.y * h),
                                end = Offset(activeWallEnd!!.x * w, activeWallEnd!!.y * h),
                                strokeWidth = 4.5f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Placed Items (Rooms, Office, Labour, Machine, Raw Material, Toilet, Entrance, etc.)
                        plan.items.forEach { item ->
                            val cx = item.xNorm * w
                            val cy = item.yNorm * h
                            val bw = item.widthNorm * w
                            val bh = item.heightNorm * h
                            val isSel = item.id == selectedItemId
                            val itemColor = Color(item.colorHex)
                            val (_, zone, pada) = VastuCompassMath.analyzePlanPosition(
                                xNorm = item.xNorm,
                                yNorm = item.yNorm,
                                chakraRotationDeg = -plan.northTiltDeg
                            )

                            rotate(degrees = item.rotationDeg, pivot = Offset(cx, cy)) {
                                drawRect(
                                    color = itemColor.copy(alpha = 0.24f),
                                    topLeft = Offset(cx - bw / 2f, cy - bh / 2f),
                                    size = Size(bw, bh)
                                )
                                drawRect(
                                    color = if (isSel) Color(0xFFD4AF37) else itemColor,
                                    topLeft = Offset(cx - bw / 2f, cy - bh / 2f),
                                    size = Size(bw, bh),
                                    style = Stroke(width = if (isSel) 4f else 2f)
                                )
                                val lblRes = textMeasurer.measure(
                                    text = "${item.label}\n(${zone.code} • ${pada.code})",
                                    style = TextStyle(
                                        color = Color(0xFF111815),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                drawText(
                                    textLayoutResult = lblRes,
                                    topLeft = Offset(cx - lblRes.size.width / 2f, cy - lblRes.size.height / 2f)
                                )
                            }
                        }
                    }

                    // Selectable Semi-Transparent Vastu Chakra Overlay
                    if (plan.showChakraOverlay) {
                        VastuChakraCanvas(
                            rotationDegrees = -plan.northTiltDeg,
                            chakraType = plan.chakraType,
                            transparentFill = true,
                            showDeityNames = true,
                            showTopLubberNeedle = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .testTag("layout_builder_chakra_canvas")
                        )
                    }
                }
            }

            // 4. Add Rooms / Labour / Office / Machine / Raw Material / Toilet / Entrance Palette
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, brandColors.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Add Rooms, Machinery, Labour, Toilet, Entrance & More",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LayoutItemCategory.entries.forEach { cat ->
                            OutlinedButton(
                                onClick = {
                                    val count = plan.items.count { it.category == cat } + 1
                                    val newItem = PlacedLayoutItem(
                                        category = cat,
                                        label = if (count > 1) "${cat.label} $count" else cat.label,
                                        xNorm = (0.35f + (plan.items.size % 4) * 0.1f).coerceIn(0.2f, 0.8f),
                                        yNorm = (0.35f + ((plan.items.size / 4) % 3) * 0.12f).coerceIn(0.2f, 0.8f),
                                        widthNorm = cat.defaultWidthNorm,
                                        heightNorm = cat.defaultHeightNorm,
                                        colorHex = cat.defaultColorHex
                                    )
                                    plan = plan.copy(items = plan.items + newItem)
                                    selectedItemId = newItem.id
                                    toolMode = BuilderToolMode.PLACE_MOVE
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("layout_add_item_${cat.name.lowercase(Locale.US)}")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(cat.label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // 5. Selected Item Zone Inspector & Size / Label Controls
            selectedItem?.let { item ->
                val (deg, zone, pada) = VastuCompassMath.analyzePlanPosition(
                    xNorm = item.xNorm,
                    yNorm = item.yNorm,
                    chakraRotationDeg = -plan.northTiltDeg
                )
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, brandColors.goldAccent.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("layout_selected_item_inspector")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.label} • Zone: ${zone.fullName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", deg)}° • Pada ${pada.code} ${pada.deity} (${if (pada.isAuspicious) "✓" else "✗"}) • ${zone.attributeTitle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    plan = plan.copy(items = plan.items.filterNot { it.id == item.id })
                                    selectedItemId = null
                                }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showEditItemDialog = item },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("Rename", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    plan = plan.copy(items = plan.items.map {
                                        if (it.id == item.id) it.copy(widthNorm = (it.widthNorm + 0.04f).coerceAtMost(0.6f)) else it
                                    })
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("W+", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    plan = plan.copy(items = plan.items.map {
                                        if (it.id == item.id) it.copy(widthNorm = (it.widthNorm - 0.04f).coerceAtLeast(0.08f)) else it
                                    })
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("W-", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    plan = plan.copy(items = plan.items.map {
                                        if (it.id == item.id) it.copy(heightNorm = (it.heightNorm + 0.04f).coerceAtMost(0.6f)) else it
                                    })
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("H+", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    plan = plan.copy(items = plan.items.map {
                                        if (it.id == item.id) it.copy(heightNorm = (it.heightNorm - 0.04f).coerceAtLeast(0.06f)) else it
                                    })
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("H-", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // 6. Save Scaled Layout + Chakra To Report Button
            PrimaryDarkGreenButton(
                text = if (isSaving) "Saving Scaled Layout..." else "Save Scaled Layout to Report",
                icon = Icons.Filled.Save,
                enabled = !isSaving,
                onClick = {
                    isSaving = true
                    scope.launch {
                        saveScaledLayoutToReport(
                            reportId = reportId,
                            existingMediaId = mediaId,
                            plan = plan,
                            repository = repository
                        )
                        isSaving = false
                        onShowMessage("Scaled Layout & Chakra saved to Report Drawings!")
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("layout_builder_save_button")
            )
        }
    }

    showEditItemDialog?.let { target ->
        var newLabel by remember(target) { mutableStateOf(target.label) }
        AlertDialog(
            onDismissRequest = { showEditItemDialog = null },
            title = { Text("Edit Item Name") },
            text = {
                VedicraTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = "Label",
                    placeholder = "e.g. CNC Machine 2, Labour Quarters"
                )
            },
            confirmButton = {
                Button(onClick = {
                    plan = plan.copy(
                        items = plan.items.map {
                            if (it.id == target.id) it.copy(label = newLabel.ifBlank { target.category.label }) else it
                        }
                    )
                    showEditItemDialog = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditItemDialog = null }) { Text("Cancel") }
            }
        )
    }
}

private fun computePlotPolygon(plan: CustomLayoutPlan, w: Float, h: Float): ScaledPlotPolygon {
    val t = plan.topLength.coerceAtLeast(1f)
    val b = plan.bottomLength.coerceAtLeast(1f)
    val l = plan.leftLength.coerceAtLeast(1f)
    val r = plan.rightLength.coerceAtLeast(1f)
    val maxHorizontal = max(t, b)
    val maxVertical = max(l, r)
    val maxDim = max(maxHorizontal, maxVertical)
    val scale = (min(w, h) * 0.70f) / maxDim
    val cx = w / 2f
    val cy = h / 2f
    val topW = t * scale
    val botW = b * scale
    val leftH = l * scale
    val rightH = r * scale
    return ScaledPlotPolygon(
        tlX = cx - topW / 2f, tlY = cy - leftH / 2f,
        trX = cx + topW / 2f, trY = cy - rightH / 2f,
        brX = cx + botW / 2f, brY = cy + rightH / 2f,
        blX = cx - botW / 2f, blY = cy + leftH / 2f
    )
}

private fun trimFloat(v: Float): String =
    if (abs(v - v.toInt()) < 0.05f) v.toInt().toString() else String.format(Locale.US, "%.1f", v)

private suspend fun saveScaledLayoutToReport(
    reportId: String,
    existingMediaId: String?,
    plan: CustomLayoutPlan,
    repository: VastuRepository
) = withContext(Dispatchers.IO) {
    val sizePx = 1600
    val bmp = createBitmap(sizePx, sizePx)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(AndroidColor.rgb(250, 248, 242))

    val w = sizePx.toFloat()
    val h = sizePx.toFloat()

    // Grid lines
    val minorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(235, 239, 236); strokeWidth = 1.5f }
    val majorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(218, 224, 220); strokeWidth = 2.5f }
    val step = w / 20f
    for (i in 0..20) {
        val p = i * step
        val paint = if (i % 5 == 0) majorPaint else minorPaint
        canvas.drawLine(p, 0f, p, h, paint)
        canvas.drawLine(0f, p, w, p, paint)
    }

    // Scaled plot boundary
    val poly = computePlotPolygon(plan, w, h)
    val polyFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(253, 251, 244); style = Paint.Style.FILL }
    val polyStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(27, 77, 62); style = Paint.Style.STROKE; strokeWidth = 6f }
    val path = AndroidPath().apply {
        moveTo(poly.tlX, poly.tlY)
        lineTo(poly.trX, poly.trY)
        lineTo(poly.brX, poly.brY)
        lineTo(poly.blX, poly.blY)
        close()
    }
    canvas.drawPath(path, polyFill)
    canvas.drawPath(path, polyStroke)

    // Dimensions text
    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(27, 77, 62)
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("TOP (N): ${trimFloat(plan.topLength)} ${plan.unit}", (poly.tlX + poly.trX) / 2f, poly.tlY - 14f, dimPaint)
    canvas.drawText("BOTTOM (S): ${trimFloat(plan.bottomLength)} ${plan.unit}", (poly.blX + poly.brX) / 2f, poly.blY + 34f, dimPaint)
    canvas.drawText("L (W): ${trimFloat(plan.leftLength)} ${plan.unit}", poly.tlX.coerceAtLeast(120f), (poly.tlY + poly.blY) / 2f, dimPaint)
    canvas.drawText("R (E): ${trimFloat(plan.rightLength)} ${plan.unit}", poly.trX.coerceAtMost(w - 120f), (poly.trY + poly.brY) / 2f, dimPaint)

    // Optional 9-Grid
    if (plan.show9Grid) {
        val gPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.argb(145, 181, 137, 46); strokeWidth = 3f }
        val gxMin = min(poly.tlX, poly.blX)
        val gxMax = max(poly.trX, poly.brX)
        val gyMin = min(poly.tlY, poly.trY)
        val gyMax = max(poly.blY, poly.brY)
        val gw = gxMax - gxMin
        val gh = gyMax - gyMin
        for (i in 1..2) {
            canvas.drawLine(gxMin + gw * i / 3f, gyMin, gxMin + gw * i / 3f, gyMax, gPaint)
            canvas.drawLine(gxMin, gyMin + gh * i / 3f, gxMax, gyMin + gh * i / 3f, gPaint)
        }
    }

    // Drawn wall lines
    val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(38, 50, 56)
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }
    plan.wallLines.forEach { wall ->
        canvas.drawLine(wall.x1 * w, wall.y1 * h, wall.x2 * w, wall.y2 * h, wallPaint)
    }

    // Placed items
    val boxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
    val itemTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(17, 24, 21)
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    plan.items.forEach { item ->
        val cx = item.xNorm * w
        val cy = item.yNorm * h
        val bw = item.widthNorm * w
        val bh = item.heightNorm * h
        val col = item.colorHex.toInt()
        boxFill.color = AndroidColor.argb(72, AndroidColor.red(col), AndroidColor.green(col), AndroidColor.blue(col))
        boxStroke.color = col
        val (_, zone, pada) = VastuCompassMath.analyzePlanPosition(item.xNorm, item.yNorm, chakraRotationDeg = -plan.northTiltDeg)
        val rect = RectF(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f)
        canvas.withRotation(item.rotationDeg, cx, cy) {
            drawRect(rect, boxFill)
            drawRect(rect, boxStroke)
            drawText(item.label, cx, cy - 4f, itemTxt)
            drawText("(${zone.code} • ${pada.code})", cx, cy + 22f, itemTxt)
        }
    }

    // Chakra overlay if enabled
    if (plan.showChakraOverlay) {
        val pad = w * 0.03f
        canvas.save()
        canvas.translate(pad, pad)
        WorkspaceRenderer.drawVastuChakraByType(
            canvas = canvas,
            width = w - pad * 2f,
            height = h - pad * 2f,
            rotationDeg = -plan.northTiltDeg,
            baseUnit = (w - pad * 2f) / 40f,
            chakraType = plan.chakraType
        )
        canvas.restore()
    }

    // Save bitmap & persist metadata
    val file = repository.mediaManager.createOriginalImageFile(reportId, "scaled_layout")
    FileOutputStream(file).use { out ->
        bmp.compress(Bitmap.CompressFormat.JPEG, 94, out)
    }
    bmp.recycle()

    val meta = WorkspaceMetadata(
        showGridOverlay = false,
        gridRotationDegrees = -plan.northTiltDeg,
        overlayStyle = GridOverlayStyle.VASTU_CHAKRA_32,
        chakraType = plan.chakraType,
        customLayoutPlan = plan
    )
    val orientLabel = "Tilt ${String.format(Locale.US, "%.1f", plan.northTiltDeg)}° (${VastuCompassMath.zoneForDegree(plan.northTiltDeg).code})"
    val itemSummary = plan.items.joinToString(", ") { item ->
        val (_, zone, pada) = VastuCompassMath.analyzePlanPosition(item.xNorm, item.yNorm, chakraRotationDeg = -plan.northTiltDeg)
        "${item.label} (${zone.code}/${pada.code})"
    }

    val existing = if (!existingMediaId.isNullOrBlank()) repository.getMediaById(existingMediaId) else null
    if (existing != null) {
        repository.mediaManager.deleteAppManagedFile(existing.filePath)
        repository.updateMedia(
            existing.copy(
                filePath = file.absolutePath,
                drawingTitle = plan.title,
                orientationDirection = orientLabel,
                description = "Dims: ${trimFloat(plan.topLength)}×${trimFloat(plan.leftLength)} ${plan.unit} • Items: $itemSummary",
                annotationsJson = JsonCodec.encodeWorkspaceMetadata(meta)
            )
        )
    } else {
        val added = repository.addCapturedCameraPhoto(
            reportId = reportId,
            file = file,
            mediaKind = MediaKind.DRAWING,
            caption = plan.title,
            drawingCategory = "Plot layout drawing",
            drawingTitle = plan.title,
            orientationDirection = orientLabel,
            description = "Dims: ${trimFloat(plan.topLength)}×${trimFloat(plan.leftLength)} ${plan.unit} • Items: $itemSummary"
        )
        repository.updateMedia(added.copy(annotationsJson = JsonCodec.encodeWorkspaceMetadata(meta)))
    }
}
