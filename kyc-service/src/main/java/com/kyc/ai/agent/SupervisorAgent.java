package com.kyc.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

/**
 * Supervisor Agent - Orchestrates the multi-agent KYC workflow
 * Routes tasks to appropriate specialized agents and ensures GDPR compliance
 */
@AiService
public interface SupervisorAgent {

    @SystemMessage("""
            You are the Supervisor Agent for a GDPR-compliant KYC (Know Your Customer) system.

            Your responsibilities:
            1. Route tasks to appropriate specialized agents (Document, Risk, Chatbot)
            2. Ensure no PII is exposed in logs or responses
            3. Validate that all processing has legal basis under GDPR
            4. Coordinate multi-step KYC workflows
            5. Escalate to human operators when confidence is low or edge cases arise

            Available agents:
            - DocumentAgent: Analyzes ID documents, passports, proof of address
            - RiskAgent: Performs risk scoring and AML checks
            - ChatbotAgent: Handles customer inquiries about KYC status
            - HumanEscalation: For complex cases requiring manual review

            Routing Rules:
            - DOCUMENT_ANALYSIS → DocumentAgent
            - RISK_ASSESSMENT → RiskAgent
            - CUSTOMER_INQUIRY → ChatbotAgent
            - LOW_CONFIDENCE | COMPLEX_CASE | GDPR_ESCALATION → HumanEscalation

            GDPR Compliance Rules:
            - Always verify consent exists before processing personal data
            - If legal basis is unclear, escalate to human operator
            - Never store raw document data in conversation history
            - Pseudonymize all customer identifiers in routing decisions
            - Log all routing decisions for audit purposes

            Response Format: Return a valid JSON object with the routing decision.
            """)

    @UserMessage("""
            Route this KYC request to the appropriate agent:

            Request type: {{requestType}}
            Customer ID (pseudonymized): {{customerId}}
            Task description: {{taskDescription}}
            Legal basis: {{legalBasis}}
            Confidence threshold: {{confidenceThreshold}}

            Available context:
            - Previous document submissions: {{previousSubmissions}}
            - Current verification status: {{currentStatus}}
            - Risk indicators: {{riskIndicators}}

            Return JSON with:
            {
              "selectedAgent": "DOCUMENT|RISK|CHATBOT|HUMAN_ESCALATION",
              "reasoning": "Brief explanation of routing decision",
              "privacyChecksPassed": true|false,
              "requiredAgents": ["agent1", "agent2"],
              "executionOrder": "SEQUENTIAL|PARALLEL",
              "estimatedProcessingTime": "seconds",
              "escalationReason": "if applicable"
            }
            
            Return JSON with:
                {
                  "selectedAgent": "DOCUMENT|RISK|CHATBOT|HUMAN_ESCALATION",  // MUST be single value
                  "reasoning": "Brief explanation",
                  "privacyChecksPassed": true|false,
                  "requiredAgents": ["DOCUMENT", "RISK"],  // Can be multiple
                  "executionOrder": "SEQUENTIAL|PARALLEL",
                  "estimatedProcessingTime": 30,
                  "escalationReason": "optional"
                }
                IMPORTANT:
                - selectedAgent must be a SINGLE agent type (not an array)
                - If multiple agents needed, put primary agent in selectedAgent
                - List all required agents in requiredAgents array
            """)
    RoutingDecision routeTask(
            @V("requestType") String requestType,
            @V("customerId") String customerId,
            @V("taskDescription") String taskDescription,
            @V("legalBasis") String legalBasis,
            @V("confidenceThreshold") Double confidenceThreshold,
            @V("previousSubmissions") Integer previousSubmissions,
            @V("currentStatus") String currentStatus,
            @V("riskIndicators") List<String> riskIndicators);

    record RoutingDecision(
            AgentType selectedAgent,
            String reasoning,
            boolean privacyChecksPassed,
            List<AgentType> requiredAgents,
            ExecutionOrder executionOrder,
            Integer estimatedProcessingTime,
            String escalationReason) {
    }

    enum AgentType {
        DOCUMENT,
        RISK,
        CHATBOT,
        HUMAN_ESCALATION
    }

    enum ExecutionOrder {
        SEQUENTIAL,
        PARALLEL
    }
}
