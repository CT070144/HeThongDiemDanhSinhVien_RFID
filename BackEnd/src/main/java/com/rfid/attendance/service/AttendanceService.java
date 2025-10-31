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


                socketIOServer.getAllClients().forEach(client ->{
                    System.out.println("sockethehe");
                    String message = null;
                    try {
                        message = objectMapper.writeValueAsString(result);
                    } catch (JsonProcessingException e) {
                        System.out.println("error convert object");
                    }
                    client.sendEvent("update-attendance",message);
                });
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
            return phieuDiemDanhRepository.save(newRecord);
        }
    }

    public PhieuDiemDanh processRfidAttendanceWithDevice(String rfid, String maThietBi) {
        PhieuDiemDanh record = processRfidAttendance(rfid);
        System.out.println(record.getRfid());
        if (record.getRfid() == null) return record;
        if (maThietBi != null && !maThietBi.isEmpty() && record.getCa() != -99) {
            Optional<ThietBi> tb = thietBiRepository.findById(maThietBi);
            tb.ifPresent(thietBi -> {
                record.setPhongHoc(thietBi.getPhongHoc());
                phieuDiemDanhRepository.save(record);
            });
        }
        System.out.printf("lỗi đấyyy");
        return record;
    }
    
    private Integer getCurrentCa() {
        LocalTime currentTime = LocalTime.now();
        
        // Ca 1: 7h - 9h25 (có thể điểm danh từ 6h50 - 9h35)
        if (currentTime.isAfter(LocalTime.of(1, 50)) && currentTime.isBefore(LocalTime.of(9, 35))) {
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
        else if (currentTime.isAfter(LocalTime.of(17, 50)) && currentTime.isBefore(LocalTime.of(20, 40))) {
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
}
