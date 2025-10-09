# Cập nhật xuất Excel với tô màu hàng theo trạng thái

## 📋 **Tổng quan thay đổi**

Đã cập nhật chức năng xuất Excel cho trang AttendanceHistory để tô màu hàng theo trạng thái điểm danh khi xuất theo lớp học phần.

## 🎨 **Quy tắc tô màu**

| Trạng thái | Màu nền | Mô tả |
|---|---|---|
| **Vắng mặt** | 🟥 Đỏ nhạt (`#FFE6E6`) | Sinh viên không có trong danh sách điểm danh |
| **Điểm danh muộn** | 🟨 Vàng nhạt (`#FFFFE0`) | Sinh viên điểm danh muộn |
| **Điểm danh đúng giờ** | ⚪ Không màu | Sinh viên điểm danh đúng giờ |

## 🔧 **Thay đổi kỹ thuật**

### **1. Cập nhật logic xử lý dữ liệu**
```javascript
// Thêm style cho mỗi hàng dữ liệu
data = studentsInLop.map((student, index) => {
  const attendanceRecord = allFilteredAttendance.find(r => r.maSinhVien === student.maSinhVien);
  let diemDanh = 'v'; // vắng mặc định
  let rowStyle = null; // Không có màu nền mặc định
  
  if (attendanceRecord) {
    if (attendanceRecord.tinhTrangDiemDanh === 'muon' || attendanceRecord.tinhTrangDiemDanh === 'MUON') {
      diemDanh = 'M'; // muộn
      rowStyle = { fill: { fgColor: { rgb: 'FFFFE0' } } }; // Vàng nhạt
    } else {
      diemDanh = 'x'; // có mặt
    }
  } else {
    // Vắng mặt - tô màu đỏ nhạt
    diemDanh = 'v'; // vắng
    rowStyle = { fill: { fgColor: { rgb: 'FFE6E6' } } }; // Đỏ nhạt
  }
  
  return {
    data: [index + 1, student.maSinhVien, student.tenSinhVien, diemDanh],
    style: rowStyle
  };
});
```

### **2. Tạo HTML table với styling**
```javascript
// Tạo HTML table với background color
data.forEach(item => {
  const backgroundColor = item.style?.fill?.fgColor?.rgb === 'FFFFE0' ? '#FFFFE0' : 
                         item.style?.fill?.fgColor?.rgb === 'FFE6E6' ? '#FFE6E6' : '';
  
  htmlTable += '<tr>';
  item.data.forEach(cell => {
    const style = backgroundColor ? 
      `style="background-color: ${backgroundColor}; padding: 5px;"` : 
      'style="padding: 5px;"';
    htmlTable += `<td ${style}>${cell}</td>`;
  });
  htmlTable += '</tr>';
});
```

### **3. Chuyển đổi HTML sang Excel**
```javascript
// Convert HTML table to worksheet
const tempDiv = document.createElement('div');
tempDiv.innerHTML = htmlTable;
ws = XLSX.utils.table_to_sheet(tempDiv.querySelector('table'));
```

## 📊 **Kết quả**

### **File Excel xuất ra sẽ có:**
- **Header**: Có màu xám nhạt với font đậm
- **Hàng vắng mặt**: Màu đỏ nhạt (`#FFE6E6`)
- **Hàng muộn**: Màu vàng nhạt (`#FFFFE0`)
- **Hàng đúng giờ**: Không có màu nền (trắng)
- **Thống kê**: Ở cuối file với thông tin đầy đủ

### **Cột trong file Excel:**
1. **STT**: Số thứ tự
2. **Mã sinh viên**: Mã sinh viên
3. **Họ và tên**: Tên đầy đủ
4. **Điểm danh**: 
   - `x`: Có mặt
   - `M`: Muộn
   - `v`: Vắng

## 🚀 **Cách sử dụng**

1. **Chọn lớp học phần** từ dropdown
2. **Chọn ngày** cụ thể
3. **Chọn ca học** (1-5)
4. **Nhấn "Xuất Excel"**
5. **File Excel** sẽ được tải về với:
   - Hàng vắng mặt có màu đỏ nhạt
   - Hàng muộn có màu vàng nhạt
   - Hàng đúng giờ không có màu nền

## ✅ **Kiểm tra**

- [ ] Xuất Excel theo lớp học phần hoạt động
- [ ] Hàng vắng mặt có màu đỏ nhạt
- [ ] Hàng muộn có màu vàng nhạt
- [ ] Hàng đúng giờ không có màu nền
- [ ] Header có màu xám nhạt và font đậm
- [ ] Thống kê hiển thị ở cuối file
- [ ] File Excel mở được và hiển thị đúng

## 📝 **Lưu ý**

- Chức năng tô màu **chỉ áp dụng** khi xuất Excel theo lớp học phần
- Khi xuất Excel tổng quát (không chọn lớp), không có tô màu
- Màu sắc được áp dụng cho toàn bộ hàng (cột A đến D)
- File Excel được tạo bằng cách chuyển đổi từ HTML table để hỗ trợ styling
