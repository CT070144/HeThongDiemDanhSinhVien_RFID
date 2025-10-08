package com.rfid.attendance.controller;

import com.rfid.attendance.entity.ThietBi;
import com.rfid.attendance.repository.ThietBiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thietbi")
@CrossOrigin(origins = "*")
public class ThietBiController {

    @Autowired
    private ThietBiRepository thietBiRepository;

    @GetMapping
    public ResponseEntity<List<ThietBi>> getAll() {
        return ResponseEntity.ok(thietBiRepository.findAll());
    }

    @GetMapping("/{maThietBi}")
    public ResponseEntity<ThietBi> getOne(@PathVariable String maThietBi) {
        return thietBiRepository.findById(maThietBi)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ThietBi tb) {
        if (thietBiRepository.existsById(tb.getMaThietBi())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Mã thiết bị đã tồn tại");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(thietBiRepository.save(tb));
    }

    @PutMapping("/{maThietBi}")
    public ResponseEntity<?> update(@PathVariable String maThietBi, @RequestBody ThietBi tb) {
        return thietBiRepository.findById(maThietBi)
                .map(existing -> {
                    existing.setPhongHoc(tb.getPhongHoc());
                    return ResponseEntity.ok(thietBiRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{maThietBi}")
    public ResponseEntity<?> delete(@PathVariable String maThietBi) {
        if (!thietBiRepository.existsById(maThietBi)) {
            return ResponseEntity.notFound().build();
        }
        thietBiRepository.deleteById(maThietBi);
        return ResponseEntity.noContent().build();
    }
}


