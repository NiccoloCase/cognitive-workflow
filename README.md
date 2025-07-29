# Cognitive Workflow Framework

> **Academic Reference**: This implementation is based on my thesis "Designing Self-Aware Multi-Agent AI Systems: A Two-Fold Framework Based on AIBOM and Reflective Architecture", Università degli Studi di Firenze, 2024/2025.
>
> 📚 **Complete thesis materials** (including PDF, experiments, use cases in AI4NE/NE4AI, and supplementary materials): [github.com/NiccoloCase/bsc-multi-agent-ai-framework](https://github.com/NiccoloCase/bsc-multi-agent-ai-framework/tree/main)

A reflective, self-aware multi-agent AI system that implements dynamic workflow execution with runtime adaptability. This framework combines the Reflection architectural pattern with an AI Bill of Materials (AIBOM) approach to create maintainable, traceable, and adaptive cognitive workflows.

**🎯 Research Focus**: This work advances the intersection of **Software Engineering for AI (SE4AI)** and **AI for Software Engineering (AI4SE)**, addressing critical challenges in building maintainable, traceable, and adaptive AI systems through principled software engineering approaches.

## 📚 Documentation

For detailed technical documentation, implementation guides, and development patterns, see the [Technical Documentation](doc/DOCUMENTATION.md).

## 🏗️ Architecture Overview

![Workflow Overview](/doc/assets/workflow.gif)

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

## 🚀 Quick Start

### Prerequisites

- MongoDB Atlas account
- OpenAI API key
- Anthropic API key
- Docker
- Make

### Environment Setup

Clone `.env.template` and edit with your variables:

```bash
cp .env.template .env
```

#### 🌐 Custom api port

The default api port is 3001, in order to set a custom port add this line to your `.env` file

```bash
# Optional: Spring port (default 3001)
APP_PORT=port_number
```

### 🗂️ MongoDB Database Setup

The application requires **three separate MongoDB databases** to function properly:

#### 1. Main Database (MongoDB Atlas - Required)

**Purpose**: Primary application database supporting hybrid search capabilities

**Requirements**:

- Must be MongoDB Atlas (for vector and full-text search support)
- Collections: `intents`, `meta_nodes`, `workflows`
- All search indexes must be in `READY` status before starting the application

Collection

Index Name

Type

`intents`

`intent_vector_index`

vectorSearch

`meta_nodes`

`node_search_index`

search

`meta_nodes`

`node_vector_index`

vectorSearch

#### 2. Test Database (Any MongoDB Instance)

**Purpose**: Used exclusively for end-to-end (E2E) tests

**⚠️ Warning**: This database is **completely cleaned** at each test execution. Do not store any important data here.

**Requirements**:

- Collections: Mirror the main database (`intents`, `meta_nodes`, `workflows`)
- No search indexes required
- Can be any MongoDB instance (Atlas not required)

#### 3. Demo Database (MongoDB Atlas - Required)

**Purpose**: RAG demonstration database used in E2E and integration tests

**Requirements**:

- Must be MongoDB Atlas
- Database name: `sample_mflix`
- Collection name: `embedded_movies`
- Vector search index: `embedded_movies_vector_index`

**Data Source**: Uses the Atlas Sample Mflix Dataset containing movie data with vector embeddings.

**Example Document Schema**:

```json
{
  "_id": { "$oid": "573a1392f29313caabcd9ca6" },
  "plot": "An ambitious and near insanely violent gangster climbs the ladder of success in the mob, but his weaknesses prove to be his downfall.",
  "genres": ["Action", "Crime", "Drama"],
  "runtime": 93,
  "rated": "PASSED",
  "cast": ["Paul Muni", "Ann Dvorak", "Karen Morley", "Osgood Perkins"],
  "title": "Scarface",
  "year": 1932,
  "plot_embedding": [
    /* 1536-dimensional vector array */
  ],
  "imdb": {
    "rating": 7.8,
    "votes": 18334,
    "id": 23427
  },
  "type": "movie"
}
```

**Critical Fields**:

- `plot_embedding`: Vector embedding field (1536 dimensions)
- Database: `sample_mflix`
- Collection: `embedded_movies`
- Vector Index: `embedded_movies_vector_index`

#### Main Database Index Configurations

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

**4. Demo Database Vector Search Index (`embedded_movies_vector_index` on `embedded_movies` collection)**

```json
{
  "fields": [
    {
      "numDimensions": 1536,
      "path": "plot_embedding",
      "similarity": "cosine",
      "type": "vector"
    }
  ]
}
```

## 🚆 Running the Application

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

## 🧪 Testing

The project includes comprehensive test coverage:

- **48 Unit Tests**: Model validation, port logic, core functionality
- **54 Integration Tests**: LLM integration, schema conversion, database operations
- **2 End-to-End Tests**: Complete workflow execution scenarios

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
