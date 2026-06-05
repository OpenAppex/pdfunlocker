package io.openappex.pdfunlocker.ui.settings

import io.openappex.pdfunlocker.data.settings.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings()
)
