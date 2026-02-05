package com.rfid.attendance.service;

import com.rfid.attendance.entity.ApiKey;
import com.rfid.attendance.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ApiKeyService {
    
    private static final int API_KEY_LENGTH = 32; // 32 bytes = 256 bits
    private static final SecureRandom secureRandom = new SecureRandom();
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    /**
     * Tạo API key mới cho thiết bị
     */
    public ApiKey generateApiKey(String maThietBi, String moTa) {
        String apiKey = generateSecureApiKey();
        
        ApiKey newApiKey = new ApiKey(apiKey, maThietBi, moTa);
        return apiKeyRepository.save(newApiKey);
    }
    
    /**
     * Tạo API key mới với thời gian hết hạn
     */
    public ApiKey generateApiKey(String maThietBi, String moTa, LocalDateTime expiresAt) {
        ApiKey apiKey = generateApiKey(maThietBi, moTa);
        apiKey.setExpiresAt(expiresAt);
        return apiKeyRepository.save(apiKey);
    }
    
    /**
     * Validate API key và cập nhật thời gian sử dụng cuối cùng
     */
    public Optional<ApiKey> validateAndUpdateUsage(String apiKey) {
        Optional<ApiKey> keyOpt = apiKeyRepository.findValidApiKey(apiKey, LocalDateTime.now());
        
        if (keyOpt.isPresent()) {
            ApiKey key = keyOpt.get();
            // Cập nhật thời gian sử dụng cuối cùng
            key.setLastUsedAt(LocalDateTime.now());
            apiKeyRepository.save(key);
            return Optional.of(key);
        }
        
        return Optional.empty();
    }
    
    /**
     * Validate API key mà không cập nhật thời gian sử dụng
     */
    public boolean isValid(String apiKey) {
        return apiKeyRepository.findValidApiKey(apiKey, LocalDateTime.now()).isPresent();
    }
    
    /**
     * Lấy API key theo ID
     */
    public Optional<ApiKey> getById(Long id) {
        return apiKeyRepository.findById(id);
    }
    
    /**
     * Lấy API key theo giá trị key
     */
    public Optional<ApiKey> getByApiKey(String apiKey) {
        return apiKeyRepository.findByApiKey(apiKey);
    }
    
    /**
     * Lấy tất cả API keys của một thiết bị
     */
    public List<ApiKey> getByMaThietBi(String maThietBi) {
        return apiKeyRepository.findByMaThietBi(maThietBi);
    }
    
    /**
     * Lấy tất cả API keys đang active của một thiết bị
     */
    public List<ApiKey> getActiveByMaThietBi(String maThietBi) {
        return apiKeyRepository.findActiveByMaThietBi(maThietBi);
    }
    
    /**
     * Lấy tất cả API keys
     */
    public List<ApiKey> getAll() {
        return apiKeyRepository.findAll();
    }
    
    /**
     * Lấy tất cả API keys đang active
     */
    public List<ApiKey> getAllActive() {
        return apiKeyRepository.findAllActive();
    }
    
    /**
     * Vô hiệu hóa API key (revoke)
     */
    public boolean revokeApiKey(Long id) {
        Optional<ApiKey> keyOpt = apiKeyRepository.findById(id);
        if (keyOpt.isPresent()) {
            ApiKey key = keyOpt.get();
            key.setActive(false);
            apiKeyRepository.save(key);
            return true;
        }
        return false;
    }
    
    /**
     * Vô hiệu hóa API key theo giá trị key
     */
    public boolean revokeApiKeyByValue(String apiKey) {
        Optional<ApiKey> keyOpt = apiKeyRepository.findByApiKey(apiKey);
        if (keyOpt.isPresent()) {
            ApiKey key = keyOpt.get();
            key.setActive(false);
            apiKeyRepository.save(key);
            return true;
        }
        return false;
    }
    
    /**
     * Kích hoạt lại API key
     */
    public boolean activateApiKey(Long id) {
        Optional<ApiKey> keyOpt = apiKeyRepository.findById(id);
        if (keyOpt.isPresent()) {
            ApiKey key = keyOpt.get();
            key.setActive(true);
            apiKeyRepository.save(key);
            return true;
        }
        return false;
    }
    
    /**
     * Xóa API key
     */
    public boolean deleteApiKey(Long id) {
        if (apiKeyRepository.existsById(id)) {
            apiKeyRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    /**
     * Xóa tất cả API keys của một thiết bị
     */
    public void deleteByMaThietBi(String maThietBi) {
        List<ApiKey> keys = apiKeyRepository.findByMaThietBi(maThietBi);
        apiKeyRepository.deleteAll(keys);
    }

    /**
     * Vô hiệu hóa tất cả API keys của một thiết bị
     */
    public void deactivateAllKeysByDevice(String maThietBi) {
        List<ApiKey> keys = apiKeyRepository.findByMaThietBi(maThietBi);
        keys.forEach(key -> key.setActive(false));
        apiKeyRepository.saveAll(keys);
    }

    /**
     * Chuyển đổi trạng thái của API key
     */
    public boolean toggleApiKeyStatus(Long id) {
        java.util.Optional<ApiKey> keyOpt = apiKeyRepository.findById(id);
        if (keyOpt.isPresent()) {
            ApiKey key = keyOpt.get();
            key.setActive(!key.getActive());
            apiKeyRepository.save(key);
            return true;
        }
        return false;
    }
    
    /**
     * Tạo API key ngẫu nhiên an toàn
     */
    private String generateSecureApiKey() {
        byte[] randomBytes = new byte[API_KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);
        
        // Encode thành Base64 URL-safe string
        String base64Key = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        // Thêm prefix để dễ nhận biết
        return "esp32_" + base64Key;
    }
}

