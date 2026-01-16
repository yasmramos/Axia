package io.github.yasmramos.axia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Secure configuration manager that loads settings from multiple sources.
 * 
 * <p>Configuration priority (highest to lowest):
 * <ol>
 *   <li>Environment variables</li>
 *   <li>External config file (AXIA_CONFIG_PATH)</li>
 *   <li>Classpath application.properties</li>
 *   <li>Default values</li>
 * </ol>
 * 
 * <p>Usage:
 * <pre>
 * ConfigManager config = ConfigManager.getInstance();
 * String dbUrl = config.getDatabaseUrl();
 * String dbUser = config.getDatabaseUser();
 * char[] dbPassword = config.getDatabasePassword();
 * </pre>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    
    private final Properties properties;
    private final String configSource;
    
    // Configuration keys
    public static final String DB_URL = "datasource.db.url";
    public static final String DB_USERNAME = "datasource.db.username";
    public static final String DB_PASSWORD = "datasource.db.password";
    public static final String DB_DRIVER = "datasource.db.driver";
    public static final String DB_MIN_CONN = "datasource.db.minConnections";
    public static final String DB_MAX_CONN = "datasource.db.maxConnections";
    
    public static final String EBEAN_DDL_GENERATE = "ebean.db.ddl.generate";
    public static final String EBEAN_DDL_RUN = "ebean.db.ddl.run";
    public static final String EBEAN_DDL_CREATE_ONLY = "ebean.db.ddl.createOnly";
    public static final String EBEAN_QUERY_LOG_LEVEL = "ebean.db.queryLogLevel";
    
    // Environment variable names
    private static final String ENV_DB_URL = "AXIA_DB_URL";
    private static final String ENV_DB_USER = "AXIA_DB_USER";
    private static final String ENV_DB_PASSWORD = "AXIA_DB_PASSWORD";
    private static final String ENV_DB_HOST = "AXIA_DB_HOST";
    private static final String ENV_DB_PORT = "AXIA_DB_PORT";
    private static final String ENV_DB_NAME = "AXIA_DB_NAME";
    private static final String ENV_CONFIG_PATH = "AXIA_CONFIG_PATH";
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ConfigManager() {
        this.properties = new Properties();
        this.configSource = loadConfiguration();
    }
    
    /**
     * Gets the singleton instance of ConfigManager.
     * 
     * @return the configuration manager instance
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    /**
     * Loads configuration from multiple sources.
     * 
     * @return the source of configuration that was used
     */
    private String loadConfiguration() {
        // First, try to load from external config file
        String externalPath = System.getenv(ENV_CONFIG_PATH);
        if (externalPath != null && !externalPath.isBlank()) {
            if (loadFromExternalFile(externalPath)) {
                log.info("Configuration loaded from external file: {}", externalPath);
                return "external-file:" + externalPath;
            }
        }
        
        // Try axia.config.path system property
        String systemPropertyPath = System.getProperty("axia.config.path");
        if (systemPropertyPath != null && !systemPropertyPath.isBlank()) {
            if (loadFromExternalFile(systemPropertyPath)) {
                log.info("Configuration loaded from system property file: {}", systemPropertyPath);
                return "system-property:" + systemPropertyPath;
            }
        }
        
        // Load from classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                properties.load(is);
                log.info("Configuration loaded from classpath application.properties");
                return "classpath:application.properties";
            }
        } catch (IOException e) {
            log.warn("Could not load application.properties from classpath: {}", e.getMessage());
        }
        
        // Use defaults
        log.warn("No configuration found, using default values");
        loadDefaults();
        return "defaults";
    }
    
    /**
     * Attempts to load configuration from an external file.
     * 
     * @param path the path to the configuration file
     * @return true if the file was loaded successfully
     */
    private boolean loadFromExternalFile(String path) {
        Path configPath = Paths.get(path);
        if (!Files.exists(configPath)) {
            log.warn("External config file not found: {}", path);
            return false;
        }
        
        try (InputStream is = new FileInputStream(configPath.toFile())) {
            properties.load(is);
            return true;
        } catch (IOException e) {
            log.error("Error loading external config file: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Loads default configuration values.
     */
    private void loadDefaults() {
        properties.setProperty(DB_DRIVER, "org.postgresql.Driver");
        properties.setProperty(DB_URL, "jdbc:postgresql://localhost:5432/axia");
        properties.setProperty(DB_USERNAME, "postgres");
        properties.setProperty(DB_PASSWORD, "");
        properties.setProperty(DB_MIN_CONN, "1");
        properties.setProperty(DB_MAX_CONN, "10");
        properties.setProperty(EBEAN_DDL_GENERATE, "true");
        properties.setProperty(EBEAN_DDL_RUN, "true");
        properties.setProperty(EBEAN_DDL_CREATE_ONLY, "false");
        properties.setProperty(EBEAN_QUERY_LOG_LEVEL, "SQL");
    }
    
    /**
     * Gets the configuration source.
     * 
     * @return the source of the current configuration
     */
    public String getConfigSource() {
        return configSource;
    }
    
    /**
     * Gets a property value.
     * 
     * @param key the property key
     * @return the property value or null if not found
     */
    public String getProperty(String key) {
        // Check environment variable first
        String envKey = key.toUpperCase().replace(".", "_");
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        
        return properties.getProperty(key);
    }
    
    /**
     * Gets a property value with a default.
     * 
     * @param key the property key
     * @param defaultValue the default value if not found
     * @return the property value or the default
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Gets the database URL.
     * 
     * @return the database JDBC URL
     */
    public String getDatabaseUrl() {
        return getProperty(DB_URL);
    }
    
    /**
     * Gets the database username.
     * 
     * @return the database username
     */
    public String getDatabaseUser() {
        return getProperty(DB_USERNAME);
    }
    
    /**
     * Gets the database password as a character array for secure handling.
     * 
     * @return the database password as char array
     */
    public char[] getDatabasePassword() {
        String password = getProperty(DB_PASSWORD);
        return password != null ? password.toCharArray() : new char[0];
    }
    
    /**
     * Gets the database driver class name.
     * 
     * @return the driver class name
     */
    public String getDatabaseDriver() {
        return getProperty(DB_DRIVER, "org.postgresql.Driver");
    }
    
    /**
     * Gets the minimum number of database connections.
     * 
     * @return the minimum connections
     */
    public int getMinConnections() {
        String value = getProperty(DB_MIN_CONN);
        return value != null ? Integer.parseInt(value) : 1;
    }
    
    /**
     * Gets the maximum number of database connections.
     * 
     * @return the maximum connections
     */
    public int getMaxConnections() {
        String value = getProperty(DB_MAX_CONN);
        return value != null ? Integer.parseInt(value) : 10;
    }
    
    /**
     * Checks if Ebean DDL generation is enabled.
     * 
     * @return true if DDL generation is enabled
     */
    public boolean isDdlGenerateEnabled() {
        String value = getProperty(EBEAN_DDL_GENERATE);
        return value == null || Boolean.parseBoolean(value);
    }
    
    /**
     * Checks if Ebean DDL should be run.
     * 
     * @return true if DDL should be run
     */
    public boolean isDdlRunEnabled() {
        String value = getProperty(EBEAN_DDL_RUN);
        return value == null || Boolean.parseBoolean(value);
    }
    
    /**
     * Gets the Ebean query log level.
     * 
     * @return the query log level
     */
    public String getQueryLogLevel() {
        return getProperty(EBEAN_QUERY_LOG_LEVEL, "SQL");
    }
    
    /**
     * Gets the database host from the connection URL or environment.
     * 
     * @return the database host
     */
    public String getDatabaseHost() {
        String url = getDatabaseUrl();
        if (url != null && url.contains("://")) {
            String afterProtocol = url.substring(url.indexOf("://") + 3);
            String host = afterProtocol.split(":")[0].split("/")[0];
            return host;
        }
        return System.getenv(ENV_DB_HOST);
    }
    
    /**
     * Gets the database port from the connection URL or environment.
     * 
     * @return the database port
     */
    public int getDatabasePort() {
        String url = getDatabaseUrl();
        if (url != null && url.contains(":")) {
            String afterHost = url.substring(url.indexOf("://") + 3);
            if (afterHost.contains(":")) {
                String portStr = afterHost.split(":")[1].split("/")[0];
                try {
                    return Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    // Ignore, will return default
                }
            }
        }
        String envPort = System.getenv(ENV_DB_PORT);
        return envPort != null ? Integer.parseInt(envPort) : 5432;
    }
    
    /**
     * Gets the database name from the connection URL or environment.
     * 
     * @return the database name
     */
    public String getDatabaseName() {
        String url = getDatabaseUrl();
        if (url != null && url.contains("/")) {
            String afterHost = url.substring(url.indexOf("://") + 3);
            if (afterHost.contains("/")) {
                String dbName = afterHost.split("/")[1].split("\\?")[0];
                return dbName;
            }
        }
        return System.getenv(ENV_DB_NAME);
    }
    
    /**
     * Checks if database is configured with environment variables only.
     * Useful for containerized deployments.
     * 
     * @return true if using environment-only configuration
     */
    public boolean isUsingEnvironmentConfig() {
        return System.getenv(ENV_DB_HOST) != null || 
               System.getenv(ENV_DB_URL) != null;
    }
    
    /**
     * Clears all loaded configuration.
     * Useful for testing or reconfiguration.
     */
    public synchronized void clear() {
        properties.clear();
        log.info("Configuration cleared");
    }
    
    /**
     * Reloads configuration from sources.
     * 
     * @return the new configuration source
     */
    public synchronized String reload() {
        properties.clear();
        instance = null;
        instance = new ConfigManager();
        return instance.configSource;
    }
    
    /**
     * Returns a safe string representation for logging.
     * Passwords are masked.
     * 
     * @return safe configuration string
     */
    public String toSafeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConfigManager{source=").append(configSource);
        sb.append(", url=").append(maskUrl(getDatabaseUrl()));
        sb.append(", user=").append(getDatabaseUser());
        sb.append(", password=***MASKED***");
        sb.append(", minConn=").append(getMinConnections());
        sb.append(", maxConn=").append(getMaxConnections());
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Masks sensitive information in URL for logging.
     * 
     * @param url the URL to mask
     * @return masked URL
     */
    private String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^:@]+@", ":***@");
    }
}
