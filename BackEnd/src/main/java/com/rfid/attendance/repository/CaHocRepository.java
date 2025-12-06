package com.rfid.attendance.repository;

import com.rfid.attendance.entity.CaHoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CaHocRepository extends JpaRepository<CaHoc, Long> {
    @Query("SELECT DISTINCT c.ngayHoc, c.ca FROM CaHoc c WHERE c.lopHocPhan = :lopHocPhan AND c.ngayHoc IS NOT NULL AND c.ca IS NOT NULL ORDER BY c.ngayHoc ASC, c.ca ASC")
    List<Object[]> findDistinctSessionsByLopHocPhan(@Param("lopHocPhan") String lopHocPhan);
    
    @Query("SELECT c FROM CaHoc c WHERE c.phongHoc = :phongHoc AND c.ngayHoc = :ngayHoc AND c.ca = :ca")
    List<CaHoc> findByPhongHocAndNgayHocAndCa(@Param("phongHoc") String phongHoc, 
                                               @Param("ngayHoc") LocalDate ngayHoc, 
                                               @Param("ca") Integer ca);
    
    @Query("SELECT c FROM CaHoc c WHERE c.phongHoc = :phongHoc AND c.ngayHoc = :ngayHoc")
    List<CaHoc> findByPhongHocAndNgayHoc(@Param("phongHoc") String phongHoc, 
                                         @Param("ngayHoc") LocalDate ngayHoc);
    
    @Query("SELECT c FROM CaHoc c WHERE c.phongHoc = :phongHoc AND (:ngayHoc IS NULL OR c.ngayHoc = :ngayHoc) AND (:ca IS NULL OR c.ca = :ca)")
    List<CaHoc> findByPhongHocAndFilters(@Param("phongHoc") String phongHoc,
                                          @Param("ngayHoc") LocalDate ngayHoc,
                                          @Param("ca") Integer ca);
}


