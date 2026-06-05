package io.openappex.pdfunlocker.ui

import android.net.Uri

data class UnlockUiState(
    val selectedPdfUri: Uri? = null,
    val selectedPdfName: String? = null,
    val password: String = "",
    val statusText: String = "Select an encrypted PDF to begin.",
    val isProcessing: Boolean = false
) {
    val canRequestSave: Boolean
        get() = !isProcessing
}
