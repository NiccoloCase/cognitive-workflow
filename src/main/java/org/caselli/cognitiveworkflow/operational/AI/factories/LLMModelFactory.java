package org.caselli.cognitiveworkflow.operational.AI.factories;

import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.logging.Logger;

/**
 * Factory for creating LLM models and chat clients.
 * @author niccolocaselli
 */
@Component
public class LLMModelFactory {


    @Value("${llm.openai.api-key:}")
    private String defaultOpenAiApiKey;

    @Value("${llm.anthropic.api-key:}")
    private String defaultAnthropicApiKey;

    Logger logger = Logger.getLogger(LLMModelFactory.class.getName());

    /**
     * Builds a configured OpenAI ChatModel instance.
     * @param apiKey The OpenAI API key
     * @param modelName The specific OpenAI model name
     * @param options Provider-specific options
     * @return A configured OpenAiChatModel instance.
     * @throws IllegalArgumentException if the API key is missing or options are of the wrong type.
     */
    public ChatModel buildOpenAiChatModel(String apiKey, String modelName, OpenAiChatOptions options) {
        if (!StringUtils.hasText(apiKey))
            throw new IllegalArgumentException("OpenAI API key is required.");

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();

        if (options != null) {
            optionsBuilder
                    .model(options.getModel())
                    .temperature(options.getTemperature())
                    .topP(options.getTopP())
                    .maxTokens(options.getMaxTokens())
                    .frequencyPenalty(options.getFrequencyPenalty())
                    .presencePenalty(options.getPresencePenalty());
        }

        if (StringUtils.hasText(modelName))
            optionsBuilder.model(modelName);

        var openaiOptions = optionsBuilder.build();

        var model = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().apiKey(apiKey).build())
                .defaultOptions(openaiOptions)
                .build();

        logger.info("OpenAI ChatModel created with model: " + modelName + " and options: temperature=" + openaiOptions.getTemperature() + ", topP=" + openaiOptions.getTopP() + ", topK=" + openaiOptions.getTopK() + ", maxTokens=" + openaiOptions.getMaxTokens());

