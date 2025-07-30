package org.caselli.cognitiveworkflow.operational.AI.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.MOP.IntentMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.caselli.cognitiveworkflow.operational.AI.LLMAbstractService;
import org.caselli.cognitiveworkflow.operational.AI.factories.LLMModelFactory;
import org.caselli.cognitiveworkflow.operational.observability.IntentDetectionObservabilityReport;
import org.caselli.cognitiveworkflow.operational.observability.ResultWithObservability;
import org.caselli.cognitiveworkflow.operational.observability.TokenUsage;
import org.caselli.cognitiveworkflow.operational.utils.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Intent Detector Service
 * @author niccolocaselli
 */
@Service
public class IntentDetectionService extends LLMAbstractService {

    private final LLMModelFactory llmModelFactory;

    @Value("${intent-detector.llm.provider}")
    private String intentProvider;

    @Value("${intent-detector.llm.api-key}")
    private String intentApiKey;

    @Value("${intent-detector.llm.model}")
    private String intentModel;

    @Value("${intent-detector.llm.temperature}")
    private double temperature;

    private final IntentMetamodelService intentMetamodelService;

    public IntentDetectionService(LLMModelFactory llmModelFactory, IntentMetamodelService intentMetamodelService) {
        this.llmModelFactory = llmModelFactory;
        this.intentMetamodelService = intentMetamodelService;
    }

    /**
     * Analyzes user input to determine intent using an LLM configured for this task.
     * @param userInput The text input from the user.
     * @return The determined intent as an IntentDetectorResult.
     *         <ul>
     *             <li>Return a predefined intent if a match is found.</li>
     *             <li>Return an invented intent if no match is found.</li>
     *             <li>Return null if intent is not clear.</li>
     *         </ul>
     */
    public ResultWithObservability<IntentDetectionResponse.IntentDetectorResult> detect(String userInput) {
        logger.info("Detecting intent for user input: {}...", userInput);

        IntentDetectionObservabilityReport observabilityReport = new IntentDetectionObservabilityReport(userInput);

        // Search top-matching intents
        var intents = intentMetamodelService.findMostSimilarIntent(userInput);

        String intentsOutput = intents.stream()
                .map(intent -> String.format("- Intent ID: %s, Intent Name: %s, Description: %s",
                        intent.getId(), intent.getName(), intent.getDescription()))
                .collect(Collectors.joining("\n"));


        observabilityReport.setSimilarIntents(intents);


        if(intents.isEmpty()) logger.info("No similar intents found");
        else logger.info("Found similar intents: " + intentsOutput);

        // Create system prompt using template
        Map<String, Object> model = Map.of("availableIntents", intentsOutput);
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_INSTRUCTIONS_TEMPLATE);
        SystemMessage systemMessage = new SystemMessage(systemPromptTemplate.create(model).getContents());




        // User message
        UserMessage userMessage = new UserMessage(userInput);

        // Construct prompt
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        logger.debug("Prompt: {}", prompt.getContents());



        // Call the LLM
        ResponseEntity<ChatResponse, IntentDetectionResponse> response = getChatClient().prompt(prompt)
                .call()
                .responseEntity(IntentDetectionResponse.class);

        Usage usage = response.getResponse().getMetadata().getUsage();
        if (usage != null) observabilityReport.setTokenUsage(
                new TokenUsage(usage.getCompletionTokens(), usage.getPromptTokens(), usage.getTotalTokens())
        );

        IntentDetectionResponse modelAnswer = response.getEntity();


        // Determine if the intent is non-existent or there has been an error
        if (modelAnswer == null || modelAnswer.getData() == null || (modelAnswer.getError() != null && !modelAnswer.getError().isEmpty()) ) {
            logger.error("Error in intent detection: {}", modelAnswer != null ? modelAnswer.getError() : "Unknown error");

            observabilityReport.markCompleted(false,  modelAnswer.getError(), null );

            return new ResultWithObservability<>(null, observabilityReport);
        }

        var result = modelAnswer.getData();

        // Post-process: determine isNew flag based on intentId presence
        boolean isNew = intents.stream()
                .noneMatch(intent -> intent.getId().equals(result.getIntentId()) || intent.getName().equals(result.getIntentName()));


