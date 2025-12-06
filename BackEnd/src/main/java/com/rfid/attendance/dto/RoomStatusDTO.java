package com.rfid.attendance.dto;

import java.io.Serializable;

public class RoomStatusDTO implements Serializable {
    private String maPhong;
    private String tenPhong;
    private String toaNha;
    private Integer tang;
    private Integer sucChua;
    private String loaiPhong;
    private String trangThai;
    private String status; // "occupied" or "empty"
    private String currentClass; // Tên lớp học phần đang diễn ra
    private Integer currentCa; // Ca học hiện tại
    private Integer studentsAttended; // Số sinh viên đã điểm danh
    private Integer totalStudents; // Tổng số sinh viên trong lớp

    public RoomStatusDTO() {
    }

    public RoomStatusDTO(String maPhong, String tenPhong, String toaNha, Integer tang, 
                        Integer sucChua, String loaiPhong, String trangThai) {
        this.maPhong = maPhong;
        this.tenPhong = tenPhong;
        this.toaNha = toaNha;
        this.tang = tang;
        this.sucChua = sucChua;
        this.loaiPhong = loaiPhong;
        this.trangThai = trangThai;
        this.status = "empty";
        this.currentClass = null;
        this.currentCa = null;
        this.studentsAttended = 0;
        this.totalStudents = 0;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(String currentClass) {
        this.currentClass = currentClass;
    }

    public Integer getCurrentCa() {
        return currentCa;
    }

    public void setCurrentCa(Integer currentCa) {
        this.currentCa = currentCa;
    }

    public Integer getStudentsAttended() {
        return studentsAttended;
    }

    public void setStudentsAttended(Integer studentsAttended) {
        this.studentsAttended = studentsAttended;
    }

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }
}

