package org.caselli.cognitiveworkflow.operational.AI.services;

import org.caselli.cognitiveworkflow.operational.AI.factories.EmbeddingModelFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * Service responsible for generating embedding vectors.
 * @author niccolocaselli
 */
@Service
public class EmbeddingService {

    @Value("${embedding.provider}")
    private String embeddingProvider;

    @Value("${embedding.api-key}")
    private String embeddingApiKey;

    @Value("${embedding.model}")
    private String embeddingModelName;

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private EmbeddingModel embeddingModel;

    private final EmbeddingModelFactory embeddingModelFactory;

    @Autowired
    public EmbeddingService(EmbeddingModelFactory embeddingModelFactory) {
        this.embeddingModelFactory = embeddingModelFactory;
    }

    /**
     * Generates an embedding vector for the given text
     *
     * @param text The input text to embed.
     * @return A List of Doubles representing the embedding vector.
     * @throws EmbeddingGenerationException if an error occurs during embedding generation.
     */
    public List<Double> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Attempted to generate embedding for empty or null text.");
            return new ArrayList<>();
        }

        log.debug("Generating embedding for text using Spring AI: {}", text);

        try {
            float[] embedding = getEmbeddingModel().embed(text);

            if (embedding != null ) {
                List<Double> embeddingList = new ArrayList<>(embedding.length);
                for (float value : embedding) embeddingList.add((double) value);
                return embeddingList;
            } else {
                throw new EmbeddingGenerationException("Spring AI EmbeddingModel returned null or empty output.");
            }
        } catch (Exception e) {
            log.error("Error generating embedding for text using Spring AI: {}", text, e);
            throw new EmbeddingGenerationException("Failed to generate embedding for text: " + text, e);
        }
    }


    /**
     * Initializes the embedding model after the properties are set.
     */
    private EmbeddingModel getEmbeddingModel() {
       if(this.embeddingModel != null) return embeddingModel;

       if(embeddingProvider == null || embeddingApiKey == null || embeddingModelName == null)
           throw new IllegalStateException("Embedding provider, model name and API key must be set before using the service.");


        this.embeddingModel = embeddingModelFactory.createEmbeddingModel(embeddingProvider, embeddingModelName, embeddingApiKey);
       return embeddingModel;
    }


    /**
     * Custom exception class for handling embedding generation errors.
     */
    public static class EmbeddingGenerationException extends RuntimeException {
        public EmbeddingGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
        public EmbeddingGenerationException(String message) {
            super(message);
        }
    }
}
