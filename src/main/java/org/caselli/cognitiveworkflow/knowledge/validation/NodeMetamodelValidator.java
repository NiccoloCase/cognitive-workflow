package org.caselli.cognitiveworkflow.knowledge.validation;

import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.ToolNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortType;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service for validating NodeMetamodels correctness.
 * - Node properties
 * - Ports validity
 * - Default values for ports
 * - serviceUri for REST nodes
 * @author niccolocaselli
 */
@Service
public class NodeMetamodelValidator {

    /**
     * Validate a NodeMetamodel
     * @param node The NodeMetamodel to validate.
     * @return A ValidationResult object
     */
    public ValidationResult validate(NodeMetamodel node) {
        ValidationResult result = new ValidationResult();

        if (node == null) {
            result.addError("NodeMetamodel cannot be null", "node");
            return result;
        }

        validateBasicNodeProperties(node, result);
        validatePorts(node.getInputPorts(), "input", result);
        validatePorts(node.getOutputPorts(), "output", result);

        // Validate specific node types
        if (node instanceof RestNodeMetamodel) {
            validateRestToolNode((RestNodeMetamodel) node, result);

        } else if (node instanceof ToolNodeMetamodel) {
            validateToolNode((ToolNodeMetamodel) node, result);
        }
        else if (node instanceof LlmNodeMetamodel) {
            validateLLMNode((LlmNodeMetamodel) node, result);
        }

        return result;
    }


    /**
     * Validates basic properties common to all node types
     * @param node NodeMetamodel to validate
     * @param result ValidationResult to store errors and warnings
     */
    private void validateBasicNodeProperties(NodeMetamodel node, ValidationResult result) {
        // Check name
        if (node.getName() == null || node.getName().trim().isEmpty())
            result.addError("Node name cannot be empty", "node");


        // Check description
        if (node.getDescription() == null || node.getDescription().trim().isEmpty())
            result.addWarning("Node description is empty", "node");


        // Check author
        if (node.getAuthor() == null || node.getAuthor().trim().isEmpty())
            result.addWarning("Node author is not specified", "node");


        // Check version
        if (node.getVersion() == null)
            result.addError("Node version cannot be null", "node");


        // Check type
        if (node.getType() == null)
            result.addError("Node type cannot be null", "node");
    }

    /**
     * Validates a collection of ports
     * @param ports Collection of ports to validate
     * @param portType Type of ports (input or output)
     */
    private void validatePorts(List<? extends Port> ports, String portType, ValidationResult result) {
        if (ports == null) {
            result.addError(portType + " ports collection cannot be null", "node." + portType + "Ports");
            return;
        }

        Set<String> portKeys = new HashSet<>();

        for (int i = 0; i < ports.size(); i++) {
            Port port = ports.get(i);
            String componentPath = "node." + portType + "Ports[" + i + "]";

            // Check port key
            if (port.getKey() == null || port.getKey().trim().isEmpty()) {
                result.addError("Port key cannot be empty", componentPath);
            } else {
                // Check for duplicate port keys
                if (portKeys.contains(port.getKey())) result.addError("Duplicate " + portType + " port key: " + port.getKey(), componentPath);
                portKeys.add(port.getKey());
            }

            // Check port schema
            validatePortSchema(port, componentPath, result);
        }
    }

    /**
     * Validates port schema
     * @param port Port to validate
     * @param componentPath Path to the port in the node
     */
    private void validatePortSchema(Port port, String componentPath, ValidationResult result) {
        PortSchema schema = port.getSchema();

        if (schema == null) {
            result.addError("Port schema cannot be null", componentPath + ".schema");
            return;
        }

        // Check port type
        if (schema.getType() == null) {
            result.addError("Port schema type cannot be null", componentPath + ".schema.type");
            return;
        }

        // Validate schema based on type
        PortType schemaType = schema.getType();

        // Check items field is used only for arrays
        if (schemaType != PortType.ARRAY && schema.getItems() != null)
            result.addError("'items' field should only be used for ARRAY type schemas", componentPath + ".schema");


        // Check properties field is used only for objects
        if (schemaType != PortType.OBJECT && schema.getProperties() != null && !schema.getProperties().isEmpty())
            result.addError("'properties' field should only be used for OBJECT type schemas", componentPath + ".schema");

        // Validate requirements based on type
        switch (schemaType) {
            case OBJECT:
                // For objects, properties should be defined
                if (schema.getProperties() == null || schema.getProperties().isEmpty()) {
                    result.addWarning("Object schema should define properties", componentPath + ".schema");
                }
                break;

            case ARRAY:
                // For arrays, items should be defined
                if (schema.getItems() == null)
                    result.addWarning("Array schema should define items", componentPath + ".schema");

                break;
        }

        validateDefaultValue(port, schema, componentPath, result);
    }

