package com.rfid.attendance.controller;

import com.rfid.attendance.service.CaHocImportService;
import com.rfid.attendance.repository.CaHocRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/cahoc")
@CrossOrigin(origins = "*")
public class CaHocController {

    private final CaHocImportService caHocImportService;
    private final CaHocRepository caHocRepository;

    public CaHocController(CaHocImportService caHocImportService, CaHocRepository caHocRepository) {
        this.caHocImportService = caHocImportService;
        this.caHocRepository = caHocRepository;
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCaHoc(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        if (file == null || file.isEmpty()) {
            response.put("message", "File không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx"))) {
            response.put("message", "File phải có định dạng Excel (.xls hoặc .xlsx)");
            return ResponseEntity.badRequest().body(response);
        }
        try {
            Map<String, Object> result = caHocImportService.importCaHocFromExcel(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            response.put("message", "Lỗi khi xử lý file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> getSessionsByLopHocPhan(@RequestParam("lopHocPhan") String lopHocPhan) {
        try {
            List<Object[]> rows = caHocRepository.findDistinctSessionsByLopHocPhan(lopHocPhan);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] r : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("ngayHoc", r[0]);
                item.put("ca", r[1]);
                result.add(item);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}


