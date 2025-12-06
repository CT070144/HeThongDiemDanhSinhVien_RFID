import React from 'react';
import { Navbar, Nav, Container, Dropdown, Badge } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate, useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import logo from '../assets/logo.png';
import { FaHome, FaUsers, FaGraduationCap, FaHistory, FaCog, FaUserCircle, FaSignOutAlt, FaBuilding } from 'react-icons/fa';

const CustomNavbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    toast.success('Đăng xuất thành công!');
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <Navbar bg="primary" variant="dark" expand="lg" className="shadow-sm" style={{ position: 'sticky', top: 0, left: 0, right: 0, zIndex: 1000, borderBottom: '3px solid #0a58ca' }}>
      <Container fluid>
        <LinkContainer to="/dashboard">
          <Navbar.Brand className="d-flex align-items-center" style={{ cursor: 'pointer' }}>
            <img
              src={logo}
              width="60"
              height="60"
              style={{ marginRight: '15px', borderRadius: '50%', border: '3px solid rgba(255,255,255,0.3)' }}
              className="d-inline-block align-top"
              alt="Logo"
            />
            <div className="d-flex flex-column">
              <span style={{ fontSize: '20px', fontWeight: '600', lineHeight: '1.2' }}>Hệ thống điểm danh RFID</span>
              <small style={{ fontSize: '12px', opacity: 0.8, textAlign: 'left'}}>Học viện Kỹ thuật Mật mã</small>
            </div>
          </Navbar.Brand>
        </LinkContainer>

        <Navbar.Toggle aria-controls="basic-navbar-nav" style={{ borderColor: 'rgba(255,255,255,0.5)' }} />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto" style={{ gap: '5px' }}>
            <LinkContainer to="/dashboard">
              <Nav.Link 
                className={`d-flex align-items-center ${isActive('/dashboard') ? 'active' : ''}`}
                style={{ 
                  borderRadius: '0.375rem',
                  padding: '0.5rem 1rem',
                  fontWeight: isActive('/dashboard') ? '600' : '400',
                  backgroundColor: isActive('/dashboard') ? 'rgba(255,255,255,0.15)' : 'transparent',
                  transition: 'all 0.3s ease'
                }}
              >
                <FaHome className="me-2" />
                <span style={{ fontSize: '20px' }}>Dashboard</span>
              </Nav.Link>
            </LinkContainer>
            <LinkContainer to="/students">
              <Nav.Link 
                className={`d-flex align-items-center ${isActive('/students') ? 'active' : ''}`}
                style={{ 
                  borderRadius: '0.375rem',
                  padding: '0.5rem 1rem',
                  fontWeight: isActive('/students') ? '600' : '400',
                  backgroundColor: isActive('/students') ? 'rgba(255,255,255,0.15)' : 'transparent',
                  transition: 'all 0.3s ease'
                }}
              >
                <FaUsers className="me-2" />
                <span style={{ fontSize: '20px' }}>Sinh viên</span>
              </Nav.Link>
            </LinkContainer>
            <LinkContainer to="/lophocphan">
              <Nav.Link 
                className={`d-flex align-items-center ${isActive('/lophocphan') ? 'active' : ''}`}
                style={{ 
                  borderRadius: '0.375rem',
                  padding: '0.5rem 1rem',
                  fontWeight: isActive('/lophocphan') ? '600' : '400',
                  backgroundColor: isActive('/lophocphan') ? 'rgba(255,255,255,0.15)' : 'transparent',
                  transition: 'all 0.3s ease'
                }}
              >
                <FaGraduationCap className="me-2" />
                <span style={{ fontSize: '20px' }}>Lớp học phần</span>
              </Nav.Link>
            </LinkContainer>
            <LinkContainer to="/attendance">
              <Nav.Link 
                className={`d-flex align-items-center ${isActive('/attendance') ? 'active' : ''}`}
                style={{ 
                  borderRadius: '0.375rem',
                  padding: '0.5rem 1rem',
                  fontWeight: isActive('/attendance') ? '600' : '400',
                  backgroundColor: isActive('/attendance') ? 'rgba(255,255,255,0.15)' : 'transparent',
                  transition: 'all 0.3s ease'
                }}
              >
                <FaHistory className="me-2" />
                <span style={{ fontSize: '20px' }}>Lịch sử điểm danh</span>
              </Nav.Link>
            </LinkContainer>
            <LinkContainer to="/room">
              <Nav.Link 
                className={`d-flex align-items-center ${isActive('/room') ? 'active' : ''}`}
                style={{ 
                  borderRadius: '0.375rem',
                  padding: '0.5rem 1rem',
                  fontWeight: isActive('/room') ? '600' : '400',
                  backgroundColor: isActive('/room') ? 'rgba(255,255,255,0.15)' : 'transparent',
                  transition: 'all 0.3s ease'
                }}
              >
                <FaBuilding className="me-2" />
                <span style={{ fontSize: '20px' }}>Phòng học</span>
              </Nav.Link>
            </LinkContainer>
            <LinkContainer to="/configuration">
              <Nav.Link 
                className={`d-flex align-items-center ${isActive('/configuration') ? 'active' : ''}`}
                style={{ 
                  borderRadius: '0.375rem',
                  padding: '0.5rem 1rem',
                  fontWeight: isActive('/configuration') ? '600' : '400',
                  backgroundColor: isActive('/configuration') ? 'rgba(255,255,255,0.15)' : 'transparent',
                  transition: 'all 0.3s ease'
                }}
              >
                <FaCog className="me-2" />
                <span style={{ fontSize: '20px' }}>Thiết lập</span>
              </Nav.Link>
            </LinkContainer>
          </Nav>
          
          {/* User info and logout */}
          <Nav>
            <Dropdown align="end">
              <Dropdown.Toggle 
                variant="outline-light" 
                id="dropdown-basic"
                className="d-flex align-items-center shadow-sm"
                style={{ 
                  border: '2px solid rgba(255,255,255,0.3)',
                  borderRadius: '0.5rem',
                  padding: '0.5rem 1rem',
                  fontWeight: '500'
                }}
              >
                <FaUserCircle className="me-2" size={20} />
                {user?.fullName || user?.username}
              </Dropdown.Toggle>

              <Dropdown.Menu className="shadow-lg" style={{ border: 'none', borderRadius: '0.5rem', marginTop: '10px' }}>
                <Dropdown.Header className="bg-light">
                  <div className="text-center p-2">
                    <FaUserCircle size={48} className="text-primary mb-2" />
                    <div className="fw-bold">{user?.fullName}</div>
                    <small className="text-muted d-block">{user?.username}</small>
                    <Badge bg="primary" className="mt-2" style={{ fontSize: '0.85rem', padding: '0.4rem 0.8rem' }}>
                      {user?.roleDescription}
                    </Badge>
                  </div>
                </Dropdown.Header>
                <Dropdown.Divider />
                <Dropdown.Item 
                  onClick={handleLogout}
                  className="d-flex align-items-center"
                  style={{ 
                    color: '#dc3545',
                    fontWeight: '500',
                    padding: '0.75rem 1.25rem'
                  }}
                >
                  <FaSignOutAlt className="me-2" />
                  Đăng xuất
                </Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default CustomNavbar;
