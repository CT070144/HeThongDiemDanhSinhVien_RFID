-- Script tạo dữ liệu mẫu cho bảng phieudiemdanh ngày 12/10/2025
-- Chạy script này sau khi đã có dữ liệu sinh viên trong bảng sinhvien

-- Xóa dữ liệu cũ nếu có (tùy chọn)
-- DELETE FROM phieudiemdanh WHERE ngay = '2025-10-12';

-- Dữ liệu mẫu cho Ca 1 (7:00-9:25)
INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
('RFID_KMA_01', 'CT070201', 'Nguyễn Văn An', 'A101', '06:55:00', '09:20:00', '2025-10-12', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_02', 'CT070202', 'Trần Thị Bình', 'A101', '07:05:00', '09:15:00', '2025-10-12', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_03', 'CT070203', 'Lê Văn Cường', 'A101', '06:50:00', NULL, '2025-10-12', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_04', 'CT070204', 'Phạm Thị Dung', 'A101', '07:10:00', '09:25:00', '2025-10-12', 1, 'muon', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_05', 'CT070205', 'Hoàng Văn Em', 'A101', '06:45:00', '09:10:00', '2025-10-12', 1, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_06', 'CT070206', 'Vũ Thị Phương', 'A101', '07:15:00', NULL, '2025-10-12', 1, 'muon', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_07', 'CT070207', 'Đỗ Văn Giang', 'A101', '06:58:00', '09:22:00', '2025-10-12', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_08', 'CT070208', 'Bùi Thị Hoa', 'A101', '07:20:00', '09:18:00', '2025-10-12', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_09', 'CT070209', 'Ngô Văn Inh', 'A101', '06:52:00', NULL, '2025-10-12', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_10', 'CT070210', 'Lý Thị Kim', 'A101', '07:25:00', '09:28:00', '2025-10-12', 1, 'muon', 'DA_RA_VE', NOW(), NOW());

-- Dữ liệu mẫu cho Ca 2 (9:35-12:00)
INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
('RFID_KMA_11', 'CT070211', 'Đinh Văn Lâm', 'A102', '09:30:00', '11:55:00', '2025-10-12', 2, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_12', 'CT070212', 'Cao Thị Mai', 'A102', '09:40:00', '11:45:00', '2025-10-12', 2, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_13', 'CT070213', 'Võ Văn Nam', 'A102', '09:32:00', NULL, '2025-10-12', 2, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_14', 'CT070214', 'Trương Thị Oanh', 'A102', '09:45:00', '12:00:00', '2025-10-12', 2, 'muon', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_15', 'CT070215', 'Lâm Văn Phúc', 'A102', '09:28:00', '11:50:00', '2025-10-12', 2, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_16', 'CT070216', 'Hồ Thị Quỳnh', 'A102', '09:50:00', NULL, '2025-10-12', 2, 'muon', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_17', 'CT070217', 'Phan Văn Rồng', 'A102', '09:33:00', '11:58:00', '2025-10-12', 2, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_18', 'CT070218', 'Đặng Thị Sương', 'A102', '09:42:00', '11:52:00', '2025-10-12', 2, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_19', 'CT070219', 'Chu Văn Tùng', 'A102', '09:35:00', NULL, '2025-10-12', 2, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_20', 'CT070220', 'Lưu Thị Uyên', 'A102', '09:48:00', '12:02:00', '2025-10-12', 2, 'muon', 'DA_RA_VE', NOW(), NOW());

-- Dữ liệu mẫu cho Ca 3 (12:30-14:55)
INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
('RFID_KMA_21', 'CT070221', 'Tôn Văn Vinh', 'A103', '12:25:00', '14:50:00', '2025-10-12', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_22', 'CT070222', 'Nguyễn Thị Xuân', 'A103', '12:35:00', '14:40:00', '2025-10-12', 3, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_23', 'CT070223', 'Trần Văn Yên', 'A103', '12:28:00', NULL, '2025-10-12', 3, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_24', 'CT070224', 'Lê Thị Zin', 'A103', '12:40:00', '14:55:00', '2025-10-12', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_25', 'CT070225', 'Phạm Văn Anh', 'A103', '12:22:00', '14:48:00', '2025-10-12', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_26', 'CT070226', 'Hoàng Thị Bảo', 'A103', '12:45:00', NULL, '2025-10-12', 3, 'muon', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_27', 'CT070227', 'Vũ Văn Châu', 'A103', '12:30:00', '14:52:00', '2025-10-12', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_28', 'CT070228', 'Đỗ Thị Duyên', 'A103', '12:38:00', '14:45:00', '2025-10-12', 3, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_29', 'CT070229', 'Bùi Văn Em', 'A103', '12:26:00', NULL, '2025-10-12', 3, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_30', 'CT070230', 'Ngô Thị Phương', 'A103', '12:42:00', '15:00:00', '2025-10-12', 3, 'muon', 'DA_RA_VE', NOW(), NOW());

-- Dữ liệu mẫu cho Ca 4 (15:05-17:30)
INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
('RFID_KMA_31', 'CT070231', 'Lý Văn Giang', 'A104', '15:00:00', '17:25:00', '2025-10-12', 4, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_32', 'CT070232', 'Đinh Thị Hoa', 'A104', '15:10:00', '17:15:00', '2025-10-12', 4, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_33', 'CT070233', 'Cao Văn Inh', 'A104', '15:03:00', NULL, '2025-10-12', 4, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_34', 'CT070234', 'Võ Thị Kim', 'A104', '15:15:00', '17:30:00', '2025-10-12', 4, 'muon', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_35', 'CT070235', 'Trương Văn Lâm', 'A104', '14:58:00', '17:20:00', '2025-10-12', 4, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_36', 'CT070236', 'Lâm Thị Mai', 'A104', '15:20:00', NULL, '2025-10-12', 4, 'muon', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_37', 'CT070237', 'Hồ Văn Nam', 'A104', '15:05:00', '17:28:00', '2025-10-12', 4, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_38', 'CT070238', 'Phan Thị Oanh', 'A104', '15:12:00', '17:22:00', '2025-10-12', 4, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_39', 'CT070239', 'Đặng Văn Phúc', 'A104', '15:01:00', NULL, '2025-10-12', 4, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_40', 'CT070240', 'Chu Thị Quỳnh', 'A104', '15:18:00', '17:32:00', '2025-10-12', 4, 'muon', 'DA_RA_VE', NOW(), NOW());

-- Dữ liệu mẫu cho Ca 5 (18:00-20:30)
INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
('RFID_KMA_41', 'CT070241', 'Lưu Văn Rồng', 'A105', '17:55:00', '20:25:00', '2025-10-12', 5, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_42', 'CT070242', 'Tôn Thị Sương', 'A105', '18:05:00', '20:15:00', '2025-10-12', 5, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_43', 'CT070243', 'Nguyễn Văn Tùng', 'A105', '17:58:00', NULL, '2025-10-12', 5, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_44', 'CT070244', 'Trần Thị Uyên', 'A105', '18:10:00', '20:30:00', '2025-10-12', 5, 'muon', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_45', 'CT070245', 'Lê Văn Vinh', 'A105', '17:52:00', '20:20:00', '2025-10-12', 5, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_46', 'CT070246', 'Phạm Thị Xuân', 'A105', '18:15:00', NULL, '2025-10-12', 5, 'muon', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_47', 'CT070247', 'Hoàng Văn Yên', 'A105', '18:00:00', '20:28:00', '2025-10-12', 5, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_48', 'CT070248', 'Vũ Thị Zin', 'A105', '18:08:00', '20:22:00', '2025-10-12', 5, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_49', 'CT070249', 'Đỗ Văn Anh', 'A105', '17:57:00', NULL, '2025-10-12', 5, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_50', 'CT070250', 'Bùi Thị Bảo', 'A105', '18:12:00', '20:32:00', '2025-10-12', 5, 'muon', 'DA_RA_VE', NOW(), NOW());

-- Thêm một số sinh viên không điểm danh (để test trạng thái "Không điểm danh ra")
-- Các sinh viên này sẽ được scheduled task tự động cập nhật trạng thái
INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
('RFID_KMA_51', 'CT070251', 'Ngô Văn Châu', 'A101', '06:58:00', NULL, '2025-10-12', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_52', 'CT070252', 'Lý Thị Duyên', 'A102', '09:35:00', NULL, '2025-10-12', 2, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_53', 'CT070253', 'Đinh Văn Em', 'A103', '12:30:00', NULL, '2025-10-12', 3, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_54', 'CT070254', 'Cao Thị Phương', 'A104', '15:05:00', NULL, '2025-10-12', 4, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_55', 'CT070255', 'Võ Văn Giang', 'A105', '18:00:00', NULL, '2025-10-12', 5, 'dung_gio', 'DANG_HOC', NOW(), NOW());

-- Hiển thị thống kê dữ liệu đã tạo
SELECT 
    ca,
    COUNT(*) as total_records,
    COUNT(CASE WHEN trangthai = 'DANG_HOC' THEN 1 END) as dang_hoc,
    COUNT(CASE WHEN trangthai = 'DA_RA_VE' THEN 1 END) as da_ra_ve,
    COUNT(CASE WHEN trangthai = 'RA_VE_SOM' THEN 1 END) as ra_ve_som,
    COUNT(CASE WHEN trangthai = 'KHONG_DIEM_DANH_RA' THEN 1 END) as khong_diem_danh_ra,
    COUNT(CASE WHEN tinhtrangdiemdanh = 'dung_gio' THEN 1 END) as dung_gio,
    COUNT(CASE WHEN tinhtrangdiemdanh = 'muon' THEN 1 END) as muon
FROM phieudiemdanh 
WHERE ngay = '2025-10-12'
GROUP BY ca
ORDER BY ca;

-- Hiển thị tổng quan
SELECT 
    'Tổng số bản ghi' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE ngay = '2025-10-12'

UNION ALL

SELECT 
    'Sinh viên đang học' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE ngay = '2025-10-12' AND trangthai = 'DANG_HOC'

UNION ALL

SELECT 
    'Sinh viên đã ra về' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE ngay = '2025-10-12' AND trangthai = 'DA_RA_VE'

UNION ALL

SELECT 
    'Sinh viên ra về sớm' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE ngay = '2025-10-12' AND trangthai = 'RA_VE_SOM'

UNION ALL

SELECT 
    'Điểm danh đúng giờ' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE ngay = '2025-10-12' AND tinhtrangdiemdanh = 'dung_gio'

UNION ALL

SELECT 
    'Điểm danh muộn' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE ngay = '2025-10-12' AND tinhtrangdiemdanh = 'muon';
