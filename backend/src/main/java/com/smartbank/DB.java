package com.smartbank;

import java.sql.*;

/** JDBC connection + schema setup (SQLite). */
public class DB {
    private static final String URL = "jdbc:sqlite:smartbank.db";

    public static Connection get() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement s = c.createStatement()) { s.execute("PRAGMA foreign_keys = ON"); }
        return c;
    }

    public static void init() throws SQLException {
        try (Connection c = get(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL REFERENCES users(id), acc_no TEXT UNIQUE NOT NULL, "
                    + "balance REAL NOT NULL DEFAULT 0 CHECK (balance >= 0))");
            s.execute("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "account_id INTEGER NOT NULL REFERENCES accounts(id), "
                    + "type TEXT NOT NULL CHECK (type IN ('DEPOSIT','WITHDRAW')), "
                    + "amount REAL NOT NULL CHECK (amount > 0), "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        }
    }
}
