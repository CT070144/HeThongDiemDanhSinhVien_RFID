# Cập nhật phiếu điểm danh - Thêm cột trạng thái mới

## 📋 **Tổng quan thay đổi**

Hệ thống phiếu điểm danh đã được cập nhật với các thay đổi sau:

### **1. Đổi tên cột**
- Cột `trangthai` cũ → `tinhtrangdiemdanh` (Tình trạng điểm danh)
- Thêm cột mới `trangthai` (Trạng thái học tập)

### **2. Cấu trúc cột mới**

| Tên cột | Mô tả | Giá trị |
|---|---|---|
| **tinhTrangDiemDanh** | Tình trạng điểm danh | `dung_gio`, `muon` |
| **trangThai** | Trạng thái học tập | `DANG_HOC`, `DA_RA_VE` |

## 🎯 **Logic hoạt động**

### **Khi sinh viên điểm danh vào:**
- `tinhTrangDiemDanh`: `dung_gio` (nếu trước giờ học) hoặc `muon` (nếu sau giờ học)
- `trangThai`: `DANG_HOC` (đang học)

### **Khi sinh viên điểm danh ra:**
- `tinhTrangDiemDanh`: Giữ nguyên (không thay đổi)
- `trangThai`: `DA_RA_VE` (đã ra về)

## 📊 **Frontend Updates**

### **1. Bảng lịch sử điểm danh**
- Thêm cột "Tình trạng điểm danh" với badge màu:
  - 🟢 **Đúng giờ** (xanh lá)
  - 🟡 **Muộn** (vàng)
- Thêm cột "Trạng thái" với badge màu:
  - 🔵 **Đang học** (xanh dương)
  - ⚫ **Đã ra về** (xám)

### **2. Xuất Excel**
- **Không xuất** cột "Trạng thái" (đang học/đã ra về)
- Chỉ xuất cột "Tình trạng điểm danh"
- Header: "Tình trạng điểm danh" thay vì "Trạng thái"

### **3. Thống kê**
- Thêm số lượng sinh viên **đang học**
- Thêm số lượng sinh viên **đã ra về**
- Hiển thị trong cả UI và file Excel

## 🔧 **Backend Updates**

### **1. Entity PhieuDiemDanh**
```java
// Cột cũ
@Column(name = "trangthai")
private TrangThai trangThai;

// Cột mới
@Column(name = "tinhtrangdiemdanh") 
private TrangThai tinhTrangDiemDanh;

@Column(name = "trangthai")
private TrangThaiHoc trangThai;
```

### **2. Enum mới**
```java
public enum TrangThaiHoc {
    DANG_HOC("Đang học"),
    DA_RA_VE("Đã ra về");
}
```

### **3. AttendanceService**
- Cập nhật logic tạo phiếu điểm danh mới
- Cập nhật logic điểm danh ra
- Sử dụng `tinhTrangDiemDanh` cho việc xác định đúng giờ/muộn
- Sử dụng `trangThai` cho việc theo dõi trạng thái học tập

## 📝 **Script Database**

### **Chạy script cập nhật:**
```sql
-- ScriptDatabase/update_attendance_columns.sql
```

**Thực hiện:**
1. Thêm cột `tinhtrangdiemdanh`
2. Thêm cột `trangthai_new`
3. Cập nhật dữ liệu từ cột cũ
4. Xóa cột cũ và đổi tên cột mới
5. Hiển thị thống kê sau cập nhật

## 🚀 **Triển khai**

### **Bước 1: Cập nhật Database**
```bash
mysql -u root -p rfid_attendance_system < ScriptDatabase/update_attendance_columns.sql
```

### **Bước 2: Deploy Backend**
- Deploy code mới với entity và service đã cập nhật
- Restart ứng dụng

### **Bước 3: Deploy Frontend**
- Deploy code mới với AttendanceHistory.js đã cập nhật
- Test giao diện và chức năng xuất Excel

## ✅ **Kiểm tra**

### **Backend:**
- [ ] Entity PhieuDiemDanh có 2 trường mới
- [ ] AttendanceService logic hoạt động đúng
- [ ] API trả về dữ liệu với cấu trúc mới

### **Frontend:**
- [ ] Bảng hiển thị 2 cột mới
- [ ] Badge màu sắc đúng
- [ ] Xuất Excel không có cột "Trạng thái"
- [ ] Thống kê hiển thị số lượng đang học/đã ra về

### **Database:**
- [ ] Dữ liệu cũ được chuyển đổi đúng
- [ ] Cột mới có dữ liệu phù hợp
- [ ] Không mất dữ liệu

## 📞 **Lưu ý**

- **Backup database** trước khi chạy script cập nhật
- Test kỹ chức năng điểm danh vào/ra
- Kiểm tra xuất Excel có đúng format
- Thống kê phải hiển thị đầy đủ các chỉ số mới
