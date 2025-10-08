-- Script kiểm tra và sửa tính toàn vẹn dữ liệu
-- Chạy script này để kiểm tra và sửa các vấn đề dữ liệu

USE rfid_attendance_system;

-- 1. Kiểm tra dữ liệu trong bảng sinhvien
SELECT 'SinhVien Table Check' as Check_Type, COUNT(*) as Count FROM sinhvien;

-- 2. Kiểm tra dữ liệu trong bảng phieudiemdanh
SELECT 'PhieuDiemDanh Table Check' as Check_Type, COUNT(*) as Count FROM phieudiemdanh;

-- 3. Kiểm tra dữ liệu trong bảng lophocphan (nếu có)
SELECT 'LopHocPhan Table Check' as Check_Type, COUNT(*) as Count FROM lophocphan;

-- 4. Kiểm tra dữ liệu trong bảng sinhvienlophocphan (nếu có)
SELECT 'SinhVienLopHocPhan Table Check' as Check_Type, COUNT(*) as Count FROM sinhvienlophocphan;

-- 5. Kiểm tra các bản ghi phieudiemdanh có mã sinh viên không tồn tại trong bảng sinhvien
SELECT 
    'Orphaned PhieuDiemDanh Records' as Check_Type,
    COUNT(*) as Count
FROM phieudiemdanh p
LEFT JOIN sinhvien s ON p.masinhvien = s.masinhvien
WHERE s.masinhvien IS NULL;

-- 6. Hiển thị các bản ghi phieudiemdanh có vấn đề
SELECT 
    p.id,
    p.masinhvien,
    p.tensinhvien,
    p.ngay,
    p.ca,
    'Missing in SinhVien table' as Issue
FROM phieudiemdanh p
LEFT JOIN sinhvien s ON p.masinhvien = s.masinhvien
WHERE s.masinhvien IS NULL
LIMIT 10;

-- 7. Kiểm tra các bản ghi sinhvienlophocphan có mã sinh viên không tồn tại
SELECT 
    'Orphaned SinhVienLopHocPhan Records' as Check_Type,
    COUNT(*) as Count
FROM sinhvienlophocphan slhp
LEFT JOIN sinhvien s ON slhp.masinhvien = s.masinhvien
WHERE s.masinhvien IS NULL;

-- 8. Kiểm tra các bản ghi sinhvienlophocphan có mã lớp học phần không tồn tại
SELECT 
    'Orphaned SinhVienLopHocPhan Records (LopHocPhan)' as Check_Type,
    COUNT(*) as Count
FROM sinhvienlophocphan slhp
LEFT JOIN lophocphan lhp ON slhp.malophocphan = lhp.malophocphan
WHERE lhp.malophocphan IS NULL;

-- 9. Tạo dữ liệu mẫu cho sinh viên nếu cần
-- Uncomment các dòng dưới nếu cần tạo dữ liệu mẫu
/*
INSERT IGNORE INTO sinhvien (rfid, masinhvien, tensinhvien) VALUES
('RFID_CT070201', 'CT070201', 'Sinh viên mẫu CT070201'),
('RFID_CT070301', 'CT070301', 'Sinh viên mẫu CT070301'),
('RFID_CT070401', 'CT070401', 'Sinh viên mẫu CT070401');
*/

-- 10. Xóa các bản ghi phieudiemdanh có mã sinh viên không tồn tại (THẬN TRỌNG!)
-- Uncomment dòng dưới nếu muốn xóa các bản ghi orphaned
/*
DELETE p FROM phieudiemdanh p
LEFT JOIN sinhvien s ON p.masinhvien = s.masinhvien
WHERE s.masinhvien IS NULL;
*/

-- 11. Hiển thị thông tin chi tiết về các bảng
SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    DATA_LENGTH,
    INDEX_LENGTH
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'rfid_attendance_system'
ORDER BY TABLE_NAME;
