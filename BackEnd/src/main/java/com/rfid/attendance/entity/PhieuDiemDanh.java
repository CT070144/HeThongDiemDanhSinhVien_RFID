package com.rfid.attendance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "phieudiemdanh")
public class PhieuDiemDanh {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rfid", length = 50)
    @NotBlank(message = "RFID không được để trống")
    private String rfid;
    
    @Column(name = "masinhvien", length = 20)
    @NotBlank(message = "Mã sinh viên không được để trống")
    private String maSinhVien;
    
    @Column(name = "tensinhvien", length = 100)
    @NotBlank(message = "Tên sinh viên không được để trống")
    private String tenSinhVien;
    
    @Column(name = "phonghoc", length = 50)
    private String phongHoc;
    
    @Column(name = "giovao")
    private LocalTime gioVao;
    
    @Column(name = "giora")
    private LocalTime gioRa;
    
    @Column(name = "ngay")
    @NotNull(message = "Ngày không được để trống")
    private LocalDate ngay;
    
    @Column(name = "ca")
    @NotNull(message = "Ca không được để trống")
    private Integer ca;
    
    @Convert(converter = TrangThaiConverter.class)
    @Column(name = "trangthai")
    @NotNull(message = "Trạng thái không được để trống")
    private TrangThai trangThai;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum TrangThai {
        MUON("muon", "Điểm danh muộn"),
        DANG_HOC("dang_hoc", "Đang học");
        
        private final String code;
        private final String description;
        
        TrangThai(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
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
    public PhieuDiemDanh() {}
    
    public PhieuDiemDanh(String rfid, String maSinhVien, String tenSinhVien, 
                         LocalDate ngay, Integer ca, TrangThai trangThai) {
        this.rfid = rfid;
        this.maSinhVien = maSinhVien;
        this.tenSinhVien = tenSinhVien;
        this.ngay = ngay;
        this.ca = ca;
        this.trangThai = trangThai;
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
    
    public String getPhongHoc() {
        return phongHoc;
    }
    
    public void setPhongHoc(String phongHoc) {
        this.phongHoc = phongHoc;
    }
    
    public LocalTime getGioVao() {
        return gioVao;
    }
    
    public void setGioVao(LocalTime gioVao) {
        this.gioVao = gioVao;
    }
    
    public LocalTime getGioRa() {
        return gioRa;
    }
    
    public void setGioRa(LocalTime gioRa) {
        this.gioRa = gioRa;
    }
    
    public LocalDate getNgay() {
        return ngay;
    }
    
    public void setNgay(LocalDate ngay) {
        this.ngay = ngay;
    }
    
    public Integer getCa() {
        return ca;
    }
    
    public void setCa(Integer ca) {
        this.ca = ca;
    }
    
    public TrangThai getTrangThai() {
        return trangThai;
    }
    
    public void setTrangThai(TrangThai trangThai) {
        this.trangThai = trangThai;
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
