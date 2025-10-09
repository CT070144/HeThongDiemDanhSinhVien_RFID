# Cập nhật hệ thống điểm danh RFID

## 📋 **Tổng quan thay đổi**

Hệ thống điểm danh đã được cập nhật theo yêu cầu mới với 5 ca học và chỉ 2 trạng thái điểm danh.

## ⏰ **Lịch ca học mới**

| Ca | Thời gian học | Thời gian điểm danh | Trạng thái |
|---|---|---|---|
| **Ca 1** | 7:00 - 9:25 | 6:50 - 9:35 | Đúng giờ: trước 7:00<br>Muộn: từ 7:00 trở đi |
| **Ca 2** | 9:35 - 12:00 | 9:25 - 12:10 | Đúng giờ: trước 9:35<br>Muộn: từ 9:35 trở đi |
| **Ca 3** | 12:30 - 14:55 | 12:20 - 15:05 | Đúng giờ: trước 12:30<br>Muộn: từ 12:30 trở đi |
| **Ca 4** | 15:05 - 17:30 | 14:55 - 17:40 | Đúng giờ: trước 15:05<br>Muộn: từ 15:05 trở đi |
| **Ca 5** | 18:00 - 20:30 | 17:50 - 20:40 | Đúng giờ: trước 18:00<br>Muộn: từ 18:00 trở đi |

## 🎯 **Quy tắc điểm danh**

### **Điểm danh đúng giờ:**
- Sinh viên điểm danh **TRƯỚC** khi ca học bắt đầu
- Ví dụ: Ca 1 bắt đầu lúc 7:00, điểm danh trước 7:00 = đúng giờ

### **Điểm danh muộn:**
- Sinh viên điểm danh **SAU** khi ca học đã bắt đầu
- Ví dụ: Ca 1 bắt đầu lúc 7:00, điểm danh từ 7:00 trở đi = muộn

## 📊 **Trạng thái mới**

| Trạng thái | Mã | Mô tả |
|---|---|---|
| `DUNG_GIO` | `dung_gio` | Điểm danh đúng giờ |
| `MUON` | `muon` | Điểm danh muộn |

## 🔧 **Thay đổi kỹ thuật**

### **1. Entity PhieuDiemDanh**
- Cập nhật enum `TrangThai` chỉ còn 2 giá trị
- Loại bỏ trạng thái `DANG_HOC` cũ

### **2. AttendanceService**
- Cập nhật logic `getCurrentCa()` hỗ trợ 5 ca học
- Cập nhật logic `determineAttendanceStatus()` theo quy tắc mới
- Mở rộng thời gian điểm danh (10 phút trước ca học)

### **3. TrangThaiConverter**
- Cập nhật converter để hỗ trợ enum mới
- Tự động chuyển đổi dữ liệu cũ sang mới

## 📝 **Script cập nhật database**

Chạy script `ScriptDatabase/update_attendance_system.sql` để:
- Cập nhật dữ liệu cũ sang trạng thái mới
- Hiển thị thống kê sau cập nhật
- Kiểm tra tính toàn vẹn dữ liệu

## 🚀 **Triển khai**

1. **Backup database** trước khi cập nhật
2. **Chạy script SQL** để cập nhật dữ liệu
3. **Deploy code mới** lên server
4. **Test hệ thống** với các ca học khác nhau

## ✅ **Kiểm tra**

- [ ] Database đã được cập nhật
- [ ] Code đã được deploy
- [ ] Test điểm danh ca 1 (6:50-9:35)
- [ ] Test điểm danh ca 2 (9:25-12:10)
- [ ] Test điểm danh ca 3 (12:20-15:05)
- [ ] Test điểm danh ca 4 (14:55-17:40)
- [ ] Test điểm danh ca 5 (17:50-20:40)
- [ ] Test ngoài giờ học (trả về lỗi)

## 📞 **Hỗ trợ**

Nếu có vấn đề trong quá trình triển khai, vui lòng liên hệ team phát triển.
