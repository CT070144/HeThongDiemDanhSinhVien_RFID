package com.rfid.attendance.repository;

import com.rfid.attendance.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    /**
     * Tìm API key theo giá trị key
     */
    Optional<ApiKey> findByApiKey(String apiKey);
    
    /**
     * Tìm API key theo mã thiết bị
     */
    List<ApiKey> findByMaThietBi(String maThietBi);
    
    /**
     * Tìm API key đang active theo mã thiết bị
     */
    @Query("SELECT a FROM ApiKey a WHERE a.maThietBi = :maThietBi AND a.active = true")
    List<ApiKey> findActiveByMaThietBi(@Param("maThietBi") String maThietBi);
    
    /**
     * Tìm API key hợp lệ (active và chưa hết hạn)
     */
    @Query("SELECT a FROM ApiKey a WHERE a.apiKey = :apiKey AND a.active = true " +
           "AND (a.expiresAt IS NULL OR a.expiresAt > :now)")
    Optional<ApiKey> findValidApiKey(@Param("apiKey") String apiKey, @Param("now") LocalDateTime now);
    
    /**
     * Cập nhật thời gian sử dụng cuối cùng
     */
    @Modifying
    @Query("UPDATE ApiKey a SET a.lastUsedAt = :lastUsedAt WHERE a.apiKey = :apiKey")
    void updateLastUsedAt(@Param("apiKey") String apiKey, @Param("lastUsedAt") LocalDateTime lastUsedAt);
    
    /**
     * Tìm tất cả API keys đang active
     */
    @Query("SELECT a FROM ApiKey a WHERE a.active = true")
    List<ApiKey> findAllActive();
}

