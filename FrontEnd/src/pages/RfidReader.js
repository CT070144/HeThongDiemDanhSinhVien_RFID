import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Alert, Badge } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { attendanceAPI } from '../services/api';

const RfidReader = () => {
  const [unprocessedRfids, setUnprocessedRfids] = useState([]);

  useEffect(() => {
    loadUnprocessedRfids();
    let isFetching = false;
    const intervalId = setInterval(async () => {
      if (isFetching) return;
      isFetching = true;
      try {
        await loadUnprocessedRfids();
      } catch (e) {
        // tránh spam toast khi polling
      } finally {
        isFetching = false;
      }
    }, 1000);
    return () => clearInterval(intervalId);
  }, []);

  const loadUnprocessedRfids = async () => {
    try {
      const response = await attendanceAPI.getUnprocessedRfids();
      setUnprocessedRfids(response.data);
    } catch (error) {
      toast.error('Lỗi khi tải danh sách RFID chưa xử lý');
    }
  };

  const handleMarkProcessed = async (id) => {
    try {
      await attendanceAPI.markProcessed(id);
      toast.success('Đã đánh dấu RFID đã xử lý');
      loadUnprocessedRfids();
    } catch (error) {
      toast.error('Có lỗi xảy ra khi đánh dấu RFID');
    }
  };

  const refreshData = () => {
    loadUnprocessedRfids();
  };

  return (
    <Container>
      <Row>
        <Col>
          <Card>
            <Card.Header className="d-flex justify-content-between align-items-center">
              <h3>Trang đọc RFID</h3>
              <Button variant="primary" onClick={refreshData}>
                Làm mới
              </Button>
            </Card.Header>
            <Card.Body>
              <Alert variant="info">
                <h5>Hướng dẫn sử dụng:</h5>
                <ul>
                  <li>Khi ESP32 đọc được thẻ RFID chưa được đăng ký, thông tin sẽ hiển thị ở bảng bên dưới</li>
                  <li>Quản trị viên có thể thêm sinh viên mới với RFID này từ trang "Quản lý sinh viên"</li>
                  <li>Sau khi thêm sinh viên thành công, nhấn "Đã xử lý" để đánh dấu RFID đã được xử lý</li>
                </ul>
              </Alert>

              <Table responsive striped bordered hover>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>RFID</th>
                    <th>Mã sinh viên</th>
                    <th>Tên sinh viên</th>
                    <th>Thời gian đọc</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {unprocessedRfids.map((rfid) => (
                    <tr key={rfid.id}>
                      <td>{rfid.id}</td>
                      <td>
                        <code className="rfid-display">{rfid.rfid}</code>
                      </td>
                      <td>{rfid.maSinhVien || '-'}</td>
                      <td>{rfid.tenSinhVien || '-'}</td>
                      <td>{new Date(rfid.createdAt).toLocaleString('vi-VN')}</td>
                      <td>
                        <Badge bg={rfid.processed ? 'success' : 'warning'}>
                          {rfid.processed ? 'Đã xử lý' : 'Chưa xử lý'}
                        </Badge>
                      </td>
                      <td>
                        {!rfid.processed && (
                          <Button
                            variant="success"
                            size="sm"
                            onClick={() => handleMarkProcessed(rfid.id)}
                          >
                            Đã xử lý
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>

              {unprocessedRfids.length === 0 && (
                <Alert variant="success">
                  Không có RFID nào chưa được xử lý.
                </Alert>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default RfidReader;
