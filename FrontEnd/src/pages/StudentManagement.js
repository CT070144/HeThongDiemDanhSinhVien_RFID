import React, { useState, useEffect, useCallback } from 'react';
import { Container, Row, Col, Card, Table, Button, Form, Modal, Alert, Badge } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { studentAPI } from '../services/api';
import api from '../services/api';

const StudentManagement = () => {
  const [students, setStudents] = useState([]);
  const [filteredStudents, setFilteredStudents] = useState([]);
  const [lopHocPhans, setLopHocPhans] = useState([]);
  const [selectedLopHocPhan, setSelectedLopHocPhan] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingStudent, setEditingStudent] = useState(null);
  const [formData, setFormData] = useState({
    maSinhVien: '',
    rfid: '',
    tenSinhVien: ''
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadStudents();
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

  const filterStudents = useCallback(async () => {
    let filtered = students;
    
    // Filter by search keyword
    if (searchKeyword.trim()) {
      filtered = filtered.filter(student =>
        student.maSinhVien.toLowerCase().includes(searchKeyword.toLowerCase()) ||
        student.tenSinhVien.toLowerCase().includes(searchKeyword.toLowerCase())
      );
    }
    
    // Filter by lop hoc phan
    if (selectedLopHocPhan) {
      try {
        const response = await api.get(`/lophocphan/${selectedLopHocPhan}/sinhvien`);
        const studentsInLop = response.data;
        const studentIdsInLop = studentsInLop.map(s => s.maSinhVien);
        filtered = filtered.filter(student => studentIdsInLop.includes(student.maSinhVien));
      } catch (error) {
        console.error('Error filtering by lop hoc phan:', error);
      }
    }
    
    setFilteredStudents(filtered);
  }, [students, searchKeyword, selectedLopHocPhan]);

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
        // Sử dụng mã sinh viên làm khóa chính cho update
        await studentAPI.update(editingStudent.maSinhVien, formData);
        toast.success('Cập nhật sinh viên thành công');
      } else {
        await studentAPI.create(formData);
        toast.success('Thêm sinh viên thành công');
      }
      
      setShowModal(false);
      setFormData({ maSinhVien: '', rfid: '', tenSinhVien: '' });
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
      maSinhVien: student.maSinhVien,
      rfid: student.rfid,
      tenSinhVien: student.tenSinhVien
    });
    setShowModal(true);
  };

  const handleDelete = async (maSinhVien) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa sinh viên này?')) {
      try {
        await studentAPI.delete(maSinhVien);
        toast.success('Xóa sinh viên thành công');
        loadStudents();
      } catch (error) {
        toast.error('Lỗi khi xóa sinh viên! Ràng buộc liên quan');
      }
    }
  };

  const handleAddNew = () => {
    setEditingStudent(null);
    setFormData({ maSinhVien: '', rfid: '', tenSinhVien: '' });
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
                <Col md={4}>
                  <Form.Control
                    type="text"
                    placeholder="Tìm kiếm theo mã sinh viên hoặc tên..."
                    value={searchKeyword}
                    onChange={(e) => setSearchKeyword(e.target.value)}
                  />
                </Col>
                <Col md={4}>
                  <Form.Select
                    value={selectedLopHocPhan}
                    onChange={(e) => setSelectedLopHocPhan(e.target.value)}
                  >
                    <option value="">Tất cả lớp học phần</option>
                    {lopHocPhans.map((lop) => (
                      <option key={lop.maLopHocPhan} value={lop.maLopHocPhan}>
                        {lop.tenLopHocPhan}
                      </option>
                    ))}
                  </Form.Select>
                </Col>
                <Col md={4} className="text-end">
                  <Button variant="primary" onClick={handleAddNew}>
                    Thêm sinh viên mới
                  </Button>
                </Col>
              </Row>

              {selectedLopHocPhan && (
                <Row className="mb-3">
                  <Col>
                    <Alert variant="info">
                      Đang hiển thị sinh viên của lớp học phần: <Badge bg="primary">
                        {lopHocPhans.find(l => l.maLopHocPhan === selectedLopHocPhan)?.tenLopHocPhan}
                      </Badge>
                    </Alert>
                  </Col>
                </Row>
              )}

              <Table responsive striped bordered hover>
                <thead>
                  <tr>
                    <th>Mã sinh viên</th>
                    <th>RFID</th>
                    <th>Tên sinh viên</th>
                    <th>Ngày tạo</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStudents
                    .slice((page - 1) * pageSize, page * pageSize)
                    .map((student) => (
                    <tr key={student.maSinhVien}>
                      <td>
                        <Badge bg="primary">{student.maSinhVien}</Badge>
                      </td>
                      <td>
                        <Badge bg="info">{student.rfid}</Badge>
                      </td>
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
                          onClick={() => handleDelete(student.maSinhVien)}
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
            {editingStudent ? (
              <>
                Sửa sinh viên
              </>
            ) : (
              'Thêm sinh viên mới'
            )}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Mã sinh viên</Form.Label>
              <Form.Control
                type="text"
                name="maSinhVien"
                value={formData.maSinhVien}
                onChange={handleInputChange}
                required
                disabled={editingStudent ? true : false}
                placeholder="Nhập mã sinh viên (VD: CT070201)"
              />
              
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>RFID </Form.Label>
              <Form.Control
                type="text"
                name="rfid"
                value={formData.rfid}
                onChange={handleInputChange}
                required
                placeholder="Nhập mã RFID (VD: RFID001)"
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
                placeholder="Nhập họ và tên sinh viên"
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
