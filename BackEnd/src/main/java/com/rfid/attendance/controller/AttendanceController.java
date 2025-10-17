package com.rfid.attendance.controller;

import com.rfid.attendance.entity.DocRfid;
import com.rfid.attendance.entity.PhieuDiemDanh;
import com.rfid.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
            @RequestParam(required = false) String maSinhVien,
            @RequestParam(required = false) String phongHoc) {
        try {
            List<PhieuDiemDanh> attendance = attendanceService.getAttendanceByFilters(ngay, ca, maSinhVien, phongHoc);
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
            System.out.println(request.getRfid());
            System.out.println(request.getMaThietBi());
            PhieuDiemDanh attendance = attendanceService.processRfidAttendanceWithDevice(request.getRfid(), request.getMaThietBi());
            String tensv = removeAccent(attendance.getTenSinhVien());
            attendance.setTenSinhVien(tensv);
            if (attendance.getRfid() == null) {
                return ResponseEntity.ok(new RfidResponse("not_found", ""));
            }
            return ResponseEntity.ok(new RfidResponse("found", attendance.getTenSinhVien()));
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            return ResponseEntity.badRequest().body(new RfidResponse("not_found", ""));
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
    
    @GetMapping("/debug/rfid/{rfid}")
    public ResponseEntity<?> debugRfid(@PathVariable String rfid) {
        try {
            // Debug thông tin RFID
            System.out.println("=== DEBUG RFID API ===");
            System.out.println("RFID nhận được: '" + rfid + "'");
            System.out.println("Độ dài: " + rfid.length());
            
            // Tìm sinh viên
            var sinhVienOpt = attendanceService.getSinhVienRepository().findByRfid(rfid.trim());
            
            if (sinhVienOpt.isPresent()) {
                var sinhVien = sinhVienOpt.get();
                return ResponseEntity.ok(Map.of(
                    "status", "found",
                    "rfid", rfid,
                    "student", Map.of(
                        "maSinhVien", sinhVien.getMaSinhVien(),
                        "tenSinhVien", sinhVien.getTenSinhVien(),
                        "rfid", sinhVien.getRfid()
                    )
                ));
            } else {
                // Hiển thị tất cả RFID trong database để debug
                var allStudents = attendanceService.getSinhVienRepository().findAll();
                List<Map<String, String>> allRfids = allStudents.stream()
                    .map(s -> Map.of(
                        "rfid", s.getRfid(),
                        "maSinhVien", s.getMaSinhVien(),
                        "tenSinhVien", s.getTenSinhVien()
                    ))
                    .collect(java.util.stream.Collectors.toList());
                
                return ResponseEntity.ok(Map.of(
                    "status", "not_found",
                    "searched_rfid", rfid,
                    "total_students", allStudents.size(),
                    "all_rfids", allRfids
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // Inner class for request body
    public static class RfidRequest {
        private String rfid;
        private String maThietBi;
        
        public String getRfid() {
            return rfid;
        }
        
        public void setRfid(String rfid) {
            this.rfid = rfid;
        }
        public String getMaThietBi() { return maThietBi; }
        public void setMaThietBi(String maThietBi) { this.maThietBi = maThietBi; }
    }

    public static class RfidResponse {
        private String status;
        private String name;
        public RfidResponse(String status, String name) {
            this.status = status; this.name = name;
        }
        public String getStatus() { return status; }
        public String getName() { return name; }
    }
    public static String removeAccent(String input) {
        // B1: Chuẩn hóa chuỗi thành dạng decomposed (chữ + dấu tách riêng)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // B2: Loại bỏ các ký tự dấu (ký tự Unicode tổ hợp)
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(normalized).replaceAll("");

        // B3: Một số ký tự đặc biệt tiếng Việt không nằm trong nhóm trên
        result = result.replace("đ", "d").replace("Đ", "D");

        return result;
    }
}
