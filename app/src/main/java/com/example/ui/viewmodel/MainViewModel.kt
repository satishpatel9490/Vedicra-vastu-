package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExportedPdfEntity
import com.example.data.local.ReportEntity
import com.example.data.repository.VastuRepository
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.pdf.PdfReportGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiMessageEvent {
    data class ToastMessage(val message: String) : UiMessageEvent
    data class OpenReport(val reportId: String) : UiMessageEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: VastuRepository,
    private val pdfGenerator: PdfReportGenerator
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val savedReports: StateFlow<List<ReportEntity>> = repository.savedReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyEditedReports: StateFlow<List<ReportEntity>> = repository.recentlyEditedReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exportedPdfs: StateFlow<List<ExportedPdfEntity>> = repository.exportedPdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<ReportEntity>> = _searchQuery
        .flatMapLatest { query -> repository.searchReports(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeReportId = MutableStateFlow<String?>(null)
    val activeReportId: StateFlow<String?> = _activeReportId.asStateFlow()

    val activeReport: StateFlow<ReportEntity?> = _activeReportId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.observeReportBundle(id)
        }
        .flatMapLatest { bundle -> flowOf(bundle?.report) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _events = MutableSharedFlow<UiMessageEvent>()
    val events = _events.asSharedFlow()

    fun setActiveReportId(reportId: String?) {
        _activeReportId.value = reportId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateAppTheme(themeMode: AppThemeMode) {
        viewModelScope.launch {
            repository.updateAppTheme(themeMode)
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            _events.emit(UiMessageEvent.ToastMessage("Settings saved"))
        }
    }

    fun importCompanyLogo(uri: Uri) {
        viewModelScope.launch {
            val savedPath = repository.mediaManager.saveCustomCompanyLogo(uri)
            if (savedPath != null) {
                repository.settingsStore.updateCompanyLogoPath(savedPath)
                _events.emit(UiMessageEvent.ToastMessage("Company logo updated"))
            } else {
                _events.emit(UiMessageEvent.ToastMessage("Failed to load selected logo"))
            }
        }
    }

    fun resetCompanyLogoToDefault() {
        viewModelScope.launch {
            val current = settings.value.companyLogoPath
            if (current.isNotBlank()) repository.mediaManager.deleteAppManagedFile(current)
            repository.settingsStore.updateCompanyLogoPath("")
            _events.emit(UiMessageEvent.ToastMessage("Restored default Vedicra logo"))
        }
    }

    fun createNewReport() {
        viewModelScope.launch {
            val newId = repository.createNewReport()
            _activeReportId.value = newId
            _events.emit(UiMessageEvent.OpenReport(newId))
        }
    }

    fun saveActiveReportFromDashboard() {
        val current = activeReport.value ?: return
        viewModelScope.launch {
            repository.saveReport(current, markAsSaved = true)
            _events.emit(UiMessageEvent.ToastMessage("Report saved"))
        }
    }

    fun resetActiveReportFromDashboard() {
        val id = _activeReportId.value ?: return
        viewModelScope.launch {
            repository.resetUnsavedForm(id)
            _events.emit(UiMessageEvent.ToastMessage("Active form reset"))
        }
    }

    fun deleteActiveReportFromDashboard() {
        val id = _activeReportId.value ?: return
        viewModelScope.launch {
            repository.deleteReportCompletely(id)
            _activeReportId.value = null
            _events.emit(UiMessageEvent.ToastMessage("Saved report deleted"))
        }
    }

    fun duplicateReport(reportId: String) {
        viewModelScope.launch {
            val newId = repository.duplicateReport(reportId)
            if (newId != null) {
                _events.emit(UiMessageEvent.ToastMessage("Report duplicated"))
                _events.emit(UiMessageEvent.OpenReport(newId))
            }
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            if (_activeReportId.value == reportId) {
                _activeReportId.value = null
            }
            repository.deleteReportCompletely(reportId)
            _events.emit(UiMessageEvent.ToastMessage("Report deleted"))
        }
    }

    fun regeneratePdfForReport(reportId: String, onDone: (ExportedPdfEntity?) -> Unit = {}) {
        viewModelScope.launch {
            val bundle = repository.getReportBundle(reportId)
            if (bundle == null) {
                _events.emit(UiMessageEvent.ToastMessage("Report not found"))
                onDone(null)
                return@launch
            }
            val result = pdfGenerator.generateReportPdf(bundle, settings.value)
            result.onSuccess { pdf ->
                repository.recordExportedPdf(pdf)
                _events.emit(UiMessageEvent.ToastMessage("PDF generated: ${pdf.fileName}"))
                onDone(pdf)
            }.onFailure { err ->
                _events.emit(UiMessageEvent.ToastMessage("PDF generation failed: ${err.message}"))
                onDone(null)
            }
        }
    }

    fun deleteExportedPdf(pdf: ExportedPdfEntity) {
        viewModelScope.launch {
            repository.deleteExportedPdf(pdf)
            _events.emit(UiMessageEvent.ToastMessage("Exported PDF removed"))
        }
    }

    fun exportBackupJson(uri: Uri) {
        viewModelScope.launch {
            repository.exportBackupJsonToUri(uri).onSuccess { count ->
                _events.emit(UiMessageEvent.ToastMessage("Exported $count report(s) to JSON"))
            }.onFailure { err ->
                _events.emit(UiMessageEvent.ToastMessage("JSON export failed: ${err.message}"))
            }
        }
    }

    fun exportFullBackupZip(uri: Uri) {
        viewModelScope.launch {
            repository.exportFullBackupZipToUri(uri).onSuccess { count ->
                _events.emit(UiMessageEvent.ToastMessage("Backed up $count report(s) with photos & drawings"))
            }.onFailure { err ->
                _events.emit(UiMessageEvent.ToastMessage("Backup failed: ${err.message}"))
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            repository.restoreBackupFromUri(uri).onSuccess { count ->
                _events.emit(UiMessageEvent.ToastMessage("Restored $count report(s) successfully"))
            }.onFailure { err ->
                _events.emit(UiMessageEvent.ToastMessage("Restore failed: ${err.message}"))
            }
        }
    }

    class Factory(
        private val repository: VastuRepository,
        private val pdfGenerator: PdfReportGenerator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, pdfGenerator) as T
        }
    }
}
