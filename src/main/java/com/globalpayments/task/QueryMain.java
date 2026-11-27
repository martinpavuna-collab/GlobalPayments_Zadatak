package com.globalpayments.task;

import com.globalpayments.task.config.AppConfig;
import com.globalpayments.task.database.DatabaseManager;
import com.globalpayments.task.database.IDatabaseManager;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Prints what is stored in the {@code transactions} table.
 * Usage: QueryMain [type] [currency]   (both are optional filters; reads the database of the server)
 */
public class QueryMain {

    public static void main(String[] args) {
        String dbFile = AppConfig.DATABASE_FILE;
        String type = AppConfig.getArg(args, 0, null);
        String currency = AppConfig.getArg(args, 1, null);

        // Opening a SQLite URL would silently create an empty file, so check first.
        if (!Files.exists(Paths.get(dbFile))) {
            System.err.println("Database file '" + dbFile + "' not found. Start the server and send some data first.");
            System.exit(1);
            return;
        }

        // UTF-8 so Croatian letters print correctly whatever the console default is.
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        IDatabaseManager databaseManager = new DatabaseManager(AppConfig.toJdbcUrl(dbFile));

        try {
            run(databaseManager, type, currency, out);
        } catch (SQLException e) {
            System.err.println("Query failed: " + e.getMessage());
            System.exit(2);
        }
    }

    /** Runs the three selects and prints the results. */
    static void run(IDatabaseManager databaseManager, String type, String currency, PrintStream out)
            throws SQLException {

        try (Connection connection = databaseManager.getConnection()) {
            selectTransactions(connection, type, currency, out);
            selectCountPerCurrency(connection, out);
            selectTotalCount(connection, out);
        }
    }

    // SELECT 1: rows (optionally filtered)
    private static void selectTransactions(Connection connection, String type, String currency, PrintStream out)
            throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT id, transaction_type, transaction_id, amount, currency, received_at "
                        + "FROM transactions WHERE 1 = 1");
        if (type != null) {
            sql.append(" AND transaction_type = ?");
        }
        if (currency != null) {
            sql.append(" AND currency = ?");
        }
        sql.append(" ORDER BY id");

        out.println("== Transactions" + (type != null ? " type=" + type : "")
                + (currency != null ? " currency=" + currency : "") + " ==");
        out.printf("%-4s %-12s %-14s %-10s %-4s %s%n", "id", "type", "transaction_id", "amount", "cur", "received_at");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int i = 1;
            if (type != null) {
                statement.setString(i++, type);
            }
            if (currency != null) {
                statement.setString(i, currency);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    out.printf("%-4d %-12s %-14s %-10s %-4s %s%n",
                            rs.getInt("id"), rs.getString("transaction_type"), rs.getString("transaction_id"),
                            rs.getString("amount"), rs.getString("currency"), rs.getString("received_at"));
                }
            }
        }
    }

    // SELECT 2: number of transactions per currency
    private static void selectCountPerCurrency(Connection connection, PrintStream out) throws SQLException {
        out.println();
        out.println("== Count per currency ==");

        String sql = "SELECT currency, COUNT(*) AS cnt FROM transactions GROUP BY currency ORDER BY currency";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                out.printf("%-4s %d%n", rs.getString("currency"), rs.getInt("cnt"));
            }
        }
    }

    // SELECT 3: total number of rows
    private static void selectTotalCount(Connection connection, PrintStream out) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM transactions");
             ResultSet rs = statement.executeQuery()) {
            out.println();
            out.println("Total: " + (rs.next() ? rs.getInt(1) : 0));
        }
    }
}
