package com.kyc.ai.controller;

import com.kyc.ai.entity.KnowledgeBase;
import com.kyc.ai.entity.KycDocument;
import com.kyc.ai.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC Operations", description = "KYC document submission and verification APIs")
@SecurityRequirement(name = "bearerAuth")
public class KycController {

        private final KycOrchestrationService orchestrationService;
        private final DocumentAnalysisService documentService;
        private final RiskAssessmentService riskService;
        private final RagService ragService;
        private final GdprService gdprService;

        @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Submit KYC document", description = "Upload and process a KYC document")
        @PreAuthorize("hasAnyRole('CUSTOMER', 'OPERATOR', 'ADMIN')")
        public ResponseEntity<KycSubmissionResponse> submitKyc(
                        @RequestHeader("X-Customer-Id") @Parameter(description = "Customer ID") String customerId,
                        @RequestHeader("X-Consent-Token") @Parameter(description = "Consent token") String consentToken,
                        @RequestParam("document") @Parameter(description = "Document file") MultipartFile document,
                        @RequestParam("docType") @Parameter(description = "Document type") KycDocument.DocumentType docType,
                        @RequestParam(value = "legalBasis", defaultValue = "LEGAL_OBLIGATION") KycDocument.LegalBasis legalBasis,
                        @AuthenticationPrincipal UserDetails userDetails) {

                log.info("KYC submission received for customer: {}, docType: {}", customerId, docType);

                KycOrchestrationService.KycSubmissionResult result = orchestrationService.submitKyc(
                                customerId, document, docType, legalBasis);

                return ResponseEntity.ok(new KycSubmissionResponse(
                                result.status(),
                                result.message(),
                                result.documentId(),
                                result.kycStatus() != null ? new KycStatusResponse(
                                                result.kycStatus().documentStatus(),
                                                result.kycStatus().riskLevel(),
                                                result.kycStatus().confidenceScore(),
                                                result.kycStatus().overallStatus(),
                                                result.kycStatus().findings()) : null));
        }

        @GetMapping("/status/{customerId}")
        @Operation(summary = "Get KYC status", description = "Get verification status for a customer")
        @PreAuthorize("hasAnyRole('CUSTOMER', 'OPERATOR', 'ADMIN') or #customerId == authentication.name")
        public ResponseEntity<KycStatusResponse> getKycStatus(
                        @PathVariable @Parameter(description = "Customer ID") String customerId) {

                KycOrchestrationService.KycStatus status = orchestrationService.getKycStatus(customerId);

                return ResponseEntity.ok(new KycStatusResponse(
                                status.documentStatus(),
                                status.riskLevel(),
                                status.confidenceScore(),
                                status.overallStatus(),
                                status.findings()));
        }

        @GetMapping("/documents/{customerId}")
        @Operation(summary = "Get customer documents", description = "List all documents for a customer")
        @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
        public ResponseEntity<List<DocumentResponse>> getCustomerDocuments(
                        @PathVariable @Parameter(description = "Customer ID") String customerId) {

                List<KycDocument> documents = documentService.getCustomerDocuments(customerId);

                List<DocumentResponse> response = documents.stream()
                                .map(d -> new DocumentResponse(
                                                d.getId(),
                                                d.getDocumentType().name(),
                                                d.getVerificationStatus().name(),
                                                d.getRiskLevel() != null ? d.getRiskLevel().name() : null,
                                                d.getConfidenceScore(),
                                                d.getCreatedAt(),
                                                d.getProcessedAt(),
                                                orchestrationService.extractFindings(d)))
                                .toList();

                return ResponseEntity.ok(response);
        }

        @PostMapping(value = "/ingest-knowledge", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Ingest regulatory document", description = "Add document to RAG knowledge base")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Map<String, String>> ingestKnowledge(
                        @RequestParam("file") @Parameter(description = "Document file") MultipartFile file,
                        @RequestParam("category") @Parameter(description = "Document category") KnowledgeBase.Category category,
                        @RequestParam("title") @Parameter(description = "Document title") String title,
                        @RequestParam("version") @Parameter(description = "Document version") String version,
                        @AuthenticationPrincipal UserDetails userDetails) {

                ragService.ingestRegulatoryDocument(file, category, title, version, userDetails.getUsername());

                return ResponseEntity.ok(Map.of(
                                "status", "SUCCESS",
                                "message", "Document ingested successfully",
                                "title", title,
                                "category", category.name()));
        }

        @GetMapping("/knowledge/search")
        @Operation(summary = "Search knowledge base", description = "Search regulatory knowledge base")
        @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
        public ResponseEntity<List<RagService.RetrievedContext>> searchKnowledge(
                        @RequestParam("query") String query,
                        @RequestParam(value = "maxResults", defaultValue = "5") int maxResults) {

                List<RagService.RetrievedContext> results = ragService.retrieveRelevantContext(query, maxResults);
                return ResponseEntity.ok(results);
        }

        @PostMapping("/risk-assessment/{customerId}")
        @Operation(summary = "Trigger risk assessment", description = "Perform AML risk assessment")
        @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
        public ResponseEntity<RiskAssessmentResponse> assessRisk(
                        @PathVariable String customerId,
                        @RequestBody @Valid RiskAssessmentRequest request) {

                RiskAssessmentService.RiskSummary summary = riskService.getRiskSummary(customerId);

                return ResponseEntity.ok(new RiskAssessmentResponse(
                                summary.customerId(),
                                summary.riskLevel() != null ? summary.riskLevel().name() : "UNKNOWN",
                                summary.verifiedDocuments(),
                                summary.overallStatus()));
        }

        // DTOs
        public record KycSubmissionResponse(
                        String status,
                        String message,
                        UUID documentId,
                        KycStatusResponse kycStatus) {
        }

        public record KycStatusResponse(
                        String documentStatus,
                        String riskLevel,
                        Double confidenceScore,
                        String overallStatus,
                        List<String> findings) {
        }

        public record DocumentResponse(
                        UUID id,
                        String documentType,
                        String verificationStatus,
                        String riskLevel,
                        Double confidenceScore,
                        java.time.LocalDateTime createdAt,
                        java.time.LocalDateTime processedAt,
                        List<String> findings) {
        }

        public record RiskAssessmentRequest(
                        String nationality,
                        String residenceCountry,
                        String occupation,
                        boolean pepStatus,
                        int adverseMediaCount) {
        }

        public record RiskAssessmentResponse(
                        String customerId,
                        String riskLevel,
                        long verifiedDocuments,
                        String overallStatus) {
        }
}
