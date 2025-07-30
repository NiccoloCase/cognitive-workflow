package org.caselli.cognitiveworkflow.operational.AI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;
import java.util.Map;

/**
 * A utility class for handling structured output from LLM responses based on port schemas.
 * This class simplifies working with Spring AI's output converters for different port types.
 * @author niccolocaselli
 */
public class PortStructuredOutputConverter {

    private static final Logger logger = LoggerFactory.getLogger(PortStructuredOutputConverter.class);


    /**
     * Adds format instructions from the appropriate converter based on the port's schema type.
     * @param messages The list of messages to add the format instruction to
     * @param port The port containing the schema
     */
    public static void addFormatInstructions(List<Message> messages, Port port) {
        PortSchema schema = port.getSchema();
        PortType portType = schema.getType();

        // Custom system prompts
        messages.add(createSchemaInstructionMessage(port));

        // OBJECTS
        if (portType == PortType.OBJECT) {
            MapOutputConverter converter = new MapOutputConverter();
            messages.add(new SystemMessage(converter.getFormat()));
        } else if (portType == PortType.ARRAY) {
            // ARRAYS of OBJECTS
            if (schema.getItems().getType() == PortType.OBJECT) {
                StructuredOutputConverter<List<Map<String, Object>>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
                messages.add(new SystemMessage(converter.getFormat()));
            }
            // ARRAYS OF ARRAYS
            else if (schema.getItems().getType() == PortType.ARRAY) {
                StructuredOutputConverter<List<List<?>>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
                messages.add(new SystemMessage(converter.getFormat()));
            }
            // ARRAYS OF FLOATS
            else if (schema.getItems().getType() == PortType.FLOAT) {
                StructuredOutputConverter<List<Float>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
                messages.add(new SystemMessage(converter.getFormat()));
            }
            // ARRAYS OF ARRAYS
            else if (schema.getItems().getType() == PortType.INT) {
                StructuredOutputConverter<List<Integer>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
                messages.add(new SystemMessage(converter.getFormat()));
            }
            // ARRAYS of STRINGS
            else {
                StructuredOutputConverter<List<String>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
                messages.add(new SystemMessage(converter.getFormat()));
            }
        }
        // INT
        else if (portType == PortType.INT) {
            StructuredOutputConverter<Integer> converter = new BeanOutputConverter<>(Integer.class);
            messages.add(new SystemMessage(converter.getFormat()));
        }
        // FLOAT
        else if (portType == PortType.FLOAT) {
            StructuredOutputConverter<Float> converter = new BeanOutputConverter<>(Float.class);
            messages.add(new SystemMessage(converter.getFormat()));
        }
    }

    /**
     * Creates a system message that provides formatting instructions based on the port's schema.
     * @param port The port that defines the expected schema for structured output
     * @return A system message containing formatting guidelines for the output
     */
    private static Message createSchemaInstructionMessage(Port port) {
        PortSchema schema = port.getSchema();
        StringBuilder instructions = new StringBuilder();

        // Append custom system instructions only for non-primitive types or arrays of non-primitive types,
        // since Spring AI does not handle nested dynamic typed schemas reliably.
        // Standard Spring AI structured output instructions will be used afterward as a common base.
        if (!schema.isPrimitiveType() && (schema.getItems() == null || !schema.getItems().isPrimitiveType())) {

            // Add the full schema as a reference for formatting
            instructions.append("The output must follow the following schema:\n").append(schema.toJson()).append("\n\n");

            // Instructions for OBJECT type schemas
            if (schema.getType() == PortType.OBJECT) {
                instructions.append("""
            For each key, provide only its value (e.g., a float, int, string, nested object, or array).
            """);
            }

            // Instructions for ARRAY of OBJECTS
            else if (schema.getType() == PortType.ARRAY && schema.getItems().getType() == PortType.OBJECT) {
                instructions.append("""
            You should return a list of objects: each element of the outer list should be an object whose fields contain only values (e.g., a float, int, string, nested object, or array).
            """);
            }

            // Instructions for ARRAY of ARRAYS
            else if (schema.getType() == PortType.ARRAY && schema.getItems().getType() == PortType.ARRAY) {
                instructions.append("""
            You should return a list of lists: each element of the outer list should be another nested list.
            If the nested list contains objects, their fields should contain only values (e.g., a float, int, string, nested object, or array).
            """);
            }

            // General guidelines for complex structures
            instructions.append("""
            **Important Guidelines:**
            - Do **not** include 'properties', 'items', or 'type' in the output; these are metadata for understanding the structure.
            - `type` indicates the expected data type (e.g., INT, FLOAT, ARRAY, OBJECT).
            - `items` describes the structure of elements within an array.
            - `properties` defines the internal structure of objects.
            """);
        }

        // Formatting rule for numeric values
        instructions.append("""
        Numbers (Float, Int, Double etc.) must be numeric, without any other characters. All numeric values must not contain underscores, and scientific notation is not allowed.
    """);

        return new SystemMessage(instructions.toString());
    }


