package com.stockpilot.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central database configuration resolving the SQLite storage location.
 * Stores stockpilot.db in an application data directory rather than temporary build folders.
 */
public final class DatabaseConfig {

    private static final String DATABASE_NAME = "stockpilot.db";
    private static final Path DATABASE_PATH = resolveDatabasePath();
    private static final String JDBC_URL = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath().toString().replace('\\', '/');

    private DatabaseConfig() {
        // Utility class; do not instantiate.
    }

    private static Path resolveDatabasePath() {
        // Priority 1: explicitly configured JVM argument (e.g. -Dstockpilot.data.dir=...)
        String customDir = System.getProperty("stockpilot.data.dir");
        if (customDir != null && !customDir.isBlank()) {
            Path customPath = Paths.get(customDir, DATABASE_NAME);
            ensureParentDirectoryExists(customPath);
            return customPath;
        }

        // Priority 2: application data directory inside project root (data/stockpilot.db)
        Path projectDataDir = Paths.get("data", DATABASE_NAME);
        ensureParentDirectoryExists(projectDataDir);
        return projectDataDir;
    }

    private static void ensureParentDirectoryExists(Path path) {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                System.err.println("Warning: Unable to create database directory: " + parent + " (" + e.getMessage() + ")");
            }
        }
    }

    public static String getDatabaseName() {
        return DATABASE_NAME;
    }

    public static Path getDatabasePath() {
        return DATABASE_PATH;
    }

    public static String getJdbcUrl() {
        return JDBC_URL;
    }
}
