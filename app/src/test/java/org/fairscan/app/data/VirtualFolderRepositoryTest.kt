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
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.fairscan.app.data.db.Document
import org.fairscan.app.data.db.DocumentSearchResult
import org.fairscan.app.data.db.Folder
import org.fairscan.app.data.db.VirtualFolderDao
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class VirtualFolderRepositoryTest {

    private val tempDir: File = createTempDirectory().toFile()

    private class FakeVirtualFolderDao : VirtualFolderDao {
        val folders = mutableListOf<Folder>()
        val documents = mutableListOf<Document>()
        var lastSearchQuery: String? = null

        override fun createFolder(folder: Folder): Long {
            val id = (folders.size + 1).toLong()
            folder.id = id
            folders.add(folder)
            return id
        }

        override fun deleteFolderById(folderId: Long) {
            folders.removeAll { it.id == folderId }
        }

        override fun updateFolderName(folderId: Long, newName: String) {
            folders.firstOrNull { it.id == folderId }?.name = newName
        }

        override fun moveDocument(documentId: Long, newFolderId: Long?) {
            documents.firstOrNull { it.id == documentId }?.folderId = newFolderId
        }

        override fun getDocumentsByFolder(folderId: Long): List<Document> {
            return documents.filter { it.folderId == folderId }
        }

        override fun getAllFolders(): List<Folder> = folders.toList()

        override fun getDocumentById(documentId: Long): Document? {
            return documents.firstOrNull { it.id == documentId }
        }

        override fun getUncategorizedDocuments(): List<Document> {
            return documents.filter { it.folderId == null }
        }

        override fun getAllDocuments(): List<Document> = documents.toList()

        override fun insertDocument(document: Document): Long {
            val id = (documents.size + 1).toLong()
            document.id = id
            documents.add(document)
            return id
        }

        override fun deleteDocument(documentId: Long) {
            documents.removeAll { it.id == documentId }
        }

        override fun updateDocumentOcrText(documentId: Long, ocrText: String) {
            documents.firstOrNull { it.id == documentId }?.ocrText = ocrText
        }

        override fun searchDocuments(query: String): List<Document> {
            lastSearchQuery = query
            val terms = query.split(" ").map { it.removeSuffix("*").lowercase() }
            return documents.filter { doc ->
                terms.any { term ->
                    (doc.name?.lowercase()?.contains(term) == true) ||
                    (doc.ocrText?.lowercase()?.contains(term) == true)
                }
            }
        }

        override fun searchDocumentsWithFolder(query: String): List<DocumentSearchResult> {
            lastSearchQuery = query
            val docs = searchDocuments(query)
            return docs.map { doc ->
                val folderName = folders.firstOrNull { it.id == doc.folderId }?.name
                DocumentSearchResult(
                    doc.id,
                    doc.uri,
                    doc.name,
                    doc.folderId,
                    folderName,
                    doc.createdAt,
                    doc.ocrText
                )
            }
        }
    }

    private class FakeContext(private val filesDirFile: File) : android.content.ContextWrapper(null) {
        override fun getFilesDir(): File = filesDirFile
    }

    @Test
    fun saveDocumentWithOcrTextAndSearch() = runTest {
        val fakeDao = FakeVirtualFolderDao()
        val context = FakeContext(tempDir)
        val repo = VirtualFolderRepository(fakeDao, context)

        val folderId = repo.createFolder("Invoices")
        val samplePdf = File(tempDir, "sample.pdf").apply { writeText("PDF Data") }

        val doc1 = repo.saveDocumentToFolder(
            sourceFile = samplePdf,
            displayName = "Invoice_Jan_2026",
            folderId = folderId,
            ocrText = "Electric Company total amount 150 USD paid"
        )

        val doc2 = repo.saveDocumentToFolder(
            sourceFile = samplePdf,
            displayName = "Receipt_Groceries",
            folderId = folderId,
            ocrText = "Supermarket fresh fruits vegetables total 45 USD"
        )

        assertThat(doc1.ocrText).isEqualTo("Electric Company total amount 150 USD paid")
        assertThat(doc2.ocrText).isEqualTo("Supermarket fresh fruits vegetables total 45 USD")

        // Search by OCR content
        val searchResults = repo.searchDocuments("electric")
        assertThat(searchResults).hasSize(1)
        assertThat(searchResults[0].name).isEqualTo("Invoice_Jan_2026")
        assertThat(searchResults[0].uri).isEqualTo(doc1.uri)
        assertThat(searchResults[0].folderId).isEqualTo(folderId)

        // Search with folder info
        val searchWithFolder = repo.searchDocumentsWithFolder("vegetables")
        assertThat(searchWithFolder).hasSize(1)
        assertThat(searchWithFolder[0].name).isEqualTo("Receipt_Groceries")
        assertThat(searchWithFolder[0].folderName).isEqualTo("Invoices")
        assertThat(searchWithFolder[0].uri).isEqualTo(doc2.uri)
        assertThat(searchWithFolder[0].ocrText).contains("Supermarket")

        // Update OCR text
        repo.updateDocumentOcrText(doc1.id, "Updated invoice text with reference number 987654")
        val updatedSearch = repo.searchDocuments("987654")
        assertThat(updatedSearch).hasSize(1)
        assertThat(updatedSearch[0].id).isEqualTo(doc1.id)
    }

    @Test
    fun emptySearchQueryReturnsEmptyList() = runTest {
        val fakeDao = FakeVirtualFolderDao()
        val context = FakeContext(tempDir)
        val repo = VirtualFolderRepository(fakeDao, context)

        assertThat(repo.searchDocuments("   ")).isEmpty()
        assertThat(repo.searchDocumentsWithFolder("   ")).isEmpty()
    }
}
