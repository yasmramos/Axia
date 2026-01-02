package io.github.yasmramos.axia.exception;

/**
 * Base exception for the Axia Accounting System.
 * 
 * <p>All custom exceptions in the system should extend this class
 * to provide a consistent exception hierarchy and facilitate
 * centralized error handling.
 * 
 * <p>Exception hierarchy:
 * <ul>
 *   <li>{@link AxiaException} - Base exception</li>
 *   <li>{@link ValidationException} - Input validation errors</li>
 *   <li>{@link BusinessException} - Business rule violations</li>
 *   <li>{@link DatabaseException} - Database operation errors</li>
 *   <li>{@link ConfigurationException} - Configuration errors</li>
 * </ul>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class AxiaException extends RuntimeException {

    private final String errorCode;
    private final String category;

    /**
     * Constructs a new AxiaException with the specified detail message.
     * 
     * @param message the detail message
     */
    public AxiaException(String message) {
        super(message);
        this.errorCode = "AXIA_ERROR";
        this.category = "GENERAL";
    }

    /**
     * Constructs a new AxiaException with the specified detail message and error code.
     * 
     * @param message the detail message
     * @param errorCode the error code for categorization
     */
    public AxiaException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.category = "GENERAL";
    }

    /**
     * Constructs a new AxiaException with the specified detail message, error code, and category.
     * 
     * @param message the detail message
     * @param errorCode the error code for categorization
     * @param category the exception category
     */
    public AxiaException(String message, String errorCode, String category) {
        super(message);
        this.errorCode = errorCode;
        this.category = category;
    }

    /**
     * Constructs a new AxiaException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public AxiaException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "AXIA_ERROR";
        this.category = "GENERAL";
    }

    /**
     * Constructs a new AxiaException with the specified detail message, error code, and cause.
     * 
     * @param message the detail message
     * @param errorCode the error code for categorization
     * @param cause the cause
     */
    public AxiaException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.category = "GENERAL";
    }

    /**
     * Gets the error code.
     * 
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the exception category.
     * 
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns a compact representation of this exception for logging.
     * 
     * @return compact string representation
     */
    public String toCompactString() {
        return String.format("%s[%s]: %s", category, errorCode, getMessage());
    }

    @Override
    public String toString() {
        return String.format("%s[%s] - %s", category, errorCode, super.toString());
    }
}
