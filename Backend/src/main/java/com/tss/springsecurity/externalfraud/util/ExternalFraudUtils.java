package com.tss.springsecurity.externalfraud.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ExternalFraudUtils {
    
    private static final Pattern PAN_PATTERN = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]{1}");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\d{12}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{10}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    /**
     * Validate PAN number format
     */
    public boolean isValidPan(String pan) {
        return pan != null && PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches();
    }
    
    /**
     * Validate Aadhaar number format
     */
    public boolean isValidAadhaar(String aadhaar) {
        return aadhaar != null && AADHAAR_PATTERN.matcher(aadhaar.trim()).matches();
    }
    
    /**
     * Validate phone number format
     */
    public boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }
    
    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Mask PAN number for logging
     */
    public String maskPan(String pan) {
        if (pan == null || pan.length() < 4) {
            return "****";
        }
        return "****" + pan.substring(pan.length() - 4);
    }
    
    /**
     * Mask Aadhaar number for logging
     */
    public String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) {
            return "****";
        }
        return "****-****-" + aadhaar.substring(aadhaar.length() - 4);
    }
    
    /**
     * Mask phone number for logging
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "******" + phone.substring(phone.length() - 4);
    }
    
    /**
     * Mask email for logging
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****@****.com";
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];
        
        String maskedUsername = username.length() > 2 ? 
            username.substring(0, 2) + "****" : "****";
        String maskedDomain = domain.length() > 4 ? 
            "****" + domain.substring(domain.length() - 4) : "****.com";
            
        return maskedUsername + "@" + maskedDomain;
    }
    
    /**
     * Generate SHA-256 hash for data
     */
    public String generateHash(String data) {
        if (data == null) {
            return null;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return data.hashCode() + ""; // Fallback to simple hash
        }
    }
    
    /**
     * Parse date string with multiple format support
     */
    public LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateString.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }
        
        log.warn("Could not parse date: {}", dateString);
        return null;
    }
    
    /**
     * Calculate age from date of birth
     */
    public int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return 0;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
    
    /**
     * Check if string is null or empty
     */
    public boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Check if two strings match (case-insensitive, null-safe)
     */
    public boolean stringsMatch(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.trim().equalsIgnoreCase(str2.trim());
    }
    
    /**
     * Normalize string for comparison
     */
    public String normalizeString(String str) {
        if (str == null) return null;
        return str.trim().toUpperCase().replaceAll("\\s+", " ");
    }
    
    /**
     * Calculate similarity score between two strings (0-100)
     */
    public int calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) return 0;
        if (str1.equals(str2)) return 100;
        
        String s1 = normalizeString(str1);
        String s2 = normalizeString(str2);
        
        if (s1.equals(s2)) return 100;
        
        // Simple Levenshtein distance based similarity
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        if (maxLength == 0) return 100;
        
        return (int) ((1.0 - (double) distance / maxLength) * 100);
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Format currency amount
     */
    public String formatCurrency(double amount) {
        if (amount >= 10000000) { // 1 crore
            return String.format("₹%.2f Cr", amount / 10000000);
        } else if (amount >= 100000) { // 1 lakh
            return String.format("₹%.2f L", amount / 100000);
        } else if (amount >= 1000) { // 1 thousand
            return String.format("₹%.2f K", amount / 1000);
        } else {
            return String.format("₹%.2f", amount);
        }
    }
    
    /**
     * Get risk color for UI display
     */
    public String getRiskColor(String riskLevel) {
        return switch (riskLevel) {
            case "CLEAN" -> "#28a745"; // Green
            case "LOW" -> "#ffc107";   // Yellow
            case "MEDIUM" -> "#fd7e14"; // Orange
            case "HIGH" -> "#dc3545";   // Red
            case "CRITICAL" -> "#6f42c1"; // Purple
            default -> "#6c757d";       // Gray
        };
    }
    
    /**
     * Generate unique screening ID
     */
    public String generateScreeningId() {
        return "EFS_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString((int) (Math.random() * 0xFFFF));
    }
    
    /**
     * Check if date is within specified days from now
     */
    public boolean isWithinDays(LocalDateTime dateTime, int days) {
        if (dateTime == null) return false;
        return dateTime.isAfter(LocalDateTime.now().minusDays(days));
    }
    
    /**
     * Check if date is older than specified days
     */
    public boolean isOlderThanDays(LocalDateTime dateTime, int days) {
        if (dateTime == null) return true;
        return dateTime.isBefore(LocalDateTime.now().minusDays(days));
    }
}
