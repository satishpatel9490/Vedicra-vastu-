package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AnalysisRowEntity
import com.example.data.local.JsonCodec
import com.example.data.local.MediaFileManager
import com.example.data.local.ReportEntity
import com.example.data.local.SettingsStore
import com.example.data.local.VastuDatabase
import com.example.data.repository.VastuRepository
import com.example.model.AnnotationType
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.ClientProblemItem
import com.example.model.CompassReading
import com.example.model.EntranceDetail
import com.example.model.GridOverlayStyle
import com.example.model.LayoutAnnotation
import com.example.model.MediaKind
import com.example.model.VastuChakraSpec
import com.example.model.VastuCompassMath
import com.example.model.VastuConstants
import com.example.model.WorkspaceMetadata
import com.example.pdf.PdfDocumentAdapter
import com.example.pdf.PdfPageHandle
import com.example.pdf.PdfReportGenerator
import com.example.pdf.WorkspaceRenderer
import com.example.ui.components.CompassTopDegreeTiltHud
import com.example.ui.components.VastuChakraCanvas
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportEditorScreen
import com.example.ui.screens.ReportsHubScreen
import com.example.ui.screens.editor.DegreesAndCompassSection
import com.example.ui.screens.editor.EntranceDetailsSection
import com.example.ui.screens.editor.UnifiedProblemsAnalysisRemediesSection
import com.example.ui.theme.VedicraVastuTheme
import com.example.ui.viewmodel.ReportEditorViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VedicraVastuComprehensiveTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: VastuDatabase
    private lateinit var mediaManager: MediaFileManager
    private lateinit var settingsStore: SettingsStore
    private lateinit var repository: VastuRepository
    private lateinit var pdfGenerator: PdfReportGenerator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, VastuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaManager = MediaFileManager(context)
        settingsStore = SettingsStore(context)
        repository = VastuRepository(
            context = context,
            dao = database.vastuDao(),
            mediaManager = mediaManager,
            settingsStore = settingsStore
        )
        pdfGenerator = PdfReportGenerator(context, mediaManager) {
            object : PdfDocumentAdapter {
                private val pages = mutableListOf<Bitmap>()
                override fun startPage(width: Int, height: Int, pageNumber: Int): PdfPageHandle {
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val c = android.graphics.Canvas(bmp)
                    return object : PdfPageHandle {
                        override val canvas: android.graphics.Canvas = c
                        val backing = bmp
                    }
                }
                override fun finishPage(page: PdfPageHandle) {
                    val bmp = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
                    pages.add(bmp)
                }
                override fun writeTo(outputStream: java.io.OutputStream) {
                    val header = "%PDF-1.4\n%Pages:${pages.size}\n".toByteArray()
                    outputStream.write(header)
                    pages.forEach { b ->
                        b.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
                        b.recycle()
                    }
                }
                override fun close() {
                    pages.forEach { if (!it.isRecycled) it.recycle() }
                    pages.clear()
                }
            }
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun dashboard_showsRequiredOptions_andHidesStatisticsCounters() {
        composeRule.setContent {
            VedicraVastuTheme(themeMode = AppThemeMode.IVORY_GOLD) {
                DashboardScreen(
                    settings = AppSettings(),
                    activeReport = null,
                    onSelectTheme = {},
                    onCreateNewReport = {},
                    onResumeActiveReport = {},
                    onSaveActiveReport = {},
                    onResetActiveReport = {},
                    onDeleteActiveReport = {},
                    onOpenReports = {},
                    onOpenSettings = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dashboard_shakti_chakra_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_grid").performScrollToNode(hasText("Create New Report"))
        composeRule.onNodeWithText("Create New Report").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_grid").performScrollToNode(hasText("Reports"))
        composeRule.onNodeWithText("Reports").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_grid").performScrollToNode(hasText("Settings"))
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_selector_button").assertIsDisplayed()

        // Merged dashboard boxes must NOT appear separately on Home
        assertEquals(0, composeRule.onAllNodesWithText("Saved Reports").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Search Reports").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Recently Edited Reports").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Exported PDF Reports").fetchSemanticsNodes().size)

        // Must NOT display any Total Clients / Total Reports counters
        assertEquals(0, composeRule.onAllNodesWithText("Total Clients", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Total Reports", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Client Count", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Report Count", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
    }

    @Test
    fun fullReportWorkflow_22AnalysisRows_photos_drawings_workspace_pdf_reset_delete() = runTest {
        // 1. Create two reports to verify reset/delete isolation
        val otherReportId = repository.createNewReport()
        val otherBundle = repository.getReportBundle(otherReportId)!!
        repository.saveReport(otherBundle.report.copy(clientName = "Protected Client"), markAsSaved = true)

        val reportId = repository.createNewReport()
        val initialBundle = repository.getReportBundle(reportId)!!
        val initialNotes = JsonCodec.decodeStringList(initialBundle.report.consultantNotesJson)
        assertEquals(10, initialNotes.size)

        // 2. Populate full client, birth, property, Swar, Land Energy, problems, remedies & 12 notes
        val updatedNotes = initialNotes.mapIndexed { idx, _ -> "Consultant observation note #${idx + 1}" } +
            listOf("Extra unlimited note 11", "Extra unlimited note 12")

        val updatedReport = initialBundle.report.copy(
            clientName = "Rajeshwar Sharma",
            mobileNumber = "9876543210",
            alternateNumber = "9123456780",
            address = "Plot 42, Jubilee Hills Road No. 10",
            areaLocality = "Jubilee Hills",
            city = "Hyderabad",
            state = "Telangana",
            pinCode = "500033",
            birthDate = "14/08/1982",
            birthTime = "06:20 AM",
            birthLocation = "Hyderabad",
            propertyType = "Factory",
            plotDegrees = "12° NNE",
            mainEntranceDirection = "Northeast",
            mainEntranceNotes = "E3/E4 auspicious pada",
            additionalEntrancesJson = JsonCodec.encodeEntrances(
                listOf(EntranceDetail(direction = "East", label = "Goods Dispatch Gate", degrees = "90°"))
            ),
            plotShape = "Rectangle",
            buildingOrientation = "North-East Facing",
            swarNadi = "Surya / Pingala Nadi (Right)",
            swarObservationTime = "10:15 AM",
            swarNotes = "Strong right nadi flow on entry",
            landEnergyGunasJson = JsonCodec.encodeStringList(listOf("Sattva", "Rajas")),
            landEnergyExplanation = "Calm central courtyard with active industrial zone",
            selectedProblemsJson = JsonCodec.encodeProblems(
                listOf(
                    ClientProblemItem(title = "Machinery breakdown", isCustom = false),
                    ClientProblemItem(title = "Money recovery problems", isCustom = false),
                    ClientProblemItem(title = "Transformer overload issue", isCustom = true)
                )
            ),
            problemSince = "Since 2 years",
            recommendedRemedies = "Balance SE fire zone and relocate heavy scrap from NE",
            activitiesToStop = "Stop storing raw scrap in NE corner",
            objectsToRemove = "Broken spare motors in North",
            objectsToAdd = "Copper pyramid & green plants in East",
            utilitiesToChange = "Shift electrical panel toward SE",
            structuralCorrections = "Raise SW plinth level by 6 inches",
            remedyPriority = "High",
            implementationInstructions = "Complete phase 1 within 15 days",
            expectedWaitingPeriod = "45 days",
            expectedFollowUpDate = "20/12/2026",
            consultantNotesJson = JsonCodec.encodeStringList(updatedNotes)
        )
        repository.saveReport(updatedReport, markAsSaved = true)

        // 3. Create real bitmap files for site photo & floor plan drawing
        val photoFile = createTestJpeg(reportId, "site_photo.jpg", android.graphics.Color.rgb(40, 110, 80))
        val photoMedia = repository.addCapturedCameraPhoto(
            reportId = reportId,
            file = photoFile,
            mediaKind = MediaKind.PHOTO,
            caption = "Main Machinery Bay"
        )
        repository.setMainSiteLayout(reportId, photoMedia.id)

        val drawingFile = createTestJpeg(reportId, "floor_plan.jpg", android.graphics.Color.rgb(245, 240, 225))
        val drawingMedia = repository.addCapturedCameraPhoto(
            reportId = reportId,
            file = drawingFile,
            mediaKind = MediaKind.DRAWING,
            caption = "Factory Ground Floor",
            drawingCategory = "Factory layout",
            drawingTitle = "Main Factory Floor Plan",
            floorNumber = "Ground",
            orientationDirection = "North",
            description = "Measured layout with machinery zones"
        )

        // 4. Annotate drawing in Workspace on a copy (original untouched!)
        val originalSizeBefore = File(drawingMedia.filePath).length()
        val workspaceMeta = WorkspaceMetadata(
            showGridOverlay = true,
            gridRotationDegrees = 12f,
            annotations = listOf(
                LayoutAnnotation(type = AnnotationType.DIRECTION_MARKER, label = "N", x = 0.5f, y = 0.1f),
                LayoutAnnotation(type = AnnotationType.DIRECTION_MARKER, label = "SE", x = 0.85f, y = 0.8f),
                LayoutAnnotation(type = AnnotationType.NORTH_ARROW, label = "North", x = 0.2f, y = 0.2f),
                LayoutAnnotation(type = AnnotationType.ROOM_LABEL, label = "Machinery Area", x = 0.5f, y = 0.5f)
            )
        )
        val sourceBmp = mediaManager.loadRotatedBitmap(drawingMedia.filePath)!!
        val annotatedBmp = WorkspaceRenderer.renderAnnotatedBitmap(sourceBmp, workspaceMeta)
        val annotatedFile = mediaManager.createAnnotatedImageFile(reportId)
        FileOutputStream(annotatedFile).use { out ->
            annotatedBmp.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        sourceBmp.recycle()
        annotatedBmp.recycle()
        repository.updateMedia(
            drawingMedia.copy(
                annotatedFilePath = annotatedFile.absolutePath,
                annotationsJson = JsonCodec.encodeWorkspaceMetadata(workspaceMeta)
            )
        )
        assertEquals(originalSizeBefore, File(drawingMedia.filePath).length())
        assertTrue(annotatedFile.exists())

        // 5. Add 22 Vastu analysis rows across categories & attach photo to row 1
        for (i in 1..22) {
            val problemName = if (i % 2 == 0) "Machinery breakdown #$i" else "Financial cashflow issue #$i"
            val row = repository.addAnalysisRow(reportId, problemName)
            val updatedRow = row.copy(
                responsibleDirectionZone = if (i % 2 == 0) "SE (Southeast / Agneya)" else "NE (Northeast / Ishanya)",
                problemObserved = "Detailed site observation for row #$i with long text wrapping verification.",
                antiActivity = "Heavy storage #$i",
                antiUtility = "Electrical load #$i",
                antiObjects = "Scrap metal #$i",
                suggestedRemedy = "Remedy A for #$i\nRemedy B for #$i",
                remedyPriority = if (i % 3 == 0) "High" else "Medium",
                waitingPeriod = "30 days",
                activitiesToStop = "Stop clutter in corner #$i",
                objectsToRemove = "Broken scrap #$i",
                objectsToAdd = "Copper strip #$i",
                notes = "Consultant follow-up note #$i",
                attachedMediaIdsJson = if (i <= 2) JsonCodec.encodeStringList(listOf(photoMedia.id, drawingMedia.id)) else "[]"
            )
            repository.upsertAnalysisRow(updatedRow)
        }

        // 6. Verify reload & multi-page PDF generation
        val fullBundle = repository.getReportBundle(reportId)!!
        assertEquals(22, fullBundle.orderedAnalysisRows.size)
        assertEquals(2, fullBundle.orderedMediaItems.size)
        assertTrue(fullBundle.orderedMediaItems.any { it.isMainSiteLayout })

        val pdfResult = pdfGenerator.generateReportPdf(fullBundle, AppSettings())
        assertTrue(
            "PDF failed: ${pdfResult.exceptionOrNull()?.stackTraceToString()}",
            pdfResult.isSuccess
        )
        val pdfEntity = pdfResult.getOrNull()!!
        assertTrue(File(pdfEntity.filePath).exists())
        assertTrue(pdfEntity.fileSizeBytes > 5000L)
        assertTrue("Expected multipage PDF, got ${pdfEntity.pageCount}", pdfEntity.pageCount >= 2)
        assertTrue(pdfEntity.fileName.startsWith("Vedicra_Vastu_Report_Rajeshwar_Sharma_"))
        repository.recordExportedPdf(pdfEntity)

        // 7. Search verification
        val searchHit = repository.searchReports("Rajeshwar").first()
        assertEquals(1, searchHit.size)

        // 8. Resetting current form must clear only target report & preserve Protected Client
        repository.resetUnsavedForm(reportId)
        val resetBundle = repository.getReportBundle(reportId)!!
        assertEquals("", resetBundle.report.clientName)
        assertEquals(0, resetBundle.analysisRows.size)
        assertNotNull(repository.getReportBundle(otherReportId))
        assertEquals("Protected Client", repository.getReportBundle(otherReportId)!!.report.clientName)

        // 9. Delete report completely & verify Protected Client remains untouched
        repository.deleteReportCompletely(reportId)
        assertNull(repository.getReportBundle(reportId))
        assertNotNull(repository.getReportBundle(otherReportId))
        assertFalse(File(pdfEntity.filePath).exists())
    }

    @Test
    fun unifiedProblemsAnalysisRemedies_tabCombined_andInlineRowsRendered() {
        val sampleRows = listOf(
            AnalysisRowEntity(
                id = "r1",
                reportId = "rep1",
                sortOrder = 0,
                problemCategory = "Financial & Business",
                clientProblem = "Sales problems",
                responsibleDirectionZone = "N (North)",
                notes = "Red wall in North corner",
                suggestedRemedy = "Remove red colour, apply blue strip",
                remedyPriority = "High",
                waitingPeriod = "30 days"
            ),
            AnalysisRowEntity(
                id = "r2",
                reportId = "rep1",
                sortOrder = 1,
                problemCategory = "Health & Wellbeing",
                clientProblem = "Major health problems",
                responsibleDirectionZone = "NNE (North-Northeast)",
                notes = "Toilet located in NNE",
                suggestedRemedy = "Treat toilet with blue metal strip",
                remedyPriority = "High",
                waitingPeriod = "45 days"
            )
        )

        composeRule.setContent {
            VedicraVastuTheme(themeMode = AppThemeMode.IVORY_GOLD) {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    UnifiedProblemsAnalysisRemediesSection(
                        report = ReportEntity(id = "rep1", problemSince = "2 years"),
                        rows = sampleRows,
                        mediaItems = emptyList(),
                        onUpdateReport = {},
                        onTogglePredefinedProblem = {},
                        onAddCustomProblem = { _, _ -> },
                        onAddRow = { _, _ -> },
                        onUpdateRow = {},
                        onDuplicateRow = {},
                        onDeleteRow = {},
                        onToggleRowAttachment = { _, _ -> }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Verify inline Problem dropdown, Expected Zones Responsible, Remarks & Remedies fields for Row 1 & Row 2
        composeRule.onNodeWithTag("problem_dropdown_row_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("expected_zones_row_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("remarks_input_row_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("remedies_input_row_1").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Sales problems").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("N (North)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Red wall in North corner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Remove red colour, apply blue strip").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("problem_dropdown_row_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Major health problems").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Problem Duration").performScrollTo().assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Since when have you been facing these problems?")
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun reportEditorScreen_hasUnifiedProblemsRemediesTab_withoutSeparateThreeTabs() = runTest {
        val reportId = repository.createNewReport()
        val editorVm = ReportEditorViewModel(reportId, repository, pdfGenerator)

        composeRule.setContent {
            VedicraVastuTheme(themeMode = AppThemeMode.IVORY_GOLD) {
                ReportEditorScreen(
                    viewModel = editorVm,
                    initialTab = 3,
                    onSelectTheme = {},
                    onBack = {},
                    onLaunchCamera = { _, _, _, _, _, _, _ -> },
                    onOpenPhotoViewer = {},
                    onOpenLayoutWorkspace = {},
                    onShowMessage = {}
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("editor_tab_2").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        // Degrees & Compass at index 2; Entrance Details at index 3; Unified Problems & Remedies at index 5; total 10 tabs (0..9)
        composeRule.onNodeWithTag("editor_tab_2").performScrollTo().assertIsDisplayed().assertTextEquals("Degrees & Compass")
        composeRule.onNodeWithTag("editor_tab_3").performScrollTo().assertIsDisplayed().assertTextEquals("Entrance Details")
        composeRule.onNodeWithTag("editor_tab_5").performScrollTo().assertIsDisplayed().assertTextEquals("Problems & Remedies")
        assertEquals(0, composeRule.onAllNodesWithTag("editor_tab_10").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Analysis Table").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Problems").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Remedies").fetchSemanticsNodes().size)

        // All predefined problems map to valid categories
        VastuConstants.predefinedProblems.forEach { p ->
            assertTrue(VastuConstants.categoryForProblem(p) in VastuConstants.problemCategories)
        }
    }

    @Test
    fun degreesAndCompassTab_showsCompassSection_shaktiChakra_andNotesDegreesIntoReport() {
        var currentReport = ReportEntity(id = "rep_deg_1")
        val addedEntrances = mutableListOf<EntranceDetail>()

        composeRule.setContent {
            VedicraVastuTheme(themeMode = AppThemeMode.IVORY_GOLD) {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DegreesAndCompassSection(
                        report = currentReport,
                        onUpdate = { transform -> currentReport = transform(currentReport) },
                        onAddEntrance = { addedEntrances.add(it) },
                        onShowMessage = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("COMPASS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("degrees_tab_shakti_chakra").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("compass_top_degree_hud").performScrollTo().assertIsDisplayed()

        // Tap "Note to Degrees of Plot" and verify 0°/360° North (Between N4 & N5) is recorded
        composeRule.onNodeWithTag("note_plot_degree_button").performScrollTo().performClick()
        assertTrue(currentReport.plotDegrees.contains("Between N4 & N5"))
        assertTrue(currentReport.propertyObservations.contains("Compass Plot Reading:"))

        // Tap "Add Entrance" and verify entrance degree entry added
        composeRule.onNodeWithTag("note_add_entrance_degree_button").performScrollTo().performClick()
        assertEquals(1, addedEntrances.size)
        assertEquals("North", addedEntrances.first().direction)
    }

    @Test
    fun vastuChakraSpec_0DegBetweenN4AndN5_all32PadasAnd16ZonesExact() {
        assertEquals(32, VastuChakraSpec.padas32.size)
        assertEquals(16, VastuChakraSpec.zones16.size)

        // Verify 0° North sits strictly between N4 (ends 360°) and N5 (starts 0°)
        val justLeftOfNorth = VastuCompassMath.padaForDegree(359.5f)
        val justRightOfNorth = VastuCompassMath.padaForDegree(0.5f)
        assertEquals("N4", justLeftOfNorth.code)
        assertEquals("Bhallat", justLeftOfNorth.deity)
        assertTrue(justLeftOfNorth.isAuspicious)

        assertEquals("N5", justRightOfNorth.code)
        assertEquals("Soma", justRightOfNorth.deity)
        assertTrue(justRightOfNorth.isAuspicious)

        // Both N4 and N5 belong to North (New Clients / Orders)
        assertEquals("N", VastuCompassMath.zoneForDegree(359.5f).code)
        assertEquals("N", VastuCompassMath.zoneForDegree(0.5f).code)
        assertEquals("New Clients / Orders", VastuCompassMath.zoneForDegree(0.0f).attributeTitle)
        assertTrue(VastuCompassMath.isOnNorthBoundary(0.2f))
        assertTrue(VastuCompassMath.isOnNorthBoundary(359.8f))

        // Verify exact 10 auspicious padas
        val auspiciousCodes = VastuChakraSpec.padas32.filter { it.isAuspicious }.map { it.code }.toSet()
        assertEquals(setOf("N3", "N4", "N5", "E3", "E4", "S3", "S4", "W3", "W4", "W5"), auspiciousCodes)

        // Verify cardinal crossings (45°, 90°, 135°, 180°, 225°, 270°, 315°)
        assertEquals("NE", VastuCompassMath.zoneForDegree(45f).code)
        assertEquals("E", VastuCompassMath.zoneForDegree(90f).code)
        assertEquals("SE", VastuCompassMath.zoneForDegree(135f).code)
        assertEquals("S", VastuCompassMath.zoneForDegree(180f).code)
        assertEquals("SW", VastuCompassMath.zoneForDegree(225f).code)
        assertEquals("W", VastuCompassMath.zoneForDegree(270f).code)
        assertEquals("NW", VastuCompassMath.zoneForDegree(315f).code)
    }

    @Test
    fun compassSensorMath_computesAzimuthAndTiltFromAccelerometerAndMagnetometer() {
        // Flat horizontal phone pointing East (+Y in phone frame = East magnetic field)
        // Gravity: (0, 0, 9.81), Magnetic field pointing East when phone top faces East => (0, -40, -20) or North => (0, 40, -20)
        val flatGravity = floatArrayOf(0f, 0f, 9.81f)
        val northMagnet = floatArrayOf(0f, 40f, -20f)
        val flatReading = VastuCompassMath.computeOrientationFromVectors(
            accel = flatGravity,
            magnet = northMagnet
        )
        assertTrue("Expected flat tilt <= 2°, got ${flatReading.tiltFromLevelDeg}", flatReading.tiltFromLevelDeg <= 2.0f)
        assertTrue(flatReading.isLevel)
        assertFalse(flatReading.isUprightCameraMode)

        // Tilted phone (~30° tilt away from flat)
        val tiltedGravity = floatArrayOf(4.9f, 0f, 8.49f)
        val tiltedReading = VastuCompassMath.computeOrientationFromVectors(
            accel = tiltedGravity,
            magnet = northMagnet
        )
        assertTrue("Expected ~30° tilt, got ${tiltedReading.tiltFromLevelDeg}", tiltedReading.tiltFromLevelDeg in 27f..33f)
        assertFalse(tiltedReading.isLevel)

        // Circular azimuth smoothing across 358° -> 2° wraps cleanly through 0°, not 180°
        val smoothed = VastuCompassMath.smoothAzimuth(358f, 2f, alpha = 0.5f)
        assertTrue("Expected wrap near 0°/360°, got $smoothed", smoothed < 2f || smoothed > 358f)
    }

    @Test
    fun workspaceRenderer_andTopDegreeTiltHud_renderCleanly() {
        val base = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
        base.eraseColor(android.graphics.Color.rgb(248, 245, 236))
        val out = WorkspaceRenderer.renderAnnotatedBitmap(
            sourceBitmap = base,
            metadata = WorkspaceMetadata(
                showGridOverlay = true,
                gridRotationDegrees = 15.5f,
                overlayStyle = GridOverlayStyle.BOTH
            )
        )
        assertEquals(600, out.width)
        assertEquals(600, out.height)
        base.recycle()
        out.recycle()

        composeRule.setContent {
            VedicraVastuTheme(themeMode = AppThemeMode.IVORY_GOLD) {
                androidx.compose.foundation.layout.Column {
                    CompassTopDegreeTiltHud(
                        reading = CompassReading(
                            azimuthDeg = 0.0f,
                            pitchDeg = 12.0f,
                            rollDeg = 4.5f,
                            tiltFromLevelDeg = 4.5f,
                            isLevel = false,
                            zone = VastuCompassMath.zoneForDegree(0.0f),
                            pada = VastuCompassMath.padaForDegree(0.0f),
                            hasHardwareSensors = true
                        ),
                        isLocked = false,
                        onToggleLock = {}
                    )
                    VastuChakraCanvas(
                        rotationDegrees = 0f,
                        showTopLubberNeedle = true,
                        modifier = Modifier
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("compass_top_degree_hud").assertIsDisplayed()
        composeRule.onNodeWithText("Tilt: 4.5°", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Between N4 & N5", substring = true).assertIsDisplayed()
    }

    @Test
    fun entranceDetailsSection_allowsMultiSelect_andShowsDevtasAndAttributesBelow() {
        var currentReport by androidx.compose.runtime.mutableStateOf(ReportEntity(id = "rep_ent_1"))

        composeRule.setContent {
            VedicraVastuTheme(themeMode = AppThemeMode.IVORY_GOLD) {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    EntranceDetailsSection(
                        report = currentReport,
                        onUpdate = { transform -> currentReport = transform(currentReport) },
                        onAddEntrance = { entrance ->
                            val list = JsonCodec.decodeEntrances(currentReport.additionalEntrancesJson).toMutableList()
                            list.add(entrance)
                            currentReport = currentReport.copy(
                                additionalEntrancesJson = JsonCodec.encodeEntrances(list)
                            )
                        },
                        onDeleteEntrance = { id ->
                            val list = JsonCodec.decodeEntrances(currentReport.additionalEntrancesJson)
                                .filterNot { it.id == id }
                            currentReport = currentReport.copy(
                                additionalEntrancesJson = JsonCodec.encodeEntrances(list)
                            )
                        },
                        onShowMessage = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Select two used entrances: N3 (Mukhya) and E3 (Jayant)
        composeRule.onNodeWithTag("entrance_pada_chip_N3").performScrollTo().performClick()
        composeRule.onNodeWithTag("entrance_pada_chip_E3").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Both should appear in the summary and detailed Devtas + Attributes cards below
        composeRule.onNodeWithTag("selected_entrance_detail_card_N3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("entrance_devta_text_N3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("entrance_attributes_text_N3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("entrance_effect_text_N3").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("selected_entrance_detail_card_E3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("entrance_devta_text_E3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("entrance_attributes_text_E3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("entrance_effect_text_E3").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun reportsHubScreen_providesSearchAllRecentAndExportedPdfsTabs() {
        val r1 = ReportEntity(id = "rep_a", clientName = "Anand Textiles", city = "Surat", isDraft = false)
        val r2 = ReportEntity(id = "rep_b", clientName = "Mehta Villa", city = "Pune", isDraft = false)
        val pdf1 = com.example.data.local.ExportedPdfEntity(
            id = "pdf_1",
            reportId = "rep_a",
            clientName = "Anand Textiles",
            fileName = "Vedicra_Anand_Textiles.pdf",
            filePath = "/tmp/Vedicra_Anand_Textiles.pdf",
            pageCount = 4
        )
        var query by androidx.compose.runtime.mutableStateOf("")

        composeRule.setContent {
            VedicraVastuTheme(themeMode = AppThemeMode.IVORY_GOLD) {
                ReportsHubScreen(
                    initialTab = 0,
                    query = query,
                    savedReports = listOf(r1, r2),
                    recentReports = listOf(r2),
                    exportedPdfs = listOf(pdf1),
                    onQueryChange = { query = it },
                    onBack = {},
                    onOpenReport = {},
                    onDuplicateReport = {},
                    onGeneratePdf = { _, _ -> },
                    onDeleteReport = {},
                    onDeletePdf = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("reports_hub_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("reports_tab_all").assertIsDisplayed()
        composeRule.onNodeWithTag("reports_tab_recent").assertIsDisplayed()
        composeRule.onNodeWithTag("reports_tab_pdfs").assertIsDisplayed()
        composeRule.onNodeWithText("Anand Textiles").assertIsDisplayed()
        composeRule.onNodeWithText("Mehta Villa").assertIsDisplayed()

        composeRule.onNodeWithTag("reports_tab_pdfs").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Vedicra_Anand_Textiles.pdf").assertIsDisplayed()
    }

    private fun createTestJpeg(reportId: String, name: String, color: Int): File {
        val file = mediaManager.createOriginalImageFile(reportId, name.substringBefore("."))
        val bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(color)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bmp.recycle()
        return file
    }
}
