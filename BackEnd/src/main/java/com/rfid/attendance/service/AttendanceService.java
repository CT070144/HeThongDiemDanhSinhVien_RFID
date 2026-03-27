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
        // Log thông tin debug
        System.out.println("=== RFID ATTENDANCE DEBUG ===");
        System.out.println("RFID nhận được: '" + rfid + "'");
        System.out.println("Độ dài RFID: " + (rfid != null ? rfid.length() : "null"));
        System.out.println("RFID trimmed: '" + (rfid != null ? rfid.trim() : "null") + "'");
        
        // Trim RFID để tránh lỗi do khoảng trắng
        String trimmedRfid = rfid != null ? rfid.trim() : "";
        if (trimmedRfid.isEmpty()) {
            System.out.println("RFID rỗng hoặc null");
            saveUnregisteredRfid(rfid);
            return new PhieuDiemDanh();
        }
        
        // Kiểm tra sinh viên có tồn tại không
        Optional<SinhVien> sinhVienOpt = sinhVienRepository.findByRfid(trimmedRfid);
        System.out.println("Kết quả tìm kiếm sinh viên: " + (sinhVienOpt.isPresent() ? "Tìm thấy" : "Không tìm thấy"));
        
        if (!sinhVienOpt.isPresent()) {
            System.out.println("Không tìm thấy sinh viên với RFID: " + trimmedRfid);
            
            // Debug: Kiểm tra tất cả RFID trong database
            List<SinhVien> allStudents = sinhVienRepository.findAll();
            System.out.println("Tổng số sinh viên trong DB: " + allStudents.size());
            System.out.println("Danh sách RFID trong DB:");
            for (SinhVien sv : allStudents) {
                System.out.println("- '" + sv.getRfid() + "' (độ dài: " + sv.getRfid().length() + ")");
            }
            
            // Nếu không tồn tại, lưu vào bảng doc_rfid (transaction riêng) và trả lỗi nghiệp vụ
            saveUnregisteredRfid(trimmedRfid);
            PhieuDiemDanh response = new PhieuDiemDanh();
            response.setRfid(null);
            return response;
        }
        
        SinhVien sinhVien = sinhVienOpt.get();
        System.out.println("Tìm thấy sinh viên: " + sinhVien.getTenSinhVien() + " (Mã: " + sinhVien.getMaSinhVien() + ")");
        
        LocalDate today = LocalDate.now(APP_ZONE_ID);
        Integer currentCa = getCurrentCa();
        System.out.println("Ngày hiện tại: " + today + ", Ca hiện tại: " + currentCa);

        LocalTime currentTime = LocalTime.now(APP_ZONE_ID);

        // Ưu tiên xử lý bản ghi "mở" (đã check-in nhưng chưa check-out), kể cả khác ngày/khác ca.
        List<PhieuDiemDanh> openRecords = findOpenRecordsForRfid(trimmedRfid);
        if (openRecords != null && !openRecords.isEmpty()) {
            PhieuDiemDanh result = splitAttendanceAcrossShifts(openRecords, sinhVien, today, currentTime, currentCa);
            return result;
        }

        if (currentCa == 0) {
            System.out.println("Ngoài giờ học");
            throw new RuntimeException("Ngoài giờ học");
        }
        
        // Tìm phiếu điểm danh hiện tại
        Optional<PhieuDiemDanh> existingRecord = phieuDiemDanhRepository
                .findByRfidAndNgayAndCa(trimmedRfid, today, currentCa);
        
        if (existingRecord.isPresent()) {
            System.out.println("tồn tại record");
            // Đã có bản ghi, cập nhật giờ ra
            PhieuDiemDanh record = existingRecord.get();
            if (record.getGioRa() == null) {
                LocalTime checkoutTimeNow = LocalTime.now(APP_ZONE_ID);
                record.setGioRa(checkoutTimeNow);
                if (record.getMaPhongBan() == null || record.getMaPhongBan().isBlank()) {
                    record.setMaPhongBan(sinhVien.getMaPhongBan());
                }
                
                // Xác định trạng thái dựa trên thời gian ra
                PhieuDiemDanh.TrangThaiHoc trangThai = determineCheckoutStatus(checkoutTimeNow, currentCa);
                record.setTrangThai(trangThai);
                
                System.out.println("Sinh viên điểm danh ra lúc: " + checkoutTimeNow + ", Trạng thái: " + trangThai.getDescription());


                PhieuDiemDanh result = phieuDiemDanhRepository.save(record);
                return  result;
            } else {
                System.out.println("Sinh viên đã điểm danh ra trong ca này");
                PhieuDiemDanh response = new PhieuDiemDanh();
                response.setRfid(record.getRfid());
                response.setTenSinhVien(record.getTenSinhVien());
                response.setCa(-99);
                return response;
            }
        } else {
            // Tạo bản ghi mới
            PhieuDiemDanh.TrangThai tinhTrangDiemDanh = determineAttendanceStatus(currentTime, currentCa);
            
            PhieuDiemDanh newRecord = new PhieuDiemDanh();
            newRecord.setRfid(trimmedRfid);
            newRecord.setMaSinhVien(sinhVien.getMaSinhVien());
            newRecord.setTenSinhVien(sinhVien.getTenSinhVien());
            newRecord.setMaPhongBan(sinhVien.getMaPhongBan());
            newRecord.setGioVao(currentTime);
            newRecord.setNgay(today);
            newRecord.setCa(currentCa);
            newRecord.setTinhTrangDiemDanh(tinhTrangDiemDanh);
            newRecord.setTrangThai(PhieuDiemDanh.TrangThaiHoc.DANG_HOC); // Mặc định đang học
            
            System.out.println("Tạo phiếu điểm danh mới: " + newRecord.getTenSinhVien() + " - Ca " + currentCa);
            PhieuDiemDanh result = phieuDiemDanhRepository.save(newRecord);
            return result;
        }
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

        LocalDate today = LocalDate.now(APP_ZONE_ID);
        LocalTime currentTime = LocalTime.now(APP_ZONE_ID);
        Integer currentCa = getCurrentCa();

        // Ưu tiên xử lý bản ghi "mở" (đã check-in nhưng chưa check-out), kể cả khác ngày/khác ca.
        List<PhieuDiemDanh> openRecords = findOpenRecordsForMaSinhVien(normalizedMaSinhVien);
        if (openRecords != null && !openRecords.isEmpty()) {
            PhieuDiemDanh result = splitAttendanceAcrossShifts(openRecords, sinhVien, today, currentTime, currentCa);

            if (maThietBi != null && !maThietBi.isEmpty() && result.getCa() != -99) {
                Optional<ThietBi> tb = thietBiRepository.findById(maThietBi);
                tb.ifPresent(thietBi -> {
                    result.setPhongHoc(thietBi.getPhongHoc());
                    phieuDiemDanhRepository.save(result);
                });
            }

            // publish event
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

        if (currentCa == 0) {
            System.out.println("Ngoài giờ học (face)");
            throw new RuntimeException("Ngoài giờ học");
        }

        // Tránh trường hợp đã điểm danh/đã ra về rồi mà ESP32 bấm lại.
        List<PhieuDiemDanh> sameShiftRecords =
                phieuDiemDanhRepository.findByMaSinhVienAndNgayAndCaOrderByCreatedAtDesc(
                        normalizedMaSinhVien, today, currentCa
                );

        if (sameShiftRecords != null && !sameShiftRecords.isEmpty()) {
            PhieuDiemDanh record = sameShiftRecords.get(0);
            if (record.getGioRa() == null) {
                // check-out trong ca hiện tại (về mặt lý thuyết không cần vì openRecords đã xử lý, nhưng giữ an toàn)
                record.setGioRa(currentTime);
                if (record.getMaPhongBan() == null || record.getMaPhongBan().isBlank()) {
                    record.setMaPhongBan(sinhVien.getMaPhongBan());
                }

                PhieuDiemDanh.TrangThaiHoc trangThai = determineCheckoutStatus(currentTime, currentCa);
                record.setTrangThai(trangThai);
                PhieuDiemDanh saved = phieuDiemDanhRepository.save(record);

                if (maThietBi != null && !maThietBi.isEmpty() && saved.getCa() != -99) {
                    Optional<ThietBi> tb = thietBiRepository.findById(maThietBi);
                    tb.ifPresent(thietBi -> {
                        saved.setPhongHoc(thietBi.getPhongHoc());
                        phieuDiemDanhRepository.save(saved);
                    });
                }

                socketIOServer.getAllClients().forEach(client -> {
                    String message = null;
                    try {
                        message = objectMapper.writeValueAsString(saved);
                    } catch (JsonProcessingException e) {
                        System.out.println("error convert object");
                    }
                    client.sendEvent("update-attendance", message);
                });

                return saved;
            } else {
                // Sinh viên đã điểm danh ra trong ca này
                PhieuDiemDanh response = new PhieuDiemDanh();
                response.setRfid(record.getRfid());
                response.setTenSinhVien(record.getTenSinhVien());
                response.setCa(-99);

                socketIOServer.getAllClients().forEach(client -> {
                    String message = null;
                    try {
                        message = objectMapper.writeValueAsString(response);
                    } catch (JsonProcessingException e) {
                        System.out.println("error convert object");
                    }
                    client.sendEvent("update-attendance", message);
                });

                return response;
            }
        }

        // Tạo bản ghi mới (check-in)
        PhieuDiemDanh.TrangThai tinhTrangDiemDanh = determineAttendanceStatus(currentTime, currentCa);
        String faceSyntheticRfid = "FACE:" + sinhVien.getMaSinhVien();

        PhieuDiemDanh newRecord = new PhieuDiemDanh();
        newRecord.setRfid(faceSyntheticRfid);
        newRecord.setMaSinhVien(sinhVien.getMaSinhVien());
        newRecord.setTenSinhVien(sinhVien.getTenSinhVien());
        newRecord.setMaPhongBan(sinhVien.getMaPhongBan());
        newRecord.setGioVao(currentTime);
        newRecord.setNgay(today);
        newRecord.setCa(currentCa);
        newRecord.setTinhTrangDiemDanh(tinhTrangDiemDanh);
        newRecord.setTrangThai(PhieuDiemDanh.TrangThaiHoc.DANG_HOC); // Mặc định đang học

        PhieuDiemDanh result = phieuDiemDanhRepository.save(newRecord);

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
                int earlyLeaveBuffer = 20;
                LocalTime earlyLeaveThreshold = shift.getGioKetThuc().minusMinutes(earlyLeaveBuffer);
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
