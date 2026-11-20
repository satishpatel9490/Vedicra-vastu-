package com.example.ui.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.JsonCodec
import com.example.data.local.ReportEntity
import com.example.model.ClientProblemItem
import com.example.model.VastuConstants
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VedicraSectionCard
import com.example.ui.components.VedicraTextField
import com.example.ui.theme.LocalVedicraColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwarAndLandEnergySection(
    report: ReportEntity,
    onUpdate: ((ReportEntity) -> ReportEntity) -> Unit,
    onToggleGuna: (String) -> Unit
) {
    val selectedGunas = remember(report.landEnergyGunasJson) {
        JsonCodec.decodeStringList(report.landEnergyGunasJson)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VedicraSectionCard(
            title = "SWAR CHECK – WHILE ENTERING THE PREMISES",
            subtitle = "Record active nostril / nadi at entry threshold",
            icon = Icons.Filled.Air
        ) {
            VastuConstants.swarOptions.forEach { option ->
                val selected = report.swarNadi == option
                Surface(
                    onClick = { onUpdate { it.copy(swarNadi = option) } },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onUpdate { it.copy(swarNadi = option) } }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VedicraTextField(
                    value = report.swarObservationTime,
                    onValueChange = { v -> onUpdate { it.copy(swarObservationTime = v) } },
                    label = "Observation Time",
                    placeholder = "e.g. 10:35 AM",
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                        onUpdate { it.copy(swarObservationTime = now) }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Now")
                }
            }

            VedicraTextField(
                value = report.swarNotes,
                onValueChange = { v -> onUpdate { it.copy(swarNotes = v) } },
                label = "Additional Swar Notes",
                singleLine = false,
                minLines = 2
            )
        }

        VedicraSectionCard(
            title = "LAND ENERGY / GUNA ANALYSIS",
            subtitle = "Select one or more Gunas observed on the premises",
            icon = Icons.Filled.Landscape
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VastuConstants.landEnergyGunas.forEach { guna ->
                    val selected = guna in selectedGunas
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleGuna(guna) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        label = { Text(guna) }
                    )
                }
            }
            VedicraTextField(
                value = report.landEnergyExplanation,
                onValueChange = { v -> onUpdate { it.copy(landEnergyExplanation = v) } },
                label = "Additional Land Energy Explanations",
                placeholder = "Describe soil energy, surroundings, vibrations, or observations",
                singleLine = false,
                minLines = 3,
                maxLines = 6
            )
        }
    }
}
