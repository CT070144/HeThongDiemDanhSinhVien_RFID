package com.rfid.attendance.controller;

import com.rfid.attendance.entity.DocRfid;
import com.rfid.attendance.entity.SinhVien;
import com.rfid.attendance.repository.DocRfidRepository;
import com.rfid.attendance.repository.SinhVienLopHocPhanRepository;
import com.rfid.attendance.service.SinhVienService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sinhvien")
@CrossOrigin(origins = "*")
public class SinhVienController {
    
    @Autowired
    private SinhVienService sinhVienService;
    @Autowired
    private DocRfidRepository docRfidRepository;
    @Autowired
    private SinhVienLopHocPhanRepository sinhVienLopHocPhanRepository;
    
    @GetMapping
    public ResponseEntity<List<SinhVien>> getAllSinhVien() {
        try {
            List<SinhVien> sinhViens = sinhVienService.getAllSinhVien();
            return ResponseEntity.ok(sinhViens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{maSinhVien}")
    public ResponseEntity<SinhVien> getSinhVienByMaSinhVien(@PathVariable String maSinhVien) {
        try {
            Optional<SinhVien> sinhVien = sinhVienService.getSinhVienByMaSinhVien(maSinhVien);
            return sinhVien.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/rfid/{rfid}")
    public ResponseEntity<SinhVien> getSinhVienByRfid(@PathVariable String rfid) {
        try {
            Optional<SinhVien> sinhVien = sinhVienService.getSinhVienByRfid(rfid);
            return sinhVien.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<SinhVien>> searchSinhVien(@RequestParam(required = false) String keyword) {
        try {
            List<SinhVien> sinhViens = sinhVienService.searchSinhVien(keyword);
            return ResponseEntity.ok(sinhViens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createSinhVien(@Valid @RequestBody SinhVien sinhVien) {
        try {
            SinhVien createdSinhVien = sinhVienService.createSinhVien(sinhVien);
            docRfidRepository.findByRfid(sinhVien.getRfid()).ifPresent(doc -> {
                doc.setMaSinhVien(sinhVien.getMaSinhVien());
                doc.setTenSinhVien(sinhVien.getTenSinhVien());
                doc.setProcessed(true);
                docRfidRepository.save(doc);
            });
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSinhVien);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{maSinhVien}")
    public ResponseEntity<?> updateSinhVien(@PathVariable String maSinhVien, @Valid @RequestBody SinhVien sinhVienDetails) {
        try {
            // Lấy thông tin sinh viên cũ trước khi cập nhật
            Optional<SinhVien> oldSinhVienOpt = sinhVienService.getSinhVienByMaSinhVien(maSinhVien);
            String oldRfid = oldSinhVienOpt.map(SinhVien::getRfid).orElse(null);
            
            SinhVien updatedSinhVien = sinhVienService.updateSinhVien(maSinhVien, sinhVienDetails);
            docRfidRepository.findByRfid(sinhVienDetails.getRfid()).ifPresent(
                    docRfid1 -> {
                        docRfid1.setProcessed(true);
                        docRfidRepository.save(docRfid1);
                    }
            );


            // Cập nhật thông tin trong bảng docrfid
            if (oldRfid != null && !oldRfid.equals(updatedSinhVien.getRfid())) {
                // RFID đã thay đổi - cập nhật bản ghi docrfid với RFID cũ
                docRfidRepository.findByRfid(oldRfid).ifPresent(doc -> {
                    doc.setRfid(updatedSinhVien.getRfid());
                    doc.setMaSinhVien(updatedSinhVien.getMaSinhVien());
                    doc.setTenSinhVien(updatedSinhVien.getTenSinhVien());
                    doc.setProcessed(true);
                    docRfidRepository.save(doc);
                });
            } else {
                // RFID không thay đổi - chỉ cập nhật thông tin sinh viên
                docRfidRepository.findByRfid(updatedSinhVien.getRfid()).ifPresent(doc -> {
                    doc.setMaSinhVien(updatedSinhVien.getMaSinhVien());
                    doc.setTenSinhVien(updatedSinhVien.getTenSinhVien());
                    doc.setProcessed(true);
                    docRfidRepository.save(doc);
                });
            }
            
            return ResponseEntity.ok(updatedSinhVien);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{maSinhVien}")
    public ResponseEntity<?> deleteSinhVien(@PathVariable String maSinhVien) {
        try {
            // Kiểm tra xem sinh viên có đang tham gia lớp học phần nào không
            var lopHocPhans = sinhVienLopHocPhanRepository.findByMaSinhVien(maSinhVien);
            if (!lopHocPhans.isEmpty()) {
                StringBuilder lopNames = new StringBuilder();
                for (int i = 0; i < lopHocPhans.size(); i++) {
                    if (i > 0) lopNames.append(", ");
                    lopNames.append(lopHocPhans.get(i).getMaLopHocPhan());
                }
                return ResponseEntity.badRequest().body(
                    "Không thể xóa sinh viên vì đang tham gia các lớp học phần: " + lopNames.toString() + 
                    ". Vui lòng xóa sinh viên khỏi các lớp học phần trước khi xóa sinh viên."
                );
            }
            
            sinhVienService.deleteSinhVien(maSinhVien);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/exists/{rfid}")
    public ResponseEntity<Boolean> checkRfidExists(@PathVariable String rfid) {
        try {
            boolean exists = sinhVienService.existsByRfid(rfid);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
