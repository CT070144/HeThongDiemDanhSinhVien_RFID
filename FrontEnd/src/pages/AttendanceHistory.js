import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Container, Row, Col, Card, Table, Form, Button, Alert, Badge, Modal } from 'react-bootstrap';
import * as XLSX from 'xlsx';
import { exportAttendanceToExcel } from '../services/exportExcel';
import { attendanceAPI } from '../services/api';
import { useNotification } from '../contexts/NotificationContext';
import api from '../services/api';
import io  from "socket.io-client";
import { formatTime } from '../services/format-time';
import { FaFilter, FaTimes, FaCalendarAlt, FaClock, FaGraduationCap, FaIdCard, FaDoorOpen, FaHistory, FaFileExport, FaChartBar, FaCheckCircle, FaExclamationTriangle, FaClock as FaClockIcon, FaInfoCircle, FaSignOutAlt } from 'react-icons/fa';


const AttendanceHistory = () => {
  const { notify } = useNotification();
  const [attendance, setAttendance] = useState([]);
  const [filteredAttendance, setFilteredAttendance] = useState([]);
  const [allFilteredAttendance, setAllFilteredAttendance] = useState([]);
  const [lopHocPhans, setLopHocPhans] = useState([]);
  const [studentsInLop, setStudentsInLop] = useState([]);
  const socketRef = useRef(null);
  const notifiedIdsRef = useRef(new Set()); // Track IDs that have been notified
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
  const [pageSize] = useState(12);
  const [filters, setFilters] = useState({
    ngay: '',
    ca: '',
    maSinhVien: '',
    phongHoc: '',
    lopHocPhan: ''
  });
  const [showLopHocPhanModal, setShowLopHocPhanModal] = useState(false);
  const [lopHocPhanSearch, setLopHocPhanSearch] = useState('');
  const [selectedLopHocPhanName, setSelectedLopHocPhanName] = useState('');

  useEffect(() => {
    loadAttendance();
    loadLopHocPhans();
  }, []);
  const loadLopHocPhans = async () => {
    try {
      const response = await api.get('/lophocphan');
      setLopHocPhans(response.data);
    } catch (error) {
      console.error('Error loading lop hoc phan:', error);
    }
  };
  useEffect(() => {
    // Initialize socket connection only once
    if (!socketRef.current) {
      console.log("Initializing socket connection...");

      const connectionUrl = process.env.REACT_APP_SOCKET_URL || "http://localhost:8099";

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
        try {
          result = typeof result === 'string' ? JSON.parse(result) : result;
        } catch (e) {
          console.error("Error parsing result:", e);
          return;
        }
        
        setAttendance((prev) => {
          // Kiểm tra xem đây có phải là bản ghi mới không (chưa có trong danh sách)
          const isNewRecord = !prev.some(r => r.id === result.id);
          
          // replace if same id exists; otherwise prepend
          const index = prev.findIndex((r) => r.id === result.id);
          let next;
          if (index !== -1) {
            next = [...prev];
            next[index] = result;
          } else {
            next = [result, ...prev];
          }
          
          // Không hiển thị thông báo ở đây vì đã có global AttendanceNotificationListener
          // Chỉ cập nhật state để hiển thị trong danh sách
          
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
    let filtered = [];

    // Nếu lọc theo lớp học phần, sử dụng API riêng
    if (filters.lopHocPhan) {
      try {
        // Gọi API mới để lấy phiếu điểm danh theo lớp học phần
        const response = await attendanceAPI.getByLopHocPhan(filters.lopHocPhan);
        filtered = response.data || [];
        
        // Lấy danh sách sinh viên trong lớp để tính thống kê
        const studentsResponse = await api.get(`/lophocphan/${filters.lopHocPhan}/sinhvien`);
        const studentsInLop = studentsResponse.data || [];
        setStudentsInLop(studentsInLop);
        
        // Tính thống kê điểm danh cho lớp
        calculateAttendanceStats(studentsInLop, filtered);
        
        // Áp dụng các filter bổ sung (ngày, ca, mã sinh viên, phòng học)
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
      } catch (error) {
        console.error('Error filtering by lop hoc phan:', error);
        notify.error('Lỗi khi lọc theo lớp học phần');
        setStudentsInLop([]);
        setAttendanceStats({ totalStudents: 0, attended: 0, absent: 0, late: 0, dangHoc: 0, daRaVe: 0, raVeSom: 0, khongDiemDanhRa: 0 });
        setAllFilteredAttendance([]);
        setFilteredAttendance([]);
        return;
      }
    } else {
      // Không lọc theo lớp học phần, sử dụng logic cũ
      filtered = [...attendance];

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
  }, [attendance, filters, page, pageSize, notify]);

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
      console.log(sortedData);
      setAttendance(sortedData);
      // Đánh dấu tất cả các bản ghi đã load là đã được thông báo để tránh hiển thị toast cho dữ liệu cũ
      sortedData.forEach(record => {
        if (record.id) {
          notifiedIdsRef.current.add(record.id);
        }
      });
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
    setSelectedLopHocPhanName('');
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
    exportAttendanceToExcel({
      attendance,
      allFilteredAttendance,
      studentsInLop,
      filters,
      lopHocPhans,
      attendanceStats
    });
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

  // Format time strings like "15:41:03.7472476" to "15:41:03"
  


  return (
    <Container fluid className="py-4">
      <Row>
        <Col>
          <Card className="shadow-sm" style={{ border: 'none' }}>
            <Card.Header className="bg-primary text-white d-flex justify-content-between align-items-center" style={{ border: 'none' }}>
              <h3 className="mb-0 d-flex align-items-center">
                <FaHistory className="me-2" />
                Lịch sử điểm danh
              </h3>
              <Button variant="light" onClick={exportExcel} className="shadow-sm">
                <FaFileExport className="me-2" />
                Xuất Excel
              </Button>
            </Card.Header>
            <Card.Body className="p-4">
              {/* Bộ lọc */}
              <Card className="mb-4 shadow-sm" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
                <Card.Header className="bg-primary text-white d-flex align-items-center" style={{ border: 'none', borderRadius: '0.375rem 0.375rem 0 0' }}>
                  <FaFilter className="me-2" />
                  <h5 className="mb-0">Bộ lọc tìm kiếm</h5>
                </Card.Header>
                <Card.Body className="p-4">
                  <Row className="g-3">
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                          <FaCalendarAlt className="me-2 text-primary" />
                          Ngày
                        </Form.Label>
                        <Form.Control
                          type="date"
                          name="ngay"
                          value={filters.ngay}
                          onChange={handleFilterChange}
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                        />
                      </Form.Group>
                    </Col>
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                          <FaClock className="me-2 text-primary" />
                          Ca học
                        </Form.Label>
                        <Form.Select
                          name="ca"
                          value={filters.ca}
                          onChange={handleFilterChange}
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
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
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                          <FaGraduationCap className="me-2 text-primary" />
                          Lớp học phần
                        </Form.Label>
                        <Button
                          variant="outline-primary"
                          className="w-100 shadow-sm text-start d-flex justify-content-between align-items-center"
                          style={{ borderRadius: '0.375rem' }}
                          onClick={() => setShowLopHocPhanModal(true)}
                        >
                          <span style={{ 
                            whiteSpace: 'nowrap', 
                            overflow: 'hidden', 
                            textOverflow: 'ellipsis',
                            flex: 1,
                            minWidth: 0
                          }}>
                            {selectedLopHocPhanName || 'Chọn lớp học phần'}
                          </span>
                        </Button>
                      </Form.Group>
                    </Col>
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                          <FaIdCard className="me-2 text-primary" />
                          Mã sinh viên
                        </Form.Label>
                        <Form.Control
                          type="text"
                          name="maSinhVien"
                          value={filters.maSinhVien}
                          onChange={handleFilterChange}
                          placeholder="Nhập mã sinh viên..."
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                        />
                      </Form.Group>
                    </Col>
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                          <FaDoorOpen className="me-2 text-primary" />
                          Phòng học
                        </Form.Label>
                        <Form.Control
                          type="text"
                          name="phongHoc"
                          value={filters.phongHoc}
                          onChange={handleFilterChange}
                          placeholder="Nhập phòng học..."
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                        />
                      </Form.Group>
                    </Col>
                    <Col xs={12} sm={6} md={4} lg={2} className="d-flex align-items-end">
                      <Button 
                        variant="outline-danger" 
                        onClick={clearFilters}
                        className="w-100 shadow-sm"
                        style={{ 
                          borderRadius: '0.375rem',
                          fontWeight: '500',
                          border: '1px solid #dc3545',
                          position: 'relative',
                          top: '-10px'
                        }}
                      >
                        <FaTimes className="me-2 clear-filters" />
                        Xóa bộ lọc
                      </Button>
                    </Col>
                  </Row>
                </Card.Body>
              </Card>

              <Row className="mt-4">
                <Col md={12}>
                  <Card className="shadow-sm" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
                    <Card.Header className="bg-info text-white d-flex align-items-center" style={{ border: 'none', borderRadius: '0.375rem 0.375rem 0 0' }}>
                      <FaChartBar className="me-2" />
                      <h5 className="mb-0">Thống kê</h5>
                    </Card.Header>
                    <Card.Body className="p-4">
                      <Row className="g-3">
                        <Col xs={6} sm={4} md={2}>
                          <div className="text-center p-3 bg-white rounded shadow-sm" style={{ border: '1px solid #dee2e6' }}>
                            <FaInfoCircle className="text-primary mb-2" size={24} />
                            <h4 className="text-primary mb-1 fw-bold">
                              {allFilteredAttendance.length > 0 ? allFilteredAttendance.length : attendance.length}
                            </h4>
                            <p className="text-muted small mb-0">Tổng bản ghi</p>
                          </div>
                        </Col>
                        <Col xs={6} sm={4} md={2}>
                          <div className="text-center p-3 bg-white rounded shadow-sm" style={{ border: '1px solid #dee2e6' }}>
                            <FaCheckCircle className="text-success mb-2" size={24} />
                            <h4 className="text-success mb-1 fw-bold">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.tinhTrangDiemDanh === 'DUNG_GIO' || r.tinhTrangDiemDanh === 'dung_gio').length}
                            </h4>
                            <p className="text-muted small mb-0">Đúng giờ</p>
                          </div>
                        </Col>
                        <Col xs={6} sm={4} md={2}>
                          <div className="text-center p-3 bg-white rounded shadow-sm" style={{ border: '1px solid #dee2e6' }}>
                            <FaExclamationTriangle className="text-warning mb-2" size={24} />
                            <h4 className="text-warning mb-1 fw-bold">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.tinhTrangDiemDanh === 'MUON' || r.tinhTrangDiemDanh === 'muon').length}
                            </h4>
                            <p className="text-muted small mb-0">Điểm danh muộn</p>
                          </div>
                        </Col>
                        <Col xs={6} sm={4} md={2}>
                          <div className="text-center p-3 bg-white rounded shadow-sm" style={{ border: '1px solid #dee2e6' }}>
                            <FaClockIcon className="text-info mb-2" size={24} />
                            <h4 className="text-info mb-1 fw-bold">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.trangThai === 'DANG_HOC' || r.trangThai === 'dang_hoc').length}
                            </h4>
                            <p className="text-muted small mb-0">Đang học</p>
                          </div>
                        </Col>
                        <Col xs={6} sm={4} md={2}>
                          <div className="text-center p-3 bg-white rounded shadow-sm" style={{ border: '1px solid #dee2e6' }}>
                            <FaCheckCircle className="text-success mb-2" size={24} />
                            <h4 className="text-success mb-1 fw-bold">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.trangThai === 'DA_RA_VE' || r.trangThai === 'da_ra_ve').length}
                            </h4>
                            <p className="text-muted small mb-0">Đã ra về</p>
                          </div>
                        </Col>
                        <Col xs={6} sm={4} md={2}>
                          <div className="text-center p-3 bg-white rounded shadow-sm" style={{ border: '1px solid #dee2e6' }}>
                            <FaSignOutAlt className="text-danger mb-2" size={24} />
                            <h4 className="text-danger mb-1 fw-bold">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.trangThai === 'KHONG_DIEM_DANH_RA' || r.trangThai === 'khong_diem_danh_ra').length}
                            </h4>
                            <p className="text-muted small mb-0">Không điểm danh ra</p>
                          </div>
                        </Col>
                      </Row>


              {/* Thông báo khi chọn lớp học phần */}
              {filters.lopHocPhan && (
                <Row className="mb-3">
                  <Col>
                    <Alert variant="info">
                      <strong>ℹ️ Thông tin:</strong> Đang hiển thị tất cả phiếu điểm danh của lớp học phần này. Bạn có thể lọc thêm theo ngày, ca, mã sinh viên hoặc phòng học.
                    </Alert>
                  </Col>
                </Row>
              )}

              {/* Thống kê lớp học phần */}
              {filters.lopHocPhan && attendanceStats.totalStudents > 0 && (
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
              <div className="table-responsive mt-4">
                <Table responsive striped hover className="mb-0" style={{ fontSize: '0.95rem' }}>
                  <thead className="table-primary">
                    <tr>
                      <th style={{ fontWeight: '600' }}>RFID</th>
                      <th style={{ fontWeight: '600' }}>Mã sinh viên</th>
                      <th style={{ fontWeight: '600' }}>Tên sinh viên</th>
                      <th style={{ fontWeight: '600' }}>Phòng học</th>
                      <th style={{ fontWeight: '600' }}>Ngày</th>
                      <th style={{ fontWeight: '600' }}>Ca</th>
                      <th style={{ fontWeight: '600' }}>Giờ vào</th>
                      <th style={{ fontWeight: '600' }}>Giờ ra</th>
                      <th style={{ fontWeight: '600' }}>Tình trạng điểm danh</th>
                      <th style={{ fontWeight: '600' }}>Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredAttendance.map((record) => (
                      <tr key={record.id ?? `${record.rfid || 'rfid'}-${record.createdAt || record.ngay || ''}-${record.ca || ''}`} style={{ verticalAlign: 'middle' }}>
                        <td>
                          <code className="bg-light px-2 py-1 rounded">{record.rfid}</code>
                        </td>
                        <td>
                          <Badge bg="secondary" style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                            {record.maSinhVien}
                          </Badge>
                        </td>
                        <td style={{ fontWeight: '500' }}>{record.tenSinhVien}</td>
                        <td>{record.phongHoc || <span className="text-muted">-</span>}</td>
                        <td>{new Date(record.ngay).toLocaleDateString('vi-VN')}</td>
                        <td>
                          <Badge bg="info" style={{ fontSize: '0.85rem' }}>
                            {getCaName(record.ca)}
                          </Badge>
                        </td>
                        <td>{formatTime(record.gioVao) || <span className="text-muted">-</span>}</td>
                        <td>{formatTime(record.gioRa) || <span className="text-muted">-</span>}</td>
                        <td>{getStatusBadge(record.tinhTrangDiemDanh)}</td>
                        <td>{getAttendanceStatusBadge(record.trangThai)}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </div>

              {filteredAttendance.length === 0 && (
                <div className="text-center py-5 mt-4">
                  <FaHistory size={64} className="text-muted mb-3" />
                  <Alert variant="info" className="d-inline-block">
                    {filters.lopHocPhan && (!filters.ngay || !filters.ca) 
                      ? "Vui lòng chọn Ngày và Ca học để xem dữ liệu điểm danh của lớp học phần."
                      : "Không có dữ liệu điểm danh nào được tìm thấy."
                    }
                  </Alert>
                </div>
              )}

              {/* Pagination */}
              {filteredAttendance.length > 0 && (
                <div className="d-flex justify-content-between align-items-center mt-4 pt-3 border-top">
                  <div className="text-muted fw-semibold">
                    Trang <span className="text-primary">{page}</span>
                  </div>
                  <div className="d-flex gap-2">
                    <Button 
                      variant="outline-secondary" 
                      disabled={page === 1} 
                      onClick={() => setPage(p => Math.max(1, p - 1))}
                      className="shadow-sm"
                    >
                      Trước
                    </Button>
                    <Button 
                      variant="outline-secondary" 
                      disabled={attendance.length < page * pageSize || filteredAttendance.length < page * pageSize} 
                      onClick={() => setPage(p => p + 1)}
                      className="shadow-sm"
                    >
                      Sau
                    </Button>
                  </div>
                </div>
              )}

              {/* Thống kê */}
              
                       
                        
             
                    </Card.Body>
                  </Card>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Modal chọn lớp học phần */}
      <Modal
        show={showLopHocPhanModal}
        onHide={() => setShowLopHocPhanModal(false)}
        size="lg"
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>Chọn lớp học phần</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Group className="mb-3">
            <Form.Label>Tìm kiếm theo mã hoặc tên lớp học phần</Form.Label>
            <Form.Control
              type="text"
              placeholder="Nhập mã lớp học phần hoặc tên lớp..."
              value={lopHocPhanSearch}
              onChange={(e) => setLopHocPhanSearch(e.target.value)}
            />
          </Form.Group>
          <div style={{ height: '400px', overflowY: 'auto' }}>
            <Table hover responsive size="sm" className="align-middle">
              <thead>
                <tr>
                  <th style={{ width: '5%' }}></th>
                  <th>Mã lớp học phần</th>
                  <th>Tên lớp học phần</th>
                </tr>
              </thead>
              <tbody>
                {lopHocPhans
                  .filter((lop) => {
                    if (!lopHocPhanSearch.trim()) return true;
                    const keyword = lopHocPhanSearch.toLowerCase();
                    return (
                      lop.maLopHocPhan.toLowerCase().includes(keyword) ||
                      (lop.tenLopHocPhan || '').toLowerCase().includes(keyword)
                    );
                  })
                  .map((lop) => (
                    <tr
                      key={lop.maLopHocPhan}
                      style={{ cursor: 'pointer' }}
                      onClick={() => {
                        setFilters((prev) => ({
                          ...prev,
                          lopHocPhan: lop.maLopHocPhan,
                        }));
                        setSelectedLopHocPhanName(lop.tenLopHocPhan);
                        setShowLopHocPhanModal(false);
                      }}
                    >
                      <td>
                        <Form.Check
                          type="radio"
                          name="lopHocPhanRadio"
                          checked={filters.lopHocPhan === lop.maLopHocPhan}
                          readOnly
                        />
                      </td>
                      <td>
                        <Badge bg="primary">{lop.maLopHocPhan}</Badge>
                      </td>
                      <td>{lop.tenLopHocPhan}</td>
                    </tr>
                  ))}
                {lopHocPhans.length === 0 && (
                  <tr>
                    <td colSpan={3} className="text-center text-muted py-3">
                      Chưa có lớp học phần nào
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="outline-secondary"
            onClick={() => {
              setFilters((prev) => ({ ...prev, lopHocPhan: '' }));
              setSelectedLopHocPhanName('');
            }}
          >
            Xóa lựa chọn
          </Button>
          <Button variant="secondary" onClick={() => setShowLopHocPhanModal(false)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
};

export default AttendanceHistory;
