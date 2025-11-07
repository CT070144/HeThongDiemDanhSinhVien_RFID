# Đặc tả thiết kế View - RFID Desktop Application

Tài liệu này mô tả chi tiết về cấu trúc, layout và components của từng view trong ứng dụng Desktop để hỗ trợ thiết kế lại bằng công cụ kéo thả.

---

## 1. MainFrame (JFrame)

**Kích thước:** 1300 x 820 pixels  
**Vị trí:** Center screen  
**Layout:** CardLayout (2 cấp)

### Cấu trúc:
```
MainFrame (JFrame)
├── rootPanel (JPanel - CardLayout)
│   ├── "login" → LoginPanel
│   └── "app" → appContainer (JPanel - BorderLayout)
│       ├── WEST → NavigationPanel (220px width)
│       └── CENTER → contentPanel (JPanel - CardLayout)
│           ├── "dashboard" → DashboardPanel
│           ├── "students" → StudentManagementPanel
│           ├── "attendance" → AttendanceHistoryPanel
│           ├── "classes" → ClassManagementPanel
│           └── "devices" → DeviceManagementPanel
```

### Components:
- **rootPanel**: CardLayout chuyển đổi giữa login và app
- **appContainer**: BorderLayout với padding 12px
- **contentPanel**: CardLayout chứa các màn hình chính

---

## 2. LoginPanel (JPanel)

**Kích thước:** Full frame  
**Background:** Color(240, 243, 246)  
**Layout:** BorderLayout với padding 40px top/bottom

### Cấu trúc:
```
LoginPanel
└── centerWrapper (JPanel - GridBagLayout, background: Color(240, 243, 246))
    └── formPanel (JPanel - GridBagLayout, 420x320px, background: WHITE)
        ├── header (JLabel) - "Hệ thống điểm danh RFID" - Font: Bold 18pt
        ├── subHeader (JLabel) - "Đăng nhập" - Font: Plain 14pt
        ├── usernameField (JTextField) - 20 columns
        ├── passwordField (JPasswordField) - 20 columns
        ├── statusLabel (JLabel) - Hiển thị trạng thái/error
        └── loginButton (JButton) - "Đăng nhập"
```

### Components chi tiết:
- **formPanel**: Border compound (line border + empty border 30px)
- **GridBagConstraints**: insets 8px, fill HORIZONTAL
- **statusLabel**: Màu đỏ khi lỗi, xanh lá khi thành công

---

## 3. NavigationPanel (JPanel)

**Kích thước:** Width 220px, height full  
**Background:** Color.WHITE  
**Layout:** BoxLayout (Y_AXIS)  
**Padding:** 16px all sides

### Cấu trúc:
```
NavigationPanel
├── nameLabel (JLabel) - "Xin chào, [FullName]"
├── roleLabel (JLabel) - Role description, color: (100, 100, 100)
├── VerticalStrut (20px)
├── Dashboard Button (JButton) - "Dashboard"
├── VerticalStrut (8px)
├── Students Button (JButton) - "Sinh viên"
├── VerticalStrut (8px)
├── Attendance Button (JButton) - "Lịch sử điểm danh"
├── VerticalStrut (8px)
├── Classes Button (JButton) - "Lớp học phần"
├── VerticalStrut (8px)
├── Devices Button (JButton) - "Thiết bị"
├── VerticalGlue
└── Logout Button (JButton) - "Đăng xuất" - Background: (240, 77, 77), Foreground: WHITE
```

### Components chi tiết:
- **Buttons**: 
  - Maximum size: (MAX_VALUE, 40px)
  - Background: (242, 245, 249) - inactive
  - Background: (214, 228, 255) - active
  - Border: EmptyBorder(8, 12, 8, 12)
  - FocusPainted: false
- **Logout Button**: Background (240, 77, 77), Foreground WHITE

---

## 4. DashboardPanel (JPanel)

**Background:** Color(250, 250, 252)  
**Padding:** 10px all sides  
**Layout:** GroupLayout (Vertical)

