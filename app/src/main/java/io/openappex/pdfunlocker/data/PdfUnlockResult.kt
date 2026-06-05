package io.openappex.pdfunlocker.data

sealed interface PdfUnlockResult {
    data class Success(val outputPath: String) : PdfUnlockResult
    data object InvalidPassword : PdfUnlockResult
    data class Error(val message: String) : PdfUnlockResult
}
