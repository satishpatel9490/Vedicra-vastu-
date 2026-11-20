package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ExportedPdfEntity
import com.example.data.local.ReportEntity
import com.example.pdf.PdfShareHelper
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.ReportListItemCard
import com.example.ui.theme.LocalVedicraColors
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    title: String,
    reports: List<ReportEntity>,
    emptyTitle: String,
    emptyMessage: String,
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit,
    onDuplicateReport: (String) -> Unit,
    onGeneratePdf: (String) -> Unit,
    onDeleteReport: (String) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<ReportEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (reports.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.FolderOpen,
                    title = emptyTitle,
                    subtitle = emptyMessage,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                ) {
                    items(reports, key = { it.id }) { report ->
                        ReportListItemCard(
                            report = report,
                            onOpen = { onOpenReport(report.id) },
                            onDuplicate = { onDuplicateReport(report.id) },
                            onGeneratePdf = { onGeneratePdf(report.id) },
                            onDelete = { pendingDelete = report }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        ConfirmActionDialog(
            title = "Delete Report?",
            message = "Delete \"${target.clientName.ifBlank { target.reportNumber }}\" and all its associated photos, drawings and annotations?",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                onDeleteReport(target.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchReportsScreen(
    query: String,
    results: List<ReportEntity>,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit,
    onDuplicateReport: (String) -> Unit,
    onGeneratePdf: (String) -> Unit,
    onDeleteReport: (String) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<ReportEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Reports") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search by client, phone, city, property, problem...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("search_reports_input")
            )

            if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmptyStateView(
                        icon = Icons.Filled.Search,
                        title = "No matching reports",
                        subtitle = "Try another client name, mobile number, city, or property type.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                ) {
                    items(results, key = { it.id }) { report ->
                        ReportListItemCard(
                            report = report,
                            onOpen = { onOpenReport(report.id) },
                            onDuplicate = { onDuplicateReport(report.id) },
                            onGeneratePdf = { onGeneratePdf(report.id) },
                            onDelete = { pendingDelete = report }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        ConfirmActionDialog(
            title = "Delete Report?",
            message = "Permanently delete \"${target.clientName.ifBlank { target.reportNumber }}\" and its files?",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                onDeleteReport(target.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportedPdfsScreen(
    pdfs: List<ExportedPdfEntity>,
    onBack: () -> Unit,
    onEditReport: (String) -> Unit,
    onRegeneratePdf: (String) -> Unit,
    onDeletePdf: (ExportedPdfEntity) -> Unit
) {
    val context = LocalContext.current
    val brandColors = LocalVedicraColors.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var pendingSaveFile by remember { mutableStateOf<File?>(null) }
    var pendingDelete by remember { mutableStateOf<ExportedPdfEntity?>(null) }

    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        val src = pendingSaveFile
        if (uri != null && src != null && src.exists()) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(src).use { input -> input.copyTo(out) }
                }
            }
        }
        pendingSaveFile = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exported PDF Reports") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (pdfs.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.PictureAsPdf,
                    title = "No Exported PDFs Yet",
                    subtitle = "Open any report and tap Generate PDF to create a shareable A4 consultancy report.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                ) {
                    items(pdfs, key = { it.id }) { pdf ->
                        val file = File(pdf.filePath)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, brandColors.cardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = pdf.fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Client: ${pdf.clientName}  •  ${pdf.pageCount} page(s)  •  ${dateFormat.format(Date(pdf.createdAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PrimaryDarkGreenButton(
                                        text = "WhatsApp",
                                        onClick = { PdfShareHelper.sharePdfViaWhatsApp(context, file, pdf.clientName) },
                                        icon = Icons.Filled.Share,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { PdfShareHelper.viewPdf(context, file) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("View")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { PdfShareHelper.sharePdf(context, file, pdf.clientName) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            pendingSaveFile = file
                                            saveAsLauncher.launch(pdf.fileName)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Save", style = MaterialTheme.typography.labelMedium)
                                    }
                                    IconButton(
                                        onClick = { onRegeneratePdf(pdf.reportId) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "Regenerate PDF")
                                    }
                                    IconButton(
                                        onClick = { onEditReport(pdf.reportId) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit Report")
                                    }
                                    IconButton(
                                        onClick = { pendingDelete = pdf },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete PDF",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        ConfirmActionDialog(
            title = "Delete Exported PDF?",
            message = "Remove \"${target.fileName}\" from exported PDFs? Your saved client report will remain intact.",
            confirmLabel = "Delete PDF",
            isDestructive = true,
            onConfirm = {
                onDeletePdf(target)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
