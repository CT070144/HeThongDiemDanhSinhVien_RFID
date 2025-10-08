-- Script để cập nhật cấu trúc bảng sinhvien
-- Thay đổi khóa chính từ RFID sang Mã sinh viên
-- RFID vẫn unique nhưng có thể chỉnh sửa

USE rfid_attendance_system;

-- Backup dữ liệu hiện tại
CREATE TABLE IF NOT EXISTS sinhvien_backup AS 
SELECT * FROM sinhvien;

-- Xóa foreign key constraints trước khi thay đổi cấu trúc
ALTER TABLE sinhvienlophocphan DROP FOREIGN KEY IF EXISTS fk_sinhvienlophocphan_masinhvien;
ALTER TABLE phieudiemdanh DROP FOREIGN KEY IF EXISTS fk_phieudiemdanh_masinhvien;

-- Tạo bảng tạm với cấu trúc mới
CREATE TABLE sinhvien_new (
    masinhvien VARCHAR(20) PRIMARY KEY,
    rfid VARCHAR(50) UNIQUE NOT NULL,
    tensinhvien VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Copy dữ liệu từ bảng cũ sang bảng mới
INSERT INTO sinhvien_new (masinhvien, rfid, tensinhvien, created_at, updated_at)
SELECT masinhvien, rfid, tensinhvien, created_at, updated_at 
FROM sinhvien;

-- Xóa bảng cũ
DROP TABLE sinhvien;

-- Đổi tên bảng mới thành tên cũ
RENAME TABLE sinhvien_new TO sinhvien;

-- Tạo lại foreign key constraints
ALTER TABLE sinhvienlophocphan 
ADD CONSTRAINT fk_sinhvienlophocphan_masinhvien 
FOREIGN KEY (masinhvien) REFERENCES sinhvien(masinhvien) ON DELETE CASCADE;

ALTER TABLE phieudiemdanh 
ADD CONSTRAINT fk_phieudiemdanh_masinhvien 
FOREIGN KEY (masinhvien) REFERENCES sinhvien(masinhvien) ON DELETE CASCADE;

-- Tạo lại index cho RFID để tối ưu tìm kiếm
CREATE INDEX idx_sinhvien_rfid ON sinhvien(rfid);

-- Kiểm tra dữ liệu
SELECT 'Số lượng sinh viên sau khi cập nhật:' as info, COUNT(*) as count FROM sinhvien;
SELECT 'Sample data:' as info, masinhvien, rfid, tensinhvien FROM sinhvien LIMIT 5;

COMMIT;
