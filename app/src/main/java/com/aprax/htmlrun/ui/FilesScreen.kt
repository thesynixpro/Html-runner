package com.aprax.htmlrun.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aprax.htmlrun.AppViewModel
import com.aprax.htmlrun.editor.Language
import com.aprax.htmlrun.project.ProjectFile
import com.aprax.htmlrun.project.ProjectFolder

private data class TreeRow(
    val folder: ProjectFolder?,
    val file: ProjectFile?,
    val depth: Int,
    val expanded: Boolean,
)

@Composable
fun FilesScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onOpenFile: () -> Unit,
) {
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::selectFolder) }

    var expandedRaw by rememberSaveable { mutableStateOf("") }
    val expanded = remember(expandedRaw) {
        expandedRaw.split('\n').filter { it.isNotEmpty() }.toSet()
    }
    var targetFolder by rememberSaveable { mutableStateOf("") }
    var showNewFile by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val tree = viewModel.tree

    Column(modifier = Modifier.fillMaxSize()) {
        FilesHeader(
            projectName = viewModel.projectName,
            hasProject = viewModel.hasProject,
            menuOpen = menuOpen,
            onMenuToggle = { menuOpen = it },
            onPickFolder = { folderPicker.launch(null) },
            onRefresh = viewModel::refreshProject,
            onForget = viewModel::forgetProject,
            onNewFile = { showNewFile = true },
            onNewFolder = { showNewFolder = true },
        )

        if (!viewModel.hasProject || tree == null) {
            EmptyProject(onPickFolder = { folderPicker.launch(null) }, contentPadding = contentPadding)
            return@Column
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        val rows = remember(tree, expanded) {
            val collected = ArrayList<TreeRow>()
            tree?.let { flatten(it, 0, expanded, collected) }
            collected
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Text(
                    text = if (targetFolder.isEmpty()) {
                        "Project root"
                    } else {
                        "New items go into /$targetFolder"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                )
            }

            items(rows, key = { rowKey(it) }) { row ->
                when {
                    row.folder != null -> FolderRow(
                        folder = row.folder,
                        depth = row.depth,
                        expanded = row.expanded,
                        onToggle = {
                            val next = if (row.expanded) {
                                expanded - row.folder.path
                            } else {
                                expanded + row.folder.path
                            }
                            expandedRaw = next.joinToString("\n")
                            targetFolder = row.folder.path
                        },
                    )

                    else -> FileRow(
                        file = row.file!!,
                        depth = row.depth,
                        active = viewModel.openFile?.path == row.file.path,
                        onClick = {
                            viewModel.openPath(row.file.path)
                            onOpenFile()
                        },
                        onDelete = { viewModel.deleteFile(row.file) },
                    )
                }
            }

            if (rows.isEmpty()) {
                item {
                    Text(
                        text = "This folder is empty",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        }
    }

    if (showNewFile) {
        NameDialog(
            title = "New file",
            label = "File name",
            initial = "",
            onConfirm = {
                viewModel.createFile(targetFolder, it)
                showNewFile = false
            },
            onDismiss = { showNewFile = false },
        )
    }

    if (showNewFolder) {
        NameDialog(
            title = "New folder",
            label = "Folder name",
            initial = "",
            onConfirm = {
                viewModel.createFolder(targetFolder, it)
                showNewFolder = false
            },
            onDismiss = { showNewFolder = false },
        )
    }
}

@Composable
private fun FilesHeader(
    projectName: String,
    hasProject: Boolean,
    menuOpen: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onPickFolder: () -> Unit,
    onRefresh: () -> Unit,
    onForget: () -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Files",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = projectName.ifEmpty { "No project folder selected" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (hasProject) {
            IconButton(onClick = onNewFile) {
                Icon(Icons.Rounded.Add, contentDescription = "New file")
            }
            IconButton(onClick = onNewFolder) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = "New folder")
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
            }
        }

        Box {
            IconButton(onClick = { onMenuToggle(true) }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuToggle(false) }) {
                DropdownMenuItem(
                    text = { Text(if (hasProject) "Choose another folder" else "Choose project folder") },
                    onClick = {
                        onMenuToggle(false)
                        onPickFolder()
                    },
                    leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                )
                if (hasProject) {
                    DropdownMenuItem(
                        text = { Text("Close project") },
                        onClick = {
                            onMenuToggle(false)
                            onForget()
                        },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyProject(
    onPickFolder: () -> Unit,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Choose your project folder",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Every file and folder inside is loaded with its structure intact, " +
                    "so the preview can use your relative paths.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onPickFolder) {
                    Icon(Icons.Rounded.Folder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Select folder")
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: ProjectFolder,
    depth: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = (14 + depth * 16).dp, end = 18.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Rounded.FolderOpen else Icons.Rounded.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = folder.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = folder.files.size.toString(),
            style = MaterialTheme.typography.labelSmall,
        )
        Icon(
            imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FileRow(
    file: ProjectFile,
    depth: Int,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (14 + depth * 16).dp, end = 4.dp)
            .background(
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 0.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconFor(file),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
            )
            Text(
                text = Language.ofFile(file.name).name.lowercase() + " - " + sizeLabel(file.size),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun flatten(
    folder: ProjectFolder,
    depth: Int,
    expanded: Set<String>,
    rows: MutableList<TreeRow>,
) {
    for (child in folder.folders) {
        val isOpen = child.path in expanded
        rows.add(TreeRow(folder = child, file = null, depth = depth, expanded = isOpen))
        if (isOpen) flatten(child, depth + 1, expanded, rows)
    }
    for (file in folder.files) {
        rows.add(TreeRow(folder = null, file = file, depth = depth, expanded = false))
    }
}

private fun rowKey(row: TreeRow): String = row.folder?.path ?: row.file?.path.orEmpty()

private fun iconFor(file: ProjectFile) = when (Language.ofFile(file.name)) {
    Language.HTML -> Icons.Rounded.Code
    Language.CSS, Language.JS -> Icons.Rounded.Code
    else -> if (file.extension in imageExtensions) Icons.Rounded.Description else Icons.Rounded.InsertDriveFile
}

private fun sizeLabel(size: Long): String = when {
    size <= 0 -> "empty"
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
}

private val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "bmp")
