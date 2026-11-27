package com.globalpayments.task.database;

import com.globalpayments.task.model.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the real SQL, against a throw-away SQLite database that exists only in memory. */
class DatabaseManagerTest {

    private InMemoryDatabase database;
    private DatabaseManager db;

    @BeforeEach
    void setUp() throws SQLException {
        database = new InMemoryDatabase();
        db = database.manager();
    }

    @AfterEach
    void tearDown() throws SQLException {
        database.close();
    }

    @Test
    void storesEachFieldInItsOwnColumn() throws SQLException {
        db.createTable();

        db.saveTransaction(new Transaction("PAYMENT", "12345", new BigDecimal("100.50"), "HRK"));
        db.saveTransaction(new Transaction("STORNO", "žćčšđ", new BigDecimal("42.00"), "EUR"));

        try (Connection c = db.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM transactions ORDER BY id")) {
            assertTrue(rs.next());
            assertEquals("PAYMENT", rs.getString("transaction_type"));
            assertEquals("12345", rs.getString("transaction_id"));
            assertEquals("100.50", rs.getString("amount"));
            assertEquals("HRK", rs.getString("currency"));
            assertNotNull(rs.getString("received_at"));

            assertTrue(rs.next());
            assertEquals("žćčšđ", rs.getString("transaction_id"));
            assertEquals("42.00", rs.getString("amount"));
            assertFalse(rs.next());
        }
    }

    @Test
    void createTableCanBeCalledRepeatedlyAndKeepsData() throws SQLException {
        db.createTable();
        db.saveTransaction(new Transaction("A", "1", new BigDecimal("1.00"), "EUR"));

        db.createTable();

        try (Connection c = db.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM transactions")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void databaseRejectsCurrencyOfWrongLength() throws SQLException {
        db.createTable();

        assertThrows(SQLException.class,
                () -> db.saveTransaction(new Transaction("A", "1", new BigDecimal("1.00"), "EU")));
    }
}
