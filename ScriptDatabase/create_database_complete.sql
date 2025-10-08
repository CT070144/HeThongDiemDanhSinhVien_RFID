-- =====================================================
-- Script tạo toàn bộ database RFID Attendance System
-- Cấu trúc mới: Mã sinh viên là khóa chính, RFID có thể chỉnh sửa
-- =====================================================

-- Tạo database
CREATE DATABASE IF NOT EXISTS rfid_attendance_system
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE rfid_attendance_system;

-- =====================================================
-- 1. BẢNG SINH VIÊN (Primary Key: maSinhVien)
-- =====================================================
DROP TABLE IF EXISTS sinhvien;
CREATE TABLE sinhvien (
    masinhvien VARCHAR(20) PRIMARY KEY,
    rfid VARCHAR(50) UNIQUE NOT NULL,
    tensinhvien VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Index cho RFID để tìm kiếm nhanh
CREATE INDEX idx_sinhvien_rfid ON sinhvien(rfid);
CREATE INDEX idx_sinhvien_tensinhvien ON sinhvien(tensinhvien);

-- =====================================================
-- 2. BẢNG THIẾT BỊ
-- =====================================================
DROP TABLE IF EXISTS thietbi;
CREATE TABLE thietbi (
    mathietbi VARCHAR(20) PRIMARY KEY,
    tenthietbi VARCHAR(100) NOT NULL,
    phonghoc VARCHAR(50),
    trangthai ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =====================================================
-- 3. BẢNG LỚP HỌC PHẦN
-- =====================================================
DROP TABLE IF EXISTS lophocphan;
CREATE TABLE lophocphan (
    malophocphan VARCHAR(50) PRIMARY KEY,
    tenlophocphan VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Index cho tên lớp học phần
CREATE INDEX idx_lophocphan_tenlophocphan ON lophocphan(tenlophocphan);

-- =====================================================
-- 4. BẢNG LIÊN KẾT SINH VIÊN VÀ LỚP HỌC PHẦN (Many-to-Many)
-- =====================================================
DROP TABLE IF EXISTS sinhvienlophocphan;
CREATE TABLE sinhvienlophocphan (
    masinhvien VARCHAR(20) NOT NULL,
    malophocphan VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (masinhvien, malophocphan),
    FOREIGN KEY (masinhvien) REFERENCES sinhvien(masinhvien) ON DELETE CASCADE,
    FOREIGN KEY (malophocphan) REFERENCES lophocphan(malophocphan) ON DELETE CASCADE
);

-- Index cho tìm kiếm
CREATE INDEX idx_sinhvienlophocphan_masinhvien ON sinhvienlophocphan(masinhvien);
CREATE INDEX idx_sinhvienlophocphan_malophocphan ON sinhvienlophocphan(malophocphan);

-- =====================================================
-- 5. BẢNG PHIẾU ĐIỂM DANH
-- =====================================================
DROP TABLE IF EXISTS phieudiemdanh;
CREATE TABLE phieudiemdanh (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rfid VARCHAR(50) NOT NULL,
    masinhvien VARCHAR(20) NOT NULL,
    tensinhvien VARCHAR(100) NOT NULL,
    phonghoc VARCHAR(50),
    giovao TIME,
    giora TIME,
    ngay DATE NOT NULL,
    ca INT NOT NULL,
    trangthai ENUM('DANG_HOC', 'DA_RA_VE', 'MUON') DEFAULT 'DANG_HOC',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (masinhvien) REFERENCES sinhvien(masinhvien) ON DELETE CASCADE
);

-- Index cho tìm kiếm hiệu quả
CREATE INDEX idx_phieudiemdanh_rfid ON phieudiemdanh(rfid);
CREATE INDEX idx_phieudiemdanh_masinhvien ON phieudiemdanh(masinhvien);
CREATE INDEX idx_phieudiemdanh_ngay ON phieudiemdanh(ngay);
CREATE INDEX idx_phieudiemdanh_ca ON phieudiemdanh(ca);
CREATE INDEX idx_phieudiemdanh_ngay_ca ON phieudiemdanh(ngay, ca);
CREATE INDEX idx_phieudiemdanh_rfid_ngay_ca ON phieudiemdanh(rfid, ngay, ca);

-- =====================================================
-- 6. BẢNG ĐỌC RFID (CHO CÁC THẺ CHƯA ĐĂNG KÝ)
-- =====================================================
DROP TABLE IF EXISTS docrfid;
CREATE TABLE docrfid (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rfid VARCHAR(50) UNIQUE NOT NULL,
    masinhvien VARCHAR(20),
    tensinhvien VARCHAR(100),
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (masinhvien) REFERENCES sinhvien(masinhvien) ON DELETE SET NULL
);

-- Index cho RFID và trạng thái xử lý
CREATE INDEX idx_docrfid_rfid ON docrfid(rfid);
CREATE INDEX idx_docrfid_processed ON docrfid(processed);
CREATE INDEX idx_docrfid_masinhvien ON docrfid(masinhvien);

-- =====================================================
-- 7. VIEW LỊCH SỬ ĐIỂM DANH
-- =====================================================
DROP VIEW IF EXISTS v_lich_su_diem_danh;
CREATE VIEW v_lich_su_diem_danh AS
SELECT
    p.id,
    p.rfid,
    p.masinhvien,
    p.tensinhvien,
    p.phonghoc,
    p.giovao,
    p.giora,
    p.ngay,
    p.ca,
    CASE p.ca
        WHEN 1 THEN 'Ca 1 (07:00-09:30)'
        WHEN 2 THEN 'Ca 2 (09:30-12:00)'
        WHEN 3 THEN 'Ca 3 (12:30-15:00)'
        WHEN 4 THEN 'Ca 4 (15:00-17:30)'
        ELSE CONCAT('Ca ', p.ca)
    END as ten_ca,
    p.trangthai,
    CASE
        WHEN p.giovao IS NOT NULL AND p.giora IS NOT NULL THEN 'Hoàn thành'
        WHEN p.giovao IS NOT NULL AND p.giora IS NULL THEN 'Đang học'
        ELSE 'Chưa điểm danh'
    END as tinh_trang,
    p.created_at,
    p.updated_at
FROM phieudiemdanh p
ORDER BY p.ngay DESC, p.ca ASC, p.created_at DESC;

-- =====================================================
-- 8. INSERT DỮ LIỆU MẪU
-- =====================================================

-- Thêm thiết bị mẫu
INSERT INTO thietbi (mathietbi, tenthietbi, phonghoc, trangthai) VALUES
('TB001', 'Máy đọc RFID Phòng A101', 'A101', 'ACTIVE'),
('TB002', 'Máy đọc RFID Phòng A102', 'A102', 'ACTIVE'),
('TB003', 'Máy đọc RFID Phòng B201', 'B201', 'ACTIVE'),
('TB004', 'Máy đọc RFID Phòng B202', 'B202', 'ACTIVE'),
('TB005', 'Máy đọc RFID Phòng C301', 'C301', 'ACTIVE');

-- Thêm sinh viên mẫu
INSERT INTO sinhvien (masinhvien, rfid, tensinhvien) VALUES
('CT070201', 'RFID001', 'Nguyễn Văn An'),
('CT070202', 'RFID002', 'Trần Thị Bình'),
('CT070203', 'RFID003', 'Lê Văn Cường'),
('CT070204', 'RFID004', 'Phạm Thị Dung'),
('CT070205', 'RFID005', 'Hoàng Văn Em'),
('CT070206', 'RFID006', 'Vũ Thị Phương'),
('CT070207', 'RFID007', 'Đặng Văn Giang'),
('CT070208', 'RFID008', 'Bùi Thị Hoa'),
('CT070209', 'RFID009', 'Ngô Văn Ích'),
('CT070210', 'RFID010', 'Dương Thị Kim');

-- Thêm lớp học phần mẫu
INSERT INTO lophocphan (malophocphan, tenlophocphan) VALUES
('CNPMN-L01', 'Lớp: Công nghệ phần mềm nhúng-1-25 (C701)'),
('CNPMN-L02', 'Lớp: Công nghệ phần mềm nhúng-2-25 (C702)'),
('CNTT-L01', 'Lớp: Công nghệ thông tin-1-30 (A101)'),
('CNTT-L02', 'Lớp: Công nghệ thông tin-2-30 (A102)'),
('KTPM-L01', 'Lớp: Kỹ thuật phần mềm-1-25 (B201)');

-- Thêm sinh viên vào lớp học phần
INSERT INTO sinhvienlophocphan (masinhvien, malophocphan) VALUES
-- Sinh viên lớp CNPMN-L01
('CT070201', 'CNPMN-L01'),
('CT070202', 'CNPMN-L01'),
('CT070203', 'CNPMN-L01'),
('CT070204', 'CNPMN-L01'),
('CT070205', 'CNPMN-L01'),

-- Sinh viên lớp CNPMN-L02
('CT070206', 'CNPMN-L02'),
('CT070207', 'CNPMN-L02'),
('CT070208', 'CNPMN-L02'),
('CT070209', 'CNPMN-L02'),
('CT070210', 'CNPMN-L02'),

-- Một số sinh viên tham gia nhiều lớp
('CT070201', 'CNTT-L01'),
('CT070202', 'CNTT-L01'),
('CT070203', 'KTPM-L01');

-- Thêm phiếu điểm danh mẫu (hôm nay)
INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, ngay, ca, trangthai) VALUES
('RFID001', 'CT070201', 'Nguyễn Văn An', 'A101', '07:05:00', CURDATE(), 1, 'DANG_HOC'),
('RFID002', 'CT070202', 'Trần Thị Bình', 'A101', '07:08:00', CURDATE(), 1, 'DANG_HOC'),
('RFID003', 'CT070203', 'Lê Văn Cường', 'A101', '07:20:00', CURDATE(), 1, 'MUON'),
('RFID006', 'CT070206', 'Vũ Thị Phương', 'A102', '09:35:00', CURDATE(), 2, 'DANG_HOC'),
('RFID007', 'CT070207', 'Đặng Văn Giang', 'A102', '09:40:00', CURDATE(), 2, 'DANG_HOC');

-- Thêm phiếu điểm danh mẫu (ngày hôm qua)
INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, trangthai) VALUES
('RFID001', 'CT070201', 'Nguyễn Văn An', 'A101', '07:03:00', '09:25:00', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, 'DA_RA_VE'),
('RFID002', 'CT070202', 'Trần Thị Bình', 'A101', '07:06:00', '09:28:00', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, 'DA_RA_VE'),
('RFID003', 'CT070203', 'Lê Văn Cường', 'A101', '07:18:00', '09:20:00', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, 'MUON'),
('RFID004', 'CT070204', 'Phạm Thị Dung', 'A101', '07:10:00', '09:30:00', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, 'DA_RA_VE'),
('RFID005', 'CT070205', 'Hoàng Văn Em', 'A101', '07:15:00', '09:25:00', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, 'DA_RA_VE');

-- =====================================================
-- 9. STORED PROCEDURES VÀ FUNCTIONS
-- =====================================================

-- Function lấy ca học hiện tại
DELIMITER //
CREATE FUNCTION get_current_ca() 
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE current_time TIME DEFAULT CURTIME();
    
    IF current_time >= '07:00:00' AND current_time < '09:30:00' THEN
        RETURN 1;
    ELSEIF current_time >= '09:30:00' AND current_time < '12:00:00' THEN
        RETURN 2;
    ELSEIF current_time >= '12:30:00' AND current_time < '15:00:00' THEN
        RETURN 3;
    ELSEIF current_time >= '15:00:00' AND current_time < '17:30:00' THEN
        RETURN 4;
    ELSE
        RETURN 0; -- Ngoài giờ học
    END IF;
END //
DELIMITER ;

-- Procedure thống kê điểm danh theo ngày
DELIMITER //
CREATE PROCEDURE sp_thong_ke_diem_danh_ngay(IN p_ngay DATE)
BEGIN
    SELECT 
        p.ngay,
        p.ca,
        CASE p.ca
            WHEN 1 THEN 'Ca 1 (07:00-09:30)'
            WHEN 2 THEN 'Ca 2 (09:30-12:00)'
            WHEN 3 THEN 'Ca 3 (12:30-15:00)'
            WHEN 4 THEN 'Ca 4 (15:00-17:30)'
            ELSE CONCAT('Ca ', p.ca)
        END as ten_ca,
        COUNT(*) as tong_phieu,
        COUNT(CASE WHEN p.trangthai = 'DANG_HOC' THEN 1 END) as dang_hoc,
        COUNT(CASE WHEN p.trangthai = 'DA_RA_VE' THEN 1 END) as da_ra_ve,
        COUNT(CASE WHEN p.trangthai = 'MUON' THEN 1 END) as muon
    FROM phieudiemdanh p
    WHERE p.ngay = p_ngay
    GROUP BY p.ngay, p.ca
    ORDER BY p.ca;
END //
DELIMITER ;

-- Procedure thống kê điểm danh theo lớp học phần
DELIMITER //
CREATE PROCEDURE sp_thong_ke_diem_danh_lop(IN p_malophocphan VARCHAR(50), IN p_ngay DATE)
BEGIN
    SELECT 
        lhp.malophocphan,
        lhp.tenlophocphan,
        p.ngay,
        COUNT(DISTINCT slhp.masinhvien) as tong_sinh_vien,
        COUNT(p.id) as so_phieu_diem_danh,
        COUNT(DISTINCT CASE WHEN p.trangthai != 'MUON' THEN p.masinhvien END) as so_sinh_vien_co_mat,
        COUNT(DISTINCT CASE WHEN p.trangthai = 'MUON' THEN p.masinhvien END) as so_sinh_vien_muon
    FROM lophocphan lhp
    LEFT JOIN sinhvienlophocphan slhp ON lhp.malophocphan = slhp.malophocphan
    LEFT JOIN phieudiemdanh p ON slhp.masinhvien = p.masinhvien AND p.ngay = p_ngay
    WHERE lhp.malophocphan = p_malophocphan
    GROUP BY lhp.malophocphan, lhp.tenlophocphan, p.ngay;
END //
DELIMITER ;

-- =====================================================
-- 10. TRIGGERS
-- =====================================================

-- Trigger cập nhật thời gian khi thay đổi phiếu điểm danh
DELIMITER //
CREATE TRIGGER tr_phieudiemdanh_update_time
    BEFORE UPDATE ON phieudiemdanh
    FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END //
DELIMITER ;

-- Trigger cập nhật thời gian khi thay đổi sinh viên
DELIMITER //
CREATE TRIGGER tr_sinhvien_update_time
    BEFORE UPDATE ON sinhvien
    FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END //
DELIMITER ;

-- =====================================================
-- 11. GRANTS VÀ PERMISSIONS
-- =====================================================

-- Tạo user cho ứng dụng (tùy chọn)
-- CREATE USER 'rfid_app'@'localhost' IDENTIFIED BY 'rfid_password_2024';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON rfid_attendance_system.* TO 'rfid_app'@'localhost';
-- FLUSH PRIVILEGES;

-- =====================================================
-- 12. KIỂM TRA VÀ THÔNG TIN HỆ THỐNG
-- =====================================================

-- Hiển thị thông tin các bảng
SELECT 
    TABLE_NAME as 'Tên bảng',
    TABLE_ROWS as 'Số dòng',
    CREATE_TIME as 'Thời gian tạo',
    TABLE_COLLATION as 'Collation'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'rfid_attendance_system'
ORDER BY TABLE_NAME;

-- Hiển thị thông tin các index
SELECT 
    TABLE_NAME as 'Bảng',
    INDEX_NAME as 'Index',
    COLUMN_NAME as 'Cột',
    NON_UNIQUE as 'Không unique'
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'rfid_attendance_system'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- Kiểm tra dữ liệu mẫu
SELECT 'Sinh viên' as 'Loại dữ liệu', COUNT(*) as 'Số lượng' FROM sinhvien
UNION ALL
SELECT 'Lớp học phần', COUNT(*) FROM lophocphan
UNION ALL
SELECT 'Phiếu điểm danh', COUNT(*) FROM phieudiemdanh
UNION ALL
SELECT 'Thiết bị', COUNT(*) FROM thietbi
UNION ALL
SELECT 'Liên kết SV-LHP', COUNT(*) FROM sinhvienlophocphan;

-- =====================================================
-- HOÀN THÀNH TẠO DATABASE
-- =====================================================

SELECT '=====================================================' as '';
SELECT 'DATABASE RFID ATTENDANCE SYSTEM ĐÃ ĐƯỢC TẠO THÀNH CÔNG!' as '';
SELECT '=====================================================' as '';
SELECT 'Cấu trúc mới:' as '';
SELECT '- Mã sinh viên là khóa chính' as '';
SELECT '- RFID có thể chỉnh sửa và không được trùng' as '';
SELECT '- Đã có dữ liệu mẫu để test' as '';
SELECT '=====================================================' as '';

COMMIT;
