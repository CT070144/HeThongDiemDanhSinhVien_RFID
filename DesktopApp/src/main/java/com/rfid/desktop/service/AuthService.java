package com.rfid.desktop.service;

import com.rfid.desktop.model.AuthResponse;
import com.rfid.desktop.model.UserAccount;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final ApiClient apiClient;

    public AuthService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public AuthResponse login(String username, String password) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);

        AuthResponse response = apiClient.post("/auth/login", payload, AuthResponse.class);
        if (response != null) {
            SessionManager.setSession(response.getToken(), response.getUser());
        }
        return response;
    }

    public void logout() {
        SessionManager.clear();
    }

    public UserAccount getAuthenticatedUser() {
        return SessionManager.getUser().orElse(null);
    }
}

