package com.rfid.attendance.repository;

import com.rfid.attendance.entity.PhieuDiemDanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhieuDiemDanhRepository extends JpaRepository<PhieuDiemDanh, Long> {
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.rfid = :rfid AND p.ngay = :ngay AND p.ca = :ca")
    Optional<PhieuDiemDanh> findByRfidAndNgayAndCa(@Param("rfid") String rfid, 
                                                   @Param("ngay") LocalDate ngay, 
                                                   @Param("ca") Integer ca);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.ngay = :ngay ORDER BY p.ca ASC, p.createdAt DESC")
    List<PhieuDiemDanh> findByNgay(@Param("ngay") LocalDate ngay);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.ca = :ca ORDER BY p.ngay DESC, p.createdAt DESC")
    List<PhieuDiemDanh> findByCa(@Param("ca") Integer ca);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.maSinhVien = :maSinhVien ORDER BY p.ngay DESC, p.ca ASC")
    List<PhieuDiemDanh> findByMaSinhVien(@Param("maSinhVien") String maSinhVien);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE " +
           "(:ngay IS NULL OR p.ngay = :ngay) AND " +
           "(:ca IS NULL OR p.ca = :ca) AND " +
           "(:maSinhVien IS NULL OR p.maSinhVien = :maSinhVien) " +
           "ORDER BY p.ngay DESC, p.ca ASC, p.createdAt DESC")
    List<PhieuDiemDanh> findByFilters(@Param("ngay") LocalDate ngay, 
                                      @Param("ca") Integer ca, 
                                      @Param("maSinhVien") String maSinhVien);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.ngay = CURRENT_DATE ORDER BY p.ca ASC, p.createdAt DESC")
    List<PhieuDiemDanh> findTodayAttendance();
}
