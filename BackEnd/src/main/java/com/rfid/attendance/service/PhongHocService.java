package com.rfid.attendance.service;

import com.rfid.attendance.entity.PhongHoc;
import com.rfid.attendance.repository.PhongHocRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PhongHocService {

    private final PhongHocRepository phongHocRepository;

    public PhongHocService(PhongHocRepository phongHocRepository) {
        this.phongHocRepository = phongHocRepository;
    }

    public List<PhongHoc> getAll() {
        return phongHocRepository.findAll();
    }

    public Optional<PhongHoc> getById(String maPhong) {
        return phongHocRepository.findById(maPhong);
    }

    public List<PhongHoc> search(String keyword) {
        return phongHocRepository.search(keyword);
    }

    public Page<PhongHoc> getPaged(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        if (keyword != null && !keyword.isBlank()) {
            return phongHocRepository.searchPaged(keyword, pageable);
        }
        return phongHocRepository.findAll(pageable);
    }

    public PhongHoc create(PhongHoc phongHoc) {
        if (phongHoc.getMaPhong() == null || phongHoc.getMaPhong().isBlank()) {
            throw new IllegalArgumentException("Mã phòng không được để trống");
        }
        if (phongHocRepository.existsById(phongHoc.getMaPhong())) {
            throw new IllegalArgumentException("Mã phòng đã tồn tại");
        }
        if (phongHoc.getTrangThai() == null || phongHoc.getTrangThai().isBlank()) {
            phongHoc.setTrangThai("active");
        }
        return phongHocRepository.save(phongHoc);
    }

    public PhongHoc update(String maPhong, PhongHoc details) {
        PhongHoc existing = phongHocRepository.findById(maPhong)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng học"));

        if (details.getTenPhong() != null) existing.setTenPhong(details.getTenPhong());
        if (details.getToaNha() != null) existing.setToaNha(details.getToaNha());
        if (details.getTang() != null) existing.setTang(details.getTang());
        if (details.getSucChua() != null) existing.setSucChua(details.getSucChua());
        if (details.getLoaiPhong() != null) existing.setLoaiPhong(details.getLoaiPhong());
        if (details.getTrangThai() != null) existing.setTrangThai(details.getTrangThai());

        return phongHocRepository.save(existing);
    }

    public void delete(String maPhong) {
        if (!phongHocRepository.existsById(maPhong)) {
            throw new IllegalArgumentException("Không tìm thấy phòng học");
        }
        phongHocRepository.deleteById(maPhong);
    }
}


