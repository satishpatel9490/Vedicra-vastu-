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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.JsonCodec
import com.example.data.local.ReportEntity
import com.example.model.EntranceDetail
import com.example.model.VastuChakraSpec
import com.example.model.VastuCompassMath
import com.example.model.VastuInnerDevta
import com.example.model.VastuPada
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VedicraSectionCard
import com.example.ui.theme.LocalVedicraColors
import java.util.Locale

private val directionFilterGroups = listOf("All", "North", "East", "South", "West")

private fun padaIdFor(code: String): String = "pada_$code"

private fun EntranceDetail.matchesPadaCode(code: String): Boolean {
    if (id == padaIdFor(code)) return true
    val trimmed = label.trim()
    return trimmed == code ||
        trimmed.startsWith("$code ") ||
        trimmed.startsWith("$code(") ||
        trimmed.startsWith("$code—") ||
        trimmed.startsWith("$code-")
}

private fun innerDevtasForPada(pada: VastuPada): List<VastuInnerDevta> {
    val mid = VastuCompassMath.normalizeDegrees(pada.midDegree)
    return VastuChakraSpec.innerDevtas13.filter { devta ->
        devta.ringLayer > 1 && run {
            val end = devta.startDegree + devta.sweepDegree
            if (end <= 360f) {
                mid >= devta.startDegree && mid < end
            } else {
                mid >= devta.startDegree || mid < (end - 360f)
            }
        }
    }
}

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
    var selectedGroup by remember { mutableStateOf("All") }

    val filteredPadas = remember(selectedGroup) {
        if (selectedGroup == "All") {
            VastuChakraSpec.padasNorthToWestOrder
        } else {
            VastuChakraSpec.padasNorthToWestOrder.filter { it.directionGroup == selectedGroup }
        }
    }

    val additionalEntrances = remember(report.additionalEntrancesJson) {
        JsonCodec.decodeEntrances(report.additionalEntrancesJson)
    }

    val selectedPadas: List<VastuPada> = remember(additionalEntrances) {
        VastuChakraSpec.padasNorthToWestOrder.filter { pada ->
            additionalEntrances.any { it.matchesPadaCode(pada.code) }
        }
    }

    val selectedPadaCodes = remember(selectedPadas) {
        selectedPadas.map { it.code }.toSet()
    }

    val customNonPadaEntrances = remember(additionalEntrances) {
        additionalEntrances.filter { entry ->
            VastuChakraSpec.padas32.none { pada -> entry.matchesPadaCode(pada.code) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VedicraSectionCard(
            title = "Site Entrances & Effects (Multi-Select)",
            subtitle = "Tap one or more entrance padas (N1–W8) used on site to inspect their Devtas, zone attributes & effects below",
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
                        onClick = { selectedGroup = group },
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

            // Selection status & Clear button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedPadas.isEmpty()) {
                        "Tap any entrances below to mark used on site"
                    } else {
                        "Used on site (${selectedPadas.size}): ${selectedPadas.joinToString(", ") { it.code }}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedPadas.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("entrance_selected_summary")
                )
                if (selectedPadas.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            selectedPadas.forEach { pada ->
                                additionalEntrances
                                    .filter { it.matchesPadaCode(pada.code) }
                                    .forEach { onDeleteEntrance(it.id) }
                            }
                            onShowMessage("Cleared selected site entrances")
                        },
                        modifier = Modifier.testTag("entrance_clear_selection_button")
                    ) {
                        Icon(Icons.Filled.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }

            // Multi-Selectable Pada Grid (N1..W8)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredPadas.forEach { pada ->
                    val isSelected = pada.code in selectedPadaCodes
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
                        onClick = {
                            if (isSelected) {
                                additionalEntrances
                                    .filter { it.matchesPadaCode(pada.code) }
                                    .forEach { onDeleteEntrance(it.id) }
                                onShowMessage("Removed entrance ${pada.code}")
                            } else {
                                val zone = VastuCompassMath.zoneForDegree(pada.midDegree)
                                val statusText = if (pada.isAuspicious) "Auspicious ✓" else "Inauspicious ✗"
                                val spanLabel = "${String.format(Locale.US, "%.2f", pada.startDegree)}° – ${String.format(Locale.US, "%.2f", pada.endDegree)}°"
                                onAddEntrance(
                                    EntranceDetail(
                                        id = padaIdFor(pada.code),
                                        direction = pada.directionGroup,
                                        label = "${pada.code} — ${pada.deity}",
                                        degrees = spanLabel,
                                        notes = "$statusText • Zone: ${zone.code} (${zone.attributeTitle}) • Devta: ${pada.deity} (#${pada.padaNumber45}/45) — ${pada.entranceEffect}"
                                    )
                                )
                                onShowMessage("Added entrance ${pada.code} (${pada.deity})")
                            }
                        },
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
        }

        // Selected Entrances: Attributes, Associated Devtas & Effects
        VedicraSectionCard(
            title = "Selected Entrances — Devtas & Attributes",
            subtitle = if (selectedPadas.isEmpty()) {
                "Select one or more entrances above to view their Devtas, zone attributes and Vastu effects"
            } else {
                "Showing ${selectedPadas.size} selected entrance(s) with associated Devtas, 16-zone attributes and effects"
            },
            icon = Icons.Filled.Layers
        ) {
            if (selectedPadas.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, brandColors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_entrances_empty_state")
                ) {
                    Text(
                        text = "No entrances marked as used yet. Tap N1–W8 chips above (for example N3, N4, E3, S3) to select multiple entrances and view all their Devtas, elements, attributes and effects here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("selected_entrances_details_list")
                ) {
                    selectedPadas.forEach { pada ->
                        val isGood = pada.isAuspicious
                        val accentColor = if (isGood) Color(0xFF2E7D32) else Color(0xFFC62828)
                        val badgeBg = if (isGood) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        val spanLabel = "${String.format(Locale.US, "%.2f", pada.startDegree)}° – ${String.format(Locale.US, "%.2f", pada.endDegree)}°"
                        val zone = VastuCompassMath.zoneForDegree(pada.midDegree)
                        val innerDevtas = innerDevtasForPada(pada)
                        val devtaAssociation = buildString {
                            append("Outer Devta: ${pada.deity} (Mandala #${pada.padaNumber45} of 45)")
                            if (innerDevtas.isNotEmpty()) {
                                append(" • Inner Devta: ")
                                append(innerDevtas.joinToString(", ") { "${it.name.replace('\n', ' ')} (#${it.devtaNumber})" })
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("selected_entrance_detail_card_${pada.code}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${pada.code} — ${pada.deity}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${pada.directionGroup} Wall • $spanLabel",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = badgeBg,
                                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = if (isGood) "✓ Auspicious" else "✗ Inauspicious",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                additionalEntrances
                                                    .filter { it.matchesPadaCode(pada.code) }
                                                    .forEach { onDeleteEntrance(it.id) }
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("remove_selected_entrance_${pada.code}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Remove ${pada.code}",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                // Associated Devtas & 16-Zone Attributes Block
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, brandColors.cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = brandColors.goldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Associated Devtas",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = devtaAssociation,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("entrance_devta_text_${pada.code}")
                                        )
                                        Text(
                                            text = "Zone & Attributes: ${zone.fullName} • ${zone.attributeTitle} • Element: ${pada.element.label}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("entrance_attributes_text_${pada.code}")
                                        )
                                    }
                                }

                                // Entrance Effect Block
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, brandColors.cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
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
                                                text = "Vastu Entrance Effect (${pada.code})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                        }
                                        Text(
                                            text = pada.entranceEffect,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("entrance_effect_text_${pada.code}")
                                        )
                                    }
                                }

                                PrimaryDarkGreenButton(
                                    text = "Set ${pada.code} as Main Entrance",
                                    icon = Icons.Filled.CheckCircle,
                                    onClick = {
                                        val statusText = if (isGood) "Auspicious ✓" else "Inauspicious ✗"
                                        val note = "${pada.code} ${pada.deity} ($spanLabel • $statusText)\nZone: ${zone.fullName} (${zone.attributeTitle})\n$devtaAssociation\nEffect: ${pada.entranceEffect}"
                                        onUpdate { curr ->
                                            curr.copy(
                                                mainEntranceDirection = pada.directionGroup,
                                                mainEntranceNotes = note
                                            )
                                        }
                                        onShowMessage("Main Entrance set to ${pada.code} (${pada.deity})")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("entrance_set_main_button_${pada.code}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Report's Logged Main & Custom Entrances Summary
        VedicraSectionCard(
            title = "Logged Report Entrances",
            subtitle = "Main Entrance and any custom compass-captured entrances included in A4 PDF",
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
                        text = report.mainEntranceNotes.ifBlank {
                            "No main entrance pada selected yet — tap an entrance above and press 'Set as Main Entrance'."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (customNonPadaEntrances.isNotEmpty()) {
                Text(
                    text = "Other Logged Entrances (${customNonPadaEntrances.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                customNonPadaEntrances.forEach { item ->
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
