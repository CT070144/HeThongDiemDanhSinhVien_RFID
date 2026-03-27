import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Container, Row, Col, Card, Table, Form, Button, Alert, Badge, Modal, Spinner } from 'react-bootstrap';
import { attendanceAPI, caLamAPI, roomAPI, phongBanAPI } from '../services/api';
import api from '../services/api';
import { useNotification } from '../contexts/NotificationContext';
import io  from "socket.io-client";
import { formatTime } from '../services/format-time';
import { FaFilter,FaArrowUp, FaArrowDown, FaTimes, FaCalendarAlt, FaClock, FaIdCard, FaDoorOpen, FaHistory, FaFileExport, FaChartBar, FaCheckCircle, FaExclamationTriangle, FaClock as FaClockIcon, FaInfoCircle, FaSignOutAlt } from 'react-icons/fa';


const AttendanceHistory = () => {
  const { notify } = useNotification();
  const [attendance, setAttendance] = useState([]);
  const [filteredAttendance, setFilteredAttendance] = useState([]);
  const [allFilteredAttendance, setAllFilteredAttendance] = useState([]);
  const socketRef = useRef(null);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [attendanceStats, setAttendanceStats] = useState({
    dangHoc: 0,
    daRaVe: 0,
    raVeSom: 0,
    khongDiemDanhRa: 0
  });
  const [page, setPage] = useState(1);
  const [pageSize] = useState(12);
  const [filters, setFilters] = useState({
    startDate: '',
    endDate: '',
    ca: '',
    maSinhVien: '',
    phongHoc: [],
    tinhTrang: '',
    trangThai: '',
    maPhongBan: '',
    sortDir: 'DESC' // DESC: ngày mới nhất -> cũ nhất
  });
  const [showPhongHocModal, setShowPhongHocModal] = useState(false);
  const [showPhongBanModal, setShowPhongBanModal] = useState(false);
  const [phongHocSearch, setPhongHocSearch] = useState('');
  const [phongBanSearch, setPhongBanSearch] = useState('');
  const [phongHocOptions, setPhongHocOptions] = useState([]);
  const [phongBanOptions, setPhongBanOptions] = useState([]);
  const [caLams, setCaLams] = useState([]);

  const [showAttendanceDetailModal, setShowAttendanceDetailModal] = useState(false);
  const [attendanceDetail, setAttendanceDetail] = useState(null);
  const [attendanceDetailLoading, setAttendanceDetailLoading] = useState(false);

  useEffect(() => {
    loadAttendance();
    loadCaLams();
    loadLookupOptions();
  }, [page]);

  const loadCaLams = async () => {
    try {
      const response = await caLamAPI.getAll();
      const data = response.data || [];
      const sorted = [...data].sort((a, b) => (a.maCa || 0) - (b.maCa || 0));
      setCaLams(sorted);
    } catch (error) {
      console.error('Error loading ca lam:', error);
      setCaLams([]);
    }
  };
  const loadLookupOptions = async () => {
    try {
      const [roomsRes, pbRes] = await Promise.all([roomAPI.getAll(), phongBanAPI.getAll()]);
      const roomOptions = (roomsRes?.data || [])
        .map((r) => r.maPhong || r.tenPhong)
        .filter(Boolean)
        .sort();
      const pbOptions = (pbRes?.data || [])
        .map((p) => p.maPhongBan)
        .filter(Boolean)
        .sort();
      setPhongHocOptions(roomOptions);
      setPhongBanOptions(pbOptions);
    } catch (error) {
      setPhongHocOptions([]);
      setPhongBanOptions([]);
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

      socketRef.current.on("update-attendance", () => {
        loadAttendance();
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

  const calculateAttendanceStats = (attendanceRecords) => {
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
    
    const dangHoc = dangHocStudents.size;
    const daRaVe = daRaVeStudents.size;
    const raVeSom = raVeSomStudents.size;
    const khongDiemDanhRa = khongDiemDanhRaStudents.size;

    setAttendanceStats({
      dangHoc,
      daRaVe,
      raVeSom,
      khongDiemDanhRa
    });
  };

  const loadAttendance = useCallback(async () => {
    try {
      const response = await attendanceAPI.getPaged({
        page: page - 1,
        size: pageSize,
        startDate: filters.startDate,
        endDate: filters.endDate,
        ca: filters.ca,
        maSinhVien: filters.maSinhVien,
        phongHoc: (filters.phongHoc || []).join(','),
        tinhTrang: filters.tinhTrang,
        trangThai: filters.trangThai,
        maPhongBan: filters.maPhongBan,
        sortDir: filters.sortDir
      });
      const content = response?.data?.content || [];
      setAttendance(content);
      setFilteredAttendance(content);
      setTotalElements(response?.data?.totalElements || 0);
      setTotalPages(response?.data?.totalPages || 0);
    } catch (error) {
      notify.error('Lỗi khi tải lịch sử chấm công');
    }
  }, [filters, notify, page, pageSize]);

  const handleViewAttendanceDetail = async (record) => {
    if (!record || !record.id) return;
    setAttendanceDetailLoading(true);
    setAttendanceDetail(null);
    try {
      const resp = typeof attendanceAPI?.getAttendanceDetail === 'function'
        ? await attendanceAPI.getAttendanceDetail(record.id)
        : await api.get(`/attendance/detail/${record.id}`);
      setAttendanceDetail(resp.data || null);
      setShowAttendanceDetailModal(true);
      console.log(resp.data);
      
    } catch (error) {
      console.error('Error loading attendance detail:', error);
    } finally {
      setAttendanceDetailLoading(false);
    }
  };

  useEffect(() => {
    loadAttendance();
  }, [loadAttendance]);

  const loadAttendanceStatsByFilters = useCallback(async () => {
    try {
      const response = await attendanceAPI.getPaged({
        page: 0,
        size: 100000,
        startDate: filters.startDate,
        endDate: filters.endDate,
        ca: filters.ca,
        maSinhVien: filters.maSinhVien,
        phongHoc: (filters.phongHoc || []).join(','),
        tinhTrang: filters.tinhTrang,
        trangThai: filters.trangThai,
        maPhongBan: filters.maPhongBan,
        sortDir: filters.sortDir
      });
      const allFiltered = response?.data?.content || [];
      setAllFilteredAttendance(allFiltered);
      calculateAttendanceStats(allFiltered);
    } catch (error) {
      setAllFilteredAttendance([]);
      calculateAttendanceStats([]);
    }
  }, [filters]);

  useEffect(() => {
    loadAttendanceStatsByFilters();
  }, [loadAttendanceStatsByFilters]);


  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setPage(1);
    setFilters(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const clearFilters = () => {
    setFilters({
      startDate: '',
      endDate: '',
      ca: '',
      maSinhVien: '',
      phongHoc: [],
      tinhTrang: '',
      trangThai: '',
      maPhongBan: '',
      sortDir: 'DESC'
    });
    setPage(1);
  };
  const hasActiveFilters = Boolean(
    filters.startDate ||
    filters.endDate ||
    filters.ca ||
    filters.maSinhVien ||
    (filters.phongHoc && filters.phongHoc.length > 0) ||
    filters.tinhTrang ||
    filters.trangThai ||
    filters.maPhongBan ||
    (filters.sortDir && filters.sortDir !== 'DESC')
  );

  const toggleSortDate = () => {
    setPage(1);
    setFilters((prev) => ({
      ...prev,
      sortDir: prev.sortDir === 'ASC' ? 'DESC' : 'ASC'
    }));
  };

  const togglePhongHoc = (roomCode) => {
    setPage(1);
    setFilters((prev) => {
      const current = prev.phongHoc || [];
      return {
        ...prev,
        phongHoc: current.includes(roomCode)
          ? current.filter((x) => x !== roomCode)
          : [...current, roomCode]
      };
    });
  };

  const togglePhongBan = (maPhongBan) => {
    setPage(1);
    setFilters((prev) => {
      const current = prev.maPhongBan ? prev.maPhongBan.split(',').map(x => x.trim()).filter(Boolean) : [];
      const next = current.includes(maPhongBan)
        ? current.filter((x) => x !== maPhongBan)
        : [...current, maPhongBan];
      return { ...prev, maPhongBan: next.join(',') };
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
      'DANG_HOC': { variant: 'primary', text: 'Đang làm' },
      'DA_RA_VE': { variant: 'success', text: 'Đã ra về' },
      'RA_VE_SOM': { variant: 'warning', text: 'Ra về sớm' },
      'KHONG_DIEM_DANH_RA': { variant: 'danger', text: 'Không chấm công ra' }
    };
    const status = statusMap[trangThai] || { variant: 'light', text: trangThai };
    return <Badge bg={status.variant}>{status.text}</Badge>;
  };

  const exportExcel = async () => {
    try {
      if (!filters.startDate || !filters.endDate) {
        notify.error('Khi xuất file bắt buộc phải chọn từ ngày và đến ngày');
        return;
      }
      const response = await attendanceAPI.exportExcel({
        startDate: filters.startDate,
        endDate: filters.endDate,
        ca: filters.ca,
        maSinhVien: filters.maSinhVien,
        phongHoc: (filters.phongHoc || []).join(','),
        tinhTrang: filters.tinhTrang,
        trangThai: filters.trangThai,
        maPhongBan: filters.maPhongBan
      });
      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      const filename = `BangChamCong_${filters.startDate}_${filters.endDate}.xlsx`;
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      notify.error('Lỗi khi xuất file Excel');
    }
  };

  const getCaName = (ca) => {
    const shift = caLams.find(item => Number(item.maCa) === Number(ca));
    if (!shift) return `Ca ${ca}`;
    const start = typeof shift.gioBatDau === 'string' ? shift.gioBatDau.substring(0, 5) : '';
    const end = typeof shift.gioKetThuc === 'string' ? shift.gioKetThuc.substring(0, 5) : '';
    if (start && end) {
      return `${shift.tenCa || `Ca ${shift.maCa}`} (${start}-${end})`;
    }
    return shift.tenCa || `Ca ${shift.maCa}`;
  };

  // Format time strings like "15:41:03.7472476" to "15:41:03"
  


  return (
    <Container
      fluid
      className="py-4"
      style={{
        '--bs-primary': '#212529',
        '--bs-primary-rgb': '33, 37, 41'
      }}
    >
      <Row>
        <Col>
          <Card className="shadow-sm" style={{ border: 'none' }}>
            <Card.Header className="bg-primary text-white d-flex justify-content-between align-items-center" style={{ border: 'none' }}>
              <h3 className="mb-0 d-flex align-items-center">
                <FaHistory className="me-2" />
                Lịch sử chấm công
              </h3>
              <div className="d-flex gap-2">
                <Button variant="outline-light" onClick={toggleSortDate} className="shadow-sm">
                  <FaCalendarAlt className="me-2" />
                  Sắp xếp thời gian {filters.sortDir === 'ASC' ? <FaArrowUp></FaArrowUp>: <FaArrowDown></FaArrowDown>}
                </Button>
                <Button variant="light" onClick={exportExcel} className="shadow-sm">
                  <FaFileExport className="me-2" />
                  Xuất Excel
                </Button>
              </div>
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
                          Từ ngày
                        </Form.Label>
                        <Form.Control
                          type="date"
                          name="startDate"
                          value={filters.startDate}
                          onChange={handleFilterChange}
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                        />
                      </Form.Group>
                    </Col>
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                          <FaCalendarAlt className="me-2 text-primary" />
                          Đến ngày
                        </Form.Label>
                        <Form.Control
                          type="date"
                          name="endDate"
                          value={filters.endDate}
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
                          Ca làm
                        </Form.Label>
                        <Form.Select
                          name="ca"
                          value={filters.ca}
                          onChange={handleFilterChange}
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                        >
                          <option value="">Tất cả ca</option>
                          {caLams.map((ca) => {
                            const start = typeof ca.gioBatDau === 'string' ? ca.gioBatDau.substring(0, 5) : '';
                            const end = typeof ca.gioKetThuc === 'string' ? ca.gioKetThuc.substring(0, 5) : '';
                            return (
                              <option key={ca.maCa} value={ca.maCa}>
                                {ca.tenCa || `Ca ${ca.maCa}`}{start && end ? ` (${start}-${end})` : ''}
                              </option>
                            );
                          })}
                        </Form.Select>
                      </Form.Group>
                    </Col>
                   
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                          <FaIdCard className="me-2 text-primary" />
                          Mã  nhân viên
                        </Form.Label>
                        <Form.Control
                          type="text"
                          name="maSinhVien"
                          value={filters.maSinhVien}
                          onChange={handleFilterChange}
                          placeholder="Nhập mã  nhân viên..."
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                        />
                      </Form.Group>
                    </Col>
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                          <FaDoorOpen className="me-2 text-primary" />
                          Vị trí chấm công
                        </Form.Label>
                        <Form.Select
                          value={(filters.phongHoc && filters.phongHoc[0]) || ''}
                          onChange={(e) => {
                            const value = e.target.value;
                            setPage(1);
                            setFilters((prev) => ({ ...prev, phongHoc: value ? [value] : [] }));
                          }}
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                        >
                          <option value="">Tất cả vị trí</option>
                          {phongHocOptions.map((roomCode) => (
                            <option key={roomCode} value={roomCode}>
                              {roomCode}
                            </option>
                          ))}
                        </Form.Select>
                      </Form.Group>
                    </Col>
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">Mã phòng ban</Form.Label>
                        <Form.Select
                          name="maPhongBan"
                          value={filters.maPhongBan}
                          onChange={handleFilterChange}
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                        >
                          <option value="">Tất cả phòng ban</option>
                          {phongBanOptions.map((code) => (
                            <option key={code} value={code}>
                              {code}
                            </option>
                          ))}
                        </Form.Select>
                      </Form.Group>
                    </Col>
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">Tình trạng</Form.Label>
                        <Form.Select
                          name="tinhTrang"
                          value={filters.tinhTrang}
                          onChange={handleFilterChange}
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                        >
                          <option value="">Tất cả</option>
                          <option value="DUNG_GIO">Đúng giờ</option>
                          <option value="MUON">Muộn</option>
                        </Form.Select>
                      </Form.Group>
                    </Col>
                    <Col xs={12} sm={6} md={4} lg={2}>
                      <Form.Group className="mb-0">
                        <Form.Label className="fw-semibold d-flex align-items-center mb-2">Trạng thái</Form.Label>
                        <Form.Select
                          name="trangThai"
                          value={filters.trangThai}
                          onChange={handleFilterChange}
                          className="shadow-sm"
                          style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                        >
                          <option value="">Tất cả</option>
                          <option value="DANG_HOC">Đang làm</option>
                          <option value="DA_RA_VE">Đã ra về</option>
                          <option value="RA_VE_SOM">Ra về sớm</option>
                          <option value="KHONG_DIEM_DANH_RA">Không chấm công ra</option>
                        </Form.Select>
                      </Form.Group>
                    </Col>
                    {hasActiveFilters && (
                      <Col xs={12} sm={6} md={4} lg={2} className="d-flex align-items-end">
                        <Button 
                          
                          onClick={clearFilters}
                          className="w-100 shadow-sm"
                          style={{ 
                            borderRadius: '0.375rem',
                            fontWeight: '500',
                        backgroundColor: '#212529',
                        border: 'none',
                            position: 'relative',
                            top: '-2px'
                          }}
                        >
                          <FaTimes className="me-2 clear-filters" />
                          Xóa bộ lọc
                        </Button>
                      </Col>
                    )}
                  </Row>
                </Card.Body>
              </Card>

              <Row className="mt-4">
                <Col md={12}>
                  <Card className="shadow-sm" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
                    <Card.Header className="text-white d-flex align-items-center" style={{ backgroundColor: '#212529', border: 'none', borderRadius: '0.375rem 0.375rem 0 0' }}>
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
                            <p className="text-muted small mb-0">Chấm công muộn</p>
                          </div>
                        </Col>
                        <Col xs={6} sm={4} md={2}>
                          <div className="text-center p-3 bg-white rounded shadow-sm" style={{ border: '1px solid #dee2e6' }}>
                            <FaClockIcon className="text-info mb-2" size={24} />
                            <h4 className="text-info mb-1 fw-bold">
                              {(allFilteredAttendance.length > 0 ? allFilteredAttendance : attendance).filter(r => r.trangThai === 'DANG_HOC' || r.trangThai === 'dang_hoc').length}
                            </h4>
                            <p className="text-muted small mb-0">Đang làm</p>
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
                            <p className="text-muted small mb-0">Không chấm công ra</p>
                          </div>
                        </Col>
                      </Row>


              {/* Bảng lịch sử */}
              <div className="table-responsive mt-4">
                <Table responsive striped hover className="mb-0" style={{ fontSize: '0.95rem' }}>
                  <thead className="table-primary">
                    <tr>
                      <th style={{ fontWeight: '600' }}>RFID</th>
                      <th style={{ fontWeight: '600' }}>Mã  nhân viên</th>
                      <th style={{ fontWeight: '600' }}>Tên  nhân viên</th>
                      
                      <th style={{ fontWeight: '600' }}>Mã phòng ban</th>
                      <th style={{ fontWeight: '600' }}>Vị trí chấm công</th>
                      <th style={{ fontWeight: '600' }}>Ngày</th>
                      <th style={{ fontWeight: '600' }}>Ca</th>
                      <th style={{ fontWeight: '600' }}>Giờ vào</th>
                      <th style={{ fontWeight: '600' }}>Giờ ra</th>
                      <th style={{ fontWeight: '600' }}>Tình trạng chấm công</th>
                      <th style={{ fontWeight: '600' }}>Trạng thái</th>
                      <th style={{ fontWeight: '600' }}>Chi tiết</th>
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
                       
                        <td>{record.maPhongBan || <span className="text-muted">-</span>}</td>
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
                        <td>
                          <Button
                            variant="outline-primary"
                            size="sm"
                            onClick={() => handleViewAttendanceDetail(record)}
                          >
                            Xem chi tiết
                          </Button>
                        </td>
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
                      ? "Vui lòng chọn Ngày và ca làm để xem dữ liệu chấm công của  ."
                      : "Không có dữ liệu chấm công nào được tìm thấy."
                    }
                  </Alert>
                </div>
              )}

              {/* Pagination */}
              {filteredAttendance.length > 0 && (
                <div className="d-flex justify-content-between align-items-center mt-4 pt-3 border-top">
                  <div className="text-muted fw-semibold">
                    Trang <span className="text-primary">{page}</span> / {Math.max(totalPages, 1)} - Tổng {totalElements} bản ghi
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
                      disabled={page >= totalPages} 
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

      <Modal
        show={showAttendanceDetailModal}
        onHide={() => setShowAttendanceDetailModal(false)}
        size="lg"
        centered
      >
        <Modal.Header closeButton className="text-white" style={{ backgroundColor: '#212529' }}>
          <Modal.Title>Chi tiết phiếu điểm danh</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {attendanceDetailLoading ? (
            <div className="d-flex align-items-center gap-2">
              <Spinner animation="border" size="sm" />
              Đang tải...
            </div>
          ) : !attendanceDetail?.attendance ? (
            <div className="text-muted">Không có dữ liệu.</div>
          ) : (
            <div className="d-flex gap-4">
              <div style={{ width: 170 }}>
                {attendanceDetail.photoDataUrl ? (
                  <img
                    src={attendanceDetail.photoDataUrl}
                    alt="Ảnh điểm danh"
                    style={{
                      width: 160,
                      height: 200,
                      objectFit: 'cover',
                      borderRadius: 8,
                      border: '1px solid #dee2e6'
                    }}
                  />
                ) : (
                  <div
                    className="text-muted small"
                    style={{
                      width: 160,
                      height: 200,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      border: '1px dashed #dee2e6',
                      borderRadius: 8
                    }}
                  >
                    Không có ảnh
                  </div>
                )}
              </div>

              <div style={{ flex: 1 }}>
                <div><strong>ID:</strong> {attendanceDetail.attendance.id}</div>
                <div><strong>RFID:</strong> {attendanceDetail.attendance.rfid}</div>
                <div><strong>Mã SV:</strong> {attendanceDetail.attendance.maSinhVien}</div>
                <div><strong>Tên SV:</strong> {attendanceDetail.attendance.tenSinhVien}</div>
                <div><strong>Bộ phận:</strong> {attendanceDetail.attendance.maPhongBan || '-'}</div>
                <div><strong>Vị trí:</strong> {attendanceDetail.attendance.phongHoc || '-'}</div>
                <div><strong>Ngày:</strong> {attendanceDetail.attendance.ngay ? new Date(attendanceDetail.attendance.ngay).toLocaleDateString('vi-VN') : '-'}</div>
                <div><strong>Ca:</strong> {getCaName(attendanceDetail.attendance.ca)}</div>
                <div><strong>Giờ vào:</strong> {formatTime(attendanceDetail.attendance.gioVao) || '-'}</div>
                <div><strong>Giờ ra:</strong> {formatTime(attendanceDetail.attendance.gioRa) || '-'}</div>
                <div><strong>Tình trạng:</strong> {getStatusBadge(attendanceDetail.attendance.tinhTrangDiemDanh)}</div>
                <div><strong>Trạng thái:</strong> {getAttendanceStatusBadge(attendanceDetail.attendance.trangThai)}</div>
              </div>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowAttendanceDetailModal(false)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>

      <Modal show={showPhongHocModal} onHide={() => setShowPhongHocModal(false)} centered>
        <Modal.Header closeButton className="text-white" style={{ backgroundColor: '#212529' }}>
          <Modal.Title>Chọn vị trí chấm công</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Control
            type="text"
            placeholder="Tìm vị trí..."
            value={phongHocSearch}
            onChange={(e) => setPhongHocSearch(e.target.value)}
            className="mb-3"
          />
          <div style={{ maxHeight: '320px', overflowY: 'auto' }}>
            {(phongHocOptions || [])
              .filter((x) => x.toLowerCase().includes(phongHocSearch.toLowerCase()))
              .map((roomCode) => (
                <Form.Check
                  key={roomCode}
                  type="checkbox"
                  className="mb-2"
                  label={roomCode}
                  checked={(filters.phongHoc || []).includes(roomCode)}
                  onChange={() => togglePhongHoc(roomCode)}
                />
              ))}
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => setFilters((prev) => ({ ...prev, phongHoc: [] }))}>Xóa lựa chọn</Button>
          <Button variant="secondary" onClick={() => setShowPhongHocModal(false)}>Đóng</Button>
        </Modal.Footer>
      </Modal>

      <Modal show={showPhongBanModal} onHide={() => setShowPhongBanModal(false)} centered>
        <Modal.Header closeButton className="text-white" style={{ backgroundColor: '#212529' }}>
          <Modal.Title>Chọn phòng ban</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Control
            type="text"
            placeholder="Tìm mã phòng ban..."
            value={phongBanSearch}
            onChange={(e) => setPhongBanSearch(e.target.value)}
            className="mb-3"
          />
          <div style={{ maxHeight: '320px', overflowY: 'auto' }}>
            {(phongBanOptions || [])
              .filter((x) => x.toLowerCase().includes(phongBanSearch.toLowerCase()))
              .map((code) => {
                const selected = (filters.maPhongBan || '').split(',').map(x => x.trim()).filter(Boolean).includes(code);
                return (
                  <Form.Check
                    key={code}
                    type="checkbox"
                    className="mb-2"
                    label={code}
                    checked={selected}
                    onChange={() => togglePhongBan(code)}
                  />
                );
              })}
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => setFilters((prev) => ({ ...prev, maPhongBan: '' }))}>Xóa lựa chọn</Button>
          <Button variant="secondary" onClick={() => setShowPhongBanModal(false)}>Đóng</Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
};

export default AttendanceHistory;
