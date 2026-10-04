package com.aprax.htmlrun.project

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

data class ProjectFile(
    val path: String,
    val name: String,
    val documentId: String,
    val mimeType: String,
    val size: Long,
) {
    val extension: String get() = path.substringAfterLast('.', "").lowercase()
}

data class ProjectFolder(
    val path: String,
    val name: String,
    val folders: List<ProjectFolder>,
    val files: List<ProjectFile>,
) {
    val isEmpty: Boolean get() = folders.isEmpty() && files.isEmpty()
}

/** Reads and writes the project folder chosen through the system folder picker. */
class ProjectRepository(context: Context) {

    private val appContext = context.applicationContext
    private val resolver get() = appContext.contentResolver

    var rootUri: Uri? = null
        private set

    var rootName: String = ""
        private set

    private var index: Map<String, String> = emptyMap()
    private var tree: ProjectFolder = ProjectFolder("", "", emptyList(), emptyList())

    val isAttached: Boolean get() = rootUri != null

    val projectTree: ProjectFolder get() = tree

    fun attach(uri: Uri): Boolean {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { resolver.takePersistableUriPermission(uri, flags) }
        rootUri = uri
        rootName = displayName(uri) ?: "Project"
        return refresh()
    }

    fun detach() {
        rootUri = null
        rootName = ""
        index = emptyMap()
        tree = ProjectFolder("", "", emptyList(), emptyList())
    }

    fun refresh(): Boolean {
        val uri = rootUri ?: return false
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return false
        val collected = LinkedHashMap<String, String>()
        tree = scan(uri, rootId, "", collected)
        index = collected
        return true
    }

    fun fileAt(path: String): ProjectFile? {
        val found = findFile(tree, path) ?: return null
        val documentId = index[path] ?: return null
        return found.copy(documentId = documentId)
    }

    fun read(path: String): String {
        val uri = uriFor(path) ?: return ""
        return runCatching {
            resolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: ""
        }.getOrDefault("")
    }

    fun write(path: String, text: String): Boolean {
        val uri = uriFor(path) ?: return false
        return runCatching {
            resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
        }.getOrDefault(false)
    }

    fun createFile(parentPath: String, name: String): Result<String> = runCatching {
        val treeUri = rootUri ?: error("No project folder selected")
        val parentId = documentIdOf(parentPath)
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentId)
        val created = DocumentsContract.createDocument(resolver, parentUri, mimeFor(name), name)
            ?: error("Could not create $name")
        val path = joinPath(parentPath, name)
        index = index + (path to DocumentsContract.getDocumentId(created))
        refresh()
        path
    }

    fun createFolder(parentPath: String, name: String): Result<String> = runCatching {
        val treeUri = rootUri ?: error("No project folder selected")
        val parentId = documentIdOf(parentPath)
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentId)
        val created = DocumentsContract.createDocument(
            resolver,
            parentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            name,
        ) ?: error("Could not create the folder $name")
        val path = joinPath(parentPath, name)
        index = index + (path to DocumentsContract.getDocumentId(created))
        refresh()
        path
    }

    fun deleteFile(path: String): Result<Unit> = runCatching {
        val uri = uriFor(path) ?: error("Could not open $path")
        if (!DocumentsContract.deleteDocument(resolver, uri)) error("Could not delete $path")
        refresh()
        Unit
    }

    /** Copies the whole project into app storage so the preview can serve it over http. */
    fun mirrorAll(): Boolean {
        val target = mirrorDir() ?: return false
        target.deleteRecursively()
        target.mkdirs()
        var copied = true
        for ((path, documentId) in index) {
            val uri = uriFor(documentId) ?: continue
            val destination = File(target, path)
            destination.parentFile?.mkdirs()
            val ok = runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                } != null
            }.getOrDefault(false)
            if (!ok) copied = false
        }
        return copied
    }

    /** Mirrors a single file so the preview shows the latest saved text. */
    fun mirrorFile(path: String, text: String): Boolean {
        val target = mirrorDir() ?: return false
        val destination = File(target, path)
        destination.parentFile?.mkdirs()
        return runCatching {
            destination.writeText(text, Charsets.UTF_8)
            true
        }.getOrDefault(false)
    }

    fun mirrorDir(): File? {
        val name = rootName.takeIf { it.isNotBlank() } ?: return null
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(File(appContext.filesDir, "projects"), safe)
    }

    private fun documentIdOf(path: String): String {
        val treeUri = rootUri ?: error("No project folder selected")
        return if (path.isEmpty()) {
            DocumentsContract.getTreeDocumentId(treeUri)
        } else {
            index[path] ?: error("The folder $path no longer exists")
        }
    }

    private fun uriFor(path: String): Uri? {
        val treeUri = rootUri ?: return null
        val documentId = index[path] ?: return null
        return runCatching { DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId) }.getOrNull()
    }

    private fun uriFor(documentId: String): Uri? {
        val treeUri = rootUri ?: return null
        return runCatching { DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId) }.getOrNull()
    }

    private fun scan(
        treeUri: Uri,
        documentId: String,
        path: String,
        collected: MutableMap<String, String>,
    ): ProjectFolder {
        val entries = childrenOf(treeUri, documentId)
        val folders = ArrayList<ProjectFolder>()
        val files = ArrayList<ProjectFile>()
        for (entry in entries) {
            val childPath = joinPath(path, entry.name)
            collected[childPath] = entry.documentId
            if (entry.isDirectory) {
                folders.add(scan(treeUri, entry.documentId, childPath, collected))
            } else {
                files.add(ProjectFile(childPath, entry.name, entry.documentId, entry.mime, entry.size))
            }
        }
        folders.sortBy { it.name.lowercase() }
        files.sortBy { it.name.lowercase() }
        return ProjectFolder(path, path.substringAfterLast('/'), folders, files)
    }

    private fun childrenOf(treeUri: Uri, documentId: String): List<Entry> {
        val childrenUri = runCatching {
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        }.getOrNull() ?: return emptyList()

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )

        return runCatching {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val entries = ArrayList<Entry>()
                while (cursor.moveToNext()) {
                    if (idColumn < 0 || nameColumn < 0) continue
                    val name = cursor.getString(nameColumn) ?: continue
                    val mime = if (mimeColumn >= 0) cursor.getString(mimeColumn) else null
                    val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
                    entries.add(
                        Entry(
                            documentId = cursor.getString(idColumn) ?: continue,
                            name = name,
                            isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                            mime = mime ?: "application/octet-stream",
                            size = size,
                        )
                    )
                }
                entries
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun findFile(folder: ProjectFolder, path: String): ProjectFile? {
        folder.files.firstOrNull { it.path == path }?.let { return it }
        for (child in folder.folders) {
            findFile(child, path)?.let { return it }
        }
        return null
    }

    private fun displayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst() && cursor.isNull(0).not()) cursor.getString(0) else null
            }
    }.getOrNull()

    private fun joinPath(parent: String, name: String): String =
        if (parent.isEmpty()) name else "$parent/$name"

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js", "mjs" -> "text/javascript"
        "json" -> "application/json"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "ico" -> "image/x-icon"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        "md" -> "text/markdown"
        else -> "text/plain"
    }

    private data class Entry(
        val documentId: String,
        val name: String,
        val isDirectory: Boolean,
        val mime: String,
        val size: Long,
    )
}
