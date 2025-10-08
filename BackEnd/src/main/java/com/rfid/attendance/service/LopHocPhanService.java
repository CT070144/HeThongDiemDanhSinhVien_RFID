package com.rfid.attendance.service;

import com.rfid.attendance.dto.LopHocPhanDTO;
import com.rfid.attendance.entity.LopHocPhan;
import com.rfid.attendance.entity.SinhVien;
import com.rfid.attendance.entity.SinhVienLopHocPhan;
import com.rfid.attendance.repository.LopHocPhanRepository;
import com.rfid.attendance.repository.SinhVienLopHocPhanRepository;
import com.rfid.attendance.repository.SinhVienRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class LopHocPhanService {
    
    @Autowired
    private LopHocPhanRepository lopHocPhanRepository;
    
    @Autowired
    private SinhVienRepository sinhVienRepository;
    
    @Autowired
    private SinhVienLopHocPhanRepository sinhVienLopHocPhanRepository;
    
    @Autowired
    private SinhVienService sinhVienService;
    
    @Transactional(readOnly = true)
    public List<LopHocPhanDTO> getAllLopHocPhan() {
        List<LopHocPhan> lopHocPhans = lopHocPhanRepository.findAllOrderByTenLopHocPhan();
        List<LopHocPhanDTO> dtos = new ArrayList<>();
        
        for (LopHocPhan lop : lopHocPhans) {
            LopHocPhanDTO dto = new LopHocPhanDTO(
                lop.getMaLopHocPhan(),
                lop.getTenLopHocPhan(),
                lop.getCreatedAt(),
                lop.getUpdatedAt()
            );
            dto.setSoSinhVien(countSinhVienInLopHocPhan(lop.getMaLopHocPhan()));
            dtos.add(dto);
        }
        
        return dtos;
    }
    
    public Optional<LopHocPhan> getLopHocPhanByMaLopHocPhan(String maLopHocPhan) {
        return lopHocPhanRepository.findByMaLopHocPhan(maLopHocPhan);
    }
    
    @Transactional(readOnly = true)
    public List<LopHocPhanDTO> searchLopHocPhan(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllLopHocPhan();
        }
        List<LopHocPhan> lopHocPhans = lopHocPhanRepository.findByKeyword(keyword.trim());
        List<LopHocPhanDTO> dtos = new ArrayList<>();
        
        for (LopHocPhan lop : lopHocPhans) {
            LopHocPhanDTO dto = new LopHocPhanDTO(
                lop.getMaLopHocPhan(),
                lop.getTenLopHocPhan(),
                lop.getCreatedAt(),
                lop.getUpdatedAt()
            );
            dto.setSoSinhVien(countSinhVienInLopHocPhan(lop.getMaLopHocPhan()));
            dtos.add(dto);
        }
        
        return dtos;
    }
    
    public LopHocPhan createLopHocPhan(LopHocPhan lopHocPhan) {
        if (lopHocPhanRepository.existsByMaLopHocPhan(lopHocPhan.getMaLopHocPhan())) {
            throw new RuntimeException("Mã lớp học phần đã tồn tại: " + lopHocPhan.getMaLopHocPhan());
        }
        return lopHocPhanRepository.save(lopHocPhan);
    }
    
    public LopHocPhan updateLopHocPhan(String maLopHocPhan, LopHocPhan lopHocPhanDetails) {
        LopHocPhan lopHocPhan = lopHocPhanRepository.findByMaLopHocPhan(maLopHocPhan)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học phần với mã: " + maLopHocPhan));
        
        lopHocPhan.setTenLopHocPhan(lopHocPhanDetails.getTenLopHocPhan());
        return lopHocPhanRepository.save(lopHocPhan);
    }
    
    public void deleteLopHocPhan(String maLopHocPhan) {
        if (!lopHocPhanRepository.existsByMaLopHocPhan(maLopHocPhan)) {
            throw new RuntimeException("Không tìm thấy lớp học phần với mã: " + maLopHocPhan);
        }
        // Xóa các bản ghi liên quan trước
        sinhVienLopHocPhanRepository.deleteByMaLopHocPhan(maLopHocPhan);
        lopHocPhanRepository.deleteById(maLopHocPhan);
    }
    
    @Transactional(readOnly = true)
    public List<SinhVien> getSinhVienByLopHocPhan(String maLopHocPhan) {
        // Sử dụng query trực tiếp để tránh lazy loading issues
        List<SinhVien> sinhViens = sinhVienRepository.findByMaSinhVienIn(
            sinhVienLopHocPhanRepository.findByMaLopHocPhan(maLopHocPhan)
                .stream()
                .map(SinhVienLopHocPhan::getMaSinhVien)
                .collect(java.util.stream.Collectors.toList())
        );
        
        return sinhViens;
    }
    
    @Transactional
    public Map<String, Object> importSinhVienFromExcel(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();
        int totalSheets = 0;
        int totalStudents = 0;
        int totalClasses = 0;
        
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            totalSheets = workbook.getNumberOfSheets();
            
            for (int sheetIndex = 0; sheetIndex < totalSheets; sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();
                
                try {
                    Map<String, Object> sheetResult = processSheet(sheet, sheetName);
                    successes.addAll((List<String>) sheetResult.get("successes"));
                    errors.addAll((List<String>) sheetResult.get("errors"));
                    totalStudents += (Integer) sheetResult.get("studentCount");
                    totalClasses += (Integer) sheetResult.get("classCount");
                } catch (Exception e) {
                    errors.add("Lỗi xử lý sheet '" + sheetName + "': " + e.getMessage());
                }
            }
        }
        
        result.put("successes", successes);
        result.put("errors", errors);
        result.put("totalSheets", totalSheets);
        result.put("totalStudents", totalStudents);
        result.put("totalClasses", totalClasses);
        
        return result;
    }
    
    private Map<String, Object> processSheet(Sheet sheet, String sheetName) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();
        int studentCount = 0;
        int classCount = 0;
        
        try {
            // Đọc tên lớp học phần từ hàng 6 (index 5)
            Row classRow = sheet.getRow(5);
            if (classRow == null || classRow.getCell(0) == null) {
                throw new RuntimeException("Không tìm thấy tên lớp học phần ở hàng 6, cột C");
            }
            
            String tenLopHocPhan = getCellValueAsString(classRow.getCell(0));
            tenLopHocPhan = tenLopHocPhan.replaceFirst("^Lớp:\\s*", "");
            if (tenLopHocPhan.trim().isEmpty()) {
                throw new RuntimeException("Tên lớp học phần không được để trống");
            }
            
            // Tạo mã lớp học phần từ tên
            String maLopHocPhan = generateMaLopHocPhan(tenLopHocPhan);
            
            // Tạo hoặc cập nhật lớp học phần
            LopHocPhan lopHocPhan;
            if (lopHocPhanRepository.existsByMaLopHocPhan(maLopHocPhan)) {
                lopHocPhan = lopHocPhanRepository.findByMaLopHocPhan(maLopHocPhan).orElse(null);
                successes.add("Cập nhật lớp học phần: " + tenLopHocPhan + " (" + maLopHocPhan + ")");
            } else {
                lopHocPhan = new LopHocPhan(maLopHocPhan, tenLopHocPhan);
                lopHocPhan = lopHocPhanRepository.save(lopHocPhan);
                successes.add("Tạo mới lớp học phần: " + tenLopHocPhan + " (" + maLopHocPhan + ")");
                classCount++;
            }
            
            // Xóa các sinh viên cũ trong lớp học phần này
            sinhVienLopHocPhanRepository.deleteByMaLopHocPhan(maLopHocPhan);
            
            // Đọc danh sách sinh viên từ hàng 10 trở đi
            int studentAddedCount = 0;
            for (int rowIndex = 9; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                
                try {
                    // Đọc mã sinh viên từ cột B
                    Cell maSinhVienCell = row.getCell(1);
                    if (maSinhVienCell == null || getCellValueAsString(maSinhVienCell).trim().isEmpty()) {
                        continue; // Bỏ qua hàng trống
                    }
                    
                    String maSinhVien = getCellValueAsString(maSinhVienCell).trim();
                    
                    // Đọc tên sinh viên từ cột C
                    Cell hoCell = row.getCell(2);
                    Cell tenCell = row.getCell(3);

                    String ho = getCellValueAsString(hoCell).trim();
                    String ten = getCellValueAsString(tenCell).trim();

                    String tenSinhVienCell = (ho + " " + ten).trim();
                    if (tenSinhVienCell == null) {
                        errors.add("Dòng " + (rowIndex + 1) + ": Không tìm thấy tên sinh viên");
                        continue;
                    }
                    
                    String tenSinhVien = tenSinhVienCell;
                    if (tenSinhVien.isEmpty()) {
                        errors.add("Dòng " + (rowIndex + 1) + ": Tên sinh viên không được để trống");
                        continue;
                    }
                    
                    // Tạo hoặc cập nhật sinh viên
                    SinhVien sinhVien;
                    Optional<SinhVien> existingSinhVien = sinhVienRepository.findByMaSinhVien(maSinhVien);
                    
                    if (existingSinhVien.isPresent()) {
                        sinhVien = existingSinhVien.get();
                        // Cập nhật tên nếu khác
                        if (!sinhVien.getTenSinhVien().equals(tenSinhVien)) {
                            sinhVien.setTenSinhVien(tenSinhVien);
                            sinhVienRepository.save(sinhVien);
                        }
                    } else {
                        // Tạo sinh viên mới với RFID tạm thời
                        String tempRfid = "TEMP_" + maSinhVien + "_" + System.currentTimeMillis();
                        sinhVien = new SinhVien(maSinhVien, tempRfid, tenSinhVien);
                        sinhVien = sinhVienRepository.save(sinhVien);
                        successes.add("Tạo mới sinh viên: " + tenSinhVien + " (" + maSinhVien + ")");
                        studentCount++;
                    }
                    
                    // Thêm sinh viên vào lớp học phần
                    if (!sinhVienLopHocPhanRepository.existsByMaSinhVienAndMaLopHocPhan(maSinhVien, maLopHocPhan)) {
                        SinhVienLopHocPhan svlhp = new SinhVienLopHocPhan(maSinhVien, maLopHocPhan);
                        sinhVienLopHocPhanRepository.save(svlhp);
                        studentAddedCount++;
                    }
                    
                } catch (Exception e) {
                    errors.add("Dòng " + (rowIndex + 1) + ": " + e.getMessage());
                }
            }
            
            successes.add("Sheet '" + sheetName + "': Thêm " + studentAddedCount + " sinh viên vào lớp " + tenLopHocPhan);
            
        } catch (Exception e) {
            errors.add("Sheet '" + sheetName + "': " + e.getMessage());
        }
        
        result.put("successes", successes);
        result.put("errors", errors);
        result.put("studentCount", studentCount);
        result.put("classCount", classCount);
        
        return result;
    }
    
    private String generateMaLopHocPhan(String tenLopHocPhan) {
        // Tìm số thứ tự từ tên lớp (ví dụ: C701, C702, C703)
        Pattern pattern = Pattern.compile("\\(C(\\d+)\\)");
        Matcher matcher = pattern.matcher(tenLopHocPhan);
        
        String soThuTu = "001";
        if (matcher.find()) {
            soThuTu = matcher.group(1);
            // Đảm bảo có ít nhất 3 chữ số
            while (soThuTu.length() < 3) {
                soThuTu = "0" + soThuTu;
            }
        }
        
        // Tạo mã viết tắt từ tên lớp
        String[] words = tenLopHocPhan.split("\\s+");
        StringBuilder maVietTat = new StringBuilder();
        
        for (String word : words) {
            if (word.equalsIgnoreCase("lớp:") || word.equalsIgnoreCase("phần")) {
                continue;
            }
            if (word.length() > 0 && Character.isLetter(word.charAt(0))) {
                maVietTat.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        
        // Nếu không có từ nào, sử dụng CNPMN làm mặc định
        if (maVietTat.length() == 0) {
            maVietTat.append("CNPMN");
        }
        
        return maVietTat.toString() + "-L" + soThuTu;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    public boolean existsByMaLopHocPhan(String maLopHocPhan) {
        return lopHocPhanRepository.existsByMaLopHocPhan(maLopHocPhan);
    }
    
    public long countSinhVienInLopHocPhan(String maLopHocPhan) {
        return sinhVienLopHocPhanRepository.countByMaLopHocPhan(maLopHocPhan);
    }
}
