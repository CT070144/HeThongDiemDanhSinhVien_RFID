package com.rfid.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity để lưu trữ API keys cho các thiết bị ESP32
 */
@Entity
@Table(name = "apikey")
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "apikey", length = 64, unique = true, nullable = false)
    private String apiKey;
    
    @Column(name = "mathietbi", length = 50, nullable = false)
    private String maThietBi;
    
    @Column(name = "mota", length = 255)
    private String moTa;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    public ApiKey() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }
    
    public ApiKey(String apiKey, String maThietBi, String moTa) {
        this();
        this.apiKey = apiKey;
        this.maThietBi = maThietBi;
        this.moTa = moTa;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
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
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }
    
    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    /**
     * Kiểm tra xem API key có còn hiệu lực không
     */
    public boolean isValid() {
        if (!active) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }
}

