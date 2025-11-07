package com.rfid.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rfid.desktop.model.AttendanceRecord;
import com.rfid.desktop.model.RfidEvent;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceService {

    private final ApiClient apiClient;

    public AttendanceService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<AttendanceRecord> getAll() throws IOException {
        return apiClient.get("/attendance", new TypeReference<List<AttendanceRecord>>() {});
    }

    public List<AttendanceRecord> getToday() throws IOException {
        return apiClient.get("/attendance/today", new TypeReference<List<AttendanceRecord>>() {});
    }

    public List<AttendanceRecord> getByStudent(String maSinhVien) throws IOException {
        return apiClient.get("/attendance/student/" + urlEncode(maSinhVien), new TypeReference<List<AttendanceRecord>>() {});
    }

    public List<AttendanceRecord> filter(String ngay, Integer ca, String maSinhVien, String phongHoc) throws IOException {
        StringBuilder query = new StringBuilder("/attendance/filter?");
        if (ngay != null && !ngay.isBlank()) {
            query.append("ngay=").append(urlEncode(ngay)).append("&");
        }
        if (ca != null) {
            query.append("ca=").append(ca).append("&");
        }
        if (maSinhVien != null && !maSinhVien.isBlank()) {
            query.append("maSinhVien=").append(urlEncode(maSinhVien)).append("&");
        }
        if (phongHoc != null && !phongHoc.isBlank()) {
            query.append("phongHoc=").append(urlEncode(phongHoc)).append("&");
        }
        String path = query.toString();
        if (path.endsWith("&") || path.endsWith("?")) {
            path = path.substring(0, path.length() - 1);
        }
        return apiClient.get(path, new TypeReference<List<AttendanceRecord>>() {});
    }

    public Map<String, Object> processRfid(String rfid, String maThietBi) throws IOException {
        Map<String, String> payload = new HashMap<>();
        payload.put("rfid", rfid);
        payload.put("maThietBi", maThietBi);
        return apiClient.post("/attendance/rfid", payload, new TypeReference<Map<String, Object>>() {});
    }

    public List<RfidEvent> getUnprocessedRfids() throws IOException {
        return apiClient.get("/attendance/unprocessed-rfids", new TypeReference<List<RfidEvent>>() {});
    }

    public void markProcessed(Long id) throws IOException {
        apiClient.put("/attendance/mark-processed/" + id, Map.of("processed", true), Void.class);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

