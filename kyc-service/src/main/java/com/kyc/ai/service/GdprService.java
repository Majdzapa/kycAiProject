package com.kyc.ai.service;

import com.kyc.ai.entity.AuditLog;
import com.kyc.ai.entity.KycDocument;
import com.kyc.ai.repository.AuditLogRepository;
import com.kyc.ai.repository.KycDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GdprService {

    @Value("${gdpr.encryption-key}")
    private String encryptionKey;

    @Value("${gdpr.data-retention-days}")
    private int retentionDays;

    @Value("${gdpr.anonymization-enabled}")
    private boolean anonymizationEnabled;

    private final KycDocumentRepository documentRepository;
    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;

    private static final Pattern PII_PATTERNS = Pattern.compile(
            "\\b\\d{3}-\\d{2}-\\d{4}\\b|" + // SSN-like
                    "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b|" + // Email
                    "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b|" + // Credit card
                    "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b" // Other card patterns
    );

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Hash sensitive identifiers using SHA-256 (one-way)
     */
    public String hashIdentifier(String identifier) {
        if (identifier == null ) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(identifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Hashing failed", e);
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Encrypt sensitive data using AES-256 GCM (reversible)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(
                    Arrays.copyOf(encryptionKey.getBytes(StandardCharsets.UTF_8), 32), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt encrypted data
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(
                    Arrays.copyOf(encryptionKey.getBytes(StandardCharsets.UTF_8), 32), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Calculate data retention expiry date
     */
    public LocalDateTime calculateRetentionExpiry() {
        return LocalDateTime.now().plusDays(retentionDays);
    }

    /**
     * Check if text contains potential PII (basic heuristic)
     */
    public boolean containsPotentialPii(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return PII_PATTERNS.matcher(text).find();
    }

    /**
     * Anonymize data for analytics/ML (irreversible masking)
     */
    public String anonymize(String data) {
        if (data == null || data.length() < 4) {
            return "****";
        }
        if (data.length() <= 8) {
            return data.substring(0, 1) + "***" + data.substring(data.length() - 1);
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }

    /**
     * Validate consent for processing
     */
    public boolean hasValidConsent(String customerId, String purpose) {
        // Implementation: Check consent registry
        // Verify consent is explicit, informed, and not expired
        // For now, return true - in production, query consent management system
        log.debug("Checking consent for customer: {}, purpose: {}", customerId, purpose);
        return true;
    }

    /**
     * Log data access for audit trail (GDPR Article 30)
     */
    @Transactional
    public void logDataAccess(String customerId, AuditLog.AuditAction action,
            AuditLog.LegalBasis legalBasis, String performedBy,
            String[] dataCategories, boolean success, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .customerId(customerId)
                    .action(action)
                    .performedBy(performedBy)
                    .ipAddress(getClientIpAddress())
                    .userAgent(request.getHeader("User-Agent"))
                    .legalBasis(legalBasis)
                    .dataCategories(dataCategories)
                    .requestId(UUID.randomUUID().toString())
                    .details(details)
                    .success(success)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {}", auditLog.getId());
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    /**
     * Export customer data (Right to Portability - GDPR Article 20)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportCustomerData(String customerId) {
        log.info("Exporting data for customer: {}", customerId);

        List<KycDocument> documents = documentRepository.findByCustomerId(customerId);
        List<AuditLog> accessLogs = auditLogRepository.findByCustomerIdAndActions(
                customerId,
                List.of(AuditLog.AuditAction.VIEW, AuditLog.AuditAction.PROCESS, AuditLog.AuditAction.EXPORT));

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("customerId", customerId);
        exportData.put("exportDate", LocalDateTime.now().toString());
        exportData.put("formatVersion", "1.0");
        exportData.put("documents", documents.stream().map(this::sanitizeDocumentForExport).toList());
        exportData.put("accessHistory", accessLogs.stream().map(this::sanitizeAuditLogForExport).toList());

        // Log the export
        logDataAccess(customerId, AuditLog.AuditAction.DATA_EXPORT,
                AuditLog.LegalBasis.GDPR_ARTICLE_17, "CUSTOMER",
                new String[] { "PERSONAL_DATA", "DOCUMENTS" }, true, null);

        return exportData;
    }

    /**
     * Delete customer data (Right to Erasure - GDPR Article 17)
     */
    @Transactional
    public boolean deleteCustomerData(String customerId) {
        log.info("Deleting data for customer: {}", customerId);

        try {
            List<KycDocument> documents = documentRepository.findByCustomerId(customerId);

            // Anonymize or delete documents
            for (KycDocument doc : documents) {
                if (anonymizationEnabled) {
                    documentRepository.anonymizeDocument(doc.getId());
                } else {
                    documentRepository.delete(doc);
                }
            }

            // Log the deletion
            logDataAccess(customerId, AuditLog.AuditAction.DATA_DELETION,
                    AuditLog.LegalBasis.GDPR_ARTICLE_17, "CUSTOMER",
                    new String[] { "PERSONAL_DATA", "DOCUMENTS" }, true, null);

            return true;
        } catch (Exception e) {
            log.error("Failed to delete customer data", e);
            return false;
        }
    }

    /**
     * Scheduled task to purge expired data
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void purgeExpiredData() {
        log.info("Running scheduled data purge");

        LocalDateTime now = LocalDateTime.now();
        int deletedCount = documentRepository.deleteExpiredDocuments(now);

        if (deletedCount > 0) {
            log.info("Purged {} expired documents", deletedCount);

            // Log the purge
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditLog.AuditAction.AUTO_PURGE)
                    .performedBy("SYSTEM")
                    .legalBasis(AuditLog.LegalBasis.GDPR_ARTICLE_17)
                    .details(safeSerialize(Map.of("deletedCount", deletedCount)))
                    .success(true)
                    .build();
            auditLogRepository.save(auditLog);
        }
    }

    /**
     * Get client IP address from request
     */
    private InetAddress getClientIpAddress() {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            return InetAddress.getByName(ip.split(",")[0].trim());
        } catch (Exception e) {
            log.warn("Could not determine client IP", e);
            return null;
        }
    }

    /**
     * Sanitize document for export (remove internal fields)
     */
    private Map<String, Object> sanitizeDocumentForExport(KycDocument doc) {
        Map<String, Object> sanitized = new HashMap<>();
        sanitized.put("id", doc.getId());
        sanitized.put("documentType", doc.getDocumentType());
        sanitized.put("verificationStatus", doc.getVerificationStatus());
        sanitized.put("createdAt", doc.getCreatedAt());
        sanitized.put("processedAt", doc.getProcessedAt());
        // Don't include raw content or internal paths
        return sanitized;
    }

    /**
     * Sanitize audit log for export
     */
    private Map<String, Object> sanitizeAuditLogForExport(AuditLog log) {
        Map<String, Object> sanitized = new HashMap<>();
        sanitized.put("action", log.getAction());
        sanitized.put("performedAt", log.getPerformedAt());
        sanitized.put("legalBasis", log.getLegalBasis());
        sanitized.put("dataCategories", log.getDataCategories());
        return sanitized;
    }

    private String safeSerialize(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize audit details", e);
            return "{\"error\": \"Serialization failed\"}";
        }
    }
}
