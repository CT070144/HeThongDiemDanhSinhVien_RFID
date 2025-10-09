-- Script cập nhật hệ thống điểm danh theo ca học mới
USE rfid_attendance_system;

-- Cập nhật các trạng thái cũ thành trạng thái mới
-- DANG_HOC -> DUNG_GIO
UPDATE phieudiemdanh 
SET trangthai = 'dung_gio' 
WHERE trangthai = 'dang_hoc';

-- DA_RA_VE -> DUNG_GIO (nếu có)
UPDATE phieudiemdanh 
SET trangthai = 'dung_gio' 
WHERE trangthai = 'da_ra_ve';

-- Hiển thị thống kê sau khi cập nhật
SELECT '=== THỐNG KÊ TRẠNG THÁI ĐIỂM DANH SAU CẬP NHẬT ===' as '';
SELECT 
    trangthai,
    COUNT(*) as so_luong,
    CASE 
        WHEN trangthai = 'dung_gio' THEN 'Điểm danh đúng giờ'
        WHEN trangthai = 'muon' THEN 'Điểm danh muộn'
        ELSE trangthai
    END as mo_ta
FROM phieudiemdanh 
GROUP BY trangthai
ORDER BY trangthai;

-- Hiển thị thống kê theo ca học
SELECT '=== THỐNG KÊ THEO CA HỌC ===' as '';
SELECT 
    ca,
    COUNT(*) as tong_so,
    SUM(CASE WHEN trangthai = 'dung_gio' THEN 1 ELSE 0 END) as dung_gio,
    SUM(CASE WHEN trangthai = 'muon' THEN 1 ELSE 0 END) as muon
FROM phieudiemdanh 
WHERE ca IS NOT NULL
GROUP BY ca
ORDER BY ca;

-- Hiển thị mẫu dữ liệu mới nhất
SELECT '=== MẪU DỮ LIỆU MỚI NHẤT ===' as '';
SELECT 
    id,
    masinhvien,
    tensinhvien,
    ca,
    giovao,
    giora,
    trangthai,
    ngay,
    created_at
FROM phieudiemdanh 
ORDER BY created_at DESC 
LIMIT 10;

SELECT '=== HOÀN THÀNH CẬP NHẬT HỆ THỐNG ĐIỂM DANH ===' as '';
