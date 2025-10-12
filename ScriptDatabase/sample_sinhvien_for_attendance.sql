-- Script tạo dữ liệu sinh viên mẫu cho bảng sinhvien
-- Chạy script này trước khi chạy sample_phieudiemdanh_12_10_2025.sql

-- Xóa dữ liệu cũ nếu có (tùy chọn - cẩn thận khi chạy)
-- DELETE FROM sinhvien WHERE masinhvien LIKE 'CT0702%';

-- Tạo dữ liệu sinh viên mẫu
INSERT INTO sinhvien (masinhvien, rfid, tensinhvien, created_at, updated_at) VALUES
('CT070201', 'RFID_KMA_01', 'Nguyễn Văn An', NOW(), NOW()),
('CT070202', 'RFID_KMA_02', 'Trần Thị Bình', NOW(), NOW()),
('CT070203', 'RFID_KMA_03', 'Lê Văn Cường', NOW(), NOW()),
('CT070204', 'RFID_KMA_04', 'Phạm Thị Dung', NOW(), NOW()),
('CT070205', 'RFID_KMA_05', 'Hoàng Văn Em', NOW(), NOW()),
('CT070206', 'RFID_KMA_06', 'Vũ Thị Phương', NOW(), NOW()),
('CT070207', 'RFID_KMA_07', 'Đỗ Văn Giang', NOW(), NOW()),
('CT070208', 'RFID_KMA_08', 'Bùi Thị Hoa', NOW(), NOW()),
('CT070209', 'RFID_KMA_09', 'Ngô Văn Inh', NOW(), NOW()),
('CT070210', 'RFID_KMA_10', 'Lý Thị Kim', NOW(), NOW()),
('CT070211', 'RFID_KMA_11', 'Đinh Văn Lâm', NOW(), NOW()),
('CT070212', 'RFID_KMA_12', 'Cao Thị Mai', NOW(), NOW()),
('CT070213', 'RFID_KMA_13', 'Võ Văn Nam', NOW(), NOW()),
('CT070214', 'RFID_KMA_14', 'Trương Thị Oanh', NOW(), NOW()),
('CT070215', 'RFID_KMA_15', 'Lâm Văn Phúc', NOW(), NOW()),
('CT070216', 'RFID_KMA_16', 'Hồ Thị Quỳnh', NOW(), NOW()),
('CT070217', 'RFID_KMA_17', 'Phan Văn Rồng', NOW(), NOW()),
('CT070218', 'RFID_KMA_18', 'Đặng Thị Sương', NOW(), NOW()),
('CT070219', 'RFID_KMA_19', 'Chu Văn Tùng', NOW(), NOW()),
('CT070220', 'RFID_KMA_20', 'Lưu Thị Uyên', NOW(), NOW()),
('CT070221', 'RFID_KMA_21', 'Tôn Văn Vinh', NOW(), NOW()),
('CT070222', 'RFID_KMA_22', 'Nguyễn Thị Xuân', NOW(), NOW()),
('CT070223', 'RFID_KMA_23', 'Trần Văn Yên', NOW(), NOW()),
('CT070224', 'RFID_KMA_24', 'Lê Thị Zin', NOW(), NOW()),
('CT070225', 'RFID_KMA_25', 'Phạm Văn Anh', NOW(), NOW()),
('CT070226', 'RFID_KMA_26', 'Hoàng Thị Bảo', NOW(), NOW()),
('CT070227', 'RFID_KMA_27', 'Vũ Văn Châu', NOW(), NOW()),
('CT070228', 'RFID_KMA_28', 'Đỗ Thị Duyên', NOW(), NOW()),
('CT070229', 'RFID_KMA_29', 'Bùi Văn Em', NOW(), NOW()),
('CT070230', 'RFID_KMA_30', 'Ngô Thị Phương', NOW(), NOW()),
('CT070231', 'RFID_KMA_31', 'Lý Văn Giang', NOW(), NOW()),
('CT070232', 'RFID_KMA_32', 'Đinh Thị Hoa', NOW(), NOW()),
('CT070233', 'RFID_KMA_33', 'Cao Văn Inh', NOW(), NOW()),
('CT070234', 'RFID_KMA_34', 'Võ Thị Kim', NOW(), NOW()),
('CT070235', 'RFID_KMA_35', 'Trương Văn Lâm', NOW(), NOW()),
('CT070236', 'RFID_KMA_36', 'Lâm Thị Mai', NOW(), NOW()),
('CT070237', 'RFID_KMA_37', 'Hồ Văn Nam', NOW(), NOW()),
('CT070238', 'RFID_KMA_38', 'Phan Thị Oanh', NOW(), NOW()),
('CT070239', 'RFID_KMA_39', 'Đặng Văn Phúc', NOW(), NOW()),
('CT070240', 'RFID_KMA_40', 'Chu Thị Quỳnh', NOW(), NOW()),
('CT070241', 'RFID_KMA_41', 'Lưu Văn Rồng', NOW(), NOW()),
('CT070242', 'RFID_KMA_42', 'Tôn Thị Sương', NOW(), NOW()),
('CT070243', 'RFID_KMA_43', 'Nguyễn Văn Tùng', NOW(), NOW()),
('CT070244', 'RFID_KMA_44', 'Trần Thị Uyên', NOW(), NOW()),
('CT070245', 'RFID_KMA_45', 'Lê Văn Vinh', NOW(), NOW()),
('CT070246', 'RFID_KMA_46', 'Phạm Thị Xuân', NOW(), NOW()),
('CT070247', 'RFID_KMA_47', 'Hoàng Văn Yên', NOW(), NOW()),
('CT070248', 'RFID_KMA_48', 'Vũ Thị Zin', NOW(), NOW()),
('CT070249', 'RFID_KMA_49', 'Đỗ Văn Anh', NOW(), NOW()),
('CT070250', 'RFID_KMA_50', 'Bùi Thị Bảo', NOW(), NOW()),
('CT070251', 'RFID_KMA_51', 'Ngô Văn Châu', NOW(), NOW()),
('CT070252', 'RFID_KMA_52', 'Lý Thị Duyên', NOW(), NOW()),
('CT070253', 'RFID_KMA_53', 'Đinh Văn Em', NOW(), NOW()),
('CT070254', 'RFID_KMA_54', 'Cao Thị Phương', NOW(), NOW()),
('CT070255', 'RFID_KMA_55', 'Võ Văn Giang', NOW(), NOW());

-- Hiển thị thống kê sinh viên đã tạo
SELECT 
    COUNT(*) as total_students,
    COUNT(CASE WHEN rfid LIKE 'RFID_KMA_%' THEN 1 END) as students_with_rfid
FROM sinhvien 
WHERE masinhvien LIKE 'CT0702%';

-- Hiển thị danh sách sinh viên
SELECT masinhvien, rfid, tensinhvien 
FROM sinhvien 
WHERE masinhvien LIKE 'CT0702%' 
ORDER BY masinhvien;
