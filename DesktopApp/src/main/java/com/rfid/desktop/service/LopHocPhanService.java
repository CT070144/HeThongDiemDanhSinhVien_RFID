package com.rfid.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.rfid.desktop.model.LopHocPhan;
import com.rfid.desktop.model.PagedResult;
import com.rfid.desktop.model.Student;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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

    /**
     * Get sessions (ngayHoc, ca) for a class by class name
     * @param tenLopHocPhan class name
     * @return List of sessions, each session is a Map with "ngayHoc" (LocalDate) and "ca" (Integer)
     */
    public List<Map<String, Object>> getSessions(String tenLopHocPhan) throws IOException {
        String encodedName = URLEncoder.encode(tenLopHocPhan, StandardCharsets.UTF_8);
        JsonNode jsonNode = apiClient.getRaw("/cahoc/sessions?lopHocPhan=" + encodedName);
        
        List<Map<String, Object>> sessions = new ArrayList<>();
        if (jsonNode != null && jsonNode.isArray()) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = apiClient.getObjectMapper();
            for (JsonNode sessionNode : jsonNode) {
                Map<String, Object> session = new HashMap<>();
                if (sessionNode.has("ngayHoc") && !sessionNode.get("ngayHoc").isNull()) {
                    try {
                        // Try to parse as LocalDate using ObjectMapper
                        LocalDate ngayHoc = mapper.treeToValue(sessionNode.get("ngayHoc"), LocalDate.class);
                        session.put("ngayHoc", ngayHoc);
                    } catch (Exception e) {
                        // Fallback: parse as string
                        String ngayHocStr = sessionNode.get("ngayHoc").asText();
                        if (ngayHocStr != null && !ngayHocStr.isEmpty()) {
                            session.put("ngayHoc", LocalDate.parse(ngayHocStr));
                        }
                    }
                }
                if (sessionNode.has("ca") && !sessionNode.get("ca").isNull()) {
                    session.put("ca", sessionNode.get("ca").asInt());
                }
                if (!session.isEmpty()) {
                    sessions.add(session);
                }
            }
        }
        return sessions;
    }
}

