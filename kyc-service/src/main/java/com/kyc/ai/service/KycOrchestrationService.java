package com.kyc.ai.service;

import com.kyc.ai.agent.SupervisorAgent;
import com.kyc.ai.entity.AuditLog;
import com.kyc.ai.entity.KycDocument;
import com.kyc.ai.repository.KycDocumentRepository;
import com.kyc.ai.service.RiskScoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kyc.ai.agent.DocumentAgent;
import com.kyc.ai.util.CountryRiskUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycOrchestrationService {

        private final SupervisorAgent supervisorAgent;
        private final DocumentAnalysisService documentService;
        private final RiskAssessmentService riskService;
        private final RiskScoringService riskScoringService;
        private final GdprService gdprService;
        private final KycDocumentRepository documentRepository;
        private final ObjectMapper objectMapper;

        /**
         * Submit KYC document and orchestrate the verification workflow
         */
        @Transactional
        public KycSubmissionResult submitKyc(String customerId, MultipartFile document,
                        KycDocument.DocumentType docType,
                        KycDocument.LegalBasis legalBasis) {
                log.info("KYC submission received for customer: {}, document type: {}",
                                customerId, docType);

                // 1. Verify consent (GDPR)
                if (!gdprService.hasValidConsent(customerId, "KYC_VERIFICATION")) {
                        gdprService.logDataAccess(
                                        customerId,
                                        AuditLog.AuditAction.CREATE,
                                        AuditLog.LegalBasis.valueOf(legalBasis.name()),
                                        customerId,
                                        new String[] { "DOCUMENT" },
                                        false,
                                        "{\"error\": \"Consent not provided\"}");
                        return new KycSubmissionResult(
                                        "REJECTED",
                                        "Consent required for KYC processing",
                                        null,
                                        null);
                }

                // 2. Route via Supervisor Agent
                SupervisorAgent.RoutingDecision routing = supervisorAgent.routeTask(
                                "DOCUMENT_ANALYSIS",
                                gdprService.hashIdentifier(customerId),
                                "Analyze " + docType + " for KYC verification",
                                legalBasis.name(),
                                0.7, // confidence threshold
                                getPreviousSubmissionCount(customerId),
                                getCurrentStatus(customerId),
                                List.of() // risk indicators
                );

                if (!routing.privacyChecksPassed()) {
                        return new KycSubmissionResult(
                                        "REJECTED",
                                        "Privacy check failed - unable to process",
                                        null,
                                        null);
                }

                // 3. Process document
                KycDocument processedDoc = documentService.processDocument(
                                customerId, document, docType, legalBasis);

                // 4. If document verified, trigger risk assessment
                RiskAssessmentService.RiskSummary riskSummary = null;
                if (processedDoc.getVerificationStatus() == KycDocument.VerificationStatus.VERIFIED) {
                        riskSummary = performRiskAssessment(customerId, processedDoc);
                }

                // 5. Determine overall KYC status
                String overallStatus = determineOverallKycStatus(customerId);

                return new KycSubmissionResult(
                                "SUCCESS",
                                "Document processed successfully",
                                processedDoc.getId(),
                                new KycStatus(
                                                processedDoc.getVerificationStatus().name(),
                                                processedDoc.getRiskLevel() != null ? processedDoc.getRiskLevel().name()
                                                                : "PENDING",
                                                processedDoc.getConfidenceScore(),
                                                overallStatus,
                                                extractFindings(processedDoc)));
        }

        /**
         * Get KYC status for customer
         */
        @Transactional(readOnly = true)
        public KycStatus getKycStatus(String customerId) {
                List<KycDocument> documents = documentRepository.findByCustomerId(customerId);

                if (documents.isEmpty()) {
                        return new KycStatus("NO_DOCUMENTS", "PENDING", 0.0, "INCOMPLETE", List.of());
                }

                // Count documents by status
                long verifiedCount = documents.stream()
                                .filter(d -> d.getVerificationStatus() == KycDocument.VerificationStatus.VERIFIED)
                                .count();
                long pendingCount = documents.stream()
                                .filter(d -> d.getVerificationStatus() == KycDocument.VerificationStatus.PENDING ||
                                                d.getVerificationStatus() == KycDocument.VerificationStatus.IN_PROGRESS)
                                .count();
                long rejectedCount = documents.stream()
                                .filter(d -> d.getVerificationStatus() == KycDocument.VerificationStatus.REJECTED)
                                .count();

                // Determine highest risk level
                KycDocument.RiskLevel highestRisk = documents.stream()
                                .map(KycDocument::getRiskLevel)
                                .filter(Objects::nonNull)
                                .max(java.util.Comparator.naturalOrder())
                                .orElse(KycDocument.RiskLevel.LOW);

                // Calculate average confidence
                double avgConfidence = documents.stream()
                                .mapToDouble(KycDocument::getConfidenceScore)
                                .filter(score -> score != -1.0)
                                .average()
                                .orElse(0.0);

                String overallStatus = determineOverallKycStatus(customerId);

                return new KycStatus(
                                verifiedCount + " verified, " + pendingCount + " pending, " + rejectedCount
                                                + " rejected",
                                highestRisk.name(),
                                avgConfidence,
                                overallStatus,
                                documents.stream().flatMap(d -> extractFindings(d).stream()).toList());
        }

        public List<String> extractFindings(KycDocument doc) {
                if (doc.getMetadata() == null || doc.getMetadata().isEmpty()) {
                        return List.of();
                }
                try {
                        Map<String, Object> metadata = objectMapper.readValue(doc.getMetadata(), Map.class);
                        Object findings = metadata.get("findings");
                        if (findings instanceof List) {
                                return (List<String>) findings;
                        }
                } catch (Exception e) {
                        log.warn("Failed to extract findings from metadata for document: {}", doc.getId());
                }
                return List.of();
        }

        /**
         * Perform risk assessment based on verified document
         */
        private RiskAssessmentService.RiskSummary performRiskAssessment(String customerId,
                        KycDocument document) {
                log.info("Performing expert risk assessment for customer: {} based on document: {}",
                                customerId, document.getId());

                try {
                        // 1. Extract and parse data from document
                        String extractedDataJson = document.getExtractedData();
                        if (extractedDataJson == null || extractedDataJson.isEmpty()) {
                                log.warn("No extracted data found for document: {}", document.getId());
                                return riskService.getRiskSummary(customerId);
                        }

                        // Use a Map for flexible parsing of ExtractedData
                        Map<String, Object> data = objectMapper.readValue(extractedDataJson,
                                        new TypeReference<Map<String, Object>>() {
                                        });

                        // Parse address if present
                        Map<String, String> address = (Map<String, String>) data.getOrDefault("address",
                                        Collections.emptyMap());

                        // 2. Identify Geographic Risks using expert utility
                        String nationality = (String) data.get("nationality");
                        String residenceCountry = address.get("country");

                        String nationalityRisk = CountryRiskUtil.getNationalityRisk(nationality);
                        String residenceRisk = CountryRiskUtil.getResidenceRisk(residenceCountry);
                        String fatfStatus = CountryRiskUtil.getFatfStatus(residenceCountry);

                        // 3. Extract unusual patterns from the document metadata
                        List<String> findings = extractFindings(document);
                        List<String> unusualPatterns = new ArrayList<>(findings);

                        // Add country risk reasons to findings if applicable
                        String natReason = CountryRiskUtil.getCountryRiskReason(nationality);
                        if (natReason != null)
                                unusualPatterns.add("Nationality Risk: " + natReason);

                        String resReason = CountryRiskUtil.getCountryRiskReason(residenceCountry);
                        if (resReason != null)
                                unusualPatterns.add("Residence Risk: " + resReason);

                        // 4. Build expert risk data
                        RiskAssessmentService.CustomerRiskData riskData = new RiskAssessmentService.CustomerRiskData(
                                        nationality != null ? nationality : "UNKNOWN",
                                        residenceCountry != null ? residenceCountry : "UNKNOWN",
                                        "NOT_SPECIFIED", // occupation
                                        "NOT_SPECIFIED", // industry
                                        "NOT_SPECIFIED", // income
                                        "NOT_SPECIFIED", // source of wealth
                                        false, // PEP (Requires external check)
                                        null, // PEP level
                                        0, // adverse media
                                        "CRITICAL".equals(nationalityRisk), // sanctions indicator
                                        false, // previous SAR
                                        nationalityRisk,
                                        residenceRisk,
                                        fatfStatus,
                                        null, // business type
                                        null, // years
                                        false, // complex ownership
                                        false, // cash intensive
                                        0, // account age
                                        "0", // volume
                                        unusualPatterns);

                        // 5. Trigger AI Risk assessment
                        var riskResult = riskService.assessCustomerRisk(customerId, riskData);

                        // 6. Calculate Advanced Risk Score (Quantitative 4-Factor Model)
                        var advancedRisk = riskScoringService.calculateRiskScore(
                                        customerId,
                                        nationality,
                                        residenceCountry,
                                        riskData.pepStatus(),
                                        List.of() // Products would be fetched here
                        );

                        // 7. Persist findings to document metadata
                        updateDocumentWithRiskFindings(document, riskResult, unusualPatterns, advancedRisk);

                        return riskService.getRiskSummary(customerId);

                } catch (Exception e) {
                        log.error("Expert risk assessment failed for customer: {}", customerId, e);
                        // Fallback to basic summary if refactored logic fails
                        return riskService.getRiskSummary(customerId);
                }
        }

        /**
         * Determine overall KYC status based on all documents
         */
        private String determineOverallKycStatus(String customerId) {
                List<KycDocument> documents = documentRepository.findByCustomerId(customerId);

                // Check for required document types
                boolean hasId = hasDocumentType(documents, KycDocument.DocumentType.ID_CARD) ||
                                hasDocumentType(documents, KycDocument.DocumentType.PASSPORT);
                boolean hasAddress = hasDocumentType(documents, KycDocument.DocumentType.PROOF_OF_ADDRESS) ||
                                hasDocumentType(documents, KycDocument.DocumentType.UTILITY_BILL);

                if (!hasId || !hasAddress) {
                        return "INCOMPLETE";
                }

                // Check if any documents need review
                boolean needsReview = documents.stream()
                                .anyMatch(d -> d.getVerificationStatus() == KycDocument.VerificationStatus.NEEDS_REVIEW);
                if (needsReview) {
                        return "UNDER_REVIEW";
                }

                // Check if any documents are rejected
                boolean hasRejected = documents.stream()
                                .anyMatch(d -> d.getVerificationStatus() == KycDocument.VerificationStatus.REJECTED);
                if (hasRejected) {
                        return "REJECTED";
                }

                // Check if all required documents are verified
                boolean allVerified = hasVerifiedDocument(documents, KycDocument.DocumentType.ID_CARD) ||
                                hasVerifiedDocument(documents, KycDocument.DocumentType.PASSPORT);
                boolean addressVerified = hasVerifiedDocument(documents, KycDocument.DocumentType.PROOF_OF_ADDRESS) ||
                                hasVerifiedDocument(documents, KycDocument.DocumentType.UTILITY_BILL);

                if (allVerified && addressVerified) {
                        // Check risk level
                        KycDocument.RiskLevel risk = documents.stream()
                                        .map(KycDocument::getRiskLevel)
                                        .filter(r -> r != null)
                                        .max(java.util.Comparator.naturalOrder())
                                        .orElse(KycDocument.RiskLevel.LOW);

                        if (risk == KycDocument.RiskLevel.CRITICAL) {
                                return "APPROVED_WITH_RESTRICTIONS";
                        }
                        return "APPROVED";
                }

                return "PENDING";
        }

        private boolean hasDocumentType(List<KycDocument> documents, KycDocument.DocumentType type) {
                return documents.stream().anyMatch(d -> d.getDocumentType() == type);
        }

        private boolean hasVerifiedDocument(List<KycDocument> documents, KycDocument.DocumentType type) {
                return documents.stream()
                                .anyMatch(d -> d.getDocumentType() == type &&
                                                d.getVerificationStatus() == KycDocument.VerificationStatus.VERIFIED);
        }

        private int getPreviousSubmissionCount(String customerId) {
                return documentRepository.findByCustomerId(customerId).size();
        }

        private String getCurrentStatus(String customerId) {
                return determineOverallKycStatus(customerId);
        }

        // Records
        public record KycSubmissionResult(
                        String status,
                        String message,
                        UUID documentId,
                        KycStatus kycStatus) {
        }

        private void updateDocumentWithRiskFindings(KycDocument document,
                        Object riskResultObj,
                        List<String> existingPatterns,
                        RiskScoringService.RiskScoreResult advancedRisk) {
                try {
                        // We use Object to avoid circular dependency issues if RiskAgent is not fully
                        // visible
                        // But we know it's a specific record, so we can access fields via reflection or
                        // casting if visible
                        // Ideally we cast to RiskAgent.RiskAssessmentResult
                        com.kyc.ai.agent.RiskAgent.RiskAssessmentResult result = (com.kyc.ai.agent.RiskAgent.RiskAssessmentResult) riskResultObj;

                        List<String> allFindings = new ArrayList<>();

                        // Add existing patterns (nationality risk, etc)
                        if (existingPatterns != null) {
                                allFindings.addAll(existingPatterns);
                        }

                        // Add Advanced Risk Score details
                        if (advancedRisk != null) {
                                allFindings.add(String.format("Risk Score: %d (%s)", advancedRisk.totalScore(),
                                                advancedRisk.riskLevel()));
                                allFindings.add(String.format("Factors: Cust=%d, Geo=%d, Prod=%d, Tx=%d",
                                                advancedRisk.customerScore(), advancedRisk.geoScore(),
                                                advancedRisk.productScore(), advancedRisk.transactionScore()));
                        }

                        // Add high severity risk factors
                        if (result.riskFactors() != null) {
                                result.riskFactors().stream()
                                                .filter(f -> f.severity() == com.kyc.ai.agent.RiskAgent.Severity.HIGH
                                                                || f.severity() == com.kyc.ai.agent.RiskAgent.Severity.MEDIUM)
                                                .forEach(f -> allFindings.add("Risk Factor: " + f.factor() + " ("
                                                                + f.severity() + ")"));
                        }

                        // Add decision rationale if risk is high
                        if (result.gdprCompliance() != null && result.gdprCompliance().decisionRationale() != null) {
                                allFindings.add("Assessment Rationale: " + result.gdprCompliance().decisionRationale());
                        }

                        // Update metadata
                        Map<String, Object> metadata = new HashMap<>();
                        if (document.getMetadata() != null && !document.getMetadata().isEmpty()) {
                                try {
                                        metadata = objectMapper.readValue(document.getMetadata(),
                                                        new TypeReference<Map<String, Object>>() {
                                                        });
                                } catch (Exception e) {
                                        // ignore
                                }
                        }

                        metadata.put("findings", allFindings);
                        document.setMetadata(objectMapper.writeValueAsString(metadata));

                        // Re-save document to persist metadata
                        documentRepository.save(document);

                } catch (Exception e) {
                        log.error("Failed to update document with risk findings", e);
                }
        }

        public record KycStatus(
                        String documentStatus,
                        String riskLevel,
                        Double confidenceScore,
                        String overallStatus,
                        List<String> findings) {
        }
}
