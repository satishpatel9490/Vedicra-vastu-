package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VastuDao {
    @Query("SELECT * FROM reports ORDER BY updatedAt DESC")
    fun observeAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE isDraft = 0 ORDER BY updatedAt DESC")
    fun observeSavedReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports ORDER BY updatedAt DESC LIMIT 25")
    fun observeRecentlyEditedReports(): Flow<List<ReportEntity>>

    @Query(
        """
        SELECT * FROM reports
        WHERE clientName LIKE '%' || :query || '%'
           OR mobileNumber LIKE '%' || :query || '%'
           OR city LIKE '%' || :query || '%'
           OR areaLocality LIKE '%' || :query || '%'
           OR propertyType LIKE '%' || :query || '%'
           OR reportNumber LIKE '%' || :query || '%'
           OR selectedProblemsJson LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun searchReports(query: String): Flow<List<ReportEntity>>

    @Transaction
    @Query("SELECT * FROM reports WHERE id = :reportId")
    fun observeReportBundle(reportId: String): Flow<FullReportBundle?>

    @Transaction
    @Query("SELECT * FROM reports WHERE id = :reportId")
    suspend fun getReportBundle(reportId: String): FullReportBundle?

    @Transaction
    @Query("SELECT * FROM reports ORDER BY updatedAt DESC")
    suspend fun getAllReportBundles(): List<FullReportBundle>

    @Query("SELECT * FROM reports WHERE id = :reportId")
    suspend fun getReportById(reportId: String): ReportEntity?

    // Use @Upsert (INSERT or UPDATE) instead of REPLACE so ForeignKey.CASCADE children are NEVER deleted!
    @Upsert
    suspend fun upsertReport(report: ReportEntity)

    @Query("UPDATE reports SET updatedAt = :updatedAt WHERE id = :reportId")
    suspend fun updateReportTimestamp(reportId: String, updatedAt: Long)

    @Query("UPDATE reports SET lastPdfPath = :pdfPath, lastPdfGeneratedAt = :generatedAt, updatedAt = :updatedAt WHERE id = :reportId")
    suspend fun updateReportPdfInfo(reportId: String, pdfPath: String, generatedAt: Long, updatedAt: Long)

    @Query("DELETE FROM reports WHERE id = :reportId")
    suspend fun deleteReportById(reportId: String)

    // Analysis Rows
    @Query("SELECT * FROM analysis_rows WHERE reportId = :reportId ORDER BY sortOrder ASC")
    fun observeAnalysisRows(reportId: String): Flow<List<AnalysisRowEntity>>

    @Query("SELECT * FROM analysis_rows WHERE reportId = :reportId ORDER BY sortOrder ASC")
    suspend fun getAnalysisRows(reportId: String): List<AnalysisRowEntity>

    @Upsert
    suspend fun upsertAnalysisRow(row: AnalysisRowEntity)

    @Upsert
    suspend fun upsertAnalysisRows(rows: List<AnalysisRowEntity>)

    @Query("DELETE FROM analysis_rows WHERE id = :rowId")
    suspend fun deleteAnalysisRowById(rowId: String)

    @Query("DELETE FROM analysis_rows WHERE reportId = :reportId")
    suspend fun deleteAnalysisRowsForReport(reportId: String)

    // Site Media (Photos & Drawings)
    @Query("SELECT * FROM site_media WHERE reportId = :reportId ORDER BY sortOrder ASC, capturedAt ASC")
    fun observeMediaForReport(reportId: String): Flow<List<SiteMediaEntity>>

    @Query("SELECT * FROM site_media WHERE id = :mediaId")
    fun observeMediaById(mediaId: String): Flow<SiteMediaEntity?>

    @Query("SELECT * FROM site_media WHERE id = :mediaId")
    suspend fun getMediaById(mediaId: String): SiteMediaEntity?

    @Query("SELECT * FROM site_media WHERE reportId = :reportId ORDER BY sortOrder ASC, capturedAt ASC")
    suspend fun getMediaForReport(reportId: String): List<SiteMediaEntity>

    @Upsert
    suspend fun upsertMedia(media: SiteMediaEntity)

    @Upsert
    suspend fun upsertMediaList(mediaList: List<SiteMediaEntity>)

    @Update
    suspend fun updateMedia(media: SiteMediaEntity)

    @Query("UPDATE site_media SET isMainSiteLayout = 0 WHERE reportId = :reportId")
    suspend fun clearMainSiteLayout(reportId: String)

    @Query("DELETE FROM site_media WHERE id = :mediaId")
    suspend fun deleteMediaById(mediaId: String)

    @Query("DELETE FROM site_media WHERE reportId = :reportId")
    suspend fun deleteMediaForReport(reportId: String)

    // Exported PDFs
    @Query("SELECT * FROM exported_pdfs ORDER BY createdAt DESC")
    fun observeExportedPdfs(): Flow<List<ExportedPdfEntity>>

    @Query("SELECT * FROM exported_pdfs ORDER BY createdAt DESC")
    suspend fun getAllExportedPdfs(): List<ExportedPdfEntity>

    @Upsert
    suspend fun upsertExportedPdf(pdf: ExportedPdfEntity)

    @Query("DELETE FROM exported_pdfs WHERE id = :pdfId")
    suspend fun deleteExportedPdfById(pdfId: String)

    @Query("DELETE FROM exported_pdfs WHERE reportId = :reportId")
    suspend fun deleteExportedPdfsForReport(reportId: String)
}
