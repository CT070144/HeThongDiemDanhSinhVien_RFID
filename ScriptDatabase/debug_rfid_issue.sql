-- Script debug vấn đề RFID không tìm được sinh viên
-- Chạy script này để kiểm tra dữ liệu trong database

USE rfid_attendance_system;

-- 1. Kiểm tra dữ liệu sinh viên
SELECT '=== KIỂM TRA DỮ LIỆU SINH VIÊN ===' as '';
SELECT COUNT(*) as 'Tổng số sinh viên' FROM sinhvien;

-- 2. Hiển thị danh sách sinh viên với RFID
SELECT '=== DANH SÁCH SINH VIÊN VÀ RFID ===' as '';
SELECT 
    masinhvien as 'Mã sinh viên',
    rfid as 'RFID',
    tensinhvien as 'Tên sinh viên',
    created_at as 'Ngày tạo'
FROM sinhvien 
ORDER BY created_at DESC
LIMIT 10;

-- 3. Kiểm tra RFID có trùng lặp không
SELECT '=== KIỂM TRA RFID TRÙNG LẶP ===' as '';
SELECT 
    rfid,
    COUNT(*) as 'Số lần xuất hiện'
FROM sinhvien 
GROUP BY rfid 
HAVING COUNT(*) > 1;

-- 4. Kiểm tra RFID có giá trị NULL hoặc rỗng
SELECT '=== KIỂM TRA RFID NULL/RỖNG ===' as '';
SELECT 
    masinhvien,
    rfid,
    tensinhvien
FROM sinhvien 
WHERE rfid IS NULL OR rfid = '' OR TRIM(rfid) = '';

-- 5. Kiểm tra độ dài RFID
SELECT '=== KIỂM TRA ĐỘ DÀI RFID ===' as '';
SELECT 
    rfid,
    LENGTH(rfid) as 'Độ dài',
    masinhvien,
    tensinhvien
FROM sinhvien 
ORDER BY LENGTH(rfid) DESC;

-- 6. Kiểm tra RFID có ký tự đặc biệt
SELECT '=== KIỂM TRA KÝ TỰ ĐẶC BIỆT TRONG RFID ===' as '';
SELECT 
    rfid,
    masinhvien,
    tensinhvien
FROM sinhvien 
WHERE rfid REGEXP '[^a-zA-Z0-9]';

-- 7. Test query tìm RFID cụ thể (thay 'RFID001' bằng RFID thực tế)
SELECT '=== TEST TÌM RFID CỤ THỂ ===' as '';
SET @test_rfid = 'RFID001'; -- Thay đổi giá trị này
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN CONCAT('Tìm thấy RFID: ', @test_rfid)
        ELSE CONCAT('KHÔNG tìm thấy RFID: ', @test_rfid)
    END as 'Kết quả tìm kiếm'
FROM sinhvien 
WHERE rfid = @test_rfid;

-- 8. Hiển thị chi tiết nếu tìm thấy
SELECT 
    masinhvien,
    rfid,
    tensinhvien,
    created_at
FROM sinhvien 
WHERE rfid = @test_rfid;

-- 9. Kiểm tra bảng docrfid (RFID chưa đăng ký)
SELECT '=== KIỂM TRA RFID CHƯA ĐĂNG KÝ ===' as '';
SELECT COUNT(*) as 'Số RFID chưa đăng ký' FROM docrfid;
SELECT * FROM docrfid ORDER BY created_at DESC LIMIT 5;

-- 10. Kiểm tra phiếu điểm danh gần đây
SELECT '=== PHIẾU ĐIỂM DANH GẦN ĐÂY ===' as '';
SELECT 
    rfid,
    masinhvien,
    tensinhvien,
    ngay,
    ca,
    giovao,
    trangthai
FROM phieudiemdanh 
ORDER BY created_at DESC 
LIMIT 5;

-- 11. Kiểm tra cấu trúc bảng sinhvien
SELECT '=== CẤU TRÚC BẢNG SINHVIEN ===' as '';
DESCRIBE sinhvien;

-- 12. Kiểm tra index
SELECT '=== INDEX CỦA BẢNG SINHVIEN ===' as '';
SHOW INDEX FROM sinhvien;

-- 13. Test case sensitivity
SELECT '=== TEST CASE SENSITIVITY ===' as '';
SELECT 
    'RFID001' as 'Test RFID',
    CASE 
        WHEN EXISTS(SELECT 1 FROM sinhvien WHERE rfid = 'RFID001') THEN 'Tìm thấy (chính xác)'
        WHEN EXISTS(SELECT 1 FROM sinhvien WHERE UPPER(rfid) = 'RFID001') THEN 'Tìm thấy (không phân biệt hoa thường)'
        ELSE 'Không tìm thấy'
    END as 'Kết quả';

-- 14. Kiểm tra collation của database
SELECT '=== COLLATION DATABASE ===' as '';
SELECT 
    SCHEMA_NAME,
    DEFAULT_COLLATION_NAME
FROM information_schema.SCHEMATA 
WHERE SCHEMA_NAME = 'rfid_attendance_system';

-- 15. Kiểm tra collation của cột rfid
SELECT '=== COLLATION CỘT RFID ===' as '';
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLLATION_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'rfid_attendance_system' 
AND TABLE_NAME = 'sinhvien' 
AND COLUMN_NAME = 'rfid';

SELECT '=== HOÀN THÀNH DEBUG ===' as '';