### Cấu trúc:
```
DashboardPanel
├── headerPanel (JPanel - GroupLayout, opaque: false)
│   ├── lblTitle (JLabel) - "Dashboard - Hệ thống điểm danh RFID"
│   │   Font: Segoe UI Bold 20pt, Color: (33, 37, 41)
│   └── btnRefresh (JButton) - "Làm mới" - 110px width, 32px height
│
├── statsPanel (JPanel - GroupLayout, opaque: false)
│   ├── cardTotalStudents (JPanel - Stat Card)
│   │   ├── lblTotalStudentsTitle (JLabel) - "Tổng số sinh viên"
│   │   └── lblTotalStudentsValue (JLabel) - Font: Bold 28pt, Color: (20, 112, 204)
│   ├── cardTodayAttendance (JPanel - Stat Card)
│   │   ├── lblTodayAttendanceTitle (JLabel) - "Điểm danh hôm nay"
│   │   └── lblTodayAttendanceValue (JLabel) - Font: Bold 28pt, Color: (27, 132, 50)
│   ├── cardUnprocessed (JPanel - Stat Card)
│   │   ├── lblUnprocessedTitle (JLabel) - "RFID chưa xử lý"
│   │   └── lblUnprocessedValue (JLabel) - Font: Bold 28pt, Color: (204, 142, 0)
│   └── cardCurrentShift (JPanel - Stat Card)
│       ├── lblCurrentShiftTitle (JLabel) - "Ca hiện tại"
│       └── lblCurrentShiftValue (JLabel) - Font: Bold 28pt, Color: (0, 102, 204)
│
├── chartsContainer (JPanel - GroupLayout, opaque: false)
│   ├── pnlChartByShift (JPanel - BorderLayout, 280px height)
│   │   └── Border: TitledBorder "Thống kê điểm danh theo ca học"
│   ├── pnlChartStatus (JPanel - BorderLayout, 280px height)
│   │   └── Border: TitledBorder "Phân bố trạng thái điểm danh"
│   └── pnlChartHourly (JPanel - BorderLayout, 320px height)
│       └── Border: TitledBorder "Xu hướng điểm danh theo giờ"
│
└── tableContainer (JPanel - GroupLayout)
    ├── Border: TitledBorder "Điểm danh hôm nay"
    ├── scrollToday (JScrollPane)
    │   └── tblTodayAttendance (JTable)
    │       Columns: RFID, Mã SV, Tên sinh viên, Ca, Giờ vào, Giờ ra, Trạng thái
    │       Row height: 28px
    │       Auto sort: true
    │       Center renderer: columns 3, 4, 5
    └── paginationPanel (PaginationPanel)
        Page size: 10 rows
```

### Stat Card Style:
- Background: WHITE
- Border: Compound (Line border Color(230, 234, 238) + Empty border 12px)
- Title: Font Segoe UI Plain 14pt, Color (73, 82, 91)
- Value: Font Segoe UI Bold 28pt, Center aligned

### Spacing:
- Gap giữa các section: 12px (UNRELATED)
- Stats panel: Equal width distribution
- Charts: 2 charts trên 1 hàng, 1 chart dưới full width

---

## 5. AttendanceHistoryPanel (JPanel)

**Background:** Color(250, 250, 252)  
**Padding:** 10px all sides  
**Layout:** GroupLayout (Vertical)

