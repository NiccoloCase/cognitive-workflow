package org.caselli.cognitiveworkflow.operational.AI.services;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.operational.AI.LLMAbstractService;
import org.caselli.cognitiveworkflow.operational.AI.factories.LLMModelFactory;
import org.caselli.cognitiveworkflow.operational.observability.TokenUsage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for managing port adaptation using LLM.
 * Maps source ports to target ports using LLM.
 * @author niccolocaselli
 */
@Service
public class PortAdapterService extends LLMAbstractService {

    private final LLMModelFactory llmModelFactory;

    @Value("${port-adapter.llm.provider:}")
    private String provider;

    @Value("${port-adapter.llm.api-key:}")
    private String apiKey;

    @Value("${port-adapter.llm.model:}")
    private String model;

    @Value("${port-adapter.llm.temperature}")
    private double temperature;


    private static final String SYSTEM_INSTRUCTIONS =
            """
            You are a Port Adapter Analysis System.
            Your task is to analyze source and target ports and generate attribute mappings based on the provided rules.
            The user will provide you with a list of source ports and a list of target ports.
           
            ## Rules:
            - Use direct paths (e.g., "A.B" not "A.schema.properties.B")
            - Prioritize required attributes
            - Return {"bindings": {}} if no mapping needed
            - Return null if mapping impossible
                        
            ## Output Guidelines:
            You must respond with valid JSON in this format:
            ```json
            {
              "bindings": {
                "SourcePort.orderId": "TargetPort.id",
                "SourcePort.customer.email": "TargetPort.contact.email"
              }
            }
            ```
            """;

    public PortAdapterService(LLMModelFactory llmModelFactory) {
        this.llmModelFactory = llmModelFactory;
    }


    /**
     * Adapts source ports to target ports using LLM
     * @param sourcePorts List of source ports
     * @param targetPorts List of target ports
     * @return PortAdaptation object containing the mapping.
     *         If no mapping is needed, it returns an empty map.
     *         If mapping is impossible, it returns null.
     */
    public PortAdaptation adaptPorts(List<Port> sourcePorts, List<Port> targetPorts) {
        logger.info("Starting port adaptation with LLM...");

        Objects.requireNonNull(sourcePorts, "Source ports list cannot be null");
        Objects.requireNonNull(targetPorts, "Target ports list cannot be null");

        if (sourcePorts.isEmpty()) {
            logger.warn("Source ports list is empty. No adaptation will be performed.");
            PortAdaptation emptyResult = new PortAdaptation();
            emptyResult.setBindings(Collections.emptyMap());
            return emptyResult;
        }
        if (targetPorts.isEmpty()) {
            logger.warn("Target ports list is empty. No adaptation will be performed.");
            PortAdaptation emptyResult = new PortAdaptation();
            emptyResult.setBindings(Collections.emptyMap());
            return emptyResult;
        }

        // Validate that all ports and their schemas in the input lists are not null
        sourcePorts.forEach(p -> {
            Objects.requireNonNull(p, "Null Port found in sourcePorts list");
            Objects.requireNonNull(p.getKey(), "Port key cannot be null for Source Port");
            Objects.requireNonNull(p.getSchema(), "Source port schema cannot be null for port with key " + p.getKey());
        });
        targetPorts.forEach(p -> {
            Objects.requireNonNull(p, "Null Port found in targetPorts list");
            Objects.requireNonNull(p.getKey(), "Port key cannot be null for Target Port");
            Objects.requireNonNull(p.getSchema(), "Target port schema cannot be null for port with key " + p.getKey());
        });

        if (!StringUtils.hasText(provider) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(model))
            throw new IllegalArgumentException("LLM configuration (adapter.llm.provider, adapter.llm.api-key, adapter.llm.model) is missing or incomplete for port adaptation.");

        try {

            String sourcePortsDescription = sourcePorts.stream()
                    .map(Port::portToJson)
                    // TODO: note that for now we escape the braces in the JSON string
                    // As reported by others this is a workaround for a bug in Spring AI
                    // In fact, without escaping the braces, the template engine tries to find variables in the JSON
                    // See https://github.com/spring-projects/spring-ai/issues/2836
                    .map(s -> s.replace("{", "\\{").replace("}", "\\}"))  // Escape braces
                    .collect(Collectors.joining(",\n"));
            sourcePortsDescription = "```json\n[" + sourcePortsDescription + "]\n```";

            String targetPortsDescription = targetPorts.stream()
                    .map(Port::portToJson)
                    .map(s -> s.replace("{", "\\{").replace("}", "\\}"))  // Escape braces
                    .collect(Collectors.joining(",\n"));
            targetPortsDescription = "```json\n[" + targetPortsDescription + "]\n```";

            String userContent = "Source Ports:\n" + sourcePortsDescription + "\n\nTarget Ports:\n" + targetPortsDescription;

            Prompt prompt = new Prompt(List.of(new SystemMessage(SYSTEM_INSTRUCTIONS), new UserMessage(userContent)));

            // Call the LLM
            ResponseEntity<ChatResponse, PortAdaptationLLMResult> response = getChatClient().prompt(prompt)
                    .call()
                    .responseEntity(PortAdaptationLLMResult.class);

            //  Structured output handling
            PortAdaptationLLMResult adaptationResult = response.getEntity();

            // Init a PortAdaptation object to hold the result
            PortAdaptation portAdaptation = new PortAdaptation();
            portAdaptation.setBindings(Collections.emptyMap());

            // Put the token usage in the PortAdaptation result
            portAdaptation.setTokenUsage(new TokenUsage(response.getResponse().getMetadata().getUsage()));

            if (adaptationResult != null && adaptationResult.getBindings() != null) {

                logger.info("LLM response mapping to PortAdaptation: " + adaptationResult);
                Map<String,String> adapterPorts = adaptationResult.getBindings();
                for (String key : adapterPorts.keySet()) logger.info(key + " -> " + adapterPorts.get(key));

                portAdaptation.setBindings(adaptationResult.getBindings());
                return portAdaptation;


            } else {
                if (adaptationResult == null) {
                    logger.warn("LLM response mapping to PortAdaptation failed");
                    return null;
                } else {
                    logger.warn("No mapping needed. Returning empty result.");
                    return portAdaptation;
                }
            }

        } catch (Exception e) {
            logger.error("Error during ports list adaptation with LLM structured output: " + e.getMessage(), e);
            return null;
        }
    }


    @Override
    protected ChatClient buildChatClient() {
        var options = new LlmNodeMetamodel.LlmModelOptions();
        options.setTemperature(temperature);
        return llmModelFactory.createChatClient(provider, model, apiKey, options);
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class PortAdaptation extends  PortAdaptationLLMResult  {
        private TokenUsage tokenUsage;
    }

    /**
     * Class representing the adaptation of ports.
     * It contains a map of bindings that represent the adaptation between source and target ports.
     * Use dot notation for nested attributes (e.g. "A.B").
     */
    @Data
    public static class PortAdaptationLLMResult {

        private Map<String, String> bindings;

        @Override
        public String toString() {
            return "PortAdaptation{" +
                    "binding=" + bindings +
                    '}';
        }
    }
}