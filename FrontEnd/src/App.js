import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { NotificationProvider } from './contexts/NotificationContext';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Login from './pages/Login';
import StudentManagement from './pages/StudentManagement';
import AttendanceHistory from './pages/AttendanceHistory';
import Configuration from './pages/Configuration';
import Dashboard from './pages/Dashboard';
import LopHocPhanManagement from './pages/LopHocPhanManagement';
import Room from './pages/Room';
import './App.css';

// Protected Route Component
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  
  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '100vh' }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }
  
  return isAuthenticated() ? children : <Navigate to="/login" replace />;
};

// Public Route Component (redirect to dashboard if already logged in)
const PublicRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  
  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '100vh' }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }
  
  return isAuthenticated() ? <Navigate to="/dashboard" replace /> : children;
};

function AppContent() {
  const { isAuthenticated } = useAuth();
  
  return (
    <div className="App d-flex flex-column" style={{ minHeight: '100vh' }}>
      {isAuthenticated() && <Navbar />}
      <div className={isAuthenticated() ? "container-fluid flex-grow-1" : ""} style={{ flex: 1 }}>
        <Routes>
          <Route path="/login" element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          } />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <Dashboard/>
            </ProtectedRoute>
          } />
          <Route path="/students" element={
            <ProtectedRoute>
              <StudentManagement />
            </ProtectedRoute>
          } />
          <Route path="/lophocphan" element={
            <ProtectedRoute>
              <LopHocPhanManagement />
            </ProtectedRoute>
          } />
          <Route path="/attendance" element={
            <ProtectedRoute>
              <AttendanceHistory />
            </ProtectedRoute>
          } />
          <Route path="/configuration" element={
            <ProtectedRoute>
              <Configuration />
            </ProtectedRoute>
          } />
          <Route path="/room" element={
            <ProtectedRoute>
              <Room />
            </ProtectedRoute>
          } />
        </Routes>
      </div>
      {isAuthenticated() && <Footer />}
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <Router>
          <AppContent />
        </Router>
      </NotificationProvider>
    </AuthProvider>
  );
}

export default App;
