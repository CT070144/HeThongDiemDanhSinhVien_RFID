package com.rfid.attendance.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rfid.attendance.entity.*;
import com.rfid.attendance.repository.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.Duration;
import java.time.DayOfWeek;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class AttendanceService {
    private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    
    @Autowired
    private PhieuDiemDanhRepository phieuDiemDanhRepository;
    
    @Autowired
    private SinhVienRepository sinhVienRepository;

    @Autowired
    private WebSocketSessionRepository webSocketSessionRepository;
    
    @Autowired
    private DocRfidRepository docRfidRepository;

    @Autowired
    private ThietBiRepository thietBiRepository;

    @Autowired
    private LopHocPhanRepository lopHocPhanRepository;
    
    @Autowired
    private CaHocRepository caHocRepository;
    
    @Autowired
    private SinhVienLopHocPhanRepository sinhVienLopHocPhanRepository;

    @Autowired
    private CaLamRepository caLamRepository;

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private SocketIOServer socketIOServer;

    @Autowired
    private AttendancePhotoStorageService attendancePhotoStorageService;

    /**
     * Gắn ảnh chụp điểm danh cho phiếu vừa được tạo/cập nhật.
     */
    @Transactional
    public PhieuDiemDanh attachAttendancePhoto(PhieuDiemDanh attendance, MultipartFile image) {
        if (attendance == null || attendance.getId() == null) {
            return attendance;
        }
        if (image == null || image.isEmpty()) {
            return attendance;
        }
        try {
            String pathFile = attendancePhotoStorageService.storePhoto(attendance.getId(), image);
            if (pathFile == null || pathFile.isBlank()) {
                return attendance;
            }
            attendance.setPathFile(pathFile);
            return phieuDiemDanhRepository.save(attendance);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu ảnh chụp điểm danh", e);
        }
    }

    /**
     * Gắn cùng 1 ảnh chụp cho nhiều phiếu điểm danh (dùng cho trường hợp split/xuyên ca).
     * Mỗi phiếu sẽ có file ảnh riêng theo attendanceId để đáp ứng yêu cầu "tất cả bản ghi đều có ảnh".
     */
    @Transactional
    public void attachAttendancePhotoToMany(List<PhieuDiemDanh> attendances, MultipartFile image) {
        if (attendances == null || attendances.isEmpty()) return;
        if (image == null || image.isEmpty()) return;
        try {
            byte[] bytes = image.getBytes();
            String originalName = image.getOriginalFilename();
            String contentType = image.getContentType();

            Set<Long> dedup = new HashSet<>();
            for (PhieuDiemDanh a : attendances) {
                if (a == null || a.getId() == null) continue;
                if (!dedup.add(a.getId())) continue;
                if (a.getPathFile() != null && !a.getPathFile().isBlank()) {
                    // Đã có ảnh rồi -> không ghi đè
                    continue;
                }
                String pathFile = attendancePhotoStorageService.storePhotoBytes(a.getId(), bytes, originalName, contentType);
                if (pathFile != null && !pathFile.isBlank()) {
                    a.setPathFile(pathFile);
                    phieuDiemDanhRepository.save(a);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu ảnh chụp điểm danh (multi)", e);
        }
    }

    /**
     * Xử lý điểm danh RFID (theo logic hiện tại) và nếu có ảnh thì gắn ảnh cho toàn bộ các phiếu bị ảnh hưởng
     * (bao gồm cả các ca ở giữa khi split/xuyên ca trong ngày).
     */
    @Transactional
    public PhieuDiemDanh processRfidAttendanceWithDeviceAndPhoto(String rfid, String maThietBi, MultipartFile image) {
        ProcessResult res = processRfidAttendanceWithDeviceAndCollect(rfid, maThietBi);
        if (image != null && !image.isEmpty()) {
            attachAttendancePhotoToMany(res.affectedRecords, image);
        }
        return res.lastRecord;
    }

    private static class ProcessResult {
        private final PhieuDiemDanh lastRecord;
        private final List<PhieuDiemDanh> affectedRecords;

        private ProcessResult(PhieuDiemDanh lastRecord, List<PhieuDiemDanh> affectedRecords) {
            this.lastRecord = lastRecord;
            this.affectedRecords = affectedRecords;
        }
    }

    /**
     * Giống {@link #processRfidAttendanceWithDevice(String, String)} nhưng trả thêm danh sách phiếu bị ảnh hưởng
     * (để phục vụ gắn ảnh hàng loạt).
     */
    private ProcessResult processRfidAttendanceWithDeviceAndCollect(String rfid, String maThietBi) {
        List<PhieuDiemDanh> affected = new ArrayList<>();

        // process core
        ProcessResult core = processRfidAttendanceAndCollect(rfid);
        PhieuDiemDanh record = core.lastRecord;
        affected.addAll(core.affectedRecords);

        if (record.getRfid() == null) {
            attachDeviceContextToUnregisteredRfid(rfid, maThietBi);
            socketIOServer.getAllClients().forEach(client -> {
                String message = null;
                try {
                    message = objectMapper.writeValueAsString(rfid);
                } catch (JsonProcessingException e) {
                    System.out.println("error convert RFID to JSON");
                }
                System.out.println("publishing invalid-rfid event: " + message);
                client.sendEvent("invalid-rfid", message);
            });
            return new ProcessResult(record, affected);
        }

        if (maThietBi != null && !maThietBi.isEmpty() && record.getCa() != -99) {
            Optional<ThietBi> tb = thietBiRepository.findById(maThietBi);
            tb.ifPresent(thietBi -> {
                record.setPhongHoc(thietBi.getPhongHoc());
                phieuDiemDanhRepository.save(record);
            });
        }

        socketIOServer.getAllClients().forEach(client -> {
            String message = null;
            try {
                message = objectMapper.writeValueAsString(record);
            } catch (JsonProcessingException e) {
                System.out.println("error convert object");
            }
            System.out.println("publishing event " + message);
            client.sendEvent("update-attendance", message);
        });

        // make sure record is included
        if (record != null) {
            affected.add(record);
        }
        return new ProcessResult(record, affected);
    }


    private ProcessResult processRfidAttendanceAndCollect(String rfid) {
        List<PhieuDiemDanh> affected = new ArrayList<>();

        String trimmedRfid = rfid != null ? rfid.trim() : "";
        if (trimmedRfid.isEmpty()) {
            saveUnregisteredRfid(rfid);
            return new ProcessResult(new PhieuDiemDanh(), affected);
        }

        Optional<SinhVien> sinhVienOpt = sinhVienRepository.findByRfid(trimmedRfid);
        if (sinhVienOpt.isEmpty()) {
            saveUnregisteredRfid(trimmedRfid);
            PhieuDiemDanh response = new PhieuDiemDanh();
            response.setRfid(null);
            return new ProcessResult(response, affected);
        }
        SinhVien sinhVien = sinhVienOpt.get();

        LocalDate today = LocalDate.now(APP_ZONE_ID);
        LocalTime now = LocalTime.now(APP_ZONE_ID);

        List<CaLam> shifts = caLamRepository.findAllByOrderByMaCaAsc();
        if (shifts == null || shifts.isEmpty()) {
            throw new IllegalStateException("Chưa cấu hình ca làm");
        }

        List<PhieuDiemDanh> todayRecords = phieuDiemDanhRepository
                .findByRfidAndNgayOrderByCaAscCreatedAtAsc(trimmedRfid, today);

        PhieuDiemDanh open = todayRecords.stream()
                .filter(r -> r != null && r.getGioRa() == null)
                .reduce((a, b) -> b)
                .orElse(null);

        Integer inShiftCa = resolveShiftContaining(shifts, now);
        Integer nextShiftCa = resolveNextShift(shifts, now);
        Integer prevShiftCa = resolvePreviousShift(shifts, now);

        if (open == null) {
            // === CHECK-IN ===
            if (inShiftCa == null) {
                // Nếu sau giờ kết thúc ca cuối cùng trong ngày thì không ghi nhận
                if (isAfterLastShiftEndSameDay(shifts, now)) {
                    System.out.println("ngoài giờ làm việc");

                    socketIOServer.getAllClients().forEach(client -> client.sendEvent("out-of-time", "Ngoài giờ làm việc"));

                    throw new RuntimeException("Ngoài giờ làm (sau ca cuối)");
                }
            }

            Integer targetCa = (inShiftCa != null) ? inShiftCa : nextShiftCa;
            if (targetCa == null) {
                throw new IllegalStateException("Không xác định được ca làm phù hợp");
            }

            boolean completedSameShift = todayRecords.stream()
                    .anyMatch(r -> r != null
                            && r.getCa() != null
                            && r.getCa().equals(targetCa)
                            && r.getGioVao() != null
                            && r.getGioRa() != null);
            if (completedSameShift) {
                PhieuDiemDanh response = new PhieuDiemDanh();
                response.setRfid(trimmedRfid);
                response.setTenSinhVien(sinhVien.getTenSinhVien());
                response.setCa(-99);
                return new ProcessResult(response, affected);
            }

            PhieuDiemDanh newRecord = new PhieuDiemDanh();
            newRecord.setRfid(trimmedRfid);
            newRecord.setMaSinhVien(sinhVien.getMaSinhVien());
            newRecord.setTenSinhVien(sinhVien.getTenSinhVien());
            newRecord.setMaPhongBan(sinhVien.getMaPhongBan());
            newRecord.setNgay(today);
            newRecord.setCa(targetCa);
            newRecord.setGioVao(now);
            newRecord.setGioRa(null);
            newRecord.setTinhTrangDiemDanh(determineAttendanceStatus(now, targetCa));
            newRecord.setTrangThai(PhieuDiemDanh.TrangThaiHoc.DANG_HOC);
            PhieuDiemDanh saved = phieuDiemDanhRepository.save(newRecord);
            affected.add(saved);
            return new ProcessResult(saved, affected);
        }

        // === CHECK-OUT ===
        Integer checkoutCa = (inShiftCa != null) ? inShiftCa : (prevShiftCa != null ? prevShiftCa : open.getCa());
        if (checkoutCa == null) checkoutCa = open.getCa();

        SplitResult split = splitAttendanceAcrossShiftsSameDayCollect(open, sinhVien, today, now, checkoutCa, shifts);
        affected.addAll(split.affectedRecords);
        return new ProcessResult(split.lastRecord, affected);
    }

    private boolean isAfterLastShiftEndSameDay(List<CaLam> shifts, LocalTime now) {
        if (shifts == null || shifts.isEmpty() || now == null) return false;
        LocalTime latestEnd = null;
        for (CaLam s : shifts) {
            if (s == null || s.getGioBatDau() == null || s.getGioKetThuc() == null) continue;
            // Bỏ ca qua đêm trong mode same-day
            if (!s.getGioKetThuc().isAfter(s.getGioBatDau())) continue;
            if (latestEnd == null || s.getGioKetThuc().isAfter(latestEnd)) {
                latestEnd = s.getGioKetThuc();
            }
        }
        return latestEnd != null && now.isAfter(latestEnd);
    }

    private static class SplitResult {
        private final PhieuDiemDanh lastRecord;
        private final List<PhieuDiemDanh> affectedRecords;

        private SplitResult(PhieuDiemDanh lastRecord, List<PhieuDiemDanh> affectedRecords) {
            this.lastRecord = lastRecord;
            this.affectedRecords = affectedRecords;
        }
    }

    private SplitResult splitAttendanceAcrossShiftsSameDayCollect(
            PhieuDiemDanh openRecord,
            SinhVien sinhVien,
            LocalDate day,
            LocalTime checkoutTime,
            Integer checkoutCa,
            List<CaLam> shifts
    ) {
        List<PhieuDiemDanh> affected = new ArrayList<>();
        PhieuDiemDanh last = splitAttendanceAcrossShiftsSameDay(openRecord, sinhVien, day, checkoutTime, checkoutCa, shifts);

        // Re-load today records for this rfid to include "ca giữa" vừa tạo/cập nhật (đảm bảo list đầy đủ)
        if (openRecord != null && openRecord.getRfid() != null && day != null) {
            affected.addAll(phieuDiemDanhRepository.findByRfidAndNgayOrderByCaAscCreatedAtAsc(openRecord.getRfid(), day));
        } else if (last != null) {
            affected.add(last);
        }
        return new SplitResult(last, affected);
    }

    public AttendanceDetailResponse getAttendanceDetail(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Thiếu id phiếu điểm danh");
        }

        PhieuDiemDanh attendance = phieuDiemDanhRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu điểm danh id=" + id));

        String photoDataUrl = null;
        if (attendance.getPathFile() != null && !attendance.getPathFile().isBlank()) {
            try {
                byte[] bytes = attendancePhotoStorageService.loadPhoto(attendance.getPathFile());
                if (bytes != null) {
                    String contentType = attendancePhotoStorageService.detectContentType(attendance.getPathFile());
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    photoDataUrl = "data:" + contentType + ";base64," + base64;
                }
            } catch (Exception ignored) {
                // Nếu đọc ảnh lỗi thì trả chi tiết không có ảnh.
            }
        }

        return new AttendanceDetailResponse(attendance, photoDataUrl);
    }

    public static class AttendanceDetailResponse {
        private final PhieuDiemDanh attendance;
        private final String photoDataUrl;

        public AttendanceDetailResponse(PhieuDiemDanh attendance, String photoDataUrl) {
            this.attendance = attendance;
            this.photoDataUrl = photoDataUrl;
        }

        public PhieuDiemDanh getAttendance() {
            return attendance;
        }

        public String getPhotoDataUrl() {
            return photoDataUrl;
        }
    }
    
    public List<PhieuDiemDanh> getAllAttendance() {
        return phieuDiemDanhRepository.findAll();
    }
    
    public List<PhieuDiemDanh> getAttendanceByFilters(LocalDate ngay, Integer ca, String maSinhVien, String phongHoc) {
        return phieuDiemDanhRepository.findByFilters(ngay, ca, maSinhVien, phongHoc);
    }
    
    public List<PhieuDiemDanh> getTodayAttendance() {
        return phieuDiemDanhRepository.findTodayAttendance();
    }
    
    public List<PhieuDiemDanh> getAttendanceByStudent(String maSinhVien) {
        return phieuDiemDanhRepository.findByMaSinhVien(maSinhVien);
    }

    public List<PhieuDiemDanh> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
        return phieuDiemDanhRepository.findByDateRange(startDate, endDate);
    }

    public Page<PhieuDiemDanh> getAttendanceByAdvancedFiltersPaged(
            LocalDate startDate,
            LocalDate endDate,
            Integer ca,
            String maSinhVien,
            String phongHoc,
            String maPhongBan,
            String tinhTrang,
            String trangThai,
            String sortDir,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

        String normalizedMaSinhVien = normalizeString(maSinhVien);
        List<String> phongHocList = splitCsvToList(phongHoc);
        List<String> maPhongBanList = splitCsvToList(maPhongBan);

        PhieuDiemDanh.TrangThai tinhTrangEnum = parseTinhTrang(tinhTrang);
        PhieuDiemDanh.TrangThaiHoc trangThaiHocEnum = parseTrangThaiHoc(trangThai);

        String normalizedSortDir = normalizeString(sortDir);
        if (normalizedSortDir == null) {
            normalizedSortDir = "DESC";
        }
        String upperSort = normalizedSortDir.toUpperCase();
        if (!"ASC".equals(upperSort) && !"DESC".equals(upperSort)) {
            throw new IllegalArgumentException("Giá trị sortDir không hợp lệ (ASC|DESC): " + sortDir);
        }

        return phieuDiemDanhRepository.findByAdvancedFiltersPaged(
                startDate,
                endDate,
                ca,
                normalizedMaSinhVien,
                phongHocList,
                phongHocList.isEmpty(),
                maPhongBanList,
                maPhongBanList.isEmpty(),
                tinhTrangEnum,
                trangThaiHocEnum,
                upperSort,
                pageable
        );
    }

    public byte[] exportAttendanceExcelByFilters(
            LocalDate startDate,
            LocalDate endDate,
            Integer ca,
            String maSinhVien,
            String phongHoc,
            String maPhongBan,
            String tinhTrang,
            String trangThai
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Bắt buộc nhập từ ngày đến ngày khi xuất file");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Đến ngày không được nhỏ hơn từ ngày");
        }

        String normalizedMaSinhVien = normalizeString(maSinhVien);
        List<String> phongHocList = splitCsvToList(phongHoc);
        List<String> maPhongBanList = splitCsvToList(maPhongBan);
        PhieuDiemDanh.TrangThai tinhTrangEnum = parseTinhTrang(tinhTrang);
        PhieuDiemDanh.TrangThaiHoc trangThaiHocEnum = parseTrangThaiHoc(trangThai);

        List<PhieuDiemDanh> records = phieuDiemDanhRepository.findByAdvancedFilters(
                startDate,
                endDate,
                ca,
                normalizedMaSinhVien,
                phongHocList,
                phongHocList.isEmpty(),
                maPhongBanList,
                maPhongBanList.isEmpty(),
                tinhTrangEnum,
                trangThaiHocEnum
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildSummarySheet(workbook, records, startDate, endDate);
            buildDetailSheet(workbook, records, startDate, endDate);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Không thể xuất file Excel", e);
        }
    }

    private void buildSummarySheet(Workbook workbook, List<PhieuDiemDanh> records, LocalDate startDate, LocalDate endDate) {
        Sheet sheet = workbook.createSheet("Tổng hợp");
        CellStyle headerStyle = createHeaderStyle(workbook);
        List<LocalDate> dateRange = buildDateRange(startDate, endDate);

        int rowIdx = 0;
        Row titleRow = sheet.createRow(rowIdx++);
        titleRow.createCell(0).setCellValue("BẢNG CHẤM CÔNG");

        Row dateRow = sheet.createRow(rowIdx++);
        dateRow.createCell(0).setCellValue("Từ ngày " + startDate + " đến ngày " + endDate);

        Row deptRow = sheet.createRow(rowIdx++);
        deptRow.createCell(0).setCellValue("Tất cả các bộ phận");

        rowIdx++;
        Row dayNumberHeader = sheet.createRow(rowIdx++);
        Row dayNameHeader = sheet.createRow(rowIdx++);
        String[] fixedHeaders = {"STT", "Mã nhân viên", "Họ và tên"};
        for (int i = 0; i < fixedHeaders.length; i++) {
            Cell c = dayNumberHeader.createCell(i);
            c.setCellValue(fixedHeaders[i]);
            c.setCellStyle(headerStyle);
            dayNameHeader.createCell(i).setCellStyle(headerStyle);
        }

        int dayStartCol = fixedHeaders.length;
        for (int i = 0; i < dateRange.size(); i++) {
            LocalDate date = dateRange.get(i);
            int colIdx = dayStartCol + i;

            Cell dayCell = dayNumberHeader.createCell(colIdx);
            dayCell.setCellValue(date.getDayOfMonth());
            dayCell.setCellStyle(headerStyle);

            Cell nameCell = dayNameHeader.createCell(colIdx);
            nameCell.setCellValue(getVietnameseDayOfWeek(date.getDayOfWeek()));
            nameCell.setCellStyle(headerStyle);
        }

        int totalHourCol = dayStartCol + dateRange.size();
        int totalCongCol = totalHourCol + 1;

        Cell totalHourHeader = dayNumberHeader.createCell(totalHourCol);
        totalHourHeader.setCellValue("Tổng giờ làm");
        totalHourHeader.setCellStyle(headerStyle);
        dayNameHeader.createCell(totalHourCol).setCellStyle(headerStyle);

        Cell totalCongHeader = dayNumberHeader.createCell(totalCongCol);
        totalCongHeader.setCellValue("Tổng công");
        totalCongHeader.setCellStyle(headerStyle);
        dayNameHeader.createCell(totalCongCol).setCellStyle(headerStyle);

        Map<String, List<PhieuDiemDanh>> byEmployee = records.stream()
                .collect(Collectors.groupingBy(
                        r -> (r.getMaSinhVien() != null ? r.getMaSinhVien() : "") + "|" + (r.getTenSinhVien() != null ? r.getTenSinhVien() : ""),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int stt = 1;
        for (Map.Entry<String, List<PhieuDiemDanh>> e : byEmployee.entrySet()) {
            List<PhieuDiemDanh> employeeRecords = e.getValue();
            if (employeeRecords.isEmpty()) {
                continue;
            }
            PhieuDiemDanh first = employeeRecords.get(0);
            double totalHours = employeeRecords.stream().mapToDouble(this::calculateHours).sum();
            double totalCong = round(totalHours / 8.0, 3);
            Map<LocalDate, Double> hourByDate = employeeRecords.stream()
                    .filter(p -> p != null && p.getNgay() != null)
                    .collect(Collectors.groupingBy(
                            PhieuDiemDanh::getNgay,
                            LinkedHashMap::new,
                            Collectors.summingDouble(this::calculateHours)
                    ));

            Row r = sheet.createRow(rowIdx++);
            r.createCell(0).setCellValue(stt++);
            r.createCell(1).setCellValue(nullToEmpty(first.getMaSinhVien()));
            r.createCell(2).setCellValue(nullToEmpty(first.getTenSinhVien()));

            for (int i = 0; i < dateRange.size(); i++) {
                LocalDate d = dateRange.get(i);
                Double h = hourByDate.get(d);
                if (h == null) {
                    r.createCell(dayStartCol + i).setCellValue("");
                    continue;
                }
                double rounded = round(h, 2);
                if (rounded > 0) {
                    r.createCell(dayStartCol + i).setCellValue("X(" + formatDecimal(rounded) + ")");
                } else {
                    r.createCell(dayStartCol + i).setCellValue("X");
                }
            }
            r.createCell(totalHourCol).setCellValue(round(totalHours, 2));
            r.createCell(totalCongCol).setCellValue(totalCong);
        }

        for (int i = 0; i <= totalCongCol; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void buildDetailSheet(Workbook workbook, List<PhieuDiemDanh> records, LocalDate startDate, LocalDate endDate) {
        Sheet sheet = workbook.createSheet("Chi tiết");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowIdx = 0;
        Row titleRow = sheet.createRow(rowIdx++);
        titleRow.createCell(0).setCellValue("BẢNG KÊ CHI TIẾT KẾT QUẢ CHẤM CÔNG");

        Row dateRow = sheet.createRow(rowIdx++);
        dateRow.createCell(0).setCellValue("Từ ngày " + startDate + " đến ngày " + endDate);

        rowIdx++;
        Row header = sheet.createRow(rowIdx++);
        String[] headers = {"STT", "Mã nhân viên", "Họ và tên", "Bộ phận", "Ngày", "Ca", "Giờ vào", "Giờ ra", "Số giờ", "Công"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        Map<String, List<PhieuDiemDanh>> byDept = records.stream()
                .collect(Collectors.groupingBy(
                        r -> resolveDepartment(r.getMaPhongBan()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int stt = 1;
        for (Map.Entry<String, List<PhieuDiemDanh>> deptEntry : byDept.entrySet()) {
            Row deptRow = sheet.createRow(rowIdx++);
            deptRow.createCell(0).setCellValue(deptEntry.getKey());

            List<PhieuDiemDanh> deptRecords = deptEntry.getValue();
            for (PhieuDiemDanh p : deptRecords) {
                Row r = sheet.createRow(rowIdx++);
                double soGio = round(calculateHours(p), 2);
                double cong = round(soGio / 8.0, 3);
                r.createCell(0).setCellValue(stt++);
                r.createCell(1).setCellValue(nullToEmpty(p.getMaSinhVien()));
                r.createCell(2).setCellValue(nullToEmpty(p.getTenSinhVien()));
                r.createCell(3).setCellValue(resolveDepartment(p.getMaPhongBan()));
                r.createCell(4).setCellValue(p.getNgay() != null ? p.getNgay().toString() : "");
                r.createCell(5).setCellValue(p.getCa() != null ? p.getCa() : 0);
                r.createCell(6).setCellValue(p.getGioVao() != null ? p.getGioVao().toString() : "");
                r.createCell(7).setCellValue(p.getGioRa() != null ? p.getGioRa().toString() : "");
                r.createCell(8).setCellValue(soGio);
                r.createCell(9).setCellValue(cong);
            }
            rowIdx++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String resolveDepartment(String maPhongBan) {
        String normalized = nullToEmpty(maPhongBan).trim();
        return normalized.isEmpty() ? "Phòng ban không xác định" : normalized;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private double calculateHours(PhieuDiemDanh p) {
        if (p == null || p.getGioVao() == null || p.getGioRa() == null) {
            return 0.0;
        }
        Duration d = Duration.between(p.getGioVao(), p.getGioRa());
        long minutes = d.toMinutes();
        if (minutes < 0) {
            minutes += 24 * 60; // ca qua ngay
        }
        return minutes / 60.0;
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatDecimal(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private List<LocalDate> buildDateRange(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            dates.add(d);
            d = d.plusDays(1);
        }
        return dates;
    }

    private String getVietnameseDayOfWeek(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return "Thứ hai";
            case TUESDAY:
                return "Thứ ba";
            case WEDNESDAY:
                return "Thứ tư";
            case THURSDAY:
                return "Thứ năm";
            case FRIDAY:
                return "Thứ sáu";
            case SATURDAY:
                return "Thứ bảy";
            case SUNDAY:
                return "Chủ nhật";
            default:
                return "";
        }
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> splitCsvToList(String value) {
        String normalized = normalizeString(value);
        if (normalized == null) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private PhieuDiemDanh.TrangThai parseTinhTrang(String value) {
        String normalized = normalizeString(value);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase();
        if ("DUNG_GIO".equals(upper) || "DUNGGIO".equals(upper) || "DUNG GIO".equals(upper) || "DUNG-GIO".equals(upper)) {
            return PhieuDiemDanh.TrangThai.DUNG_GIO;
        }
        if ("MUON".equals(upper)) {
            return PhieuDiemDanh.TrangThai.MUON;
        }
        throw new IllegalArgumentException("Giá trị tình trạng không hợp lệ: " + value);
    }

    private PhieuDiemDanh.TrangThaiHoc parseTrangThaiHoc(String value) {
        String normalized = normalizeString(value);
        if (normalized == null) {
            return null;
        }
        try {
            return PhieuDiemDanh.TrangThaiHoc.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Giá trị trạng thái không hợp lệ: " + value);
        }
    }
    
    public PhieuDiemDanh processRfidAttendance(String rfid){
        return processRfidAttendanceAndCollect(rfid).lastRecord;
    }

    private Integer resolveShiftContaining(List<CaLam> shifts, LocalTime now) {
        if (shifts == null || now == null) return null;
        for (CaLam shift : shifts) {
            if (shift == null || shift.getMaCa() == null || shift.getGioBatDau() == null || shift.getGioKetThuc() == null) {
                continue;
            }
            if (isNowInShiftWindow(now, shift.getGioBatDau(), shift.getGioKetThuc())) {
                return shift.getMaCa();
            }
        }
        return null;
    }

    private Integer resolveNextShift(List<CaLam> shifts, LocalTime now) {
        if (shifts == null || shifts.isEmpty() || now == null) return null;
        CaLam best = null;
        long minMinutes = Long.MAX_VALUE;
        for (CaLam shift : shifts) {
            if (shift == null || shift.getMaCa() == null || shift.getGioBatDau() == null) continue;
            long minutes = minutesUntilNextOccurrence(now, shift.getGioBatDau());
            if (minutes < minMinutes) {
                minMinutes = minutes;
                best = shift;
            }
        }
        return best != null ? best.getMaCa() : null;
    }

    private Integer resolvePreviousShift(List<CaLam> shifts, LocalTime now) {
        if (shifts == null || shifts.isEmpty() || now == null) return null;
        CaLam best = null;
        long minMinutesFromEnd = Long.MAX_VALUE;
        for (CaLam shift : shifts) {
            if (shift == null || shift.getMaCa() == null || shift.getGioKetThuc() == null) continue;

            // Nếu ca qua đêm: trong mode "same-day" không chọn làm previous shift (tránh kết thúc sai ngày)
            if (shift.getGioBatDau() != null && shift.getGioKetThuc() != null && !shift.getGioKetThuc().isAfter(shift.getGioBatDau())) {
                continue;
            }

            LocalTime end = shift.getGioKetThuc();
            if (end.isAfter(now)) continue;
            long diff = Duration.between(end, now).toMinutes();
            if (diff >= 0 && diff < minMinutesFromEnd) {
                minMinutesFromEnd = diff;
                best = shift;
            }
        }
        return best != null ? best.getMaCa() : null;
    }

    private PhieuDiemDanh splitAttendanceAcrossShiftsSameDay(
            PhieuDiemDanh openRecord,
            SinhVien sinhVien,
            LocalDate day,
            LocalTime checkoutTime,
            Integer checkoutCa,
            List<CaLam> shifts
    ) {
        if (openRecord == null || openRecord.getNgay() == null || openRecord.getGioVao() == null || openRecord.getCa() == null) {
            throw new IllegalStateException("Bản ghi vào không hợp lệ");
        }
        if (day == null || checkoutTime == null) {
            throw new IllegalStateException("Thời điểm checkout không hợp lệ");
        }
        if (!day.equals(openRecord.getNgay())) {
            // Mode mới: chỉ xử lý trong ngày
            throw new IllegalStateException("Mode same-day: bản ghi vào không thuộc ngày hiện tại");
        }

        LocalDateTime start = LocalDateTime.of(day, openRecord.getGioVao());
        LocalDateTime end = LocalDateTime.of(day, checkoutTime);
        if (end.isBefore(start)) {
            throw new IllegalStateException("Checkout nhỏ hơn checkin trong cùng ngày");
        }

        // Build shift slots trong đúng ngày (bỏ ca qua đêm)
        List<ShiftSlot> slots = new ArrayList<>();
        for (CaLam shift : shifts) {
            if (shift == null || shift.getMaCa() == null || shift.getGioBatDau() == null || shift.getGioKetThuc() == null) {
                continue;
            }
            LocalDateTime sStart = LocalDateTime.of(day, shift.getGioBatDau());
            LocalDateTime sEnd = LocalDateTime.of(day, shift.getGioKetThuc());
            if (!sEnd.isAfter(sStart)) {
                continue;
            }
            slots.add(new ShiftSlot(shift.getMaCa(), sStart, sEnd));
        }
        slots.sort(Comparator.comparing(slot -> slot.start));

        List<ShiftSlot> covered = slots.stream()
                .filter(slot -> start.isBefore(slot.end) && end.isAfter(slot.start))
                .collect(Collectors.toList());

        // Không giao ca nào -> kết thúc record gốc theo ca gần nhất
        if (covered.isEmpty()) {
            openRecord.setGioRa(checkoutTime);
            openRecord.setTinhTrangDiemDanh(determineAttendanceStatus(openRecord.getGioVao(), openRecord.getCa()));
            openRecord.setTrangThai(determineCheckoutStatus(checkoutTime, checkoutCa));
            return phieuDiemDanhRepository.save(openRecord);
        }

        PhieuDiemDanh lastSaved = null;
        for (ShiftSlot slot : covered) {
            LocalDateTime segStart = start.isAfter(slot.start) ? start : slot.start;
            LocalDateTime segEnd = end.isBefore(slot.end) ? end : slot.end;
            if (!segEnd.isAfter(segStart)) continue;

            Integer segCa = slot.maCa;

            PhieuDiemDanh record;
            if (segCa.equals(openRecord.getCa())) {
                record = openRecord;
            } else {
                Optional<PhieuDiemDanh> existing = phieuDiemDanhRepository.findByRfidAndNgayAndCa(openRecord.getRfid(), day, segCa);
                record = existing.orElseGet(PhieuDiemDanh::new);
            }

            record.setRfid(openRecord.getRfid());
            record.setMaSinhVien(openRecord.getMaSinhVien() != null ? openRecord.getMaSinhVien() : sinhVien.getMaSinhVien());
            record.setTenSinhVien(openRecord.getTenSinhVien() != null ? openRecord.getTenSinhVien() : sinhVien.getTenSinhVien());
            record.setMaPhongBan(openRecord.getMaPhongBan() != null ? openRecord.getMaPhongBan() : sinhVien.getMaPhongBan());
            record.setPhongHoc(openRecord.getPhongHoc());
            record.setNgay(day);
            record.setCa(segCa);
            record.setGioVao(segStart.toLocalTime());
            record.setGioRa(segEnd.toLocalTime());
            record.setTinhTrangDiemDanh(determineAttendanceStatus(record.getGioVao(), segCa));
            record.setTrangThai(determineCheckoutStatus(record.getGioRa(), segCa));

            lastSaved = phieuDiemDanhRepository.save(record);
        }

        if (lastSaved == null) {
            throw new IllegalStateException("Không thể kết thúc điểm danh theo ca trong ngày");
        }

        if (checkoutCa != null && checkoutCa != 0) {
            lastSaved.setCa(checkoutCa);
        }
        return lastSaved;
    }

    public PhieuDiemDanh processRfidAttendanceWithDevice(String rfid, String maThietBi) {
        PhieuDiemDanh record = processRfidAttendance(rfid);
        if (record.getRfid() == null) {
            attachDeviceContextToUnregisteredRfid(rfid, maThietBi);
            // RFID không tồn tại, publish event invalid-rfid
            socketIOServer.getAllClients().forEach(client -> {
                String message = null;
                try {
                    message = objectMapper.writeValueAsString(rfid);
                } catch (JsonProcessingException e) {
                    System.out.println("error convert RFID to JSON");
                }
                System.out.println("publishing invalid-rfid event: " + message);
                client.sendEvent("invalid-rfid", message);
            });
            return record;
        }
        if (maThietBi != null && !maThietBi.isEmpty() && record.getCa() != -99) {
            Optional<ThietBi> tb = thietBiRepository.findById(maThietBi);
            tb.ifPresent(thietBi -> {
                record.setPhongHoc(thietBi.getPhongHoc());
                phieuDiemDanhRepository.save(record);
            });
        }
        //publish event
        socketIOServer.getAllClients().forEach(client ->{
            String message = null;
            try {
                message = objectMapper.writeValueAsString(record);
            } catch (JsonProcessingException e) {
                System.out.println("error convert object");
            }
            System.out.println("publishing event " + message);
            client.sendEvent("update-attendance",message);
        });
        return record;
    }

    /**
     * Chấm công khuôn mặt dựa trên mã sinh viên do Python nhận diện trả về.
     * Luồng nghiệp vụ tương tự RFID: nếu có bản ghi "mở" (gioRa == null) thì coi là check-out,
     * nếu không thì tạo bản ghi mới (check-in).
     */
    public PhieuDiemDanh processFaceAttendanceWithDevice(String maSinhVien, String maThietBi) {
        if (maSinhVien == null || maSinhVien.trim().isEmpty()) {
            PhieuDiemDanh response = new PhieuDiemDanh();
            // publish invalid-face event
            socketIOServer.getAllClients().forEach(client -> {
                String message = null;
                try {
                    message = objectMapper.writeValueAsString("");
                } catch (JsonProcessingException e) {
                    System.out.println("error convert face payload to JSON");
                }
                client.sendEvent("invalid-face", message);
            });
            return response;
        }

        String normalizedMaSinhVien = maSinhVien.trim();

        Optional<SinhVien> sinhVienOpt = sinhVienRepository.findByMaSinhVien(normalizedMaSinhVien);
        if (!sinhVienOpt.isPresent()) {
            PhieuDiemDanh response = new PhieuDiemDanh();
            // publish invalid-face event
            socketIOServer.getAllClients().forEach(client -> {
                String message = null;
                try {
                    message = objectMapper.writeValueAsString(
                            java.util.Map.of(
                                    "maSinhVien", normalizedMaSinhVien
                            )
                    );
                } catch (JsonProcessingException e) {
                    System.out.println("error convert face payload to JSON");
                }
                client.sendEvent("invalid-face", message);
            });
            return response;
        }

        SinhVien sinhVien = sinhVienOpt.get();

        // Mode mới: chỉ xử lý trong NGÀY HIỆN TẠI (tương tự RFID)
        LocalDate today = LocalDate.now(APP_ZONE_ID);
        LocalTime now = LocalTime.now(APP_ZONE_ID);

        List<CaLam> shifts = caLamRepository.findAllByOrderByMaCaAsc();
        if (shifts == null || shifts.isEmpty()) {
            throw new IllegalStateException("Chưa cấu hình ca làm");
        }

        List<PhieuDiemDanh> todayRecords = phieuDiemDanhRepository
                .findByMaSinhVienAndNgayOrderByCaAscCreatedAtAsc(normalizedMaSinhVien, today);

        PhieuDiemDanh open = todayRecords.stream()
                .filter(r -> r != null && r.getGioRa() == null)
                .reduce((a, b) -> b)
                .orElse(null);

        Integer inShiftCa = resolveShiftContaining(shifts, now);
        Integer nextShiftCa = resolveNextShift(shifts, now);
        Integer prevShiftCa = resolvePreviousShift(shifts, now);

        PhieuDiemDanh result;
        if (open == null) {
            // check-in
            Integer targetCa = (inShiftCa != null) ? inShiftCa : nextShiftCa;
            if (targetCa == null) {
                throw new IllegalStateException("Không xác định được ca làm phù hợp");
            }

            boolean completedSameShift = todayRecords.stream()
                    .anyMatch(r -> r != null
                            && r.getCa() != null
                            && r.getCa().equals(targetCa)
                            && r.getGioVao() != null
                            && r.getGioRa() != null);
            if (completedSameShift) {
                PhieuDiemDanh response = new PhieuDiemDanh();
                response.setRfid("FACE:" + normalizedMaSinhVien);
                response.setTenSinhVien(sinhVien.getTenSinhVien());
                response.setCa(-99);
                result = response;
            } else {
                PhieuDiemDanh.TrangThai tinhTrangDiemDanh = determineAttendanceStatus(now, targetCa);
                String faceSyntheticRfid = "FACE:" + sinhVien.getMaSinhVien();

                PhieuDiemDanh newRecord = new PhieuDiemDanh();
                newRecord.setRfid(faceSyntheticRfid);
                newRecord.setMaSinhVien(sinhVien.getMaSinhVien());
                newRecord.setTenSinhVien(sinhVien.getTenSinhVien());
                newRecord.setMaPhongBan(sinhVien.getMaPhongBan());
                newRecord.setGioVao(now);
                newRecord.setNgay(today);
                newRecord.setCa(targetCa);
                newRecord.setTinhTrangDiemDanh(tinhTrangDiemDanh);
                newRecord.setTrangThai(PhieuDiemDanh.TrangThaiHoc.DANG_HOC);
                result = phieuDiemDanhRepository.save(newRecord);
            }
        } else {
            // check-out
            Integer checkoutCa = (inShiftCa != null) ? inShiftCa : (prevShiftCa != null ? prevShiftCa : open.getCa());
            if (checkoutCa == null) checkoutCa = open.getCa();
            result = splitAttendanceAcrossShiftsSameDay(open, sinhVien, today, now, checkoutCa, shifts);
        }

        if (maThietBi != null && !maThietBi.isEmpty() && result.getCa() != -99) {
            Optional<ThietBi> tb = thietBiRepository.findById(maThietBi);
            tb.ifPresent(thietBi -> {
                result.setPhongHoc(thietBi.getPhongHoc());
                phieuDiemDanhRepository.save(result);
            });
        }

        socketIOServer.getAllClients().forEach(client -> {
            String message = null;
            try {
                message = objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException e) {
                System.out.println("error convert object");
            }
            client.sendEvent("update-attendance", message);
        });

        return result;
    }
    
    private Integer getCurrentCa() {
        LocalTime now = LocalTime.now(APP_ZONE_ID);
        List<CaLam> shifts = caLamRepository.findAllByOrderByMaCaAsc();
        if (shifts == null || shifts.isEmpty()) {
            return 0;
        }

        for (CaLam shift : shifts) {
            if (shift == null || shift.getGioBatDau() == null || shift.getGioKetThuc() == null || shift.getMaCa() == null) {
                continue;
            }

            LocalTime startWindow = shift.getGioBatDau();
            LocalTime endWindow = shift.getGioKetThuc();
            if (isNowInShiftWindow(now, startWindow, endWindow)) {
                return shift.getMaCa();
            }
        }

        // Không thuộc window ca nào:
        // chọn ca có thời điểm bắt đầu kế tiếp gần nhất (kể cả rollover sang ngày hôm sau).
        CaLam nearestNextShift = null;
        long minMinutesUntilStart = Long.MAX_VALUE;

        for (CaLam shift : shifts) {
            if (shift == null || shift.getMaCa() == null || shift.getGioBatDau() == null) {
                continue;
            }
            long minutesUntilStart = minutesUntilNextOccurrence(now, shift.getGioBatDau());
            if (minutesUntilStart < minMinutesUntilStart) {
                minMinutesUntilStart = minutesUntilStart;
                nearestNextShift = shift;
            }
        }

        if (nearestNextShift != null) {
            return nearestNextShift.getMaCa();
        }

        // Không xác định được ca phù hợp
        return 0;
    }

    private boolean isNowInShiftWindow(LocalTime now, LocalTime startWindow, LocalTime endWindow) {
        // Ca bình thường trong ngày.
        if (endWindow.isAfter(startWindow)) {
            return !now.isBefore(startWindow) && !now.isAfter(endWindow);
        }
        // Ca qua đêm (ví dụ 22:00 -> 06:00): in-window nếu >= start hoặc <= end.
        return !now.isBefore(startWindow) || !now.isAfter(endWindow);
    }

    private long minutesUntilNextOccurrence(LocalTime now, LocalTime target) {
        int nowMinutes = now.getHour() * 60 + now.getMinute();
        int targetMinutes = target.getHour() * 60 + target.getMinute();
        if (targetMinutes >= nowMinutes) {
            return targetMinutes - nowMinutes;
        }
        return (24 * 60 - nowMinutes) + targetMinutes;
    }
    
    private PhieuDiemDanh.TrangThai determineAttendanceStatus(LocalTime currentTime, Integer ca) {
        if (ca == null) {
            throw new IllegalStateException("Không xác định ca làm");
        }

        // Dựa 100% vào cấu hình ca làm từ DB (không hard-code fallback)
        Optional<CaLam> shiftOpt = caLamRepository.findByMaCa(ca);
        if (shiftOpt.isPresent()) {
            CaLam shift = shiftOpt.get();
            if (shift.getGioBatDau() != null) {

                // Cho phép điểm danh muộn trong giới hạn <= start + allowance
                if (!currentTime.isAfter(shift.getGioBatDau())) {
                    return PhieuDiemDanh.TrangThai.DUNG_GIO;
                }
                return PhieuDiemDanh.TrangThai.MUON;
            }
        }
        // Không tìm thấy ca làm phù hợp -> không chấm trạng thái theo hard-code
        throw new IllegalStateException("Không tìm thấy cấu hình ca làm cho maCa=" + ca);
    }
    
    private PhieuDiemDanh.TrangThaiHoc determineCheckoutStatus(LocalTime checkoutTime, Integer ca) {
        if (ca == null) {
            throw new IllegalStateException("Không xác định ca làm");
        }

        // Dựa 100% vào cấu hình ca làm từ DB (không hard-code fallback)
        Optional<CaLam> shiftOpt = caLamRepository.findByMaCa(ca);
        if (shiftOpt.isPresent()) {
            CaLam shift = shiftOpt.get();
            if (shift.getGioKetThuc() != null) {
                // Giữ nguyên quy tắc cũ: ra về sớm nếu trước (end - 20 phút)
               
                LocalTime earlyLeaveThreshold = shift.getGioKetThuc();
                if (checkoutTime.isBefore(earlyLeaveThreshold)) {
                    return PhieuDiemDanh.TrangThaiHoc.RA_VE_SOM;
                }
                return PhieuDiemDanh.TrangThaiHoc.DA_RA_VE;
            }
        }
        // Không tìm thấy ca làm phù hợp -> không chấm trạng thái theo hard-code
        throw new IllegalStateException("Không tìm thấy cấu hình ca làm cho maCa=" + ca);
    }

    private List<PhieuDiemDanh> findOpenRecordsForRfid(String rfid) {
        if (rfid == null || rfid.isBlank()) {
            return List.of();
        }
        return phieuDiemDanhRepository.findByRfid(rfid).stream()
                .filter(r -> r != null && r.getGioRa() == null)
                .collect(Collectors.toList());
    }

    private List<PhieuDiemDanh> findOpenRecordsForMaSinhVien(String maSinhVien) {
        if (maSinhVien == null || maSinhVien.isBlank()) {
            return List.of();
        }
        return phieuDiemDanhRepository.findByMaSinhVien(maSinhVien).stream()
                .filter(r -> r != null && r.getGioRa() == null)
                .collect(Collectors.toList());
    }

    private PhieuDiemDanh splitAttendanceAcrossShifts(
            List<PhieuDiemDanh> openRecords,
            SinhVien sinhVien,
            LocalDate checkoutDate,
            LocalTime checkoutTime,
            Integer checkoutCa
    ) {
        if (openRecords == null || openRecords.isEmpty()) {
            throw new IllegalStateException("Không có bản ghi mở để xử lý");
        }
        if (checkoutDate == null || checkoutTime == null) {
            throw new IllegalStateException("Thời điểm checkout không hợp lệ");
        }

        // Chọn bản ghi mở mới nhất làm mốc bắt đầu phiên làm.
        PhieuDiemDanh startRecord = openRecords.stream()
                .max(Comparator
                        .comparing(PhieuDiemDanh::getNgay, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(r -> r.getGioVao() != null ? r.getGioVao() : LocalTime.MIN))
                .orElse(openRecords.get(0));

        if (startRecord.getNgay() == null || startRecord.getCa() == null) {
            throw new IllegalStateException("Bản ghi mở thiếu ngày/ca");
        }

        CaLam startShift = requireShift(startRecord.getCa());
        LocalTime startClock = startRecord.getGioVao() != null ? startRecord.getGioVao() : startShift.getGioBatDau();
        LocalDateTime startDateTime = LocalDateTime.of(startRecord.getNgay(), startClock);
        LocalDateTime endDateTime = LocalDateTime.of(checkoutDate, checkoutTime);

        if (endDateTime.isBefore(startDateTime)) {
            throw new IllegalStateException("Thời điểm checkout nhỏ hơn thời điểm checkin");
        }

        List<CaLam> shifts = caLamRepository.findAll().stream()
                .filter(s -> s != null && s.getMaCa() != null && s.getGioBatDau() != null && s.getGioKetThuc() != null)
                .sorted(Comparator.comparing(CaLam::getGioBatDau))
                .collect(Collectors.toList());

        if (shifts.isEmpty()) {
            throw new IllegalStateException("Không có cấu hình ca làm trong DB");
        }

        List<ShiftSlot> slots = buildShiftSlots(shifts, startDateTime.toLocalDate().minusDays(1), endDateTime.toLocalDate().plusDays(1));
        List<ShiftSlot> coveredSlots = slots.stream()
                .filter(slot -> isIntersected(startDateTime, endDateTime, slot.start, slot.end))
                .sorted(Comparator.comparing(slot -> slot.start))
                .collect(Collectors.toList());

        if (coveredSlots.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy ca làm nào giao với khoảng thời gian làm việc");
        }

        PhieuDiemDanh lastSaved = null;
        for (int i = 0; i < coveredSlots.size(); i++) {
            ShiftSlot slot = coveredSlots.get(i);
            LocalDateTime segmentStart = max(startDateTime, slot.start);
            LocalDateTime segmentEnd = min(endDateTime, slot.end);

            if (!segmentEnd.isAfter(segmentStart)) {
                continue;
            }

            LocalDate segmentNgay = slot.start.toLocalDate();
            Integer segmentCa = slot.maCa;

            PhieuDiemDanh record;
            if (segmentNgay.equals(startRecord.getNgay()) && segmentCa.equals(startRecord.getCa())) {
                record = startRecord;
            } else {
                Optional<PhieuDiemDanh> existing = phieuDiemDanhRepository.findByRfidAndNgayAndCa(
                        startRecord.getRfid(), segmentNgay, segmentCa
                );
                record = existing.orElseGet(PhieuDiemDanh::new);
            }

            record.setRfid(startRecord.getRfid());
            record.setMaSinhVien(startRecord.getMaSinhVien() != null ? startRecord.getMaSinhVien() : sinhVien.getMaSinhVien());
            record.setTenSinhVien(startRecord.getTenSinhVien() != null ? startRecord.getTenSinhVien() : sinhVien.getTenSinhVien());
            record.setMaPhongBan(startRecord.getMaPhongBan() != null ? startRecord.getMaPhongBan() : sinhVien.getMaPhongBan());
            record.setPhongHoc(startRecord.getPhongHoc());
            record.setNgay(segmentNgay);
            record.setCa(segmentCa);
            record.setGioVao(segmentStart.toLocalTime());
            record.setGioRa(segmentEnd.toLocalTime());
            record.setTinhTrangDiemDanh(determineAttendanceStatus(segmentStart.toLocalTime(), segmentCa));
            record.setTrangThai(determineCheckoutStatus(segmentEnd.toLocalTime(), segmentCa));

            lastSaved = phieuDiemDanhRepository.save(record);
        }

        if (lastSaved == null) {
            throw new IllegalStateException("Không thể tạo bản ghi điểm danh liên ca");
        }

        // Đồng bộ ca trả về cho event publish: ưu tiên ca checkout nếu có, fallback ca segment cuối.
        if (checkoutCa != null && checkoutCa != 0) {
            lastSaved.setCa(checkoutCa);
        }
        return lastSaved;
    }

    private CaLam requireShift(Integer maCa) {
        return caLamRepository.findByMaCa(maCa)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy cấu hình ca làm cho maCa=" + maCa));
    }

    private List<ShiftSlot> buildShiftSlots(List<CaLam> shifts, LocalDate fromDate, LocalDate toDate) {
        List<ShiftSlot> slots = new ArrayList<>();
        if (fromDate == null || toDate == null || shifts == null || shifts.isEmpty()) {
            return slots;
        }

        LocalDate d = fromDate;
        while (!d.isAfter(toDate)) {
            for (CaLam shift : shifts) {
                LocalDateTime start = LocalDateTime.of(d, shift.getGioBatDau());
                LocalDateTime end = LocalDateTime.of(d, shift.getGioKetThuc());
                // Hỗ trợ ca qua ngày: end <= start nghĩa là qua nửa đêm.
                if (!end.isAfter(start)) {
                    end = end.plusDays(1);
                }
                slots.add(new ShiftSlot(shift.getMaCa(), start, end));
            }
            d = d.plusDays(1);
        }
        return slots;
    }

    private boolean isIntersected(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    private static class ShiftSlot {
        private final Integer maCa;
        private final LocalDateTime start;
        private final LocalDateTime end;

        private ShiftSlot(Integer maCa, LocalDateTime start, LocalDateTime end) {
            this.maCa = maCa;
            this.start = start;
            this.end = end;
        }
    }
    
    public List<DocRfid> getUnprocessedRfids() {
        return docRfidRepository.findUnprocessedRfids();
    }
    
    public void markRfidAsProcessed(Long id) {
        Optional<DocRfid> docRfidOpt = docRfidRepository.findById(id);
        if (docRfidOpt.isPresent()) {
            DocRfid docRfid = docRfidOpt.get();
            docRfidRepository.delete(docRfid);
        }
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveUnregisteredRfid(String rfid) {
        if (!docRfidRepository.existsByRfid(rfid)) {
            DocRfid d = new DocRfid(rfid);
            docRfidRepository.save(d);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attachDeviceContextToUnregisteredRfid(String rfid, String maThietBi) {
        if (rfid == null || rfid.trim().isEmpty()) {
            return;
        }
        String trimmedRfid = rfid.trim();
        String trimmedDeviceId = maThietBi == null ? null : maThietBi.trim();

        String phongHoc = null;
        if (trimmedDeviceId != null && !trimmedDeviceId.isEmpty()) {
            Optional<ThietBi> tb = thietBiRepository.findById(trimmedDeviceId);
            if (tb.isPresent()) {
                phongHoc = tb.get().getPhongHoc();
            }
        }

        DocRfid doc = docRfidRepository.findByRfid(trimmedRfid).orElseGet(() -> new DocRfid(trimmedRfid));
        doc.setMaThietBi(trimmedDeviceId);
        doc.setPhongHoc(phongHoc);
        docRfidRepository.save(doc);
    }

    /**
     * Cuối ngày: đánh dấu các phiếu chưa có giờ ra thành KHONG_DIEM_DANH_RA.
     * @return số lượng phiếu đã cập nhật
     */
    @Transactional
    public int finalizeMissingCheckoutForDate(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(APP_ZONE_ID);
        List<PhieuDiemDanh> unclosed = phieuDiemDanhRepository.findByNgayAndGioRaIsNull(targetDate);
        int updated = 0;
        for (PhieuDiemDanh record : unclosed) {
            if (record == null) continue;
            record.setTrangThai(PhieuDiemDanh.TrangThaiHoc.KHONG_DIEM_DANH_RA);
            phieuDiemDanhRepository.save(record);
            updated++;
        }
        return updated;
    }
    
    // Getter cho repository để debug
    public SinhVienRepository getSinhVienRepository() {
        return sinhVienRepository;
    }
    
    /**
     * Đồng bộ dữ liệu từ bảng sinhvien sang phieudiemdanh dựa trên mã sinh viên
     * Cập nhật tensinhvien và rfid trong phieudiemdanh từ dữ liệu trong sinhvien
     * 
     * @return Map chứa thống kê kết quả đồng bộ
     */
    @Transactional
    public Map<String, Object> syncStudentInfoFromMaSinhVien() {
        Map<String, Object> result = new java.util.HashMap<>();
        int totalRecords = 0;
        int updatedRecords = 0;
        int notFoundRecords = 0;
        List<String> notFoundMaSinhViens = new java.util.ArrayList<>();
        
        // Lấy tất cả các phiếu điểm danh
        List<PhieuDiemDanh> allAttendance = phieuDiemDanhRepository.findAll();
        totalRecords = allAttendance.size();
        
        System.out.println("=== BẮT ĐẦU ĐỒNG BỘ DỮ LIỆU SINH VIÊN (THEO MÃ SINH VIÊN) ===");
        System.out.println("Tổng số phiếu điểm danh: " + totalRecords);
        
        // Tạo map để cache thông tin sinh viên theo mã sinh viên
        Map<String, SinhVien> sinhVienMap = sinhVienRepository.findAll().stream()
            .collect(Collectors.toMap(
                sv -> sv.getMaSinhVien() != null ? sv.getMaSinhVien().trim() : "",
                Function.identity(),
                (existing, replacement) -> existing
            ));
        
        System.out.println("Tổng số sinh viên trong hệ thống: " + sinhVienMap.size());
        
        // Duyệt qua từng phiếu điểm danh và cập nhật
        for (PhieuDiemDanh attendance : allAttendance) {
            if (attendance.getMaSinhVien() == null || attendance.getMaSinhVien().trim().isEmpty()) {
                notFoundRecords++;
                continue;
            }
            
            String trimmedMaSinhVien = attendance.getMaSinhVien().trim();
            SinhVien sinhVien = sinhVienMap.get(trimmedMaSinhVien);
            
            if (sinhVien != null) {
                // Kiểm tra xem có cần cập nhật không
                boolean needsUpdate = false;
                
                // Cập nhật tên sinh viên nếu khác
                if (attendance.getTenSinhVien() == null || 
                    !sinhVien.getTenSinhVien().equals(attendance.getTenSinhVien())) {
                    attendance.setTenSinhVien(sinhVien.getTenSinhVien());
                    needsUpdate = true;
                }

                String sinhVienMaPhongBan = sinhVien.getMaPhongBan() != null ? sinhVien.getMaPhongBan().trim() : "";
                String attendanceMaPhongBan = attendance.getMaPhongBan() != null ? attendance.getMaPhongBan().trim() : "";
                if (!sinhVienMaPhongBan.equals(attendanceMaPhongBan)) {
                    attendance.setMaPhongBan(sinhVien.getMaPhongBan());
                    needsUpdate = true;
                }
                
                // Cập nhật RFID nếu khác hoặc null
                String sinhVienRfid = sinhVien.getRfid() != null ? sinhVien.getRfid().trim() : "";
                String attendanceRfid = attendance.getRfid() != null ? attendance.getRfid().trim() : "";
                
                if (!sinhVienRfid.equals(attendanceRfid)) {
                    attendance.setRfid(sinhVien.getRfid());
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    phieuDiemDanhRepository.save(attendance);
                    updatedRecords++;
                    System.out.println("Đã cập nhật: Mã SV=" + trimmedMaSinhVien + 
                                     ", Tên: " + attendance.getTenSinhVien() + 
                                     ", RFID: " + attendance.getRfid() +
                                     ", Mã phòng ban: " + attendance.getMaPhongBan());
                }
            } else {
                notFoundRecords++;
                if (!notFoundMaSinhViens.contains(trimmedMaSinhVien)) {
                    notFoundMaSinhViens.add(trimmedMaSinhVien);
                }
                System.out.println("Không tìm thấy sinh viên với mã sinh viên: " + trimmedMaSinhVien);
            }
        }
        
        System.out.println("=== KẾT THÚC ĐỒNG BỘ ===");
        System.out.println("Tổng số bản ghi: " + totalRecords);
        System.out.println("Số bản ghi đã cập nhật: " + updatedRecords);
        System.out.println("Số bản ghi không tìm thấy sinh viên: " + notFoundRecords);
        
        result.put("totalRecords", totalRecords);
        result.put("updatedRecords", updatedRecords);
        result.put("notFoundRecords", notFoundRecords);
        result.put("notFoundMaSinhViens", notFoundMaSinhViens);
        result.put("message", "Đồng bộ hoàn tất. Đã cập nhật " + updatedRecords + " bản ghi.");
        
        return result;
    }
    
    /**
     * Lấy danh sách phiếu điểm danh theo lớp học phần
     * Lấy tất cả ca học của lớp học phần, sau đó lấy các phiếu điểm danh có ca học và ngày học 
     * mà lớp học phần diễn ra và so sánh với danh sách sinh viên của lớp học phần đó
     * 
     * @param maLopHocPhan Mã lớp học phần
     * @return Danh sách phiếu điểm danh của sinh viên trong lớp học phần
     */
    @Transactional(readOnly = true)
    public List<PhieuDiemDanh> getAttendanceByLopHocPhan(String maLopHocPhan) {
        // 1. Lấy thông tin lớp học phần
        Optional<LopHocPhan> lopHocPhanOpt = lopHocPhanRepository.findByMaLopHocPhan(maLopHocPhan);
        if (!lopHocPhanOpt.isPresent()) {
            throw new RuntimeException("Không tìm thấy lớp học phần với mã: " + maLopHocPhan);
        }
        
        LopHocPhan lopHocPhan = lopHocPhanOpt.get();
        String tenLopHocPhan = lopHocPhan.getTenLopHocPhan();
        
        // 2. Lấy tất cả ca học của lớp học phần (ngày và ca)
        List<Object[]> distinctSessions = caHocRepository.findDistinctSessionsByLopHocPhan(tenLopHocPhan);
        
        if (distinctSessions.isEmpty()) {
            // Không có ca học nào, trả về danh sách rỗng
            return new java.util.ArrayList<>();
        }
        
        // 3. Lấy danh sách mã sinh viên trong lớp học phần
        List<SinhVienLopHocPhan> sinhVienLopHocPhans = sinhVienLopHocPhanRepository.findByMaLopHocPhan(maLopHocPhan);
        List<String> maSinhVienList = sinhVienLopHocPhans.stream()
                .map(SinhVienLopHocPhan::getMaSinhVien)
                .collect(Collectors.toList());
        
        if (maSinhVienList.isEmpty()) {
            // Không có sinh viên nào trong lớp, trả về danh sách rỗng
            return new java.util.ArrayList<>();
        }
        
        // 4. Lấy tất cả phiếu điểm danh có ngày và ca trùng với các ca học của lớp
        // Và CHỈ lấy của sinh viên trong lớp học phần (sử dụng query tối ưu với IN clause)
        List<PhieuDiemDanh> allAttendance = new java.util.ArrayList<>();
        
        // Đảm bảo danh sách mã sinh viên không rỗng và không có giá trị null/empty
        List<String> validMaSinhVienList = maSinhVienList.stream()
                .filter(msv -> msv != null && !msv.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        
        if (validMaSinhVienList.isEmpty()) {
            // Không có sinh viên hợp lệ trong lớp, trả về danh sách rỗng
            return new java.util.ArrayList<>();
        }
        
        System.out.println("=== LỌC PHIẾU ĐIỂM DANH THEO LỚP HỌC PHẦN ===");
        System.out.println("Mã lớp học phần: " + maLopHocPhan);
        System.out.println("Tên lớp học phần: " + tenLopHocPhan);
        System.out.println("Số sinh viên trong lớp: " + validMaSinhVienList.size());
        System.out.println("Số ca học: " + distinctSessions.size());
        
        for (Object[] session : distinctSessions) {
            LocalDate ngayHoc = (LocalDate) session[0];
            Integer ca = (Integer) session[1];
            
            if (ngayHoc != null && ca != null) {
                // Sử dụng query tối ưu để lấy CHỈ phiếu điểm danh của sinh viên trong lớp
                // Query này sẽ tự động lọc theo maSinhVien IN (danh sách mã sinh viên của lớp)
                List<PhieuDiemDanh> attendanceForSession = phieuDiemDanhRepository.findByNgayAndCaAndMaSinhVienIn(
                        ngayHoc, ca, validMaSinhVienList);
                
                // Đảm bảo tất cả phiếu điểm danh trả về đều có mã sinh viên trong danh sách
                // (Double check để đảm bảo an toàn)
                List<PhieuDiemDanh> verifiedAttendance = attendanceForSession.stream()
                        .filter(att -> att.getMaSinhVien() != null && 
                                      validMaSinhVienList.contains(att.getMaSinhVien().trim()))
                        .collect(Collectors.toList());
                
                System.out.println("Ngày: " + ngayHoc + ", Ca: " + ca + 
                                 " - Tìm thấy " + verifiedAttendance.size() + " phiếu điểm danh");
                
                allAttendance.addAll(verifiedAttendance);
            }
        }
        
        System.out.println("Tổng số phiếu điểm danh: " + allAttendance.size());
        
        // 5. Sắp xếp theo ngày giảm dần, ca tăng dần, thời gian tạo giảm dần
        allAttendance.sort((a, b) -> {
            int dateCompare = b.getNgay().compareTo(a.getNgay());
            if (dateCompare != 0) return dateCompare;
            
            int caCompare = Integer.compare(a.getCa() != null ? a.getCa() : 0, 
                                           b.getCa() != null ? b.getCa() : 0);
            if (caCompare != 0) return caCompare;
            
            // So sánh theo createdAt nếu có
            if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
            return 0;
        });
        
        return allAttendance;
    }
}
