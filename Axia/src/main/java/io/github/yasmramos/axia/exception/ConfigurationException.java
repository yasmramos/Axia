package io.github.yasmramos.axia.exception;

/**
 * Exception thrown when configuration is invalid or missing.
 * 
 * <p>This exception indicates problems with application configuration,
 * such as missing required properties, invalid values, or
 * configuration file errors.
 * 
 * <p>Common scenarios:
 * <ul>
 *   <li>Missing required configuration property</li>
 *   <li>Invalid property value format</li>
 *   <li>Configuration file not found</li>
 *   <li>Environment variable not set</li>
 *   <li>Configuration conflict</li>
 * </ul>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class ConfigurationException extends AxiaException {

    private final String propertyName;
    private final String propertyValue;

    /**
     * Enum for configuration error types.
     */
    public enum ConfigErrorType {
        MISSING_PROPERTY("Required configuration property is missing"),
        INVALID_VALUE("Configuration property has invalid value"),
        FILE_NOT_FOUND("Configuration file not found"),
        FILE_PARSE_ERROR("Error parsing configuration file"),
        ENVIRONMENT_MISSING("Required environment variable is not set"),
        TYPE_MISMATCH("Configuration value type mismatch"),
        DEPENDENCY_CONFLICT("Configuration dependencies not met"),
        UNKNOWN("Unknown configuration error");

        private final String description;

        ConfigErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final ConfigErrorType configErrorType;

    /**
     * Constructs a new ConfigurationException with the specified message.
     * 
     * @param message the detail message
     */
    public ConfigurationException(String message) {
        super(message, "CONFIG_ERROR", "CONFIGURATION");
        this.propertyName = null;
        this.propertyValue = null;
        this.configErrorType = ConfigErrorType.UNKNOWN;
    }

    /**
     * Constructs a new ConfigurationException with message and error type.
     * 
     * @param message the detail message
     * @param configErrorType the type of configuration error
     */
    public ConfigurationException(String message, ConfigErrorType configErrorType) {
        super(message, "CONFIG_" + configErrorType.name(), "CONFIGURATION");
        this.propertyName = null;
        this.propertyValue = null;
        this.configErrorType = configErrorType;
    }

    /**
     * Constructs a new ConfigurationException for a missing property.
     * 
     * @param propertyName the missing property name
     */
    public ConfigurationException forProperty(String propertyName) {
        return new ConfigurationException("Required configuration property not found: " + propertyName, 
              ConfigErrorType.MISSING_PROPERTY, propertyName, null);
    }

    /**
     * Constructs a new ConfigurationException for an invalid property value.
     * 
     * @param propertyName the property name
     * @param propertyValue the invalid value
     * @param reason the reason it's invalid
     */
    public ConfigurationException(String propertyName, String propertyValue, String reason) {
        super(String.format("Invalid configuration value for '%s': '%s'. %s", 
            propertyName, propertyValue, reason),
              "CONFIG_INVALID_VALUE", "CONFIGURATION");
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.configErrorType = ConfigErrorType.INVALID_VALUE;
    }

    /**
     * Constructs a new ConfigurationException with all details.
     * 
     * @param message the detail message
     * @param configErrorType the type of configuration error
     * @param propertyName the property name (optional)
     * @param propertyValue the property value (optional)
     */
    public ConfigurationException(String message, ConfigErrorType configErrorType, 
                                  String propertyName, String propertyValue) {
        super(message, "CONFIG_" + configErrorType.name(), "CONFIGURATION");
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.configErrorType = configErrorType;
    }

    /**
     * Constructs a new ConfigurationException with cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, "CONFIG_ERROR", cause);
        this.propertyName = null;
        this.propertyValue = null;
        this.configErrorType = ConfigErrorType.UNKNOWN;
    }

    /**
     * Gets the property name associated with this exception.
     * 
     * @return the property name or null
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Gets the property value associated with this exception.
     * 
     * @return the property value or null
     */
    public String getPropertyValue() {
        return propertyValue;
    }

    /**
     * Gets the configuration error type.
     * 
     * @return the error type
     */
    public ConfigErrorType getConfigErrorType() {
        return configErrorType;
    }

    /**
     * Checks if this exception is related to a specific property.
     * 
     * @return true if property-related
     */
    public boolean isPropertyRelated() {
        return propertyName != null;
    }

    /**
     * Factory method for missing property.
     * 
     * @param propertyName the property name
     * @return the exception instance
     */
    public static ConfigurationException missingProperty(String propertyName) {
        return new ConfigurationException("Required configuration property not found: " + propertyName, 
              ConfigErrorType.MISSING_PROPERTY, propertyName, null);
    }

    /**
     * Factory method for missing environment variable.
     * 
     * @param envVarName the environment variable name
     * @return the exception instance
     */
    public static ConfigurationException missingEnvironmentVariable(String envVarName) {
        ConfigurationException ex = new ConfigurationException(
            "Required environment variable not set: " + envVarName,
            ConfigErrorType.ENVIRONMENT_MISSING
        );
        return ex;
    }

    /**
     * Factory method for invalid property value.
     * 
     * @param propertyName the property name
     * @param propertyValue the invalid value
     * @param expectedFormat the expected format
     * @return the exception instance
     */
    public static ConfigurationException invalidPropertyValue(String propertyName, 
                                                               String propertyValue,
                                                               String expectedFormat) {
        return new ConfigurationException(
            propertyName, propertyValue, 
            "Expected format: " + expectedFormat
        );
    }

    /**
     * Factory method for configuration file not found.
     * 
     * @param filePath the file path
     * @return the exception instance
     */
    public static ConfigurationException fileNotFound(String filePath) {
        ConfigurationException ex = new ConfigurationException(
            "Configuration file not found: " + filePath,
            ConfigErrorType.FILE_NOT_FOUND
        );
        return ex;
    }

    /**
     * Factory method for configuration file parse error.
     * 
     * @param filePath the file path
     * @param reason the parse error reason
     * @return the exception instance
     */
    public static ConfigurationException fileParseError(String filePath, String reason) {
        ConfigurationException ex = new ConfigurationException(
            "Error parsing configuration file " + filePath + ": " + reason,
            ConfigErrorType.FILE_PARSE_ERROR
        );
        return ex;
    }
}
