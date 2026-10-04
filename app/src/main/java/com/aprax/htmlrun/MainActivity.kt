package com.aprax.htmlrun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import com.aprax.htmlrun.ui.CodeScreen
import com.aprax.htmlrun.ui.FilesScreen
import com.aprax.htmlrun.ui.PreviewScreen
import com.aprax.htmlrun.ui.SettingsScreen
import com.aprax.htmlrun.ui.theme.HtmlRunnerTheme

private enum class Section(val label: String, val icon: ImageVector) {
    FILES("Files", Icons.Rounded.FolderOpen),
    CODE("Code", Icons.Rounded.Code),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HtmlRunnerTheme(themeMode = viewModel.settings.themeMode) {
                RunnerApp(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveNow()
    }
}

@Composable
private fun RunnerApp(viewModel: AppViewModel) {
    var section by rememberSaveable { mutableStateOf(Section.FILES) }
    val snackbarHostState = remember { SnackbarHostState() }
    val statusBars = WindowInsets.statusBars.asPaddingValues()

    val message = viewModel.statusMessage
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    BackHandler(enabled = section != Section.FILES && !viewModel.previewVisible) {
        section = Section.FILES
    }

    if (viewModel.previewVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            PreviewScreen(
                viewModel = viewModel,
                onClose = { viewModel.closePreview() },
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars,
            ) {
                Section.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        onClick = { section = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = padding.calculateStartPadding(layoutDirection),
            end = padding.calculateEndPadding(layoutDirection),
            top = statusBars.calculateTopPadding(),
            bottom = padding.calculateBottomPadding(),
        )

        AnimatedContent(
            targetState = section,
            transitionSpec = {
                fadeIn(animationSpec = tween(160)) togetherWith
                    fadeOut(animationSpec = tween(120))
            },
            label = "section",
            modifier = Modifier.fillMaxSize(),
        ) { target ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (target) {
                    Section.FILES -> FilesScreen(
                        viewModel = viewModel,
                        contentPadding = contentPadding,
                        onOpenFile = { section = Section.CODE },
                    )

                    Section.CODE -> CodeScreen(
                        viewModel = viewModel,
                        contentPadding = contentPadding,
                        onOpenPreview = { viewModel.openPreview() },
                    )

                    Section.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        contentPadding = contentPadding,
                    )
                }
            }
        }
    }
}
