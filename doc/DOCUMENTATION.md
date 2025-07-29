# Technical Documentation

## Table of Contents

1. [Architecture Implementation](#architecture-implementation)
2. [Core Components Deep Dive](#core-components-deep-dive)
3. [Data Models & Persistence](#data-models--persistence)
4. [API Implementation](#api-implementation)
5. [Workflow Execution Engine](#workflow-execution-engine)
6. [AI Services Integration](#ai-services-integration)
7. [Port System Architecture](#port-system-architecture)
8. [Observability Implementation](#observability-implementation)
9. [Development Patterns](#development-patterns)
10. [Testing Implementation](#testing-implementation)

## Architecture Implementation

### Meta-Object Protocol (MOP) Implementation

The Meta-Object Protocol (MOP) is the core mechanism that enables the Reflection architectural pattern. It provides a standardized interface for manipulating meta-objects (workflows, nodes, intents) and managing the relationship between the Knowledge Layer (meta-level) and Operational Layer (base-level).

#### MOP Services Architecture

The MOP consists of several key services that manage different aspects of the meta-level:

- **WorkflowMetamodelService**: Manages workflow definitions and their lifecycle
- **NodeMetamodelService**: Handles node meta-models and their versioning
- **IntentMetamodelService**: Manages intent definitions and semantic matching
- **NodeHybridSearchService**: Provides semantic and keyword search capabilities
- **Event System**: Coordinates meta-level updates through events

#### Runtime Adaptations and Consistency

A central advantage of the Reflection pattern is its support for runtime adaptability. In this architecture, agents both retrieve and modify knowledge during execution to accommodate modifications in the environment, dependency structures, or evolving user requirements.

**Operational agents interact with the Knowledge Layer** to query or update system knowledge in response to runtime conditions. These updates may originate:

- **Externally**: From software releases, dependency upgrades, or the introduction of new meta-models
- **Internally**: By components (e.g., the Port Adapter Service) or by system processes (e.g., the generation of new intents and workflows)

**To ensure synchronization and consistency** between the operational level and the Knowledge Layer, meta-model changes are propagated as events. These events, dispatched via the Meta-Object Protocol (MOP), are received by subscribed operational components, which then adapt their configurations accordingly.

#### Meta-Model Versioning

The versioning of meta-model catalogs follows the Semantic Versioning standard:

- **Minor and patch version changes** are applied in place within the existing document
- **Major version changes** (i.e., breaking updates) result in the creation of a new meta-model entry within the catalog

Each meta-model is identified by:

- **Unique ID**: Specific version identifier
- **Family ID**: Groups all versions belonging to the same logical component

Within a workflow, nodes reference a specific version of a node, ensuring consistency and reproducibility.

#### Strategies for Instance Updates

Two primary operational update strategies are employed depending on the nature of the meta-model change:

**Hot-Swapping**: For non-breaking updates, if the instance is not currently running, the system may swap the meta-model in place, seamlessly refreshing the instance.

**Re-instantiation**: In cases where the instance is currently active or the update constitutes a breaking change (e.g., altered node dependencies in a workflow), the affected meta-model is marked as deprecated. Any future execution of such instances requires a complete re-creation, even if it was previously persisted in the registry.

#### Event-Driven Meta-Level Updates

The MOP uses an event-driven architecture to maintain consistency between layers:

1. **Meta-model changes** trigger events in the Knowledge Layer
2. **Events are published** through the MOP to subscribed operational components
3. **Operational components** receive events and update their configurations
4. **Consistency is maintained** across the entire system

This mechanism ensures that changes in the meta-level are immediately reflected in the operational level, enabling dynamic adaptation without system restarts.

### Reflection Pattern Implementation

The framework implements the Reflection architectural pattern through two distinct layers:

#### Knowledge Layer (Meta-Level)

The Knowledge Layer manages the meta-objects that define the system's structure and behavior:

- **Model Management**: Meta-objects (WorkflowMetamodel, NodeMetamodel, IntentMetamodel)
- **MOP Services**: Meta-Object Protocol services for manipulation
- **Repository Layer**: Data access and persistence
- **Validation Logic**: Model validation and consistency checking

#### Operational Layer (Base-Level)

The Operational Layer handles real-time execution and runtime behavior:

- **Execution Engine**: Runtime workflow execution
- **Instance Management**: Runtime object instances
- **AI Services**: AI service implementations
- **Observability**: Execution monitoring and tracing
- **Registry Management**: Instance lifecycle management

### Service Layer Architecture

All services follow the Spring Boot service pattern with dependency injection, ensuring loose coupling and testability. The service layer provides a clean separation between business logic and infrastructure concerns.

## Core Components Deep Dive

### Workflow Orchestrator

The Workflow Orchestrator is the central coordinator that manages the complete workflow execution process. It orchestrates the interaction between different services to ensure smooth workflow execution.

**Execution Flow**:

1. **Intent Detection**: Analyzes the user request to determine the appropriate intent
2. **Routing**: Selects the most suitable workflow based on the detected intent
3. **Input Mapping**: Transforms user variables into the format expected by the workflow
4. **Execution**: Coordinates the actual workflow execution with dependency resolution
5. **Output Extraction**: Collects and formats the final results

The orchestrator maintains observability throughout the entire process, tracking performance metrics and token usage for each phase.

### Workflow Executor

The Workflow Executor implements a DAG-based execution engine that handles complex workflow dependencies and parallel execution where possible.

**Key Capabilities**:

- **Dependency Resolution**: Uses topological sorting to determine the correct execution order
- **Parallel Execution**: Identifies and executes independent nodes concurrently
- **Error Handling**: Provides robust error recovery and rollback mechanisms
- **Resource Management**: Efficiently manages computational resources during execution

**Execution Strategy**:
The executor follows a dependency-first approach, ensuring that all prerequisites are satisfied before executing each node. This prevents deadlocks and ensures data consistency across the workflow.

### Node Instance Management

Node instances are runtime representations of node meta-models. The system uses a factory pattern to create appropriate instances based on the node type.

**Instance Lifecycle**:

1. **Creation**: Instances are created from meta-models when workflows are instantiated
2. **Configuration**: Runtime configuration is applied based on workflow-specific settings
3. **Execution**: Instances execute their logic with provided inputs
4. **Cleanup**: Resources are properly released after execution

**Type-Specific Behavior**:
Each node type has specialized behavior:

- **LLM Nodes**: Handle language model interactions with token tracking
- **Embedding Nodes**: Generate vector representations for semantic operations
- **Vector Database Nodes**: Perform similarity search and storage operations
- **REST Nodes**: Integrate with external APIs and services
- **Gateway Nodes**: Implement conditional routing and decision logic

### Execution Context

The Execution Context is a specialized data structure that maintains state during workflow execution. It provides enhanced data access capabilities using dot notation for nested structures.

**Key Features**:

- **Dot Notation Support**: Enables access to nested data structures using path notation
- **Dynamic Structure Creation**: Automatically creates intermediate structures as needed
- **Deep Copy Capabilities**: Ensures data isolation between different execution phases
- **Type Safety**: Maintains data type consistency throughout execution

**Data Access Patterns**:
The context supports various data access patterns:

- Simple key-value pairs
- Nested object structures
- Array access with numeric indices
- Complex nested paths with mixed structures

This flexibility enables complex data transformations and routing within workflows while maintaining data integrity.

## Data Models & Persistence

### Meta-model Mapping Strategy

The system uses a sophisticated mapping strategy to translate between different data representations and ensure consistency across layers.

**Mapping Layers**:

- **Database Mapping**: MongoDB document mapping with custom serialization
- **API Mapping**: REST API request/response mapping with validation
- **Internal Mapping**: Cross-layer meta-model mapping with versioning
- **Runtime Mapping**: Dynamic mapping during workflow execution

**Versioning Strategy**:
Each meta-model follows semantic versioning principles:

- **Major Version**: Breaking changes that require new meta-model entries
- **Minor Version**: Backward-compatible feature additions
- **Patch Version**: Backward-compatible bug fixes
- **Label**: Optional pre-release or build metadata

**Family Grouping**:
Each meta-model belongs to a family that groups all versions of the same logical component. This enables:

- **Version History**: Tracking changes over time
- **Rollback Capabilities**: Reverting to previous versions when needed
- **Dependency Management**: Ensuring compatible versions are used together

**Validation Rules**:
The system enforces semantic versioning rules to prevent invalid version transitions and ensure system stability.

### Vector Search Implementation

Vector embeddings are used throughout the system for semantic search and similarity matching capabilities.

**Embedding Generation**:

- **Text Embeddings**: Generated from node descriptions and metadata
- **Intent Embeddings**: Created from intent definitions for matching
- **Content Embeddings**: Produced from actual content for similarity search

**Search Capabilities**:

- **Semantic Search**: Find similar components based on meaning
- **Hybrid Search**: Combine semantic and keyword-based search
- **Similarity Metrics**: Support for cosine and euclidean distance calculations

**Performance Optimization**:

- **Indexed Vectors**: MongoDB Atlas vector search indexes
- **Dimensionality**: Optimized for 1536-dimensional embeddings
- **Batch Processing**: Efficient handling of multiple embeddings

### Data Consistency and Transactions

The system ensures data consistency through careful transaction management and event-driven updates.

**Consistency Mechanisms**:

- **Event Sourcing**: All changes are captured as events
- **Causal Consistency**: Maintains logical ordering of operations
- **Eventual Consistency**: Handles distributed updates gracefully

**Transaction Boundaries**:

- **Meta-model Updates**: Atomic operations for meta-model changes
- **Workflow Execution**: Transactional workflow state management
- **Cross-layer Synchronization**: Consistent updates between knowledge and operational layers

## Validation System

### Hot Validation Architecture

The framework implements a comprehensive hot validation system that operates at multiple levels to ensure data integrity and system reliability.

**Validation Levels**:

- **Meta-model Validation**: Validates individual meta-models during creation and updates
- **Workflow Validation**: Validates complete workflow structures and relationships
- **Runtime Validation**: Validates data during workflow execution
- **Cross-reference Validation**: Validates relationships between different meta-models

**Validation Timing**:

- **Pre-save Validation**: Validates meta-models before persistence
- **Pre-execution Validation**: Validates workflows before execution
- **Runtime Validation**: Validates data during execution
- **Post-execution Validation**: Validates results after execution

### Error vs Warning Classification

The validation system distinguishes between errors and warnings based on their impact on system operation and potential for runtime resolution.

**Error Classification**:
Errors represent conditions that prevent normal operation and must be fixed before the system can proceed:

- **Structural Errors**: Missing required fields, invalid references, or broken relationships
- **Type Errors**: Incompatible data types that cannot be resolved
- **Configuration Errors**: Invalid configuration that prevents component initialization
- **Reference Errors**: Missing or invalid references to other components

**Warning Classification**:
Warnings represent conditions that may impact performance or functionality but don't prevent operation:

- **Port Incompatibilities**: Port type mismatches that can be resolved at runtime
- **Missing Optional Fields**: Optional fields that are recommended but not required
- **Performance Warnings**: Conditions that may impact performance
- **Deprecation Warnings**: Use of deprecated features or patterns

### Port Compatibility and Binding Validation

Port compatibility validation is a critical aspect of the system that distinguishes between errors and warnings based on runtime resolution capabilities.

**Port Incompatibilities as Warnings**:
Port incompatibilities are classified as warnings rather than errors because they can be resolved at runtime through the Port Adapter Service:

- **Type Mismatches**: Different port types that can be adapted
- **Schema Incompatibilities**: Structural differences that can be transformed
- **Missing Properties**: Optional properties that can be provided at runtime
- **Format Differences**: Data format differences that can be converted

**Runtime Resolution Process**:
When port incompatibilities are detected:

1. **Warning Generation**: The validation system generates warnings for incompatible ports
2. **Runtime Detection**: During execution, the Port Adapter Service detects incompatibilities
3. **Adaptation Attempt**: The service attempts to create adaptation logic
4. **Execution Continuation**: If adaptation succeeds, execution continues
5. **Error Escalation**: If adaptation fails, warnings escalate to runtime errors

**Explicit vs Implicit Binding Validation**:
The system validates both explicit and implicit bindings differently:

- **Explicit Bindings**: Direct port-to-port connections defined in workflow edges
- **Implicit Bindings**: Automatic connections based on matching port names
- **Validation Strategy**: Explicit bindings are validated more strictly than implicit ones

### Validation Result Structure

The validation system provides structured results that include both errors and warnings with detailed context.

**Result Components**:

- **Error Collection**: List of validation errors with component paths and messages
- **Warning Collection**: List of validation warnings with component paths and messages
- **Validation Status**: Overall validation status (valid/invalid)
- **Component Paths**: Hierarchical paths to identify validation issues
- **Context Information**: Additional context for understanding validation issues

**Component Path System**:
The system uses hierarchical component paths to precisely identify validation issues:

- **Node Paths**: `workflow.nodes.{nodeId}.{property}`
- **Port Paths**: `node.{portType}Ports[{index}].{property}`
- **Edge Paths**: `workflow.edges.{edgeId}.{property}`
- **Schema Paths**: `port.schema.{property}`

### Validation Extensibility

The validation system is designed to be extensible, allowing new validation rules to be added as new meta-model types are introduced.

**Validation Rule Addition**:
When new meta-model types are added to the system:

1. **Validator Implementation**: Create or extend appropriate validator classes
2. **Validation Rules**: Define specific validation rules for the new type
3. **Error/Warning Classification**: Determine appropriate classification for validation issues
4. **Integration**: Integrate validators into the validation pipeline
5. **Testing**: Create comprehensive unit tests for validation rules

**Required Validator Classes**:
The system includes several validator classes that must be extended for new meta-model types:

- **NodeMetamodelValidator**: Validates individual node meta-models
- **WorkflowMetamodelValidator**: Validates complete workflow structures
- **IntentMetamodelValidator**: Validates intent definitions (to be implemented)

### Validation Testing Requirements

Comprehensive unit testing of validation classes is essential to ensure system reliability and maintainability.

**Testing Strategy**:
Validation tests should cover:

- **Valid Input Testing**: Ensure valid meta-models pass validation
- **Invalid Input Testing**: Ensure invalid meta-models are properly rejected
- **Error Classification Testing**: Verify correct error vs warning classification
- **Edge Case Testing**: Test boundary conditions and unusual inputs
- **Performance Testing**: Ensure validation performance is acceptable

**Test Coverage Requirements**:
Validation tests should achieve high coverage:

- **Code Coverage**: Minimum 90% line coverage for validator classes
- **Branch Coverage**: Test all validation logic branches
- **Error Path Coverage**: Test all error and warning generation paths
- **Integration Coverage**: Test validation integration with other components

**Test Data Management**:
Validation tests require comprehensive test data:

- **Valid Meta-models**: Complete, valid meta-models for positive testing
- **Invalid Meta-models**: Meta-models with various validation issues
- **Edge Cases**: Meta-models with boundary conditions
- **Complex Scenarios**: Meta-models with complex relationships and dependencies

**Test Implementation Patterns**:
Validation tests should follow consistent patterns:

- **Setup Methods**: Create test data and configure validators
- **Assertion Methods**: Verify validation results and error/warning classifications
- **Helper Methods**: Create reusable test data builders
- **Mock Integration**: Mock dependencies for isolated testing

**Continuous Validation**:
Validation tests should be integrated into the continuous integration pipeline:

- **Pre-commit Validation**: Run validation tests before code commits
- **Build Validation**: Include validation tests in build processes
- **Regression Testing**: Ensure new changes don't break existing validation
- **Performance Monitoring**: Monitor validation performance over time

## API Implementation

### RESTful API Design

The framework exposes a RESTful API that follows standard HTTP conventions and provides comprehensive workflow management capabilities.

**API Design Principles**:

- **Resource-Oriented**: All endpoints represent resources (workflows, nodes, intents)
- **Stateless**: Each request contains all necessary information
- **Cacheable**: Responses are designed to be cacheable where appropriate
- **Uniform Interface**: Consistent use of HTTP methods and status codes

**Core Resources**:

- **Workflows**: Complete workflow definitions and execution
- **Nodes**: Node meta-models and configurations
- **Intents**: Intent definitions and matching capabilities

### Controller Patterns

All controllers follow consistent patterns for request handling, validation, and response formatting.

**Request Processing Flow**:

1. **Validation**: Input validation using Jakarta Validation annotations
2. **Business Logic**: Delegation to appropriate service layer
3. **Response Formatting**: Consistent response structure and error handling
4. **Observability**: Performance monitoring and logging

**Error Handling Strategy**:

- **Global Exception Handler**: Centralized error processing
- **Structured Error Responses**: Consistent error message format
- **HTTP Status Codes**: Appropriate status codes for different error types
- **Validation Errors**: Detailed validation failure information

### Workflow Execution API

The workflow execution endpoint provides the primary interface for running workflows with natural language requests.

**Execution Flow**:

1. **Request Processing**: Parse and validate the execution request
2. **Orchestration**: Coordinate the complete workflow execution process
3. **Response Formatting**: Structure the results with optional observability data
4. **Performance Monitoring**: Track execution time and resource usage

**Response Structure**:

- **Output Data**: The actual results from workflow execution
- **Observability Data**: Optional execution traces and metrics
- **Metadata**: Execution timing and resource consumption information

### CRUD Operations

The API provides comprehensive CRUD operations for all resource types with proper validation and versioning support.

**Create Operations**:

- **Validation**: Comprehensive input validation
- **Version Management**: Automatic version assignment
- **Event Publishing**: Notify subscribers of new resources
- **Response**: Return the created resource with generated identifiers

**Read Operations**:

- **Caching**: Efficient caching for frequently accessed resources
- **Filtering**: Support for various filtering and search criteria
- **Pagination**: Handle large result sets efficiently
- **Projection**: Return only required fields when specified

**Update Operations**:

- **Version Validation**: Ensure valid version transitions
- **Concurrency Control**: Handle concurrent update scenarios
- **Event Publishing**: Notify subscribers of changes
- **Rollback Support**: Enable reverting to previous versions

**Delete Operations**:

- **Soft Deletes**: Mark resources as deleted rather than physical removal
- **Dependency Checking**: Ensure no active dependencies exist
- **Event Publishing**: Notify subscribers of deletions
- **Cleanup**: Proper resource cleanup and deallocation

## Workflow Execution Engine

### Node Instance Management

Node instances are runtime representations of node meta-models. The system uses a factory pattern to create appropriate instances based on the node type.

**Instance Lifecycle**:

1. **Creation**: Instances are created from meta-models when workflows are instantiated
2. **Configuration**: Runtime configuration is applied based on workflow-specific settings
3. **Execution**: Instances execute their logic with provided inputs
4. **Cleanup**: Resources are properly released after execution

**Type-Specific Behavior**:
Each node type has specialized behavior:

- **LLM Nodes**: Handle language model interactions with token tracking
- **Embedding Nodes**: Generate vector representations for semantic operations
- **Vector Database Nodes**: Perform similarity search and storage operations
- **REST Nodes**: Integrate with external APIs and services
- **Gateway Nodes**: Implement conditional routing and decision logic

### Execution Context Implementation

The Execution Context is a specialized data structure that maintains state during workflow execution. It provides enhanced data access capabilities using dot notation for nested structures.

**Key Features**:

- **Dot Notation Support**: Enables access to nested data structures using path notation
- **Dynamic Structure Creation**: Automatically creates intermediate structures as needed
- **Deep Copy Capabilities**: Ensures data isolation between different execution phases
- **Type Safety**: Maintains data type consistency throughout execution

**Data Access Patterns**:
The context supports various data access patterns:

- Simple key-value pairs
- Nested object structures
- Array access with numeric indices
- Complex nested paths with mixed structures

This flexibility enables complex data transformations and routing within workflows while maintaining data integrity.

### Dependency Resolution Algorithm

The workflow executor implements topological sorting for dependency resolution to ensure proper execution order.

**Resolution Process**:

1. **Graph Construction**: Build adjacency list from workflow edges
2. **In-Degree Calculation**: Determine dependencies for each node
3. **Topological Sorting**: Use Kahn's algorithm to determine execution order
4. **Cycle Detection**: Identify and handle circular dependencies
5. **Parallel Execution**: Identify nodes that can execute concurrently

**Execution Strategy**:
The executor follows a dependency-first approach, ensuring that all prerequisites are satisfied before executing each node. This prevents deadlocks and ensures data consistency across the workflow.

## AI Services Integration

### LLM Service Architecture

The framework provides a comprehensive AI services layer that abstracts different LLM providers and provides consistent interfaces for various AI operations.

**Service Abstraction**:
All LLM services extend a common base class that provides:

- **Provider Abstraction**: Support for multiple LLM providers (OpenAI, Anthropic)
- **Token Tracking**: Automatic tracking of token usage for cost analysis
- **Observability**: Performance monitoring and execution time tracking
- **Error Handling**: Consistent error handling across all services

### LLM Service Implementations

#### Intent Detection Service

**Purpose**: Analyzes natural language requests to determine user intent

**Process Flow**:

1. **Vector Search**: Find similar intents using semantic embeddings
2. **Context Building**: Create context from available intents
3. **LLM Classification**: Use language model to classify the request
4. **Intent Creation**: Generate new intents for unrecognized patterns
5. **Confidence Scoring**: Assign confidence scores to detected intents

**Configuration**:
The service is configurable for different LLM providers, models, and temperature settings to optimize for intent detection accuracy.

#### Port Adapter Service

**Purpose**: Uses LLMs to resolve port compatibility issues at runtime

**Adaptation Process**:

1. **Schema Analysis**: Compare source and target port schemas
2. **Compatibility Assessment**: Determine if adaptation is possible
3. **Transformation Generation**: Create data transformation logic
4. **Validation**: Verify that the adaptation maintains data integrity
5. **Meta-model Update**: Store successful adaptations for future use

**Benefits**:

- **Runtime Flexibility**: Handle interface changes without system restarts
- **Component Reusability**: Enable components from different vendors to work together
- **System Resilience**: Automatically adapt to breaking changes in dependencies

#### Input Mapper Service

**Purpose**: Maps user variables to workflow input ports using LLM understanding

**Mapping Process**:

1. **Port Analysis**: Examine workflow input port requirements
2. **Variable Extraction**: Identify relevant user variables
3. **Semantic Mapping**: Use LLM to understand relationships
4. **Schema Validation**: Ensure mapped data conforms to port schemas
5. **Context Creation**: Build execution context with mapped data

**Intelligence**:
The service uses natural language understanding to map user intent to technical requirements, enabling natural language workflow execution.

#### Embedding Service

**Purpose**: Generates vector embeddings for semantic search and similarity matching

**Capabilities**:

- **Text Embeddings**: Convert text to high-dimensional vectors
- **Batch Processing**: Efficiently handle multiple texts
- **Model Selection**: Support for different embedding models
- **Dimensionality Management**: Optimize for search performance

**Applications**:

- **Semantic Search**: Find similar components based on meaning
- **Intent Matching**: Match user requests to available intents
- **Content Similarity**: Identify related content and components

### LLM Model Factory

The framework uses a factory pattern to create LLM clients for different providers, ensuring consistent interfaces and configuration management.

**Provider Support**:

- **OpenAI**: GPT models with various configurations
- **Anthropic**: Claude models for different use cases
- **Extensible**: Easy addition of new providers

**Configuration Management**:

- **Model Selection**: Choose appropriate models for different tasks
- **Parameter Tuning**: Configure temperature, tokens, and other parameters
- **Cost Optimization**: Balance performance and cost considerations

### Structured Output Conversion

The framework provides utilities for converting LLM responses to structured data, ensuring reliable data extraction and validation.

**Conversion Process**:

1. **Response Parsing**: Extract JSON from LLM responses
2. **Schema Validation**: Ensure data conforms to expected structure
3. **Type Conversion**: Convert data to appropriate types
4. **Error Handling**: Gracefully handle malformed responses

**Benefits**:

- **Reliability**: Consistent data extraction from LLM responses
- **Type Safety**: Ensure data types match expected schemas
- **Error Recovery**: Handle various response formats gracefully

## Port System Architecture

### Port Fundamentals

Ports are the communication interfaces between nodes in the workflow. They define the data structure, validation rules, and transformation logic for data flowing between nodes.

**Port Structure**:

- **Key**: Unique identifier for the port
- **Schema**: Data structure definition and validation rules
- **Type**: Specialization type (Standard, LLM, Embeddings, etc.)
- **Default Value**: Optional default value for the port

**Port Roles**:
Ports serve different roles depending on their specialization:

- **Data Transfer**: Standard ports for general data flow
- **AI Integration**: LLM ports for language model interactions
- **Vector Operations**: Embedding ports for vector data
- **External Integration**: REST ports for API communication

### Port Schema Implementation

Port schemas define the structure and validation rules:

```java
public class PortSchema {
    private PortType type;                          // Basic data type
    private PortSchema items;                       // Array item schema
    private Map<String, PortSchema> properties;     // Object properties
    private Boolean required;                       // Required flag

    public enum PortType {
        STRING, INT, FLOAT, BOOLEAN, OBJECT, ARRAY
    }

    // Schema compatibility checking
    public static boolean isCompatible(PortSchema source, PortSchema target) {
        if (!isTypeCompatible(source.type, target.type)) return false;

        // Handle arrays
        if (source.type == PortType.ARRAY) {
            if (target.type != PortType.ARRAY) return false;
            return isCompatible(source.items, target.items);
        }

        // Handle objects
        if (source.type == PortType.OBJECT) {
            if (target.type != PortType.OBJECT) return false;
            // Check property compatibility
            for (Map.Entry<String, PortSchema> targetProp : target.properties.entrySet()) {
                PortSchema sourcePropSchema = source.properties.get(targetProp.getKey());
                if (sourcePropSchema == null || !isCompatible(sourcePropSchema, targetProp.getValue())) {
                    return false;
                }
            }
        }

        return true;
    }

    // Value validation
    public boolean isValidValue(Object value) {
        if (value == null) return !required;

        switch (type) {
            case STRING: return value instanceof String;
            case INT: return value instanceof Integer || value instanceof Long;
            case FLOAT: return value instanceof Float || value instanceof Double;
            case BOOLEAN: return value instanceof Boolean;
            case ARRAY: return validateArrayValue(value);
            case OBJECT: return validateObjectValue(value);
            default: return false;
        }
    }
}
```

### Specialized Port Types

#### LLM Port

**Purpose**: Handles LLM-specific data structures and roles

**Roles**:

- **User Prompt**: User input for LLM processing
- **System Prompt Variable**: System prompt template variables
- **Response**: LLM generated response

**Special Features**:

- **Role-based Processing**: Different handling based on port role
- **Template Support**: Variable substitution in prompts
- **Response Formatting**: Structured response handling

#### Embeddings Port

**Purpose**: Handles vector embedding data

**Roles**:

- **Input Text**: Text to be embedded
- **Output Vector**: Generated embedding vector

**Special Features**:

- **Dimensionality Management**: Handle different vector dimensions
- **Batch Processing**: Support for multiple embeddings
- **Format Conversion**: Convert between different vector formats

#### Vector Database Port

**Purpose**: Handles vector database operations

**Roles**:

- **Query Vector**: Vector for similarity search
- **Search Results**: Search results with scores
- **Stored Vector**: Vector to be stored

**Special Features**:

- **Similarity Metrics**: Support for different distance calculations
- **Result Formatting**: Structured search result handling
- **Batch Operations**: Efficient batch processing

#### REST Port

**Purpose**: Handles REST API communication

**Roles**:

- **Request Body**: HTTP request body
- **Response Body**: HTTP response body
- **Request Headers**: HTTP request headers

**Special Features**:

- **HTTP Method Support**: Handle different HTTP methods
- **Authentication**: Support for various auth mechanisms
- **Error Handling**: Graceful handling of API errors

### Creating New Port Types

The framework provides a flexible architecture for creating new port types to support specialized use cases.

**Extension Process**:

1. **Define Port Class**: Create new port class extending base Port
2. **Define Roles**: Specify the roles this port type supports
3. **Register Type**: Add to the port type system
4. **Implement Builder**: Provide builder pattern for easy construction

**Integration Points**:

- **Schema System**: Integrate with existing schema validation
- **Compatibility Checking**: Add compatibility rules for new types
- **Serialization**: Ensure proper JSON serialization
- **Documentation**: Update documentation and examples

### Port Binding and Compatibility

#### Dot Notation for Nested Access

The port system supports dot notation for accessing nested properties, enabling complex data transformations and mappings.

**Path Resolution**:

- **Base Port Resolution**: Find the base port from the path
- **Nested Path Resolution**: Navigate through nested properties
- **Schema Validation**: Ensure paths are valid for the schema
- **Error Handling**: Graceful handling of invalid paths

**Use Cases**:

- **Data Transformation**: Map specific fields between ports
- **Nested Access**: Access deeply nested data structures
- **Conditional Mapping**: Map data based on specific paths
- **Data Filtering**: Extract specific fields from complex objects

#### Schema Path Resolution

The system can resolve complex paths through nested schemas to determine the exact data structure at any level.

**Resolution Process**:

1. **Path Parsing**: Break down the dot-notation path
2. **Schema Navigation**: Traverse the schema structure
3. **Validation**: Ensure each path segment is valid
4. **Result Return**: Return the schema at the specified path

**Error Handling**:

- **Invalid Paths**: Handle paths that don't exist
- **Type Mismatches**: Handle attempts to access properties on wrong types
- **Missing Schemas**: Handle undefined schema properties

## Execution Context and Dot Notation

### ExecutionContext Implementation

The Execution Context is a specialized data structure that maintains state during workflow execution. It provides enhanced data access capabilities using dot notation for nested structures.

**Core Features**:

- **Dot Notation Support**: Enables access to nested data structures using path notation
- **Dynamic Structure Creation**: Automatically creates intermediate structures as needed
- **Deep Copy Capabilities**: Ensures data isolation between different execution phases
- **Type Safety**: Maintains data type consistency throughout execution

**Data Access Patterns**:
The context supports various data access patterns:

- Simple key-value pairs
- Nested object structures
- Array access with numeric indices
- Complex nested paths with mixed structures

This flexibility enables complex data transformations and routing within workflows while maintaining data integrity.

### Dot Notation Examples

The execution context supports complex data access patterns using dot notation:

**Simple Access**:

- Direct key-value access for simple data
- Immediate retrieval without path resolution

**Nested Object Access**:

- Access properties within nested objects
- Automatic object creation for missing paths
- Support for deeply nested structures

**Array Access**:

- Access elements using numeric indices
- Support for nested arrays
- Automatic array expansion when needed

**Mixed Access**:

- Combine object and array access
- Support for complex nested structures
- Flexible path resolution

### Implementation Details

The execution context implements sophisticated path resolution and data manipulation capabilities.

**Path Resolution**:

- **Tokenization**: Break paths into individual components
- **Container Navigation**: Navigate through maps and lists
- **Type Detection**: Determine container types dynamically
- **Error Recovery**: Handle missing or invalid paths gracefully

**Data Manipulation**:

- **Dynamic Creation**: Create missing containers automatically
- **Type Preservation**: Maintain exact data types during copying
- **Deep Copying**: Ensure data isolation between contexts
- **Memory Management**: Efficient memory usage for large structures

**Container Operations**:

- **Map Operations**: Standard map operations with path support
- **List Operations**: List manipulation with index-based access
- **Mixed Operations**: Operations that work across different container types
- **Validation**: Ensure operations maintain data integrity

### Deep Copy Support

The execution context provides comprehensive deep copy capabilities to ensure data isolation.

**Copy Process**:

- **Recursive Traversal**: Deep traversal of all nested structures
- **Type Preservation**: Maintain exact data types during copying
- **Reference Isolation**: Ensure no shared references between copies
- **Memory Efficiency**: Optimize memory usage for large structures

**Use Cases**:

- **Context Isolation**: Prevent data leakage between executions
- **Snapshot Creation**: Create point-in-time copies for analysis
- **Parallel Execution**: Support concurrent workflow execution
- **Rollback Support**: Enable reverting to previous states

## Observability Implementation

### Observability Report Structure

All observability reports follow a consistent structure to ensure uniform monitoring and analysis capabilities.

**Common Fields**:

- **Timing Information**: Start time, end time, and duration
- **Token Usage**: Comprehensive token consumption tracking
- **Error Information**: Detailed error reporting when failures occur
- **Metadata**: Additional context and configuration information

**Report Types**:

- **Intent Detection Reports**: Track intent detection performance
- **Routing Reports**: Monitor workflow selection and routing
- **Input Mapping Reports**: Track input transformation processes
- **Workflow Execution Reports**: Monitor complete workflow execution
- **Orchestration Reports**: Aggregate reports from all phases

### Token Usage Tracking

Token usage tracking is essential for cost analysis, performance optimization, and experimentation.

**Tracking Components**:

- **Prompt Tokens**: Tokens consumed by input prompts
- **Generation Tokens**: Tokens generated in responses
- **Total Tokens**: Complete token consumption
- **Cost Calculation**: Financial impact of token usage

**Aggregation Methods**:

- **Service-level Aggregation**: Combine tokens from individual services
- **Workflow-level Aggregation**: Total tokens for complete workflows
- **Time-based Aggregation**: Token usage over specific time periods
- **Provider-based Aggregation**: Tokens by LLM provider

**Analysis Capabilities**:

- **Cost Optimization**: Identify expensive operations
- **Performance Analysis**: Correlate tokens with performance
- **Usage Patterns**: Understand typical token consumption
- **Budget Management**: Track against usage budgets

### Orchestration Observability

The orchestrator aggregates observability from all execution phases to provide comprehensive monitoring.

**Aggregation Process**:

- **Phase Collection**: Gather reports from all execution phases
- **Token Summation**: Calculate total token usage across phases
- **Timing Analysis**: Analyze timing across different phases
- **Error Correlation**: Correlate errors across phases

**Reporting Capabilities**:

- **Summary Reports**: High-level execution summaries
- **Detailed Reports**: Phase-by-phase breakdowns
- **Performance Metrics**: Timing and resource usage analysis
- **Error Analysis**: Comprehensive error reporting and analysis

**Use Cases**:

- **Performance Monitoring**: Track system performance over time
- **Cost Analysis**: Understand operational costs
- **Debugging**: Identify performance bottlenecks and errors
- **Optimization**: Guide system optimization efforts

## Development Patterns

### Service Registration Pattern

Services are registered using Spring's component scanning to ensure proper dependency injection and lifecycle management.

**Registration Process**:

- **Component Scanning**: Automatic discovery of service classes
- **Dependency Injection**: Automatic injection of dependencies
- **Lifecycle Management**: Proper initialization and cleanup
- **Configuration Integration**: Integration with configuration system

**Benefits**:

- **Loose Coupling**: Services are decoupled from their consumers
- **Testability**: Easy mocking and testing of individual services
- **Modularity**: Clear separation of concerns
- **Maintainability**: Easy to modify and extend services

### Configuration Pattern

Configuration is externalized using property injection to ensure flexibility and environment-specific settings.

**Configuration Sources**:

- **Environment Variables**: Runtime configuration
- **Configuration Files**: Persistent configuration
- **Default Values**: Fallback configuration
- **Dynamic Configuration**: Runtime configuration updates

**Configuration Management**:

- **Type Safety**: Strongly typed configuration values
- **Validation**: Configuration validation and error checking
- **Documentation**: Self-documenting configuration
- **Versioning**: Configuration version management

**Benefits**:

- **Environment Flexibility**: Different configurations for different environments
- **Security**: Secure handling of sensitive configuration
- **Maintainability**: Centralized configuration management
- **Deployment Flexibility**: Easy deployment across environments

### Validation Pattern

Model validation uses Jakarta Validation annotations to ensure data integrity and consistency.

**Validation Levels**:

- **Field-level Validation**: Validate individual fields
- **Object-level Validation**: Validate object relationships
- **Cross-field Validation**: Validate relationships between fields
- **Business Rule Validation**: Validate business logic constraints

**Validation Features**:

- **Automatic Validation**: Automatic validation during data processing
- **Error Reporting**: Detailed error messages and locations
- **Custom Validators**: Support for custom validation logic
- **Performance Optimization**: Efficient validation processing

**Benefits**:

- **Data Integrity**: Ensure data quality and consistency
- **Error Prevention**: Catch errors early in the process
- **User Experience**: Provide clear error messages
- **System Reliability**: Prevent invalid data from causing issues

### Event-Driven Pattern

The system uses events for meta-level updates to ensure loose coupling and scalability.

**Event Types**:

- **Creation Events**: Notify when new resources are created
- **Update Events**: Notify when resources are modified
- **Deletion Events**: Notify when resources are deleted
- **Custom Events**: Application-specific events

**Event Processing**:

- **Asynchronous Processing**: Non-blocking event processing
- **Event Ordering**: Maintain event order when necessary
- **Error Handling**: Graceful handling of event processing errors
- **Retry Logic**: Automatic retry for failed event processing

**Benefits**:

- **Loose Coupling**: Components are decoupled through events
- **Scalability**: Easy to add new event consumers
- **Reliability**: Robust event processing with error handling
- **Extensibility**: Easy to extend system with new event types

## Testing Implementation

### Unit Test Pattern

Unit tests focus on isolated component testing to ensure individual components work correctly.

**Testing Strategy**:

- **Isolation**: Test components in isolation
- **Mocking**: Use mocks for external dependencies
- **Coverage**: Ensure comprehensive test coverage
- **Performance**: Include performance testing where appropriate

**Test Structure**:

- **Arrange**: Set up test data and conditions
- **Act**: Execute the component under test
- **Assert**: Verify expected outcomes
- **Cleanup**: Clean up test resources

**Best Practices**:

- **Test Naming**: Clear, descriptive test names
- **Test Independence**: Tests should not depend on each other
- **Fast Execution**: Tests should execute quickly
- **Maintainability**: Tests should be easy to maintain

### Integration Test Pattern

Integration tests verify component interactions to ensure components work together correctly.

**Testing Scope**:

- **Component Integration**: Test interactions between components
- **Service Integration**: Test service layer interactions
- **Database Integration**: Test data persistence and retrieval
- **External Service Integration**: Test external service interactions

**Test Configuration**:

- **Test Databases**: Isolated test database instances
- **Mock Services**: Mock external services where appropriate
- **Test Data**: Comprehensive test data sets
- **Environment Setup**: Proper test environment configuration

**Validation Focus**:

- **Data Flow**: Verify data flows correctly between components
- **Error Handling**: Test error scenarios and recovery
- **Performance**: Verify acceptable performance under load
- **Concurrency**: Test concurrent access scenarios

### End-to-End Test Pattern

E2E tests verify complete system behavior to ensure the entire system works correctly.

**Testing Scope**:

- **Complete Workflows**: Test entire workflow execution
- **API Integration**: Test complete API interactions
- **User Scenarios**: Test realistic user scenarios
- **System Integration**: Test integration with external systems

**Test Environment**:

- **Production-like Environment**: Environment similar to production
- **Real Data**: Realistic test data sets
- **External Services**: Real or realistic external service interactions
- **Performance Monitoring**: Monitor performance during tests

**Validation Criteria**:

- **Functional Correctness**: Verify correct system behavior
- **Performance Requirements**: Ensure performance meets requirements
- **Reliability**: Verify system reliability under various conditions
- **User Experience**: Ensure good user experience

### Test Data Management

Test data is managed through dedicated test configurations to ensure consistent and reliable testing.

**Data Management**:

- **Test Data Sets**: Comprehensive test data for various scenarios
- **Data Isolation**: Isolated test data to prevent interference
- **Data Cleanup**: Proper cleanup after tests
- **Data Versioning**: Version control for test data

**Configuration Management**:

- **Environment-specific Configuration**: Different configurations for different environments
- **Test-specific Configuration**: Configuration specific to testing needs
- **Dynamic Configuration**: Runtime configuration updates for tests
- **Configuration Validation**: Validation of test configurations

**Benefits**:

- **Consistency**: Consistent test results across environments
- **Reliability**: Reliable test execution
- **Maintainability**: Easy to maintain and update test data
- **Coverage**: Comprehensive test coverage with various data scenarios

### Mocking External Services

External services are mocked using WireMock to ensure reliable and fast testing.

**Mocking Strategy**:

- **Service Simulation**: Simulate external service behavior
- **Response Control**: Control service responses for testing
- **Error Simulation**: Simulate various error conditions
- **Performance Simulation**: Simulate different performance characteristics

**Mock Configuration**:

- **Request Matching**: Match requests to appropriate responses
- **Response Definition**: Define expected responses
- **Error Scenarios**: Define error scenarios for testing
- **Performance Characteristics**: Define response times and patterns

**Benefits**:

- **Reliability**: Reliable test execution independent of external services
- **Speed**: Fast test execution without external dependencies
- **Control**: Full control over test scenarios
- **Coverage**: Test various external service scenarios
