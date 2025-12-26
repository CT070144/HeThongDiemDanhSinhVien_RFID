package com.rfid.attendance.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rfid.attendance.entity.*;
import com.rfid.attendance.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@Transactional
public class AttendanceService {
    
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
    ObjectMapper objectMapper;
    @Autowired
    private SocketIOServer socketIOServer;
    
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
        
        LocalDate today = LocalDate.now();
        Integer currentCa = getCurrentCa();
        System.out.println("Ngày hiện tại: " + today + ", Ca hiện tại: " + currentCa);
        
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
                LocalTime currentTime = LocalTime.now();
                record.setGioRa(currentTime);
                
                // Xác định trạng thái dựa trên thời gian ra
                PhieuDiemDanh.TrangThaiHoc trangThai = determineCheckoutStatus(currentTime, currentCa);
                record.setTrangThai(trangThai);
                
                System.out.println("Sinh viên điểm danh ra lúc: " + currentTime + ", Trạng thái: " + trangThai.getDescription());


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
            LocalTime currentTime = LocalTime.now();
            PhieuDiemDanh.TrangThai tinhTrangDiemDanh = determineAttendanceStatus(currentTime, currentCa);
            
            PhieuDiemDanh newRecord = new PhieuDiemDanh();
            newRecord.setRfid(trimmedRfid);
            newRecord.setMaSinhVien(sinhVien.getMaSinhVien());
            newRecord.setTenSinhVien(sinhVien.getTenSinhVien());
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
    
    private Integer getCurrentCa() {
        LocalTime currentTime = LocalTime.now();
        
        // Ca 1: 7h - 9h25 (có thể điểm danh từ 6h50 - 9h35)
        if (currentTime.isAfter(LocalTime.of(00, 10)) && currentTime.isBefore(LocalTime.of(9, 35))) {
            return 1;
        }
        // Ca 2: 9h35 - 12h (có thể điểm danh từ 9h25 - 12h10)
        else if (currentTime.isAfter(LocalTime.of(9, 25)) && currentTime.isBefore(LocalTime.of(12, 30))) {
            return 2;
        }
        // Ca 3: 12h30 - 14h55 (có thể điểm danh từ 12h20 - 15h5)
        else if (currentTime.isAfter(LocalTime.of(12, 20)) && currentTime.isBefore(LocalTime.of(15, 5))) {
            return 3;
        }
        // Ca 4: 15h05 - 17h30 (có thể điểm danh từ 14h55 - 17h40)
        else if (currentTime.isAfter(LocalTime.of(14, 55)) && currentTime.isBefore(LocalTime.of(17, 40))) {
            return 4;
        }
        // Ca 5: 18h - 20h30 (có thể điểm danh từ 17h50 - 20h40)
        else if (currentTime.isAfter(LocalTime.of(17, 50)) && currentTime.isBefore(LocalTime.of(20, 30))) {
            return 5;
        }
        else {
            // Ngoài giờ học
            return 0;
        }
    }
    
    private PhieuDiemDanh.TrangThai determineAttendanceStatus(LocalTime currentTime, Integer ca) {
        switch (ca) {
            case 1:
                // Ca 1: 7h - 9h25, đúng giờ nếu trước 7h, muộn nếu từ 7h trở đi
                if (currentTime.isBefore(LocalTime.of(7, 0))) {
                    return PhieuDiemDanh.TrangThai.DUNG_GIO;
                } else {
                    return PhieuDiemDanh.TrangThai.MUON;
                }
            case 2:
                // Ca 2: 9h35 - 12h, đúng giờ nếu trước 9h35, muộn nếu từ 9h35 trở đi
                if (currentTime.isBefore(LocalTime.of(9, 35))) {
                    return PhieuDiemDanh.TrangThai.DUNG_GIO;
                } else {
                    return PhieuDiemDanh.TrangThai.MUON;
                }
            case 3:
                // Ca 3: 12h30 - 14h55, đúng giờ nếu trước 12h30, muộn nếu từ 12h30 trở đi
                if (currentTime.isBefore(LocalTime.of(12, 30))) {
                    return PhieuDiemDanh.TrangThai.DUNG_GIO;
                } else {
                    return PhieuDiemDanh.TrangThai.MUON;
                }
            case 4:
                // Ca 4: 15h05 - 17h30, đúng giờ nếu trước 15h05, muộn nếu từ 15h05 trở đi
                if (currentTime.isBefore(LocalTime.of(15, 5))) {
                    return PhieuDiemDanh.TrangThai.DUNG_GIO;
                } else {
                    return PhieuDiemDanh.TrangThai.MUON;
                }
            case 5:
                // Ca 5: 18h - 20h30, đúng giờ nếu trước 18h, muộn nếu từ 18h trở đi
                if (currentTime.isBefore(LocalTime.of(18, 0))) {
                    return PhieuDiemDanh.TrangThai.DUNG_GIO;
                } else {
                    return PhieuDiemDanh.TrangThai.MUON;
                }
            default:
                return PhieuDiemDanh.TrangThai.DUNG_GIO;
        }
    }
    
