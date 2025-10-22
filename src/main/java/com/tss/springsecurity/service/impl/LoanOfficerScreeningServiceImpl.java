package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.LoanScreeningRequest;
import com.tss.springsecurity.dto.LoanScreeningResponse;
import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import com.tss.springsecurity.service.EnhancedLoanScreeningService;
import com.tss.springsecurity.service.LoanOfficerScreeningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanOfficerScreeningServiceImpl implements LoanOfficerScreeningService {
    
    private final OfficerApplicationAssignmentRepository assignmentRepository;
    private final ComplianceOfficerApplicationAssignmentRepository complianceAssignmentRepository;
    private final ApplicantLoanDetailsRepository loanRepository;
    private final LoanOfficerRepository loanOfficerRepository;
    private final ComplianceOfficerRepository complianceOfficerRepository;
    
    @Autowired
    private EnhancedLoanScreeningService enhancedScreeningService;
    
    @Value("${loan.risk-score.threshold:70}")
    private Integer riskScoreThreshold;
    
    @Override
    @Transactional(readOnly = true)
    public List<LoanScreeningResponse> getAssignedLoansForOfficer(Long officerId) {
        log.info("Getting assigned loans for officer: {}", officerId);
        
        List<OfficerApplicationAssignment> assignments = assignmentRepository
                .findByOfficer_OfficerId(officerId);
        
        return assignments.stream()
                .filter(assignment -> "PENDING".equals(assignment.getStatus()) || "IN_PROGRESS".equals(assignment.getStatus()))
                .map(this::mapToScreeningResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public LoanScreeningResponse getLoanDetailsForScreening(Long assignmentId) {
        log.info("Getting loan details for screening: {}", assignmentId);
        
        // Try to find in loan officer assignments first
        OfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment != null) {
            return mapToScreeningResponse(assignment);
        }
        
        // If not found, try compliance officer assignments
        ComplianceOfficerApplicationAssignment complianceAssignment = complianceAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        return mapComplianceToScreeningResponse(complianceAssignment);
    }
    
    @Override
    public LoanScreeningResponse processLoanScreening(Long officerId, LoanScreeningRequest request) {
        log.info("Processing loan screening by officer {} for assignment {}", officerId, request.getAssignmentId());
        
        OfficerApplicationAssignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + request.getAssignmentId()));
        
        // Verify the officer owns this assignment
        if (!assignment.getOfficer().getOfficerId().equals(officerId)) {
            throw new RuntimeException("Officer is not authorized to process this assignment");
        }
        
        ApplicantLoanDetails loan = getLoanForApplicant(assignment.getApplicant().getApplicantId());
        
        // Perform enhanced screening to get normalized score
        try {
            EnhancedLoanScreeningService.EnhancedScoringResult enhancedResult = 
                enhancedScreeningService.performEnhancedScreening(assignment.getApplicant().getApplicantId());
            
            // Update loan with normalized score
            loan.setRiskScore(enhancedResult.getNormalizedScore().intValue());
            loanRepository.save(loan);
            
            log.info("Updated loan {} with enhanced normalized score: {}", loan.getLoanId(), enhancedResult.getNormalizedScore());
        } catch (Exception e) {
            log.error("Error performing enhanced screening for applicant {}, using existing score", assignment.getApplicant().getApplicantId(), e);
        }
        
        // Check if officer can approve/reject based on risk score
        boolean canApproveReject = loan.getRiskScore() < riskScoreThreshold;
        
        switch (request.getAction().toUpperCase()) {
            case "APPROVE":
                if (!canApproveReject) {
                    throw new RuntimeException("Cannot approve loan with high risk score. Must escalate to compliance.");
                }
                return approveLoan(assignment, loan, request.getRemarks());
                
            case "REJECT":
                if (!canApproveReject) {
                    throw new RuntimeException("Cannot reject loan with high risk score. Must escalate to compliance.");
                }
                return rejectLoan(assignment, loan, request.getRejectionReason());
                
            case "ESCALATE_TO_COMPLIANCE":
                return escalateToCompliance(request.getAssignmentId(), request.getRemarks());
                
            default:
                throw new RuntimeException("Invalid action: " + request.getAction());
        }
    }
    
    @Override
    public LoanScreeningResponse escalateToCompliance(Long assignmentId, String remarks) {
        log.info("Escalating assignment {} to compliance", assignmentId);
        
        OfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        ApplicantLoanDetails loan = getLoanForApplicant(assignment.getApplicant().getApplicantId());
        
        // Find available compliance officer for this loan type
        List<ComplianceOfficer> availableOfficers = complianceOfficerRepository
                .findByLoanTypeOrderByWorkload(loan.getLoanType());
        
        if (availableOfficers.isEmpty()) {
            throw new RuntimeException("No compliance officers available for loan type: " + loan.getLoanType());
        }
        
        ComplianceOfficer complianceOfficer = availableOfficers.get(0);
        
        // Create compliance assignment
        ComplianceOfficerApplicationAssignment complianceAssignment = new ComplianceOfficerApplicationAssignment();
        complianceAssignment.setComplianceOfficer(complianceOfficer);
        complianceAssignment.setApplicant(assignment.getApplicant());
        complianceAssignment.setStatus("PENDING");
        complianceAssignment.setPriority("HIGH"); // Escalated cases are high priority
        complianceAssignment.setRemarks("Escalated from loan officer: " + (remarks != null ? remarks : "High risk score"));
        
        complianceAssignmentRepository.save(complianceAssignment);
        
        // Update original assignment status
        assignment.setStatus("ESCALATED_TO_COMPLIANCE");
        assignment.setRemarks("Escalated to compliance officer: " + complianceOfficer.getFirstName() + " " + complianceOfficer.getLastName());
        assignment.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        
        log.info("Assignment {} escalated to compliance officer {}", assignmentId, complianceOfficer.getOfficerId());
        
        return mapToScreeningResponse(assignment);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LoanScreeningResponse> getComplianceEscalations() {
        log.info("Getting compliance escalations");
        
        List<ComplianceOfficerApplicationAssignment> assignments = complianceAssignmentRepository
                .findByStatus("PENDING");
        
        return assignments.stream()
                .map(this::mapComplianceToScreeningResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public LoanScreeningResponse processComplianceDecision(Long complianceOfficerId, LoanScreeningRequest request) {
        log.info("Processing compliance decision by officer {} for assignment {}", complianceOfficerId, request.getAssignmentId());
        
        ComplianceOfficerApplicationAssignment assignment = complianceAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Compliance assignment not found with ID: " + request.getAssignmentId()));
        
        // Verify the compliance officer owns this assignment
        if (!assignment.getComplianceOfficer().getOfficerId().equals(complianceOfficerId)) {
            throw new RuntimeException("Compliance officer is not authorized to process this assignment");
        }
        
        ApplicantLoanDetails loan = getLoanForApplicant(assignment.getApplicant().getApplicantId());
        
        switch (request.getAction().toUpperCase()) {
            case "APPROVE":
                return approveComplianceLoan(assignment, loan, request.getRemarks());
                
            case "REJECT":
                return rejectComplianceLoan(assignment, loan, request.getRejectionReason());
                
            default:
                throw new RuntimeException("Invalid action for compliance: " + request.getAction());
        }
    }
    
    private LoanScreeningResponse approveLoan(OfficerApplicationAssignment assignment, ApplicantLoanDetails loan, String remarks) {
        assignment.setStatus("APPROVED");
        assignment.setRemarks(remarks);
        assignment.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        
        // Update loan status
        loan.setStatus("approved");
        loan.setReviewedAt(LocalDateTime.now());
        loanRepository.save(loan);
        
        log.info("Loan {} approved by officer {}", loan.getLoanId(), assignment.getOfficer().getOfficerId());
        
        return mapToScreeningResponse(assignment);
    }
    
    private LoanScreeningResponse rejectLoan(OfficerApplicationAssignment assignment, ApplicantLoanDetails loan, String rejectionReason) {
        assignment.setStatus("REJECTED");
        assignment.setRemarks(rejectionReason);
        assignment.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        
        // Update loan status
        loan.setStatus("rejected");
        loan.setReviewedAt(LocalDateTime.now());
        loanRepository.save(loan);
        
        log.info("Loan {} rejected by officer {}", loan.getLoanId(), assignment.getOfficer().getOfficerId());
        
        return mapToScreeningResponse(assignment);
    }
    
    private LoanScreeningResponse approveComplianceLoan(ComplianceOfficerApplicationAssignment assignment, ApplicantLoanDetails loan, String remarks) {
        assignment.setStatus("APPROVED");
        assignment.setRemarks(remarks);
        assignment.setCompletedAt(LocalDateTime.now());
        complianceAssignmentRepository.save(assignment);
        
        // Update loan status
        loan.setStatus("approved");
        loan.setReviewedAt(LocalDateTime.now());
        loanRepository.save(loan);
        
        log.info("Loan {} approved by compliance officer {}", loan.getLoanId(), assignment.getComplianceOfficer().getOfficerId());
        
        return mapComplianceToScreeningResponse(assignment);
    }
    
    private LoanScreeningResponse rejectComplianceLoan(ComplianceOfficerApplicationAssignment assignment, ApplicantLoanDetails loan, String rejectionReason) {
        assignment.setStatus("REJECTED");
        assignment.setRemarks(rejectionReason);
        assignment.setCompletedAt(LocalDateTime.now());
        complianceAssignmentRepository.save(assignment);
        
        // Update loan status
        loan.setStatus("rejected");
        loan.setReviewedAt(LocalDateTime.now());
        loanRepository.save(loan);
        
        log.info("Loan {} rejected by compliance officer {}", loan.getLoanId(), assignment.getComplianceOfficer().getOfficerId());
        
        return mapComplianceToScreeningResponse(assignment);
    }
    
    private ApplicantLoanDetails getLoanForApplicant(Long applicantId) {
        return loanRepository.findByApplicant_ApplicantId(applicantId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Loan not found for applicant: " + applicantId));
    }
    
    private String determineRiskLevel(Integer riskScore) {
        if (riskScore < 30) return "LOW";
        if (riskScore < riskScoreThreshold) return "MEDIUM";
        return "HIGH";
    }
    
    private LoanScreeningResponse mapToScreeningResponse(OfficerApplicationAssignment assignment) {
        ApplicantLoanDetails loan = getLoanForApplicant(assignment.getApplicant().getApplicantId());
        
        LoanScreeningResponse response = new LoanScreeningResponse();
        response.setAssignmentId(assignment.getAssignmentId());
        response.setLoanId(loan.getLoanId());
        response.setApplicantId(assignment.getApplicant().getApplicantId());
        response.setApplicantName(assignment.getApplicant().getFirstName() + " " + assignment.getApplicant().getLastName());
        response.setLoanType(loan.getLoanType());
        response.setLoanAmount(loan.getLoanAmount());
        response.setRiskScore(loan.getRiskScore());
        response.setRiskLevel(determineRiskLevel(loan.getRiskScore()));
        response.setCanApproveReject(loan.getRiskScore() < riskScoreThreshold);
        response.setStatus(assignment.getStatus());
        response.setRemarks(assignment.getRemarks());
        response.setAssignedAt(assignment.getAssignedAt());
        response.setProcessedAt(assignment.getCompletedAt());
        response.setOfficerId(assignment.getOfficer().getOfficerId());
        response.setOfficerName(assignment.getOfficer().getFirstName() + " " + assignment.getOfficer().getLastName());
        response.setOfficerType("LOAN_OFFICER");
        
        return response;
    }
    
    private LoanScreeningResponse mapComplianceToScreeningResponse(ComplianceOfficerApplicationAssignment assignment) {
        ApplicantLoanDetails loan = getLoanForApplicant(assignment.getApplicant().getApplicantId());
        
        LoanScreeningResponse response = new LoanScreeningResponse();
        response.setAssignmentId(assignment.getAssignmentId());
        response.setLoanId(loan.getLoanId());
        response.setApplicantId(assignment.getApplicant().getApplicantId());
        response.setApplicantName(assignment.getApplicant().getFirstName() + " " + assignment.getApplicant().getLastName());
        response.setLoanType(loan.getLoanType());
        response.setLoanAmount(loan.getLoanAmount());
        response.setRiskScore(loan.getRiskScore());
        response.setRiskLevel(determineRiskLevel(loan.getRiskScore()));
        response.setCanApproveReject(true); // Compliance officers can always approve/reject
        response.setStatus(assignment.getStatus());
        response.setRemarks(assignment.getRemarks());
        response.setAssignedAt(assignment.getAssignedAt());
        response.setProcessedAt(assignment.getCompletedAt());
        response.setOfficerId(assignment.getComplianceOfficer().getOfficerId());
        response.setOfficerName(assignment.getComplianceOfficer().getFirstName() + " " + assignment.getComplianceOfficer().getLastName());
        response.setOfficerType("COMPLIANCE_OFFICER");
        
        return response;
    }
}