    /**
     * Processes the LLM response according to the port's schema type.
     * @param responseText The raw text response from the LLM
     * @param port The port containing the schema for structured output
     * @return The processed response as the appropriate Java type
     */
    public static Object processResponse(String responseText, Port port) {
        PortSchema schema = port.getSchema();
        PortType portType = schema.getType();

        // OBJECTS
        if (portType == PortType.OBJECT) {
            MapOutputConverter converter = new MapOutputConverter();
            return converter.convert(responseText);
        } else if (portType == PortType.ARRAY) {
            try {
                // ARRAYS od OBJECTS
                if (schema.getItems().getType() == PortType.OBJECT) {
                    StructuredOutputConverter<List<Map<String, Object>>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
                    });
                    return converter.convert(responseText);
                }
                // ARRAYS of ARRAYS
                else if (schema.getItems().getType() == PortType.ARRAY) {
                    StructuredOutputConverter<List<List<?>>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
                    });
                    return converter.convert(responseText);
                }
                // ARRAYS of FLOATS
                else if (schema.getItems().getType() == PortType.FLOAT) {
                    StructuredOutputConverter<List<Float>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
                    });
                    return converter.convert(responseText);
                }
                // ARRAYS of INTs
                else if (schema.getItems().getType() == PortType.INT) {
                    StructuredOutputConverter<List<Integer>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
                    });
                    return converter.convert(responseText);
                }
                // ARRAYS of STRINGS
                else {
                    StructuredOutputConverter<List<String>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
                    return converter.convert(responseText);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse LLM response: " + responseText);
                throw new RuntimeException("Failed to parse LLM response", e);
            }
        }
        // INT
        else if (portType == PortType.INT) {
            StructuredOutputConverter<Integer> converter = new BeanOutputConverter<>(Integer.class);
            return converter.convert(responseText);
        }
        // FLOAT
        else if (portType == PortType.FLOAT) {
            StructuredOutputConverter<Float> converter = new BeanOutputConverter<>(Float.class);
            return converter.convert(responseText);
        }
        // SIMPLE TEXT
        else {
            return responseText;
        }
    }

    /**
     * Helper method to process the response directly with ChatClient
     * @param chatClient The chat client to use for response processing
     * @param messages The messages to send to the chat client
     * @param port The port containing the schema
     * @return The processed response
     */
    public static PortStructuredOutputConverterResult processWithChatClient(ChatClient chatClient, List<Message> messages, Port port) {
        addFormatInstructions(messages, port);
        Prompt prompt = new Prompt(messages);

        logger.info("Prompt contents: {}", prompt.getContents());

        var response = chatClient.prompt(prompt).call();

        String responseText = response.content();

        logger.info("Raw LLM response: {}", responseText);

        var res = processResponse(responseText, port);

        logger.info("Converted response: {}", res);

        return new PortStructuredOutputConverterResult(res, responseText, response.chatResponse());
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PortStructuredOutputConverterResult {
        private Object result;
        private String rawResponse;
        private ChatResponse chatResponse;
    }
}