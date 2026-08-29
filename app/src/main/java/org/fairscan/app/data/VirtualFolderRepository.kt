/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.fairscan.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fairscan.app.data.db.Document
import org.fairscan.app.data.db.DocumentSearchResult
import org.fairscan.app.data.db.Folder
import org.fairscan.app.data.db.VirtualFolderDao
import java.io.File

class VirtualFolderRepository(
    private val dao: VirtualFolderDao,
    context: Context
) {
    // Single unified directory on-device for all physical PDF files
    val unifiedPdfDirectory: File = File(context.filesDir, "scanned_documents").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    suspend fun createFolder(name: String): Long = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext -1L
        dao.createFolder(Folder(0L, trimmed, System.currentTimeMillis()))
    }

    suspend fun deleteFolder(folderId: Long) = withContext(Dispatchers.IO) {
        dao.deleteFolderById(folderId)
    }

    suspend fun renameFolder(folderId: Long, newName: String) = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            dao.updateFolderName(folderId, trimmed)
        }
    }

    suspend fun getAllFoldersList(): List<Folder> = withContext(Dispatchers.IO) {
        dao.getAllFolders()
    }

    suspend fun getOrCreateDefaultFolder(): Folder = withContext(Dispatchers.IO) {
        val list = dao.getAllFolders()
        if (list.isNotEmpty()) {
            list.first()
        } else {
            val id = dao.createFolder(Folder(0L, "Belgelerim", System.currentTimeMillis()))
            Folder(id, "Belgelerim", System.currentTimeMillis())
        }
    }

    suspend fun getDocumentsInFolderList(folderId: Long): List<Document> = withContext(Dispatchers.IO) {
        dao.getDocumentsByFolder(folderId)
    }

    suspend fun getAllDocumentsList(): List<Document> = withContext(Dispatchers.IO) {
        dao.getAllDocuments()
    }

    suspend fun moveDocument(documentId: Long, newFolderId: Long) = withContext(Dispatchers.IO) {
        dao.moveDocument(documentId, newFolderId)
    }

    suspend fun deleteDocument(documentId: Long, deletePhysicalFile: Boolean = true) = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(documentId)
        if (doc != null) {
            dao.deleteDocument(documentId)
            if (deletePhysicalFile) {
                try {
                    val file = File(doc.uri)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun updateDocumentOcrText(documentId: Long, ocrText: String) = withContext(Dispatchers.IO) {
        dao.updateDocumentOcrText(documentId, ocrText)
    }

    /**
     * Executes an on-device full-text search against document names and OCR text
     * asynchronously using Kotlin Coroutines.
     */
    suspend fun searchDocuments(query: String): List<Document> = withContext(Dispatchers.IO) {
        val formattedQuery = formatFtsQuery(query)
        if (formattedQuery.isBlank()) {
            emptyList()
        } else {
            dao.searchDocuments(formattedQuery)
        }
    }

    /**
     * Executes an on-device full-text search returning matching documents along with
     * their URIs and folder details asynchronously using Kotlin Coroutines.
     */
    suspend fun searchDocumentsWithFolder(query: String): List<DocumentSearchResult> = withContext(Dispatchers.IO) {
        val formattedQuery = formatFtsQuery(query)
        if (formattedQuery.isBlank()) {
            emptyList()
        } else {
            dao.searchDocumentsWithFolder(formattedQuery)
        }
    }

    private fun formatFtsQuery(query: String): String {
        val sanitized = query.trim().replace(Regex("[*\"'+\\-:]"), " ")
        val words = sanitized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return ""
        return words.joinToString(" ") { "$it*" }
    }

    /**
     * Copies the generated PDF to the single unified storage directory and creates
     * the database reference pointing to it under the target folder.
     */
    suspend fun saveDocumentToFolder(
        sourceFile: File,
        displayName: String,
        folderId: Long,
        ocrText: String? = null
    ): Document = withContext(Dispatchers.IO) {
        val safeName = displayName.ifBlank { "Scan_${System.currentTimeMillis()}" }
        val targetFileName = "${System.currentTimeMillis()}_${safeName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}.pdf"
        val physicalFile = File(unifiedPdfDirectory, targetFileName)
        sourceFile.copyTo(physicalFile, overwrite = true)

        val document = Document(
            0L,
            physicalFile.absolutePath,
            safeName,
            folderId,
            System.currentTimeMillis(),
            ocrText
        )
        val generatedId = dao.insertDocument(document)
        document.id = generatedId
        document
    }
}

