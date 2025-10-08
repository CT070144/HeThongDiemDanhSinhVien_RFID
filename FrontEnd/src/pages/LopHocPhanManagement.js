import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Modal, Form, Alert, Badge, Spinner, Pagination } from 'react-bootstrap';
import { toast } from 'react-toastify';
import * as XLSX from 'xlsx';
import api from '../services/api';

const LopHocPhanManagement = () => {
  const [lopHocPhans, setLopHocPhans] = useState([]);
  const [sinhViens, setSinhViens] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [showStudentModal, setShowStudentModal] = useState(false);
  const [selectedLopHocPhan, setSelectedLopHocPhan] = useState(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showImportModal, setShowImportModal] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);

  // Pagination states
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(10);
  const [totalStudents, setTotalStudents] = useState(0);

  // Form states
  const [formData, setFormData] = useState({
    maLopHocPhan: '',
    tenLopHocPhan: ''
  });

  useEffect(() => {
    fetchLopHocPhans();
  }, []);

  const fetchLopHocPhans = async () => {
    setLoading(true);
    try {
      const response = await api.get('/lophocphan');
      setLopHocPhans(response.data);
    } catch (error) {
      toast.error('Lỗi khi tải danh sách lớp học phần');
      console.error('Error fetching lop hoc phan:', error);
    } finally {
      setLoading(false);
    }
  };

  const searchLopHocPhans = async () => {
    setLoading(true);
    try {
      const response = await api.get(`/lophocphan/search?keyword=${encodeURIComponent(searchKeyword)}`);
      setLopHocPhans(response.data);
    } catch (error) {
      toast.error('Lỗi khi tìm kiếm lớp học phần');
      console.error('Error searching lop hoc phan:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchKeyword.trim()) {
      searchLopHocPhans();
    } else {
      fetchLopHocPhans();
    }
  };

  const fetchSinhViensByLopHocPhan = async (maLopHocPhan) => {
    try {
      const response = await api.get(`/lophocphan/${maLopHocPhan}/sinhvien`);
      setSinhViens(response.data);
      setTotalStudents(response.data.length);
      setCurrentPage(1); // Reset to first page
      setSelectedLopHocPhan(maLopHocPhan); // Lưu thông tin lớp được chọn
      setShowStudentModal(true);
    } catch (error) {
      toast.error('Lỗi khi tải danh sách sinh viên');
      console.error('Error fetching students:', error);
    }
  };

  const handleCreate = () => {
    setFormData({ maLopHocPhan: '', tenLopHocPhan: '' });
    setShowModal(true);
  };

  const handleEdit = (lopHocPhan) => {
    setFormData({
      maLopHocPhan: lopHocPhan.maLopHocPhan,
      tenLopHocPhan: lopHocPhan.tenLopHocPhan
    });
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (formData.maLopHocPhan && lopHocPhans.some(lhp => lhp.maLopHocPhan === formData.maLopHocPhan)) {
        // Update existing
        await api.put(`/lophocphan/${formData.maLopHocPhan}`, formData);
        toast.success('Cập nhật lớp học phần thành công');
      } else {
        // Create new
        await api.post('/lophocphan', formData);
        toast.success('Tạo lớp học phần thành công');
      }
      
      setShowModal(false);
      fetchLopHocPhans();
    } catch (error) {
      const message = error.response?.data?.message || 'Có lỗi xảy ra';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (maLopHocPhan, tenLopHocPhan) => {
    if (window.confirm(`Bạn có chắc chắn muốn xóa lớp học phần "${tenLopHocPhan}"?`)) {
      try {
        await api.delete(`/lophocphan/${maLopHocPhan}`);
        toast.success('Xóa lớp học phần thành công');
        fetchLopHocPhans();
      } catch (error) {
        const message = error.response?.data?.message || 'Có lỗi xảy ra';
        toast.error(message);
      }
    }
  };

  const handleImportFile = async (e) => {
    e.preventDefault();
    if (!importFile) {
      toast.error('Vui lòng chọn file để import');
      return;
    }

    setImporting(true);
    const formData = new FormData();
    formData.append('file', importFile);

    try {
      const response = await api.post('/lophocphan/import', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      setImportResult(response.data);
      toast.success('Import hoàn thành!');
      setShowImportModal(false);
      fetchLopHocPhans();
    } catch (error) {
      const message = error.response?.data?.message || 'Có lỗi xảy ra khi import';
      toast.error(message);
    } finally {
      setImporting(false);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('vi-VN');
  };

  // Pagination functions
  const getTotalPages = () => {
    return Math.ceil(totalStudents / itemsPerPage);
  };

  const getCurrentPageStudents = () => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    return sinhViens.slice(startIndex, endIndex);
  };

  const handlePageChange = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  // Export functions
  const exportStudentsToExcel = (lopHocPhan) => {
    if (!sinhViens || sinhViens.length === 0) {
      toast.error('Không có dữ liệu sinh viên để xuất');
      return;
    }

    const wb = XLSX.utils.book_new();
    
    // Prepare data for export
    const exportData = sinhViens.map((sinhVien, index) => ({
      'STT': index + 1,
      'Mã sinh viên': sinhVien.maSinhVien,
      'Tên sinh viên': sinhVien.tenSinhVien,
      'RFID': sinhVien.rfid,
      'Ngày tạo': new Date(sinhVien.createdAt).toLocaleDateString('vi-VN')
    }));

    // Create worksheet
    const ws = XLSX.utils.json_to_sheet(exportData);
    
    // Set column widths
    ws['!cols'] = [
      { width: 5 },   // STT
      { width: 15 },  // Mã sinh viên
      { width: 30 },  // Tên sinh viên
      { width: 20 },  // RFID
      { width: 15 }   // Ngày tạo
    ];

    XLSX.utils.book_append_sheet(wb, ws, 'Danh sách sinh viên');
    
    // Generate filename
    const filename = `DanhSachSinhVien_${lopHocPhan.maLopHocPhan}_${new Date().toISOString().split('T')[0]}.xlsx`;
    
    XLSX.writeFile(wb, filename);
   
  };

  // Close student modal and reset pagination
  const handleCloseStudentModal = () => {
    setShowStudentModal(false);
    setCurrentPage(1);
    setSinhViens([]);
    setTotalStudents(0);
    setSelectedLopHocPhan(null);
  };

  return (
    <Container fluid className="py-4">
      <Row>
        <Col>
          <Card>
            <Card.Header className="d-flex justify-content-between align-items-center">
              <h4 className="mb-0">Quản lý lớp học phần</h4>
              <div>
                <Button variant="primary" onClick={handleCreate} className="me-2">
                  Thêm lớp học phần
                </Button>
                <Button variant="success" onClick={() => setShowImportModal(true)}>
                  Import Excel
                </Button>
              </div>
            </Card.Header>
            <Card.Body>
              {/* Search Form */}
              <Form onSubmit={handleSearch} className="mb-3">
                <Row>
                  <Col md={8}>
                    <Form.Control
                      type="text"
                      placeholder="Tìm kiếm theo mã lớp học phần hoặc tên lớp..."
                      value={searchKeyword}
                      onChange={(e) => setSearchKeyword(e.target.value)}
                    />
                  </Col>
                  <Col md={4}>
                    <Button type="submit" variant="outline-primary">
                      Tìm kiếm
                    </Button>
                    <Button 
                      type="button" 
                      variant="outline-secondary" 
                      className="ms-2"
                      onClick={() => {
                        setSearchKeyword('');
                        fetchLopHocPhans();
                      }}
                    >
                      Làm mới
                    </Button>
                  </Col>
                </Row>
              </Form>

              {/* Lop Hoc Phan Table */}
              {loading ? (
                <div className="text-center py-4">
                  <Spinner animation="border" />
                  <p className="mt-2">Đang tải...</p>
                </div>
              ) : (
                <Table responsive striped hover>
                  <thead>
                    <tr>
                      <th>Mã lớp học phần</th>
                      <th>Tên lớp học phần</th>
                      <th>Số sinh viên</th>
                      <th>Ngày tạo</th>
                      <th>Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    {lopHocPhans.map((lopHocPhan) => (
                      <tr key={lopHocPhan.maLopHocPhan}>
                        <td>
                          <Badge bg="primary">{lopHocPhan.maLopHocPhan}</Badge>
                        </td>
                        <td>{lopHocPhan.tenLopHocPhan}</td>
                        <td>
                          <Badge bg="info">
                            {lopHocPhan.soSinhVien || 0} sinh viên
                          </Badge>
                        </td>
                        <td>{formatDate(lopHocPhan.createdAt)}</td>
                        <td>
                          <Button
                            variant="info"
                            size="sm"
                            className="me-1"
                            onClick={() => fetchSinhViensByLopHocPhan(lopHocPhan.maLopHocPhan)}
                          >
                            Xem sinh viên
                          </Button>
                          <Button
                            variant="warning"
                            size="sm"
                            className="me-1"
                            onClick={() => handleEdit(lopHocPhan)}
                          >
                            Sửa
                          </Button>
                          <Button
                            variant="danger"
                            size="sm"
                            onClick={() => handleDelete(lopHocPhan.maLopHocPhan, lopHocPhan.tenLopHocPhan)}
                          >
                            Xóa
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}

              {lopHocPhans.length === 0 && !loading && (
                <div className="text-center py-4">
                  <p className="text-muted">Không có lớp học phần nào</p>
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Create/Edit Modal */}
      <Modal show={showModal} onHide={() => setShowModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>
            {formData.maLopHocPhan && lopHocPhans.some(lhp => lhp.maLopHocPhan === formData.maLopHocPhan) 
              ? 'Sửa lớp học phần' 
              : 'Thêm lớp học phần mới'}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Mã lớp học phần</Form.Label>
              <Form.Control
                type="text"
                value={formData.maLopHocPhan}
                onChange={(e) => setFormData({ ...formData, maLopHocPhan: e.target.value })}
                placeholder="Nhập mã lớp học phần"
                required
                disabled={formData.maLopHocPhan && lopHocPhans.some(lhp => lhp.maLopHocPhan === formData.maLopHocPhan)}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Tên lớp học phần</Form.Label>
              <Form.Control
                type="text"
                value={formData.tenLopHocPhan}
                onChange={(e) => setFormData({ ...formData, tenLopHocPhan: e.target.value })}
                placeholder="Nhập tên lớp học phần"
                required
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowModal(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" disabled={loading}>
              {loading ? <Spinner size="sm" /> : 'Lưu'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Student List Modal */}
      <Modal show={showStudentModal} onHide={handleCloseStudentModal} size="xl">
        <Modal.Header closeButton>
          <Modal.Title>
            Danh sách sinh viên - {selectedLopHocPhan && lopHocPhans.find(l => l.maLopHocPhan === selectedLopHocPhan)?.tenLopHocPhan}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {selectedLopHocPhan && (
            <Alert variant="info" className="mb-3">
              <strong>Tổng số sinh viên:</strong> {totalStudents} sinh viên
            </Alert>
          )}
          
          <Table responsive striped>
            <thead>
              <tr>
                <th>STT</th>
                <th>Mã sinh viên</th>
                <th>Tên sinh viên</th>
                <th>RFID</th>
                <th>Ngày tạo</th>
              </tr>
            </thead>
            <tbody>
              {getCurrentPageStudents().map((sinhVien, index) => (
                <tr key={sinhVien.rfid}>
                  <td>{(currentPage - 1) * itemsPerPage + index + 1}</td>
                  <td>
                    <Badge bg="secondary">{sinhVien.maSinhVien}</Badge>
                  </td>
                  <td>{sinhVien.tenSinhVien}</td>
                  <td>
                    <code>{sinhVien.rfid}</code>
                  </td>
                  <td>{new Date(sinhVien.createdAt).toLocaleDateString('vi-VN')}</td>
                </tr>
              ))}
            </tbody>
          </Table>
          
          {sinhViens.length === 0 && (
            <div className="text-center py-3">
              <p className="text-muted">Không có sinh viên nào trong lớp học phần này</p>
            </div>
          )}

          {/* Pagination */}
          {totalStudents > itemsPerPage && (
            <div className="d-flex justify-content-center mt-3">
              <Pagination>
                <Pagination.Prev 
                  disabled={currentPage === 1}
                  onClick={() => handlePageChange(currentPage - 1)}
                />
                {Array.from({ length: getTotalPages() }, (_, i) => i + 1).map(pageNumber => (
                  <Pagination.Item
                    key={pageNumber}
                    active={pageNumber === currentPage}
                    onClick={() => handlePageChange(pageNumber)}
                  >
                    {pageNumber}
                  </Pagination.Item>
                ))}
                <Pagination.Next 
                  disabled={currentPage === getTotalPages()}
                  onClick={() => handlePageChange(currentPage + 1)}
                />
              </Pagination>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button 
            variant="success" 
            onClick={() => {
              const lopInfo = lopHocPhans.find(l => l.maLopHocPhan === selectedLopHocPhan);
              exportStudentsToExcel(lopInfo);
            }}
            disabled={sinhViens.length === 0}
          >
            Xuất Excel
          </Button>
          <Button variant="secondary" onClick={handleCloseStudentModal}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>

      {/* Import Modal */}
      <Modal show={showImportModal} onHide={() => setShowImportModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Import danh sách sinh viên từ Excel</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleImportFile}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Chọn file Excel</Form.Label>
              <Form.Control
                type="file"
                accept=".xls,.xlsx"
                onChange={(e) => setImportFile(e.target.files[0])}
                required
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowImportModal(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" disabled={importing || !importFile}>
              {importing ? <Spinner size="sm" /> : 'Import'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Import Result Modal */}
      {importResult && (
        <Modal show={!!importResult} onHide={() => setImportResult(null)} size="lg">
          <Modal.Header closeButton>
            <Modal.Title>Kết quả import</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Alert variant="success">
              <strong>Tổng kết:</strong>
              <ul className="mb-0">
                <li>Số sheet: {importResult.totalSheets}</li>
                <li>Số lớp học phần mới: {importResult.totalClasses}</li>
                <li>Số sinh viên mới: {importResult.totalStudents}</li>
              </ul>
            </Alert>
            
            {/* Hiển thị thông tin chi tiết về các lớp học phần */}
            {importResult.successes && importResult.successes.length > 0 && (
              <div className="mb-3">
                <h6 className="text-success">Thành công:</h6>
                <div className="row">
                  {importResult.successes.map((success, index) => {
                    // Tìm thông tin về lớp học phần từ success message
                    const isClassInfo = success.includes('Tạo mới lớp học phần') || success.includes('Cập nhật lớp học phần');
                    const isStudentInfo = success.includes('Thêm') && success.includes('sinh viên vào lớp');
                    
                    return (
                      <div key={index} className="col-12 mb-2">
                        {isClassInfo ? (
                          <div className="card border-success">
                            <div className="card-body py-2">
                              <div className="d-flex align-items-center">
                                <span className="text-success me-2">✓</span>
                                <div>
                                  <strong className="text-success">{success}</strong>
                                  <div className="small text-muted">
                                    {success.includes('Tạo mới') ? 'Lớp học phần mới được tạo' : 'Lớp học phần đã được cập nhật'}
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                        ) : isStudentInfo ? (
                          <div className="card border-info">
                            <div className="card-body py-2">
                              <div className="d-flex align-items-center">
                                <span className="text-info me-2">📊</span>
                                <div>
                                  <strong className="text-info">{success}</strong>
                                  <div className="small text-muted">Danh sách sinh viên đã được cập nhật</div>
                                </div>
                              </div>
                            </div>
                          </div>
                        ) : (
                          <div className="d-flex align-items-center">
                            <span className="text-success me-2">✓</span>
                            <span className="text-success">{success}</span>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
            
            {importResult.errors && importResult.errors.length > 0 && (
              <div>
                <h6 className="text-danger">Lỗi:</h6>
                <ul className="list-unstyled">
                  {importResult.errors.map((error, index) => (
                    <li key={index} className="text-danger">✗ {error}</li>
                  ))}
                </ul>
              </div>
            )}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="primary" onClick={() => setImportResult(null)}>
              Đóng
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </Container>
  );
};

export default LopHocPhanManagement;
