package com.kyc.ai.service;

import com.kyc.ai.agent.RiskAgent;
import com.kyc.ai.entity.AuditLog;
import com.kyc.ai.entity.KycDocument;
import com.kyc.ai.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private final RiskAgent riskAgent;
    private final GdprService gdprService;
    private final KycDocumentRepository documentRepository;

    /**
     * Perform risk assessment for a customer
     */
    @Transactional
    public RiskAgent.RiskAssessmentResult assessCustomerRisk(String customerId,
                                                             CustomerRiskData riskData) {
        log.info("Performing risk assessment for customer: {}", customerId);

        try {
            // Call AI risk agent
            RiskAgent.RiskAssessmentResult result = riskAgent.assessRisk(
                    gdprService.hashIdentifier(customerId),
                    riskData.nationality(),
                    riskData.residenceCountry(),
                    riskData.occupation(),
                    riskData.industrySector(),
                    riskData.incomeRange(),
                    riskData.sourceOfWealth(),
                    riskData.pepStatus(),
                    riskData.pepLevel(),
                    riskData.adverseMediaCount(),
                    riskData.sanctionsMatch(),
                    riskData.previousSar(),
                    riskData.nationalityRisk(),
                    riskData.residenceRisk(),
                    riskData.fatfStatus(),
                    riskData.businessType(),
                    riskData.yearsInOperation(),
                    riskData.complexOwnership(),
                    riskData.cashIntensive(),
                    riskData.accountAge(),
                    riskData.avgMonthlyVolume(),
                    riskData.unusualPatterns());

            // Update all customer documents with risk level
            List<KycDocument> documents = documentRepository.findByCustomerId(customerId);
            for (KycDocument doc : documents) {
                doc.setRiskLevel(KycDocument.RiskLevel.valueOf(result.riskLevel().name()));
            }
            documentRepository.saveAll(documents);

            // Log the assessment
            gdprService.logDataAccess(
                    customerId,
                    AuditLog.AuditAction.PROCESS,
                    AuditLog.LegalBasis.LEGAL_OBLIGATION,
                    "RISK_AGENT",
                    new String[] { "RISK_SCORE", "AML_DATA" },
                    true,
                    String.format("{\"riskLevel\": \"%s\", \"riskScore\": %d}",
                            result.riskLevel(), result.riskScore()));

            return result;

        } catch (Exception e) {
            log.error("Risk assessment failed", e);
            gdprService.logDataAccess(
                    customerId,
                    AuditLog.AuditAction.PROCESS,
                    AuditLog.LegalBasis.LEGAL_OBLIGATION,
                    "RISK_AGENT",
                    new String[] { "RISK_SCORE" },
                    false,
                    "{\"error\": \"" + e.getMessage() + "\"}");
            throw new RuntimeException("Risk assessment failed", e);
        }
    }

    /**
     * Record for customer risk data
     */
    public record CustomerRiskData(
            String nationality,
            String residenceCountry,
            String occupation,
            String industrySector,
            String incomeRange,
            String sourceOfWealth,
            boolean pepStatus,
            String pepLevel,
            int adverseMediaCount,
            boolean sanctionsMatch,
            boolean previousSar,
            String nationalityRisk,
            String residenceRisk,
            String fatfStatus,
            String businessType,
            Integer yearsInOperation,
            boolean complexOwnership,
            boolean cashIntensive,
            Integer accountAge,
            String avgMonthlyVolume,
            List<String> unusualPatterns) {
    }

    /**
     * Get risk summary for customer
     */
    @Transactional(readOnly = true)
    public RiskSummary getRiskSummary(String customerId) {
        List<KycDocument> documents = documentRepository.findByCustomerId(customerId);

        if (documents.isEmpty()) {
            return new RiskSummary(customerId, null, 0, "NO_DATA");
        }

        KycDocument.RiskLevel highestRisk = documents.stream()
                .map(KycDocument::getRiskLevel)
                .filter(r -> r != null)
                .max(java.util.Comparator.naturalOrder())
                .orElse(null);

        long verifiedCount = documents.stream()
                .filter(d -> d.getVerificationStatus() == KycDocument.VerificationStatus.VERIFIED)
                .count();

        return new RiskSummary(
                customerId,
                highestRisk,
                verifiedCount,
                documents.get(0).getVerificationStatus().name());
    }

    public record RiskSummary(
            String customerId,
            KycDocument.RiskLevel riskLevel,
            long verifiedDocuments,
            String overallStatus) {
    }
}
