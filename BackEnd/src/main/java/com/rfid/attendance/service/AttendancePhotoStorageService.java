package com.rfid.attendance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class AttendancePhotoStorageService {

    @Value("${attendance.upload-dir:uploads/attendance}")
    private String uploadDir;

    /**
     * Lưu ảnh và trả về relative path (để ghi vào `PhieuDiemDanh.pathFile`).
     */
    public String storePhoto(Long attendanceId, MultipartFile file) throws IOException {
        if (attendanceId == null) {
            throw new IllegalArgumentException("attendanceId không được để trống");
        }
        if (file == null || file.isEmpty()) {
            return null;
        }

        String ext = getFileExtension(file.getOriginalFilename(), file.getContentType());
        if (ext == null) {
            throw new IllegalArgumentException("Định dạng ảnh không hợp lệ");
        }

        String filename = UUID.randomUUID().toString() + "." + ext;
        Path dir = Paths.get(uploadDir, String.valueOf(attendanceId));
        Files.createDirectories(dir);

        Path target = dir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return Paths.get(uploadDir, String.valueOf(attendanceId), filename).toString().replace("\\", "/");
    }

    /**
     * Lưu ảnh từ bytes (để tái sử dụng 1 ảnh cho nhiều phiếu điểm danh) và trả về relative path.
     */
    public String storePhotoBytes(Long attendanceId, byte[] bytes, String originalFilename, String contentType) throws IOException {
        if (attendanceId == null) {
            throw new IllegalArgumentException("attendanceId không được để trống");
        }
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        String ext = getFileExtension(originalFilename, contentType);
        if (ext == null) {
            throw new IllegalArgumentException("Định dạng ảnh không hợp lệ");
        }

        String filename = UUID.randomUUID().toString() + "." + ext;
        Path dir = Paths.get(uploadDir, String.valueOf(attendanceId));
        Files.createDirectories(dir);

        Path target = dir.resolve(filename);
        Files.write(target, bytes);

        return Paths.get(uploadDir, String.valueOf(attendanceId), filename).toString().replace("\\", "/");
    }

    public byte[] loadPhoto(String pathFile) throws IOException {
        if (pathFile == null || pathFile.isBlank()) {
            return null;
        }
        Path filePath = Paths.get(pathFile).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            return null;
        }
        return Files.readAllBytes(filePath);
    }

    public String detectContentType(String pathFile) {
        if (pathFile == null || pathFile.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        try {
            Path filePath = Paths.get(pathFile).toAbsolutePath().normalize();
            String ct = Files.probeContentType(filePath);
            return (ct == null || ct.isBlank()) ? MediaType.APPLICATION_OCTET_STREAM_VALUE : ct;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    private String getFileExtension(String originalFilename, String contentType) {
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.contains("jpeg") || ct.contains("jpg")) return "jpg";
            if (ct.contains("png")) return "png";
            if (ct.contains("webp")) return "webp";
            if (ct.contains("gif")) return "gif";
        }
        if (originalFilename == null || !originalFilename.contains(".")) return null;
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (ext.equals("jpeg")) return "jpg";
        if (ext.equals("jpg") || ext.equals("png") || ext.equals("webp") || ext.equals("gif")) return ext;
        return null;
    }
}

