import React, { useState, useEffect, useCallback } from 'react';
import { Container, Row, Col, Card, Table, Button, Form, Modal, Alert, Badge, ProgressBar, Spinner, InputGroup } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { studentAPI, attendanceAPI, phongBanAPI } from '../services/api';
import api from '../services/api';
import * as XLSX from 'xlsx';
import { FaUsers, FaPlus, FaUpload, FaSearch, FaEdit, FaTrash, FaIdCard, FaQrcode, FaCheckCircle, FaList } from 'react-icons/fa';

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
    tenSinhVien: '',
    maPhongBan: ''
  });
  const [loading, setLoading] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [scanning, setScanning] = useState(false);
  const [latestRfid, setLatestRfid] = useState(null);
  const [rfidStatus, setRfidStatus] = useState(null); // 'new', 'exists', 'current'
  const [isDraggingImport, setIsDraggingImport] = useState(false);
  const [phongBans, setPhongBans] = useState([]);
  const [showPhongBanManageModal, setShowPhongBanManageModal] = useState(false);
  const [phongBanForm, setPhongBanForm] = useState({
    maPhongBan: '',
    tenPhongBan: '',
    moTa: ''
  });
  const [editingPhongBan, setEditingPhongBan] = useState(null);
  const [savingPhongBan, setSavingPhongBan] = useState(false);
  const [showPhongBanModal, setShowPhongBanModal] = useState(false);
  const [phongBanSearch, setPhongBanSearch] = useState('');

  useEffect(() => {
    loadStudents();
    loadLopHocPhans();
    loadPhongBans();
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
              toast.warning(`RFID ${latestUnprocessed.rfid} đã được đăng ký cho nhân viên: ${existingStudent.data.tenSinhVien} (${existingStudent.data.maSinhVien}). Hãy thử thẻ khác.`);
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

  const loadPhongBans = async () => {
    try {
      const response = await phongBanAPI.getAll();
      const data = response.data || [];
      setPhongBans(data.sort((a, b) => (a.maPhongBan || '').localeCompare(b.maPhongBan || '')));
    } catch (error) {
      console.error('Error loading phong ban:', error);
      setPhongBans([]);
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
      toast.error('Lỗi khi tải danh sách nhân viên');
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
        toast.error('RFID này đã được đăng ký cho nhân viên khác. Vui lòng chọn RFID khác.');
        setLoading(false);
        return;
      }

      if (editingStudent) {
        // Sử dụng mã nhân viên làm khóa chính cho update
        await studentAPI.update(editingStudent.maSinhVien, formData);
        toast.success('Cập nhật nhân viên thành công');
      } else {
        await studentAPI.create(formData);
        toast.success('Thêm nhân viên thành công');
      }
      
      handleCloseModal();
      setFormData({ maSinhVien: '', rfid: '', tenSinhVien: '', maPhongBan: '' });
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
      tenSinhVien: student.tenSinhVien,
      maPhongBan: student.maPhongBan || ''
    });
    setLatestRfid(null);
    setRfidStatus('current'); // RFID hiện tại của nhân viên đang sửa
    setScanning(false);
    setShowModal(true);
  };

  const handleDelete = async (maSinhVien) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa nhân viên này?')) {
      try {
        await studentAPI.delete(maSinhVien);
        toast.success('Xóa nhân viên thành công');
        loadStudents();
      } catch (error) {
        toast.error('Lỗi khi xóa nhân viên! Ràng buộc liên quan');
      }
    }
  };

  const handleAddNew = () => {
    setEditingStudent(null);
    setFormData({ maSinhVien: '', rfid: '', tenSinhVien: '', maPhongBan: '' });
    setLatestRfid(null);
    setRfidStatus(null);
    setScanning(false);
    setShowModal(true);
  };

  const resetPhongBanForm = () => {
    setPhongBanForm({ maPhongBan: '', tenPhongBan: '', moTa: '' });
    setEditingPhongBan(null);
  };

  const openCreatePhongBanModal = () => {
    resetPhongBanForm();
    setShowPhongBanManageModal(true);
  };

  const handleEditPhongBan = (pb) => {
    setEditingPhongBan(pb.maPhongBan);
    setPhongBanForm({
      maPhongBan: pb.maPhongBan || '',
      tenPhongBan: pb.tenPhongBan || '',
      moTa: pb.moTa || ''
    });
    setShowPhongBanManageModal(true);
  };

  const handleSavePhongBan = async () => {
    if (!phongBanForm.maPhongBan.trim() || !phongBanForm.tenPhongBan.trim()) {
      toast.error('Vui lòng nhập mã phòng ban và tên phòng ban');
      return;
    }
    setSavingPhongBan(true);
    try {
      if (editingPhongBan) {
        await phongBanAPI.update(editingPhongBan, {
          tenPhongBan: phongBanForm.tenPhongBan.trim(),
          moTa: phongBanForm.moTa.trim()
        });
        toast.success('Cập nhật phòng ban thành công');
      } else {
        await phongBanAPI.create({
          maPhongBan: phongBanForm.maPhongBan.trim(),
          tenPhongBan: phongBanForm.tenPhongBan.trim(),
          moTa: phongBanForm.moTa.trim()
        });
        setFormData((prev) => ({ ...prev, maPhongBan: phongBanForm.maPhongBan.trim() }));
        toast.success('Thêm phòng ban thành công');
      }
      await loadPhongBans();
      resetPhongBanForm();
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Không thể lưu phòng ban');
    } finally {
      setSavingPhongBan(false);
    }
  };

  const handleDeletePhongBan = async (maPhongBan) => {
    if (!window.confirm(`Bạn có chắc muốn xóa phòng ban ${maPhongBan}?`)) {
      return;
    }
    try {
      await phongBanAPI.delete(maPhongBan);
      await loadPhongBans();
      if (formData.maPhongBan === maPhongBan) {
        setFormData((prev) => ({ ...prev, maPhongBan: '' }));
      }
      if (editingPhongBan === maPhongBan) {
        resetPhongBanForm();
      }
      toast.success('Xóa phòng ban thành công');
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Không thể xóa phòng ban');
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      const allowedTypes = [
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      ];
      
      if (!allowedTypes.includes(file.type) && !file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
        toast.error('Dữ liệu trong file không đúng định dạng');
        return;
      }
      
      setImportFile(file);
    }
  };

  const handleImportFileDrop = (e) => {
    e.preventDefault();
    setIsDraggingImport(false);
    const file = e.dataTransfer.files?.[0];
    if (file && (file.name.endsWith('.xlsx') || file.name.endsWith('.xls'))) {
      setImportFile(file);
    } else {
      toast.error('Vui lòng chọn file Excel (.xlsx hoặc .xls)');
    }
  };

  const handleImportFileDragOver = (e) => {
    e.preventDefault();
    setIsDraggingImport(true);
  };

  const handleImportFileDragLeave = (e) => {
    e.preventDefault();
    setIsDraggingImport(false);
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
            return header.includes('mã nhân viên') || 
                   header.includes('masinhvien') || 
                   header.includes('student id') ||
                   header.includes('ma_sinh_vien');
          });
          
          const tenSinhVienIndex = headers.findIndex(h => {
            if (!h) return false;
            const header = h.toString().toLowerCase().trim();
            return header.includes('họ và tên') || 
                   header.includes('hovaten') || 
                   header.includes('tên nhân viên') ||
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

          const maPhongBanIndex = headers.findIndex(h => {
            if (!h) return false;
            const header = h.toString().toLowerCase().trim();
            return header.includes('mã phòng ban') ||
                   header.includes('maphongban') ||
                   header.includes('ma phong ban') ||
                   header.includes('department code') ||
                   header.includes('phong ban');
          });
          
          console.log('Column indices:', { maSinhVienIndex, tenSinhVienIndex, rfidIndex, maPhongBanIndex });
          
          if (maSinhVienIndex === -1 || tenSinhVienIndex === -1 || rfidIndex === -1 || maPhongBanIndex === -1) {
            const missingColumns = [];
            if (maSinhVienIndex === -1) missingColumns.push('Mã nhân viên');
            if (tenSinhVienIndex === -1) missingColumns.push('Họ và tên');
            if (rfidIndex === -1) missingColumns.push('RFID');
            if (maPhongBanIndex === -1) missingColumns.push('Mã phòng ban');
            
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
                rfid: row[rfidIndex].toString().trim(),
                maPhongBan: row[maPhongBanIndex] ? row[maPhongBanIndex].toString().trim() : ''
              };
              console.log(`Parsed student ${i}:`, student);
              students.push(student);
            } else {
              console.log(`Row ${i} skipped - missing data:`, {
                maSinhVien: row?.[maSinhVienIndex],
                tenSinhVien: row?.[tenSinhVienIndex],
                rfid: row?.[rfidIndex],
                maPhongBan: row?.[maPhongBanIndex]
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
      
      // Tự tạo mới phòng ban nếu mã phòng ban trong file chưa tồn tại
      const currentPhongBanCodes = new Set(
        (phongBans || [])
          .map(pb => (pb.maPhongBan || '').trim())
          .filter(Boolean)
      );
      const importPhongBanCodes = Array.from(
        new Set(
          students
            .map(s => (s.maPhongBan || '').trim())
            .filter(Boolean)
        )
      );
      const missingPhongBanCodes = importPhongBanCodes.filter(code => !currentPhongBanCodes.has(code));

      for (const code of missingPhongBanCodes) {
        try {
          await phongBanAPI.create({
            maPhongBan: code,
            tenPhongBan: `Phòng ban ${code}`,
            moTa: 'Tạo tự động từ import Excel'
          });
        } catch (error) {
          const message = error?.response?.data?.message || error?.response?.data || '';
          if (!String(message).toLowerCase().includes('đã tồn tại')) {
            throw error;
          }
        }
      }
      if (missingPhongBanCodes.length > 0) {
        await loadPhongBans();
      }

      // Gửi dữ liệu lên server
      const response = await studentAPI.bulkUpdateRfid(students);
      const result = response.data;
      
      setImportResult(result);
      
      // Hiển thị kết quả
      if (result.successCount > 0) {
        const msg = missingPhongBanCodes.length > 0
          ? `Cập nhật thành công ${result.successCount} nhân viên, đã tạo ${missingPhongBanCodes.length} phòng ban mới`
          : `Cập nhật thành công ${result.successCount} nhân viên`;
        toast.success(msg);
      }
      
      if (result.failureCount > 0) {
        toast.error(`${result.failureCount} nhân viên cập nhật thất bại`);
      }
      
      // Reload danh sách nhân viên
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
    setIsDraggingImport(false);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setScanning(false);
    setLatestRfid(null);
    setRfidStatus(null);
  };

  const modalPrimaryStyle = {
    backgroundColor: '#212529',
    borderColor: '#212529',
    color: '#fff'
  };

  return (
    <Container
      className="py-4"
      style={{
        '--bs-primary': '#212529',
        '--bs-primary-rgb': '33, 37, 41'
      }}
    >
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
          background: #212529; 
          display: inline-block; 
          animation: pulse 1.2s infinite ease-in-out; 
        }
        .scan-dot.d2 { animation-delay: .2s; }
        .scan-dot.d3 { animation-delay: .4s; }
      `}</style>
      <Row>
        <Col>
          <Card className="shadow-sm" style={{ border: 'none' }}>
            <Card.Header className="bg-primary text-white d-flex justify-content-between align-items-center" style={{ border: 'none' }}>
              <h3 className="mb-0 d-flex align-items-center">
                <FaUsers className="me-2" />
                Thông tin nhân viên
              </h3>
              <div>
                <Button 
                  variant="light" 
                  onClick={() => setShowImportModal(true)}
                  className="me-2 shadow-sm"
                >
                  <FaUpload className="me-2" />
                  Cập nhật RFID
                </Button>
                <Button variant="light" onClick={handleAddNew} className="shadow-sm">
                  <FaPlus className="me-2" />
                  Thêm mới
                </Button>
              </div>
            </Card.Header>
            <Card.Body className="p-4">
              {selectedLopHocPhan && (
                <Alert variant="info" className="mb-4 shadow-sm" style={{ border: '1px solid #0dcaf0' }}>
                  <Badge bg="primary" className="me-2">
                    {lopHocPhans.find(l => l.maLopHocPhan === selectedLopHocPhan)?.tenLopHocPhan}
                  </Badge>
                </Alert>
              )}

              {/* Search Form */}
              <Card className="mb-4 shadow-sm" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
                <Card.Body className="p-3">
                  <Form.Group className="mb-0">
                    <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                      <FaSearch className="me-2 text-primary" />
                      Tìm kiếm
                    </Form.Label>
                    <Form.Control
                      type="text"
                      placeholder="Tìm kiếm theo mã nhân viên hoặc tên..."
                      value={searchKeyword}
                      onChange={(e) => setSearchKeyword(e.target.value)}
                      className="shadow-sm"
                      style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                    />
                  </Form.Group>
                </Card.Body>
              </Card>

              {/* Table */}
              <div className="table-responsive">
                <Table responsive striped hover className="mb-0" style={{ fontSize: '0.95rem' }}>
                  <thead className="table-primary">
                    <tr>
                      <th style={{textAlign:'left', fontWeight: '600'}}>Mã nhân viên</th>
                      <th style={{textAlign:'left', fontWeight: '600'}}>RFID</th>
                      <th style={{textAlign:'left', fontWeight: '600'}}>Tên nhân viên</th>
                      <th style={{textAlign:'left', fontWeight: '600'}}>Mã phòng ban</th>
                      <th style={{textAlign:'left', fontWeight: '600'}}>Ngày tạo</th>
                      <th style={{textAlign:'center', fontWeight: '600'}}>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredStudents
                      .slice((page - 1) * pageSize, page * pageSize)
                      .map((student) => (
                      <tr key={student.maSinhVien} style={{ verticalAlign: 'middle' }}>
                        <td style={{textAlign:'left'}}>
                          <Badge bg="primary" style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                            {student.maSinhVien}
                          </Badge>
                        </td>
                        <td style={{textAlign:'left'}}>
                          <Badge bg="info" style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                            <FaQrcode className="me-1" />
                            {student.rfid}
                          </Badge>
                        </td>
                        <td style={{textAlign:'left', fontWeight: '500'}}>{student.tenSinhVien}</td>
                        <td style={{textAlign:'left'}}>
                          {student.maPhongBan ? (
                            <Badge bg="secondary">{student.maPhongBan}</Badge>
                          ) : (
                            <span className="text-muted">-</span>
                          )}
                        </td>
                        <td style={{textAlign:'left'}}>{new Date(student.createdAt).toLocaleDateString('vi-VN')}</td>
                        <td style={{textAlign:'center'}}>
                          <div className="d-flex gap-2 justify-content-center">
                            <Button
                              variant="warning"
                              size="sm"
                              onClick={() => handleEdit(student)}
                              className="shadow-sm"
                            >
                              <FaEdit className="me-1" />
                              Sửa
                            </Button>
                            <Button
                              variant="danger"
                              size="sm"
                              onClick={() => handleDelete(student.maSinhVien)}
                              className="shadow-sm"
                            >
                              <FaTrash className="me-1" />
                              Xóa
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </div>

              {filteredStudents.length === 0 && (
                <div className="text-center py-5">
                  <FaUsers size={64} className="text-muted mb-3" />
                  <Alert variant="info" className="d-inline-block">
                    Không có nhân viên nào được tìm thấy.
                  </Alert>
                </div>
              )}

              {filteredStudents.length > 0 && (
                <div className="d-flex justify-content-between align-items-center mt-4 pt-3 border-top">
                  <div className="text-muted fw-semibold">
                    Trang <span className="text-primary">{page}</span>
                  </div>
                  <div className="d-flex gap-2">
                    <Button
                      variant="outline-secondary"
                      disabled={page === 1}
                      onClick={() => setPage((p) => Math.max(1, p - 1))}
                      className="shadow-sm"
                    >
                      Trước
                    </Button>
                    <Button
                      variant="outline-secondary"
                      disabled={filteredStudents.length <= page * pageSize}
                      onClick={() => setPage((p) => p + 1)}
                      className="shadow-sm"
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

      {/* Modal thêm/sửa nhân viên */}
      <Modal show={showModal} onHide={handleCloseModal} size="lg">
        <Modal.Header closeButton className="text-white" style={{ backgroundColor: '#212529' }}>
          <Modal.Title className="d-flex align-items-center">
            <FaUsers className="me-2" />
            {editingStudent ? 'Sửa nhân viên' : 'Thêm nhân viên mới'}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold d-flex align-items-center">
                <FaIdCard className="me-2" style={{ color: '#212529' }} />
                Mã nhân viên
              </Form.Label>
              <Form.Control
                type="text"
                name="maSinhVien"
                value={formData.maSinhVien}
                onChange={handleInputChange}
                required
                disabled={editingStudent ? true : false}
                placeholder="Nhập mã nhân viên (VD: CT070201)"
                className="shadow-sm"
                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold d-flex align-items-center">
                <FaQrcode className="me-2" style={{ color: '#212529' }} />
                RFID
              </Form.Label>

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
                'linear-gradient(90deg, transparent, rgba(33,37,41,0.5), transparent)',
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
          <span><strong>RFID đã được đăng ký!</strong> Thẻ này đã được sử dụng bởi nhân viên khác.</span>
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
              <Form.Label className="fw-semibold d-flex align-items-center">
                <FaUsers className="me-2" style={{ color: '#212529' }} />
                Tên nhân viên
              </Form.Label>
              <Form.Control
                type="text"
                name="tenSinhVien"
                value={formData.tenSinhVien}
                onChange={handleInputChange}
                required
                placeholder="Nhập họ và tên nhân viên"
                className="shadow-sm"
                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Phòng ban</Form.Label>
              <InputGroup>
                <Form.Control
                  type="text"
                  readOnly
                  value={
                    formData.maPhongBan
                      ? `${formData.maPhongBan} - ${phongBans.find(pb => pb.maPhongBan === formData.maPhongBan)?.tenPhongBan || ''}`
                      : ''
                  }
                  placeholder="Chưa chọn phòng ban"
                  className="shadow-sm"
                  style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                />
                <Button
                  variant="primary"
                  type="button"
                  size="sm"
                  position="relative"
                  style={{top:"-10px"}}
                  onClick={() => setShowPhongBanModal(true)}
                  title="Chọn phòng ban"
                >
                  <FaList />
                </Button>
                <Button
                  variant="success"
                  type="button"
                  size="sm"
                  position="relative"
                  style={{top:"-10px"}}
                  onClick={openCreatePhongBanModal}
                  title="Quản lý phòng ban"
                >
                  <FaPlus />
                </Button>
              </InputGroup>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={handleCloseModal}>
              Hủy
            </Button>
            <Button 
              variant="primary"
              style={modalPrimaryStyle}
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
        <Modal.Header closeButton className="text-white" style={{ backgroundColor: '#212529' }}>
          <Modal.Title className="d-flex align-items-center">
            <FaUpload className="me-2" />
            Import cập nhật RFID từ Excel
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Alert variant="info">
            <strong>Vui lòng chọn file muốn cập nhật RFID (Hỗ trợ định dạng .xls và .xlsx)</strong>
            
          </Alert>
          
          <Form.Group className="mb-3">
            <Form.Label className="fw-semibold d-flex align-items-center">
              <FaUpload className="me-2" style={{ color: '#212529' }} />
              Chọn file Excel
            </Form.Label>
            <div
              onDrop={handleImportFileDrop}
              onDragOver={handleImportFileDragOver}
              onDragLeave={handleImportFileDragLeave}
              style={{
                border: `2px dashed ${isDraggingImport ? '#212529' : '#dee2e6'}`,
                borderRadius: '0.5rem',
                padding: '2rem',
                textAlign: 'center',
                backgroundColor: importFile ? '#e7f3ff' : isDraggingImport ? '#f0f7ff' : '#f8f9fa',
                cursor: 'pointer',
                transition: 'all 0.3s ease'
              }}
              onClick={() => document.getElementById('student-import-file-input').click()}
            >
              <input
                id="student-import-file-input"
                type="file"
                accept=".xlsx,.xls"
                onChange={handleFileChange}
                disabled={importing}
                style={{ display: 'none' }}
              />
              {importFile ? (
                <div>
                  <FaCheckCircle className="text-success mb-2" size={32} />
                  <p className="mb-1 fw-semibold text-success">{importFile.name}</p>
                  <p className="text-muted small mb-2">Click để chọn file khác</p>
                  <Button variant="outline-danger" size="sm" onClick={(e) => { e.stopPropagation(); setImportFile(null); }} disabled={importing}>
                    <FaTrash className="me-1" />
                    Xóa file
                  </Button>
                </div>
              ) : (
                <div>
                  <FaUpload className="mb-2" size={32} style={{ color: '#212529' }} />
                  <p className="mb-1 fw-semibold">Kéo thả file Excel vào đây hoặc click để chọn</p>
                  <p className="text-muted small">Hỗ trợ file .xlsx, .xls</p>
                </div>
              )}
            </div>
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
            style={modalPrimaryStyle}
            onClick={handleImport} 
            disabled={!importFile || importing}
          >
            {importing ? 'Đang xử lý...' : 'Import'}
          </Button>
        </Modal.Footer>
      </Modal>

      <Modal
        show={showPhongBanModal}
        onHide={() => setShowPhongBanModal(false)}
        size="lg"
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>Chọn phòng ban</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Group className="mb-3">
            <Form.Label>Tìm kiếm theo mã hoặc tên phòng ban</Form.Label>
            <Form.Control
              type="text"
              placeholder="Nhập từ khóa..."
              value={phongBanSearch}
              onChange={(e) => setPhongBanSearch(e.target.value)}
            />
          </Form.Group>
          <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
            <Table hover responsive size="sm" className="align-middle">
              <thead>
                <tr>
                  <th style={{ width: '6%' }} />
                  <th>Mã phòng ban</th>
                  <th>Tên phòng ban</th>
                  <th>Mô tả</th>
                </tr>
              </thead>
              <tbody>
                {phongBans
                  .filter((pb) => {
                    if (!phongBanSearch.trim()) return true;
                    const keyword = phongBanSearch.toLowerCase();
                    return (
                      (pb.maPhongBan || '').toLowerCase().includes(keyword) ||
                      (pb.tenPhongBan || '').toLowerCase().includes(keyword)
                    );
                  })
                  .map((pb) => (
                    <tr
                      key={pb.maPhongBan}
                      style={{ cursor: 'pointer' }}
                      onClick={() => {
                        setFormData((prev) => ({ ...prev, maPhongBan: pb.maPhongBan }));
                        setShowPhongBanModal(false);
                      }}
                    >
                      <td>
                        <Form.Check
                          type="radio"
                          name="phongBanRadio"
                          checked={formData.maPhongBan === pb.maPhongBan}
                          readOnly
                        />
                      </td>
                      <td>
                        <Badge bg="secondary">{pb.maPhongBan}</Badge>
                      </td>
                      <td>{pb.tenPhongBan}</td>
                      <td>{pb.moTa || <span className="text-muted">-</span>}</td>
                    </tr>
                  ))}
                {phongBans.length === 0 && (
                  <tr>
                    <td colSpan={4} className="text-center text-muted py-3">
                      Chưa có phòng ban nào
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="outline-secondary"
            onClick={() => {
              setFormData((prev) => ({ ...prev, maPhongBan: '' }));
              setShowPhongBanModal(false);
            }}
          >
            Xóa lựa chọn
          </Button>
          <Button variant="secondary" onClick={() => setShowPhongBanModal(false)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>

      <Modal
        show={showPhongBanManageModal}
        onHide={() => {
          setShowPhongBanManageModal(false);
          resetPhongBanForm();
        }}
        size="lg"
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>{editingPhongBan ? 'Sửa phòng ban' : 'Thêm phòng ban'}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Row className="g-2">
            <Col md={4}>
              <Form.Control
                placeholder="Mã phòng ban"
                value={phongBanForm.maPhongBan}
                disabled={!!editingPhongBan}
                onChange={(e) => setPhongBanForm((prev) => ({ ...prev, maPhongBan: e.target.value }))}
              />
            </Col>
            <Col md={4}>
              <Form.Control
                placeholder="Tên phòng ban"
                value={phongBanForm.tenPhongBan}
                onChange={(e) => setPhongBanForm((prev) => ({ ...prev, tenPhongBan: e.target.value }))}
              />
            </Col>
            <Col md={4}>
              <Form.Control
                placeholder="Mô tả (tuỳ chọn)"
                value={phongBanForm.moTa}
                onChange={(e) => setPhongBanForm((prev) => ({ ...prev, moTa: e.target.value }))}
              />
            </Col>
          </Row>
          <div className="mt-3 d-flex gap-2 justify-content-end">
            {editingPhongBan && (
              <Button variant="outline-secondary" onClick={resetPhongBanForm} disabled={savingPhongBan}>
                Tạo mới
              </Button>
            )}
            <Button variant="success" onClick={handleSavePhongBan} disabled={savingPhongBan}>
              {savingPhongBan ? 'Đang lưu...' : (editingPhongBan ? 'Cập nhật' : 'Thêm phòng ban')}
            </Button>
          </div>

          <hr />
          <div style={{ maxHeight: '280px', overflowY: 'auto' }}>
            <Table hover responsive size="sm" className="align-middle">
              <thead>
                <tr>
                  <th>Mã phòng ban</th>
                  <th>Tên phòng ban</th>
                  <th>Mô tả</th>
                  <th style={{ width: '120px' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {phongBans.map((pb) => (
                  <tr key={pb.maPhongBan}>
                    <td><Badge bg="secondary">{pb.maPhongBan}</Badge></td>
                    <td>{pb.tenPhongBan}</td>
                    <td>{pb.moTa || <span className="text-muted">-</span>}</td>
                    <td>
                      <div className="d-flex gap-1">
                        <Button size="sm" variant="warning" onClick={() => handleEditPhongBan(pb)}>
                          <FaEdit />
                        </Button>
                        <Button size="sm" variant="danger" onClick={() => handleDeletePhongBan(pb.maPhongBan)}>
                          <FaTrash />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
                {phongBans.length === 0 && (
                  <tr>
                    <td colSpan={4} className="text-center text-muted py-3">
                      Chưa có phòng ban nào
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          </div>
        </Modal.Body>
      </Modal>
    </Container>
  );
};

export default StudentManagement;
