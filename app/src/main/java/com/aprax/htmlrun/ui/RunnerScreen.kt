package com.aprax.htmlrun.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aprax.htmlrun.editor.CodeEditor
import com.aprax.htmlrun.runner.ConsoleEntry
import com.aprax.htmlrun.runner.FileTab
import com.aprax.htmlrun.runner.ProjectFiles
import com.aprax.htmlrun.runner.RunnerViewModel
import kotlinx.coroutines.delay

private val ScreenBackground = Color(0xFF0F1115)
private val PanelBackground = Color(0xFF151922)
private val BorderColor = Color(0xFF252B38)
private val DimText = Color(0xFF7A869C)
private val ConsoleBackground = Color(0xFF0B0D12)
private val OkText = Color(0xFF3ECF8E)
private val ErrorText = Color(0xFFFF6B6B)
private val WarnText = Color(0xFFF0C674)
private val InfoText = Color(0xFFA8C9FF)

private enum class Pane { CODE, SPLIT, PREVIEW }

@Composable
fun RunnerScreen(viewModel: RunnerViewModel = viewModel()) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isWide = configuration.screenWidthDp >= 700

    var pane by rememberSaveable { mutableStateOf(Pane.SPLIT) }
    var consoleVisible by rememberSaveable { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    var resetDialogVisible by remember { mutableStateOf(false) }

    val openProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            consoleVisible = true
            ProjectFiles.read(context, uri)
                .onSuccess { project ->
                    viewModel.openProject(project)
                }
                .onFailure { throwable ->
                    viewModel.appendConsoleEntry("error", throwable.message ?: "Could not open the selected file")
                }
        }
    }

    LaunchedEffect(viewModel.autoRun, viewModel.html, viewModel.css, viewModel.js) {
        if (viewModel.autoRun) {
            delay(700)
            viewModel.run()
        }
    }

    val editorValue = viewModel.editorValues[viewModel.activeTab]
        ?: TextFieldValue(viewModel.textOf(viewModel.activeTab))

    val showEditor = isWide || pane != Pane.PREVIEW
    val showPreview = isWide || pane != Pane.CODE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        TopBar(
            autoRun = viewModel.autoRun,
            errorCount = viewModel.errorCount,
            onRun = viewModel::run,
            onToggleConsole = { consoleVisible = !consoleVisible },
            onMenu = { menuOpen = true },
        )

        FileTabs(
            activeTab = viewModel.activeTab,
            onSelect = viewModel::selectTab,
            showPaneToggle = !isWide,
            pane = pane,
            onPaneChange = { pane = it },
        )

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (showEditor) {
                CodeEditor(
                    value = editorValue,
                    onValueChange = viewModel::onEditorValueChange,
                    language = viewModel.languageOf(viewModel.activeTab),
                    onRunShortcut = viewModel::run,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            if (showEditor && showPreview) {
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderColor))
            }
            if (showPreview) {
                PreviewPane(
                    document = viewModel.previewDocument,
                    onConsoleMessage = viewModel::appendFromBridge,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        if (consoleVisible) {
            ConsolePanel(
                entries = viewModel.consoleEntries,
                onClear = viewModel::clearConsole,
            )
        }

        StatusBar(
            tab = viewModel.activeTab,
            editorValue = editorValue,
            autoRun = viewModel.autoRun,
        )
    }

    if (menuOpen) {
        OverflowMenu(
            autoRun = viewModel.autoRun,
            onToggleAutoRun = viewModel::toggleAutoRun,
            onOpenProject = {
                menuOpen = false
                openProjectLauncher.launch(ProjectFiles.mimeTypes)
            },
            onExport = {
                menuOpen = false
                exportDocument(context, viewModel.previewDocument)
            },
            onClearConsole = {
                menuOpen = false
                viewModel.clearConsole()
            },
            onClearFile = {
                menuOpen = false
                viewModel.clearEditorTab()
            },
            onReset = {
                menuOpen = false
                resetDialogVisible = true
            },
            onDismiss = { menuOpen = false },
        )
    }

    if (resetDialogVisible) {
        ResetDialog(
            onConfirm = {
                resetDialogVisible = false
                viewModel.resetToTemplate()
            },
            onDismiss = { resetDialogVisible = false },
        )
    }
}

