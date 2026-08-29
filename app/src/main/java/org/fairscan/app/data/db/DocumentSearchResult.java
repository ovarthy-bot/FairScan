package org.fairscan.app.data.db;

/**
 * Result model representing a search hit with document data and folder name.
 */
public class DocumentSearchResult {
    private long id;
    private String uri;
    private String name;
    private Long folderId;
    private long createdAt;
    private String ocrText;
    private String folderName;

    public DocumentSearchResult(long id, String uri, String name, Long folderId, long createdAt, String ocrText, String folderName) {
        this.id = id;
        this.uri = uri;
        this.name = name;
        this.folderId = folderId;
        this.createdAt = createdAt;
        this.ocrText = ocrText;
        this.folderName = folderName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
}
