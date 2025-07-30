package org.caselli.cognitiveworkflow.knowledge.validation;

import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortType;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.EdgeCondition;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating WorkflowMetamodels correctness.
 * - DAG structure verification
 * - Node reference validity
 * - Port compatibility between connected nodes
 * @author niccolocaselli
 */
@Service
public class WorkflowMetamodelValidator {

    private final NodeMetamodelService nodeMetamodelService;


    public WorkflowMetamodelValidator(NodeMetamodelService nodeMetamodelService) {
        this.nodeMetamodelService = nodeMetamodelService;
    }

    /**
     * Helper method to get a NodeMetamodel by its ID
     * @param nodeId The ID of the node to retrieve
     * @return The NodeMetamodel if found, null otherwise
     */
    private NodeMetamodel getNodeMetamodelById(String nodeId) {
        Optional<NodeMetamodel> res = nodeMetamodelService.getById(nodeId);
        return res.orElse(null);
    }

    /**
     * Validates a workflow metamodel for correctness
     * @param workflow The workflow metamodel to validate
     * @return Validation result with errors and warnings
     */
    public ValidationResult validate(WorkflowMetamodel workflow) {
        ValidationResult result = new ValidationResult();

        if (workflow == null) {
            result.addError("Workflow metamodel cannot be null", "workflow");
            return result;
        }

        // Basic validations
        validateBasicProperties(workflow, result);

        // Validate node references
        validateNodeReferences(workflow, result);

        // Validate edge references
        validateEdgeReferences(workflow, result);

        // Validate workflow is a DAG (no cycles)
        validateDAG(workflow, result);

        // Validate port compatibility between ports
        validatePortConnections(workflow, result);

        // Validate entry and exit points
        validateEntryAndExitPoints(workflow, result);

        // Validate edge conditions
        validateEdgeConditions(workflow, result);

        return result;
    }



    /**
     * Returns a new map of bindings, excluding any that are invalid or incompatible.
     * This method iterates through the original bindings and only includes those
     * where both the source and target port schemas can be resolved and are compatible.
     *
     * @param source The NodeMetamodel of the source node
     * @param target The NodeMetamodel of the target node
     * @param originalBindings The original map of bindings from the WorkflowEdge (sourceFullPath -> targetFullPath).
     * @return A new Map containing only the valid and compatible bindings
     */
    public Map<String, String> filterCompatibleBindings(NodeMetamodel source, NodeMetamodel target, Map<String, String> originalBindings) {
        if (originalBindings == null || originalBindings.isEmpty()) return Collections.emptyMap();

        Map<String, String> fixedBindings = new HashMap<>();

        for (Map.Entry<String, String> binding : originalBindings.entrySet()) {
            String sourceFullPath = binding.getKey();
            String targetFullPath = binding.getValue();

            PortSchema actualSourceSchema = Port.getResolvedSchemaForPort(source.getOutputPorts(), sourceFullPath);
            PortSchema actualTargetSchema = Port.getResolvedSchemaForPort(target.getInputPorts(), targetFullPath);

            if (actualTargetSchema != null && PortSchema.isCompatible(actualSourceSchema, actualTargetSchema)) {
                fixedBindings.put(sourceFullPath, targetFullPath);
            }
        }

        return fixedBindings;
    }



