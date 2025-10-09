-- Script tạo bảng users cho hệ thống authentication
USE rfid_attendance_system;

-- Tạo bảng users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    role ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL
);

-- Tạo index cho username và email
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(is_active);

-- Insert default admin user (password: admin123 - đã được hash bằng BCrypt)
-- BCrypt hash của "admin123" với salt rounds 10
INSERT INTO users (username, password, full_name, email, role, is_active) 
VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Quản trị viên hệ thống', 'admin@example.com', 'ADMIN', TRUE)
ON DUPLICATE KEY UPDATE 
    password = VALUES(password),
    full_name = VALUES(full_name),
    email = VALUES(email),
    role = VALUES(role),
    is_active = VALUES(is_active);

-- Kiểm tra dữ liệu
SELECT '=== KIỂM TRA BẢNG USERS ===' as '';
SELECT 
    id,
    username,
    full_name,
    email,
    role,
    is_active,
    created_at,
    last_login
FROM users;

SELECT '=== HOÀN THÀNH TẠO BẢNG USERS ===' as '';
