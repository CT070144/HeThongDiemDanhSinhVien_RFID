import React from 'react';
import { Navbar, Nav, Container } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import logo from '../assets/logo.png';

const CustomNavbar = () => {
  return (
    <Navbar bg="dark" variant="dark" expand="lg">
      <Container>
        
        <LinkContainer to="/">
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
            <LinkContainer to="/">
              <Nav.Link>Dashboard</Nav.Link>
            </LinkContainer>
            <LinkContainer to="/students">
              <Nav.Link>Quản lý sinh viên</Nav.Link>
            </LinkContainer>
            <LinkContainer to="/attendance">
              <Nav.Link>Lịch sử điểm danh</Nav.Link>
            </LinkContainer>
            <LinkContainer to="/rfid-reader">
              <Nav.Link>Đọc RFID</Nav.Link>
            </LinkContainer>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default CustomNavbar;
