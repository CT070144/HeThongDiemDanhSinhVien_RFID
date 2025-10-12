# Hệ thống cập nhật trạng thái điểm danh tự động

## Tổng quan

Đã cập nhật hệ thống điểm danh RFID với các tính năng mới:
- 4 trạng thái điểm danh: Đang học, Đã ra về, Ra về sớm, Không điểm danh ra
- Tự động hóa cập nhật trạng thái bằng scheduled tasks
- Cải thiện chức năng export với ghi chú chi tiết

## Các thay đổi Backend

### 1. Cập nhật Entity PhieuDiemDanh
**File:** `BackEnd/src/main/java/com/rfid/attendance/entity/PhieuDiemDanh.java`

- Thêm 2 trạng thái mới vào enum `TrangThaiHoc`:
  - `RA_VE_SOM("Ra về sớm")`
  - `KHONG_DIEM_DANH_RA("Không điểm danh ra")`

### 2. Cập nhật AttendanceService
**File:** `BackEnd/src/main/java/com/rfid/attendance/service/AttendanceService.java`

- Thêm method `determineCheckoutStatus()` để xác định trạng thái khi sinh viên điểm danh ra
- Logic xác định "Ra về sớm": nếu điểm danh ra trước 20 phút kết thúc ca học
- Cập nhật logic xử lý điểm danh ra với trạng thái phù hợp

### 3. Tạo ScheduledAttendanceService
**File:** `BackEnd/src/main/java/com/rfid/attendance/service/ScheduledAttendanceService.java`

- **Scheduled task 1:** Chạy mỗi 10 phút để cập nhật trạng thái sinh viên chưa điểm danh ra
- **Scheduled task 2:** Chạy ngay sau khi ca học kết thúc (9:30, 12:30, 15:30, 17:30, 20:30)
- Tự động set trạng thái "Không điểm danh ra" cho sinh viên còn "Đang học" sau khi ca kết thúc

### 4. Cập nhật Repository
**File:** `BackEnd/src/main/java/com/rfid/attendance/repository/PhieuDiemDanhRepository.java`

- Thêm 2 query methods mới:
  - `findByNgayAndTrangThaiAndGioRaIsNull()`
  - `findByNgayAndCaAndTrangThaiAndGioRaIsNull()`

### 5. Bật Scheduling
**File:** `BackEnd/src/main/java/com/rfid/attendance/RfidAttendanceSystemApplication.java`

- Thêm `@EnableScheduling` annotation

## Các thay đổi Frontend

### 1. Cập nhật AttendanceHistory Component
**File:** `FrontEnd/src/pages/AttendanceHistory.js`

#### Thống kê mới:
- Thêm `raVeSom` và `khongDiemDanhRa` vào attendanceStats
- Cập nhật `calculateAttendanceStats()` để tính toán các trạng thái mới

#### Hiển thị trạng thái:
- Thêm function `getAttendanceStatusBadge()` để hiển thị badge cho 4 trạng thái:
  - Đang học (Primary - xanh dương)
  - Đã ra về (Success - xanh lá)
  - Ra về sớm (Warning - vàng)
  - Không điểm danh ra (Danger - đỏ)

#### Export Excel cải tiến:
- Thêm cột "Ghi chú" trong export theo lớp học phần
- Tự động ghi chú "Ra về sớm" hoặc "Không điểm danh ra" cho sinh viên tương ứng
- Thêm thống kê mới vào báo cáo Excel
- Cập nhật HTML table với colspan phù hợp (5 cột thay vì 4)

## Logic hoạt động

### Quy trình điểm danh:
1. **Điểm danh vào:** Trạng thái = "Đang học"
2. **Điểm danh ra:**
   - Trước 20 phút kết thúc ca → "Ra về sớm"
   - Sau 20 phút kết thúc ca → "Đã ra về"

### Tự động cập nhật:
1. **Mỗi 10 phút:** Kiểm tra sinh viên "Đang học" và cập nhật "Không điểm danh ra" nếu đã quá giờ
2. **Sau mỗi ca học:** Tự động cập nhật tất cả sinh viên còn "Đang học" thành "Không điểm danh ra"

### Thời gian ca học:
- **Ca 1:** 7:00-9:25 (Ra về sớm trước 9:05)
- **Ca 2:** 9:35-12:00 (Ra về sớm trước 11:40)
- **Ca 3:** 12:30-14:55 (Ra về sớm trước 14:35)
- **Ca 4:** 15:05-17:30 (Ra về sớm trước 17:10)
- **Ca 5:** 18:00-20:30 (Ra về sớm trước 20:10)

## Lợi ích

1. **Tự động hóa hoàn toàn:** Không cần can thiệp thủ công để cập nhật trạng thái
2. **Báo cáo chi tiết:** Export Excel có đầy đủ thông tin và ghi chú
3. **Theo dõi chính xác:** Phân biệt rõ ràng các trường hợp ra về sớm và không điểm danh ra
4. **Thời gian thực:** Cập nhật trạng thái liên tục mỗi 10 phút
5. **Thống kê đầy đủ:** Báo cáo tổng hợp tất cả các trạng thái

## Cách sử dụng

1. **Xem trạng thái:** Vào trang "Lịch sử điểm danh" để xem các badge màu sắc
2. **Export báo cáo:** Lọc theo lớp học phần + ngày + ca, sau đó xuất Excel
3. **Theo dõi tự động:** Hệ thống sẽ tự động cập nhật trạng thái mà không cần can thiệp

## Ghi chú kỹ thuật

- Scheduled tasks chạy từ thứ 2 đến thứ 6 (MON-FRI)
- Buffer time 5 phút sau khi ca kết thúc trước khi cập nhật trạng thái
- Tất cả thay đổi được log ra console để theo dõi
- Frontend hiển thị real-time với polling mỗi 1 giây
