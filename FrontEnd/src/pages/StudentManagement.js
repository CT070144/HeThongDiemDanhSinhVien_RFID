import React, { useState, useEffect, useCallback } from 'react';
import { Container, Row, Col, Card, Table, Button, Form, Modal, Alert } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { studentAPI } from '../services/api';

const StudentManagement = () => {
  const [students, setStudents] = useState([]);
  const [filteredStudents, setFilteredStudents] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingStudent, setEditingStudent] = useState(null);
  const [formData, setFormData] = useState({
    rfid: '',
    maSinhVien: '',
    tenSinhVien: ''
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadStudents();
  }, []);

  const filterStudents = useCallback(() => {
    if (!searchKeyword.trim()) {
      setFilteredStudents(students);
    } else {
      const filtered = students.filter(student =>
        student.maSinhVien.toLowerCase().includes(searchKeyword.toLowerCase()) ||
        student.tenSinhVien.toLowerCase().includes(searchKeyword.toLowerCase())
      );
      setFilteredStudents(filtered);
    }
  }, [students, searchKeyword]);

  useEffect(() => {
    filterStudents();
  }, [filterStudents]);

  const loadStudents = async () => {
    try {
      const response = await studentAPI.getAll();
      setStudents(response.data);
      setPage(1);
    } catch (error) {
      toast.error('Lỗi khi tải danh sách sinh viên');
    }
  };


  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (editingStudent) {
        await studentAPI.update(editingStudent.rfid, formData);
        toast.success('Cập nhật sinh viên thành công');
      } else {
        await studentAPI.create(formData);
        toast.success('Thêm sinh viên thành công');
      }
      
      setShowModal(false);
      setFormData({ rfid: '', maSinhVien: '', tenSinhVien: '' });
      setEditingStudent(null);
      loadStudents();
    } catch (error) {
      toast.error(error.response?.data || 'Có lỗi xảy ra');
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (student) => {
    setEditingStudent(student);
    setFormData({
      rfid: student.rfid,
      maSinhVien: student.maSinhVien,
      tenSinhVien: student.tenSinhVien
    });
    setShowModal(true);
  };

  const handleDelete = async (rfid) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa sinh viên này?')) {
      try {
        await studentAPI.delete(rfid);
        toast.success('Xóa sinh viên thành công');
        loadStudents();
      } catch (error) {
        toast.error('Lỗi khi xóa sinh viên! Ràng buộc liên quan');
      }
    }
  };

  const handleAddNew = () => {
    setEditingStudent(null);
    setFormData({ rfid: '', maSinhVien: '', tenSinhVien: '' });
    setShowModal(true);
  };

  return (
    <Container>
      <Row>
        <Col>
          <Card>
            <Card.Header>
              <h3>Quản lý sinh viên</h3>
            </Card.Header>
            <Card.Body>
              <Row className="mb-3">
                <Col md={6}>
                  <Form.Control
                    type="text"
                    placeholder="Tìm kiếm theo mã sinh viên hoặc tên..."
                    value={searchKeyword}
                    onChange={(e) => setSearchKeyword(e.target.value)}
                  />
                </Col>
                <Col md={6} className="text-end">
                  <Button variant="primary" onClick={handleAddNew}>
                    Thêm sinh viên mới
                  </Button>
                </Col>
              </Row>

              <Table responsive striped bordered hover>
                <thead>
                  <tr>
                    <th>RFID</th>
                    <th>Mã sinh viên</th>
                    <th>Tên sinh viên</th>
                    <th>Ngày tạo</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStudents
                    .slice((page - 1) * pageSize, page * pageSize)
                    .map((student) => (
                    <tr key={student.rfid}>
                      <td>{student.rfid}</td>
                      <td>{student.maSinhVien}</td>
                      <td>{student.tenSinhVien}</td>
                      <td>{new Date(student.createdAt).toLocaleDateString('vi-VN')}</td>
                      <td>
                        <Button
                          variant="warning"
                          size="sm"
                          onClick={() => handleEdit(student)}
                          className="me-2"
                        >
                          Sửa
                        </Button>
                        <Button
                          variant="danger"
                          size="sm"
                          onClick={() => handleDelete(student.rfid)}
                        >
                          Xóa
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>

              {filteredStudents.length === 0 && (
                <Alert variant="info">
                  Không có sinh viên nào được tìm thấy.
                </Alert>
              )}

              {filteredStudents.length > 0 && (
                <div className="d-flex justify-content-between align-items-center mt-3">
                  <div>Trang {page}</div>
                  <div className="d-flex gap-2">
                    <Button
                      variant="outline-secondary"
                      disabled={page === 1}
                      onClick={() => setPage((p) => Math.max(1, p - 1))}
                    >
                      Trước
                    </Button>
                    <Button
                      variant="outline-secondary"
                      disabled={filteredStudents.length <= page * pageSize}
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

      {/* Modal thêm/sửa sinh viên */}
      <Modal show={showModal} onHide={() => setShowModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>
            {editingStudent ? 'Sửa sinh viên' : 'Thêm sinh viên mới'}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>RFID</Form.Label>
              <Form.Control
                type="text"
                name="rfid"
                value={formData.rfid}
                onChange={handleInputChange}
                required
                disabled={editingStudent}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Mã sinh viên</Form.Label>
              <Form.Control
                type="text"
                name="maSinhVien"
                value={formData.maSinhVien}
                onChange={handleInputChange}
                required
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Tên sinh viên</Form.Label>
              <Form.Control
                type="text"
                name="tenSinhVien"
                value={formData.tenSinhVien}
                onChange={handleInputChange}
                required
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowModal(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" disabled={loading}>
              {loading ? 'Đang xử lý...' : (editingStudent ? 'Cập nhật' : 'Thêm')}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </Container>
  );
};

export default StudentManagement;
