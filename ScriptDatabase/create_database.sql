-- Tạo database cho hệ thống điểm danh sinh viên
CREATE DATABASE IF NOT EXISTS rfid_attendance_system;
USE rfid_attendance_system;

-- Bảng sinh viên
CREATE TABLE sinhvien (
    rfid VARCHAR(50) PRIMARY KEY,
    masinhvien VARCHAR(20) NOT NULL UNIQUE,
    tensinhvien VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bảng phiếu điểm danh
CREATE TABLE phieudiemdanh (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rfid VARCHAR(50) NOT NULL,
    masinhvien VARCHAR(20) NOT NULL,
    tensinhvien VARCHAR(100) NOT NULL,
    phonghoc VARCHAR(50) NULL,
    giovao TIME,
    giora TIME,
    ngay DATE NOT NULL,
    ca INT NOT NULL CHECK (ca IN (1, 2, 3, 4)),
    trangthai ENUM('muon', 'dang_hoc', 'da_ra_ve') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rfid) REFERENCES sinhvien(rfid) ON DELETE CASCADE,
    INDEX idx_ngay_ca (ngay, ca),
    INDEX idx_rfid_ngay (rfid, ngay)
);

-- Bảng đọc RFID (cho các thẻ chưa được đăng ký)
CREATE TABLE docRfid (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rfid VARCHAR(50) NOT NULL UNIQUE,
    masinhvien VARCHAR(20) NULL,
    tensinhvien VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE
);

