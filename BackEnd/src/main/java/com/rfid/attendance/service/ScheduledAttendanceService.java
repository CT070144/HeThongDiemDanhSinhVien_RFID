package com.rfid.attendance.service;

import com.rfid.attendance.entity.PhieuDiemDanh;
import com.rfid.attendance.entity.CaLam;
import com.rfid.attendance.repository.PhieuDiemDanhRepository;
import com.rfid.attendance.repository.CaLamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ScheduledAttendanceService {
    
    @Autowired
    private PhieuDiemDanhRepository phieuDiemDanhRepository;

    @Autowired
    private CaLamRepository caLamRepository;

    @Autowired
    private AttendanceService attendanceService;
    

    /**
     * Chạy lúc 23:59 mỗi ngày:
     * quét toàn bộ phiếu trong ngày, phiếu nào chưa có giờ ra => KHONG_DIEM_DANH_RA.
     */
    @Scheduled(cron = "0 38 23 * * *", zone = "Asia/Ho_Chi_Minh")
    public void finalizeMissingCheckoutAtEndOfDay() {
        LocalDate today = LocalDate.now();
        int updated = attendanceService.finalizeMissingCheckoutForDate(today);
        if (updated > 0) {
            System.out.println("23:59 cập nhật " + updated + " phiếu chưa điểm danh ra thành KHONG_DIEM_DANH_RA");
        }
    }
    
    private LocalTime getSessionEndTime(Integer ca) {
        if (ca != null) {
            Optional<CaLam> shiftOpt = caLamRepository.findByMaCa(ca);
            if (shiftOpt.isPresent() && shiftOpt.get().getGioKetThuc() != null) {
                return shiftOpt.get().getGioKetThuc();
            }
        }

        // Fallback giá trị cũ để đảm bảo hệ thống chạy được khi DB chưa có cấu hình.
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
