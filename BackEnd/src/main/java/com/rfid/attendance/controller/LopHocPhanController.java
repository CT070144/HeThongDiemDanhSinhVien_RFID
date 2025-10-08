package com.rfid.attendance.controller;

import com.rfid.attendance.dto.LopHocPhanDTO;
import com.rfid.attendance.entity.LopHocPhan;
import com.rfid.attendance.entity.SinhVien;
import com.rfid.attendance.service.LopHocPhanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/lophocphan")
@CrossOrigin(origins = "*")
public class LopHocPhanController {
    
    @Autowired
    private LopHocPhanService lopHocPhanService;
    
    @GetMapping
    public ResponseEntity<List<LopHocPhanDTO>> getAllLopHocPhan() {
        try {
            List<LopHocPhanDTO> lopHocPhans = lopHocPhanService.getAllLopHocPhan();
            return ResponseEntity.ok(lopHocPhans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{maLopHocPhan}")
    public ResponseEntity<LopHocPhan> getLopHocPhanByMaLopHocPhan(@PathVariable String maLopHocPhan) {
        try {
            Optional<LopHocPhan> lopHocPhan = lopHocPhanService.getLopHocPhanByMaLopHocPhan(maLopHocPhan);
            if (lopHocPhan.isPresent()) {
                return ResponseEntity.ok(lopHocPhan.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<LopHocPhanDTO>> searchLopHocPhan(@RequestParam(required = false) String keyword) {
        try {
            List<LopHocPhanDTO> lopHocPhans = lopHocPhanService.searchLopHocPhan(keyword);
            return ResponseEntity.ok(lopHocPhans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createLopHocPhan(@RequestBody LopHocPhan lopHocPhan) {
        Map<String, Object> response = new HashMap<>();
        try {
            LopHocPhan created = lopHocPhanService.createLopHocPhan(lopHocPhan);
            response.put("success", true);
            response.put("message", "Tạo lớp học phần thành công");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PutMapping("/{maLopHocPhan}")
    public ResponseEntity<Map<String, Object>> updateLopHocPhan(@PathVariable String maLopHocPhan, @RequestBody LopHocPhan lopHocPhanDetails) {
        Map<String, Object> response = new HashMap<>();
        try {
            LopHocPhan updated = lopHocPhanService.updateLopHocPhan(maLopHocPhan, lopHocPhanDetails);
            response.put("success", true);
            response.put("message", "Cập nhật lớp học phần thành công");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @DeleteMapping("/{maLopHocPhan}")
    public ResponseEntity<Map<String, Object>> deleteLopHocPhan(@PathVariable String maLopHocPhan) {
        Map<String, Object> response = new HashMap<>();
        try {
            lopHocPhanService.deleteLopHocPhan(maLopHocPhan);
            response.put("success", true);
            response.put("message", "Xóa lớp học phần thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/{maLopHocPhan}/sinhvien")
    public ResponseEntity<List<SinhVien>> getSinhVienByLopHocPhan(@PathVariable String maLopHocPhan) {
        try {
            List<SinhVien> sinhViens = lopHocPhanService.getSinhVienByLopHocPhan(maLopHocPhan);
            return ResponseEntity.ok(sinhViens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importSinhVienFromExcel(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "File không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (!isExcelFile(file)) {
            response.put("success", false);
            response.put("message", "File phải có định dạng Excel (.xls hoặc .xlsx)");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            Map<String, Object> result = lopHocPhanService.importSinhVienFromExcel(file);
            
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) result.get("errors");
            @SuppressWarnings("unchecked")
            List<String> successes = (List<String>) result.get("successes");
            
            response.put("success", true);
            response.put("message", "Import hoàn thành");
            response.put("totalSheets", result.get("totalSheets"));
            response.put("totalStudents", result.get("totalStudents"));
            response.put("totalClasses", result.get("totalClasses"));
            response.put("successes", successes);
            response.put("errors", errors);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi xử lý file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/{maLopHocPhan}/count")
    public ResponseEntity<Map<String, Object>> getStudentCountByLopHocPhan(@PathVariable String maLopHocPhan) {
        Map<String, Object> response = new HashMap<>();
        try {
            long count = lopHocPhanService.countSinhVienInLopHocPhan(maLopHocPhan);
            response.put("success", true);
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private boolean isExcelFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        return fileName != null && (fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx"));
    }
}
