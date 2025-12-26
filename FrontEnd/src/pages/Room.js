import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Alert, Badge, Modal, Form, Dropdown, Tabs, Tab, Spinner } from 'react-bootstrap';
import { roomAPI, attendanceAPI } from '../services/api';
import { useNotification } from '../contexts/NotificationContext';
import { FaBuilding, FaDoorOpen, FaUsers, FaCalendarCheck, FaClock, FaFileDownload, FaFilter, FaSearch, FaEye, FaCheckCircle, FaTimesCircle } from 'react-icons/fa';
import * as XLSX from 'xlsx';

const Room = () => {
  const { notify } = useNotification();
  // State cho phòng học
  const [rooms, setRooms] = useState([]);
  const [filteredRooms, setFilteredRooms] = useState([]);
  const [allRooms, setAllRooms] = useState([]); // Lưu tất cả phòng học để populate dropdown options
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [roomDetail, setRoomDetail] = useState(null);
  const [loading, setLoading] = useState(false);

  // Hàm xác định ca hiện tại dựa trên thời gian (theo logic AttendanceService.java)
  const getCurrentCa = () => {
    const now = new Date();
    const hour = now.getHours();
    const minute = now.getMinutes();
    const totalMinutes = hour * 60 + minute;

    // Ca 1: 7h - 9h25 (có thể điểm danh từ 00:10 - 9:35)
    if (totalMinutes >= 10 && totalMinutes < 575) {
      return 1;
    }
    // Ca 2: 9h35 - 12h (có thể điểm danh từ 9:25 - 12:30)
    if (totalMinutes >= 565 && totalMinutes < 750) {
      return 2;
    }
    // Ca 3: 12h30 - 14h55 (có thể điểm danh từ 12:20 - 15:05)
    if (totalMinutes >= 740 && totalMinutes < 905) {
      return 3;
    }
    // Ca 4: 15h05 - 17h30 (có thể điểm danh từ 14:55 - 17:40)
    if (totalMinutes >= 895 && totalMinutes < 1060) {
      return 4;
    }
    // Ca 5: 18h - 20h30 (có thể điểm danh từ 17:50 - 23:40)
    if (totalMinutes >= 1070 && totalMinutes < 1420) {
      return 5;
    }
    // Ngoài giờ học
    return null;
  };

  // Hàm lấy ngày hiện tại theo format YYYY-MM-DD
  const getTodayDate = () => {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  // State cho filters - mặc định ngày và ca hiện tại
  const [filterToaNha, setFilterToaNha] = useState('TA1');
  const [filterTang, setFilterTang] = useState('');
  const [filterNgay, setFilterNgay] = useState(getTodayDate());
  const [filterCa, setFilterCa] = useState(() => {
    const currentCa = getCurrentCa();
    return currentCa ? String(currentCa) : '';
  });
  const [viewMode, setViewMode] = useState('model'); // 'model', 'list', or 'schedule'
  const [scheduleData, setScheduleData] = useState([]);
  const [scheduleLoading, setScheduleLoading] = useState(false);

  // Load tất cả phòng học một lần khi component mount để populate dropdown
  useEffect(() => {
    loadAllRooms();
  }, []);

  useEffect(() => {
    if (viewMode === 'schedule') {
      loadSchedule();
    } else {
      loadRooms();
    }
  }, [filterToaNha, filterTang, filterNgay, filterCa, viewMode]);

  // Load tất cả phòng học (không filter) để populate dropdown options
  const loadAllRooms = async () => {
    try {
      const response = await roomAPI.getRoomsWithStatus(undefined, undefined, undefined, undefined);
      setAllRooms(response.data || []);
    } catch (error) {
      console.error('Error loading all rooms:', error);
    }
  };

  // Load phòng học với filter
  const loadRooms = async () => {
    setLoading(true);
    try {
      const params = {
        toaNha: filterToaNha || undefined,
        tang: filterTang ? parseInt(filterTang) : undefined,
        ngay: filterNgay || undefined,
        ca: filterCa ? parseInt(filterCa) : undefined,
      };
      
      const response = await roomAPI.getRoomsWithStatus(params.toaNha, params.tang, params.ngay, params.ca);
      setRooms(response.data);
      setFilteredRooms(response.data);
    } catch (error) {
      console.error('Error loading rooms:', error);
      notify.error('Không thể tải danh sách phòng học');
      setRooms([]);
      setFilteredRooms([]);
    } finally {
      setLoading(false);
    }
  };

  const loadSchedule = async () => {
    setScheduleLoading(true);
    try {
      const params = {
        toaNha: filterToaNha || undefined,
        tang: filterTang ? parseInt(filterTang) : undefined,
        ngay: filterNgay || undefined,
      };
      
      const response = await roomAPI.getRoomSchedule(params.toaNha, params.tang, params.ngay);
      setScheduleData(response.data);
    } catch (error) {
      console.error('Error loading schedule:', error);
      notify.error('Không thể tải lịch sử dụng phòng học');
      setScheduleData([]);
    } finally {
      setScheduleLoading(false);
    }
  };

  const getUniqueToaNha = () => {
    // Lấy từ tất cả phòng học (không filter) để dropdown luôn có đầy đủ options
    const allToaNha = [...new Set(allRooms.map(r => r.toaNha))].filter(Boolean).sort();
    return allToaNha;
  };

  const getUniqueTang = () => {
    // Lấy từ tất cả phòng học (không filter) để dropdown luôn có đầy đủ options
    // Nếu đã chọn tòa nhà, chỉ lấy tầng của tòa nhà đó
    let roomsToUse = allRooms;
    if (filterToaNha) {
      roomsToUse = allRooms.filter(r => r.toaNha === filterToaNha);
    }
    const allTang = [...new Set(roomsToUse.map(r => r.tang))].filter(Boolean).sort((a, b) => a - b);
    return allTang;
  };

  const getRoomsByFloor = (toaNha, tang) => {
    return filteredRooms.filter(r => r.toaNha === toaNha && r.tang === tang);
  };

  const handleViewDetail = async (room) => {
    setSelectedRoom(room);
    setLoading(true);
    try {
      // Lấy ngày và ca từ room object (khi click từ biểu đồ lịch) hoặc từ filter
      const params = {
        ngay: room.ngay || filterNgay || undefined,
        ca: room.ca !== undefined ? room.ca : (filterCa ? parseInt(filterCa) : undefined),
      };
      
      const response = await roomAPI.getRoomDetail(room.maPhong, params.ngay, params.ca);
      const detail = response.data;
      console.log(detail);
      
      // Format lại dữ liệu để phù hợp với UI
      setRoomDetail({
        ...detail,
        classInfo: detail.classInfo ? {
          ...detail.classInfo,
          ngayHoc: detail.classInfo.ngayHoc 
            ? new Date(detail.classInfo.ngayHoc).toLocaleDateString('vi-VN')
            : new Date().toLocaleDateString('vi-VN')
        } : null,
        attendanceList: detail.attendanceList ? detail.attendanceList.map(att => ({
          ...att,
          gioVao: att.gioVao ? (typeof att.gioVao === 'string' ? att.gioVao : att.gioVao.substring(0, 5)) : '-'
        })) : []
      });
      setShowDetailModal(true);
    } catch (error) {
      console.error('Error loading room detail:', error);
      notify.error('Không thể tải chi tiết phòng học');
    } finally {
      setLoading(false);
    }
  };

  const handleExportExcel = async () => {
    try {
      // Load schedule data nếu chưa có hoặc viewMode không phải schedule
      let dataToExport = scheduleData;
      if (viewMode !== 'schedule' || scheduleData.length === 0) {
        setScheduleLoading(true);
        try {
          const params = {
            toaNha: filterToaNha || undefined,
            tang: filterTang ? parseInt(filterTang) : undefined,
            ngay: filterNgay || undefined,
          };
          const response = await roomAPI.getRoomSchedule(params.toaNha, params.tang, params.ngay);
          dataToExport = response.data;
        } catch (error) {
          console.error('Error loading schedule for export:', error);
          notify.error('Không thể tải dữ liệu lịch học để xuất Excel');
          return;
        } finally {
          setScheduleLoading(false);
        }
      }

      if (!dataToExport || dataToExport.length === 0) {
        notify.warning('Không có dữ liệu để xuất Excel');
        return;
      }

      // Nhóm dữ liệu theo tòa nhà
      const groupedByBuilding = {};
      dataToExport.forEach(room => {
        const toaNha = room.toaNha || 'Không xác định';
        if (!groupedByBuilding[toaNha]) {
          groupedByBuilding[toaNha] = [];
        }
        groupedByBuilding[toaNha].push(room);
      });

      const wb = XLSX.utils.book_new();

      // Tạo sheet cho mỗi tòa nhà
      Object.entries(groupedByBuilding).forEach(([toaNha, rooms]) => {
        // Chuẩn bị dữ liệu cho sheet
        const sheetData = [];
        
        // Header row
        const headerRow = ['PHÒNG', 'CA 1 (07:00-09:25)', 'CA 2 (09:35-12:00)', 'CA 3 (12:30-14:55)', 'CA 4 (15:05-17:30)', 'CA 5 (18:00-20:30)'];
        sheetData.push(headerRow);

        // Data rows
        rooms.forEach(room => {
          const row = [room.maPhong || ''];
          
          // Kiểm tra "TRỐNG CẢ NGÀY"
          const isEmptyAllDay = room.caSchedules && room.caSchedules.length > 0 &&
            room.caSchedules[0] && 
            room.caSchedules[0].tenLopHocPhan === 'TRỐNG CẢ NGÀY';
          
          if (isEmptyAllDay) {
            row.push('TRỐNG CẢ NGÀY');
            // Thêm các cột còn lại là rỗng (nhưng sẽ merge trong Excel)
            for (let i = 1; i < 5; i++) {
              row.push('');
            }
            sheetData.push(row);
            return;
          }

          // Xử lý từng ca
          const processedCaIndices = new Set();
          
          for (let caIndex = 0; caIndex < 5; caIndex++) {
            // Bỏ qua nếu ca này đã được xử lý (là phần của lớp học kéo dài)
            if (processedCaIndices.has(caIndex)) {
              continue;
            }

            const ca = room.caSchedules && room.caSchedules[caIndex];
            
            if (!ca) {
              row.push('');
              continue;
            }

            // Kiểm tra lớp học kéo dài nhiều ca
            if (ca.isSpanning && ca.tenLopHocPhan && ca.status !== 'empty') {
              // Tìm tất cả các ca có cùng tên lớp học phần
              const sameClassCas = [];
              for (let i = caIndex; i < 5; i++) {
                const c = room.caSchedules[i];
                if (c && c.tenLopHocPhan === ca.tenLopHocPhan && c.isSpanning && c.status !== 'empty') {
                  sameClassCas.push(i);
                  processedCaIndices.add(i);
                } else {
                  break;
                }
              }
              
              // Đây là ca đầu tiên của lớp học kéo dài
              let cellValue = ca.tenLopHocPhan || '';
              if (ca.giangVien) {
                cellValue += `\n${ca.giangVien}`;
              }
              if (ca.thoiGianBatDau && ca.thoiGianKetThuc) {
                const startTime = typeof ca.thoiGianBatDau === 'string' 
                  ? ca.thoiGianBatDau.substring(0, 5) 
                  : ca.thoiGianBatDau;
                const endTime = typeof ca.thoiGianKetThuc === 'string' 
                  ? ca.thoiGianKetThuc.substring(0, 5) 
                  : ca.thoiGianKetThuc;
                cellValue += `\n${startTime}-${endTime}`;
              }
              row.push(cellValue);
              
              // Bỏ qua các ca tiếp theo của lớp học kéo dài
              for (let i = 1; i < sameClassCas.length; i++) {
                row.push('');
              }
            } else {
              // Ca bình thường
              if (ca.tenLopHocPhan && ca.tenLopHocPhan !== 'TRỐNG CẢ NGÀY') {
                let cellValue = ca.tenLopHocPhan;
                if (ca.giangVien) {
                  cellValue += `\n${ca.giangVien}`;
                }
                if (ca.thoiGianBatDau && ca.thoiGianKetThuc) {
                  const startTime = typeof ca.thoiGianBatDau === 'string' 
                    ? ca.thoiGianBatDau.substring(0, 5) 
                    : ca.thoiGianBatDau;
                  const endTime = typeof ca.thoiGianKetThuc === 'string' 
                    ? ca.thoiGianKetThuc.substring(0, 5) 
                    : ca.thoiGianKetThuc;
                  cellValue += `\n${startTime}-${endTime}`;
                }
                row.push(cellValue);
              } else if (ca.status === 'empty' && ca.tenLopHocPhan === 'Trống') {
                row.push('Trống');
              } else {
                row.push('');
              }
              processedCaIndices.add(caIndex);
            }
          }

          sheetData.push(row);
        });

        // Tạo worksheet
        const ws = XLSX.utils.aoa_to_sheet(sheetData);

        // Set column widths
        ws['!cols'] = [
          { wch: 15 },  // PHÒNG
          { wch: 40 },  // CA 1
          { wch: 40 },  // CA 2
          { wch: 40 },  // CA 3
          { wch: 40 },  // CA 4
          { wch: 40 },  // CA 5
        ];

        // Style header row
        const headerRange = XLSX.utils.decode_range(ws['!ref']);
        for (let col = 0; col <= headerRange.e.c; col++) {
          const cellAddress = XLSX.utils.encode_cell({ r: 0, c: col });
          if (!ws[cellAddress]) continue;
          ws[cellAddress].s = {
            font: { bold: true, color: { rgb: 'FFFFFF' } },
            fill: { fgColor: { rgb: '4472C4' } },
            alignment: { horizontal: 'center', vertical: 'center', wrapText: true }
          };
        }

        // Style data rows
        for (let row = 1; row <= headerRange.e.r; row++) {
          for (let col = 0; col <= headerRange.e.c; col++) {
            const cellAddress = XLSX.utils.encode_cell({ r: row, c: col });
            if (!ws[cellAddress]) continue;
            
            // Set alignment và wrap text
            if (!ws[cellAddress].s) ws[cellAddress].s = {};
            ws[cellAddress].s.alignment = { 
              horizontal: 'center', 
              vertical: 'center', 
              wrapText: true 
            };
            
            // Style cho các ô có lớp học (màu đỏ)
            if (col > 0 && ws[cellAddress].v && ws[cellAddress].v !== 'Trống' && ws[cellAddress].v !== 'TRỐNG CẢ NGÀY' && ws[cellAddress].v !== '') {
              ws[cellAddress].s.fill = { fgColor: { rgb: 'DC3545' } };
              ws[cellAddress].s.font = { color: { rgb: 'FFFFFF' }, bold: true };
            }
            // Style cho các ô trống (màu xanh nhạt)
            else if (col > 0 && ws[cellAddress].v === 'Trống') {
              ws[cellAddress].s.fill = { fgColor: { rgb: 'D4EDDA' } };
              ws[cellAddress].s.font = { color: { rgb: '155724' }, bold: true };
            }
          }
        }

        // Set row heights
        ws['!rows'] = [];
        for (let row = 0; row <= headerRange.e.r; row++) {
          ws['!rows'][row] = { hpt: 60 }; // Height in points
        }

        // Append sheet với tên tòa nhà
        const sheetName = `Tòa ${toaNha}`.substring(0, 31); // Excel sheet name limit is 31 characters
        XLSX.utils.book_append_sheet(wb, ws, sheetName);
      });

      // Generate filename
      const dateStr = filterNgay 
        ? new Date(filterNgay).toISOString().split('T')[0]
        : new Date().toISOString().split('T')[0];
      const filename = `LichSuDungPhongHoc_${dateStr}.xlsx`;
      
      XLSX.writeFile(wb, filename);
      notify.success(`Xuất Excel thành công! Đã tạo ${Object.keys(groupedByBuilding).length} sheet.`);
    } catch (error) {
      console.error('Error exporting Excel:', error);
      notify.error('Lỗi khi xuất Excel: ' + error.message);
    }
  };

  const getStatusColor = (status) => {
    return status === 'occupied' ? 'danger' : 'success';
  };

  const getStatusText = (status) => {
    return status === 'occupied' ? 'Hoạt động' : 'Trống';
  };

  const getStatusIcon = (status) => {
    return status === 'occupied' ? <FaUsers /> : <FaCheckCircle />;
  };

  const getCaName = (ca) => {
    const caNames = {
      1: 'Ca 1 (07:00-09:25)',
      2: 'Ca 2 (09:35-12:00)',
      3: 'Ca 3 (12:30-14:55)',
      4: 'Ca 4 (15:05-17:30)',
      5: 'Ca 5 (18:00-20:30)'
    };
    return caNames[ca] || `Ca ${ca}`;
  };

  const groupedByBuilding = () => {
    const grouped = {};
    filteredRooms.forEach(room => {
      if (!grouped[room.toaNha]) {
        grouped[room.toaNha] = {};
      }
      if (!grouped[room.toaNha][room.tang]) {
        grouped[room.toaNha][room.tang] = [];
      }
      grouped[room.toaNha][room.tang].push(room);
    });
    return grouped;
  };

  return (
    <Container fluid className="py-4">
      <Row>
        <Col>
          <Card className="shadow-sm mb-4" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
            <Card.Header className="bg-primary text-white d-flex justify-content-between align-items-center" style={{ border: 'none' }}>
              <div className="d-flex align-items-center">
                <FaBuilding className="me-2" size={24} />
                <h3 className="mb-0">Phòng học</h3>
              </div>
              <div className="d-flex gap-2">
                <Button variant="light" onClick={handleExportExcel} className="shadow-sm">
                  <FaFileDownload className="me-2" />
                  Xuất Excel
                </Button>
                <Dropdown>
                  <Dropdown.Toggle variant="light" className="shadow-sm">
                    <FaFilter className="me-2" />
                    {viewMode === 'model' ? 'Mô hình' : viewMode === 'list' ? 'Danh sách' : 'Biểu đồ lịch'}
                  </Dropdown.Toggle>
                  <Dropdown.Menu>
                    <Dropdown.Item onClick={() => setViewMode('model')}>Xem lưới</Dropdown.Item>
                    <Dropdown.Item onClick={() => setViewMode('list')}>Xem danh sách</Dropdown.Item>
                    <Dropdown.Item onClick={() => setViewMode('schedule')}>Biểu đồ lịch</Dropdown.Item>
                  </Dropdown.Menu>
                </Dropdown>
              </div>
            </Card.Header>
            <Card.Body className="p-4">
              {loading && (
                <div className="text-center py-3">
                  <Spinner animation="border" variant="primary" />
                  <p className="mt-2">Đang tải dữ liệu...</p>
                </div>
              )}
              {/* Filters */}
              <Card className="mb-4 shadow-sm" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
                <Card.Body className="p-3">
                  <Row className="g-3">
                    <Col md={3}>
                      <Form.Group>
                        <Form.Label className="fw-semibold d-flex align-items-center">
                          <FaBuilding className="me-2 text-primary" />
                          Tòa nhà
                        </Form.Label>
                        <Form.Select
                          value={filterToaNha}
                          onChange={(e) => {
                            setFilterToaNha(e.target.value);
                            // Reset filter tầng khi thay đổi tòa nhà
                            setFilterTang('');
                          }}
                          className="shadow-sm"
                        >
                          <option value="">Tất cả</option>
                          {getUniqueToaNha().map(toa => (
                            <option key={toa} value={toa}>Tòa {toa}</option>
                          ))}
                        </Form.Select>
                      </Form.Group>
                    </Col>
                    <Col md={3}>
                      <Form.Group>
                        <Form.Label className="fw-semibold d-flex align-items-center">
                          <FaBuilding className="me-2 text-primary" />
                          Tầng
                        </Form.Label>
                        <Form.Select
                          value={filterTang}
                          onChange={(e) => setFilterTang(e.target.value)}
                          className="shadow-sm"
                        >
                          <option value="">Tất cả</option>
                          {getUniqueTang().map(tang => (
                            <option key={tang} value={tang}>Tầng {tang}</option>
                          ))}
                        </Form.Select>
                      </Form.Group>
                    </Col>
                    <Col md={3}>
                      <Form.Group>
                        <Form.Label className="fw-semibold d-flex align-items-center">
                          <FaCalendarCheck className="me-2 text-primary" />
                          Ngày
                        </Form.Label>
                        <Form.Control
                          type="date"
                          value={filterNgay}
                          onChange={(e) => setFilterNgay(e.target.value)}
                          className="shadow-sm"
                        />
                      </Form.Group>
                    </Col>
                    {viewMode !== 'schedule' && (
                      <Col md={3}>
                        <Form.Group>
                          <Form.Label className="fw-semibold d-flex align-items-center">
                            <FaClock className="me-2 text-primary" />
                            Ca học
                          </Form.Label>
                          <Form.Select
                            value={filterCa}
                            onChange={(e) => setFilterCa(e.target.value)}
                            className="shadow-sm"
                          >
                            <option value="">Tất cả</option>
                            <option value="1">Ca 1 (07:00-09:25)</option>
                            <option value="2">Ca 2 (09:35-12:00)</option>
                            <option value="3">Ca 3 (12:30-14:55)</option>
                            <option value="4">Ca 4 (15:05-17:30)</option>
                            <option value="5">Ca 5 (18:00-20:30)</option>
                          </Form.Select>
                        </Form.Group>
                      </Col>
                    )}
                  </Row>
                  <Row className="mt-2">
                    <Col>
                      <Button
                        variant="outline-secondary"
                        onClick={() => {
                          setFilterToaNha('');
                          setFilterTang('');
                          setFilterNgay(getTodayDate());
                          const currentCa = getCurrentCa();
                          setFilterCa(currentCa ? String(currentCa) : '');
                        }}
                        className="shadow-sm"
                      >
                        <FaSearch className="me-2" />
                        Xóa bộ lọc
                      </Button>
                      <Button
                        variant="primary"
                        onClick={loadRooms}
                        className="shadow-sm ms-2"
                        disabled={loading}
                      >
                        {loading ? <Spinner size="sm" className="me-2" /> : null}
                        Tải lại
                      </Button>
                    </Col>
                  </Row>
                </Card.Body>
              </Card>

              {/* View Mode: Model */}
              {viewMode === 'model' && (
                <div>
                  {Object.entries(groupedByBuilding()).map(([toaNha, floors]) => (
                    <Card key={toaNha} className="mb-4 shadow-sm">
                      <Card.Header className="bg-info text-white">
                        <h5 className="mb-0">Tòa {toaNha}</h5>
                      </Card.Header>
                      <Card.Body>
                        {Object.entries(floors).sort(([a], [b]) => a - b).map(([tang, floorRooms]) => (
                          <div key={`${toaNha}-${tang}`} className="mb-4">
                            <h6 className="mb-3 fw-bold">Tầng {tang}</h6>
                            <div className="d-flex flex-wrap gap-3">
                              {floorRooms.map(room => (
                                <Card
                                  key={room.maPhong}
                                  className="shadow-sm"
                                  style={{
                                    width: '200px',
                                    minHeight: '267px',
                                    cursor: 'pointer',
                                    border: `2px solid ${room.status === 'occupied' ? '#dc3545' : '#28a745'}`,
                                    transition: 'transform 0.2s'
                                  }}
                                  onClick={() => handleViewDetail(room)}
                                  onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.05)'}
                                  onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
                                >
                                  <Card.Body className="p-3">
                                    <div className="d-flex justify-content-between align-items-start mb-2">
                                      <Badge bg={getStatusColor(room.status)} className="d-flex align-items-center gap-1">
                                        {getStatusIcon(room.status)}
                                        {getStatusText(room.status)}
                                      </Badge>
                                    </div>
                                    <h6 className="fw-bold mb-1">{room.maPhong}</h6>
                                    <p className="text-muted small mb-2">{room.tenPhong}</p>
                                    {room.status === 'occupied' && (
                                      <div className="mt-2">
                                        <div className="small">
                                          <strong>Lớp:</strong> {room.currentClass}
                                        </div>
                                        <div className="small">
                                          <strong>Ca:</strong> {getCaName(room.currentCa)}
                                        </div>
                                        <div className="small">
                                          <strong>Điểm danh:</strong> {room.studentsAttended}/{room.totalStudents}
                                        </div>
                                      </div>
                                    )}
                                    <div className="mt-2">
                                      <small className="text-muted">
                                        Sức chứa: {room.sucChua} | {room.loaiPhong}
                                      </small>
                                    </div>
                                  </Card.Body>
                                </Card>
                              ))}
                            </div>
                          </div>
                        ))}
                      </Card.Body>
                    </Card>
                  ))}
                </div>
              )}

              {/* View Mode: List */}
              {viewMode === 'list' && (
                <div className="table-responsive">
                  <Table responsive striped hover className="mb-0" style={{ fontSize: '0.95rem' }}>
                    <thead className="table-primary">
                      <tr>
                        <th style={{ fontWeight: '600' }}>Mã phòng</th>
                        <th style={{ fontWeight: '600' }}>Tên phòng</th>
                        <th style={{ fontWeight: '600' }}>Tòa nhà</th>
                        <th style={{ fontWeight: '600' }}>Tầng</th>
                        <th style={{ fontWeight: '600' }}>Sức chứa</th>
                        <th style={{ fontWeight: '600' }}>Loại</th>
                        <th style={{ fontWeight: '600' }}>Trạng thái</th>
                        <th style={{ fontWeight: '600' }}>Lớp học hiện tại</th>
                        <th style={{ fontWeight: '600' }}>Điểm danh</th>
                        <th style={{ fontWeight: '600' }}>Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredRooms.map(room => (
                        <tr key={room.maPhong} style={{ verticalAlign: 'middle' }}>
                          <td>
                            <Badge bg="primary" style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                              {room.maPhong}
                            </Badge>
                          </td>
                          <td style={{ fontWeight: '500' }}>{room.tenPhong}</td>
                          <td>Tòa {room.toaNha}</td>
                          <td>Tầng {room.tang}</td>
                          <td>{room.sucChua}</td>
                          <td>{room.loaiPhong}</td>
                          <td>
                            <Badge bg={getStatusColor(room.status)} className="d-flex align-items-center gap-1" style={{ width: 'fit-content' }}>
                              {getStatusIcon(room.status)}
                              {getStatusText(room.status)}
                            </Badge>
                          </td>
                          <td>{room.currentClass || <span className="text-muted">-</span>}</td>
                          <td>
                            {room.status === 'occupied' ? (
                              <Badge bg="info">
                                {room.studentsAttended}/{room.totalStudents}
                              </Badge>
                            ) : (
                              <span className="text-muted">-</span>
                            )}
                          </td>
                          <td>
                            <Button
                              variant="outline-primary"
                              size="sm"
                              onClick={() => handleViewDetail(room)}
                              className="shadow-sm"
                            >
                              <FaEye className="me-1" />
                              Xem chi tiết
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              )}

              {/* View Mode: Schedule */}
              {viewMode === 'schedule' && (
                <div>
                  {scheduleLoading ? (
                    <div className="text-center py-5">
                      <Spinner animation="border" variant="primary" />
                      <p className="mt-2">Đang tải lịch sử dụng...</p>
                    </div>
                  ) : (
                    <>
                      {scheduleData.length > 0 ? (
                        <>
                          <Card className="mb-3 shadow-sm">
                            <Card.Header className="bg-info text-white">
                              <h5 className="mb-0">
                                {filterTang ? `Tầng ${filterTang}` : 'Tất cả các tầng'}: Biểu đồ Lịch sử dụng ({filterNgay ? new Date(filterNgay).toLocaleDateString('vi-VN') : new Date().toLocaleDateString('vi-VN')})
                              </h5>
                            </Card.Header>
                          </Card>
                          
                          <div className="table-responsive">
                            <Table bordered hover className="mb-0" style={{ fontSize: '0.9rem' }}>
                              <thead className="table-primary" style={{ position: 'sticky', top: 0, zIndex: 10 }}>
                                <tr>
                                  <th style={{ fontWeight: '600', minWidth: '120px', textAlign: 'center', verticalAlign: 'middle' }}>PHÒNG</th>
                                  <th style={{ fontWeight: '600', textAlign: 'center', verticalAlign: 'middle' }}>CA 1<br />(07:00 - 09:25)</th>
                                  <th style={{ fontWeight: '600', textAlign: 'center', verticalAlign: 'middle' }}>CA 2<br />(09:35 - 12:00)</th>
                                  <th style={{ fontWeight: '600', textAlign: 'center', verticalAlign: 'middle' }}>CA 3<br />(12:30 - 14:55)</th>
                                  <th style={{ fontWeight: '600', textAlign: 'center', verticalAlign: 'middle' }}>CA 4<br />(15:05 - 17:30)</th>
                                  <th style={{ fontWeight: '600', textAlign: 'center', verticalAlign: 'middle' }}>CA 5<br />(18:00 - 20:30)</th>
                                </tr>
                              </thead>
                              <tbody>
                                {scheduleData.map((room) => {
                                  // Kiểm tra xem phòng có trống cả ngày không
                                  // Phòng trống cả ngày khi ca đầu tiên có "TRỐNG CẢ NGÀY"
                                  const isEmptyAllDay = room.caSchedules && room.caSchedules.length > 0 &&
                                    room.caSchedules[0] && 
                                    room.caSchedules[0].tenLopHocPhan === 'TRỐNG CẢ NGÀY';
                                  
                                  return (
                                    <tr key={room.maPhong}>
                                      <td style={{ fontWeight: '600', textAlign: 'center', verticalAlign: 'middle', backgroundColor: '#f8f9fa' }}>
                                        <div>
                                          <Badge bg="primary" style={{ fontSize: '0.9rem', padding: '0.4rem 0.6rem' }}>
                                            {room.maPhong}
                                          </Badge>
                                        </div>
                                        <small className="text-muted d-block mt-1">{room.tenPhong}</small>
                                      </td>
                                      {room.caSchedules.map((ca, index) => {
                                        // Xử lý "TRỐNG CẢ NGÀY" trước - hiển thị với colSpan 5
                                        if (isEmptyAllDay) {
                                          if (index === 0 && ca.tenLopHocPhan === 'TRỐNG CẢ NGÀY') {
                                            // Hiển thị "TRỐNG CẢ NGÀY" ở ca đầu tiên với colSpan 5 (gộp 5 cột)
                                            return (
                                              <td
                                                key={index}
                                                colSpan={5}
                                                style={{
                                                  textAlign: 'center',
                                                  verticalAlign: 'middle',
                                                  minWidth: '150px',
                                                  padding: '1rem',
                                                  border: '1px solid #dee2e6',
                                                  backgroundColor: '#ffffff',
                                                  color: '#6c757d',
                                                  fontWeight: '500'
                                                }}
                                              >
                                                <div style={{ fontSize: '0.9rem' }}>
                                                  {ca.tenLopHocPhan}
                                                </div>
                                              </td>
                                            );
                                          } else if (index > 0) {
                                            // Bỏ qua các ca còn lại nếu đã hiển thị "TRỐNG CẢ NGÀY"
                                            return null;
                                          }
                                        }

                                        const getStatusStyle = () => {
                                          switch (ca.status) {
                                            case 'occupied':
                                              return { backgroundColor: '#dc3545', color: 'white', fontWeight: '500' };
                                            case 'upcoming':
                                              return { backgroundColor: '#fd7e14', color: 'white', fontWeight: '500' };
                                            case 'maintenance':
                                              return { backgroundColor: '#6c757d', color: 'white', fontWeight: '500' };
                                            case 'empty':
                                              if (ca.tenLopHocPhan === 'Trống') {
                                                return { backgroundColor: '#d4edda', color: '#155724', fontWeight: '500' };
                                              }
                                              return { backgroundColor: '#ffffff', color: '#6c757d' };
                                            default:
                                              return { backgroundColor: '#ffffff', color: '#6c757d' };
                                          }
                                        };

                                        const style = getStatusStyle();
                                        const isSpanning = ca.isSpanning && ca.tenLopHocPhan && ca.status !== 'empty';

                                        // Xử lý lớp học kéo dài nhiều ca
                                        if (isSpanning) {
                                          // Tìm ca đầu tiên của lớp học này
                                          const firstCaIndex = room.caSchedules.findIndex((c, idx) => 
                                            idx <= index && c.tenLopHocPhan === ca.tenLopHocPhan && c.isSpanning
                                          );
                                          
                                          // Nếu là ca đầu tiên, hiển thị với colSpan (gộp các cột)
                                          if (firstCaIndex === index) {
                                            return (
                                              <td
                                                key={index}
                                                colSpan={ca.spanCount}
                                                style={{
                                                  ...style,
                                                  textAlign: 'center',
                                                  verticalAlign: 'middle',
                                                  minWidth: '150px',
                                                  padding: '1rem',
                                                  border: '1px solid #dee2e6',
                                                  cursor: ca.status === 'occupied' || ca.status === 'upcoming' ? 'pointer' : 'default'
                                                }}
                                                onClick={() => {
                                                  if (ca.status === 'occupied' || ca.status === 'upcoming') {
                                                    handleViewDetail({ 
                                                      maPhong: room.maPhong,
                                                      ca: ca.ca,
                                                      ngay: filterNgay || new Date().toISOString().split('T')[0]
                                                    });
                                                  }
                                                }}
                                              >
                                                {ca.tenLopHocPhan && (
                                                  <div>
                                                    <div style={{ fontSize: '0.85rem', marginBottom: '0.25rem' }}>
                                                      {ca.tenLopHocPhan}
                                                    </div>
                                                    {ca.giangVien && (
                                                      <small style={{ fontSize: '0.75rem', opacity: 0.9 }}>
                                                        {ca.giangVien}
                                                      </small>
                                                    )}
                                                  </div>
                                                )}
                                              </td>
                                            );
                                          }
                                          
                                          // Nếu là ca tiếp theo của lớp học kéo dài, không hiển thị
                                          return null;
                                        }

                                        // Hiển thị ca bình thường
                                        return (
                                          <td
                                            key={index}
                                            style={{
                                              ...style,
                                              textAlign: 'center',
                                              verticalAlign: 'middle',
                                              minWidth: '150px',
                                              padding: '1rem',
                                              border: '1px solid #dee2e6',
                                              cursor: ca.status === 'occupied' || ca.status === 'upcoming' ? 'pointer' : 'default'
                                            }}
                                            onClick={() => {
                                              if (ca.status === 'occupied' || ca.status === 'upcoming') {
                                                handleViewDetail({ 
                                                  maPhong: room.maPhong,
                                                  ca: ca.ca,
                                                  ngay: filterNgay || new Date().toISOString().split('T')[0]
                                                });
                                              }
                                            }}
                                          >
                                            {ca.tenLopHocPhan ? (
                                              <div>
                                                <div style={{ fontSize: '0.85rem', marginBottom: '0.25rem' }}>
                                                  {ca.tenLopHocPhan}
                                                </div>
                                                {ca.giangVien && (
                                                  <small style={{ fontSize: '0.75rem', opacity: 0.9 }}>
                                                    {ca.giangVien}
                                                  </small>
                                                )}
                                              </div>
                                            ) : (
                                              <span style={{ fontSize: '0.85rem' }}>-</span>
                                            )}
                                          </td>
                                        );
                                      })}
                                    </tr>
                                  );
                                })}
                              </tbody>
                            </Table>
                          </div>

                          {/* Legend */}
                          <Card className="mt-3 shadow-sm">
                            <Card.Header className="bg-light">
                              <h6 className="mb-0">Chú thích Trạng thái</h6>
                            </Card.Header>
                            <Card.Body>
                              <Row className="g-3">
                                <Col md={3}>
                                  <div className="d-flex align-items-center">
                                    <div style={{ width: '20px', height: '20px', backgroundColor: '#dc3545', borderRadius: '50%', marginRight: '10px' }}></div>
                                    <span><strong>Đang Dùng</strong> (Busy)</span>
                                  </div>
                                </Col>
                                <Col md={3}>
                                  <div className="d-flex align-items-center">
                                    <div style={{ width: '20px', height: '20px', backgroundColor: '#d4edda', borderRadius: '50%', marginRight: '10px', border: '1px solid #155724' }}></div>
                                    <span><strong>Trống</strong> (Available)</span>
                                  </div>
                                </Col>
                                <Col md={3}>
                                  <div className="d-flex align-items-center">
                                    <div style={{ width: '20px', height: '20px', backgroundColor: '#fd7e14', borderRadius: '50%', marginRight: '10px' }}></div>
                                    <span><strong>Sắp Tới</strong> (Upcoming - Bắt đầu trong 30p)</span>
                                  </div>
                                </Col>
                                <Col md={3}>
                                  <div className="d-flex align-items-center">
                                    <div style={{ width: '20px', height: '20px', backgroundColor: '#6c757d', borderRadius: '50%', marginRight: '10px' }}></div>
                                    <span><strong>Bảo Trì/Khóa</strong> (Maintenance/Locked)</span>
                                  </div>
                                </Col>
                              </Row>
                            </Card.Body>
                          </Card>
                        </>
                      ) : (
                        <div className="text-center py-5">
                          <FaBuilding size={64} className="text-muted mb-3" />
                          <Alert variant="info" className="d-inline-block">
                            Không có dữ liệu lịch sử dụng cho tầng này.
                          </Alert>
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}

              {viewMode !== 'schedule' && filteredRooms.length === 0 && (
                <div className="text-center py-5">
                  <FaBuilding size={64} className="text-muted mb-3" />
                  <Alert variant="info" className="d-inline-block">
                    Không tìm thấy phòng học nào.
                  </Alert>
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Modal Chi tiết phòng học */}
      <Modal show={showDetailModal} onHide={() => setShowDetailModal(false)} size="xl">
        <Modal.Header closeButton className="bg-primary text-white">
          <Modal.Title className="d-flex align-items-center">
            <FaDoorOpen className="me-2" />
            Chi tiết phòng học: {selectedRoom?.maPhong}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body className="p-4">
          {roomDetail && (
            <>
              {/* Thông tin phòng học */}
              <Card className="mb-4 shadow-sm">
                <Card.Header className="bg-light">
                  <h6 className="mb-0">Thông tin phòng học</h6>
                </Card.Header>
                <Card.Body>
                  <Row>
                    <Col md={6}>
                      <p><strong>Mã phòng:</strong> {roomDetail.maPhong}</p>
                      <p><strong>Tên phòng:</strong> {roomDetail.tenPhong}</p>
                      <p><strong>Tòa nhà:</strong> Tòa {roomDetail.toaNha}</p>
                      <p><strong>Tầng:</strong> Tầng {roomDetail.tang}</p>
                    </Col>
                    <Col md={6}>
                      <p><strong>Sức chứa:</strong> {roomDetail.sucChua} người</p>
                      <p><strong>Loại phòng:</strong> {roomDetail.loaiPhong}</p>
                      <p><strong>Trạng thái:</strong>
                        <Badge bg={getStatusColor(roomDetail.status)} className="ms-2">
                          {getStatusText(roomDetail.status)}
                        </Badge>
                      </p>
                    </Col>
                  </Row>
                </Card.Body>
              </Card>

              {/* Thông tin lớp học hiện tại */}
              {roomDetail.classInfo ? (
                <>
                  <Card className="mb-4 shadow-sm border-success">
                    <Card.Header className="bg-success text-white">
                      <h6 className="mb-0 d-flex align-items-center">
                        <FaCalendarCheck className="me-2" />
                        Lớp học đang diễn ra
                      </h6>
                    </Card.Header>
                    <Card.Body>
                      <Row>
                        <Col md={6}>
                          <p><strong>Tên lớp học phần:</strong> {roomDetail.classInfo.tenLopHocPhan}</p>
                          <p><strong>Giảng viên:</strong> {roomDetail.classInfo.giangVien}</p>
                          <p><strong>Ca học:</strong> {getCaName(roomDetail.classInfo.ca)}</p>
                          <p><strong>Ngày học:</strong> {roomDetail.classInfo.ngayHoc}</p>
                        </Col>
                        <Col md={6}>
                          <p><strong>Thời gian:</strong> {roomDetail.classInfo.thoiGianBatDau} - {roomDetail.classInfo.thoiGianKetThuc}</p>
                          <p><strong>Tổng số sinh viên:</strong> {roomDetail.classInfo.soSinhVien}</p>
                          <p><strong>Đã điểm danh:</strong>
                            <Badge bg="success" className="ms-2">
                              {roomDetail.classInfo.soSinhVienDaDiemDanh}/{roomDetail.classInfo.soSinhVien}
                            </Badge>
                          </p>
                          <div className="mt-2">
                            <div className="progress" style={{ height: '20px' }}>
                              <div
                                className="progress-bar bg-success"
                                role="progressbar"
                                style={{ width: `${(roomDetail.classInfo.soSinhVienDaDiemDanh / roomDetail.classInfo.soSinhVien) * 100}%` }}
                              >
                                {Math.round((roomDetail.classInfo.soSinhVienDaDiemDanh / roomDetail.classInfo.soSinhVien) * 100 || 0)}%
                              </div>
                            </div>
                          </div>
                        </Col>
                      </Row>
                    </Card.Body>
                  </Card>

                  {/* Danh sách điểm danh */}
                  <Card className="shadow-sm">
                    <Card.Header className="bg-info text-white">
                      <h6 className="mb-0">Danh sách điểm danh</h6>
                    </Card.Header>
                    <Card.Body>
                      <div className="table-responsive" style={{ maxHeight: '400px', overflowY: 'auto' }}>
                        <Table striped hover size="sm">
                          <thead className="table-info sticky-top">
                            <tr>
                              <th>STT</th>
                              <th>Mã sinh viên</th>
                              <th>Tên sinh viên</th>
                              <th>Giờ vào</th>
                              <th>Trạng thái</th>
                            </tr>
                          </thead>
                          <tbody>
                            {roomDetail.attendanceList.map((att, index) => (
                              <tr key={index}>
                                <td>{index + 1}</td>
                                <td>
                                  <Badge bg="secondary">{att.maSinhVien}</Badge>
                                </td>
                                <td>{att.tenSinhVien}</td>
                                <td>{att.gioVao}</td>
                                <td>
                                  <Badge bg={att.trangThai === 'Đúng giờ' ? 'success' : 'warning'}>
                                    {att.trangThai}
                                  </Badge>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </Table>
                      </div>
                    </Card.Body>
                  </Card>
                </>
              ) : (
                <Alert variant="info">
                  <FaCheckCircle className="me-2" />
                  Phòng học hiện đang trống, không có lớp học nào đang diễn ra.
                </Alert>
              )}
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowDetailModal(false)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
};

export default Room;

