package com.rfid.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LopHocPhan {

    private String maLopHocPhan;
    private String tenLopHocPhan;
    private String giangVien;
    private String phongHoc;
    private String hinhThucHoc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer soSinhVien;

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

    public Integer getSoSinhVien() {
        return soSinhVien;
    }

    public void setSoSinhVien(Integer soSinhVien) {
        this.soSinhVien = soSinhVien;
    }
}

