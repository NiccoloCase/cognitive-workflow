# Cognitive Workflow Framework - Technical Documentation

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Concepts](#core-concepts)
3. [Data Models & Meta-models](#data-models--meta-models)
4. [Workflow Execution](#workflow-execution)
5. [AI Services Integration](#ai-services-integration)
6. [Port System](#port-system)
7. [Validation System](#validation-system)
8. [API Implementation](#api-implementation)
9. [Development Guidelines](#development-guidelines)
10. [Testing Requirements](#testing-requirements)

---

## Architecture Overview

### Reflection Pattern Implementation

The Cognitive Workflow Framework implements a reflective, self-aware architecture using the Meta-Object Protocol (MOP) pattern. This enables the system to inspect and modify its own behavior at runtime.

**Dual-Layer Architecture**:

- **Knowledge Layer (Meta-Level)**: Contains meta-models that define system behavior
- **Operational Layer (Base-Level)**: Contains runtime instances that execute workflows

**MOP Benefits**:

- **Runtime Adaptability**: System can modify its behavior without restart
- **Self-Inspection**: Components can examine their own structure and behavior
- **Dynamic Evolution**: New capabilities can be added at runtime
- **Consistency Management**: Changes propagate automatically between layers

### Runtime Adaptations

The system supports runtime adaptations through the MOP, enabling agents to modify knowledge during execution to accommodate environmental changes, dependency updates, or evolving requirements.

**Adaptation Sources**:

- **External Changes**: Software releases, dependency upgrades, new meta-models
- **Internal Triggers**: Port Adapter Service, new intent generation, workflow evolution
- **User Requirements**: Evolving user needs and preferences

**Consistency Mechanisms**:

- **Event Propagation**: Meta-model changes propagate as events via MOP
- **Subscriber Updates**: Operational components adapt configurations automatically
- **Version Management**: Semantic versioning ensures compatibility

### Meta-model Versioning

The system implements semantic versioning for meta-model catalogs to ensure compatibility and enable evolution.

**Version Strategies**:

- **Minor/Patch Changes**: Applied in-place within existing documents
- **Major Changes**: Create new meta-model entries for breaking changes
- **Family Grouping**: Group all versions of the same logical component

**Instance Update Strategies**:

- **Hot-Swapping**: In-place updates for non-breaking changes on inactive instances
- **Re-instantiation**: Complete recreation for active instances or breaking changes

---

## Core Concepts

### Meta-Object Protocol (MOP)

The MOP enables the system to inspect and modify its own structure and behavior at runtime.

**Core Components**:

- **Meta-model Services**: Manage creation, updates, and retrieval of meta-models
- **Event System**: Propagate changes between knowledge and operational layers
- **Validation System**: Ensure meta-model integrity and consistency
- **Search Services**: Enable semantic and hybrid search capabilities

**Event-Driven Updates**:

- **Creation Events**: Notify when new meta-models are created
- **Update Events**: Notify when existing meta-models are modified
- **Deletion Events**: Notify when meta-models are removed
- **Custom Events**: Application-specific events for specialized behavior

### Execution Context

The Execution Context is a specialized data structure that maintains state during workflow execution with advanced dot notation support.

**Key Features**:

- **Dot Notation Support**: Access nested objects and arrays using path notation
- **Dynamic Structure Creation**: Automatically create intermediate structures
- **Deep Copy Capabilities**: Ensure data isolation between execution phases
- **Type Safety**: Maintain data type consistency throughout execution

**Dot Notation Examples**:

- **Object Properties**: `car.wheels.frontLeft.pressure`
- **Array Indices**: `car.wheels.1.pressure`
- **Mixed Access**: `car.wheels.1.sensors.0.temperature`
- **Nested Arrays**: `users.0.addresses.1.city`

**Unit Testing Requirements**:
Comprehensive unit testing is vital for system reliability:

- **Dot Notation Testing**: Test all patterns (objects, arrays, mixed)
- **Edge Case Testing**: Test boundary conditions and error scenarios
- **Performance Testing**: Ensure acceptable performance for complex operations
- **Concurrency Testing**: Verify thread safety for concurrent access
- **Memory Testing**: Validate memory usage and cleanup for large structures

---

## Data Models & Meta-models

### Meta-model Structure

Meta-models define the structure and behavior of system components at the knowledge level.

**Core Meta-model Types**:

- **WorkflowMetamodel**: Defines complete workflow structures and relationships
- **NodeMetamodel**: Defines reusable node components with input/output ports
- **IntentMetamodel**: Defines user intent patterns for workflow selection

**Common Properties**:

- **Versioning**: Semantic versioning for compatibility management
- **Embeddings**: Vector representations for semantic search
- **Metadata**: Descriptive information for discovery and management
- **Validation**: Schema validation for data integrity

### Vector Search Implementation

Vector embeddings enable semantic search and similarity matching throughout the system.

**Embedding Types**:

- **Text Embeddings**: Generated from descriptions and metadata
- **Intent Embeddings**: Created from intent definitions for matching
- **Content Embeddings**: Produced from actual content for similarity

**Search Capabilities**:

- **Semantic Search**: Find similar components based on meaning
- **Hybrid Search**: Combine semantic and keyword-based search
- **Similarity Metrics**: Support for cosine and euclidean distance

**Performance Optimization**:

- **Indexed Vectors**: MongoDB Atlas vector search indexes
- **Dimensionality**: Optimized for 1536-dimensional embeddings
- **Batch Processing**: Efficient handling of multiple embeddings

### Data Consistency and Transactions

The system ensures data consistency through careful transaction management and event-driven updates.

**Consistency Mechanisms**:

- **Event Sourcing**: All changes captured as events
- **Causal Consistency**: Maintain logical ordering of operations
- **Eventual Consistency**: Handle distributed updates gracefully

**Transaction Boundaries**:

- **Meta-model Updates**: Atomic operations for meta-model changes
- **Workflow Execution**: Transactional workflow state management
- **Cross-layer Synchronization**: Consistent updates between layers

---

## Workflow Execution

### Execution Architecture

The workflow execution system orchestrates the complete lifecycle of workflow processing from intent detection to result delivery.

**Execution Phases**:

1. **Intent Detection**: Analyze user request to determine intent
2. **Workflow Selection**: Select appropriate workflow based on intent
3. **Input Mapping**: Map user variables to workflow inputs
4. **Workflow Execution**: Execute workflow nodes in dependency order
5. **Result Extraction**: Extract and format final results

**Current Execution Model**:

- **Sequential Execution**: Nodes executed one at a time in topological order
- **No Concurrency**: Concurrent execution not yet supported
- **Dependency Resolution**: Uses topological sorting for execution order
- **Single-threaded**: All node execution occurs in single execution thread

**Future Concurrency Support**:
The framework is designed to support concurrent execution in future versions, prioritizing simplicity and reliability through sequential execution.

### Dependency Resolution Algorithm

The workflow executor implements topological sorting for dependency resolution to ensure proper execution order.

**Resolution Process**:

1. **Graph Construction**: Build adjacency list from workflow edges
2. **In-Degree Calculation**: Determine dependencies for each node
3. **Topological Sorting**: Use Kahn's algorithm to determine execution order
4. **Cycle Detection**: Identify and handle circular dependencies
5. **Sequential Execution**: Execute nodes one at a time in topological order

### Node Instance Management

Node instances are runtime representations of node meta-models created through a factory pattern.

**Instance Lifecycle**:

1. **Creation**: Instances created from meta-models when workflows are instantiated
2. **Configuration**: Runtime configuration applied based on workflow-specific settings
3. **Execution**: Instances execute their logic with provided inputs
4. **Cleanup**: Resources properly released after execution

**Node Type Specializations**:

- **LLM Nodes**: Handle language model interactions with token tracking
- **Embedding Nodes**: Generate vector representations for semantic operations
- **Vector Database Nodes**: Perform similarity search and storage operations
- **REST Nodes**: Integrate with external APIs and services
- **Gateway Nodes**: Currently support only transparent gateways that pass input to output without transformation

### Gateway Node Limitations

**Current Gateway Support**:
The framework currently supports only transparent gateway nodes with minimal functionality:

- **Transparent Operation**: Output ports are a mirror of input ports
- **No Transformation**: Data passes through without any modification
- **No Conditional Logic**: No routing or decision-making capabilities
- **Simple Pass-through**: Input data is directly copied to output

**Future Gateway Types**:
The framework is designed to support more sophisticated gateway types in future versions:

- **Conditional Gateways**: Route data based on conditions
- **Decision Gateways**: Implement complex decision logic
- **Split/Merge Gateways**: Handle parallel execution paths
- **Error Handling Gateways**: Manage error conditions and recovery

---

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

#### Embedding Service

**Purpose**: Generates vector embeddings for semantic search and similarity matching

**Capabilities**:

- **Text Embeddings**: Convert text to high-dimensional vectors
- **Batch Processing**: Efficiently handle multiple texts
- **Model Selection**: Support for different embedding models
- **Dimensionality Management**: Optimize for search performance

### LLM Model Factory

The framework uses a factory pattern to create LLM clients for different providers, ensuring consistent interfaces and configuration management.

**Provider Support**:

- **OpenAI**: GPT models with various configurations
- **Anthropic**: Claude models for different use cases
- **Extensible**: Easy addition of new providers

### Structured Output Conversion

The framework provides utilities for converting LLM responses to structured data, ensuring reliable data extraction and validation.

**Conversion Process**:

1. **Response Parsing**: Extract JSON from LLM responses
2. **Schema Validation**: Ensure data conforms to expected structure
3. **Type Conversion**: Convert data to appropriate types
4. **Error Handling**: Gracefully handle malformed responses

---

## Port System

### Port Fundamentals

Ports are the communication interfaces between nodes in the workflow, defining data structure, validation rules, and transformation logic.

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

Port schemas define the structure and validation rules for data flowing through ports.

**Schema Types**:

- **Primitive Types**: String, integer, float, boolean
- **Complex Types**: Objects with defined properties
- **Array Types**: Collections with item schemas
- **Nested Types**: Complex nested structures

**Validation Capabilities**:

- **Type Checking**: Ensure data matches expected types
- **Required Fields**: Validate mandatory data presence
- **Nested Validation**: Validate complex nested structures
- **Compatibility Checking**: Determine if schemas can be connected

### Specialized Port Types

#### LLM Port

**Purpose**: Handles LLM-specific data structures and roles

**Roles**:

- **User Prompt**: User input for LLM processing
- **System Prompt Variable**: System prompt template variables
- **Response**: LLM generated response

#### Embeddings Port

**Purpose**: Handles vector embedding data

**Roles**:

- **Input Text**: Text to be embedded
- **Output Vector**: Generated embedding vector

#### Vector Database Port

**Purpose**: Handles vector database operations

**Roles**:

- **Query Vector**: Vector for similarity search
- **Search Results**: Search results with scores
- **Stored Vector**: Vector to be stored

#### REST Port

**Purpose**: Handles REST API communication

**Roles**:

- **Request Body**: HTTP request body
- **Response Body**: HTTP response body
- **Request Headers**: HTTP request headers

### Port Binding and Compatibility

#### Dot Notation for Nested Access

The port system supports dot notation for accessing nested properties, enabling complex data transformations and mappings.

**Path Resolution**:

- **Base Port Resolution**: Find the base port from the path
- **Nested Path Resolution**: Navigate through nested properties
- **Schema Validation**: Ensure paths are valid for the schema
- **Error Handling**: Graceful handling of invalid paths

#### Schema Path Resolution

The system can resolve complex paths through nested schemas to determine the exact data structure at any level.

**Resolution Process**:

1. **Path Parsing**: Break down the dot-notation path
2. **Schema Navigation**: Traverse the schema structure
3. **Validation**: Ensure each path segment is valid
4. **Result Return**: Return the schema at the specified path

### Creating New Port Types

The framework provides a flexible architecture for creating new port types to support specialized use cases.

**Extension Process**:

1. **Define Port Class**: Create new port class extending base Port
2. **Define Roles**: Specify the roles this port type supports
3. **Register Type**: Add to the port type system
4. **Implement Builder**: Provide builder pattern for easy construction

---

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

### Validation Result Structure

The validation system provides structured results that include both errors and warnings with detailed context.

**Result Components**:

- **Error Collection**: List of validation errors with component paths and messages
- **Warning Collection**: List of validation warnings with component paths and messages
- **Validation Status**: Overall validation status (valid/invalid)
- **Component Paths**: Hierarchical paths to identify validation issues
- **Context Information**: Additional context for understanding validation issues

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

---

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

---

## Development Guidelines

### Service Registration Pattern

Services are registered using Spring's component scanning to ensure proper dependency injection and lifecycle management.

**Registration Process**:

- **Component Scanning**: Automatic discovery of service classes
- **Dependency Injection**: Automatic injection of dependencies
- **Lifecycle Management**: Proper initialization and cleanup
- **Configuration Integration**: Integration with configuration system

### Configuration Pattern

Configuration is externalized using property injection to ensure flexibility and environment-specific settings.

**Configuration Sources**:

- **Environment Variables**: Runtime configuration
- **Configuration Files**: Persistent configuration
- **Default Values**: Fallback configuration
- **Dynamic Configuration**: Runtime configuration updates

### Validation Pattern

Model validation uses Jakarta Validation annotations to ensure data integrity and consistency.

**Validation Levels**:

- **Field-level Validation**: Validate individual fields
- **Object-level Validation**: Validate object relationships
- **Cross-field Validation**: Validate relationships between fields
- **Business Rule Validation**: Validate business logic constraints

### Event-Driven Pattern

The system uses events for meta-level updates to ensure loose coupling and scalability.

**Event Types**:

- **Creation Events**: Notify when new resources are created
- **Update Events**: Notify when resources are modified
- **Deletion Events**: Notify when resources are deleted
- **Custom Events**: Application-specific events

---

## Testing Requirements

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

### Integration Test Pattern

Integration tests verify component interactions to ensure components work together correctly.

**Testing Scope**:

- **Component Integration**: Test interactions between components
- **Service Integration**: Test service layer interactions
- **Database Integration**: Test data persistence and retrieval
- **External Service Integration**: Test external service interactions

### End-to-End Test Pattern

E2E tests verify complete system behavior to ensure the entire system works correctly.

**Testing Scope**:

- **Complete Workflows**: Test entire workflow execution
- **API Integration**: Test complete API interactions
- **User Scenarios**: Test realistic user scenarios
- **System Integration**: Test integration with external systems

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

### Mocking External Services

External services are mocked using WireMock to ensure reliable and fast testing.

**Mocking Strategy**:

- **Service Simulation**: Simulate external service behavior
- **Response Control**: Control service responses for testing
- **Error Simulation**: Simulate various error conditions
- **Performance Simulation**: Simulate different performance characteristics
