package com.rfid.attendance.service;

import com.rfid.attendance.entity.PhieuDiemDanh;
import com.rfid.attendance.repository.PhieuDiemDanhRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class ScheduledAttendanceService {
    
    @Autowired
    private PhieuDiemDanhRepository phieuDiemDanhRepository;
    
    /**
     * Tự động cập nhật trạng thái cho sinh viên chưa điểm danh ra
     * Chạy mỗi 10 phút
     */
    @Scheduled(fixedRate = 300000) // 10 phút = 600,000 milliseconds
    public void updateAttendanceStatus() {
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        
        // Lấy tất cả phiếu điểm danh hôm nay có trạng thái "Đang học" và chưa có giờ ra
        List<PhieuDiemDanh> activeAttendance = phieuDiemDanhRepository
                .findByNgayAndTrangThaiAndGioRaIsNull(today, PhieuDiemDanh.TrangThaiHoc.DANG_HOC);
        
        int updatedCount = 0;
        
        for (PhieuDiemDanh attendance : activeAttendance) {
            Integer ca = attendance.getCa();
            LocalTime sessionEndTime = getSessionEndTime(ca);
            
            // Nếu đã quá thời gian kết thúc ca học (cộng thêm 5 phút buffer)
            if (currentTime.isAfter(sessionEndTime.plusMinutes(5))) {
                attendance.setTrangThai(PhieuDiemDanh.TrangThaiHoc.KHONG_DIEM_DANH_RA);
                phieuDiemDanhRepository.save(attendance);
                updatedCount++;
                
                System.out.println("Cập nhật trạng thái 'Không điểm danh ra' cho sinh viên: " + 
                                 attendance.getTenSinhVien() + " - Ca " + ca);
            }
        }
        
        if (updatedCount > 0) {
            System.out.println("Đã cập nhật trạng thái cho " + updatedCount + " sinh viên");
        }
    }
    
    /**
     * Cập nhật trạng thái cho một ca học cụ thể
     * Chạy ngay sau khi ca học kết thúc
     */
    @Scheduled(cron = "0 30 9,12,15,17,20 * * MON-FRI") // 9:30, 12:30, 15:30, 17:30, 20:30 từ T2-T6
    public void updateAttendanceStatusAfterSession() {
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        Integer currentSession = getCurrentSessionFromTime(currentTime);
        
        if (currentSession != null) {
            List<PhieuDiemDanh> activeAttendance = phieuDiemDanhRepository
                    .findByNgayAndCaAndTrangThaiAndGioRaIsNull(today, currentSession, PhieuDiemDanh.TrangThaiHoc.DANG_HOC);
            
            int updatedCount = 0;
            
            for (PhieuDiemDanh attendance : activeAttendance) {
                attendance.setTrangThai(PhieuDiemDanh.TrangThaiHoc.KHONG_DIEM_DANH_RA);
                phieuDiemDanhRepository.save(attendance);
                updatedCount++;
                
                System.out.println("Cập nhật trạng thái 'Không điểm danh ra' cho sinh viên: " + 
                                 attendance.getTenSinhVien() + " - Ca " + currentSession);
            }
            
            if (updatedCount > 0) {
                System.out.println("Đã cập nhật trạng thái cho " + updatedCount + " sinh viên sau ca " + currentSession);
            }
        }
    }
    
    private LocalTime getSessionEndTime(Integer ca) {
        switch (ca) {
            case 1: return LocalTime.of(9, 25);  // Ca 1: 7h - 9h25
            case 2: return LocalTime.of(12, 0);  // Ca 2: 9h35 - 12h
            case 3: return LocalTime.of(14, 55); // Ca 3: 12h30 - 14h55
            case 4: return LocalTime.of(17, 30); // Ca 4: 15h05 - 17h30
            case 5: return LocalTime.of(20, 30); // Ca 5: 18h - 20h30
            default: return LocalTime.of(23, 59);
        }
    }
    
    private Integer getCurrentSessionFromTime(LocalTime time) {
        // 9:30 -> Ca 1 vừa kết thúc
        if (time.equals(LocalTime.of(9, 30))) return 1;
        // 12:30 -> Ca 2 vừa kết thúc  
        if (time.equals(LocalTime.of(12, 30))) return 2;
        // 15:30 -> Ca 3 vừa kết thúc
        if (time.equals(LocalTime.of(15, 30))) return 3;
        // 17:30 -> Ca 4 vừa kết thúc
        if (time.equals(LocalTime.of(17, 30))) return 4;
        // 20:30 -> Ca 5 vừa kết thúc
        if (time.equals(LocalTime.of(20, 30))) return 5;
        
        return null;
    }
}
