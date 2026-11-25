package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
object SavedReportsRoute

@Serializable
object SearchReportsRoute

@Serializable
object RecentReportsRoute

@Serializable
object ExportedPdfsRoute

@Serializable
object SettingsRoute

@Serializable
data class ReportEditorRoute(
    val reportId: String,
    val initialTab: Int = 0
)

@Serializable
data class CameraCaptureRoute(
    val reportId: String,
    val mediaKind: String = "PHOTO",
    val drawingCategory: String = "",
    val drawingTitle: String = "",
    val floorNumber: String = "",
    val orientationDirection: String = "",
    val description: String = "",
    val additionalNotes: String = ""
)

@Serializable
data class PhotoViewerRoute(
    val mediaId: String
)

@Serializable
data class LayoutWorkspaceRoute(
    val mediaId: String
)

@Serializable
data class MapCompassAlignRoute(
    val reportId: String,
    val initialChakraType: String = "SHAKTI_16_32"
)

@Serializable
data class LayoutBuilderRoute(
    val reportId: String,
    val mediaId: String? = null
)
