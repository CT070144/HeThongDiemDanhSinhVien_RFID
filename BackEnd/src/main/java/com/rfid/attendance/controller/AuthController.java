package com.rfid.attendance.controller;

import com.rfid.attendance.entity.User;
import com.rfid.attendance.repository.UserRepository;
import com.rfid.attendance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Tìm user theo username
            Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Tên đăng nhập hoặc mật khẩu không đúng"
                ));
            }
            
            User user = userOpt.get();
            
            // Kiểm tra user có active không
            if (!user.getIsActive()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Tài khoản đã bị vô hiệu hóa"
                ));
            }
            
            // Kiểm tra password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Tên đăng nhập hoặc mật khẩu không đúng"
                ));
            }
            
            // Tạo JWT token
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
            
            // Cập nhật last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            // Trả về thông tin user và token
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "roleDescription", user.getRole().getDescription()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Có lỗi xảy ra khi đăng nhập"
            ));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Với JWT, logout thường được xử lý ở frontend bằng cách xóa token
        // Ở đây chúng ta chỉ trả về success message
        return ResponseEntity.ok(Map.of(
            "message", "Đăng xuất thành công"
        ));
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Token không hợp lệ"
                ));
            }
            
            String token = authHeader.substring(7);
            
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Token đã hết hạn hoặc không hợp lệ"
                ));
            }
            
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);
            
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Người dùng không tồn tại"
                ));
            }
            
            User user = userOpt.get();
            
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "fullName", user.getFullName(),
                    "email", user.getEmail(),
                    "role", user.getRole().name(),
                    "roleDescription", user.getRole().getDescription()
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Token không hợp lệ"
            ));
        }
    }
    
    // Inner classes for request/response
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
