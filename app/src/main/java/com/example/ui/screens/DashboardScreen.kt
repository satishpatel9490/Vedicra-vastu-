package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ReportEntity
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.ThemeSelectorMenu
import com.example.ui.screens.editor.ShaktiChakraCompassDialog
import com.example.ui.theme.LocalVedicraColors
import java.io.File

private data class DashboardOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String,
    val isPrimary: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    settings: AppSettings,
    activeReport: ReportEntity?,
    onSelectTheme: (AppThemeMode) -> Unit,
    onCreateNewReport: () -> Unit,
    onResumeActiveReport: (String) -> Unit,
    onSaveActiveReport: () -> Unit,
    onResetActiveReport: () -> Unit,
    onDeleteActiveReport: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val brandColors = LocalVedicraColors.current
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showShaktiChakraDialog by remember { mutableStateOf(false) }

    val options = remember(
        onCreateNewReport,
        onOpenReports,
        onOpenSettings
    ) {
        listOf(
            DashboardOption(
                title = "Create New Report",
                subtitle = "Start a site inspection, capture photos & floor plans",
                icon = Icons.Filled.AddCircle,
                testTag = "dash_create_new_report",
                isPrimary = true,
                onClick = onCreateNewReport
            ),
            DashboardOption(
                title = "Reports",
                subtitle = "Search, open, edit, recent & export A4 PDF reports",
                icon = Icons.Filled.FolderCopy,
                testTag = "dash_reports",
                onClick = onOpenReports
            ),
            DashboardOption(
                title = "Settings",
                subtitle = "Branding, PDF style, themes, JSON & ZIP backup",
                icon = Icons.Filled.Settings,
                testTag = "dash_settings",
                onClick = onOpenSettings
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = settings.companyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Professional Vastu Inspection & Consultancy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Contextual top-right actions when a report is open
                    if (activeReport != null) {
                        IconButton(
                            onClick = onSaveActiveReport,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dash_save_active_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Save Open Report",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dash_reset_active_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = "Reset Current Form",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (!activeReport.isDraft) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dash_delete_active_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete Saved Report",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    ThemeSelectorMenu(
                        currentTheme = settings.appTheme,
                        onSelectTheme = onSelectTheme
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val columns = if (maxWidth >= 700.dp) 3 else 2

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .align(Alignment.TopCenter)
                    .fillMaxSize()
                    .testTag("dashboard_grid")
            ) {
                // Hero Consultancy Identity Banner (NO client/report count statistics!)
                item(span = { GridItemSpan(columns) }) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, brandColors.goldAccent.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            brandColors.headerGradientStart,
                                            brandColors.headerGradientEnd
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val logoFile = settings.companyLogoPath.takeIf { it.isNotBlank() }?.let { File(it) }
                                val logoModifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color(0xFFE6C574).copy(alpha = 0.8f), CircleShape)
                                if (logoFile != null && logoFile.exists()) {
                                    AsyncImage(
                                        model = logoFile,
                                        contentDescription = "Company Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = logoModifier
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_vastu_brand_logo),
                                        contentDescription = "Vedicra Vastu Solutions Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = logoModifier
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = settings.companyName,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color(0xFFFAF6EE)
                                    )
                                    Text(
                                        text = "Professional Vastu Inspection & Consultancy",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFE7DFC9)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.14f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Explore,
                                                    contentDescription = null,
                                                    tint = Color(0xFFE6C574),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Consultant: ${settings.consultantName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFFAF6EE),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Surface(
                                            onClick = { showShaktiChakraDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE6C574),
                                            modifier = Modifier.testTag("dashboard_shakti_chakra_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Explore,
                                                    contentDescription = "Open Shakti Chakra Compass",
                                                    tint = Color(0xFF1A2520),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Shakti Chakra",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1A2520)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Open Report Bar (if user navigated back to dashboard with an active report)
                if (activeReport != null) {
                    item(span = { GridItemSpan(columns) }) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = brandColors.goldContainer
                            ),
                            border = BorderStroke(1.dp, brandColors.goldAccent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Active Open Inspection",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = brandColors.onGoldContainer
                                    )
                                    Text(
                                        text = activeReport.clientName.ifBlank { "Untitled Site Report (${activeReport.reportNumber})" },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = brandColors.onGoldContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                PrimaryDarkGreenButton(
                                    text = "Resume",
                                    onClick = { onResumeActiveReport(activeReport.id) },
                                    modifier = Modifier.testTag("resume_active_report_button")
                                )
                            }
                        }
                    }
                }

                // 3 Core Navigation Cards: Create New Report (featured), Reports (merged hub), Settings
                items(
                    items = options,
                    key = { it.testTag },
                    span = { item ->
                        if (item.isPrimary && columns == 2) GridItemSpan(columns) else GridItemSpan(1)
                    }
                ) { item ->
                    DashboardActionCard(option = item)
                }
            }
        }
    }

    if (showResetConfirm && activeReport != null) {
        ConfirmActionDialog(
            title = "Reset Active Form?",
            message = "This clears only the currently open report form. Your other saved reports will not be affected.",
            confirmLabel = "Reset Form",
            isDestructive = true,
            onConfirm = {
                showResetConfirm = false
                onResetActiveReport()
            },
            onDismiss = { showResetConfirm = false }
        )
    }

    if (showDeleteConfirm && activeReport != null) {
        ConfirmActionDialog(
            title = "Delete Saved Report?",
            message = "Permanently delete \"${activeReport.clientName.ifBlank { activeReport.reportNumber }}\" and all its app-managed site photos and layouts?",
            confirmLabel = "Delete Report",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDeleteActiveReport()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showShaktiChakraDialog) {
        ShaktiChakraCompassDialog(
            onDismiss = { showShaktiChakraDialog = false }
        )
    }
}

@Composable
private fun DashboardActionCard(option: DashboardOption) {
    val brandColors = LocalVedicraColors.current
    val containerColor = if (option.isPrimary) {
        brandColors.actionDarkGreen
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (option.isPrimary) {
        brandColors.onActionDarkGreen
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (option.isPrimary) {
        brandColors.onActionDarkGreen.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = option.onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (option.isPrimary) null else BorderStroke(1.dp, brandColors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (option.isPrimary) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .testTag(option.testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (option.isPrimary) {
                    Color.White.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.title,
                        tint = if (option.isPrimary) Color(0xFFEBD089) else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
