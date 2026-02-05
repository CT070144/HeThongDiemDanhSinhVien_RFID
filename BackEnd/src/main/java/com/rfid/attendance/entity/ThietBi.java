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

    @Column(name = "active")
    private Boolean active = true;

    public ThietBi() {}

    public ThietBi(String maThietBi, String phongHoc) {
        this.maThietBi = maThietBi;
        this.phongHoc = phongHoc;
        this.active = true;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}


