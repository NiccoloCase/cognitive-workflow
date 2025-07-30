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
- **Hot-Swapping**: For non-breaking updates, if the instance is not currently running, the system may swap the meta-model in place, seamlessly refreshing the instance
- **Re-instantiation**: In cases where the instance is currently active or the update constitutes a breaking change (e.g., altered node dependencies in a workflow), the affected meta-model is marked as deprecated. Any future execution of such instances requires a complete re-creation, even if it was previously persisted in the registry
- Semantic versioning ensures compatibility (MAJOR.MINOR.PATCH)

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

- **NodeMetamodelValidator**:
  - Basic properties (name, description, author, version, type)
  - Port validation (keys, schemas, default values, duplicates)
  - Node-specific validation (REST URI, LLM model/provider, tool configuration)
  - Schema compatibility and type checking
- **WorkflowMetamodelValidator**:
  - DAG structure verification (no cycles)
  - Node reference validity and existence
  - Port compatibility between connected nodes
  - Entry/exit point validation
  - Edge condition validation
  - Required input satisfaction
- **IntentMetamodelValidator**: Validate intent definitions (to be implemented)

**⚠️ CRITICAL WARNING**: Any significant modification to meta-models (NodeMetamodel, WorkflowMetamodel, IntentMetamodel) MUST include:

1. **Validation Logic**: Add appropriate validation rules in the corresponding validator classes
2. **Unit Testing**: Create comprehensive unit tests for the new validation logic
3. **Error/Warning Classification**: Determine whether validation issues are errors or warnings
4. **Integration**: Ensure validation is called during meta-model creation/updates

### Operational Layer Components

#### WorkflowOrchestrator

- Coordinates complete workflow execution process
- Manages execution phases: intent detection → workflow selection → input mapping → execution → result extraction
- Handles observability and error management

#### Instance Management

- **NodeFactory**: Creates appropriate node instances from meta-models

- **WorkflowFactory**: Creates appropriate workflow instances from meta-models

- **NodeInstanceManager**: Manages node lifecycle and execution. It is responsible for getting a new instance of a node from a node meta-model by using the NodeFactory or by reusing an existing instance saved in a registry.

- **WorkflowInstanceManager**: Manages workflow instances. It is responsible for getting a new instance of a workflow from a workflow meta-model by using the WorkflowFactory or by reusing an existing instance saved in a registry.

#### Routing and Execution

- **RoutingManager**: Handles workflow selection based on intent
- **WorkflowExecutor**: Executes workflows in topological order
- **ExecutionContext**: Specialized HashMap supporting dot notation for nested access (example: `car.wheels.1.pressure`, `users.0.addresses.1.city`). For now the execution context is a single global instance for the entire workflow execution. It is used to store the input and output data of the workflow's nodes.

---

## Data Models

### Meta-model Types

- **WorkflowMetamodel**: Complete workflow definitions
- **NodeMetamodel**: Reusable node components with ports
- **IntentMetamodel**: User intent patterns for workflow selection

### Semantic Versioning

- **MAJOR**: Breaking changes
- **MINOR**: New features
- **PATCH**: Bug fixes

Each meta-model type (WorkflowMetamodel, NodeMetamodel, IntentMetamodel) maintains an independent semantic versioning scheme (MAJOR.MINOR.PATCH). For NodeMetamodels, non-breaking changes (minor or patch updates) result in the existing MongoDB document being updated in place, preserving the same document `_id`. In contrast, breaking changes (major version increments) are stored as entirely new documents, each with a unique `_id`. Every NodeMetamodel document also includes a `familyId` field, which groups all versions belonging to the same logical node lineage. Thus, `_id` uniquely identifies a specific version of a node, while `familyId` links all versions of that node family for version tracking and compatibility management. Workflows are DAGs of nodes, each referenced with their ID (not familyID).

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
- **Critical**: Requires comprehensive unit testing for all new functionality! Any modification to the execution context must be reflected in the InputMapper and PortAdapter services.

### Current Execution Model

- **Sequential**: Nodes executed one at a time in topological order
- **No Concurrency**: Concurrent execution not yet supported
- **Dependency Resolution**: Kahn's algorithm for execution order

### Node Types

#### AI Nodes

- **LLM Nodes**: Language model interactions with token tracking
- **Embedding Nodes**: Vector representation generation
- **Vector Database Nodes**: Similarity search and storage

#### Tool Nodes

- **REST Nodes**: External API integration

#### Flow Nodes

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

- **Unit Tests**: Fast tests for basic functionality. Mocking external services is required. No database access is allowed. No LLM calls are allowed.
- **Integration Tests**: Component interactions and service integration. Mocking of database is required. Real LLM calls are instead suggested in order to prevent regression of the AI services.
- **E2E Tests**: Complete workflow execution and API interactions. No mocking is allowed.
