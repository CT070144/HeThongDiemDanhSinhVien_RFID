import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Table, Modal, Form, Alert, Tab, Tabs } from 'react-bootstrap';
import { toast } from 'react-toastify';
import api from '../services/api';

const CourseManagement = () => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingCourse, setEditingCourse] = useState(null);
  const [showImportModal, setShowImportModal] = useState(false);
  const [showStudentImportModal, setShowStudentImportModal] = useState(false);
  const [showTestModal, setShowTestModal] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState('');
  const [formData, setFormData] = useState({
    maLopHocPhan: '',
    tenMonHocPhan: '',
    giangVien: '',
    soSinhVienThamGia: ''
  });
  const [importData, setImportData] = useState({
    file: null,
    semesterStartDate: '',
    semesterEndDate: ''
  });
  const [studentImportData, setStudentImportData] = useState({
    file: null
  });

  useEffect(() => {
    fetchCourses();
  }, []);

  const fetchCourses = async () => {
    try {
      setLoading(true);
      const response = await api.get('/lophocphan');
      setCourses(response.data);
    } catch (error) {
      toast.error('Lỗi tải danh sách lớp học phần');
      console.error('Error fetching courses:', error);
    } finally {
      setLoading(false);
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
    try {
      const courseData = {
        ...formData,
        soSinhVienThamGia: parseInt(formData.soSinhVienThamGia)
      };

      if (editingCourse) {
        await api.put(`/lophocphan/${editingCourse.maLopHocPhan}`, courseData);
        toast.success('Cập nhật lớp học phần thành công');
      } else {
        await api.post('/lophocphan', courseData);
        toast.success('Thêm lớp học phần thành công');
      }

      setShowModal(false);
      setEditingCourse(null);
      setFormData({ maLopHocPhan: '', tenMonHocPhan: '', giangVien: '', soSinhVienThamGia: '' });
      fetchCourses();
    } catch (error) {
      const errorMessage = error.response?.data || 'Có lỗi xảy ra';
      toast.error(errorMessage);
    }
  };

  const handleEdit = (course) => {
    setEditingCourse(course);
    setFormData({
      maLopHocPhan: course.maLopHocPhan,
      tenMonHocPhan: course.tenMonHocPhan,
      giangVien: course.giangVien,
      soSinhVienThamGia: course.soSinhVienThamGia.toString()
    });
    setShowModal(true);
  };

  const handleDelete = async (maLopHocPhan) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa lớp học phần này?')) {
      try {
        await api.delete(`/lophocphan/${maLopHocPhan}`);
        toast.success('Xóa lớp học phần thành công');
        fetchCourses();
      } catch (error) {
        const errorMessage = error.response?.data || 'Có lỗi xảy ra';
        toast.error(errorMessage);
      }
    }
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingCourse(null);
    setFormData({ maLopHocPhan: '', tenMonHocPhan: '', giangVien: '', soSinhVienThamGia: '' });
  };

  const handleFileChange = (e) => {
    setImportData(prev => ({
      ...prev,
      file: e.target.files[0]
    }));
  };

  const handleStudentFileChange = (e) => {
    setStudentImportData(prev => ({
      ...prev,
      file: e.target.files[0]
    }));
  };

  const handleTestFileChange = (e) => {
    setImportData(prev => ({
      ...prev,
      file: e.target.files[0]
    }));
  };

  const handleTestSubmit = async (e) => {
    e.preventDefault();
    if (!importData.file) {
      toast.error('Vui lòng chọn file để test');
      return;
    }

    try {
      const formData = new FormData();
      formData.append('file', importData.file);

      const response = await api.post('/system-management/test-file-read', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      toast.success('Test đọc file thành công: ' + response.data.message);
      setShowTestModal(false);
      setImportData({ file: null, semesterStartDate: '', semesterEndDate: '' });
    } catch (error) {
      const errorMessage = error.response?.data || 'Có lỗi xảy ra';
      toast.error(errorMessage);
    }
  };

  const handleImportSubmit = async (e) => {
    e.preventDefault();
    if (!importData.file || !importData.semesterStartDate || !importData.semesterEndDate) {
      toast.error('Vui lòng điền đầy đủ thông tin');
      return;
    }

    try {
      const formData = new FormData();
      formData.append('file', importData.file);
      formData.append('semesterStartDate', importData.semesterStartDate);
      formData.append('semesterEndDate', importData.semesterEndDate);

      const response = await api.post('/system-management/import-course-schedule', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      toast.success('Import lịch học thành công');
      setShowImportModal(false);
      setImportData({ file: null, semesterStartDate: '', semesterEndDate: '' });
      fetchCourses();
    } catch (error) {
      const errorMessage = error.response?.data || 'Có lỗi xảy ra';
      toast.error(errorMessage);
    }
  };

  const handleStudentImportSubmit = async (e) => {
    e.preventDefault();
    if (!studentImportData.file || !selectedCourse) {
      toast.error('Vui lòng chọn file và lớp học phần');
      return;
    }

    try {
      const formData = new FormData();
      formData.append('file', studentImportData.file);
      formData.append('maLopHocPhan', selectedCourse);

      const response = await api.post('/system-management/import-student-list', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      toast.success('Import danh sách sinh viên thành công');
      setShowStudentImportModal(false);
      setStudentImportData({ file: null });
      setSelectedCourse('');
    } catch (error) {
      const errorMessage = error.response?.data || 'Có lỗi xảy ra';
      toast.error(errorMessage);
    }
  };

  if (loading) {
    return (
      <Container className="mt-4">
        <div className="text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      </Container>
    );
  }

  return (
    <Container className="mt-4">
      <Row>
        <Col>
          <Card>
            <Card.Header className="d-flex justify-content-between align-items-center">
              <h4>Quản lý lớp học phần</h4>
              <div>
                <Button 
                  variant="success" 
                  className="me-2"
                  onClick={() => setShowImportModal(true)}
                >
                  Import lịch học
                </Button>
                <Button 
                  variant="info" 
                  className="me-2"
                  onClick={() => setShowStudentImportModal(true)}
                >
                  Import sinh viên
                </Button>
                <Button 
                  variant="warning" 
                  className="me-2"
                  onClick={() => setShowTestModal(true)}
                >
                  Test đọc file
                </Button>
                <Button 
                  variant="primary" 
                  onClick={() => setShowModal(true)}
                >
                  Thêm lớp học phần
                </Button>
              </div>
            </Card.Header>
            <Card.Body>
              <Table striped bordered hover responsive>
                <thead>
                  <tr>
                    <th>STT</th>
                    <th>Mã lớp học phần</th>
                    <th>Tên môn học phần</th>
                    <th>Giảng viên</th>
                    <th>Số sinh viên</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {courses.map((course, index) => (
                    <tr key={course.maLopHocPhan}>
                      <td>{index + 1}</td>
                      <td>{course.maLopHocPhan}</td>
                      <td>{course.tenMonHocPhan}</td>
                      <td>{course.giangVien}</td>
                      <td>{course.soSinhVienThamGia}</td>
                      <td>
                        <Button
                          variant="outline-primary"
                          size="sm"
                          onClick={() => handleEdit(course)}
                          className="me-2"
                        >
                          Sửa
                        </Button>
                        <Button
                          variant="outline-danger"
                          size="sm"
                          onClick={() => handleDelete(course.maLopHocPhan)}
                        >
                          Xóa
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Modal for Add/Edit Course */}
      <Modal show={showModal} onHide={handleCloseModal} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>
            {editingCourse ? 'Sửa lớp học phần' : 'Thêm lớp học phần mới'}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit}>
          <Modal.Body>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Mã lớp học phần *</Form.Label>
                  <Form.Control
                    type="text"
                    name="maLopHocPhan"
                    value={formData.maLopHocPhan}
                    onChange={handleInputChange}
                    required
                    disabled={editingCourse}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Tên môn học phần *</Form.Label>
                  <Form.Control
                    type="text"
                    name="tenMonHocPhan"
                    value={formData.tenMonHocPhan}
                    onChange={handleInputChange}
                    required
                  />
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Giảng viên *</Form.Label>
                  <Form.Control
                    type="text"
                    name="giangVien"
                    value={formData.giangVien}
                    onChange={handleInputChange}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Số sinh viên tham gia *</Form.Label>
                  <Form.Control
                    type="number"
                    name="soSinhVienThamGia"
                    value={formData.soSinhVienThamGia}
                    onChange={handleInputChange}
                    required
                    min="0"
                  />
                </Form.Group>
              </Col>
            </Row>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={handleCloseModal}>
              Hủy
            </Button>
            <Button variant="primary" type="submit">
              {editingCourse ? 'Cập nhật' : 'Thêm mới'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Modal for Import Course Schedule */}
      <Modal show={showImportModal} onHide={() => setShowImportModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Import lịch học từ Excel</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleImportSubmit}>
          <Modal.Body>
            <Alert variant="info">
              <strong>Hướng dẫn:</strong> Chọn file Excel chứa thông tin lịch học theo cấu trúc mẫu.
            </Alert>
            <Form.Group className="mb-3">
              <Form.Label>File Excel *</Form.Label>
              <Form.Control
                type="file"
                accept=".xlsx,.xls"
                onChange={handleFileChange}
                required
              />
            </Form.Group>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Ngày bắt đầu học kỳ *</Form.Label>
                  <Form.Control
                    type="date"
                    value={importData.semesterStartDate}
                    onChange={(e) => setImportData(prev => ({ ...prev, semesterStartDate: e.target.value }))}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Ngày kết thúc học kỳ *</Form.Label>
                  <Form.Control
                    type="date"
                    value={importData.semesterEndDate}
                    onChange={(e) => setImportData(prev => ({ ...prev, semesterEndDate: e.target.value }))}
                    required
                  />
                </Form.Group>
              </Col>
            </Row>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowImportModal(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit">
              Import
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Modal for Import Student List */}
      <Modal show={showStudentImportModal} onHide={() => setShowStudentImportModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Import danh sách sinh viên</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleStudentImportSubmit}>
          <Modal.Body>
            <Alert variant="info">
              <strong>Hướng dẫn:</strong> Chọn file Excel chứa danh sách sinh viên theo cấu trúc mẫu.
            </Alert>
            <Form.Group className="mb-3">
              <Form.Label>Lớp học phần *</Form.Label>
              <Form.Select
                value={selectedCourse}
                onChange={(e) => setSelectedCourse(e.target.value)}
                required
              >
                <option value="">Chọn lớp học phần</option>
                {courses.map(course => (
                  <option key={course.maLopHocPhan} value={course.maLopHocPhan}>
                    {course.maLopHocPhan} - {course.tenMonHocPhan}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>File Excel *</Form.Label>
              <Form.Control
                type="file"
                accept=".xlsx,.xls"
                onChange={handleStudentFileChange}
                required
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowStudentImportModal(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit">
              Import
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Modal for Test File Read */}
      <Modal show={showTestModal} onHide={() => setShowTestModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Test đọc file Excel</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleTestSubmit}>
          <Modal.Body>
            <Alert variant="warning">
              <strong>Lưu ý:</strong> Chức năng này chỉ để test việc đọc file Excel, không import dữ liệu.
            </Alert>
            <Form.Group className="mb-3">
              <Form.Label>File Excel *</Form.Label>
              <Form.Control
                type="file"
                accept=".xlsx,.xls"
                onChange={handleTestFileChange}
                required
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowTestModal(false)}>
              Hủy
            </Button>
            <Button variant="warning" type="submit">
              Test đọc file
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </Container>
  );
};

export default CourseManagement;
