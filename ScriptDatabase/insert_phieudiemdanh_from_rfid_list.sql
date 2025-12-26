-- =====================================================
-- Script tạo dữ liệu cho bảng phieudiemdanh
-- Dựa trên danh sách RFID và logic trong AttendanceService.java
-- Ngày tạo: 2025-01-XX
-- =====================================================

USE rfid_attendance_system;

-- Lưu ý: Script này giả định các sinh viên đã tồn tại trong bảng sinhvien
-- với các RFID tương ứng. Nếu chưa có, cần tạo trước.

-- =====================================================
-- DỮ LIỆU CHO CA 1 (7:00-9:25)
-- Có thể điểm danh từ 6:50 - 9:35
-- Đúng giờ: trước 7:00, Muộn: từ 7:00 trở đi
-- Ra về sớm: trước 9:05, Đã ra về: từ 9:05 trở đi
-- =====================================================

INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
-- Phòng 102-TA1
('RFID_KMA_04', 'CT070204', 'Phạm Thị Dung', '102-TA1', '06:55:00', '09:20:00', '2025-10-27', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_05', 'CT070205', 'Hoàng Văn Em', '102-TA1', '07:05:00', '09:15:00', '2025-11-03', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_06', 'CT070206', 'Vũ Thị Phương', '102-TA1', '06:50:00', NULL, '2025-11-10', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_07', 'CT070207', 'Đỗ Văn Giang', '102-TA1', '07:10:00', '09:25:00', '2025-10-29', 1, 'muon', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_08', 'CT070208', 'Bùi Thị Hoa', '102-TA1', '06:45:00', '09:10:00', '2025-11-05', 1, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),

