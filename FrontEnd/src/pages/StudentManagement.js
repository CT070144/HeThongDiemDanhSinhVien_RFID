import React, { useState, useEffect, useCallback } from 'react';
import { Container, Row, Col, Card, Table, Button, Form, Modal, Alert, Badge, ProgressBar, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { studentAPI, attendanceAPI } from '../services/api';
import api from '../services/api';
import * as XLSX from 'xlsx';

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
  const [showImportModal, setShowImportModal] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [scanning, setScanning] = useState(false);
  const [latestRfid, setLatestRfid] = useState(null);
  const [rfidStatus, setRfidStatus] = useState(null); // 'new', 'exists', 'current'

  useEffect(() => {
    loadStudents();
    loadLopHocPhans();
  }, []);

  // Effect để quét RFID khi modal mở
  useEffect(() => {
    if (!showModal || !scanning) return;
    
    let isFetching = false;
    const intervalId = setInterval(async () => {
      if (isFetching) return;
      isFetching = true;
      try {
        const response = await attendanceAPI.getUnprocessedRfids();
        const unprocessedRfids = response.data || [];
        
        // Tìm RFID mới nhất chưa được xử lý
        const latestUnprocessed = unprocessedRfids
          .filter(rfid => !rfid.processed)
          .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0];
        
        if (latestUnprocessed && latestUnprocessed.rfid !== latestRfid) {
          setLatestRfid(latestUnprocessed.rfid);
          
          // Kiểm tra xem RFID đã được đăng ký chưa
          try {
            const existingStudent = await studentAPI.getByRfid(latestUnprocessed.rfid);
            if (existingStudent && existingStudent.data) {
              // RFID đã được đăng ký
              setRfidStatus('exists');
              setFormData(prev => ({
                ...prev,
                rfid: latestUnprocessed.rfid
              }));
              toast.warning(`RFID ${latestUnprocessed.rfid} đã được đăng ký cho sinh viên: ${existingStudent.data.tenSinhVien} (${existingStudent.data.maSinhVien}). Hãy thử thẻ khác.`);
              // Không dừng quét khi RFID đã đăng ký, để người dùng có thể thử thẻ khác
            } else {
              // RFID chưa được đăng ký
              setRfidStatus('new');
              setFormData(prev => ({
                ...prev,
                rfid: latestUnprocessed.rfid
              }));
              toast.success(`Quét thấy RFID mới: ${latestUnprocessed.rfid}`);
              // Dừng quét khi tìm thấy RFID hợp lệ
              setScanning(false);
            }
          } catch (error) {
            // RFID chưa được đăng ký (lỗi 404)
            setRfidStatus('new');
            setFormData(prev => ({
              ...prev,
              rfid: latestUnprocessed.rfid
            }));
            toast.success(`Quét thấy RFID mới: ${latestUnprocessed.rfid}`);
            // Dừng quét khi tìm thấy RFID hợp lệ
            setScanning(false);
          }
        }
      } catch (error) {
        // Silent error handling
      } finally {
        isFetching = false;
      }
    }, 1000);
    
    return () => clearInterval(intervalId);
  }, [showModal, scanning, latestRfid]);

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
    
    // Reset RFID status khi người dùng nhập thủ công
    if (name === 'rfid') {
      setRfidStatus(null);
      setLatestRfid(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      // Kiểm tra RFID đã được đăng ký chưa (chỉ khi thêm mới hoặc thay đổi RFID)
      if (rfidStatus === 'exists' && (!editingStudent || formData.rfid !== editingStudent.rfid)) {
        toast.error('RFID này đã được đăng ký cho sinh viên khác. Vui lòng chọn RFID khác.');
        setLoading(false);
        return;
      }

      if (editingStudent) {
        // Sử dụng mã sinh viên làm khóa chính cho update
        await studentAPI.update(editingStudent.maSinhVien, formData);
        toast.success('Cập nhật sinh viên thành công');
      } else {
        await studentAPI.create(formData);
        toast.success('Thêm sinh viên thành công');
      }
      
      handleCloseModal();
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
    setLatestRfid(null);
    setRfidStatus('current'); // RFID hiện tại của sinh viên đang sửa
    setScanning(false);
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
    setLatestRfid(null);
    setRfidStatus(null);
    setScanning(false);
    setShowModal(true);
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      const allowedTypes = [
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      ];
      
      if (!allowedTypes.includes(file.type)) {
        toast.error('Dữ liệu trong file không đúng định dạng');
        return;
      }
      
      setImportFile(file);
    }
  };

  const parseExcelFile = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      
      reader.onload = (e) => {
        try {
          const data = new Uint8Array(e.target.result);
          const workbook = XLSX.read(data, { type: 'array' });
          const sheetName = workbook.SheetNames[0];
          const worksheet = workbook.Sheets[sheetName];
          const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 });
          console.log(jsonData);
          if (jsonData.length < 2) {
            reject(new Error('Dữ liệu trong file không đúng định dạng'));
            return;
          }
          
          // Lấy header
          const headers = jsonData[0];
          console.log('Headers found:', headers);
          
          // Tìm vị trí các cột với nhiều tùy chọn tên cột
          const maSinhVienIndex = headers.findIndex(h => {
            if (!h) return false;
            const header = h.toString().toLowerCase().trim();
            return header.includes('mã sinh viên') || 
                   header.includes('masinhvien') || 
                   header.includes('student id') ||
                   header.includes('ma_sinh_vien');
          });
          
          const tenSinhVienIndex = headers.findIndex(h => {
            if (!h) return false;
            const header = h.toString().toLowerCase().trim();
            return header.includes('họ và tên') || 
                   header.includes('hovaten') || 
                   header.includes('tên sinh viên') ||
                   header.includes('tensinhvien') ||
                   header.includes('full name') ||
                   header.includes('ho va ten') ||
                   header.includes('ten_sinh_vien');
          });
          
          const rfidIndex = headers.findIndex(h => {
            if (!h) return false;
            const header = h.toString().toLowerCase().trim();
            return header.includes('rfid');
          });
          
          console.log('Column indices:', { maSinhVienIndex, tenSinhVienIndex, rfidIndex });
          
          if (maSinhVienIndex === -1 || tenSinhVienIndex === -1 || rfidIndex === -1) {
            const missingColumns = [];
            if (maSinhVienIndex === -1) missingColumns.push('Mã sinh viên');
            if (tenSinhVienIndex === -1) missingColumns.push('Họ và tên');
            if (rfidIndex === -1) missingColumns.push('RFID');
            
            reject(new Error('Dữ liệu trong file không đúng định dạng'));
            return;
          }
          
          // Parse dữ liệu
          const students = [];
          for (let i = 1; i < jsonData.length; i++) {
            const row = jsonData[i];
            console.log(`Row ${i}:`, row);
            
            if (row && row[maSinhVienIndex] && row[tenSinhVienIndex] && row[rfidIndex]) {
              const student = {
                maSinhVien: row[maSinhVienIndex].toString().trim(),
                tenSinhVien: row[tenSinhVienIndex].toString().trim(),
                rfid: row[rfidIndex].toString().trim()
              };
              console.log(`Parsed student ${i}:`, student);
              students.push(student);
            } else {
              console.log(`Row ${i} skipped - missing data:`, {
                maSinhVien: row?.[maSinhVienIndex],
                tenSinhVien: row?.[tenSinhVienIndex],
                rfid: row?.[rfidIndex]
              });
            }
          }
          
          if (students.length === 0) {
            reject(new Error('Dữ liệu trong file không đúng định dạng'));
            return;
          }
          
          resolve(students);
        } catch (error) {
          reject(new Error('Dữ liệu trong file không đúng định dạng'));
        }
      };
      
      reader.onerror = () => {
        reject(new Error('Dữ liệu trong file không đúng định dạng'));
      };
      
      reader.readAsArrayBuffer(file);
    });
  };

  const handleImport = async () => {
    if (!importFile) {
      toast.error('Dữ liệu trong file không đúng định dạng');
      return;
    }
    
    setImporting(true);
    setImportResult(null);
    
    try {
      const students = await parseExcelFile(importFile);
      
      if (students.length === 0) {
        toast.error('Dữ liệu trong file không đúng định dạng');
        setImporting(false);
        return;
      }
      
      // Gửi dữ liệu lên server
      const response = await studentAPI.bulkUpdateRfid(students);
      const result = response.data;
      
      setImportResult(result);
      
      // Hiển thị kết quả
      if (result.successCount > 0) {
        toast.success(`Cập nhật thành công ${result.successCount} sinh viên`);
      }
      
      if (result.failureCount > 0) {
        toast.error(`${result.failureCount} sinh viên cập nhật thất bại`);
      }
      
      // Reload danh sách sinh viên
      loadStudents();
      
    } catch (error) {
      toast.error(error.response?.data || error.message || 'Dữ liệu trong file không đúng định dạng');
    } finally {
      setImporting(false);
    }
  };

  const handleCloseImportModal = () => {
    setShowImportModal(false);
    setImportFile(null);
    setImportResult(null);
    setImporting(false);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setScanning(false);
    setLatestRfid(null);
    setRfidStatus(null);
  };

  return (
    <Container>
      <style>{`
        @keyframes sweep {
          0% { left: -40%; }
          100% { left: 100%; }
        }
        @keyframes pulse {
          0% { transform: scale(1); opacity: 0.9; }
          70% { transform: scale(1.35); opacity: 0.2; }
          100% { transform: scale(1); opacity: 0.9; }
        }
        .scan-dot { 
          width: 8px; 
          height: 8px; 
          border-radius: 50%; 
          background: #0d6efd; 
          display: inline-block; 
          animation: pulse 1.2s infinite ease-in-out; 
        }
        .scan-dot.d2 { animation-delay: .2s; }
        .scan-dot.d3 { animation-delay: .4s; }
      `}</style>
      <Row>
        <Col>
          <Card>
            <Card.Header>
              <h3>Sinh viên</h3>
           
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
                  <Button 
                    variant="success" 
                    onClick={() => setShowImportModal(true)}
                    className="me-2"
                  >
                   Cập nhật RFID
                  </Button>
                  <Button variant="primary" onClick={handleAddNew}>
                    Thêm sinh viên mới
                  </Button>
                </Col>
              </Row>

              {selectedLopHocPhan && (
                <Row className="mb-3">
                  <Col>
                    <Alert variant="info">
                   <Badge bg="primary">
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
      <Modal show={showModal} onHide={handleCloseModal}>
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
  <Form.Label>RFID</Form.Label>

  {/* Gom input và nút vào cùng 1 hàng */}
  <div className="d-flex align-items-center gap-2">
    <Form.Control
      type="text"
      name="rfid"
      value={formData.rfid}
      onChange={handleInputChange}
      required
      placeholder="Nhập mã RFID hoặc quét từ thiết bị"
      readOnly={scanning}
      className={
        rfidStatus === 'exists'
          ? 'border-warning'
          : rfidStatus === 'new'
          ? 'border-success'
          : rfidStatus === 'current'
          ? 'border-info'
          : ''
      }
    />

    {/* Nút nằm bên phải input */}
    <Button
      style={{
        width: '160px', // mở rộng nút về bên trái
        height: '38px',
        position: 'relative',
        top: -5
      }}
      variant={scanning ? 'danger' : 'success'}
      onClick={() => setScanning(!scanning)}
      disabled={loading}
    >
      {scanning ? (
        <>
          <Spinner size="sm" className="me-1" />
          Dừng quét
        </>
      ) : (
        'Quét RFID'
      )}
    </Button>
  </div>

  {scanning && (
    <Alert variant="info" className="mt-2">
      <div className="d-flex align-items-center">
        <Spinner animation="border" size="sm" className="me-2" />
        <span className="me-3">
          Đang quét RFID... Hãy đưa thẻ RFID vào thiết bị
        </span>
        <div className="d-flex gap-1">
          <span className="scan-dot" />
          <span className="scan-dot d2" />
          <span className="scan-dot d3" />
        </div>
      </div>
      <div
        className="position-relative mt-2"
        style={{ height: 4, overflow: 'hidden', borderRadius: 2 }}
      >
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: '-40%',
            width: '40%',
            height: '100%',
            background:
              'linear-gradient(90deg, transparent, rgba(13,110,253,0.5), transparent)',
            animation: 'sweep 1.2s linear infinite',
          }}
        />
        <div
          style={{
            width: '100%',
            height: '100%',
            background:
              'repeating-linear-gradient(90deg, #e9ecef 0, #e9ecef 10px, #f8f9fa 10px, #f8f9fa 20px)',
          }}
        />
      </div>
    </Alert>
  )}
  
  {/* Hiển thị trạng thái RFID */}
  {rfidStatus === 'exists' && (
    <Alert variant="warning" className="mt-2">
      <div className="d-flex align-items-center justify-content-between">
        <div className="d-flex align-items-center">
          <i className="fas fa-exclamation-triangle me-2"></i>
          <span><strong>RFID đã được đăng ký!</strong> Thẻ này đã được sử dụng bởi sinh viên khác.</span>
        </div>
        <Button
          variant="outline-warning"
          size="sm"
          onClick={() => setScanning(false)}
        >
          Dừng quét
        </Button>
      </div>
    </Alert>
  )}
  
  {rfidStatus === 'new' && (
    <Alert variant="success" className="mt-2">
      <div className="d-flex align-items-center">
        <i className="fas fa-check-circle me-2"></i>
        <span><strong>Tìm thấy RFID mới !</strong></span>
      </div>
    </Alert>
  )}
  
 
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
            <Button variant="secondary" onClick={handleCloseModal}>
              Hủy
            </Button>
            <Button 
              variant="primary" 
              type="submit" 
              disabled={
                loading || 
                (rfidStatus === 'exists' && (!editingStudent || formData.rfid !== editingStudent.rfid))
              }
            >
              {loading ? 'Đang xử lý...' : (editingStudent ? 'Cập nhật' : 'Thêm')}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Modal import Excel */}
      <Modal show={showImportModal} onHide={handleCloseImportModal} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Import cập nhật RFID từ Excel</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Alert variant="info">
            <strong>Vui lòng chọn file muốn cập nhật RFID (Hỗ trợ định dạng .xls và .xlsx)</strong>
            
          </Alert>
          
          <Form.Group className="mb-3">
            <Form.Label>Chọn file Excel</Form.Label>
            <Form.Control
              type="file"
              accept=".xls,.xlsx"
              onChange={handleFileChange}
              disabled={importing}
            />
            {importFile && (
              <Form.Text className="text-muted">
                Đã chọn: {importFile.name}
              </Form.Text>
            )}
          </Form.Group>

          {importing && (
            <div className="mb-3">
              <ProgressBar animated now={100} />
              <small className="text-muted">Đang xử lý file Excel...</small>
            </div>
          )}

          {importResult && (
            <Alert variant={importResult.failureCount === 0 ? "success" : "warning"}>
              <h6>Kết quả import:</h6>
              <ul className="mb-2">
                <li><strong>Tổng số:</strong> {importResult.totalProcessed}</li>
                <li><strong>Thành công:</strong> {importResult.successCount}</li>
                <li><strong>Thất bại:</strong> {importResult.failureCount}</li>
              </ul>
              
              {importResult.errors && importResult.errors.length > 0 && (
                <div>
                  <strong>Chi tiết lỗi:</strong>
                  <ul className="mb-0 mt-1">
                    {importResult.errors.slice(0, 5).map((error, index) => (
                      <li key={index}><small>{error}</small></li>
                    ))}
                    {importResult.errors.length > 5 && (
                      <li><small>... và {importResult.errors.length - 5} lỗi khác</small></li>
                    )}
                  </ul>
                </div>
              )}
            </Alert>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleCloseImportModal} disabled={importing}>
            Đóng
          </Button>
          <Button 
            variant="primary" 
            onClick={handleImport} 
            disabled={!importFile || importing}
          >
            {importing ? 'Đang xử lý...' : 'Import'}
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
};

export default StudentManagement;
