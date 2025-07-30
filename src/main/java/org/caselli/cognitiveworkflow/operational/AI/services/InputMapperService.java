package org.caselli.cognitiveworkflow.operational.AI.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.AI.LLMAbstractService;
import org.caselli.cognitiveworkflow.operational.AI.factories.LLMModelFactory;
import org.caselli.cognitiveworkflow.operational.observability.InputMapperObservabilityReport;
import org.caselli.cognitiveworkflow.operational.observability.ResultWithObservability;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LLM-based service for mapping input variables to workflow nodes.
 * This service analyzes unstructured variables and determines the most suitable starting node
 * in a workflow, mapping the variables to the node input structure.
 * @author niccolocaselli
 */
@Service
public class InputMapperService extends LLMAbstractService {
    private final LLMModelFactory llmModelFactory;

    @Value("${input-mapper.llm.provider:}")
    private String provider;

    @Value("${input-mapper.llm.api-key:}")
    private String apiKey;

    @Value("${input-mapper.llm.model:}")
    private String model;

    @Value("${port-adapter.llm.temperature}")
    private double temperature;

    public InputMapperService(LLMModelFactory llmModelFactory) {
        this.llmModelFactory = llmModelFactory;
    }


    public ResultWithObservability<InputMapperResult>  mapInput(Map<String, Object> variables, List<NodeMetamodel> nodes) {
        return mapInput(variables, nodes, null);
    }


