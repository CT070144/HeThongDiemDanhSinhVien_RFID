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
}


