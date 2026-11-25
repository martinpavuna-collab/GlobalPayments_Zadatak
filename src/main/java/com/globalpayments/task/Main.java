package com.globalpayments.task;

import com.globalpayments.task.common.protocol.FrameReader;
import com.globalpayments.task.common.protocol.IReader;
import com.globalpayments.task.config.AppConfig;
import com.globalpayments.task.database.DatabaseManager;
import com.globalpayments.task.database.IDatabaseManager;
import com.globalpayments.task.server.MessageServer;

import java.sql.SQLException;

import static com.globalpayments.task.config.AppConfig.SERVER_PORT;


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
