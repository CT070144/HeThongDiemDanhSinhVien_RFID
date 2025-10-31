import React, { useState, useEffect, useCallback,useRef } from 'react';
import { Container, Row, Col, Card, Table, Button, Alert, Badge } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { studentAPI, attendanceAPI, deviceAPI } from '../services/api';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
  PointElement,
  LineElement,
} from 'chart.js';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
import io  from "socket.io-client";
import { formatTime } from '../services/format-time';

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
  PointElement,
  LineElement
);

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
  const socketRef = useRef(null);
  const [chartData, setChartData] = useState({
    attendanceByCa: {},
    attendanceByHour: [],
    attendanceStatus: {},
    weeklyAttendance: []
  });

  const loadDashboardData = useCallback(async () => {
    try {
      setLoading(true);
      
      // Load students count
      const studentsResponse = await studentAPI.getAll();
      const totalStudents = studentsResponse.data.length;

      // Load today's attendance
      const attendanceResponse = await attendanceAPI.getToday();
      const todayAttendance = attendanceResponse.data;
      todayAttendance.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

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

      // Calculate chart data
      calculateChartData(todayAttendance);
    } catch (error) {
      toast.error('Lỗi khi tải dữ liệu dashboard');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboardData();
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
       loadDashboardData();
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

  const calculateChartData = (attendanceData) => {
    // Calculate attendance by ca
    const attendanceByCa = {};
    const attendanceByHour = Array(24).fill(0);
    const attendanceStatus = {
      'Đúng giờ': 0,
      'Muộn': 0,
      'Đang học': 0,
      'Đã ra về': 0,
      'Ra về sớm': 0,
      'Không điểm danh ra': 0
    };

    attendanceData.forEach(record => {
      // Count by ca
      const ca = record.ca || 0;
      attendanceByCa[ca] = (attendanceByCa[ca] || 0) + 1;

      // Count by hour
      if (record.gioVao) {
        const hour = parseInt(record.gioVao.split(':')[0]);
        attendanceByHour[hour]++;
      }

      // Count by status
      if (record.tinhTrangDiemDanh === 'DUNG_GIO' || record.tinhTrangDiemDanh === 'dung_gio') {
        attendanceStatus['Đúng giờ']++;
      } else if (record.tinhTrangDiemDanh === 'MUON' || record.tinhTrangDiemDanh === 'muon') {
        attendanceStatus['Muộn']++;
      }

      if (record.trangThai === 'DANG_HOC' || record.trangThai === 'dang_hoc') {
        attendanceStatus['Đang học']++;
      } else if (record.trangThai === 'DA_RA_VE' || record.trangThai === 'da_ra_ve') {
        attendanceStatus['Đã ra về']++;
      } else if (record.trangThai === 'RA_VE_SOM' || record.trangThai === 'ra_ve_som') {
        attendanceStatus['Ra về sớm']++;
      } else if (record.trangThai === 'KHONG_DIEM_DANH_RA' || record.trangThai === 'khong_diem_danh_ra') {
        attendanceStatus['Không điểm danh ra']++;
      }
    });

    setChartData({
      attendanceByCa,
      attendanceByHour,
      attendanceStatus,
      weeklyAttendance: [] // TODO: Implement weekly data
    });
  };

  const getStatusBadge = (trangThai) => {
    const statusMap = {
      'MUON': { variant: 'warning', text: 'Muộn' },
      'muon': { variant: 'warning', text: 'Muộn' },
      'DUNG_GIO': { variant: 'success', text: 'Đúng giờ' },
      'dung_gio': { variant: 'success', text: 'Đúng giờ' },
      'DANG_HOC': { variant: 'primary', text: 'Đang học' },
      'dang_hoc': { variant: 'primary', text: 'Đang học' },
      'DA_RA_VE': { variant: 'success', text: 'Đã ra về' },
      'da_ra_ve': { variant: 'success', text: 'Đã ra về' },
      'RA_VE_SOM': { variant: 'warning', text: 'Ra về sớm' },
      'ra_ve_som': { variant: 'warning', text: 'Ra về sớm' },
      'KHONG_DIEM_DANH_RA': { variant: 'danger', text: 'Không điểm danh ra' },
      'khong_diem_danh_ra': { variant: 'danger', text: 'Không điểm danh ra' }
    };
    
    const status = statusMap[trangThai] || { variant: 'light', text: trangThai };
    return <Badge bg={status.variant}>{status.text}</Badge>;
  };

  // Chart configurations
  const attendanceByCaChartData = {
    labels: ['Ca 1', 'Ca 2', 'Ca 3', 'Ca 4', 'Ca 5'],
    datasets: [
      {
        label: 'Số lượng điểm danh',
        data: [
          chartData.attendanceByCa[1] || 0,
          chartData.attendanceByCa[2] || 0,
          chartData.attendanceByCa[3] || 0,
          chartData.attendanceByCa[4] || 0,
          chartData.attendanceByCa[5] || 0
        ],
        backgroundColor: [
          'rgba(54, 162, 235, 0.8)',
          'rgba(255, 99, 132, 0.8)',
          'rgba(255, 206, 86, 0.8)',
          'rgba(75, 192, 192, 0.8)',
          'rgba(153, 102, 255, 0.8)'
        ],
        borderColor: [
          'rgba(54, 162, 235, 1)',
          'rgba(255, 99, 132, 1)',
          'rgba(255, 206, 86, 1)',
          'rgba(75, 192, 192, 1)',
          'rgba(153, 102, 255, 1)'
        ],
        borderWidth: 2
      }
    ]
  };

  const attendanceByHourChartData = {
    labels: Array.from({length: 24}, (_, i) => `${i}:00`),
    datasets: [
      {
        label: 'Số lượng điểm danh',
        data: chartData.attendanceByHour,
        borderColor: 'rgba(75, 192, 192, 1)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.4,
        fill: true
      }
    ]
  };

  const attendanceStatusChartData = {
    labels: Object.keys(chartData.attendanceStatus),
    datasets: [
      {
        data: Object.values(chartData.attendanceStatus),
        backgroundColor: [
          '#28a745', // Đúng giờ - xanh lá
          '#ffc107', // Muộn - vàng
          '#609d25', // Đang học - xanh dương
          '#007bff', // Đã ra về - xanh lá
          '#fd7e14', // Ra về sớm - cam
          '#dc3545'  // Không điểm danh ra - đỏ
        ],
        borderColor: [
          '#28a745',
          '#ffc107',
          '#609d25',
          '#007bff',
          '#fd7e14',
          '#dc3545'
        ],
        borderWidth: 2
      }
    ]
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: true,
        font: {
          size: 16,
          weight: 'bold'
        }
      }
    }
  };

  const doughnutOptions = {
    ...chartOptions,
    plugins: {
      ...chartOptions.plugins,
      legend: {
        position: 'bottom',
      }
    }
  };

  return (
    <Container>
      <Row>
        <Col>
          <h2 style={{marginTop: '10px'}} className="mb-4">Dashboard - Hệ thống điểm danh RFID</h2>
          
          {/* Thống kê tổng quan */}
          <Row className="mb-4">
            <Col md={3}>
              <Card className="text-center h-100">
                <Card.Body className="d-flex flex-column justify-content-center">
                  <div className="mb-2">
                    <i className="fas fa-users fa-3x text-primary"></i>
                  </div>
                  <h2 className="text-primary mb-1">{stats.totalStudents}</h2>
                  <p className="mb-0 text-muted">Tổng số sinh viên</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center h-100">
                <Card.Body className="d-flex flex-column justify-content-center">
                  <div className="mb-2">
                    <i className="fas fa-calendar-check fa-3x text-success"></i>
                  </div>
                  <h2 className="text-success mb-1">{stats.todayAttendance}</h2>
                  <p className="mb-0 text-muted">Điểm danh hôm nay</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center h-100">
                <Card.Body className="d-flex flex-column justify-content-center">
                  <div className="mb-2">
                    <i className="fas fa-exclamation-triangle fa-3x text-warning"></i>
                  </div>
                  <h2 className="text-warning mb-1">{stats.unprocessedRfids}</h2>
                  <p className="mb-0 text-muted">RFID chưa xử lý</p>
                </Card.Body>
              </Card>
            </Col>
            <Col md={3}>
              <Card className="text-center h-100">
                <Card.Body className="d-flex flex-column justify-content-center">
                  <div className="mb-2">
                    <i className="fas fa-clock fa-3x text-info"></i>
                  </div>
                  <h5 className="text-info mb-1">{getCaName(stats.currentCa)}</h5>
                  <p className="mb-0 text-muted">Ca hiện tại</p>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          {/* Biểu đồ thống kê */}
          <Row className="mb-4">
            <Col md={6}>
              <Card className="h-100">
                <Card.Header>
                  <h5 className="mb-0">
                    <i className="fas fa-chart-bar me-2 text-primary"></i>
                    Điểm danh theo ca học
                  </h5>
                </Card.Header>
                <Card.Body>
                  <div style={{ height: '300px' }}>
                    <Bar 
                      data={attendanceByCaChartData} 
                      options={{
                        ...chartOptions,
                        plugins: {
                          ...chartOptions.plugins,
                          title: {
                            ...chartOptions.plugins.title,
                            text: 'Thống kê điểm danh theo từng ca học'
                          }
                        }
                      }} 
                    />
                  </div>
                </Card.Body>
              </Card>
            </Col>
            <Col md={6}>
              <Card className="h-100">
                <Card.Header>
                  <h5 className="mb-0">
                    <i className="fas fa-chart-pie me-2 text-success"></i>
                    Trạng thái điểm danh
                  </h5>
                </Card.Header>
                <Card.Body>
                  <div style={{ height: '300px' }}>
                    <Doughnut 
                      data={attendanceStatusChartData} 
                      options={{
                        ...doughnutOptions,
                        plugins: {
                          ...doughnutOptions.plugins,
                          title: {
                            ...doughnutOptions.plugins.title,
                            text: 'Phân bố trạng thái điểm danh'
                          }
                        }
                      }} 
                    />
                  </div>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          {/* Biểu đồ theo giờ */}
          <Row className="mb-4">
            <Col md={12}>
              <Card>
                <Card.Header>
                  <h5 className="mb-0">
                    <i className="fas fa-chart-line me-2 text-info"></i>
                    Điểm danh theo giờ trong ngày
                  </h5>
                </Card.Header>
                <Card.Body>
                  <div style={{ height: '400px' }}>
                    <Line 
                      data={attendanceByHourChartData} 
                      options={{
                        ...chartOptions,
                        plugins: {
                          ...chartOptions.plugins,
                          title: {
                            ...chartOptions.plugins.title,
                            text: 'Xu hướng điểm danh theo từng giờ'
                          }
                        },
                        scales: {
                          x: {
                            title: {
                              display: true,
                              text: 'Giờ trong ngày'
                            }
                          },
                          y: {
                            title: {
                              display: true,
                              text: 'Số lượng điểm danh'
                            },
                            beginAtZero: true
                          }
                        }
                      }} 
                    />
                  </div>
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
                            <td>{formatTime(record.gioVao)}</td>
                            <td>{formatTime(record.gioRa)}</td>
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
