package com.rfid.attendance.entity;

import java.io.Serializable;
import java.util.Objects;

public class SinhVienLopHocPhanId implements Serializable {
    
    private String maSinhVien;
    private String maLopHocPhan;
    
    // Default constructor
    public SinhVienLopHocPhanId() {}
    
    // Constructor with parameters
    public SinhVienLopHocPhanId(String maSinhVien, String maLopHocPhan) {
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
    
    // equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SinhVienLopHocPhanId that = (SinhVienLopHocPhanId) o;
        return Objects.equals(maSinhVien, that.maSinhVien) &&
               Objects.equals(maLopHocPhan, that.maLopHocPhan);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(maSinhVien, maLopHocPhan);
    }
}
