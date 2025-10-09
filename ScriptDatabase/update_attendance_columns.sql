-- Script cập nhật cột trong bảng phieudiemdanh
USE rfid_attendance_system;

-- Thêm cột mới tình trạng điểm danh
ALTER TABLE phieudiemdanh 
ADD COLUMN tinhtrangdiemdanh VARCHAR(20) DEFAULT 'dung_gio';

-- Thêm cột mới trạng thái (đang học/đã ra về)
ALTER TABLE phieudiemdanh 
ADD COLUMN trangthai_new VARCHAR(20) DEFAULT 'DANG_HOC';

-- Cập nhật dữ liệu từ cột cũ sang cột mới
UPDATE phieudiemdanh 
SET tinhtrangdiemdanh = trangthai
WHERE tinhtrangdiemdanh IS NULL;

-- Cập nhật trạng thái dựa trên giờ ra
UPDATE phieudiemdanh 
SET trangthai_new = CASE 
    WHEN giora IS NULL THEN 'DANG_HOC'
    ELSE 'DA_RA_VE'
END
WHERE trangthai_new IS NULL;

-- Xóa cột cũ
ALTER TABLE phieudiemdanh DROP COLUMN trangthai;

-- Đổi tên cột mới
ALTER TABLE phieudiemdanh CHANGE COLUMN trangthai_new trangthai VARCHAR(20);

-- Cập nhật NOT NULL constraint cho tình trạng điểm danh
ALTER TABLE phieudiemdanh 
MODIFY COLUMN tinhtrangdiemdanh VARCHAR(20) NOT NULL DEFAULT 'dung_gio';

-- Hiển thị thống kê sau cập nhật
SELECT '=== THỐNG KÊ TÌNH TRẠNG ĐIỂM DANH ===' as '';
SELECT 
    tinhtrangdiemdanh,
    COUNT(*) as so_luong,
    CASE 
        WHEN tinhtrangdiemdanh = 'dung_gio' THEN 'Điểm danh đúng giờ'
        WHEN tinhtrangdiemdanh = 'muon' THEN 'Điểm danh muộn'
        ELSE tinhtrangdiemdanh
    END as mo_ta
FROM phieudiemdanh 
GROUP BY tinhtrangdiemdanh
ORDER BY tinhtrangdiemdanh;

SELECT '=== THỐNG KÊ TRẠNG THÁI HỌC ===' as '';
SELECT 
    trangthai,
    COUNT(*) as so_luong,
    CASE 
        WHEN trangthai = 'DANG_HOC' THEN 'Đang học'
        WHEN trangthai = 'DA_RA_VE' THEN 'Đã ra về'
        ELSE trangthai
    END as mo_ta
FROM phieudiemdanh 
GROUP BY trangthai
ORDER BY trangthai;

-- Hiển thị mẫu dữ liệu mới
SELECT '=== MẪU DỮ LIỆU SAU CẬP NHẬT ===' as '';
SELECT 
    id,
    masinhvien,
    tensinhvien,
    ca,
    giovao,
    giora,
    tinhtrangdiemdanh,
    trangthai,
    ngay,
    created_at
FROM phieudiemdanh 
ORDER BY created_at DESC 
LIMIT 10;

SELECT '=== HOÀN THÀNH CẬP NHẬT CỘT PHIẾU ĐIỂM DANH ===' as '';