@Composable
private fun TopBar(
    autoRun: Boolean,
    errorCount: Int,
    onRun: () -> Unit,
    onToggleConsole: () -> Unit,
    onMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(PanelBackground)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "HTML Runner",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
        if (autoRun) {
            Text(
                text = "AUTO",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = OkText,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1B2A22))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onRun),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Run",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }

        IconButton(onClick = onRun) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Reload preview",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggleConsole) {
            Icon(
                imageVector = Icons.Rounded.Terminal,
                contentDescription = "Toggle console",
                tint = if (errorCount > 0) ErrorText else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onMenu) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FileTabs(
    activeTab: FileTab,
    onSelect: (FileTab) -> Unit,
    showPaneToggle: Boolean,
    pane: Pane,
    onPaneChange: (Pane) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(PanelBackground),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileTab.entries.forEach { tab ->
            val selected = tab == activeTab
            Column(
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = tab.title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else DimText,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showPaneToggle) {
            IconButton(onClick = { onPaneChange(Pane.CODE) }) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = "Editor only",
                    tint = if (pane == Pane.CODE) MaterialTheme.colorScheme.primary else DimText,
                )
            }
            IconButton(onClick = { onPaneChange(Pane.PREVIEW) }) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = "Preview only",
                    tint = if (pane == Pane.PREVIEW) MaterialTheme.colorScheme.primary else DimText,
                )
            }
        }
    }
}

@Composable
private fun ConsolePanel(
    entries: List<ConsoleEntry>,
    onClear: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(ConsoleBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(ConsoleBackground),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "CONSOLE",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = DimText,
                modifier = Modifier.padding(start = 12.dp),
            )
            Text(
                text = entries.size.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF242B3A))
                    .padding(horizontal = 8.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Clear console",
                    tint = DimText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (entries.isEmpty()) {
            Text(
                text = "Console output appears here.",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFF4A5468),
                modifier = Modifier.padding(12.dp),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(entries) { _, entry ->
                    ConsoleRow(entry)
                }
            }
        }
    }
}

@Composable
private fun ConsoleRow(entry: ConsoleEntry) {
    val color = when (entry.level) {
        "error" -> ErrorText
        "warn" -> WarnText
        "info" -> InfoText
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        Text(
            text = when (entry.level) {
                "log" -> ">"
                "info" -> "i"
                "warn" -> "!"
                else -> "x"
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = DimText,
            modifier = Modifier.width(14.dp),
        )
        Text(
            text = entry.text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = color,
        )
    }
}

@Composable
private fun StatusBar(
    tab: FileTab,
    editorValue: TextFieldValue,
    autoRun: Boolean,
) {
    val lines = remember(editorValue.text) { editorValue.text.count { it == '\n' } + 1 }
    val beforeCursor = editorValue.text.take(editorValue.selection.start)
    val lineNumber = beforeCursor.count { it == '\n' } + 1
    val column = beforeCursor.length - beforeCursor.lastIndexOf('\n')

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(PanelBackground)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(tab.fileName, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DimText)
        Text("$lines lines", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DimText)
        Text("Ln $lineNumber, Col $column", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DimText)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (autoRun) "auto run on" else "manual run",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = if (autoRun) OkText else DimText,
        )
    }
}

@Composable
private fun OverflowMenu(
    autoRun: Boolean,
    onToggleAutoRun: () -> Unit,
    onOpenProject: () -> Unit,
    onExport: () -> Unit,
    onClearConsole: () -> Unit,
    onClearFile: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(220.dp)
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        ) {
            MenuRow(
                icon = Icons.Rounded.Check,
                label = "Auto run",
                trailing = {
                    Switch(
                        checked = autoRun,
                        onCheckedChange = { onToggleAutoRun() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
            )
            MenuRow(Icons.Rounded.FolderOpen, "Open project file", onOpenProject)
            MenuRow(Icons.Rounded.Share, "Export HTML", onExport)
            MenuRow(Icons.Rounded.Delete, "Clear console", onClearConsole)
            MenuRow(Icons.Rounded.Delete, "Clear this file", onClearFile)
            MenuRow(Icons.Rounded.RestartAlt, "Reset to template", onReset)
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DimText,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

@Composable
private fun ResetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Popup(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                .padding(20.dp),
        ) {
            Text(
                text = "Reset project?",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The HTML, CSS and JS files will be replaced by the starter template.",
                fontSize = 13.sp,
                color = DimText,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "Cancel",
                    fontSize = 13.sp,
                    color = DimText,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(
                    text = "Reset",
                    fontSize = 13.sp,
                    color = ErrorText,
                    modifier = Modifier
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun exportDocument(context: android.content.Context, document: String) {
    val directory = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
    val file = java.io.File(directory, "index.html")
    file.writeText(document)
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/html"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export HTML"))
}