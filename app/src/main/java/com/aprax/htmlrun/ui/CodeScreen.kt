package com.aprax.htmlrun.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aprax.htmlrun.AppViewModel
import com.aprax.htmlrun.editor.CodeEditor

@Composable
fun CodeScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onOpenPreview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = contentPadding.calculateBottomPadding()),
    ) {
        CodeHeader(
            fileName = viewModel.openFile?.name,
            dirty = viewModel.isDirty,
            onBack = viewModel::closeFile,
            onSearch = { viewModel.setSearchVisible(true) },
            onSave = viewModel::saveNow,
            onRun = onOpenPreview,
        )

        CodeToolRow(
            enabled = viewModel.openFile != null,
            canFormat = viewModel.canFormat,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onFormat = viewModel::formatDocument,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        AnimatedVisibility(
            visible = viewModel.searchVisible,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            SearchPanel(viewModel = viewModel)
        }

        val file = viewModel.openFile
        if (file == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        modifier = Modifier.size(58.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("No file open", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Pick a file in Files to start editing",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        } else {
            CodeEditor(
                value = viewModel.editorValue,
                onValueChange = viewModel::onEdit,
                language = viewModel.language,
                settings = viewModel.settings,
                searchQuery = viewModel.searchQuery,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onRunShortcut = onOpenPreview,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CodeHeader(
    fileName: String?,
    dirty: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSave: () -> Unit,
    onRun: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 4.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = fileName != null) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close file")
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName ?: "Code",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    fileName == null -> "Open a file to edit it"
                    dirty -> "Unsaved changes"
                    else -> "Saved"
                },
                style = MaterialTheme.typography.labelSmall,
            )
        }

        IconButton(onClick = onSearch, enabled = fileName != null) {
            Icon(Icons.Rounded.Search, contentDescription = "Find and replace")
        }
        IconButton(onClick = onSave, enabled = fileName != null) {
            Icon(Icons.Rounded.Save, contentDescription = "Save")
        }

        Button(
            onClick = onRun,
            enabled = fileName != null,
            colors = ButtonDefaults.buttonColors(),
            modifier = Modifier.height(40.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Preview", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CodeToolRow(
    enabled: Boolean,
    canFormat: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFormat: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onUndo, enabled = enabled) {
            Icon(Icons.Rounded.Undo, contentDescription = "Undo")
        }
        IconButton(onClick = onRedo, enabled = enabled) {
            Icon(Icons.Rounded.Redo, contentDescription = "Redo")
        }
        IconButton(onClick = onFormat, enabled = enabled && canFormat) {
            Icon(Icons.Rounded.FormatAlignLeft, contentDescription = "Format document")
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Tab indents - Ctrl+Enter runs the preview",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SearchPanel(viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text("Find") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    viewModel.searchQuery.isEmpty() -> ""
                    viewModel.matchCount == 0 -> "No results"
                    else -> "${viewModel.currentMatch + 1} of ${viewModel.matchCount}"
                },
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButtonChip("Next", onClick = viewModel::nextMatch)
            Spacer(Modifier.width(6.dp))
            TextButtonChip("Previous", onClick = viewModel::previousMatch)
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = { viewModel.setSearchVisible(false) },
                colors = IconButtonDefaults.iconButtonColors(),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close search")
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = viewModel.replaceText,
                onValueChange = viewModel::setReplaceText,
                label = { Text("Replace with") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButtonChip("One", onClick = viewModel::replaceCurrentMatch, enabled = viewModel.matchCount > 0)
                TextButtonChip("All", onClick = viewModel::replaceAllMatches, enabled = viewModel.matchCount > 0)
            }
        }
    }
}

@Composable
private fun TextButtonChip(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .background(
                color = if (enabled) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                shape = RoundedCornerShape(9.dp),
            )
            .padding(horizontal = 4.dp),
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        ) {
            Text(label, fontSize = 13.sp)
        }
    }
}
