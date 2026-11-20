package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AnalysisRowEntity
import com.example.data.local.ExportedPdfEntity
import com.example.data.local.FullReportBundle
import com.example.data.local.JsonCodec
import com.example.data.local.MediaFileManager
import com.example.data.local.ReportEntity
import com.example.data.local.SettingsStore
import com.example.data.local.SiteMediaEntity
import com.example.data.local.VastuDao
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.MediaKind
import com.example.model.VastuConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val reports: List<FullReportBundle> = emptyList(),
    val exportedPdfs: List<ExportedPdfEntity> = emptyList()
)

class VastuRepository(
    private val context: Context,
    private val dao: VastuDao,
    val mediaManager: MediaFileManager,
    val settingsStore: SettingsStore
) {
    val allReports: Flow<List<ReportEntity>> = dao.observeAllReports()
    val savedReports: Flow<List<ReportEntity>> = dao.observeSavedReports()
    val recentlyEditedReports: Flow<List<ReportEntity>> = dao.observeRecentlyEditedReports()
    val exportedPdfs: Flow<List<ExportedPdfEntity>> = dao.observeExportedPdfs()
    val settings: Flow<AppSettings> = settingsStore.settingsFlow

    fun searchReports(query: String): Flow<List<ReportEntity>> =
        if (query.isBlank()) dao.observeAllReports() else dao.searchReports(query.trim())

    fun observeReportBundle(reportId: String): Flow<FullReportBundle?> =
        dao.observeReportBundle(reportId)

    fun observeMediaById(mediaId: String): Flow<SiteMediaEntity?> =
        dao.observeMediaById(mediaId)

    suspend fun getReportBundle(reportId: String): FullReportBundle? =
        dao.getReportBundle(reportId)

    suspend fun getMediaById(mediaId: String): SiteMediaEntity? =
        dao.getMediaById(mediaId)

    suspend fun updateAppTheme(themeMode: AppThemeMode) =
        settingsStore.updateAppTheme(themeMode)

    suspend fun updateSettings(settings: AppSettings) =
        settingsStore.updateSettings(settings)

    suspend fun createNewReport(): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val shortCode = id.take(4).uppercase(Locale.US)
        val initialNotes = List(10) { "" }
        val newReport = ReportEntity(
            id = id,
            reportNumber = "VV-$dateStamp-$shortCode",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            inspectionDate = System.currentTimeMillis(),
            isDraft = true,
            consultantNotesJson = JsonCodec.encodeStringList(initialNotes)
        )
        dao.upsertReport(newReport)
        id
    }

    suspend fun saveReport(report: ReportEntity, markAsSaved: Boolean = true) {
        val updated = report.copy(
            updatedAt = System.currentTimeMillis(),
            isDraft = if (markAsSaved) false else report.isDraft
        )
        dao.upsertReport(updated)
    }

    suspend fun resetUnsavedForm(reportId: String): ReportEntity = withContext(Dispatchers.IO) {
        val existing = dao.getReportById(reportId)
        val resetNotes = List(10) { "" }
        val fresh = ReportEntity(
            id = reportId,
            reportNumber = existing?.reportNumber?.ifBlank {
                "VV-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}-${reportId.take(4).uppercase(Locale.US)}"
            } ?: "VV-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}-${reportId.take(4).uppercase(Locale.US)}",
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            inspectionDate = System.currentTimeMillis(),
            isDraft = true,
            consultantNotesJson = JsonCodec.encodeStringList(resetNotes)
        )
        dao.upsertReport(fresh)
        dao.deleteAnalysisRowsForReport(reportId)
        dao.deleteMediaForReport(reportId)
        mediaManager.deleteReportMediaDirectory(reportId)
        fresh
    }

    suspend fun duplicateReport(sourceReportId: String): String? = withContext(Dispatchers.IO) {
        val source = dao.getReportBundle(sourceReportId) ?: return@withContext null
        val newReportId = UUID.randomUUID().toString()
        val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val shortCode = newReportId.take(4).uppercase(Locale.US)

        val clonedReport = source.report.copy(
            id = newReportId,
            reportNumber = "VV-$dateStamp-$shortCode",
            clientName = if (source.report.clientName.isBlank()) "Copy Report" else "${source.report.clientName} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDraft = false,
            lastPdfPath = null,
            lastPdfGeneratedAt = null
        )
        dao.upsertReport(clonedReport)

        val mediaIdMap = mutableMapOf<String, String>()
        val clonedMedia = source.orderedMediaItems.map { media ->
            val newMediaId = UUID.randomUUID().toString()
            mediaIdMap[media.id] = newMediaId
            val newOriginalFile = mediaManager.createOriginalImageFile(newReportId, "copy")
            mediaManager.copyFileWithinReport(media.filePath, newOriginalFile)

            var newAnnotatedPath: String? = null
            if (!media.annotatedFilePath.isNullOrBlank()) {
                val newAnnotatedFile = mediaManager.createAnnotatedImageFile(newReportId)
                if (mediaManager.copyFileWithinReport(media.annotatedFilePath, newAnnotatedFile)) {
                    newAnnotatedPath = newAnnotatedFile.absolutePath
                }
            }

            media.copy(
                id = newMediaId,
                reportId = newReportId,
                filePath = newOriginalFile.absolutePath,
                annotatedFilePath = newAnnotatedPath
            )
        }
        if (clonedMedia.isNotEmpty()) {
            dao.upsertMediaList(clonedMedia)
        }

        val clonedRows = source.orderedAnalysisRows.map { row ->
            val mappedMediaIds = JsonCodec.decodeStringList(row.attachedMediaIdsJson)
                .mapNotNull { oldId -> mediaIdMap[oldId] }
            row.copy(
                id = UUID.randomUUID().toString(),
                reportId = newReportId,
                attachedMediaIdsJson = JsonCodec.encodeStringList(mappedMediaIds)
            )
        }
        if (clonedRows.isNotEmpty()) {
            dao.upsertAnalysisRows(clonedRows)
        }

        newReportId
    }

    suspend fun deleteReportCompletely(reportId: String) = withContext(Dispatchers.IO) {
        val pdfs = dao.getAllExportedPdfs().filter { it.reportId == reportId }
        pdfs.forEach { pdf -> mediaManager.deleteAppManagedFile(pdf.filePath) }
        dao.deleteExportedPdfsForReport(reportId)
        mediaManager.deleteReportMediaDirectory(reportId)
        dao.deleteReportById(reportId)
    }

    // Analysis Table Operations
    suspend fun addAnalysisRow(
        reportId: String,
        initialProblem: String = "",
        problemCategory: String = ""
    ): AnalysisRowEntity {
        val currentRows = dao.getAnalysisRows(reportId)
        val nextOrder = (currentRows.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val resolvedCategory = problemCategory.ifBlank {
            if (initialProblem.isNotBlank()) VastuConstants.categoryForProblem(initialProblem)
            else VastuConstants.problemCategories.first()
        }
        val newRow = AnalysisRowEntity(
            id = UUID.randomUUID().toString(),
            reportId = reportId,
            sortOrder = nextOrder,
            problemCategory = resolvedCategory,
            clientProblem = initialProblem
        )
        dao.upsertAnalysisRow(newRow)
        touchReportTimestamp(reportId)
        return newRow
    }

    suspend fun upsertAnalysisRow(row: AnalysisRowEntity) {
        dao.upsertAnalysisRow(row)
        touchReportTimestamp(row.reportId)
    }

    suspend fun duplicateAnalysisRow(row: AnalysisRowEntity) {
        val currentRows = dao.getAnalysisRows(row.reportId).sortedBy { it.sortOrder }
        val index = currentRows.indexOfFirst { it.id == row.id }
        val copy = row.copy(id = UUID.randomUUID().toString())
        val updated = currentRows.toMutableList().apply {
            if (index >= 0) add(index + 1, copy) else add(copy)
        }.mapIndexed { idx, item -> item.copy(sortOrder = idx) }
        dao.upsertAnalysisRows(updated)
        touchReportTimestamp(row.reportId)
    }

    suspend fun deleteAnalysisRow(row: AnalysisRowEntity) {
        dao.deleteAnalysisRowById(row.id)
        val remaining = dao.getAnalysisRows(row.reportId).sortedBy { it.sortOrder }
            .mapIndexed { idx, item -> item.copy(sortOrder = idx) }
        if (remaining.isNotEmpty()) {
            dao.upsertAnalysisRows(remaining)
        }
        touchReportTimestamp(row.reportId)
    }

    // Media & Drawing Operations
    suspend fun addCapturedCameraPhoto(
        reportId: String,
        file: File,
        mediaKind: MediaKind,
        caption: String = "",
        drawingCategory: String = "",
        drawingTitle: String = "",
        floorNumber: String = "",
        orientationDirection: String = "",
        description: String = "",
        additionalNotes: String = ""
    ): SiteMediaEntity = withContext(Dispatchers.IO) {
        mediaManager.normalizeCameraImage(file)
        val current = dao.getMediaForReport(reportId)
        val nextOrder = (current.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val entity = SiteMediaEntity(
            id = UUID.randomUUID().toString(),
            reportId = reportId,
            mediaKind = mediaKind.name,
            filePath = file.absolutePath,
            caption = caption,
            description = description,
            drawingCategory = drawingCategory,
            drawingTitle = drawingTitle.ifBlank { drawingCategory },
            floorNumber = floorNumber,
            orientationDirection = orientationDirection,
            additionalNotes = additionalNotes,
            capturedAt = System.currentTimeMillis(),
            sortOrder = nextOrder
        )
        dao.upsertMedia(entity)
        touchReportTimestamp(reportId)
        entity
    }

    suspend fun importGalleryImages(
        reportId: String,
        uris: List<Uri>,
        mediaKind: MediaKind,
        drawingCategory: String = "",
        drawingTitle: String = "",
        floorNumber: String = "",
        orientationDirection: String = "",
        description: String = "",
        additionalNotes: String = ""
    ): List<SiteMediaEntity> = withContext(Dispatchers.IO) {
        val current = dao.getMediaForReport(reportId)
        var nextOrder = (current.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val added = mutableListOf<SiteMediaEntity>()
        uris.forEach { uri ->
            val file = mediaManager.importImageFromUri(
                reportId = reportId,
                uri = uri,
                prefix = if (mediaKind == MediaKind.DRAWING) "drawing" else "photo"
            )
            if (file != null) {
                val entity = SiteMediaEntity(
                    id = UUID.randomUUID().toString(),
                    reportId = reportId,
                    mediaKind = mediaKind.name,
                    filePath = file.absolutePath,
                    drawingCategory = drawingCategory,
                    drawingTitle = drawingTitle.ifBlank { drawingCategory },
                    floorNumber = floorNumber,
                    orientationDirection = orientationDirection,
                    description = description,
                    additionalNotes = additionalNotes,
                    capturedAt = System.currentTimeMillis(),
                    sortOrder = nextOrder++
                )
                dao.upsertMedia(entity)
                added.add(entity)
            }
        }
        if (added.isNotEmpty()) touchReportTimestamp(reportId)
        added
    }

    suspend fun updateMedia(media: SiteMediaEntity) {
        dao.updateMedia(media)
        touchReportTimestamp(media.reportId)
    }

    suspend fun setMainSiteLayout(reportId: String, mediaId: String) {
        dao.clearMainSiteLayout(reportId)
        val target = dao.getMediaById(mediaId) ?: return
        dao.updateMedia(target.copy(isMainSiteLayout = true))
        touchReportTimestamp(reportId)
    }

    suspend fun moveMediaOrder(reportId: String, mediaId: String, moveUp: Boolean, mediaKind: MediaKind) {
        val list = dao.getMediaForReport(reportId)
            .filter { it.mediaKind == mediaKind.name }
            .sortedBy { it.sortOrder }
            .toMutableList()
        val idx = list.indexOfFirst { it.id == mediaId }
        if (idx < 0) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx !in list.indices) return
        val item = list.removeAt(idx)
        list.add(targetIdx, item)
        val reordered = list.mapIndexed { index, m -> m.copy(sortOrder = index) }
        dao.upsertMediaList(reordered)
        touchReportTimestamp(reportId)
    }

    suspend fun deleteMediaItem(media: SiteMediaEntity) = withContext(Dispatchers.IO) {
        // Also detach from any analysis rows referencing this media ID
        val rows = dao.getAnalysisRows(media.reportId)
        rows.forEach { row ->
            val attached = JsonCodec.decodeStringList(row.attachedMediaIdsJson)
            if (media.id in attached) {
                val updatedIds = attached.filterNot { it == media.id }
                dao.upsertAnalysisRow(row.copy(attachedMediaIdsJson = JsonCodec.encodeStringList(updatedIds)))
            }
        }
        mediaManager.deleteAppManagedFile(media.filePath)
        mediaManager.deleteAppManagedFile(media.annotatedFilePath)
        dao.deleteMediaById(media.id)
        touchReportTimestamp(media.reportId)
    }

    // Exported PDFs
    suspend fun recordExportedPdf(pdf: ExportedPdfEntity) {
        dao.upsertExportedPdf(pdf)
        dao.updateReportPdfInfo(
            reportId = pdf.reportId,
            pdfPath = pdf.filePath,
            generatedAt = pdf.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteExportedPdf(pdf: ExportedPdfEntity) = withContext(Dispatchers.IO) {
        mediaManager.deleteAppManagedFile(pdf.filePath)
        dao.deleteExportedPdfById(pdf.id)
    }

    private suspend fun touchReportTimestamp(reportId: String) {
        dao.updateReportTimestamp(reportId, System.currentTimeMillis())
    }

    // Backup & Restore (JSON + Full ZIP Package)
    suspend fun exportBackupJsonToUri(targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val bundles = dao.getAllReportBundles()
            val pdfs = dao.getAllExportedPdfs()
            val payload = BackupPayload(
                reports = bundles,
                exportedPdfs = pdfs
            )
            val jsonText = JsonCodec.json.encodeToString(payload)
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(jsonText.toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open output stream")
            bundles.size
        }
    }

    suspend fun exportFullBackupZipToUri(targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val bundles = dao.getAllReportBundles()
            val pdfs = dao.getAllExportedPdfs()
            // Convert file paths inside payload to relative paths under filesDir so restore is portable
            val filesRootPath = context.filesDir.canonicalPath
            val normalizedBundles = bundles.map { bundle ->
                val normalizedMedia = bundle.mediaItems.map { m ->
                    m.copy(
                        filePath = toRelativeAppPath(m.filePath, filesRootPath),
                        annotatedFilePath = m.annotatedFilePath?.let { toRelativeAppPath(it, filesRootPath) }
                    )
                }
                bundle.copy(mediaItems = normalizedMedia)
            }
            val payload = BackupPayload(
                reports = normalizedBundles,
                exportedPdfs = pdfs
            )
            val jsonText = JsonCodec.json.encodeToString(payload)

            context.contentResolver.openOutputStream(targetUri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    // 1. Write manifest JSON
                    zos.putNextEntry(ZipEntry("backup_data.json"))
                    zos.write(jsonText.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    // 2. Write all referenced files (photos, drawings, annotated versions)
                    val addedEntries = mutableSetOf<String>()
                    bundles.forEach { bundle ->
                        bundle.mediaItems.forEach { media ->
                            listOfNotNull(media.filePath, media.annotatedFilePath).forEach { absPath ->
                                val file = File(absPath)
                                if (file.exists()) {
                                    val rel = toRelativeAppPath(absPath, filesRootPath).trimStart('/')
                                    if (addedEntries.add(rel)) {
                                        zos.putNextEntry(ZipEntry("files/$rel"))
                                        BufferedInputStream(FileInputStream(file)).use { bis ->
                                            bis.copyTo(zos)
                                        }
                                        zos.closeEntry()
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: error("Unable to open ZIP output stream")

            bundles.size
        }
    }

    suspend fun restoreBackupFromUri(sourceUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val mimeType = context.contentResolver.getType(sourceUri) ?: ""
            val fileName = sourceUri.lastPathSegment ?: ""
            val isZip = mimeType.contains("zip", ignoreCase = true) ||
                fileName.endsWith(".zip", ignoreCase = true)

            if (isZip) {
                restoreFromZip(sourceUri)
            } else {
                restoreFromJson(sourceUri)
            }
        }
    }

    private suspend fun restoreFromJson(sourceUri: Uri): Int {
        val text = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("Unable to read backup file")
        val payload = JsonCodec.json.decodeFromString<BackupPayload>(text)
        return applyRestoredPayload(payload)
    }

    private suspend fun restoreFromZip(sourceUri: Uri): Int {
        var jsonText: String? = null
        val filesRoot = context.filesDir.canonicalFile
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory) {
                        if (name == "backup_data.json") {
                            jsonText = zis.readBytes().toString(Charsets.UTF_8)
                        } else if (name.startsWith("files/")) {
                            val relPath = name.removePrefix("files/")
                            val outFile = File(filesRoot, relPath).canonicalFile
                            if (outFile.path.startsWith(filesRoot.path)) {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: error("Unable to read ZIP backup")

        val rawJson = jsonText ?: error("Missing backup_data.json inside backup package")
        val payload = JsonCodec.json.decodeFromString<BackupPayload>(rawJson)
        return applyRestoredPayload(payload)
    }

    private suspend fun applyRestoredPayload(payload: BackupPayload): Int {
        val filesRoot = context.filesDir.canonicalPath
        payload.reports.forEach { bundle ->
            dao.upsertReport(bundle.report)
            if (bundle.analysisRows.isNotEmpty()) {
                dao.upsertAnalysisRows(bundle.analysisRows)
            }
            val restoredMedia = bundle.mediaItems.map { m ->
                m.copy(
                    filePath = fromRelativeAppPath(m.filePath, filesRoot),
                    annotatedFilePath = m.annotatedFilePath?.let { fromRelativeAppPath(it, filesRoot) }
                )
            }
            if (restoredMedia.isNotEmpty()) {
                dao.upsertMediaList(restoredMedia)
            }
        }
        return payload.reports.size
    }

    private fun toRelativeAppPath(path: String, filesRootPath: String): String {
        val canonical = runCatching { File(path).canonicalPath }.getOrDefault(path)
        return if (canonical.startsWith(filesRootPath)) {
            canonical.removePrefix(filesRootPath).trimStart('/')
        } else path
    }

    private fun fromRelativeAppPath(path: String, filesRootPath: String): String {
        if (path.startsWith("/")) {
            val file = File(path)
            if (file.exists()) return path
            val idx = path.indexOf("reports/")
            if (idx >= 0) return File(filesRootPath, path.substring(idx)).absolutePath
            return path
        }
        return File(filesRootPath, path).absolutePath
    }
}
