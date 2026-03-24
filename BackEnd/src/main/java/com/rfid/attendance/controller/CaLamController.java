package com.rfid.attendance.controller;

import com.rfid.attendance.dto.CaLamRequest;
import com.rfid.attendance.entity.CaLam;
import com.rfid.attendance.service.CaLamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/calam")
@CrossOrigin(origins = "*")
public class CaLamController {

    private final CaLamService caLamService;

    public CaLamController(CaLamService caLamService) {
        this.caLamService = caLamService;
    }

    @GetMapping
    public ResponseEntity<List<CaLam>> getAll() {
        return ResponseEntity.ok(caLamService.getAll());
    }

    @GetMapping("/{maCa}")
    public ResponseEntity<CaLam> getByMaCa(@PathVariable Integer maCa) {
        Optional<CaLam> caLam = caLamService.getByMaCa(maCa);
        return caLam.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody @Valid CaLamRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            CaLam caLam = new CaLam();
            caLam.setMaCa(request.getMaCa());
            caLam.setTenCa(request.getTenCa());
            caLam.setGioBatDau(request.getGioBatDau());
            caLam.setGioKetThuc(request.getGioKetThuc());
            caLam.setChoPhepTrePhut(request.getChoPhepTrePhut());

            CaLam created = caLamService.create(caLam);
            res.put("success", true);
            res.put("data", created);
            res.put("message", "Tạo ca làm thành công");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi tạo ca làm");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @PutMapping("/{maCa}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Integer maCa,
            @RequestBody @Valid CaLamRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            CaLam caLam = new CaLam();
            caLam.setMaCa(maCa);
            caLam.setTenCa(request.getTenCa());
            caLam.setGioBatDau(request.getGioBatDau());
            caLam.setGioKetThuc(request.getGioKetThuc());
            caLam.setChoPhepTrePhut(request.getChoPhepTrePhut());

            CaLam updated = caLamService.update(maCa, caLam);
            res.put("success", true);
            res.put("data", updated);
            res.put("message", "Cập nhật ca làm thành công");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi cập nhật ca làm");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @DeleteMapping("/{maCa}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer maCa) {
        Map<String, Object> res = new HashMap<>();
        try {
            caLamService.delete(maCa);
            res.put("success", true);
            res.put("message", "Xóa ca làm thành công");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi xóa ca làm");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }
}

