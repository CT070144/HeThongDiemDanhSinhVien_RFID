import React from 'react';
import { Container, Row, Col } from 'react-bootstrap';
import { FaEnvelope, FaPhone, FaMapMarkerAlt, FaCopyright } from 'react-icons/fa';

const Footer = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer 
      className="text-white mt-auto" 
      style={{ 
        backgroundColor: '#212529',
        borderTop: '3px solid #212529',
        padding: '2rem 0',
        marginTop: '3rem'
      }}
    >
      <Container>
        <Row className="g-4">
          <Col md={4}>
            <h5 className="mb-3" style={{ color: '#f8f9fa', fontWeight: '600' }}>
              Hệ thống chấm công 
            </h5>
            <p className="text-white-50 mb-0" style={{ fontSize: '0.9rem', lineHeight: '1.6' }}>
              Hệ thống quản lý chấm công tự động sử dụng công nghệ RFID, 
              giúp theo dõi và quản lý nhân viên một cách hiệu quả và chính xác.
            </p>
          </Col>
          
          <Col md={4}>
            <h5 className="mb-3" style={{ color: '#f8f9fa', fontWeight: '600' }}>
              Thông tin liên hệ
            </h5>
            <div className="d-flex flex-column gap-2">
              <div className="d-flex align-items-center text-white-50" style={{ fontSize: '0.9rem' }}>
                <FaMapMarkerAlt className="me-2" style={{ color: '#adb5bd' }} />
                <span>Học viện Kỹ thuật Mật mã, Hà Nội</span>
              </div>
              <div className="d-flex align-items-center text-white-50" style={{ fontSize: '0.9rem' }}>
                <FaEnvelope className="me-2" style={{ color: '#adb5bd' }} />
                <span>info@kma.edu.vn</span>
              </div>
              <div className="d-flex align-items-center text-white-50" style={{ fontSize: '0.9rem' }}>
                <FaPhone className="me-2" style={{ color: '#adb5bd' }} />
                <span>+84 24 3832 1234</span>
              </div>
            </div>
          </Col>
          
          <Col md={4}>
            <h5 className="mb-3" style={{ color: '#f8f9fa', fontWeight: '600' }}>
              Hỗ trợ
            </h5>
            <div className="d-flex flex-column gap-2">
              <a href="#" className="text-white-50 text-decoration-none" style={{ fontSize: '0.9rem' }}>
                Hướng dẫn sử dụng
              </a>
              <a href="#" className="text-white-50 text-decoration-none" style={{ fontSize: '0.9rem' }}>
                Câu hỏi thường gặp
              </a>
              <a href="#" className="text-white-50 text-decoration-none" style={{ fontSize: '0.9rem' }}>
                Báo lỗi hệ thống
              </a>
            </div>
          </Col>
        </Row>
        
        <hr className="my-4" style={{ borderColor: 'rgba(255,255,255,0.1)' }} />
        
        <Row>
          <Col className="text-center">
            <p className="mb-0 text-white-50 d-flex align-items-center justify-content-center" style={{ fontSize: '0.85rem' }}>
              <FaCopyright className="me-2" />
              {currentYear} Học viện Kỹ thuật Mật mã. Tất cả quyền được bảo lưu.
            </p>
          </Col>
        </Row>
      </Container>
    </footer>
  );
};

export default Footer;

