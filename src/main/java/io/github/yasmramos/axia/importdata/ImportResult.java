package io.github.yasmramos.axia.importdata;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import java.util.Collections;
import java.util.List;

/**
 * Result of an import operation.
 *
 * @author Yasmany Ramos Garcia
 */
@Component
public class ImportResult {

    private final int successCount;
    private final List<String> errors;

    @Inject
    public ImportResult(int successCount, List<String> errors) {
        this.successCount = successCount;
        this.errors = errors != null ? errors : Collections.emptyList();
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getErrorCount() {
        return errors.size();
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean isFullySuccessful() {
        return errors.isEmpty() && successCount > 0;
    }
}
