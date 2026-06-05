package io.openappex.pdfunlocker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.openappex.pdfunlocker.ui.theme.PDFUnlockerTheme

@Composable
fun UnlockRoute(
    onOpenSettings: () -> Unit,
    viewModel: UnlockViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            viewModel.onPdfSelectionCancelled()
        } else {
            viewModel.onPdfSelected(uri, uri.displayName(context))
        }
    }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onSaveUnlockedPdfClicked()
        } else {
            viewModel.onStoragePermissionDenied()
        }
    }

    UnlockScreen(
        uiState = uiState,
        onSelectPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
        onPasswordChanged = viewModel::onPasswordChanged,
        onOpenSettings = onOpenSettings,
        onSaveUnlockedPdf = {
            if (context.needsLegacyDownloadsPermission()) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                viewModel.onSaveUnlockedPdfClicked()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    uiState: UnlockUiState,
    onSelectPdf: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSaveUnlockedPdf: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "PDF Unlocker") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PdfSelectionSection(
                selectedPdfName = uiState.selectedPdfName,
                onSelectPdf = onSelectPdf
            )
            PasswordSection(
                password = uiState.password,
                onPasswordChanged = onPasswordChanged
            )
            Button(
                onClick = onSaveUnlockedPdf,
                enabled = uiState.canRequestSave,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(text = "Save Unlocked PDF")
            }
            StatusSection(statusText = uiState.statusText)
        }
    }
}

@Composable
private fun PdfSelectionSection(
    selectedPdfName: String?,
    onSelectPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Encrypted PDF",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = selectedPdfName ?: "No PDF selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onSelectPdf,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Select PDF")
            }
        }
    }
}

@Composable
private fun PasswordSection(
    password: String,
    onPasswordChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChanged,
        label = { Text(text = "PDF Password") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(text = if (passwordVisible) "Hide" else "Show")
            }
        }
    )
}

@Composable
private fun StatusSection(
    statusText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun Uri.displayName(context: Context): String {
    val resolver = context.contentResolver
    resolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex)
        }
    }
    return lastPathSegment ?: "Selected PDF"
}

private fun Context.needsLegacyDownloadsPermission(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
}

@Preview(showBackground = true)
@Composable
private fun UnlockScreenPreview() {
    PDFUnlockerTheme {
        UnlockScreen(
            uiState = UnlockUiState(
                selectedPdfName = "locked-document.pdf",
                statusText = "Selected locked-document.pdf. Enter the PDF password to continue."
            ),
            onSelectPdf = {},
            onPasswordChanged = {},
            onSaveUnlockedPdf = {},
            onOpenSettings = {}
        )
    }
}
