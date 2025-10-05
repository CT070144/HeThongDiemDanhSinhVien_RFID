import React, { useState, useEffect, useCallback } from 'react';
import { Container, Row, Col, Card, Table, Button, Alert, Badge } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { studentAPI, attendanceAPI, deviceAPI } from '../services/api';

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalStudents: 0,
    todayAttendance: 0,
    unprocessedRfids: 0,
    currentCa: 1
  });
  const [todayAttendance, setTodayAttendance] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(5);
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(false);

  const loadDashboardData = useCallback(async () => {
    try {
      setLoading(true);
      
      // Load students count
      const studentsResponse = await studentAPI.getAll();
      const totalStudents = studentsResponse.data.length;

      // Load today's attendance
      const attendanceResponse = await attendanceAPI.getToday();
      const todayAttendance = attendanceResponse.data;

      // Load unprocessed RFIDs
      const rfidsResponse = await attendanceAPI.getUnprocessedRfids();
      const unprocessedRfids = rfidsResponse.data.filter(rfid => !rfid.processed).length;

      // Determine current ca
      const currentCa = getCurrentCa();

      setStats({
        totalStudents,
        todayAttendance: todayAttendance.length,
        unprocessedRfids,
        currentCa
      });

      setTodayAttendance(todayAttendance);
      setPage(1);
      const devicesRes = await deviceAPI.getAll();
      setDevices(devicesRes.data || []);
    } catch (error) {
      toast.error('Lỗi khi tải dữ liệu dashboard');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboardData();
    // Refresh data every 30 seconds
    const interval = setInterval(loadDashboardData, 30000);
    return () => clearInterval(interval);
  }, [loadDashboardData]);


  const getCurrentCa = () => {
    const now = new Date();
    const hour = now.getHours();
    const minute = now.getMinutes();
    const currentTime = hour * 60 + minute;

    if (currentTime >= 420 && currentTime < 570) return 1; // 07:00 - 09:30
    if (currentTime >= 570 && currentTime < 720) return 2; // 09:30 - 12:00
    if (currentTime >= 750 && currentTime < 900) return 3; // 12:30 - 15:00
    if (currentTime >= 900 && currentTime < 1050) return 4; // 15:00 - 17:30
    return 0; // Ngoài giờ học
  };

  const getCaName = (ca) => {
    const caMap = {
      1: 'Ca 1 (07:00-09:30)',
      2: 'Ca 2 (09:30-12:00)',
      3: 'Ca 3 (12:30-15:00)',
      4: 'Ca 4 (15:00-17:30)'
    };
    return caMap[ca] || 'Ngoài giờ học';
  };

  const getStatusBadge = (trangThai) => {
    const statusMap = {
      'muon': { variant: 'warning', text: 'Muộn' },
      'dang_hoc': { variant: 'success', text: 'Đang học' },
      'da_ra_ve': { variant: 'secondary', text: 'Đã ra về' }
    };
    
    const status = statusMap[trangThai] || { variant: 'light', text: trangThai };
    return <Badge bg={status.variant}>{status.text}</Badge>;
  };

  return (
    <Container>
      <Row>
        <Col>
          <h2 className="mb-4">Dashboard - Hệ thống điểm danh RFID</h2>
          
          {/* Thống kê tổng quan */}
          <Row className="mb-4">
            <Col md={3}>
              <Card className="text-center">
                <Card.Body>
                  <h3 className="text-primary">{stats.totalStudents}</h3>
                  <p className="mb-0">Tổng số sinh viên</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center">
                <Card.Body>
                  <h3 className="text-success">{stats.todayAttendance}</h3>
                  <p className="mb-0">Điểm danh hôm nay</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center">
                <Card.Body>
                  <h3 className="text-warning">{stats.unprocessedRfids}</h3>
                  <p className="mb-0">RFID chưa xử lý</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center">
                <Card.Body>
                  <h3 className="text-info">{getCaName(stats.currentCa)}</h3>
                  <p className="mb-0">Ca hiện tại</p>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          {/* Điểm danh hôm nay */}
          <Row>
            <Col>
              <Card>
                <Card.Header className="d-flex justify-content-between align-items-center">
                  <h4>Điểm danh hôm nay</h4>
                  <Button variant="outline-primary" onClick={loadDashboardData} disabled={loading}>
                    {loading ? 'Đang tải...' : 'Làm mới'}
                  </Button>
                </Card.Header>
                <Card.Body>
                  {todayAttendance.length > 0 ? (
                    <Table responsive striped bordered hover>
                      <thead>
                        <tr>
                          <th>RFID</th>
                          <th>Mã sinh viên</th>
                          <th>Tên sinh viên</th>
                          <th>Ca</th>
                          <th>Giờ vào</th>
                          <th>Giờ ra</th>
                          <th>Trạng thái</th>
                        </tr>
                      </thead>
                      <tbody>
                        {todayAttendance
                          .slice((page - 1) * pageSize, page * pageSize)
                          .map((record) => (
                          <tr key={record.id}>
                            <td>{record.rfid}</td>
                            <td>{record.maSinhVien}</td>
                            <td>{record.tenSinhVien}</td>
                            <td>{getCaName(record.ca)}</td>
                            <td>{record.gioVao || '-'}</td>
                            <td>{record.gioRa || '-'}</td>
                            <td>{getStatusBadge(record.trangThai)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  ) : (
                    <Alert variant="info">
                      Chưa có điểm danh nào hôm nay.
                    </Alert>
                  )}
                  {todayAttendance.length > 0 && (
                    <div className="d-flex justify-content-between align-items-center mt-3">
                      <div>Trang {page}</div>
                      <div className="d-flex gap-2">
                        <Button variant="outline-secondary" disabled={page === 1} onClick={() => setPage((p) => Math.max(1, p - 1))}>
                          Trước
                        </Button>
                        <Button
                          variant="outline-secondary"
                          disabled={todayAttendance.length <= page * pageSize}
                          onClick={() => setPage((p) => p + 1)}
                        >
                          Sau
                        </Button>
                      </div>
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Col>
          </Row>

          {/* Hướng dẫn sử dụng */}
          <Row className="mt-4">
            <Col>
              <Card>
                <Card.Header>
                  <h4>Hướng dẫn sử dụng hệ thống</h4>
                </Card.Header>
                <Card.Body>
                  <Row>
                    <Col md={6}>
                      <h5>Quản lý sinh viên:</h5>
                      <ul>
                        <li>Thêm, sửa, xóa thông tin sinh viên</li>
                        <li>Tìm kiếm sinh viên theo mã hoặc tên</li>
                        <li>Mỗi sinh viên có RFID duy nhất</li>
                      </ul>
                    </Col>
                    <Col md={6}>
                      <h5>Điểm danh:</h5>
                      <ul>
                        <li>ESP32 tự động đọc thẻ RFID</li>
                        <li>Hệ thống tự động xác định ca học</li>
                        <li>Theo dõi trạng thái: vào, ra, muộn</li>
                      </ul>
                    </Col>
                  </Row>
                  <Row className="mt-3">
                    <Col md={12}>
                      <Alert variant="success">
                        <strong>Lưu ý:</strong> Hệ thống tự động cập nhật dữ liệu real-time. 
                        Khi có sinh viên điểm danh, thông tin sẽ hiển thị ngay lập tức trên dashboard.
                      </Alert>
                    </Col>
                  </Row>
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </Col>
      </Row>
    </Container>
  );
};

export default Dashboard;