    /**
     * Maps input variables to the input ports of the workflow starting nodes using an LLM.
     *
     * @param variables    Map of unstructured variables
     * @param nodes        List of starting workflow nodes
     * @param requestInput Request of the user in natural language
     * @return InputMapperResult containing the found variable-to-port bindings,
     * or null if no suitable mapping could be determined
     */
    public ResultWithObservability<InputMapperResult> mapInput(Map<String, Object> variables, List<NodeMetamodel> nodes, String requestInput) {
        logger.info("Starting input mapping with LLM for {} variables and {} nodes", variables.size(), nodes.size());

        InputMapperObservabilityReport observabilityReport = new InputMapperObservabilityReport(variables, nodes, requestInput);
        ResultWithObservability<InputMapperResult> resultWithObservability = new ResultWithObservability<>(new InputMapperResult(new ExecutionContext()), observabilityReport);



        if (nodes.isEmpty()) {
            logger.warn("No nodes available for mapping");
            observabilityReport.markCompleted(false, "No nodes available for mapping", null);
            return null;
        }

        if (variables.isEmpty() && (requestInput == null || requestInput.isEmpty())) {
            logger.info("No variables provided, skipping mapping");
            observabilityReport.markCompleted(false, "No variables provided, skipping mapping", null);
            return resultWithObservability;
        }


        try {
            String nodesDescription = buildNodesDescription(nodes);
            String variablesDescription = buildVariablesDescription(variables);
            String requestDescription = "<request_text>" + requestInput + "</request_text>";

            // Build the prompt
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM_INSTRUCTIONS),
                    new UserMessage(variablesDescription + "\n\n" + nodesDescription + "\n\n" + requestDescription)
            ));

            // Call the LLM
            ResponseEntity<ChatResponse, InputMapperLLMResult> response = getChatClient().prompt(prompt)
                    .call()
                    .responseEntity(InputMapperLLMResult.class);

            Usage usage = response.getResponse().getMetadata().getUsage();
            if (usage != null) observabilityReport.setTokenUsage(
                    new TokenUsage(usage.getCompletionTokens(), usage.getPromptTokens(), usage.getTotalTokens())
            );

            InputMapperLLMResult result = response.getEntity();

            logger.info("LLM returned {}", result != null ? result.getBindings() : "null");

            var res =  processLLMResult(result, nodes);
            resultWithObservability.setResult(res);

            observabilityReport.setInputMapperResult(res);
            observabilityReport.markCompleted(true, null, null);

            return resultWithObservability;
        } catch (Exception e) {
            logger.error("Input mapping failed: {}", e.getMessage(), e);

            observabilityReport.markCompleted(false, e.getMessage(), e);

            return null;
        }
    }

    private String buildNodesDescription(List<NodeMetamodel> nodes) {
        StringBuilder builder = new StringBuilder("<nodes_list>\n");

        nodes.forEach(node -> {

            if(node.getInputPorts() != null && !node.getInputPorts().isEmpty()){

                String portsDescription = node.getInputPorts().stream()
                        .map(Port::portToJson)
                        .map(s -> s.replace("{", "\\{").replace("}", "\\}"))
                        .collect(Collectors.joining(",\n    "));

                builder.append("- Node ID: ").append(node.getId()).append("\n")
                        .append("- Name: ").append(node.getName()).append("\n")
                        .append("- Description: ").append(node.getDescription()).append("\n")
                        .append("  Input Ports:\n")
                        .append("  ```json\n")
                        .append("  [\n    ").append(portsDescription).append("\n  ]\n")
                        .append("  ```\n\n");
            }

        });

        return builder.append("</nodes_list>").toString();
    }

    private String buildVariablesDescription(Map<String, Object> variables) {
        StringBuilder builder = new StringBuilder("<user_variables>\n");
        variables.forEach((key, value) ->
                builder.append("- ").append(key).append(": ").append(value).append("\n")
        );
        return builder.append("</user_variables>").toString();
    }

    private InputMapperResult processLLMResult(InputMapperLLMResult llmResult, List<NodeMetamodel> nodes) {
        if (llmResult == null || llmResult.getBindings() == null || llmResult.getBindings().isEmpty()) {
            logger.warn("LLM returned null result");

            // check if the input ports of all the initial nodes are satisfied (i.e., no required ports)
            if (nodes.stream().allMatch(node -> node.getInputPorts().stream().noneMatch(port -> port.getSchema().getRequired() != null && port.getSchema().getRequired()))) {
                return new InputMapperResult(new ExecutionContext());
            }

            logger.warn("No suitable variable-to-port can be determined for the provided nodes and variables");

            return null;
        }

        // Create the context
        ExecutionContext context = new ExecutionContext();
        if (llmResult.getBindings() != null) context.putAll(llmResult.getBindings());

        // Final check to see if the LLM response is valid
        if (!isGeneratedContextValid(nodes, context)) {
            logger.error("The LLM provided an input that do not satisfy the required ports of all the initial nodes");
            return null;
        }

        return new InputMapperResult(context);
    }

    private void validateLlmConfiguration() {
        if (!StringUtils.hasText(provider) ||
                !StringUtils.hasText(apiKey) ||
                !StringUtils.hasText(model)) {
            throw new IllegalArgumentException("LLM configuration (provider, api-key, model) is missing or incomplete"
            );
        }
    }

    @Override
    protected ChatClient buildChatClient() {
        validateLlmConfiguration();

        var options = new LlmNodeMetamodel.LlmModelOptions();
        options.setTemperature(temperature);
        return llmModelFactory.createChatClient(provider, model, apiKey, options);
    }


    /**
     * Validates that all required ports of the nodes are satisfied by the generated bindings
     *
     * @param nodes   List of nodes to validate against
     * @param context ExecutionContext containing the generated bindings
     */
    private boolean isGeneratedContextValid(List<NodeMetamodel> nodes, ExecutionContext context) {
        if (context == null) return false;

        for (var node : nodes) {
            for (var port : node.getInputPorts())
                if (
                        port.getSchema().getRequired() != null &&
                                port.getSchema().getRequired() &&
                                !port.getSchema().isValidValue(context.get(port.getKey()))
                )
                    return false;
        }

        return true;
    }

    /**
     * Helper class for structured LLM calls for Input Mapping
     */
    @Data
    private static class InputMapperLLMResult {
        @JsonProperty(required = true)
        Map<String, String> bindings;
    }

    /**
     * InputMapperService response
     */
    @Data
    @AllArgsConstructor
    public static class InputMapperResult {
        ExecutionContext context;
    }

    private static final String SYSTEM_INSTRUCTIONS =
            """
                    # MISSION
                    You are a **Data Population System**. Your goal is to generate a JSON object that maps values from user inputs to the input ports of workflow nodes.
                        
                    ---
                        
                    # DATA SOURCES
                    - `<user_variables>`: Key-value pairs.
                    - `<nodes_list>`: Workflow nodes with their input port schemas.
                    - `<request_text>`: The user's original plain text request.
                        
                    ---
                        
                    # CORE DIRECTIVE
                    1.  **Extract** all relevant data from `<user_variables>` and `<request_text>`.
                    2.  **Map** the extracted data to the corresponding input ports defined in `<nodes_list>`.
                    3.  **Generate** a key-value mappings (bindings) in dot notation according *MAPPING SYNTAX*
                        
                    ---
                        
                    # RULES & CONSTRAINTS
                        
                    ## YOU MUST:
                    - **Prioritize Required Ports**: Ensure all mandatory ports are populated first.
                    - **Use Source Data ONLY**: Bind values exclusively from the provided data sources.
                    - **Perform Safe Transformations**:
                        - **Convert Types**: Safely cast data to match the port's required `primitiveType` (string, number, boolean).
                        - **Manipulate Strings**: Extract substrings or parse values from text.
                        - **Convert Units**: Use common knowledge for conversions (e.g., kg to lbs, EUR to USD) only when rates are not provided.
                        - **Structure Data**: Organize flat data into the required `parent.child` or `array.index` format.
                        
                    ## YOU MUST NOT:
                    - **Invent or Hallucinate Data**: If a value is missing, **DO NOT** create it. Leave the port unbound.
                    - **Use Placeholders**: Do not use "N/A", "TBD", or any default values.
                    - **Extrapolate Information**: Do not infer data that isn't explicitly present (e.g., creating an email from a name).
                    - **Generate Complex Values**: The final mapped values **MUST** be primitives (string, number, or boolean), not JSON objects or arrays.
                        
                    ---
                        
                    # MAPPING SYNTAX
                    - **Simple**: `"port_name": "value"`
                    - **Nested**: `"customer.address": "123 Main St"`
                    - **Array**: `"items.0": "Apple"`
                    - **Array of Object**: `"items.2.name": "Apple"`
                        
                    ---
                        
                    # OUTPUT FORMAT
                    Return a single JSON object with the key `bindings`. If no bindings are possible, return an empty `bindings` object.
                        
                    **Example:**
                    ```json
                    {
                      "bindings": {
                        "product_id": "SKU-12345",
                        "quantity": 2,
                        "is_priority_shipping": true,
                        "customer.name": "Jane Doe",
                        "line_items.0.item_name": "Laptop"
                      }
                    }
                    ```
                    """;
}