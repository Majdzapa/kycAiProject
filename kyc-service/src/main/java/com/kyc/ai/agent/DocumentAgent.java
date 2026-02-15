package com.kyc.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;
import java.util.Map;

/**
 * Document Agent - Analyzes identity documents for KYC verification
 * Extracts structured information while ensuring GDPR compliance
 */
@AiService
public interface DocumentAgent {

  @SystemMessage("""
      You are a Document Analysis Agent for KYC (Know Your Customer) verification.

      Your task: Analyze identity documents and extract structured information with confidence scores.

      GDPR Compliance Rules:
      1. Process only necessary data (data minimization principle)
      2. Never store full document numbers in plain text - always hash or tokenize
      3. Report confidence scores for each extraction field
      4. Flag any validation warnings or suspicious patterns
      5. Document processing purpose and legal basis

      Document types handled:
      - National ID cards (passport cards, national identity cards)
      - Passports (all countries, machine-readable zone)
      - Driver's licenses
      - Proof of address (utility bills, bank statements, government correspondence)

      Extraction Guidelines:
      - Full name: Extract complete name as shown on document
      - Date of birth: Normalize to ISO format (YYYY-MM-DD)
      - Document number: Extract but mark for hashing (never store plain)
      - Expiry date: Normalize to ISO format, flag if expired
      - Issuing authority: Full name of issuing organization
      - Nationality: ISO country code if possible
      - Address: For proof of address documents only

      Validation Rules:
      - Check document expiry dates
      - Verify document number format matches document type
      - Cross-reference extracted data for consistency
      - Flag any signs of tampering or forgery

      Output: Structured JSON with extracted data, confidence scores, and GDPR metadata
      """)

  @UserMessage("""
      Analyze this document for KYC verification:

      Document type: {{docType}}
      Document country: {{country}}
      Extracted text (OCR): {{ocrText}}
      Customer reference (pseudonymized): {{customerRef}}
      Processing legal basis: {{legalBasis}}

      Extract and return the following information:

      1. Personal Information:
         - Full name (exactly as on document)
         - Date of birth (YYYY-MM-DD format)
         - Nationality (ISO country code)
         - Gender (if available)

      2. Document Information:
         - Document number (to be hashed - mark as sensitive)
         - Document type (specific subtype)
         - Issue date (YYYY-MM-DD)
         - Expiry date (YYYY-MM-DD)
         - Issuing authority/organization
         - Place of issue (if available)

      3. Address Information (for proof of address only):
         - Street address
         - City
         - Postal/ZIP code
         - Country

      Return JSON format:
      {
        "extractedData": {
          "fullName": "...",
          "dateOfBirth": "YYYY-MM-DD",
          "nationality": "...",
          "gender": "...",
          "documentNumberHash": "...",
          "documentType": "...",
          "issueDate": "YYYY-MM-DD",
          "expiryDate": "YYYY-MM-DD",
          "issuingAuthority": "...",
          "placeOfIssue": "...",
          "address": {
            "street": "...",
            "city": "...",
            "postalCode": "...",
            "country": "..."
          }
        },
        "confidenceScores": {
          "fullName": 0.0-1.0,
          "dateOfBirth": 0.0-1.0,
          "documentNumber": 0.0-1.0,
          "overall": 0.0-1.0
        },
        "validationResults": {
          "documentValid": true|false,
          "notExpired": true|false,
          "formatValid": true|false,
          "suspiciousPatterns": ["..."]
        },
        "validationWarnings": ["..."],
        "gdprMetadata": {
          "dataMinimized": true,
          "sensitiveFieldsHashed": ["documentNumber"],
          "retentionDays": 90,
          "processingPurpose": "KYC_VERIFICATION",
          "legalBasis": "LEGAL_OBLIGATION"
        }
      }
      """)
  DocumentAnalysisResult analyzeDocument(
      @V("docType") String docType,
      @V("country") String country,
      @V("ocrText") String ocrText,
      @V("customerRef") String customerRef,
      @V("legalBasis") String legalBasis);

  record DocumentAnalysisResult(
      ExtractedData extractedData,
      ConfidenceScores confidenceScores,
      ValidationResults validationResults,
      List<String> validationWarnings,
      GdprMetadata gdprMetadata) {
  }

  record ExtractedData(
      String fullName,
      String dateOfBirth,
      String nationality,
      String gender,
      String documentNumberHash,
      String documentType,
      String issueDate,
      String expiryDate,
      String issuingAuthority,
      String placeOfIssue,
      Address address) {
  }

  record Address(
      String street,
      String city,
      String postalCode,
      String country) {
  }

  record ConfidenceScores(
      double fullName,
      double dateOfBirth,
      double documentNumber,
      double nationality,
      double overall) {
  }

  record ValidationResults(
      boolean documentValid,
      boolean notExpired,
      boolean formatValid,
      List<String> suspiciousPatterns) {
  }

  record GdprMetadata(
      boolean dataMinimized,
      List<String> sensitiveFieldsHashed,
      int retentionDays,
      String processingPurpose,
      String legalBasis) {
  }
}
