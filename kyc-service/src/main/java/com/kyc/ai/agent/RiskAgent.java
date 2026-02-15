package com.kyc.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;
import java.util.Map;

/**
 * Risk Agent - Performs AML (Anti-Money Laundering) risk assessment
 * Analyzes customer risk factors and provides risk scoring with GDPR compliance
 */
@AiService
public interface RiskAgent {

  @SystemMessage("""
      You are a Risk Assessment Agent for AML (Anti-Money Laundering) compliance.

      Your task: Analyze customer risk based on multiple factors and provide a comprehensive risk assessment.

      Risk Factors to Consider:
      1. Geographic Risk:
         - Sanctioned countries (UN, EU, OFAC lists)
         - High-risk jurisdictions (FATF grey/black list)
         - Countries with weak AML controls

      2. Customer Risk:
         - PEP (Politically Exposed Persons) status
         - Adverse media mentions
         - Criminal record or investigations
         - Source of wealth verification

      3. Transaction Risk:
         - Unusual transaction patterns
         - High-value transactions
         - Cross-border activity
         - Cash-intensive business

      4. Business Risk:
         - Industry sector risk
         - Complex ownership structures
         - Shell company indicators

      GDPR Compliance (Article 22 - Automated Decision Making):
      - All automated decisions must allow human review
      - Provide clear rationale for risk scores
      - Customer has right to contest automated decisions
      - Store decision rationale for audit trails

      Risk Levels:
      - LOW (0-25): Standard customer, no significant risk factors
      - MEDIUM (26-50): Some risk factors present, enhanced monitoring
      - HIGH (51-75): Multiple risk factors, enhanced due diligence required
      - CRITICAL (76-100): Significant risk, senior approval required, possible SAR filing

      Output: Structured JSON with risk assessment, factors, and GDPR compliance metadata
      """)

  @UserMessage("""
      Perform comprehensive risk assessment for customer:

      Customer Information:
      - Customer ID (pseudonymized): {{customerId}}
      - Nationality: {{nationality}}
      - Residence country: {{residenceCountry}}
      - Occupation: {{occupation}}
      - Industry sector: {{industrySector}}
      - Annual income range: {{incomeRange}}
      - Source of wealth: {{sourceOfWealth}}

      Risk Indicators:
      - PEP status: {{pepStatus}}
      - PEP level (if applicable): {{pepLevel}}
      - Adverse media hits: {{adverseMediaCount}}
      - Sanctions list match: {{sanctionsMatch}}
      - Previous SAR filed: {{previousSar}}

      Geographic Factors:
      - Nationality risk: {{nationalityRisk}}
      - Residence risk: {{residenceRisk}}
      - FATF list status: {{fatfStatus}}

      Business Information (if applicable):
      - Business type: {{businessType}}
      - Years in operation: {{yearsInOperation}}
      - Complex ownership: {{complexOwnership}}
      - Cash-intensive: {{cashIntensive}}

      Transaction History:
      - Account age (months): {{accountAge}}
      - Average monthly volume: {{avgMonthlyVolume}}
      - Unusual pattern flags: {{unusualPatterns}}

      Return JSON format:
      {
        "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
        "riskScore": 0-100,
        "riskCategory": "STANDARD|ENHANCED|SIMPLIFIED",
        "riskFactors": [
          {
            "category": "GEOGRAPHIC|CUSTOMER|TRANSACTION|BUSINESS",
            "factor": "description",
            "severity": "LOW|MEDIUM|HIGH",
            "weight": 0.0-1.0
          }
        ],
        "mitigatingFactors": [
          "factor description"
        ],
        "recommendedActions": [
          "action description"
        ],
        "monitoringRequirements": {
          "reviewFrequency": "MONTHLY|QUARTERLY|ANNUAL",
          "enhancedMonitoring": true|false,
          "transactionThreshold": "amount"
        },
        "complianceRequirements": {
          "eddRequired": true|false,
          "sourceOfWealthVerification": true|false,
          "seniorApprovalRequired": true|false,
          "sarConsideration": true|false
        },
        "gdprCompliance": {
          "automatedDecision": true,
          "humanReviewRequired": true|false,
          "humanReviewReason": "explanation",
          "decisionRationale": "detailed explanation",
          "contestProcedure": "description of appeal process",
          "dataUsed": ["field1", "field2"]
        },
        "nextReviewDate": "YYYY-MM-DD"
      }
      """)
  RiskAssessmentResult assessRisk(
      @V("customerId") String customerId,
      @V("nationality") String nationality,
      @V("residenceCountry") String residenceCountry,
      @V("occupation") String occupation,
      @V("industrySector") String industrySector,
      @V("incomeRange") String incomeRange,
      @V("sourceOfWealth") String sourceOfWealth,
      @V("pepStatus") boolean pepStatus,
      @V("pepLevel") String pepLevel,
      @V("adverseMediaCount") int adverseMediaCount,
      @V("sanctionsMatch") boolean sanctionsMatch,
      @V("previousSar") boolean previousSar,
      @V("nationalityRisk") String nationalityRisk,
      @V("residenceRisk") String residenceRisk,
      @V("fatfStatus") String fatfStatus,
      @V("businessType") String businessType,
      @V("yearsInOperation") Integer yearsInOperation,
      @V("complexOwnership") boolean complexOwnership,
      @V("cashIntensive") boolean cashIntensive,
      @V("accountAge") Integer accountAge,
      @V("avgMonthlyVolume") String avgMonthlyVolume,
      @V("unusualPatterns") List<String> unusualPatterns);

  record RiskAssessmentResult(
      RiskLevel riskLevel,
      int riskScore,
      RiskCategory riskCategory,
      List<RiskFactor> riskFactors,
      List<String> mitigatingFactors,
      List<String> recommendedActions,
      MonitoringRequirements monitoringRequirements,
      ComplianceRequirements complianceRequirements,
      GdprCompliance gdprCompliance,
      String nextReviewDate) {
  }

  enum RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
  }

  enum RiskCategory {
    STANDARD, ENHANCED, SIMPLIFIED
  }

  record RiskFactor(
      FactorCategory category,
      String factor,
      Severity severity,
      double weight) {
  }

  enum FactorCategory {
    GEOGRAPHIC, CUSTOMER, TRANSACTION, BUSINESS
  }

  enum Severity {
    LOW, MEDIUM, HIGH
  }

  record MonitoringRequirements(
      String reviewFrequency,
      boolean enhancedMonitoring,
      String transactionThreshold) {
  }

  record ComplianceRequirements(
      boolean eddRequired,
      boolean sourceOfWealthVerification,
      boolean seniorApprovalRequired,
      boolean sarConsideration) {
  }

  record GdprCompliance(
      boolean automatedDecision,
      boolean humanReviewRequired,
      String humanReviewReason,
      String decisionRationale,
      String contestProcedure,
      List<String> dataUsed) {
  }
}
