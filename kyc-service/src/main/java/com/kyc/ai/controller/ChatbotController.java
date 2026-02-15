package com.kyc.ai.controller;

import com.kyc.ai.agent.ChatbotAgent;
import com.kyc.ai.entity.AuditLog;
import com.kyc.ai.service.GdprService;
import com.kyc.ai.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "KYC Support Chatbot APIs")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    private final ChatbotAgent chatbotAgent;
    private final GdprService gdprService;
    private final RagService ragService;

    // In-memory conversation store (use Redis in production)
    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();

    @PostMapping("/message")
    @Operation(summary = "Send message to chatbot", description = "Chat with KYC support assistant")
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestHeader(value = "X-Session-Id", required = false) 
                @Parameter(description = "Session ID for conversation continuity") String sessionId,
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String conversationId = sessionId != null ? sessionId : UUID.randomUUID().toString();
        
        // Get or create conversation
        Conversation conversation = conversations.computeIfAbsent(conversationId, 
            id -> new Conversation(id, userDetails.getUsername()));

        // Sanitize message before processing
        String sanitizedMessage = sanitizeMessage(request.message());

        // Retrieve RAG context
        String context = ragService.retrieveContextForChatbot(sanitizedMessage);

        // Get conversation history
        String history = String.join("\n", conversation.messages);

        // Generate response
        ChatbotAgent.ChatResponse agentResponse = chatbotAgent.chat(
            conversationId,
            sanitizedMessage,
            history,
            context,
            request.hasActiveApplication(),
            request.currentStatus(),
            request.documentsSubmitted()
        );

        // Update conversation history
        conversation.addMessage("User: " + sanitizedMessage);
        conversation.addMessage("Assistant: " + agentResponse.response());

        // Log interaction (GDPR)
        gdprService.logDataAccess(
            userDetails.getUsername(),
            AuditLog.AuditAction.CHAT_INTERACTION,
            AuditLog.LegalBasis.LEGITIMATE_INTEREST,
            "CHATBOT",
            new String[]{"CONVERSATION"},
            true,
            "{\"sessionId\": \"" + conversationId + "\"}"
        );

        return ResponseEntity.ok(new ChatResponse(
            conversationId,
            agentResponse.response(),
            agentResponse.suggestedActions(),
            agentResponse.escalationNeeded(),
            agentResponse.escalationReason(),
            agentResponse.disclaimer()
        ));
    }

    @DeleteMapping("/history/{sessionId}")
    @Operation(summary = "Delete conversation history", description = "GDPR Right to Erasure for chat history")
    public ResponseEntity<Map<String, String>> deleteHistory(
            @PathVariable @Parameter(description = "Session ID") String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        conversations.remove(sessionId);
        
        gdprService.logDataAccess(
            userDetails.getUsername(),
            AuditLog.AuditAction.DATA_DELETION,
            AuditLog.LegalBasis.GDPR_ARTICLE_17,
            userDetails.getUsername(),
            new String[]{"CONVERSATION"},
            true,
            "{\"sessionId\": \"" + sessionId + "\"}"
        );

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Conversation history deleted"
        ));
    }

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "Get conversation history", description = "Retrieve chat history")
    public ResponseEntity<ConversationHistoryResponse> getHistory(
            @PathVariable @Parameter(description = "Session ID") String sessionId) {

        Conversation conversation = conversations.get(sessionId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new ConversationHistoryResponse(
            sessionId,
            conversation.messages,
            conversation.createdAt
        ));
    }

    private String sanitizeMessage(String message) {
        // Remove potential PII from logs
        return message
            .replaceAll("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b", "[REDACTED_CARD]")
            .replaceAll("\\b\\d{3}-\\d{2}-\\d{4}\\b", "[REDACTED_SSN]")
            .replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "[REDACTED_EMAIL]");
    }

    // Inner classes
    private static class Conversation {
        final String id;
        final String userId;
        final List<String> messages = new java.util.ArrayList<>();
        final java.time.LocalDateTime createdAt;

        Conversation(String id, String userId) {
            this.id = id;
            this.userId = userId;
            this.createdAt = java.time.LocalDateTime.now();
        }

        void addMessage(String message) {
            messages.add(message);
            // Keep only last 20 messages to prevent memory issues
            if (messages.size() > 20) {
                messages.remove(0);
            }
        }
    }

    // DTOs
    public record ChatRequest(
        String message,
        boolean hasActiveApplication,
        String currentStatus,
        List<String> documentsSubmitted
    ) {}

    public record ChatResponse(
        String sessionId,
        String response,
        List<String> suggestedActions,
        boolean escalationNeeded,
        String escalationReason,
        String disclaimer
    ) {}

    public record ConversationHistoryResponse(
        String sessionId,
        List<String> messages,
        java.time.LocalDateTime createdAt
    ) {}
}
