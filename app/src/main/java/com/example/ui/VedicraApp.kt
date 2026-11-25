package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.di.VedicraAppContainer
import com.example.model.MediaKind
import com.example.model.VastuChakraType
import com.example.ui.navigation.CameraCaptureRoute
import com.example.ui.navigation.DashboardRoute
import com.example.ui.navigation.ExportedPdfsRoute
import com.example.ui.navigation.LayoutBuilderRoute
import com.example.ui.navigation.LayoutWorkspaceRoute
import com.example.ui.navigation.MapCompassAlignRoute
import com.example.ui.navigation.PhotoViewerRoute
import com.example.ui.navigation.RecentReportsRoute
import com.example.ui.navigation.ReportEditorRoute
import com.example.ui.navigation.SavedReportsRoute
import com.example.ui.navigation.SearchReportsRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportedPdfsScreen
import com.example.ui.screens.LayoutBuilderScreen
import com.example.ui.screens.LayoutWorkspaceScreen
import com.example.ui.screens.MapCompassAlignScreen
import com.example.ui.screens.PhotoViewerScreen
import com.example.ui.screens.ReportEditorScreen
import com.example.ui.screens.ReportListScreen
import com.example.ui.screens.SearchReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.VedicraVastuTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ReportEditorViewModel
import com.example.ui.viewmodel.UiMessageEvent
import com.example.ui.viewmodel.WorkspaceViewModel
import kotlinx.coroutines.launch

