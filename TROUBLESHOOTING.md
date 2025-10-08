# Hướng dẫn khắc phục lỗi Database

## Lỗi: BeanCreationException với view v_lich_su_diem_danh

### Nguyên nhân:
Lỗi này xảy ra khi view `v_lich_su_diem_danh` tham chiếu đến các bảng hoặc cột không tồn tại hoặc không có quyền truy cập.

### Cách khắc phục:

#### Phương pháp 1: Khắc phục nhanh (Khuyến nghị)
```sql
-- Chạy script này trong MySQL Workbench hoặc command line
source ScriptDatabase/quick_fix.sql
```

#### Phương pháp 2: Khắc phục đầy đủ
```sql
-- Chạy script này để khắc phục toàn bộ vấn đề
source ScriptDatabase/fix_database_issues.sql
```

#### Phương pháp 3: Khắc phục thủ công
1. **Mở MySQL Workbench hoặc command line MySQL**
2. **Kết nối đến database `rfid_attendance_system`**
3. **Chạy các lệnh sau:**

```sql
USE rfid_attendance_system;

-- Xóa view cũ
DROP VIEW IF EXISTS v_lich_su_diem_danh;

-- Tạo lại view
CREATE VIEW v_lich_su_diem_danh AS
SELECT 
    p.id,
    p.rfid,
    p.masinhvien,
    p.tensinhvien,
    p.phonghoc,
    p.giovao,
    p.giora,
    p.ngay,
    p.ca,
    CASE p.ca
        WHEN 1 THEN 'Ca 1 (07:00-09:30)'
        WHEN 2 THEN 'Ca 2 (09:30-12:00)'
        WHEN 3 THEN 'Ca 3 (12:30-15:00)'
        WHEN 4 THEN 'Ca 4 (15:00-17:30)'
        ELSE CONCAT('Ca ', p.ca)
    END as ten_ca,
    p.trangthai,
    CASE 
        WHEN p.giovao IS NOT NULL AND p.giora IS NOT NULL THEN 'Hoàn thành'
        WHEN p.giovao IS NOT NULL AND p.giora IS NULL THEN 'Đang học'
        ELSE 'Chưa điểm danh'
    END as tinh_trang,
    p.created_at,
    p.updated_at
FROM phieudiemdanh p
ORDER BY p.ngay DESC, p.ca ASC, p.created_at DESC;
```

4. **Sau đó chạy script tạo bảng mới:**
```sql
source ScriptDatabase/update_database.sql
```

### Kiểm tra sau khi khắc phục:

1. **Kiểm tra view đã được tạo:**
```sql
SHOW CREATE VIEW v_lich_su_diem_danh;
```

2. **Kiểm tra các bảng mới:**
```sql
SHOW TABLES;
-- Phải thấy: lophocphan, sinhvienlophocphan
```

3. **Kiểm tra cấu trúc bảng:**
```sql
DESCRIBE lophocphan;
DESCRIBE sinhvienlophocphan;
```

4. **Restart Spring Boot application**

### Lỗi thường gặp khác:

#### Lỗi: Table 'rfid_attendance_system.lophocphan' doesn't exist
**Giải pháp:** Chạy script `update_database.sql`

#### Lỗi: Foreign key constraint fails
**Giải pháp:** Đảm bảo bảng `sinhvien` đã tồn tại trước khi tạo bảng `sinhvienlophocphan`

#### Lỗi: Column 'malophocphan' cannot be null
**Giải pháp:** Kiểm tra dữ liệu import, đảm bảo có đầy đủ thông tin

### Kiểm tra quyền database:

Nếu vẫn gặp lỗi quyền, hãy kiểm tra:

```sql
-- Kiểm tra user hiện tại
SELECT USER(), CURRENT_USER();

-- Kiểm tra quyền
SHOW GRANTS FOR CURRENT_USER();

-- Nếu cần, cấp quyền đầy đủ
GRANT ALL PRIVILEGES ON rfid_attendance_system.* TO 'your_username'@'localhost';
FLUSH PRIVILEGES;
```

### Logs để debug:

1. **Kiểm tra Spring Boot logs** để xem lỗi chi tiết
2. **Kiểm tra MySQL error log** nếu có
3. **Kiểm tra connection string** trong `application.properties`

### Liên hệ hỗ trợ:

Nếu vẫn gặp vấn đề, hãy cung cấp:
- Log lỗi đầy đủ từ Spring Boot
- Kết quả của `SHOW TABLES;`
- Kết quả của `SHOW CREATE VIEW v_lich_su_diem_danh;`
