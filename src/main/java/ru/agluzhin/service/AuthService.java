package ru.agluzhin.service;

import ru.agluzhin.config.AppConfig;
import ru.agluzhin.dao.UserDao;
import ru.agluzhin.exception.AdminAlreadyExistsException;
import ru.agluzhin.exception.InvalidCredentialsException;
import ru.agluzhin.exception.UserAlreadyExistsException;
import ru.agluzhin.model.Role;
import ru.agluzhin.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final SecretKey jwtKey;
    private final long jwtExpirationMs;

    public AuthService() {
        this(new UserDao());
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
        AppConfig cfg = AppConfig.getInstance();
        this.jwtKey = Keys.hmacShaKeyFor(cfg.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = cfg.getJwtExpirationSeconds() * 1000L;
    }

    /**
     * Registers a new user. Throws if login taken or second admin attempted.
     */
    public void register(String login, String password, Role role)
            throws UserAlreadyExistsException, AdminAlreadyExistsException {

        validateLogin(login);
        validatePassword(password);

        if (userDao.findByLogin(login).isPresent()) {
            log.warn("Registration rejected: login '{}' already taken", login);
            throw new UserAlreadyExistsException(login);
        }

        if (role == Role.ADMIN && userDao.existsByRole(Role.ADMIN)) {
            log.warn("Registration rejected: admin already exists");
            throw new AdminAlreadyExistsException();
        }

        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setRole(role);
        userDao.save(user);

        log.info("User registered: login={} role={}", login, role);
    }

    /**
     * Authenticates user and returns a signed JWT.
     */
    public String login(String login, String password) throws InvalidCredentialsException {
        User user = userDao.findByLogin(login)
                .orElseThrow(() -> {
                    log.warn("Login failed: user '{}' not found", login);
                    return new InvalidCredentialsException();
                });

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            log.warn("Login failed: wrong password for user '{}'", login);
            throw new InvalidCredentialsException();
        }

        String token = buildToken(user);
        log.info("User logged in: login={} role={}", login, user.getRole());
        return token;
    }

    private String buildToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("login", user.getLogin())
                .claim("role", user.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpirationMs))
                .signWith(jwtKey)
                .compact();
    }

    /**
     * Parses and validates a JWT. Returns the claims as a simple holder.
     */
    public TokenClaims parseToken(String token) {
        var claims = Jwts.parser()
                .verifyWith(jwtKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new TokenClaims(
                Long.parseLong(claims.getSubject()),
                claims.get("login", String.class),
                Role.valueOf(claims.get("role", String.class))
        );
    }

    // --- Validation helpers ---

    private void validateLogin(String login) {
        if (login == null || login.isBlank() || login.length() < 3 || login.length() > 100) {
            throw new IllegalArgumentException("Login must be 3–100 characters");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
    }

    /**
     * Immutable container for parsed JWT claims.
     */
    public record TokenClaims(long userId, String login, Role role) {
    }

}