@Composable
fun VedicraApp(container: VedicraAppContainer) {
    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(container.repository, container.pdfGenerator)
    )
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val savedReports by mainViewModel.savedReports.collectAsStateWithLifecycle()
    val recentReports by mainViewModel.recentlyEditedReports.collectAsStateWithLifecycle()
    val exportedPdfs by mainViewModel.exportedPdfs.collectAsStateWithLifecycle()
    val searchQuery by mainViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by mainViewModel.searchResults.collectAsStateWithLifecycle()
    val activeReport by mainViewModel.activeReport.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(msg: String) {
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(mainViewModel) {
        mainViewModel.events.collect { event ->
            when (event) {
                is UiMessageEvent.ToastMessage -> showMessage(event.message)
                is UiMessageEvent.OpenReport -> {
                    mainViewModel.setActiveReportId(event.reportId)
                    navController.navigate(ReportEditorRoute(reportId = event.reportId))
                }
            }
        }
    }

    VedicraVastuTheme(themeMode = settings.appTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = DashboardRoute
            ) {
                composable<DashboardRoute> {
                    DashboardScreen(
                        settings = settings,
                        activeReport = activeReport,
                        onSelectTheme = mainViewModel::updateAppTheme,
                        onCreateNewReport = mainViewModel::createNewReport,
                        onResumeActiveReport = { id ->
                            mainViewModel.setActiveReportId(id)
                            navController.navigate(ReportEditorRoute(reportId = id))
                        },
                        onSaveActiveReport = mainViewModel::saveActiveReportFromDashboard,
                        onResetActiveReport = mainViewModel::resetActiveReportFromDashboard,
                        onDeleteActiveReport = mainViewModel::deleteActiveReportFromDashboard,
                        onOpenSavedReports = { navController.navigate(SavedReportsRoute) },
                        onOpenSearchReports = { navController.navigate(SearchReportsRoute) },
                        onOpenRecentReports = { navController.navigate(RecentReportsRoute) },
                        onOpenExportedPdfs = { navController.navigate(ExportedPdfsRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) }
                    )
                }

                composable<SavedReportsRoute> {
                    ReportListScreen(
                        title = "Saved Reports",
                        reports = savedReports,
                        emptyTitle = "No Saved Reports Yet",
                        emptyMessage = "Create a new report and tap Save to store client Vastu inspections offline.",
                        onBack = { navController.popBackStack() },
                        onOpenReport = { id ->
                            mainViewModel.setActiveReportId(id)
                            navController.navigate(ReportEditorRoute(reportId = id))
                        },
                        onDuplicateReport = mainViewModel::duplicateReport,
                        onGeneratePdf = { id ->
                            mainViewModel.regeneratePdfForReport(id) { pdf ->
                                if (pdf != null) navController.navigate(ExportedPdfsRoute)
                            }
                        },
                        onDeleteReport = mainViewModel::deleteReport
                    )
                }

                composable<RecentReportsRoute> {
                    ReportListScreen(
                        title = "Recently Edited Reports",
                        reports = recentReports,
                        emptyTitle = "No Recent Inspections",
                        emptyMessage = "Your newly created and recently edited reports will appear here.",
                        onBack = { navController.popBackStack() },
                        onOpenReport = { id ->
                            mainViewModel.setActiveReportId(id)
                            navController.navigate(ReportEditorRoute(reportId = id))
                        },
                        onDuplicateReport = mainViewModel::duplicateReport,
                        onGeneratePdf = { id ->
                            mainViewModel.regeneratePdfForReport(id) { pdf ->
                                if (pdf != null) navController.navigate(ExportedPdfsRoute)
                            }
                        },
                        onDeleteReport = mainViewModel::deleteReport
                    )
                }

                composable<SearchReportsRoute> {
                    SearchReportsScreen(
                        query = searchQuery,
                        results = searchResults,
                        onQueryChange = mainViewModel::updateSearchQuery,
                        onBack = { navController.popBackStack() },
                        onOpenReport = { id ->
                            mainViewModel.setActiveReportId(id)
                            navController.navigate(ReportEditorRoute(reportId = id))
                        },
                        onDuplicateReport = mainViewModel::duplicateReport,
                        onGeneratePdf = { id ->
                            mainViewModel.regeneratePdfForReport(id) { pdf ->
                                if (pdf != null) navController.navigate(ExportedPdfsRoute)
                            }
                        },
                        onDeleteReport = mainViewModel::deleteReport
                    )
                }

                composable<ExportedPdfsRoute> {
                    ExportedPdfsScreen(
                        pdfs = exportedPdfs,
                        onBack = { navController.popBackStack() },
                        onEditReport = { id ->
                            mainViewModel.setActiveReportId(id)
                            navController.navigate(ReportEditorRoute(reportId = id))
                        },
                        onRegeneratePdf = { id -> mainViewModel.regeneratePdfForReport(id) },
                        onDeletePdf = mainViewModel::deleteExportedPdf
                    )
                }

                composable<SettingsRoute> {
                    SettingsScreen(
                        settings = settings,
                        onBack = { navController.popBackStack() },
                        onSaveSettings = mainViewModel::updateSettings,
                        onImportLogo = mainViewModel::importCompanyLogo,
                        onResetLogo = mainViewModel::resetCompanyLogoToDefault,
                        onExportBackupJson = mainViewModel::exportBackupJson,
                        onExportFullZip = mainViewModel::exportFullBackupZip,
                        onRestoreBackup = mainViewModel::restoreBackup
                    )
                }

                composable<ReportEditorRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<ReportEditorRoute>()
                    LaunchedEffect(route.reportId) {
                        mainViewModel.setActiveReportId(route.reportId)
                    }
                    val editorViewModel: ReportEditorViewModel = viewModel(
                        key = "editor_${route.reportId}",
                        factory = ReportEditorViewModel.Factory(
                            reportId = route.reportId,
                            repository = container.repository,
                            pdfGenerator = container.pdfGenerator
                        )
                    )
                    ReportEditorScreen(
                        viewModel = editorViewModel,
                        initialTab = route.initialTab,
                        onSelectTheme = mainViewModel::updateAppTheme,
                        onBack = { navController.popBackStack() },
                        onLaunchCamera = { mediaKind, cat, title, floor, orient, desc, notes ->
                            navController.navigate(
                                CameraCaptureRoute(
                                    reportId = route.reportId,
                                    mediaKind = mediaKind,
                                    drawingCategory = cat,
                                    drawingTitle = title,
                                    floorNumber = floor,
                                    orientationDirection = orient,
                                    description = desc,
                                    additionalNotes = notes
                                )
                            )
                        },
                        onOpenPhotoViewer = { mediaId ->
                            navController.navigate(PhotoViewerRoute(mediaId = mediaId))
                        },
                        onOpenLayoutWorkspace = { mediaId ->
                            navController.navigate(LayoutWorkspaceRoute(mediaId = mediaId))
                        },
                        onOpenMapAlign = { chakraName ->
                            navController.navigate(
                                MapCompassAlignRoute(
                                    reportId = route.reportId,
                                    initialChakraType = chakraName
                                )
                            )
                        },
                        onOpenLayoutBuilder = { mediaId ->
                            navController.navigate(
                                LayoutBuilderRoute(
                                    reportId = route.reportId,
                                    mediaId = mediaId
                                )
                            )
                        },
                        onShowMessage = ::showMessage
                    )
                }

                composable<CameraCaptureRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<CameraCaptureRoute>()
                    val kind = runCatching { MediaKind.valueOf(route.mediaKind) }.getOrDefault(MediaKind.PHOTO)
                    CameraCaptureScreen(
                        reportId = route.reportId,
                        mediaKind = kind,
                        drawingCategory = route.drawingCategory,
                        drawingTitle = route.drawingTitle,
                        floorNumber = route.floorNumber,
                        orientationDirection = route.orientationDirection,
                        description = route.description,
                        additionalNotes = route.additionalNotes,
                        repository = container.repository,
                        mediaManager = container.mediaManager,
                        onDone = { navController.popBackStack() },
                        onShowMessage = ::showMessage
                    )
                }

                composable<PhotoViewerRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<PhotoViewerRoute>()
                    PhotoViewerScreen(
                        mediaId = route.mediaId,
                        repository = container.repository,
                        onBack = { navController.popBackStack() },
                        onOpenWorkspace = { mediaId ->
                            navController.navigate(LayoutWorkspaceRoute(mediaId = mediaId))
                        }
                    )
                }

                composable<LayoutWorkspaceRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<LayoutWorkspaceRoute>()
                    val workspaceViewModel: WorkspaceViewModel = viewModel(
                        key = "workspace_${route.mediaId}",
                        factory = WorkspaceViewModel.Factory(
                            mediaId = route.mediaId,
                            repository = container.repository
                        )
                    )
                    LayoutWorkspaceScreen(
                        viewModel = workspaceViewModel,
                        onBack = { navController.popBackStack() },
                        onShowMessage = ::showMessage
                    )
                }

                composable<MapCompassAlignRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<MapCompassAlignRoute>()
                    val initialChakra = runCatching { VastuChakraType.valueOf(route.initialChakraType) }
                        .getOrDefault(VastuChakraType.SHAKTI_16_32)
                    MapCompassAlignScreen(
                        reportId = route.reportId,
                        initialChakraType = initialChakra,
                        repository = container.repository,
                        onBack = { navController.popBackStack() },
                        onOpenWorkspace = { mediaId ->
                            navController.navigate(LayoutWorkspaceRoute(mediaId = mediaId))
                        },
                        onShowMessage = ::showMessage
                    )
                }

                composable<LayoutBuilderRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<LayoutBuilderRoute>()
                    LayoutBuilderScreen(
                        reportId = route.reportId,
                        mediaId = route.mediaId,
                        repository = container.repository,
                        onBack = { navController.popBackStack() },
                        onShowMessage = ::showMessage
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}
