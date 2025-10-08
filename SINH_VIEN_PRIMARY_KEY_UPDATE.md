# Cập nhật cấu trúc SinhVien - Thay đổi khóa chính từ RFID sang Mã sinh viên

## Tóm tắt thay đổi:

### ✅ **1. Entity SinhVien**
- **Khóa chính**: Thay đổi từ `rfid` sang `maSinhVien`
- **RFID**: Vẫn unique nhưng có thể chỉnh sửa
- **Constructor**: Cập nhật thứ tự tham số: `SinhVien(maSinhVien, rfid, tenSinhVien)`

```java
@Entity
@Table(name = "sinhvien")
public class SinhVien {
    @Id
    @Column(name = "masinhvien", length = 20)
    private String maSinhVien;
    
    @Column(name = "rfid", length = 50, unique = true)
    private String rfid;
    
    // ... other fields
}
```

### ✅ **2. SinhVienService**
- **updateSinhVien()**: Sử dụng `maSinhVien` làm tham số thay vì `rfid`
- **deleteSinhVien()**: Sử dụng `maSinhVien` làm tham số
- **RFID Validation**: Kiểm tra RFID trùng lặp khi cập nhật
- **Sync Logic**: Cập nhật RFID trong các bảng liên quan khi thay đổi

### ✅ **3. SinhVienController**
- **GET /{maSinhVien}**: Lấy sinh viên theo mã sinh viên
- **GET /rfid/{rfid}**: Lấy sinh viên theo RFID (giữ nguyên cho tương thích)
- **PUT /{maSinhVien}**: Cập nhật sinh viên theo mã sinh viên
- **DELETE /{maSinhVien}**: Xóa sinh viên theo mã sinh viên

### ✅ **4. PhieuDiemDanhRepository**
- **updateStudentInfoByRfid()**: Cập nhật signature để nhận cả `oldRfid` và `newRfid`
- **Sync Logic**: Cập nhật RFID trong bảng phieudiemdanh khi sinh viên thay đổi RFID

### ✅ **5. LopHocPhanService**
- **Excel Import**: Sửa constructor call để phù hợp với thứ tự tham số mới
- **Student Creation**: Tạo sinh viên với RFID tạm thời nếu chưa có RFID

### ✅ **6. Database Migration Script**
- **update_sinhvien_primary_key.sql**: Script để cập nhật cấu trúc database
- **Backup**: Tự động backup dữ liệu trước khi thay đổi
- **Foreign Keys**: Cập nhật các ràng buộc khóa ngoại
- **Index**: Tạo index cho RFID để tối ưu tìm kiếm

## Lợi ích của thay đổi:

### 🎯 **1. Tính nhất quán**
- Mã sinh viên là định danh chính thức của sinh viên
- RFID chỉ là phương tiện điểm danh, có thể thay đổi

### 🔧 **2. Tính linh hoạt**
- RFID có thể chỉnh sửa khi cần thiết (thẻ bị hỏng, thay thẻ mới)
- Mã sinh viên không đổi, đảm bảo tính nhất quán dữ liệu

### 🚀 **3. Hiệu suất**
- Index trên RFID để tìm kiếm nhanh
- Khóa chính trên mã sinh viên cho các bảng liên quan

### 🛡️ **4. Bảo mật**
- Mã sinh viên không thay đổi, đảm bảo tính toàn vẹn dữ liệu
- RFID có thể reset khi cần thiết

## Cách thực hiện migration:

### **Bước 1: Backup Database**
```sql
-- Script tự động backup trong update_sinhvien_primary_key.sql
CREATE TABLE sinhvien_backup AS SELECT * FROM sinhvien;
```

### **Bước 2: Chạy Migration Script**
```bash
mysql -u username -p rfid_attendance_system < ScriptDatabase/update_sinhvien_primary_key.sql
```

### **Bước 3: Kiểm tra kết quả**
```sql
-- Kiểm tra cấu trúc bảng
DESCRIBE sinhvien;

-- Kiểm tra dữ liệu
SELECT COUNT(*) FROM sinhvien;
SELECT masinhvien, rfid, tensinhvien FROM sinhvien LIMIT 5;
```

## API Endpoints mới:

### **Sinh viên theo mã sinh viên:**
- `GET /api/sinhvien/{maSinhVien}` - Lấy sinh viên theo mã
- `PUT /api/sinhvien/{maSinhVien}` - Cập nhật sinh viên theo mã
- `DELETE /api/sinhvien/{maSinhVien}` - Xóa sinh viên theo mã

### **Sinh viên theo RFID (tương thích ngược):**
- `GET /api/sinhvien/rfid/{rfid}` - Lấy sinh viên theo RFID

### **Kiểm tra tồn tại:**
- `GET /api/sinhvien/exists/{rfid}` - Kiểm tra RFID có tồn tại

## Lưu ý quan trọng:

### ⚠️ **1. Tương thích ngược**
- Các API cũ vẫn hoạt động với endpoint `/rfid/{rfid}`
- Frontend cần cập nhật để sử dụng mã sinh viên làm khóa chính

### ⚠️ **2. Data Integrity**
- Script migration tự động xử lý foreign key constraints
- Backup tự động trước khi thay đổi

### ⚠️ **3. Testing**
- Test kỹ các chức năng điểm danh sau khi migration
- Kiểm tra tính toàn vẹn dữ liệu trong các bảng liên quan

## Kết quả mong đợi:

1. ✅ **Mã sinh viên** là khóa chính duy nhất
2. ✅ **RFID** vẫn unique nhưng có thể chỉnh sửa
3. ✅ **Tương thích ngược** với các API hiện tại
4. ✅ **Performance** được cải thiện với index phù hợp
5. ✅ **Data integrity** được đảm bảo với foreign keys

## Files đã thay đổi:

### **Backend:**
- `SinhVien.java` - Entity với khóa chính mới
- `SinhVienService.java` - Service logic cập nhật
- `SinhVienController.java` - Controller endpoints mới
- `PhieuDiemDanhRepository.java` - Repository methods cập nhật
- `LopHocPhanService.java` - Import logic sửa

### **Database:**
- `update_sinhvien_primary_key.sql` - Migration script

### **Documentation:**
- `SINH_VIEN_PRIMARY_KEY_UPDATE.md` - Tài liệu này

Hệ thống đã sẵn sàng để migration và sử dụng với cấu trúc mới!
