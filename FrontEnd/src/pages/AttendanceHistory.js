import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Container, Row, Col, Card, Table, Form, Button, Alert, Badge } from 'react-bootstrap';
import * as XLSX from 'xlsx';
import { toast } from 'react-toastify';
import { attendanceAPI } from '../services/api';
import api from '../services/api';
import io  from "socket.io-client";


const AttendanceHistory = () => {
  const [attendance, setAttendance] = useState([]);
  const [filteredAttendance, setFilteredAttendance] = useState([]);
  const [allFilteredAttendance, setAllFilteredAttendance] = useState([]);
  const [lopHocPhans, setLopHocPhans] = useState([]);
  const [studentsInLop, setStudentsInLop] = useState([]);
  const socketRef = useRef(null);
  const [attendanceStats, setAttendanceStats] = useState({
    totalStudents: 0,
    attended: 0,
    absent: 0,
    late: 0,
    dangHoc: 0,
    daRaVe: 0,
    raVeSom: 0,
    khongDiemDanhRa: 0
  });
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [filters, setFilters] = useState({
    ngay: '',
    ca: '',
    maSinhVien: '',
    phongHoc: '',
    lopHocPhan: ''
  });

  useEffect(() => {
    loadAttendance();
   
  }, []);
  useEffect(() => {
    // Initialize socket connection only once
    if (!socketRef.current) {
      console.log("Initializing socket connection...");

      const connectionUrl = "http://localhost:8099";

      socketRef.current = io(connectionUrl, {
        path: "/socket.io",
        query: { token: localStorage.getItem('token') },
        transports: ["websocket", "polling"],
        reconnection: true,
        reconnectionAttempts: 10,
        reconnectionDelay: 1000,
      });
      socketRef.current.on("connect", () => {
        console.log("Socket connected");
      });

      socketRef.current.on("disconnect", () => {
        console.log("Socket disconnected");
      });

      socketRef.current.on("connect_error", (err) => {
        console.error("Socket connect_error:", err.message || err);
      });

      socketRef.current.on("update-attendance", (result) => {
        console.log("New message received:", result);
        result = JSON.parse(result);
      setAttendance((prev) => {
        // replace if same id exists; otherwise prepend
        const index = prev.findIndex((r) => r.id === result.id);
        let next;
        if (index !== -1) {
          next = [...prev];
          next[index] = result;
        } else {
          next = [result, ...prev];
        }
        return next.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      });



      });
    }

    // Cleanup function - disconnect socket when component unmounts
    return () => {
      if (socketRef.current) {
        console.log("Disconnecting socket...");
        socketRef.current.disconnect();
        socketRef.current = null;
      }
    };
  }, []);

  const filterAttendance = useCallback(async () => {
    let filtered = [...attendance];

    // Validate: Nếu lọc theo lớp học phần thì bắt buộc phải có ngày và ca
    if (filters.lopHocPhan && (!filters.ngay || !filters.ca)) {
      // Không filter gì cả nếu thiếu ngày hoặc ca
      setStudentsInLop([]);
      setAttendanceStats({ totalStudents: 0, attended: 0, absent: 0, late: 0, dangHoc: 0, daRaVe: 0, raVeSom: 0, khongDiemDanhRa: 0 });
      setAllFilteredAttendance([]);
      setFilteredAttendance([]);
      return;
    }

    if (filters.ngay) {
      filtered = filtered.filter(item => item.ngay === filters.ngay);
    }

    if (filters.ca) {
      filtered = filtered.filter(item => item.ca === parseInt(filters.ca));
    }

    if (filters.maSinhVien) {
      filtered = filtered.filter(item =>
        item.maSinhVien.toLowerCase().includes(filters.maSinhVien.toLowerCase())
      );
    }

    if (filters.phongHoc) {
      filtered = filtered.filter(item => (item.phongHoc || '').toLowerCase().includes(filters.phongHoc.toLowerCase()));
    }

    // Filter by lop hoc phan
    if (filters.lopHocPhan) {
      try {
        const response = await api.get(`/lophocphan/${filters.lopHocPhan}/sinhvien`);
        const studentsInLop = response.data;
        const studentIdsInLop = studentsInLop.map(s => s.maSinhVien);
        filtered = filtered.filter(item => studentIdsInLop.includes(item.maSinhVien));
        setStudentsInLop(studentsInLop);
        
        // Calculate attendance stats for the class
        calculateAttendanceStats(studentsInLop, filtered);
      } catch (error) {
        console.error('Error filtering by lop hoc phan:', error);
      }
    } else {
      setStudentsInLop([]);
      setAttendanceStats({ totalStudents: 0, attended: 0, absent: 0, late: 0, dangHoc: 0, daRaVe: 0, raVeSom: 0, khongDiemDanhRa: 0 });
    }

    // Đảm bảo dữ liệu đã lọc cũng được sắp xếp theo thời gian tạo mới nhất
    const sortedFiltered = filtered.sort((a, b) => 
      new Date(b.createdAt) - new Date(a.createdAt)
    );
    
    const start = (page - 1) * pageSize;
    const end = start + pageSize;
    setAllFilteredAttendance(sortedFiltered);
    setFilteredAttendance(sortedFiltered.slice(start, end));
  }, [attendance, filters, page, pageSize]);

  const calculateAttendanceStats = (studentsInLop, attendanceRecords) => {
    const totalStudents = studentsInLop.length;
    const attendedStudents = new Set(attendanceRecords.map(r => r.maSinhVien));
    const lateStudents = new Set(attendanceRecords.filter(r => 
      r.tinhTrangDiemDanh === 'muon' || r.tinhTrangDiemDanh === 'MUON'
    ).map(r => r.maSinhVien));
    const dangHocStudents = new Set(attendanceRecords.filter(r => 
      r.trangThai === 'DANG_HOC' || r.trangThai === 'dang_hoc'
    ).map(r => r.maSinhVien));
    const daRaVeStudents = new Set(attendanceRecords.filter(r => 
      r.trangThai === 'DA_RA_VE' || r.trangThai === 'da_ra_ve'
    ).map(r => r.maSinhVien));
    const raVeSomStudents = new Set(attendanceRecords.filter(r => 
      r.trangThai === 'RA_VE_SOM' || r.trangThai === 'ra_ve_som'
    ).map(r => r.maSinhVien));
    const khongDiemDanhRaStudents = new Set(attendanceRecords.filter(r => 
      r.trangThai === 'KHONG_DIEM_DANH_RA' || r.trangThai === 'khong_diem_danh_ra'
    ).map(r => r.maSinhVien));
    
    const attended = attendedStudents.size;
    const late = lateStudents.size;
    const absent = totalStudents - attended;
    const dangHoc = dangHocStudents.size;
    const daRaVe = daRaVeStudents.size;
    const raVeSom = raVeSomStudents.size;
    const khongDiemDanhRa = khongDiemDanhRaStudents.size;

    setAttendanceStats({
      totalStudents,
      attended,
      absent,
      late,
      dangHoc,
      daRaVe,
      raVeSom,
      khongDiemDanhRa
    });
  };

  useEffect(() => {
    filterAttendance();
  }, [filterAttendance]);

  const loadAttendance = async () => {
    try {
      const response = await attendanceAPI.getAll();
      // Sắp xếp theo thời gian tạo mới nhất lên đầu
      const sortedData = response.data.sort((a, b) => 
        new Date(b.createdAt) - new Date(a.createdAt)
      );
      setAttendance(sortedData);
    } catch (error) {
      // tránh spam toast do polling liên tục
      // toast.error('Lỗi khi tải lịch sử điểm danh');
    }

  };


  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const clearFilters = () => {
    setFilters({
      ngay: '',
      ca: '',
      maSinhVien: '',
      phongHoc: '',
      lopHocPhan: ''
    });
  };

  const getStatusBadge = (trangThai) => {
    const statusMap = {
      'MUON': { variant: 'warning', text: 'Muộn' },
      'DUNG_GIO': { variant: 'success', text: 'Đúng giờ' }
    };
    const status = statusMap[trangThai] || { variant: 'light', text: trangThai };
    return <Badge bg={status.variant}>{status.text}</Badge>;
  };

  const getAttendanceStatusBadge = (trangThai) => {
    const statusMap = {
      'DANG_HOC': { variant: 'primary', text: 'Đang học' },
      'DA_RA_VE': { variant: 'success', text: 'Đã ra về' },
      'RA_VE_SOM': { variant: 'warning', text: 'Ra về sớm' },
      'KHONG_DIEM_DANH_RA': { variant: 'danger', text: 'Không điểm danh ra' }
    };
    const status = statusMap[trangThai] || { variant: 'light', text: trangThai };
    return <Badge bg={status.variant}>{status.text}</Badge>;
  };

  const exportExcel = () => {
    // Kiểm tra điều kiện export khi lọc theo lớp học phần
    if (filters.lopHocPhan && (!filters.ngay || !filters.ca)) {
      toast.error('Khi lọc theo lớp học phần, bạn phải chọn cả Ngày và Ca học để xuất Excel!');
      return;
    }

    const wb = XLSX.utils.book_new();
    
    // Create header information
    const headerInfo = [];
    
    if (filters.lopHocPhan) {
      const selectedLop = lopHocPhans.find(l => l.maLopHocPhan === filters.lopHocPhan);
      const caName = getCaName(parseInt(filters.ca));
      
      headerInfo.push(['BAN CƠ YẾU CHÍNH PHỦ', '', '', '', '', '', '', '', 'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM']);
      headerInfo.push(['HỌC VIỆN KỸ THUẬT MẬT MÃ', '', '', '', '', '', '', '', 'Độc lập - Tự do - Hạnh phúc']);
      headerInfo.push([]);
      headerInfo.push([]);
      headerInfo.push(['', '', '', 'DANH SÁCH ĐIỂM DANH SINH VIÊN', '', '', '', '']);
      headerInfo.push(['', '', `Lớp: ${selectedLop?.tenLopHocPhan}`, '', '', '', '', '']);
      headerInfo.push(['', '', `Ngày: ${new Date(filters.ngay).toLocaleDateString('vi-VN')} - ${caName}`, '', '', '', '', '']);
      headerInfo.push(['', '', `Học kỳ 1 - Năm học 2025 - 2026`, '', '', '', '', '']);
      headerInfo.push([]);
      headerInfo.push(['STT', 'Mã sinh viên', 'Họ và tên', 'Điểm danh', 'Ghi chú', '', '', '']);
    } else {
      // Header for general attendance export
      headerInfo.push(['BAN CƠ YẾU CHÍNH PHỦ', '', '', '', '', '', '', '', '', '', 'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM']);
      headerInfo.push(['HỌC VIỆN KỸ THUẬT MẬT MÃ', '', '', '', '', '', '', '', '', '', 'Độc lập - Tự do - Hạnh phúc']);
      headerInfo.push([]);
      headerInfo.push([]);
      headerInfo.push(['', '', '', '', 'LỊCH SỬ ĐIỂM DANH SINH VIÊN', '', '', '', '', '', '']);
      headerInfo.push(['', '', '', '', `Xuất ngày: ${new Date().toLocaleDateString('vi-VN')}`, '', '', '', '', '', '']);
      headerInfo.push([]);
      headerInfo.push(['RFID', 'Mã sinh viên', 'Tên sinh viên', 'Phòng học', 'Ngày', 'Ca', 'Giờ vào', 'Giờ ra', 'Tình trạng điểm danh', 'Ghi chú', 'Thời gian tạo']);
    }
    
    // Create attendance data
    let data = [];
    
    if (filters.lopHocPhan && studentsInLop.length > 0) {
      // Export all students in class with attendance status
      data = studentsInLop.map((student, index) => {
        const attendanceRecord = allFilteredAttendance.find(r => r.maSinhVien === student.maSinhVien);
        let diemDanh = 'v'; // vắng mặc định
        let ghiChu = ''; // ghi chú mặc định
        let rowStyle = null; // Không có màu nền mặc định
        
        if (attendanceRecord) {
          if (attendanceRecord.tinhTrangDiemDanh === 'muon' || attendanceRecord.tinhTrangDiemDanh === 'MUON') {
            diemDanh = 'M'; // muộn
            rowStyle = { fill: { fgColor: { rgb: 'FFFFE0' } } }; // Vàng nhạt
          } else {
            diemDanh = 'x'; // có mặt
          }
          
          // Thêm ghi chú dựa trên trạng thái
          if (attendanceRecord.trangThai === 'RA_VE_SOM' || attendanceRecord.trangThai === 'ra_ve_som') {
            ghiChu = 'Ra về sớm';
          } else if (attendanceRecord.trangThai === 'KHONG_DIEM_DANH_RA' || attendanceRecord.trangThai === 'khong_diem_danh_ra') {
            ghiChu = 'Không điểm danh ra';
          }
        } else {
          // Vắng mặt - tô màu đỏ nhạt
          diemDanh = 'v'; // vắng
          rowStyle = { fill: { fgColor: { rgb: 'FFE6E6' } } }; // Đỏ nhạt
        }
        
        return {
          data: [
            index + 1,
            student.maSinhVien,
            student.tenSinhVien,
            diemDanh,
            ghiChu
          ],
          style: rowStyle
        };
      });
    } else {
      // Export all attendance records với ghi chú trạng thái
      data = (allFilteredAttendance.length ? allFilteredAttendance : attendance).map(r => {
        let ghiChu = '';
        
        // Thêm ghi chú dựa trên trạng thái
        if (r.trangThai === 'RA_VE_SOM' || r.trangThai === 'ra_ve_som') {
          ghiChu = 'Ra về sớm';
        } else if (r.trangThai === 'KHONG_DIEM_DANH_RA' || r.trangThai === 'khong_diem_danh_ra') {
          ghiChu = 'Không điểm danh ra';
        }
        
        return [
          r.rfid,
          r.maSinhVien,
          r.tenSinhVien,
          r.phongHoc || '',
          r.ngay,
          r.ca,
          r.gioVao || '',
          r.gioRa || '',
          r.tinhTrangDiemDanh === 'dung_gio' ? 'Đúng giờ' : r.tinhTrangDiemDanh === 'muon' ? 'Muộn' : r.tinhTrangDiemDanh,
          ghiChu,
          new Date(r.createdAt).toLocaleString('vi-VN')
        ];
      });
    }
    
    // Combine header and data
    let allData = [...headerInfo];
    
    // Add data with proper formatting for class-specific export
    if (filters.lopHocPhan && studentsInLop.length > 0) {
      // For class-specific export, add data rows
      data.forEach(item => {
        allData.push(item.data);
      });
    } else {
      // For general export, add all data
      allData = [...allData, ...data];
    }
    
    // Add statistics if filtering by class
    if (filters.lopHocPhan && attendanceStats.totalStudents > 0) {
      allData.push([]);
      allData.push(['THỐNG KÊ:', '', '', '', '']);
      allData.push([`Tổng số sinh viên: ${attendanceStats.totalStudents}`, '', '', '', '']);
      allData.push([`Số sinh viên tham gia: ${attendanceStats.attended}`, '', '', '', '']);
      allData.push([`Số sinh viên vắng: ${attendanceStats.absent}`, '', '', '', '']);
      allData.push([`Số sinh viên muộn: ${attendanceStats.late}`, '', '', '', '']);
      allData.push([`Số sinh viên đang học: ${attendanceStats.dangHoc}`, '', '', '', '']);
      allData.push([`Số sinh viên đã ra về: ${attendanceStats.daRaVe}`, '', '', '', '']);
      allData.push([`Số sinh viên ra về sớm: ${attendanceStats.raVeSom}`, '', '', '', '']);
      allData.push([`Số sinh viên không điểm danh ra: ${attendanceStats.khongDiemDanhRa}`, '', '', '', '']);
    }
    
    let ws;
    
    // For class-specific export with styling, create HTML table and convert to Excel
    if (filters.lopHocPhan && studentsInLop.length > 0) {
      // Create HTML table with styling
      let htmlTable = '<table border="1" style="border-collapse: collapse;">';
      
      // Add header rows
      headerInfo.forEach(row => {
        htmlTable += '<tr>';
        row.forEach(cell => {
          htmlTable += `<td style="font-weight: bold; text-align: center; background-color: #f0f0f0; padding: 5px;">${cell || ''}</td>`;
        });
        htmlTable += '</tr>';
      });
      
      // Add data rows with styling
      data.forEach(item => {
        const backgroundColor = item.style?.fill?.fgColor?.rgb === 'FFFFE0' ? '#FFFFE0' : 
                               item.style?.fill?.fgColor?.rgb === 'FFE6E6' ? '#FFE6E6' : '';
        
        htmlTable += '<tr>';
        item.data.forEach(cell => {
          const style = backgroundColor ? `style="background-color: ${backgroundColor}; padding: 5px;"` : 'style="padding: 5px;"';
          htmlTable += `<td ${style}>${cell}</td>`;
        });
        htmlTable += '</tr>';
      });
      
      // Add statistics rows
      if (attendanceStats.totalStudents > 0) {
        htmlTable += '<tr><td colspan="5"></td></tr>';
        htmlTable += '<tr><td style="font-weight: bold;" colspan="5">THỐNG KÊ:</td></tr>';
        htmlTable += `<tr><td colspan="5">Tổng số sinh viên: ${attendanceStats.totalStudents}</td></tr>`;
        htmlTable += `<tr><td colspan="5">Số sinh viên tham gia: ${attendanceStats.attended}</td></tr>`;
        htmlTable += `<tr><td colspan="5">Số sinh viên vắng: ${attendanceStats.absent}</td></tr>`;
        htmlTable += `<tr><td colspan="5">Số sinh viên muộn: ${attendanceStats.late}</td></tr>`;
        htmlTable += `<tr><td colspan="5">Số sinh viên đang học: ${attendanceStats.dangHoc}</td></tr>`;
        htmlTable += `<tr><td colspan="5">Số sinh viên đã ra về: ${attendanceStats.daRaVe}</td></tr>`;
        htmlTable += `<tr><td colspan="5">Số sinh viên ra về sớm: ${attendanceStats.raVeSom}</td></tr>`;
        htmlTable += `<tr><td colspan="5">Số sinh viên không điểm danh ra: ${attendanceStats.khongDiemDanhRa}</td></tr>`;
      }
      
      htmlTable += '</table>';
      
      // Convert HTML table to worksheet
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = htmlTable;
      ws = XLSX.utils.table_to_sheet(tempDiv.querySelector('table'));
    } else {
      // For general export, use regular method
      ws = XLSX.utils.aoa_to_sheet(allData);
    }
    
    // Style the worksheet
    if (filters.lopHocPhan) {
      // Set column widths for class-specific export
      ws['!cols'] = [
        { width: 5 },   // STT
        { width: 15 },  // Mã sinh viên
        { width: 25 },  // Họ và tên
        { width: 12 }   // Điểm danh
      ];
    } else {
      // Set column widths for general attendance export
      ws['!cols'] = [
        { width: 20 },  // RFID
        { width: 15 },  // Mã sinh viên
        { width: 25 },  // Tên sinh viên
        { width: 15 },  // Phòng học
        { width: 12 },  // Ngày
        { width: 8 },   // Ca
        { width: 12 },  // Giờ vào
        { width: 12 },  // Giờ ra
        { width: 20 },  // Tình trạng điểm danh
        { width: 20 },  // Ghi chú
        { width: 20 }   // Thời gian tạo
      ];
    }
    
    XLSX.utils.book_append_sheet(wb, ws, 'Attendance');
    
    // Generate filename
    let filename = 'attendance.xlsx';
    if (filters.lopHocPhan) {
      const selectedLop = lopHocPhans.find(l => l.maLopHocPhan === filters.lopHocPhan);
      const dateStr = filters.ngay || new Date().toISOString().split('T')[0];
      filename = `DiemDanh_${selectedLop?.maLopHocPhan}_${dateStr}.xlsx`;
    } else {
      // General attendance export filename
      const dateStr = new Date().toISOString().split('T')[0];
      filename = `LichSuDiemDanh_${dateStr}.xlsx`;
    }
    
    XLSX.writeFile(wb, filename);
  };

  const getCaName = (ca) => {
    const caMap = {
      1: 'Ca 1 (07:00-09:25)',
      2: 'Ca 2 (09:35-12:00)',
      3: 'Ca 3 (12:30-14:55)',
      4: 'Ca 4 (15:05-17:30)',
      5: 'Ca 5 (18:00-20:30)'
    };
    return caMap[ca] || `Ca ${ca}`;
  };


  return (
    <Container>
      <Row>
        <Col>
          <Card>
            <Card.Header className="d-flex justify-content-between align-items-center">
              <h3>Lịch sử điểm danh</h3>
              <Button variant="outline-success" onClick={exportExcel}>Xuất Excel</Button>
            </Card.Header>
            <Card.Body>
              {/* Bộ lọc */}
              <Row className="mb-3">
                <Col md={2}>
                  <Form.Group>
                    <Form.Label>Ngày</Form.Label>
                    <Form.Control
                      type="date"
                      name="ngay"
                      value={filters.ngay}
                      onChange={handleFilterChange}
                    />
                  </Form.Group>
                </Col>
                <Col md={2}>
                  <Form.Group>
                    <Form.Label>Ca học</Form.Label>
                    <Form.Select
                      name="ca"
                      value={filters.ca}
                      onChange={handleFilterChange}
                    >
                      <option value="">Tất cả ca</option>
                      <option value="1">Ca 1 (07:00-09:25)</option>
                      <option value="2">Ca 2 (09:35-12:00)</option>
                      <option value="3">Ca 3 (12:30-14:55)</option>
                      <option value="4">Ca 4 (15:05-17:30)</option>
                      <option value="5">Ca 5 (18:00-20:30)</option>
                    </Form.Select>
                  </Form.Group>
                </Col>
                <Col md={2}>
                  <Form.Group>
                    <Form.Label>Lớp học phần</Form.Label>
                    <Form.Select
                      name="lopHocPhan"
                      value={filters.lopHocPhan}
                      onChange={handleFilterChange}
                    >
                      <option value="">Tất cả lớp</option>
                      {lopHocPhans.map((lop) => (
                        <option key={lop.maLopHocPhan} value={lop.maLopHocPhan}>
                          {lop.tenLopHocPhan}
                        </option>
                      ))}
                    </Form.Select>
                  </Form.Group>
                </Col>
                <Col md={2}>
                  <Form.Group>
                    <Form.Label>Mã sinh viên</Form.Label>
                    <Form.Control
                      type="text"
                      name="maSinhVien"
                      value={filters.maSinhVien}
                      onChange={handleFilterChange}
                      placeholder="Nhập mã sinh viên..."
                    />
                  </Form.Group>
                </Col>
                <Col md={2}>
                  <Form.Group>
                    <Form.Label>Phòng học</Form.Label>
                    <Form.Control
                      type="text"
                      name="phongHoc"
                      value={filters.phongHoc}
                      onChange={handleFilterChange}
                      placeholder="Nhập phòng học..."
                    />
                  </Form.Group>
                </Col>
                <Col md={2} className="d-flex align-items-end">
                  <Button style={{position: 'relative', top: '-10px'}} variant="outline-secondary" onClick={clearFilters}>
                    Xóa bộ lọc
                  </Button>
                </Col>
              </Row>

              {/* Cảnh báo khi chọn lớp học phần nhưng thiếu ngày/ca */}
              {filters.lopHocPhan && (!filters.ngay || !filters.ca) && (
                <Row className="mb-3">
                  <Col>
                    <Alert variant="warning">
                      <strong>⚠️ Lưu ý:</strong> Khi lọc theo lớp học phần, bạn phải chọn cả <strong>Ngày</strong> và <strong>Ca học</strong> để xem kết quả.
                    </Alert>
                  </Col>
                </Row>
              )}

              {/* Thống kê lớp học phần */}
              {filters.lopHocPhan && filters.ngay && filters.ca && attendanceStats.totalStudents > 0 && (
                <Row className="mb-3">
                  <Col>
                    <Alert variant="info">
                      <strong>Thống kê lớp học phần:</strong>
                      <div className="mt-2">
                        <Badge bg="primary" className="me-2">
                          Tổng số sinh viên: {attendanceStats.totalStudents}
                        </Badge>
                        <Badge bg="success" className="me-2">
                          Tham gia: {attendanceStats.attended}
                        </Badge>
                        <Badge bg="danger" className="me-2">
                          Vắng: {attendanceStats.absent}
                        </Badge>
                        <Badge bg="warning" className="me-2">
                          Muộn: {attendanceStats.late}
                        </Badge>
                       
                      </div>
                    </Alert>
                  </Col>
                </Row>
              )}

              {/* Bảng lịch sử */}
              <Table responsive striped bordered hover>
                <thead>
                  <tr>
                    <th>RFID</th>
                    <th>Mã sinh viên</th>
                    <th>Tên sinh viên</th>
                    <th>Phòng học</th>
                    <th>Ngày</th>
                    <th>Ca</th>
                    <th>Giờ vào</th>
                    <th>Giờ ra</th>
                    <th>Tình trạng điểm danh</th>
                    <th>Trạng thái</th>
                    <th>Thời gian tạo</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAttendance.map((record) => (
                    <tr key={record.id ?? `${record.rfid || 'rfid'}-${record.createdAt || record.ngay || ''}-${record.ca || ''}`}>
                      <td>{record.rfid}</td>
                      <td>{record.maSinhVien}</td>
                      <td>{record.tenSinhVien}</td>
                      <td>{record.phongHoc || '-'}</td>
                      <td>{new Date(record.ngay).toLocaleDateString('vi-VN')}</td>
                      <td>{getCaName(record.ca)}</td>
                      <td>{record.gioVao || '-'}</td>
                      <td>{record.gioRa || '-'}</td>
                      <td>{getStatusBadge(record.tinhTrangDiemDanh)}</td>
                      <td>{getAttendanceStatusBadge(record.trangThai)}</td>
                      <td>{new Date(record.createdAt).toLocaleString('vi-VN')}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>

              {filteredAttendance.length === 0 && (
                <Alert variant="info">
                  {filters.lopHocPhan && (!filters.ngay || !filters.ca) 
                    ? "Vui lòng chọn Ngày và Ca học để xem dữ liệu điểm danh của lớp học phần."
                    : "Không có dữ liệu điểm danh nào được tìm thấy."
                  }
                </Alert>
              )}

              {/* Pagination */}
              <div className="d-flex justify-content-between align-items-center mt-3">
                <div>Trang {page}</div>
                <div className="d-flex gap-2">
                  <Button variant="outline-secondary" disabled={page === 1} onClick={() => setPage(p => Math.max(1, p - 1))}>Trước</Button>
                  <Button variant="outline-secondary" disabled={attendance.length <= page * pageSize} onClick={() => setPage(p => p + 1)}>Sau</Button>
                </div>
              </div>

              {/* Thống kê */}
              <Row className="mt-4">
                <Col md={12}>
                  <Card>
                    <Card.Header>
                      <h5>Thống kê</h5>
                    </Card.Header>
                    <Card.Body>
                      <Row>
                        <Col md={4}>
                          <div className="text-center">
                            <h4 className="text-primary">
                              {allFilteredAttendance.length > 0 ? allFilteredAttendance.length : attendance.length}
                            </h4>
                            <p>Tổng bản ghi</p>
                          </div>
                        </Col>
                        <Col md={4}>
                          <div className="text-center">
                            <h4 className="text-success">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.tinhTrangDiemDanh === 'DUNG_GIO' || r.tinhTrangDiemDanh === 'dung_gio').length}
                            </h4>
                            <p>Đúng giờ</p>
                          </div>
                        </Col>
                        <Col md={4}>
                          <div className="text-center">
                            <h4 className="text-warning">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.tinhTrangDiemDanh === 'MUON' || r.tinhTrangDiemDanh === 'muon').length}
                            </h4>
                            <p>Điểm danh muộn</p>
                          </div>
                        </Col>
                        <Col md={4}>
                          <div className="text-center">
                            <h4 className="text-info">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.trangThai === 'DANG_HOC' || r.trangThai === 'dang_hoc').length}
                            </h4>
                            <p>Đang học</p>
                          </div>
                        </Col>
                      <Col md={4}>
                          <div className="text-center">
                            <h4 className="text-success">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.trangThai === 'DA_RA_VE' || r.trangThai === 'da_ra_ve').length}
                            </h4>
                            <p>Đã ra về</p>
                          </div>
                        </Col>
                        <Col md={4}>
                          <div className="text-center">
                            <h4 className="text-danger">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.trangThai === 'KHONG_DIEM_DANH_RA' || r.trangThai === 'khong_diem_danh_ra').length}
                            </h4>
                            <p>Không điểm danh ra</p>
                          </div>
                        </Col>
             
                      </Row>

                       
                        
             
                    </Card.Body>
                  </Card>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default AttendanceHistory;
