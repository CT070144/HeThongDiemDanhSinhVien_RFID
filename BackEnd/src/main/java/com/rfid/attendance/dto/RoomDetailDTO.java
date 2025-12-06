package com.rfid.attendance.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class RoomDetailDTO implements Serializable {
    private String maPhong;
    private String tenPhong;
    private String toaNha;
    private Integer tang;
    private Integer sucChua;
    private String loaiPhong;
    private String trangThai;
    private ClassInfoDTO classInfo;
    private List<AttendanceInfoDTO> attendanceList;

    public RoomDetailDTO() {
    }

    public static class ClassInfoDTO implements Serializable {
        private String tenLopHocPhan;
        private String maLopHocPhan;
        private String giangVien;
        private Integer ca;
        private LocalDate ngayHoc;
        private String thoiGianBatDau;
        private String thoiGianKetThuc;
        private Integer soSinhVien;
        private Integer soSinhVienDaDiemDanh;

        // Getters and Setters
        public String getTenLopHocPhan() {
            return tenLopHocPhan;
        }

        public void setTenLopHocPhan(String tenLopHocPhan) {
            this.tenLopHocPhan = tenLopHocPhan;
        }

        public String getMaLopHocPhan() {
            return maLopHocPhan;
        }

        public void setMaLopHocPhan(String maLopHocPhan) {
            this.maLopHocPhan = maLopHocPhan;
        }

        public String getGiangVien() {
            return giangVien;
        }

        public void setGiangVien(String giangVien) {
            this.giangVien = giangVien;
        }

        public Integer getCa() {
            return ca;
        }

        public void setCa(Integer ca) {
            this.ca = ca;
        }

        public LocalDate getNgayHoc() {
            return ngayHoc;
        }

        public void setNgayHoc(LocalDate ngayHoc) {
            this.ngayHoc = ngayHoc;
        }

        public String getThoiGianBatDau() {
            return thoiGianBatDau;
        }

        public void setThoiGianBatDau(String thoiGianBatDau) {
            this.thoiGianBatDau = thoiGianBatDau;
        }

        public String getThoiGianKetThuc() {
            return thoiGianKetThuc;
        }

        public void setThoiGianKetThuc(String thoiGianKetThuc) {
            this.thoiGianKetThuc = thoiGianKetThuc;
        }

        public Integer getSoSinhVien() {
            return soSinhVien;
        }

        public void setSoSinhVien(Integer soSinhVien) {
            this.soSinhVien = soSinhVien;
        }

        public Integer getSoSinhVienDaDiemDanh() {
            return soSinhVienDaDiemDanh;
        }

        public void setSoSinhVienDaDiemDanh(Integer soSinhVienDaDiemDanh) {
            this.soSinhVienDaDiemDanh = soSinhVienDaDiemDanh;
        }
    }

    public static class AttendanceInfoDTO implements Serializable {
        private String maSinhVien;
        private String tenSinhVien;
        private LocalTime gioVao;
        private String trangThai;

        // Getters and Setters
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

        public LocalTime getGioVao() {
            return gioVao;
        }

        public void setGioVao(LocalTime gioVao) {
            this.gioVao = gioVao;
        }

        public String getTrangThai() {
            return trangThai;
        }

        public void setTrangThai(String trangThai) {
            this.trangThai = trangThai;
        }
    }

    // Getters and Setters
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

    public ClassInfoDTO getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(ClassInfoDTO classInfo) {
        this.classInfo = classInfo;
    }

    public List<AttendanceInfoDTO> getAttendanceList() {
        return attendanceList;
    }

    public void setAttendanceList(List<AttendanceInfoDTO> attendanceList) {
        this.attendanceList = attendanceList;
    }
}

