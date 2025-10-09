import React, { useState } from 'react';
import { Container, Row, Col, Card, Form, Button, Alert } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import ta1Background from '../assets/ta1-background.jpg';

const Login = () => {
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      const data = await response.json();

      if (response.ok) {
        // Sử dụng AuthContext để login
        login(data.token, data.user);
        
        toast.success('Đăng nhập thành công!');
        navigate('/dashboard');
      } else {
        setError(data.error || 'Đăng nhập thất bại');
        toast.error(data.error || 'Đăng nhập thất bại');
      }
    } catch (error) {
      setError('Có lỗi xảy ra khi đăng nhập');
      toast.error('Có lỗi xảy ra khi đăng nhập');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      minHeight: '100vh', 
      backgroundImage: `url(${ta1Background})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
      position: 'relative',
      display: 'flex',
      alignItems: 'center'
    }}>
      {/* Overlay để làm tối background */}
      <div style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.6)',
        zIndex: 1
      }}></div>
      <Container style={{ position: 'relative', zIndex: 2 }}>
        <Row className="justify-content-center">
          <Col md={6} lg={4}>
            <Card className="shadow-lg border-0">
              <Card.Body className="p-5">
                  <div className="text-center mb-4">
                    <h2 style={{color: 'rgb(32, 36, 40)'}} className="fw-bold">Đăng nhập</h2>
                    <p style={{color: 'rgb(32, 36, 40)'}}>Hệ thống quản lý điểm danh RFID - KMA</p>
                  </div>

                {error && (
                  <Alert variant="danger" className="mb-3">
                    {error}
                  </Alert>
                )}

                <Form onSubmit={handleSubmit}>
                  <Form.Group className="mb-3">
                    <Form.Label style={{color: 'rgb(32, 36, 40)'}}>Tên đăng nhập</Form.Label>
                    <Form.Control
                      type="text"
                      name="username"
                      value={formData.username}
                      onChange={handleInputChange}
                      required
                      placeholder="Nhập tên đăng nhập"
                      disabled={loading}
                    />
                  </Form.Group>

                  <Form.Group className="mb-4">
                    <Form.Label style={{color: 'rgb(32, 36, 40)'}}>Mật khẩu</Form.Label>
                    <Form.Control
                      type="password"
                      name="password"
                      value={formData.password}
                      onChange={handleInputChange}
                      required
                      placeholder="Nhập mật khẩu"
                      disabled={loading}
                    />
                  </Form.Group>

                  <Button 
                    variant="primary" 
                    type="submit" 
                    style={{backgroundColor: '#f12c29',borderColor: '#f12c29'}}
                    className="w-100 mb-3"
                    disabled={loading}
                  >
                    {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
                  </Button>
                </Form>

            
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
    </div>
  );
};

export default Login;
