# Hướng dẫn sử dụng tính năng Lớp học phần

## Tổng quan
Hệ thống RFID đã được cập nhật với tính năng quản lý lớp học phần, cho phép:
- Quản lý danh sách các lớp học phần
- Import danh sách sinh viên từ file Excel
- Lọc sinh viên theo lớp học phần
- Lọc lịch sử điểm danh theo lớp học phần
- Xuất báo cáo Excel với thống kê chi tiết

## 1. Quản lý Lớp học phần

### Truy cập trang quản lý
1. Đăng nhập vào hệ thống
2. Click vào menu "Lớp học phần" trên thanh điều hướng

### Thêm lớp học phần mới
1. Click nút "Thêm lớp học phần"
2. Nhập mã lớp học phần (ví dụ: CNPMN-L01)
3. Nhập tên lớp học phần (ví dụ: Công nghệ phần mềm nhúng-1-25 (C701))
4. Click "Lưu"

### Import danh sách sinh viên từ Excel
1. Click nút "Import Excel"
2. Chọn file Excel (.xls hoặc .xlsx)
3. Click "Import"

#### Cấu trúc file Excel:
- File có thể có nhiều sheet
- Mỗi sheet là một danh sách sinh viên của một lớp học phần
- **Hàng 6 (C6)**: Chứa tên lớp học phần
- **Dữ liệu sinh viên**: Bắt đầu từ hàng 10
  - **Cột B**: Mã sinh viên
  - **Cột C**: Tên sinh viên
  - **Cột D**: Lớp (tùy chọn)

#### Ví dụ cấu trúc file:
```
Hàng 5: DANH SÁCH ĐIỂM DANH SINH VIÊN
Hàng 6: Lớp: Công nghệ phần mềm nhúng-1-25 (C701)
Hàng 7: Học kỳ 1 - Năm học 2025 - 2026
Hàng 9: STT | Mã sinh viên | Họ và tên | Lớp
Hàng 10: 1 | CT070301 | Võ Hồng An | CT7A
Hàng 11: 2 | CT070201 | Vũ Quốc Anh | CT7B
```

### Xem danh sách sinh viên trong lớp
1. Click nút "Xem sinh viên" trong danh sách lớp học phần
2. Modal sẽ hiển thị danh sách tất cả sinh viên trong lớp đó

## 2. Quản lý Sinh viên

### Lọc sinh viên theo lớp học phần
1. Truy cập trang "Quản lý sinh viên"
2. Chọn lớp học phần từ dropdown "Lớp học phần"
3. Danh sách sinh viên sẽ được lọc theo lớp đã chọn

## 3. Lịch sử điểm danh

### Lọc theo lớp học phần
1. Truy cập trang "Lịch sử điểm danh"
2. Chọn lớp học phần từ dropdown "Lớp học phần"
3. Có thể kết hợp với các filter khác (ngày, ca học, mã sinh viên, phòng học)

### Thống kê lớp học phần
Khi lọc theo lớp học phần, hệ thống sẽ hiển thị:
- **Tổng số sinh viên**: Số sinh viên có trong lớp học phần
- **Tham gia**: Số sinh viên đã điểm danh
- **Vắng**: Số sinh viên không điểm danh
- **Muộn**: Số sinh viên điểm danh muộn

### Xuất báo cáo Excel

#### Xuất báo cáo thông thường
- Click "Xuất Excel" để xuất tất cả dữ liệu điểm danh hiện tại

#### Xuất báo cáo theo lớp học phần
1. Chọn lớp học phần từ filter
2. Click "Xuất Excel"
3. File Excel sẽ được tạo với:
   - **Tiêu đề**: Thông tin trường, lớp học phần, học kỳ
   - **Danh sách sinh viên**: Tất cả sinh viên trong lớp
   - **Cột điểm danh**: 
     - `x`: Có mặt
     - `v`: Vắng
     - `M`: Muộn
   - **Thống kê**: Tổng hợp số liệu ở cuối file

## 4. Cấu trúc Database

### Bảng mới được tạo:
- `lophocphan`: Lưu trữ thông tin lớp học phần
- `sinhvienlophocphan`: Bảng trung gian (nhiều-nhiều) giữa sinh viên và lớp học phần

### Chạy script cập nhật database:
```sql
-- Chạy file ScriptDatabase/update_database.sql
```

## 5. API Endpoints

### Lớp học phần:
- `GET /api/lophocphan` - Lấy danh sách tất cả lớp học phần
- `GET /api/lophocphan/{maLopHocPhan}` - Lấy thông tin lớp học phần
- `POST /api/lophocphan` - Tạo lớp học phần mới
- `PUT /api/lophocphan/{maLopHocPhan}` - Cập nhật lớp học phần
- `DELETE /api/lophocphan/{maLopHocPhan}` - Xóa lớp học phần
- `GET /api/lophocphan/{maLopHocPhan}/sinhvien` - Lấy danh sách sinh viên trong lớp
- `POST /api/lophocphan/import` - Import danh sách sinh viên từ Excel

## 6. Lưu ý quan trọng

1. **Mã lớp học phần**: Hệ thống tự động tạo mã từ tên lớp (ví dụ: "Công nghệ phần mềm nhúng-1-25 (C701)" → "CNPMN-L01")

2. **Import Excel**: 
   - File phải có định dạng .xls hoặc .xlsx
   - Tên lớp học phần phải nằm ở hàng 6, cột C
   - Dữ liệu sinh viên bắt đầu từ hàng 10

3. **Quan hệ nhiều-nhiều**: Một sinh viên có thể tham gia nhiều lớp học phần khác nhau

4. **Xuất Excel**: Khi xuất theo lớp học phần, file sẽ chứa tất cả sinh viên trong lớp, không chỉ những sinh viên đã điểm danh

5. **Thống kê**: Chỉ hiển thị khi đã chọn lớp học phần cụ thể

## 7. Xử lý lỗi thường gặp

### Lỗi import Excel:
- Kiểm tra định dạng file (phải là .xls hoặc .xlsx)
- Đảm bảo tên lớp học phần ở đúng vị trí (hàng 6, cột C)
- Kiểm tra dữ liệu sinh viên có đầy đủ mã và tên

### Lỗi filter:
- Đảm bảo đã chọn đúng lớp học phần
- Kiểm tra kết nối database

### Lỗi xuất Excel:
- Đảm bảo có dữ liệu để xuất
- Kiểm tra quyền ghi file trên máy tính
