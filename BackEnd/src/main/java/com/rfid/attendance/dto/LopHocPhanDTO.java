package com.rfid.attendance.dto;

import java.time.LocalDateTime;

public class LopHocPhanDTO {
    private String maLopHocPhan;
    private String tenLopHocPhan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long soSinhVien;

    public LopHocPhanDTO() {}

    public LopHocPhanDTO(String maLopHocPhan, String tenLopHocPhan, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.maLopHocPhan = maLopHocPhan;
        this.tenLopHocPhan = tenLopHocPhan;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public long getSoSinhVien() {
        return soSinhVien;
    }

    public void setSoSinhVien(long soSinhVien) {
        this.soSinhVien = soSinhVien;
    }
}
