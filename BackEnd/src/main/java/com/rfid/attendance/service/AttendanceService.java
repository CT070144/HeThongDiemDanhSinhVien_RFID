package com.rfid.attendance.service;

import com.rfid.attendance.entity.DocRfid;
import com.rfid.attendance.entity.PhieuDiemDanh;
import com.rfid.attendance.entity.SinhVien;
import com.rfid.attendance.entity.ThietBi;
import com.rfid.attendance.repository.DocRfidRepository;
import com.rfid.attendance.repository.PhieuDiemDanhRepository;
import com.rfid.attendance.repository.SinhVienRepository;
import com.rfid.attendance.repository.ThietBiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AttendanceService {
    
    @Autowired
    private PhieuDiemDanhRepository phieuDiemDanhRepository;
    
    @Autowired
    private SinhVienRepository sinhVienRepository;
    
    @Autowired
    private DocRfidRepository docRfidRepository;

    @Autowired
    private ThietBiRepository thietBiRepository;
    
    public List<PhieuDiemDanh> getAllAttendance() {
        return phieuDiemDanhRepository.findAll();
    }
    
    public List<PhieuDiemDanh> getAttendanceByFilters(LocalDate ngay, Integer ca, String maSinhVien, String phongHoc) {
        return phieuDiemDanhRepository.findByFilters(ngay, ca, maSinhVien, phongHoc);
    }
    
    public List<PhieuDiemDanh> getTodayAttendance() {
        return phieuDiemDanhRepository.findTodayAttendance();
    }
    
    public List<PhieuDiemDanh> getAttendanceByStudent(String maSinhVien) {
        return phieuDiemDanhRepository.findByMaSinhVien(maSinhVien);
    }
    
    public PhieuDiemDanh processRfidAttendance(String rfid) {
        // Kiểm tra sinh viên có tồn tại không
        Optional<SinhVien> sinhVienOpt = sinhVienRepository.findByRfid(rfid);
        
        if (!sinhVienOpt.isPresent()) {
            // Nếu không tồn tại, lưu vào bảng doc_rfid (transaction riêng) và trả lỗi nghiệp vụ
            saveUnregisteredRfid(rfid);
            return new PhieuDiemDanh();
        }
        
        SinhVien sinhVien = sinhVienOpt.get();
        LocalDate today = LocalDate.now();
        Integer currentCa = getCurrentCa();
        if (currentCa == 0) {
            throw new RuntimeException("Ngoài giờ học");
        }
        
        // Tìm phiếu điểm danh hiện tại
        Optional<PhieuDiemDanh> existingRecord = phieuDiemDanhRepository
                .findByRfidAndNgayAndCa(rfid, today, currentCa);
        
        if (existingRecord.isPresent()) {
            // Đã có bản ghi, cập nhật giờ ra
            PhieuDiemDanh record = existingRecord.get();
            if (record.getGioRa() == null) {
                record.setGioRa(LocalTime.now());
                record.setTrangThai(PhieuDiemDanh.TrangThai.DA_RA_VE);
                return phieuDiemDanhRepository.save(record);
            } else {
                throw new RuntimeException("Sinh viên đã điểm danh ra trong ca này");
            }
        } else {
            // Tạo bản ghi mới
            LocalTime currentTime = LocalTime.now();
            PhieuDiemDanh.TrangThai trangThai = determineAttendanceStatus(currentTime, currentCa);
            
            PhieuDiemDanh newRecord = new PhieuDiemDanh();
            newRecord.setRfid(rfid);
            newRecord.setMaSinhVien(sinhVien.getMaSinhVien());
            newRecord.setTenSinhVien(sinhVien.getTenSinhVien());
            newRecord.setGioVao(currentTime);
            newRecord.setNgay(today);
            newRecord.setCa(currentCa);
            newRecord.setTrangThai(trangThai);
            
            return phieuDiemDanhRepository.save(newRecord);
        }
    }

    public PhieuDiemDanh processRfidAttendanceWithDevice(String rfid, String maThietBi) {
        PhieuDiemDanh record = processRfidAttendance(rfid);
        if (record.getId() == null) return record;
        if (maThietBi != null && !maThietBi.isEmpty()) {
            Optional<ThietBi> tb = thietBiRepository.findById(maThietBi);
            tb.ifPresent(thietBi -> {
                record.setPhongHoc(thietBi.getPhongHoc());
                phieuDiemDanhRepository.save(record);
            });
        }
        return record;
    }
    
    private Integer getCurrentCa() {
        LocalTime currentTime = LocalTime.now();
        
        if (currentTime.isAfter(LocalTime.of(7, 0)) && currentTime.isBefore(LocalTime.of(9, 30))) {
            return 1;
        } else if (currentTime.isAfter(LocalTime.of(9, 30)) && currentTime.isBefore(LocalTime.of(12, 0))) {
            return 2;
        } else if (currentTime.isAfter(LocalTime.of(12, 30)) && currentTime.isBefore(LocalTime.of(15, 0))) {
            return 3;
        } else if (currentTime.isAfter(LocalTime.of(15, 0)) && currentTime.isBefore(LocalTime.of(22, 30))) {
            return 4;
        } else {
            // Ngoài giờ học
            return 0;
        }
    }
    
    private PhieuDiemDanh.TrangThai determineAttendanceStatus(LocalTime currentTime, Integer ca) {
        switch (ca) {
            case 1:
                if (currentTime.isAfter(LocalTime.of(7, 15))) {
                    return PhieuDiemDanh.TrangThai.MUON;
                }
                break;
            case 2:
                if (currentTime.isAfter(LocalTime.of(9, 45))) {
                    return PhieuDiemDanh.TrangThai.MUON;
                }
                break;
            case 3:
                if (currentTime.isAfter(LocalTime.of(12, 45))) {
                    return PhieuDiemDanh.TrangThai.MUON;
                }
                break;
            case 4:
                if (currentTime.isAfter(LocalTime.of(15, 15))) {
                    return PhieuDiemDanh.TrangThai.MUON;
                }
                break;
        }
        return PhieuDiemDanh.TrangThai.DANG_HOC;
    }
    
    public List<DocRfid> getUnprocessedRfids() {
        return docRfidRepository.findUnprocessedRfids();
    }
    
    public void markRfidAsProcessed(Long id) {
        Optional<DocRfid> docRfidOpt = docRfidRepository.findById(id);
        if (docRfidOpt.isPresent()) {
            DocRfid docRfid = docRfidOpt.get();
            docRfidRepository.delete(docRfid);
        }
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveUnregisteredRfid(String rfid) {
        if (!docRfidRepository.existsByRfid(rfid)) {
            DocRfid d = new DocRfid(rfid);
            docRfidRepository.save(d);
        }
    }
}
