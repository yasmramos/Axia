package io.github.yasmramos.axia.importdata;

import java.util.Collections;
import java.util.List;

/**
 * Result of an import operation.
 *
 * @author Yasmany Ramos Garcia
 */
public class ImportResult {

    private final int successCount;
    private final List<String> errors;

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
