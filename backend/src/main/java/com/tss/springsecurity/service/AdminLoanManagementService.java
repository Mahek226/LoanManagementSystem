package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.LoanApplicationResponse;
import com.tss.springsecurity.dto.LoanProgressTimelineResponse;

import java.util.List;
import java.util.Map;

public interface AdminLoanManagementService {
    
    /**
     * Get all loans with pagination
     * @param page Page number
     * @param size Page size
     * @return Map containing loans and pagination info
     */
    Map<String, Object> getAllLoans(int page, int size);
    
    /**
     * Get loan by ID
     * @param loanId Loan ID
     * @return Loan application response
     */
    LoanApplicationResponse getLoanById(Long loanId);
    
    /**
     * Get loan progress timeline
     * @param loanId Loan ID
     * @return Loan progress timeline
     */
    LoanProgressTimelineResponse getLoanProgress(Long loanId);
    
    /**
     * Get loans by status
     * @param status Loan status
     * @return List of loan application responses
     */
    List<LoanApplicationResponse> getLoansByStatus(String status);
}
