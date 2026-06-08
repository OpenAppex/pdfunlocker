package io.openappex.pdfunlocker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.openappex.pdfunlocker.data.settings.AppTheme
import io.openappex.pdfunlocker.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val uiState: StateFlow<SettingsUiState> = repository.settingsFlow
        .map { SettingsUiState(settings = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun onOutputFolderSelected(uri: String, label: String) {
        viewModelScope.launch {
            repository.updateOutputFolder(uri, label)
        }
    }

    fun onResetOutputFolder() {
        viewModelScope.launch {
            repository.resetOutputFolder()
        }
    }

    fun onThemeChanged(theme: AppTheme) {
        viewModelScope.launch {
            repository.updateTheme(theme)
        }
    }
}
