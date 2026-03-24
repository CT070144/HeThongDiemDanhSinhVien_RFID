import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Alert, Badge, Modal, Form, Tabs, Tab, Spinner, Pagination } from 'react-bootstrap';
import { attendanceAPI, studentAPI, deviceAPI, roomAPI, caLamAPI } from '../services/api';
import { useNotification } from '../contexts/NotificationContext';
import { FaQrcode,FaClock, FaCog, FaPlay, FaStop, FaCopy, FaTrash, FaFilter, FaDesktop, FaDoorOpen, FaCheckCircle, FaExclamationTriangle, FaBuilding, FaPlus, FaEdit, FaSearch, FaEye, FaEyeSlash, FaKey, FaInfo } from 'react-icons/fa';

const SettingsPage = () => {
  const { notify } = useNotification();
  const [unprocessedRfids, setUnprocessedRfids] = useState([]);
  const [page, setPage] = useState(1);
  const pageSize = 8;
  const [showModal, setShowModal] = useState(false);
  const [scannedInfo, setScannedInfo] = useState({ rfid: '', name: '', maSinhVien: '', status: '' });
  const [polling, setPolling] = useState(false);
  const [devices, setDevices] = useState([]);
  const [newDevice, setNewDevice] = useState({ maThietBi: '', phongHoc: '' });
  const [statusFilter, setStatusFilter] = useState('all');
  const [rooms, setRooms] = useState([]);
  const [roomKeyword, setRoomKeyword] = useState('');
  const [roomPage, setRoomPage] = useState(0);
  const [roomSize, setRoomSize] = useState(10);
  const [roomTotalPages, setRoomTotalPages] = useState(0);
  const [roomTotalElements, setRoomTotalElements] = useState(0);
  const [editingRoom, setEditingRoom] = useState(null);
  const [roomForm, setRoomForm] = useState({
    maPhong: '',
    tenPhong: '',
    toaNha: '',
    tang: '',
    sucChua: '',
    loaiPhong: '',
    trangThai: 'active',
  });
  // Device details modal state
  const [showDeviceDetailsModal, setShowDeviceDetailsModal] = useState(false);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [deviceApiKeys, setDeviceApiKeys] = useState([]);
  const [visibleApiKeys, setVisibleApiKeys] = useState(new Set());
  const [showNewApiKeyAlert, setShowNewApiKeyAlert] = useState(null);

  // Ca làm (shift) management
  const [caLams, setCaLams] = useState([]);
  const [caLoading, setCaLoading] = useState(false);
  const [editingCa, setEditingCa] = useState(null);
  const [caForm, setCaForm] = useState({
    maCa: '',
    tenCa: '',
    gioBatDau: '',
    gioKetThuc: '',
    choPhepTrePhut: '',
  });

  useEffect(() => {
    loadUnprocessedRfids();
    loadDevices();
    loadRooms();
    loadCaLams();
  }, []);

  useEffect(() => {
    loadRooms();
  }, [roomPage, roomSize]);

  useEffect(() => {
    if (!polling) return;
    let isFetching = false;
    const intervalId = setInterval(async () => {
      if (isFetching) return;
      isFetching = true;
      try {
        await loadUnprocessedRfids();
      } catch (e) {
      } finally {
        isFetching = false;
      }
    }, 1000);
    return () => clearInterval(intervalId);
  }, [polling]);

  const loadUnprocessedRfids = async () => {
    try {
      const response = await attendanceAPI.getUnprocessedRfids();
      setUnprocessedRfids(response.data);
    } catch (error) {
      // silent
    }
  };

  const loadDevices = async () => {
    try {
      const res = await deviceAPI.getAll();
      setDevices(res.data || []);
    } catch (e) {}
  };

  const handleDeleteUnregistered = async (id) => {
    try {
      // reuse markProcessed as delete not provided; ideally have delete API
      // For now, mark processed as a way to hide from list
      await attendanceAPI.markProcessed(id);
      await loadUnprocessedRfids();
      notify.success('Đã xóa RFID chưa đăng ký');
    } catch (e) {
      notify.error('Thao tác thất bại');
    }
  };

  const handleScanRfid = async (rfid) => {
    try {
      const existsRes = await studentAPI.getByRfid(rfid);
      const found = existsRes && existsRes.data && existsRes.status === 200;
      const name = found ? existsRes.data.tenSinhVien : '';
      const maSinhVien = found ? existsRes.data.maSinhVien : '';
      setScannedInfo({ rfid, name, maSinhVien, status: found ? 'found' : 'not_found' });
      setShowModal(true);
    } catch (e) {
      setScannedInfo({ rfid, name: '', maSinhVien: '', status: 'not_found' });
      setShowModal(true);
    }
  };

  const handleCreateDevice = async (e) => {
    e.preventDefault();
    if (!newDevice.maThietBi || !newDevice.phongHoc) {
      notify.error('Vui lòng nhập đủ Mã thiết bị và vị trí');
      return;
    }
    try {
      const res = await deviceAPI.create({ maThietBi: newDevice.maThietBi, phongHoc: newDevice.phongHoc });
      notify.success('Đã tạo thiết bị');
      // Show API key alert
      if (res.data.apiKey) {
        setShowNewApiKeyAlert(res.data.apiKey);
        setTimeout(() => setShowNewApiKeyAlert(null), 10000); // Auto close after 10 seconds
      }
      setNewDevice({ maThietBi: '', phongHoc: '' });
      loadDevices();
    } catch (e) {
      notify.error('Không thể tạo thiết bị');
    }
  };

  const loadRooms = async (keyword) => {
    try {
      const keywordToUse = keyword !== undefined ? keyword : roomKeyword;
      const res = await roomAPI.getPaged(roomPage, roomSize, keywordToUse);
      setRooms(res.data.content || []);
      setRoomTotalPages(res.data.totalPages || 0);
      setRoomTotalElements(res.data.totalElements || 0);
    } catch (e) {
      // silent
    }
  };

  // Device management functions
  const handleViewDeviceDetails = async (device) => {
    setSelectedDevice(device);
    setShowDeviceDetailsModal(true);
    setVisibleApiKeys(new Set());
    try {
      const res = await deviceAPI.getApiKeys(device.maThietBi);
      setDeviceApiKeys(res.data || []);
    } catch (e) {
      notify.error('Không thể tải API keys');
    }
  };

  const handleToggleDeviceStatus = async (maThietBi) => {
    try {
      const currentDevice = devices.find(d => d.maThietBi === maThietBi) || selectedDevice;
      const res = currentDevice?.active
        ? await deviceAPI.deactivate(maThietBi)
        : await deviceAPI.activate(maThietBi);
      const updated = res.data;
      setSelectedDevice(updated);
      setDevices(devices.map(d => d.maThietBi === maThietBi ? updated : d));

      // Khi trạng thái thiết bị đổi, backend có thể đã đổi trạng thái API key.
      // Refresh lại danh sách API key để đồng bộ đúng dữ liệu hiển thị.
      if (showDeviceDetailsModal && selectedDevice?.maThietBi === maThietBi) {
        try {
          const apiKeysRes = await deviceAPI.getApiKeys(maThietBi);
          setDeviceApiKeys(apiKeysRes.data || []);
        } catch (apiKeyError) {
          notify.error('Đã đổi trạng thái thiết bị nhưng không tải được danh sách API key mới');
        }
      }

      notify.success(updated.active ? 'Thiết bị đã kích hoạt' : 'Thiết bị đã vô hiệu hóa');
    } catch (e) {
      notify.error('Không thể thay đổi trạng thái thiết bị');
    }
  };

  const handleCreateNewApiKey = async () => {
    if (!selectedDevice) return;
    try {
      const res = await deviceAPI.createApiKey(selectedDevice.maThietBi, { 
        moTa: `API key - ${new Date().toLocaleString('vi-VN')}` 
      });
      setDeviceApiKeys([...deviceApiKeys, res.data.apiKey]);
      setShowNewApiKeyAlert(res.data.keyValue);
      notify.success('Đã tạo API key mới');
      setTimeout(() => setShowNewApiKeyAlert(null), 10000);
    } catch (e) {
      notify.error('Không thể tạo API key mới');
    }
  };

  const handleToggleApiKeyVisibility = (keyId) => {
    const newVisible = new Set(visibleApiKeys);
    if (newVisible.has(keyId)) {
      newVisible.delete(keyId);
    } else {
      newVisible.add(keyId);
    }
    setVisibleApiKeys(newVisible);
  };

  const handleToggleApiKeyStatus = async (keyId) => {
    try {
      const currentKey = deviceApiKeys.find(k => k.id === keyId);
      if (!currentKey) {
        notify.error('Không tìm thấy API key');
        return;
      }
      if (currentKey.active) {
        await deviceAPI.deactivateApiKey(keyId);
      } else {
        await deviceAPI.activateApiKey(keyId);
      }
      const updated = deviceApiKeys.map(k => 
        k.id === keyId ? { ...k, active: !k.active } : k
      );
      setDeviceApiKeys(updated);
      notify.success('Trạng thái API key đã cập nhật');
    } catch (e) {
      notify.error('Không thể cập nhật API key');
    }
  };

  const handleDeleteApiKey = async (keyId) => {
    if (!window.confirm('Bạn có chắc muốn xóa API key này?')) return;
    try {
      await deviceAPI.deleteApiKey(keyId);
      setDeviceApiKeys(deviceApiKeys.filter(k => k.id !== keyId));
      notify.success('API key đã xóa');
    } catch (e) {
      notify.error('Không thể xóa API key');
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    notify.success('Đã sao chép vào clipboard');
  };

  const resetRoomForm = () => {
    setEditingRoom(null);
    setRoomForm({
      maPhong: '',
      tenPhong: '',
      toaNha: '',
      tang: '',
      sucChua: '',
      loaiPhong: '',
      trangThai: 'active',
    });
  };

  const resetCaForm = () => {
    setEditingCa(null);
    setCaForm({
      maCa: '',
      tenCa: '',
      gioBatDau: '',
      gioKetThuc: '',
      choPhepTrePhut: '',
    });
  };

  const loadCaLams = async () => {
    try {
      setCaLoading(true);
      const res = await caLamAPI.getAll();
      setCaLams(res.data || []);
    } catch (e) {
      // silent
    } finally {
      setCaLoading(false);
    }
  };

  const handleEditCaLam = (ca) => {
    if (!ca) return;
    setEditingCa(ca.maCa);
    setCaForm({
      maCa: ca.maCa ?? '',
      tenCa: ca.tenCa ?? '',
      gioBatDau: typeof ca.gioBatDau === 'string' ? ca.gioBatDau.substring(0, 5) : ca.gioBatDau,
      gioKetThuc: typeof ca.gioKetThuc === 'string' ? ca.gioKetThuc.substring(0, 5) : ca.gioKetThuc,
      choPhepTrePhut: ca.choPhepTrePhut ?? '',
    });
  };

  const handleDeleteCaLam = async (maCa) => {
    if (!window.confirm('Bạn có chắc muốn xóa ca làm này?')) return;
    try {
      await caLamAPI.delete(maCa);
      notify.success('Đã xóa ca làm');
      if (editingCa === maCa) resetCaForm();
      await loadCaLams();
    } catch (e) {
      notify.error('Không thể xóa ca làm');
    }
  };

  const handleSubmitCaLam = async (e) => {
    e.preventDefault();

    const maCaNum = Number(caForm.maCa);
    const choPhepTrePhutNum = Number(caForm.choPhepTrePhut);

    if (!maCaNum || Number.isNaN(maCaNum)) {
      notify.error('Vui lòng nhập `mã ca` hợp lệ');
      return;
    }
    if (!caForm.tenCa || !caForm.tenCa.trim()) {
      notify.error('Vui lòng nhập `tên ca`');
      return;
    }
    if (!caForm.gioBatDau || !caForm.gioKetThuc) {
      notify.error('Vui lòng nhập `giờ bắt đầu` và `giờ kết thúc`');
      return;
    }
    if (Number.isNaN(choPhepTrePhutNum) || choPhepTrePhutNum < 0) {
      notify.error('Vui lòng nhập `phút được phép trễ` hợp lệ');
      return;
    }

    const payload = {
      maCa: maCaNum,
      tenCa: caForm.tenCa.trim(),
      gioBatDau: caForm.gioBatDau,
      gioKetThuc: caForm.gioKetThuc,
      choPhepTrePhut: choPhepTrePhutNum,
    };

    try {
      if (editingCa !== null) {
        await caLamAPI.update(editingCa, payload);
        notify.success('Cập nhật ca làm thành công');
      } else {
        await caLamAPI.create(payload);
        notify.success('Tạo ca làm thành công');
      }
      resetCaForm();
      await loadCaLams();
    } catch (e) {
      const msg = e.response?.data?.message || 'Lưu ca làm thất bại';
      notify.error(msg);
    }
  };

  const handleSaveRoom = async (e) => {
    e.preventDefault();
    if (!roomForm.maPhong) {
      notify.error('Vui lòng nhập Mã vị trí');
      return;
    }
    try {
      const payload = {
        ...roomForm,
        tang: roomForm.tang !== '' ? Number(roomForm.tang) : null,
        sucChua: roomForm.sucChua !== '' ? Number(roomForm.sucChua) : null,
      };
      if (editingRoom) {
        await roomAPI.update(editingRoom, payload);
        notify.success('Cập nhật vị trí thành công');
      } else {
        await roomAPI.create(payload);
        notify.success('Tạo vị trí thành công');
      }
      resetRoomForm();
      loadRooms(roomKeyword);
    } catch (e) {
      const msg = e.response?.data?.message || 'Lưu vị trí thất bại';
      notify.error(msg);
    }
  };

  const handleEditRoom = (room) => {
    setEditingRoom(room.maPhong);
    setRoomForm({
      maPhong: room.maPhong || '',
      tenPhong: room.tenPhong || '',
      toaNha: room.toaNha || '',
      tang: room.tang ?? '',
      sucChua: room.sucChua ?? '',
      loaiPhong: room.loaiPhong || '',
      trangThai: room.trangThai || 'active',
    });
  };

  const handleDeleteRoom = async (maPhong) => {
    if (!window.confirm('Xóa vị trí này?')) return;
    try {
      await roomAPI.delete(maPhong);
      notify.success('Đã xóa vị trí');
      if (editingRoom === maPhong) resetRoomForm();
      loadRooms(roomKeyword);
    } catch (e) {
      const msg = e.response?.data?.message || 'Xóa vị trí thất bại';
      notify.error(msg);
    }
  };

  // Helper function to generate pagination items with ellipsis
  const getPaginationItems = (currentPage, totalPages) => {
    if (totalPages <= 1) {
      return [0];
    }
    
    const items = [];
    const delta = 2; // Number of pages to show on each side of current page
    
    // If total pages is small, show all pages
    if (totalPages <= 7) {
      for (let i = 0; i < totalPages; i++) {
        items.push(i);
      }
      return items;
    }
    
    // Always show first page
    items.push(0);
    
    // Calculate start and end of page range around current page
    let start = Math.max(1, currentPage - delta);
    let end = Math.min(totalPages - 2, currentPage + delta);
    
    // Add ellipsis after first page if there's a gap
    if (start > 2) {
      items.push('ellipsis-start');
    } else if (start === 2) {
      // If start is 2, show page 1 instead of ellipsis
      items.push(1);
    }
    
    // Add pages in range (skip if already added)
    for (let i = start; i <= end; i++) {
      if (i !== 0 && i !== totalPages - 1 && !items.includes(i)) {
        items.push(i);
      }
    }
    
    // Add ellipsis before last page if there's a gap
    if (end < totalPages - 3) {
      items.push('ellipsis-end');
    } else if (end === totalPages - 3 && !items.includes(totalPages - 2)) {
      // If end is totalPages - 3, show page totalPages - 2 instead of ellipsis
      items.push(totalPages - 2);
    }
    
    // Always show last page
    items.push(totalPages - 1);
    
    return items;
  };

  const filteredRfids = unprocessedRfids.filter(item => {
    if (statusFilter === 'registered') return !!item.processed;
    if (statusFilter === 'unregistered') return !item.processed;
    return true;
  });

  return (
    <Container
      fluid
      className="py-4 configuration-theme"
      style={{
        '--bs-primary': '#212529',
        '--bs-primary-rgb': '33, 37, 41'
      }}
    >
      <Row>
        <Col>
          <Card className="shadow-sm mb-4" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
            <Card.Header className="bg-primary text-white d-flex align-items-center" style={{ border: 'none' }}>
              <FaCog className="me-2" size={24} />
              <h3 className="mb-0">Cài đặt hệ thống</h3>
            </Card.Header>
          </Card>
          <style>{`
            .configuration-theme .btn-primary {
              background-color: #212529 !important;
              border-color: #212529 !important;
            }
            .configuration-theme .btn-outline-primary {
              color: #212529 !important;
              border-color: #212529 !important;
            }
            .configuration-theme .btn-outline-primary:hover,
            .configuration-theme .btn-outline-primary:focus,
            .configuration-theme .btn-outline-primary:active {
              background-color: #212529 !important;
              border-color: #212529 !important;
              color: #fff !important;
            }
            .configuration-theme .nav-tabs .nav-link {
              color: #212529 !important;
            }
            .configuration-theme .nav-tabs .nav-link.active {
              color: #fff !important;
              background-color: #212529 !important;
              border-color: #212529 #212529 #fff !important;
            }
            @keyframes sweep {
              0% { left: -40%; }
              100% { left: 100%; }
            }
            @keyframes pulse {
              0% { transform: scale(1); opacity: 0.9; }
              70% { transform: scale(1.35); opacity: 0.2; }
              100% { transform: scale(1); opacity: 0.9; }
            }
            .scan-dot { width: 10px; height: 10px; border-radius: 50%; background:#212529; display:inline-block; animation: pulse 1.2s infinite ease-in-out; }
            .scan-dot.d2 { animation-delay: .2s }
            .scan-dot.d3 { animation-delay: .4s }
          `}</style>
          <Tabs defaultActiveKey="read" className="mb-3">
            <Tab eventKey="read" title={
              <span className="d-flex align-items-center">
                <FaQrcode className="me-2" />
                Quét RFID
              </span>
            }>
              <Card  style={{ border: '2px solid #dee2e6', borderRadius: '0.5rem' }}>
                <Card.Header className="bg-primary text-white d-flex justify-content-between align-items-center" style={{ border: 'none', borderRadius: '0.5rem 0.5rem 0 0' }}>
                  <div className="d-flex align-items-center">
                    <FaQrcode className="me-2" />
                    <h5 className="mb-0">Nhận các RFID được quét</h5>
                  </div>
                  <div className="d-flex align-items-center gap-2">
                    <Form.Select 
                      size="sm" 
                      style={{width: 190}} 
                      value={statusFilter} 
                      onChange={e => { setStatusFilter(e.target.value); setPage(1); }}
                      className="shadow-sm"
                    >
                      <option value="all">Tất cả trạng thái</option>
                      <option value="registered">Đã đăng ký</option>
                      <option value="unregistered">Chưa đăng ký</option>
                    </Form.Select>
                    <Button 
                      size="sm" 
                      variant={polling ? 'danger' : 'success'} 
                      onClick={() => setPolling(!polling)}
                      className="shadow-sm"
                    >
                      {polling ? (
                        <>
                          <FaStop className="me-2" />
                          Dừng quét
                        </>
                      ) : (
                        <>
                          <FaPlay className="me-2" />
                          Quét RFID
                        </>
                      )}
                    </Button>
                  </div>
                </Card.Header>
                <Card.Body className="p-4">
                  {polling && (
                    <div className="mb-4 p-4 border rounded shadow-sm" style={{ backgroundColor: '#f8f9fa', border: '2px solid #0dcaf0' }}>
                      <div className="d-flex align-items-center justify-content-center gap-3 mb-3">
                        <Spinner animation="border" variant="primary" />
                        <div className="fw-semibold fs-5">Đang quét RFID...</div>
                      </div>
                      <div className="position-relative mt-3" style={{height:8, overflow:'hidden', borderRadius:4}}>
                        <div style={{position:'absolute', top:0, left:'-40%', width:'40%', height:'100%', background:'linear-gradient(90deg, transparent, rgba(33,37,41,0.5), transparent)', animation:'sweep 1.2s linear infinite'}} />
                        <div style={{width:'100%', height:'100%', background:'repeating-linear-gradient(90deg, #e9ecef 0, #e9ecef 10px, #f8f9fa 10px, #f8f9fa 20px)'}} />
                      </div>
                      <div className="mt-3 d-flex justify-content-center gap-2">
                        <span className="scan-dot" />
                        <span className="scan-dot d2" />
                        <span className="scan-dot d3" />
                      </div>
                    </div>
                  )}

                  <div className="table-responsive">
                    <Table responsive striped hover className="mb-0" style={{ fontSize: '0.95rem' }}>
                      <thead className="table-info">
                        <tr>
                          <th style={{ fontWeight: '600' }}>ID</th>
                          <th style={{ fontWeight: '600' }}>RFID</th>
                          <th style={{ fontWeight: '600' }}>Vị trí</th>
                          <th style={{ fontWeight: '600' }}>Mã nhân viên</th>
                          <th style={{ fontWeight: '600' }}>Tên nhân viên</th>
                          <th style={{ fontWeight: '600' }}>Thời gian đọc</th>
                          <th style={{ fontWeight: '600' }}>Trạng thái</th>
                          <th style={{ fontWeight: '600', textAlign: 'center' }}>Thao tác</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredRfids.slice((page-1)*pageSize, page*pageSize).map((rfid) => (
                          <tr key={rfid.id} style={{ verticalAlign: 'middle' }}>
                            <td>{rfid.id}</td>
                            <td>
                              <code className="bg-light px-2 py-1 rounded" style={{ fontSize: '0.9rem' }}>
                                {rfid.rfid}
                              </code>
                            </td>
                            <td style={{ fontWeight: '500' }}>
                              {rfid.phongHoc || rfid.viTri || rfid.maThietBi || <span className="text-muted">-</span>}
                            </td>
                            <td>
                              {rfid.maSinhVien ? (
                                <Badge bg="secondary" style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                                  {rfid.maSinhVien}
                                </Badge>
                              ) : (
                                <span className="text-muted">-</span>
                              )}
                            </td>
                            <td style={{ fontWeight: '500' }}>{rfid.tenSinhVien || <span className="text-muted">-</span>}</td>
                            <td>{new Date(rfid.createdAt).toLocaleString('vi-VN')}</td>
                            <td>
                              <Badge bg={rfid.processed ? 'success' : 'warning'} style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                                {rfid.processed ? (
                                  <>
                                    <FaCheckCircle className="me-1" />
                                    Đã đăng ký
                                  </>
                                ) : (
                                  <>
                                    <FaExclamationTriangle className="me-1" />
                                    Chưa đăng ký
                                  </>
                                )}
                              </Badge>
                            </td>
                            <td style={{ textAlign: 'center' }}>
                              <div className="d-flex gap-2 justify-content-center">
                                {!rfid.processed && (
                                  <Button 
                                    variant="outline-danger" 
                                    size="sm" 
                                    onClick={() => handleDeleteUnregistered(rfid.id)}
                                    className="shadow-sm"
                                  >
                                    <FaTrash className="me-1" />
                                    Xóa
                                  </Button>
                                )}
                                <Button 
                                  variant="outline-secondary" 
                                  size="sm" 
                                  onClick={() => copyToClipboard(rfid.rfid)}
                                  className="shadow-sm"
                                >
                                  <FaCopy className="me-1" />
                                  Copy
                                </Button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </div>

                  {filteredRfids.length === 0 && (
                    <div className="text-center py-5">
                      <FaQrcode size={64} className="text-muted mb-3" />
                      <Alert variant="success" className="d-inline-block">
                        Không có RFID nào chưa được đăng ký.
                      </Alert>
                    </div>
                  )}

                  {filteredRfids.length > 0 && (
                    <div className="d-flex justify-content-between align-items-center mt-4 pt-3 border-top">
                      <div className="text-muted fw-semibold">
                        Trang <span className="text-primary">{page}</span>
                      </div>
                      <div className="d-flex gap-2">
                        <Button 
                          variant="outline-secondary" 
                          disabled={page === 1} 
                          onClick={() => setPage(p => Math.max(1, p - 1))}
                          className="shadow-sm"
                        >
                          Trước
                        </Button>
                        <Button 
                          variant="outline-secondary" 
                          disabled={filteredRfids.length <= page * pageSize} 
                          onClick={() => setPage(p => p + 1)}
                          className="shadow-sm"
                        >
                          Sau
                        </Button>
                      </div>
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Tab>
            <Tab eventKey="device" title={
              <span className="d-flex align-items-center">
                <FaDesktop className="me-2" />
                Thiết lập thiết bị
              </span>
            }>
              <Card className="shadow-sm" style={{ border: '2px solid #dee2e6', borderRadius: '0.5rem' }}>
                <Card.Header className="bg-primary text-white d-flex align-items-center" style={{ border: 'none', borderRadius: '0.5rem 0.5rem 0 0' }}>
                  <FaDesktop className="me-2" />
                  <h5 className="mb-0">Đăng ký thiết bị</h5>
                </Card.Header>
                <Card.Body className="p-4">
                  <Card className="mb-4 shadow-sm" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
                    <Card.Body className="p-4">
                      <Form onSubmit={handleCreateDevice}>
                        <Row className="g-3 align-items-end">
                          <Col md={4}>
                            <Form.Group className="mb-0">
                              <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                                <FaDesktop className="me-2 text-primary" />
                                Mã thiết bị
                              </Form.Label>
                              <Form.Control 
                                value={newDevice.maThietBi} 
                                onChange={(e) => setNewDevice(v => ({ ...v, maThietBi: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="Nhập mã thiết bị..."
                              />
                            </Form.Group>
                          </Col>
                          <Col md={4}>
                            <Form.Group className="mb-0">
                              <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                                <FaDoorOpen className="me-2 text-primary" />
                                Vị trí
                              </Form.Label>
                              <Form.Control 
                                value={newDevice.phongHoc} 
                                onChange={(e) => setNewDevice(v => ({ ...v, phongHoc: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="Nhập vị trí..."
                              />
                            </Form.Group>
                          </Col>
                          <Col md={4} className="d-flex align-items-end">
                            <Button type="submit" variant="primary" style={{ position: 'relative', top: '-10px' }} className="w-100 shadow-sm">
                              <FaCheckCircle className="me-2" />
                              Lưu thiết bị
                            </Button>
                          </Col>
                        </Row>
                      </Form>
                    </Card.Body>
                  </Card>

                  <div className="table-responsive">
                    <Table responsive striped hover className="mb-0" style={{ fontSize: '0.95rem' }}>
                      <thead className="table-primary">
                        <tr>
                          <th style={{ fontWeight: '600' }}>Mã thiết bị</th>
                          <th style={{ fontWeight: '600' }}>Vị trí</th>
                          <th style={{ fontWeight: '600' }}>Trạng thái</th>
                          <th style={{ fontWeight: '600', textAlign: 'center' }}>Thao tác</th>
                        </tr>
                      </thead>
                      <tbody>
                        {devices.map(d => (
                          <tr key={d.maThietBi} style={{ verticalAlign: 'middle' }}>
                            <td>
                              <Badge bg="primary" style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                                {d.maThietBi}
                              </Badge>
                            </td>
                            <td style={{ fontWeight: '500' }}>{d.phongHoc}</td>
                            <td>
                              <Badge bg={d.active ? 'success' : 'secondary'} style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                                {d.active ? 'Hoạt động' : 'Không hoạt động'}
                              </Badge>
                            </td>
                            <td style={{ textAlign: 'center' }}>
                              <Button 
                                variant="outline-primary" 
                                size="sm"
                                onClick={() => handleViewDeviceDetails(d)}
                                className="shadow-sm"
                              >
                                <FaInfo className="me-1" />
                                Chi tiết
                              </Button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </div>

                  {devices.length === 0 && (
                    <div className="text-center py-5">
                      <FaDesktop size={64} className="text-muted mb-3" />
                      <Alert variant="info" className="d-inline-block">
                        Chưa có thiết bị nào được đăng ký.
                      </Alert>
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Tab>

            <Tab eventKey="shift" title={
              <span className="d-flex align-items-center">
                <FaClock className="me-2" />
                Cài đặt ca làm
              </span>
            }>
              <Card className="shadow-sm" style={{ border: '2px solid #dee2e6', borderRadius: '0.5rem' }}>
                <Card.Header className="bg-primary text-white d-flex align-items-center" style={{ border: 'none', borderRadius: '0.5rem 0.5rem 0 0' }}>
                  <FaClock className="me-2" />
                  <h5 className="mb-0">Đặt ca làm</h5>
                </Card.Header>
                <Card.Body className="p-4">
                  <Row className="g-4">
                    <Col md={3}>
                      <Card className="shadow-sm" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
                        <Card.Body className="p-3">
                          <Form onSubmit={handleSubmitCaLam}>
                            <Form.Group className="mb-3">
                              <Form.Label className="fw-semibold mb-2">Mã ca</Form.Label>
                              <Form.Control
                                type="number"
                                value={caForm.maCa}
                                disabled={editingCa !== null}
                                onChange={(e) => setCaForm(v => ({ ...v, maCa: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="1..5"
                              />
                            </Form.Group>

                            <Form.Group className="mb-3">
                              <Form.Label className="fw-semibold mb-2">Tên ca</Form.Label>
                              <Form.Control
                                value={caForm.tenCa}
                                onChange={(e) => setCaForm(v => ({ ...v, tenCa: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="Ví dụ: Ca 1"
                              />
                            </Form.Group>

                            <Form.Group className="mb-3">
                              <Form.Label className="fw-semibold mb-2">Giờ bắt đầu</Form.Label>
                              <Form.Control
                                type="time"
                                value={caForm.gioBatDau}
                                onChange={(e) => setCaForm(v => ({ ...v, gioBatDau: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                              />
                            </Form.Group>

                            <Form.Group className="mb-3">
                              <Form.Label className="fw-semibold mb-2">Giờ kết thúc</Form.Label>
                              <Form.Control
                                type="time"
                                value={caForm.gioKetThuc}
                                onChange={(e) => setCaForm(v => ({ ...v, gioKetThuc: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                              />
                            </Form.Group>

                            

                            <div className="d-flex gap-2">
                              <Button type="submit" variant="primary" className="w-100 shadow-sm">
                                {editingCa !== null ? 'Cập nhật ca' : 'Tạo ca'}
                              </Button>
                              {editingCa !== null && (
                                <Button
                                  type="button"
                                  variant="outline-secondary"
                                  onClick={resetCaForm}
                                  className="shadow-sm"
                                >
                                  Hủy
                                </Button>
                              )}
                            </div>
                          </Form>
                        </Card.Body>
                      </Card>
                    </Col>

                    <Col md={9}>
                      <Card className="shadow-sm" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
                        <Card.Body className="p-3">
                          <div className="d-flex justify-content-between align-items-center mb-3">
                            <h6 className="mb-0 fw-semibold">Danh sách ca</h6>
                            {caLoading && <Spinner animation="border" size="sm" variant="primary" />}
                          </div>

                          {caLams.length === 0 && !caLoading ? (
                            <Alert variant="info" className="mb-0">
                              Chưa có ca làm nào. Hãy tạo mới ở form bên trái.
                            </Alert>
                          ) : (
                            <div className="table-responsive">
                              <Table responsive striped hover className="mb-0" style={{ fontSize: '0.95rem' }}>
                                <thead className="table-light">
                                  <tr>
                                    <th>Mã ca</th>
                                    <th>Tên ca</th>
                                    <th>Giờ bắt đầu</th>
                                    <th>Giờ kết thúc</th>
                                   
                                    <th style={{ textAlign: 'center' }}>Thao tác</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {caLams.map((ca) => (
                                    <tr key={ca.maCa} style={{ verticalAlign: 'middle' }}>
                                      <td>
                                        <Badge bg="primary" style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                                          {ca.maCa}
                                        </Badge>
                                      </td>
                                      <td style={{ fontWeight: '500' }}>{ca.tenCa || '-'}</td>
                                      <td>{typeof ca.gioBatDau === 'string' ? ca.gioBatDau.substring(0, 5) : (ca.gioBatDau ?? '-')}</td>
                                      <td>{typeof ca.gioKetThuc === 'string' ? ca.gioKetThuc.substring(0, 5) : (ca.gioKetThuc ?? '-')}</td>
                                     
                                      <td style={{ textAlign: 'center' }}>
                                        <div className="d-flex gap-2 justify-content-center">
                                          <Button
                                            variant="outline-primary"
                                            size="sm"
                                            onClick={() => handleEditCaLam(ca)}
                                            className="shadow-sm"
                                          >
                                            <FaEdit className="me-1" />
                                            Sửa
                                          </Button>
                                          <Button
                                            variant="outline-danger"
                                            size="sm"
                                            onClick={() => handleDeleteCaLam(ca.maCa)}
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
                          )}
                        </Card.Body>
                      </Card>
                    </Col>
                  </Row>
                </Card.Body>
              </Card>
            </Tab>
           
          </Tabs>
        </Col>
      </Row>

      <Modal show={showModal} onHide={() => setShowModal(false)} size="lg">
        <Modal.Header closeButton className="text-white" style={{ backgroundColor: '#212529' }}>
          <Modal.Title className="d-flex align-items-center">
            <FaQrcode className="me-2" />
            Kết quả quét RFID
          </Modal.Title>
        </Modal.Header>
        <Modal.Body className="p-4">
          <div className="mb-3">
            <Form.Label className="fw-semibold d-flex align-items-center mb-2">
              <FaQrcode className="me-2" style={{ color: '#212529' }} />
              RFID:
            </Form.Label>
            <code className="bg-light px-3 py-2 rounded d-block" style={{ fontSize: '1.1rem' }}>
              {scannedInfo.rfid}
            </code>
          </div>
          {scannedInfo.status === 'found' ? (
            <div>
              <Alert variant="success" className="shadow-sm" style={{ border: '1px solid #28a745' }}>
                <div className="d-flex align-items-center mb-2">
                  <FaCheckCircle className="me-2" size={20} />
                  <strong>RFID đã được đăng ký</strong>
                </div>
                <div className="mt-3">
                  <p className="mb-2">
                    <strong>Tên nhân viên:</strong> 
                    <Badge bg="secondary" className="ms-2" style={{ fontSize: '0.95rem', padding: '0.4rem 0.6rem' }}>
                      {scannedInfo.name}
                    </Badge>
                  </p>
                  <p className="mb-0">
                    <strong>Mã nhân viên:</strong> 
                    <Badge bg="primary" className="ms-2" style={{ fontSize: '0.95rem', padding: '0.4rem 0.6rem' }}>
                      {scannedInfo.maSinhVien}
                    </Badge>
                  </p>
                </div>
              </Alert>
            </div>
          ) : (
            <Alert variant="warning" className="shadow-sm" style={{ border: '1px solid #ffc107' }}>
              <div className="d-flex align-items-center">
                <FaExclamationTriangle className="me-2" size={20} />
                <div>
                  <strong>RFID chưa được đăng ký.</strong> Hãy copy để đăng ký mới.
                </div>
              </div>
            </Alert>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowModal(false)} className="shadow-sm">
            Đóng
          </Button>
          {scannedInfo.status === 'not_found' && (
            <Button variant="primary" onClick={() => copyToClipboard(scannedInfo.rfid)} className="shadow-sm">
              <FaCopy className="me-2" />
              Copy RFID
            </Button>
          )}
        </Modal.Footer>
      </Modal>

      {/* New API Key Alert */}
      {showNewApiKeyAlert && (
        <Alert 
          variant="success" 
          dismissible 
          onClose={() => setShowNewApiKeyAlert(null)}
          style={{
            position: 'fixed',
            top: 20,
            right: 20,
            zIndex: 9999,
            maxWidth: '500px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
          }}
        >
          <Alert.Heading className="d-flex align-items-center mb-3">
            <FaKey className="me-2" />
            API Key mới được tạo
          </Alert.Heading>
          <p className="mb-3 fw-semibold text-danger">
            ⚠️ Lưu API key này ngay! Bạn sẽ không thể xem lại nó.
          </p>
          <div className="bg-light p-3 rounded mb-3 position-relative" style={{ wordBreak: 'break-all', fontSize: '0.85rem', fontFamily: 'monospace' }}>
            {showNewApiKeyAlert}
            <Button 
              variant="outline-secondary"
              size="sm"
              onClick={() => copyToClipboard(showNewApiKeyAlert)}
              style={{ position: 'absolute', top: 10, right: 10 }}
            >
              <FaCopy />
            </Button>
          </div>
          <small className="text-muted">Cửa sổ này sẽ tự đóng trong vài giây.</small>
        </Alert>
      )}

      {/* Device Details Modal */}
      <Modal show={showDeviceDetailsModal} onHide={() => setShowDeviceDetailsModal(false)} size="lg">
        <Modal.Header closeButton className="text-white" style={{ backgroundColor: '#212529', border: 'none' }}>
          <Modal.Title className="d-flex align-items-center">
            <FaDesktop className="me-2" />
            Chi tiết thiết bị: {selectedDevice?.maThietBi}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body className="p-4">
          {selectedDevice && (
            <>
              {/* Device Information */}
              <div className="mb-4 p-3 bg-light rounded border">
                <div className="row">
                  <div className="col-md-6">
                    <p className="mb-2">
                      <strong>Mã thiết bị:</strong>
                      <Badge bg="primary" className="ms-2">{selectedDevice.maThietBi}</Badge>
                    </p>
                  </div>
                  <div className="col-md-6">
                    <p className="mb-2">
                      <strong>Vị trí:</strong>
                      <span className="ms-2">{selectedDevice.phongHoc}</span>
                    </p>
                  </div>
                </div>
                <div className="row mt-3">
                  <div className="col-md-6">
                    <p className="mb-0">
                      <strong>Trạng thái:</strong>
                      <Badge bg={selectedDevice.active ? 'success' : 'secondary'} className="ms-2">
                        {selectedDevice.active ? 'Hoạt động' : 'Không hoạt động'}
                      </Badge>
                    </p>
                  </div>
                  <div className="col-md-6">
                    <Button
                      variant={selectedDevice.active ? 'outline-danger' : 'outline-success'}
                      size="sm"
                      onClick={() => handleToggleDeviceStatus(selectedDevice.maThietBi)}
                    >
                      {selectedDevice.active ? 'Vô hiệu hóa' : 'Kích hoạt'}
                    </Button>
                  </div>
                </div>
              </div>

              {/* API Keys Section */}
              <div className="mb-4">
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <h6 className="mb-0 d-flex align-items-center">
                    <FaKey className="me-2" />
                    Danh sách API Keys ({deviceApiKeys.length})
                  </h6>
                  <Button
                    variant="success"
                    size="sm"
                    onClick={handleCreateNewApiKey}
                  >
                    <FaPlus className="me-1" />
                    Tạo API Key mới
                  </Button>
                </div>

                {deviceApiKeys.length > 0 ? (
                  <div className="table-responsive">
                    <Table striped hover className="mb-0" style={{ fontSize: '0.9rem' }}>
                      <thead className="table-light">
                        <tr>
                          <th>ID</th>
                          <th>API Key</th>
                          <th>Mô tả</th>
                          <th>Trạng thái</th>
                          <th style={{ textAlign: 'center' }}>Thao tác</th>
                        </tr>
                      </thead>
                      <tbody>
                        {deviceApiKeys.map(key => (
                          <tr key={key.id} style={{ verticalAlign: 'middle' }}>
                            <td>{key.id}</td>
                            <td>
                              <code className="bg-light px-2 py-1 rounded" style={{ fontSize: '0.8rem', wordBreak: 'break-all' }}>
                                {visibleApiKeys.has(key.id) ? key.apiKey : '••••••••••••••••'}
                              </code>
                              <Button
                                variant="link"
                                size="sm"
                                onClick={() => handleToggleApiKeyVisibility(key.id)}
                                className="ms-2 p-0 text-decoration-none"
                                style={{ fontSize: '0.85rem' }}
                              >
                                {visibleApiKeys.has(key.id) ? (
                                  <>
                                    <FaEyeSlash className="me-1" />
                                    Ẩn
                                  </>
                                ) : (
                                  <>
                                    <FaEye className="me-1" />
                                    Hiện
                                  </>
                                )}
                              </Button>
                              {visibleApiKeys.has(key.id) && (
                                <Button
                                  variant="link"
                                  size="sm"
                                  onClick={() => copyToClipboard(key.apiKey)}
                                  className="ms-1 p-0 text-decoration-none"
                                  style={{ fontSize: '0.85rem' }}
                                >
                                  <FaCopy className="me-1" />
                                  Sao chép
                                </Button>
                              )}
                            </td>
                            <td>{key.moTa || '-'}</td>
                            <td>
                              <Badge bg={key.active ? 'success' : 'secondary'} style={{ fontSize: '0.8rem' }}>
                                {key.active ? 'Active' : 'Inactive'}
                              </Badge>
                            </td>
                            <td style={{ textAlign: 'center' }}>
                              <Button
                                variant={key.active ? 'outline-warning' : 'outline-success'}
                                size="sm"
                                onClick={() => handleToggleApiKeyStatus(key.id)}
                                className="me-2"
                                style={{ fontSize: '0.8rem', padding: '0.25rem 0.5rem' }}
                              >
                                {key.active ? 'Vô hiệu' : 'Kích hoạt'}
                              </Button>
                              <Button
                                variant="outline-danger"
                                size="sm"
                                onClick={() => handleDeleteApiKey(key.id)}
                                style={{ fontSize: '0.8rem', padding: '0.25rem 0.5rem' }}
                              >
                                <FaTrash />
                              </Button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </div>
                ) : (
                  <Alert variant="info" className="mb-0">
                    <FaKey className="me-2" />
                    Chưa có API key nào. Tạo API key mới để kích hoạt thiết bị.
                  </Alert>
                )}
              </div>
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowDeviceDetailsModal(false)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
};

export default SettingsPage;
