package com.rfid.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Entity
@Table(name = "calam")
public class CaLam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã ca (hiện tại hệ thống đang dùng 1..5)
    @NotNull
    @Column(name = "ma_ca", nullable = false, unique = true)
    private Integer maCa;

    @NotBlank
    @Column(name = "ten_ca", length = 100, nullable = false)
    private String tenCa;

    @NotNull
    @Column(name = "gio_bat_dau", nullable = false)
    private LocalTime gioBatDau;

    @NotNull
    @Column(name = "gio_ket_thuc", nullable = false)
    private LocalTime gioKetThuc;

    // Phút được phép trễ sau `gioBatDau`
    @NotNull
    @Min(0)
    @Max(240)
    @Column(name = "cho_phep_tre_phut", nullable = false)
    private Integer choPhepTrePhut;

    public Long getId() {
        return id;
    }

    public Integer getMaCa() {
        return maCa;
    }

    public void setMaCa(Integer maCa) {
        this.maCa = maCa;
    }

    public String getTenCa() {
        return tenCa;
    }

    public void setTenCa(String tenCa) {
        this.tenCa = tenCa;
    }

    public LocalTime getGioBatDau() {
        return gioBatDau;
    }

    public void setGioBatDau(LocalTime gioBatDau) {
        this.gioBatDau = gioBatDau;
    }

    public LocalTime getGioKetThuc() {
        return gioKetThuc;
    }

    public void setGioKetThuc(LocalTime gioKetThuc) {
        this.gioKetThuc = gioKetThuc;
    }

    public Integer getChoPhepTrePhut() {
        return choPhepTrePhut;
    }

    public void setChoPhepTrePhut(Integer choPhepTrePhut) {
        this.choPhepTrePhut = choPhepTrePhut;
    }
}

