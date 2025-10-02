package com.rfid.attendance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "sinhvien")
public class SinhVien {
    
    @Id
    @Column(name = "rfid", length = 50)
    @NotBlank(message = "RFID không được để trống")
    private String rfid;
    
    @Column(name = "masinhvien", length = 20, unique = true)
    @NotBlank(message = "Mã sinh viên không được để trống")
    private String maSinhVien;
    
    @Column(name = "tensinhvien", length = 100)
    @NotBlank(message = "Tên sinh viên không được để trống")
    private String tenSinhVien;
    
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
    
    public SinhVien(String rfid, String maSinhVien, String tenSinhVien) {
        this.rfid = rfid;
        this.maSinhVien = maSinhVien;
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
