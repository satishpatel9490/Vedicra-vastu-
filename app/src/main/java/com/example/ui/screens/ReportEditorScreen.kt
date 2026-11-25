package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppThemeMode
import com.example.model.MediaKind
import com.example.pdf.PdfShareHelper
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.ThemeSelectorMenu
import com.example.ui.screens.editor.ClientDetailsSection
import com.example.ui.screens.editor.ConsultantNotesSection
import com.example.ui.screens.editor.DegreesAndCompassSection
import com.example.ui.screens.editor.EntranceDetailsSection
import com.example.ui.screens.editor.PdfGenerateAndShareSection
import com.example.ui.screens.editor.PropertyDetailsSection
import com.example.ui.screens.editor.SiteDrawingsAndWorkspaceSection
import com.example.ui.screens.editor.SitePhotographySection
import com.example.ui.screens.editor.SwarAndLandEnergySection
import com.example.ui.screens.editor.UnifiedProblemsAnalysisRemediesSection
import com.example.ui.viewmodel.EditorEvent
import com.example.ui.viewmodel.ReportEditorViewModel
import java.io.File

private val editorTabTitles = listOf(
    "Client",
    "Property",
    "Degrees & Compass",
    "Entrance Details",
    "Swar & Energy",
    "Problems & Remedies",
    "Notes",
    "Site Photos",
    "Drawings & Grid",
    "PDF & Share"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportEditorScreen(
    viewModel: ReportEditorViewModel,
    initialTab: Int = 0,
    onSelectTheme: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
    onLaunchCamera: (
        mediaKind: String,
        drawingCategory: String,
        drawingTitle: String,
        floorNumber: String,
        orientationDirection: String,
        description: String,
        additionalNotes: String
    ) -> Unit,
    onOpenPhotoViewer: (String) -> Unit,
    onOpenLayoutWorkspace: (String) -> Unit,
    onOpenMapAlign: (String) -> Unit = {},
    onOpenLayoutBuilder: (String?) -> Unit = {},
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val bundle by viewModel.bundle.collectAsStateWithLifecycle()
    val draftReport by viewModel.draftReport.collectAsStateWithLifecycle()
    val draftRows by viewModel.draftRows.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()
    val lastSaved by viewModel.lastSavedTimestamp.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, editorTabTitles.lastIndex)) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Message -> onShowMessage(event.text)
                is EditorEvent.PdfReady -> {
                    onShowMessage("PDF Ready: ${event.pdf.fileName}")
                    selectedTab = editorTabTitles.lastIndex
                }
                EditorEvent.ReportDeleted -> {
                    onShowMessage("Report deleted")
                    onBack()
                }
            }
        }
    }

    val report = draftReport ?: bundle?.report
    val analysisRows = if (draftRows.isNotEmpty()) draftRows else (bundle?.orderedAnalysisRows ?: emptyList())
    val allMedia = bundle?.orderedMediaItems ?: emptyList()
    val sitePhotos = remember(allMedia) { allMedia.filter { it.mediaKind == MediaKind.PHOTO.name } }
    val siteDrawings = remember(allMedia) { allMedia.filter { it.mediaKind == MediaKind.DRAWING.name } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = report?.clientName?.ifBlank { "New Vastu Inspection" } ?: "Loading...",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append(report?.reportNumber ?: "")
                                if (lastSaved != null) append(" • Auto-saved")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Dashboard")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveReportExplicitly() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("editor_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("editor_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = "Reset Form",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (report != null && !report.isDraft) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("editor_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Report",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    ThemeSelectorMenu(
                        currentTheme = settings.appTheme,
                        onSelectTheme = onSelectTheme
                    )
                }
            )
        },
        bottomBar = {
            if (report != null) {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (selectedTab > 0) selectedTab -= 1 },
                            enabled = selectedTab > 0,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Section", modifier = Modifier.size(18.dp))
                        }

                        PrimaryDarkGreenButton(
                            text = if (isGeneratingPdf) "Generating..." else "Generate PDF",
                            icon = Icons.Filled.PictureAsPdf,
                            enabled = !isGeneratingPdf,
                            onClick = { viewModel.generatePdf() },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = { if (selectedTab < editorTabTitles.lastIndex) selectedTab += 1 },
                            enabled = selectedTab < editorTabTitles.lastIndex,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Section", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    editorTabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier.testTag("editor_tab_$index")
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .widthIn(max = 820.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> ClientDetailsSection(
                            report = report,
                            onUpdate = viewModel::updateReportField
                        )
                        1 -> PropertyDetailsSection(
                            report = report,
                            onUpdate = viewModel::updateReportField,
                            onAddEntrance = viewModel::addAdditionalEntrance,
                            onUpdateEntrance = viewModel::updateAdditionalEntrance,
                            onDeleteEntrance = viewModel::deleteAdditionalEntrance,
                            onOpenCompassTab = { selectedTab = 2 }
                        )
                        2 -> DegreesAndCompassSection(
                            report = report,
                            onUpdate = viewModel::updateReportField,
                            onAddEntrance = viewModel::addAdditionalEntrance,
                            onShowMessage = onShowMessage,
                            onOpenMapAlign = { chakra -> onOpenMapAlign(chakra.name) }
                        )
                        3 -> EntranceDetailsSection(
                            report = report,
                            onUpdate = viewModel::updateReportField,
                            onAddEntrance = viewModel::addAdditionalEntrance,
                            onDeleteEntrance = viewModel::deleteAdditionalEntrance,
                            onShowMessage = onShowMessage
                        )
                        4 -> SwarAndLandEnergySection(
                            report = report,
                            onUpdate = viewModel::updateReportField,
                            onToggleGuna = viewModel::toggleGuna
                        )
                        5 -> UnifiedProblemsAnalysisRemediesSection(
                            report = report,
                            rows = analysisRows,
                            mediaItems = allMedia,
                            onUpdateReport = viewModel::updateReportField,
                            onTogglePredefinedProblem = viewModel::togglePredefinedProblem,
                            onAddCustomProblem = viewModel::addCustomProblem,
                            onAddRow = viewModel::addAnalysisRow,
                            onUpdateRow = viewModel::updateAnalysisRow,
                            onDuplicateRow = viewModel::duplicateAnalysisRow,
                            onDeleteRow = viewModel::deleteAnalysisRow,
                            onToggleRowAttachment = viewModel::toggleRowMediaAttachment
                        )
                        6 -> ConsultantNotesSection(
                            report = report,
                            onUpdateNote = viewModel::updateConsultantNote,
                            onAddMoreNote = viewModel::addMoreConsultantNote,
                            onDeleteNote = viewModel::deleteConsultantNote
                        )
                        7 -> SitePhotographySection(
                            photos = sitePhotos,
                            onCaptureFromCamera = {
                                onLaunchCamera(MediaKind.PHOTO.name, "", "", "", "", "", "")
                            },
                            onImportFromGallery = viewModel::importPhotosFromGallery,
                            onOpenViewer = onOpenPhotoViewer,
                            onOpenWorkspace = onOpenLayoutWorkspace,
                            onRotatePhoto = viewModel::rotateMedia,
                            onUpdateMedia = viewModel::updateMediaDetails,
                            onMarkMainLayout = viewModel::setMainSiteLayout,
                            onMoveOrder = viewModel::moveMediaOrder,
                            onDeleteMedia = viewModel::deleteMedia
                        )
                        8 -> SiteDrawingsAndWorkspaceSection(
                            drawings = siteDrawings,
                            onCaptureDrawing = { cat, title, floor, orient, desc, notes ->
                                onLaunchCamera(
                                    MediaKind.DRAWING.name,
                                    cat,
                                    title,
                                    floor,
                                    orient,
                                    desc,
                                    notes
                                )
                            },
                            onImportDrawing = { uri: Uri, cat, title, floor, orient, desc, notes ->
                                viewModel.importDrawingFromGallery(
                                    uri = uri,
                                    drawingCategory = cat,
                                    drawingTitle = title,
                                    floorNumber = floor,
                                    orientationDirection = orient,
                                    description = desc,
                                    additionalNotes = notes
                                )
                            },
                            onOpenViewer = onOpenPhotoViewer,
                            onOpenWorkspace = onOpenLayoutWorkspace,
                            onRotateDrawing = viewModel::rotateMedia,
                            onUpdateDrawing = viewModel::updateMediaDetails,
                            onMarkMainLayout = viewModel::setMainSiteLayout,
                            onMoveOrder = viewModel::moveMediaOrder,
                            onDeleteDrawing = viewModel::deleteMedia,
                            onOpenLayoutBuilder = onOpenLayoutBuilder
                        )
                        9 -> {
                            val currentBundle = bundle
                            if (currentBundle != null) {
                                PdfGenerateAndShareSection(
                                    bundle = currentBundle.copy(report = report),
                                    isGenerating = isGeneratingPdf,
                                    onGeneratePdf = viewModel::generatePdf
                                )
                            }
                        }
                    }

                    // Quick PDF / WhatsApp reminder at bottom if generated
                    val lastPdf = report.lastPdfPath?.let { File(it) }?.takeIf { it.exists() }
                    if (selectedTab != editorTabTitles.lastIndex && lastPdf != null) {
                        Spacer(Modifier.size(4.dp))
                        OutlinedButton(
                            onClick = { PdfShareHelper.sharePdfViaWhatsApp(context, lastPdf, report.clientName) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Share Latest PDF on WhatsApp")
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        ConfirmActionDialog(
            title = "Reset Current Report Form?",
            message = "This clears all fields and photos of this active report only. Other saved reports remain safe.",
            confirmLabel = "Reset Form",
            isDestructive = true,
            onConfirm = {
                showResetConfirm = false
                viewModel.resetCurrentReportForm()
            },
            onDismiss = { showResetConfirm = false }
        )
    }

    if (showDeleteConfirm) {
        ConfirmActionDialog(
            title = "Delete Saved Report?",
            message = "Permanently delete this report and all its app-managed photos, drawings and annotations?",
            confirmLabel = "Delete Report",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteCurrentReport()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
