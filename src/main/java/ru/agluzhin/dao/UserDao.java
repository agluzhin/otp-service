package ru.agluzhin.dao;

import ru.agluzhin.config.DatabaseConfig;
import ru.agluzhin.model.Role;
import ru.agluzhin.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    private final DataSource dataSource;

    public UserDao() {
        this.dataSource = DatabaseConfig.getInstance().getDataSource();
    }

    // Constructor for testing (allows mock DataSource injection)
    public UserDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(User user) {
        String sql = "INSERT INTO users (login, password, role) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getLogin());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            log.debug("Saved user login={} role={}", user.getLogin(), user.getRole());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + user.getLogin(), e);
        }
    }

    public Optional<User> findByLogin(String login) {
        String sql = "SELECT id, login, password, role FROM users WHERE login = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by login: " + login, e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, login, password, role FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by id: " + id, e);
        }
        return Optional.empty();
    }

    public boolean existsByRole(Role role) {
        String sql = "SELECT 1 FROM users WHERE role = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence by role: " + role, e);
        }
    }

    public List<User> findAllByRole(Role role) {
        String sql = "SELECT id, login, password, role FROM users WHERE role = ? ORDER BY id";
        List<User> users = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list users by role: " + role, e);
        }
        return users;
    }

    public void deleteById(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            int affected = ps.executeUpdate();
            log.debug("Deleted {} user(s) with id={}", affected, id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user id=" + id, e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setLogin(rs.getString("login"));
        user.setPasswordHash(rs.getString("password"));
        user.setRole(Role.valueOf(rs.getString("role")));
        return user;
    }

}
