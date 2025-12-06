package com.rfid.attendance.controller;

import com.rfid.attendance.dto.RoomDetailDTO;
import com.rfid.attendance.dto.RoomScheduleDTO;
import com.rfid.attendance.dto.RoomStatusDTO;
import com.rfid.attendance.entity.PhongHoc;
import com.rfid.attendance.service.PhongHocService;
import com.rfid.attendance.service.RoomStatusService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/phonghoc")
@CrossOrigin(origins = "*")
public class PhongHocController {

    private final PhongHocService phongHocService;
    private final RoomStatusService roomStatusService;

    public PhongHocController(PhongHocService phongHocService, RoomStatusService roomStatusService) {
        this.phongHocService = phongHocService;
        this.roomStatusService = roomStatusService;
    }

    @GetMapping
    public ResponseEntity<List<PhongHoc>> getAll(@RequestParam(required = false) String keyword) {
        try {
            if (keyword != null && !keyword.isBlank()) {
                return ResponseEntity.ok(phongHocService.search(keyword));
            }
            return ResponseEntity.ok(phongHocService.getAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<Map<String, Object>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        try {
            Page<PhongHoc> result = phongHocService.getPaged(page, size, keyword);
            Map<String, Object> body = new HashMap<>();
            body.put("content", result.getContent());
            body.put("page", result.getNumber());
            body.put("size", result.getSize());
            body.put("totalElements", result.getTotalElements());
            body.put("totalPages", result.getTotalPages());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{maPhong}")
    public ResponseEntity<PhongHoc> getById(@PathVariable String maPhong) {
        try {
            Optional<PhongHoc> phongHoc = phongHocService.getById(maPhong);
            return phongHoc.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody PhongHoc phongHoc) {
        Map<String, Object> res = new HashMap<>();
        try {
            PhongHoc created = phongHocService.create(phongHoc);
            res.put("success", true);
            res.put("data", created);
            res.put("message", "Tạo phòng học thành công");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi tạo phòng học");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @PutMapping("/{maPhong}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String maPhong, @RequestBody PhongHoc phongHoc) {
        Map<String, Object> res = new HashMap<>();
        try {
            PhongHoc updated = phongHocService.update(maPhong, phongHoc);
            res.put("success", true);
            res.put("data", updated);
            res.put("message", "Cập nhật phòng học thành công");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi cập nhật phòng học");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @DeleteMapping("/{maPhong}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String maPhong) {
        Map<String, Object> res = new HashMap<>();
        try {
            phongHocService.delete(maPhong);
            res.put("success", true);
            res.put("message", "Xóa phòng học thành công");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi xóa phòng học");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    /**
     * Lấy danh sách phòng học với trạng thái (trống/đang sử dụng)
     */
    @GetMapping("/status")
    public ResponseEntity<List<RoomStatusDTO>> getRoomsWithStatus(
            @RequestParam(required = false) String toaNha,
            @RequestParam(required = false) Integer tang,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) Integer ca) {
        try {
            List<RoomStatusDTO> rooms = roomStatusService.getRoomsWithStatus(toaNha, tang, ngay, ca);
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy chi tiết phòng học
     */
    @GetMapping("/{maPhong}/detail")
    public ResponseEntity<RoomDetailDTO> getRoomDetail(
            @PathVariable String maPhong,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) Integer ca) {
        try {
            RoomDetailDTO detail = roomStatusService.getRoomDetail(maPhong, ngay, ca);
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy lịch sử dụng phòng học theo tầng và ngày
     */
    @GetMapping("/schedule")
    public ResponseEntity<List<RoomScheduleDTO>> getRoomSchedule(
            @RequestParam(required = false) String toaNha,
            @RequestParam(required = false) Integer tang,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        try {
            List<RoomScheduleDTO> schedule = roomStatusService.getRoomScheduleByFloor(toaNha, tang, ngay);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}