    /**
     * Validates basic properties of the workflow metamodel
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateBasicProperties(WorkflowMetamodel workflow, ValidationResult result) {

        if (workflow.getName() == null || workflow.getName().isEmpty())
            result.addError("Workflow name cannot be null or empty", "workflow.name");


        if (workflow.getNodes() == null || workflow.getNodes().isEmpty())
            result.addError("Workflow must contain at least one node", "workflow.nodes");


        if (workflow.getEdges() == null)
            result.addError("Workflow edges list cannot be null", "workflow.edges");
        else{
            for (var edge : workflow.getEdges()) {
                if (edge.getId() == null || edge.getId().isEmpty()) {
                    result.addError("All workflow edges must have a non-null and non-empty ID", "workflow.edges");
                    break;
                }
            }
        }
    }

    /**
     * Validates that all nodes referenced in the workflow exist in the catalog
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateNodeReferences(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getNodes() == null) return;

        Set<String> nodeIds = new HashSet<>();

        for (WorkflowNode node : workflow.getNodes()) {
            String nodeId = node.getNodeMetamodelId();

            // Check if node ID is valid
            if (nodeId == null || nodeId.isEmpty()) {
                result.addError("Node ID cannot be null or empty", "workflow.nodes");
                continue;
            }

            // Check for duplicate node IDs
            if (!nodeIds.add(nodeId))
                result.addError("Duplicate node ID found: " + nodeId, "workflow.nodes");


            // Check if node exists in the repository
            var nodeMetamodel = getNodeMetamodelById(nodeId);
            if (nodeMetamodel == null) result.addError("Referenced node does not exist in repository: " + nodeId, "workflow.nodes");
            else if (!nodeMetamodel.getIsLatest()) {
                // Check if the node has a newer version
                result.addWarning("Workflow is using the node " + nodeId + " which has a newer version. Consider updating the dependency", "workflow.nodes");
            }
        }
    }

    /**
     * Validates that all edges reference valid nodes
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateEdgeReferences(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getEdges() == null || workflow.getNodes() == null) return;

        Set<String> nodeIds = workflow.getNodes().stream()
                .map(WorkflowNode::getId)
                .collect(Collectors.toSet());

        for (WorkflowEdge edge : workflow.getEdges()) {
            String sourceId = edge.getSourceNodeId();
            String targetId = edge.getTargetNodeId();

            if (sourceId == null || sourceId.isEmpty())
                result.addError("Edge source node ID cannot be null or empty", "workflow.edges");
            else if (!nodeIds.contains(sourceId))

                result.addError("Edge references non-existent source node: " + sourceId, "workflow.edges");

            if (targetId == null || targetId.isEmpty())
                result.addError("Edge target node ID cannot be null or empty", "workflow.edges");

            else if (!nodeIds.contains(targetId))
                result.addError("Edge references non-existent target node: " + targetId, "workflow.edges");


            // Self-loop check
            if (sourceId != null && sourceId.equals(targetId))
                result.addError("Self-loop detected on node: " + sourceId, "workflow.edges");

        }
    }

    /**
     * Validates that the workflow is a Directed Acyclic Graph (DAG)
     * Uses topological sort to detect cycles
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateDAG(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getNodes() == null || workflow.getEdges() == null) return;

        // Adjacency list using WORKFLOW NODE IDs (important!)
        Map<String, List<String>> adjacencyList = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // Initialize with workflow node IDs
        for (WorkflowNode node : workflow.getNodes()) {
            String nodeId = node.getId(); // Critical fix: use workflow node ID
            adjacencyList.put(nodeId, new ArrayList<>());
            inDegree.put(nodeId, 0);
        }

        // Build adjacency list and in-degree map using edge refs
        for (WorkflowEdge edge : workflow.getEdges()) {
            String sourceId = edge.getSourceNodeId();
            String targetId = edge.getTargetNodeId();

            if (sourceId != null && targetId != null
                    && adjacencyList.containsKey(sourceId)
                    && adjacencyList.containsKey(targetId)) {
                adjacencyList.get(sourceId).add(targetId);
                inDegree.put(targetId, inDegree.get(targetId) + 1);
            }
        }

        // Topological sort (Kahn's algorithm)
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet())
            if (entry.getValue() == 0) queue.add(entry.getKey());

        int visitedCount = 0;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            visitedCount++;

            for (String neighbor : adjacencyList.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0)
                    queue.add(neighbor);
            }
        }

        // Cycle detection logic
        if (visitedCount != workflow.getNodes().size()) {
            result.addError("Cycle detected in workflow graph", "workflow.structure");

            // Identify nodes involved in cycles
            Set<String> cyclicNodes = inDegree.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            result.addError("Nodes involved in cycles: " + String.join(", ", cyclicNodes), "workflow.structure");
        }
    }

    /**
     * Validates that the workflow has a clear entry point and exit points
     * @param workflow Current workflow
     * @param result Validation result
     */
    public void validateEntryAndExitPoints(WorkflowMetamodel workflow, ValidationResult result) {
        Set<String> entryPoints = workflow.getEntryNodes();
        if (entryPoints.isEmpty()) result.addError("Workflow has no entry point", "workflow.structure");

        Set<String> exitPoints = workflow.getExitNodes();
        if (exitPoints.isEmpty()) result.addError("Workflow has no exit point (all nodes have outgoing edges)", "workflow.structure");
    }

