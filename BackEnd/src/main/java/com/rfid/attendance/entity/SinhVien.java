package com.rfid.attendance.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "sinhvien")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SinhVien {
    
    @Id
    @Column(name = "masinhvien", length = 20)
    @NotBlank(message = "Mã sinh viên không được để trống")
    private String maSinhVien;
    
    @Column(name = "rfid", length = 50, unique = true)
    @NotBlank(message = "RFID không được để trống")
    private String rfid;
    
    @Column(name = "tensinhvien", length = 100)
    @NotBlank(message = "Tên sinh viên không được để trống")
    private String tenSinhVien;

    @Column(name = "maphongban", length = 50)
    private String maPhongBan;

    // Lưu vector embedding của khuôn mặt dạng JSON TEXT (vd: [0.12, -0.33, ...])
    // dùng cho API /compare của Python.
    @Lob
    @Column(name = "faceid", columnDefinition = "TEXT")
    private String faceid;

    // Lưu đường dẫn tương đối (relative path) tới ảnh avatar trên server.
    // Ví dụ: uploads/avatars/CT070201/uuid.jpg
    @Column(name = "path_avatar", length = 500)
    private String pathAvatar;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public SinhVien() {}
    
    public SinhVien(String maSinhVien, String rfid, String tenSinhVien) {
        this.maSinhVien = maSinhVien;
        this.rfid = rfid;
        this.tenSinhVien = tenSinhVien;
    }
    
    // Getters and Setters
    public String getRfid() {
        return rfid;
    }
    
    public void setRfid(String rfid) {
        this.rfid = rfid;
    }
    
    public String getMaSinhVien() {
        return maSinhVien;
    }
    
    public void setMaSinhVien(String maSinhVien) {
        this.maSinhVien = maSinhVien;
    }
    
    public String getTenSinhVien() {
        return tenSinhVien;
    }
    
    public void setTenSinhVien(String tenSinhVien) {
        this.tenSinhVien = tenSinhVien;
    }

    public String getMaPhongBan() {
        return maPhongBan;
    }

    public void setMaPhongBan(String maPhongBan) {
        this.maPhongBan = maPhongBan;
    }

    public String getFaceid() {
        return faceid;
    }

    public void setFaceid(String faceid) {
        this.faceid = faceid;
    }

    public String getPathAvatar() {
        return pathAvatar;
    }

    public void setPathAvatar(String pathAvatar) {
        this.pathAvatar = pathAvatar;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
