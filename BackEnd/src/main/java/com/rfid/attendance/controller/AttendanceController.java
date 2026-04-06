package com.rfid.attendance.controller;

import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rfid.attendance.entity.DocRfid;
import com.rfid.attendance.entity.PhieuDiemDanh;
import com.rfid.attendance.repository.DocRfidRepository;
import com.rfid.attendance.repository.ThietBiRepository;
import com.rfid.attendance.service.PythonFaceEncodingService;
import com.rfid.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {
    
    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private PythonFaceEncodingService pythonFaceEncodingService;
    
    @Autowired
    private DocRfidRepository docRfidRepository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private SocketIOServer socketIOServer;
    @Autowired
    private ThietBiRepository thietBiRepository;


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

    @GetMapping("/range")
    public ResponseEntity<List<PhieuDiemDanh>> getAttendanceByDateRange(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        try {
            List<PhieuDiemDanh> attendance = attendanceService.getAttendanceByDateRange(startDate, endDate);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<?> getAttendancePaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Integer ca,
            @RequestParam(required = false) String maSinhVien,
            @RequestParam(required = false) String phongHoc,
            @RequestParam(required = false) String tinhTrang,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String maPhongBan,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        try {
            Page<PhieuDiemDanh> result = attendanceService.getAttendanceByAdvancedFiltersPaged(
                    startDate, endDate, ca, maSinhVien, phongHoc, maPhongBan, tinhTrang, trangThai, sortDir, page, size
            );
            return ResponseEntity.ok(Map.of(
                    "content", result.getContent(),
                    "page", result.getNumber(),
                    "size", result.getSize(),
                    "totalElements", result.getTotalElements(),
                    "totalPages", result.getTotalPages()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi lấy dữ liệu điểm danh phân trang", "message", e.getMessage()));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportAttendanceExcel(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Integer ca,
            @RequestParam(required = false) String maSinhVien,
            @RequestParam(required = false) String phongHoc,
            @RequestParam(required = false) String tinhTrang,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String maPhongBan
    ) {
        try {
            byte[] fileBytes = attendanceService.exportAttendanceExcelByFilters(
                    startDate, endDate, ca, maSinhVien, phongHoc, maPhongBan, tinhTrang, trangThai
            );
            String filename = "BangChamCong_" +
                    (startDate != null ? startDate.format(DateTimeFormatter.ISO_DATE) : "") +
                    "_" +
                    (endDate != null ? endDate.format(DateTimeFormatter.ISO_DATE) : "") +
                    ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(fileBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi xuất file excel", "message", e.getMessage()));
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
    
    @PostMapping(value = "/rfid", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> processRfidAttendance(@RequestBody RfidRequest request) {
        try {
            String rfid = request.getRfid() != null ? request.getRfid().trim() : "";
            String maThietBi = request.getMaThietBi() != null ? request.getMaThietBi().trim() : null;
            if (rfid.isBlank()) {
                return ResponseEntity.badRequest().body(new RfidResponse("not_found", ""));
            }

            // Kiểm tra RFID tồn tại và có faceid không trước khi yêu cầu chụp ảnh
            var svOpt = attendanceService.getSinhVienRepository().findByRfid(rfid);
            if (svOpt.isEmpty()) {
                // Lưu RFID lạ vào bảng docrfid (tránh trùng do rfid unique)
                String viTri = null;
                try {
                    if (maThietBi != null && !maThietBi.isBlank()) {
                        var tbOpt = thietBiRepository.findById(maThietBi);
                        if (tbOpt.isPresent()) {
                            viTri = tbOpt.get().getPhongHoc();
                        }
                    }
                    if (!docRfidRepository.existsByRfid(rfid)) {
                        DocRfid doc = new DocRfid(rfid);
                        doc.setMaThietBi(maThietBi);
                        docRfidRepository.save(doc);
                    }
                } catch (Exception ex) {
                    // Không chặn flow nếu lỗi lưu (ví dụ trùng unique do race)
                    System.out.println("Không thể lưu docrfid: " + ex.getMessage());
                }

                // Publish event invalid-rfid để frontend thông báo
                String payloadToSend;
                try {
                    payloadToSend = objectMapper.writeValueAsString(Map.of(
                            "rfid", rfid,
                            "maThietBi", maThietBi,
                            "requestedAt", java.time.Instant.now().toString(),
                            "viTri", viTri
                    ));
                } catch (JsonProcessingException ex) {
                    payloadToSend = rfid; // fallback đơn giản
                }
                final String payloadFinal = payloadToSend;
                socketIOServer.getAllClients().forEach(client -> client.sendEvent("invalid-rfid", payloadFinal));
                return ResponseEntity.ok(new RfidResponse("not_found", ""));

            }
            var sv = svOpt.get();
            if (sv.getFaceid() == null || sv.getFaceid().isBlank()) {
                return ResponseEntity.ok(new RfidResponse("faceid_not_found", ""));
            }

            // Yêu cầu web mở camera chụp ảnh và so khớp (qua Socket.IO)
            // Frontend cần lắng nghe event "request-face-capture"
            String payloadToSend;
            try {
                payloadToSend = objectMapper.writeValueAsString(Map.of(
                        "rfid", rfid,
                        "maThietBi", maThietBi,
                        "requestedAt", java.time.Instant.now().toString()
                ));
            } catch (JsonProcessingException ex) {
                payloadToSend = rfid; // fallback đơn giản
            }
            final String payloadFinal = payloadToSend;
            System.out.println(payloadFinal.toString()+"hehe");
            socketIOServer.getAllClients().forEach(client -> client.sendEvent("request-face-capture", payloadFinal));

            // Trả về trạng thái yêu cầu xác thực khuôn mặt
            return ResponseEntity.ok(new RfidResponse("Face_required", ""));
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            return ResponseEntity.badRequest().body(new RfidResponse("not_found", ""));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Multipart version để ESP32 đẩy kèm ảnh.
     * - Nếu có ảnh: check cả RFID + face (so khớp với SinhVien.faceid) rồi mới chấm công.
     * - Nếu không có ảnh: chỉ chấm công theo RFID như cũ.
     */
    @PostMapping(value = "/rfid", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processRfidAttendanceWithOptionalFace(
            @RequestParam("rfid") String rfid,
            @RequestParam(required = false) String maThietBi,
            @RequestParam(required = false, value = "image") MultipartFile image,
            @RequestParam(required = false, value = "file") MultipartFile file,
            @RequestAttribute(value = "deviceId", required = false) String deviceId
    ) {
        System.out.println(rfid+"input");
        try {
            String rfidTrim = rfid != null ? rfid.trim() : "";
            if (rfidTrim.isEmpty()) {
                return ResponseEntity.badRequest().body(new RfidResponse("not_found", ""));
            }

            String deviceIdFinal = (maThietBi != null && !maThietBi.isBlank()) ? maThietBi.trim() : deviceId;

            MultipartFile inputImage = (image != null && !image.isEmpty()) ? image : file;

            // Nếu ESP32 gửi ảnh => verify face + rfid.
            if (inputImage != null && !inputImage.isEmpty()) {
                System.out.println(rfidTrim);
                var svOpt = attendanceService.getSinhVienRepository().findByRfid(rfidTrim);

                System.out.println(svOpt+"kết quả");
                if (svOpt.isEmpty()) {
                    return ResponseEntity.ok(new RfidResponse("not_found rfid", ""));
                }

                var sv = svOpt.get();
                String faceid = sv.getFaceid();
                if (faceid == null || faceid.isBlank()) {
                    return ResponseEntity.ok(new RfidResponse("faceid_not_found", ""));
                }

                var compare = pythonFaceEncodingService.compareFace(inputImage, faceid);
                if (compare == null || !compare.isMatched()) {
                    String payloadToSend = rfidTrim;
                    try {
                        payloadToSend = objectMapper.writeValueAsString(rfidTrim);
                    } catch (JsonProcessingException ex) {
                        // fallback: gửi thẳng chuỗi
                    }
                    final String payloadFinal = payloadToSend;
                    socketIOServer.getAllClients().forEach(client -> client.sendEvent("invalid-face", payloadFinal));
                    return ResponseEntity.ok(new RfidResponse("face_mismatch", ""));
                }
            }

            // Nếu verified (hoặc không gửi ảnh) => chấm công theo logic hiện có của RFID.
            PhieuDiemDanh attendance = attendanceService.processRfidAttendanceWithDeviceAndPhoto(rfidTrim, deviceIdFinal, inputImage);
            if (attendance.getRfid() == null) {
                return ResponseEntity.ok(new RfidResponse("not_found", ""));
            }

            attendance.setTenSinhVien(removeAccent(attendance.getTenSinhVien()));
            return ResponseEntity.ok(new RfidResponse("found", attendance.getTenSinhVien()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new RfidResponse("not_found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processFaceAttendance(
            @RequestParam("uid") String uid,
            @RequestParam("image") MultipartFile image,
            @RequestParam(required = false) String maThietBi,
            @RequestAttribute(value = "deviceId", required = false) String deviceId
    ) {
        try {
            String uidTrim = uid != null ? uid.trim() : "";
            if (uidTrim.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Thiếu UID"));
            }
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Thiếu file ảnh khuôn mặt"));
            }

            String deviceIdFinal = (maThietBi != null && !maThietBi.isBlank()) ? maThietBi.trim() : deviceId;

            // Step 1: kiểm tra UID có tồn tại hay không để tránh tốn tài nguyên AI
            var svOpt = attendanceService.getSinhVienRepository().findByRfid(uidTrim);
            if (svOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy nhân viên cho UID"));
            }

            var sv = svOpt.get();
            String faceid = sv.getFaceid();
            if (faceid == null || faceid.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Chưa có faceid cho mã sinh viên"));
            }

            // So sánh ảnh với template embedding (faceid) bằng Python /compare
            var compare = pythonFaceEncodingService.compareFace(image, faceid);
            if (compare == null || !compare.isMatched()) {
                // Inform UI (nếu bạn muốn hiển thị toast khi sai mặt)
                String payloadToSend = uidTrim;
                try {
                    payloadToSend = objectMapper.writeValueAsString(uidTrim);
                } catch (JsonProcessingException ex) {
                    // fallback: gửi thẳng chuỗi
                }
                final String payloadFinal = payloadToSend;
                socketIOServer.getAllClients().forEach(client -> client.sendEvent("invalid-face", payloadFinal));
                return ResponseEntity.ok(new FaceResponse("failed", ""));
            }

            // Nếu success thì chấm công theo logic hiện có của RFID (re-use time/shift splitting)
            PhieuDiemDanh attendance = attendanceService.processRfidAttendanceWithDevice(uidTrim, deviceIdFinal);
            if (attendance.getRfid() == null) {
                return ResponseEntity.ok(new FaceResponse("not_found", ""));
            }

            // Lưu ảnh chụp điểm danh cho phiếu vừa tạo/cập nhật
            attendance = attendanceService.attachAttendancePhoto(attendance, image);

            attendance.setTenSinhVien(removeAccent(attendance.getTenSinhVien()));
            return ResponseEntity.ok(new FaceResponse("success", attendance.getTenSinhVien()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new FaceResponse("not_found", ""));
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
    
    /**
     * API đồng bộ dữ liệu từ bảng sinhvien sang phieudiemdanh dựa trên mã sinh viên
     * Cập nhật tensinhvien và rfid trong phieudiemdanh từ dữ liệu trong sinhvien
     * 
     * @return ResponseEntity chứa thống kê kết quả đồng bộ
     */
    @PostMapping("/sync-student-info")
    public ResponseEntity<?> syncStudentInfoFromMaSinhVien() {
        try {
            System.out.println("=== API ĐỒNG BỘ DỮ LIỆU SINH VIÊN (THEO MÃ SINH VIÊN) ===");
            Map<String, Object> result = attendanceService.syncStudentInfoFromMaSinhVien();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Lỗi khi đồng bộ dữ liệu: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Lỗi khi đồng bộ dữ liệu",
                    "message", e.getMessage()
                ));
        }
    }
    
    /**
     * API lấy danh sách phiếu điểm danh theo lớp học phần
     * Lấy tất cả ca học của lớp học phần, sau đó lấy các phiếu điểm danh có ca học và ngày học 
     * mà lớp học phần diễn ra và so sánh với danh sách sinh viên của lớp học phần đó
     * 
     * @param maLopHocPhan Mã lớp học phần
     * @return Danh sách phiếu điểm danh của sinh viên trong lớp học phần
     */
    @GetMapping("/by-lophocphan/{maLopHocPhan}")
    public ResponseEntity<?> getAttendanceByLopHocPhan(@PathVariable String maLopHocPhan) {
        try {
            List<PhieuDiemDanh> attendance = attendanceService.getAttendanceByLopHocPhan(maLopHocPhan);
            return ResponseEntity.ok(attendance);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy phiếu điểm danh theo lớp học phần: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Lỗi khi lấy phiếu điểm danh theo lớp học phần",
                    "message", e.getMessage()
                ));
        }
    }

    /**
     * Xem chi tiết phiếu điểm danh, bao gồm ảnh chụp (nếu có) dưới dạng dataUrl.
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getAttendanceDetail(@PathVariable Long id) {
        try {
            System.out.println(attendanceService.getAttendanceDetail(id));
            return ResponseEntity.ok(attendanceService.getAttendanceDetail(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi khi lấy chi tiết phiếu điểm danh"));
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

    public static class FaceResponse {
        private String status;
        private String name;
        public FaceResponse(String status, String name) {
            this.status = status;
            this.name = name;
        }
        public String getStatus() { return status; }
        public String getName() { return name; }
    }
    public static String removeAccent(String input) {
        if (input == null) {
            return "";
        }
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
