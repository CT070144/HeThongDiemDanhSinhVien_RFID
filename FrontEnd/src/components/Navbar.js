import React from 'react';
import { Navbar, Nav, Container, Dropdown } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import logo from '../assets/logo.png';

const CustomNavbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    toast.success('Đăng xuất thành công!');
    navigate('/login');
  };

  return (
    <Navbar bg="dark" variant="dark" expand="lg">
      <Container>
        
        <LinkContainer to="/dashboard">
          <Navbar.Brand>
            <img
              src={logo}
              width="72"
              height="72"
              style={{ marginRight: '10px',borderRadius: '40px' }}
              className="d-inline-block align-top"
              alt="Logo"
            />
            <span style={{ fontSize: '24px', lineHeight: '72px' }}>Hệ thống điểm danh RFID</span>
          </Navbar.Brand>
        </LinkContainer>

        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            <LinkContainer to="/dashboard">
              <Nav.Link>Dashboard</Nav.Link>
            </LinkContainer>
            <LinkContainer to="/students">
              <Nav.Link>Sinh viên</Nav.Link>
            </LinkContainer>
            <LinkContainer to="/lophocphan">
              <Nav.Link>Lớp học phần</Nav.Link>
            </LinkContainer>
            <LinkContainer to="/attendance">
              <Nav.Link>Lịch sử điểm danh</Nav.Link>
            </LinkContainer>
            <LinkContainer to="/rfid-reader">
              <Nav.Link>Thiết lập RFID</Nav.Link>
            </LinkContainer>
          </Nav>
          
          {/* User info and logout */}
          <Nav>
            <Dropdown align="end">
              <Dropdown.Toggle variant="outline-light" id="dropdown-basic">
                <i className="fas fa-user-circle me-2"></i>
                {user?.fullName || user?.username}
              </Dropdown.Toggle>

              <Dropdown.Menu>
                <Dropdown.Header>
                  <div className="text-center">
                    <div className="fw-bold">{user?.fullName}</div>
                    <small className="text-muted">{user?.username}</small>
                    <br />
                    <span className="badge bg-primary">{user?.roleDescription}</span>
                  </div>
                </Dropdown.Header>
                <Dropdown.Divider />
                <Dropdown.Item onClick={handleLogout}>
                  <i className="fas fa-sign-out-alt me-2"></i>
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