        return model;
    }


    /**
     * Builds a configured Anthropic ChatModel instance.
     * @param apiKey The Anthropic API key
     * @param modelName The specific Anthropic model name
     * @param options Provider-specific options
     * @return A configured AnthropicChatModel instance
     * @throws IllegalArgumentException if the API key is missing or options are of the wrong type.
     */
    public ChatModel buildAnthropicChatModel(String apiKey, String modelName, AnthropicChatOptions options) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("Anthropic API key is required to build the model.");
        }

        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder();

        if (options != null) {
            optionsBuilder
                    .model(options.getModel())
                    .temperature(options.getTemperature())
                    .topP(options.getTopP())
                    .maxTokens(options.getMaxTokens())
                    .topK(options.getTopK())
                    .thinking(options.getThinking());
        }

        if (StringUtils.hasText(modelName))
            optionsBuilder.model(modelName);

        optionsBuilder.maxTokens((options != null ? options.getMaxTokens() : null) == null ? 1000 : options.getMaxTokens());

        var anthropicOptions = optionsBuilder.build();

        var model = AnthropicChatModel.builder()
                .anthropicApi(new AnthropicApi(apiKey))
                .defaultOptions(anthropicOptions)
                .build();

        logger.info("Anthropic ChatModel created with model: " + modelName + " and options: temperature=" + anthropicOptions.getTemperature() + ", topP=" + anthropicOptions.getTopP() + ", topK=" + anthropicOptions.getTopK() + ", maxTokens=" + anthropicOptions.getMaxTokens());

        return model;
    }

    /**
     * Builds a generic ChatModel based on the specified provider.
     * @param provider The LLM provider ("openai" or "anthropic").
     * @param apiKey The API key for the provider. Must not be null or empty.
     * @param modelName The specific model name. Can be null or empty.
     * @param options Provider-specific options object (e.g., OpenAiChatOptions, AnthropicChatOptions), can be null.
     * @return A configured ChatModel instance.
     * @throws IllegalArgumentException if the provider is unsupported, API key is missing, or options type is incorrect.
     */
    public ChatModel buildChatModel(String provider, String apiKey, String modelName, Object options) {
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("LLM provider is required.");
        }

        return switch (provider.toLowerCase()) {
            case "openai" -> {
                if (options != null && !(options instanceof OpenAiChatOptions))
                    throw new IllegalArgumentException("Options must be OpenAiChatOptions for provider 'openai'");

                yield buildOpenAiChatModel(apiKey, modelName, (OpenAiChatOptions) options);
            }
            case "anthropic" -> {
                if (options != null && !(options instanceof AnthropicChatOptions))
                    throw new IllegalArgumentException("Options must be AnthropicChatOptions for provider 'anthropic'");

                yield buildAnthropicChatModel(apiKey, modelName, (AnthropicChatOptions) options);
            }

            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        };
    }


    /**
     * Creates a ChatClient based on provider, model, Api Key and options.
     *
     * @param provider  The LLM provider.
     * @param modelName The specific model name.
     * @param apiKey    The API key (Optional). If null a default API Key will be used if existing
     * @param options   Provider-specific options object (Optional).
     * @return A ChatClient instance.
     * @throws IllegalArgumentException if inputs are invalid.
     */
    public ChatClient createChatClient(String provider, String modelName, String apiKey, LlmNodeMetamodel.LlmModelOptions options) {
        ChatModel chatModel = buildChatModel(provider, getApiKeyOrDefault(provider, apiKey), modelName, convertOptions(provider, options));
        return ChatClient.create(chatModel);
    }

    /**
     * Creates a ChatClient based on provider and model (Using default API key if it exists)
     *
     * @param provider  The LLM provider.
     * @param modelName The specific model name.
     * @throws IllegalArgumentException if inputs are invalid. (E.g., default api key not configure)
     */
    public ChatClient createChatClient(String provider, String modelName) {
        return createChatClient(provider, modelName, null, null);
    }

    /**
     * Creates a ChatClient from an already built ChatModel instance.
     * @param chatModel The ChatModel instance.
     * @return A ChatClient instance.
     * @throws IllegalArgumentException if chatModel is null.
     */
    public ChatClient createChatClient(ChatModel chatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
        return ChatClient.create(chatModel);
    }

    /**
     * Returns the provided API key if it is not null or blank; otherwise, returns the default API key
     * configured for the specified provider.
     *
     * <p>If no API key is provided and no default is configured for the given provider,
     * this method throws an {@link IllegalArgumentException}.
     *
     * @param provider the LLM provider name (e.g. "openai", "anthropic")
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
            case "anthropic" -> {
                if (!StringUtils.hasText(defaultAnthropicApiKey))
                    throw new IllegalArgumentException("Anthropic API key not provided and no default set.");
                yield defaultAnthropicApiKey;
            }
            default -> throw new IllegalArgumentException("Unsupported provider for default API key: " + provider);
        };
    }


    /**
     * Helper method to convert options from a generic BaseLlmModelOptions object
     * to a provider-specific options object.
     * @param provider The LLM provider ("openai" or "anthropic").
     * @param options The common options object to convert.
     * @return The converted provider-specific options object, or null if the input options are null.
     * @throws IllegalArgumentException if the provider is unsupported or option values have incorrect formats.
     */
    private Object convertOptions(String provider, LlmNodeMetamodel.LlmModelOptions options) {
        if (options == null) return null;

        return switch (provider.toLowerCase()) {
            case "openai" -> {
                OpenAiChatOptions.Builder builder = getOpenAiOptionsBuilder(options);
                yield builder.build();
            }
            case "anthropic" -> {
                AnthropicChatOptions.Builder builder = getAnthropicOptionsBuilder(options);
                yield builder.build();
            }
            default -> throw new IllegalArgumentException("Unsupported LLM provider for options conversion: " + provider);
        };
    }

    /**
     * Creates a builder for OpenAiChatOptions based on the provided options.
     * @param options The BaseLlmModelOptions object containing the options.
     * @return A builder for OpenAiChatOptions.
     */
    private static AnthropicChatOptions.Builder getAnthropicOptionsBuilder(LlmNodeMetamodel.LlmModelOptions options) {
        AnthropicChatOptions.Builder builder = AnthropicChatOptions.builder();
        if (options.getTemperature() != null) builder.temperature(options.getTemperature());
        if (options.getTopP() != null) builder.topP(options.getTopP());
        if (options.getMaxTokens() != null) builder.maxTokens(options.getMaxTokens());
        return builder;
    }

    /**
     * Creates a builder for OpenAiChatOptions based on the provided options.
     * @param options The LlmModelOptions object containing the options.
     * @return A builder for OpenAiChatOptions.
     */
    private static OpenAiChatOptions.Builder getOpenAiOptionsBuilder(LlmNodeMetamodel.LlmModelOptions options) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (options.getTemperature() != null) builder.temperature(options.getTemperature());
        if (options.getTopP() != null) builder.topP(options.getTopP());
        if (options.getMaxTokens() != null) builder.maxTokens(options.getMaxTokens());
        return builder;
    }

}