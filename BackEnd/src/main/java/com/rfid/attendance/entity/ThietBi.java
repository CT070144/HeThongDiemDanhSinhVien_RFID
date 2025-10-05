package com.rfid.attendance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "thietbi")
public class ThietBi {
    @Id
    @Column(name = "mathietbi", length = 50)
    @NotBlank
    private String maThietBi;

    @Column(name = "phonghoc", length = 50)
    @NotBlank
    private String phongHoc;

    public ThietBi() {}

    public ThietBi(String maThietBi, String phongHoc) {
        this.maThietBi = maThietBi;
        this.phongHoc = phongHoc;
    }

    public String getMaThietBi() {
        return maThietBi;
    }

    public void setMaThietBi(String maThietBi) {
        this.maThietBi = maThietBi;
    }

    public String getPhongHoc() {
        return phongHoc;
    }

    public void setPhongHoc(String phongHoc) {
        this.phongHoc = phongHoc;
    }
}


