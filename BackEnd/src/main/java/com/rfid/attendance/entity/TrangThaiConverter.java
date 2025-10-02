package com.rfid.attendance.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TrangThaiConverter implements AttributeConverter<PhieuDiemDanh.TrangThai, String> {

    @Override
    public String convertToDatabaseColumn(PhieuDiemDanh.TrangThai attribute) {
        if (attribute == null) return null;
        return attribute.getCode();
    }

    @Override
    public PhieuDiemDanh.TrangThai convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        switch (dbData) {
            case "muon":
                return PhieuDiemDanh.TrangThai.MUON;
            case "dang_hoc":
                return PhieuDiemDanh.TrangThai.DANG_HOC;
            case "da_ra_ve":
                return PhieuDiemDanh.TrangThai.DA_RA_VE;
            default:
                // Không khớp, mặc định đang học
                return PhieuDiemDanh.TrangThai.DANG_HOC;
        }
    }
}


