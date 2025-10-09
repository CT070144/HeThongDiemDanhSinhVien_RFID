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
            case "dung_gio":
                return PhieuDiemDanh.TrangThai.DUNG_GIO;
            case "muon":
                return PhieuDiemDanh.TrangThai.MUON;
            case "dang_hoc":
                // Convert old DANG_HOC status to DUNG_GIO
                return PhieuDiemDanh.TrangThai.DUNG_GIO;
            case "da_ra_ve":
                // Convert old DA_RA_VE status to DUNG_GIO
                return PhieuDiemDanh.TrangThai.DUNG_GIO;
            default:
                // Không khớp, mặc định đúng giờ
                return PhieuDiemDanh.TrangThai.DUNG_GIO;
        }
    }
}


