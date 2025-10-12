# Cải tiến xử lý DocRfid trong Bulk Update RFID

## Tổng quan

Đã cải tiến chức năng import cập nhật RFID để tự động xử lý và đồng bộ hóa với bảng `docrfid`, đảm bảo các bản ghi RFID được đánh dấu là đã xử lý sau khi import thành công.

## Các thay đổi Backend

### 1. Cập nhật SinhVienService.bulkUpdateRfid()

**File:** `BackEnd/src/main/java/com/rfid/attendance/service/SinhVienService.java`

#### Xử lý cập nhật sinh viên đã tồn tại:
```java
// Sync to docrfid - cập nhật RFID cũ nếu có
docRfidRepository.findByRfid(oldRfid).ifPresent(doc -> {
    doc.setRfid(saved.getRfid());
    doc.setMaSinhVien(saved.getMaSinhVien());
    doc.setTenSinhVien(saved.getTenSinhVien());
    doc.setProcessed(true);  // Đánh dấu đã xử lý
    docRfidRepository.save(doc);
});

// Kiểm tra và cập nhật docrfid với RFID mới nếu có
docRfidRepository.findByRfid(saved.getRfid()).ifPresent(doc -> {
    doc.setMaSinhVien(saved.getMaSinhVien());
    doc.setTenSinhVien(saved.getTenSinhVien());
    doc.setProcessed(true);  // Đánh dấu đã xử lý
    docRfidRepository.save(doc);
});
```

#### Xử lý tạo sinh viên mới:
```java
// Sync to docrfid - kiểm tra và cập nhật trạng thái đã xử lý
docRfidRepository.findByRfid(newSinhVien.getRfid()).ifPresent(doc -> {
    doc.setMaSinhVien(newSinhVien.getMaSinhVien());
    doc.setTenSinhVien(newSinhVien.getTenSinhVien());
    doc.setProcessed(true);  // Đánh dấu đã xử lý
    docRfidRepository.save(doc);
});
```

### 2. Cập nhật SinhVienController.bulkUpdateRfid()

**File:** `BackEnd/src/main/java/com/rfid/attendance/controller/SinhVienController.java`

#### Thêm logging chi tiết:
```java
@PostMapping("/bulk-update-rfid")
public ResponseEntity<?> bulkUpdateRfid(@RequestBody List<SinhVien> sinhVienList) {
    try {
        System.out.println("=== BULK UPDATE RFID ===");
        System.out.println("Số lượng sinh viên cần xử lý: " + sinhVienList.size());
        
        var result = sinhVienService.bulkUpdateRfid(sinhVienList);
        
        System.out.println("Kết quả xử lý:");
        System.out.println("- Tổng số: " + result.get("totalProcessed"));
        System.out.println("- Thành công: " + result.get("successCount"));
        System.out.println("- Thất bại: " + result.get("failureCount"));
        
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        System.out.println("Lỗi khi cập nhật hàng loạt: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi cập nhật hàng loạt: " + e.getMessage());
    }
}
```

## Logic xử lý DocRfid

### 1. Trường hợp cập nhật sinh viên đã tồn tại:

1. **Kiểm tra RFID cũ:** Tìm bản ghi docrfid với RFID cũ và cập nhật:
   - Chuyển RFID cũ thành RFID mới
   - Cập nhật thông tin sinh viên
   - Đánh dấu `processed = true`

2. **Kiểm tra RFID mới:** Tìm bản ghi docrfid với RFID mới và cập nhật:
   - Cập nhật thông tin sinh viên
   - Đánh dấu `processed = true`

### 2. Trường hợp tạo sinh viên mới:

1. **Kiểm tra RFID:** Tìm bản ghi docrfid với RFID của sinh viên mới
2. **Cập nhật thông tin:** Nếu tìm thấy, cập nhật:
   - Mã sinh viên
   - Tên sinh viên
   - Đánh dấu `processed = true`

## Lợi ích

### 1. **Tự động đồng bộ hóa:**
- Không cần xử lý thủ công các bản ghi docrfid
- Tự động đánh dấu đã xử lý sau khi import thành công

### 2. **Xử lý đầy đủ các trường hợp:**
- RFID cũ → RFID mới
- RFID mới được tạo
- Sinh viên mới với RFID đã có trong docrfid

### 3. **Logging chi tiết:**
- Theo dõi quá trình xử lý
- Debug dễ dàng khi có lỗi
- Báo cáo kết quả rõ ràng

### 4. **Đảm bảo tính nhất quán:**
- Tất cả bản ghi docrfid liên quan đều được cập nhật
- Thông tin sinh viên được đồng bộ chính xác

## Quy trình hoạt động

1. **Import Excel:** Người dùng upload file Excel với thông tin sinh viên và RFID
2. **Parse dữ liệu:** Hệ thống đọc và validate dữ liệu từ Excel
3. **Xử lý từng sinh viên:**
   - Kiểm tra sinh viên đã tồn tại hay chưa
   - Cập nhật hoặc tạo mới sinh viên
   - Đồng bộ với bảng docrfid
   - Đánh dấu docrfid đã xử lý
4. **Báo cáo kết quả:** Trả về thống kê chi tiết về số lượng thành công/thất bại

## Logging và Debug

### Console logs sẽ hiển thị:
```
=== BULK UPDATE RFID ===
Số lượng sinh viên cần xử lý: 5
Đã cập nhật docrfid RFID cũ: RFID001 -> RFID002 cho sinh viên: Nguyễn Văn A
Đã cập nhật docrfid RFID mới: RFID002 cho sinh viên: Nguyễn Văn A
Đã cập nhật docrfid cho sinh viên mới: Trần Thị B - RFID: RFID003
Kết quả xử lý:
- Tổng số: 5
- Thành công: 4
- Thất bại: 1
```

## Tương thích

- Tương thích với tất cả các chức năng hiện có
- Không ảnh hưởng đến logic điểm danh
- Hoạt động với cả sinh viên mới và sinh viên đã tồn tại
- Xử lý được cả trường hợp thay đổi RFID và không thay đổi RFID
