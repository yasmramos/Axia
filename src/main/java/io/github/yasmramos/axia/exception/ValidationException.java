package io.github.yasmramos.axia.exception;

import io.github.yasmramos.axia.validation.ValidationResult;

import java.util.List;

/**
 * Exception thrown when input validation fails.
 * 
 * <p>This exception encapsulates multiple validation errors and provides
 * methods to access them individually or as a combined message.
 * 
 * <p>Example usage:
 * <pre>
 * ValidationResult result = validator.validateAccountCode(code);
 * if (!result.isValid()) {
 *     throw new ValidationException(result.getErrors());
 * }
 * </pre>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class ValidationException extends AxiaException {

    private final List<String> validationErrors;

    /**
     * Constructs a new ValidationException with a single error message.
     * 
     * @param message the error message
     */
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", "VALIDATION");
        this.validationErrors = List.of(message);
    }

    /**
     * Constructs a new ValidationException with multiple error messages.
     * 
     * @param errors the list of validation errors
     */
    public ValidationException(List<String> errors) {
        super("Validation failed: " + String.join("; ", errors), "VALIDATION_ERROR", "VALIDATION");
        this.validationErrors = errors;
    }

    /**
     * Constructs a new ValidationException from a ValidationResult.
     * 
     * @param result the validation result
     */
    public ValidationException(ValidationResult result) {
        super("Validation failed: " + result.getErrorMessage(), "VALIDATION_ERROR", "VALIDATION");
        this.validationErrors = result.getErrors();
    }

    /**
     * Constructs a new ValidationException with a message and list of errors.
     * 
     * @param message the error message
     * @param errors the list of validation errors
     */
    public ValidationException(String message, List<String> errors) {
        super(message, "VALIDATION_ERROR", "VALIDATION");
        this.validationErrors = errors;
    }

    /**
     * Gets the list of validation errors.
     * 
     * @return the list of validation errors
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Gets the first validation error.
     * 
     * @return the first error message or null if no errors
     */
    public String getFirstError() {
        return validationErrors.isEmpty() ? null : validationErrors.get(0);
    }

    /**
     * Checks if there are multiple validation errors.
     * 
     * @return true if there are multiple errors
     */
    public boolean hasMultipleErrors() {
        return validationErrors.size() > 1;
    }

    /**
     * Returns the number of validation errors.
     * 
     * @return the error count
     */
    public int getErrorCount() {
        return validationErrors.size();
    }
}
