package com.example.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.JsonCodec
import com.example.data.local.ReportEntity
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VedicraSectionCard
import com.example.ui.components.VedicraTextField

@Composable
fun ConsultantNotesSection(
    report: ReportEntity,
    onUpdateNote: (Int, String) -> Unit,
    onAddMoreNote: () -> Unit,
    onDeleteNote: (Int) -> Unit
) {
    val notes = remember(report.consultantNotesJson) {
        val decoded = JsonCodec.decodeStringList(report.consultantNotesJson)
        if (decoded.isEmpty()) List(10) { "" } else decoded
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VedicraSectionCard(
            title = "Consultant Notes",
            subtitle = "Initially 10 editable multiline notes — add unlimited notes as needed",
            icon = Icons.Filled.EditNote
        ) {
            notes.forEachIndexed { idx, noteText ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VedicraTextField(
                        value = noteText,
                        onValueChange = { onUpdateNote(idx, it) },
                        label = "Note ${idx + 1}",
                        singleLine = false,
                        minLines = 2,
                        maxLines = 6,
                        modifier = Modifier.weight(1f),
                        testTag = "consultant_note_${idx + 1}"
                    )
                    IconButton(
                        onClick = { onDeleteNote(idx) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("delete_consultant_note_${idx + 1}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete Note ${idx + 1}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            PrimaryDarkGreenButton(
                text = "Add More Notes",
                icon = Icons.Filled.Add,
                onClick = onAddMoreNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_more_notes_button")
            )
        }
    }
}
