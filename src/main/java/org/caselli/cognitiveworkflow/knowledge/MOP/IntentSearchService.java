package org.caselli.cognitiveworkflow.knowledge.MOP;

import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.operational.AI.services.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import org.bson.Document;

/**
 * Service responsible for searching for the most similar IntentMetamodel
 * based on user input embedding using MongoDB Atlas Vector Search.
 * @author niccolocaselli
 */
@Service
public class IntentSearchService {

    private static final Logger log = LoggerFactory.getLogger(IntentSearchService.class);

    private final MongoTemplate mongoTemplate;
    private final EmbeddingService embeddingService;


    private final String collectionName ="intents"; // The name of the IntentMetamodels collection
    private final String vectorSearchIndexName = "intent_vector_index";  // The name of the Atlas Vector Search index
    private final String vectorSearchFieldName = "embedding"; // The field name of embedding in the IntentMetamodel
    private final int numCandidates = 100; // Number of documents to scan per shard
    private final int limit = 10; // Number of results to return

    @Autowired
    public IntentSearchService(MongoTemplate mongoTemplate, EmbeddingService embeddingService) {
        this.mongoTemplate = mongoTemplate;
        this.embeddingService = embeddingService;
    }

    /**
     * Finds the most similar IntentMetamodels based on the user's input text.
     * Uses MongoDB Atlas Vector Search to perform similarity search on intent embeddings.
     *
     * @param userInput The text input from the user.
     * @return An Optional containing the best matching IntentMetamodel if found above the threshold,
     * otherwise an empty Optional.
     */
    public List<IntentMetamodel> findMostSimilarIntent(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            log.warn("Received empty or null user input for intent search.");
            return Collections.emptyList();
        }

        try {
            // Generate embedding for the user input
            List<Double> userInputEmbedding = embeddingService.generateEmbedding(userInput);

            if (userInputEmbedding == null || userInputEmbedding.isEmpty()) {
                log.error("Failed to generate embedding for user input.");
                return Collections.emptyList();
            }

            // Create the aggregation pipeline for vector search
            Document vectorSearch = new Document("$vectorSearch", new Document("queryVector", userInputEmbedding)
                    .append("path", vectorSearchFieldName)
                    .append("numCandidates", numCandidates)
                    .append("limit", limit)
                    .append("index", vectorSearchIndexName)
            );

            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.stage(Document.parse(vectorSearch.toJson()))
            );

            AggregationResults<IntentMetamodel> results = mongoTemplate.aggregate(
                    aggregation,
                    collectionName,
                    IntentMetamodel.class
            );

            return results.getMappedResults();
        }
        catch (Exception e) {
            log.error("Error during intent search: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