-- Phòng 106-TB1
('RFID_KMA_09', 'CT070209', 'Ngô Văn Inh', '106-TB1', '06:58:00', '09:22:00', '2025-11-12', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_10', 'CT070210', 'Lý Thị Kim', '106-TB1', '07:15:00', NULL, '2025-11-17', 1, 'muon', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_11', 'CT070211', 'Đinh Văn Lâm', '106-TB1', '07:20:00', '09:18:00', '2025-11-24', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_12', 'CT070212', 'Cao Thị Mai', '106-TB1', '06:52:00', NULL, '2025-12-01', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 201-TA1
('RFID_KMA_13', 'CT070213', 'Võ Văn Nam', '201-TA1', '07:25:00', '09:28:00', '2025-12-08', 1, 'muon', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_14', 'CT070214', 'Trương Thị Oanh', '201-TA1', '06:48:00', '09:12:00', '2025-12-15', 1, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_15', 'CT070215', 'Lâm Văn Phúc', '201-TA1', '07:00:00', NULL, '2025-11-26', 1, 'muon', 'DANG_HOC', NOW(), NOW());

-- =====================================================
-- DỮ LIỆU CHO CA 2 (9:35-12:00)
-- Có thể điểm danh từ 9:25 - 12:30
-- Đúng giờ: trước 9:35, Muộn: từ 9:35 trở đi
-- Ra về sớm: trước 11:40, Đã ra về: từ 11:40 trở đi
-- =====================================================

INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
-- Phòng 202-TA1
('RFID_KMA_16', 'CT070216', 'Hồ Thị Quỳnh', '202-TA1', '09:30:00', '11:55:00', '2025-12-03', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_17', 'CT070217', 'Phan Văn Rồng', '202-TA1', '09:40:00', '11:45:00', '2025-12-10', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_18', 'CT070218', 'Đặng Thị Sương', '202-TA1', '09:32:00', NULL, '2025-12-17', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_19', 'CT070219', 'Chu Văn Tùng', '202-TA1', '09:45:00', '12:00:00', '2025-12-22', 1, 'muon', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_20', 'CT070220', 'Lưu Thị Uyên', '202-TA1', '09:28:00', '11:50:00', '2025-12-24', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),

-- Phòng 203-TA1
('RFID_KMA_22', 'CT070222', 'Nguyễn Thị Xuân', '203-TA1', '09:50:00', NULL, '2025-12-26', 1, 'muon', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_23', 'CT070223', 'Trần Văn Yên', '203-TA1', '09:33:00', '11:58:00', '2025-10-27', 2, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_24', 'CT070224', 'Lê Thị Zin', '203-TA1', '09:42:00', '11:52:00', '2025-11-03', 2, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_25', 'CT070225', 'Phạm Văn Anh', '203-TA1', '09:35:00', NULL, '2025-11-10', 2, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 204-TB5
('RFID_KMA_26', 'CT070226', 'Hoàng Thị Bảo', '204-TB5', '09:48:00', '12:02:00', '2025-10-29', 2, 'muon', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_27', 'CT070227', 'Vũ Văn Châu', '204-TB5', '09:25:00', '11:38:00', '2025-11-05', 2, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_28', 'CT070228', 'Đỗ Thị Duyên', '204-TB5', '09:36:00', NULL, '2025-11-12', 2, 'muon', 'DANG_HOC', NOW(), NOW());

-- =====================================================
-- DỮ LIỆU CHO CA 3 (12:30-14:55)
-- Có thể điểm danh từ 12:20 - 15:05
-- Đúng giờ: trước 12:30, Muộn: từ 12:30 trở đi
-- Ra về sớm: trước 14:35, Đã ra về: từ 14:35 trở đi
-- =====================================================

INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
-- Phòng 301-TA1
('RFID_KMA_29', 'CT070229', 'Bùi Văn Em', '301-TA1', '12:25:00', '14:50:00', '2025-11-17', 2, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_30', 'CT070230', 'Ngô Thị Phương', '301-TA1', '12:35:00', '14:40:00', '2025-11-24', 2, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_31', 'CT070231', 'Lý Văn Giang', '301-TA1', '12:28:00', NULL, '2025-12-01', 2, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_32', 'CT070232', 'Đinh Thị Hoa', '301-TA1', '12:40:00', '14:55:00', '2025-12-08', 2, 'muon', 'DA_RA_VE', NOW(), NOW()),

-- Phòng 302-TA1
('RFID_KMA_33', 'CT070233', 'Cao Văn Inh', '302-TA1', '12:22:00', '14:48:00', '2025-12-15', 2, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_34', 'CT070234', 'Võ Thị Kim', '302-TA1', '12:45:00', NULL, '2025-11-26', 2, 'muon', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_35', 'CT070235', 'Trương Văn Lâm', '302-TA1', '12:30:00', '14:52:00', '2025-12-03', 2, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_36', 'CT070236', 'Lâm Thị Mai', '302-TA1', '12:38:00', '14:45:00', '2025-12-10', 2, 'muon', 'RA_VE_SOM', NOW(), NOW()),

-- Phòng 303-TA1
('RFID_KMA_37', 'CT070237', 'Hồ Văn Nam', '303-TA1', '12:26:00', NULL, '2025-12-17', 2, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_38', 'CT070238', 'Phan Thị Oanh', '303-TA1', '12:42:00', '15:00:00', '2025-12-22', 2, 'muon', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_39', 'CT070239', 'Đặng Văn Phúc', '303-TA1', '12:20:00', '14:32:00', '2025-12-24', 2, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),

-- Phòng 304-TA1
('RFID_KMA_40', 'CT070240', 'Chu Thị Quỳnh', '304-TA1', '12:33:00', NULL, '2025-12-26', 2, 'muon', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_41', 'CT070241', 'Lưu Văn Rồng', '304-TA1', '12:24:00', '14:50:00', '2025-10-27', 4, 'dung_gio', 'DA_RA_VE', NOW(), NOW());

-- =====================================================
-- DỮ LIỆU CHO CA 4 (15:05-17:30)
-- Có thể điểm danh từ 14:55 - 17:40
-- Đúng giờ: trước 15:05, Muộn: từ 15:05 trở đi
-- Ra về sớm: trước 17:10, Đã ra về: từ 17:10 trở đi
-- =====================================================

INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
-- Phòng 401-TA1
('RFID_KMA_42', 'CT070242', 'Tôn Thị Sương', '401-TA1', '15:00:00', '17:25:00', '2025-11-03', 4, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_43', 'CT070243', 'Nguyễn Văn Tùng', '401-TA1', '15:10:00', '17:15:00', '2025-11-10', 4, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_44', 'CT070244', 'Trần Thị Uyên', '401-TA1', '15:03:00', NULL, '2025-10-29', 4, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_45', 'CT070245', 'Lê Văn Vinh', '401-TA1', '15:15:00', '17:30:00', '2025-11-05', 4, 'muon', 'DA_RA_VE', NOW(), NOW()),

-- Phòng 402-TA1
('RFID_KMA_46', 'CT070246', 'Phạm Thị Xuân', '402-TA1', '14:58:00', '17:20:00', '2025-11-12', 4, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_47', 'CT070247', 'Hoàng Văn Yên', '402-TA1', '15:20:00', NULL, '2025-11-17', 4, 'muon', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_48', 'CT070248', 'Vũ Thị Zin', '402-TA1', '15:05:00', '17:28:00', '2025-11-24', 4, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),

-- Phòng 403-TA1
('RFID_KMA_49', 'CT070249', 'Đỗ Văn Anh', '403-TA1', '15:12:00', '17:22:00', '2025-12-01', 4, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_50', 'CT070250', 'Bùi Thị Bảo', '403-TA1', '15:01:00', NULL, '2025-12-08', 4, 'dung_gio', 'DANG_HOC', NOW(), NOW()),
('RFID_KMA_51', 'CT070251', 'Ngô Văn Châu', '403-TA1', '15:18:00', '17:32:00', '2025-12-15', 4, 'muon', 'DA_RA_VE', NOW(), NOW()),

-- Phòng 404-TA1
('RFID_KMA_52', 'CT070252', 'Lý Thị Duyên', '404-TA1', '14:56:00', '17:12:00', '2025-11-26', 4, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_53', 'CT070253', 'Đinh Văn Em', '404-TA1', '15:08:00', NULL, '2025-12-03', 4, 'muon', 'DANG_HOC', NOW(), NOW());

-- =====================================================
-- DỮ LIỆU CHO CA 5 (18:00-20:30)
-- Có thể điểm danh từ 17:50 - 20:30
-- Đúng giờ: trước 18:00, Muộn: từ 18:00 trở đi
-- Ra về sớm: trước 20:10, Đã ra về: từ 20:10 trở đi
-- =====================================================

INSERT INTO phieudiemdanh (rfid, masinhvien, tensinhvien, phonghoc, giovao, giora, ngay, ca, tinhtrangdiemdanh, trangthai, created_at, updated_at) VALUES
-- Phòng 502-TA1
('RFID_KMA_54', 'CT070254', 'Cao Thị Phương', '502-TA1', '17:55:00', '20:25:00', '2025-12-10', 4, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('RFID_KMA_55', 'CT070255', 'Võ Văn Giang', '502-TA1', '18:05:00', '20:15:00', '2025-12-17', 4, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('RFID_KMA_56', 'CT070256', 'Trương Văn Lâm', '502-TA1', '17:58:00', NULL, '2025-12-22', 4, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 503-TA1
('TEMP_CT050207_1765012675947', 'CT050207', 'Nguyễn Văn A', '503-TA1', '18:10:00', '20:30:00', '2025-12-24', 4, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT050221_1765012676101', 'CT050221', 'Trần Thị B', '503-TA1', '17:52:00', '20:20:00', '2025-12-26', 4, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT060410_1765012674578', 'CT060410', 'Lê Văn C', '503-TA1', '18:15:00', NULL, '2025-08-11', 1, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 504-TA1
('TEMP_CT060435_1765012675065', 'CT060435', 'Phạm Thị D', '504-TA1', '18:00:00', '20:28:00', '2025-08-18', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT060437_1765012675034', 'CT060437', 'Hoàng Văn E', '504-TA1', '18:08:00', '20:22:00', '2025-08-13', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070101_1765012675241', 'CT070101', 'Vũ Thị F', '504-TA1', '17:57:00', NULL, '2025-08-20', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 601-TA1
('TEMP_CT070102_1765012675874', 'CT070102', 'Đỗ Văn G', '601-TA1', '18:12:00', '20:32:00', '2025-08-15', 1, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070103_1765012675205', 'CT070103', 'Bùi Thị H', '601-TA1', '17:50:00', '20:18:00', '2025-08-22', 1, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070104_1765012675218', 'CT070104', 'Ngô Văn I', '601-TA1', '18:03:00', NULL, '2025-08-25', 1, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 603-TA1
('TEMP_CT070106_1765012675891', 'CT070106', 'Lý Thị K', '603-TA1', '17:54:00', '20:26:00', '2025-08-27', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070109_1765012675282', 'CT070109', 'Đinh Văn L', '603-TA1', '18:07:00', '20:19:00', '2025-09-03', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070110_1765012674639', 'CT070110', 'Cao Thị M', '603-TA1', '18:20:00', NULL, '2025-09-05', 1, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 604-TA1
('TEMP_CT070112_1765012675941', 'CT070112', 'Võ Văn N', '604-TA1', '17:59:00', '20:24:00', '2025-09-10', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070113_1765012675295', 'CT070113', 'Trương Thị O', '604-TA1', '18:02:00', '20:16:00', '2025-09-17', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070115_1765012675308', 'CT070115', 'Lâm Văn P', '604-TA1', '17:56:00', NULL, '2025-09-24', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 701-TA1
('TEMP_CT070116_1765012675924', 'CT070116', 'Hồ Thị Q', '701-TA1', '18:11:00', '20:29:00', '2025-10-01', 1, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070118_1765012675908', 'CT070118', 'Phan Văn R', '701-TA1', '17:53:00', '20:21:00', '2025-10-08', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070120_1765012675957', 'CT070120', 'Đặng Thị S', '701-TA1', '18:04:00', NULL, '2025-09-12', 1, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 703-TA1
('TEMP_CT070123_1765012675381', 'CT070123', 'Chu Văn T', '703-TA1', '17:51:00', '20:17:00', '2025-09-19', 1, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070124_1765012676058', 'CT070124', 'Lưu Thị U', '703-TA1', '18:06:00', '20:27:00', '2025-09-26', 1, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070125_1765012675391', 'CT070125', 'Tôn Văn V', '703-TA1', '18:18:00', NULL, '2025-10-03', 1, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 802-TA1
('TEMP_CT070127_1765012674740', 'CT070127', 'Nguyễn Thị X', '802-TA1', '17:48:00', '20:14:00', '2025-10-10', 1, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070129_1765012675423', 'CT070129', 'Trần Văn Y', '802-TA1', '18:01:00', '20:23:00', '2025-08-11', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070134_1765012676168', 'CT070134', 'Lê Thị Z', '802-TA1', '17:55:00', NULL, '2025-08-18', 3, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 502-TA1 (tiếp tục)
('TEMP_CT070140_1765012676203', 'CT070140', 'Phạm Văn AA', '502-TA1', '18:09:00', '20:31:00', '2025-08-13', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070142_1765012675539', 'CT070142', 'Hoàng Thị BB', '502-TA1', '17:49:00', '20:13:00', '2025-08-20', 3, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),

-- Phòng 503-TA1 (tiếp tục)
('TEMP_CT070143_1765012675565', 'CT070143', 'Vũ Văn CC', '503-TA1', '18:14:00', NULL, '2025-08-15', 3, 'muon', 'DANG_HOC', NOW(), NOW()),
('TEMP_CT070144_1765012676262', 'CT070144', 'Đỗ Thị DD', '503-TA1', '17:46:00', '20:11:00', '2025-08-22', 3, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070145_1765012675614', 'CT070145', 'Bùi Văn EE', '503-TA1', '18:16:00', '20:33:00', '2025-08-25', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),

-- Phòng 504-TA1 (tiếp tục)
('TEMP_CT070146_1765012675605', 'CT070146', 'Ngô Thị FF', '504-TA1', '17:58:00', '20:25:00', '2025-08-27', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070148_1765012676303', 'CT070148', 'Lý Văn GG', '504-TA1', '18:13:00', NULL, '2025-09-03', 3, 'muon', 'DANG_HOC', NOW(), NOW()),
('TEMP_CT070149_1765012674989', 'CT070149', 'Đinh Thị HH', '504-TA1', '17:47:00', '20:12:00', '2025-09-05', 3, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),

-- Phòng 601-TA1 (tiếp tục)
('TEMP_CT070150_1765012676312', 'CT070150', 'Cao Văn II', '601-TA1', '18:17:00', '20:34:00', '2025-09-10', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070153_1765012675687', 'CT070153', 'Võ Thị JJ', '601-TA1', '17:59:00', '20:26:00', '2025-09-12', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070154_1765012675719', 'CT070154', 'Trương Văn KK', '601-TA1', '18:19:00', NULL, '2025-09-15', 3, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 603-TA1 (tiếp tục)
('TEMP_CT070155_1765012675732', 'CT070155', 'Lâm Thị LL', '603-TA1', '17:45:00', '20:10:00', '2025-09-22', 3, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070157_1765012675770', 'CT070157', 'Hồ Văn MM', '603-TA1', '18:21:00', '20:35:00', '2025-09-29', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070158_1765012676391', 'CT070158', 'Phan Thị NN', '603-TA1', '18:00:00', '20:27:00', '2025-10-06', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),

-- Phòng 604-TA1 (tiếp tục)
('TEMP_CT070159_1765012675799', 'CT070159', 'Đặng Văn OO', '604-TA1', '18:22:00', NULL, '2025-10-13', 3, 'muon', 'DANG_HOC', NOW(), NOW()),
('TEMP_CT070161_1765012675813', 'CT070161', 'Chu Thị PP', '604-TA1', '17:44:00', '20:09:00', '2025-10-20', 3, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070162_1765012675135', 'CT070162', 'Lưu Văn QQ', '604-TA1', '18:23:00', '20:36:00', '2025-10-27', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),

-- Phòng 701-TA1 (tiếp tục)
('TEMP_CT070163_1764690456445', 'CT070163', 'Tôn Thị RR', '701-TA1', '17:43:00', '20:08:00', '2025-11-03', 3, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070164_1764690456448', 'CT070164', 'Nguyễn Văn SS', '701-TA1', '18:24:00', NULL, '2025-09-17', 3, 'muon', 'DANG_HOC', NOW(), NOW()),
('TEMP_CT070165_1764690456688', 'CT070165', 'Trần Thị TT', '701-TA1', '18:01:00', '20:28:00', '2025-09-24', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),

-- Phòng 703-TA1 (tiếp tục)
('TEMP_CT070202_1764690455856', 'CT070202', 'Lê Văn UU', '703-TA1', '17:42:00', '20:07:00', '2025-10-01', 3, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070203_1764690455871', 'CT070203', 'Phạm Thị VV', '703-TA1', '18:25:00', '20:37:00', '2025-10-08', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070205_1764690455882', 'CT070205', 'Hoàng Văn WW', '703-TA1', '18:26:00', NULL, '2025-09-19', 3, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 802-TA1 (tiếp tục)
('TEMP_CT070206_1764690455887', 'CT070206', 'Vũ Thị XX', '802-TA1', '17:41:00', '20:06:00', '2025-09-26', 3, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070209_1764690455905', 'CT070209', 'Đỗ Văn YY', '802-TA1', '18:27:00', '20:38:00', '2025-10-03', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070211_1764690455918', 'CT070211', 'Bùi Thị ZZ', '802-TA1', '18:02:00', '20:29:00', '2025-10-10', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),

-- Phòng 102-TA1 (Ca 1 - bổ sung TEMP)
('TEMP_CT070214_1764690455933', 'CT070214', 'Ngô Văn AAA', '102-TA1', '06:56:00', '09:21:00', '2025-08-12', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070215_1764690455937', 'CT070215', 'Lý Thị BBB', '102-TA1', '07:06:00', '09:16:00', '2025-08-19', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070216_1764690455942', 'CT070216', 'Đinh Văn CCC', '102-TA1', '06:51:00', NULL, '2025-08-14', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 106-TB1 (Ca 1 - bổ sung TEMP)
('TEMP_CT070217_1764690455947', 'CT070217', 'Cao Thị DDD', '106-TB1', '07:11:00', '09:26:00', '2025-08-21', 1, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070218_1764690455954', 'CT070218', 'Võ Văn EEE', '106-TB1', '06:46:00', '09:11:00', '2025-08-26', 1, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070219_1764690455960', 'CT070219', 'Trương Thị FFF', '106-TB1', '07:21:00', NULL, '2025-08-28', 1, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 201-TA1 (Ca 1 - bổ sung TEMP)
('TEMP_CT070220_1764690455966', 'CT070220', 'Lâm Văn GGG', '201-TA1', '06:59:00', '09:23:00', '2025-09-04', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070221_1764690455972', 'CT070221', 'Hồ Thị HHH', '201-TA1', '07:16:00', '09:19:00', '2025-09-09', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070224_1764690455987', 'CT070224', 'Phan Văn III', '201-TA1', '06:53:00', NULL, '2025-09-16', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 202-TA1 (Ca 2 - bổ sung TEMP)
('TEMP_CT070225_1764690455993', 'CT070225', 'Đặng Thị JJJ', '202-TA1', '09:31:00', '11:56:00', '2025-09-11', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070226_1764690455997', 'CT070226', 'Chu Văn KKK', '202-TA1', '09:41:00', '11:46:00', '2025-09-18', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070227_1764690456003', 'CT070227', 'Lưu Thị LLL', '202-TA1', '09:33:00', NULL, '2025-09-22', 1, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 203-TA1 (Ca 2 - bổ sung TEMP)
('TEMP_CT070228_1764690456009', 'CT070228', 'Tôn Văn MMM', '203-TA1', '09:46:00', '12:01:00', '2025-09-23', 1, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070229_1764690456017', 'CT070229', 'Nguyễn Thị NNN', '203-TA1', '09:29:00', '11:51:00', '2025-09-25', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070230_1764690456023', 'CT070230', 'Trần Văn OOO', '203-TA1', '09:51:00', NULL, '2025-09-29', 1, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 204-TB5 (Ca 2 - bổ sung TEMP)
('TEMP_CT070231_1764690456029', 'CT070231', 'Lê Thị PPP', '204-TB5', '09:34:00', '11:59:00', '2025-10-06', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070232_1764690456036', 'CT070232', 'Phạm Văn QQQ', '204-TB5', '09:43:00', '11:53:00', '2025-09-30', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070233_1764690456040', 'CT070233', 'Hoàng Thị RRR', '204-TB5', '09:36:00', NULL, '2025-10-07', 1, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 301-TA1 (Ca 3 - bổ sung TEMP)
('TEMP_CT070234_1764690456046', 'CT070234', 'Vũ Văn SSS', '301-TA1', '12:26:00', '14:51:00', '2025-10-02', 1, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070235_1764690456053', 'CT070235', 'Đỗ Thị TTT', '301-TA1', '12:36:00', '14:41:00', '2025-10-09', 1, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070236_1764690456059', 'CT070236', 'Bùi Văn UUU', '301-TA1', '12:29:00', NULL, '2025-08-12', 3, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 302-TA1 (Ca 3 - bổ sung TEMP)
('TEMP_CT070237_1764690456065', 'CT070237', 'Ngô Thị VVV', '302-TA1', '12:41:00', '14:56:00', '2025-08-19', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070240_1764690456080', 'CT070240', 'Lý Văn WWW', '302-TA1', '12:23:00', '14:49:00', '2025-08-14', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070241_1764690456086', 'CT070241', 'Đinh Thị XXX', '302-TA1', '12:46:00', NULL, '2025-08-21', 3, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 303-TA1 (Ca 3 - bổ sung TEMP)
('TEMP_CT070242_1764690456091', 'CT070242', 'Cao Văn YYY', '303-TA1', '12:31:00', '14:53:00', '2025-08-26', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070243_1764690456097', 'CT070243', 'Võ Thị ZZZ', '303-TA1', '12:39:00', '14:46:00', '2025-08-28', 3, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070246_1764690456111', 'CT070246', 'Trương Văn AAAA', '303-TA1', '12:27:00', NULL, '2025-09-04', 3, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 304-TA1 (Ca 3 - bổ sung TEMP)
('TEMP_CT070247_1764690456115', 'CT070247', 'Lâm Thị BBBB', '304-TA1', '12:43:00', '15:01:00', '2025-09-09', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070248_1764690456120', 'CT070248', 'Hồ Văn CCCC', '304-TA1', '12:21:00', '14:33:00', '2025-09-16', 3, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070249_1764690456126', 'CT070249', 'Phan Thị DDDD', '304-TA1', '12:34:00', NULL, '2025-09-11', 3, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 401-TA1 (Ca 4 - bổ sung TEMP)
('TEMP_CT070250_1764690456129', 'CT070250', 'Đặng Văn EEEE', '401-TA1', '15:01:00', '17:26:00', '2025-09-18', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070251_1764690456132', 'CT070251', 'Chu Thị FFFF', '401-TA1', '15:11:00', '17:16:00', '2025-09-23', 3, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070253_1764690456141', 'CT070253', 'Lưu Văn GGGG', '401-TA1', '15:04:00', NULL, '2025-09-25', 3, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 402-TA1 (Ca 4 - bổ sung TEMP)
('TEMP_CT070254_1764690456146', 'CT070254', 'Tôn Thị HHHH', '402-TA1', '15:16:00', '17:31:00', '2025-09-30', 3, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070256_1764690456158', 'CT070256', 'Nguyễn Văn IIII', '402-TA1', '14:59:00', '17:21:00', '2025-10-07', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070257_1764690456424', 'CT070257', 'Trần Thị JJJJ', '402-TA1', '15:21:00', NULL, '2025-10-02', 3, 'muon', 'DANG_HOC', NOW(), NOW()),

-- Phòng 403-TA1 (Ca 4 - bổ sung TEMP)
('TEMP_CT070258_1765012676406', 'CT070258', 'Lê Văn KKKK', '403-TA1', '15:06:00', '17:29:00', '2025-10-09', 3, 'dung_gio', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070259_1764690456428', 'CT070259', 'Phạm Thị LLLL', '403-TA1', '15:13:00', '17:23:00', '2025-09-18', 4, 'muon', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070261_1765012676427', 'CT070261', 'Hoàng Văn MMMM', '403-TA1', '15:02:00', NULL, '2025-09-25', 4, 'dung_gio', 'DANG_HOC', NOW(), NOW()),

-- Phòng 404-TA1 (Ca 4 - bổ sung TEMP)
('TEMP_CT070262_1765012676434', 'CT070262', 'Vũ Thị NNNN', '404-TA1', '15:19:00', '17:33:00', '2025-10-02', 4, 'muon', 'DA_RA_VE', NOW(), NOW()),
('TEMP_CT070263_1764690456670', 'CT070263', 'Đỗ Văn OOOO', '404-TA1', '14:57:00', '17:13:00', '2025-10-09', 4, 'dung_gio', 'RA_VE_SOM', NOW(), NOW()),
('TEMP_CT070264_1764690456683', 'CT070264', 'Bùi Thị PPPP', '404-TA1', '15:09:00', NULL, '2025-10-16', 4, 'muon', 'DANG_HOC', NOW(), NOW()),
('TEMP_CT070265_1764690456679', 'CT070265', 'Ngô Văn QQQQ', '404-TA1', '15:07:00', '17:24:00', '2025-10-23', 4, 'muon', 'RA_VE_SOM', NOW(), NOW());

-- =====================================================
-- THỐNG KÊ DỮ LIỆU ĐÃ TẠO
-- =====================================================

SELECT '=== THỐNG KÊ THEO CA ===' as '';
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
GROUP BY ca
ORDER BY ca;

SELECT '=== THỐNG KÊ THEO PHÒNG HỌC ===' as '';
SELECT 
    phonghoc,
    COUNT(*) as total_students,
    COUNT(CASE WHEN trangthai = 'DANG_HOC' THEN 1 END) as dang_hoc,
    COUNT(CASE WHEN trangthai = 'DA_RA_VE' THEN 1 END) as da_ra_ve
FROM phieudiemdanh 
GROUP BY phonghoc
ORDER BY phonghoc;

SELECT '=== TỔNG QUAN ===' as '';
SELECT 
    'Tổng số bản ghi' as metric,
    COUNT(*) as value
FROM phieudiemdanh 

UNION ALL

SELECT 
    'Sinh viên đang học' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE trangthai = 'DANG_HOC'

UNION ALL

SELECT 
    'Sinh viên đã ra về' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE trangthai = 'DA_RA_VE'

UNION ALL

SELECT 
    'Sinh viên ra về sớm' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE trangthai = 'RA_VE_SOM'

UNION ALL

SELECT 
    'Điểm danh đúng giờ' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE tinhtrangdiemdanh = 'dung_gio'

UNION ALL

SELECT 
    'Điểm danh muộn' as metric,
    COUNT(*) as value
FROM phieudiemdanh 
WHERE tinhtrangdiemdanh = 'muon';

