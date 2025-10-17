# Đặc tả Use Case - Hệ thống Quản lý Lớp học phần RFID

## Mục lục
1. [UC01: Thêm lớp học phần thủ công](#uc01-thêm-lớp-học-phần-thủ-công)
2. [UC02: Import lớp học phần từ Excel](#uc02-import-lớp-học-phần-từ-excel)
3. [UC03: Sửa lớp học phần](#uc03-sửa-lớp-học-phần)
4. [UC04: Import file Excel cập nhật RFID](#uc04-import-file-excel-cập-nhật-rfid)
5. [UC05: Thêm thiết bị](#uc05-thêm-thiết-bị)

---

## UC01: Thêm lớp học phần thủ công

### Thông tin cơ bản
- **Tên Use Case**: Thêm lớp học phần thủ công
- **Mã Use Case**: UC01
- **Tác nhân chính**: Giảng viên/Quản trị viên
- **Mô tả**: Cho phép tạo mới một lớp học phần bằng cách nhập thông tin thủ công
- **Mức độ ưu tiên**: Cao

### Tiền điều kiện
1. Hệ thống đang hoạt động bình thường
2. Người dùng đã đăng nhập với quyền quản lý lớp học phần
3. Trang "Lớp học phần" đang được hiển thị

### Sự kiện chính
**Mô tả**: Người dùng tạo mới lớp học phần bằng cách nhập thông tin trực tiếp

**Luồng sự kiện chính**:
1. Người dùng nhấn nút "Thêm lớp học phần"
2. Hệ thống hiển thị modal "Thêm lớp học phần mới"
3. Người dùng nhập mã lớp học phần
4. Người dùng nhập tên lớp học phần
5. Người dùng nhấn nút "Lưu"
6. Hệ thống kiểm tra tính hợp lệ của dữ liệu
7. Hệ thống kiểm tra mã lớp học phần có trùng lặp không
8. Hệ thống tạo mới lớp học phần trong cơ sở dữ liệu
9. Hệ thống hiển thị thông báo thành công
10. Hệ thống đóng modal và cập nhật danh sách lớp học phần

### Luồng sự kiện phụ

#### A1: Mã lớp học phần đã tồn tại
- **A1.1**: Hệ thống hiển thị thông báo lỗi "Mã lớp học phần đã tồn tại"
- **A1.2**: Người dùng sửa mã lớp học phần và thử lại từ bước 5

#### A2: Dữ liệu không hợp lệ
- **A2.1**: Hệ thống hiển thị thông báo lỗi chi tiết
- **A2.2**: Người dùng sửa dữ liệu và thử lại từ bước 5

#### A3: Lỗi hệ thống
- **A3.1**: Hệ thống hiển thị thông báo lỗi "Có lỗi xảy ra, vui lòng thử lại"
- **A3.2**: Người dùng có thể thử lại hoặc hủy bỏ

### Hậu điều kiện
1. Lớp học phần mới được tạo thành công trong cơ sở dữ liệu
2. Danh sách lớp học phần được cập nhật
3. Hệ thống sẵn sàng cho các thao tác tiếp theo

---

## UC02: Import lớp học phần từ Excel

### Thông tin cơ bản
- **Tên Use Case**: Import lớp học phần từ Excel
- **Mã Use Case**: UC02
- **Tác nhân chính**: Giảng viên/Quản trị viên
- **Mô tả**: Cho phép tạo hàng loạt lớp học phần và sinh viên từ file Excel
- **Mức độ ưu tiên**: Cao

### Tiền điều kiện
1. Hệ thống đang hoạt động bình thường
2. Người dùng đã đăng nhập với quyền quản lý lớp học phần
3. File Excel có định dạng đúng theo template
4. File Excel chứa dữ liệu hợp lệ

### Sự kiện chính
**Mô tả**: Người dùng import dữ liệu lớp học phần từ file Excel

**Luồng sự kiện chính**:
1. Người dùng nhấn nút "Import Excel"
2. Hệ thống hiển thị modal "Import danh sách sinh viên từ Excel"
3. Người dùng chọn file Excel từ máy tính
4. Người dùng nhấn nút "Import"
5. Hệ thống kiểm tra định dạng file (phải là .xls hoặc .xlsx)
6. Hệ thống đọc và phân tích dữ liệu từ file Excel
7. Hệ thống kiểm tra tính hợp lệ của dữ liệu
8. Hệ thống tạo mới/cập nhật lớp học phần
9. Hệ thống thêm sinh viên vào các lớp học phần
10. Hệ thống hiển thị kết quả import chi tiết
11. Người dùng xem kết quả và đóng modal
12. Hệ thống cập nhật danh sách lớp học phần

### Luồng sự kiện phụ

#### A1: File không đúng định dạng
- **A1.1**: Hệ thống hiển thị thông báo "File phải có định dạng Excel (.xls hoặc .xlsx)"
- **A1.2**: Người dùng chọn file khác từ bước 3

#### A2: Dữ liệu không hợp lệ
- **A2.1**: Hệ thống hiển thị danh sách lỗi chi tiết
- **A2.2**: Người dùng sửa file Excel và import lại từ bước 3

#### A3: Lỗi khi đọc file
- **A3.1**: Hệ thống hiển thị thông báo "Lỗi khi xử lý file"
- **A3.2**: Người dùng kiểm tra file và thử lại

### Hậu điều kiện
1. Các lớp học phần mới được tạo trong cơ sở dữ liệu
2. Sinh viên được thêm vào các lớp học phần tương ứng
3. Báo cáo kết quả import được lưu trữ
4. Danh sách lớp học phần được cập nhật

---

## UC03: Sửa lớp học phần

### Thông tin cơ bản
- **Tên Use Case**: Sửa lớp học phần
- **Mã Use Case**: UC03
- **Tác nhân chính**: Giảng viên/Quản trị viên
- **Mô tả**: Cho phép chỉnh sửa thông tin lớp học phần và quản lý danh sách sinh viên
- **Mức độ ưu tiên**: Cao

### Tiền điều kiện
1. Hệ thống đang hoạt động bình thường
2. Người dùng đã đăng nhập với quyền quản lý lớp học phần
3. Lớp học phần cần sửa đã tồn tại trong hệ thống
4. Trang "Lớp học phần" đang được hiển thị

### Sự kiện chính
**Mô tả**: Người dùng chỉnh sửa thông tin lớp học phần và quản lý sinh viên

**Luồng sự kiện chính**:
1. Người dùng nhấn nút "Sửa" tại lớp học phần cần chỉnh sửa
2. Hệ thống hiển thị modal "Sửa lớp học phần" với 3 tabs
3. Hệ thống tải danh sách sinh viên hiện tại trong lớp
4. Người dùng có thể:
   - **Tab 1**: Sửa thông tin cơ bản (tên lớp học phần)
   - **Tab 2**: Thêm sinh viên vào lớp
   - **Tab 3**: Xóa sinh viên khỏi lớp
5. Người dùng thực hiện các thao tác cần thiết
6. Người dùng nhấn nút "Lưu"
7. Hệ thống cập nhật thông tin lớp học phần
8. Hệ thống hiển thị thông báo thành công
9. Hệ thống đóng modal và cập nhật danh sách

### Luồng sự kiện phụ

#### A1: Thêm sinh viên vào lớp
- **A1.1**: Người dùng chuyển sang tab "Thêm sinh viên"
- **A1.2**: Người dùng tìm kiếm sinh viên (tùy chọn)
- **A1.3**: Người dùng chọn sinh viên bằng checkbox
- **A1.4**: Người dùng nhấn "Thêm X sinh viên"
- **A1.5**: Hệ thống thêm sinh viên vào lớp và cập nhật danh sách

#### A2: Xóa sinh viên khỏi lớp
- **A2.1**: Người dùng chuyển sang tab "Xóa sinh viên"
- **A2.2**: Người dùng tìm kiếm sinh viên (tùy chọn)
- **A2.3**: Người dùng chọn sinh viên cần xóa bằng checkbox
- **A2.4**: Người dùng nhấn "Xóa X sinh viên"
- **A2.5**: Hệ thống xóa sinh viên khỏi lớp và cập nhật danh sách

#### A3: Lỗi khi cập nhật
- **A3.1**: Hệ thống hiển thị thông báo lỗi chi tiết
- **A3.2**: Người dùng sửa lỗi và thử lại

### Hậu điều kiện
1. Thông tin lớp học phần được cập nhật
2. Danh sách sinh viên trong lớp được cập nhật
3. Danh sách lớp học phần được refresh
4. Hệ thống sẵn sàng cho các thao tác tiếp theo

---

## UC04: Import file Excel cập nhật RFID

### Thông tin cơ bản
- **Tên Use Case**: Import file Excel cập nhật RFID
- **Mã Use Case**: UC04
- **Tác nhân chính**: Quản trị viên hệ thống
- **Mô tả**: Cho phép cập nhật thông tin RFID cho sinh viên từ file Excel
- **Mức độ ưu tiên**: Trung bình

### Tiền điều kiện
1. Hệ thống đang hoạt động bình thường
2. Người dùng đã đăng nhập với quyền quản trị viên
3. File Excel chứa thông tin RFID hợp lệ
4. Sinh viên cần cập nhật đã tồn tại trong hệ thống

### Sự kiện chính
**Mô tả**: Người dùng cập nhật thông tin RFID cho sinh viên từ file Excel

**Luồng sự kiện chính**:
1. Người dùng truy cập trang quản lý sinh viên
2. Người dùng nhấn nút "Import Excel" (nếu có)
3. Hệ thống hiển thị modal import
4. Người dùng chọn file Excel chứa thông tin RFID
5. Người dùng nhấn nút "Import"
6. Hệ thống kiểm tra định dạng file
7. Hệ thống đọc và phân tích dữ liệu RFID
8. Hệ thống kiểm tra tính hợp lệ của RFID
9. Hệ thống cập nhật thông tin RFID cho sinh viên
10. Hệ thống hiển thị kết quả cập nhật
11. Người dùng xem kết quả và đóng modal

### Luồng sự kiện phụ

#### A1: RFID không hợp lệ
- **A1.1**: Hệ thống hiển thị danh sách RFID lỗi
- **A1.2**: Người dùng sửa file Excel và import lại

#### A2: Sinh viên không tồn tại
- **A2.1**: Hệ thống hiển thị danh sách sinh viên không tìm thấy
- **A2.2**: Người dùng kiểm tra và sửa file Excel

#### A3: RFID đã được sử dụng
- **A3.1**: Hệ thống hiển thị cảnh báo RFID trùng lặp
- **A3.2**: Người dùng xác nhận hoặc hủy bỏ thao tác

### Hậu điều kiện
1. Thông tin RFID của sinh viên được cập nhật
2. Báo cáo kết quả cập nhật được lưu trữ
3. Danh sách sinh viên được cập nhật
4. Hệ thống RFID sẵn sàng nhận diện sinh viên mới

---

## UC05: Thêm thiết bị

### Thông tin cơ bản
- **Tên Use Case**: Thêm thiết bị
- **Mã Use Case**: UC05
- **Tác nhân chính**: Quản trị viên hệ thống
- **Mô tả**: Cho phép đăng ký thiết bị RFID mới vào hệ thống
- **Mức độ ưu tiên**: Trung bình

### Tiền điều kiện
1. Hệ thống đang hoạt động bình thường
2. Người dùng đã đăng nhập với quyền quản trị viên
3. Thiết bị RFID đã được kết nối và cấu hình
4. Trang "Cài đặt" đang được hiển thị

### Sự kiện chính
**Mô tả**: Người dùng đăng ký thiết bị RFID mới cho phòng học

**Luồng sự kiện chính**:
1. Người dùng truy cập trang "Cài đặt"
2. Người dùng chuyển sang tab "Thiết lập thiết bị"
3. Người dùng nhập mã thiết bị
4. Người dùng nhập phòng học
5. Người dùng nhấn nút "Lưu thiết bị"
6. Hệ thống kiểm tra tính hợp lệ của dữ liệu
7. Hệ thống kiểm tra mã thiết bị có trùng lặp không
8. Hệ thống lưu thông tin thiết bị vào cơ sở dữ liệu
9. Hệ thống hiển thị thông báo thành công
10. Hệ thống cập nhật danh sách thiết bị

### Luồng sự kiện phụ

#### A1: Mã thiết bị đã tồn tại
- **A1.1**: Hệ thống hiển thị thông báo "Mã thiết bị đã tồn tại"
- **A1.2**: Người dùng nhập mã thiết bị khác từ bước 3

#### A2: Dữ liệu không đầy đủ
- **A2.1**: Hệ thống hiển thị thông báo "Vui lòng nhập đủ Mã thiết bị và Phòng học"
- **A2.2**: Người dùng bổ sung thông tin và thử lại từ bước 5

#### A3: Lỗi hệ thống
- **A3.1**: Hệ thống hiển thị thông báo "Không thể tạo thiết bị"
- **A3.2**: Người dùng kiểm tra kết nối và thử lại

### Hậu điều kiện
1. Thiết bị mới được đăng ký trong hệ thống
2. Danh sách thiết bị được cập nhật
3. Thiết bị sẵn sàng hoạt động trong phòng học được chỉ định
4. Hệ thống có thể nhận diện thiết bị khi có hoạt động điểm danh

---

## Bảng tóm tắt Use Cases

| UC | Tên Use Case | Tác nhân | Mức độ ưu tiên | Trạng thái |
|----|-------------|----------|----------------|------------|
| UC01 | Thêm lớp học phần thủ công | Giảng viên/QTV | Cao | Hoàn thành |
| UC02 | Import lớp học phần từ Excel | Giảng viên/QTV | Cao | Hoàn thành |
| UC03 | Sửa lớp học phần | Giảng viên/QTV | Cao | Hoàn thành |
| UC04 | Import file Excel cập nhật RFID | QTV hệ thống | Trung bình | Hoàn thành |
| UC05 | Thêm thiết bị | QTV hệ thống | Trung bình | Hoàn thành |

---

## Ghi chú kỹ thuật

### Công nghệ sử dụng
- **Frontend**: ReactJS với Bootstrap
- **Backend**: Spring Boot
- **Database**: MySQL
- **File Processing**: Apache POI

### Định dạng file Excel
- **Import lớp học phần**: Sheet = Tên lớp, Cột A = Mã sinh viên, Cột B = Tên sinh viên
- **Import RFID**: Cột A = Mã sinh viên, Cột B = RFID

### Validation rules
- Mã lớp học phần: Không được trùng lặp, bắt buộc
- Mã sinh viên: Bắt buộc, định dạng chuẩn
- RFID: Duy nhất, định dạng hex
- Mã thiết bị: Duy nhất, bắt buộc
