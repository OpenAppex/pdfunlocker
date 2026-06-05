package io.openappex.pdfunlocker.data.settings

data class AppSettings(
    val outputFilenameSuffix: String = "_unlocked",
    val outputFolderUri: String? = null,
    val outputFolderLabel: String = DefaultOutputFolderLabel,
    val theme: AppTheme = AppTheme.System
)

const val DefaultOutputFolderLabel = "Downloads/PdfUnlocker"
