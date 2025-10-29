package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.LoanScreeningRequest;
import com.tss.springsecurity.dto.LoanScreeningResponse;
import com.tss.springsecurity.dto.LoanScreeningDecision;
import com.tss.springsecurity.dto.ScreeningDashboardResponse;
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
        
        // Return all assignments (including completed ones) for dashboard statistics
        return assignments.stream()
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
        
        // Check if assignment is already processed
        if ("APPROVED".equals(assignment.getStatus()) || "REJECTED".equals(assignment.getStatus()) || 
            "ESCALATED_TO_COMPLIANCE".equals(assignment.getStatus())) {
            throw new RuntimeException("This assignment has already been processed with status: " + assignment.getStatus());
        }
        
        // Verify the officer owns this assignment
        if (!assignment.getOfficer().getOfficerId().equals(officerId)) {
            throw new RuntimeException("Officer is not authorized to process this assignment");
        }
        
        ApplicantLoanDetails loan = getLoanForApplicant(assignment.getApplicant().getApplicantId());
        log.info("Found loan {} for applicant {}, current risk score: {}", loan.getLoanId(), assignment.getApplicant().getApplicantId(), loan.getRiskScore());
        
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
        log.info("Risk score: {}, Threshold: {}, Can approve/reject: {}", loan.getRiskScore(), riskScoreThreshold, canApproveReject);
        
        switch (request.getAction().toUpperCase()) {
            case "APPROVE":
                if (!canApproveReject) {
                    log.warn("Cannot approve loan {} - risk score {} exceeds threshold {}", loan.getLoanId(), loan.getRiskScore(), riskScoreThreshold);
                    throw new RuntimeException("Cannot approve loan with high risk score (" + loan.getRiskScore() + "). Must escalate to compliance.");
                }
                log.info("Approving loan {} by officer {}", loan.getLoanId(), officerId);
                return approveLoan(assignment, loan, request.getRemarks());
                
            case "REJECT":
                if (!canApproveReject) {
                    log.warn("Cannot reject loan {} - risk score {} exceeds threshold {}", loan.getLoanId(), loan.getRiskScore(), riskScoreThreshold);
                    throw new RuntimeException("Cannot reject loan with high risk score (" + loan.getRiskScore() + "). Must escalate to compliance.");
                }
                log.info("Rejecting loan {} by officer {}", loan.getLoanId(), officerId);
                return rejectLoan(assignment, loan, request.getRejectionReason());
                
            case "ESCALATE_TO_COMPLIANCE":
                log.info("Escalating loan {} to compliance", loan.getLoanId());
                return escalateToCompliance(request.getAssignmentId(), request.getRemarks());
                
            default:
                log.error("Invalid action received: {}", request.getAction());
                throw new RuntimeException("Invalid action: " + request.getAction());
        }
    }
    
    @Override
    public LoanScreeningResponse escalateToCompliance(Long assignmentId, String remarks) {
        log.info("Escalating assignment {} to compliance", assignmentId);
        
        OfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        ApplicantLoanDetails loan = getLoanForApplicant(assignment.getApplicant().getApplicantId());
        
        // Find any available compliance officer (compliance officers handle all loan types)
        List<ComplianceOfficer> availableOfficers = complianceOfficerRepository.findAllComplianceOfficersOrderByWorkload();
        
        if (availableOfficers.isEmpty()) {
            throw new RuntimeException("No compliance officers available");
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
    
    @Override
    @Transactional(readOnly = true)
    public ScreeningDashboardResponse getScreeningDashboard(Long officerId) {
        log.info("Getting screening dashboard for officer: {}", officerId);
        
        LoanOfficer officer = loanOfficerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Loan officer not found with ID: " + officerId));
        
        // Get basic officer info
        String officerName = officer.getFirstName() + " " + officer.getLastName();
        
        // Calculate statistics
        List<OfficerApplicationAssignment> allAssignments = assignmentRepository.findByOfficer_OfficerId(officerId);
        List<OfficerApplicationAssignment> pendingAssignments = allAssignments.stream()
                .filter(a -> "PENDING".equals(a.getStatus()) || "IN_PROGRESS".equals(a.getStatus()))
                .toList();
        
        // Count completed today
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long completedToday = allAssignments.stream()
                .filter(a -> a.getCompletedAt() != null && a.getCompletedAt().isAfter(startOfDay))
                .count();
        
        // Count escalated loans
        long escalatedLoans = allAssignments.stream()
                .filter(a -> "ESCALATED_TO_COMPLIANCE".equals(a.getStatus()))
                .count();
        
        // Get recent assignments (last 5)
        List<LoanScreeningResponse> recentAssignments = pendingAssignments.stream()
                .limit(5)
                .map(this::mapToScreeningResponse)
                .collect(Collectors.toList());
        
        // Get urgent loans (high risk or overdue)
        List<LoanScreeningResponse> urgentLoans = pendingAssignments.stream()
                .filter(a -> {
                    ApplicantLoanDetails loan = getLoanForApplicant(a.getApplicant().getApplicantId());
                    return loan.getRiskScore() >= riskScoreThreshold || 
                           a.getAssignedAt().isBefore(LocalDateTime.now().minusDays(2));
                })
                .limit(3)
                .map(this::mapToScreeningResponse)
                .collect(Collectors.toList());
        
        // Create dashboard response
        ScreeningDashboardResponse dashboard = new ScreeningDashboardResponse(
                officerId, officerName, officer.getLoanType());
        
        dashboard.setTotalAssignedLoans(allAssignments.size());
        dashboard.setPendingScreenings(pendingAssignments.size());
        dashboard.setCompletedToday((int) completedToday);
        dashboard.setEscalatedLoans((int) escalatedLoans);
        dashboard.setRecentAssignments(recentAssignments);
        dashboard.setUrgentLoans(urgentLoans);
        
        // Set workload status
        if (pendingAssignments.size() <= 5) {
            dashboard.setWorkloadStatus("LIGHT");
        } else if (pendingAssignments.size() <= 15) {
            dashboard.setWorkloadStatus("NORMAL");
        } else if (pendingAssignments.size() <= 25) {
            dashboard.setWorkloadStatus("HEAVY");
        } else {
            dashboard.setWorkloadStatus("OVERLOADED");
        }
        
        return dashboard;
    }
    
    @Override
    public LoanScreeningResponse screenAssignedLoan(Long officerId, Long assignmentId, LoanScreeningDecision decision) {
        log.info("Screening assigned loan - Officer: {}, Assignment: {}, Decision: {}", 
                officerId, assignmentId, decision.getDecision());
        
        OfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        // Verify the officer owns this assignment
        if (!assignment.getOfficer().getOfficerId().equals(officerId)) {
            throw new RuntimeException("Officer is not authorized to process this assignment");
        }
        
        ApplicantLoanDetails loan = getLoanForApplicant(assignment.getApplicant().getApplicantId());
        
        // Process the decision
        switch (decision.getDecision().toUpperCase()) {
            case "APPROVE":
                return processApprovalDecision(assignment, loan, decision);
                
            case "REJECT":
                return processRejectionDecision(assignment, loan, decision);
                
            case "ESCALATE":
                return escalateToCompliance(assignmentId, decision.getRemarks());
                
            case "NEED_MORE_INFO":
                return requestMoreInformation(assignment, loan, decision);
                
            default:
                throw new RuntimeException("Invalid decision: " + decision.getDecision());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LoanScreeningResponse> getScreeningHistory(Long officerId, int page, int size) {
        log.info("Getting screening history for officer: {}, page: {}, size: {}", officerId, page, size);
        
        List<OfficerApplicationAssignment> completedAssignments = assignmentRepository
                .findByOfficer_OfficerId(officerId).stream()
                .filter(a -> "APPROVED".equals(a.getStatus()) || 
                           "REJECTED".equals(a.getStatus()) || 
                           "ESCALATED_TO_COMPLIANCE".equals(a.getStatus()))
                .sorted((a1, a2) -> a2.getCompletedAt().compareTo(a1.getCompletedAt()))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
        
        return completedAssignments.stream()
                .map(this::mapToScreeningResponse)
                .collect(Collectors.toList());
    }
    
    private LoanScreeningResponse processApprovalDecision(OfficerApplicationAssignment assignment, 
                                                         ApplicantLoanDetails loan, 
                                                         LoanScreeningDecision decision) {
        // Check if officer can approve based on risk score
        if (loan.getRiskScore() >= riskScoreThreshold && 
            (decision.getRequiresManagerApproval() == null || !decision.getRequiresManagerApproval())) {
            throw new RuntimeException("Cannot approve high-risk loan without manager approval. Please escalate to compliance.");
        }
        
        assignment.setStatus("APPROVED");
        assignment.setRemarks(decision.getRemarks());
        assignment.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        
        // Update loan status
        loan.setStatus("approved");
        loan.setReviewedAt(LocalDateTime.now());
        loanRepository.save(loan);
        
        log.info("Loan {} approved by officer {}", loan.getLoanId(), assignment.getOfficer().getOfficerId());
        
        return mapToScreeningResponse(assignment);
    }
    
    private LoanScreeningResponse processRejectionDecision(OfficerApplicationAssignment assignment, 
                                                          ApplicantLoanDetails loan, 
                                                          LoanScreeningDecision decision) {
        assignment.setStatus("REJECTED");
        assignment.setRemarks(decision.getRejectionReason() != null ? 
                             decision.getRejectionReason() : decision.getRemarks());
        assignment.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        
        // Update loan status
        loan.setStatus("rejected");
        loan.setReviewedAt(LocalDateTime.now());
        loanRepository.save(loan);
        
        log.info("Loan {} rejected by officer {}", loan.getLoanId(), assignment.getOfficer().getOfficerId());
        
        return mapToScreeningResponse(assignment);
    }
    
    private LoanScreeningResponse requestMoreInformation(OfficerApplicationAssignment assignment, 
                                                        ApplicantLoanDetails loan, 
                                                        LoanScreeningDecision decision) {
        assignment.setStatus("PENDING_INFO");
        assignment.setRemarks("Additional information requested: " + decision.getRemarks());
        assignmentRepository.save(assignment);
        
        log.info("More information requested for loan {} by officer {}", 
                loan.getLoanId(), assignment.getOfficer().getOfficerId());
        
        return mapToScreeningResponse(assignment);
    }
}
