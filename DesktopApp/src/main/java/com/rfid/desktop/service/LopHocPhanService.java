package com.rfid.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rfid.desktop.model.LopHocPhan;
import com.rfid.desktop.model.PagedResult;
import com.rfid.desktop.model.Student;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LopHocPhanService {

    private final ApiClient apiClient;

    public LopHocPhanService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<LopHocPhan> getAll() throws IOException {
        return apiClient.get("/lophocphan", new TypeReference<List<LopHocPhan>>() {});
    }

    public PagedResult<LopHocPhan> getPaged(int page, int size, String keyword) throws IOException {
        StringBuilder path = new StringBuilder("/lophocphan/paged?page=")
                .append(page)
                .append("&size=")
                .append(size);
        if (keyword != null && !keyword.isBlank()) {
            path.append("&keyword=")
                    .append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        }
        return apiClient.get(path.toString(), new TypeReference<PagedResult<LopHocPhan>>() {});
    }

    public List<Student> getStudents(String maLopHocPhan) throws IOException {
        return apiClient.get("/lophocphan/" + maLopHocPhan + "/sinhvien", new TypeReference<List<Student>>() {});
    }

    public void create(LopHocPhan payload) throws IOException {
        apiClient.post("/lophocphan", payload, Void.class);
    }

    public void update(String maLopHocPhan, LopHocPhan payload) throws IOException {
        apiClient.put("/lophocphan/" + maLopHocPhan, payload, Void.class);
    }

    public void delete(String maLopHocPhan) throws IOException {
        apiClient.delete("/lophocphan/" + maLopHocPhan);
    }

    public void addStudent(String maLopHocPhan, String maSinhVien) throws IOException {
        Map<String, String> payload = new HashMap<>();
        payload.put("maLopHocPhan", maLopHocPhan);
        payload.put("maSinhVien", maSinhVien);
        apiClient.post("/lophocphan/add-student", payload, Void.class);
    }

    public void removeStudent(String maLopHocPhan, String maSinhVien) throws IOException {
        Map<String, String> payload = new HashMap<>();
        payload.put("maLopHocPhan", maLopHocPhan);
        payload.put("maSinhVien", maSinhVien);
        apiClient.post("/lophocphan/remove-student", payload, Void.class);
    }
}

