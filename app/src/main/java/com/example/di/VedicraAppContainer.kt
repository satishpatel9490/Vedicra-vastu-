package com.example.di

import android.content.Context
import com.example.data.local.MediaFileManager
import com.example.data.local.SettingsStore
import com.example.data.local.VastuDatabase
import com.example.data.repository.VastuRepository
import com.example.pdf.PdfReportGenerator

class VedicraAppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = VastuDatabase.getInstance(appContext)
    val mediaManager = MediaFileManager(appContext)
    val settingsStore = SettingsStore(appContext)
    val repository = VastuRepository(
        context = appContext,
        dao = database.vastuDao(),
        mediaManager = mediaManager,
        settingsStore = settingsStore
    )
    val pdfGenerator = PdfReportGenerator(
        context = appContext,
        mediaManager = mediaManager
    )
}