    private PhieuDiemDanh.TrangThaiHoc determineCheckoutStatus(LocalTime checkoutTime, Integer ca) {
        switch (ca) {
            case 1:
                // Ca 1: 7h - 9h25, ra về sớm nếu trước 9h05 (20 phút trước khi kết thúc)
                if (checkoutTime.isBefore(LocalTime.of(9, 5))) {
                    return PhieuDiemDanh.TrangThaiHoc.RA_VE_SOM;
                } else {
                    return PhieuDiemDanh.TrangThaiHoc.DA_RA_VE;
                }
            case 2:
                // Ca 2: 9h35 - 12h, ra về sớm nếu trước 11h40 (20 phút trước khi kết thúc)
                if (checkoutTime.isBefore(LocalTime.of(11, 40))) {
                    return PhieuDiemDanh.TrangThaiHoc.RA_VE_SOM;
                } else {
                    return PhieuDiemDanh.TrangThaiHoc.DA_RA_VE;
                }
            case 3:
                // Ca 3: 12h30 - 14h55, ra về sớm nếu trước 14h35 (20 phút trước khi kết thúc)
                if (checkoutTime.isBefore(LocalTime.of(14, 35))) {
                    return PhieuDiemDanh.TrangThaiHoc.RA_VE_SOM;
                } else {
                    return PhieuDiemDanh.TrangThaiHoc.DA_RA_VE;
                }
            case 4:
                // Ca 4: 15h05 - 17h30, ra về sớm nếu trước 17h10 (20 phút trước khi kết thúc)
                if (checkoutTime.isBefore(LocalTime.of(17, 10))) {
                    return PhieuDiemDanh.TrangThaiHoc.RA_VE_SOM;
                } else {
                    return PhieuDiemDanh.TrangThaiHoc.DA_RA_VE;
                }
            case 5:
                // Ca 5: 18h - 20h30, ra về sớm nếu trước 20h10 (20 phút trước khi kết thúc)
                if (checkoutTime.isBefore(LocalTime.of(20, 10))) {
                    return PhieuDiemDanh.TrangThaiHoc.RA_VE_SOM;
                } else {
                    return PhieuDiemDanh.TrangThaiHoc.DA_RA_VE;
                }
            default:
                return PhieuDiemDanh.TrangThaiHoc.DA_RA_VE;
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
    
    // Getter cho repository để debug
    public SinhVienRepository getSinhVienRepository() {
        return sinhVienRepository;
    }
    
    /**
     * Đồng bộ dữ liệu từ bảng sinhvien sang phieudiemdanh dựa trên RFID
     * Cập nhật masinhvien và tensinhvien trong phieudiemdanh từ dữ liệu trong sinhvien
     * 
     * @return Map chứa thống kê kết quả đồng bộ
     */
    @Transactional
    public Map<String, Object> syncStudentInfoFromRfid() {
        Map<String, Object> result = new java.util.HashMap<>();
        int totalRecords = 0;
        int updatedRecords = 0;
        int notFoundRecords = 0;
        List<String> notFoundRfids = new java.util.ArrayList<>();
        
        // Lấy tất cả các phiếu điểm danh
        List<PhieuDiemDanh> allAttendance = phieuDiemDanhRepository.findAll();
        totalRecords = allAttendance.size();
        
        System.out.println("=== BẮT ĐẦU ĐỒNG BỘ DỮ LIỆU SINH VIÊN ===");
        System.out.println("Tổng số phiếu điểm danh: " + totalRecords);
        
        // Tạo map để cache thông tin sinh viên theo RFID
        Map<String, SinhVien> sinhVienMap = sinhVienRepository.findAll().stream()
            .collect(Collectors.toMap(
                sv -> sv.getRfid() != null ? sv.getRfid().trim() : "",
                Function.identity(),
                (existing, replacement) -> existing
            ));
        
        System.out.println("Tổng số sinh viên trong hệ thống: " + sinhVienMap.size());
        
        // Duyệt qua từng phiếu điểm danh và cập nhật
        for (PhieuDiemDanh attendance : allAttendance) {
            if (attendance.getRfid() == null || attendance.getRfid().trim().isEmpty()) {
                notFoundRecords++;
                continue;
            }
            
            String trimmedRfid = attendance.getRfid().trim();
            SinhVien sinhVien = sinhVienMap.get(trimmedRfid);
            
            if (sinhVien != null) {
                // Kiểm tra xem có cần cập nhật không
                boolean needsUpdate = false;
                
                if (!sinhVien.getMaSinhVien().equals(attendance.getMaSinhVien())) {
                    attendance.setMaSinhVien(sinhVien.getMaSinhVien());
                    needsUpdate = true;
                }
                
                if (!sinhVien.getTenSinhVien().equals(attendance.getTenSinhVien())) {
                    attendance.setTenSinhVien(sinhVien.getTenSinhVien());
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    phieuDiemDanhRepository.save(attendance);
                    updatedRecords++;
                    System.out.println("Đã cập nhật: RFID=" + trimmedRfid + 
                                     ", Mã SV: " + attendance.getMaSinhVien() + 
                                     ", Tên: " + attendance.getTenSinhVien());
                }
            } else {
                notFoundRecords++;
                if (!notFoundRfids.contains(trimmedRfid)) {
                    notFoundRfids.add(trimmedRfid);
                }
                System.out.println("Không tìm thấy sinh viên với RFID: " + trimmedRfid);
            }
        }
        
        System.out.println("=== KẾT THÚC ĐỒNG BỘ ===");
        System.out.println("Tổng số bản ghi: " + totalRecords);
        System.out.println("Số bản ghi đã cập nhật: " + updatedRecords);
        System.out.println("Số bản ghi không tìm thấy sinh viên: " + notFoundRecords);
        
        result.put("totalRecords", totalRecords);
        result.put("updatedRecords", updatedRecords);
        result.put("notFoundRecords", notFoundRecords);
        result.put("notFoundRfids", notFoundRfids);
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
