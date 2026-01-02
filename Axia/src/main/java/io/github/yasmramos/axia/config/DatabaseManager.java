package io.github.yasmramos.axia.config;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database configuration and connection management.
 * 
 * <p>Provides a singleton database instance using Ebean ORM.
 * Configuration is loaded from application.properties.
 * 
 * <p>Usage:
 * <pre>
 * Database db = DatabaseManager.getDatabase();
 * // ... perform operations
 * DatabaseManager.shutdown(); // when done
 * </pre>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    /** Singleton database instance */
    private static Database database;

    /** Private constructor to prevent instantiation */
    private DatabaseManager() {}

    /**
     * Gets the singleton database instance.
     * 
     * <p>Creates the database connection on first call using
     * settings from application.properties.
     * 
     * @return the Ebean Database instance
     */
    public static synchronized Database getDatabase() {
        if (database == null) {
            io.ebean.config.DatabaseConfig config = new io.ebean.config.DatabaseConfig();
            config.setName("db");
            config.loadFromProperties();
            config.setDefaultServer(true);
            config.setRegister(true);

            database = DatabaseFactory.create(config);
            log.info("Database connection established");
        }
        return database;
    }

    /**
     * Shuts down the database connection.
     * 
     * <p>Should be called when the application terminates
     * to release database resources.
     */
    public static void shutdown() {
        if (database != null) {
            database.shutdown();
            database = null;
            log.info("Database connection closed");
        }
    }
}
