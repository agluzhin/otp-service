package ru.agluzhin.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.agluzhin.model.OtpConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpConfigDaoTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;

    private OtpConfigDao dao;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        dao = new OtpConfigDao(dataSource);
    }

    @Test
    void get_returnsConfig() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("code_length")).thenReturn(8);
        when(resultSet.getInt("ttl_seconds")).thenReturn(600);

        OtpConfig config = dao.get();

        assertThat(config.getCodeLength()).isEqualTo(8);
        assertThat(config.getTtlSeconds()).isEqualTo(600);
    }

    @Test
    void get_throws_whenNoRow() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThatThrownBy(() -> dao.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("seed row");
    }

    @Test
    void update_executesUpdate() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        dao.update(new OtpConfig(6, 300));

        verify(preparedStatement).setInt(1, 6);
        verify(preparedStatement).setInt(2, 300);
        verify(preparedStatement).executeUpdate();
    }

}
