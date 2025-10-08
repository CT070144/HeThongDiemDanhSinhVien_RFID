package com.rfid.attendance.repository;

import com.rfid.attendance.entity.SinhVienLopHocPhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SinhVienLopHocPhanRepository extends JpaRepository<SinhVienLopHocPhan, String> {
    
    @Query("SELECT s FROM SinhVienLopHocPhan s WHERE s.maSinhVien = :maSinhVien")
    List<SinhVienLopHocPhan> findByMaSinhVien(@Param("maSinhVien") String maSinhVien);
    
    @Query("SELECT s FROM SinhVienLopHocPhan s WHERE s.maLopHocPhan = :maLopHocPhan")
    List<SinhVienLopHocPhan> findByMaLopHocPhan(@Param("maLopHocPhan") String maLopHocPhan);
    
    @Query("SELECT s FROM SinhVienLopHocPhan s WHERE s.maSinhVien = :maSinhVien AND s.maLopHocPhan = :maLopHocPhan")
    SinhVienLopHocPhan findByMaSinhVienAndMaLopHocPhan(@Param("maSinhVien") String maSinhVien, @Param("maLopHocPhan") String maLopHocPhan);
    
    @Query("SELECT COUNT(s) FROM SinhVienLopHocPhan s WHERE s.maLopHocPhan = :maLopHocPhan")
    long countByMaLopHocPhan(@Param("maLopHocPhan") String maLopHocPhan);
    
    @Query("SELECT sv FROM SinhVien sv " +
           "JOIN SinhVienLopHocPhan slhp ON sv.maSinhVien = slhp.maSinhVien " +
           "WHERE slhp.maLopHocPhan = :maLopHocPhan")
    List<Object[]> findSinhVienByLopHocPhan(@Param("maLopHocPhan") String maLopHocPhan);
    
    void deleteByMaSinhVienAndMaLopHocPhan(String maSinhVien, String maLopHocPhan);
    
    void deleteByMaLopHocPhan(String maLopHocPhan);
    
    boolean existsByMaSinhVienAndMaLopHocPhan(String maSinhVien, String maLopHocPhan);
}
