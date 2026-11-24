package com.example.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.FullReportBundle
import com.example.data.local.SiteMediaEntity
import com.example.model.MediaKind
import com.example.model.VastuConstants
import com.example.pdf.PdfShareHelper
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VedicraDropdownField
import com.example.ui.components.VedicraSectionCard
import com.example.ui.components.VedicraTextField
import com.example.ui.theme.LocalVedicraColors
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SitePhotographySection(
    photos: List<SiteMediaEntity>,
    onCaptureFromCamera: () -> Unit,
    onImportFromGallery: (List<Uri>) -> Unit,
    onOpenViewer: (String) -> Unit,
    onOpenWorkspace: (String) -> Unit,
    onRotatePhoto: (SiteMediaEntity) -> Unit,
    onUpdateMedia: (SiteMediaEntity) -> Unit,
    onMarkMainLayout: (SiteMediaEntity) -> Unit,
    onMoveOrder: (SiteMediaEntity, Boolean) -> Unit,
    onDeleteMedia: (SiteMediaEntity) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) onImportFromGallery(uris)
    }

    var editingCaptionMedia by remember { mutableStateOf<SiteMediaEntity?>(null) }
    var pendingDeleteMedia by remember { mutableStateOf<SiteMediaEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VedicraSectionCard(
            title = "SITE PHOTOGRAPHY & DRAWING CAPTURE",
            subtitle = "Capture site photos directly inside Vedicra or select from gallery",
            icon = Icons.Filled.CameraAlt
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryDarkGreenButton(
                    text = "Capture From Camera",
                    icon = Icons.Filled.CameraAlt,
                    onClick = onCaptureFromCamera,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("capture_camera_button")
                )
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("select_gallery_button")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Select From Gallery")
                }
            }
        }

        if (photos.isEmpty()) {
            EmptyMediaHintCard(
                title = "No Site Photographs Yet",
                subtitle = "Tap 'Capture From Camera' to take site photographs continuously without leaving the app."
            )
        } else {
            photos.forEachIndexed { idx, photo ->
                MediaItemCard(
                    media = photo,
                    isFirst = idx == 0,
                    isLast = idx == photos.lastIndex,
                    onOpenViewer = { onOpenViewer(photo.id) },
                    onOpenWorkspace = { onOpenWorkspace(photo.id) },
                    onRotate = { onRotatePhoto(photo) },
                    onEditDetails = { editingCaptionMedia = photo },
                    onTogglePdf = { onUpdateMedia(photo.copy(includeInPdf = !photo.includeInPdf)) },
                    onMarkMainLayout = { onMarkMainLayout(photo) },
                    onMoveUp = { onMoveOrder(photo, true) },
                    onMoveDown = { onMoveOrder(photo, false) },
                    onDelete = { pendingDeleteMedia = photo }
                )
            }
        }
    }

    editingCaptionMedia?.let { target ->
        EditPhotoDetailsDialog(
            media = target,
            onDismiss = { editingCaptionMedia = null },
            onSave = { updated ->
                onUpdateMedia(updated)
                editingCaptionMedia = null
            }
        )
    }

    pendingDeleteMedia?.let { target ->
        ConfirmActionDialog(
            title = "Delete Photograph?",
            message = "Remove this photograph from the report? Unrelated phone gallery photos are never deleted.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                onDeleteMedia(target)
                pendingDeleteMedia = null
            },
            onDismiss = { pendingDeleteMedia = null }
        )
    }
}

