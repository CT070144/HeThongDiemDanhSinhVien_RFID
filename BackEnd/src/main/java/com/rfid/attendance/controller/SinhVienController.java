package com.rfid.attendance.controller;

import com.rfid.attendance.entity.DocRfid;
import com.rfid.attendance.entity.SinhVien;
import com.rfid.attendance.repository.DocRfidRepository;
import com.rfid.attendance.repository.SinhVienLopHocPhanRepository;
import com.rfid.attendance.service.SinhVienAvatarStorageService;
import com.rfid.attendance.service.SinhVienService;
import com.rfid.attendance.service.PythonFaceEncodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/sinhvien")
@CrossOrigin(origins = "*")
public class SinhVienController {
    
    @Autowired
    private SinhVienService sinhVienService;
    @Autowired
    private PythonFaceEncodingService pythonFaceEncodingService;

    @Autowired
    private SinhVienAvatarStorageService avatarStorageService;
    @Autowired
    private DocRfidRepository docRfidRepository;
    @Autowired
    private SinhVienLopHocPhanRepository sinhVienLopHocPhanRepository;
    
    @GetMapping
    public ResponseEntity<List<SinhVien>> getAllSinhVien() {
        try {
            List<SinhVien> sinhViens = sinhVienService.getAllSinhVien();
            return ResponseEntity.ok(sinhViens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{maSinhVien}")
    public ResponseEntity<SinhVien> getSinhVienByMaSinhVien(@PathVariable String maSinhVien) {
        try {
            Optional<SinhVien> sinhVien = sinhVienService.getSinhVienByMaSinhVien(maSinhVien);
            return sinhVien.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/rfid/{rfid}")
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
    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

    /**
     * Create sinh viên (multipart) để đính kèm ảnh => encode faceid.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createSinhVienMultipart(
            @RequestParam String maSinhVien,
            @RequestParam String rfid,
            @RequestParam String tenSinhVien,
            @RequestParam(required = false) String maPhongBan,
            @RequestParam(required = false, value = "files") List<MultipartFile> files,
            @RequestParam(required = false, value = "file") MultipartFile file,
            @RequestParam(required = false, value = "image") MultipartFile image
    ) {
        try {
            maSinhVien = maSinhVien != null ? maSinhVien.trim() : "";
            rfid = rfid != null ? rfid.trim() : "";
            tenSinhVien = tenSinhVien != null ? tenSinhVien.trim() : "";

            if (maSinhVien.isEmpty()) return ResponseEntity.badRequest().body("Mã sinh viên không được để trống");
            if (rfid.isEmpty()) return ResponseEntity.badRequest().body("RFID không được để trống");
            if (tenSinhVien.isEmpty()) return ResponseEntity.badRequest().body("Tên sinh viên không được để trống");

            // Gom tất cả ảnh mẫu upload vào 1 danh sách (ưu tiên "files" nếu có).
            List<MultipartFile> toEncodeFiles = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                toEncodeFiles.addAll(files);
            } else if (file != null && !file.isEmpty()) {
                toEncodeFiles.add(file);
            } else if (image != null && !image.isEmpty()) {
                toEncodeFiles.add(image);
            }

            MultipartFile primaryAvatarFile = !toEncodeFiles.isEmpty() ? toEncodeFiles.get(0) : null;
            String faceid = null;
            if (primaryAvatarFile != null && !toEncodeFiles.isEmpty()) {
                faceid = pythonFaceEncodingService.encodeFaces(toEncodeFiles);
            }

            String pathAvatar = null;
            // Lưu avatar chỉ từ ảnh đầu tiên.
            if (primaryAvatarFile != null && !primaryAvatarFile.isEmpty()) {
                pathAvatar = avatarStorageService.storeAvatar(maSinhVien, primaryAvatarFile);
            }

            SinhVien sinhVien = new SinhVien();
            sinhVien.setMaSinhVien(maSinhVien);
            sinhVien.setRfid(rfid);
            sinhVien.setTenSinhVien(tenSinhVien);
            sinhVien.setMaPhongBan(maPhongBan);
            sinhVien.setFaceid(faceid);
            sinhVien.setPathAvatar(pathAvatar);

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
    
    @PutMapping(value = "/{maSinhVien}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateSinhVien(@PathVariable String maSinhVien, @Valid @RequestBody SinhVien sinhVienDetails) {
        try {

            // Lấy thông tin sinh viên cũ trước khi cập nhật
            Optional<SinhVien> oldSinhVienOpt = sinhVienService.getSinhVienByMaSinhVien(maSinhVien);
            String oldRfid = oldSinhVienOpt.map(SinhVien::getRfid).orElse(null);

            SinhVien updatedSinhVien = sinhVienService.updateSinhVien(maSinhVien, sinhVienDetails);
            docRfidRepository.findByRfid(sinhVienDetails.getRfid()).ifPresent(
                    docRfid1 -> {
                        docRfid1.setProcessed(true);
                        docRfidRepository.save(docRfid1);
                    }
            );


            // Cập nhật thông tin trong bảng docrfid
            if (oldRfid != null && !oldRfid.equals(updatedSinhVien.getRfid())) {
                // RFID đã thay đổi - cập nhật bản ghi docrfid với RFID cũ
                docRfidRepository.findByRfid(oldRfid).ifPresent(doc -> {
                    doc.setRfid(updatedSinhVien.getRfid());
                    doc.setMaSinhVien(updatedSinhVien.getMaSinhVien());
                    doc.setTenSinhVien(updatedSinhVien.getTenSinhVien());
                    doc.setProcessed(true);
                    docRfidRepository.save(doc);
                });
            } else {
                // RFID không thay đổi - chỉ cập nhật thông tin sinh viên
                docRfidRepository.findByRfid(updatedSinhVien.getRfid()).ifPresent(doc -> {
                    doc.setMaSinhVien(updatedSinhVien.getMaSinhVien());
                    doc.setTenSinhVien(updatedSinhVien.getTenSinhVien());
                    doc.setProcessed(true);
                    docRfidRepository.save(doc);
                });
            }
            
            return ResponseEntity.ok(updatedSinhVien);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update sinh viên (multipart) để đính kèm ảnh => encode faceid.
     * Nếu không gửi ảnh thì giữ faceid cũ.
     */
    @PutMapping(value = "/{maSinhVien}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSinhVienMultipart(
            @PathVariable String maSinhVien,
            @RequestParam String rfid,
            @RequestParam String tenSinhVien,
            @RequestParam(required = false) String maPhongBan,
            @RequestParam(required = false, value = "files") List<MultipartFile> files,
            @RequestParam(required = false, value = "file") MultipartFile file,
            @RequestParam(required = false, value = "image") MultipartFile image
    ) {
        try {
            maSinhVien = maSinhVien != null ? maSinhVien.trim() : "";
            rfid = rfid != null ? rfid.trim() : "";
            tenSinhVien = tenSinhVien != null ? tenSinhVien.trim() : "";

            if (maSinhVien.isEmpty()) return ResponseEntity.badRequest().body("Mã sinh viên không hợp lệ");
            if (rfid.isEmpty()) return ResponseEntity.badRequest().body("RFID không được để trống");
            if (tenSinhVien.isEmpty()) return ResponseEntity.badRequest().body("Tên sinh viên không được để trống");

            // Lấy thông tin sinh viên cũ trước khi cập nhật
            Optional<SinhVien> oldSinhVienOpt = sinhVienService.getSinhVienByMaSinhVien(maSinhVien);
            String oldRfid = oldSinhVienOpt.map(SinhVien::getRfid).orElse(null);

            // Gom tất cả ảnh mẫu upload vào 1 danh sách (ưu tiên "files" nếu có).
            List<MultipartFile> toEncodeFiles = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                toEncodeFiles.addAll(files);
            } else if (file != null && !file.isEmpty()) {
                toEncodeFiles.add(file);
            } else if (image != null && !image.isEmpty()) {
                toEncodeFiles.add(image);
            }

            MultipartFile primaryAvatarFile = !toEncodeFiles.isEmpty() ? toEncodeFiles.get(0) : null;

            String faceid = null;
            if (primaryAvatarFile != null && !toEncodeFiles.isEmpty()) {
                faceid = pythonFaceEncodingService.encodeFaces(toEncodeFiles);
            }

            String pathAvatar = null;
            if (primaryAvatarFile != null && !primaryAvatarFile.isEmpty()) {
                pathAvatar = avatarStorageService.storeAvatar(maSinhVien, primaryAvatarFile);
            }

            SinhVien sinhVienDetails = new SinhVien();
            sinhVienDetails.setRfid(rfid);
            sinhVienDetails.setTenSinhVien(tenSinhVien);
            sinhVienDetails.setMaPhongBan(maPhongBan);
            sinhVienDetails.setFaceid(faceid); // có thể null => service sẽ giữ faceid cũ
            sinhVienDetails.setPathAvatar(pathAvatar); // có thể null => service sẽ giữ avatar cũ

            SinhVien updatedSinhVien = sinhVienService.updateSinhVien(maSinhVien, sinhVienDetails);
            docRfidRepository.findByRfid(sinhVienDetails.getRfid()).ifPresent(
                    docRfid1 -> {
                        docRfid1.setProcessed(true);
                        docRfidRepository.save(docRfid1);
                    }
            );

            // Cập nhật thông tin trong bảng docrfid
            if (oldRfid != null && !oldRfid.equals(updatedSinhVien.getRfid())) {
                // RFID đã thay đổi - cập nhật bản ghi docrfid với RFID cũ
                docRfidRepository.findByRfid(oldRfid).ifPresent(doc -> {
                    doc.setRfid(updatedSinhVien.getRfid());
                    doc.setMaSinhVien(updatedSinhVien.getMaSinhVien());
                    doc.setTenSinhVien(updatedSinhVien.getTenSinhVien());
                    doc.setProcessed(true);
                    docRfidRepository.save(doc);
                });
            } else {
                // RFID không thay đổi - chỉ cập nhật thông tin sinh viên
                docRfidRepository.findByRfid(updatedSinhVien.getRfid()).ifPresent(doc -> {
                    doc.setMaSinhVien(updatedSinhVien.getMaSinhVien());
                    doc.setTenSinhVien(updatedSinhVien.getTenSinhVien());
                    doc.setProcessed(true);
                    docRfidRepository.save(doc);
                });
            }

            return ResponseEntity.ok(updatedSinhVien);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{maSinhVien}/avatar")
    public ResponseEntity<?> getAvatar(@PathVariable String maSinhVien) {
        try {
            Optional<SinhVien> svOpt = sinhVienService.getSinhVienByMaSinhVien(maSinhVien);
            if (svOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Không tìm thấy sinh viên"));
            }

            SinhVien sv = svOpt.get();
            String pathAvatar = sv.getPathAvatar();
            if (pathAvatar == null || pathAvatar.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sinh viên chưa có avatar"));
            }

            byte[] bytes = avatarStorageService.loadAvatar(pathAvatar);
            if (bytes == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Không tìm thấy file avatar"));
            }

            String contentType = avatarStorageService.detectContentType(pathAvatar);
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Không thể tải ảnh avatar", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi tải ảnh avatar", "message", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{maSinhVien}")
    public ResponseEntity<?> deleteSinhVien(@PathVariable String maSinhVien) {
        try {
            // Kiểm tra xem sinh viên có đang tham gia lớp học phần nào không
            var lopHocPhans = sinhVienLopHocPhanRepository.findByMaSinhVien(maSinhVien);
            if (!lopHocPhans.isEmpty()) {
                StringBuilder lopNames = new StringBuilder();
                for (int i = 0; i < lopHocPhans.size(); i++) {
                    if (i > 0) lopNames.append(", ");
                    lopNames.append(lopHocPhans.get(i).getMaLopHocPhan());
                }
                return ResponseEntity.badRequest().body(
                    "Không thể xóa sinh viên vì đang tham gia các lớp học phần: " + lopNames.toString() + 
                    ". Vui lòng xóa sinh viên khỏi các lớp học phần trước khi xóa sinh viên."
                );
            }
            
            sinhVienService.deleteSinhVien(maSinhVien);
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
    
    @PostMapping("/bulk-update-rfid")
    public ResponseEntity<?> bulkUpdateRfid(@RequestBody List<SinhVien> sinhVienList) {
        try {
            System.out.println("=== BULK UPDATE RFID ===");
            System.out.println("Số lượng sinh viên cần xử lý: " + sinhVienList.size());
            
            var result = sinhVienService.bulkUpdateRfid(sinhVienList);
            
            System.out.println("Kết quả xử lý:");
            System.out.println("- Tổng số: " + result.get("totalProcessed"));
            System.out.println("- Thành công: " + result.get("successCount"));
            System.out.println("- Thất bại: " + result.get("failureCount"));
            
            if ((Integer) result.get("failureCount") > 0) {
                System.out.println("Chi tiết lỗi: " + result.get("errors"));
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.out.println("Lỗi khi cập nhật hàng loạt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật hàng loạt: " + e.getMessage());
        }
    }
}
