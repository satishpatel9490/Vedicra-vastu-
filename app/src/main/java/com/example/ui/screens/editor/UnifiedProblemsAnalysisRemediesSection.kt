package com.example.ui.screens.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.AnalysisRowEntity
import com.example.data.local.JsonCodec
import com.example.data.local.ReportEntity
import com.example.data.local.SiteMediaEntity
import com.example.model.VastuConstants
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VedicraDropdownField
import com.example.ui.components.VedicraSectionCard
import com.example.ui.components.VedicraTextField
import com.example.ui.theme.LocalVedicraColors
import java.io.File

private const val CUSTOM_PROBLEM_OPTION = "Custom Problem..."

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedProblemsAnalysisRemediesSection(
    report: ReportEntity,
    rows: List<AnalysisRowEntity>,
    mediaItems: List<SiteMediaEntity>,
    onUpdateReport: ((ReportEntity) -> ReportEntity) -> Unit,
    onTogglePredefinedProblem: (String) -> Unit,
    onAddCustomProblem: (String, String) -> Unit,
    onAddRow: (initialProblem: String, category: String) -> Unit,
    onUpdateRow: (AnalysisRowEntity) -> Unit,
    onDuplicateRow: (AnalysisRowEntity) -> Unit,
    onDeleteRow: (AnalysisRowEntity) -> Unit,
    onToggleRowAttachment: (AnalysisRowEntity, String) -> Unit
) {
    val brandColors = LocalVedicraColors.current
    var fullTableMode by rememberSaveable { mutableStateOf(false) }
    var showSummarySection by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("All Categories") }
    var attachingRow by remember { mutableStateOf<AnalysisRowEntity?>(null) }
    var pendingDeleteRow by remember { mutableStateOf<AnalysisRowEntity?>(null) }

    val mediaMap = remember(mediaItems) { mediaItems.associateBy { it.id } }

    val filteredRows = remember(rows, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All Categories") {
            rows
        } else {
            rows.filter { r ->
                val cat = r.problemCategory.ifBlank { VastuConstants.categoryForProblem(r.clientProblem) }
                cat == selectedCategoryFilter
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Header & Add Problem Row Controls
        VedicraSectionCard(
            title = "Problems & Remedies",
            subtitle = "Select problem from dropdown, enter expected zones responsible, remarks & remedies row by row",
            icon = Icons.AutoMirrored.Filled.ListAlt,
            headerTrailing = {
                IconButton(
                    onClick = { fullTableMode = !fullTableMode },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_unified_view_mode")
                ) {
                    Icon(
                        imageVector = if (fullTableMode) Icons.AutoMirrored.Filled.ListAlt else Icons.Filled.TableChart,
                        contentDescription = if (fullTableMode) "Switch to Row Form View" else "Switch to Table View"
                    )
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryDarkGreenButton(
                    text = "Add Problem Row",
                    icon = Icons.Filled.Add,
                    onClick = {
                        val defaultCat = if (selectedCategoryFilter == "All Categories") {
                            VastuConstants.problemCategories.first()
                        } else {
                            selectedCategoryFilter
                        }
                        onAddRow("", defaultCat)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_unified_row_button")
                )
                OutlinedButton(
                    onClick = { fullTableMode = !fullTableMode },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(if (fullTableMode) "Rows" else "Table")
                }
            }

            if (rows.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filterOptions = listOf("All Categories") + VastuConstants.problemCategories
                    filterOptions.forEach { cat ->
                        val count = if (cat == "All Categories") rows.size else rows.count {
                            it.problemCategory.ifBlank { VastuConstants.categoryForProblem(it.clientProblem) } == cat
                        }
                        if (cat == "All Categories" || count > 0) {
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = {
                                    Text(
                                        text = if (cat == "All Categories") "All ($count)" else "$cat ($count)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Rows arranged one after the other down side one by one
        if (filteredRows.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, brandColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "No problem & remedy rows added yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tap 'Add Problem Row' to pick a problem from the dropdown menu and enter its expected zones responsible, remarks, and remedies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrimaryDarkGreenButton(
                        text = "Add First Problem Row",
                        icon = Icons.Filled.Add,
                        onClick = { onAddRow("", VastuConstants.problemCategories.first()) }
                    )
                }
            }
        } else if (fullTableMode) {
            HorizontalProblemsRemediesTable(
                rows = filteredRows,
                onUpdateRow = onUpdateRow,
                onAttachMedia = { attachingRow = it },
                onDuplicateRow = onDuplicateRow,
                onDeleteRow = { pendingDeleteRow = it }
            )
        } else {
            filteredRows.forEachIndexed { idx, row ->
                key(row.id) {
                    InlineProblemZoneRemarksRemedyCard(
                        serialNumber = idx + 1,
                        row = row,
                        attachedMedia = JsonCodec.decodeStringList(row.attachedMediaIdsJson).mapNotNull { mediaMap[it] },
                        onUpdateRow = onUpdateRow,
                        onAttachMedia = { attachingRow = row },
                        onDuplicate = { onDuplicateRow(row) },
                        onDelete = { pendingDeleteRow = row }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    val defaultCat = if (selectedCategoryFilter == "All Categories") {
                        VastuConstants.problemCategories.first()
                    } else {
                        selectedCategoryFilter
                    }
                    onAddRow("", defaultCat)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_another_row_bottom_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Another Problem & Remedy Row")
            }
        }

        // 3. Problem Facing Since (kept as-is!)
        VedicraSectionCard(
            title = "Problem Duration",
            subtitle = "Since when have you been facing these problems?"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("6 months", "1 year", "2 years", "3–5 years", "5+ years", "Since moving in").forEach { preset ->
                    FilterChip(
                        selected = report.problemSince == preset,
                        onClick = { onUpdateReport { it.copy(problemSince = preset) } },
                        label = { Text(preset) }
                    )
                }
            }
            VedicraTextField(
                value = report.problemSince,
                onValueChange = { v -> onUpdateReport { it.copy(problemSince = v) } },
                label = "Since when have you been facing these problems?",
                placeholder = "Exact date, approximate month/year, or duration"
            )
        }

        // 4. Overall Remedy Timeline, Instructions & Disclaimer (Collapsible Summary Card)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, brandColors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSummarySection = !showSummarySection },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Overall Follow-Up, Timeline & Summary",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Priority: ${report.remedyPriority} • Waiting Period: ${
                                    if (report.expectedWaitingPeriod == "Custom period" && report.customWaitingPeriod.isNotBlank()) {
                                        report.customWaitingPeriod
                                    } else report.expectedWaitingPeriod
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showSummarySection) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand Summary"
                    )
                }

                AnimatedVisibility(visible = showSummarySection) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HorizontalDivider()
                        Text("Overall Remedy Priority", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VastuConstants.remedyPriorities.forEach { priority ->
                                FilterChip(
                                    selected = report.remedyPriority == priority,
                                    onClick = { onUpdateReport { it.copy(remedyPriority = priority) } },
                                    label = { Text(priority) }
                                )
                            }
                        }

                        Text("Expected Result Waiting Period", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VastuConstants.waitingPeriods.forEach { period ->
                                FilterChip(
                                    selected = report.expectedWaitingPeriod == period,
                                    onClick = { onUpdateReport { it.copy(expectedWaitingPeriod = period) } },
                                    label = { Text(period) }
                                )
                            }
                        }
                        if (report.expectedWaitingPeriod == "Custom period") {
                            VedicraTextField(
                                value = report.customWaitingPeriod,
                                onValueChange = { v -> onUpdateReport { it.copy(customWaitingPeriod = v) } },
                                label = "Specify Custom Waiting Period",
                                placeholder = "e.g. 120 days / 4 months"
                            )
                        }
                        VedicraTextField(
                            value = report.expectedFollowUpDate,
                            onValueChange = { v -> onUpdateReport { it.copy(expectedFollowUpDate = v) } },
                            label = "Expected Follow-Up Date",
                            placeholder = "DD/MM/YYYY"
                        )
                        VedicraTextField(
                            value = report.implementationInstructions,
                            onValueChange = { v -> onUpdateReport { it.copy(implementationInstructions = v) } },
                            label = "Overall Implementation Instructions",
                            singleLine = false,
                            minLines = 2
                        )
                        VedicraTextField(
                            value = report.recommendedRemedies,
                            onValueChange = { v -> onUpdateReport { it.copy(recommendedRemedies = v) } },
                            label = "Additional General Remedies Summary",
                            singleLine = false,
                            minLines = 2
                        )
                        VedicraTextField(
                            value = report.remedyNotes,
                            onValueChange = { v -> onUpdateReport { it.copy(remedyNotes = v) } },
                            label = "Follow-Up & Additional Notes",
                            singleLine = false,
                            minLines = 2
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = brandColors.goldContainer,
                            border = BorderStroke(1.dp, brandColors.goldAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = brandColors.onGoldContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Disclaimer: Expected waiting periods are professional estimates only. Results depend on full implementation and multiple external factors and are not guaranteed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = brandColors.onGoldContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    attachingRow?.let { target ->
        val freshRow = rows.firstOrNull { it.id == target.id } ?: target
        UnifiedRowMediaAttachmentDialog(
            row = freshRow,
            allMedia = mediaItems,
            onToggleAttachment = { mediaId -> onToggleRowAttachment(freshRow, mediaId) },
            onDismiss = { attachingRow = null }
        )
    }

    pendingDeleteRow?.let { target ->
        ConfirmActionDialog(
            title = "Delete Row?",
            message = "Remove '${target.clientProblem.ifBlank { "this row" }}' and its expected zones, remarks, and remedies?",
            confirmLabel = "Delete Row",
            isDestructive = true,
            onConfirm = {
                onDeleteRow(target)
                pendingDeleteRow = null
            },
            onDismiss = { pendingDeleteRow = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineProblemZoneRemarksRemedyCard(
    serialNumber: Int,
    row: AnalysisRowEntity,
    attachedMedia: List<SiteMediaEntity>,
    onUpdateRow: (AnalysisRowEntity) -> Unit,
    onAttachMedia: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val brandColors = LocalVedicraColors.current
    var showAdvancedDetails by rememberSaveable(row.id) { mutableStateOf(false) }
    var useCustomProblemInput by remember(row.id) {
        mutableStateOf(
            row.clientProblem.isNotBlank() &&
                row.clientProblem !in VastuConstants.predefinedProblems
        )
    }

    val resolvedCategory = row.problemCategory.ifBlank {
        if (row.clientProblem.isNotBlank()) VastuConstants.categoryForProblem(row.clientProblem)
        else VastuConstants.problemCategories.first()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, brandColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("unified_row_card_$serialNumber")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row Header: Serial #, Category Badge, Expand Extra Details, Attach Photos, Duplicate, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = brandColors.actionDarkGreen
                    ) {
                        Text(
                            text = "#$serialNumber",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = resolvedCategory,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showAdvancedDetails = !showAdvancedDetails },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "More Vastu Details",
                            tint = if (showAdvancedDetails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onAttachMedia,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("attach_media_row_$serialNumber")
                    ) {
                        BadgedBox(
                            badge = {
                                if (attachedMedia.isNotEmpty()) {
                                    Badge { Text("${attachedMedia.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = "Attach Photos",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate Row", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete Row",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 700.dp

                if (isWide) {
                    // Wide screen: All 4 boxes side-by-side cleanly
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        ProblemDropdownColumn(
                            row = row,
                            useCustomInput = useCustomProblemInput,
                            onSelectPredefined = { selected ->
                                if (selected == CUSTOM_PROBLEM_OPTION) {
                                    useCustomProblemInput = true
                                } else {
                                    useCustomProblemInput = false
                                    onUpdateRow(
                                        row.copy(
                                            clientProblem = selected,
                                            problemCategory = VastuConstants.categoryForProblem(selected)
                                        )
                                    )
                                }
                            },
                            onCustomTextChange = { text ->
                                onUpdateRow(
                                    row.copy(
                                        clientProblem = text,
                                        problemCategory = VastuConstants.categoryForProblem(text)
                                    )
                                )
                            },
                            serialNumber = serialNumber,
                            modifier = Modifier.weight(1.1f)
                        )

                        ExpectedZoneInputBox(
                            value = row.responsibleDirectionZone,
                            onValueChange = { z -> onUpdateRow(row.copy(responsibleDirectionZone = z)) },
                            serialNumber = serialNumber,
                            modifier = Modifier.weight(0.95f)
                        )

                        VedicraTextField(
                            value = row.notes,
                            onValueChange = { r -> onUpdateRow(row.copy(notes = r)) },
                            label = "Remarks",
                            placeholder = "Site remarks / observations",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier.weight(1f),
                            testTag = "remarks_input_row_$serialNumber"
                        )

                        VedicraTextField(
                            value = row.suggestedRemedy,
                            onValueChange = { rem -> onUpdateRow(row.copy(suggestedRemedy = rem)) },
                            label = "Remedies",
                            placeholder = "Remedies for this problem",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier.weight(1.1f),
                            testTag = "remedies_input_row_$serialNumber"
                        )
                    }
                } else {
                    // Phone layout:
                    // Row 1 (Side by Side): Problem Dropdown  |  Expected Zones Responsible
                    // Below one after the other: Remarks Box -> Remedies Box
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            ProblemDropdownColumn(
                                row = row,
                                useCustomInput = useCustomProblemInput,
                                onSelectPredefined = { selected ->
                                    if (selected == CUSTOM_PROBLEM_OPTION) {
                                        useCustomProblemInput = true
                                    } else {
                                        useCustomProblemInput = false
                                        onUpdateRow(
                                            row.copy(
                                                clientProblem = selected,
                                                problemCategory = VastuConstants.categoryForProblem(selected)
                                            )
                                        )
                                    }
                                },
                                onCustomTextChange = { text ->
                                    onUpdateRow(
                                        row.copy(
                                            clientProblem = text,
                                            problemCategory = VastuConstants.categoryForProblem(text)
                                        )
                                    )
                                },
                                serialNumber = serialNumber,
                                modifier = Modifier.weight(1.08f)
                            )

                            ExpectedZoneInputBox(
                                value = row.responsibleDirectionZone,
                                onValueChange = { z -> onUpdateRow(row.copy(responsibleDirectionZone = z)) },
                                serialNumber = serialNumber,
                                modifier = Modifier.weight(0.92f)
                            )
                        }

                        VedicraTextField(
                            value = row.notes,
                            onValueChange = { r -> onUpdateRow(row.copy(notes = r)) },
                            label = "Remarks",
                            placeholder = "Enter remarks / observations for this problem",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 6,
                            testTag = "remarks_input_row_$serialNumber"
                        )

                        VedicraTextField(
                            value = row.suggestedRemedy,
                            onValueChange = { rem -> onUpdateRow(row.copy(suggestedRemedy = rem)) },
                            label = "Remedies",
                            placeholder = "Enter remedies for this problem",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 6,
                            testTag = "remedies_input_row_$serialNumber"
                        )
                    }
                }
            }

            // Optional expandable detailed Vastu fields (Anti-activity, Anti-utility, Anti-objects, Priority, Wait Period, Stop/Remove/Add/Structural)
            AnimatedVisibility(visible = showAdvancedDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    Text(
                        text = "Additional Vastu Analysis & Action Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    VedicraDropdownField(
                        label = "Problem Category",
                        selectedValue = resolvedCategory,
                        options = VastuConstants.problemCategories,
                        onOptionSelected = { cat -> onUpdateRow(row.copy(problemCategory = cat)) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Priority:", style = MaterialTheme.typography.labelMedium)
                        VastuConstants.remedyPriorities.forEach { p ->
                            FilterChip(
                                selected = row.remedyPriority == p,
                                onClick = { onUpdateRow(row.copy(remedyPriority = p)) },
                                label = { Text(p, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    VedicraTextField(
                        value = row.waitingPeriod,
                        onValueChange = { w -> onUpdateRow(row.copy(waitingPeriod = w)) },
                        label = "Row Expected Waiting Period",
                        placeholder = "e.g. 21 days / 45 days"
                    )
                    VedicraTextField(
                        value = row.problemObserved,
                        onValueChange = { p -> onUpdateRow(row.copy(problemObserved = p)) },
                        label = "Detailed Problem Observed",
                        singleLine = false
                    )
                    VedicraTextField(
                        value = row.antiActivity,
                        onValueChange = { a -> onUpdateRow(row.copy(antiActivity = a)) },
                        label = "Anti Activity",
                        singleLine = false
                    )
                    VedicraTextField(
                        value = row.antiUtility,
                        onValueChange = { u -> onUpdateRow(row.copy(antiUtility = u)) },
                        label = "Anti Utility",
                        singleLine = false
                    )
                    VedicraTextField(
                        value = row.antiObjects,
                        onValueChange = { o -> onUpdateRow(row.copy(antiObjects = o)) },
                        label = "Anti Objects",
                        singleLine = false
                    )
                    VedicraTextField(
                        value = row.activitiesToStop,
                        onValueChange = { s -> onUpdateRow(row.copy(activitiesToStop = s)) },
                        label = "Activities to Stop",
                        singleLine = false
                    )
                    VedicraTextField(
                        value = row.objectsToRemove,
                        onValueChange = { r -> onUpdateRow(row.copy(objectsToRemove = r)) },
                        label = "Objects to Remove",
                        singleLine = false
                    )
                    VedicraTextField(
                        value = row.objectsToAdd,
                        onValueChange = { a -> onUpdateRow(row.copy(objectsToAdd = a)) },
                        label = "Objects to Add",
                        singleLine = false
                    )
                    VedicraTextField(
                        value = row.utilitiesToChange,
                        onValueChange = { u -> onUpdateRow(row.copy(utilitiesToChange = u)) },
                        label = "Utilities to Change",
                        singleLine = false
                    )
                    VedicraTextField(
                        value = row.structuralCorrections,
                        onValueChange = { s -> onUpdateRow(row.copy(structuralCorrections = s)) },
                        label = "Structural Corrections",
                        singleLine = false
                    )
                }
            }

            if (attachedMedia.isNotEmpty()) {
                HorizontalDivider()
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(attachedMedia, key = { it.id }) { m ->
                        val path = m.annotatedFilePath?.takeIf { File(it).exists() } ?: m.filePath
                        AsyncImage(
                            model = File(path),
                            contentDescription = m.caption.ifBlank { "Attached Photo" },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemDropdownColumn(
    row: AnalysisRowEntity,
    useCustomInput: Boolean,
    onSelectPredefined: (String) -> Unit,
    onCustomTextChange: (String) -> Unit,
    serialNumber: Int,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val allOptions = remember {
        VastuConstants.predefinedProblems + CUSTOM_PROBLEM_OPTION
    }
    val displayValue = when {
        useCustomInput -> CUSTOM_PROBLEM_OPTION
        row.clientProblem.isNotBlank() -> row.clientProblem
        else -> ""
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPicker = true }
        ) {
            OutlinedTextField(
                value = displayValue.ifBlank { "Select Problem" },
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Problem") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select Problem"
                    )
                },
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("problem_dropdown_row_$serialNumber")
            )
        }

        if (showPicker) {
            AlertDialog(
                onDismissRequest = { showPicker = false },
                title = { Text("Select Problem") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allOptions.forEach { option ->
                            val selected = option == displayValue
                            Surface(
                                onClick = {
                                    showPicker = false
                                    onSelectPredefined(option)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (useCustomInput || row.clientProblem == "Other problems") {
            VedicraTextField(
                value = if (row.clientProblem == "Other problems") "" else row.clientProblem,
                onValueChange = onCustomTextChange,
                label = "Custom Problem",
                placeholder = "Type specific problem",
                testTag = "custom_problem_row_$serialNumber"
            )
        }
    }
}

@Composable
private fun ExpectedZoneInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    serialNumber: Int,
    modifier: Modifier = Modifier
) {
    var showZonePicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Expected Zones Responsible") },
            placeholder = { Text("e.g. NE, SE") },
            singleLine = false,
            minLines = 1,
            maxLines = 3,
            trailingIcon = {
                IconButton(onClick = { showZonePicker = true }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Pick Vastu Zone"
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expected_zones_row_$serialNumber")
        )
    }

    if (showZonePicker) {
        AlertDialog(
            onDismissRequest = { showZonePicker = false },
            title = { Text("Add Vastu Zone") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VastuConstants.sixteenVastuZones.forEach { zone ->
                        Surface(
                            onClick = {
                                val updated = if (value.isBlank()) zone else "$value, $zone"
                                onValueChange(updated)
                                showZonePicker = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = zone,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showZonePicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun HorizontalProblemsRemediesTable(
    rows: List<AnalysisRowEntity>,
    onUpdateRow: (AnalysisRowEntity) -> Unit,
    onAttachMedia: (AnalysisRowEntity) -> Unit,
    onDuplicateRow: (AnalysisRowEntity) -> Unit,
    onDeleteRow: (AnalysisRowEntity) -> Unit
) {
    val brandColors = LocalVedicraColors.current
    val hScroll = rememberScrollState()
    val headers = listOf(
        "S.No" to 46.dp,
        "Problem (Dropdown)" to 200.dp,
        "Expected Zones Responsible" to 190.dp,
        "Remarks" to 200.dp,
        "Remedies" to 220.dp,
        "Photos & Actions" to 150.dp
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, brandColors.cardBorder),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.horizontalScroll(hScroll)) {
            Row(
                modifier = Modifier
                    .background(brandColors.actionDarkGreen)
                    .padding(vertical = 10.dp, horizontal = 8.dp)
            ) {
                headers.forEach { (title, width) ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .width(width)
                            .padding(horizontal = 6.dp)
                    )
                }
            }

            rows.forEachIndexed { idx, row ->
                val attachedCount = JsonCodec.decodeStringList(row.attachedMediaIdsJson).size
                var useCustom by remember(row.id) {
                    mutableStateOf(row.clientProblem.isNotBlank() && row.clientProblem !in VastuConstants.predefinedProblems)
                }
                Row(
                    modifier = Modifier
                        .background(
                            if (idx % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            else Color.Transparent
                        )
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${idx + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .width(40.dp)
                            .padding(top = 14.dp, start = 6.dp)
                    )
                    ProblemDropdownColumn(
                        row = row,
                        useCustomInput = useCustom,
                        onSelectPredefined = { selected ->
                            if (selected == CUSTOM_PROBLEM_OPTION) {
                                useCustom = true
                            } else {
                                useCustom = false
                                onUpdateRow(
                                    row.copy(
                                        clientProblem = selected,
                                        problemCategory = VastuConstants.categoryForProblem(selected)
                                    )
                                )
                            }
                        },
                        onCustomTextChange = { text ->
                            onUpdateRow(
                                row.copy(
                                    clientProblem = text,
                                    problemCategory = VastuConstants.categoryForProblem(text)
                                )
                            )
                        },
                        serialNumber = idx + 1,
                        modifier = Modifier.width(194.dp)
                    )
                    ExpectedZoneInputBox(
                        value = row.responsibleDirectionZone,
                        onValueChange = { z -> onUpdateRow(row.copy(responsibleDirectionZone = z)) },
                        serialNumber = idx + 1,
                        modifier = Modifier.width(184.dp)
                    )
                    VedicraTextField(
                        value = row.notes,
                        onValueChange = { n -> onUpdateRow(row.copy(notes = n)) },
                        label = "Remarks",
                        singleLine = false,
                        minLines = 2,
                        modifier = Modifier.width(194.dp)
                    )
                    VedicraTextField(
                        value = row.suggestedRemedy,
                        onValueChange = { r -> onUpdateRow(row.copy(suggestedRemedy = r)) },
                        label = "Remedies",
                        singleLine = false,
                        minLines = 2,
                        modifier = Modifier.width(214.dp)
                    )
                    Row(
                        modifier = Modifier
                            .width(150.dp)
                            .padding(top = 8.dp)
                    ) {
                        IconButton(onClick = { onAttachMedia(row) }) {
                            BadgedBox(badge = { if (attachedCount > 0) Badge { Text("$attachedCount") } }) {
                                Icon(Icons.Filled.AttachFile, contentDescription = "Attach Photos", modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = { onDuplicateRow(row) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteRow(row) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun UnifiedRowMediaAttachmentDialog(
    row: AnalysisRowEntity,
    allMedia: List<SiteMediaEntity>,
    onToggleAttachment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val attachedIds = remember(row.attachedMediaIdsJson) {
        JsonCodec.decodeStringList(row.attachedMediaIdsJson).toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attach Site Photos / Drawings") },
        text = {
            if (allMedia.isEmpty()) {
                Text("No site photographs or drawings captured yet. Capture or import photos in the Site Photos or Drawings tab, then attach them here.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select one or more photos/drawings to link to this row:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    allMedia.forEach { media ->
                        val isChecked = media.id in attachedIds
                        val displayPath = media.annotatedFilePath?.takeIf { File(it).exists() } ?: media.filePath
                        Surface(
                            onClick = { onToggleAttachment(media.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = File(displayPath),
                                    contentDescription = media.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = media.drawingTitle.ifBlank { media.caption.ifBlank { media.mediaKind } },
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (media.description.isNotBlank()) {
                                        Text(
                                            text = media.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleAttachment(media.id) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Done")
            }
        }
    )
}
