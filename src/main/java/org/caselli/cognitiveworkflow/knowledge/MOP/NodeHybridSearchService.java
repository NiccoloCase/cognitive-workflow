package org.caselli.cognitiveworkflow.knowledge.MOP;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bson.Document;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.AI.services.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for hybrid search of Nodes using MongoDB Atlas Vector Search and Full-Text Search.
 * Implements Reciprocal Rank Fusion (RRF) pattern for combining results.
 * @author niccolocaselli
 */
@Service
public class NodeHybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(NodeHybridSearchService.class);

    private final MongoTemplate mongoTemplate;
    private final EmbeddingService embeddingService;

    // Configuration constants
    private final String collectionName = "meta_nodes";
    private final String fullTextSearchIndexName = "node_search_index";
    private final String vectorSearchIndexName = "node_vector_index";
    private final String vectorSearchFieldName = "embedding";

    // Hybrid search parameters
    private final double vectorWeight = 0.7;
    private final double fullTextWeight = 0.3;
    private final int numCandidates = 100;
    private final int searchLimit = 20;
    private final int finalLimit = 10;

    @Autowired
    public NodeHybridSearchService(MongoTemplate mongoTemplate, EmbeddingService embeddingService) {
        this.mongoTemplate = mongoTemplate;
        this.embeddingService = embeddingService;
    }

    /**
     * Perform the Hybrid Search (Semantic Search + Full Text Search) on the Metamodel Catalog Repository With Optional Filtering
     * @param input The input query
     * @param filter Filters
     * @return Returns a list of matching meta-models
     */
    public List<NodeSearchResult> performHybridSearch(String input, NodeSearchFilter filter) {
        if (input == null || input.trim().isEmpty()) return Collections.emptyList();

        try {
            List<Double> userInputEmbedding = embeddingService.generateEmbedding(input);
            if (userInputEmbedding == null || userInputEmbedding.isEmpty()) {
                log.error("Failed to generate embedding for user input.");
                return Collections.emptyList();
            }

            List<Document> pipeline = buildHybridSearchPipeline(input, userInputEmbedding, filter);


            Aggregation aggregation = Aggregation.newAggregation(
                    pipeline.stream()
                            .map(doc -> Aggregation.stage(Document.parse(doc.toJson())))
                            .toArray(AggregationOperation[]::new)
            );

            AggregationResults<Document> results = mongoTemplate.aggregate(
                    aggregation,
                    collectionName,
                    Document.class
            );


            List<NodeSearchResult> finalResults = results.getMappedResults().stream().map(doc -> {
                NodeMetamodel model = mongoTemplate.getConverter().read(NodeMetamodel.class, doc);
                Double combined = doc.getDouble("combined_score");
                Double vector = doc.getDouble("vector_score");
                Double fulltext = doc.getDouble("fulltext_score");

                return new NodeSearchResult(model, combined, vector, fulltext);
            }).toList();


            log.info("Hybrid search returned {} results for query: '{}'", finalResults.size(), input);

            return finalResults;

        } catch (Exception e) {
            log.error("Error during hybrid search for query '{}': {}", input, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Builds the MongoDB aggregation pipeline for hybrid search using RRF pattern.
     * @param query The text query
     * @param queryVector The Query Embedding
     * @param filter Filters
     */
    private List<Document> buildHybridSearchPipeline(String query, List<Double> queryVector, NodeSearchFilter filter) {

        // Stage 1: Vector Search
        Document vectorSearchStage = new Document("$vectorSearch", new Document()
                .append("index", vectorSearchIndexName)
                .append("path", vectorSearchFieldName)
                .append("queryVector", queryVector)
                .append("numCandidates", numCandidates)
                .append("limit", searchLimit)
        );

        // Stage 2: Add vector search metadata
        Document addVectorMetaStage = new Document("$addFields", new Document()
                .append("search_score", new Document("$meta", "vectorSearchScore"))
                .append("search_type", "vector")
        );

        // Stage 3: Union with full-text search
        Document unionWithStage = new Document("$unionWith", new Document()
                .append("coll", collectionName)
                .append("pipeline", Arrays.asList(
                        new Document("$search", new Document()
                                .append("index", fullTextSearchIndexName)
                                .append("compound", new Document()
                                        .append("should", List.of(
                                                new Document("text", new Document()
                                                        .append("query", query)
                                                        .append("path", Arrays.asList("name", "description", "qualitativeDescriptor._all_text"))
                                                        .append("fuzzy", new Document("maxEdits", 1))
                                                )
                                        ))
                                )
                        ),
                        new Document("$limit", searchLimit),
                        new Document("$addFields", new Document()
                                .append("search_score", new Document("$meta", "searchScore"))
                                .append("search_type", "fulltext")
                        )
                ))
        );

        // Stage 4: Group by document ID and calculate RRF score
        Document groupByIdStage = new Document("$group", new Document()
                .append("_id", "$_id")
                .append("doc", new Document("$first", "$$ROOT"))
                .append("vector_score", new Document("$max", new Document("$cond", Arrays.asList(
                        new Document("$eq", Arrays.asList("$search_type", "vector")),
                        new Document("$toDouble", "$search_score"), // Cast to double here
                        0.0
                ))))
                .append("fulltext_score", new Document("$max", new Document("$cond", Arrays.asList(
                        new Document("$eq", Arrays.asList("$search_type", "fulltext")),
                        new Document("$toDouble", "$search_score"), // Cast to double here
                        0.0
                ))))
        );

        // Stage 5: Replace root with original document and add combined score
        Document replaceRootStage = new Document("$replaceRoot", new Document()
                .append("newRoot", new Document("$mergeObjects", Arrays.asList(
                        "$doc",
                        new Document()
                                .append("vector_score", "$vector_score")
                                .append("fulltext_score", "$fulltext_score")
                                .append("combined_score", new Document("$add", Arrays.asList(
                                        new Document("$multiply", Arrays.asList(vectorWeight, "$vector_score")),
                                        new Document("$multiply", Arrays.asList(fullTextWeight, "$fulltext_score"))
                                )))
                )))
        );

        // Stage 6: Filter Stage
        Document filterDoc = new Document();
        Document filterStage = new Document("$match", filterDoc);
        if (Boolean.TRUE.equals(filter.getOnlyEnabled())) filterDoc.append("enabled", true);
        if (Boolean.TRUE.equals(filter.getOnlyLatest())) filterDoc.append("isLatest", true);
        if (filter.getTypes() != null && !filter.getTypes().isEmpty()) {
            List<String> typeStrings = filter.getTypes().stream().map(Enum::name).collect(Collectors.toList());
            filterDoc.append("type", new Document("$in", typeStrings));
        }

        // Stage 7: Remove search metadata fields
        Document unsetStage = new Document("$unset", Arrays.asList("search_score", "search_type"));

        // Stage 8: Sort by combined score
        Document sortStage = new Document("$sort", new Document("combined_score", -1));

        // Stage 9: Limit final results
        Document limitStage = new Document("$limit", finalLimit);

        return Arrays.asList(
                vectorSearchStage,
                addVectorMetaStage,
                unionWithStage,
                groupByIdStage,
                replaceRootStage,
                filterStage,
                unsetStage,
                sortStage,
                limitStage
        );
    }


    /**
     * Execute an Atlas Vector Search (Semantic Search) in the Node Metamodel Repository
     * @param input The input query
     * @return Returns a list of matching meta-models
     */
    public List<NodeMetamodel> performSemanticSearch(String input) {
        if (input == null || input.trim().isEmpty()) return Collections.emptyList();


        try {
            List<Double> userInputEmbedding = embeddingService.generateEmbedding(input);
            if (userInputEmbedding == null || userInputEmbedding.isEmpty()) {
                log.error("Failed to generate embedding for user input.");
                return Collections.emptyList();
            }

            Document vectorSearchStage = new Document("$vectorSearch", new Document()
                    .append("queryVector", userInputEmbedding)
                    .append("path", vectorSearchFieldName)
                    .append("numCandidates", numCandidates)
                    .append("limit", finalLimit)
                    .append("index", vectorSearchIndexName)
            );

            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.stage(Document.parse(vectorSearchStage.toJson()))
            );

            AggregationResults<NodeMetamodel> results = mongoTemplate.aggregate(
                    aggregation,
                    collectionName,
                    NodeMetamodel.class
            );

            return results.getMappedResults();
        } catch (Exception e) {
            log.error("Error during semantic search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    /**
     * Execute a Full-Text Atlas Search in the Node Metamodel Repository
     * @param input The input query
     * @return Returns a list of matching meta-models
     */
    public List<NodeMetamodel> performFullTextSearch(String input) {
        if (input == null || input.trim().isEmpty()) return Collections.emptyList();

        try {
            Document searchStage = new Document("$search", new Document()
                    .append("index", fullTextSearchIndexName)
                    .append("compound", new Document()
                            .append("should", List.of(
                                    new Document("text", new Document()
                                            .append("query", input)
                                            .append("path", Arrays.asList("name", "description", "qualitativeDescriptor._all_text"))
                                            .append("fuzzy", new Document("maxEdits", 1))
                                    )
                            ))
                    )
            );

            Document limitStage = new Document("$limit", finalLimit);

            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.stage(Document.parse(searchStage.toJson())),
                    Aggregation.stage(Document.parse(limitStage.toJson()))
            );

            AggregationResults<NodeMetamodel> results = mongoTemplate.aggregate(
                    aggregation,
                    collectionName,
                    NodeMetamodel.class
            );

            return results.getMappedResults();
        } catch (Exception e) {
            log.error("Error during full-text search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    @Data
    @AllArgsConstructor
    public static class NodeSearchResult {
        private NodeMetamodel node;
        private Number combinedScore;
        private Number vectorScore;
        private Number fulltextScore;
    }

    @Data
    public static class NodeSearchFilter {
        private List<NodeMetamodel.NodeType> types;
        private Boolean onlyEnabled = true;
        private Boolean onlyLatest = true;
    }
}