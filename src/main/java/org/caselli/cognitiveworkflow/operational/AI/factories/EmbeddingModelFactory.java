package org.caselli.cognitiveworkflow.operational.AI.factories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Factory for creating embedding models.
 * @author niccolocaselli
 */
@Component
public class EmbeddingModelFactory {

    @Value("${llm.openai.api-key:}")
    private String defaultOpenAiApiKey;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Creates an EmbeddingModel based on the specified provider.
     * @param provider The provider name (e.g., "openai", "vertex").
     * @param modelName The Embeddings Model
     * @param apiKey The API key (Optional). If null a default API Key will be used if existing
     * @return An instance of EmbeddingModel.
     */
    public EmbeddingModel createEmbeddingModel(String provider, String modelName, String apiKey) {
        return switch (provider.toLowerCase()) {
            case "openai" -> createOpenAiEmbeddingModel(getApiKeyOrDefault(provider, apiKey), modelName);
            case "vertex" -> createVertexEmbeddingModel(getApiKeyOrDefault(provider, apiKey), modelName);
            default -> throw new IllegalStateException("Unexpected value: " + provider.toLowerCase());
        };
    }

    /**
     * Creates an EmbeddingModel based on the specified provider. Using Default API key
     * @param provider The provider name (e.g., "openai", "vertex").
     * @param modelName The Embeddings Model
     * @return An instance of EmbeddingModel.
     */
    public EmbeddingModel createEmbeddingModel(String provider, String modelName) {
        return createEmbeddingModel(provider, modelName, null);
    }



    /**
     * Builds a configured OpenAI EmbeddingModel instance.
     * @param apiKey The OpenAI API key
     * @param modelName The specific OpenAI model name
     * @throws IllegalArgumentException if the API key is missing
     */
    private EmbeddingModel createOpenAiEmbeddingModel(String apiKey, String modelName) {
        if (!StringUtils.hasText(apiKey)) throw new IllegalArgumentException("OpenAI API key is required");
        if (!StringUtils.hasText(modelName)) throw new IllegalArgumentException("Model name is required");

        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(apiKey).build();

        var openAiEmbeddingModel = new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(modelName).build(),
                RetryUtils.DEFAULT_RETRY_TEMPLATE
        );

        logger.info("Created OpenAI Embedding Model with model name: '{}', metadata mode: {}", modelName, MetadataMode.EMBED);

        return openAiEmbeddingModel;
    }

    /**
     * Builds a configured Vertex EmbeddingModel instance.
     * @param apiKey The Vertex API key
     * @param modelName The specific Vertex model name
     * @throws IllegalArgumentException if the API key is missing
     */
    private EmbeddingModel createVertexEmbeddingModel(String apiKey, String modelName) {
        if (!StringUtils.hasText(apiKey)) throw new IllegalArgumentException("OpenAI API key is required");
        if (!StringUtils.hasText(modelName)) throw new IllegalArgumentException("Model name is required");
        // TODO: Implement Vertex Embedding Model creation
        throw new UnsupportedOperationException("Vertex Embedding Model is not yet implemented.");
    }

    /**
     * Returns the provided API key if it is not null or blank; otherwise, returns the default API key
     * configured for the specified provider.
     *
     * <p>If no API key is provided and no default is configured for the given provider,
     * this method throws an {@link IllegalArgumentException}.
     *
     * @param provider the LLM provider name (e.g. "openai", "vertex" etc.)
     * @param apiKey the API key to use, or null/empty to fallback to the default
     * @return a non-null API key to use for the given provider
     * @throws IllegalArgumentException if the provider is unsupported or no API key is available
     */
    private String getApiKeyOrDefault(String provider, String apiKey) {
        if (StringUtils.hasText(apiKey)) return apiKey;

        return switch (provider.toLowerCase()) {
            case "openai" -> {
                if (!StringUtils.hasText(defaultOpenAiApiKey))
                    throw new IllegalArgumentException("OpenAI API key not provided and no default set.");
                yield defaultOpenAiApiKey;
            }
            // TODO: add vertex
            case "vertex" -> throw new IllegalArgumentException("Vertex provider not supported for default API key");
            default -> throw new IllegalArgumentException("Unsupported provider for default API key: " + provider);
        };
    }

}