### Cấu trúc:
```
AttendanceHistoryPanel
├── headerPanel (JPanel - GroupLayout, opaque: false)
│   ├── lblTitle (JLabel) - "Lịch sử điểm danh"
│   │   Font: Segoe UI Bold 20pt, Color: (33, 37, 41)
│   ├── btnReload (JButton) - "Làm mới" - 100px width, 32px height
│   └── btnExport (JButton) - "Xuất Excel" - 120px width, 32px height
│
├── filtersPanel (JPanel - GroupLayout, opaque: false)
│   ├── filterRow1 (JPanel - GroupLayout, opaque: false)
│   │   ├── lblNgay (JLabel) - "Ngày (yyyy-MM-dd):"
│   │   ├── txtNgay (JTextField) - 150px width, 32px height
│   │   ├── lblCa (JLabel) - "Ca học:"
│   │   ├── cboCa (JComboBox) - ["", "1", "2", "3", "4", "5"] - 140px width, 32px height
│   │   ├── lblClass (JLabel) - "Lớp học phần:"
│   │   └── cboLopHocPhan (JComboBox<LopHocPhan>) - 240px width, 32px height
│   │
│   ├── filterRow2 (JPanel - GroupLayout, opaque: false)
│   │   ├── lblMaSinhVien (JLabel) - "Mã sinh viên:"
│   │   ├── txtMaSinhVien (JTextField) - 180px width, 32px height
│   │   ├── lblPhongHoc (JLabel) - "Phòng học:"
│   │   ├── txtPhongHoc (JTextField) - 180px width, 32px height
│   │   └── btnClear (JButton) - "Xóa bộ lọc" - 120px width, 32px height
│   │
│   └── lblWarning (JLabel) - 24px height, Color: (204, 102, 0)
│
├── statsPanel (JPanel - GroupLayout, opaque: false)
│   ├── Stat Card: "Tổng bản ghi" (lblGeneralTotal)
│   ├── Stat Card: "Đúng giờ" (lblGeneralOnTime)
│   ├── Stat Card: "Muộn" (lblGeneralLate)
│   ├── Stat Card: "Đang học" (lblGeneralDangHoc)
│   ├── Stat Card: "Đã ra về" (lblGeneralDaRaVe)
│   └── Stat Card: "Không điểm danh ra" (lblGeneralKhongRa)
│
├── panelClassStats (JPanel - BorderLayout, initially hidden)
│   └── Border: TitledBorder "Thống kê lớp học phần"
│   └── Hiển thị khi có lớp được chọn + ngày + ca
│
└── tableContainer (JPanel - GroupLayout)
    ├── Border: TitledBorder "Bảng lịch sử điểm danh"
    ├── scrollPane (JScrollPane)
    │   └── table (JTable)
    │       Columns: RFID, Mã SV, Tên sinh viên, Phòng học, Ngày, Ca, Giờ vào, Giờ ra, Tình trạng, Trạng thái
    │       Row height: 26px
    │       Auto sort: true
    │       Center renderer: columns 4, 5, 6, 7
    └── paginationPanel (PaginationPanel)
        Page size: 12 rows
```

### Stat Card Style:
- Tương tự DashboardPanel
- Value: Font Segoe UI Bold 18pt, Color (52, 71, 103)

### Spacing:
- Gap giữa các section: 12px (UNRELATED)
- Filter rows: Gap 12px giữa các field (RELATED), 12px giữa các row (UNRELATED)

---

## 6. StudentManagementPanel (JPanel)

**Background:** Color(250, 250, 252)  
**Padding:** 10px all sides  
**Layout:** GroupLayout (Vertical)

### Cấu trúc:
```
StudentManagementPanel
├── headerPanel (JPanel - GroupLayout, opaque: false)
│   └── lblTitle (JLabel) - "Quản lý sinh viên"
│       Font: Segoe UI Bold 20pt, Color: (33, 37, 41)
│
├── controlsPanel (JPanel - GroupLayout, opaque: false)
│   ├── filterPanel (JPanel - GroupLayout, opaque: false)
│   │   ├── lblSearch (JLabel) - "Tìm kiếm:"
│   │   ├── searchField (JTextField) - 240px width, 32px height, 25 columns
│   │   └── refreshButton (JButton) - "Làm mới" - 110px width, 32px height
│   │
│   └── actionPanel (JPanel - GroupLayout, opaque: false)
│       ├── addButton (JButton) - "Thêm mới" - 110px width, 32px height
│       ├── editButton (JButton) - "Sửa" - 110px width, 32px height
│       ├── deleteButton (JButton) - "Xóa" - 110px width, 32px height
│       └── importButton (JButton) - "Import RFID" - 130px width, 32px height
│
├── tableContainer (JPanel - GroupLayout)
│   ├── Border: TitledBorder "Danh sách sinh viên"
│   ├── scrollPane (JScrollPane)
│   │   └── table (JTable)
│   │       Columns: Mã sinh viên, RFID, Tên sinh viên, Ngày tạo
│   │       Row height: 26px
│   │       Auto sort: true
│   └── paginationPanel (PaginationPanel)
│       Page size: 15 rows
│
└── statusLabel (JLabel) - 24px height, Color: (90, 90, 90)
```

### StudentFormDialog (JDialog):
**Kích thước:** 420 x 320px  
**Layout:** BorderLayout(10, 10)

```
StudentFormDialog
├── form (JPanel - GridBagLayout)
│   ├── maSinhVienField (JTextField) - 20 columns
│   │   Enabled: false khi edit
│   ├── tenSinhVienField (JTextField) - 20 columns
│   ├── rfidField (JTextField) - 20 columns
│   ├── scanButton (JButton) - "Quét RFID" / "Dừng quét"
│   └── scanStatusLabel (JLabel)
│
└── buttons (JPanel - FlowLayout RIGHT)
    ├── cancel (JButton) - "Hủy"
    └── save (JButton) - "Lưu"
```

