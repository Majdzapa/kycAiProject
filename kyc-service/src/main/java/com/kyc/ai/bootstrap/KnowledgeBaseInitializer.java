package com.kyc.ai.bootstrap;

import com.kyc.ai.entity.KnowledgeBase;
import com.kyc.ai.repository.KnowledgeBaseRepository;
import com.kyc.ai.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Bootstrap the Knowledge Base with essential KYC/AML content on startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseInitializer implements CommandLineRunner {

    private final RagService ragService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    @Override
    public void run(String... args) {
        if (knowledgeBaseRepository.count() > 0) {
            log.info("Knowledge Base already populated. Skipping initialization.");
            return;
        }

        log.info("Initializing Knowledge Base with default regulatory content...");

        try {
            // 1. FATF 40 Recommendations (Summary)
            String fatfContent = """
                    The FATF Recommendations are the comprehensive international standards on combating money laundering and terrorist financing.

                    Key Recommendations for KYC/CDD:
                    1. Customer Due Diligence (Recommendation 10): Financial institutions must identify and verify customers when establishing business relations, carrying out occasional transactions above USD/EUR 15,000, or when there is suspicion of ML/TF.
                    2. Record Keeping (Recommendation 11): Maintain all necessary records on transactions and CDD for at least 5 years.
                    3. PEPs (Recommendation 12): Implement appropriate risk management systems for Politically Exposed Persons, including senior management approval and source of wealth verification.
                    4. Reliance on Third Parties (Recommendation 17): Institutions may rely on third parties for CDD but remain ultimately responsible.
                    5. High-Risk Countries (Recommendation 19): Apply enhanced due diligence to business relationships and transactions with natural and legal persons from countries for which this is called for by the FATF.
                    """;
            ragService.ingestRegulatoryContent(fatfContent, KnowledgeBase.Category.REGULATION,
                    "FATF 40 Recommendations (KYC Highlight)", "2023 Update", "SYSTEM_BOOTSTRAP");

            // 2. EU 5th AML Directive (5AMLD)
            String amld5Content = """
                    The 5th Anti-Money Laundering Directive (5AMLD) entered into force on 10 January 2020.

                    Key Changes affecting KYC:
                    1. Virtual Currencies: Cryptocurrency exchanges and custodian wallet providers are now "obliged entities" subject to KYC/AML rules.
                    2. Prepaid Cards: The threshold for anonymous prepaid card verification was lowered to EUR 150.
                    3. Beneficial Ownership: Member states must maintain public central registers of beneficial owners of corporate and other legal entities.
                    4. High-Risk Third Countries: Enhanced due diligence is mandatory for checks on flows from high-risk third countries.
                    5. PEP Lists: Member states must issue functional PEP lists (roles considered prominent public functions).
                    """;
            ragService.ingestRegulatoryContent(amld5Content, KnowledgeBase.Category.REGULATION,
                    "EU 5th AML Directive Summary", "2018/843", "SYSTEM_BOOTSTRAP");

            // 3. Customer Due Diligence (CDD) Procedures
            String cddContent = """
                    Standard Customer Due Diligence (CDD) Procedure:

                    1. Identification: Obtain customer's full name, address, and date of birth.
                    2. Verification: Verify identity using reliable, independent source documents (e.g., Passport, ID Card).
                       - For documents: Check validity, photo match, and security features.
                       - For addresses: Verify via utility bill or bank statement (< 3 months old).
                    3. Nature of Business: Understand the purpose and intended nature of the business relationship.
                    4. Beneficial Ownership: Identify and verify beneficial owners (>25% ownership or control).

                    Enhanced Due Diligence (EDD) is required for:
                    - Customers from high-risk jurisdictions.
                    - Politically Exposed Persons (PEPs).
                    - Complex ownership structures.
                    - Non-face-to-face business relationships without electronic safeguards.
                    """;
            ragService.ingestRegulatoryContent(cddContent, KnowledgeBase.Category.PROCEDURE,
                    "Standard CDD and EDD Procedures", "v1.0", "SYSTEM_BOOTSTRAP");

            // 4. Red Flag Indicators
            String redFlagsContent = """
                    KYC/AML Red Flag Indicators (Suspicious Activity):

                    Customer Behavior:
                    - Reluctance to provide information or identification.
                    - Attempting to avoid reporting thresholds (structuring/smurfing).
                    - Transactions inconsistent with the customer's known profile or business.
                    - Use of nominees or straw men to conceal beneficial ownership.

                    Document Anomalies:
                    - Documents appearing altered, forged, or photocopied.
                    - Information on document inconsistent with other details provided.
                    - Inconsistent fonts or alignment on physical documents.

                    Geographic Risks:
                    - Funds originating from or destined to high-risk jurisdictions (tax havens, sanctioned countries).
                    - Complex cross-border structures with no apparent economic rationale.
                    """;
            ragService.ingestRegulatoryContent(redFlagsContent, KnowledgeBase.Category.GUIDELINE,
                    "AML Red Flag Indicators", "v2.0", "SYSTEM_BOOTSTRAP");

            // 5. GDPR Privacy Policy for KYC
            String gdprContent = """
                    Privacy Policy for KYC Data Processing (GDPR Compliance):

                    1. Legal Basis: Art. 6(1)(c) GDPR - Processing is necessary for compliance with a legal obligation (AML/CFT Laws).
                    2. Data Minimization: We collect only data strictly necessary for identity verification and risk assessment.
                    3. Retention Period: KYC data is retained for 5 years after the end of the business relationship, as required by AML laws.
                    4. Data Subject Rights:
                       - Right to access personal data.
                       - Right to rectification of inaccurate data.
                       - Note: Right to erasure (Right to be Forgotten) is limited due to legal retention obligations.
                    5. Automated Decision Making: Customers have the right to request human intervention for automated decisions that significantly affect them (e.g., account rejection).
                    """;
            ragService.ingestRegulatoryContent(gdprContent, KnowledgeBase.Category.POLICY,
                    "KYC Data Privacy Policy", "2024-A", "SYSTEM_BOOTSTRAP");

            log.info("Knowledge Base initialization completed successfully.");

        } catch (Exception e) {
            log.error("Failed to initialize Knowledge Base", e);
        }
    }
}
