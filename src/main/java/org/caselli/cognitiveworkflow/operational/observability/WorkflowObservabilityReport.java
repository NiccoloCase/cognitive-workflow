package org.caselli.cognitiveworkflow.operational.observability;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.utils.DurationToMillisSerializer;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Workflow Execution report for observability
 * @author niccolocaselli
 */
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class WorkflowObservabilityReport extends ObservabilityReport {

    private final String workflowId;
    private final String workflowName;

    private final Map<String, NodeExecutionDetail> nodeExecutions = new ConcurrentHashMap<>();

    private final List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

    private final List<EdgeEvaluationDetail> edgeEvaluations = Collections.synchronizedList(new ArrayList<>());

    private final List<PortAdaptationDetail> portAdaptations = Collections.synchronizedList(new ArrayList<>());

    @JsonIgnore
    private final Map<String, Map<String, Object>> contextSnapshots = new ConcurrentHashMap<>();

    private final WorkflowExecutionMetrics metrics = new WorkflowExecutionMetrics();

    private final ExecutionContext initialContext;

    private final List<TokenUsage> tokenUsages = Collections.synchronizedList(new ArrayList<>());


    public WorkflowObservabilityReport(String workflowId, String workflowName, ExecutionContext initialContext) {
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.initialContext = new ExecutionContext(initialContext); // deep copy
    }




    /**
     * Marks the workflow execution as completed
     */
    @Override
    public void markCompleted(boolean success, String errorMessage, Throwable exception) {
        super.markCompleted(success, errorMessage, exception);
        calculateMetrics();
    }

    /**
     * Records the start of a node execution
     */
    public void recordNodeStart(String nodeId, String nodeName, String nodeType, ExecutionContext inputContext) {
        NodeExecutionDetail detail = new NodeExecutionDetail(nodeId, nodeName, nodeType);
        detail.recordStart();
        nodeExecutions.put(nodeId, detail);
        executionOrder.add(nodeId);

        // Take context snapshot before node execution
        contextSnapshots.put("before_" + nodeId, createContextSnapshot(inputContext));
    }

    /**
     * Records the completion of a node execution
     */
    public void recordNodeCompletion(String nodeId, boolean success, String errorMessage, Throwable exception, ExecutionContext outputContext) {
        NodeExecutionDetail detail = nodeExecutions.get(nodeId);
        if (detail != null) {
            // Take context snapshot after node execution
            Map<String, Object> afterSnapshot = createContextSnapshot(outputContext);
            contextSnapshots.put("after_" + nodeId, afterSnapshot);

            // Calculate context differences and update the node detail
            Map<String, Object> beforeSnapshot = contextSnapshots.get("before_" + nodeId);
            detail.recordCompletion(success, errorMessage, exception, beforeSnapshot, afterSnapshot);
        }
    }

    /**
     * Records edge evaluation details
     */
    public void recordEdgeEvaluation(String sourceNodeId, String targetNodeId, String edgeId, boolean conditionPassed, String conditionDetails, Map<String, String> appliedBindings) {
        EdgeEvaluationDetail detail = new EdgeEvaluationDetail(
                sourceNodeId, targetNodeId, edgeId, conditionPassed, conditionDetails, appliedBindings, Instant.now()
        );
        edgeEvaluations.add(detail);
    }

    /**
     * Records port adaptation attempts
     */
    public void recordPortAdaptation(String nodeId, List<String> missingInputs, Map<String, String> suggestedBindings, boolean successful, TokenUsage tokenUsage) {
        PortAdaptationDetail detail = new PortAdaptationDetail(
                nodeId, missingInputs, suggestedBindings, successful, Instant.now()
        );
        portAdaptations.add(detail);

        // Record token usage if available
        if (tokenUsage != null) {
            recordTokenUsage(tokenUsage);
        }
    }

    /**
     * Creates a safe snapshot of the execution context
     */
    private Map<String, Object> createContextSnapshot(ExecutionContext context) {
        Map<String, Object> snapshot = new HashMap<>();
        for (String key : context.keySet()) {
            Object value = context.get(key);
            snapshot.put(key, value != null ? value.toString() : null);
        }
        return snapshot;
    }


    /**
     * Record token usage for the workflow execution
     */
    public void recordTokenUsage(TokenUsage tokenUsage) {
        if (tokenUsage != null) {
            tokenUsages.add(tokenUsage);
        }
    }


    /**
     * Calculates various metrics
     */
    private void calculateMetrics() {
        metrics.totalNodes = nodeExecutions.size();
        metrics.successfulNodes = (int) nodeExecutions.values().stream().mapToLong(n -> n.success ? 1 : 0).sum();
        metrics.failedNodes = metrics.totalNodes - metrics.successfulNodes;

        // Timing statistics
        List<Duration> executionTimes = nodeExecutions.values().stream()
                .filter(n -> n.executionTime != null)
                .map(n -> n.executionTime)
                .sorted()
                .toList();

        if (!executionTimes.isEmpty()) {
            metrics.fastestNodeTime = executionTimes.get(0);
            metrics.slowestNodeTime = executionTimes.get(executionTimes.size() - 1);

            // Average
            long totalNanos = executionTimes.stream().mapToLong(Duration::toNanos).sum();
            metrics.averageNodeTime = Duration.ofNanos(totalNanos / executionTimes.size());

            // Median
            int medianIndex = executionTimes.size() / 2;
            metrics.medianNodeTime = executionTimes.size() % 2 == 0
                    ? Duration.ofNanos((executionTimes.get(medianIndex - 1).toNanos() +
                    executionTimes.get(medianIndex).toNanos()) / 2)
                    : executionTimes.get(medianIndex);
        }

        metrics.totalEdgeEvaluations = edgeEvaluations.size();
        metrics.passedEdgeEvaluations = (int) edgeEvaluations.stream().mapToLong(e -> e.conditionPassed ? 1 : 0).sum();
        metrics.totalPortAdaptations = portAdaptations.size();
        metrics.successfulPortAdaptations = (int) portAdaptations.stream().mapToLong(p -> p.successful ? 1 : 0).sum();
    }

    /**
     * Represents the differences in context between two snapshots
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContextDifferences {
        private Map<String, Object> addedKeys = new HashMap<>();
        private Map<String, ValueChange> modifiedKeys = new HashMap<>();
        private Map<String, Object> removedKeys = new HashMap<>();

                /**
                 * Represents a value change from before to after
                 */
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public record ValueChange(Object beforeValue, Object afterValue) {
        }

        /**
         * Check if there are any differences
         */
        public boolean isEmpty() {
            return addedKeys.isEmpty() && modifiedKeys.isEmpty() && removedKeys.isEmpty();
        }
    }

    /**
     * Detailed execution information for a single node
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NodeExecutionDetail {
        private final String nodeId;
        private final String nodeName;
        private final String nodeType;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private Instant startTime;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private Instant endTime;

        @JsonSerialize(using = DurationToMillisSerializer.class)
        private Duration executionTime;
        private boolean success;
        private String errorMessage;
        private Throwable exception;

        // Context differences instead of full snapshots
        private ContextDifferences contextChanges;

        // Resource usage (if available)
        private Long memoryUsedBytes;
        private final Integer threadId;

        public NodeExecutionDetail(String nodeId, String nodeName, String nodeType) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.nodeType = nodeType;
            this.threadId = (int) Thread.currentThread().getId();
        }

        public void recordStart() {
            this.startTime = Instant.now();

            // Record memory usage before execution
            Runtime runtime = Runtime.getRuntime();
            this.memoryUsedBytes = runtime.totalMemory() - runtime.freeMemory();
        }

        public void recordCompletion(boolean success, String errorMessage, Throwable exception,
                                     Map<String, Object> beforeSnapshot, Map<String, Object> afterSnapshot) {
            this.endTime = Instant.now();
            this.executionTime = Duration.between(startTime, endTime);
            this.success = success;
            this.errorMessage = errorMessage;
            this.exception = exception;

            // Calculate context differences instead of storing full snapshots
            this.contextChanges = calculateContextDifferences(beforeSnapshot, afterSnapshot);
        }

        /**
         * Calculates the differences between two context snapshots
         */
        private ContextDifferences calculateContextDifferences(Map<String, Object> before, Map<String, Object> after) {
            ContextDifferences diff = new ContextDifferences();

            if (before == null) before = new HashMap<>();
            if (after == null) after = new HashMap<>();

            // Find added and modified keys
            for (Map.Entry<String, Object> entry : after.entrySet()) {
                String key = entry.getKey();
                Object afterValue = entry.getValue();
                Object beforeValue = before.get(key);

                if (!before.containsKey(key)) {
                    diff.addedKeys.put(key, afterValue);
                } else if (!Objects.equals(beforeValue, afterValue)) {
                    diff.modifiedKeys.put(key, new ContextDifferences.ValueChange(beforeValue, afterValue));
                }
            }

            // Find removed keys
            for (String key : before.keySet()) {
                if (!after.containsKey(key)) {
                    diff.removedKeys.put(key, before.get(key));
                }
            }

            return diff;
        }
    }

    /**
     * Details about edge condition evaluations
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EdgeEvaluationDetail {
        private final String sourceNodeId;
        private final String targetNodeId;
        private final String edgeId;
        private final boolean conditionPassed;
        private final String conditionDetails;
        private final Map<String, String> appliedBindings;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private final Instant evaluationTime;

        public EdgeEvaluationDetail(String sourceNodeId, String targetNodeId, String edgeId,
                                    boolean conditionPassed, String conditionDetails,
                                    Map<String, String> appliedBindings, Instant evaluationTime) {
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
            this.edgeId = edgeId;
            this.conditionPassed = conditionPassed;
            this.conditionDetails = conditionDetails;
            this.appliedBindings = appliedBindings != null ? new HashMap<>(appliedBindings) : null;
            this.evaluationTime = evaluationTime;
        }
    }

    /**
     * Details about port adaptation attempts
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PortAdaptationDetail {
        private final String nodeId;
        private final List<String> missingInputs;
        private final Map<String, String> suggestedBindings;
        private final boolean successful;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private final Instant adaptationTime;

        public PortAdaptationDetail(String nodeId, List<String> missingInputs,
                                    Map<String, String> suggestedBindings, boolean successful,
                                    Instant adaptationTime) {
            this.nodeId = nodeId;
            this.missingInputs = missingInputs != null ? new ArrayList<>(missingInputs) : null;
            this.suggestedBindings = suggestedBindings != null ? new HashMap<>(suggestedBindings) : null;
            this.successful = successful;
            this.adaptationTime = adaptationTime;
        }
    }

    /**
     * Aggregated metrics for the workflow execution
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WorkflowExecutionMetrics {
        private int totalNodes;

        private int successfulNodes;

        private int failedNodes;

        @JsonSerialize(using = DurationToMillisSerializer.class)
        private Duration fastestNodeTime;

        @JsonSerialize(using = DurationToMillisSerializer.class)
        private Duration slowestNodeTime;

        @JsonSerialize(using = DurationToMillisSerializer.class)
        private Duration averageNodeTime;

        @JsonSerialize(using = DurationToMillisSerializer.class)
        private Duration medianNodeTime;

        private int totalEdgeEvaluations;

        private int passedEdgeEvaluations;

        private int totalPortAdaptations;

        private int successfulPortAdaptations;
    }
}