package com.rfid.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "phongban")
public class PhongBan {

    @Id
    @Column(name = "maphongban", length = 50)
    @NotBlank(message = "Mã phòng ban không được để trống")
    private String maPhongBan;

    @Column(name = "tenphongban", length = 100)
    @NotBlank(message = "Tên phòng ban không được để trống")
    @Size(max = 100)
    private String tenPhongBan;

    @Column(name = "mota", length = 255)
    @Size(max = 255)
    private String moTa;

    public String getMaPhongBan() {
        return maPhongBan;
    }

    public void setMaPhongBan(String maPhongBan) {
        this.maPhongBan = maPhongBan;
    }

    public String getTenPhongBan() {
        return tenPhongBan;
    }

    public void setTenPhongBan(String tenPhongBan) {
        this.tenPhongBan = tenPhongBan;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }
}
