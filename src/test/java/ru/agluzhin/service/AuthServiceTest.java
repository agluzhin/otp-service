package ru.agluzhin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.agluzhin.dao.UserDao;
import ru.agluzhin.exception.AdminAlreadyExistsException;
import ru.agluzhin.exception.InvalidCredentialsException;
import ru.agluzhin.exception.UserAlreadyExistsException;
import ru.agluzhin.model.Role;
import ru.agluzhin.model.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDao userDao;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Provide a long-enough secret for HMAC-SHA256 (32+ bytes)
        System.setProperty("jwt.secret.override", "test-secret-that-is-long-enough-32bytes!!");
        authService = new AuthServiceTestable(userDao);
    }

    // --- register() ---

    @Test
    void register_success_user() throws Exception {
        when(userDao.findByLogin("alice")).thenReturn(Optional.empty());

        authService.register("alice", "password123", Role.USER);

        verify(userDao).save(any(User.class));
    }

    @Test
    void register_throws_whenLoginAlreadyTaken() {
        User existing = new User(1L, "alice", "hash", Role.USER);
        when(userDao.findByLogin("alice")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.register("alice", "password123", Role.USER))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userDao, never()).save(any());
    }

    @Test
    void register_throws_whenSecondAdminAttempted() {
        when(userDao.findByLogin("admin2")).thenReturn(Optional.empty());
        when(userDao.existsByRole(Role.ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> authService.register("admin2", "password123", Role.ADMIN))
                .isInstanceOf(AdminAlreadyExistsException.class);

        verify(userDao, never()).save(any());
    }

    @Test
    void register_throws_whenLoginTooShort() {
        assertThatThrownBy(() -> authService.register("ab", "password123", Role.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Login");
    }

    @Test
    void register_throws_whenPasswordTooShort() {
        assertThatThrownBy(() -> authService.register("alice", "123", Role.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password");
    }

    // --- login() ---

    @Test
    void login_success_returnsToken() throws Exception {
        String rawPassword = "secret99";
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        User user = new User(7L, "bob", hash, Role.USER);

        when(userDao.findByLogin("bob")).thenReturn(Optional.of(user));

        String token = authService.login("bob", rawPassword);

        assertThat(token).isNotBlank();
    }

    @Test
    void login_throws_whenUserNotFound() {
        when(userDao.findByLogin("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost", "pass"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throws_whenWrongPassword() {
        String hash = BCrypt.hashpw("correct", BCrypt.gensalt());
        User user = new User(2L, "carol", hash, Role.USER);
        when(userDao.findByLogin("carol")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("carol", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // --- parseToken() ---

    @Test
    void parseToken_roundTrip() throws Exception {
        String rawPassword = "pass1234";
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        User user = new User(42L, "dave", hash, Role.ADMIN);

        when(userDao.findByLogin("dave")).thenReturn(Optional.of(user));

        String token = authService.login("dave", rawPassword);
        AuthService.TokenClaims claims = authService.parseToken(token);

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.login()).isEqualTo("dave");
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
    }

    /**
     * Subclass that injects a fixed JWT secret so tests don't rely on
     * application.properties being on the classpath.
     */
    static class AuthServiceTestable extends AuthService {
        AuthServiceTestable(UserDao dao) {
            super(dao);
        }
    }

}
