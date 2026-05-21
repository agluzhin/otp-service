package ru.agluzhin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.agluzhin.dao.OtpCodeDao;
import ru.agluzhin.dao.OtpConfigDao;
import ru.agluzhin.dao.UserDao;
import ru.agluzhin.exception.UserNotFoundException;
import ru.agluzhin.model.OtpConfig;
import ru.agluzhin.model.Role;
import ru.agluzhin.model.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserDao userDao;
    @Mock
    private OtpCodeDao otpCodeDao;
    @Mock
    private OtpConfigDao otpConfigDao;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userDao, otpCodeDao, otpConfigDao);
    }

    // --- updateOtpConfig() ---

    @Test
    void updateOtpConfig_success() {
        adminService.updateOtpConfig(6, 300);
        verify(otpConfigDao).update(new OtpConfig(6, 300));
    }

    @Test
    void updateOtpConfig_throws_whenCodeLengthTooSmall() {
        assertThatThrownBy(() -> adminService.updateOtpConfig(3, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codeLength");

        verify(otpConfigDao, never()).update(any());
    }

    @Test
    void updateOtpConfig_throws_whenCodeLengthTooLarge() {
        assertThatThrownBy(() -> adminService.updateOtpConfig(11, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codeLength");
    }

    @Test
    void updateOtpConfig_throws_whenTtlTooSmall() {
        assertThatThrownBy(() -> adminService.updateOtpConfig(6, 59))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttlSeconds");
    }

    @Test
    void updateOtpConfig_throws_whenTtlTooLarge() {
        assertThatThrownBy(() -> adminService.updateOtpConfig(6, 3601))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttlSeconds");
    }

    // --- listUsers() ---

    @Test
    void listUsers_returnsOnlyRoleUser() {
        List<User> users = List.of(
                new User(1L, "alice", "hash", Role.USER),
                new User(2L, "bob", "hash", Role.USER)
        );
        when(userDao.findAllByRole(Role.USER)).thenReturn(users);

        List<User> result = adminService.listUsers();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(u -> u.getRole() == Role.USER);
    }

    // --- deleteUser() ---

    @Test
    void deleteUser_success() throws Exception {
        User user = new User(5L, "carol", "hash", Role.USER);
        when(userDao.findById(5L)).thenReturn(Optional.of(user));

        adminService.deleteUser(5L);

        verify(otpCodeDao).deleteByUserId(5L);
        verify(userDao).deleteById(5L);
    }

    @Test
    void deleteUser_throws_whenUserNotFound() {
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class);

        verify(otpCodeDao, never()).deleteByUserId(anyLong());
        verify(userDao, never()).deleteById(anyLong());
    }

    @Test
    void deleteUser_throws_whenTargetIsAdmin() {
        User admin = new User(1L, "admin", "hash", Role.ADMIN);
        when(userDao.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.deleteUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("admin");

        verify(userDao, never()).deleteById(anyLong());
    }

}
