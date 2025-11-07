package com.rfid.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rfid.desktop.model.Student;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StudentService {

    private final ApiClient apiClient;

    public StudentService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<Student> getAll() throws IOException {
        return apiClient.get("/sinhvien", new TypeReference<List<Student>>() {});
    }

    public Student getByRfid(String rfid) throws IOException {
        return apiClient.get("/sinhvien/" + rfid, Student.class);
    }

    public Student create(Student student) throws IOException {
        return apiClient.post("/sinhvien", student, Student.class);
    }

    public Student update(String maSinhVien, Student student) throws IOException {
        return apiClient.put("/sinhvien/" + maSinhVien, student, Student.class);
    }

    public void delete(String maSinhVien) throws IOException {
        apiClient.delete("/sinhvien/" + maSinhVien);
    }

    public Map<String, Object> bulkUpdate(List<Student> students) throws IOException {
        return apiClient.post("/sinhvien/bulk-update-rfid", students, new TypeReference<Map<String, Object>>() {});
    }

    public boolean checkExists(String rfid) throws IOException {
        Map<String, Object> response = apiClient.get("/sinhvien/exists/" + rfid, new TypeReference<Map<String, Object>>() {});
        Object exists = response != null ? response.get("exists") : null;
        if (exists instanceof Boolean) {
            return (Boolean) exists;
        }
        if (exists instanceof String) {
            return Boolean.parseBoolean((String) exists);
        }
        return false;
    }

    public List<Student> emptyListOnError(IOException ex) {
        return Collections.emptyList();
    }
}

