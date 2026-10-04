package com.aprax.htmlrun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aprax.htmlrun.AppViewModel
import com.aprax.htmlrun.data.INDENT_RANGE
import com.aprax.htmlrun.data.EditorFont
import com.aprax.htmlrun.data.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings = viewModel.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Appearance, editor and preview behaviour",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingsSection(title = "Appearance", icon = Icons.Rounded.Palette) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ChoiceChip(
                        label = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.updateSettings { it.copy(themeMode = mode) } },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Editor font: ${settings.editorFont.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorFont.entries.forEach { font ->
                    ChoiceChip(
                        label = font.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = settings.editorFont == font,
                        onClick = { viewModel.updateSettings { it.copy(editorFont = font) } },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Font size: ${settings.fontSizeSp} sp",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = settings.fontSizeSp.toFloat(),
                onValueChange = { value ->
                    viewModel.updateSettings { it.copy(fontSizeSp = value.toInt().coerceIn(it.fontSizeRange)) }
                },
                valueRange = settings.fontSizeRange.first.toFloat()..settings.fontSizeRange.last.toFloat(),
                steps = settings.fontSizeRange.count() - 2,
            )
        }

        SettingsSection(title = "Editor", icon = Icons.Rounded.Code) {
            ToggleRow(
                title = "Line numbers",
                subtitle = "Show the gutter beside your code",
                checked = settings.showLineNumbers,
                onChange = { value -> viewModel.updateSettings { it.copy(showLineNumbers = value) } },
            )
            ToggleRow(
                title = "Word wrap",
                subtitle = "Scroll long lines instead of wrapping them",
                checked = settings.wordWrap,
                onChange = { value -> viewModel.updateSettings { it.copy(wordWrap = value) } },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Indent size: ${settings.indentSize} spaces",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                INDENT_RANGE.forEach { size ->
                    ChoiceChip(
                        label = size.toString(),
                        selected = settings.indentSize == size,
                        onClick = { viewModel.updateSettings { it.copy(indentSize = size) } },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tab and Enter keep the indentation of the previous line.",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        SettingsSection(title = "Preview", icon = Icons.Rounded.Settings) {
            ToggleRow(
                title = "JavaScript",
                subtitle = "Allow scripts to run in the preview",
                checked = settings.javaScriptEnabled,
                onChange = { value -> viewModel.updateSettings { it.copy(javaScriptEnabled = value) } },
            )
            ToggleRow(
                title = "Reload project files",
                subtitle = "Copy the folder again every time you open the preview",
                checked = settings.autoReloadPreview,
                onChange = { value -> viewModel.updateSettings { it.copy(autoReloadPreview = value) } },
            )
        }

        SettingsSection(title = "Files", icon = Icons.Rounded.Settings) {
            ToggleRow(
                title = "Autosave",
                subtitle = "Write changes back to the project folder automatically",
                checked = settings.autosave,
                onChange = { value -> viewModel.updateSettings { it.copy(autosave = value) } },
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "HTML Runner",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        fontSize = 13.sp,
        color = foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