    /**
     * Validates that edge conditions reference existing ports with compatible types.
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateEdgeConditions(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getEdges() == null || workflow.getNodes() == null) return;

        Map<String, NodeMetamodel> nodesById = workflow.getNodes().stream()
                .filter(node -> node.getNodeMetamodelId() != null)
                .filter(node -> getNodeMetamodelById(node.getNodeMetamodelId()) != null)
                .collect(Collectors.toMap(
                        WorkflowNode::getNodeMetamodelId,
                        node -> getNodeMetamodelById(node.getNodeMetamodelId())
                ));

        for (WorkflowEdge edge : workflow.getEdges()) {
            if (edge.getCondition() == null) continue;

            for (var expression :edge.getCondition().getExpressions()) {


            if (expression.getPort() == null) {
                result.addError(
                        "Condition port cannot be null or empty",
                        "workflow.edges." + edge.getId()
                );
                continue;
            }


            String sourceNodeId = edge.getSourceNodeId();

            NodeMetamodel sourceNode = nodesById.get(sourceNodeId);
            if (sourceNode == null) continue;

            String portKey = expression.getPort();

            // Check if the port exists in the source node's outputs
            if (sourceNode.getOutputPorts() == null || sourceNode.getOutputPorts().stream().noneMatch(p -> p.getKey().equals(portKey))) {

                result.addError(
                        "Condition references non-existent output port '" + portKey +
                                "' in node: " + sourceNodeId,
                        "workflow.edges." + edge.getId()
                );

                continue;
            }

            // Check if the port type is compatible with the expected value
            Optional<? extends Port> port = sourceNode.getOutputPorts().stream()
                    .filter(p -> p.getKey().equals(portKey))
                    .findFirst();

            if (port.isPresent()) {

                var expectedValue = expression.getValue().toString();

                PortSchema schema = port.get().getSchema();
                if (schema != null) {
                    boolean isValidValue = validateExpectedValue(schema.getType(), expectedValue, expression.getOperation());
                    if (!isValidValue) {
                        result.addError(
                                "Condition value '" + expectedValue +
                                        "' is incompatible with port type '" + schema.getType() + "'",
                                "workflow.edges." + edge.getId()
                        );
                    }
                }
            }
        }
        }
    }

    /**
     * Checks if a condition's expected value matches the port's type.
     * @param portType The type of the port
     * @param expectedValue The expected value to validate
     * @param operation The operation to validate against
     */
    private boolean validateExpectedValue(PortType portType, String expectedValue, EdgeCondition.Operation operation){

        if (expectedValue == null)
            return operation == EdgeCondition.Operation.IS_NULL || operation == EdgeCondition.Operation.IS_NOT_NULL ||
                    operation == EdgeCondition.Operation.IS_TRUE || operation == EdgeCondition.Operation.IS_FALSE;

        if(operation == EdgeCondition.Operation.CONTAINS || operation == EdgeCondition.Operation.STARTS_WITH || operation == EdgeCondition.Operation.IN || operation == EdgeCondition.Operation.NOT_IN) {
            return portType == PortType.ARRAY;
        }

        try {
            return switch (portType) {
                case BOOLEAN -> expectedValue.equalsIgnoreCase("true") ||
                        expectedValue.equalsIgnoreCase("false");
                case INT -> {
                    Integer.parseInt(expectedValue);
                    yield true;
                }
                case FLOAT -> {
                    Float.parseFloat(expectedValue);
                    yield true;
                }
                case STRING -> true;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates port connections within a workflow, including type compatibility and satisfaction of required inputs.
     * Supports dot-notation for bindings.
     *
     * <p><b>Important notes:</b>
     * <ul>
     *   <li>Port incompatibility issues generate warnings rather than errors, as they are not fatal
     *       and can be corrected at runtime by a PortAdaptor.</li>
     *   <li>Errors are only thrown when an implicit binding is incompatible.</li>
     * </ul>
     *
     * @param workflow The workflow metamodel to validate
     * @param result   The validation result object that will contain any warnings or errors
     * @throws IllegalArgumentException If the workflow or result parameters are null
     */
    private void validatePortConnections(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getNodes() == null || workflow.getEdges() == null) return;

        Map<String, NodeMetamodel> workflowNodeIdToMetamodel = new HashMap<>();
        Map<String, WorkflowNode> workflowNodesMap = new HashMap<>();

        for (WorkflowNode wn : workflow.getNodes()) {
            if (wn.getId() != null && !wn.getId().isEmpty()) {
                workflowNodesMap.put(wn.getId(), wn);
                if (wn.getNodeMetamodelId() != null && !wn.getNodeMetamodelId().isEmpty()) {
                    NodeMetamodel nmm = getNodeMetamodelById(wn.getNodeMetamodelId());

                    if (nmm != null) workflowNodeIdToMetamodel.put(wn.getId(), nmm);
                }
            }
        }

        Map<String, Set<String>> satisfiedRequiredInputs = new HashMap<>();
        Map<String, Set<String>> allRequiredInputs = new HashMap<>();

        for (WorkflowNode wn : workflow.getNodes()) {
            String wnId = wn.getId();
            if (wnId == null || wnId.isEmpty()) continue;
            NodeMetamodel nmm = workflowNodeIdToMetamodel.get(wnId);
            if (nmm == null || nmm.getInputPorts() == null) continue;

            Set<String> requiredBaseInputs = nmm.getInputPorts().stream()
                    .filter(port -> port.getKey() != null && port.getSchema() != null && Boolean.TRUE.equals(port.getSchema().getRequired()))
                    .map(Port::getKey)
                    .collect(Collectors.toSet());

            if (!requiredBaseInputs.isEmpty()) {
                allRequiredInputs.put(wnId, requiredBaseInputs);
                satisfiedRequiredInputs.put(wnId, new HashSet<>());
            }
        }

        // Loop over each edge
        for (WorkflowEdge edge : workflow.getEdges()) {
            String sourceId = edge.getSourceNodeId();
            String targetId = edge.getTargetNodeId();

            if (sourceId == null || targetId == null || !workflowNodeIdToMetamodel.containsKey(sourceId) || !workflowNodeIdToMetamodel.containsKey(targetId)) continue;

            NodeMetamodel sourceNodeMetamodel = workflowNodeIdToMetamodel.get(sourceId);
            NodeMetamodel targetNodeMetamodel = workflowNodeIdToMetamodel.get(targetId);

            // Check the compatibility and the satisfiability of the source and target nodes
            Set<String> satisfiedByThisEdge = checkAndReportEdgePortCompatibility(
                    edge, sourceNodeMetamodel, targetNodeMetamodel, result
            );

            if (allRequiredInputs.containsKey(targetId)) satisfiedRequiredInputs.get(targetId).addAll(satisfiedByThisEdge);
        }


        // Final check of all required inputs
        for (Map.Entry<String, Set<String>> entry : allRequiredInputs.entrySet()) {
            String workflowNodeId = entry.getKey();

            // Skip entry nodes as their inputs are provided externally
            if (workflow.getEntryNodes().contains(workflowNodeId)) continue;

            Set<String> required = entry.getValue();
            Set<String> satisfied = satisfiedRequiredInputs.getOrDefault(workflowNodeId, Collections.emptySet());
            Set<String> unsatisfied = new HashSet<>(required);
            unsatisfied.removeAll(satisfied);

            if (!unsatisfied.isEmpty()) {
                WorkflowNode wn = workflowNodesMap.get(workflowNodeId);
                String nmmId = (wn != null && wn.getNodeMetamodelId() != null) ? wn.getNodeMetamodelId() : "[UNKNOWN_METAMODEL]";
                result.addWarning("WorkflowNode ID '" + workflowNodeId + "' (Metamodel ID: '" + nmmId + "') has unsatisfied required input port(s): " + String.join(", ", unsatisfied), "workflow.nodes." + workflowNodeId + ".requiredInputs.unsatisfied");
            }
        }
    }


    /**
     * Performs compatibility checks for a single edge's explicit and implicit bindings.
     *
     * @param edge The WorkflowEdge to validate.
     * @param sourceNodeMetamodel The NodeMetamodel of the source node.
     * @param targetNodeMetamodel The NodeMetamodel of the target node.
     * @param result The ValidationResult to add errors/warnings to.
     * @return A Set of base input port keys that are satisfied by this edge (for required input tracking).
     */
    private Set<String> checkAndReportEdgePortCompatibility(
            WorkflowEdge edge,
            NodeMetamodel sourceNodeMetamodel,
            NodeMetamodel targetNodeMetamodel,
            ValidationResult result) {

        String sourceId = edge.getSourceNodeId();
        String targetId = edge.getTargetNodeId();
        String edgeId = edge.getId();
        String edgeContext = "workflow.edges." + (edgeId == null ? "[UNIDENTIFIED_EDGE]" : edgeId);

        Set<String> explicitlyBoundTargetBasePorts = new HashSet<>();
        Set<String> satisfiedBasePorts = new HashSet<>(); // Ports on target that this edge satisfies

        // 1) Explicit Bindings
        if (edge.getBindings() != null && !edge.getBindings().isEmpty()) {
            for (Map.Entry<String, String> binding : edge.getBindings().entrySet()) {
                String sourceFullPath = binding.getKey();
                String targetFullPath = binding.getValue();

                PortSchema actualSourceSchema = Port.getResolvedSchemaForPort(sourceNodeMetamodel.getOutputPorts(), sourceFullPath);
                PortSchema actualTargetSchema = Port.getResolvedSchemaForPort(targetNodeMetamodel.getInputPorts(), targetFullPath);

                if (actualSourceSchema != null && actualTargetSchema != null) {
                    if (!PortSchema.isCompatible(actualSourceSchema, actualTargetSchema)) {
                        result.addError("Port type mismatch on explicit binding: Source '" + sourceFullPath + "' (type: " + actualSourceSchema.getType() + ") is incompatible with target '" + targetFullPath + "' (type: " + actualTargetSchema.getType() + "). " + "Edge: '" + edgeId + "', WorkflowNodes: '" + sourceId + "' -> '" + targetId + "'.", edgeContext + ".binding.typeMismatch");
                    } else {
                        // If compatible, it satisfies the port
                        satisfiedBasePorts.add(targetFullPath.split("\\.", 2)[0]);
                    }
                    explicitlyBoundTargetBasePorts.add(targetFullPath.split("\\.", 2)[0]);
                } else {
                    if (actualSourceSchema == null) {
                        result.addError("Source port path '" + sourceFullPath + "' in explicit binding not found or invalid schema in node: " + sourceId, edgeContext);
                    }
                    if (actualTargetSchema == null) {
                        result.addError("Target port path '" + targetFullPath + "' in explicit binding not found or invalid schema in node: " + targetId, edgeContext);
                    }
                }
            }
        }

        // 2. Implicit Bindings
        if (targetNodeMetamodel.getInputPorts() != null && sourceNodeMetamodel.getOutputPorts() != null) {
            Map<String, Port> targetInputBasePorts = targetNodeMetamodel.getInputPorts().stream()
                    .filter(p -> p.getKey() != null)
                    .collect(Collectors.toMap(Port::getKey, p -> p, (p1, p2) -> p1));

            Map<String, Port> sourceOutputBasePorts = sourceNodeMetamodel.getOutputPorts().stream()
                    .filter(p -> p.getKey() != null)
                    .collect(Collectors.toMap(Port::getKey, p -> p, (p1, p2) -> p1));

            for (Map.Entry<String, Port> targetPortEntry : targetInputBasePorts.entrySet()) {
                String targetBaseKey = targetPortEntry.getKey();
                Port targetBasePort = targetPortEntry.getValue();

                // Consider for implicit binding only if not explicitly bound in this edge
                if (!explicitlyBoundTargetBasePorts.contains(targetBaseKey) && sourceOutputBasePorts.containsKey(targetBaseKey)) {
                    Port sourceBasePort = sourceOutputBasePorts.get(targetBaseKey);
                    if (sourceBasePort.getSchema() != null && targetBasePort.getSchema() != null) {
                        if (!PortSchema.isCompatible(sourceBasePort.getSchema(), targetBasePort.getSchema())) {
                            result.addWarning("Port type mismatch on implicitly matched port '" + targetBaseKey + "': Source type " + sourceBasePort.getSchema().getType() + " to target type " + targetBasePort.getSchema().getType() + ". Edge: '" + edgeId + "', WorkflowNodes: '" + sourceId + "' -> '" + targetId + "'.", edgeContext + ".implicitBinding.typeMismatch");
                        } else {
                            // If compatible, it satisfies the port
                            satisfiedBasePorts.add(targetBaseKey);
                        }
                    } else {
                        if (sourceBasePort.getSchema() == null) result.addWarning("Source port '" + targetBaseKey + "' in implicit match has no schema. Node: " + sourceId, edgeContext);
                        if (targetBasePort.getSchema() == null) result.addWarning("Target port '" + targetBaseKey + "' in implicit match has no schema. Node: " + targetId, edgeContext);
                    }
                }
            }
        }
        return satisfiedBasePorts;
    }
}
