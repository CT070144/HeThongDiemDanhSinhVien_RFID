package com.rfid.attendance.service;

import com.rfid.attendance.entity.SinhVien;
import com.rfid.attendance.repository.DocRfidRepository;
import com.rfid.attendance.repository.PhieuDiemDanhRepository;
import com.rfid.attendance.repository.SinhVienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SinhVienService {
    
    @Autowired
    private SinhVienRepository sinhVienRepository;
    @Autowired
    private DocRfidRepository docRfidRepository;
    @Autowired
    private PhieuDiemDanhRepository phieuDiemDanhRepository;
    
    public List<SinhVien> getAllSinhVien() {
        return sinhVienRepository.findAll();
    }
    
    public Optional<SinhVien> getSinhVienByRfid(String rfid) {
        return sinhVienRepository.findByRfid(rfid);
    }
    
    public Optional<SinhVien> getSinhVienByMaSinhVien(String maSinhVien) {
        return sinhVienRepository.findByMaSinhVien(maSinhVien);
    }
    
    public List<SinhVien> searchSinhVien(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSinhVien();
        }
        return sinhVienRepository.findByKeyword(keyword.trim());
    }
    
    public SinhVien createSinhVien(SinhVien sinhVien) {
        if (sinhVienRepository.existsByRfid(sinhVien.getRfid())) {
            throw new RuntimeException("RFID đã tồn tại: " + sinhVien.getRfid());
        }
        if (sinhVienRepository.existsByMaSinhVien(sinhVien.getMaSinhVien())) {
            throw new RuntimeException("Mã sinh viên đã tồn tại: " + sinhVien.getMaSinhVien());
        }
        return sinhVienRepository.save(sinhVien);
    }
    
    public SinhVien updateSinhVien(String maSinhVien, SinhVien sinhVienDetails) {
        SinhVien sinhVien = sinhVienRepository.findByMaSinhVien(maSinhVien)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên với mã: " + maSinhVien));
        
        // Kiểm tra RFID có bị trùng không (nếu thay đổi)
        if (!sinhVien.getRfid().equals(sinhVienDetails.getRfid()) &&
            sinhVienRepository.existsByRfid(sinhVienDetails.getRfid())) {
            throw new RuntimeException("RFID đã tồn tại: " + sinhVienDetails.getRfid());
        }
        
        // Lưu RFID cũ để sync với các bảng khác
        String oldRfid = sinhVien.getRfid();
        
        sinhVien.setRfid(sinhVienDetails.getRfid());
        sinhVien.setTenSinhVien(sinhVienDetails.getTenSinhVien());
        
        SinhVien saved = sinhVienRepository.save(sinhVien);
        
        // Sync to docrfid (cập nhật RFID mới)
        docRfidRepository.findByRfid(oldRfid).ifPresent(doc -> {
            doc.setRfid(saved.getRfid());
            doc.setMaSinhVien(saved.getMaSinhVien());
            doc.setTenSinhVien(saved.getTenSinhVien());
            docRfidRepository.save(doc);
        });
        
        // Sync to phieudiemdanh (cập nhật RFID mới)
        phieuDiemDanhRepository.updateStudentInfoByRfid(oldRfid, saved.getRfid(), saved.getMaSinhVien(), saved.getTenSinhVien());
        
        return saved;
    }
    
    public void deleteSinhVien(String maSinhVien) {
        SinhVien sinhVien = sinhVienRepository.findByMaSinhVien(maSinhVien)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên với mã: " + maSinhVien));
        
        String rfid = sinhVien.getRfid();
        
        // Delete related records
        sinhVienRepository.deleteById(maSinhVien);
        docRfidRepository.findByRfid(rfid).ifPresent(docRfidRepository::delete);
        phieuDiemDanhRepository.deleteByRfid(rfid);
    }
    
    public boolean existsByRfid(String rfid) {
        return sinhVienRepository.existsByRfid(rfid);
    }
    
    public boolean existsByMaSinhVien(String maSinhVien) {
        return sinhVienRepository.existsByMaSinhVien(maSinhVien);
    }
}