### Spacing:
- Gap giữa filterPanel và actionPanel: 12px (UNRELATED)
- Gap giữa các button: 12px (UNRELATED)

---

## 7. ClassManagementPanel (JPanel)

**Background:** Color(250, 250, 252)  
**Padding:** 10px all sides  
**Layout:** GroupLayout (Vertical)

### Cấu trúc:
```
ClassManagementPanel
├── headerPanel (JPanel - BorderLayout, opaque: false)
│   └── lblTitle (JLabel) - "Quản lý lớp học phần"
│       Font: Segoe UI Bold 20pt, Color: (33, 37, 41)
│
├── toolbar (JPanel - FlowLayout LEFT, gap: 8px, opaque: false)
│   ├── txtSearch (JTextField) - 220px width, 30px height
│   ├── btnSearch (JButton) - "Tìm kiếm"
│   ├── btnRefresh (JButton) - "Làm mới"
│   ├── btnCreate (JButton) - "Thêm lớp"
│   ├── btnEdit (JButton) - "Sửa"
│   ├── btnDelete (JButton) - "Xóa"
│   └── btnViewStudents (JButton) - "Sinh viên"
│
├── scrollPane (JScrollPane)
│   ├── Border: TitledBorder "Danh sách lớp học phần"
│   └── table (JTable)
│       Columns: Mã lớp, Tên lớp, Giảng viên, Hình thức, Phòng học, Số SV
│       Row height: 26px
│       Auto sort: true
│       Center renderer: Integer columns
│
├── paginationPanel (PaginationPanel)
│   Page size: 10 rows
│
└── lblStatus (JLabel) - 24px height, Color: (90, 90, 90)
```

### ClassDialog (JDialog):
**Kích thước:** 420 x 320px  
**Layout:** BorderLayout(10, 10)  
**Padding:** 12px all sides

```
ClassDialog
├── form (JPanel - GridLayout(0, 1, 6, 6))
│   ├── labeledField("Mã lớp học phần", txtMa)
│   │   txtMa: 20 columns, editable: false khi edit
│   ├── labeledField("Tên lớp học phần", txtTen)
│   │   txtTen: 20 columns
│   ├── labeledField("Giảng viên", txtGiangVien)
│   ├── labeledField("Phòng học", txtPhongHoc)
│   └── labeledField("Hình thức học", txtHinhThuc)
│
└── actions (JPanel - FlowLayout RIGHT)
    ├── btnCancel (JButton) - "Hủy"
    └── btnSave (JButton) - "Lưu"
```

### StudentListDialog (JDialog):
**Kích thước:** 560 x 420px  
**Layout:** BorderLayout(10, 10)  
**Padding:** 10px all sides

```
StudentListDialog
├── btnExport (JButton) - "Xuất Excel" - NORTH
├── scroll (JScrollPane) - CENTER
│   └── studentTable (JTable)
│       Columns: Mã sinh viên, Tên sinh viên, RFID, Ngày tạo
│       Row height: 24px
└── studentPagination (PaginationPanel) - SOUTH
    Page size: 15 rows
```

### Spacing:
- Gap giữa headerPanel và toolbar: 12px (UNRELATED)
- Gap giữa toolbar và scrollPane: 12px (UNRELATED)

---

## 8. DeviceManagementPanel (JPanel)

**Background:** Color(250, 250, 252)  
**Padding:** 10px all sides  
**Layout:** BorderLayout(10, 10)

