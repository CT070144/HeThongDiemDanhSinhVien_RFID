package com.rfid.attendance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

@Service
public class PythonFaceRecognitionService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${face.python.base-url:http://localhost:5000}")
    private String baseUrl;

    @Value("${face.python.compare-path:/compare}")
    private String comparePath;

    public PythonFaceRecognitionService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Gọi Python: gửi ảnh + embedding template để so khớp 1:1.
     * Python sẽ trả về {"match": true|false, "distance": x, "threshold": y}.
     */
    public FaceRecognitionResult recognizeWithTemplate(
            MultipartFile image,
            String templateEmbeddingJson,
            double tolerance,
            String employeeName
    ) {
        if (image == null || image.isEmpty()) {
            return new FaceRecognitionResult("not_found", null);
        }
        if (templateEmbeddingJson == null || templateEmbeddingJson.isBlank()) {
            return new FaceRecognitionResult("not_found", null);
        }

        String url = buildUrl();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            ByteArrayResource resource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    String original = image.getOriginalFilename();
                    return original != null ? original : "image.jpg";
                }
            };

            body.add("file", resource);
            // Python service mới nhận field "encoding" (1 vector dạng JSON array)
            body.add("encoding", templateEmbeddingJson);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<FaceRecognitionApiResponse> resp =
                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, FaceRecognitionApiResponse.class);

            FaceRecognitionApiResponse payload = resp.getBody();
            if (payload == null) {
                return new FaceRecognitionResult("not_found", null);
            }

            boolean matched = payload.getMatch() != null && payload.getMatch();
            return new FaceRecognitionResult(matched ? "success" : "failed", payload.getDistance());
        } catch (IOException e) {
            return new FaceRecognitionResult("not_found", null);
        } catch (Exception e) {
            return new FaceRecognitionResult("not_found", null);
        }
    }

    private String buildUrl() {
        String trimmedBase = baseUrl != null ? baseUrl.trim() : "";
        String trimmedPath = comparePath != null ? comparePath.trim() : "";
        if (!trimmedPath.startsWith("/")) {
            trimmedPath = "/" + trimmedPath;
        }
        if (trimmedBase.endsWith("/")) {
            trimmedBase = trimmedBase.substring(0, trimmedBase.length() - 1);
        }
        return trimmedBase + trimmedPath;
    }

    public static class FaceRecognitionResult {
        private final String status; // success|failed|not_found
        private final Double distance;

        public FaceRecognitionResult(String status, Double distance) {
            this.status = status;
            this.distance = distance;
        }

        public String getStatus() {
            return status;
        }

        public Double getDistance() {
            return distance;
        }
    }

    // DTO ánh xạ JSON response từ Python
    public static class FaceRecognitionApiResponse {
        private Boolean match;
        private Double distance;
        private Double threshold;

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

        public Double getThreshold() {
            return threshold;
        }

        public void setThreshold(Double threshold) {
            this.threshold = threshold;
        }
    }
}

