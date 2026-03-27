package com.rfid.attendance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;

@Service
public class SinhVienAvatarStorageService {

    @Value("${avatar.upload-dir:uploads/avatars}")
    private String uploadDir;

    /**
     * Lưu avatar vào thư mục upload và trả về relativePath (để lưu vào DB).
     * relativePath ví dụ: uploads/avatars/CT070201/uuid.jpg
     */
    public String storeAvatar(String maSinhVien, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String safeMaSinhVien = maSinhVien != null ? maSinhVien.trim() : "unknown";
        if (safeMaSinhVien.isEmpty()) safeMaSinhVien = "unknown";

        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(originalFilename, file.getContentType());
        if (ext == null) {
            throw new IllegalArgumentException("Định dạng ảnh không hợp lệ");
        }

        // Tạo tên file độc nhất để tránh ghi đè ngoài ý muốn.
        String filename = UUID.randomUUID().toString() + "." + ext;
        Path dir = Paths.get(uploadDir, safeMaSinhVien);
        Files.createDirectories(dir);

        Path target = dir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        // Lưu relative path (chuẩn hoá cho nhiều môi trường).
        String relativePath = Paths.get(uploadDir, safeMaSinhVien, filename).toString().replace("\\", "/");
        return relativePath;
    }

    public byte[] loadAvatar(String pathAvatar) throws IOException {
        if (pathAvatar == null || pathAvatar.isBlank()) {
            return null;
        }
        Path filePath = Paths.get(pathAvatar).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            return null;
        }
        return Files.readAllBytes(filePath);
    }

    public String detectContentType(String pathAvatar) {
        if (pathAvatar == null || pathAvatar.isBlank()) return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try {
            Path filePath = Paths.get(pathAvatar).toAbsolutePath().normalize();
            String ct = Files.probeContentType(filePath);
            if (ct == null || ct.isBlank()) return MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return ct;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    public boolean deleteAvatar(String pathAvatar) throws IOException {
        if (pathAvatar == null || pathAvatar.isBlank()) return false;
        Path filePath = Paths.get(pathAvatar).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) return false;
        return Files.deleteIfExists(filePath);
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
        if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp") || ext.equals("gif")) {
            if (ext.equals("jpeg")) return "jpg";
            return ext;
        }
        return null;
    }
}

