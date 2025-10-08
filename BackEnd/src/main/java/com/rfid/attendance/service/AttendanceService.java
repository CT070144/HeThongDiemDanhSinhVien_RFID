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
        // Log thông tin debug
        System.out.println("=== RFID ATTENDANCE DEBUG ===");
        System.out.println("RFID nhận được: '" + rfid + "'");
        System.out.println("Độ dài RFID: " + (rfid != null ? rfid.length() : "null"));
        System.out.println("RFID trimmed: '" + (rfid != null ? rfid.trim() : "null") + "'");
        
        // Trim RFID để tránh lỗi do khoảng trắng
        String trimmedRfid = rfid != null ? rfid.trim() : "";
        if (trimmedRfid.isEmpty()) {
            System.out.println("RFID rỗng hoặc null");
            saveUnregisteredRfid(rfid);
            return new PhieuDiemDanh();
        }
        
        // Kiểm tra sinh viên có tồn tại không
        Optional<SinhVien> sinhVienOpt = sinhVienRepository.findByRfid(trimmedRfid);
        System.out.println("Kết quả tìm kiếm sinh viên: " + (sinhVienOpt.isPresent() ? "Tìm thấy" : "Không tìm thấy"));
        
        if (!sinhVienOpt.isPresent()) {
            System.out.println("Không tìm thấy sinh viên với RFID: " + trimmedRfid);
            
            // Debug: Kiểm tra tất cả RFID trong database
            List<SinhVien> allStudents = sinhVienRepository.findAll();
            System.out.println("Tổng số sinh viên trong DB: " + allStudents.size());
            System.out.println("Danh sách RFID trong DB:");
            for (SinhVien sv : allStudents) {
                System.out.println("- '" + sv.getRfid() + "' (độ dài: " + sv.getRfid().length() + ")");
            }
            
            // Nếu không tồn tại, lưu vào bảng doc_rfid (transaction riêng) và trả lỗi nghiệp vụ
            saveUnregisteredRfid(trimmedRfid);
            return new PhieuDiemDanh();
        }
        
        SinhVien sinhVien = sinhVienOpt.get();
        System.out.println("Tìm thấy sinh viên: " + sinhVien.getTenSinhVien() + " (Mã: " + sinhVien.getMaSinhVien() + ")");
        
        LocalDate today = LocalDate.now();
        Integer currentCa = getCurrentCa();
        System.out.println("Ngày hiện tại: " + today + ", Ca hiện tại: " + currentCa);
        
        if (currentCa == 0) {
            System.out.println("Ngoài giờ học");
            throw new RuntimeException("Ngoài giờ học");
        }
        
        // Tìm phiếu điểm danh hiện tại
        Optional<PhieuDiemDanh> existingRecord = phieuDiemDanhRepository
                .findByRfidAndNgayAndCa(trimmedRfid, today, currentCa);
        
        if (existingRecord.isPresent()) {
            System.out.println("tồn tại record");
            // Đã có bản ghi, cập nhật giờ ra
            PhieuDiemDanh record = existingRecord.get();
            if (record.getGioRa() == null) {
                record.setGioRa(LocalTime.now());
                // Không thay đổi trạng thái, giữ nguyên trạng thái hiện tại (MUON hoặc DANG_HOC)
                return phieuDiemDanhRepository.save(record);
            } else {
                throw new RuntimeException("Sinh viên đã điểm danh ra trong ca này");
            }
        } else {
            // Tạo bản ghi mới
            LocalTime currentTime = LocalTime.now();
            PhieuDiemDanh.TrangThai trangThai = determineAttendanceStatus(currentTime, currentCa);
            
            PhieuDiemDanh newRecord = new PhieuDiemDanh();
            newRecord.setRfid(trimmedRfid);
            newRecord.setMaSinhVien(sinhVien.getMaSinhVien());
            newRecord.setTenSinhVien(sinhVien.getTenSinhVien());
            newRecord.setGioVao(currentTime);
            newRecord.setNgay(today);
            newRecord.setCa(currentCa);
            newRecord.setTrangThai(trangThai);
            
            System.out.println("Tạo phiếu điểm danh mới: " + newRecord.getTenSinhVien() + " - Ca " + currentCa);
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
        
        if (currentTime.isAfter(LocalTime.of(0, 0)) && currentTime.isBefore(LocalTime.of(9, 30))) {
            return 1;
        } else if (currentTime.isAfter(LocalTime.of(9, 30)) && currentTime.isBefore(LocalTime.of(12, 0))) {
            return 2;
        } else if (currentTime.isAfter(LocalTime.of(12, 30)) && currentTime.isBefore(LocalTime.of(15, 0))) {
            return 3;
        } else if (currentTime.isAfter(LocalTime.of(15, 0)) && currentTime.isBefore(LocalTime.of(23, 55))) {
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
    
    // Getter cho repository để debug
    public SinhVienRepository getSinhVienRepository() {
        return sinhVienRepository;
    }
}
