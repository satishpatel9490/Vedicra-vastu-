package com.example.ui.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.JsonCodec
import com.example.data.local.ReportEntity
import com.example.model.EntranceDetail
import com.example.model.VastuConstants
import com.example.ui.components.VedicraDropdownField
import com.example.ui.components.VedicraSectionCard
import com.example.ui.components.VedicraTextField
import com.example.ui.theme.LocalVedicraColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClientDetailsSection(
    report: ReportEntity,
    onUpdate: ((ReportEntity) -> ReportEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VedicraSectionCard(
            title = "Client Details",
            subtitle = "Inspection Date: ${dateFormat.format(Date(report.inspectionDate))} • Report ${report.reportNumber}",
            icon = Icons.Filled.Person
        ) {
            VedicraTextField(
                value = report.clientName,
                onValueChange = { v -> onUpdate { it.copy(clientName = v) } },
                label = "Client Name *",
                placeholder = "Enter full name",
                testTag = "client_name_input"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VedicraTextField(
                    value = report.mobileNumber,
                    onValueChange = { v -> onUpdate { it.copy(mobileNumber = v) } },
                    label = "Mobile Number",
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.weight(1f),
                    testTag = "client_mobile_input"
                )
                VedicraTextField(
                    value = report.alternateNumber,
                    onValueChange = { v -> onUpdate { it.copy(alternateNumber = v) } },
                    label = "Alternate Number",
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.weight(1f)
                )
            }
            VedicraTextField(
                value = report.address,
                onValueChange = { v -> onUpdate { it.copy(address = v) } },
                label = "Address",
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VedicraTextField(
                    value = report.areaLocality,
                    onValueChange = { v -> onUpdate { it.copy(areaLocality = v) } },
                    label = "Area / Locality",
                    modifier = Modifier.weight(1f)
                )
                VedicraTextField(
                    value = report.city,
                    onValueChange = { v -> onUpdate { it.copy(city = v) } },
                    label = "City",
                    modifier = Modifier.weight(1f),
                    testTag = "client_city_input"
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VedicraTextField(
                    value = report.state,
                    onValueChange = { v -> onUpdate { it.copy(state = v) } },
                    label = "State",
                    modifier = Modifier.weight(1f)
                )
                VedicraTextField(
                    value = report.pinCode,
                    onValueChange = { v -> onUpdate { it.copy(pinCode = v) } },
                    label = "PIN Code",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        VedicraSectionCard(
            title = "Birth Details (Optional)",
            subtitle = "Useful for astrological / Vastu correlation if provided by client",
            icon = Icons.Filled.Cake
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VedicraTextField(
                    value = report.birthDate,
                    onValueChange = { v -> onUpdate { it.copy(birthDate = v) } },
                    label = "Date of Birth",
                    placeholder = "DD/MM/YYYY",
                    modifier = Modifier.weight(1f)
                )
                VedicraTextField(
                    value = report.birthTime,
                    onValueChange = { v -> onUpdate { it.copy(birthTime = v) } },
                    label = "Birth Time",
                    placeholder = "e.g. 07:45 AM",
                    modifier = Modifier.weight(1f)
                )
            }
            VedicraTextField(
                value = report.birthLocation,
                onValueChange = { v -> onUpdate { it.copy(birthLocation = v) } },
                label = "Birth Location",
                placeholder = "City, State"
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertyDetailsSection(
    report: ReportEntity,
    onUpdate: ((ReportEntity) -> ReportEntity) -> Unit,
    onAddEntrance: (EntranceDetail) -> Unit,
    onUpdateEntrance: (EntranceDetail) -> Unit,
    onDeleteEntrance: (String) -> Unit,
    onOpenCompassTab: () -> Unit = {}
) {
    val brandColors = LocalVedicraColors.current
    val additionalEntrances = remember(report.additionalEntrancesJson) {
        JsonCodec.decodeEntrances(report.additionalEntrancesJson)
    }
    var editingEntrance by remember { mutableStateOf<EntranceDetail?>(null) }
    var isNewEntrance by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VedicraSectionCard(
            title = "Property Details",
            subtitle = "Site classification, orientation & compass degrees",
            icon = Icons.Filled.HomeWork
        ) {
            Text(
                text = "Property Type",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VastuConstants.propertyTypes.forEach { type ->
                    FilterChip(
                        selected = report.propertyType == type,
                        onClick = { onUpdate { it.copy(propertyType = type) } },
                        label = { Text(type) }
                    )
                }
            }
            if (report.propertyType == "Other") {
                VedicraTextField(
                    value = report.propertyTypeOther,
                    onValueChange = { v -> onUpdate { it.copy(propertyTypeOther = v) } },
                    label = "Specify Property Type"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VedicraTextField(
                    value = report.plotDegrees,
                    onValueChange = { v -> onUpdate { it.copy(plotDegrees = v) } },
                    label = "Degrees of Plot",
                    placeholder = "e.g. 0° N / 42° NE",
                    modifier = Modifier.weight(1f)
                )
                VedicraDropdownField(
                    label = "Plot Shape",
                    selectedValue = report.plotShape.ifBlank { "Select Shape" },
                    options = VastuConstants.plotShapes,
                    onOptionSelected = { v -> onUpdate { it.copy(plotShape = v) } },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedButton(
                onClick = onOpenCompassTab,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Measure & Note Degrees Using Shakti Chakra Compass")
            }

            VedicraTextField(
                value = report.buildingOrientation,
                onValueChange = { v -> onUpdate { it.copy(buildingOrientation = v) } },
                label = "Building Orientation",
                placeholder = "e.g. East-facing parallel to road"
            )

            VedicraTextField(
                value = report.propertyObservations,
                onValueChange = { v -> onUpdate { it.copy(propertyObservations = v) } },
                label = "Additional Property Observations",
                singleLine = false,
                minLines = 3,
                maxLines = 6
            )
        }

        VedicraSectionCard(
            title = "Entrance Directions",
            subtitle = "Main entrance and additional site entrances",
            icon = Icons.Filled.DoorFront,
            headerTrailing = {
                OutlinedButton(
                    onClick = {
                        isNewEntrance = true
                        editingEntrance = EntranceDetail(
                            direction = "East",
                            label = "Side / Rear Gate"
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Entrance")
                }
            }
        ) {
            VedicraDropdownField(
                label = "Main Entrance Direction",
                selectedValue = report.mainEntranceDirection,
                options = VastuConstants.entranceDirections,
                onOptionSelected = { v -> onUpdate { it.copy(mainEntranceDirection = v) } }
            )
            VedicraTextField(
                value = report.mainEntranceNotes,
                onValueChange = { v -> onUpdate { it.copy(mainEntranceNotes = v) } },
                label = "Main Entrance Notes / Pada",
                placeholder = "e.g. E3/E4 pada, wooden threshold",
                singleLine = false,
                minLines = 2
            )

            if (additionalEntrances.isNotEmpty()) {
                Text(
                    text = "Additional Entrance Details (${additionalEntrances.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                additionalEntrances.forEach { entrance ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, brandColors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${entrance.label.ifBlank { "Entrance" }} — ${entrance.direction}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                val detailText = buildString {
                                    if (entrance.degrees.isNotBlank()) append("Degrees: ${entrance.degrees}  ")
                                    if (entrance.notes.isNotBlank()) append(entrance.notes)
                                }
                                if (detailText.isNotBlank()) {
                                    Text(
                                        text = detailText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        isNewEntrance = false
                                        editingEntrance = entrance
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Entrance")
                                }
                                IconButton(
                                    onClick = { onDeleteEntrance(entrance.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete Entrance",
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

    editingEntrance?.let { target ->
        var label by remember(target) { mutableStateOf(target.label) }
        var direction by remember(target) { mutableStateOf(target.direction) }
        var degrees by remember(target) { mutableStateOf(target.degrees) }
        var notes by remember(target) { mutableStateOf(target.notes) }

        AlertDialog(
            onDismissRequest = { editingEntrance = null },
            title = { Text(if (isNewEntrance) "Add Additional Entrance" else "Edit Entrance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    VedicraTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = "Entrance Label",
                        placeholder = "e.g. Side Gate, Service Entry"
                    )
                    VedicraDropdownField(
                        label = "Direction",
                        selectedValue = direction,
                        options = VastuConstants.entranceDirections,
                        onOptionSelected = { direction = it }
                    )
                    VedicraTextField(
                        value = degrees,
                        onValueChange = { degrees = it },
                        label = "Degrees (Optional)",
                        placeholder = "e.g. 92°"
                    )
                    VedicraTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes",
                        singleLine = false,
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val saved = target.copy(
                            label = label,
                            direction = direction,
                            degrees = degrees,
                            notes = notes
                        )
                        if (isNewEntrance) onAddEntrance(saved) else onUpdateEntrance(saved)
                        editingEntrance = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntrance = null }) { Text("Cancel") }
            }
        )
    }
}
