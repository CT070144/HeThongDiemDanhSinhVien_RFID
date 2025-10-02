package com.rfid.attendance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "docrfid1")
public class DocRfid {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rfid", length = 50, unique = true)
    @NotBlank(message = "RFID không được để trống")
    private String rfid;
    
    @Column(name = "masinhvien", length = 20)
    private String maSinhVien;
    
    @Column(name = "tensinhvien", length = 100)
    private String tenSinhVien;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "processed")
    private Boolean processed = false;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public DocRfid() {}
    
    public DocRfid(String rfid) {
        this.rfid = rfid;
    }
    
    public DocRfid(String rfid, String maSinhVien, String tenSinhVien) {
        this.rfid = rfid;
        this.maSinhVien = maSinhVien;
        this.tenSinhVien = tenSinhVien;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public Boolean getProcessed() {
        return processed;
    }
    
    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }
}
