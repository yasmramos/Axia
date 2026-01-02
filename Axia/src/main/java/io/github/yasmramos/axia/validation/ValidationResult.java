package io.github.yasmramos.axia.validation;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import java.util.Collections;
import java.util.List;

/**
 * Represents the result of a validation operation.
 *
 * @author Yasmany Ramos Garcia
 */
@Component
public class ValidationResult {

    private final boolean valid;
    private final List<String> errors;

    @Inject
    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : Collections.emptyList();
    }

    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult failure(String error) {
        return new ValidationResult(false, List.of(error));
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getErrorMessage() {
        return String.join("; ", errors);
    }
}
