package com.globalpayments.task.database;

import com.globalpayments.task.model.Transaction;

import java.sql.Connection;
import java.sql.SQLException;

public interface IDatabaseManager {

    Connection getConnection() throws SQLException;

    void createTable() throws SQLException;

    void saveTransaction(Transaction transaction) throws SQLException;
}
