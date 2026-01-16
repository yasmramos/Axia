package io.github.yasmramos.axia.exception;

/**
 * Exception thrown when a database operation fails.
 * 
 * <p>This exception wraps database-related errors and provides
 * methods to determine the type of database error for appropriate handling.
 * 
 * <p>Common error types:
 * <ul>
 *   <li>CONNECTION - Unable to connect to database</li>
 *   <li>CONSTRAINT - Database constraint violation</li>
 *   <li>INTEGRITY - Referential integrity violation</li>
 *   <li>TRANSACTION - Transaction management error</li>
 *   <li>QUERY - Query execution error</li>
 *   <li>DDL - Schema modification error</li>
 * </ul>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class DatabaseException extends AxiaException {

    private final ErrorType errorType;
    private final String sqlState;

    /**
     * Enum for database error types.
     */
    public enum ErrorType {
        CONNECTION("Unable to establish database connection"),
        CONSTRAINT("Database constraint violation"),
        INTEGRITY("Referential integrity violation"),
        TRANSACTION("Transaction management error"),
        QUERY("Query execution error"),
        DDL("Schema modification error"),
        UNKNOWN("Unknown database error");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Constructs a new DatabaseException with the specified message and error type.
     * 
     * @param message the detail message
     * @param errorType the type of database error
     */
    public DatabaseException(String message, ErrorType errorType) {
        super(message, "DB_" + errorType.name(), "DATABASE");
        this.errorType = errorType;
        this.sqlState = null;
    }

    /**
     * Constructs a new DatabaseException with message, error type, and SQL state.
     * 
     * @param message the detail message
     * @param errorType the type of database error
     * @param sqlState the SQL state code
     */
    public DatabaseException(String message, ErrorType errorType, String sqlState) {
        super(message, "DB_" + errorType.name(), "DATABASE");
        this.errorType = errorType;
        this.sqlState = sqlState;
    }

    /**
     * Constructs a new DatabaseException with message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, "DB_ERROR", cause);
        this.errorType = ErrorType.UNKNOWN;
        this.sqlState = null;
    }

    /**
     * Constructs a new DatabaseException with message, error type, and cause.
     * 
     * @param message the detail message
     * @param errorType the type of database error
     * @param cause the cause
     */
    public DatabaseException(String message, ErrorType errorType, Throwable cause) {
        super(message, "DB_" + errorType.name(), cause);
        this.errorType = errorType;
        this.sqlState = null;
    }

    /**
     * Gets the database error type.
     * 
     * @return the error type
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Gets the SQL state code.
     * 
     * @return the SQL state code or null if not available
     */
    public String getSqlState() {
        return sqlState;
    }

    /**
     * Checks if this is a connection error.
     * 
     * @return true if connection error
     */
    public boolean isConnectionError() {
        return errorType == ErrorType.CONNECTION;
    }

    /**
     * Checks if this is a constraint violation.
     * 
     * @return true if constraint violation
     */
    public boolean isConstraintViolation() {
        return errorType == ErrorType.CONSTRAINT || errorType == ErrorType.INTEGRITY;
    }

    /**
     * Factory method for connection error.
     * 
     * @param message the detail message
     * @return the exception instance
     */
    public static DatabaseException connectionError(String message) {
        return new DatabaseException(message, ErrorType.CONNECTION);
    }

    /**
     * Factory method for connection error with cause.
     * 
     * @param message the detail message
     * @param cause the cause
     * @return the exception instance
     */
    public static DatabaseException connectionError(String message, Throwable cause) {
        return new DatabaseException(message, ErrorType.CONNECTION, cause);
    }

    /**
     * Factory method for constraint violation.
     * 
     * @param message the detail message
     * @param cause the cause
     * @return the exception instance
     */
    public static DatabaseException constraintViolation(String message, Throwable cause) {
        return new DatabaseException(message, ErrorType.CONSTRAINT, cause);
    }

    /**
     * Factory method for transaction error.
     * 
     * @param message the detail message
     * @param cause the cause
     * @return the exception instance
     */
    public static DatabaseException transactionError(String message, Throwable cause) {
        return new DatabaseException(message, ErrorType.TRANSACTION, cause);
    }

    /**
     * Factory method for query error.
     * 
     * @param message the detail message
     * @param cause the cause
     * @return the exception instance
     */
    public static DatabaseException queryError(String message, Throwable cause) {
        return new DatabaseException(message, ErrorType.QUERY, cause);
    }

    /**
     * Creates a DatabaseException from a generic exception, inferring the error type.
     * 
     * @param message the detail message
     * @param cause the cause
     * @return the exception instance
     */
    public static DatabaseException fromException(String message, Throwable cause) {
        String causeMessage = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
        ErrorType errorType = ErrorType.UNKNOWN;

        if (causeMessage.contains("connection") || causeMessage.contains("refused") || 
            causeMessage.contains("timeout") || causeMessage.contains("unreachable")) {
            errorType = ErrorType.CONNECTION;
        } else if (causeMessage.contains("constraint") || causeMessage.contains("unique") || 
                   causeMessage.contains("foreign key") || causeMessage.contains("integrity")) {
            errorType = ErrorType.INTEGRITY;
        } else if (causeMessage.contains("transaction") || causeMessage.contains("rollback") || 
                   causeMessage.contains("concurrent")) {
            errorType = ErrorType.TRANSACTION;
        } else if (causeMessage.contains("syntax") || causeMessage.contains("column") || 
                   causeMessage.contains("table") || causeMessage.contains("query")) {
            errorType = ErrorType.QUERY;
        }

        return new DatabaseException(message, errorType, cause);
    }
}
