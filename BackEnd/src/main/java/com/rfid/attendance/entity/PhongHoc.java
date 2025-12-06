package com.rfid.attendance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "phonghoc")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhongHoc {

    @Id
    @Column(name = "ma_phong", length = 20)
    @NotBlank
    private String maPhong;

    @Column(name = "ten_phong", length = 100)
    @Size(max = 100)
    private String tenPhong;

    @Column(name = "toa_nha", length = 20)
    @Size(max = 20)
    private String toaNha;

    @Column(name = "tang")
    private Integer tang;

    @Column(name = "suc_chua")
    private Integer sucChua;

    @Column(name = "loai_phong", length = 50)
    @Size(max = 50)
    private String loaiPhong;

    @Builder.Default
    @Column(name = "trang_thai", length = 20)
    @Size(max = 20)
    private String trangThai = "active";



    public String getMaPhong() {
        return maPhong;
    }

    public void setMaPhong(String maPhong) {
        this.maPhong = maPhong;
    }

    public String getTenPhong() {
        return tenPhong;
    }

    public void setTenPhong(String tenPhong) {
        this.tenPhong = tenPhong;
    }

    public String getToaNha() {
        return toaNha;
    }

    public void setToaNha(String toaNha) {
        this.toaNha = toaNha;
    }

    public Integer getTang() {
        return tang;
    }

    public void setTang(Integer tang) {
        this.tang = tang;
    }

    public Integer getSucChua() {
        return sucChua;
    }

    public void setSucChua(Integer sucChua) {
        this.sucChua = sucChua;
    }

    public String getLoaiPhong() {
        return loaiPhong;
    }

    public void setLoaiPhong(String loaiPhong) {
        this.loaiPhong = loaiPhong;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}


