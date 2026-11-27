package com.globalpayments.task;

import com.globalpayments.task.common.protocol.FrameReader;
import com.globalpayments.task.common.protocol.IReader;
import com.globalpayments.task.config.AppConfig;
import com.globalpayments.task.database.DatabaseManager;
import com.globalpayments.task.database.IDatabaseManager;
import com.globalpayments.task.server.MessageServer;

import java.sql.SQLException;

/** Server. Usage: ServerMain [port] (default port: see AppConfig). */
public class ServerMain {
    public static void main(String[] args) {

        int port;

        try {
            port = AppConfig.getPort(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
            return;
        }

        String outputFile = AppConfig.OUTPUT_FILE;
        String dbFile =  AppConfig.DATABASE_FILE;

        IReader reader = new FrameReader();
        IDatabaseManager databaseManager = new DatabaseManager(AppConfig.toJdbcUrl(dbFile));

        try {
            databaseManager.createTable();
        } catch (SQLException e) {
            System.err.println("Cannot prepare database '" + dbFile + "': " + e.getMessage());
            System.exit(2);
            return;
        }

        MessageServer server = new MessageServer(port, outputFile, reader, databaseManager);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Server failed: " + e.getMessage());
            System.exit(2);
        }
    }
}
