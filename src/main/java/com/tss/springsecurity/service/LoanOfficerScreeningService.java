package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.LoanScreeningRequest;
import com.tss.springsecurity.dto.LoanScreeningResponse;
import com.tss.springsecurity.dto.LoanScreeningDecision;
import com.tss.springsecurity.dto.ScreeningDashboardResponse;

import java.util.List;

public interface LoanOfficerScreeningService {
    
    List<LoanScreeningResponse> getAssignedLoansForOfficer(Long officerId);
    
    LoanScreeningResponse getLoanDetailsForScreening(Long assignmentId);
    
    LoanScreeningResponse processLoanScreening(Long officerId, LoanScreeningRequest request);
    
    LoanScreeningResponse escalateToCompliance(Long assignmentId, String remarks);
    
    List<LoanScreeningResponse> getComplianceEscalations();
    
    LoanScreeningResponse processComplianceDecision(Long complianceOfficerId, LoanScreeningRequest request);
    
    // New enhanced screening methods
    ScreeningDashboardResponse getScreeningDashboard(Long officerId);
    
    LoanScreeningResponse screenAssignedLoan(Long officerId, Long assignmentId, LoanScreeningDecision decision);
    
    List<LoanScreeningResponse> getScreeningHistory(Long officerId, int page, int size);
}
