package org.caselli.cognitiveworkflow.knowledge.MOP;

import jakarta.annotation.PostConstruct;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.IntentMetamodelCatalog;
import org.caselli.cognitiveworkflow.operational.AI.services.EmbeddingService;
import org.slf4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing intent metamodels in the knowledge layer.
 * @author niccolocaselli
 */
@Service
public class IntentMetamodelService {

    Logger logger = org.slf4j.LoggerFactory.getLogger(IntentMetamodelService.class);

    private final IntentMetamodelCatalog repository;
    private final IntentSearchService intentSearchService;
    private final WorkflowMetamodelService workflowMetamodelService;

    private final EmbeddingService embeddingService;

    public IntentMetamodelService(IntentMetamodelCatalog repository, EmbeddingService embeddingService, IntentSearchService intentSearchService, WorkflowMetamodelService workflowMetamodelService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.intentSearchService = intentSearchService;
        this.workflowMetamodelService = workflowMetamodelService;
    }

    @PostConstruct
    public void init() {
        // For testing purposes only:
        //generateEmbeddingForAll();
    }

    /**
     * Find all intents in the catalog
     * @return List of intents
     */
    @Cacheable(value = "intentMetamodels")
    public List<IntentMetamodel> findAll() {
        return repository.findAll();
    }


    /**
     * Find an intent by its id
     * @param id Intent id
     * @return Optional of IntentMetamodel
     */
    @Cacheable(value = "intentMetamodels", key = "#id")
    public Optional<IntentMetamodel> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Create a new intent in the catalog
     * @param intent Intent to create
     * @return Created IntentMetamodel
     */
    @CacheEvict(value = "intentMetamodels", allEntries = true)
    public IntentMetamodel create(IntentMetamodel intent) {
        intent.setId(UUID.randomUUID().toString());  // Always ignore the ID provided by the user

        // Generate and set the embedding
        generateAndSetEmbedding(intent);

        return repository.save(intent);

    }

    /**
     * Update an existing intent in the catalog
     * @param intent Intent to update
     * @return Updated IntentMetamodel
     */
    @CacheEvict(value = "intentMetamodels", allEntries = true)
    public IntentMetamodel update(IntentMetamodel intent) {
        // Check it exists
        var existingIntent = repository.findById(intent.getId());
        if (existingIntent.isEmpty())
            throw new IllegalArgumentException("Intent with id " + intent.getId() + " does not exist");

        // Generate and set the embedding
        generateAndSetEmbedding(intent);

        return repository.save(intent);
    }

    /**
     * Delete an intent from the catalog
     * @param id Intent id
     * @return true if the intent was deleted, false otherwise
     */
    @Cacheable(value = "intentMetamodels", key = "#id")
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    /**
     * Delete an intent from the catalog
     * @param id Id of the intent to update
     */
    @CacheEvict(value = "intentMetamodels", allEntries = true)
    public void deleteById(String id) {
        // Remove the intent from all workflows
        workflowMetamodelService.removeIntentFromAllWorkflows(id);

        // Delete the intent from the repository
        repository.deleteById(id);
    }


    /**
     * Private helper method to generate embedding for an intent
     * and set it on the intent object.
     * @param intent The intent object to generate embedding for.
     */
    private void generateAndSetEmbedding(IntentMetamodel intent) {
        String textToEmbed = intent.getName() + " " + intent.getDescription();

        // Generate the embedding using the embedding service
        List<Double> embedding = embeddingService.generateEmbedding(textToEmbed);

        // Set the generated embedding on the intent object
        intent.setEmbedding(embedding);
    }


    /**
     * Get most similar intents to a given input
     */
    public List<IntentMetamodel> findMostSimilarIntent(String input) {
        return intentSearchService.findMostSimilarIntent(input);
    }

    /**
     * Generate embedding for all intents in the catalog
     * For testing purposes only
     */
    private void generateEmbeddingForAll() {
        List<IntentMetamodel> intents = repository.findAll();
        for (IntentMetamodel intent : intents) {
            generateAndSetEmbedding(intent);
            update(intent);
        }
        this.logger.info("Generated embedding for {} intents", intents.size());
    }
}