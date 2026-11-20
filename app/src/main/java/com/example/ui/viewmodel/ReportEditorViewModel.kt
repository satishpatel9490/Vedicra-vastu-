package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AnalysisRowEntity
import com.example.data.local.ExportedPdfEntity
import com.example.data.local.FullReportBundle
import com.example.data.local.JsonCodec
import com.example.data.local.ReportEntity
import com.example.data.local.SiteMediaEntity
import com.example.data.repository.VastuRepository
import com.example.model.AppSettings
import com.example.model.ClientProblemItem
import com.example.model.EntranceDetail
import com.example.model.MediaKind
import com.example.model.VastuConstants
import com.example.pdf.PdfReportGenerator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface EditorEvent {
    data class Message(val text: String) : EditorEvent
    data class PdfReady(val pdf: ExportedPdfEntity) : EditorEvent
    data object ReportDeleted : EditorEvent
}

@OptIn(FlowPreview::class)
class ReportEditorViewModel(
    private val reportId: String,
    private val repository: VastuRepository,
    private val pdfGenerator: PdfReportGenerator
) : ViewModel() {

    val bundle: StateFlow<FullReportBundle?> = repository.observeReportBundle(reportId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _draftReport = MutableStateFlow<ReportEntity?>(null)
    val draftReport: StateFlow<ReportEntity?> = _draftReport.asStateFlow()

    private val _draftRows = MutableStateFlow<List<AnalysisRowEntity>>(emptyList())
    val draftRows: StateFlow<List<AnalysisRowEntity>> = _draftRows.asStateFlow()

    private val pendingRowUpdates = LinkedHashMap<String, AnalysisRowEntity>()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    private val _lastSavedTimestamp = MutableStateFlow<Long?>(null)
    val lastSavedTimestamp: StateFlow<Long?> = _lastSavedTimestamp.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events = _events.asSharedFlow()

    private val autoSaveTrigger = MutableSharedFlow<ReportEntity>(extraBufferCapacity = 32)
    private val rowAutoSaveTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 32)

    init {
        viewModelScope.launch {
            bundle.collect { currentBundle ->
                if (currentBundle != null && _draftReport.value == null) {
                    _draftReport.value = currentBundle.report
                } else if (currentBundle != null && _draftReport.value != null) {
                    // Sync PDF fields if updated externally
                    val d = _draftReport.value!!
                    if (d.lastPdfPath != currentBundle.report.lastPdfPath ||
                        d.lastPdfGeneratedAt != currentBundle.report.lastPdfGeneratedAt
                    ) {
                        _draftReport.value = d.copy(
                            lastPdfPath = currentBundle.report.lastPdfPath,
                            lastPdfGeneratedAt = currentBundle.report.lastPdfGeneratedAt
                        )
                    }
                }

                if (currentBundle != null) {
                    val merged = currentBundle.orderedAnalysisRows.map { dbRow ->
                        pendingRowUpdates[dbRow.id] ?: dbRow
                    }
                    _draftRows.value = merged
                }
            }
        }

        viewModelScope.launch {
            autoSaveTrigger
                .debounce(450)
                .collect { reportToSave ->
                    repository.saveReport(reportToSave, markAsSaved = false)
                    _lastSavedTimestamp.value = System.currentTimeMillis()
                }
        }

        viewModelScope.launch {
            rowAutoSaveTrigger
                .debounce(350)
                .collect {
                    flushPendingRowUpdates()
                }
        }
    }

    private suspend fun flushPendingRowUpdates() {
        val snapshot = synchronized(pendingRowUpdates) {
            if (pendingRowUpdates.isEmpty()) emptyList()
            else pendingRowUpdates.values.toList().also { pendingRowUpdates.clear() }
        }
        snapshot.forEach { row ->
            repository.upsertAnalysisRow(row)
        }
        if (snapshot.isNotEmpty()) {
            _lastSavedTimestamp.value = System.currentTimeMillis()
        }
    }

    fun updateReportField(transform: (ReportEntity) -> ReportEntity) {
        val current = _draftReport.value ?: bundle.value?.report ?: return
        val updated = transform(current).copy(updatedAt = System.currentTimeMillis())
        _draftReport.value = updated
        autoSaveTrigger.tryEmit(updated)
    }

    fun saveReportExplicitly() {
        val current = _draftReport.value ?: bundle.value?.report ?: return
        viewModelScope.launch {
            flushPendingRowUpdates()
            val saved = current.copy(isDraft = false, updatedAt = System.currentTimeMillis())
            _draftReport.value = saved
            repository.saveReport(saved, markAsSaved = true)
            _lastSavedTimestamp.value = System.currentTimeMillis()
            _events.emit(EditorEvent.Message("Report saved successfully"))
        }
    }

    fun resetCurrentReportForm() {
        viewModelScope.launch {
            synchronized(pendingRowUpdates) { pendingRowUpdates.clear() }
            val fresh = repository.resetUnsavedForm(reportId)
            _draftReport.value = fresh
            _draftRows.value = emptyList()
            _events.emit(EditorEvent.Message("Current form cleared"))
        }
    }

    fun deleteCurrentReport() {
        viewModelScope.launch {
            repository.deleteReportCompletely(reportId)
            _events.emit(EditorEvent.ReportDeleted)
        }
    }

    // Entrances helpers
    fun addAdditionalEntrance(entrance: EntranceDetail) {
        updateReportField { r ->
            val list = JsonCodec.decodeEntrances(r.additionalEntrancesJson).toMutableList()
            list.add(entrance)
            r.copy(additionalEntrancesJson = JsonCodec.encodeEntrances(list))
        }
    }

    fun updateAdditionalEntrance(entrance: EntranceDetail) {
        updateReportField { r ->
            val list = JsonCodec.decodeEntrances(r.additionalEntrancesJson).map {
                if (it.id == entrance.id) entrance else it
            }
            r.copy(additionalEntrancesJson = JsonCodec.encodeEntrances(list))
        }
    }

    fun deleteAdditionalEntrance(entranceId: String) {
        updateReportField { r ->
            val list = JsonCodec.decodeEntrances(r.additionalEntrancesJson).filterNot { it.id == entranceId }
            r.copy(additionalEntrancesJson = JsonCodec.encodeEntrances(list))
        }
    }

    // Land Energy Gunas
    fun toggleGuna(guna: String) {
        updateReportField { r ->
            val list = JsonCodec.decodeStringList(r.landEnergyGunasJson).toMutableList()
            if (guna in list) list.remove(guna) else list.add(guna)
            r.copy(landEnergyGunasJson = JsonCodec.encodeStringList(list))
        }
    }

    // Client Problems (synced with unified category-wise Analysis & Remedies table)
    fun togglePredefinedProblem(title: String) {
        val currentBundle = bundle.value
        val alreadySelected = JsonCodec.decodeProblems(_draftReport.value?.selectedProblemsJson ?: "[]")
            .any { it.title == title && !it.isCustom }

        updateReportField { r ->
            val list = JsonCodec.decodeProblems(r.selectedProblemsJson).toMutableList()
            val existing = list.firstOrNull { it.title == title && !it.isCustom }
            if (existing != null) {
                list.remove(existing)
            } else {
                list.add(ClientProblemItem(title = title, isCustom = false))
            }
            r.copy(selectedProblemsJson = JsonCodec.encodeProblems(list))
        }

        viewModelScope.launch {
            val rows = currentBundle?.orderedAnalysisRows ?: emptyList()
            if (!alreadySelected) {
                if (rows.none { it.clientProblem.equals(title, ignoreCase = true) }) {
                    repository.addAnalysisRow(
                        reportId = reportId,
                        initialProblem = title,
                        problemCategory = VastuConstants.categoryForProblem(title)
                    )
                }
            } else {
                // Remove unedited placeholder row if user untoggles chip
                rows.filter {
                    it.clientProblem.equals(title, ignoreCase = true) &&
                        it.responsibleDirectionZone.isBlank() &&
                        it.problemObserved.isBlank() &&
                        it.suggestedRemedy.isBlank() &&
                        it.antiActivity.isBlank() &&
                        it.antiUtility.isBlank() &&
                        it.antiObjects.isBlank() &&
                        it.notes.isBlank()
                }.forEach { emptyRow ->
                    repository.deleteAnalysisRow(emptyRow)
                }
            }
        }
    }

    fun addCustomProblem(title: String, category: String = "") {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return
        val resolvedCategory = category.ifBlank { VastuConstants.categoryForProblem(trimmed) }
        updateReportField { r ->
            val list = JsonCodec.decodeProblems(r.selectedProblemsJson).toMutableList()
            if (list.none { it.title.equals(trimmed, ignoreCase = true) }) {
                list.add(ClientProblemItem(title = trimmed, isCustom = true))
            }
            r.copy(selectedProblemsJson = JsonCodec.encodeProblems(list))
        }
        viewModelScope.launch {
            val rows = bundle.value?.orderedAnalysisRows ?: emptyList()
            if (rows.none { it.clientProblem.equals(trimmed, ignoreCase = true) }) {
                repository.addAnalysisRow(
                    reportId = reportId,
                    initialProblem = trimmed,
                    problemCategory = resolvedCategory
                )
            }
        }
    }

    fun editProblem(problemId: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isBlank()) return
        updateReportField { r ->
            val list = JsonCodec.decodeProblems(r.selectedProblemsJson).map {
                if (it.id == problemId) it.copy(title = trimmed) else it
            }
            r.copy(selectedProblemsJson = JsonCodec.encodeProblems(list))
        }
    }

    fun deleteProblem(problemId: String) {
        updateReportField { r ->
            val list = JsonCodec.decodeProblems(r.selectedProblemsJson).filterNot { it.id == problemId }
            r.copy(selectedProblemsJson = JsonCodec.encodeProblems(list))
        }
    }

    // Consultant Notes
    fun updateConsultantNote(index: Int, text: String) {
        updateReportField { r ->
            val notes = JsonCodec.decodeStringList(r.consultantNotesJson).toMutableList()
            while (notes.size <= index) notes.add("")
            notes[index] = text
            r.copy(consultantNotesJson = JsonCodec.encodeStringList(notes))
        }
    }

    fun addMoreConsultantNote() {
        updateReportField { r ->
            val notes = JsonCodec.decodeStringList(r.consultantNotesJson).toMutableList()
            notes.add("")
            r.copy(consultantNotesJson = JsonCodec.encodeStringList(notes))
        }
    }

    fun deleteConsultantNote(index: Int) {
        updateReportField { r ->
            val notes = JsonCodec.decodeStringList(r.consultantNotesJson).toMutableList()
            if (index in notes.indices) notes.removeAt(index)
            r.copy(consultantNotesJson = JsonCodec.encodeStringList(notes))
        }
    }

    // Unified Problems, Analysis & Remedies Table CRUD
    fun addAnalysisRow(initialProblem: String = "", category: String = "") {
        viewModelScope.launch {
            flushPendingRowUpdates()
            val added = repository.addAnalysisRow(reportId, initialProblem, category)
            _draftRows.value = (_draftRows.value + added).sortedBy { it.sortOrder }
        }
    }

    fun updateAnalysisRow(row: AnalysisRowEntity) {
        val normalizedCategory = row.problemCategory.ifBlank {
            if (row.clientProblem.isNotBlank()) VastuConstants.categoryForProblem(row.clientProblem)
            else VastuConstants.problemCategories.first()
        }
        val normalizedRow = row.copy(problemCategory = normalizedCategory)

        _draftRows.value = _draftRows.value.map {
            if (it.id == normalizedRow.id) normalizedRow else it
        }
        synchronized(pendingRowUpdates) {
            pendingRowUpdates[normalizedRow.id] = normalizedRow
        }
        syncSelectedProblemsFromRows(_draftRows.value)
        rowAutoSaveTrigger.tryEmit(Unit)
    }

    private fun syncSelectedProblemsFromRows(rows: List<AnalysisRowEntity>) {
        val titles = rows.mapNotNull { r ->
            r.clientProblem.trim().takeIf { it.isNotBlank() }
        }.distinctBy { it.lowercase() }
        updateReportField { r ->
            val currentList = JsonCodec.decodeProblems(r.selectedProblemsJson)
            val synced = titles.map { title ->
                val existing = currentList.firstOrNull { it.title.equals(title, ignoreCase = true) }
                existing ?: ClientProblemItem(
                    title = title,
                    isCustom = VastuConstants.predefinedProblems.none { it.equals(title, ignoreCase = true) }
                )
            }
            r.copy(selectedProblemsJson = JsonCodec.encodeProblems(synced))
        }
    }

    fun duplicateAnalysisRow(row: AnalysisRowEntity) {
        viewModelScope.launch {
            flushPendingRowUpdates()
            repository.duplicateAnalysisRow(row)
            _events.emit(EditorEvent.Message("Row duplicated"))
        }
    }

    fun deleteAnalysisRow(row: AnalysisRowEntity) {
        synchronized(pendingRowUpdates) {
            pendingRowUpdates.remove(row.id)
        }
        val remaining = _draftRows.value.filterNot { it.id == row.id }
        _draftRows.value = remaining
        syncSelectedProblemsFromRows(remaining)
        viewModelScope.launch {
            repository.deleteAnalysisRow(row)
        }
    }

    fun toggleRowMediaAttachment(row: AnalysisRowEntity, mediaId: String) {
        val currentIds = JsonCodec.decodeStringList(row.attachedMediaIdsJson).toMutableList()
        if (mediaId in currentIds) currentIds.remove(mediaId) else currentIds.add(mediaId)
        updateAnalysisRow(row.copy(attachedMediaIdsJson = JsonCodec.encodeStringList(currentIds)))
    }

    // Site Media & Drawings
    fun importPhotosFromGallery(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val added = repository.importGalleryImages(reportId, uris, MediaKind.PHOTO)
            _events.emit(EditorEvent.Message("Imported ${added.size} photograph(s)"))
        }
    }

    fun importDrawingFromGallery(
        uri: Uri,
        drawingCategory: String,
        drawingTitle: String,
        floorNumber: String,
        orientationDirection: String,
        description: String,
        additionalNotes: String
    ) {
        viewModelScope.launch {
            val added = repository.importGalleryImages(
                reportId = reportId,
                uris = listOf(uri),
                mediaKind = MediaKind.DRAWING,
                drawingCategory = drawingCategory,
                drawingTitle = drawingTitle,
                floorNumber = floorNumber,
                orientationDirection = orientationDirection,
                description = description,
                additionalNotes = additionalNotes
            )
            if (added.isNotEmpty()) {
                _events.emit(EditorEvent.Message("Drawing added to report"))
            }
        }
    }

    fun updateMediaDetails(media: SiteMediaEntity) {
        viewModelScope.launch {
            repository.updateMedia(media)
        }
    }

    fun rotateMedia(media: SiteMediaEntity) {
        viewModelScope.launch {
            val nextDeg = (media.rotationDegrees + 90) % 360
            repository.updateMedia(media.copy(rotationDegrees = nextDeg))
        }
    }

    fun setMainSiteLayout(media: SiteMediaEntity) {
        viewModelScope.launch {
            repository.setMainSiteLayout(reportId, media.id)
            _events.emit(EditorEvent.Message("Marked as Main Site Layout"))
        }
    }

    fun moveMediaOrder(media: SiteMediaEntity, moveUp: Boolean) {
        viewModelScope.launch {
            val kind = runCatching { MediaKind.valueOf(media.mediaKind) }.getOrDefault(MediaKind.PHOTO)
            repository.moveMediaOrder(reportId, media.id, moveUp, kind)
        }
    }

    fun deleteMedia(media: SiteMediaEntity) {
        viewModelScope.launch {
            repository.deleteMediaItem(media)
            _events.emit(EditorEvent.Message("Photograph deleted"))
        }
    }

    // PDF Generation
    fun generatePdf() {
        val currentDraft = _draftReport.value ?: bundle.value?.report ?: return
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            // Ensure latest edits (report fields & inline rows) are flushed to Room first
            flushPendingRowUpdates()
            val savedReport = currentDraft.copy(isDraft = false, updatedAt = System.currentTimeMillis())
            _draftReport.value = savedReport
            repository.saveReport(savedReport, markAsSaved = true)

            val freshBundle = repository.getReportBundle(reportId)
            if (freshBundle == null) {
                _isGeneratingPdf.value = false
                _events.emit(EditorEvent.Message("Unable to load report data for PDF"))
                return@launch
            }

            val result = pdfGenerator.generateReportPdf(freshBundle, settings.value)
            _isGeneratingPdf.value = false

            result.onSuccess { pdf ->
                repository.recordExportedPdf(pdf)
                _events.emit(EditorEvent.PdfReady(pdf))
            }.onFailure { err ->
                _events.emit(EditorEvent.Message("PDF error: ${err.message}"))
            }
        }
    }

    // Camera capture direct helpers
    fun createCameraOutputFile(): File =
        repository.mediaManager.createOriginalImageFile(reportId, "cam")

    fun saveCameraCapture(
        file: File,
        mediaKind: MediaKind,
        caption: String,
        drawingCategory: String,
        drawingTitle: String,
        floorNumber: String,
        orientationDirection: String,
        description: String,
        additionalNotes: String
    ) {
        viewModelScope.launch {
            repository.addCapturedCameraPhoto(
                reportId = reportId,
                file = file,
                mediaKind = mediaKind,
                caption = caption,
                drawingCategory = drawingCategory,
                drawingTitle = drawingTitle,
                floorNumber = floorNumber,
                orientationDirection = orientationDirection,
                description = description,
                additionalNotes = additionalNotes
            )
        }
    }

    class Factory(
        private val reportId: String,
        private val repository: VastuRepository,
        private val pdfGenerator: PdfReportGenerator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportEditorViewModel(reportId, repository, pdfGenerator) as T
        }
    }
}
