package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.FontSizeOption
import com.example.model.PdfThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vedicra_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val COMPANY_LOGO_PATH = stringPreferencesKey("company_logo_path")
        val CONSULTANT_NAME = stringPreferencesKey("consultant_name")
        val CONTACT_NUMBER = stringPreferencesKey("contact_number")
        val PDF_THEME = stringPreferencesKey("pdf_theme")
        val DEFAULT_FONT_SIZE = stringPreferencesKey("default_font_size")
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            companyName = prefs[Keys.COMPANY_NAME] ?: "VEDICRA VASTU SOLUTIONS",
            companyLogoPath = prefs[Keys.COMPANY_LOGO_PATH] ?: "",
            consultantName = prefs[Keys.CONSULTANT_NAME] ?: "S. SATISH PATEL",
            contactNumber = prefs[Keys.CONTACT_NUMBER] ?: "9000505678",
            pdfTheme = prefs[Keys.PDF_THEME]?.let { name ->
                PdfThemeOption.entries.firstOrNull { it.name == name }
            } ?: PdfThemeOption.CLASSIC_IVORY_GOLD,
            defaultFontSize = prefs[Keys.DEFAULT_FONT_SIZE]?.let { name ->
                FontSizeOption.entries.firstOrNull { it.name == name }
            } ?: FontSizeOption.STANDARD,
            appTheme = prefs[Keys.APP_THEME]?.let { name ->
                AppThemeMode.entries.firstOrNull { it.name == name }
            } ?: AppThemeMode.IVORY_GOLD
        )
    }

    suspend fun getCurrentSettings(): AppSettings = settingsFlow.first()

    suspend fun updateAppTheme(themeMode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_THEME] = themeMode.name
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COMPANY_NAME] = settings.companyName.ifBlank { "VEDICRA VASTU SOLUTIONS" }
            prefs[Keys.COMPANY_LOGO_PATH] = settings.companyLogoPath
            prefs[Keys.CONSULTANT_NAME] = settings.consultantName.ifBlank { "S. SATISH PATEL" }
            prefs[Keys.CONTACT_NUMBER] = settings.contactNumber.ifBlank { "9000505678" }
            prefs[Keys.PDF_THEME] = settings.pdfTheme.name
            prefs[Keys.DEFAULT_FONT_SIZE] = settings.defaultFontSize.name
            prefs[Keys.APP_THEME] = settings.appTheme.name
        }
    }

    suspend fun updateCompanyLogoPath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COMPANY_LOGO_PATH] = path
        }
    }
}
