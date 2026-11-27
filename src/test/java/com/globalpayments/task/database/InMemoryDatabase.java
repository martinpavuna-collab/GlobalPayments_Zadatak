package com.globalpayments.task.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/**
 * A private SQLite database that lives only in memory (no file is created anywhere).
 * {@link DatabaseManager} opens a new connection per operation, and a plain ":memory:" database
 * disappears with its connection, so this keeps one connection open for the lifetime of the test.
 */
public class InMemoryDatabase implements AutoCloseable {

    private final Connection keepAlive;
    private final DatabaseManager manager;

    public InMemoryDatabase() throws SQLException {
        String url = "jdbc:sqlite:file:mem-" + UUID.randomUUID() + "?mode=memory&cache=shared";
        keepAlive = DriverManager.getConnection(url);
        manager = new DatabaseManager(url);
    }

    public DatabaseManager manager() {
        return manager;
    }

    @Override
    public void close() throws SQLException {
        keepAlive.close();
    }
}
