package com.kyc.ai.service;

import com.kyc.ai.entity.KnowledgeBase;
import com.kyc.ai.repository.KnowledgeBaseRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    @Qualifier("knowledgeBase")
    private final EmbeddingStore<TextSegment> knowledgeBaseStore;

    private final EmbeddingModel embeddingModel;
    private final GdprService gdprService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ContentRetriever contentRetriever;

    /**
     * Ingest regulatory documents into vector store for RAG
     * GDPR: Only store public regulatory info, not personal data
     */
    @Transactional
    public void ingestRegulatoryDocument(MultipartFile file, KnowledgeBase.Category category,
            String title, String version, String ingestedBy) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Validate no PII in document (basic check)
            if (gdprService.containsPotentialPii(content)) {
                throw new IllegalArgumentException(
                        "Document appears to contain PII. Only regulatory docs allowed for RAG ingestion.");
            }

            // Save to database
            KnowledgeBase kbEntry = KnowledgeBase.builder()
                    .category(category)
                    .title(title)
                    .text(content)
                    .version(version)
                    .effectiveDate(LocalDate.now())
                    .ingestedBy(ingestedBy)
                    .build();

            knowledgeBaseRepository.save(kbEntry);

            // Ingest into vector store
            Document document = Document.from(content);
            document.metadata().put("category", category.name());
            document.metadata().put("title", title);
            document.metadata().put("version", version);
            document.metadata().put("source", file.getOriginalFilename());
            document.metadata().put("ingestion_date", LocalDate.now().toString());
            document.metadata().put("knowledge_base_id", kbEntry.getId().toString());

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(knowledgeBaseStore)
                    .build();

            ingestor.ingest(document);

            log.info("Ingested regulatory document: {} (category: {}, version: {})",
                    title, category, version);

        } catch (IOException e) {
            log.error("Failed to ingest document", e);
            throw new RuntimeException("Failed to ingest document", e);
        }
    }

    /**
     * Ingest regulatory content directly from string (for bootstrapping)
     */
    @Transactional
    public void ingestRegulatoryContent(String content, KnowledgeBase.Category category,
            String title, String version, String ingestedBy) {
        try {
            // Validate no PII in document (basic check)
            if (gdprService.containsPotentialPii(content)) {
                throw new IllegalArgumentException(
                        "Content appears to contain PII. Only regulatory docs allowed for RAG ingestion.");
            }

            // Save to database
            KnowledgeBase kbEntry = KnowledgeBase.builder()
                    .category(category)
                    .title(title)
                    .text(content)
                    .version(version)
                    .effectiveDate(LocalDate.now())
                    .ingestedBy(ingestedBy)
                    .build();

            knowledgeBaseRepository.save(kbEntry);

            // Ingest into vector store
            Document document = Document.from(content);
            document.metadata().put("category", category.name());
            document.metadata().put("title", title);
            document.metadata().put("version", version);
            document.metadata().put("source", "SYSTEM_BOOTSTRAP");
            document.metadata().put("ingestion_date", LocalDate.now().toString());
            document.metadata().put("knowledge_base_id", kbEntry.getId().toString());

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(knowledgeBaseStore)
                    .build();

            ingestor.ingest(document);

            log.info("Ingested regulatory content: {} (category: {}, version: {})",
                    title, category, version);

        } catch (Exception e) {
            log.error("Failed to ingest content", e);
            throw new RuntimeException("Failed to ingest content", e);
        }
    }

    /**
     * Retrieve relevant context for RAG based on query
     * Note: Content objects don't have scores. If you need scores,
     * use EmbeddingStore.findRelevant() directly instead of ContentRetriever
     */
    public List<RetrievedContext> retrieveRelevantContext(String query, int maxResults) {
        log.debug("Retrieving context for query: {}", query);

        List<Content> contents = contentRetriever.retrieve(Query.from(query));

        return contents.stream()
                .limit(maxResults)
                .map(content -> {
                    TextSegment segment = content.textSegment();
                    return new RetrievedContext(
                            segment.text(),
                            segment.metadata().getString("category"),
                            segment.metadata().getString("title"),
                            segment.metadata().getString("source"),
                            0.0 // Score not available from ContentRetriever
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieve relevant context WITH SCORES using EmbeddingStore directly
     * This version returns actual relevance scores
     */
    public List<RetrievedContext> retrieveRelevantContextWithScores(String query, int maxResults) {
        log.debug("Retrieving context with scores for query: {}", query);

        // Embed the query
        var queryEmbedding = embeddingModel.embed(query).content();

        // Search in embedding store with scores
        var results = knowledgeBaseStore.findRelevant(queryEmbedding, maxResults, 0.7);

        return results.stream()
                .map(match -> {
                    TextSegment segment = match.embedded();
                    return new RetrievedContext(
                            segment.text(),
                            segment.metadata().getString("category"),
                            segment.metadata().getString("title"),
                            segment.metadata().getString("source"),
                            match.score() // Actual relevance score
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieve context formatted for chatbot use
     */
    public String retrieveContextForChatbot(String query) {
        // Use the version with scores for better context
        List<RetrievedContext> contexts = retrieveRelevantContextWithScores(query, 3);

        if (contexts.isEmpty()) {
            return "No specific regulatory information found for this query.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Relevant regulatory information:\n\n");

        for (int i = 0; i < contexts.size(); i++) {
            RetrievedContext ctx = contexts.get(i);
            sb.append("[").append(i + 1).append("] ")
                    .append(ctx.title()).append(" (").append(ctx.category()).append(")")
                    .append(" - Relevance: ").append(String.format("%.2f%%", ctx.relevanceScore() * 100))
                    .append("\n")
                    .append(ctx.content()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Search knowledge base by category
     */
    public List<KnowledgeBase> searchByCategory(KnowledgeBase.Category category) {
        return knowledgeBaseRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    /**
     * Full-text search in knowledge base
     */
    public List<KnowledgeBase> searchByContent(String searchTerm) {
        return knowledgeBaseRepository.searchByContent(searchTerm);
    }

    /**
     * Get all knowledge base categories
     */
    public List<KnowledgeBase.Category> getAllCategories() {
        return knowledgeBaseRepository.findAllCategories();
    }

    /**
     * Delete knowledge base entry
     */
    @Transactional
    public void deleteKnowledgeBaseEntry(UUID id) {
        knowledgeBaseRepository.deleteById(id);
        log.info("Deleted knowledge base entry: {}", id);
    }

    public record RetrievedContext(
            String content,
            String category,
            String title,
            String source,
            double relevanceScore) {
    }
}