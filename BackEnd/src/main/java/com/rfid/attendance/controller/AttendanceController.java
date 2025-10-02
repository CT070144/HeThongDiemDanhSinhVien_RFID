package com.rfid.attendance.controller;

import com.rfid.attendance.entity.DocRfid;
import com.rfid.attendance.entity.PhieuDiemDanh;
import com.rfid.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {
    
    @Autowired
    private AttendanceService attendanceService;
    
    @GetMapping
    public ResponseEntity<List<PhieuDiemDanh>> getAllAttendance() {
        try {
            List<PhieuDiemDanh> attendance = attendanceService.getAllAttendance();
            if (attendance == null || attendance.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/today")
    public ResponseEntity<List<PhieuDiemDanh>> getTodayAttendance() {
        try {
            List<PhieuDiemDanh> attendance = attendanceService.getTodayAttendance();
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/filter")
    public ResponseEntity<List<PhieuDiemDanh>> getAttendanceByFilters(
            @RequestParam(required = false) LocalDate ngay,
            @RequestParam(required = false) Integer ca,
            @RequestParam(required = false) String maSinhVien) {
        try {
            List<PhieuDiemDanh> attendance = attendanceService.getAttendanceByFilters(ngay, ca, maSinhVien);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/student/{maSinhVien}")
    public ResponseEntity<List<PhieuDiemDanh>> getAttendanceByStudent(@PathVariable String maSinhVien) {
        try {
            List<PhieuDiemDanh> attendance = attendanceService.getAttendanceByStudent(maSinhVien);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/rfid")
    public ResponseEntity<?> processRfidAttendance(@RequestBody RfidRequest request) {
        try {
            PhieuDiemDanh attendance = attendanceService.processRfidAttendance(request.getRfid());
            return ResponseEntity.ok(attendance);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/unprocessed-rfids")
    public ResponseEntity<List<DocRfid>> getUnprocessedRfids() {
        try {
            List<DocRfid> unprocessedRfids = attendanceService.getUnprocessedRfids();
            return ResponseEntity.ok(unprocessedRfids);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/mark-processed/{id}")
    public ResponseEntity<?> markRfidAsProcessed(@PathVariable Long id) {
        try {
            attendanceService.markRfidAsProcessed(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Inner class for request body
    public static class RfidRequest {
        private String rfid;
        
        public String getRfid() {
            return rfid;
        }
        
        public void setRfid(String rfid) {
            this.rfid = rfid;
        }
    }
}
