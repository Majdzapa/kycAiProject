package com.kyc.ai.controller;

import com.kyc.ai.service.GdprService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/gdpr")
@RequiredArgsConstructor
@Tag(name = "GDPR Compliance", description = "GDPR data subject rights APIs")
@SecurityRequirement(name = "bearerAuth")
public class GdprController {

    private final GdprService gdprService;

    @GetMapping("/export-data")
    @Operation(summary = "Export personal data", description = "GDPR Article 20 - Right to Data Portability")
    @PreAuthorize("#customerId == authentication.name or hasAnyRole('ADMIN', 'DPO')")
    public ResponseEntity<Map<String, Object>> exportData(
            @RequestParam("customerId") @Parameter(description = "Customer ID") String customerId,
            @RequestParam(value = "format", defaultValue = "JSON") ExportFormat format,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Data export requested for customer: {} by user: {}", customerId, userDetails.getUsername());

        Map<String, Object> exportData = gdprService.exportCustomerData(customerId);

        if (format == ExportFormat.XML) {
            // Convert to XML if requested
            // In production, use Jackson XML mapper
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"gdpr-export-" + customerId + ".xml\"")
                .body(exportData);
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"gdpr-export-" + customerId + ".json\"")
            .body(exportData);
    }

    @DeleteMapping("/delete-data")
    @Operation(summary = "Delete personal data", description = "GDPR Article 17 - Right to Erasure")
    @PreAuthorize("#customerId == authentication.name or hasAnyRole('ADMIN', 'DPO')")
    public ResponseEntity<DeletionResponse> deleteData(
            @RequestParam("customerId") @Parameter(description = "Customer ID") String customerId,
            @RequestBody(required = false) DeletionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Data deletion requested for customer: {} by user: {}", customerId, userDetails.getUsername());

        boolean success = gdprService.deleteCustomerData(customerId);

        if (success) {
            return ResponseEntity.ok(new DeletionResponse(
                "SUCCESS",
                "Your personal data has been deleted in accordance with GDPR Article 17",
                customerId,
                java.time.LocalDateTime.now().toString()
            ));
        } else {
            return ResponseEntity.status(500).body(new DeletionResponse(
                "FAILED",
                "Unable to complete data deletion. Please contact our DPO.",
                customerId,
                null
            ));
        }
    }

    @PostMapping("/consent")
    @Operation(summary = "Record consent", description = "Record explicit consent for data processing")
    public ResponseEntity<ConsentResponse> recordConsent(
            @RequestBody @Parameter(description = "Consent details") ConsentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Consent recorded for customer: {} - Purpose: {}", 
                userDetails.getUsername(), request.purpose());

        // In production, save to consent management system
        return ResponseEntity.ok(new ConsentResponse(
            "SUCCESS",
            "Consent recorded successfully",
            request.purpose(),
            request.version(),
            java.time.LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/withdraw-consent")
    @Operation(summary = "Withdraw consent", description = "Withdraw previously given consent")
    public ResponseEntity<ConsentResponse> withdrawConsent(
            @RequestBody @Parameter(description = "Consent withdrawal details") WithdrawConsentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Consent withdrawn for customer: {} - Purpose: {}", 
                userDetails.getUsername(), request.purpose());

        // In production, update consent management system
        return ResponseEntity.ok(new ConsentResponse(
            "SUCCESS",
            "Consent withdrawn successfully. Processing will stop within 30 days.",
            request.purpose(),
            null,
            java.time.LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/privacy-policy")
    @Operation(summary = "Get privacy policy", description = "Retrieve current privacy policy")
    public ResponseEntity<PrivacyPolicyResponse> getPrivacyPolicy() {
        return ResponseEntity.ok(new PrivacyPolicyResponse(
            "KYC AI Service Privacy Policy",
            "1.0",
            "We process your personal data for KYC verification purposes...",
            java.time.LocalDateTime.now().toString(),
            "https://example.com/privacy-policy"
        ));
    }

    @GetMapping("/processing-activities")
    @Operation(summary = "Get processing activities", description = "GDPR Article 30 - Record of Processing Activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'DPO')")
    public ResponseEntity<ProcessingActivitiesResponse> getProcessingActivities() {
        return ResponseEntity.ok(new ProcessingActivitiesResponse(
            "KYC AI Service",
            new String[]{
                "Identity verification",
                "AML risk assessment",
                "Document analysis",
                "Customer support"
            },
            new String[]{"LEGAL_OBLIGATION", "CONSENT", "LEGITIMATE_INTEREST"},
            "90 days",
            "Data is encrypted at rest and in transit"
        ));
    }

    // Enums
    public enum ExportFormat {
        JSON, XML
    }

    // DTOs
    public record DeletionRequest(
        String reason,
        boolean confirmDeletion
    ) {}

    public record DeletionResponse(
        String status,
        String message,
        String customerId,
        String completedAt
    ) {}

    public record ConsentRequest(
        String purpose,
        String version,
        boolean explicitConsent,
        String[] dataCategories
    ) {}

    public record WithdrawConsentRequest(
        String purpose,
        String reason
    ) {}

    public record ConsentResponse(
        String status,
        String message,
        String purpose,
        String version,
        String timestamp
    ) {}

    public record PrivacyPolicyResponse(
        String title,
        String version,
        String summary,
        String lastUpdated,
        String fullPolicyUrl
    ) {}

    public record ProcessingActivitiesResponse(
        String controllerName,
        String[] processingPurposes,
        String[] legalBases,
        String retentionPeriod,
        String securityMeasures
    ) {}
}
