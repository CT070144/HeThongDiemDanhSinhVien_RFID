package com.rfid.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Lưu vector embedding khuôn mặt (128 float) dạng JSON để Python so sánh 1:1.
 * Dùng ddl-auto=update nên DB sẽ tự tạo bảng/column khi deploy.
 */
@Entity
@Table(name = "face_embedding")
public class FaceEmbedding {

    @Id
    @Column(name = "masinhvien", length = 20)
    private String maSinhVien;

    @Lob
    @Column(name = "embedding_json", columnDefinition = "TEXT")
    private String embeddingJson;

    public FaceEmbedding() {}

    public FaceEmbedding(String maSinhVien, String embeddingJson) {
        this.maSinhVien = maSinhVien;
        this.embeddingJson = embeddingJson;
    }

    public String getMaSinhVien() {
        return maSinhVien;
    }

    public void setMaSinhVien(String maSinhVien) {
        this.maSinhVien = maSinhVien;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
    }
}

