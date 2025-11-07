package com.rfid.desktop.websocket;

import com.rfid.desktop.service.SessionManager;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles the Socket.IO connection for realtime attendance updates.
 */
public class WebSocketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketService.class);
    private static final String DEFAULT_SOCKET_URL = "http://localhost:8099";

    private final CopyOnWriteArrayList<AttendanceUpdateListener> attendanceListeners = new CopyOnWriteArrayList<>();
    private Socket socket;

    public void addAttendanceListener(AttendanceUpdateListener listener) {
        attendanceListeners.add(Objects.requireNonNull(listener));
    }

    public void removeAttendanceListener(AttendanceUpdateListener listener) {
        attendanceListeners.remove(listener);
    }

    public synchronized void connect() {
        if (socket != null && socket.connected()) {
            return;
        }

        String url = Optional.ofNullable(System.getProperty("rfid.socket.url"))
                .orElseGet(() -> Optional.ofNullable(System.getenv("RFID_SOCKET_URL"))
                        .orElse(DEFAULT_SOCKET_URL));

        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionAttempts = 10;
            options.reconnectionDelay = 1000;
            options.transports = new String[]{"websocket", "polling"};

            Map<String, String> query = new HashMap<>();
            SessionManager.getToken().ifPresent(token -> query.put("token", token));
            options.query = buildQueryString(query);

            socket = IO.socket(url, options);
            socket.on(Socket.EVENT_CONNECT, onConnect());
            socket.on(Socket.EVENT_DISCONNECT, onDisconnect());
            socket.on(Socket.EVENT_CONNECT_ERROR, onError("connect_error"));
            socket.on("connect_timeout", onError("connect_timeout"));
            socket.on("update-attendance", onAttendanceUpdate());
            socket.connect();
        } catch (URISyntaxException e) {
            LOGGER.error("Không thể kết nối tới WebSocket: {}", e.getMessage());
        }
    }

    public synchronized void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
        }
    }

    private String buildQueryString(Map<String, String> query) {
        if (query.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        query.forEach((key, value) -> {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(key).append('=').append(value);
        });
        return builder.toString();
    }

    private Emitter.Listener onConnect() {
        return args -> LOGGER.info("Kết nối WebSocket thành công");
    }

    private Emitter.Listener onDisconnect() {
        return args -> LOGGER.info("WebSocket đã ngắt kết nối");
    }

    private Emitter.Listener onError(String type) {
        return args -> LOGGER.error("Sự cố WebSocket [{}]: {}", type, args != null && args.length > 0 ? args[0] : "unknown");
    }

    private Emitter.Listener onAttendanceUpdate() {
        return args -> {
            if (args == null || args.length == 0) {
                return;
            }
            Object payload = args[0];
            for (AttendanceUpdateListener listener : attendanceListeners) {
                listener.onAttendanceUpdated(payload);
            }
        };
    }

    public interface AttendanceUpdateListener {
        void onAttendanceUpdated(Object payload);
    }
}

