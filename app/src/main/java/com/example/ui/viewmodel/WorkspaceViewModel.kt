package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.JsonCodec
import com.example.data.local.SiteMediaEntity
import com.example.data.repository.VastuRepository
import com.example.model.LayoutAnnotation
import com.example.model.WorkspaceMetadata
import com.example.pdf.WorkspaceRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface WorkspaceEvent {
    data class Message(val text: String) : WorkspaceEvent
    data class ExportReady(val file: File) : WorkspaceEvent
}

class WorkspaceViewModel(
    private val mediaId: String,
    private val repository: VastuRepository
) : ViewModel() {

    private val _media = MutableStateFlow<SiteMediaEntity?>(null)
    val media: StateFlow<SiteMediaEntity?> = _media.asStateFlow()

    private val _baseBitmap = MutableStateFlow<Bitmap?>(null)
    val baseBitmap: StateFlow<Bitmap?> = _baseBitmap.asStateFlow()

    private val _workspaceMeta = MutableStateFlow(WorkspaceMetadata())
    val workspaceMeta: StateFlow<WorkspaceMetadata> = _workspaceMeta.asStateFlow()

    private val _selectedAnnotationId = MutableStateFlow<String?>(null)
    val selectedAnnotationId: StateFlow<String?> = _selectedAnnotationId.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _events = MutableSharedFlow<WorkspaceEvent>()
    val events = _events.asSharedFlow()

    private val undoStack = mutableListOf<WorkspaceMetadata>()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            val loaded = repository.getMediaById(mediaId) ?: return@launch
            _media.value = loaded
            _workspaceMeta.value = JsonCodec.decodeWorkspaceMetadata(loaded.annotationsJson)
            reloadBitmap(loaded)
        }
    }

    private suspend fun reloadBitmap(entity: SiteMediaEntity) = withContext(Dispatchers.IO) {
        val bmp = repository.mediaManager.loadRotatedBitmap(
            filePath = entity.filePath,
            extraRotationDegrees = entity.rotationDegrees,
            maxDimension = 2048
        )
        _baseBitmap.value = bmp
    }

    fun rotateImage90() {
        val current = _media.value ?: return
        viewModelScope.launch {
            val nextRotation = (current.rotationDegrees + 90) % 360
            val updated = current.copy(rotationDegrees = nextRotation)
            _media.value = updated
            repository.updateMedia(updated)
            reloadBitmap(updated)
        }
    }

    fun toggleGridOverlay() {
        pushUndo()
        _workspaceMeta.value = _workspaceMeta.value.copy(
            showGridOverlay = !_workspaceMeta.value.showGridOverlay
        )
    }

    fun setGridRotation(degrees: Float) {
        _workspaceMeta.value = _workspaceMeta.value.copy(
            gridRotationDegrees = com.example.model.VastuCompassMath.normalizeDegrees(degrees)
        )
    }

    fun setOverlayStyle(style: com.example.model.GridOverlayStyle) {
        pushUndo()
        _workspaceMeta.value = _workspaceMeta.value.copy(
            showGridOverlay = true,
            overlayStyle = style
        )
    }

    fun setChakraType(chakraType: com.example.model.VastuChakraType) {
        pushUndo()
        _workspaceMeta.value = _workspaceMeta.value.copy(
            showGridOverlay = true,
            chakraType = chakraType
        )
    }

    fun updateCropRect(left: Float, top: Float, right: Float, bottom: Float) {
        val cLeft = left.coerceIn(0f, 0.85f)
        val cTop = top.coerceIn(0f, 0.85f)
        val cRight = right.coerceIn(cLeft + 0.1f, 1f)
        val cBottom = bottom.coerceIn(cTop + 0.1f, 1f)
        _workspaceMeta.value = _workspaceMeta.value.copy(
            cropLeft = cLeft,
            cropTop = cTop,
            cropRight = cRight,
            cropBottom = cBottom
        )
    }

    fun resetCrop() {
        pushUndo()
        _workspaceMeta.value = _workspaceMeta.value.copy(
            cropLeft = 0f,
            cropTop = 0f,
            cropRight = 1f,
            cropBottom = 1f
        )
    }

    fun addEraseStroke(stroke: com.example.model.EraseBrushStroke) {
        if (stroke.points.isEmpty()) return
        pushUndo()
        _workspaceMeta.value = _workspaceMeta.value.copy(
            eraseStrokes = _workspaceMeta.value.eraseStrokes + stroke
        )
    }

    fun clearEraseStrokes() {
        pushUndo()
        _workspaceMeta.value = _workspaceMeta.value.copy(eraseStrokes = emptyList())
    }

    fun selectAnnotation(id: String?) {
        _selectedAnnotationId.value = id
    }

    fun addAnnotation(annotation: LayoutAnnotation) {
        pushUndo()
        val updated = _workspaceMeta.value.annotations + annotation
        _workspaceMeta.value = _workspaceMeta.value.copy(annotations = updated)
        _selectedAnnotationId.value = annotation.id
    }

    fun updateAnnotation(annotation: LayoutAnnotation, recordUndo: Boolean = false) {
        if (recordUndo) pushUndo()
        val updated = _workspaceMeta.value.annotations.map {
            if (it.id == annotation.id) annotation else it
        }
        _workspaceMeta.value = _workspaceMeta.value.copy(annotations = updated)
    }

    fun deleteSelectedAnnotation() {
        val id = _selectedAnnotationId.value ?: return
        pushUndo()
        val updated = _workspaceMeta.value.annotations.filterNot { it.id == id }
        _workspaceMeta.value = _workspaceMeta.value.copy(annotations = updated)
        _selectedAnnotationId.value = null
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            _workspaceMeta.value = undoStack.removeAt(undoStack.lastIndex)
        }
    }

    fun clearAllAnnotations() {
        pushUndo()
        _workspaceMeta.value = WorkspaceMetadata(
            showGridOverlay = false,
            gridRotationDegrees = 0f,
            annotations = emptyList()
        )
        _selectedAnnotationId.value = null
    }

    private fun pushUndo() {
        undoStack.add(_workspaceMeta.value)
        if (undoStack.size > 25) undoStack.removeAt(0)
    }

    fun saveAnnotatedVersion(shareAfterSave: Boolean = false) {
        val currentMedia = _media.value ?: return
        val sourceBitmap = _baseBitmap.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val resultFile = withContext(Dispatchers.IO) {
                runCatching {
                    val rendered = WorkspaceRenderer.renderAnnotatedBitmap(sourceBitmap, _workspaceMeta.value)
                    val outFile = repository.mediaManager.createAnnotatedImageFile(currentMedia.reportId)
                    FileOutputStream(outFile).use { fos ->
                        rendered.compress(Bitmap.CompressFormat.PNG, 95, fos)
                    }
                    rendered.recycle()

                    // Remove previous annotated copy only (original untouched!)
                    repository.mediaManager.deleteAppManagedFile(currentMedia.annotatedFilePath)

                    val updatedMedia = currentMedia.copy(
                        annotatedFilePath = outFile.absolutePath,
                        annotationsJson = JsonCodec.encodeWorkspaceMetadata(_workspaceMeta.value)
                    )
                    repository.updateMedia(updatedMedia)
                    _media.value = updatedMedia
                    outFile
                }.getOrNull()
            }
            _isSaving.value = false
            if (resultFile != null) {
                if (shareAfterSave) {
                    _events.emit(WorkspaceEvent.ExportReady(resultFile))
                } else {
                    _events.emit(WorkspaceEvent.Message("Annotated layout saved as new version (original preserved)"))
                }
            } else {
                _events.emit(WorkspaceEvent.Message("Failed to save annotated layout"))
            }
        }
    }

    class Factory(
        private val mediaId: String,
        private val repository: VastuRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkspaceViewModel(mediaId, repository) as T
        }
    }
}
