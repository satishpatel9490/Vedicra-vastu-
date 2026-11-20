package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reportNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val inspectionDate: Long = System.currentTimeMillis(),
    val isDraft: Boolean = true,

    // Client Details
    val clientName: String = "",
    val mobileNumber: String = "",
    val alternateNumber: String = "",
    val address: String = "",
    val areaLocality: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: String = "",

    // Optional Birth Details
    val birthDate: String = "",
    val birthTime: String = "",
    val birthLocation: String = "",

    // Property Details
    val propertyType: String = "House",
    val propertyTypeOther: String = "",
    val plotDegrees: String = "",
    val mainEntranceDirection: String = "North",
    val mainEntranceNotes: String = "",
    val additionalEntrancesJson: String = "[]",
    val plotShape: String = "",
    val buildingOrientation: String = "",
    val propertyObservations: String = "",

    // Swar Check
    val swarNadi: String = "",
    val swarObservationTime: String = "",
    val swarNotes: String = "",

    // Land Energy / Guna Analysis
    val landEnergyGunasJson: String = "[]",
    val landEnergyExplanation: String = "",

    // Client Problems
    val selectedProblemsJson: String = "[]",
    val problemSince: String = "",

    // Remedies & Implementation
    val recommendedRemedies: String = "",
    val activitiesToStop: String = "",
    val objectsToRemove: String = "",
    val objectsToAdd: String = "",
    val utilitiesToChange: String = "",
    val structuralCorrections: String = "",
    val remedyPriority: String = "Medium",
    val implementationInstructions: String = "",
    val expectedWaitingPeriod: String = "30 days",
    val customWaitingPeriod: String = "",
    val expectedFollowUpDate: String = "",
    val remedyNotes: String = "",

    // Consultant Notes (10 editable multiline notes initially)
    val consultantNotesJson: String = "[\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"]",

    // Last PDF info
    val lastPdfPath: String? = null,
    val lastPdfGeneratedAt: Long? = null
)

@Serializable
@Entity(
    tableName = "analysis_rows",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reportId"])]
)
data class AnalysisRowEntity(
    @PrimaryKey val id: String,
    val reportId: String,
    val sortOrder: Int = 0,
    val problemCategory: String = "",
    val clientProblem: String = "",
    val responsibleDirectionZone: String = "",
    val problemObserved: String = "",
    val antiActivity: String = "",
    val antiUtility: String = "",
    val antiObjects: String = "",
    val suggestedRemedy: String = "",
    val remedyPriority: String = "Medium",
    val waitingPeriod: String = "",
    val activitiesToStop: String = "",
    val objectsToRemove: String = "",
    val objectsToAdd: String = "",
    val utilitiesToChange: String = "",
    val structuralCorrections: String = "",
    val notes: String = "",
    val attachedMediaIdsJson: String = "[]"
)

@Serializable
@Entity(
    tableName = "site_media",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reportId"])]
)
data class SiteMediaEntity(
    @PrimaryKey val id: String,
    val reportId: String,
    val mediaKind: String = "PHOTO",
    val filePath: String = "",
    val annotatedFilePath: String? = null,
    val annotationsJson: String = "{}",
    val rotationDegrees: Int = 0,
    val caption: String = "",
    val description: String = "",
    val drawingCategory: String = "",
    val drawingTitle: String = "",
    val floorNumber: String = "",
    val orientationDirection: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
    val additionalNotes: String = "",
    val sortOrder: Int = 0,
    val isMainSiteLayout: Boolean = false,
    val includeInPdf: Boolean = true
)

@Serializable
@Entity(tableName = "exported_pdfs")
data class ExportedPdfEntity(
    @PrimaryKey val id: String,
    val reportId: String,
    val clientName: String,
    val fileName: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 1,
    val fileSizeBytes: Long = 0L
)

@Serializable
data class FullReportBundle(
    @Embedded val report: ReportEntity,
    @Relation(parentColumn = "id", entityColumn = "reportId")
    val analysisRows: List<AnalysisRowEntity>,
    @Relation(parentColumn = "id", entityColumn = "reportId")
    val mediaItems: List<SiteMediaEntity>
) {
    val orderedAnalysisRows: List<AnalysisRowEntity>
        get() = analysisRows.sortedBy { it.sortOrder }

    val orderedMediaItems: List<SiteMediaEntity>
        get() = mediaItems.sortedBy { it.sortOrder }
}
