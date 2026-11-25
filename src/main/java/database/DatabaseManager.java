package main.java.database;

import main.java.config.AppConfig;

import java.sql.*;
import java.time.LocalDateTime;

public class DatabaseManager implements IDatabaseManager {

    private final String databaseUrl = AppConfig.DATABASE_URL;

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    public void createTable() throws SQLException {

        String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "message TEXT NOT NULL, " +
                "received_at TEXT NOT NULL" +
                ")";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
        }
    }

    public void saveMessage(String message) throws SQLException {

        String sql = "INSERT INTO messages (message, received_at) " +
                "VALUES (?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, message);
            statement.setString(2, LocalDateTime.now().toString());

            statement.executeUpdate();
        }
    }
}