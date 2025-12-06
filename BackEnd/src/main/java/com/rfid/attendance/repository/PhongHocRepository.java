package com.rfid.attendance.repository;

import com.rfid.attendance.entity.PhongHoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PhongHocRepository extends JpaRepository<PhongHoc, String> {

    @Query("SELECT p FROM PhongHoc p WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(p.maPhong) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.tenPhong) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.toaNha) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<PhongHoc> search(@Param("keyword") String keyword);

    @Query("SELECT p FROM PhongHoc p WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(p.maPhong) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.tenPhong) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.toaNha) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<PhongHoc> searchPaged(@Param("keyword") String keyword, Pageable pageable);
}


