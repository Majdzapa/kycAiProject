package com.kyc.ai.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class LangChain4jConfig {

        @Value("${spring.datasource.url}")
        private String datasourceUrl;

        @Value("${spring.datasource.username}")
        private String datasourceUsername;

        @Value("${spring.datasource.password}")
        private String datasourcePassword;

        @Value("${langchain4j.ollama.base-url}")
        private String ollamaBaseUrl;

        // ================== Chat Models ==================

        @Bean
        public ChatLanguageModel chatLanguageModel() {
                log.info("Initializing Ollama Chat Model at: {}", ollamaBaseUrl);
                return OllamaChatModel.builder()
                                .baseUrl(ollamaBaseUrl)
                                .modelName("llama3.2")
                                .temperature(0.7)
                                .timeout(Duration.ofSeconds(300))
                                .build();
        }

        // @Bean
        // @Qualifier("visionModel")
        // public ChatLanguageModel visionLanguageModel() {
        // log.info("Initializing Ollama Vision Model at: {}", ollamaBaseUrl);
        // return OllamaChatModel.builder()
        // .baseUrl(ollamaBaseUrl)
        // .modelName("llava-phi3")
        // .temperature(0.5)
        // .timeout(Duration.ofSeconds(180))
        // .build();
        // }

        // ================== Embedding Model ==================

        @Bean
        public EmbeddingModel embeddingModel() {
                log.info("Initializing Ollama Embedding Model at: {}", ollamaBaseUrl);
                return OllamaEmbeddingModel.builder()
                                .baseUrl(ollamaBaseUrl)
                                .modelName("nomic-embed-text")
                                .timeout(Duration.ofSeconds(180))
                                .build();
        }

        // ================== Embedding Stores ==================

        @Bean
        @Qualifier("kycDocuments")
        public EmbeddingStore<TextSegment> embeddingStore() {
                log.info("Initializing PgVector Embedding Store for KYC documents");

                DatabaseConfig dbConfig = parseDatasourceUrl(datasourceUrl);
                log.debug("PgVector Config - Host: {}, Port: {}, Database: {}, Table: kyc_documents",
                                dbConfig.host(), dbConfig.port(), dbConfig.database());

                return PgVectorEmbeddingStore.builder()
                                .host(dbConfig.host())
                                .port(dbConfig.port())
                                .database(dbConfig.database())
                                .user(datasourceUsername)
                                .password(datasourcePassword)
                                .dimension(768) // nomic-embed-text dimension
                                .table("kyc_documents")
                                .createTable(false) // We manage the table manually
                                .build();
        }

        @Bean
        @Qualifier("knowledgeBase")
        public EmbeddingStore<TextSegment> knowledgeBaseStore() {
                log.info("Initializing PgVector Embedding Store for knowledge base");

                DatabaseConfig dbConfig = parseDatasourceUrl(datasourceUrl);
                log.debug("PgVector Config - Host: {}, Port: {}, Database: {}, Table: knowledge_base",
                                dbConfig.host(), dbConfig.port(), dbConfig.database());

                return PgVectorEmbeddingStore.builder()
                                .host(dbConfig.host())
                                .port(dbConfig.port())
                                .database(dbConfig.database())
                                .user(datasourceUsername)
                                .password(datasourcePassword)
                                .dimension(768)
                                .table("knowledge_base")
                                .createTable(false)
                                .build();
        }

        // ================== Content Retriever ==================

        @Bean
        public ContentRetriever contentRetriever(
                        @Qualifier("knowledgeBase") EmbeddingStore<TextSegment> knowledgeBaseStore,
                        EmbeddingModel embeddingModel) {
                log.info("Initializing Content Retriever with maxResults=5, minScore=0.7");

                return EmbeddingStoreContentRetriever.builder()
                                .embeddingStore(knowledgeBaseStore)
                                .embeddingModel(embeddingModel)
                                .maxResults(5)
                                .minScore(0.7)
                                .build();
        }

        // ================== Helper Methods ==================

        private DatabaseConfig parseDatasourceUrl(String jdbcUrl) {
                // Parse: jdbc:postgresql://localhost:5432/kyc_db
                String cleanUrl = jdbcUrl.replace("jdbc:postgresql://", "");
                String[] hostPort = cleanUrl.split("/")[0].split(":");
                String database = cleanUrl.split("/")[1].split("\\?")[0]; // Remove query params if any

                String host = hostPort[0];
                int port = Integer.parseInt(hostPort[1]);

                return new DatabaseConfig(host, port, database);
        }

        private record DatabaseConfig(String host, int port, String database) {
        }
}