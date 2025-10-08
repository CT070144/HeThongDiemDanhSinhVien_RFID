# Khắc phục lỗi Lazy Loading - HttpMessageNotWritableException

## Lỗi gặp phải:
```
HttpMessageNotWritableException: Could not write JSON: Unable to find com.rfid.attendance.entity.SinhVien with id CT070201
```

## Nguyên nhân:
Lỗi này xảy ra do **Lazy Loading** trong JPA. Khi Spring Boot cố gắng serialize entity để trả về JSON, nó cần access vào các thuộc tính được đánh dấu `@ManyToOne(fetch = FetchType.LAZY)`, nhưng session đã đóng.

## Giải pháp đã áp dụng:

### 1. Thêm @JsonIgnore cho các thuộc tính lazy
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "masinhvien", insertable = false, updatable = false)
@JsonIgnore
private SinhVien sinhVien;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "malophocphan", insertable = false, updatable = false)
@JsonIgnore
private LopHocPhan lopHocPhan;
```

### 2. Tạo DTO để tránh vấn đề lazy loading
```java
public class LopHocPhanDTO {
    private String maLopHocPhan;
    private String tenLopHocPhan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long soSinhVien;
    // ... getters/setters
}
```

### 3. Sử dụng @Transactional(readOnly = true) cho query methods
```java
@Transactional(readOnly = true)
public List<LopHocPhanDTO> getAllLopHocPhan() {
    // ...
}
```

### 4. Cập nhật Service và Controller để sử dụng DTO
- Service trả về DTO thay vì Entity
- Controller nhận và trả về DTO

## Các bước khắc phục:

### Bước 1: Kiểm tra dữ liệu trong database
```sql
source ScriptDatabase/check_data_integrity.sql
```

### Bước 2: Khắc phục view nếu cần
```sql
source ScriptDatabase/quick_fix.sql
```

### Bước 3: Tạo bảng mới nếu chưa có
```sql
source ScriptDatabase/update_database.sql
```

### Bước 4: Restart Spring Boot application

## Kiểm tra sau khi khắc phục:

### 1. Kiểm tra API endpoints:
```bash
# Test API lấy danh sách lớp học phần
curl -X GET http://localhost:8080/api/lophocphan

# Test API tìm kiếm
curl -X GET "http://localhost:8080/api/lophocphan/search?keyword=CNPMN"

# Test API lấy sinh viên theo lớp
curl -X GET http://localhost:8080/api/lophocphan/CNPMN-L01/sinhvien
```

### 2. Kiểm tra frontend:
- Truy cập trang "Lớp học phần"
- Kiểm tra danh sách hiển thị đúng
- Test chức năng tìm kiếm
- Test chức năng xem sinh viên

### 3. Kiểm tra logs:
- Không còn lỗi `HttpMessageNotWritableException`
- Không còn lỗi lazy loading

## Các lỗi liên quan khác:

### Lỗi: Entity không tồn tại
**Giải pháp:** Kiểm tra dữ liệu trong database và tạo dữ liệu mẫu nếu cần

### Lỗi: Session đã đóng
**Giải pháp:** Sử dụng DTO hoặc @JsonIgnore

### Lỗi: Circular reference
**Giải pháp:** Sử dụng @JsonIgnore hoặc @JsonManagedReference/@JsonBackReference

## Best Practices:

1. **Luôn sử dụng DTO cho API responses**
2. **Tránh expose entity trực tiếp qua REST API**
3. **Sử dụng @JsonIgnore cho các thuộc tính lazy**
4. **Sử dụng @Transactional(readOnly = true) cho query methods**
5. **Kiểm tra tính toàn vẹn dữ liệu định kỳ**

## Monitoring:

### Log patterns để theo dõi:
```
- HttpMessageNotWritableException
- LazyInitializationException
- Could not write JSON
- Unable to find entity
```

### Metrics để theo dõi:
- Response time của API
- Error rate
- Database connection pool

## Troubleshooting:

### Nếu vẫn gặp lỗi:
1. Kiểm tra logs chi tiết
2. Kiểm tra database integrity
3. Kiểm tra entity mappings
4. Kiểm tra transaction boundaries

### Debug steps:
1. Enable SQL logging trong application.properties
2. Kiểm tra Hibernate session management
3. Kiểm tra entity lifecycle
4. Kiểm tra JSON serialization configuration
