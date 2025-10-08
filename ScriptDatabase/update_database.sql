-- Script cập nhật database để thêm tính năng lớp học phần
-- Chạy script này để tạo các bảng mới và cập nhật cấu trúc database

-- Khắc phục lỗi view trước khi tạo bảng mới
DROP VIEW IF EXISTS v_lich_su_diem_danh;

-- Tạo bảng lophocphan
CREATE TABLE IF NOT EXISTS lophocphan (
    malophocphan VARCHAR(50) PRIMARY KEY,
    tenlophocphan VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tạo bảng sinhvienlophocphan (bảng trung gian nhiều-nhiều)
CREATE TABLE IF NOT EXISTS sinhvienlophocphan (
    masinhvien VARCHAR(20) NOT NULL,
    malophocphan VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (masinhvien, malophocphan),
    FOREIGN KEY (masinhvien) REFERENCES sinhvien(masinhvien) ON DELETE CASCADE,
    FOREIGN KEY (malophocphan) REFERENCES lophocphan(malophocphan) ON DELETE CASCADE
);

-- Tạo index để tối ưu hiệu suất
CREATE INDEX IF NOT EXISTS idx_lophocphan_ten ON lophocphan(tenlophocphan);
CREATE INDEX IF NOT EXISTS idx_sinhvienlophocphan_masinhvien ON sinhvienlophocphan(masinhvien);
CREATE INDEX IF NOT EXISTS idx_sinhvienlophocphan_malophocphan ON sinhvienlophocphan(malophocphan);

-- Thêm comment cho các bảng
ALTER TABLE lophocphan COMMENT = 'Bảng lưu trữ thông tin các lớp học phần';
ALTER TABLE sinhvienlophocphan COMMENT = 'Bảng trung gian lưu trữ mối quan hệ nhiều-nhiều giữa sinh viên và lớp học phần';

-- Thêm comment cho các cột
ALTER TABLE lophocphan 
MODIFY COLUMN malophocphan VARCHAR(50) COMMENT 'Mã lớp học phần (ví dụ: CNPMN-L01)',
MODIFY COLUMN tenlophocphan VARCHAR(200) COMMENT 'Tên đầy đủ của lớp học phần';

ALTER TABLE sinhvienlophocphan
MODIFY COLUMN masinhvien VARCHAR(20) COMMENT 'Mã sinh viên',
MODIFY COLUMN malophocphan VARCHAR(50) COMMENT 'Mã lớp học phần';

-- Tạo một số dữ liệu mẫu (tùy chọn)
-- INSERT INTO lophocphan (malophocphan, tenlophocphan) VALUES 
-- ('CNPMN-L01', 'Công nghệ phần mềm nhúng-1-25 (C701)'),
-- ('CNPMN-L02', 'Công nghệ phần mềm nhúng-1-25 (C702)'),
-- ('CNPMN-L03', 'Công nghệ phần mềm nhúng-1-25 (C703)');

-- Tạo lại view sau khi đã tạo các bảng mới
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

COMMIT;
