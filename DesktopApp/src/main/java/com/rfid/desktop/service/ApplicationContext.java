package com.rfid.desktop.service;

import com.rfid.desktop.websocket.WebSocketService;

import java.util.Objects;

/**
 * Simple service locator to share REST clients and services across Swing panels.
 */
public class ApplicationContext {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api";

    private final ApiClient apiClient;
    private final AuthService authService;
    private final StudentService studentService;
    private final AttendanceService attendanceService;
    private final DeviceService deviceService;
    private final LopHocPhanService lopHocPhanService;
    private final WebSocketService webSocketService;

    public ApplicationContext() {
        String baseUrl = Objects.requireNonNullElseGet(System.getProperty("rfid.api.base-url"),
                () -> Objects.requireNonNullElse(System.getenv("RFID_API_BASE_URL"), DEFAULT_BASE_URL));

        this.apiClient = new ApiClient(baseUrl);
        this.authService = new AuthService(apiClient);
        this.studentService = new StudentService(apiClient);
        this.attendanceService = new AttendanceService(apiClient);
        this.deviceService = new DeviceService(apiClient);
        this.lopHocPhanService = new LopHocPhanService(apiClient);
        this.webSocketService = new WebSocketService();
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public StudentService getStudentService() {
        return studentService;
    }

    public AttendanceService getAttendanceService() {
        return attendanceService;
    }

    public DeviceService getDeviceService() {
        return deviceService;
    }

    public LopHocPhanService getLopHocPhanService() {
        return lopHocPhanService;
    }

    public WebSocketService getWebSocketService() {
        return webSocketService;
    }
}

