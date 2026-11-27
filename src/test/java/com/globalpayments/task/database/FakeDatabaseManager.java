package com.globalpayments.task.database;

import com.globalpayments.task.model.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory stand-in for the database, for tests that only care about what the server hands to it.
 * Records saved transactions and can be told to fail, to test the server's error handling.
 */
public class FakeDatabaseManager implements IDatabaseManager {

    private final List<Transaction> saved = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean failOnSave;
    private volatile int tableCreations;

    public List<Transaction> saved() {
        return new ArrayList<>(saved);
    }

    /** Saved transactions as {@code type|id|amount|currency}, same format as the messages. */
    public List<String> savedAsLines() {
        List<String> lines = new ArrayList<>();
        for (Transaction t : saved()) {
            lines.add(t.toString());
        }
        return lines;
    }

    public void failOnSave(boolean fail) {
        this.failOnSave = fail;
    }

    public int tableCreations() {
        return tableCreations;
    }

    @Override
    public Connection getConnection() {
        throw new UnsupportedOperationException("fake database has no JDBC connection");
    }

    @Override
    public void createTable() {
        tableCreations++;
    }

    @Override
    public void saveTransaction(Transaction transaction) throws SQLException {
        if (failOnSave) {
            throw new SQLException("simulated database failure");
        }
        saved.add(transaction);
    }
}
