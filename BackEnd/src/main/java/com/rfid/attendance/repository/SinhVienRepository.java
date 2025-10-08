package com.rfid.attendance.repository;

import com.rfid.attendance.entity.SinhVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SinhVienRepository extends JpaRepository<SinhVien, String> {



    @Query("SELECT s FROM SinhVien s WHERE TRIM(s.rfid) = TRIM(:rfid)")
    Optional<SinhVien> findByRfid(@Param("rfid") String rfid);
    
    Optional<SinhVien> findByMaSinhVien(String maSinhVien);
    
    @Query("SELECT s FROM SinhVien s WHERE " +
           "LOWER(s.maSinhVien) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.tenSinhVien) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<SinhVien> findByKeyword(@Param("keyword") String keyword);
    
    boolean existsByRfid(String rfid);
    
    boolean existsByMaSinhVien(String maSinhVien);
    
    List<SinhVien> findByMaSinhVienIn(List<String> maSinhViens);
}