@Composable
fun SiteDrawingsAndWorkspaceSection(
    drawings: List<SiteMediaEntity>,
    onCaptureDrawing: (
        category: String,
        title: String,
        floor: String,
        orientation: String,
        description: String,
        notes: String
    ) -> Unit,
    onImportDrawing: (
        uri: Uri,
        category: String,
        title: String,
        floor: String,
        orientation: String,
        description: String,
        notes: String
    ) -> Unit,
    onOpenViewer: (String) -> Unit,
    onOpenWorkspace: (String) -> Unit,
    onRotateDrawing: (SiteMediaEntity) -> Unit,
    onUpdateDrawing: (SiteMediaEntity) -> Unit,
    onMarkMainLayout: (SiteMediaEntity) -> Unit,
    onMoveOrder: (SiteMediaEntity, Boolean) -> Unit,
    onDeleteDrawing: (SiteMediaEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDrawing by remember { mutableStateOf<SiteMediaEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<SiteMediaEntity?>(null) }

    var pendingCategory by remember { mutableStateOf(VastuConstants.drawingCategories.first()) }
    var pendingTitle by remember { mutableStateOf("") }
    var pendingFloor by remember { mutableStateOf("") }
    var pendingOrientation by remember { mutableStateOf("North") }
    var pendingDescription by remember { mutableStateOf("") }
    var pendingNotes by remember { mutableStateOf("") }

    val drawingPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImportDrawing(
                uri,
                pendingCategory,
                pendingTitle.ifBlank { pendingCategory },
                pendingFloor,
                pendingOrientation,
                pendingDescription,
                pendingNotes
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VedicraSectionCard(
            title = "SITE DRAWINGS & LAYOUTS",
            subtitle = "Organize plot plans, floor layouts & open the Site Grid & Layout Workspace",
            icon = Icons.Filled.Draw,
            headerTrailing = {
                PrimaryDarkGreenButton(
                    text = "Add Drawing",
                    icon = Icons.Filled.Add,
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_drawing_button")
                )
            }
        ) {
            Text(
                text = "Tap 'Open Grid & Layout Workspace' on any floor plan to add N/NE/E/SE/S/SW/W/NW markers, North arrow, degree/room/zone labels and notes on a copy while keeping the original image untouched.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (drawings.isEmpty()) {
            EmptyMediaHintCard(
                title = "No Site Drawings Added",
                subtitle = "Use 'Add Drawing' to capture or import floor plans, plot layouts, machinery layouts, or electrical plans."
            )
        } else {
            drawings.forEachIndexed { idx, drawing ->
                MediaItemCard(
                    media = drawing,
                    isFirst = idx == 0,
                    isLast = idx == drawings.lastIndex,
                    onOpenViewer = { onOpenViewer(drawing.id) },
                    onOpenWorkspace = { onOpenWorkspace(drawing.id) },
                    onRotate = { onRotateDrawing(drawing) },
                    onEditDetails = { editingDrawing = drawing },
                    onTogglePdf = { onUpdateDrawing(drawing.copy(includeInPdf = !drawing.includeInPdf)) },
                    onMarkMainLayout = { onMarkMainLayout(drawing) },
                    onMoveUp = { onMoveOrder(drawing, true) },
                    onMoveDown = { onMoveOrder(drawing, false) },
                    onDelete = { pendingDelete = drawing }
                )
            }
        }
    }

    if (showAddDialog) {
        AddDrawingDialog(
            onDismiss = { showAddDialog = false },
            onCaptureViaCamera = { cat, title, floor, orient, desc, notes ->
                showAddDialog = false
                onCaptureDrawing(cat, title, floor, orient, desc, notes)
            },
            onSelectFromGallery = { cat, title, floor, orient, desc, notes ->
                pendingCategory = cat
                pendingTitle = title
                pendingFloor = floor
                pendingOrientation = orient
                pendingDescription = desc
                pendingNotes = notes
                showAddDialog = false
                drawingPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }

    editingDrawing?.let { target ->
        EditDrawingDetailsDialog(
            media = target,
            onDismiss = { editingDrawing = null },
            onSave = { updated ->
                onUpdateDrawing(updated)
                editingDrawing = null
            }
        )
    }

    pendingDelete?.let { target ->
        ConfirmActionDialog(
            title = "Delete Drawing?",
            message = "Remove \"${target.drawingTitle.ifBlank { target.drawingCategory }}\" and its annotated layout version?",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                onDeleteDrawing(target)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
fun PdfGenerateAndShareSection(
    bundle: FullReportBundle,
    isGenerating: Boolean,
    onGeneratePdf: () -> Unit
) {
    val context = LocalContext.current
    val brandColors = LocalVedicraColors.current
    val report = bundle.report
    val lastPdfFile = report.lastPdfPath?.let { File(it) }?.takeIf { it.exists() }

    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && lastPdfFile != null && lastPdfFile.exists()) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(lastPdfFile).use { input -> input.copyTo(out) }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VedicraSectionCard(
            title = "Generate & Share A4 PDF Report",
            subtitle = "Includes all 17 Vastu consultancy sections, repeating analysis tables, site photos & annotated plans",
            icon = Icons.Filled.PictureAsPdf
        ) {
            val includedMediaCount = bundle.mediaItems.count { it.includeInPdf }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Report Summary Ready for PDF",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Client: ${report.clientName.ifBlank { "Not entered" }} • Property: ${report.propertyType}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Analysis Rows: ${bundle.analysisRows.size} • Media Included in PDF: $includedMediaCount",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            PrimaryDarkGreenButton(
                text = if (isGenerating) "Generating A4 PDF..." else if (lastPdfFile != null) "Regenerate PDF Report" else "Generate A4 PDF Report",
                icon = Icons.Filled.PictureAsPdf,
                enabled = !isGenerating,
                onClick = onGeneratePdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_pdf_button")
            )

            if (isGenerating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Rendering tables, photos and layouts...")
                }
            }
        }

        if (lastPdfFile != null) {
            VedicraSectionCard(
                title = "Ready to Share",
                subtitle = lastPdfFile.name
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryDarkGreenButton(
                        text = "Share via WhatsApp",
                        icon = Icons.Filled.Share,
                        onClick = { PdfShareHelper.sharePdfViaWhatsApp(context, lastPdfFile, report.clientName) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("whatsapp_share_pdf_button")
                    )
                    OutlinedButton(
                        onClick = { PdfShareHelper.viewPdf(context, lastPdfFile) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("View PDF")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { PdfShareHelper.sharePdf(context, lastPdfFile, report.clientName) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Android Sharesheet")
                    }
                    OutlinedButton(
                        onClick = { saveAsLauncher.launch(lastPdfFile.name) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Save PDF Copy")
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaItemCard(
    media: SiteMediaEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onOpenViewer: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onRotate: () -> Unit,
    onEditDetails: () -> Unit,
    onTogglePdf: () -> Unit,
    onMarkMainLayout: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val brandColors = LocalVedicraColors.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val annotatedPath = media.annotatedFilePath
    val hasAnnotated = !annotatedPath.isNullOrBlank() && File(annotatedPath).exists()
    val displayPath = if (hasAnnotated) annotatedPath else media.filePath
    val visualRotation = if (hasAnnotated) 0f else media.rotationDegrees.toFloat()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (media.isMainSiteLayout) 1.5.dp else 1.dp,
            color = if (media.isMainSiteLayout) brandColors.goldAccent else brandColors.cardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = Modifier.size(104.dp)) {
                    AsyncImage(
                        model = File(displayPath),
                        contentDescription = media.caption.ifBlank { "Site image" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .rotate(visualRotation)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (media.isMainSiteLayout) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = brandColors.goldContainer
                            ) {
                                Text(
                                    text = "Main Layout",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = brandColors.onGoldContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (hasAnnotated) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Annotated",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = media.drawingTitle.ifBlank { media.caption.ifBlank { if (media.mediaKind == MediaKind.DRAWING.name) media.drawingCategory.ifBlank { "Site Drawing" } else "Site Photograph" } },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (media.mediaKind == MediaKind.DRAWING.name) {
                        val meta = listOfNotNull(
                            media.drawingCategory.takeIf { it.isNotBlank() },
                            media.floorNumber.takeIf { it.isNotBlank() }?.let { "Floor: $it" },
                            media.orientationDirection.takeIf { it.isNotBlank() }?.let { "Dir: $it" }
                        ).joinToString(" • ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (media.orientationDirection.isNotBlank()) {
                        Text(
                            text = "Heading: ${media.orientationDirection}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (media.description.isNotBlank()) {
                        Text(
                            text = media.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(media.capturedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = media.includeInPdf,
                        onCheckedChange = { onTogglePdf() }
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Include in PDF", style = MaterialTheme.typography.labelMedium)
                }

                Row {
                    IconButton(onClick = onMarkMainLayout, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(
                            imageVector = if (media.isMainSiteLayout) Icons.Filled.Star else Icons.Filled.StarOutline,
                            contentDescription = "Mark as Main Site Layout",
                            tint = if (media.isMainSiteLayout) brandColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onRotate, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90 Degrees")
                    }
                    IconButton(onClick = onEditDetails, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Caption & Info")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenWorkspace,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Grid & Annotate", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onOpenViewer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Filled.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Full Screen", style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move Up")
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move Down")
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete Photo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMediaHintCard(title: String, subtitle: String) {
    val brandColors = LocalVedicraColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, brandColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditPhotoDetailsDialog(
    media: SiteMediaEntity,
    onDismiss: () -> Unit,
    onSave: (SiteMediaEntity) -> Unit
) {
    var caption by remember(media) { mutableStateOf(media.caption) }
    var description by remember(media) { mutableStateOf(media.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Photo Caption & Description") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VedicraTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = "Caption",
                    placeholder = "e.g. Main Gate Facing East"
                )
                VedicraTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    singleLine = false,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(media.copy(caption = caption, description = description)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddDrawingDialog(
    onDismiss: () -> Unit,
    onCaptureViaCamera: (String, String, String, String, String, String) -> Unit,
    onSelectFromGallery: (String, String, String, String, String, String) -> Unit
) {
    var category by remember { mutableStateOf(VastuConstants.drawingCategories.first()) }
    var title by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var orientation by remember { mutableStateOf("North") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Site Drawing / Floor Plan") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(410.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VedicraDropdownField(
                    label = "Drawing Type",
                    selectedValue = category,
                    options = VastuConstants.drawingCategories,
                    onOptionSelected = { category = it }
                )
                VedicraTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Drawing Title",
                    placeholder = category
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VedicraTextField(
                        value = floor,
                        onValueChange = { floor = it },
                        label = "Floor Number",
                        placeholder = "e.g. Ground",
                        modifier = Modifier.weight(1f)
                    )
                    VedicraDropdownField(
                        label = "Orientation",
                        selectedValue = orientation,
                        options = VastuConstants.entranceDirections,
                        onOptionSelected = { orientation = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                VedicraTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    singleLine = false,
                    minLines = 2
                )
                VedicraTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Additional Notes",
                    singleLine = false,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCaptureViaCamera(
                        category,
                        title.ifBlank { category },
                        floor,
                        orientation,
                        description,
                        notes
                    )
                }
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Camera")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(
                    onClick = {
                        onSelectFromGallery(
                            category,
                            title.ifBlank { category },
                            floor,
                            orientation,
                            description,
                            notes
                        )
                    }
                ) {
                    Text("Gallery")
                }
            }
        }
    )
}

@Composable
private fun EditDrawingDetailsDialog(
    media: SiteMediaEntity,
    onDismiss: () -> Unit,
    onSave: (SiteMediaEntity) -> Unit
) {
    var category by remember(media) { mutableStateOf(media.drawingCategory.ifBlank { VastuConstants.drawingCategories.first() }) }
    var title by remember(media) { mutableStateOf(media.drawingTitle) }
    var floor by remember(media) { mutableStateOf(media.floorNumber) }
    var orientation by remember(media) { mutableStateOf(media.orientationDirection.ifBlank { "North" }) }
    var description by remember(media) { mutableStateOf(media.description) }
    var notes by remember(media) { mutableStateOf(media.additionalNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Drawing Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VedicraDropdownField(
                    label = "Drawing Type",
                    selectedValue = category,
                    options = VastuConstants.drawingCategories,
                    onOptionSelected = { category = it }
                )
                VedicraTextField(value = title, onValueChange = { title = it }, label = "Drawing Title")
                VedicraTextField(value = floor, onValueChange = { floor = it }, label = "Floor Number")
                VedicraDropdownField(
                    label = "Direction / Orientation",
                    selectedValue = orientation,
                    options = VastuConstants.entranceDirections,
                    onOptionSelected = { orientation = it }
                )
                VedicraTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    singleLine = false,
                    minLines = 2
                )
                VedicraTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Additional Notes",
                    singleLine = false,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        media.copy(
                            drawingCategory = category,
                            drawingTitle = title,
                            floorNumber = floor,
                            orientationDirection = orientation,
                            description = description,
                            additionalNotes = notes
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
