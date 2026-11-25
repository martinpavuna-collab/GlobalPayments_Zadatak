package com.globalpayments.task.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface IDatabaseManager {

    public Connection getConnection() throws SQLException;

    public void createTable() throws SQLException;

    public void saveMessage(String message) throws SQLException;
}
