package com.rfid.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ca_hoc")
public class CaHoc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_sheet", length = 100)
    private String tenSheet;

    @Column(name = "lop_hoc_phan", length = 255)
    private String lopHocPhan;

    @Column(name = "hinh_thuc_hoc")
    private String hinhThucHoc;

    @Column(name = "so_tiet_tuan")
    private Integer soTietTuan;

    @Column(name = "thu")
    private Integer thu;

    @Column(name = "tiet_hoc", length = 20)
    private String tietHoc;

    @Column(name = "phong_hoc", length = 50)
    private String phongHoc;

    @Column(name = "ngay_hoc")
    private LocalDate ngayHoc;

    @Column(name = "giao_vien", length = 255)
    private String giaoVien;

    @Column(name = "ca")
    private Integer ca;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenSheet() {
        return tenSheet;
    }

    public void setTenSheet(String tenSheet) {
        this.tenSheet = tenSheet;
    }

    public String getLopHocPhan() {
        return lopHocPhan;
    }

    public void setLopHocPhan(String lopHocPhan) {
        this.lopHocPhan = lopHocPhan;
    }

    public String getHinhThucHoc() {
        return hinhThucHoc;
    }

    public void setHinhThucHoc(String hinhThucHoc) {
        this.hinhThucHoc = hinhThucHoc;
    }

    public Integer getSoTietTuan() {
        return soTietTuan;
    }

    public void setSoTietTuan(Integer soTietTuan) {
        this.soTietTuan = soTietTuan;
    }

    public Integer getThu() {
        return thu;
    }

    public void setThu(Integer thu) {
        this.thu = thu;
    }

    public String getTietHoc() {
        return tietHoc;
    }

    public void setTietHoc(String tietHoc) {
        this.tietHoc = tietHoc;
    }

    public String getPhongHoc() {
        return phongHoc;
    }

    public void setPhongHoc(String phongHoc) {
        this.phongHoc = phongHoc;
    }

    public LocalDate getNgayHoc() {
        return ngayHoc;
    }

    public void setNgayHoc(LocalDate ngayHoc) {
        this.ngayHoc = ngayHoc;
    }

    public String getGiaoVien() {
        return giaoVien;
    }

    public void setGiaoVien(String giaoVien) {
        this.giaoVien = giaoVien;
    }

    public Integer getCa() {
        return ca;
    }

    public void setCa(Integer ca) {
        this.ca = ca;
    }
}


