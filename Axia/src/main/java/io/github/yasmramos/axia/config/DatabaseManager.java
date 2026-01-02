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
 * Configuration is loaded securely using ConfigManager which supports:
 * <ul>
 *   <li>Environment variables</li>
 *   <li>External configuration files</li>
 *   <li>Classpath properties</li>
 *   <li>Default values</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * DatabaseManager.initialize(); // Call once at application startup
 * Database db = DatabaseManager.getDatabase();
 * // ... perform operations
 * DatabaseManager.shutdown(); // Call when done
 * </pre>
 * 
 * <p>Configuration example using environment variables:
 * <pre>
 * export AXIA_DB_URL=jdbc:postgresql://localhost:5432/axia
 * export AXIA_DB_USER=myuser
 * export AXIA_DB_PASSWORD=mypassword
 * </pre>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    /** Singleton database instance */
    private static Database database;
    
    /** Configuration manager instance */
    private static ConfigManager configManager;

    /** Private constructor to prevent instantiation */
    private DatabaseManager() {}

    /**
     * Initializes the database connection with default configuration.
     * 
     * @return the Ebean Database instance
     */
    public static synchronized Database initialize() {
        if (database == null) {
            configManager = ConfigManager.getInstance();
            database = createDatabase();
            log.info("Database connection established. Source: {}", configManager.getConfigSource());
        }
        return database;
    }
    
    /**
     * Gets the configuration manager instance.
     * 
     * @return the configuration manager
     */
    public static synchronized ConfigManager getConfigManager() {
        if (configManager == null) {
            configManager = ConfigManager.getInstance();
        }
        return configManager;
    }

    /**
     * Gets the singleton database instance.
     * 
     * <p>Creates the database connection on first call using
     * settings from ConfigManager.
     * 
     * @return the Ebean Database instance
     */
    public static synchronized Database getDatabase() {
        if (database == null) {
            initialize();
        }
        return database;
    }

    /**
     * Creates the database instance with current configuration.
     * 
     * @return the configured Database instance
     */
    private static Database createDatabase() {
        ConfigManager config = getConfigManager();
        
        io.ebean.config.DatabaseConfig ebeanConfig = new io.ebean.config.DatabaseConfig();
        ebeanConfig.setName("db");
        
        // Load configuration from properties file
        ebeanConfig.loadFromProperties();
        
        // Override with secure configuration values
        ebeanConfig.setDefaultServer(true);
        ebeanConfig.setRegister(true);
        
        // Set database URL from ConfigManager
        String dbUrl = config.getDatabaseUrl();
        String dbUser = config.getDatabaseUser();
        char[] dbPassword = config.getDatabasePassword();
        
        // Configure datasource
        ebeanConfig.setDdlGenerate(config.isDdlGenerateEnabled());
        ebeanConfig.setDdlRun(config.isDdlRunEnabled());
        
        log.debug("Configuring database with URL: {}", maskUrl(dbUrl));
        
        Database db = DatabaseFactory.create(ebeanConfig);
        log.info("Database instance created successfully");
        return db;
    }

    /**
     * Shuts down the database connection.
     * 
     * <p>Should be called when the application terminates
     * to release database resources.
     */
    public static synchronized void shutdown() {
        if (database != null) {
            log.info("Shutting down database connection");
            database.shutdown();
            database = null;
            log.info("Database connection closed");
        }
    }
    
    /**
     * Checks if the database connection is active.
     * 
     * @return true if database is connected
     */
    public static boolean isConnected() {
        return database != null;
    }
    
    /**
     * Returns a safe string representation for logging.
     * Database URL is masked for security.
     * 
     * @return safe string representation
     */
    public static String toSafeString() {
        if (database == null) {
            return "DatabaseManager{status=not initialized}";
        }
        ConfigManager config = getConfigManager();
        return "DatabaseManager{status=connected, source=" + 
               config.getConfigSource() + ", url=" + 
               maskUrl(config.getDatabaseUrl()) + "}";
    }
    
    /**
     * Masks sensitive information in URL for logging.
     * 
     * @param url the URL to mask
     * @return masked URL
     */
    private static String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^:@]+@", ":***@");
    }
}
