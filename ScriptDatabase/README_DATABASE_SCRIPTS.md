# Hướng dẫn sử dụng Script Database RFID Attendance System

## 📁 Danh sách Scripts

### 1. **`create_database_complete.sql`** ⭐ **KHUYÊN DÙNG**
- **Mục đích**: Tạo toàn bộ database với dữ liệu mẫu
- **Bao gồm**: 
  - Cấu trúc bảng, index, view
  - Stored procedures và functions
  - Triggers
  - Dữ liệu mẫu đầy đủ
  - Permissions và grants
- **Sử dụng khi**: Lần đầu setup hệ thống hoặc demo

### 2. **`reset_and_create_database.sql`** 🔄
- **Mục đích**: Reset hoàn toàn và tạo lại database
- **Bao gồm**:
  - Xóa database cũ
  - Tạo lại cấu trúc mới
  - Dữ liệu mẫu cơ bản
- **Sử dụng khi**: Muốn xóa hết dữ liệu cũ và bắt đầu lại

### 3. **`create_database_structure_only.sql`** 🏗️
- **Mục đích**: Chỉ tạo cấu trúc bảng, không có dữ liệu
- **Bao gồm**:
  - Cấu trúc bảng, index, view
  - Không có dữ liệu mẫu
- **Sử dụng khi**: Setup production hoặc muốn tự import dữ liệu

### 4. **`update_sinhvien_primary_key.sql`** 🔧
- **Mục đích**: Migration từ cấu trúc cũ sang mới
- **Bao gồm**:
  - Backup dữ liệu
  - Thay đổi khóa chính
  - Cập nhật foreign keys
- **Sử dụng khi**: Đã có database cũ, muốn chuyển sang cấu trúc mới

## 🚀 Cách sử dụng

### **Option 1: Setup mới hoàn toàn (Khuyến nghị)**
```bash
# Tạo database với dữ liệu mẫu
mysql -u root -p < ScriptDatabase/create_database_complete.sql
```

### **Option 2: Reset và tạo lại**
```bash
# ⚠️ CẢNH BÁO: Xóa toàn bộ dữ liệu cũ
mysql -u root -p < ScriptDatabase/reset_and_create_database.sql
```

### **Option 3: Chỉ tạo cấu trúc**
```bash
# Tạo cấu trúc không có dữ liệu mẫu
mysql -u root -p < ScriptDatabase/create_database_structure_only.sql
```

### **Option 4: Migration từ cấu trúc cũ**
```bash
# Chuyển từ cấu trúc cũ sang mới (giữ dữ liệu)
mysql -u root -p < ScriptDatabase/update_sinhvien_primary_key.sql
```

## 📊 Cấu trúc Database mới

### **Bảng chính:**
1. **`sinhvien`** - Thông tin sinh viên
   - **Khóa chính**: `masinhvien`
   - **RFID**: `unique`, có thể chỉnh sửa

2. **`lophocphan`** - Lớp học phần
   - **Khóa chính**: `malophocphan`

3. **`sinhvienlophocphan`** - Liên kết sinh viên-lớp học phần
   - **Khóa chính**: `(masinhvien, malophocphan)`

4. **`phieudiemdanh`** - Phiếu điểm danh
   - **Khóa chính**: `id` (auto increment)
   - **Foreign key**: `masinhvien`

5. **`thietbi`** - Thiết bị RFID
   - **Khóa chính**: `mathietbi`

6. **`docrfid`** - Thẻ RFID chưa đăng ký
   - **Khóa chính**: `id` (auto increment)

### **View:**
- **`v_lich_su_diem_danh`** - View lịch sử điểm danh

### **Indexes:**
- Tối ưu cho tìm kiếm RFID, mã sinh viên, ngày, ca học

## 🔍 Kiểm tra sau khi chạy script

### **1. Kiểm tra cấu trúc bảng:**
```sql
USE rfid_attendance_system;
SHOW TABLES;
DESCRIBE sinhvien;
DESCRIBE lophocphan;
DESCRIBE phieudiemdanh;
```

### **2. Kiểm tra dữ liệu (nếu có mẫu):**
```sql
SELECT COUNT(*) FROM sinhvien;
SELECT COUNT(*) FROM lophocphan;
SELECT COUNT(*) FROM sinhvienlophocphan;
SELECT COUNT(*) FROM phieudiemdanh;
```

### **3. Kiểm tra view:**
```sql
SELECT * FROM v_lich_su_diem_danh LIMIT 5;
```

### **4. Kiểm tra index:**
```sql
SHOW INDEX FROM sinhvien;
SHOW INDEX FROM phieudiemdanh;
```

## ⚠️ Lưu ý quan trọng

### **Trước khi chạy script:**
1. **Backup dữ liệu** nếu đã có database cũ
2. **Kiểm tra quyền** user MySQL có đủ quyền tạo database
3. **Đóng ứng dụng** đang sử dụng database

### **Sau khi chạy script:**
1. **Restart backend** để áp dụng cấu trúc mới
2. **Test các chức năng** điểm danh và quản lý sinh viên
3. **Kiểm tra log** để đảm bảo không có lỗi

## 🔧 Troubleshooting

### **Lỗi "Access denied":**
```bash
# Đăng nhập với user có quyền cao hơn
mysql -u root -p
```

### **Lỗi "Database exists":**
```bash
# Sử dụng script reset hoặc xóa database cũ
mysql -u root -p -e "DROP DATABASE rfid_attendance_system;"
```

### **Lỗi "Table exists":**
```bash
# Script tự động DROP TABLE trước khi tạo mới
# Nếu vẫn lỗi, chạy thủ công:
mysql -u root -p -e "USE rfid_attendance_system; DROP TABLE IF EXISTS sinhvien;"
```

## 📝 Dữ liệu mẫu (nếu có)

### **Sinh viên mẫu:**
- 10 sinh viên với mã từ CT070201 đến CT070210
- RFID từ RFID001 đến RFID010

### **Lớp học phần mẫu:**
- CNPMN-L01, CNPMN-L02 (Công nghệ phần mềm nhúng)
- CNTT-L01, CNTT-L02 (Công nghệ thông tin)
- KTPM-L01 (Kỹ thuật phần mềm)

### **Thiết bị mẫu:**
- 5 thiết bị từ TB001 đến TB005
- Phòng học từ A101, A102, B201, B202, C301

### **Liên kết mẫu:**
- Sinh viên được phân vào các lớp học phần
- Một số sinh viên tham gia nhiều lớp

## 🎯 Kết quả mong đợi

Sau khi chạy script thành công:
1. ✅ Database `rfid_attendance_system` được tạo
2. ✅ 6 bảng chính với cấu trúc đúng
3. ✅ Index được tạo để tối ưu hiệu suất
4. ✅ View `v_lich_su_diem_danh` hoạt động
5. ✅ Dữ liệu mẫu (nếu có) được insert
6. ✅ Foreign key constraints hoạt động
7. ✅ Backend có thể kết nối và hoạt động bình thường

## 📞 Hỗ trợ

Nếu gặp vấn đề:
1. Kiểm tra log MySQL: `tail -f /var/log/mysql/error.log`
2. Kiểm tra quyền user: `SHOW GRANTS FOR 'user'@'localhost';`
3. Kiểm tra version MySQL: `SELECT VERSION();`

**Chúc bạn setup thành công! 🎉**
