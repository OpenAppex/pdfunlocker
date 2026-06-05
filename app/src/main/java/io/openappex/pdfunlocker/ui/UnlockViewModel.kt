package io.openappex.pdfunlocker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.openappex.pdfunlocker.data.PdfUnlockRepository
import io.openappex.pdfunlocker.data.PdfUnlockResult
import io.openappex.pdfunlocker.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UnlockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PdfUnlockRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    fun onPdfSelected(uri: Uri, displayName: String) {
        _uiState.update {
            it.copy(
                selectedPdfUri = uri,
                selectedPdfName = displayName,
                statusText = "Selected $displayName. Enter the PDF password to continue."
            )
        }
    }

    fun onPdfSelectionCancelled() {
        _uiState.update {
            it.copy(statusText = "PDF selection cancelled.")
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(password = password)
        }
    }

    fun onStoragePermissionDenied() {
        _uiState.update {
            it.copy(statusText = "Storage permission is required to save to Downloads on this Android version.")
        }
    }

    fun onSaveUnlockedPdfClicked() {
        val currentState = uiState.value
        val sourceUri = currentState.selectedPdfUri
        val sourceName = currentState.selectedPdfName
        val password = currentState.password

        when {
            sourceUri == null || sourceName == null -> {
                _uiState.update { it.copy(statusText = "Select an encrypted PDF first.") }
                return
            }

            password.isBlank() -> {
                _uiState.update { it.copy(statusText = "Enter the PDF password before saving.") }
                return
            }

            currentState.isProcessing -> return
        }

        _uiState.update {
            it.copy(
                isProcessing = true,
                statusText = "Processing..."
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val settings = settingsRepository.settingsFlow.first()
            val result = repository.unlockPdf(
                sourceUri = sourceUri,
                sourceName = sourceName,
                password = password,
                settings = settings
            )

            _uiState.update {
                when (result) {
                    PdfUnlockResult.InvalidPassword -> it.copy(
                        isProcessing = false,
                        statusText = "Invalid password"
                    )

                    is PdfUnlockResult.Error -> it.copy(
                        isProcessing = false,
                        statusText = result.message
                    )

                    is PdfUnlockResult.Success -> it.copy(
                        isProcessing = false,
                        password = "",
                        statusText = "PDF unlocked successfully\nOutput file path: ${result.outputPath}"
                    )
                }
            }
        }
    }
}
