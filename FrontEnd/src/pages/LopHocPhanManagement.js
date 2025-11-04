import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Modal, Form, Alert, Badge, Spinner, Pagination, Tabs, Tab } from 'react-bootstrap';
import { toast } from 'react-toastify';
import * as XLSX from 'xlsx';
import { exportClassAttendanceMatrix } from '../services/exportExcel';
import api from '../services/api';
import { FaFileDownload } from "react-icons/fa";

const LopHocPhanManagement = () => {
  const [lopHocPhans, setLopHocPhans] = useState([]);
  const [lhpPage, setLhpPage] = useState(0);
  const [lhpSize, setLhpSize] = useState(10);
  const [lhpTotalPages, setLhpTotalPages] = useState(0);
  const [lhpTotalElements, setLhpTotalElements] = useState(0);
  const [sinhViens, setSinhViens] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [showStudentModal, setShowStudentModal] = useState(false);
  const [selectedLopHocPhan, setSelectedLopHocPhan] = useState(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showImportModal, setShowImportModal] = useState(false);
  const [showCaHocImportModal, setShowCaHocImportModal] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [caHocImportFile, setCaHocImportFile] = useState(null);
  const [tkbUploading, setTkbUploading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [allStudents, setAllStudents] = useState([]);
  const [studentsInClass, setStudentsInClass] = useState([]);
  const [selectedStudentsToAdd, setSelectedStudentsToAdd] = useState([]);
  const [selectedStudentsToRemove, setSelectedStudentsToRemove] = useState([]);
  const [addStudentSearchTerm, setAddStudentSearchTerm] = useState('');
  const [removeStudentSearchTerm, setRemoveStudentSearchTerm] = useState('');

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
    fetchAllStudents();
  }, [lhpPage, lhpSize]);

  const fetchLopHocPhans = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append('page', lhpPage);
      params.append('size', lhpSize);
      if (searchKeyword && searchKeyword.trim()) {
        params.append('keyword', searchKeyword.trim());
      }
      const response = await api.get(`/lophocphan/paged?${params.toString()}`);
      setLopHocPhans(response.data.content || []);
      setLhpTotalPages(response.data.totalPages || 0);
      setLhpTotalElements(response.data.totalElements || 0);
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
      setLhpPage(0);
      await fetchLopHocPhans();
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

  const fetchAllStudents = async () => {
    try {
      const response = await api.get('/sinhvien');
      setAllStudents(response.data);
    } catch (error) {
      toast.error('Lỗi khi tải danh sách sinh viên');
      console.error('Error fetching all students:', error);
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

  const handleDownloadAttendanceMatrix = async (lop) => {
    try {
      // 1) Fetch sessions (ca, ngay) for this class
      const sessionsRes = await api.get(`/cahoc/sessions?lopHocPhan=${encodeURIComponent(lop.tenLopHocPhan)}`);
      const sessions = sessionsRes.data || [];
      if (!sessions.length) {
        toast.info('Chưa có lịch học trong ca_hoc cho lớp này');
        return;
      }

      // 2) Fetch students of the class
      const studentsRes = await api.get(`/lophocphan/${lop.maLopHocPhan}/sinhvien`);
      const students = studentsRes.data || [];
      if (!students.length) {
        toast.info('Lớp chưa có sinh viên');
        return;
      }

      // 3) Fetch all attendance and filter needed set
      const attendanceRes = await api.get('/attendance');
      const attendance = (attendanceRes.data || []).filter(r => 
        students.some(s => s.maSinhVien === r.maSinhVien)
      );

      // 4) Export using util
      exportClassAttendanceMatrix({
        lopHocPhan: lop.maLopHocPhan,
        students: students,
        sessions: sessions,
        attendance: attendance
      });
    } catch (e) {
      console.error(e);
      toast.error('Lỗi khi tạo file điểm danh'+e.message);
    }
  };


  const handleCreate = () => {
    setFormData({ maLopHocPhan: '', tenLopHocPhan: '' });
    setShowModal(true);
  };

  const handleEdit = async (lopHocPhan) => {
    setFormData({
      maLopHocPhan: lopHocPhan.maLopHocPhan,
      tenLopHocPhan: lopHocPhan.tenLopHocPhan
    });
    
    // Load students in this class for editing
    try {
      const response = await api.get(`/lophocphan/${lopHocPhan.maLopHocPhan}/sinhvien`);
      setStudentsInClass(response.data);
    } catch (error) {
      console.error('Error fetching students in class:', error);
      setStudentsInClass([]);
    }
    
    // Reset selections and search terms
    setSelectedStudentsToAdd([]);
    setSelectedStudentsToRemove([]);
    setAddStudentSearchTerm('');
    setRemoveStudentSearchTerm('');
    
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

  const openStudentListFromInfo = async () => {
    if (!formData.maLopHocPhan) {
      toast.error('Chưa có mã lớp học phần');
      return;
    }
    if (!lopHocPhans.some(lhp => lhp.maLopHocPhan === formData.maLopHocPhan)) {
      toast.error('Lớp học phần chưa tồn tại, vui lòng lưu trước');
      return;
    }
    setShowModal(false);
    await fetchSinhViensByLopHocPhan(formData.maLopHocPhan);
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

  const handleCaHocImportSubmit = async (e) => {
    e.preventDefault();
    if (!caHocImportFile) {
      toast.error('Vui lòng chọn file Excel');
      return;
    }
    const formData = new FormData();
    formData.append('file', caHocImportFile);
    try {
      setTkbUploading(true);
      const response = await api.post('/cahoc/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      toast.success('Import lịch học thành công: ' + (response.data?.count ?? ''));
      setShowCaHocImportModal(false);
      setCaHocImportFile(null);
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data || 'Có lỗi xảy ra khi import';
      toast.error(message);
    }
    finally {
      setTkbUploading(false);
      fetchLopHocPhans();
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

  // Handle adding students to class
  const handleAddStudents = async () => {
    if (selectedStudentsToAdd.length === 0) {
      toast.warning('Vui lòng chọn sinh viên để thêm');
      return;
    }

    if (!formData.maLopHocPhan) {
      toast.error('Không tìm thấy mã lớp học phần');
      return;
    }

    try {
      setLoading(true);
      
      // Add each selected student to the class
      for (const studentId of selectedStudentsToAdd) {
        const requestData = {
          maLopHocPhan: formData.maLopHocPhan,
          maSinhVien: studentId
        };
        console.log('Adding student:', requestData);
        await api.post('/lophocphan/add-student', requestData);
      }
      
      toast.success(`Đã thêm ${selectedStudentsToAdd.length} sinh viên vào lớp`);
      
      // Refresh data
      await fetchLopHocPhans();
      
      // Reload students in class
      const response = await api.get(`/lophocphan/${formData.maLopHocPhan}/sinhvien`);
      setStudentsInClass(response.data);
      
      setSelectedStudentsToAdd([]);
    } catch (error) {
      toast.error('Lỗi khi thêm sinh viên vào lớp');
      console.error('Error adding students:', error);
    } finally {
      setLoading(false);
    }
  };

  // Handle removing students from class
  const handleRemoveStudents = async () => {
    if (selectedStudentsToRemove.length === 0) {
      toast.warning('Vui lòng chọn sinh viên để xóa');
      return;
    }

    if (!formData.maLopHocPhan) {
      toast.error('Không tìm thấy mã lớp học phần');
      return;
    }

    try {
      setLoading(true);
      
      // Remove each selected student from the class
      for (const studentId of selectedStudentsToRemove) {
        const requestData = {
          maLopHocPhan: formData.maLopHocPhan,
          maSinhVien: studentId
        };
        console.log('Removing student:', requestData);
        await api.post('/lophocphan/remove-student', requestData);
      }
      
      toast.success(`Đã xóa ${selectedStudentsToRemove.length} sinh viên khỏi lớp`);
      
      // Refresh data
      await fetchLopHocPhans();
      
      // Reload students in class
      const response = await api.get(`/lophocphan/${formData.maLopHocPhan}/sinhvien`);
      setStudentsInClass(response.data);
      
      setSelectedStudentsToRemove([]);
    } catch (error) {
      toast.error('Lỗi khi xóa sinh viên khỏi lớp');
      console.error('Error removing students:', error);
    } finally {
      setLoading(false);
    }
  };

  // Get students available to add (not already in class)
  const getAvailableStudents = () => {
    const studentsInClassIds = studentsInClass.map(s => s.maSinhVien);
    return allStudents.filter(student => !studentsInClassIds.includes(student.maSinhVien));
  };

  // Get filtered students for adding (with search)
  const getFilteredAvailableStudents = () => {
    const availableStudents = getAvailableStudents();
    if (!addStudentSearchTerm.trim()) return availableStudents;
    
    const searchLower = addStudentSearchTerm.toLowerCase();
    return availableStudents.filter(student => 
      student.maSinhVien.toLowerCase().includes(searchLower) ||
      student.tenSinhVien.toLowerCase().includes(searchLower)
    );
  };

  // Get filtered students for removing (with search)
  const getFilteredStudentsInClass = () => {
    if (!removeStudentSearchTerm.trim()) return studentsInClass;
    
    const searchLower = removeStudentSearchTerm.toLowerCase();
    return studentsInClass.filter(student => 
      student.maSinhVien.toLowerCase().includes(searchLower) ||
      student.tenSinhVien.toLowerCase().includes(searchLower)
    );
  };

  // Handle checkbox selection for adding students
  const handleAddStudentCheckboxChange = (studentId, isChecked) => {
    if (isChecked) {
      setSelectedStudentsToAdd([...selectedStudentsToAdd, studentId]);
    } else {
      setSelectedStudentsToAdd(selectedStudentsToAdd.filter(id => id !== studentId));
    }
  };

  // Handle checkbox selection for removing students
  const handleRemoveStudentCheckboxChange = (studentId, isChecked) => {
    if (isChecked) {
      setSelectedStudentsToRemove([...selectedStudentsToRemove, studentId]);
    } else {
      setSelectedStudentsToRemove(selectedStudentsToRemove.filter(id => id !== studentId));
    }
  };

  // Select all available students
  const handleSelectAllAvailable = () => {
    const filteredStudents = getFilteredAvailableStudents();
    const allIds = filteredStudents.map(s => s.maSinhVien);
    setSelectedStudentsToAdd(allIds);
  };

  // Deselect all available students
  const handleDeselectAllAvailable = () => {
    setSelectedStudentsToAdd([]);
  };

  // Select all students in class
  const handleSelectAllInClass = () => {
    const filteredStudents = getFilteredStudentsInClass();
    const allIds = filteredStudents.map(s => s.maSinhVien);
    setSelectedStudentsToRemove(allIds);
  };

  // Deselect all students in class
  const handleDeselectAllInClass = () => {
    setSelectedStudentsToRemove([]);
  };

  // Close student modal and reset pagination
  const handleCloseStudentModal = () => {
    setShowStudentModal(false);
    setCurrentPage(1);
    setSinhViens([]);
    setTotalStudents(0);
    setSelectedLopHocPhan(null);
  };


  // Close main modal and reset all states
  const handleCloseModal = () => {
    setShowModal(false);
    setStudentsInClass([]);
    setSelectedStudentsToAdd([]);
    setSelectedStudentsToRemove([]);
    setAddStudentSearchTerm('');
    setRemoveStudentSearchTerm('');
  };

  return (
    <Container fluid className="py-4">
      <Row>
        <Col>
          <Card>
            <Card.Header className="d-flex justify-content-between align-items-center">
              <h4 className="mb-0">Lớp học phần</h4>
              <div>
                <Button variant="primary" onClick={handleCreate} className="me-2">
                  Thêm lớp học phần
                </Button>
                <Button 
                  variant="secondary" 
                  className="ms-2"
                  onClick={() => setShowCaHocImportModal(true)}
                >
                  Import TKB
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
                        setLhpPage(0);
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
                      <th style={{textAlign: 'left'}}>Mã lớp học phần</th>
                      <th style={{textAlign: 'left'}}>Tên lớp học phần</th>
                      <th style={{textAlign: 'left'} }>Giảng viên</th>
                      <th style={{textAlign: 'left'}}>Hình thức học</th>
                      <th style={{textAlign: 'left'}}>Phòng học</th>
                      <th >Số sinh viên</th>
                      <th>File điểm danh</th>
                      <th>Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    {lopHocPhans.map((lopHocPhan) => (
                      <tr key={lopHocPhan.maLopHocPhan}>
                        <td style={{textAlign: 'left'}}>
                          <Badge bg="primary">{lopHocPhan.maLopHocPhan}</Badge>
                        </td>
                        
                        <td style={{textAlign: 'left'}}>{lopHocPhan.tenLopHocPhan}</td>
                        <td style={{textAlign: 'left'}}>{lopHocPhan.giangVien || '-'}</td>
                        <td style={{textAlign: 'left'}}>{lopHocPhan.hinhThucHoc || '-'}</td>
                        <td style={{textAlign: 'left'}}>{lopHocPhan.phongHoc || '-'}</td>
                        <td>
                          <Badge bg="info">
                            {lopHocPhan.soSinhVien || 0} sinh viên
                          </Badge>
                        </td>
                        <td>
                            <FaFileDownload size={30} color="gray" style={{cursor: 'pointer'}} onClick={() => handleDownloadAttendanceMatrix(lopHocPhan)} />
                        </td>
                        <td>
                          <Button
                            variant="warning"
                            size="sm"
                            className="me-1"
                            onClick={() => handleEdit(lopHocPhan)}
                            style={{marginBottom: '5px'}}
                          >
                            Sửa
                          </Button>
                          <Button
                            variant="danger"
                            style={{marginBottom: '5px'}}
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

              {/* Pagination for LopHocPhan */}
              {lhpTotalPages > 1 && (
                <div className="d-flex justify-content-between align-items-center mt-3">
                  <div className="text-muted small">Tổng: {lhpTotalElements} lớp học phần</div>
                  <Pagination className="mb-0">
                    <Pagination.Prev 
                      disabled={lhpPage === 0}
                      onClick={() => setLhpPage(Math.max(0, lhpPage - 1))}
                    />
                    {Array.from({ length: lhpTotalPages }, (_, i) => i).map(pageNumber => (
                      <Pagination.Item
                        key={pageNumber}
                        active={pageNumber === lhpPage}
                        onClick={() => setLhpPage(pageNumber)}
                      >
                        {pageNumber + 1}
                      </Pagination.Item>
                    ))}
                    <Pagination.Next 
                      disabled={lhpPage >= lhpTotalPages - 1}
                      onClick={() => setLhpPage(Math.min(lhpTotalPages - 1, lhpPage + 1))}
                    />
                  </Pagination>
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Create/Edit Modal */}
      <Modal show={showModal} onHide={handleCloseModal} size="xl">
        <Modal.Header closeButton>
          <Modal.Title>
            {formData.maLopHocPhan && lopHocPhans.some(lhp => lhp.maLopHocPhan === formData.maLopHocPhan) 
              ? 'Sửa lớp học phần' 
              : 'Thêm lớp học phần mới'}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Tabs defaultActiveKey="info" className="mb-3">
            <Tab eventKey="info" title="Thông tin lớp học phần">
              <Form onSubmit={handleSubmit}>
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
                <div className="d-flex justify-content-between">
                  <div></div>
                  <div className="d-flex gap-2">
                    <Button 
                      variant="outline-primary" 
                      type="button"
                      onClick={openStudentListFromInfo}
                      disabled={!formData.maLopHocPhan || !lopHocPhans.some(lhp => lhp.maLopHocPhan === formData.maLopHocPhan)}
                    >
                      Danh sách SV
                    </Button>
                   
                  </div>
                </div>
              </Form>
            </Tab>
            
            <Tab 
              eventKey="add-students" 
              title="Thêm sinh viên"
              disabled={!formData.maLopHocPhan || !lopHocPhans.some(lhp => lhp.maLopHocPhan === formData.maLopHocPhan)}
            >
              <Card>
                <Card.Header className="bg-success text-white">
                  <h6 className="mb-0">Thêm sinh viên vào lớp</h6>
                </Card.Header>
                <Card.Body>
                  <div className="d-flex justify-content-end mb-3">
                    <Button variant="outline-success" size="sm" onClick={() => setShowImportModal(true)}>
                      Import danh sách sinh viên từ Excel
                    </Button>
                  </div>
                  {/* Search */}
                  <Form.Group className="mb-3">
                    <Form.Label>Tìm kiếm sinh viên:</Form.Label>
                    <Form.Control
                      type="text"
                      placeholder="Nhập mã sinh viên hoặc tên..."
                      value={addStudentSearchTerm}
                      onChange={(e) => setAddStudentSearchTerm(e.target.value)}
                    />
                  </Form.Group>
                  
                  {/* Select All/Deselect All buttons */}
                  <div className="d-flex gap-2 mb-3">
                    <Button variant="outline-success" size="sm" onClick={handleSelectAllAvailable}>
                      Chọn tất cả
                    </Button>
                    <Button variant="outline-secondary" size="sm" onClick={handleDeselectAllAvailable}>
                      Bỏ chọn tất cả
                    </Button>
                    <Badge bg="success">
                      Đã chọn: {selectedStudentsToAdd.length}
                    </Badge>
                  </div>
                  
                  {/* Student list with checkboxes */}
                  <div style={{ maxHeight: '300px', overflowY: 'auto', border: '1px solid #dee2e6', borderRadius: '0.375rem', padding: '10px' }}>
                    {getFilteredAvailableStudents().length === 0 ? (
                      <p className="text-muted text-center py-3 mb-0">
                        {addStudentSearchTerm.trim() ? 'Không tìm thấy sinh viên nào' : 'Không có sinh viên nào để thêm'}
                      </p>
                    ) : (
                      getFilteredAvailableStudents().map((student) => (
                        <div key={student.maSinhVien} className="d-flex align-items-center mb-2 p-2 border-bottom">
                          <Form.Check
                            type="checkbox"
                            id={`add-${student.maSinhVien}`}
                            checked={selectedStudentsToAdd.includes(student.maSinhVien)}
                            onChange={(e) => handleAddStudentCheckboxChange(student.maSinhVien, e.target.checked)}
                            className="me-3"
                          />
                          <div className="flex-grow-1">
                            <div className="fw-semibold">{student.maSinhVien}</div>
                            <div className="text-muted small">{student.tenSinhVien}</div>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                  
                  <Button 
                    variant="success" 
                    onClick={handleAddStudents}
                    disabled={selectedStudentsToAdd.length === 0 || loading}
                    className="w-100 mt-3"
                  >
                    {loading ? <Spinner size="sm" /> : `Thêm ${selectedStudentsToAdd.length} sinh viên`}
                  </Button>
                </Card.Body>
              </Card>
            </Tab>
            
            <Tab 
              eventKey="remove-students" 
              title="Xóa sinh viên"
              disabled={!formData.maLopHocPhan || !lopHocPhans.some(lhp => lhp.maLopHocPhan === formData.maLopHocPhan)}
            >
              <Card>
                <Card.Header className="bg-danger text-white">
                  <h6 className="mb-0">Xóa sinh viên khỏi lớp</h6>
                </Card.Header>
                <Card.Body>
                  {/* Search */}
                  <Form.Group className="mb-3">
                    <Form.Label>Tìm kiếm sinh viên:</Form.Label>
                    <Form.Control
                      type="text"
                      placeholder="Nhập mã sinh viên hoặc tên..."
                      value={removeStudentSearchTerm}
                      onChange={(e) => setRemoveStudentSearchTerm(e.target.value)}
                    />
                  </Form.Group>
                  
                  {/* Select All/Deselect All buttons */}
                  <div className="d-flex gap-2 mb-3">
                    <Button variant="outline-danger" size="sm" onClick={handleSelectAllInClass}>
                      Chọn tất cả
                    </Button>
                    <Button variant="outline-secondary" size="sm" onClick={handleDeselectAllInClass}>
                      Bỏ chọn tất cả
                    </Button>
                    <Badge bg="danger">
                      Đã chọn: {selectedStudentsToRemove.length}
                    </Badge>
                  </div>
                  
                  {/* Student list with checkboxes */}
                  <div style={{ maxHeight: '300px', overflowY: 'auto', border: '1px solid #dee2e6', borderRadius: '0.375rem', padding: '10px' }}>
                    {getFilteredStudentsInClass().length === 0 ? (
                      <p className="text-muted text-center py-3 mb-0">
                        {removeStudentSearchTerm.trim() ? 'Không tìm thấy sinh viên nào' : 'Không có sinh viên nào trong lớp'}
                      </p>
                    ) : (
                      getFilteredStudentsInClass().map((student) => (
                        <div key={student.maSinhVien} className="d-flex align-items-center mb-2 p-2 border-bottom">
                          <Form.Check
                            type="checkbox"
                            id={`remove-${student.maSinhVien}`}
                            checked={selectedStudentsToRemove.includes(student.maSinhVien)}
                            onChange={(e) => handleRemoveStudentCheckboxChange(student.maSinhVien, e.target.checked)}
                            className="me-3"
                          />
                          <div className="flex-grow-1">
                            <div className="fw-semibold">{student.maSinhVien}</div>
                            <div className="text-muted small">{student.tenSinhVien}</div>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                  
                  <Button 
                    variant="danger" 
                    onClick={handleRemoveStudents}
                    disabled={selectedStudentsToRemove.length === 0 || loading}
                    className="w-100 mt-3"
                  >
                    {loading ? <Spinner size="sm" /> : `Xóa ${selectedStudentsToRemove.length} sinh viên`}
                  </Button>
                </Card.Body>
              </Card>
            </Tab>
          </Tabs>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleCloseModal}>
            Hủy
          </Button>
          <Button variant="primary" onClick={handleSubmit} disabled={loading}>
            {loading ? <Spinner size="sm" /> : 'Lưu'}
          </Button>
        </Modal.Footer>
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

      {/* Import CaHoc Modal */}
      <Modal show={showCaHocImportModal} onHide={() => setShowCaHocImportModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Import lịch học</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCaHocImportSubmit}>
          <Modal.Body>
            <Alert variant="info">
              <strong>Chọn file Excel (.xls/.xlsx).</strong>
            </Alert>
            {tkbUploading && (
              <div className="d-flex align-items-center mb-3">
                <Spinner animation="border" size="sm" className="me-2" />
                <span>Đang tải file...</span>
              </div>
            )}
            <Form.Group className="mb-3">
              <Form.Label>Chọn file Excel</Form.Label>
              <Form.Control
                type="file"
                accept=".xls,.xlsx"
                onChange={(e) => setCaHocImportFile(e.target.files[0])}
                required
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowCaHocImportModal(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" disabled={!caHocImportFile || tkbUploading}>
              {tkbUploading ? 'Đang tải...' : 'Import'}
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
