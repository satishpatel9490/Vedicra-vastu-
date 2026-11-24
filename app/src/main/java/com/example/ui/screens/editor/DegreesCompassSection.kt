package com.example.ui.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ReportEntity
import com.example.model.CompassAccuracy
import com.example.model.CompassReading
import com.example.model.EntranceDetail
import com.example.model.VastuCompassMath
import com.example.model.VastuConstants
import com.example.sensor.rememberVastuCompassState
import com.example.ui.components.CompassTopDegreeTiltHud
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VastuChakraCanvas
import com.example.ui.components.VedicraDropdownField
import com.example.ui.components.VedicraSectionCard
import com.example.ui.components.VedicraTextField
import com.example.ui.theme.LocalVedicraColors
import java.util.Locale

private val quickPresetDegrees = listOf(
    0f to "0°/360° N",
    45f to "45° NE",
    90f to "90° E",
    135f to "135° SE",
    180f to "180° S",
    225f to "225° SW",
    270f to "270° W",
    315f to "315° NW"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DegreesAndCompassSection(
    report: ReportEntity,
    onUpdate: ((ReportEntity) -> ReportEntity) -> Unit,
    onAddEntrance: (EntranceDetail) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val liveCompass by rememberVastuCompassState(enabled = true)
    var useManualDial by remember(liveCompass.hasHardwareSensors) {
        mutableStateOf(!liveCompass.hasHardwareSensors)
    }
    var manualAzimuthDeg by remember { mutableFloatStateOf(0f) }
    var isLocked by remember { mutableStateOf(false) }
    var lockedReading by remember { mutableStateOf<CompassReading?>(null) }

    val activeReading = remember(liveCompass, useManualDial, manualAzimuthDeg, isLocked, lockedReading) {
        val current = if (useManualDial || !liveCompass.hasHardwareSensors) {
            liveCompass.copy(
                azimuthDeg = manualAzimuthDeg,
                zone = VastuCompassMath.zoneForDegree(manualAzimuthDeg),
                pada = VastuCompassMath.padaForDegree(manualAzimuthDeg),
                accuracy = if (liveCompass.hasHardwareSensors) liveCompass.accuracy else CompassAccuracy.MANUAL
            )
        } else {
            liveCompass
        }
        if (isLocked) lockedReading ?: current else current
    }

    val formattedHeading = remember(activeReading.azimuthDeg) {
        VastuCompassMath.formatHeadingLabel(activeReading.azimuthDeg)
    }
    val mappedDirection = remember(activeReading.azimuthDeg) {
        VastuCompassMath.entranceDirectionForDegree(activeReading.azimuthDeg)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VedicraSectionCard(
            title = "COMPASS",
            subtitle = "Shakti Chakra • 32 Padas & 16 Zones • 0°/360° North lies between N4 & N5",
            icon = Icons.Filled.Explore
        ) {
            // Top Degree & Tilt HUD
            CompassTopDegreeTiltHud(
                reading = activeReading,
                isLocked = isLocked,
                titlePrefix = "SHAKTI CHAKRA",
                onToggleLock = {
                    if (!isLocked) {
                        lockedReading = activeReading
                    }
                    isLocked = !isLocked
                }
            )

            // Interactive Shakti Chakra rotating/tilting with Compass North 0° (between N4 & N5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                VastuChakraCanvas(
                    rotationDegrees = -activeReading.azimuthDeg,
                    transparentFill = false,
                    showDeityNames = true,
                    showTopLubberNeedle = true,
                    tiltRollDeg = activeReading.rollDeg,
                    tiltPitchDeg = activeReading.pitchDeg,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .testTag("degrees_tab_shakti_chakra")
                )
            }

            // Sensor vs Manual Dial Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (liveCompass.hasHardwareSensors) {
                    FilterChip(
                        selected = !useManualDial,
                        onClick = { useManualDial = false },
                        label = { Text("Live Sensor") }
                    )
                }
                FilterChip(
                    selected = useManualDial || !liveCompass.hasHardwareSensors,
                    onClick = { useManualDial = true },
                    label = { Text("Manual Dial") },
                    modifier = Modifier.testTag("compass_manual_mode_chip")
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (activeReading.pada.isAuspicious) {
                        "${activeReading.pada.code} ${activeReading.pada.deity} ✓"
                    } else {
                        "${activeReading.pada.code} ${activeReading.pada.deity} ✗"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (activeReading.pada.isAuspicious) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            if (useManualDial || !liveCompass.hasHardwareSensors) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", manualAzimuthDeg)}°",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = manualAzimuthDeg,
                            onValueChange = {
                                manualAzimuthDeg = it
                                if (isLocked) {
                                    lockedReading = activeReading.copy(
                                        azimuthDeg = it,
                                        zone = VastuCompassMath.zoneForDegree(it),
                                        pada = VastuCompassMath.padaForDegree(it)
                                    )
                                }
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("compass_manual_degree_slider")
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        quickPresetDegrees.forEach { (deg, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(manualAzimuthDeg - deg) < 1f ||
                                    (deg == 0f && manualAzimuthDeg > 359f),
                                onClick = { manualAzimuthDeg = deg },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Quick Capture Actions to Note Degrees Directly Into the Report
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, LocalVedicraColors.current.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Note Captured Reading Into Report",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Current: $formattedHeading • ${activeReading.zone.attributeTitle} • Tilt ${String.format(Locale.US, "%.1f", activeReading.tiltFromLevelDeg)}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    PrimaryDarkGreenButton(
                        text = "Note to Degrees of Plot",
                        icon = Icons.Filled.CheckCircle,
                        onClick = {
                            val noteEntry = "Compass Plot Reading: $formattedHeading | Tilt: ${String.format(Locale.US, "%.1f", activeReading.tiltFromLevelDeg)}° | Zone: ${activeReading.zone.fullName} (${activeReading.zone.attributeTitle})"
                            onUpdate { curr ->
                                val updatedObs = if (curr.propertyObservations.isBlank()) {
                                    noteEntry
                                } else if (!curr.propertyObservations.contains(noteEntry)) {
                                    "${curr.propertyObservations.trim()}\n$noteEntry"
                                } else {
                                    curr.propertyObservations
                                }
                                curr.copy(
                                    plotDegrees = formattedHeading,
                                    propertyObservations = updatedObs
                                )
                            }
                            onShowMessage("Noted to Plot Degrees: $formattedHeading")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_plot_degree_button")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val status = if (activeReading.pada.isAuspicious) "Auspicious ✓" else "Inauspicious ✗"
                                val entranceNote = "Main Door: $formattedHeading • ${activeReading.pada.code} ${activeReading.pada.deity} ($status)"
                                onUpdate { curr ->
                                    curr.copy(
                                        mainEntranceDirection = mappedDirection,
                                        mainEntranceNotes = entranceNote
                                    )
                                }
                                onShowMessage("Main Entrance noted: $mappedDirection ($formattedHeading)")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("note_main_entrance_degree_button")
                        ) {
                            Icon(Icons.Filled.DoorFront, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Main Entrance", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                val status = if (activeReading.pada.isAuspicious) "Auspicious ✓" else "Inauspicious ✗"
                                onAddEntrance(
                                    EntranceDetail(
                                        direction = mappedDirection,
                                        label = "Compass Reading",
                                        degrees = formattedHeading,
                                        notes = "${activeReading.zone.attributeTitle} • $status"
                                    )
                                )
                                onShowMessage("Added entrance degree: $formattedHeading")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("note_add_entrance_degree_button")
                        ) {
                            Icon(Icons.Filled.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Entrance", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Editable Degree Notes Summary Card
        VedicraSectionCard(
            title = "Noted Degrees & Orientation Log",
            subtitle = "Saved directly to the report and included in A4 PDF output",
            icon = Icons.Filled.EditNote
        ) {
            VedicraTextField(
                value = report.plotDegrees,
                onValueChange = { v -> onUpdate { it.copy(plotDegrees = v) } },
                label = "Degrees of Plot",
                placeholder = "e.g. 0.0° / 360° N (Between N4 & N5)",
                testTag = "degrees_tab_plot_degrees_input"
            )
            VedicraDropdownField(
                label = "Main Entrance Direction",
                selectedValue = report.mainEntranceDirection,
                options = VastuConstants.entranceDirections,
                onOptionSelected = { v -> onUpdate { it.copy(mainEntranceDirection = v) } }
            )
            VedicraTextField(
                value = report.mainEntranceNotes,
                onValueChange = { v -> onUpdate { it.copy(mainEntranceNotes = v) } },
                label = "Main Entrance Degree & Pada Notes",
                placeholder = "e.g. N4 Bhallat ✓ / N5 Soma ✓",
                singleLine = false,
                minLines = 2
            )
            VedicraTextField(
                value = report.propertyObservations,
                onValueChange = { v -> onUpdate { it.copy(propertyObservations = v) } },
                label = "Noted Compass Readings & Site Degree Observations",
                placeholder = "Tap 'Note to Degrees of Plot' above or type site degree notes here",
                singleLine = false,
                minLines = 3,
                maxLines = 8,
                testTag = "degrees_tab_observations_input"
            )
        }
    }
}

/**
 * Standalone Shakti Chakra Compass modal accessible directly from the Home Dashboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShaktiChakraCompassDialog(
    onDismiss: () -> Unit
) {
    val liveCompass by rememberVastuCompassState(enabled = true)
    var useManualDial by remember(liveCompass.hasHardwareSensors) {
        mutableStateOf(!liveCompass.hasHardwareSensors)
    }
    var manualAzimuthDeg by remember { mutableFloatStateOf(0f) }
    var isLocked by remember { mutableStateOf(false) }
    var lockedReading by remember { mutableStateOf<CompassReading?>(null) }

    val activeReading = remember(liveCompass, useManualDial, manualAzimuthDeg, isLocked, lockedReading) {
        val current = if (useManualDial || !liveCompass.hasHardwareSensors) {
            liveCompass.copy(
                azimuthDeg = manualAzimuthDeg,
                zone = VastuCompassMath.zoneForDegree(manualAzimuthDeg),
                pada = VastuCompassMath.padaForDegree(manualAzimuthDeg),
                accuracy = if (liveCompass.hasHardwareSensors) liveCompass.accuracy else CompassAccuracy.MANUAL
            )
        } else {
            liveCompass
        }
        if (isLocked) lockedReading ?: current else current
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, LocalVedicraColors.current.goldAccent.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .testTag("dashboard_shakti_chakra_dialog")
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
                    Column {
                        Text(
                            text = "SHAKTI CHAKRA COMPASS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "0°/360° North between N4 & N5 • 32 Padas & 16 Zones",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Shakti Chakra")
                    }
                }

                CompassTopDegreeTiltHud(
                    reading = activeReading,
                    isLocked = isLocked,
                    titlePrefix = "SHAKTI CHAKRA",
                    onToggleLock = {
                        if (!isLocked) lockedReading = activeReading
                        isLocked = !isLocked
                    }
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    VastuChakraCanvas(
                        rotationDegrees = -activeReading.azimuthDeg,
                        transparentFill = false,
                        showDeityNames = true,
                        showTopLubberNeedle = true,
                        tiltRollDeg = activeReading.rollDeg,
                        tiltPitchDeg = activeReading.pitchDeg,
                        modifier = Modifier
                            .widthIn(max = 350.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (liveCompass.hasHardwareSensors) {
                        FilterChip(
                            selected = !useManualDial,
                            onClick = { useManualDial = false },
                            label = { Text("Live Sensor") }
                        )
                    }
                    FilterChip(
                        selected = useManualDial || !liveCompass.hasHardwareSensors,
                        onClick = { useManualDial = true },
                        label = { Text("Manual Dial") }
                    )
                }

                if (useManualDial || !liveCompass.hasHardwareSensors) {
                    Slider(
                        value = manualAzimuthDeg,
                        onValueChange = { manualAzimuthDeg = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
