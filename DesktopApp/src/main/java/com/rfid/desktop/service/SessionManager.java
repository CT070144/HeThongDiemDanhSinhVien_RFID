package com.rfid.desktop.service;

import com.rfid.desktop.model.UserAccount;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stores the authentication context (JWT token and user info) for the desktop app lifetime.
 */
public final class SessionManager {

    private static final AtomicReference<String> TOKEN = new AtomicReference<>();
    private static final AtomicReference<UserAccount> USER = new AtomicReference<>();

    private SessionManager() {
    }

    public static void setSession(String token, UserAccount user) {
        TOKEN.set(token);
        USER.set(user);
    }

    public static Optional<String> getToken() {
        return Optional.ofNullable(TOKEN.get());
    }

    public static Optional<UserAccount> getUser() {
        return Optional.ofNullable(USER.get());
    }

    public static void clear() {
        TOKEN.set(null);
        USER.set(null);
    }
}

