import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Alert, Badge, Modal, Form, Tabs, Tab, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { attendanceAPI, studentAPI, deviceAPI } from '../services/api';

const SettingsPage = () => {
  const [unprocessedRfids, setUnprocessedRfids] = useState([]);
  const [page, setPage] = useState(1);
  const pageSize = 8;
  const [showModal, setShowModal] = useState(false);
  const [scannedInfo, setScannedInfo] = useState({ rfid: '', name: '', maSinhVien: '', status: '' });
  const [polling, setPolling] = useState(false);
  const [devices, setDevices] = useState([]);
  const [newDevice, setNewDevice] = useState({ maThietBi: '', phongHoc: '' });
  const [statusFilter, setStatusFilter] = useState('all');

  useEffect(() => {
    loadUnprocessedRfids();
    loadDevices();
  }, []);

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
    toast.success('Đã copy RFID');
  };

  const handleDeleteUnregistered = async (id) => {
    try {
      // reuse markProcessed as delete not provided; ideally have delete API
      // For now, mark processed as a way to hide from list
      await attendanceAPI.markProcessed(id);
      await loadUnprocessedRfids();
      toast.success('Đã xóa RFID chưa đăng ký');
    } catch (e) {
      toast.error('Thao tác thất bại');
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
      toast.error('Vui lòng nhập đủ Mã thiết bị và Phòng học');
      return;
    }
    try {
      await deviceAPI.create({ maThietBi: newDevice.maThietBi, phongHoc: newDevice.phongHoc });
      toast.success('Đã tạo thiết bị');
      setNewDevice({ maThietBi: '', phongHoc: '' });
      loadDevices();
    } catch (e) {
      toast.error('Không thể tạo thiết bị');
    }
  };

  const filteredRfids = unprocessedRfids.filter(item => {
    if (statusFilter === 'registered') return !!item.processed;
    if (statusFilter === 'unregistered') return !item.processed;
    return true;
  });

  return (
    <Container>
      <Row>
        <Col>
          <h3>Cài đặt</h3>
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
            <Tab eventKey="read" title="Quét RFID">
              <Card>
                <Card.Header className="d-flex justify-content-between align-items-center">
                  <div>Nhận các RFID được quét</div>
                  <div className="d-flex align-items-center gap-2">
                    <Form.Select size="sm" style={{width: 190}} value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(1); }}>
                      <option value="all">Tất cả trạng thái</option>
                      <option value="registered">Đã đăng ký</option>
                      <option value="unregistered">Chưa đăng ký</option>
                    </Form.Select>
                    <Button size="sm" variant={polling ? 'danger' : 'success'} onClick={() => setPolling(!polling)}>
                      {polling ? 'Dừng quét' : 'Quét RFID'}
                    </Button>
                  </div>
                </Card.Header>
                <Card.Body>
                  {polling && (
                    <div className="mb-3 p-3 border rounded bg-light">
                      <div className="d-flex align-items-center justify-content-center gap-3">
                        <Spinner animation="border" variant="primary" />
                        <div className="fw-semibold">Đang quét RFID...</div>
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
                      {filteredRfids.slice((page-1)*pageSize, page*pageSize).map((rfid) => (
                        <tr key={rfid.id}>
                          <td>{rfid.id}</td>
                          <td><code className="rfid-display">{rfid.rfid}</code></td>
                          <td>{rfid.maSinhVien || '-'}</td>
                          <td>{rfid.tenSinhVien || '-'}</td>
                          <td>{new Date(rfid.createdAt).toLocaleString('vi-VN')}</td>
                          <td>
                            <Badge bg={rfid.processed ? 'success' : 'warning'}>
                              {rfid.processed ? 'Đã đăng ký' : 'Chưa đăng ký'}
                            </Badge>
                          </td>
                          <td className="d-flex gap-2">
                            {!rfid.processed && (
                              <Button variant="outline-danger" size="sm" onClick={() => handleDeleteUnregistered(rfid.id)}>Xóa</Button>
                            )}
                            <Button variant="outline-secondary" size="sm" onClick={() => copyToClipboard(rfid.rfid)}>Copy</Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>

                  {filteredRfids.length === 0 && (
                    <Alert variant="success">Không có RFID nào chưa được đăng ký.</Alert>
                  )}

                  <div className="d-flex justify-content-between align-items-center mt-3">
                    <div>Trang {page}</div>
                    <div className="d-flex gap-2">
                      <Button variant="outline-secondary" disabled={page === 1} onClick={() => setPage(p => Math.max(1, p - 1))}>Trước</Button>
                      <Button variant="outline-secondary" disabled={filteredRfids.length <= page * pageSize} onClick={() => setPage(p => p + 1)}>Sau</Button>
                    </div>
                  </div>
                </Card.Body>
              </Card>
            </Tab>
            <Tab eventKey="device" title="Thiết lập thiết bị">
              <Card>
                <Card.Header style={{backgroundColor: '#0d6efd', color: '#fff'}}>Đăng ký thiết bị cho phòng học</Card.Header>
                <Card.Body>
                  <Form onSubmit={handleCreateDevice} className="mb-4">
                    <Row>
                      <Col md={4}>
                        <Form.Group>
                          <Form.Label>Mã thiết bị</Form.Label>
                          <Form.Control value={newDevice.maThietBi} onChange={(e) => setNewDevice(v => ({ ...v, maThietBi: e.target.value }))} />
                        </Form.Group>
                      </Col>
                      <Col md={4}>
                        <Form.Group>
                          <Form.Label>Phòng học</Form.Label>
                          <Form.Control value={newDevice.phongHoc} onChange={(e) => setNewDevice(v => ({ ...v, phongHoc: e.target.value }))} />
                        </Form.Group>
                      </Col>
                      <Col md={4} style={{position: 'relative', top: -10}} className="d-flex align-items-end">
                        <Button type="submit">Lưu thiết bị</Button>
                      </Col>
                    </Row>
                  </Form>

                  <Table responsive bordered hover>
                    <thead>
                      <tr>
                        <th>Mã thiết bị</th>
                        <th>Phòng học</th>
                      </tr>
                    </thead>
                    <tbody>
                      {devices.map(d => (
                        <tr key={d.maThietBi}>
                          <td>{d.maThietBi}</td>
                          <td>{d.phongHoc}</td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </Card.Body>
              </Card>
            </Tab>
          </Tabs>
        </Col>
      </Row>

      <Modal show={showModal} onHide={() => setShowModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Kết quả quét RFID</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p><strong>RFID:</strong> <code>{scannedInfo.rfid}</code></p>
          {scannedInfo.status === 'found' ? (
            <>
              <p><strong>Tên sinh viên:</strong> {scannedInfo.name}</p>
              <p><strong>Mã sinh viên:</strong> {scannedInfo.maSinhVien}</p>
            </>
          ) : (
            <Alert variant="warning">RFID chưa được đăng ký. Hãy copy để đăng ký mới.</Alert>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowModal(false)}>Đóng</Button>
          {scannedInfo.status === 'not_found' && (
            <Button variant="primary" onClick={() => copyToClipboard(scannedInfo.rfid)}>Copy RFID</Button>
          )}
        </Modal.Footer>
      </Modal>
    </Container>
  );
};

export default SettingsPage;
