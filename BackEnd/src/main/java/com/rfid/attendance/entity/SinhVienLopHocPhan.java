package com.rfid.attendance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "sinhvienlophocphan")
@IdClass(SinhVienLopHocPhanId.class)
public class SinhVienLopHocPhan {
    
    @Id
    @Column(name = "masinhvien", length = 20)
    @NotBlank(message = "Mã sinh viên không được để trống")
    private String maSinhVien;
    
    @Id
    @Column(name = "malophocphan", length = 50)
    @NotBlank(message = "Mã lớp học phần không được để trống")
    private String maLopHocPhan;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "masinhvien", insertable = false, updatable = false)
    @JsonIgnore
    private SinhVien sinhVien;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "malophocphan", insertable = false, updatable = false)
    @JsonIgnore
    private LopHocPhan lopHocPhan;
    
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
    public SinhVienLopHocPhan() {}
    
    public SinhVienLopHocPhan(String maSinhVien, String maLopHocPhan) {
        this.maSinhVien = maSinhVien;
        this.maLopHocPhan = maLopHocPhan;
    }
    
    // Getters and Setters
    public String getMaSinhVien() {
        return maSinhVien;
    }
    
    public void setMaSinhVien(String maSinhVien) {
        this.maSinhVien = maSinhVien;
    }
    
    public String getMaLopHocPhan() {
        return maLopHocPhan;
    }
    
    public void setMaLopHocPhan(String maLopHocPhan) {
        this.maLopHocPhan = maLopHocPhan;
    }
    
    public SinhVien getSinhVien() {
        return sinhVien;
    }
    
    public void setSinhVien(SinhVien sinhVien) {
        this.sinhVien = sinhVien;
    }
    
    public LopHocPhan getLopHocPhan() {
        return lopHocPhan;
    }
    
    public void setLopHocPhan(LopHocPhan lopHocPhan) {
        this.lopHocPhan = lopHocPhan;
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
