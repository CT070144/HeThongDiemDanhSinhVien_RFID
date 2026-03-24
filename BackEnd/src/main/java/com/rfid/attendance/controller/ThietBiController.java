package com.rfid.attendance.controller;

import com.rfid.attendance.entity.ThietBi;
import com.rfid.attendance.entity.ApiKey;
import com.rfid.attendance.repository.ThietBiRepository;
import com.rfid.attendance.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/thietbi")
@CrossOrigin(origins = "*")
public class ThietBiController {

    @Autowired
    private ThietBiRepository thietBiRepository;

    @Autowired
    private ApiKeyService apiKeyService;

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

        // Save device
        ThietBi savedDevice = thietBiRepository.save(tb);
        
        // Generate API key for the device
        ApiKey apiKey = apiKeyService.generateApiKey(savedDevice.getMaThietBi(), "Default API key for " + savedDevice.getMaThietBi());
        
        // Return device with API key
        Map<String, Object> response = new HashMap<>();
        response.put("device", savedDevice);
        response.put("apiKey", apiKey.getApiKey());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{maThietBi}")
    public ResponseEntity<?> update(@PathVariable String maThietBi, @RequestBody ThietBi tb) {
        return thietBiRepository.findById(maThietBi)
                .map(existing -> {
                    existing.setPhongHoc(tb.getPhongHoc());
                    
                    // If device status is being changed to inactive, deactivate all its API keys
                    if (tb.getActive() != null && !tb.getActive() && existing.getActive()) {
                        apiKeyService.deactivateAllKeysByDevice(maThietBi);
                    }
                    
                    existing.setActive(tb.getActive());
                    return ResponseEntity.ok(thietBiRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{maThietBi}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable String maThietBi) {
        return thietBiRepository.findById(maThietBi)
                .map(existing -> {
                    Boolean newStatus = !existing.getActive();
                    existing.setActive(newStatus);
                    
                    // If deactivating, deactivate all API keys
                    if (!newStatus) {
                        apiKeyService.deactivateAllKeysByDevice(maThietBi);
                    }
                    
                    return ResponseEntity.ok(thietBiRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{maThietBi}/activate")
    public ResponseEntity<?> activateDevice(@PathVariable String maThietBi) {
        return thietBiRepository.findById(maThietBi)
                .map(existing -> {
                    existing.setActive(true);
                    return ResponseEntity.ok(thietBiRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{maThietBi}/deactivate")
    public ResponseEntity<?> deactivateDevice(@PathVariable String maThietBi) {
        return thietBiRepository.findById(maThietBi)
                .map(existing -> {
                    existing.setActive(false);
                    
                    // Deactivate all API keys when device is deactivated
                    apiKeyService.deactivateAllKeysByDevice(maThietBi);
                    
                    return ResponseEntity.ok(thietBiRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{maThietBi}")
    public ResponseEntity<?> delete(@PathVariable String maThietBi) {
        if (!thietBiRepository.existsById(maThietBi)) {
            return ResponseEntity.notFound().build();
        }
        
        // Deactivate all API keys first
        apiKeyService.deactivateAllKeysByDevice(maThietBi);
        
        // Then delete the device
        thietBiRepository.deleteById(maThietBi);
        return ResponseEntity.noContent().build();
    }

    // API Key Management Endpoints
    
    @GetMapping("/{maThietBi}/api-keys")
    public ResponseEntity<List<ApiKey>> getApiKeysByDevice(@PathVariable String maThietBi) {
        List<ApiKey> keys = apiKeyService.getByMaThietBi(maThietBi);
        return ResponseEntity.ok(keys);
    }

    @PostMapping("/{maThietBi}/api-keys")
    public ResponseEntity<?> createApiKey(@PathVariable String maThietBi, @RequestBody Map<String, String> request) {
        // Verify device exists
        if (!thietBiRepository.existsById(maThietBi)) {
            return ResponseEntity.notFound().build();
        }
        
        String moTa = request.getOrDefault("moTa", "API key for " + maThietBi);
        ApiKey apiKey = apiKeyService.generateApiKey(maThietBi, moTa);
        
        Map<String, Object> response = new HashMap<>();
        response.put("apiKey", apiKey);
        response.put("keyValue", apiKey.getApiKey()); // Plain key to display once
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/api-keys/{id}/toggle")
    public ResponseEntity<?> toggleApiKeyStatus(@PathVariable Long id) {
        if (apiKeyService.toggleApiKeyStatus(id)) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/api-keys/{id}/activate")
    public ResponseEntity<?> activateApiKey(@PathVariable Long id) {
        if (apiKeyService.activateApiKey(id)) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/api-keys/{id}/revoke")
    public ResponseEntity<?> revokeApiKey(@PathVariable Long id) {
        if (apiKeyService.revokeApiKey(id)) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/api-keys/{id}/deactivate")
    public ResponseEntity<?> deactivateApiKey(@PathVariable Long id) {
        if (apiKeyService.revokeApiKey(id)) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<?> deleteApiKey(@PathVariable Long id) {
        if (apiKeyService.deleteApiKey(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}


