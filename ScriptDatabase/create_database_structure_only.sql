-- =====================================================
-- Script tạo CẤU TRÚC DATABASE RFID Attendance System
-- Chỉ tạo bảng, index, view - KHÔNG có dữ liệu mẫu
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

-- Index cho sinh viên
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

-- Index cho lớp học phần
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

-- Index cho liên kết
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

-- Index cho phiếu điểm danh
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

-- Index cho đọc RFID
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
-- 8. KIỂM TRA CẤU TRÚC
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

-- =====================================================
-- HOÀN THÀNH TẠO CẤU TRÚC
-- =====================================================

SELECT '=====================================================' as '';
SELECT 'CẤU TRÚC DATABASE RFID ATTENDANCE SYSTEM ĐÃ ĐƯỢC TẠO!' as '';
SELECT '=====================================================' as '';
SELECT 'Cấu trúc:' as '';
SELECT '- Mã sinh viên là khóa chính' as '';
SELECT '- RFID có thể chỉnh sửa và không được trùng' as '';
SELECT '- Chưa có dữ liệu mẫu' as '';
SELECT '- Sẵn sàng để import dữ liệu' as '';
SELECT '=====================================================' as '';

COMMIT;
