package org.caselli.cognitiveworkflow.API.controllers;

import org.apache.coyote.BadRequestException;
import org.caselli.cognitiveworkflow.knowledge.MOP.NodeHybridSearchService;
import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.node.*;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * REST controller for managing node operations.
 * @author niccolocaselli
 */
@Validated
@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final NodeMetamodelService nodeMetamodelService;

    @Autowired
    public NodeController(NodeMetamodelService nodeMetamodelService) {
        this.nodeMetamodelService = nodeMetamodelService;
    }

    /**
     * Get all node metamodels with pagination
     */
    @GetMapping
    public ResponseEntity<Page<NodeMetamodel>> getAllNodeMetamodels(Pageable pageable) {
        Page<NodeMetamodel> page = nodeMetamodelService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Search for specific nodes
     */
    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(name = "query", required = true) String query) {
        var filter = new NodeHybridSearchService.NodeSearchFilter();
        var res = nodeMetamodelService.search(query, filter);
        return ResponseEntity.ok(res);
    }

    /**
     * Create a new LLM node metamodel
     */
    @PostMapping("/llm")
    public ResponseEntity<NodeMetamodel> createLlmNodeMetamodel(@Valid @RequestBody LlmNodeMetamodel llmNodeMetamodel) throws BadRequestException {
        var result = nodeMetamodelService.createNodeMetamodel(llmNodeMetamodel);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Create a new REST Tool node metamodel
     */
    @PostMapping("/rest-tool")
    public ResponseEntity<NodeMetamodel> createRestToolNodeMetamodel(@Valid @RequestBody RestNodeMetamodel restNodeMetamodel) throws BadRequestException {
        var result = nodeMetamodelService.createNodeMetamodel(restNodeMetamodel);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }


    /**
     * Create a new Vector DB node metamodel
     */
    @PostMapping("/vector-db")
    public ResponseEntity<NodeMetamodel> createVectorDbNodeMetamodel(@Valid @RequestBody VectorDbNodeMetamodel metamodel) throws BadRequestException {
        var result = nodeMetamodelService.createNodeMetamodel(metamodel);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }


    /**
     * Create a new Embeddings AI node metamodel
     */
    @PostMapping("/embeddings")
    public ResponseEntity<NodeMetamodel> createEmbeddingsNodeMetamodel(@Valid @RequestBody EmbeddingsNodeMetamodel metamodel) throws BadRequestException {
        var result = nodeMetamodelService.createNodeMetamodel(metamodel);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Create a new Gateway
     */
    @PostMapping("/gateway")
    public ResponseEntity<NodeMetamodel> createLlmNodeMetamodel(@Valid @RequestBody GatewayNodeMetamodel gatewayNodeMetamodel) throws BadRequestException {
        var result = nodeMetamodelService.createNodeMetamodel(gatewayNodeMetamodel);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }


    /**
     * Update an existing Vector DB node metamodel
     */
    @PutMapping("/vector-db/{id}")
    public ResponseEntity<NodeMetamodel> updateVectorDbNodeMetamodel(
            @PathVariable String id,
            @Valid @RequestBody VectorDbNodeMetamodel metamodel) throws  BadRequestException {

        return validateAndUpdateNode(
                id,
                metamodel,
                node -> node.getType() == NodeMetamodel.NodeType.TOOL &&
                        ((ToolNodeMetamodel) node).getToolType() == ToolNodeMetamodel.ToolType.VECTOR_DB
        );
    }

    /**
     * Update an existing REST Tool node metamodel
     */
    @PutMapping("/rest-tool/{id}")
    public ResponseEntity<NodeMetamodel> updateRestToolNodeMetamodel(
            @PathVariable String id,
            @Valid @RequestBody RestNodeMetamodel metamodel)  throws  BadRequestException {

        return validateAndUpdateNode(
                id,
                metamodel,
                node -> node.getType() == NodeMetamodel.NodeType.TOOL &&
                        ((ToolNodeMetamodel) node).getToolType() == ToolNodeMetamodel.ToolType.REST
        );
    }

    /**
     * Update an existing LLM node metamodel
     */
    @PutMapping("/llm/{id}")
    public ResponseEntity<NodeMetamodel> updateLlmNodeMetamodel(
            @PathVariable String id,
            @Valid @RequestBody LlmNodeMetamodel metamodel)  throws  BadRequestException {

        return validateAndUpdateNode(
                id,
                metamodel,
                node -> node.getType() == NodeMetamodel.NodeType.AI &&
                        node instanceof AiNodeMetamodel &&
                        ((AiNodeMetamodel) node).getModelType() == AiNodeMetamodel.ModelType.LLM
        );
    }

    /**
     * Update an existing Embeddings node metamodel
     */
    @PutMapping("/embeddings/{id}")
    public ResponseEntity<NodeMetamodel> updateEmbeddingsNodeMetamodel(
            @PathVariable String id,
            @Valid @RequestBody EmbeddingsNodeMetamodel metamodel)  throws  BadRequestException {

        return validateAndUpdateNode(
                id,
                metamodel,
                node -> node.getType() == NodeMetamodel.NodeType.AI &&
                        node instanceof AiNodeMetamodel &&
                        ((AiNodeMetamodel) node).getModelType() == AiNodeMetamodel.ModelType.EMBEDDINGS
        );
    }

    /**
     * Update an existing Gateway node metamodel
     */
    @PutMapping("/gateway/{id}")
    public ResponseEntity<NodeMetamodel> updateGatewayNodeMetamodel(
            @PathVariable String id,
            @Valid @RequestBody GatewayNodeMetamodel metamodel)  throws  BadRequestException {

        return validateAndUpdateNode(
                id,
                metamodel,
                node -> node.getType() == NodeMetamodel.NodeType.FLOW &&
                        node instanceof GatewayNodeMetamodel &&
                        ((GatewayNodeMetamodel) node).getControlType() == GatewayNodeMetamodel.ControlType.GATEWAY
        );
    }

    /**
     * Helper method to validate and update node metamodels
     * @param familyId The ID of the node family to update
     * @param metamodel The new node metamodel data
     * @param validationPredicate Predicate to validate node type
     * @return ResponseEntity with the updated node or appropriate error status
     */
    private <T extends NodeMetamodel> ResponseEntity<NodeMetamodel> validateAndUpdateNode(
            String familyId,
            T metamodel,
            Predicate<NodeMetamodel> validationPredicate) throws BadRequestException {

        // Check if node exists
        Optional<NodeMetamodel> existing = nodeMetamodelService.getLatestVersionByFamilyId(familyId);
        if (existing.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        // Validate node type
        if (!validationPredicate.test(existing.get())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();


        // Check if the version is valid
        if (!Version.isValidVersionBump(existing.get().getVersion(), metamodel.getVersion())) {
            throw new BadRequestException(
                    String.format("Invalid version bump: the new version %s is not compatible with the existing version %s",
                            metamodel.getVersion(),
                            existing.get().getVersion())
            );
        }

        NodeMetamodel result = nodeMetamodelService.updateNode(familyId, metamodel);
        return ResponseEntity.ok(result);
    }
}
