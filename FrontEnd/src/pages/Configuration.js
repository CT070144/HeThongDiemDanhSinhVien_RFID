import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Alert, Badge, Modal, Form, Tabs, Tab, Spinner, Pagination } from 'react-bootstrap';
import { attendanceAPI, studentAPI, deviceAPI, roomAPI } from '../services/api';
import { useNotification } from '../contexts/NotificationContext';
import { FaQrcode, FaCog, FaPlay, FaStop, FaCopy, FaTrash, FaFilter, FaDesktop, FaDoorOpen, FaCheckCircle, FaExclamationTriangle, FaBuilding, FaPlus, FaEdit, FaSearch } from 'react-icons/fa';

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

  useEffect(() => {
    loadUnprocessedRfids();
    loadDevices();
    loadRooms();
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

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    notify.success('Đã copy RFID');
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
      notify.error('Vui lòng nhập đủ Mã thiết bị và Phòng học');
      return;
    }
    try {
      await deviceAPI.create({ maThietBi: newDevice.maThietBi, phongHoc: newDevice.phongHoc });
      notify.success('Đã tạo thiết bị');
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

  const handleSaveRoom = async (e) => {
    e.preventDefault();
    if (!roomForm.maPhong) {
      notify.error('Vui lòng nhập Mã phòng');
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
        notify.success('Cập nhật phòng học thành công');
      } else {
        await roomAPI.create(payload);
        notify.success('Tạo phòng học thành công');
      }
      resetRoomForm();
      loadRooms(roomKeyword);
    } catch (e) {
      const msg = e.response?.data?.message || 'Lưu phòng học thất bại';
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
    if (!window.confirm('Xóa phòng học này?')) return;
    try {
      await roomAPI.delete(maPhong);
      notify.success('Đã xóa phòng học');
      if (editingRoom === maPhong) resetRoomForm();
      loadRooms(roomKeyword);
    } catch (e) {
      const msg = e.response?.data?.message || 'Xóa phòng học thất bại';
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
    <Container fluid className="py-4">
      <Row>
        <Col>
          <Card className="shadow-sm mb-4" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
            <Card.Header className="bg-primary text-white d-flex align-items-center" style={{ border: 'none' }}>
              <FaCog className="me-2" size={24} />
              <h3 className="mb-0">Cài đặt hệ thống</h3>
            </Card.Header>
          </Card>
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
            .scan-dot { width: 10px; height: 10px; border-radius: 50%; background:#0d6efd; display:inline-block; animation: pulse 1.2s infinite ease-in-out; }
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
                        <div style={{position:'absolute', top:0, left:'-40%', width:'40%', height:'100%', background:'linear-gradient(90deg, transparent, rgba(13,110,253,0.5), transparent)', animation:'sweep 1.2s linear infinite'}} />
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
                          <th style={{ fontWeight: '600' }}>Mã sinh viên</th>
                          <th style={{ fontWeight: '600' }}>Tên sinh viên</th>
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
                  <h5 className="mb-0">Đăng ký thiết bị cho phòng học</h5>
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
                                Phòng học
                              </Form.Label>
                              <Form.Control 
                                value={newDevice.phongHoc} 
                                onChange={(e) => setNewDevice(v => ({ ...v, phongHoc: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="Nhập phòng học..."
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
                          <th style={{ fontWeight: '600' }}>Phòng học</th>
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
            <Tab eventKey="room" title={
              <span className="d-flex align-items-center">
                <FaBuilding className="me-2" />
                Phòng học
              </span>
            }>
              <Card className="shadow-sm" style={{ border: '2px solid #dee2e6', borderRadius: '0.5rem' }}>
                <Card.Header className="bg-primary text-white d-flex align-items-center justify-content-between" style={{ border: 'none', borderRadius: '0.5rem 0.5rem 0 0' }}>
                  <div className="d-flex align-items-center">
                    <FaBuilding className="me-2" />
                    <h5 className="mb-0">{editingRoom ? 'Cập nhật phòng học' : 'Thêm phòng học'}</h5>
                  </div>
                  <Button variant="light" size="sm" className="shadow-sm" onClick={resetRoomForm}>
                    <FaPlus className="me-1" />
                    Tạo mới
                  </Button>
                </Card.Header>
                <Card.Body className="p-4">
                  <Card className="mb-4 shadow-sm" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
                    <Card.Body className="p-3">
                      <Form onSubmit={handleSaveRoom}>
                        <Row className="g-3 align-items-end">
                          <Col md={3}>
                            <Form.Group className="mb-0">
                              <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                                <FaDoorOpen className="me-2 text-primary" />
                                Mã phòng
                              </Form.Label>
                              <Form.Control
                                value={roomForm.maPhong}
                                onChange={(e) => setRoomForm(v => ({ ...v, maPhong: e.target.value }))}
                                disabled={!!editingRoom}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="VD: A101"
                              />
                            </Form.Group>
                          </Col>
                          <Col md={3}>
                            <Form.Group className="mb-0">
                              <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                                <FaBuilding className="me-2 text-primary" />
                                Tên phòng
                              </Form.Label>
                              <Form.Control
                                value={roomForm.tenPhong}
                                onChange={(e) => setRoomForm(v => ({ ...v, tenPhong: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="Phòng máy, Phòng lab..."
                              />
                            </Form.Group>
                          </Col>
                          <Col md={2}>
                            <Form.Group className="mb-0">
                              <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                                <FaBuilding className="me-2 text-primary" />
                                Tòa nhà
                              </Form.Label>
                              <Form.Control
                                value={roomForm.toaNha}
                                onChange={(e) => setRoomForm(v => ({ ...v, toaNha: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="A, B, C..."
                              />
                            </Form.Group>
                          </Col>
                          <Col md={2}>
                            <Form.Group className="mb-0">
                              <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                                <FaBuilding className="me-2 text-primary" />
                                Tầng
                              </Form.Label>
                              <Form.Control
                                type="number"
                                value={roomForm.tang}
                                onChange={(e) => setRoomForm(v => ({ ...v, tang: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="VD: 1"
                              />
                            </Form.Group>
                          </Col>
                          <Col md={2}>
                            <Form.Group className="mb-0">
                              <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                                <FaBuilding className="me-2 text-primary" />
                                Sức chứa
                              </Form.Label>
                              <Form.Control
                                type="number"
                                value={roomForm.sucChua}
                                onChange={(e) => setRoomForm(v => ({ ...v, sucChua: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="VD: 40"
                              />
                            </Form.Group>
                          </Col>
                        </Row>
                        <Row className="g-3 align-items-end mt-3">
                          <Col md={3}>
                            <Form.Group className="mb-0">
                              <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                                <FaBuilding className="me-2 text-primary" />
                                Loại phòng
                              </Form.Label>
                              <Form.Control
                                value={roomForm.loaiPhong}
                                onChange={(e) => setRoomForm(v => ({ ...v, loaiPhong: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                                placeholder="Lý thuyết, Lab, Thực hành..."
                              />
                            </Form.Group>
                          </Col>
                          <Col md={3}>
                            <Form.Group style={{ position: 'relative', top: '-10px' }} className="mb-0">
                              <Form.Label className="fw-semibold d-flex align-items-center mb-2">
                                <FaBuilding className="me-2 text-primary" />
                                Trạng thái
                              </Form.Label>
                              <Form.Select
                                value={roomForm.trangThai}
                                onChange={(e) => setRoomForm(v => ({ ...v, trangThai: e.target.value }))}
                                className="shadow-sm"
                                style={{ border: '1px solid #dee2e6', borderRadius: '0.375rem' }}
                              >
                                <option value="active">active</option>
                                <option value="inactive">inactive</option>
                              </Form.Select>
                            </Form.Group>
                          </Col>
                          <Col md={6} className="d-flex justify-content-end">
                            <Button type="submit" variant="primary" className="shadow-sm" style={{ minWidth: 180 }}>
                              <FaCheckCircle className="me-2" />
                              {editingRoom ? 'Cập nhật phòng' : 'Lưu phòng học'}
                            </Button>
                          </Col>
                        </Row>
                      </Form>
                    </Card.Body>
                  </Card>

                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <div className="d-flex align-items-center gap-2">
                      <Form.Control
                        placeholder="Tìm kiếm phòng (mã/tên/tòa nhà)..."
                        value={roomKeyword}
                        onChange={(e) => setRoomKeyword(e.target.value)}
                        className="shadow-sm"
                        style={{ maxWidth: 320, border: '1px solid #dee2e6', borderRadius: '0.375rem', position: 'relative', top: '8px' }}
                      />
                      <Button variant="outline-primary" className=" w-100 shadow-sm shadow-sm" onClick={() => { setRoomPage(0); loadRooms(roomKeyword); }}>
                        <FaSearch className="me-1" />
                        Tìm
                      </Button>
                      <Button variant="outline-secondary" className="w-100 shadow-sm" onClick={() => { setRoomKeyword(''); setRoomPage(0); loadRooms(''); }}>
                        <FaFilter className="me-1" />
                        Xóa lọc
                      </Button>
                    </div>
                  </div>

                  <div className="table-responsive">
                    <Table responsive striped hover className="mb-0" style={{ fontSize: '0.95rem' }}>
                      <thead className="table-primary">
                        <tr>
                          <th style={{ fontWeight: '600' }}>Mã phòng</th>
                          <th style={{ fontWeight: '600' }}>Tên phòng</th>
                          <th style={{ fontWeight: '600' }}>Tòa nhà</th>
                          <th style={{ fontWeight: '600' }}>Tầng</th>
                          <th style={{ fontWeight: '600' }}>Sức chứa</th>
                          <th style={{ fontWeight: '600' }}>Loại</th>
                          <th style={{ fontWeight: '600' }}>Trạng thái</th>
                          <th style={{ fontWeight: '600' }}>Thao tác</th>
                        </tr>
                      </thead>
                      <tbody>
                        {rooms.map(r => (
                          <tr key={r.maPhong} style={{ verticalAlign: 'middle' }}>
                            <td>
                              <Badge bg="primary" style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                                {r.maPhong}
                              </Badge>
                            </td>
                            <td style={{ fontWeight: '500' }}>{r.tenPhong || <span className="text-muted">-</span>}</td>
                            <td>{r.toaNha || <span className="text-muted">-</span>}</td>
                            <td>{r.tang ?? <span className="text-muted">-</span>}</td>
                            <td>{r.sucChua ?? <span className="text-muted">-</span>}</td>
                            <td>{r.loaiPhong || <span className="text-muted">-</span>}</td>
                            <td>
                              <Badge bg={r.trangThai === 'active' ? 'success' : 'secondary'} style={{ fontSize: '0.9rem', padding: '0.5rem 0.75rem' }}>
                                {r.trangThai || 'active'}
                              </Badge>
                            </td>
                            <td style={{ textAlign: 'center' }}>
                              <div className="d-flex gap-2 justify-content-center">
                                <Button
                                  variant="outline-warning"
                                  size="sm"
                                  onClick={() => handleEditRoom(r)}
                                  className="shadow-sm"
                                >
                                  <FaEdit className="me-1" />
                                  Sửa
                                </Button>
                                <Button
                                  variant="outline-danger"
                                  size="sm"
                                  onClick={() => handleDeleteRoom(r.maPhong)}
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

                  {rooms.length === 0 && (
                    <div className="text-center py-5">
                      <FaBuilding size={64} className="text-muted mb-3" />
                      <Alert variant="info" className="d-inline-block">
                        Chưa có phòng học nào.
                      </Alert>
                    </div>
                  )}

                  {/* Pagination for Rooms */}
                  {roomTotalPages > 1 && (
                    <div className="d-flex justify-content-between align-items-center mt-4 pt-3 border-top">
                      <div className="text-muted fw-semibold">
                        Tổng: <span className="text-primary">{roomTotalElements}</span> phòng học
                      </div>
                      <Pagination className="mb-0">
                        <Pagination.Prev 
                          disabled={roomPage === 0}
                          onClick={() => setRoomPage(Math.max(0, roomPage - 1))}
                          className="shadow-sm"
                        />
                        {getPaginationItems(roomPage, roomTotalPages).map((item, index) => {
                          if (item === 'ellipsis-start' || item === 'ellipsis-end') {
                            return (
                              <Pagination.Ellipsis key={`ellipsis-${index}`} disabled />
                            );
                          }
                          return (
                            <Pagination.Item
                              key={item}
                              active={item === roomPage}
                              onClick={() => setRoomPage(item)}
                              className="shadow-sm"
                            >
                              {item + 1}
                            </Pagination.Item>
                          );
                        })}
                        <Pagination.Next 
                          disabled={roomPage >= roomTotalPages - 1}
                          onClick={() => setRoomPage(Math.min(roomTotalPages - 1, roomPage + 1))}
                          className="shadow-sm"
                        />
                      </Pagination>
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Tab>
          </Tabs>
        </Col>
      </Row>

      <Modal show={showModal} onHide={() => setShowModal(false)} size="lg">
        <Modal.Header closeButton className={scannedInfo.status === 'found' ? 'bg-success text-white' : 'bg-warning text-white'}>
          <Modal.Title className="d-flex align-items-center">
            <FaQrcode className="me-2" />
            Kết quả quét RFID
          </Modal.Title>
        </Modal.Header>
        <Modal.Body className="p-4">
          <div className="mb-3">
            <Form.Label className="fw-semibold d-flex align-items-center mb-2">
              <FaQrcode className="me-2 text-primary" />
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
                    <strong>Tên sinh viên:</strong> 
                    <Badge bg="secondary" className="ms-2" style={{ fontSize: '0.95rem', padding: '0.4rem 0.6rem' }}>
                      {scannedInfo.name}
                    </Badge>
                  </p>
                  <p className="mb-0">
                    <strong>Mã sinh viên:</strong> 
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
    </Container>
  );
};

export default SettingsPage;
