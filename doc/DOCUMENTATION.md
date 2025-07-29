# Cognitive Workflow Framework - Technical Documentation

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Components](#core-components)
3. [Data Models](#data-models)
4. [Workflow Execution](#workflow-execution)
5. [AI Services](#ai-services)
6. [Port System](#port-system)
7. [Validation](#validation)
8. [Development Guidelines](#development-guidelines)

---

## Architecture Overview

### Reflection Pattern

- **Knowledge Layer**: Meta-models defining system behavior
- **Operational Layer**: Runtime instances executing workflows
- **MOP**: Meta-Object Protocol enables runtime self-inspection and modification

### Runtime Adaptations

- Meta-model changes propagate as events via MOP
- Hot-swapping for non-breaking changes, re-instantiation for breaking changes
- Semantic versioning ensures compatibility

---

## Core Components

### Knowledge Layer Components

#### Meta-Object Protocol (MOP)

- **Meta-model Services**: Manage creation, updates, retrieval
- **Event System**: Propagate changes between layers
- **Validation System**: Ensure meta-model integrity
- **Search Services**: Semantic and hybrid search capabilities

#### Repositories

- **IntentMetamodelCatalog**: Intent meta-model persistence
- **NodeMetamodelCatalog**: Node meta-model persistence
- **WorkflowMetamodelCatalog**: Workflow meta-model persistence

#### Validator Services

- **NodeMetamodelValidator**: Validate individual node meta-models
- **WorkflowMetamodelValidator**: Validate complete workflow structures
- **IntentMetamodelValidator**: Validate intent definitions (to be implemented)

### Operational Layer Components

#### WorkflowOrchestrator

- Coordinates complete workflow execution process
- Manages execution phases: intent detection → workflow selection → input mapping → execution → result extraction
- Handles observability and error management

#### Node Instance Management

- **NodeFactory**: Creates appropriate node instances from meta-models
- **NodeInstanceManager**: Manages node lifecycle and execution
- **WorkflowInstanceManager**: Manages workflow instances and state

#### Routing and Execution

- **RoutingManager**: Handles workflow selection based on intent
- **WorkflowExecutor**: Executes workflows in topological order
- **ExecutionContext**: Specialized HashMap supporting dot notation for nested access

---

## Data Models

### Meta-model Types

- **WorkflowMetamodel**: Complete workflow definitions
- **NodeMetamodel**: Reusable node components with ports
- **IntentMetamodel**: User intent patterns for workflow selection

### Vector Search

- 1536-dimensional embeddings for semantic search
- MongoDB Atlas vector indexes
- Hybrid search combining semantic and keyword matching

---

## Workflow Execution

### Execution Context

- Specialized HashMap supporting dot notation for nested access
- Examples: `car.wheels.1.pressure`, `users.0.addresses.1.city`
- Deep copy capabilities for data isolation
- **Critical**: Requires comprehensive unit testing for all new functionality

### Current Execution Model

- **Sequential**: Nodes executed one at a time in topological order
- **No Concurrency**: Concurrent execution not yet supported
- **Dependency Resolution**: Kahn's algorithm for execution order

### Node Types

- **LLM Nodes**: Language model interactions with token tracking
- **Embedding Nodes**: Vector representation generation
- **Vector Database Nodes**: Similarity search and storage
- **REST Nodes**: External API integration
- **Gateway Nodes**: Currently only transparent (pass-through) - no conditional logic

### Execution Phases

1. Intent Detection → 2. Workflow Selection → 3. Input Mapping → 4. Workflow Execution → 5. Result Extraction

---

## AI Services

### LLM Services

- **Intent Detection**: Analyze requests to determine user intent
- **Port Adapter**: Resolve port compatibility issues at runtime using LLMs
- **Input Mapper**: Map user variables to workflow inputs using LLM understanding
- **Embedding Service**: Generate vector embeddings for semantic search

### Providers

- OpenAI (GPT models)
- Anthropic (Claude models)
- Extensible for new providers

---

## Port System

### Port Types

- **Standard**: General data transfer
- **LLM**: Language model interactions (USER_PROMPT, SYSTEM_PROMPT_VARIABLE, RESPONSE)
- **Embeddings**: Vector data operations
- **Vector Database**: Similarity search operations
- **REST**: API communication

### Port Schema

- Defines data structure and validation rules
- Supports primitive types, objects, arrays, nested structures
- Compatibility checking between ports

### Binding

- **Explicit**: Direct port-to-port connections in workflow edges
- **Implicit**: Automatic connections based on matching port names
- Dot notation support: `port.schema.property.array.0.field`

---

## Validation

### Error vs Warning Classification

- **Errors**: Prevent operation (structural, type, configuration, reference errors)
- **Warnings**: May impact performance but don't prevent operation (port incompatibilities, missing optional fields)

### Port Incompatibilities as Warnings

- Classified as warnings because they can be resolved at runtime via Port Adapter Service
- If runtime adaptation fails, warnings escalate to errors

### Validation Extensibility

- New meta-model types require extending appropriate validator classes
- Required validators: NodeMetamodelValidator, WorkflowMetamodelValidator, IntentMetamodelValidator

---

## Development Guidelines

### Service Registration

- Use Spring component scanning for automatic discovery
- Proper dependency injection and lifecycle management

### Configuration

- Externalize using property injection
- Support environment variables, config files, default values

### Validation

- Use Jakarta Validation annotations
- Field-level, object-level, cross-field, and business rule validation

### Event-Driven Updates

- Use events for meta-level updates
- Creation, update, deletion, and custom events

### Testing Requirements

- **Unit Tests**: 90% line coverage minimum for validator classes
- **Integration Tests**: Component interactions and service integration
- **E2E Tests**: Complete workflow execution and API interactions
- **Validation Tests**: Comprehensive testing of all validation rules and error/warning classification
- **Mock External Services**: Use WireMock for reliable testing

### Adding New Meta-model Types

1. Create/extend appropriate validator classes
2. Define validation rules and error/warning classification
3. Integrate into validation pipeline
4. Create comprehensive unit tests
5. Update documentation
