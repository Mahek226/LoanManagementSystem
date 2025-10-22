package com.tss.springsecurity.service;

import com.tss.springsecurity.service.EnhancedLoanScreeningService.EnhancedScoringResult;

/**
 * Interface for Enhanced Loan Screening Service
 * Provides normalized scoring combining internal and external fraud detection
 */
public interface EnhancedLoanScreeningServiceInterface {
    
    /**
     * Perform enhanced loan screening with normalized scoring
     * 
     * @param applicantId The ID of the applicant to screen
     * @return EnhancedScoringResult containing normalized scores and detailed breakdown
     */
    EnhancedScoringResult performEnhancedScreening(Long applicantId);
    
    /**
     * Get scoring configuration details
     * 
     * @return Configuration details for scoring weights and thresholds
     */
    ScoringConfiguration getScoringConfiguration();
    
    /**
     * Configuration class for scoring parameters
     */
    class ScoringConfiguration {
        private Integer internalMaxScore;
        private Integer externalMaxScore;
        private Double internalWeight;
        private Double externalWeight;
        private Integer riskScoreThreshold;
        
        // Getters and setters
        public Integer getInternalMaxScore() { return internalMaxScore; }
        public void setInternalMaxScore(Integer internalMaxScore) { this.internalMaxScore = internalMaxScore; }
        
        public Integer getExternalMaxScore() { return externalMaxScore; }
        public void setExternalMaxScore(Integer externalMaxScore) { this.externalMaxScore = externalMaxScore; }
        
        public Double getInternalWeight() { return internalWeight; }
        public void setInternalWeight(Double internalWeight) { this.internalWeight = internalWeight; }
        
        public Double getExternalWeight() { return externalWeight; }
        public void setExternalWeight(Double externalWeight) { this.externalWeight = externalWeight; }
        
        public Integer getRiskScoreThreshold() { return riskScoreThreshold; }
        public void setRiskScoreThreshold(Integer riskScoreThreshold) { this.riskScoreThreshold = riskScoreThreshold; }
    }
}
