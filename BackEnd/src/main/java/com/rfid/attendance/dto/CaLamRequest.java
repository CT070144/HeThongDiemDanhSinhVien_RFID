package com.rfid.attendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public class CaLamRequest {

    @NotNull
    private Integer maCa;

    @NotBlank
    private String tenCa;

    @NotNull
    private LocalTime gioBatDau;

    @NotNull
    private LocalTime gioKetThuc;

    @NotNull
    @Min(0)
    @Max(240)
    private Integer choPhepTrePhut;

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

