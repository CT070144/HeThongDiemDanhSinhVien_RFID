-- =====================================================
-- Script RESET và TẠO LẠI toàn bộ database RFID Attendance System
-- Sử dụng khi cần xóa toàn bộ và tạo lại từ đầu
-- =====================================================

-- ⚠️ CẢNH BÁO: Script này sẽ XÓA TOÀN BỘ dữ liệu hiện tại!
-- Chỉ sử dụng khi muốn reset hoàn toàn hệ thống

-- =====================================================
-- BƯỚC 1: XÓA TOÀN BỘ DATABASE CŨ
-- =====================================================

-- Xóa toàn bộ database (cẩn thận!)
DROP DATABASE IF EXISTS rfid_attendance_system;

-- =====================================================
-- BƯỚC 2: TẠO LẠI DATABASE MỚI
-- =====================================================

-- Tạo database mới
CREATE DATABASE rfid_attendance_system
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE rfid_attendance_system;

-- =====================================================
-- BƯỚC 3: TẠO CÁC BẢNG
-- =====================================================

-- 1. BẢNG SINH VIÊN (Primary Key: maSinhVien)
CREATE TABLE sinhvien (
    masinhvien VARCHAR(20) PRIMARY KEY,
    rfid VARCHAR(50) UNIQUE NOT NULL,
    tensinhvien VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Index cho sinh viên
CREATE INDEX idx_sinhvien_rfid ON sinhvien(rfid);
CREATE INDEX idx_sinhvien_tensinhvien ON sinhvien(tensinhvien);

-- 2. BẢNG THIẾT BỊ
CREATE TABLE thietbi (
    mathietbi VARCHAR(20) PRIMARY KEY,
    tenthietbi VARCHAR(100) NOT NULL,
    phonghoc VARCHAR(50),
    trangthai ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. BẢNG LỚP HỌC PHẦN
CREATE TABLE lophocphan (
    malophocphan VARCHAR(50) PRIMARY KEY,
    tenlophocphan VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_lophocphan_tenlophocphan ON lophocphan(tenlophocphan);

-- 4. BẢNG LIÊN KẾT SINH VIÊN VÀ LỚP HỌC PHẦN
CREATE TABLE sinhvienlophocphan (
    masinhvien VARCHAR(20) NOT NULL,
    malophocphan VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (masinhvien, malophocphan),
    FOREIGN KEY (masinhvien) REFERENCES sinhvien(masinhvien) ON DELETE CASCADE,
    FOREIGN KEY (malophocphan) REFERENCES lophocphan(malophocphan) ON DELETE CASCADE
);

CREATE INDEX idx_sinhvienlophocphan_masinhvien ON sinhvienlophocphan(masinhvien);
CREATE INDEX idx_sinhvienlophocphan_malophocphan ON sinhvienlophocphan(malophocphan);

-- 5. BẢNG PHIẾU ĐIỂM DANH
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

-- Index cho phiếu điểm danh
CREATE INDEX idx_phieudiemdanh_rfid ON phieudiemdanh(rfid);
CREATE INDEX idx_phieudiemdanh_masinhvien ON phieudiemdanh(masinhvien);
CREATE INDEX idx_phieudiemdanh_ngay ON phieudiemdanh(ngay);
CREATE INDEX idx_phieudiemdanh_ca ON phieudiemdanh(ca);
CREATE INDEX idx_phieudiemdanh_ngay_ca ON phieudiemdanh(ngay, ca);
CREATE INDEX idx_phieudiemdanh_rfid_ngay_ca ON phieudiemdanh(rfid, ngay, ca);

-- 6. BẢNG ĐỌC RFID
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

CREATE INDEX idx_docrfid_rfid ON docrfid(rfid);
CREATE INDEX idx_docrfid_processed ON docrfid(processed);
CREATE INDEX idx_docrfid_masinhvien ON docrfid(masinhvien);

-- =====================================================
-- BƯỚC 4: TẠO VIEW
-- =====================================================

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
-- BƯỚC 5: INSERT DỮ LIỆU MẪU
-- =====================================================

-- Thiết bị mẫu
INSERT INTO thietbi (mathietbi, tenthietbi, phonghoc, trangthai) VALUES
('TB001', 'Máy đọc RFID Phòng A101', 'A101', 'ACTIVE'),
('TB002', 'Máy đọc RFID Phòng A102', 'A102', 'ACTIVE'),
('TB003', 'Máy đọc RFID Phòng B201', 'B201', 'ACTIVE'),
('TB004', 'Máy đọc RFID Phòng B202', 'B202', 'ACTIVE'),
('TB005', 'Máy đọc RFID Phòng C301', 'C301', 'ACTIVE');

-- Sinh viên mẫu
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

-- Lớp học phần mẫu
INSERT INTO lophocphan (malophocphan, tenlophocphan) VALUES
('CNPMN-L01', 'Lớp: Công nghệ phần mềm nhúng-1-25 (C701)'),
('CNPMN-L02', 'Lớp: Công nghệ phần mềm nhúng-2-25 (C702)'),
('CNTT-L01', 'Lớp: Công nghệ thông tin-1-30 (A101)'),
('CNTT-L02', 'Lớp: Công nghệ thông tin-2-30 (A102)'),
('KTPM-L01', 'Lớp: Kỹ thuật phần mềm-1-25 (B201)');

-- Liên kết sinh viên và lớp học phần
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

-- =====================================================
-- BƯỚC 6: KIỂM TRA KẾT QUẢ
-- =====================================================

-- Hiển thị thông tin các bảng
SELECT 
    TABLE_NAME as 'Bảng',
    TABLE_ROWS as 'Số dòng'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'rfid_attendance_system'
ORDER BY TABLE_NAME;

-- Kiểm tra dữ liệu mẫu
SELECT 'Sinh viên' as 'Loại', COUNT(*) as 'Số lượng' FROM sinhvien
UNION ALL
SELECT 'Lớp học phần', COUNT(*) FROM lophocphan
UNION ALL
SELECT 'Thiết bị', COUNT(*) FROM thietbi
UNION ALL
SELECT 'Liên kết SV-LHP', COUNT(*) FROM sinhvienlophocphan;

-- =====================================================
-- HOÀN THÀNH
-- =====================================================

SELECT '=====================================================' as '';
SELECT 'DATABASE ĐÃ ĐƯỢC RESET VÀ TẠO LẠI THÀNH CÔNG!' as '';
SELECT '=====================================================' as '';
SELECT 'Cấu trúc mới:' as '';
SELECT '- Mã sinh viên là khóa chính' as '';
SELECT '- RFID có thể chỉnh sửa và không được trùng' as '';
SELECT '- Đã có dữ liệu mẫu để test' as '';
SELECT '=====================================================' as '';

COMMIT;
