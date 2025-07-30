package org.caselli.cognitiveworkflow.knowledge.validation;


import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class for storing validation results
 * @author niccolocaselli
 */
public class ValidationResult {
    private final List<ValidationError> errors = new ArrayList<>();
    private final List<ValidationWarning> warnings = new ArrayList<>();

    public void addError(String message, String component) {
        errors.add(new ValidationError(message, component));
    }

    public void addWarning(String message, String component) {
        warnings.add(new ValidationWarning(message, component));
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<ValidationWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public int getWarningCount() {
        return warnings.size();
    }

    public int getErrorCount() {
        return errors.size();
    }

    public void printErrors(Logger logger) {
        if (!getErrors().isEmpty()) {
            logger.error("Found {} validation errors:", errors.size());
            for (int i = 0; i < errors.size(); i++) {
                ValidationError error = errors.get(i);
                logger.error("[Error {}/{}] Component: {} - Message: {}", i + 1, errors.size(), error.component, error.message);
            }
        }
    }

    public void printWarnings(Logger logger) {
        if (!getWarnings().isEmpty()) {
            logger.warn("Found {} validation warnings:", warnings.size());
            for (int i = 0; i < warnings.size(); i++) {
                ValidationWarning warning = warnings.get(i);
                logger.warn("[Warning {}/{}] Component: {} - Message: {}", i + 1, warnings.size(), warning.component, warning.message);
            }
        }
    }

    public record ValidationError(String message, String component) {}


    public record ValidationWarning(String message, String component) {}
}


