package com.kyc.ai.controller;

import com.kyc.ai.entity.AuditLog;
import com.kyc.ai.entity.KnowledgeBase;
import com.kyc.ai.service.GdprService;
import com.kyc.ai.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Knowledge Base", description = "APIs for managing the AI Knowledge Base")
@SecurityRequirement(name = "bearerAuth")
public class RagController {

    private final RagService ragService;
    private final GdprService gdprService;

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ingest regulatory document", description = "Upload a regulatory document (PDF, TXT) to the knowledge base")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponse(responseCode = "200", description = "Document ingested successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file or PII detected")
    public ResponseEntity<Map<String, String>> ingestDocument(
            @Parameter(description = "Regulatory document file") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Document category") @RequestParam("category") KnowledgeBase.Category category,
            @Parameter(description = "Document title") @RequestParam("title") String title,
            @Parameter(description = "Document version") @RequestParam("version") String version,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received request to ingest document: {} (User: {})", title, userDetails.getUsername());

        try {
            ragService.ingestRegulatoryDocument(file, category, title, version, userDetails.getUsername());

            gdprService.logDataAccess(
                    userDetails.getUsername(),
                    AuditLog.AuditAction.CREATE,
                    AuditLog.LegalBasis.LEGITIMATE_INTEREST,
                    "KNOWLEDGE_BASE",
                    new String[] { "REGULATORY_DOC" },
                    true,
                    String.format("{\"title\": \"%s\", \"category\": \"%s\"}", title, category));

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Document ingested successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Ingestion rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Ingestion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ERROR",
                    "message", "Internal server error during ingestion"));
        }
    }
}
