package com.rfid.attendance.service;

import com.rfid.attendance.entity.PhongBan;
import com.rfid.attendance.repository.PhongBanRepository;
import com.rfid.attendance.repository.SinhVienRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PhongBanService {

    private final PhongBanRepository phongBanRepository;
    private final SinhVienRepository sinhVienRepository;

    public PhongBanService(PhongBanRepository phongBanRepository, SinhVienRepository sinhVienRepository) {
        this.phongBanRepository = phongBanRepository;
        this.sinhVienRepository = sinhVienRepository;
    }

    public List<PhongBan> getAll() {
        return phongBanRepository.findAll();
    }

    public Optional<PhongBan> getById(String maPhongBan) {
        return phongBanRepository.findById(maPhongBan);
    }

    public PhongBan create(PhongBan phongBan) {
        if (phongBan.getMaPhongBan() == null || phongBan.getMaPhongBan().isBlank()) {
            throw new IllegalArgumentException("Mã phòng ban không được để trống");
        }
        if (phongBanRepository.existsById(phongBan.getMaPhongBan())) {
            throw new IllegalArgumentException("Mã phòng ban đã tồn tại");
        }
        return phongBanRepository.save(phongBan);
    }

    public PhongBan update(String maPhongBan, PhongBan details) {
        PhongBan existing = phongBanRepository.findById(maPhongBan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ban"));

        if (details.getTenPhongBan() != null) {
            existing.setTenPhongBan(details.getTenPhongBan());
        }
        if (details.getMoTa() != null) {
            existing.setMoTa(details.getMoTa());
        }
        return phongBanRepository.save(existing);
    }

    public void delete(String maPhongBan) {
        if (!phongBanRepository.existsById(maPhongBan)) {
            throw new IllegalArgumentException("Không tìm thấy phòng ban");
        }
        if (sinhVienRepository.existsByMaPhongBan(maPhongBan)) {
            throw new IllegalArgumentException("Không thể xóa phòng ban vì đã phát sinh nhân viên thuộc phòng ban này");
        }
        phongBanRepository.deleteById(maPhongBan);
    }
}
