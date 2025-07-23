# Cognitive Workflow Framework

> **Academic Reference**: This implementation is based on my thesis "Designing Self-Aware Multi-Agent AI Systems: A Two-Fold Framework Based on AIBOM and Reflective Architecture", Università degli Studi di Firenze, 2024/2025.
> 
> 📚 **Complete thesis materials** (including PDF, experiments, use cases in AI4NE/NE4AI, and supplementary materials): [github.com/NiccoloCase/bsc-multi-agent-ai-framework](https://github.com/NiccoloCase/bsc-multi-agent-ai-framework/tree/main)

A reflective, self-aware multi-agent AI system that implements dynamic workflow execution with runtime adaptability. This framework combines the Reflection architectural pattern with an AI Bill of Materials (AIBOM) approach to create maintainable, traceable, and adaptive cognitive workflows.

**🎯 Research Focus**: This work advances the intersection of **Software Engineering for AI (SE4AI)** and **AI for Software Engineering (AI4SE)**, addressing critical challenges in building maintainable, traceable, and adaptive AI systems through principled software engineering approaches.

## 🚀 Quick Start

### Prerequisites

- MongoDB Atlas account
- OpenAI API key
- Anthropic API key (optional)
- Docker
- Make

### Environment Setup

Clone `.env.template` and edit with your variables:

```bash
cp .env.template .env
```

#### Custom api port
The default api port is 3001, in order to set a custom port add this line to your `.env` file
```bash
# Optional: Spring port (default 3001)
APP_PORT=port_number
```

### MongoDB Atlas Setup

The application requires specific search indexes to be created in MongoDB Atlas. **All indexes must be in `READY` status before starting the application.**

| Collection | Index Name | Type |
|------------|------------|------|
| `intents` | `intent_vector_index` | vectorSearch |
| `meta_nodes` | `node_search_index` | search | 
| `meta_nodes` | `node_vector_index` | vectorSearch | 

#### Index Configurations

**1. Full-Text Search Index for Nodes (`node_search_index` on `meta_nodes` collection)**

```json
{
  "mappings": {
    "dynamic": false,
    "fields": {
      "author": {
        "analyzer": "lucene.keyword",
        "type": "string"
      },
      "createdAt": {
        "type": "date"
      },
      "description": {
        "analyzer": "lucene.standard",
        "searchAnalyzer": "lucene.standard",
        "type": "string"
      },
      "enabled": {
        "type": "boolean"
      },
      "familyId": {
        "analyzer": "lucene.keyword",
        "type": "string"
      },
      "isLatest": {
        "type": "boolean"
      },
      "name": {
        "analyzer": "lucene.standard",
        "multi": {
          "exact": {
            "analyzer": "lucene.keyword",
            "type": "string"
          }
        },
        "searchAnalyzer": "lucene.standard",
        "type": "string"
      },
      "qualitativeDescriptor": {
        "dynamic": true,
        "fields": {
          "_all_text": {
            "analyzer": "lucene.standard",
            "type": "string"
          }
        },
        "type": "document"
      },
      "quantitativeDescriptor": {
        "dynamic": true,
        "type": "document"
      },
      "type": {
        "analyzer": "lucene.keyword",
        "type": "string"
      },
      "updatedAt": {
        "type": "date"
      },
      "version": {
        "fields": {
          "label": {
            "analyzer": "lucene.keyword",
            "type": "string"
          },
          "major": {
            "type": "number"
          },
          "minor": {
            "type": "number"
          },
          "patch": {
            "type": "number"
          }
        },
        "type": "document"
      }
    }
  },
  "analyzers": [
    {
      "charFilters": [
        {
          "mappings": {
            "AI": "artificial intelligence",
            "DB": "Data base",
            "DL": "deep learning",
            "ML": "machine learning",
            "NLP": "natural language processing"
          },
          "type": "mapping"
        }
      ],
      "name": "technical_analyzer",
      "tokenizer": {
        "type": "standard"
      }
    }
  ]
}
```

**2. Vector Search Index for Nodes (`node_vector_index` on `meta_nodes` collection)**

```json
{
  "fields": [
    {
      "numDimensions": 1536,
      "path": "embedding",
      "similarity": "cosine",
      "type": "vector"
    }
  ]
}
```

**3. Vector Search Index for Intents (`intent_vector_index` on `intents` collection)**

```json
{
  "fields": [
    {
      "numDimensions": 1536,
      "path": "embedding",
      "similarity": "cosine",
      "type": "vector"
    }
  ]
}
```

### Running the Application

```bash
# Clone the repository
git clone https://github.com/NiccoloCase/cognitive-workflow.git
cd cognitive-workflow

# Install dependencies and run
make run
```

The application will start on `http://localhost:3001`

### Clean the container
To remove a container in order to restart with a fresh new enviroment, exectute the following make script
```bash
make clean
```

## 🏗️ Architecture Overview

### Dual-Layer Architecture

The framework implements a two-layer architecture based on the Reflection pattern:

- **Knowledge Layer (Meta-Level)**: Manages workflow meta-models, node definitions, and intent configurations
- **Operational Layer (Base-Level)**: Handles real-time workflow execution and orchestration

### Core Components

#### 🧠 Intent Detection Service
- Analyzes natural language requests
- Retrieves or creates intent metadata
- Supports semantic intent matching

#### 🔀 Routing Manager  
- Queries workflows associated with detected intents
- Applies scoring and selection algorithms
- Supports diversity sampling for workflow selection

#### ⚙️ Workflow Engine
- Custom-built for dynamic AI workflows
- DAG-based execution with dependency resolution  
- Runtime adaptation and port matching

#### 🔌 Port Adapter Service
- Automatic port compatibility resolution using LLMs
- Runtime workflow patching
- Reduces system brittleness to interface changes

#### 🔍 Hybrid Search
- Combines semantic and keyword search
- Enables component discovery and retrieval
- Supports workflow synthesis from existing components

## 🧪 Testing

The project includes comprehensive test coverage:

- **48 Unit Tests**: Model validation, port logic, core functionality
- **54 Integration Tests**: LLM integration, schema conversion, database operations  
- **2 End-to-End Tests**: Complete workflow execution scenarios

## 🧪 Testing with Makefile

The `Makefile` provides commands to simplify testing tasks. Below are the available testing commands and their usage:

### Run Unit Tests
```bash
make unit_test
```
Runs the unit tests inside the `dev` container.

### Run End-to-End Tests
```bash
make e2e_test
```
Runs the end-to-end tests inside the `dev` container.

### Run Integration Tests
```bash
make integration_test
```
Runs the integration tests inside the `dev` container.

### Run Focused Tests
```bash
make focus_test
```
Runs tests with the `focus` tag, including both unit and integration tests.

### Run All Tests
```bash
make all_tests
```
Runs all tests: unit, integration, and end-to-end tests.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`) 
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Acknowledgments

- **Università degli Studi di Firenze** - Department of Information Engineering
- **Prof. Enrico Vicario** - Thesis Supervisor  
- **Marco Becattini** - Co-supervisor
- **Software Technologies Lab** - SALLMA architecture foundation

---
