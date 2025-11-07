package com.rfid.desktop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Thin wrapper around OkHttp for performing JSON-based REST calls with automatic
 * JWT propagation.
 */
public class ApiClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public <T> T get(String path, Class<T> responseType) throws IOException {
        Request request = authorisedBuilder(path)
                .get()
                .build();
        return execute(request, responseType);
    }

    public <T> T get(String path, TypeReference<T> typeReference) throws IOException {
        Request request = authorisedBuilder(path)
                .get()
                .build();
        return execute(request, typeReference);
    }

    public <T> T post(String path, Object body, Class<T> responseType) throws IOException {
        Request request = authorisedBuilder(path)
                .post(jsonBody(body))
                .build();
        return execute(request, responseType);
    }

    public <T> T post(String path, Object body, TypeReference<T> typeReference) throws IOException {
        Request request = authorisedBuilder(path)
                .post(jsonBody(body))
                .build();
        return execute(request, typeReference);
    }

    public <T> T put(String path, Object body, Class<T> responseType) throws IOException {
        Request request = authorisedBuilder(path)
                .put(jsonBody(body))
                .build();
        return execute(request, responseType);
    }

    public void delete(String path) throws IOException {
        Request request = authorisedBuilder(path)
                .delete()
                .build();
        execute(request);
    }

    public JsonNode getRaw(String path) throws IOException {
        Request request = authorisedBuilder(path)
                .get()
                .build();
        return execute(request, JsonNode.class);
    }

    private RequestBody jsonBody(Object body) throws JsonProcessingException {
        if (body == null) {
            return RequestBody.create(new byte[0], JSON);
        }
        String json = objectMapper.writeValueAsString(body);
        return RequestBody.create(json, JSON);
    }

    private Request.Builder authorisedBuilder(String path) {
        String url = path.startsWith("http") ? path : baseUrl + path;
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json");

        Optional<String> token = SessionManager.getToken();
        token.ifPresent(t -> builder.addHeader("Authorization", "Bearer " + t));
        return builder;
    }

    private void execute(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw buildException(response);
            }
        } catch (SocketTimeoutException ex) {
            throw new IOException("Yêu cầu tới máy chủ bị hết thời gian chờ", ex);
        }
    }

    private <T> T execute(Request request, Class<T> responseType) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw buildException(response);
            }
            if (responseType == Void.class || response.body() == null) {
                return null;
            }
            try (ResponseBody body = response.body()) {
                String json = body != null ? body.string() : null;
                if (json == null || json.isBlank()) {
                    return null;
                }
                if (responseType == String.class) {
                    return responseType.cast(json);
                }
                return objectMapper.readValue(json, responseType);
            }
        } catch (SocketTimeoutException ex) {
            throw new IOException("Yêu cầu tới máy chủ bị hết thời gian chờ", ex);
        }
    }

    private <T> T execute(Request request, TypeReference<T> typeReference) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw buildException(response);
            }
            try (ResponseBody body = response.body()) {
                String json = body != null ? body.string() : null;
                if (json == null || json.isBlank()) {
                    return null;
                }
                return objectMapper.readValue(json, typeReference);
            }
        } catch (SocketTimeoutException ex) {
            throw new IOException("Yêu cầu tới máy chủ bị hết thời gian chờ", ex);
        }
    }

    private IOException buildException(Response response) throws IOException {
        String errorBody = null;
        try (ResponseBody body = response.body()) {
            if (body != null) {
                errorBody = body.string();
            }
        }

        if (errorBody != null && !errorBody.isBlank()) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(errorBody, new TypeReference<>() {});
                Object message = parsed.getOrDefault("error", parsed.getOrDefault("message", errorBody));
                return new IOException(message.toString());
            } catch (Exception ignored) {
                // fall back to raw body text
            }
        }

        return new IOException("Máy chủ trả về lỗi HTTP " + response.code());
    }
}

