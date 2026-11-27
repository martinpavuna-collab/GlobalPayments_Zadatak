package com.globalpayments.task;

import com.globalpayments.task.database.DatabaseManager;
import com.globalpayments.task.model.Transaction;
import com.globalpayments.task.database.InMemoryDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class QueryMainTest {

    private InMemoryDatabase database;
    private DatabaseManager db;

    @BeforeEach
    void fillDatabase() throws SQLException {
        database = new InMemoryDatabase(); // in memory only, no file
        db = database.manager();
        db.createTable();
        db.saveTransaction(new Transaction("UPLATA", "ČĆŽŠĐ", new BigDecimal("100.50"), "HRK"));
        db.saveTransaction(new Transaction("STORNO", "žćčšđ", new BigDecimal("42.00"), "EUR"));
        db.saveTransaction(new Transaction("UPLATA", "777", new BigDecimal("5.00"), "EUR"));
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        database.close();
    }

    private String run(String type, String currency) throws SQLException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        QueryMain.run(db, type, currency, new PrintStream(bytes, true, StandardCharsets.UTF_8));
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void listsAllRowsCountsPerCurrencyAndTotal() throws SQLException {
        String out = run(null, null);

        assertTrue(out.contains("ČĆŽŠĐ"));
        assertTrue(out.contains("žćčšđ"));
        assertTrue(out.contains("EUR  2"));
        assertTrue(out.contains("HRK  1"));
        assertTrue(out.contains("Total: 3"));
    }

    @Test
    void filtersByTypeAndCurrency() throws SQLException {
        String out = run("UPLATA", "EUR");

        assertTrue(out.contains("777"));
        assertFalse(out.contains("ČĆŽŠĐ"));
        assertFalse(out.contains("žćčšđ"));
    }
}