    /**
     * Validates a tool node metamodel
     * @param node ToolNodeMetamodel to validate
     * @param result ValidationResult to store errors and warnings
     */
    private void validateToolNode(ToolNodeMetamodel node, ValidationResult result) {
        // Check service URI
        if (node.getUri() == null || node.getUri().trim().isEmpty())
            result.addError("Tool node service URI cannot be empty", "node.serviceUri");

        // Check tool type
        if (node.getToolType() == null)
            result.addError("Tool type cannot be null", "node.toolType");
    }

    /**
     * Validates a LLM tool node metamodel
     */
    private void validateLLMNode(LlmNodeMetamodel node, ValidationResult result) {

        if (node.getModelName() == null) result.addError("LLM Node Model cannot be null", "node.modelName");
        if (node.getProvider() == null)
            result.addError("LLM Node Model Provider cannot be null", "node.llmProvider");

    }

    /**
     * Validates a REST tool node metamodel
     */
    private void validateRestToolNode(RestNodeMetamodel node, ValidationResult result) {
        validateToolNode(node, result);

        if (node.getInvocationMethod() == null)
            result.addError("REST tool invocation method cannot be null", "node.invocationMethod");


        // Validate service URI format
        String serviceUri = node.getUri();
        if (serviceUri != null && !serviceUri.trim().isEmpty()) {
            if (!serviceUri.startsWith("http://") && !serviceUri.startsWith("https://"))
                result.addError("REST service URI must start with http:// or https://", "node.serviceUri");

            // Check if the URI contains template variables that are not mapped to input ports
            validateRestUriTemplateVariables(node, result);
        }
    }

    /**
     * Validates that URI template variables are mapped to input ports (e.g., /users/{userId})
     * @param node RestNodeMetamodel to validate
     * @param result ValidationResult to store errors and warnings
     */
    private void validateRestUriTemplateVariables(RestNodeMetamodel node, ValidationResult result) {
        String serviceUri = node.getUri();
        List<String> templateVars = extractTemplateVariables(serviceUri);
        List<String> pathVariablesPortKeys = node.getInputPorts().stream()
                .filter(Objects::nonNull)
                .filter(port -> (port.getRole() != null) && port.getRole().equals(RestPort.RestPortRole.REQ_PATH_VARIABLE))
                .map(Port::getKey)
                .toList();

        for (String var : templateVars)
            if (!pathVariablesPortKeys.contains(var))
                result.addError("URI template variable '" + var + "' is not mapped to any input port", "node.serviceUri");
    }

    /**
     * Extracts template variables from a URI
     * @param uri URI to extract variables from
     * @return List of template variable names
     */
    private List<String> extractTemplateVariables(String uri) {
        List<String> variables = new ArrayList<>();

        int start = 0;

        while (true) {
            int openBrace = uri.indexOf('{', start);
            if (openBrace == -1) break;

            int closeBrace = uri.indexOf('}', openBrace);
            if (closeBrace == -1) break;

            String variable = uri.substring(openBrace + 1, closeBrace);
            variables.add(variable);
            start = closeBrace + 1;
        }

        return variables;
    }

    /**
     * Validates default values for ports
     * @param port Port to validate
     * @param schema PortSchema to validate against
     * @param componentPath Path to the port in the node
     */
    private void validateDefaultValue(Port port, PortSchema schema, String componentPath, ValidationResult result) {
        Object defaultValue = port.getDefaultValue();
        if (defaultValue == null) return;

        // Check if the default value is valid according to the schema
        if (!PortSchema.isValidValue(defaultValue, schema))
            result.addError("Default value is not valid for the schema", componentPath + ".defaultValue");
    }
}