package com.rfid.attendance.service;

import com.rfid.attendance.dto.RoomDetailDTO;
import com.rfid.attendance.dto.RoomScheduleDTO;
import com.rfid.attendance.dto.RoomStatusDTO;
import com.rfid.attendance.entity.CaHoc;
import com.rfid.attendance.entity.LopHocPhan;
import com.rfid.attendance.entity.PhieuDiemDanh;
import com.rfid.attendance.entity.PhongHoc;
import com.rfid.attendance.repository.CaHocRepository;
import com.rfid.attendance.repository.LopHocPhanRepository;
import com.rfid.attendance.repository.PhieuDiemDanhRepository;
import com.rfid.attendance.repository.PhongHocRepository;
import com.rfid.attendance.repository.SinhVienLopHocPhanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomStatusService {

    private final PhongHocRepository phongHocRepository;
    private final CaHocRepository caHocRepository;
    private final PhieuDiemDanhRepository phieuDiemDanhRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final SinhVienLopHocPhanRepository sinhVienLopHocPhanRepository;

    public RoomStatusService(PhongHocRepository phongHocRepository,
                            CaHocRepository caHocRepository,
                            PhieuDiemDanhRepository phieuDiemDanhRepository,
                            LopHocPhanRepository lopHocPhanRepository,
                            SinhVienLopHocPhanRepository sinhVienLopHocPhanRepository) {
        this.phongHocRepository = phongHocRepository;
        this.caHocRepository = caHocRepository;
        this.phieuDiemDanhRepository = phieuDiemDanhRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.sinhVienLopHocPhanRepository = sinhVienLopHocPhanRepository;
    }

    /**
     * Lấy danh sách phòng học với trạng thái (trống/đang sử dụng)
     * @param toaNha Filter theo tòa nhà (optional)
     * @param tang Filter theo tầng (optional)
     * @param ngay Filter theo ngày (optional, mặc định là hôm nay)
     * @param ca Filter theo ca (optional, mặc định là ca hiện tại)
     * @return Danh sách RoomStatusDTO
     */
    public List<RoomStatusDTO> getRoomsWithStatus(String toaNha, Integer tang, LocalDate ngay, Integer ca) {
        // Mặc định lấy ngày hôm nay và ca hiện tại nếu không có filter
        if (ngay == null) {
            ngay = LocalDate.now();
        }
        
        // Xác định ca hiện tại nếu không có filter
        if (ca == null) {
            ca = getCurrentCa();
        }

        // Lấy tất cả phòng học
        List<PhongHoc> allRooms = phongHocRepository.findAll();
        
        // Filter theo tòa nhà và tầng nếu có
        if (toaNha != null && !toaNha.isBlank()) {
            allRooms = allRooms.stream()
                    .filter(r -> toaNha.equals(r.getToaNha()))
                    .collect(Collectors.toList());
        }
        
        if (tang != null) {
            allRooms = allRooms.stream()
                    .filter(r -> tang.equals(r.getTang()))
                    .collect(Collectors.toList());
        }

        List<RoomStatusDTO> result = new ArrayList<>();
        
        for (PhongHoc room : allRooms) {
            RoomStatusDTO dto = new RoomStatusDTO(
                    room.getMaPhong(),
                    room.getTenPhong(),
                    room.getToaNha(),
                    room.getTang(),
                    room.getSucChua(),
                    room.getLoaiPhong(),
                    room.getTrangThai()
            );

            // Kiểm tra xem phòng có lớp học đang diễn ra không
            List<CaHoc> currentCaHoc = caHocRepository.findByPhongHocAndNgayHocAndCa(
                    room.getMaPhong(), ngay, ca);

            if (!currentCaHoc.isEmpty()) {
                // Phòng đang có lớp học
                CaHoc caHoc = currentCaHoc.get(0);
                dto.setStatus("occupied");
                dto.setCurrentClass(caHoc.getLopHocPhan());
                dto.setCurrentCa(caHoc.getCa());

                // Lấy thông tin lớp học phần
                String tenLopHocPhan = caHoc.getLopHocPhan();
                if (tenLopHocPhan != null && !tenLopHocPhan.isBlank()) {
                    // Tìm mã lớp học phần từ tên
                    String maLopHocPhan = findMaLopHocPhan(tenLopHocPhan);
                    
                    if (maLopHocPhan != null) {
                        Optional<LopHocPhan> lopHocPhan = lopHocPhanRepository.findByMaLopHocPhan(maLopHocPhan);
                        
                        if (lopHocPhan.isPresent()) {
                            // Đếm tổng số sinh viên trong lớp
                            long totalStudents = sinhVienLopHocPhanRepository.countByMaLopHocPhan(maLopHocPhan);
                            dto.setTotalStudents((int) totalStudents);

                            // Đếm số sinh viên đã điểm danh
                            List<PhieuDiemDanh> attendance = phieuDiemDanhRepository.findByPhongHocAndNgayAndCa(
                                    room.getMaPhong(), ngay, ca);
                            dto.setStudentsAttended(attendance.size());
                        }
                    }
                }
            } else {
                // Phòng trống
                dto.setStatus("empty");
            }

            result.add(dto);
        }

        return result;
    }

    /**
     * Lấy chi tiết phòng học
     * @param maPhong Mã phòng
     * @param ngay Ngày (optional, mặc định hôm nay)
     * @param ca Ca (optional, mặc định ca hiện tại)
     * @return RoomDetailDTO
     */
    public RoomDetailDTO getRoomDetail(String maPhong, LocalDate ngay, Integer ca) {
        Optional<PhongHoc> phongHocOpt = phongHocRepository.findById(maPhong);
        if (phongHocOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy phòng học: " + maPhong);
        }

        PhongHoc phongHoc = phongHocOpt.get();
        RoomDetailDTO dto = new RoomDetailDTO();
        dto.setMaPhong(phongHoc.getMaPhong());
        dto.setTenPhong(phongHoc.getTenPhong());
        dto.setToaNha(phongHoc.getToaNha());
        dto.setTang(phongHoc.getTang());
        dto.setSucChua(phongHoc.getSucChua());
        dto.setLoaiPhong(phongHoc.getLoaiPhong());
        dto.setTrangThai(phongHoc.getTrangThai());

        // Mặc định lấy ngày hôm nay và ca hiện tại nếu không có filter
        if (ngay == null) {
            ngay = LocalDate.now();
        }
        if (ca == null) {
            ca = getCurrentCa();
        }

        // Tìm lớp học đang diễn ra
        List<CaHoc> currentCaHoc = caHocRepository.findByPhongHocAndNgayHocAndCa(maPhong, ngay, ca);

        if (!currentCaHoc.isEmpty()) {
            CaHoc caHoc = currentCaHoc.get(0);
            RoomDetailDTO.ClassInfoDTO classInfo = new RoomDetailDTO.ClassInfoDTO();
            classInfo.setTenLopHocPhan(caHoc.getLopHocPhan());
            classInfo.setCa(caHoc.getCa());
            classInfo.setNgayHoc(ngay);
            classInfo.setGiangVien(caHoc.getGiaoVien());

            // Lấy thời gian ca học
            String[] timeRange = getCaTimeRange(caHoc.getCa());
            classInfo.setThoiGianBatDau(timeRange[0]);
            classInfo.setThoiGianKetThuc(timeRange[1]);

            // Lấy thông tin lớp học phần
            String tenLopHocPhan = caHoc.getLopHocPhan();
            if (tenLopHocPhan != null && !tenLopHocPhan.isBlank()) {
                String maLopHocPhan = findMaLopHocPhan(tenLopHocPhan);
                
                if (maLopHocPhan != null) {
                    Optional<LopHocPhan> lopHocPhan = lopHocPhanRepository.findByMaLopHocPhan(maLopHocPhan);
                    
                    if (lopHocPhan.isPresent()) {
                        classInfo.setMaLopHocPhan(maLopHocPhan);
                        if (classInfo.getGiangVien() == null || classInfo.getGiangVien().isBlank()) {
                            classInfo.setGiangVien(lopHocPhan.get().getGiangVien());
                        }

                        // Đếm tổng số sinh viên
                        long totalStudents = sinhVienLopHocPhanRepository.countByMaLopHocPhan(maLopHocPhan);
                        classInfo.setSoSinhVien((int) totalStudents);
                    }
                }
            }

            // Lấy danh sách điểm danh
            List<PhieuDiemDanh> attendance = phieuDiemDanhRepository.findByPhongHocAndNgayAndCa(maPhong, ngay, ca);
            classInfo.setSoSinhVienDaDiemDanh(attendance.size());

            // Chuyển đổi danh sách điểm danh sang DTO
            List<RoomDetailDTO.AttendanceInfoDTO> attendanceList = new ArrayList<>();
            for (PhieuDiemDanh pdd : attendance) {
                RoomDetailDTO.AttendanceInfoDTO attInfo = new RoomDetailDTO.AttendanceInfoDTO();
                attInfo.setMaSinhVien(pdd.getMaSinhVien());
                attInfo.setTenSinhVien(pdd.getTenSinhVien());
                attInfo.setGioVao(pdd.getGioVao());
                
                // Xác định trạng thái điểm danh
                if (pdd.getTinhTrangDiemDanh() != null) {
                    switch (pdd.getTinhTrangDiemDanh()) {
                        case DUNG_GIO:
                            attInfo.setTrangThai("Đúng giờ");
                            break;
                        case MUON:
                            attInfo.setTrangThai("Muộn");
                            break;
                        default:
                            attInfo.setTrangThai("Chưa xác định");
                    }
                } else {
                    attInfo.setTrangThai("Chưa xác định");
                }
                
                attendanceList.add(attInfo);
            }

            dto.setClassInfo(classInfo);
            dto.setAttendanceList(attendanceList);
        }

        return dto;
    }

    /**
     * Xác định ca học hiện tại dựa trên thời gian
     */
    private Integer getCurrentCa() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int totalMinutes = hour * 60 + minute;

        // Ca 1: 07:00-09:25 (420-565)
        if (totalMinutes >= 420 && totalMinutes < 565) {
            return 1;
        }
        // Ca 2: 09:35-12:00 (575-720)
        if (totalMinutes >= 575 && totalMinutes < 720) {
            return 2;
        }
        // Ca 3: 12:30-14:55 (750-895)
        if (totalMinutes >= 750 && totalMinutes < 895) {
            return 3;
        }
        // Ca 4: 15:05-17:30 (905-1050)
        if (totalMinutes >= 905 && totalMinutes < 1050) {
            return 4;
        }
        // Ca 5: 18:00-20:30 (1080-1230)
        if (totalMinutes >= 1080 && totalMinutes < 1230) {
            return 5;
        }
        // Ngoài giờ học
        return null;
    }

    /**
     * Lấy thời gian ca học
     */
    private String[] getCaTimeRange(Integer ca) {
        if (ca == null) {
            return new String[]{"", ""};
        }
        return switch (ca) {
            case 1 -> new String[]{"07:00", "09:25"};
            case 2 -> new String[]{"09:35", "12:00"};
            case 3 -> new String[]{"12:30", "14:55"};
            case 4 -> new String[]{"15:05", "17:30"};
            case 5 -> new String[]{"18:00", "20:30"};
            default -> new String[]{"", ""};
        };
    }

    /**
     * Tìm mã lớp học phần từ tên lớp học phần
     */
    private String findMaLopHocPhan(String tenLopHocPhan) {
        if (tenLopHocPhan == null || tenLopHocPhan.isBlank()) {
            return null;
        }
        
        // Tìm trong database theo tên
        List<LopHocPhan> allLopHocPhan = lopHocPhanRepository.findAll();
        for (LopHocPhan lhp : allLopHocPhan) {
            if (tenLopHocPhan.equals(lhp.getTenLopHocPhan())) {
                return lhp.getMaLopHocPhan();
            }
        }
        
        // Nếu không tìm thấy, thử generate mã từ tên (sử dụng logic tương tự LopHocPhanCodeUtil)
        try {
            String maLopHocPhan = com.rfid.attendance.util.LopHocPhanCodeUtil.generateMaLopHocPhan(tenLopHocPhan);
            // Kiểm tra xem mã này có tồn tại trong database không
            if (lopHocPhanRepository.existsByMaLopHocPhan(maLopHocPhan)) {
                return maLopHocPhan;
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return null;
    }

    /**
     * Lấy lịch sử dụng phòng học theo tầng và ngày
     * @param toaNha Tòa nhà
     * @param tang Tầng
     * @param ngay Ngày (mặc định hôm nay)
     * @return Danh sách RoomScheduleDTO
     */
    public List<RoomScheduleDTO> getRoomScheduleByFloor(String toaNha, Integer tang, LocalDate ngay) {
        if (ngay == null) {
            ngay = LocalDate.now();
        }

        // Lấy tất cả phòng học trong tầng
        List<PhongHoc> rooms = phongHocRepository.findAll().stream()
                .filter(r -> {
                    if (toaNha != null && !toaNha.isBlank() && !toaNha.equals(r.getToaNha())) {
                        return false;
                    }
                    if (tang != null && !tang.equals(r.getTang())) {
                        return false;
                    }
                    return true;
                })
                .sorted((r1, r2) -> {
                    // Sắp xếp theo mã phòng
                    if (r1.getMaPhong() != null && r2.getMaPhong() != null) {
                        return r1.getMaPhong().compareTo(r2.getMaPhong());
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        List<RoomScheduleDTO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (PhongHoc room : rooms) {
            RoomScheduleDTO scheduleDTO = new RoomScheduleDTO();
            scheduleDTO.setMaPhong(room.getMaPhong());
            scheduleDTO.setTenPhong(room.getTenPhong());
            scheduleDTO.setToaNha(room.getToaNha());
            scheduleDTO.setTang(room.getTang());

            // Lấy tất cả ca học trong ngày của phòng này
            List<CaHoc> allCaHoc = caHocRepository.findByPhongHocAndNgayHoc(room.getMaPhong(), ngay);
            if (allCaHoc == null) {
                allCaHoc = new ArrayList<>();
            }
            
            // Tạo map để nhóm ca học theo lớp học phần (để xử lý lớp học kéo dài nhiều ca)
            Map<String, List<CaHoc>> classCaMap = new HashMap<>();
            for (CaHoc caHoc : allCaHoc) {
                if (caHoc != null) {
                    String key = caHoc.getLopHocPhan() != null ? caHoc.getLopHocPhan() : "unknown";
                    classCaMap.computeIfAbsent(key, k -> new ArrayList<>()).add(caHoc);
                }
            }

            // Tạo danh sách ca schedule cho 5 ca (1-5)
            List<RoomScheduleDTO.CaScheduleDTO> caSchedules = new ArrayList<>();


            boolean isEmptyAllDay = allCaHoc.isEmpty();
            for (int ca = 1; ca <= 5; ca++) {
                RoomScheduleDTO.CaScheduleDTO caSchedule = new RoomScheduleDTO.CaScheduleDTO();
                caSchedule.setCa(ca);
                
                // Tìm ca học tương ứng
                CaHoc caHoc = null;
                final int currentCa = ca; // Tạo biến final để sử dụng trong lambda
                caHoc = allCaHoc.stream()
                        .filter(c -> c != null && c.getCa() != null && currentCa == c.getCa().intValue())
                        .findFirst()
                        .orElse(null);
                
                // Xử lý trường hợp trống cả ngày
                if (isEmptyAllDay) {
                    if (ca == 1) {
                        // Chỉ đánh dấu ở ca đầu tiên với spanCount = 5
                        caSchedule.setStatus("empty");
                        caSchedule.setTenLopHocPhan("TRỐNG CẢ NGÀY");
                        caSchedule.setSpanCount(5);
                    } else {
                        // Các ca khác không hiển thị gì (đánh dấu là spanning)
                        caSchedule.setStatus("empty");
                        caSchedule.setTenLopHocPhan(null);
                        caSchedule.setSpanning(true);
                    }
                    caSchedules.add(caSchedule);
                    continue;
                }
                
                if (caHoc != null) {
                    // Phòng có lớp học trong ca này
                    caSchedule.setStatus("occupied");
                    caSchedule.setTenLopHocPhan(caHoc.getLopHocPhan());
                    caSchedule.setGiangVien(caHoc.getGiaoVien());
                    
                    // Lấy thời gian ca học
                    String[] timeRange = getCaTimeRange(ca);
                    if (timeRange[0] != null && !timeRange[0].isEmpty()) {
                        caSchedule.setThoiGianBatDau(LocalTime.parse(timeRange[0]));
                        caSchedule.setThoiGianKetThuc(LocalTime.parse(timeRange[1]));
                    }

                    // Kiểm tra xem lớp học có kéo dài nhiều ca không
                    String tenLopHocPhan = caHoc.getLopHocPhan();
                    if (tenLopHocPhan != null && !tenLopHocPhan.isBlank()) {
                        List<CaHoc> sameClass = classCaMap.get(tenLopHocPhan);
                        if (sameClass != null && sameClass.size() > 1) {
                            // Lớp học kéo dài nhiều ca
                            caSchedule.setSpanning(true);
                            caSchedule.setSpanCount(sameClass.size());
                            
                            // Chỉ hiển thị tên lớp ở ca đầu tiên (ca nhỏ nhất)
                            List<Integer> caNumbers = sameClass.stream()
                                    .map(CaHoc::getCa)
                                    .filter(c -> c != null)
                                    .sorted()
                                    .collect(Collectors.toList());
                            
                            if (!caNumbers.isEmpty() && !caNumbers.get(0).equals(ca)) {
                                // Không phải ca đầu tiên của lớp học kéo dài
                                caSchedule.setTenLopHocPhan(null); // Ẩn tên ở các ca sau
                            }
                        }
                    }

                    // Lấy thông tin số sinh viên
                    String maLopHocPhan = findMaLopHocPhan(caHoc.getLopHocPhan());
                    if (maLopHocPhan != null) {
                        long totalStudents = sinhVienLopHocPhanRepository.countByMaLopHocPhan(maLopHocPhan);
                        caSchedule.setSoSinhVien((int) totalStudents);
                        
                        List<PhieuDiemDanh> attendance = phieuDiemDanhRepository.findByPhongHocAndNgayAndCa(
                                room.getMaPhong(), ngay, ca);
                        caSchedule.setSoSinhVienDaDiemDanh(attendance.size());
                    }

                    // Kiểm tra trạng thái "upcoming" (sắp tới trong 30 phút)
                    if (ngay.equals(LocalDate.now())) {
                        LocalTime caStartTime = caSchedule.getThoiGianBatDau();
                        if (caStartTime != null) {
                            LocalDateTime caStartDateTime = LocalDateTime.of(ngay, caStartTime);
                            long minutesUntilStart = java.time.Duration.between(now, caStartDateTime).toMinutes();
                            
                            if (minutesUntilStart > 0 && minutesUntilStart <= 30) {
                                caSchedule.setStatus("upcoming");
                            }
                        }
                    }
                } else {
                    // Phòng trống trong ca này (không có ca học)
                    caSchedule.setStatus("empty");
                    caSchedule.setTenLopHocPhan("Trống");
                }

                // Kiểm tra trạng thái bảo trì (ưu tiên cao hơn)
                if ("inactive".equals(room.getTrangThai()) || "maintenance".equals(room.getTrangThai())) {
                    caSchedule.setStatus("maintenance");
                    if (!isEmptyAllDay || ca != 1) {
                        caSchedule.setTenLopHocPhan("Bảo trì");
                    }
                }

                caSchedules.add(caSchedule);
            }

            scheduleDTO.setCaSchedules(caSchedules);
            result.add(scheduleDTO);
        }

        return result;
    }
}

