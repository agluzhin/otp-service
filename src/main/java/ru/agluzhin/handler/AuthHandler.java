package ru.agluzhin.handler;

import ru.agluzhin.exception.AdminAlreadyExistsException;
import ru.agluzhin.exception.InvalidCredentialsException;
import ru.agluzhin.exception.UserAlreadyExistsException;
import ru.agluzhin.model.Role;
import ru.agluzhin.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Handles public authentication endpoints:
 *   POST /auth/register
 *   POST /auth/login
 */
public class AuthHandler extends BaseHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);

    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();

        log.info("→ {} {}", method, path);

        try {
            if ("POST".equalsIgnoreCase(method) && path.endsWith("/register")) {
                handleRegister(exchange);
            } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/login")) {
                handleLogin(exchange);
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in AuthHandler", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        Map<String, Object> body = parseJson(readBody(exchange));

        String login    = requireString(body, "login");
        String password = requireString(body, "password");
        String roleStr  = requireString(body, "role");

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "Role must be ADMIN or USER");
            return;
        }

        try {
            authService.register(login, password, role);
            log.info("← 201 registered login={} role={}", login, role);
            sendJson(exchange, 201, Map.of("message", "User registered successfully"));
        } catch (UserAlreadyExistsException e) {
            sendError(exchange, 409, e.getMessage());
        } catch (AdminAlreadyExistsException e) {
            sendError(exchange, 409, e.getMessage());
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, Object> body = parseJson(readBody(exchange));

        String login    = requireString(body, "login");
        String password = requireString(body, "password");

        try {
            String token = authService.login(login, password);
            log.info("← 200 login successful login={}", login);
            sendJson(exchange, 200, Map.of("token", token));
        } catch (InvalidCredentialsException e) {
            sendError(exchange, 401, e.getMessage());
        }
    }

}
