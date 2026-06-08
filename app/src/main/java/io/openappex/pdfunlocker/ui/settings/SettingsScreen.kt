package io.openappex.pdfunlocker.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.openappex.pdfunlocker.data.settings.AppSettings
import io.openappex.pdfunlocker.data.settings.AppTheme
import io.openappex.pdfunlocker.data.settings.DefaultOutputFolderLabel
import io.openappex.pdfunlocker.ui.theme.PDFUnlockerTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.persistTreePermission(uri)
            viewModel.onOutputFolderSelected(
                uri = uri.toString(),
                label = uri.folderLabel()
            )
        }
    }

    BackHandler(onBack = onBack)

    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectOutputFolder = { folderPicker.launch(null) },
        onResetOutputFolder = viewModel::onResetOutputFolder,
        onThemeChanged = viewModel::onThemeChanged
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSelectOutputFolder: () -> Unit,
    onResetOutputFolder: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutputFilenameSection()
            OutputFolderSection(
                folderLabel = settings.outputFolderLabel,
                isDefaultFolder = settings.outputFolderUri == null,
                onSelectFolder = onSelectOutputFolder,
                onResetFolder = onResetOutputFolder
            )
            ThemeSection(
                selectedTheme = settings.theme,
                onThemeChanged = onThemeChanged
            )
            AppInfoSection()
        }
    }
}

@Composable
private fun OutputFilenameSection(
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
                text = "Output filename format",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = true,
                        onClick = { },
                        role = Role.RadioButton,
                        enabled = false
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = true,
                    onClick = null,
                    enabled = false
                )
                Text(
                    text = "_unlocked_DDMMYY_HHMMSS",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = "Example: document_unlocked_250226_143005.pdf",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OutputFolderSection(
    folderLabel: String,
    isDefaultFolder: Boolean,
    onSelectFolder: () -> Unit,
    onResetFolder: () -> Unit,
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
                text = "Output folder",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = folderLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectFolder,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Select Folder")
                }
                TextButton(
                    onClick = onResetFolder,
                    enabled = !isDefaultFolder
                ) {
                    Text(text = "Default")
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(
    selectedTheme: AppTheme,
    onThemeChanged: (AppTheme) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium
            )
            AppTheme.entries.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedTheme == theme,
                            onClick = { onThemeChanged(theme) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTheme == theme,
                        onClick = { onThemeChanged(theme) }
                    )
                    Text(
                        text = theme.displayName(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppInfoSection(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/OpenAppex/pdfunlocker"

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
                text = "About",
                style = MaterialTheme.typography.titleMedium
            )
            InfoRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null
                    )
                },
                title = "Version",
                value = "2026.01"
            )
            InfoRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null
                    )
                },
                title = "State",
                value = "Stable"
            )
            InfoRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Business,
                        contentDescription = null
                    )
                },
                title = "Organization",
                value = "OpenAppex"
            )
            InfoRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null
                    )
                },
                title = "Developer",
                value = "Amit Lohar, using AI assistance"
            )
            TextButton(
                onClick = { uriHandler.openUri(githubUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "View on GitHub")
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun AppTheme.displayName(): String {
    return when (this) {
        AppTheme.System -> "System"
        AppTheme.Light -> "Light"
        AppTheme.Dark -> "Dark"
        AppTheme.Amoled -> "AMOLED"
    }
}

private fun Context.persistTreePermission(uri: Uri) {
    try {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // Some providers grant access for the current process without persistable permissions.
    }
}

private fun Uri.folderLabel(): String {
    val treeDocumentId = DocumentsContract.getTreeDocumentId(this)
    if (treeDocumentId.isNotBlank()) {
        return treeDocumentId.substringAfterLast(':').ifBlank { treeDocumentId }
    }
    return lastPathSegment ?: DefaultOutputFolderLabel
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PDFUnlockerTheme {
        SettingsScreen(
            uiState = SettingsUiState(settings = AppSettings()),
            onBack = {},
            onSelectOutputFolder = {},
            onResetOutputFolder = {},
            onThemeChanged = {}
        )
    }
}
