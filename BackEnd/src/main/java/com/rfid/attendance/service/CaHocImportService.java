package com.rfid.attendance.service;

import com.rfid.attendance.entity.CaHoc;
import com.rfid.attendance.entity.LopHocPhan;
import com.rfid.attendance.repository.CaHocRepository;
import com.rfid.attendance.repository.LopHocPhanRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CaHocImportService {

    private final CaHocRepository caHocRepository;
    private final LopHocPhanRepository lopHocPhanRepository;

    public CaHocImportService(CaHocRepository caHocRepository, LopHocPhanRepository lopHocPhanRepository) {
        this.caHocRepository = caHocRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
    }

    public Map<String, Object> importCaHocFromExcel(MultipartFile file) throws Exception {
        Map<String, Object> result = new HashMap<>();
        int totalSheets = 0;
        int totalRows = 0;
        int generatedDays = 0;
        int saved = 0;
        List<CaHoc> toSave = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            totalSheets = workbook.getNumberOfSheets();

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String tenSheet = sheet.getSheetName();

                int firstDataRowIndex = 4; // skip rows 0..3
                for (int r = firstDataRowIndex; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    String colE = getMergedCellValue(sheet, row.getCell(4)); // E
                    String colF = getMergedCellValue(sheet, row.getCell(5)); // F
                    String colG = getMergedCellValue(sheet, row.getCell(6)); // G
                    String colH = getMergedCellValue(sheet, row.getCell(7)); // H
                    String colI = getMergedCellValue(sheet, row.getCell(8)); // I
                    String colJ = getMergedCellValue(sheet, row.getCell(9)); // J
                    String colK = getMergedCellValue(sheet, row.getCell(10)); // K
                    String colL = getMergedCellValue(sheet, row.getCell(11)); // L
                    String colM = getMergedCellValue(sheet, row.getCell(12)); // M

                    // Skip empty lines with no schedule info
                    if (isAllBlank(colE, colF, colG, colH, colI, colJ, colK, colL, colM)) {
                        continue;
                    }

                    totalRows++;

                    // Ensure LopHocPhan exists or create with extra info
                    ensureLopHocPhanExists(nullIfBlank(colE), nullIfBlank(colM), nullIfBlank(colJ), nullIfBlank(colF));

                    Integer thu = parseInteger(colH);
                    LocalDate startDate = parseDate(colK);
                    LocalDate endDate = parseDate(colL);

                    if (thu == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
                        // If date range or thu invalid, skip this row
                        continue;
                    }

                    // Generate all dates in [startDate, endDate] that match thu
                    LocalDate current = startDate;
                    while (!current.isAfter(endDate)) {
                        if (matchesThuVN(current, thu)) {
                            CaHoc caHoc = new CaHoc();
                            caHoc.setTenSheet(tenSheet);
                            caHoc.setLopHocPhan(nullIfBlank(colE));
                            caHoc.setHinhThucHoc(nullIfBlank(colF));
                            caHoc.setSoTietTuan(parseInteger(colG));
                            caHoc.setThu(thu);
                            caHoc.setTietHoc(nullIfBlank(colI));
                            caHoc.setPhongHoc(nullIfBlank(colJ));
                            caHoc.setNgayHoc(current);
                            caHoc.setGiaoVien(nullIfBlank(colM));
                            caHoc.setCa(getCaFromTietHoc(colI));
                            toSave.add(caHoc);
                            generatedDays++;
                        }
                        current = current.plusDays(1);
                    }
                }
            }
        }

        if (!toSave.isEmpty()) {
            saved = caHocRepository.saveAll(toSave).size();
        }

        result.put("message", "Import thành công");
        result.put("totalSheets", totalSheets);
        result.put("count", saved);
        result.put("totalRows", totalRows);
        result.put("generatedDays", generatedDays);
        return result;
    }

    private String getMergedCellValue(Sheet sheet, Cell cell) {
        if (cell == null) return "";
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.isInRange(cell)) {
                Row firstRow = sheet.getRow(region.getFirstRow());
                if (firstRow == null) return "";
                Cell firstCell = firstRow.getCell(region.getFirstColumn());
                return firstCell != null ? getCellAsString(firstCell).trim() : "";
            }
        }
        return getCellAsString(cell).trim();
    }

    private String getCellAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Format date cells into dd/MM/yy text so our parser can handle consistently
                    java.util.Date date = cell.getDateCellValue();
                    java.time.LocalDate ld = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    yield ld.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
                }
                double d = cell.getNumericCellValue();
                if (Math.floor(d) == d) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    private Integer getCaFromTietHoc(String tietHoc) {
        if (tietHoc == null) return null;
        String normalized = tietHoc.replace("→", "->").trim();
        return switch (normalized) {
            case "1->3" -> 1;
            case "4->6" -> 2;
            case "7->9" -> 3;
            case "10->12" -> 4;
            case "13->16" -> 5;
            default -> null;
        };
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String v = value.trim();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
        try {
            return LocalDate.parse(v, formatter);
        } catch (DateTimeParseException e) {
            // Try dd/MM/yyyy as fallback
            try {
                return LocalDate.parse(v, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private Integer parseInteger(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isAllBlank(String... values) {
        if (values == null) return true;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return false;
        }
        return true;
    }

    private String nullIfBlank(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean matchesThuVN(LocalDate date, Integer thuVN) {
        if (thuVN == null) return false;
        // Java DayOfWeek: MON=1 .. SUN=7
        int javaDow = date.getDayOfWeek().getValue(); // 1..7
        int vnThu = javaDow + 1; // MON(1)->2, ... SAT(6)->7, SUN(7)->8
        if (thuVN == 8 && javaDow == 7) return true; // Support CN if ever provided as 8
        return vnThu == thuVN;
    }

    private void ensureLopHocPhanExists(String tenLopHocPhan, String giangVien, String phongHoc, String hinhThucHoc) {
        String baseCode = com.rfid.attendance.util.LopHocPhanCodeUtil.generateMaLopHocPhan(tenLopHocPhan);
        if (tenLopHocPhan == null || tenLopHocPhan.isBlank()) return;
        if (lopHocPhanRepository.existsByMaLopHocPhan(baseCode)) return;
        LopHocPhan lhp = new LopHocPhan(baseCode, tenLopHocPhan);
        lhp.setGiangVien(giangVien);
        lhp.setPhongHoc(phongHoc);
        lhp.setHinhThucHoc(hinhThucHoc);
        lopHocPhanRepository.save(lhp);
    }

    
}


