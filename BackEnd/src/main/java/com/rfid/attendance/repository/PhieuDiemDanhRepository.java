package com.rfid.attendance.repository;

import com.rfid.attendance.entity.PhieuDiemDanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.ngay = :ngay ORDER BY p.ca DESC, p.createdAt DESC")
    List<PhieuDiemDanh> findByNgay(@Param("ngay") LocalDate ngay);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.ca = :ca ORDER BY p.ngay DESC, p.createdAt DESC")
    List<PhieuDiemDanh> findByCa(@Param("ca") Integer ca);

    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.rfid = :rfid ORDER BY p.ngay DESC, p.createdAt DESC")
    List<PhieuDiemDanh> findByRfid(@Param("rfid") String rfid);

    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.rfid = :rfid AND p.ngay = :ngay ORDER BY p.ca ASC, p.createdAt ASC, p.id ASC")
    List<PhieuDiemDanh> findByRfidAndNgayOrderByCaAscCreatedAtAsc(@Param("rfid") String rfid,
                                                                  @Param("ngay") LocalDate ngay);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.maSinhVien = :maSinhVien ORDER BY p.ngay DESC, p.ca ASC")
    List<PhieuDiemDanh> findByMaSinhVien(@Param("maSinhVien") String maSinhVien);

    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.maSinhVien = :maSinhVien AND p.ngay = :ngay ORDER BY p.ca ASC, p.createdAt ASC, p.id ASC")
    List<PhieuDiemDanh> findByMaSinhVienAndNgayOrderByCaAscCreatedAtAsc(@Param("maSinhVien") String maSinhVien,
                                                                        @Param("ngay") LocalDate ngay);

    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.maSinhVien = :maSinhVien AND p.ngay = :ngay AND p.ca = :ca " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<PhieuDiemDanh> findByMaSinhVienAndNgayAndCaOrderByCreatedAtDesc(
            @Param("maSinhVien") String maSinhVien,
            @Param("ngay") LocalDate ngay,
            @Param("ca") Integer ca
    );
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE " +
           "(:ngay IS NULL OR p.ngay = :ngay) AND " +
           "(:ca IS NULL OR p.ca = :ca) AND " +
           "(:maSinhVien IS NULL OR p.maSinhVien = :maSinhVien) AND " +
           "(:phongHoc IS NULL OR p.phongHoc = :phongHoc) " +
           "ORDER BY p.ngay DESC, p.ca ASC, p.createdAt DESC")
    List<PhieuDiemDanh> findByFilters(@Param("ngay") LocalDate ngay, 
                                      @Param("ca") Integer ca, 
                                      @Param("maSinhVien") String maSinhVien,
                                      @Param("phongHoc") String phongHoc);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.ngay = CURRENT_DATE ORDER BY p.ca ASC, p.createdAt DESC")
    List<PhieuDiemDanh> findTodayAttendance();

    @Query("SELECT p FROM PhieuDiemDanh p WHERE " +
            "(:startDate IS NULL OR p.ngay >= :startDate) AND " +
            "(:endDate IS NULL OR p.ngay <= :endDate) " +
            "ORDER BY p.ngay DESC, p.ca ASC, p.createdAt DESC")
    List<PhieuDiemDanh> findByDateRange(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    Page<PhieuDiemDanh> findAll(Pageable pageable);
    @Query("SELECT p FROM PhieuDiemDanh p ORDER BY p.ngay DESC, p.ca ASC, p.createdAt DESC")
    Page<PhieuDiemDanh> findAllPaged(Pageable pageable);
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.ngay = CURRENT_DATE ORDER BY p.ca ASC, p.createdAt DESC")
    Page<PhieuDiemDanh> findTodayAttendancePaged(Pageable pageable);

    @Query("SELECT p FROM PhieuDiemDanh p WHERE " +
            "(:startDate IS NULL OR p.ngay >= :startDate) AND " +
            "(:endDate IS NULL OR p.ngay <= :endDate) AND " +
            "(:ca IS NULL OR p.ca = :ca) AND " +
            "(:maSinhVien IS NULL OR :maSinhVien = '' OR LOWER(p.maSinhVien) LIKE LOWER(CONCAT('%', :maSinhVien, '%'))) AND " +
            "(:phongHocListEmpty = true OR p.phongHoc IN :phongHocList) AND " +
            "(:maPhongBanListEmpty = true OR p.maPhongBan IN :maPhongBanList) AND " +
            "(:tinhTrang IS NULL OR p.tinhTrangDiemDanh = :tinhTrang) AND " +
            "(:trangThaiHoc IS NULL OR p.trangThai = :trangThaiHoc) " +
            "ORDER BY " +
            "CASE WHEN UPPER(:sortDir) = 'ASC' THEN p.ngay END ASC, " +
            "CASE WHEN UPPER(:sortDir) = 'DESC' THEN p.ngay END DESC, " +
            "p.ca ASC, p.createdAt DESC, p.id DESC")
    Page<PhieuDiemDanh> findByAdvancedFiltersPaged(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("ca") Integer ca,
                                                   @Param("maSinhVien") String maSinhVien,
                                                   @Param("phongHocList") List<String> phongHocList,
                                                   @Param("phongHocListEmpty") boolean phongHocListEmpty,
                                                   @Param("maPhongBanList") List<String> maPhongBanList,
                                                   @Param("maPhongBanListEmpty") boolean maPhongBanListEmpty,
                                                   @Param("tinhTrang") PhieuDiemDanh.TrangThai tinhTrang,
                                                   @Param("trangThaiHoc") PhieuDiemDanh.TrangThaiHoc trangThaiHoc,
                                                   @Param("sortDir") String sortDir,
                                                   Pageable pageable);

    @Query("SELECT p FROM PhieuDiemDanh p WHERE " +
            "(:startDate IS NULL OR p.ngay >= :startDate) AND " +
            "(:endDate IS NULL OR p.ngay <= :endDate) AND " +
            "(:ca IS NULL OR p.ca = :ca) AND " +
            "(:maSinhVien IS NULL OR :maSinhVien = '' OR LOWER(p.maSinhVien) LIKE LOWER(CONCAT('%', :maSinhVien, '%'))) AND " +
            "(:phongHocListEmpty = true OR p.phongHoc IN :phongHocList) AND " +
            "(:maPhongBanListEmpty = true OR p.maPhongBan IN :maPhongBanList) AND " +
            "(:tinhTrang IS NULL OR p.tinhTrangDiemDanh = :tinhTrang) AND " +
            "(:trangThaiHoc IS NULL OR p.trangThai = :trangThaiHoc) " +
            "ORDER BY p.maPhongBan ASC, p.maSinhVien ASC, p.ngay ASC, p.ca ASC, p.gioVao ASC")
    List<PhieuDiemDanh> findByAdvancedFilters(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              @Param("ca") Integer ca,
                                              @Param("maSinhVien") String maSinhVien,
                                              @Param("phongHocList") List<String> phongHocList,
                                              @Param("phongHocListEmpty") boolean phongHocListEmpty,
                                              @Param("maPhongBanList") List<String> maPhongBanList,
                                              @Param("maPhongBanListEmpty") boolean maPhongBanListEmpty,
                                              @Param("tinhTrang") PhieuDiemDanh.TrangThai tinhTrang,
                                              @Param("trangThaiHoc") PhieuDiemDanh.TrangThaiHoc trangThaiHoc);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE PhieuDiemDanh p SET p.rfid = :newRfid, p.maSinhVien = :maSinhVien, p.tenSinhVien = :tenSinhVien, p.maPhongBan = :maPhongBan WHERE p.rfid = :oldRfid")
    int updateStudentInfoByRfid(@Param("oldRfid") String oldRfid,
                                @Param("newRfid") String newRfid,
                                @Param("maSinhVien") String maSinhVien,
                                @Param("tenSinhVien") String tenSinhVien,
                                @Param("maPhongBan") String maPhongBan);

    @org.springframework.transaction.annotation.Transactional
    void deleteByRfid(String rfid);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.ngay = :ngay AND p.trangThai = :trangThai AND p.gioRa IS NULL")
    List<PhieuDiemDanh> findByNgayAndTrangThaiAndGioRaIsNull(@Param("ngay") LocalDate ngay, 
                                                             @Param("trangThai") PhieuDiemDanh.TrangThaiHoc trangThai);

    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.ngay = :ngay AND p.gioRa IS NULL")
    List<PhieuDiemDanh> findByNgayAndGioRaIsNull(@Param("ngay") LocalDate ngay);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.ngay = :ngay AND p.ca = :ca AND p.trangThai = :trangThai AND p.gioRa IS NULL")
    List<PhieuDiemDanh> findByNgayAndCaAndTrangThaiAndGioRaIsNull(@Param("ngay") LocalDate ngay, 
                                                                  @Param("ca") Integer ca, 
                                                                  @Param("trangThai") PhieuDiemDanh.TrangThaiHoc trangThai);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE p.phongHoc = :phongHoc AND p.ngay = :ngay AND p.ca = :ca ORDER BY p.gioVao ASC")
    List<PhieuDiemDanh> findByPhongHocAndNgayAndCa(@Param("phongHoc") String phongHoc,
                                                    @Param("ngay") LocalDate ngay,
                                                    @Param("ca") Integer ca);
    
    @Query("SELECT p FROM PhieuDiemDanh p WHERE " +
           "p.ngay = :ngay AND " +
           "p.ca = :ca AND " +
           "p.maSinhVien IN :maSinhVienList " +
           "ORDER BY p.createdAt DESC")
    List<PhieuDiemDanh> findByNgayAndCaAndMaSinhVienIn(@Param("ngay") LocalDate ngay,
                                                        @Param("ca") Integer ca,
                                                        @Param("maSinhVienList") List<String> maSinhVienList);
}
