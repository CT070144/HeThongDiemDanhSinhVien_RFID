-- Script khắc phục nhanh lỗi view
-- Chạy script này để sửa lỗi view v_lich_su_diem_danh

USE rfid_attendance_system;

-- Xóa view cũ nếu tồn tại
DROP VIEW IF EXISTS v_lich_su_diem_danh;

-- Tạo lại view với cấu trúc đơn giản hơn
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
