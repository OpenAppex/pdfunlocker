package io.openappex.pdfunlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.openappex.pdfunlocker.data.settings.AppSettings
import io.openappex.pdfunlocker.data.settings.AppTheme
import io.openappex.pdfunlocker.data.settings.SettingsRepository
import io.openappex.pdfunlocker.ui.UnlockRoute
import io.openappex.pdfunlocker.ui.settings.SettingsRoute
import io.openappex.pdfunlocker.ui.theme.PDFUnlockerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _root_ide_package_.io.openappex.pdfunlocker.PDFUnlockerApp()
        }
    }
}

@Composable
private fun PDFUnlockerApp() {
    val context = LocalContext.current.applicationContext
    val settingsRepository = remember {
        _root_ide_package_.io.openappex.pdfunlocker.data.settings.SettingsRepository(context = context)
    }
    val settings by settingsRepository.settingsFlow.collectAsState(initial = _root_ide_package_.io.openappex.pdfunlocker.data.settings.AppSettings())
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (settings.theme) {
        AppTheme.System -> systemDarkTheme
        AppTheme.Light -> false
        AppTheme.Dark,
        AppTheme.Amoled -> true
    }
    val amoledTheme = settings.theme == AppTheme.Amoled
    var currentScreen by remember { mutableStateOf(AppScreen.Unlock) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    PDFUnlockerTheme(
        darkTheme = darkTheme,
        amoledTheme = amoledTheme
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                AppScreen.Unlock -> UnlockRoute(
                    onOpenSettings = { currentScreen = AppScreen.Settings }
                )

                AppScreen.Settings -> SettingsRoute(
                    onBack = { suffixWasBlank ->
                        currentScreen = AppScreen.Unlock
                        if (suffixWasBlank) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Suffix is empty. Default _unlocked will be used.",
                                    actionLabel = "Dismiss",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    }
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private enum class AppScreen {
    Unlock,
    Settings
}
