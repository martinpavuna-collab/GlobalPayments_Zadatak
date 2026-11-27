package com.globalpayments.task.database;

import com.globalpayments.task.config.AppConfig;
import com.globalpayments.task.model.Transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

public class DatabaseManager implements IDatabaseManager {

    private final String databaseUrl;

    public DatabaseManager() {
        this(AppConfig.DATABASE_URL);
    }

    public DatabaseManager(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    /**
     * One row per received transaction, each field in its own column.
     * amount is TEXT holding the exact decimal (e.g. '42.00'): SQLite has no fixed-point type
     * and REAL/NUMERIC would round-trip through binary floating point.
     */
    public void createTable() throws SQLException {

        String sql = "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "transaction_type TEXT NOT NULL, " +
                "transaction_id TEXT NOT NULL, " +
                "amount TEXT NOT NULL, " +
                "currency TEXT NOT NULL CHECK (length(currency) = 3), " +
                "received_at TEXT NOT NULL" +
                ")";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
        }
    }

    public void saveTransaction(Transaction transaction) throws SQLException {

        String sql = "INSERT INTO transactions " +
                "(transaction_type, transaction_id, amount, currency, received_at) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, transaction.type());
            statement.setString(2, transaction.id());
            statement.setString(3, transaction.amount().toPlainString());
            statement.setString(4, transaction.currency());
            statement.setString(5, Instant.now().toString()); // UTC

            statement.executeUpdate();
        }
    }
}
