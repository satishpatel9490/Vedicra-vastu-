package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.repository.VastuRepository
import com.example.ui.components.PrimaryDarkGreenButton
import com.example.ui.components.VedicraTextField
import com.example.ui.theme.LocalVedicraColors
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    mediaId: String,
    repository: VastuRepository,
    onBack: () -> Unit,
    onOpenWorkspace: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val media by repository.observeMediaById(mediaId).collectAsStateWithLifecycle(initialValue = null)
    val brandColors = LocalVedicraColors.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showAnnotated by remember { mutableStateOf(true) }
    var showEditCaptionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = media?.drawingTitle?.ifBlank { media?.caption?.ifBlank { "Site Image" } } ?: "Viewer",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.FitScreen, contentDescription = "Fit to Screen")
                    }
                    media?.let { m ->
                        IconButton(
                            onClick = {
                                scope.launch {
                                    repository.updateMedia(m.copy(rotationDegrees = (m.rotationDegrees + 90) % 360))
                                }
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90")
                        }
                        IconButton(
                            onClick = { showEditCaptionDialog = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Caption")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.85f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        val current = media
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val annotatedPath = current.annotatedFilePath
            val hasAnnotated = !annotatedPath.isNullOrBlank() && File(annotatedPath).exists()
            val displayPath = if (showAnnotated && hasAnnotated) annotatedPath else current.filePath
            val rotation = if (showAnnotated && hasAnnotated) 0f else current.rotationDegrees.toFloat()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 6f)
                                scale = nextScale
                                if (nextScale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(displayPath),
                        contentDescription = current.caption.ifBlank { "Full Screen Site Photograph" },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .rotate(rotation)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (current.caption.isNotBlank() || current.description.isNotBlank()) {
                            Text(
                                text = current.caption.ifBlank { current.drawingTitle },
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (current.description.isNotBlank()) {
                                Text(
                                    text = current.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = current.includeInPdf,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            repository.updateMedia(current.copy(includeInPdf = checked))
                                        }
                                    }
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Include in PDF", style = MaterialTheme.typography.labelMedium)
                            }

                            if (hasAnnotated) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = showAnnotated,
                                        onCheckedChange = { showAnnotated = it }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Annotated", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch { repository.setMainSiteLayout(current.reportId, current.id) }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (current.isMainSiteLayout) Icons.Filled.Star else Icons.Filled.StarOutline,
                                    contentDescription = null,
                                    tint = if (current.isMainSiteLayout) brandColors.goldAccent else MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (current.isMainSiteLayout) "Main Layout" else "Set Main")
                            }

                            PrimaryDarkGreenButton(
                                text = "Grid & Annotate",
                                icon = Icons.Filled.GridOn,
                                onClick = { onOpenWorkspace(current.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (showEditCaptionDialog) {
                var caption by remember(current) { mutableStateOf(current.caption) }
                var description by remember(current) { mutableStateOf(current.description) }
                AlertDialog(
                    onDismissRequest = { showEditCaptionDialog = false },
                    title = { Text("Edit Caption & Description") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            VedicraTextField(value = caption, onValueChange = { caption = it }, label = "Caption")
                            VedicraTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = "Description",
                                singleLine = false,
                                minLines = 2
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.updateMedia(current.copy(caption = caption, description = description))
                                    showEditCaptionDialog = false
                                }
                            }
                        ) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditCaptionDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}
