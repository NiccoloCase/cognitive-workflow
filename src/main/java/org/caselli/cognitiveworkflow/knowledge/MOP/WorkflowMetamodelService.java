package org.caselli.cognitiveworkflow.knowledge.MOP;

import jakarta.annotation.Nonnull;
import org.apache.coyote.BadRequestException;
import org.caselli.cognitiveworkflow.knowledge.MOP.event.WorkflowMetamodelUpdateEvent;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.WorkflowMetamodelCatalog;
import org.caselli.cognitiveworkflow.knowledge.validation.ValidationResult;
import org.caselli.cognitiveworkflow.knowledge.validation.WorkflowMetamodelValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing workflow metamodels in the knowledge layer.
 * @author niccolocaselli
 */
@Service
public class WorkflowMetamodelService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowMetamodelService.class);

    private final WorkflowMetamodelCatalog repository;
    private final ApplicationEventPublisher eventPublisher;
    final private  WorkflowMetamodelValidator workflowMetamodelValidator;
    final private NodeMetamodelService nodeMetamodelService;

    public WorkflowMetamodelService(WorkflowMetamodelCatalog repository, ApplicationEventPublisher eventPublisher, WorkflowMetamodelValidator workflowMetamodelValidator, NodeMetamodelService nodeMetamodelService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.workflowMetamodelValidator = workflowMetamodelValidator;
        this.nodeMetamodelService = nodeMetamodelService;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        // Only for demo purposes. Not required in production.
        this.validateAllCatalog();
    }


    /**
     * Get all the Workflows metamodel in the MongoDB collection
     * @return All the existing workflows metamodel
     */
    @Cacheable(value = "workflowMetamodels")
    public List<WorkflowMetamodel> getAllWorkflows() {
        return repository.findAll();
    }

    /**
     * Get a specific Workflow metamodel by its ID
     */
    @Cacheable(value = "workflowMetamodels", key = "#id")
    public Optional<WorkflowMetamodel> getWorkflowById(String id) {
        return repository.findById(id);
    }

    /**
     * Save in the DB a new Workflow Metamodel
     * @param workflowMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    @CacheEvict(value = "workflowMetamodels", allEntries = true)
    public WorkflowMetamodel createWorkflow(WorkflowMetamodel workflowMetamodel) throws BadRequestException {
        if (workflowMetamodel.getId() != null && repository.existsById(workflowMetamodel.getId())) {
            throw new BadRequestException("WorkflowMetamodel with id " + workflowMetamodel.getId() + " already exists.");
        }

        workflowMetamodel.setId(UUID.randomUUID().toString());
        workflowMetamodel.setCreatedAt(LocalDateTime.now());

        // Validate the workflow
        var res = workflowMetamodelValidator.validate(workflowMetamodel);
        if(!res.isValid()) throw new BadRequestException("WorkflowMetamodel is not valid: " + res.getErrors());

        return repository.save(workflowMetamodel);
    }

    /**
     * Update an existing Workflow Metamodel
     * Updates the MongoDB document and notifies the Operational Layer
     * @param id Id of the Workflow Metamodel
     * @param updatedData New Workflow Metamodel
     * @return Return the newly saved Document
     */
    @CacheEvict(value = "workflowMetamodels", key = "#id")
    public WorkflowMetamodel updateWorkflow(String id, WorkflowMetamodel updatedData) {

        // Check if the documents exists
        repository.findById(id).orElseThrow(() -> new IllegalArgumentException("WorkflowMetamodel with id " + id + " does not exist."));

        // Make sure that the id is present
        updatedData.setId(id);


        // Validate the workflow
        var res = workflowMetamodelValidator.validate(updatedData);
        if(!res.isValid()) throw new IllegalArgumentException("WorkflowMetamodel is not valid: " + res.getErrors());


        WorkflowMetamodel saved = repository.save(updatedData);

        // Notify the Operational Level of the modification
        eventPublisher.publishEvent(new WorkflowMetamodelUpdateEvent(id, saved));

        return saved;
    }


    /**
     * Delete a workflow metamodel by its ID
     * @param id ID of the workflow metamodel to delete
     */
    @CacheEvict(value = "workflowMetamodels", key = "#id")
    public void deleteWorkflow(String id) {
        repository.deleteById(id);
    }


    /**
     * Finds the first N workflow metamodels that can handle the specified intent.
     * Results sorted by the intent's score in descending order
     * @param intentId The ID of the intent to search for
     * @param n The number of workflows to retrieve
     * @return A list of workflow metamodels that handle the intent, sorted by score
     */
    @Cacheable(value = "workflowMetamodels", key = "#intentId + '_' + #n")
    public List<WorkflowMetamodel> findTopNHandlingIntent(String intentId, int n) {
        return repository.findByHandledIntents_IntentId(intentId, n);
    }



    /**
     * Update the bindings of multiple edges within a workflow metamodel in a single operation
     * @param workflowId The ID of the workflow metamodel containing the edges
     * @param edgeBindingsMap A map where keys are edge IDs and values are the new bindings for each edge
     * @throws IllegalArgumentException if workflow or any edge is not found, or if validation fails
     */
    @CacheEvict(value = "workflowMetamodels", key = "#workflowId")
    public void updateMultipleEdgeBindings(String workflowId, Map<String, Map<String, String>> edgeBindingsMap) {

        logger.info("Updating bindings for {} edges in workflow {}", edgeBindingsMap.size(), workflowId);

        // Retrieve the workflow metamodel
        WorkflowMetamodel workflow = repository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("WorkflowMetamodel with id " + workflowId + " does not exist."));

        // Update bindings for each edge
        boolean hasChanges = false;
        for (Map.Entry<String, Map<String, String>> entry : edgeBindingsMap.entrySet()) {
            String edgeId = entry.getKey();
            Map<String, String> newBindings = entry.getValue();

            // Find the edge to update
            WorkflowEdge targetEdge = workflow.getEdges().stream()
                    .filter(edge -> edgeId.equals(edge.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Edge with id " + edgeId + " not found in workflow " + workflowId));


            // Update the bindings with filtered ones
            targetEdge.setBindings(newBindings);
            hasChanges = true;

            logger.info("Updated bindings for edge {} with: {}", edgeId, newBindings);
        }

        if (!hasChanges) {
            logger.info("No edge bindings to update for workflow {}", workflowId);
            return;
        }

        // Remove the bindings that are not compatible
        filterOutIncompatibleEdges(workflow);


        // Validate the updated workflow
        ValidationResult validationResult = workflowMetamodelValidator.validate(workflow);
        if (!validationResult.isValid()) throw new IllegalArgumentException("Updated workflow is not valid: " + validationResult.getErrors());

        // Save the updated workflow
        WorkflowMetamodel savedWorkflow = repository.save(workflow);

        // Publish update event to notify the Operational Layer
        eventPublisher.publishEvent(new WorkflowMetamodelUpdateEvent(workflowId, savedWorkflow));

        logger.info("Successfully updated bindings for {} edges in workflow {}", edgeBindingsMap.size(), workflowId);
    }


    /**
     * Remove an intent from all workflows that reference it
     * This method is called when an intent is being deleted
     * @param intentId The intent ID to remove from all workflows
     */
    public void removeIntentFromAllWorkflows(String intentId) {
        logger.info("Removing intent {} from all referencing workflows", intentId);

        // Find all workflows that reference this intent
        List<WorkflowMetamodel> referencingWorkflows = findWorkflowsReferencingIntent(intentId);

        if (referencingWorkflows.isEmpty()) {
            logger.info("No workflows reference intent {}, nothing to update", intentId);
            return;
        }

        logger.info("Found {} workflow(s) referencing intent {}. Removing references.",
                referencingWorkflows.size(), intentId);

        int successCount = 0;
        int failureCount = 0;

        for (WorkflowMetamodel workflow : referencingWorkflows) {
            try {
                removeIntentFromWorkflow(workflow, intentId);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to remove intent {} from workflow {}: {}", intentId, workflow.getId(), e.getMessage());
            }
        }

        logger.info("Completed intent {} removal: {} successful, {} failed", intentId, successCount, failureCount);

        if (failureCount > 0)
            throw new RuntimeException(String.format("Failed to remove intent %s from %d out of %d workflows", intentId, failureCount, referencingWorkflows.size()));
    }

    /**
     * Find all workflows that reference the given intent ID
     * @param intentId The intent ID to search for
     * @return List of workflows that reference this intent
     */
    private List<WorkflowMetamodel> findWorkflowsReferencingIntent(String intentId) {
        // Use the existing repository method but without limit to get all matching workflows
        // This leverages the existing MongoDB aggregation pipeline but removes the enabled filter
        // We need to update all workflows regardless of their enabled status
        return repository.findAllByHandledIntents_IntentId(intentId);
    }

    /**
     * Remove intent reference from a single workflow
     * @param workflow The workflow to update
     * @param intentId The intent ID to remove
     */
    private void removeIntentFromWorkflow(WorkflowMetamodel workflow, String intentId) {
        // Remove the intent from the handledIntents list
        if (workflow.getHandledIntents() != null) {
            List<WorkflowMetamodel.WorkflowIntentCapability> updatedIntents =
                    workflow.getHandledIntents().stream()
                            .filter(intent -> !intentId.equals(intent.getIntentId()))
                            .collect(java.util.stream.Collectors.toList());

            workflow.setHandledIntents(updatedIntents);

            // Use the existing update method to ensure proper validation and caching
            updateWorkflow(workflow.getId(), workflow);

            logger.info("Removed intent {} reference from workflow {}", intentId, workflow.getId());
        }
    }



    /**
     * Validates all workflow metamodels in the MongoDB repository.
     * Prints the validation results to the logs.
     */
    private void validateAllCatalog() {
        logger.info("-------------------------------------------------");
        logger.info("Starting validation of Workflow Metamodels on Startup...");

        List<WorkflowMetamodel> workflows = repository.findAll();

        int validCount = 0;
        int invalidCount = 0;
        int totalWarnings = 0;
        int totalErrors = 0;

        for (WorkflowMetamodel workflow : workflows) {

            ValidationResult result = workflowMetamodelValidator.validate(workflow);

            if (result.isValid()) {
                validCount++;
                logger.info("[{}/{}] Workflow metamodel with ID {} is valid.", validCount + invalidCount, workflows.size(), workflow.getId());


                int warningCount = result.getWarningCount();
                if (warningCount > 0) {
                    totalWarnings += warningCount;
                    logger.warn("Found {} warnings for valid workflow ID {}", warningCount, workflow.getId());
                }

                result.printWarnings(logger);

            } else {
                invalidCount++;

                logger.error("[{}/{}] Workflow metamodel with ID {} is invalid", validCount + invalidCount, workflows.size(), workflow.getId());

                // Count and log errors/warnings
                int errorCount = result.getErrorCount();
                int warningCount = result.getWarningCount();
                totalErrors += errorCount;
                totalWarnings += warningCount;

                logger.error("Found {} errors and {} warnings for invalid workflow ID {}", errorCount, warningCount, workflow.getId());

                result.printWarnings(logger);
                result.printErrors(logger);
            }
        }


        logger.info("Validation completed. Results:");
        logger.info(" - Total workflows processed: {}", workflows.size());
        logger.info(" - Valid workflows: {}", validCount);
        logger.info(" - Invalid workflows: {}", invalidCount);
        logger.info(" - Total warnings across all workflows: {}", totalWarnings);
        logger.info(" - Total errors across all workflows: {}", totalErrors);
        logger.info("-------------------------------------------------");
    }



    /**
     * Filters out incompatible edges in the workflow metamodel.
     * This method ensures that edges have valid source and target nodes and that all edge bindings refer to existing and compatible ports
     * between source and target nodes. Edges with invalid nodes are removed entirely.
     * @param metamodel The workflow metamodel containing edges to filter
     */
    private void filterOutIncompatibleEdges(WorkflowMetamodel metamodel) {
        logger.info("Filtering incompatible edges and bindings for workflow {}", metamodel.getId());

        int totalFilteredBindings = 0;
        int totalRemovedEdges = 0;
        Iterator<WorkflowEdge> edgeIterator = metamodel.getEdges().iterator();

        while (edgeIterator.hasNext()) {
            WorkflowEdge edge = edgeIterator.next();

            try {
                // Get source and target nodes for filtering
                String sourceNodeId = edge.getSourceNodeId();
                String targetNodeId = edge.getTargetNodeId();

                // Find source node in workflow
                var sourceWorkflowNode = metamodel.getNodes().stream()
                        .filter(node -> sourceNodeId.equals(node.getId()))
                        .findFirst();

                if (sourceWorkflowNode.isEmpty()) {
                    logger.warn("Removing edge {} - source node with id {} not found in workflow {}",
                            edge.getId(), sourceNodeId, metamodel.getId());
                    edgeIterator.remove();
                    totalRemovedEdges++;
                    continue;
                }

                // Find target node in workflow
                var targetWorkflowNode = metamodel.getNodes().stream()
                        .filter(node -> targetNodeId.equals(node.getId()))
                        .findFirst();

                if (targetWorkflowNode.isEmpty()) {
                    logger.warn("Removing edge {} - target node with id {} not found in workflow {}",
                            edge.getId(), targetNodeId, metamodel.getId());
                    edgeIterator.remove();
                    totalRemovedEdges++;
                    continue;
                }

                // Get node metamodel IDs
                String sourceNodeMetamodelId = sourceWorkflowNode.get().getNodeMetamodelId();
                String targetNodeMetamodelId = targetWorkflowNode.get().getNodeMetamodelId();

                // Retrieve node metamodels from service
                var sourceNodeMetamodel = this.nodeMetamodelService.getById(sourceNodeMetamodelId);
                var targetNodeMetamodel = this.nodeMetamodelService.getById(targetNodeMetamodelId);

                // Check if node metamodels exist
                if (sourceNodeMetamodel.isEmpty()) {
                    logger.warn("Removing edge {} - source node metamodel with id {} not found",
                            edge.getId(), sourceNodeMetamodelId);
                    edgeIterator.remove();
                    totalRemovedEdges++;
                    continue;
                }

                if (targetNodeMetamodel.isEmpty()) {
                    logger.warn("Removing edge {} - target node metamodel with id {} not found",
                            edge.getId(), targetNodeMetamodelId);
                    edgeIterator.remove();
                    totalRemovedEdges++;
                    continue;
                }

                // Process edge bindings if they exist
                if (edge.getBindings() != null && !edge.getBindings().isEmpty()) {
                    // Get current bindings
                    Map<String, String> currentBindings = new HashMap<>(edge.getBindings());

                    // Filter bindings to ensure compatibility
                    var filteredBindings = workflowMetamodelValidator.filterCompatibleBindings(
                            sourceNodeMetamodel.get(),
                            targetNodeMetamodel.get(),
                            currentBindings
                    );

                    // Log filtered out bindings
                    Map<String, String> filteredOutBindings = new HashMap<>(currentBindings);
                    filteredBindings.forEach(filteredOutBindings::remove);

                    if (!filteredOutBindings.isEmpty()) {
                        totalFilteredBindings += filteredOutBindings.size();
                        logger.info("Filtered out {} incompatible bindings for edge {} (from {} to {}): {}",
                                filteredOutBindings.size(), edge.getId(), sourceNodeId, targetNodeId, filteredOutBindings);
                    }

                    // Update the edge with filtered bindings
                    edge.setBindings(filteredBindings);
                }

            } catch (Exception e) {
                logger.error("Error processing edge {} in workflow {} - removing edge: {}",
                        edge.getId(), metamodel.getId(), e.getMessage());
                edgeIterator.remove();
                totalRemovedEdges++;
            }
        }

        logger.info("Completed filtering for workflow {}. Edges removed: {}, Bindings filtered: {}",
                metamodel.getId(), totalRemovedEdges, totalFilteredBindings);
    }
}
