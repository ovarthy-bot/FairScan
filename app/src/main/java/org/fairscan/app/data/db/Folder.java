package org.fairscan.app.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a virtual folder for scanned documents.
 */
@Entity(tableName = "folders")
public class Folder {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private long createdAt;

    public Folder(long id, String name, long createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
