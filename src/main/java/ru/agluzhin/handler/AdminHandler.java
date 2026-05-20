package ru.agluzhin.handler;

import ru.agluzhin.exception.UserNotFoundException;
import ru.agluzhin.model.User;
import ru.agluzhin.service.AdminService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Handles admin-only endpoints (protected by JwtFilter with Role.ADMIN):
 *   POST   /admin/otp-config         — update OTP config
 *   GET    /admin/users              — list all non-admin users
 *   DELETE /admin/users/{id}         — delete user by id
 */
public class AdminHandler extends BaseHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

    private final AdminService adminService;

    public AdminHandler(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();

        log.info("→ {} {} userId={}", method, path, getUserId(exchange));

        try {
            if ("POST".equalsIgnoreCase(method) && path.endsWith("/otp-config")) {
                handleUpdateConfig(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.endsWith("/users")) {
                handleListUsers(exchange);
            } else if ("DELETE".equalsIgnoreCase(method) && path.matches(".*/users/\\d+")) {
                handleDeleteUser(exchange);
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in AdminHandler", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleUpdateConfig(HttpExchange exchange) throws IOException {
        Map<String, Object> body = parseJson(readBody(exchange));

        int codeLength = requireInt(body, "codeLength");
        int ttlSeconds = requireInt(body, "ttlSeconds");

        adminService.updateOtpConfig(codeLength, ttlSeconds);
        log.info("← 200 OTP config updated codeLength={} ttlSeconds={}", codeLength, ttlSeconds);
        sendOk(exchange, "OTP configuration updated");
    }

    private void handleListUsers(HttpExchange exchange) throws IOException {
        List<User> users = adminService.listUsers();

        // Return only safe fields — never expose password hashes
        List<Map<String, Object>> response = users.stream()
                .map(u -> Map.<String, Object>of(
                        "id",    u.getId(),
                        "login", u.getLogin(),
                        "role",  u.getRole().name()))
                .toList();

        log.info("← 200 listing {} user(s)", users.size());
        sendJson(exchange, 200, response);
    }

    private void handleDeleteUser(HttpExchange exchange) throws IOException {
        long userId;
        try {
            userId = Long.parseLong(lastPathSegment(exchange));
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid user id");
            return;
        }

        try {
            adminService.deleteUser(userId);
            log.info("← 200 user deleted id={}", userId);
            sendOk(exchange, "User deleted");
        } catch (UserNotFoundException e) {
            sendError(exchange, 404, e.getMessage());
        }
    }

}
