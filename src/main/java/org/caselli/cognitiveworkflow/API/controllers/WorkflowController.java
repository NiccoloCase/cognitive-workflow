package org.caselli.cognitiveworkflow.API.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;
import org.apache.coyote.BadRequestException;
import org.caselli.cognitiveworkflow.config.EnvironmentHelper;
import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Version;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.execution.WorkflowOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller for managing workflow operations.
 * @author niccolocaselli
 */
@Validated
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowMetamodelService workflowMetamodelService;
    private final WorkflowOrchestrator workflowOrchestrator;

    private final EnvironmentHelper environmentHelper;

    @Autowired
    public WorkflowController(WorkflowMetamodelService workflowService, WorkflowOrchestrator workflowOrchestrator, EnvironmentHelper environmentHelper) {
        this.workflowMetamodelService = workflowService;
        this.workflowOrchestrator = workflowOrchestrator;
        this.environmentHelper = environmentHelper;
    }

    @GetMapping
    public ResponseEntity<List<WorkflowMetamodel>> getAllWorkflows() {
        return ResponseEntity.ok(workflowMetamodelService.getAllWorkflows());
    }

    @PostMapping
    public ResponseEntity<WorkflowMetamodel> createWorkflow(@Valid @RequestBody WorkflowMetamodel workflow) throws BadRequestException {
        return ResponseEntity.ok(workflowMetamodelService.createWorkflow(workflow));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowMetamodel> updateWorkflow(
            @PathVariable String id,
            @Valid @RequestBody WorkflowMetamodel metamodel) throws BadRequestException {


        // Check that the workflow exists
        var existing = workflowMetamodelService.getWorkflowById(id);
        if (existing.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();


        // Check if the version is valid
        if (!Version.isValidVersionBump(existing.get().getVersion(), metamodel.getVersion())) {
            throw new BadRequestException(
                    String.format("Invalid version bump: the new version %s is not compatible with the existing version %s",
                            metamodel.getVersion(),
                            existing.get().getVersion())
            );
        }

        return ResponseEntity.ok(workflowMetamodelService.updateWorkflow(id, metamodel));
    }

    @PostMapping("/execute")
    public ResponseEntity<Object> execute(@RequestBody ExecuteDTO request) {
        long startTime = System.nanoTime();
        var res = workflowOrchestrator.orchestrateWorkflow(request.request);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println("Execution time: " + duration + " nanoseconds");

        /*
            On dev the output always include the observability trace, unless it is explicitly disabled
            by setting observability=false in the request body
         */
        var isObservabilityActive = environmentHelper.isDev();
        if(!request.observability) isObservabilityActive = false;

        if(isObservabilityActive) return ResponseEntity.ok(res);
        else return ResponseEntity.ok(res.getOutput());
    }

    @Data
    public static class ExecuteDTO {
        @JsonProperty("request")
        String request;

        @JsonProperty("observability")
        boolean observability;
    }
}