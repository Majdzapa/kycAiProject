package com.kyc.ai.service;

import com.kyc.ai.agent.DocumentAgent;
import com.kyc.ai.entity.AuditLog;
import com.kyc.ai.entity.KycDocument;
import com.kyc.ai.repository.KycDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAnalysisService {

    private final DocumentAgent documentAgent;
    private final GdprService gdprService;
    private final KycDocumentRepository documentRepository;
    private final MinioClient minioClient;
    private final Tesseract tesseract;
    private final ObjectMapper objectMapper;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Process KYC document submission
     */
    @Transactional
    public KycDocument processDocument(String customerId, MultipartFile file,
            KycDocument.DocumentType docType,
            KycDocument.LegalBasis legalBasis) {
        log.info("Processing document for customer: {}, type: {}", customerId, docType);

        try {
            // 1. Store document in MinIO
            String storagePath = storeDocument(file, customerId);

            // 2. Create document record
            KycDocument document = KycDocument.builder()
                    .customerId(customerId)
                    .documentType(docType)
                    .storagePath(storagePath)
                    .verificationStatus(KycDocument.VerificationStatus.IN_PROGRESS)
                    .processingLegalBasis(legalBasis)
                    .consentTimestamp(LocalDateTime.now())
                    .dataRetentionUntil(gdprService.calculateRetentionExpiry())
                    .build();

            document = documentRepository.save(document);

            // 3. Extract text using OCR
            String ocrText = extractText(file);
            document.setText(gdprService.encrypt(ocrText));

            // 4. Analyze with AI agent
            DocumentAgent.DocumentAnalysisResult analysis = documentAgent.analyzeDocument(
                    docType.name(),
                    "UNKNOWN", // Country could be detected or provided
                    ocrText,
                    gdprService.hashIdentifier(customerId),
                    legalBasis.name());
            log.info("this is the extrated data from document ",analysis.toString());

            // 5. Update document with analysis results
            document.setConfidenceScore(analysis.confidenceScores().overall());
            document.setExtractedData(convertToJson(analysis.extractedData()));
            document.setVerificationStatus(determineVerificationStatus(analysis));
            document.setProcessedAt(LocalDateTime.now());
            document.setProcessedBy("AI_AGENT");

            // Store findings in metadata
            List<String> findings = new java.util.ArrayList<>();
            if (analysis.validationWarnings() != null)
                findings.addAll(analysis.validationWarnings());
            if (analysis.validationResults().suspiciousPatterns() != null)
                findings.addAll(analysis.validationResults().suspiciousPatterns());
            document.setMetadata(safeSerialize(Map.of("findings", findings)));

            gdprService.logDataAccess(
                    customerId,
                    AuditLog.AuditAction.PROCESS,
                    AuditLog.LegalBasis.valueOf(legalBasis.name()),
                    "AI_AGENT",
                    new String[] { "DOCUMENT", "BIOMETRIC" },
                    true,
                    safeSerialize(Map.of(
                            "docType", docType,
                            "confidence", analysis.confidenceScores().overall(),
                            "findings", findings)));

            return documentRepository.save(document);

        } catch (Exception e) {
            log.error("Document processing failed", e);
            gdprService.logDataAccess(
                    customerId,
                    AuditLog.AuditAction.PROCESS,
                    AuditLog.LegalBasis.valueOf(legalBasis.name()),
                    "AI_AGENT",
                    new String[] { "DOCUMENT" },
                    false,
                    safeSerialize(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error")));
            throw new RuntimeException("Document processing failed", e);
        }
    }

    /**
     * Extract text from document using OCR
     */
    public String extractText(MultipartFile file) throws IOException {
        log.info("Starting OCR extraction for file: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());
        try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                log.error("Could not read image from file: {}", file.getOriginalFilename());
                throw new IOException("Could not read image from file");
            }

            log.debug("Image read successfully ({}x{}). Calling Tesseract...",
                    image.getWidth(), image.getHeight());
            String result = tesseract.doOCR(image);

            if (result == null) {
                log.warn("Tesseract returned null result");
                return "";
            }

            log.info("OCR extraction completed successfully. Extracted {} characters.",
                    result.length());
            return result;
        } catch (TesseractException e) {
            log.error("OCR failed for file: {}", file.getOriginalFilename(), e);
            throw new IOException("OCR processing failed", e);
        } catch (Exception e) {
            log.error("Unexpected error during OCR processing for file: {}",
                    file.getOriginalFilename(), e);
            throw new IOException("Unexpected OCR error", e);
        }
    }

    /**
     * Store document in MinIO
     */
    private String storeDocument(MultipartFile file, String customerId) throws Exception {
        String objectName = String.format("%s/%s/%s_%s",
                customerId,
                LocalDateTime.now().toLocalDate(),
                UUID.randomUUID(),
                file.getOriginalFilename());

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        log.debug("Stored document at: {}", objectName);
        return objectName;
    }

    /**
     * Delete document from storage
     */
    public void deleteDocument(String storagePath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storagePath)
                            .build());
            log.debug("Deleted document: {}", storagePath);
        } catch (Exception e) {
            log.error("Failed to delete document: {}", storagePath, e);
        }
    }

    /**
     * Determine verification status based on analysis
     */
    private KycDocument.VerificationStatus determineVerificationStatus(
            DocumentAgent.DocumentAnalysisResult analysis) {

        // If confidence is too low, require human review
        if (analysis.confidenceScores().overall() < 0.7) {
            return KycDocument.VerificationStatus.NEEDS_REVIEW;
        }

        // If validation fails, reject
        if (!analysis.validationResults().documentValid() ||
                !analysis.validationResults().notExpired()) {
            return KycDocument.VerificationStatus.REJECTED;
        }

        // If suspicious patterns detected, needs review
        if (analysis.validationResults().suspiciousPatterns() != null &&
                !analysis.validationResults().suspiciousPatterns().isEmpty()) {
            return KycDocument.VerificationStatus.NEEDS_REVIEW;
        }

        return KycDocument.VerificationStatus.VERIFIED;
    }

    private String convertToJson(DocumentAgent.ExtractedData data) {
        return safeSerialize(data);
    }

    private String safeSerialize(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert data to JSON", e);
            // Fallback to a simple JSON if serialization fails
            return String.format(
                    "{\"error\": \"Serialization failed\", \"message\": \"%s\"}",
                    e.getMessage().replace("\"", "\\\""));
        }
    }

    /**
     * Get document by ID
     */
    @Transactional(readOnly = true)
    public KycDocument getDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
    }

    /**
     * Get documents by customer ID
     */
    @Transactional(readOnly = true)
    public java.util.List<KycDocument> getCustomerDocuments(String customerId) {
        return documentRepository.findByCustomerId(customerId);
    }
}
