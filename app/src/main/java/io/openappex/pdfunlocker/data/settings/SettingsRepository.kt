package io.openappex.pdfunlocker.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"
)

class SettingsRepository(
    private val context: Context
) {
    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            outputFilenameSuffix = preferences[OutputFilenameSuffixKey] ?: "_unlocked",
            outputFolderUri = preferences[OutputFolderUriKey],
            outputFolderLabel = preferences[OutputFolderLabelKey] ?: DefaultOutputFolderLabel,
            theme = preferences[ThemeKey]?.toAppTheme() ?: AppTheme.System
        )
    }

    suspend fun updateOutputFilenameSuffix(suffix: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[OutputFilenameSuffixKey] = suffix
        }
    }

    suspend fun useDefaultOutputFilenameSuffix() {
        context.settingsDataStore.edit { preferences ->
            preferences[OutputFilenameSuffixKey] = "_unlocked"
        }
    }

    suspend fun updateOutputFolder(uri: String, label: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[OutputFolderUriKey] = uri
            preferences[OutputFolderLabelKey] = label
        }
    }

    suspend fun resetOutputFolder() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(OutputFolderUriKey)
            preferences[OutputFolderLabelKey] = DefaultOutputFolderLabel
        }
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.settingsDataStore.edit { preferences ->
            preferences[ThemeKey] = theme.name
        }
    }

    private fun String.toAppTheme(): AppTheme {
        return AppTheme.entries.firstOrNull { it.name == this } ?: AppTheme.System
    }

    private companion object {
        val OutputFilenameSuffixKey = stringPreferencesKey("output_filename_suffix")
        val OutputFolderUriKey = stringPreferencesKey("output_folder_uri")
        val OutputFolderLabelKey = stringPreferencesKey("output_folder_label")
        val ThemeKey = stringPreferencesKey("theme")
    }
}
