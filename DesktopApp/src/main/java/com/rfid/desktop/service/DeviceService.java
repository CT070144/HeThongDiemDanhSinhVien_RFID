package com.rfid.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rfid.desktop.model.Device;

import java.io.IOException;
import java.util.List;

public class DeviceService {

    private final ApiClient apiClient;

    public DeviceService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<Device> getAll() throws IOException {
        return apiClient.get("/thietbi", new TypeReference<List<Device>>() {});
    }

    public Device getOne(String maThietBi) throws IOException {
        return apiClient.get("/thietbi/" + maThietBi, Device.class);
    }

    public Device create(Device device) throws IOException {
        return apiClient.post("/thietbi", device, Device.class);
    }

    public Device update(String maThietBi, Device device) throws IOException {
        return apiClient.put("/thietbi/" + maThietBi, device, Device.class);
    }

    public void delete(String maThietBi) throws IOException {
        apiClient.delete("/thietbi/" + maThietBi);
    }
}

