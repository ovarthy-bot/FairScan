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
package org.fairscan.app.ui.screens.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.fairscan.app.AppContainer
import org.fairscan.app.data.VirtualFolderRepository
import org.fairscan.app.data.db.Document
import org.fairscan.app.data.db.DocumentSearchResult
import org.fairscan.app.data.db.Folder

class FoldersViewModel(
    container: AppContainer
) : ViewModel() {

    val repository: VirtualFolderRepository = container.virtualFolderRepository

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder: StateFlow<Folder?> = _selectedFolder.asStateFlow()

    private val _documentsInSelectedFolder = MutableStateFlow<List<Document>>(emptyList())
    val documentsInSelectedFolder: StateFlow<List<Document>> = _documentsInSelectedFolder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<DocumentSearchResult>>(emptyList())
    val searchResults: StateFlow<List<DocumentSearchResult>> = _searchResults.asStateFlow()

    init {
        refreshFolders()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                _searchResults.value = repository.searchDocumentsWithFolder(query)
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun refreshFolders() {
        viewModelScope.launch {
            _folders.value = repository.getAllFoldersList()
            val currentSelected = _selectedFolder.value
            if (currentSelected != null) {
                _documentsInSelectedFolder.value = repository.getDocumentsInFolderList(currentSelected.id)
            }
        }
    }

    fun selectFolder(folder: Folder?) {
        _selectedFolder.value = folder
        viewModelScope.launch {
            if (folder != null) {
                _documentsInSelectedFolder.value = repository.getDocumentsInFolderList(folder.id)
            } else {
                _documentsInSelectedFolder.value = emptyList()
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val newId = repository.createFolder(name)
            refreshFolders()
            if (newId > 0 && _selectedFolder.value == null) {
                val newFolder = Folder(newId, name.trim(), System.currentTimeMillis())
                selectFolder(newFolder)
            }
        }
    }

    fun renameFolder(folderId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.renameFolder(folderId, trimmed)
            val updatedFolders = repository.getAllFoldersList()
            _folders.value = updatedFolders
            val currentSelected = _selectedFolder.value
            if (currentSelected?.id == folderId) {
                val updatedFolder = updatedFolders.firstOrNull { it.id == folderId }
                _selectedFolder.value = updatedFolder
            }
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            if (_selectedFolder.value?.id == folderId) {
                _selectedFolder.value = null
                _documentsInSelectedFolder.value = emptyList()
            }
            repository.deleteFolder(folderId)
            refreshFolders()
        }
    }

    fun moveDocument(documentId: Long, newFolderId: Long) {
        viewModelScope.launch {
            repository.moveDocument(documentId, newFolderId)
            val currentSelected = _selectedFolder.value
            if (currentSelected != null) {
                _documentsInSelectedFolder.value = repository.getDocumentsInFolderList(currentSelected.id)
            }
        }
    }

    fun deleteDocument(documentId: Long) {
        viewModelScope.launch {
            repository.deleteDocument(documentId, deletePhysicalFile = true)
            val currentSelected = _selectedFolder.value
            if (currentSelected != null) {
                _documentsInSelectedFolder.value = repository.getDocumentsInFolderList(currentSelected.id)
            }
        }
    }
}
