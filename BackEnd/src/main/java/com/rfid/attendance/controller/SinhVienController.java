package com.rfid.attendance.controller;

import com.rfid.attendance.entity.SinhVien;
import com.rfid.attendance.repository.DocRfidRepository;
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
    
    @GetMapping
    public ResponseEntity<List<SinhVien>> getAllSinhVien() {
        try {
            List<SinhVien> sinhViens = sinhVienService.getAllSinhVien();
            return ResponseEntity.ok(sinhViens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{rfid}")
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
    
    @PutMapping("/{rfid}")
    public ResponseEntity<?> updateSinhVien(@PathVariable String rfid, @Valid @RequestBody SinhVien sinhVienDetails) {
        try {
            SinhVien updatedSinhVien = sinhVienService.updateSinhVien(rfid, sinhVienDetails);
            docRfidRepository.findByRfid(updatedSinhVien.getRfid()).ifPresent(doc -> {
                doc.setMaSinhVien(updatedSinhVien.getMaSinhVien());
                doc.setTenSinhVien(updatedSinhVien.getTenSinhVien());
                docRfidRepository.save(doc);
            });
            return ResponseEntity.ok(updatedSinhVien);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{rfid}")
    public ResponseEntity<?> deleteSinhVien(@PathVariable String rfid) {
        try {
            sinhVienService.deleteSinhVien(rfid);
            docRfidRepository.findByRfid(rfid).ifPresent(doc -> docRfidRepository.delete(doc));
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
