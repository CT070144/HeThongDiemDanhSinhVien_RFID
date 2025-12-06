package com.rfid.attendance.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class RoomScheduleDTO implements Serializable {
    private String maPhong;
    private String tenPhong;
    private String toaNha;
    private Integer tang;
    private List<CaScheduleDTO> caSchedules; // Danh sách các ca trong ngày

    public RoomScheduleDTO() {
    }

    public static class CaScheduleDTO implements Serializable {
        private Integer ca;
        private String status; // "occupied", "empty", "upcoming", "maintenance"
        private String tenLopHocPhan;
        private String giangVien;
        private LocalTime thoiGianBatDau;
        private LocalTime thoiGianKetThuc;
        private Integer soSinhVien;
        private Integer soSinhVienDaDiemDanh;
        private boolean isSpanning; // Lớp học kéo dài nhiều ca
        private Integer spanCount; // Số ca kéo dài

        // Getters and Setters
        public Integer getCa() {
            return ca;
        }

        public void setCa(Integer ca) {
            this.ca = ca;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
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

        public LocalTime getThoiGianBatDau() {
            return thoiGianBatDau;
        }

        public void setThoiGianBatDau(LocalTime thoiGianBatDau) {
            this.thoiGianBatDau = thoiGianBatDau;
        }

        public LocalTime getThoiGianKetThuc() {
            return thoiGianKetThuc;
        }

        public void setThoiGianKetThuc(LocalTime thoiGianKetThuc) {
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

        public boolean isSpanning() {
            return isSpanning;
        }

        public void setSpanning(boolean spanning) {
            isSpanning = spanning;
        }

        public Integer getSpanCount() {
            return spanCount;
        }

        public void setSpanCount(Integer spanCount) {
            this.spanCount = spanCount;
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

    public List<CaScheduleDTO> getCaSchedules() {
        return caSchedules;
    }

    public void setCaSchedules(List<CaScheduleDTO> caSchedules) {
        this.caSchedules = caSchedules;
    }
}

