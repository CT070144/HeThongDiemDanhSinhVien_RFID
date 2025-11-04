package com.rfid.attendance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "lophocphan")
public class LopHocPhan {
    
    @Id
    @Column(name = "malophocphan", length = 50)
    @NotBlank(message = "Mã lớp học phần không được để trống")
    private String maLopHocPhan;
    
    @Column(name = "tenlophocphan", length = 200)
    @NotBlank(message = "Tên lớp học phần không được để trống")
    private String tenLopHocPhan;

    @Column(name = "giangvien", length = 255)
    private String giangVien;

    @Column(name = "phonghoc", length = 100)
    private String phongHoc;

    @Column(name = "hinhthuchoc", length = 100)
    private String hinhThucHoc;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "lopHocPhan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<SinhVienLopHocPhan> sinhVienLopHocPhans;
    
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
    public LopHocPhan() {}
    
    public LopHocPhan(String maLopHocPhan, String tenLopHocPhan) {
        this.maLopHocPhan = maLopHocPhan;
        this.tenLopHocPhan = tenLopHocPhan;
    }
    
    // Getters and Setters
    public String getMaLopHocPhan() {
        return maLopHocPhan;
    }
    
    public void setMaLopHocPhan(String maLopHocPhan) {
        this.maLopHocPhan = maLopHocPhan;
    }
    
    public String getTenLopHocPhan() {
        return tenLopHocPhan;
    }
    
    public void setTenLopHocPhan(String tenLopHocPhan) {
        this.tenLopHocPhan = tenLopHocPhan;
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
    
    public Set<SinhVienLopHocPhan> getSinhVienLopHocPhans() {
        return sinhVienLopHocPhans;
    }
    
    public void setSinhVienLopHocPhans(Set<SinhVienLopHocPhan> sinhVienLopHocPhans) {
        this.sinhVienLopHocPhans = sinhVienLopHocPhans;
    }

    public String getGiangVien() {
        return giangVien;
    }

    public void setGiangVien(String giangVien) {
        this.giangVien = giangVien;
    }

    public String getPhongHoc() {
        return phongHoc;
    }

    public void setPhongHoc(String phongHoc) {
        this.phongHoc = phongHoc;
    }

    public String getHinhThucHoc() {
        return hinhThucHoc;
    }

    public void setHinhThucHoc(String hinhThucHoc) {
        this.hinhThucHoc = hinhThucHoc;
    }
}
