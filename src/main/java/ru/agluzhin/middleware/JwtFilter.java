package ru.agluzhin.middleware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.agluzhin.model.Role;
import ru.agluzhin.service.AuthService;
import ru.agluzhin.service.AuthService.TokenClaims;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Decorator that validates the JWT from the Authorization header and,
 * optionally, enforces a required role before delegating to the real handler.
 */
public class JwtFilter implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_LOGIN = "login";
    public static final String ATTR_ROLE = "role";

    private final HttpHandler delegate;
    private final AuthService authService;
    private final Role requiredRole;

    public JwtFilter(HttpHandler delegate, AuthService authService, Role requiredRole) {
        this.delegate = delegate;
        this.authService = authService;
        this.requiredRole = requiredRole;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(exchange, 401, "Missing or malformed Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        TokenClaims claims;
        try {
            claims = authService.parseToken(token);
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            sendError(exchange, 401, "Invalid or expired token");
            return;
        }

        if (requiredRole != null && claims.role() != requiredRole) {
            log.warn("Access denied: userId={} role={} requiredRole={}", claims.userId(), claims.role(), requiredRole);
            sendError(exchange, 403, "Access denied");
            return;
        }

        // Inject claims so handlers can read them without re-parsing the token
        exchange.setAttribute(ATTR_USER_ID, claims.userId());
        exchange.setAttribute(ATTR_LOGIN, claims.login());
        exchange.setAttribute(ATTR_ROLE, claims.role());

        delegate.handle(exchange);
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

}
