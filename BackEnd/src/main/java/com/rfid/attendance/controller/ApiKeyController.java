package com.rfid.attendance.controller;

import com.rfid.attendance.entity.ApiKey;
import com.rfid.attendance.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/apikey")
@CrossOrigin(origins = "*")
public class ApiKeyController {
    
    @Autowired
    private ApiKeyService apiKeyService;
    
    /**
     * Tạo API key mới cho thiết bị
     */
    @PostMapping
    public ResponseEntity<?> createApiKey(@RequestBody CreateApiKeyRequest request) {
        try {
            ApiKey apiKey = apiKeyService.generateApiKey(
                request.getMaThietBi(), 
                request.getMoTa()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", apiKey.getId());
            response.put("apiKey", apiKey.getApiKey()); // Chỉ trả về khi tạo mới
            response.put("maThietBi", apiKey.getMaThietBi());
            response.put("moTa", apiKey.getMoTa());
            response.put("createdAt", apiKey.getCreatedAt());
            response.put("expiresAt", apiKey.getExpiresAt());
            response.put("message", "API key created successfully. Save this key securely!");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create API key: " + e.getMessage()));
        }
    }
    
    /**
     * Lấy tất cả API keys
     */
    @GetMapping
    public ResponseEntity<List<ApiKey>> getAllApiKeys() {
        List<ApiKey> apiKeys = apiKeyService.getAll();
        // Ẩn giá trị API key thực tế để bảo mật
        apiKeys.forEach(key -> key.setApiKey("***hidden***"));
        return ResponseEntity.ok(apiKeys);
    }
    
    /**
     * Lấy API keys theo mã thiết bị
     */
    @GetMapping("/device/{maThietBi}")
    public ResponseEntity<List<ApiKey>> getApiKeysByDevice(@PathVariable String maThietBi) {
        List<ApiKey> apiKeys = apiKeyService.getByMaThietBi(maThietBi);
        // Ẩn giá trị API key thực tế
        apiKeys.forEach(key -> key.setApiKey("***hidden***"));
        return ResponseEntity.ok(apiKeys);
    }
    
    /**
     * Lấy API key theo ID (không hiển thị giá trị key)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getApiKeyById(@PathVariable Long id) {
        Optional<ApiKey> apiKeyOpt = apiKeyService.getById(id);
        if (apiKeyOpt.isPresent()) {
            ApiKey apiKey = apiKeyOpt.get();
            apiKey.setApiKey("***hidden***"); // Ẩn giá trị key
            return ResponseEntity.ok(apiKey);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "API key not found"));
    }
    
    /**
     * Vô hiệu hóa API key
     */
    @PutMapping("/{id}/revoke")
    public ResponseEntity<?> revokeApiKey(@PathVariable Long id) {
        boolean success = apiKeyService.revokeApiKey(id);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "API key revoked successfully"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "API key not found"));
    }
    
    /**
     * Kích hoạt lại API key
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateApiKey(@PathVariable Long id) {
        boolean success = apiKeyService.activateApiKey(id);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "API key activated successfully"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "API key not found"));
    }
    
    /**
     * Xóa API key
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApiKey(@PathVariable Long id) {
        boolean success = apiKeyService.deleteApiKey(id);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "API key deleted successfully"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "API key not found"));
    }
    
    /**
     * Validate API key (dùng để test)
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateApiKey(@RequestBody ValidateApiKeyRequest request) {
        boolean isValid = apiKeyService.isValid(request.getApiKey());
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        if (isValid) {
            Optional<ApiKey> apiKeyOpt = apiKeyService.getByApiKey(request.getApiKey());
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                response.put("maThietBi", apiKey.getMaThietBi());
                response.put("active", apiKey.getActive());
                response.put("expiresAt", apiKey.getExpiresAt());
            }
        }
        return ResponseEntity.ok(response);
    }
    
    // Inner classes for request DTOs
    public static class CreateApiKeyRequest {
        private String maThietBi;
        private String moTa;
        private LocalDateTime expiresAt;
        
        public String getMaThietBi() {
            return maThietBi;
        }
        
        public void setMaThietBi(String maThietBi) {
            this.maThietBi = maThietBi;
        }
        
        public String getMoTa() {
            return moTa;
        }
        
        public void setMoTa(String moTa) {
            this.moTa = moTa;
        }
        
        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
        
        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
    
    public static class ValidateApiKeyRequest {
        private String apiKey;
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}

