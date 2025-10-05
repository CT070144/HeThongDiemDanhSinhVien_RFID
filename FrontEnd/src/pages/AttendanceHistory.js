import React, { useState, useEffect, useCallback } from 'react';
import { Container, Row, Col, Card, Table, Form, Button, Alert, Badge } from 'react-bootstrap';
import * as XLSX from 'xlsx';
import { toast } from 'react-toastify';
import { attendanceAPI } from '../services/api';

const AttendanceHistory = () => {
  const [attendance, setAttendance] = useState([]);
  const [filteredAttendance, setFilteredAttendance] = useState([]);
  const [allFilteredAttendance, setAllFilteredAttendance] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [filters, setFilters] = useState({
    ngay: '',
    ca: '',
    maSinhVien: '',
    phongHoc: ''
  });

  useEffect(() => {
    loadAttendance();
    let isFetching = false;
    const intervalId = setInterval(async () => {
      if (isFetching) return;
      isFetching = true;
      try {
        await loadAttendance();
      } catch (e) {
        // tránh spam toast khi polling
        // console.error(e);
      } finally {
        isFetching = false;
      }
    }, 1000);
    return () => clearInterval(intervalId);
  }, []);

  const filterAttendance = useCallback(() => {
    let filtered = [...attendance];

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

    const start = (page - 1) * pageSize;
    const end = start + pageSize;
    setAllFilteredAttendance(filtered);
    setFilteredAttendance(filtered.slice(start, end));
  }, [attendance, filters, page, pageSize]);

  useEffect(() => {
    filterAttendance();
  }, [filterAttendance]);

  const loadAttendance = async () => {
    try {
      const response = await attendanceAPI.getAll();
      setAttendance(response.data);
    } catch (error) {
      // tránh spam toast do polling liên tục
      // toast.error('Lỗi khi tải lịch sử điểm danh');
    }
  console.log(allFilteredAttendance);
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
      phongHoc: ''
    });
  };

  const getStatusBadge = (trangThai) => {
    const statusMap = {
      'MUON': { variant: 'warning', text: 'Muộn' },
      'DANG_HOC': { variant: 'success', text: 'Đúng giờ' },
      'DA_RA_VE': { variant: 'secondary', text: 'Đã ra về' }
    };
    const status = statusMap[trangThai] || { variant: 'light', text: trangThai };
    return <Badge bg={status.variant}>{status.text}</Badge>;
  };

  const exportCsv = () => {
    const data = (allFilteredAttendance.length ? allFilteredAttendance : attendance).map(r => ({
      RFID: r.rfid,
      MaSinhVien: r.maSinhVien,
      TenSinhVien: r.tenSinhVien,
      PhongHoc: r.phongHoc || '',
      Ngay: r.ngay,
      Ca: r.ca,
      GioVao: r.gioVao || '',
      GioRa: r.gioRa || '',
      ThờiGianTạo: r.createdAt
    }));
    const ws = XLSX.utils.json_to_sheet(data);
    const csv = XLSX.utils.sheet_to_csv(ws);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'attendance.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const getCaName = (ca) => {
    const caMap = {
      1: 'Ca 1 (07:00-09:30)',
      2: 'Ca 2 (09:30-12:00)',
      3: 'Ca 3 (12:30-15:00)',
      4: 'Ca 4 (15:00-17:30)'
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
              <Button variant="outline-success" onClick={exportCsv}>Xuất CSV</Button>
            </Card.Header>
            <Card.Body>
              {/* Bộ lọc */}
              <Row className="mb-3">
                <Col md={3}>
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
                <Col md={3}>
                  <Form.Group>
                    <Form.Label>Ca học</Form.Label>
                    <Form.Select
                      name="ca"
                      value={filters.ca}
                      onChange={handleFilterChange}
                    >
                      <option value="">Tất cả ca</option>
                      <option value="1">Ca 1 (07:00-09:30)</option>
                      <option value="2">Ca 2 (09:30-12:00)</option>
                      <option value="3">Ca 3 (12:30-15:00)</option>
                      <option value="4">Ca 4 (15:00-17:30)</option>
                    </Form.Select>
                  </Form.Group>
                </Col>
                <Col md={3}>
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
                <Col md={3}>
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
                <Col md={3} className="d-flex align-items-end">
                  <Button variant="outline-secondary" onClick={clearFilters}>
                    Xóa bộ lọc
                  </Button>
                </Col>
              </Row>

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
                    <th>Trạng thái</th>
                    <th>Thời gian tạo</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAttendance.map((record) => (
                    <tr key={record.id}>
                      <td>{record.rfid}</td>
                      <td>{record.maSinhVien}</td>
                      <td>{record.tenSinhVien}</td>
                      <td>{record.phongHoc || '-'}</td>
                      <td>{new Date(record.ngay).toLocaleDateString('vi-VN')}</td>
                      <td>{getCaName(record.ca)}</td>
                      <td>{record.gioVao || '-'}</td>
                      <td>{record.gioRa || '-'}</td>
                      <td>{getStatusBadge(record.trangThai)}</td>
                      <td>{new Date(record.createdAt).toLocaleString('vi-VN')}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>

              {filteredAttendance.length === 0 && (
                <Alert variant="info">
                  Không có dữ liệu điểm danh nào được tìm thấy.
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
                        <Col md={3}>
                          <div className="text-center">
                            <h4 className="text-primary">
                              {filteredAttendance.length}
                            </h4>
                            <p>Tổng số bản ghi</p>
                          </div>
                        </Col>
                        <Col md={3}>
                          <div className="text-center">
                            <h4 className="text-success">
                              {filteredAttendance.filter(r => r.trangThai === 'DANG_HOC').length}
                            </h4>
                            <p>Đang học</p>
                          </div>
                        </Col>
                        <Col md={3}>
                          <div className="text-center">
                            <h4 className="text-warning">
                              {filteredAttendance.filter(r => r.trangThai === 'MUON').length}
                            </h4>
                            <p>Điểm danh muộn</p>
                          </div>
                        </Col>
                        <Col md={3}>
                          <div className="text-center">
                            <h4 className="text-secondary">
                              {filteredAttendance.filter(r => r.trangThai === 'DA_RA_VE').length}
                            </h4>
                            <p>Đã ra về</p>
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
