package ru.agluzhin.service;

import ru.agluzhin.dao.OtpCodeDao;
import ru.agluzhin.dao.OtpConfigDao;
import ru.agluzhin.dao.UserDao;
import ru.agluzhin.exception.UserNotFoundException;
import ru.agluzhin.model.OtpConfig;
import ru.agluzhin.model.Role;
import ru.agluzhin.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserDao      userDao;
    private final OtpCodeDao   otpCodeDao;
    private final OtpConfigDao otpConfigDao;

    public AdminService() {
        this(new UserDao(), new OtpCodeDao(), new OtpConfigDao());
    }

    public AdminService(UserDao userDao, OtpCodeDao otpCodeDao, OtpConfigDao otpConfigDao) {
        this.userDao      = userDao;
        this.otpCodeDao   = otpCodeDao;
        this.otpConfigDao = otpConfigDao;
    }

    /**
     * Updates OTP generation configuration.
     */
    public void updateOtpConfig(int codeLength, int ttlSeconds) {
        if (codeLength < 4 || codeLength > 10) {
            throw new IllegalArgumentException("codeLength must be between 4 and 10");
        }
        if (ttlSeconds < 60 || ttlSeconds > 3600) {
            throw new IllegalArgumentException("ttlSeconds must be between 60 and 3600");
        }
        otpConfigDao.update(new OtpConfig(codeLength, ttlSeconds));
        log.info("OTP config updated by admin: codeLength={} ttlSeconds={}", codeLength, ttlSeconds);
    }

    /**
     * Returns all non-admin users.
     */
    public List<User> listUsers() {
        return userDao.findAllByRole(Role.USER);
    }

    /**
     * Deletes a user and all their OTP codes (cascade also handled at DB level).
     */
    public void deleteUser(long userId) throws UserNotFoundException {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot delete admin account");
        }

        // OTP codes are deleted via ON DELETE CASCADE, but explicit call keeps logic visible
        otpCodeDao.deleteByUserId(userId);
        userDao.deleteById(userId);
        log.info("User deleted by admin: userId={} login={}", userId, user.getLogin());
    }

}
