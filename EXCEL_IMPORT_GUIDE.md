# Hướng dẫn Import Excel cho cập nhật RFID sinh viên

## Cách sử dụng

1. Vào trang **Quản lý sinh viên**
2. Click nút **"Import Excel"**
3. Chọn file Excel (.xls hoặc .xlsx)
4. Click **"Import"** để xử lý

## Cấu trúc file Excel

File Excel phải có **3 cột** với tên như sau:

| Cột 1 | Cột 2 | Cột 3 |
|-------|-------|-------|
| Mã sinh viên | Tên sinh viên | RFID |

### Ví dụ:

| Mã sinh viên | Tên sinh viên | RFID |
|--------------|---------------|------|
| CT070201 | Nguyễn Văn A | RFID001 |
| CT070202 | Trần Thị B | RFID002 |
| CT070203 | Lê Văn C | RFID003 |

## Chức năng

- **Nếu sinh viên đã tồn tại**: Cập nhật RFID mới
- **Nếu sinh viên chưa tồn tại**: Tạo sinh viên mới
- **Hiển thị kết quả**: Tổng số thành công/thất bại

## Lưu ý

- Tên cột có thể chứa từ khóa: "mã sinh viên", "tên sinh viên", "rfid"
- Hỗ trợ cả định dạng .xls và .xlsx
- File phải có ít nhất 1 dòng dữ liệu (không tính header)
- Không được để trống các trường bắt buộc
