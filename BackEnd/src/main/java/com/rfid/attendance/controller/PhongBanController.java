package com.rfid.attendance.controller;

import com.rfid.attendance.entity.PhongBan;
import com.rfid.attendance.service.PhongBanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/phongban")
@CrossOrigin(origins = "*")
public class PhongBanController {

    private final PhongBanService phongBanService;

    public PhongBanController(PhongBanService phongBanService) {
        this.phongBanService = phongBanService;
    }

    @GetMapping
    public ResponseEntity<List<PhongBan>> getAll() {
        try {
            return ResponseEntity.ok(phongBanService.getAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{maPhongBan}")
    public ResponseEntity<PhongBan> getById(@PathVariable String maPhongBan) {
        try {
            Optional<PhongBan> phongBan = phongBanService.getById(maPhongBan);
            return phongBan.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody PhongBan phongBan) {
        Map<String, Object> res = new HashMap<>();
        try {
            PhongBan created = phongBanService.create(phongBan);
            res.put("success", true);
            res.put("data", created);
            res.put("message", "Tạo phòng ban thành công");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi tạo phòng ban");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @PutMapping("/{maPhongBan}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String maPhongBan, @RequestBody PhongBan phongBan) {
        Map<String, Object> res = new HashMap<>();
        try {
            PhongBan updated = phongBanService.update(maPhongBan, phongBan);
            res.put("success", true);
            res.put("data", updated);
            res.put("message", "Cập nhật phòng ban thành công");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi cập nhật phòng ban");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @DeleteMapping("/{maPhongBan}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String maPhongBan) {
        Map<String, Object> res = new HashMap<>();
        try {
            phongBanService.delete(maPhongBan);
            res.put("success", true);
            res.put("message", "Xóa phòng ban thành công");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi xóa phòng ban");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }
}
