package org.fairscan.app.data.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.PrimaryKey;

/**
 * FTS4 (Full-Text Search) virtual table linked to the Document entity.
 */
@Entity(tableName = "documents_fts")
@Fts4(contentEntity = Document.class)
public class DocumentFts {

    @PrimaryKey
    @ColumnInfo(name = "rowid")
    private long rowid;

    private String name;
    private String ocrText;

    public DocumentFts(String name, String ocrText) {
        this.name = name;
        this.ocrText = ocrText;
    }

    public long getRowid() {
        return rowid;
    }

    public void setRowid(long rowid) {
        this.rowid = rowid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }
}
