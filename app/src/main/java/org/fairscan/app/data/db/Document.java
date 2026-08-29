package org.fairscan.app.data.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Represents a scanned document reference stored in a virtual folder.
 */
@Entity(
    tableName = "documents",
    foreignKeys = @ForeignKey(
        entity = Folder.class,
        parentColumns = "id",
        childColumns = "folderId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("folderId")}
)
public class Document {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String uri;
    private String name;
    private Long folderId;
    private long createdAt;
    private String ocrText;

    public Document(long id, String uri, String name, Long folderId, long createdAt, String ocrText) {
        this.id = id;
        this.uri = uri;
        this.name = name;
        this.folderId = folderId;
        this.createdAt = createdAt;
        this.ocrText = ocrText;
    }

    @Ignore
    public Document(long id, String uri, String name, Long folderId, long createdAt) {
        this(id, uri, name, folderId, createdAt, null);
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
}