-- Bảng thiết bị điểm danh
CREATE TABLE thietbi (
    mathietbi VARCHAR(50) PRIMARY KEY,
    phonghoc VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bảng quản lý ca học
CREATE TABLE cahoc (
    id INT PRIMARY KEY,
    ten_ca VARCHAR(50) NOT NULL,
    gio_bat_dau TIME NOT NULL,
    gio_ket_thuc TIME NOT NULL
);

-- Insert dữ liệu ca học
INSERT INTO cahoc (id, ten_ca, gio_bat_dau, gio_ket_thuc) VALUES
(1, 'Ca 1', '07:00:00', '09:30:00'),
(2, 'Ca 2', '09:30:00', '12:00:00'),
(3, 'Ca 3', '12:30:00', '15:00:00'),
(4, 'Ca 4', '15:00:00', '17:30:00');

-- Bảng users cho quản trị viên (tùy chọn)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role ENUM('admin', 'user') DEFAULT 'user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert admin mặc định
INSERT INTO users (username, password, email, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'admin@example.com', 'admin');

-- Tạo view để hiển thị lịch sử điểm danh với thông tin chi tiết
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
    c.ten_ca,
    c.gio_bat_dau,
    c.gio_ket_thuc,
    p.trangthai,
    CASE 
        WHEN p.giovao IS NOT NULL AND p.giora IS NOT NULL THEN 'Hoàn thành'
        WHEN p.giovao IS NOT NULL AND p.giora IS NULL THEN 'Đang học'
        ELSE 'Chưa điểm danh'
    END as tinh_trang,
    p.created_at,
    p.updated_at
FROM phieudiemdanh p
LEFT JOIN cahoc c ON p.ca = c.id
ORDER BY p.ngay DESC, p.ca ASC, p.created_at DESC;

-- DỮ LIỆU MẪU

-- Thiết bị (5 mẫu)
INSERT INTO thietbi (mathietbi, phonghoc) VALUES
('DEV-001', 'P101'),
('DEV-002', 'P102'),
('DEV-003', 'P201'),
('DEV-004', 'P202'),
('DEV-005', 'LAB1');

-- Sinh viên (20 mẫu)
INSERT INTO sinhvien (rfid, masinhvien, tensinhvien) VALUES
('RFID0001', 'SV0001', 'Nguyen Van A'),
('RFID0002', 'SV0002', 'Tran Thi B'),
('RFID0003', 'SV0003', 'Le Van C'),
('RFID0004', 'SV0004', 'Pham Thi D'),
('RFID0005', 'SV0005', 'Hoang Van E'),
('RFID0006', 'SV0006', 'Bui Thi F'),
('RFID0007', 'SV0007', 'Vu Van G'),
('RFID0008', 'SV0008', 'Do Thi H'),
('RFID0009', 'SV0009', 'Ngo Van I'),
('RFID0010', 'SV0010', 'Dang Thi K'),
('RFID0011', 'SV0011', 'Phan Van L'),
('RFID0012', 'SV0012', 'Duong Thi M'),
('RFID0013', 'SV0013', 'Ha Van N'),
('RFID0014', 'SV0014', 'Trinh Thi O'),
('RFID0015', 'SV0015', 'Vuong Van P'),
('RFID0016', 'SV0016', 'Vu Thi Q'),
('RFID0017', 'SV0017', 'Ngo Thi R'),
('RFID0018', 'SV0018', 'Le Thi S'),
('RFID0019', 'SV0019', 'Do Van T'),
('RFID0020', 'SV0020', 'Nguyen Thi U');

-- docRfid (20 mẫu: 10 đã đăng ký/processed, 10 chưa đăng ký)
INSERT INTO docRfid (rfid, masinhvien, tensinhvien, processed) VALUES
('RFID0001', 'SV0001', 'Nguyen Van A', TRUE),
('RFID0002', 'SV0002', 'Tran Thi B', TRUE),
('RFID0003', 'SV0003', 'Le Van C', TRUE),
('RFID0004', 'SV0004', 'Pham Thi D', TRUE),
('RFID0005', 'SV0005', 'Hoang Van E', TRUE),
('RFID0006', 'SV0006', 'Bui Thi F', TRUE),
('RFID0007', 'SV0007', 'Vu Van G', TRUE),
('RFID0008', 'SV0008', 'Do Thi H', TRUE),
('RFID0009', 'SV0009', 'Ngo Van I', TRUE),
('RFID0010', 'SV0010', 'Dang Thi K', TRUE),
('URFID001', NULL, NULL, FALSE),
('URFID002', NULL, NULL, FALSE),
('URFID003', NULL, NULL, FALSE),
('URFID004', NULL, NULL, FALSE),
('URFID005', NULL, NULL, FALSE),
('URFID006', NULL, NULL, FALSE),
('URFID007', NULL, NULL, FALSE),
('URFID008', NULL, NULL, FALSE),
('URFID009', NULL, NULL, FALSE),
('URFID010', NULL, NULL, FALSE);

-- Phiếu điểm danh (khoảng 20 mẫu, phân bổ ca/ngày/phòng)
-- Giả lập ngày hôm nay và hai ngày gần đây
INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, trangthai)
VALUES
('RFID0001', 'SV0001', 'Nguyen Van A', 'P101', '07:05:00', NULL, CURRENT_DATE, 1, 'dang_hoc'),
('RFID0002', 'SV0002', 'Tran Thi B', 'P101', '07:20:00', NULL, CURRENT_DATE, 1, 'muon'),
('RFID0003', 'SV0003', 'Le Van C', 'P102', '09:35:00', NULL, CURRENT_DATE, 2, 'dang_hoc'),
('RFID0004', 'SV0004', 'Pham Thi D', 'P201', '12:40:00', NULL, CURRENT_DATE, 3, 'dang_hoc'),
('RFID0005', 'SV0005', 'Hoang Van E', 'P202', '15:10:00', NULL, CURRENT_DATE, 4, 'dang_hoc'),
('RFID0006', 'SV0006', 'Bui Thi F', 'LAB1', '07:40:00', '08:50:00', CURRENT_DATE, 1, 'da_ra_ve'),
('RFID0007', 'SV0007', 'Vu Van G', 'P101', '07:50:00', '09:10:00', CURRENT_DATE, 1, 'da_ra_ve'),
('RFID0008', 'SV0008', 'Do Thi H', 'P102', '09:50:00', '10:30:00', CURRENT_DATE, 2, 'da_ra_ve'),
('RFID0009', 'SV0009', 'Ngo Van I', 'P201', '12:50:00', '13:20:00', CURRENT_DATE, 3, 'da_ra_ve'),
('RFID0010', 'SV0010', 'Dang Thi K', 'P202', '15:20:00', '16:10:00', CURRENT_DATE, 4, 'da_ra_ve'),

('RFID0011', 'SV0011', 'Phan Van L', 'P101', '07:02:00', NULL, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 1, 'dang_hoc'),
('RFID0012', 'SV0012', 'Duong Thi M', 'P102', '09:50:00', NULL, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 2, 'dang_hoc'),
('RFID0013', 'SV0013', 'Ha Van N', 'P201', '12:31:00', NULL, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 3, 'dang_hoc'),
('RFID0014', 'SV0014', 'Trinh Thi O', 'P202', '15:25:00', NULL, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 4, 'dang_hoc'),
('RFID0015', 'SV0015', 'Vuong Van P', 'LAB1', '07:30:00', '09:00:00', DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 1, 'da_ra_ve'),

('RFID0016', 'SV0016', 'Vu Thi Q', 'P101', '07:10:00', NULL, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 1, 'dang_hoc'),
('RFID0017', 'SV0017', 'Ngo Thi R', 'P102', '09:32:00', NULL, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 2, 'dang_hoc'),
('RFID0018', 'SV0018', 'Le Thi S', 'P201', '12:29:00', NULL, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 3, 'muon'),
('RFID0019', 'SV0019', 'Do Van T', 'P202', '15:05:00', NULL, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 4, 'dang_hoc'),
('RFID0020', 'SV0020', 'Nguyen Thi U', 'LAB1', '07:18:00', '08:55:00', DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 1, 'da_ra_ve');
