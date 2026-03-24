package com.rfid.attendance.service;

import com.rfid.attendance.entity.CaLam;
import com.rfid.attendance.repository.CaLamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CaLamService {

    private final CaLamRepository caLamRepository;

    public CaLamService(CaLamRepository caLamRepository) {
        this.caLamRepository = caLamRepository;
    }

    public List<CaLam> getAll() {
        return caLamRepository.findAllByOrderByMaCaAsc();
    }

    public Optional<CaLam> getByMaCa(Integer maCa) {
        return caLamRepository.findByMaCa(maCa);
    }

    public CaLam create(CaLam caLam) {
        if (caLam == null) {
            throw new IllegalArgumentException("Ca làm không được để trống");
        }
        if (caLam.getMaCa() == null) {
            throw new IllegalArgumentException("Mã ca không được để trống");
        }
        if (caLamRepository.findByMaCa(caLam.getMaCa()).isPresent()) {
            throw new IllegalArgumentException("Mã ca đã tồn tại: " + caLam.getMaCa());
        }
        validateTimeRange(caLam.getGioBatDau(), caLam.getGioKetThuc());
        if (!StringUtils.hasText(caLam.getTenCa())) {
            throw new IllegalArgumentException("Tên ca không được để trống");
        }
        if (caLam.getChoPhepTrePhut() == null || caLam.getChoPhepTrePhut() < 0) {
            throw new IllegalArgumentException("Cho phép trễ phút không hợp lệ");
        }

        // Kiểm tra trùng giờ giữa các ca
        checkOverlappingShifts(caLam);

        return caLamRepository.save(caLam);
    }

    public CaLam update(Integer maCa, CaLam caLam) {
        if (maCa == null) {
            throw new IllegalArgumentException("Mã ca không được để trống");
        }
        Optional<CaLam> existingOpt = caLamRepository.findByMaCa(maCa);
        if (!existingOpt.isPresent()) {
            throw new IllegalArgumentException("Không tìm thấy ca có mã: " + maCa);
        }
        CaLam existing = existingOpt.get();
        if (caLam == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật không được để trống");
        }

        existing.setTenCa(caLam.getTenCa());
        existing.setGioBatDau(caLam.getGioBatDau());
        existing.setGioKetThuc(caLam.getGioKetThuc());
        existing.setChoPhepTrePhut(caLam.getChoPhepTrePhut());

        validateTimeRange(existing.getGioBatDau(), existing.getGioKetThuc());

        // Kiểm tra trùng giờ với các ca khác (loại trừ chính ca đang update)
        checkOverlappingShifts(existing);

        return caLamRepository.save(existing);
    }

    public void delete(Integer maCa) {
        if (maCa == null) {
            throw new IllegalArgumentException("Mã ca không được để trống");
        }
        Optional<CaLam> existingOpt = caLamRepository.findByMaCa(maCa);
        if (!existingOpt.isPresent()) {
            throw new IllegalArgumentException("Không tìm thấy ca có mã: " + maCa);
        }
        caLamRepository.delete(existingOpt.get());
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Giờ bắt đầu/kết thúc không được để trống");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }
    }

    /**
     * Kiểm tra ca có bị trùng khoảng giờ với ca khác hay không.
     * Rule: coi khoảng giờ là [start, end) (end trùng thì không overlap).
     */
    private void checkOverlappingShifts(CaLam candidate) {
        if (candidate == null || candidate.getGioBatDau() == null || candidate.getGioKetThuc() == null) {
            return;
        }

        LocalTime newStart = candidate.getGioBatDau();
        LocalTime newEnd = candidate.getGioKetThuc();

        List<CaLam> existing = caLamRepository.findAll()
                .stream()
                .filter(c -> c != null && c.getMaCa() != null)
                .collect(Collectors.toList());

        for (CaLam shift : existing) {
            if (shift == null) continue;

            // Khi update: bỏ qua chính shift có cùng maCa
            if (candidate.getMaCa() != null && candidate.getMaCa().equals(shift.getMaCa())) {
                continue;
            }

            LocalTime oldStart = shift.getGioBatDau();
            LocalTime oldEnd = shift.getGioKetThuc();
            if (oldStart == null || oldEnd == null) continue;

            boolean overlaps = newStart.isBefore(oldEnd) && newEnd.isAfter(oldStart);
            if (overlaps) {
                throw new IllegalArgumentException(
                        "Trùng giờ làm giữa ca " + candidate.getMaCa() + " và ca " + shift.getMaCa()
                );
            }
        }
    }
}

