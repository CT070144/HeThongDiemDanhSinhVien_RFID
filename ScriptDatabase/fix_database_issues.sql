-- Script khắc phục lỗi database
-- Chạy script này để sửa các vấn đề với view và bảng

USE rfid_attendance_system;

-- 1. Xóa view cũ nếu tồn tại (để tránh xung đột)
DROP VIEW IF EXISTS v_lich_su_diem_danh;

-- 2. Đảm bảo tất cả các bảng cần thiết đã tồn tại
-- Kiểm tra và tạo bảng sinhvien nếu chưa có
CREATE TABLE IF NOT EXISTS sinhvien (
    rfid VARCHAR(50) PRIMARY KEY,
    masinhvien VARCHAR(20) NOT NULL UNIQUE,
    tensinhvien VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Kiểm tra và tạo bảng phieudiemdanh nếu chưa có
CREATE TABLE IF NOT EXISTS phieudiemdanh (
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

-- Kiểm tra và tạo bảng cahoc nếu chưa có
CREATE TABLE IF NOT EXISTS cahoc (
    id INT PRIMARY KEY,
    ten_ca VARCHAR(50) NOT NULL,
    gio_bat_dau TIME NOT NULL,
    gio_ket_thuc TIME NOT NULL
);

-- Insert dữ liệu ca học nếu chưa có
INSERT IGNORE INTO cahoc (id, ten_ca, gio_bat_dau, gio_ket_thuc) VALUES
(1, 'Ca 1', '07:00:00', '09:30:00'),
(2, 'Ca 2', '09:30:00', '12:00:00'),
(3, 'Ca 3', '12:30:00', '15:00:00'),
(4, 'Ca 4', '15:00:00', '17:30:00');

-- 3. Tạo lại view với cấu trúc đúng
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

-- 4. Tạo các bảng mới cho tính năng lớp học phần (nếu chưa có)
CREATE TABLE IF NOT EXISTS lophocphan (
    malophocphan VARCHAR(50) PRIMARY KEY,
    tenlophocphan VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sinhvienlophocphan (
    masinhvien VARCHAR(20) NOT NULL,
    malophocphan VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (masinhvien, malophocphan),
    FOREIGN KEY (masinhvien) REFERENCES sinhvien(masinhvien) ON DELETE CASCADE,
    FOREIGN KEY (malophocphan) REFERENCES lophocphan(malophocphan) ON DELETE CASCADE
);

-- 5. Tạo index để tối ưu hiệu suất
CREATE INDEX IF NOT EXISTS idx_lophocphan_ten ON lophocphan(tenlophocphan);
CREATE INDEX IF NOT EXISTS idx_sinhvienlophocphan_masinhvien ON sinhvienlophocphan(masinhvien);
CREATE INDEX IF NOT EXISTS idx_sinhvienlophocphan_malophocphan ON sinhvienlophocphan(malophocphan);

-- 6. Kiểm tra và tạo các bảng khác nếu cần
CREATE TABLE IF NOT EXISTS docRfid (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rfid VARCHAR(50) NOT NULL UNIQUE,
    masinhvien VARCHAR(20) NULL,
    tensinhvien VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS thietbi (
    mathietbi VARCHAR(50) PRIMARY KEY,
    phonghoc VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role ENUM('admin', 'user') DEFAULT 'user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Kiểm tra và insert admin mặc định nếu chưa có
INSERT IGNORE INTO users (username, password, email, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'admin@example.com', 'admin');

-- 8. Hiển thị thông tin về các bảng đã được tạo
SHOW TABLES;

-- 9. Kiểm tra view đã được tạo thành công
SHOW CREATE VIEW v_lich_su_diem_danh;

COMMIT;
