package org.caselli.cognitiveworkflow.operational.observability;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.operational.AI.services.IntentDetectionService;
import java.util.List;

/**
 * Intent detection observability trace
 * @author niccolocaselli
 */
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class IntentDetectionObservabilityReport extends ObservabilityReport {

    IntentDetectionService.IntentDetectionResponse.IntentDetectorResult intentDetectorResult;

    String inputRequest;

    List<IntentMetamodel> similarIntents;

    TokenUsage tokenUsage;

    public IntentDetectionObservabilityReport(String inputRequest) {
        this.inputRequest = inputRequest;
    }
}
