package com.stockpilot.database;

/**
 * Central database configuration placeholder.
 * SQLite persistence will be wired in during a later development step.
 */
public final class DatabaseConfig {

    private static final String DATABASE_FILE = "stockpilot.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DATABASE_FILE;

    private DatabaseConfig() {
        // Prevent instantiation of utility configuration class.
    }

    public static String getDatabaseFile() {
        return DATABASE_FILE;
    }

    public static String getJdbcUrl() {
        return JDBC_URL;
    }
}

