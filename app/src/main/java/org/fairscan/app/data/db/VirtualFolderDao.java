package org.fairscan.app.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

/**
 * Data Access Object for Folders and Virtual Document Categorization.
 */
@Dao
public interface VirtualFolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long createFolder(Folder folder);

    @Query("DELETE FROM folders WHERE id = :folderId")
    void deleteFolderById(long folderId);

    @Query("UPDATE folders SET name = :newName WHERE id = :folderId")
    void updateFolderName(long folderId, String newName);

    @Query("UPDATE documents SET folderId = :newFolderId WHERE id = :documentId")
    void moveDocument(long documentId, Long newFolderId);

    @Query("SELECT * FROM documents WHERE folderId = :folderId ORDER BY createdAt DESC")
    List<Document> getDocumentsByFolder(long folderId);

    @Query("SELECT * FROM folders ORDER BY name ASC")
    List<Folder> getAllFolders();

    @Query("SELECT * FROM documents WHERE id = :documentId LIMIT 1")
    Document getDocumentById(long documentId);

    @Query("SELECT * FROM documents WHERE folderId IS NULL ORDER BY createdAt DESC")
    List<Document> getUncategorizedDocuments();

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    List<Document> getAllDocuments();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertDocument(Document document);

    @Query("DELETE FROM documents WHERE id = :documentId")
    void deleteDocument(long documentId);

    @Query("UPDATE documents SET ocrText = :ocrText WHERE id = :documentId")
    void updateDocumentOcrText(long documentId, String ocrText);

    @Query("SELECT documents.* FROM documents " +
           "JOIN documents_fts ON documents.id = documents_fts.rowid " +
           "WHERE documents_fts MATCH :query " +
           "ORDER BY documents.createdAt DESC")
    List<Document> searchDocuments(String query);

    @Query("SELECT documents.id, documents.uri, documents.name, documents.folderId, " +
           "folders.name AS folderName, documents.createdAt, documents.ocrText " +
           "FROM documents " +
           "JOIN documents_fts ON documents.id = documents_fts.rowid " +
           "LEFT JOIN folders ON documents.folderId = folders.id " +
           "WHERE documents_fts MATCH :query " +
           "ORDER BY documents.createdAt DESC")
    List<DocumentSearchResult> searchDocumentsWithFolder(String query);
}

