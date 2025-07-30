package org.caselli.cognitiveworkflow.operational.observability;

import lombok.Data;

/**
 * Helper class to return a result and the observability trace for a process
 * @param <T> The result class
 * @author niccolocaselli
 */
@Data
public class ResultWithObservability<T> {
    public ObservabilityReport observabilityReport;

    public T result;

    public ResultWithObservability(T result, ObservabilityReport observabilityReport) {
        this.observabilityReport = observabilityReport;
        this.result = result;
    }

    public ResultWithObservability(ObservabilityReport observabilityReport) {
        this.observabilityReport = observabilityReport;
        this.result = null;
    }
}
