package com.anotepad.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale

class FileRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun listChildren(dirTreeUri: Uri): List<DocumentNode> = withContext(Dispatchers.IO) {
        val dir = DocumentFile.fromTreeUri(context, dirTreeUri) ?: return@withContext emptyList()
        val children = dir.listFiles().mapNotNull { file ->
            val name = file.name ?: return@mapNotNull null
            val isDir = file.isDirectory
            val uri = if (isDir) toTreeUri(file.uri) else file.uri
            DocumentNode(name = name, uri = uri, isDirectory = isDir)
        }
        val (dirs, files) = children.partition { it.isDirectory }
        val filteredFiles = files.filter { isSupportedExtension(it.name) }
        dirs.sortedBy { it.name.lowercase(Locale.getDefault()) } +
            filteredFiles.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    suspend fun listNamesInDirectory(dirTreeUri: Uri): Set<String> = withContext(Dispatchers.IO) {
        val dir = DocumentFile.fromTreeUri(context, dirTreeUri) ?: return@withContext emptySet()
        dir.listFiles().mapNotNull { it.name }.toSet()
    }

    suspend fun listFilesRecursive(dirTreeUri: Uri): List<DocumentNode> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, dirTreeUri) ?: return@withContext emptyList()
        val results = mutableListOf<DocumentNode>()
        val stack = ArrayDeque<DocumentFile>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            current.listFiles().forEach { file ->
                val name = file.name ?: return@forEach
                if (file.isDirectory) {
                    stack.add(file)
                } else if (isSupportedExtension(name)) {
                    results.add(DocumentNode(name = name, uri = file.uri, isDirectory = false))
                }
            }
        }
        results
    }

    suspend fun readText(fileUri: Uri): String = withContext(Dispatchers.IO) {
        resolver.openInputStream(fileUri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        } ?: ""
    }

    suspend fun writeText(fileUri: Uri, text: String) = withContext(Dispatchers.IO) {
        resolver.openOutputStream(fileUri, "wt")?.use { output ->
            OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                writer.write(text)
            }
        }
    }

    suspend fun createFile(dirTreeUri: Uri, displayName: String, mimeType: String): Uri? =
        withContext(Dispatchers.IO) {
            val dir = DocumentFile.fromTreeUri(context, dirTreeUri) ?: return@withContext null
            dir.createFile(mimeType, displayName)?.uri
        }

    suspend fun renameFile(fileUri: Uri, newName: String): Uri? = withContext(Dispatchers.IO) {
        DocumentsContract.renameDocument(resolver, fileUri, newName)
    }

    fun isSupportedExtension(name: String): Boolean {
        val lower = name.lowercase(Locale.getDefault())
        return lower.endsWith(".txt") || lower.endsWith(".md")
    }

    fun guessMimeType(name: String): String {
        val lower = name.lowercase(Locale.getDefault())
        return if (lower.endsWith(".md")) "text/markdown" else "text/plain"
    }

    fun getDisplayName(uri: Uri): String? {
        return DocumentFile.fromSingleUri(context, uri)?.name
    }

    fun parentTreeUri(fileUri: Uri): Uri? {
        val authority = fileUri.authority ?: return null
        val docId = runCatching { DocumentsContract.getDocumentId(fileUri) }.getOrNull() ?: return null
        val parentId = docId.substringBeforeLast('/', docId)
        if (parentId == docId) return null
        return DocumentsContract.buildTreeDocumentUri(authority, parentId)
    }

    private fun toTreeUri(uri: Uri): Uri {
        val authority = uri.authority ?: return uri
        val docId = DocumentsContract.getDocumentId(uri)
        return DocumentsContract.buildTreeDocumentUri(authority, docId)
    }
}
