package com.rfid.attendance.repository;

import com.rfid.attendance.entity.LopHocPhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LopHocPhanRepository extends JpaRepository<LopHocPhan, String> {
    
    Optional<LopHocPhan> findByMaLopHocPhan(String maLopHocPhan);
    
    @Query("SELECT l FROM LopHocPhan l WHERE " +
           "LOWER(l.maLopHocPhan) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.tenLopHocPhan) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<LopHocPhan> findByKeyword(@Param("keyword") String keyword);
    
    boolean existsByMaLopHocPhan(String maLopHocPhan);
    
    @Query("SELECT l FROM LopHocPhan l ORDER BY l.tenLopHocPhan ASC")
    List<LopHocPhan> findAllOrderByTenLopHocPhan();
}
