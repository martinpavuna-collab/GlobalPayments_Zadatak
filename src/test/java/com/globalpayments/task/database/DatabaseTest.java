package com.globalpayments.task.database;

import com.globalpayments.task.database.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static java.sql.DriverManager.getConnection;

public class DatabaseTest {

    public static void main(String[] args) {

        DatabaseManager databaseManager = new DatabaseManager();

        try (Connection connection = databaseManager.getConnection()) {
            System.out.println("Connected to SQLite!");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            databaseManager.createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Table created!");

//        try {
//            databaseManager.saveMessage("UPLATA|ČĆŽŠĐ|100.50|HRK");
//            databaseManager.saveMessage("STORNO|žćčšđ|42.00|EUR");
//            databaseManager.saveMessage("KRAJ");
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }


        System.out.println("Messages saved!");


        System.out.println(
                new java.io.File("messages.db").getAbsolutePath());

        String sql = "SELECT id, message, received_at FROM messages";

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String message = resultSet.getString("message");
                String receivedAt = resultSet.getString("received_at");

                System.out.println(
                        id + " | " + message + " | " + receivedAt
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


}