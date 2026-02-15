package com.kyc.ai.util;

import java.util.Map;
import java.util.Set;

/**
 * Utility for Expert Jurisdiction and Country Risk Classification
 * Based on FATF (Financial Action Task Force) status and general AML risk
 * indicators.
 */
public class CountryRiskUtil {

    // FATF Blacklist (High-Risk Jurisdictions subject to a Call for Action)
    private static final Set<String> FATF_BLACKLIST = Set.of(
            "KP", // North Korea
            "IR", // Iran
            "MM" // Myanmar
    );

    // FATF Greylist (Jurisdictions under Increased Monitoring)
    private static final Set<String> FATF_GREYLIST = Set.of(
            "BG", "BF", "CM", "CD", "HR", "HT", "JM", "JO", "ML",
            "MZ", "NG", "PH", "SN", "ZA", "SS", "SY", "TZ", "TR",
            "UG", "VN", "YE");

    /**
     * Get Nationality Risk Level
     */
    public static String getNationalityRisk(String countryCode) {
        if (countryCode == null)
            return "LOW";
        String code = countryCode.toUpperCase();

        if (FATF_BLACKLIST.contains(code))
            return "CRITICAL";
        if (FATF_GREYLIST.contains(code))
            return "HIGH";

        return "LOW";
    }

    /**
     * Get Residence Risk Level
     */
    public static String getResidenceRisk(String countryCode) {
        if (countryCode == null)
            return "LOW";
        String code = countryCode.toUpperCase();

        if (FATF_BLACKLIST.contains(code))
            return "HIGH"; // Slightly lower than nationality but still high
        if (FATF_GREYLIST.contains(code))
            return "MEDIUM";

        return "LOW";
    }

    /**
     * Get FATF Status string
     */
    public static String getFatfStatus(String countryCode) {
        if (countryCode == null)
            return "NONE";
        String code = countryCode.toUpperCase();

        if (FATF_BLACKLIST.contains(code))
            return "BLACKLISTED";
        if (FATF_GREYLIST.contains(code))
            return "GREYLISTED";

        return "NONE";
    }

    /**
     * Get a human-readable reason for country risk
     */
    public static String getCountryRiskReason(String countryCode) {
        if (countryCode == null)
            return null;
        String code = countryCode.toUpperCase();

        if (FATF_BLACKLIST.contains(code)) {
            return "Country is on the FATF Blacklist (High-Risk Juridictions)";
        }
        if (FATF_GREYLIST.contains(code)) {
            return "Country is on the FATF Greylist (Increased Monitoring)";
        }
        return null;
    }
}
