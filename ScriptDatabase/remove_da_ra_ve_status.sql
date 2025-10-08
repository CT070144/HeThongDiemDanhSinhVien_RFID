-- Script để bỏ trạng thái "Đã ra về" (DA_RA_VE) khỏi hệ thống
-- Chạy script này sau khi cập nhật code backend và frontend

USE rfid_attendance_system;

-- Bước 1: Kiểm tra dữ liệu hiện có với trạng thái "da_ra_ve"
SELECT '=== KIỂM TRA DỮ LIỆU HIỆN CÓ ===' as '';
SELECT 
    COUNT(*) as 'Số bản ghi có trạng thái da_ra_ve'
FROM phieudiemdanh 
WHERE trangthai = 'da_ra_ve';

-- Hiển thị một số bản ghi mẫu
SELECT 
    id,
    rfid,
    masinhvien,
    tensinhvien,
    ngay,
    ca,
    giovao,
    giora,
    trangthai,
    created_at
FROM phieudiemdanh 
WHERE trangthai = 'da_ra_ve'
ORDER BY created_at DESC
LIMIT 10;

-- Bước 2: Cập nhật tất cả bản ghi có trạng thái "da_ra_ve" thành "dang_hoc"
UPDATE phieudiemdanh 
SET trangthai = 'dang_hoc'
WHERE trangthai = 'da_ra_ve';

-- Bước 3: Kiểm tra kết quả sau khi cập nhật
SELECT '=== KẾT QUẢ SAU KHI CẬP NHẬT ===' as '';
SELECT 
    COUNT(*) as 'Số bản ghi còn lại có trạng thái da_ra_ve'
FROM phieudiemdanh 
WHERE trangthai = 'da_ra_ve';

-- Thống kê trạng thái sau khi cập nhật
SELECT 
    trangthai,
    COUNT(*) as 'Số lượng'
FROM phieudiemdanh 
GROUP BY trangthai
ORDER BY trangthai;

-- Bước 4: Cập nhật view v_lich_su_diem_danh nếu cần
-- (View sẽ tự động cập nhật vì nó query từ bảng phieudiemdanh)

-- Bước 5: Kiểm tra dữ liệu trong view
SELECT '=== KIỂM TRA VIEW ===' as '';
SELECT 
    trangthai,
    COUNT(*) as 'Số lượng trong view'
FROM v_lich_su_diem_danh 
GROUP BY trangthai
ORDER BY trangthai;

-- Bước 6: Hiển thị một số bản ghi mẫu sau khi cập nhật
SELECT '=== DỮ LIỆU MẪU SAU KHI CẬP NHẬT ===' as '';
SELECT 
    id,
    rfid,
    masinhvien,
    tensinhvien,
    ngay,
    ca,
    giovao,
    giora,
    trangthai,
    CASE
        WHEN giovao IS NOT NULL AND giora IS NOT NULL THEN 'Hoàn thành'
        WHEN giovao IS NOT NULL AND giora IS NULL THEN 'Đang học'
        ELSE 'Chưa điểm danh'
    END as tinh_trang,
    created_at
FROM phieudiemdanh 
WHERE giora IS NOT NULL  -- Chỉ hiển thị những bản ghi đã có giờ ra
ORDER BY created_at DESC
LIMIT 10;

COMMIT;

SELECT '=== HOÀN THÀNH CẬP NHẬT ===' as '';
SELECT 'Trạng thái "Đã ra về" đã được loại bỏ khỏi hệ thống' as 'Kết quả';
