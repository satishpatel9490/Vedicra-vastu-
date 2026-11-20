package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.FontSizeOption
import com.example.model.PdfThemeOption
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VedicraSectionCard
import com.example.ui.components.VedicraTextField
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSaveSettings: (AppSettings) -> Unit,
    onImportLogo: (Uri) -> Unit,
    onResetLogo: () -> Unit,
    onExportBackupJson: (Uri) -> Unit,
    onExportFullZip: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit
) {
    var companyName by remember(settings.companyName) { mutableStateOf(settings.companyName) }
    var consultantName by remember(settings.consultantName) { mutableStateOf(settings.consultantName) }
    var contactNumber by remember(settings.contactNumber) { mutableStateOf(settings.contactNumber) }
    var selectedPdfTheme by remember(settings.pdfTheme) { mutableStateOf(settings.pdfTheme) }
    var selectedFontSize by remember(settings.defaultFontSize) { mutableStateOf(settings.defaultFontSize) }
    var selectedAppTheme by remember(settings.appTheme) { mutableStateOf(settings.appTheme) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImportLogo(uri)
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackupJson(uri)
    }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) onExportFullZip(uri)
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRestoreBackup(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 780.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VedicraSectionCard(
                    title = "Company & Consultant Profile",
                    subtitle = "Appears in the app header and final PDF footer",
                    icon = Icons.Filled.Business
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val customLogo = settings.companyLogoPath.takeIf { it.isNotBlank() }?.let { File(it) }
                        val settingsLogoMod = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), CircleShape)
                        if (customLogo != null && customLogo.exists()) {
                            AsyncImage(
                                model = customLogo,
                                contentDescription = "Company Logo",
                                contentScale = ContentScale.Crop,
                                modifier = settingsLogoMod
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_vastu_brand_logo),
                                contentDescription = "Default Vedicra Logo",
                                contentScale = ContentScale.Crop,
                                modifier = settingsLogoMod
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    logoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Change Logo") }
                            if (settings.companyLogoPath.isNotBlank()) {
                                OutlinedButton(
                                    onClick = onResetLogo,
                                    shape = RoundedCornerShape(10.dp)
                                ) { Text("Reset") }
                            }
                        }
                    }

                    VedicraTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = "Company Name"
                    )
                    VedicraTextField(
                        value = consultantName,
                        onValueChange = { consultantName = it },
                        label = "Consultant Name"
                    )
                    VedicraTextField(
                        value = contactNumber,
                        onValueChange = { contactNumber = it },
                        label = "Contact Number",
                        keyboardType = KeyboardType.Phone
                    )
                }

                VedicraSectionCard(
                    title = "Application & PDF Appearance",
                    icon = Icons.Filled.Palette
                ) {
                    Text("Application Theme", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedAppTheme == mode,
                                onClick = {
                                    selectedAppTheme = mode
                                    onSaveSettings(
                                        settings.copy(
                                            companyName = companyName,
                                            consultantName = consultantName,
                                            contactNumber = contactNumber,
                                            pdfTheme = selectedPdfTheme,
                                            defaultFontSize = selectedFontSize,
                                            appTheme = mode
                                        )
                                    )
                                },
                                label = { Text(mode.displayName) }
                            )
                        }
                    }

                    Text("PDF Report Theme", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PdfThemeOption.entries.forEach { pdfTheme ->
                            FilterChip(
                                selected = selectedPdfTheme == pdfTheme,
                                onClick = { selectedPdfTheme = pdfTheme },
                                label = { Text(pdfTheme.displayName) }
                            )
                        }
                    }

                    Text("Default PDF Font Size", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FontSizeOption.entries.forEach { size ->
                            FilterChip(
                                selected = selectedFontSize == size,
                                onClick = { selectedFontSize = size },
                                label = { Text(size.displayName) }
                            )
                        }
                    }

                    PrimaryDarkGreenButton(
                        text = "Save Settings",
                        icon = Icons.Filled.Save,
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    companyName = companyName,
                                    consultantName = consultantName,
                                    contactNumber = contactNumber,
                                    pdfTheme = selectedPdfTheme,
                                    defaultFontSize = selectedFontSize,
                                    appTheme = selectedAppTheme
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_settings_button")
                    )
                }

                VedicraSectionCard(
                    title = "Backup & Restore",
                    subtitle = "Export complete reports with photos, drawings & annotations, or JSON report data",
                    icon = Icons.Filled.Backup
                ) {
                    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    PrimaryDarkGreenButton(
                        text = "Export Complete Backup (.zip)",
                        icon = Icons.Filled.Backup,
                        onClick = { exportZipLauncher.launch("Vedicra_Vastu_Backup_$stamp.zip") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportJsonLauncher.launch("Vedicra_Vastu_Reports_$stamp.json") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                restoreLauncher.launch(arrayOf("application/zip", "application/json", "*/*"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }

                VedicraSectionCard(
                    title = "About Application",
                    icon = Icons.Filled.Info
                ) {
                    Text(
                        text = "VEDICRA VASTU SOLUTIONS — Professional Vastu Inspection & Consultancy",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Designed for offline field site visits, integrated CameraX site photography, floor plan organization, directional grid annotation, and multi-page A4 PDF consultancy reporting.\n\nPrivacy: All client data, site photographs, drawings, and reports are stored privately and locally on your device unless you explicitly share a PDF or export a backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
