package com.rfid.attendance;

import com.rfid.attendance.entity.User;
import com.rfid.attendance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RfidAttendanceSystemApplication implements CommandLineRunner {

    @Autowired
    private UserService userService;

    public static void main(String[] args) {
        SpringApplication.run(RfidAttendanceSystemApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Tạo default admin user khi khởi động ứng dụng
        User admin = userService.createDefaultAdmin();
        if (admin != null) {
            System.out.println("=== TÀI KHOẢN MẶC ĐỊNH ĐÃ ĐƯỢC TẠO ===");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");
            System.out.println("Role: ADMIN");
            System.out.println("=====================================");
        } else {
            System.out.println("Tài khoản admin đã tồn tại trong hệ thống");
        }
    }
}
