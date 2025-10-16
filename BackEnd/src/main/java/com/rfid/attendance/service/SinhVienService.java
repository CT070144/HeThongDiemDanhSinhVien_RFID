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
import java.util.HashMap;
import java.util.Map;

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
        if(!sinhVienDetails.getRfid().equals(oldRfid)){
        docRfidRepository.findByRfid(oldRfid).ifPresent(docRfid -> {
            docRfidRepository.delete(docRfid);
        });}
        // Sync to docrfid (cập nhật RFID mới)
        docRfidRepository.findByRfid(sinhVienDetails.getRfid()).ifPresent(doc -> {
            System.out.println("vô đây r nèeeee");
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
    
    public Map<String, Object> bulkUpdateRfid(List<SinhVien> sinhVienList) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new java.util.ArrayList<>();
        
        for (SinhVien sinhVienData : sinhVienList) {
            try {
                Optional<SinhVien> existingSinhVien = sinhVienRepository.findByMaSinhVien(sinhVienData.getMaSinhVien());
                
                if (existingSinhVien.isPresent()) {
                    // Cập nhật sinh viên đã tồn tại
                    SinhVien sinhVien = existingSinhVien.get();
                    String oldRfid = sinhVien.getRfid();
                    
                    // Kiểm tra RFID có bị trùng không (nếu thay đổi)
                    if (!sinhVien.getRfid().equals(sinhVienData.getRfid()) &&
                        sinhVienRepository.existsByRfid(sinhVienData.getRfid())) {
                        throw new RuntimeException("RFID đã tồn tại: " + sinhVienData.getRfid());
                    }
                    
                    sinhVien.setRfid(sinhVienData.getRfid());
                    sinhVien.setTenSinhVien(sinhVienData.getTenSinhVien());
                    
                    SinhVien saved = sinhVienRepository.save(sinhVien);
                    
                    // Sync to docrfid - cập nhật RFID cũ nếu có
                    docRfidRepository.findByRfid(oldRfid).ifPresent(doc -> {
                        doc.setRfid(saved.getRfid());
                        doc.setMaSinhVien(saved.getMaSinhVien());
                        doc.setTenSinhVien(saved.getTenSinhVien());
                        doc.setProcessed(true);
                        docRfidRepository.save(doc);
                        System.out.println("Đã cập nhật docrfid RFID cũ: " + oldRfid + " -> " + saved.getRfid() + " cho sinh viên: " + saved.getTenSinhVien());
                    });
                    
                    // Kiểm tra và cập nhật docrfid với RFID mới nếu có
                    docRfidRepository.findByRfid(saved.getRfid()).ifPresent(doc -> {
                        doc.setMaSinhVien(saved.getMaSinhVien());
                        doc.setTenSinhVien(saved.getTenSinhVien());
                        doc.setProcessed(true);
                        docRfidRepository.save(doc);
                        System.out.println("Đã cập nhật docrfid RFID mới: " + saved.getRfid() + " cho sinh viên: " + saved.getTenSinhVien());
                    });
                    
                    // Sync to phieudiemdanh
                    phieuDiemDanhRepository.updateStudentInfoByRfid(oldRfid, saved.getRfid(), saved.getMaSinhVien(), saved.getTenSinhVien());
                    
                } else {
                    // Tạo sinh viên mới
                    if (sinhVienRepository.existsByRfid(sinhVienData.getRfid())) {
                        throw new RuntimeException("RFID đã tồn tại: " + sinhVienData.getRfid());
                    }
                    
                    SinhVien newSinhVien = sinhVienRepository.save(sinhVienData);
                    
                    // Sync to docrfid - kiểm tra và cập nhật trạng thái đã xử lý
                    docRfidRepository.findByRfid(newSinhVien.getRfid()).ifPresent(doc -> {
                        doc.setMaSinhVien(newSinhVien.getMaSinhVien());
                        doc.setTenSinhVien(newSinhVien.getTenSinhVien());
                        doc.setProcessed(true);
                        docRfidRepository.save(doc);
                        System.out.println("Đã cập nhật docrfid cho sinh viên mới: " + newSinhVien.getTenSinhVien() + " - RFID: " + newSinhVien.getRfid());
                    });
                }
                
                successCount++;
                
            } catch (Exception e) {
                failureCount++;
                errors.add("Sinh viên " + sinhVienData.getMaSinhVien() + ": " + e.getMessage());
            }
        }
        
        result.put("totalProcessed", sinhVienList.size());
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("errors", errors);
        
        return result;
    }
}
