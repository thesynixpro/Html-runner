package com.aprax.htmlrun

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aprax.htmlrun.data.AppSettings
import com.aprax.htmlrun.data.SettingsStore
import com.aprax.htmlrun.editor.CodeFormatter
import com.aprax.htmlrun.editor.EditorHistory
import com.aprax.htmlrun.editor.Language
import com.aprax.htmlrun.project.ProjectFile
import com.aprax.htmlrun.project.ProjectFolder
import com.aprax.htmlrun.project.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    private val settingsStore = SettingsStore(application)
    private val history = EditorHistory()

    var settings by mutableStateOf(settingsStore.load())
        private set

    var tree by mutableStateOf<ProjectFolder?>(null)
        private set

    var projectName by mutableStateOf("")
        private set

    var hasProject by mutableStateOf(false)
        private set

    var openFile by mutableStateOf<ProjectFile?>(null)
        private set

    var editorValue by mutableStateOf(TextFieldValue(""))
        private set

    var isDirty by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    var previewVisible by mutableStateOf(false)

    var previewGeneration by mutableStateOf(0)
        private set

    var searchVisible by mutableStateOf(false)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var replaceText by mutableStateOf("")
        private set

    var currentMatch by mutableStateOf(0)
        private set

    private var saveJob: Job? = null
    private var matchCacheKey: Pair<String, String>? = null
    private var matchCache: List<IntRange> = emptyList()

    val language: Language get() = Language.ofFile(openFile?.name.orEmpty())

    val matchCount: Int get() = matches(searchQuery).size

    val canFormat: Boolean get() = CodeFormatter.supports(language)

    val entryPage: String?
        get() {
            val root = tree ?: return null
            val htmlFiles = collectFiles(root).filter { it.extension == "html" || it.extension == "htm" }
            return htmlFiles.firstOrNull { it.path.lowercase().substringAfterLast('/') == "index.html" }?.path
                ?: htmlFiles.firstOrNull()?.path
        }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(settings)
        if (updated == settings) return
        settings = updated
        settingsStore.save(updated)
    }

    fun selectFolder(uri: Uri) {
        viewModelScope.launch {
            val attached = withContext(Dispatchers.IO) { repository.attach(uri) }
            if (attached) {
                applyTree()
                statusMessage = "Opened ${repository.rootName}"
                val page = entryPage
                if (page != null) openPath(page)
            } else {
                repository.detach()
                hasProject = false
                tree = null
                statusMessage = "That folder could not be read"
            }
        }
    }

    fun refreshProject() {
        if (!repository.isAttached) return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repository.refresh() }
            applyTree()
            if (!ok) statusMessage = "Could not read the project folder"
        }
    }

    fun forgetProject() {
        saveNow()
        repository.detach()
        hasProject = false
        tree = null
        projectName = ""
        openFile = null
        editorValue = TextFieldValue("")
        previewVisible = false
    }

    fun openPath(path: String) {
        val file = tree?.let { findFile(it, path) } ?: return
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { repository.read(file.path) }
            openFile = file
            editorValue = TextFieldValue(text)
            history.reset(editorValue)
            isDirty = false
            searchQuery = ""
            replaceText = ""
            currentMatch = 0
        }
    }

    fun closeFile() {
        saveNow()
        openFile = null
        editorValue = TextFieldValue("")
        isDirty = false
        searchVisible = false
    }

    fun onEdit(next: TextFieldValue) {
        if (next == editorValue) return
        history.onChange(editorValue, next, System.currentTimeMillis())
        editorValue = next
        if (openFile != null) {
            isDirty = true
            if (settings.autosave) scheduleSave()
        }
        if (searchQuery.isNotEmpty()) currentMatch = 0
    }

    fun undo() {
        val target = history.undo(editorValue) ?: return
        editorValue = target
        markEdited()
    }

    fun redo() {
        val target = history.redo(editorValue) ?: return
        editorValue = target
        markEdited()
    }

    fun formatDocument() {
        val file = openFile ?: return
        if (!CodeFormatter.supports(language)) return
        val formatted = CodeFormatter.format(editorValue.text, settings.indentSize)
        if (formatted == editorValue.text) {
            statusMessage = "Already formatted"
            return
        }
        onEdit(TextFieldValue(formatted, TextRange(formatted.length)))
        statusMessage = "Formatted ${file.name}"
    }

    fun saveNow() {
        val file = openFile ?: return
        if (!isDirty) return
        saveJob?.cancel()
        val text = editorValue.text
        viewModelScope.launch {
            val written = withContext(Dispatchers.IO) {
                val ok = repository.write(file.path, text)
                if (ok) repository.mirrorFile(file.path, text)
                ok
            }
            isDirty = false
            statusMessage = if (written) "Saved ${file.name}" else "Could not save ${file.name}"
        }
    }

    fun createFile(parentPath: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.createFile(parentPath, name.trim()) }
            result.onSuccess {
                applyTree()
                statusMessage = "Created ${it.substringAfterLast('/')}"
            }.onFailure {
                statusMessage = it.message ?: "Could not create the file"
            }
        }
    }

    fun createFolder(parentPath: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.createFolder(parentPath, name.trim()) }
            result.onSuccess {
                applyTree()
                statusMessage = "Created ${it.substringAfterLast('/')}"
            }.onFailure {
                statusMessage = it.message ?: "Could not create the folder"
            }
        }
    }

    fun deleteFile(file: ProjectFile) {
        if (openFile?.path == file.path) closeFile()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.deleteFile(file.path) }
            result.onSuccess {
                applyTree()
                statusMessage = "Deleted ${file.name}"
            }.onFailure {
                statusMessage = it.message ?: "Could not delete the file"
            }
        }
    }

    fun openPreview() {
        val page = entryPage
        if (page == null) {
            statusMessage = "This project has no HTML file"
            return
        }
        saveNow()
        viewModelScope.launch {
            if (settings.autoReloadPreview) {
                withContext(Dispatchers.IO) { repository.mirrorAll() }
            }
            previewGeneration++
            previewVisible = true
        }
    }

    fun reloadPreview() {
        saveNow()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.mirrorAll() }
            previewGeneration++
        }
    }

    fun closePreview() {
        previewVisible = false
    }

    fun previewDirectory() = repository.mirrorDir()

    fun showMessage(message: String) {
        statusMessage = message
    }

    fun consumeMessage() {
        statusMessage = null
    }

    fun showSearch(visible: Boolean) {
        searchVisible = visible
        if (!visible) {
            searchQuery = ""
            replaceText = ""
            currentMatch = 0
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        currentMatch = 0
        jumpToMatch(0)
    }

    fun updateReplaceText(text: String) {
        replaceText = text
    }

    fun nextMatch() {
        val total = matchCount
        if (total == 0) return
        jumpToMatch((currentMatch + 1) % total)
    }

    fun previousMatch() {
        val total = matchCount
        if (total == 0) return
        jumpToMatch((currentMatch - 1 + total) % total)
    }

    fun replaceCurrentMatch() {
        val matches = matches(searchQuery)
        if (matches.isEmpty()) return
        val index = currentMatch.coerceIn(matches.indices)
        val range = matches[index]
        val text = editorValue.text.replaceRange(range.first, range.last + 1, replaceText)
        val caret = range.first + replaceText.length
        onEdit(TextFieldValue(text, TextRange(caret)))
        currentMatch = index
    }

    fun replaceAllMatches() {
        val matches = matches(searchQuery)
        if (matches.isEmpty()) return
        val text = editorValue.text
        val builder = StringBuilder(text.length)
        var cursor = 0
        for (range in matches) {
            builder.appendRange(text, cursor, range.first)
            builder.append(replaceText)
            cursor = range.last + 1
        }
        builder.appendRange(text, cursor, text.length)
        val updated = builder.toString()
        onEdit(TextFieldValue(updated, TextRange(updated.length)))
        statusMessage = "Replaced ${matches.size} match(es)"
    }

    override fun onCleared() {
        saveJob?.cancel()
        super.onCleared()
    }

    private fun markEdited() {
        if (openFile == null) return
        isDirty = true
        if (settings.autosave) scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            saveJob = null
            saveNow()
        }
    }

    private fun applyTree() {
        tree = repository.projectTree
        projectName = repository.rootName
        hasProject = repository.isAttached
    }

    private fun jumpToMatch(index: Int) {
        val found = matches(searchQuery)
        if (found.isEmpty()) {
            currentMatch = 0
            return
        }
        val safe = index.coerceIn(found.indices)
        currentMatch = safe
        val range = found[safe]
        editorValue = editorValue.copy(selection = TextRange(range.first, range.last + 1))
    }

    private fun matches(query: String): List<IntRange> {
        if (query.isEmpty()) return emptyList()
        val key = editorValue.text to query
        if (matchCacheKey == key) return matchCache
        val text = editorValue.text
        val found = ArrayList<IntRange>()
        var from = 0
        while (from <= text.length - query.length) {
            val index = text.indexOf(query, from, ignoreCase = true)
            if (index < 0) break
            found.add(index until index + query.length)
            from = index + query.length
        }
        matchCacheKey = key
        matchCache = found
        return found
    }

    private fun findFile(folder: ProjectFolder, path: String): ProjectFile? {
        folder.files.firstOrNull { it.path == path }?.let { return it }
        folder.folders.forEach { child ->
            findFile(child, path)?.let { return it }
        }
        return null
    }

    private fun collectFiles(folder: ProjectFolder): List<ProjectFile> {
        val files = ArrayList(folder.files)
        folder.folders.forEach { files.addAll(collectFiles(it)) }
        return files
    }

    private companion object {
        const val AUTOSAVE_DELAY_MS = 700L
    }
}
