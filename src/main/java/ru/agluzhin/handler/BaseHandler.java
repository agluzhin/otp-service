package ru.agluzhin.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.agluzhin.middleware.JwtFilter;
import ru.agluzhin.model.Role;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Shared utilities for reading request bodies, writing JSON responses,
 * and extracting JWT-injected claims.
 */
public abstract class BaseHandler {

    private static final Logger log = LoggerFactory.getLogger(BaseHandler.class);
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseJson(String body) throws IOException {
        return MAPPER.readValue(body, Map.class);
    }

    protected String requireString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return val.toString().trim();
    }

    protected int requireInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Field '" + key + "' must be an integer");
        }
    }

    protected void sendJson(HttpExchange exchange, int status, Object responseBody) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(responseBody);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    protected void sendError(HttpExchange exchange, int status, String message) throws IOException {
        log.debug("Responding {} – {}", status, message);
        sendJson(exchange, status, Map.of("error", message));
    }

    protected void sendOk(HttpExchange exchange, String message) throws IOException {
        sendJson(exchange, 200, Map.of("message", message));
    }

    protected long getUserId(HttpExchange exchange) {
        return (long) exchange.getAttribute(JwtFilter.ATTR_USER_ID);
    }

    protected Role getRole(HttpExchange exchange) {
        return (Role) exchange.getAttribute(JwtFilter.ATTR_ROLE);
    }

    /**
     * Returns the last path segment, e.g. "/admin/users/42" → "42".
     * Returns empty string if the path ends with '/'.
     */
    protected String lastPathSegment(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "";
    }

}