        if(isNew){
            result.setIntentId(null);
            result.setNew(true);

            // Format the invented intent name to be in ALL_UPPERCASE_WITH_UNDERSCORES format
            String formattedName = StringUtils.toUppercaseSnakeCase(result.getIntentName());
            result.setIntentName(formattedName);

        } else {
            result.setNew(false);

            // Perform a final check to ensure the intentId is real
            var intent = intents.stream()
                    .filter(i -> i.getId().equals(result.getIntentId()))
                    .findFirst();

            if (intent.isEmpty()) {

                // If the intentId is not found in the list of intents
                // this means that in the previous step the intent was found by name not by id
                // so we need to set the intentId to the real intentId
                result.setIntentId(intents.stream()
                        .filter(i -> i.getName().equals(result.getIntentName()))
                        .findFirst()
                        .map(IntentMetamodel::getId)
                        .orElse(null));
            }
        }

        // Format all the user variables to be in ALL_UPPERCASE_WITH_UNDERSCORES format
        var originalVariables = result.getUserVariables();
        Map<String, Object> newVariables = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : originalVariables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String formattedKey = StringUtils.toUppercaseSnakeCase(key);
            if (!formattedKey.equals(key)) newVariables.put(formattedKey, value);
            else newVariables.put(key, value);

        }
        result.setUserVariables(newVariables);

        // Observability
        observabilityReport.setIntentDetectorResult(result);
        observabilityReport.markCompleted(true,  null, null );


        return new ResultWithObservability<>(result, observabilityReport);
    }

    @Override
    protected ChatClient buildChatClient() {
        var options = new LlmNodeMetamodel.LlmModelOptions();
        options.setTemperature(temperature);
        return llmModelFactory.createChatClient(intentProvider, intentModel, intentApiKey, options);
    }



    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IntentDetectionResponse {
        @JsonProperty("data")
        private IntentDetectorResult data;

        @JsonProperty("error")
        private String error;

        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class IntentDetectorResult {
            @JsonProperty(required = true)
            private String intentName;

            @JsonProperty
            private String intentId;

            @JsonProperty(required = true)
            private double confidence;

            @JsonProperty(required = true)
            private boolean isNew;

            @JsonProperty(required = true)
            private Map<String, Object> userVariables;
        }
    }

    private static final String SYSTEM_INSTRUCTIONS_TEMPLATE =
            """
                    You are a highly accurate Intent Detection System.
                    Your job is to determine a user's intent from their input and return it in structured JSON format.
    
                    ---
    
                    ## AVAILABLE INTENTS:
                    {availableIntents}
    
                    ---
    
                    ## STEP-BY-STEP INSTRUCTIONS:
    
                    1. **Understand the User Input** \s
                       Carefully read and comprehend the user's input. Break it down into any requests, questions, or tasks.
    
                    2. **Check if the Input is Truly Nonsensical or Not a Request** \s
                        Determine if the input is entirely meaningless, gibberish, or clearly not a request or command directed at you. \s
                           -  If YES (it is nonsensical/not a request), return the JSON error format.
                           -  If NO (it is a meaningful request), continue to the next step.
    
                    3. **Identify the Core Intent** \s
                        What is the user trying to do? Be specific. \s
                        Example: "I want to translate this text to Spanish" → The user wants to *translate text*.
    
                    4. **Match or Propose an Intent**\s
                        Compare the identified core intention against the descriptions of the “AVAILABLE INTENTS. Aim for a clear and confident match to an existing intent whenever the user's request aligns well with one.
                       - If there’s a clear, confident match to an AVAILABLE INTENT, use that intent's ID and Name.
                       - If there is NO good match among the AVAILABLE INTENTS, but the input is a valid request, you MUST propose a NEW intent. The proposed intent name must be descriptive and in UPPERCASE_WITH_UNDERSCORES format (e.g., TRANSLATE_TEXT, SEND_EMAIL, GET_WEATHER).
    
                    5. **Extract Variables** \s
                       Identify important variables needed to fulfill the request. Use descriptive UPPERCASE_WITH_UNDERSCORES names for variables. \s
                       - Example for "translate this text to Spanish": `TARGET_LANGUAGE: "Spanish"`, `SOURCE_TEXT: "this text"` (if the text was provided directly).
                       - Example for "I want to buy a pizza for this evening at 8pm": `FOOD_ITEM: "pizza"`, `TIME: "this evening at 8pm"`.
    
                    6. **Assign a Confidence Score** \s
                       Reflect how confident you are in the determined intent (0.0 for very unsure, 1.0 for very sure).
    
                    ---
                    
                    ## Remember:
                    - If you don't find a strong match in the Available Intents, you MUST propose a new intent with a descriptive name.
                    - *Always prioritize* matching to EXISTING intents when appropriate.
                    - Think step by step before giving the final answer. Only output the final JSON.
            """;
}