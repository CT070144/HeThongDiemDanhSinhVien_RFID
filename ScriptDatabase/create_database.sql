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
