package org.caselli.cognitiveworkflow.operational.observability;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Observability class for Workflow Routing
 * @author niccolocaselli
 */
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class RoutingObservabilityReport extends ObservabilityReport {

    String intentId;
    String selectedWorkflowId;

    public RoutingObservabilityReport(String intentId){
        this.intentId = intentId;
    }
}
