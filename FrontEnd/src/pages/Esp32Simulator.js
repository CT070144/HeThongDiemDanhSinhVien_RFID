import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Badge, Button, Card, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { FaCamera, FaPlay, FaStop, FaUpload, FaUndo } from 'react-icons/fa';
import api from '../services/api';
import { useNotification } from '../contexts/NotificationContext';

const Esp32Simulator = () => {
  const { notify } = useNotification();
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);

  const [rfid, setRfid] = useState('AF34951F');
  const [maThietBi, setMaThietBi] = useState('DEVICE_001');
  const [apiKey, setApiKey] = useState('esp32_u4GPoXQRDPOhwkyCbmS_eGDKusiN4JPJlHP7u5vH5Yc');
  const [cameraOn, setCameraOn] = useState(false);
  const [capturedBlob, setCapturedBlob] = useState(null);
  const [capturedUrl, setCapturedUrl] = useState('');
  const [uploadedBlob, setUploadedBlob] = useState(null);
  const [uploadedUrl, setUploadedUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const canSubmit = useMemo(() => {
    return Boolean(rfid.trim()) && Boolean(uploadedBlob || capturedBlob);
  }, [rfid, uploadedBlob, capturedBlob]);

  const stopCamera = async () => {
    try {
      const stream = streamRef.current;
      if (stream) {
        stream.getTracks().forEach((t) => t.stop());
      }
    } finally {
      streamRef.current = null;
      setCameraOn(false);
    }
  };

  const startCamera = async () => {
    try {
      if (!navigator?.mediaDevices?.getUserMedia) {
        notify.error('Trình duyệt không hỗ trợ camera (getUserMedia).');
        return;
      }
      // Stop existing stream first (if any)
      await stopCamera();

      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user' },
        audio: false
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setCameraOn(true);
    } catch (e) {
      console.error(e);
      notify.error('Không thể mở webcam. Hãy cấp quyền camera và thử lại.');
    }
  };

  const clearCapture = () => {
    setCapturedBlob(null);
    if (capturedUrl) URL.revokeObjectURL(capturedUrl);
    setCapturedUrl('');
  };

  const clearUpload = () => {
    setUploadedBlob(null);
    if (uploadedUrl) URL.revokeObjectURL(uploadedUrl);
    setUploadedUrl('');
  };

  const handleUploadImage = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type?.startsWith('image/')) {
      notify.error('Vui lòng chọn file ảnh (jpg/png/...)');
      return;
    }
    clearUpload();
    setUploadedBlob(file);
    setUploadedUrl(URL.createObjectURL(file));
  };

  const capturePhoto = async () => {
    try {
      const video = videoRef.current;
      const canvas = canvasRef.current;
      if (!video || !canvas) return;

      const w = video.videoWidth || 640;
      const h = video.videoHeight || 480;
      canvas.width = w;
      canvas.height = h;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(video, 0, 0, w, h);

      const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.92));
      if (!blob) {
        notify.error('Chụp ảnh thất bại.');
        return;
      }
      clearCapture();
      setCapturedBlob(blob);
      setCapturedUrl(URL.createObjectURL(blob));
    } catch (e) {
      console.error(e);
      notify.error('Chụp ảnh thất bại.');
    }
  };

  const submit = async () => {
    if (!canSubmit) {
      notify.error('Vui lòng nhập RFID và chọn ảnh (chụp hoặc upload) trước khi gửi.');
      return;
    }
    setLoading(true);
    setResult(null);
    try {
      const fd = new FormData();
      fd.append('rfid', rfid.trim());
      // Backend expects "maThietBi" as request param; "deviceId" is request attribute set by API-key filter
      if (maThietBi.trim()) fd.append('maThietBi', maThietBi.trim());
      // Backend đang nhận @RequestParam("image") hoặc @RequestParam("file")
      const imageToSend = uploadedBlob || capturedBlob;
      fd.append('image', imageToSend, imageToSend?.name || 'capture.jpg');

      const resp = await api.post('/attendance/rfid', fd, {
        headers: {
          'Content-Type': 'multipart/form-data',
          ...(apiKey.trim() ? { 'X-API-Key': apiKey.trim() } : {})
        }
      });
      setResult(resp?.data ?? null);

      const status = resp?.data?.status;
      if (status === 'found' || status === 'success') {
        notify.success(`Gửi thành công: ${resp?.data?.name || ''}`);
      } else {
        notify.info(`Kết quả: ${status || 'unknown'}`);
      }
    } catch (e) {
      console.error(e);
      const status = e?.response?.status;
      const msg = e?.response?.data?.error || e?.response?.data?.message || 'Lỗi khi gửi request';
      notify.error(status ? `${status}: ${msg}` : msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    return () => {
      clearCapture();
      clearUpload();
      stopCamera();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Container fluid className="py-4">
      <Row className="g-3">
        <Col xs={12}>
          <Card className="shadow-sm" style={{ border: 'none' }}>
            <Card.Header className="text-white d-flex justify-content-between align-items-center" style={{ backgroundColor: '#212529', border: 'none' }}>
              <div className="d-flex align-items-center gap-2">
                <FaCamera />
                <span className="fw-semibold">Mô phỏng ESP32 - Chấm công RFID + Ảnh</span>
              </div>
              <Badge bg={cameraOn ? 'success' : 'secondary'}>
                {cameraOn ? 'Camera: ON' : 'Camera: OFF'}
              </Badge>
            </Card.Header>
            <Card.Body className="p-4">
              <Row className="g-3">
                <Col xs={12} md={4}>
                  <Card className="shadow-sm" style={{ border: 'none', backgroundColor: '#f8f9fa' }}>
                    <Card.Body>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-semibold">RFID (bắt buộc)</Form.Label>
                        <Form.Control
                          value={rfid}
                          onChange={(e) => setRfid(e.target.value)}
                          placeholder="Nhập UID/RFID..."
                        />
                      </Form.Group>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-semibold">Mã thiết bị (tuỳ chọn)</Form.Label>
                        <Form.Control
                          value={maThietBi}
                          onChange={(e) => setMaThietBi(e.target.value)}
                          placeholder="VD: TB01"
                        />
                      </Form.Group>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-semibold">X-API-Key (tuỳ chọn)</Form.Label>
                        <Form.Control
                          value={apiKey}
                          onChange={(e) => setApiKey(e.target.value)}
                          placeholder="Nhập API key của thiết bị..."
                        />
                        <Form.Text className="text-muted">
                          Nếu không dùng JWT đăng nhập, hãy dùng <code>X-API-Key</code> để có role <code>ESP32_DEVICE</code>.
                        </Form.Text>
                        
                      </Form.Group>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-semibold">Upload ảnh (tuỳ chọn)</Form.Label>
                        <Form.Control type="file" accept="image/*" onChange={handleUploadImage} />
                        <Form.Text className="text-muted">
                          Nếu đã upload ảnh thì hệ thống sẽ ưu tiên gửi ảnh upload (thay vì ảnh chụp webcam).
                        </Form.Text>
                        <div className="d-flex gap-2 mt-2">
                          <Button variant="outline-secondary" size="sm" onClick={clearUpload} disabled={!uploadedBlob}>
                            Xóa ảnh upload
                          </Button>
                        </div>
                      </Form.Group>

                      <div className="d-grid gap-2">
                        {!cameraOn ? (
                          <Button variant="dark" onClick={startCamera}>
                            <FaPlay className="me-2" />
                            Bật camera
                          </Button>
                        ) : (
                          <Button variant="outline-dark" onClick={stopCamera}>
                            <FaStop className="me-2" />
                            Tắt camera
                          </Button>
                        )}
                        <Button variant="primary" onClick={capturePhoto} disabled={!cameraOn}>
                          <FaCamera className="me-2" />
                          Chụp ảnh
                        </Button>
                        <Button variant="outline-secondary" onClick={clearCapture} disabled={!capturedBlob}>
                          <FaUndo className="me-2" />
                          Chụp lại
                        </Button>
                        <Button variant="success" onClick={submit} disabled={!canSubmit || loading}>
                          {loading ? (
                            <>
                              <Spinner animation="border" size="sm" className="me-2" />
                              Đang gửi...
                            </>
                          ) : (
                            <>
                              <FaUpload className="me-2" />
                              Gửi request
                            </>
                          )}
                        </Button>
                      </div>

                      
                    </Card.Body>
                  </Card>
                </Col>

                <Col xs={12} md={8}>
                  <Card className="shadow-sm" style={{ border: 'none' }}>
                    <Card.Body>
                      <Row className="g-3">
                        <Col xs={12} md={6}>
                          <div className="fw-semibold mb-2">Preview camera</div>
                          <div style={{ border: '1px solid #dee2e6', borderRadius: 8, overflow: 'hidden', background: '#000' }}>
                            <video ref={videoRef} style={{ width: '100%', height: 320, objectFit: 'cover' }} playsInline muted />
                          </div>
                        </Col>
                        <Col xs={12} md={6}>
                          <div className="fw-semibold mb-2">Ảnh đã chọn (upload hoặc chụp)</div>
                          <div style={{ border: '1px solid #dee2e6', borderRadius: 8, overflow: 'hidden', background: '#f8f9fa', height: 320, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            {(uploadedUrl || capturedUrl) ? (
                              <img src={uploadedUrl || capturedUrl} alt="selected" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                            ) : (
                              <div className="text-muted">Chưa có ảnh</div>
                            )}
                          </div>
                        </Col>
                      </Row>

                      <canvas ref={canvasRef} style={{ display: 'none' }} />

                      <div className="mt-3">
                        <div className="fw-semibold mb-2">Kết quả trả về</div>
                        <pre className="bg-light p-3 rounded mb-0" style={{ border: '1px solid #dee2e6', minHeight: 110 }}>
                          {result ? JSON.stringify(result, null, 2) : 'Chưa gửi request.'}
                        </pre>
                      </div>
                    </Card.Body>
                  </Card>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default Esp32Simulator;

