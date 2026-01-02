package io.github.yasmramos.axia.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized exception handler for the Axia Accounting System.
 * 
 * <p>Provides consistent exception handling across the application,
 * including logging, user notification, and recovery strategies.
 * 
 * <p>Usage:
 * <pre>
 * try {
 *     // operation that may throw exception
 * } catch (Exception e) {
 *     ExceptionHandler.handle(e);
 * }
 * </pre>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public final class ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    // Private constructor to prevent instantiation
    private ExceptionHandler() {}

    /**
     * Handles an exception with the default strategy.
     * 
     * <p>Logs the exception and determines if it should be
     * re-thrown or converted to a user-friendly message.
     * 
     * @param exception the exception to handle
     * @return the handled exception (potentially wrapped)
     */
    public static RuntimeException handle(Exception exception) {
        return handle(exception, HandlingStrategy.LOG_AND_THROW);
    }

    /**
     * Handles an exception with a specific strategy.
     * 
     * @param exception the exception to handle
     * @param strategy the handling strategy
     * @return the handled exception according to strategy
     */
    public static RuntimeException handle(Exception exception, HandlingStrategy strategy) {
        if (exception == null) {
            throw new IllegalArgumentException("Exception cannot be null");
        }

        // If already an Axia exception, just log and handle
        if (exception instanceof AxiaException) {
            return handleAxiaException((AxiaException) exception, strategy);
        }

        // Wrap other exceptions
        AxiaException wrapped = wrapException(exception);
        return handleAxiaException(wrapped, strategy);
    }

    /**
     * Handles an AxiaException with the specified strategy.
     * 
     * @param exception the AxiaException to handle
     * @param strategy the handling strategy
     * @return the handled exception
     */
    private static RuntimeException handleAxiaException(AxiaException exception, 
                                                          HandlingStrategy strategy) {
        switch (strategy) {
            case LOG_ONLY:
                logException(exception);
                return exception;

            case LOG_AND_THROW:
                logException(exception);
                return exception;

            case USER_FRIENDLY:
                logException(exception);
                return new UserFriendlyException(exception);

            case SILENT:
                return exception;

            case WRAP_AND_THROW:
                logException(exception);
                return wrapWithContext(exception);

            default:
                logException(exception);
                return exception;
        }
    }

    /**
     * Logs an exception with appropriate level based on type.
     * 
     * @param exception the exception to log
     */
    private static void logException(AxiaException exception) {
        switch (exception.getCategory()) {
            case "DATABASE":
                if (exception instanceof DatabaseException) {
                    DatabaseException dbEx = (DatabaseException) exception;
                    if (dbEx.isConnectionError()) {
                        log.error("Database connection error: {}", exception.toCompactString());
                    } else {
                        log.warn("Database error: {} - SQL State: {}", 
                            exception.toCompactString(), dbEx.getSqlState());
                    }
                } else {
                    log.warn("Database exception: {}", exception.toCompactString());
                }
                break;

            case "VALIDATION":
                log.debug("Validation error: {}", exception.toCompactString());
                break;

            case "BUSINESS":
                log.info("Business rule violation: {}", exception.toCompactString());
                break;

            case "CONFIGURATION":
                log.error("Configuration error: {}", exception.toCompactString());
                break;

            default:
                log.error("Application error: {}", exception.toCompactString(), exception);
        }
    }

    /**
     * Wraps a generic exception in an AxiaException.
     * 
     * @param exception the exception to wrap
     * @return the wrapped exception
     */
    private static AxiaException wrapException(Exception exception) {
        if (exception instanceof RuntimeException) {
            return new AxiaException(
                exception.getMessage() != null ? exception.getMessage() : "Unknown error",
                "WRAPPED_ERROR",
                exception
            );
        }

        return new AxiaException(
            exception.getMessage() != null ? exception.getMessage() : "Unknown error",
            "SYSTEM_ERROR",
            exception
        );
    }

    /**
     * Wraps an exception with additional context.
     * 
     * @param exception the exception to wrap
     * @return the wrapped exception
     */
    private static AxiaException wrapWithContext(AxiaException exception) {
        return new AxiaException(
            exception.getMessage(),
            exception.getErrorCode(),
            exception
        );
    }

    /**
     * Handles an exception and returns a user-friendly message.
     * 
     * @param exception the exception
     * @return user-friendly message
     */
    public static String getUserFriendlyMessage(Exception exception) {
        if (exception instanceof AxiaException) {
            AxiaException axiaEx = (AxiaException) exception;
            return getUserFriendlyMessage(axiaEx);
        }
        return "An unexpected error occurred. Please contact support.";
    }

    /**
     * Gets a user-friendly message for an AxiaException.
     * 
     * @param exception the exception
     * @return user-friendly message
     */
    public static String getUserFriendlyMessage(AxiaException exception) {
        switch (exception.getCategory()) {
            case "VALIDATION":
                return "Please check your input: " + exception.getMessage();

            case "BUSINESS":
                return "Operation not allowed: " + exception.getMessage();

            case "DATABASE":
                DatabaseException dbEx = (DatabaseException) exception;
                if (dbEx.isConnectionError()) {
                    return "Unable to connect to the database. Please try again later.";
                }
                return "A database error occurred. Please contact support.";

            case "CONFIGURATION":
                return "Configuration error. Please check your settings.";

            default:
                return "An unexpected error occurred. Please contact support.";
        }
    }

    /**
     * Determines if an exception should cause application shutdown.
     * 
     * @param exception the exception
     * @return true if application should shutdown
     */
    public static boolean shouldShutdown(Exception exception) {
        if (exception instanceof ConfigurationException) {
            ConfigErrorType errorType = ((ConfigurationException) exception).getConfigErrorType();
            return errorType == ConfigErrorType.MISSING_PROPERTY ||
                   errorType == ConfigErrorType.FILE_NOT_FOUND;
        }

        if (exception instanceof DatabaseException) {
            return ((DatabaseException) exception).isConnectionError();
        }

        return false;
    }

    /**
     * Recovery suggestion for an exception.
     * 
     * @param exception the exception
     * @return recovery suggestion
     */
    public static String getRecoverySuggestion(Exception exception) {
        if (exception instanceof ValidationException) {
            return "Review the input data and correct any errors before retrying.";
        }

        if (exception instanceof BusinessException) {
            BusinessException bizEx = (BusinessException) exception;
            if (BusinessException.ENTRY_POSTED.equals(bizEx.getErrorCode())) {
                return "Posted entries cannot be modified. Consider creating a reversal entry.";
            }
            return "Please review the business rules and try a different operation.";
        }

        if (exception instanceof DatabaseException) {
            DatabaseException dbEx = (DatabaseException) exception;
            if (dbEx.isConnectionError()) {
                return "Check database connection settings and ensure the database server is running.";
            }
            if (dbEx.isConstraintViolation()) {
                return "The operation conflicts with existing data. Please review and try again.";
            }
            return "Database operation failed. Please try again or contact support.";
        }

        if (exception instanceof ConfigurationException) {
            return "Check application configuration and ensure all required settings are provided.";
        }

        return "Please try again. If the problem persists, contact support.";
    }

    /**
     * Strategies for exception handling.
     */
    public enum HandlingStrategy {
        /**
         * Log the exception and re-throw it.
         */
        LOG_AND_THROW,

        /**
         * Log the exception but don't re-throw.
         */
        LOG_ONLY,

        /**
         * Convert to a user-friendly exception.
         */
        USER_FRIENDLY,

        /**
         * Don't log, just return the exception.
         */
        SILENT,

        /**
         * Wrap with additional context and throw.
         */
        WRAP_AND_THROW
    }

    /**
     * User-friendly exception for display to end users.
     */
    public static class UserFriendlyException extends RuntimeException {
        private final String userMessage;
        private final String errorCode;

        public UserFriendlyException(AxiaException original) {
            super(original.getMessage(), original);
            this.userMessage = getUserFriendlyMessage(original);
            this.errorCode = original.getErrorCode();
        }

        public String getUserMessage() {
            return userMessage;
        }

        public String getErrorCode() {
            return errorCode;
        }

        @Override
        public String getMessage() {
            return userMessage;
        }
    }
}
