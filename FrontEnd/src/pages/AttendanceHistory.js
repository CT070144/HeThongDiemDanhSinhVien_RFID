import React, { useState, useEffect, useCallback } from 'react';
import { Container, Row, Col, Card, Table, Form, Button, Alert, Badge } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { attendanceAPI } from '../services/api';

const AttendanceHistory = () => {
  const [attendance, setAttendance] = useState([]);
  const [filteredAttendance, setFilteredAttendance] = useState([]);
  const [filters, setFilters] = useState({
    ngay: '',
    ca: '',
    maSinhVien: ''
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

    setFilteredAttendance(filtered);
  }, [attendance, filters]);

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
      maSinhVien: ''
    });
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
            <Card.Header>
              <h3>Lịch sử điểm danh</h3>
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
                              {filteredAttendance.filter(r => r.trangThai === 'dang_hoc').length}
                            </h4>
                            <p>Đang học</p>
                          </div>
                        </Col>
                        <Col md={3}>
                          <div className="text-center">
                            <h4 className="text-warning">
                              {filteredAttendance.filter(r => r.trangThai === 'muon').length}
                            </h4>
                            <p>Điểm danh muộn</p>
                          </div>
                        </Col>
                        <Col md={3}>
                          <div className="text-center">
                            <h4 className="text-secondary">
                              {filteredAttendance.filter(r => r.trangThai === 'da_ra_ve').length}
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
