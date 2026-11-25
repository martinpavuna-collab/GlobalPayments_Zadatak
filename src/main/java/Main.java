package main.java;

import main.java.common.protocol.FrameReader;
import main.java.common.protocol.IReader;
import main.java.config.AppConfig;
import main.java.database.DatabaseManager;
import main.java.database.IDatabaseManager;
import main.java.server.MessageServer;

import java.sql.SQLException;

import static main.java.config.AppConfig.SERVER_PORT;


public class Main {
    public static void main(String[] args) {

        int port;

        try {
            port = AppConfig.getPort(args);
        } catch (IllegalArgumentException e) {
            System.out.println("Configuration error: " + e.getMessage());
            return;
        }

        IReader reader = new FrameReader();
        IDatabaseManager databaseManager = new DatabaseManager();

        try {
            databaseManager.createTable();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        MessageServer server = new MessageServer(port, reader,databaseManager);

        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Server failed: " + e.getMessage());
        }
    }
}