### Cấu trúc:
```
DeviceManagementPanel
├── tabs (JTabbedPane) - CENTER
│   ├── Tab 1: "Quét RFID" → rfidPanel
│   │   ├── toolbar (JPanel - FlowLayout LEFT, gap: 10px, opaque: false)
│   │   │   ├── statusFilter (JComboBox) - ["Tất cả", "Đã đăng ký", "Chưa đăng ký"]
│   │   │   │   Preferred size: 180px width, 28px height
│   │   │   ├── btnTogglePolling (JButton) - "Quét RFID" / "Dừng quét"
│   │   │   └── btnRefreshRfids (JButton) - "Làm mới"
│   │   │
│   │   ├── scrollPane (JScrollPane)
│   │   │   ├── Border: TitledBorder "RFID chưa xử lý"
│   │   │   └── rfidTable (JTable)
│   │   │       Columns: ID, RFID, Mã SV, Tên sinh viên, Thời gian, Trạng thái
│   │   │       Row height: 26px
│   │   │       Auto sort: true
│   │   │       Center renderer: Integer columns
│   │   │       Popup menu: Copy RFID, Đánh dấu đã xử lý
│   │   │
│   │   └── rfidPagination (PaginationPanel)
│   │       Page size: 10 rows
│   │
│   └── Tab 2: "Thiết bị" → devicePanel
│       ├── form (JPanel - FlowLayout LEFT, gap: 10px)
│       │   ├── Border: TitledBorder "Đăng ký thiết bị"
│       │   ├── lblMaThietBi (JLabel) - "Mã thiết bị:"
│       │   ├── txtMaThietBi (JTextField) - 14 columns
│       │   ├── lblPhongHoc (JLabel) - "Phòng học:"
│       │   ├── txtPhongHoc (JTextField) - 14 columns
│       │   ├── btnSaveDevice (JButton) - "Lưu thiết bị"
│       │   ├── btnRefreshDevices (JButton) - "Làm mới"
│       │   └── btnDeleteDevice (JButton) - "Xóa thiết bị"
│       │
│       └── scrollPane (JScrollPane)
│           ├── Border: TitledBorder "Thiết bị đã đăng ký"
│           └── deviceTable (JTable)
│               Columns: Mã thiết bị, Phòng học
│               Row height: 26px
│               Auto sort: true
│
└── statusLabel (JLabelStatus) - SOUTH
    Color: (90, 90, 90)
    Border: EmptyBorder(4, 0, 0, 0)
```

### Spacing:
- Tab content: BorderLayout với gap 10px
- Toolbar: FlowLayout với gap 10px horizontal, 5px vertical

---

## 9. PlaceholderPanel (JPanel)

**Layout:** BorderLayout  
**Component:** JLabel với text được truyền vào, center aligned

---

## Thông số chung

### Màu sắc:
- Background chính: Color(250, 250, 252)
- Background card: Color.WHITE
- Border card: Color(230, 234, 238)
- Text chính: Color(33, 37, 41)
- Text phụ: Color(90, 90, 90)
- Text cảnh báo: Color(204, 102, 0)
- Text lỗi: Color.RED
- Text thành công: Color(33, 150, 83)

### Font:
- Title: Segoe UI Bold 20pt
- Subtitle: Segoe UI Plain 14pt
- Stat value: Segoe UI Bold 18-28pt
- Button: Default font

### Kích thước button:
- Standard: 110px width, 32px height
- Wide: 120-130px width, 32px height
- Small: 100px width, 32px height

### Kích thước text field:
- Standard: 150-240px width, 32px height
- Small: 140px width, 32px height

### Spacing:
- Gap RELATED: 6-8px
- Gap UNRELATED: 12px
- Padding panel: 10-12px
- Padding card: 12px

### Table settings:
- Row height: 24-28px
- Auto sort: true
- Fills viewport: true
- Center renderer cho các cột số và thời gian

### Pagination:
- Page size: 10-15 rows tùy panel
- Component: PaginationPanel (custom)

---

## Lưu ý khi thiết kế

1. **GroupLayout**: Sử dụng GroupLayout cho các panel chính để có control tốt về spacing
2. **BorderLayout**: Sử dụng cho các container lớn và dialog
3. **FlowLayout**: Sử dụng cho toolbar và form đơn giản
4. **GridBagLayout**: Sử dụng cho form phức tạp (LoginPanel, Dialog)
5. **BoxLayout**: Sử dụng cho NavigationPanel (vertical list)
6. **CardLayout**: Sử dụng cho MainFrame để chuyển đổi màn hình
7. **TitledBorder**: Sử dụng cho các section có border và title
8. **EmptyBorder**: Sử dụng cho padding
9. **CompoundBorder**: Sử dụng cho card style (line + empty)

### Best Practices:
- Luôn set opaque = false cho các panel container
- Sử dụng preferred size cho các component quan trọng
- Set row height cho table để đồng nhất
- Sử dụng center renderer cho các cột số
- Thêm status label ở cuối mỗi panel để hiển thị thông báo
- Sử dụng SwingWorker cho các tác vụ async
- Validate input trước khi submit form

