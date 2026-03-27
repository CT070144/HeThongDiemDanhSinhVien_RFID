package com.rfid.attendance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Service
public class PythonFaceEncodingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${face.python.base-url:http://localhost:5000}")
    private String baseUrl;

    @Value("${face.python.encode-path:/encode}")
    private String encodePath;

    @Value("${face.python.compare-path:/compare}")
    private String comparePath;

    public PythonFaceEncodingService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * POST {baseUrl}/encode
     * Body multipart/form-data:
     * - files: ảnh khuôn mặt mẫu (upload nhiều ảnh)
     *
     * Response:
     * { "encodings": [[...], [...]] }
     */
    public String encodeFaces(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Thiếu danh sách ảnh để encode");
        }

        // Lọc bỏ file rỗng
        List<MultipartFile> validFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();
        if (validFiles.isEmpty()) {
            throw new IllegalArgumentException("Không có ảnh hợp lệ để encode");
        }

        String url = buildUrl(encodePath);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            for (MultipartFile file : validFiles) {
                // Python AI mới dùng trường "files" (list) cho encode nhiều ảnh.
                body.add("files", toByteArrayResource(file));
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<EncodeApiResponse> resp =
                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, EncodeApiResponse.class);

            EncodeApiResponse payload = resp.getBody();
            if (payload == null || payload.getEncodings() == null) {
                throw new RuntimeException("Python trả về encoding rỗng");
            }

            // Lưu dạng JSON array string (list[list[float]]) để gửi lại cho /compare.
            return objectMapper.writeValueAsString(payload.getEncodings());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Không thể parse encoding từ Python", e);
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc dữ liệu file ảnh để encode", e);
        }
    }

    /**
     * Wrapper cho trường hợp chỉ encode 1 ảnh.
     */
    public String encodeFace(MultipartFile file) {
        return encodeFaces(file == null ? List.of() : List.of(file));
    }

    /**
     * POST {baseUrl}/compare
     * Body multipart/form-data:
     * - file: ảnh input (ESP32 đẩy lên)
     * - encodings: Text dạng "[[0.1,0.2,...],[...]]"
     */
    public CompareResult compareFace(MultipartFile inputImage, String encodingJson) {
        if (inputImage == null || inputImage.isEmpty()) {
            throw new IllegalArgumentException("Thiếu file ảnh để compare");
        }
        if (encodingJson == null || encodingJson.isBlank()) {
            throw new IllegalArgumentException("Thiếu encodings (faceid) để compare");
        }

        String url = buildUrl(comparePath);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = toByteArrayResource(inputImage);

            body.add("file", resource);
            body.add("encodings", encodingJson);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<CompareApiResponse> resp =
                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, CompareApiResponse.class);

            CompareApiResponse payload = resp.getBody();
            if (payload == null) {
                return new CompareResult(false, null, null);
            }

            boolean matched = payload.getMatch() != null && payload.getMatch();
            return new CompareResult(matched, null, payload.getDistance());
        } catch (Exception e) {
            // Nếu Python lỗi hoặc không parse được: coi như không khớp (an toàn cho chấm công).
            return new CompareResult(false, null, null);
        }
    }

    private ByteArrayResource toByteArrayResource(MultipartFile file) throws IOException {
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                String original = file.getOriginalFilename();
                return original != null ? original : "image.jpg";
            }
        };
    }

    private String buildUrl(String path) {
        String trimmedBase = baseUrl != null ? baseUrl.trim() : "";
        String trimmedPath = path != null ? path.trim() : "";
        if (!trimmedPath.startsWith("/")) {
            trimmedPath = "/" + trimmedPath;
        }
        if (trimmedBase.endsWith("/")) {
            trimmedBase = trimmedBase.substring(0, trimmedBase.length() - 1);
        }
        return trimmedBase + trimmedPath;
    }

    public static class CompareResult {
        private final boolean matched;
        private final String status;
        private final Double distance;

        public CompareResult(boolean matched, String status, Double distance) {
            this.matched = matched;
            this.status = status;
            this.distance = distance;
        }

        public boolean isMatched() {
            return matched;
        }

        public String getStatus() {
            return status;
        }

        public Double getDistance() {
            return distance;
        }
    }

    public static class EncodeApiResponse {
        private List<List<Double>> encodings;

        public List<List<Double>> getEncodings() {
            return encodings;
        }

        public void setEncodings(List<List<Double>> encodings) {
            this.encodings = encodings;
        }
    }

    /**
     * Format response của /compare có thể thay đổi theo Python.
     * Các trường bên dưới là "tùy chọn" để parse linh hoạt.
     */
    public static class CompareApiResponse {
        private Boolean match;
        private Double distance;
        private String error;

        public Boolean getMatch() {
            return match;
        }

        public void setMatch(Boolean match) {
            this.match = match;
        }

        public Double getDistance() {
            return distance;
        }

        public void setDistance(Double distance) {
            this.distance = distance;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}

