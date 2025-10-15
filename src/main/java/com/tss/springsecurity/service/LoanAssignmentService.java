package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.LoanAssignmentRequest;
import com.tss.springsecurity.dto.LoanAssignmentResponse;
import com.tss.springsecurity.dto.OfficerSummary;

import java.util.List;

public interface LoanAssignmentService {
    
    LoanAssignmentResponse assignLoanToOfficer(LoanAssignmentRequest request);
    
    LoanAssignmentResponse reassignLoan(Long assignmentId, Long newOfficerId, String remarks);
    
    List<LoanAssignmentResponse> getAssignmentsByOfficer(Long officerId);
    
    List<LoanAssignmentResponse> getAssignmentsByStatus(String status);
    
    LoanAssignmentResponse updateAssignmentStatus(Long assignmentId, String status, String remarks);
    
    List<LoanAssignmentResponse> getAllAssignments();
    
    LoanAssignmentResponse getAssignmentById(Long assignmentId);
    
    List<OfficerSummary> getAvailableOfficersByLoanType(String loanType);
}
