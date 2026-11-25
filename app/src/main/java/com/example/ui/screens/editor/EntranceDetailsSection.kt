package com.example.ui.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.JsonCodec
import com.example.data.local.ReportEntity
import com.example.model.EntranceDetail
import com.example.model.VastuChakraSpec
import com.example.model.VastuPada
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VedicraSectionCard
import com.example.ui.theme.LocalVedicraColors
import java.util.Locale

private val directionFilterGroups = listOf("All", "North", "East", "South", "West")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntranceDetailsSection(
    report: ReportEntity,
    onUpdate: ((ReportEntity) -> ReportEntity) -> Unit,
    onAddEntrance: (EntranceDetail) -> Unit,
    onDeleteEntrance: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val brandColors = LocalVedicraColors.current
    var selectedGroup by remember { mutableStateOf("North") }
    var selectedPadaCode by remember { mutableStateOf("N4") }

    val filteredPadas = remember(selectedGroup) {
        if (selectedGroup == "All") {
            VastuChakraSpec.padasNorthToWestOrder
        } else {
            VastuChakraSpec.padasNorthToWestOrder.filter { it.directionGroup == selectedGroup }
        }
    }

    val selectedPada: VastuPada = remember(selectedPadaCode, filteredPadas) {
        VastuChakraSpec.padas32.firstOrNull { it.code == selectedPadaCode }
            ?: filteredPadas.firstOrNull()
            ?: VastuChakraSpec.padas32.first()
    }

    val additionalEntrances = remember(report.additionalEntrancesJson) {
        JsonCodec.decodeEntrances(report.additionalEntrancesJson)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VedicraSectionCard(
            title = "32 Entrances & Effects",
            subtitle = "Select an entrance pada (N1–W8) to inspect its Vastu effect & save to report",
            icon = Icons.Filled.DoorFront
        ) {
            // Direction Group Filter
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                directionFilterGroups.forEach { group ->
                    FilterChip(
                        selected = selectedGroup == group,
                        onClick = {
                            selectedGroup = group
                            val firstInGroup = if (group == "All") {
                                VastuChakraSpec.padasNorthToWestOrder.first()
                            } else {
                                VastuChakraSpec.padasNorthToWestOrder.first { it.directionGroup == group }
                            }
                            if (group != "All" && selectedPada.directionGroup != group) {
                                selectedPadaCode = firstInGroup.code
                            }
                        },
                        label = {
                            val suffix = when (group) {
                                "North" -> " (N1–N8)"
                                "East" -> " (E1–E8)"
                                "South" -> " (S1–S8)"
                                "West" -> " (W1–W8)"
                                else -> " (32)"
                            }
                            Text(
                                text = "$group$suffix",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("entrance_filter_${group.lowercase(Locale.US)}")
                    )
                }
            }

            // Selectable Pada Grid (N1..N8 etc.)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredPadas.forEach { pada ->
                    val isSelected = pada.code == selectedPada.code
                    val statusTint = if (pada.isAuspicious) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val containerColor = when {
                        isSelected -> statusTint
                        pada.isAuspicious -> Color(0xFFE8F5E9)
                        else -> Color(0xFFFFEBEE)
                    }
                    val contentColor = when {
                        isSelected -> Color.White
                        pada.isAuspicious -> Color(0xFF1B5E20)
                        else -> Color(0xFF8E1B1B)
                    }

                    Surface(
                        onClick = { selectedPadaCode = pada.code },
                        shape = RoundedCornerShape(10.dp),
                        color = containerColor,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) brandColors.goldAccent else statusTint.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("entrance_pada_chip_${pada.code}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${pada.code} ${if (pada.isAuspicious) "✓" else "✗"}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                            Text(
                                text = pada.deity,
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Selected Entrance Detailed Effects Card
            val isGood = selectedPada.isAuspicious
            val accentColor = if (isGood) Color(0xFF2E7D32) else Color(0xFFC62828)
            val badgeBg = if (isGood) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            val spanLabel = "${String.format(Locale.US, "%.2f", selectedPada.startDegree)}° – ${String.format(Locale.US, "%.2f", selectedPada.endDegree)}°"

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_entrance_detail_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedPada.code} — ${selectedPada.deity}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${selectedPada.directionGroup} Wall • $spanLabel • Element: ${selectedPada.element.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isGood) "✓ Auspicious" else "✗ Inauspicious",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, brandColors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Vastu Entrance Effect (${selectedPada.code})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Text(
                                text = selectedPada.entranceEffect,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("entrance_effect_text")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrimaryDarkGreenButton(
                            text = "Set Main Entrance",
                            icon = Icons.Filled.CheckCircle,
                            onClick = {
                                val statusText = if (isGood) "Auspicious ✓" else "Inauspicious ✗"
                                val note = "${selectedPada.code} ${selectedPada.deity} ($spanLabel • $statusText)\nEffect: ${selectedPada.entranceEffect}"
                                onUpdate { curr ->
                                    curr.copy(
                                        mainEntranceDirection = selectedPada.directionGroup,
                                        mainEntranceNotes = note
                                    )
                                }
                                onShowMessage("Main Entrance set to ${selectedPada.code} (${selectedPada.deity})")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("entrance_set_main_button")
                        )
                        OutlinedButton(
                            onClick = {
                                val statusText = if (isGood) "Auspicious ✓" else "Inauspicious ✗"
                                onAddEntrance(
                                    EntranceDetail(
                                        direction = selectedPada.directionGroup,
                                        label = "${selectedPada.code} (${selectedPada.deity})",
                                        degrees = spanLabel,
                                        notes = "$statusText — ${selectedPada.entranceEffect}"
                                    )
                                )
                                onShowMessage("Added ${selectedPada.code} to entrances list")
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("entrance_add_list_button")
                        ) {
                            Icon(Icons.Filled.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add to List", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Report's Logged Entrances & Effects Summary
        VedicraSectionCard(
            title = "Logged Report Entrances",
            subtitle = "Included in the Property Section & A4 PDF Report",
            icon = Icons.Filled.DoorFront
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, brandColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Main Entrance: ${report.mainEntranceDirection}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = report.mainEntranceNotes.ifBlank { "No pada effect selected yet — select N1–W8 above and tap 'Set Main Entrance'." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (additionalEntrances.isNotEmpty()) {
                Text(
                    text = "Additional Entrances (${additionalEntrances.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                additionalEntrances.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, brandColors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${item.label.ifBlank { "Entrance" }} • ${item.direction}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (item.degrees.isNotBlank()) {
                                    Text(
                                        text = "Degrees: ${item.degrees}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (item.notes.isNotBlank()) {
                                    Text(
                                        text = item.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteEntrance(item.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Remove Entrance",
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
