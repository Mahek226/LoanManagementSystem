package com.tss.springsecurity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tss.springsecurity.dto.*;
import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import com.tss.springsecurity.service.ComplianceOfficerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceOfficerServiceImpl implements ComplianceOfficerService {
    
    private final ComplianceOfficerApplicationAssignmentRepository assignmentRepository;
    private final ComplianceOfficerRepository complianceOfficerRepository;
    private final DocumentResubmissionRepository documentResubmissionRepository;
    private final ApplicantRepository applicantRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    public LoanScreeningResponse getLoanScreeningDetails(Long assignmentId) {
        log.info("Getting loan screening details for assignment ID: {}", assignmentId);
        
        ComplianceOfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        return mapToLoanScreeningResponse(assignment);
    }
    
    @Override
    @Transactional
    public DocumentResubmissionResponse requestDocumentResubmission(DocumentResubmissionRequest request) {
        log.info("Processing document resubmission request for assignment ID: {}", request.getAssignmentId());
        
        ComplianceOfficerApplicationAssignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + request.getAssignmentId()));
        
        // Create document resubmission record
        DocumentResubmission resubmission = new DocumentResubmission();
        resubmission.setAssignment(assignment);
        resubmission.setApplicant(assignment.getApplicant());
        resubmission.setRequestedByOfficer(assignment.getComplianceOfficer());
        
        try {
            resubmission.setRequestedDocuments(objectMapper.writeValueAsString(request.getDocumentTypes()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing document types", e);
        }
        
        resubmission.setReason(request.getReason());
        resubmission.setAdditionalComments(request.getAdditionalComments());
        resubmission.setPriorityLevel(request.getPriorityLevel());
        resubmission.setStatus("REQUESTED");
        
        DocumentResubmission savedResubmission = documentResubmissionRepository.save(resubmission);
        
        // Update assignment status to indicate document resubmission requested
        assignment.setStatus("DOCUMENT_RESUBMISSION_REQUESTED");
        assignment.setRemarks("Document resubmission requested: " + request.getReason());
        assignmentRepository.save(assignment);
        
        return mapToDocumentResubmissionResponse(savedResubmission);
    }
    
    @Override
    public List<DocumentResubmissionResponse> getDocumentResubmissionRequests(Long assignmentId) {
        log.info("Getting document resubmission requests for assignment ID: {}", assignmentId);
        
        List<DocumentResubmission> resubmissions = documentResubmissionRepository.findByAssignmentIdOrderByRequestedAtDesc(assignmentId);
        
        return resubmissions.stream()
                .map(this::mapToDocumentResubmissionResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public LoanScreeningResponse approveLoan(Long assignmentId, Long complianceOfficerId, String remarks) {
        log.info("Approving loan for assignment ID: {} by compliance officer: {}", assignmentId, complianceOfficerId);
        
        ComplianceOfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        ComplianceOfficer officer = complianceOfficerRepository.findById(complianceOfficerId)
                .orElseThrow(() -> new RuntimeException("Compliance officer not found with ID: " + complianceOfficerId));
        
        // Update assignment status
        assignment.setStatus("APPROVED");
        assignment.setRemarks(remarks != null ? remarks : "Approved by compliance officer");
        assignment.setCompletedAt(LocalDateTime.now());
        
        // Update applicant approval status
        Applicant applicant = assignment.getApplicant();
        applicant.setApprovalStatus("APPROVED");
        applicant.setIsApproved(true);
        
        assignmentRepository.save(assignment);
        applicantRepository.save(applicant);
        
        log.info("Loan approved successfully for assignment ID: {}", assignmentId);
        return mapToLoanScreeningResponse(assignment);
    }
    
    @Override
    @Transactional
    public LoanScreeningResponse rejectLoan(Long assignmentId, Long complianceOfficerId, String rejectionReason, String remarks) {
        log.info("Rejecting loan for assignment ID: {} by compliance officer: {}", assignmentId, complianceOfficerId);
        
        ComplianceOfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        ComplianceOfficer officer = complianceOfficerRepository.findById(complianceOfficerId)
                .orElseThrow(() -> new RuntimeException("Compliance officer not found with ID: " + complianceOfficerId));
        
        // Update assignment status
        assignment.setStatus("REJECTED");
        assignment.setRemarks(String.format("Rejected: %s. %s", rejectionReason, remarks != null ? remarks : ""));
        assignment.setCompletedAt(LocalDateTime.now());
        
        // Update applicant approval status
        Applicant applicant = assignment.getApplicant();
        applicant.setApprovalStatus("REJECTED");
        applicant.setIsApproved(false);
        
        assignmentRepository.save(assignment);
        applicantRepository.save(applicant);
        
        log.info("Loan rejected successfully for assignment ID: {}", assignmentId);
        return mapToLoanScreeningResponse(assignment);
    }
    
    @Override
    @Transactional
    public LoanScreeningResponse processComplianceDecision(Long complianceOfficerId, ComplianceDecisionRequest request) {
        log.info("Processing compliance decision for assignment ID: {} by officer: {}", request.getAssignmentId(), complianceOfficerId);
        
        switch (request.getDecision().toUpperCase()) {
            case "APPROVE":
                return approveLoan(request.getAssignmentId(), complianceOfficerId, request.getRemarks());
            case "REJECT":
                if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                    throw new RuntimeException("Rejection reason is required for rejection decision");
                }
                return rejectLoan(request.getAssignmentId(), complianceOfficerId, request.getRejectionReason(), request.getRemarks());
            case "REQUEST_DOCUMENTS":
                if (request.getDocumentResubmission() == null) {
                    throw new RuntimeException("Document resubmission details are required for REQUEST_DOCUMENTS decision");
                }
                DocumentResubmissionResponse docResponse = requestDocumentResubmission(request.getDocumentResubmission());
                return getLoanScreeningDetails(request.getAssignmentId());
            default:
                throw new RuntimeException("Invalid decision: " + request.getDecision());
        }
    }
    
    @Override
    public Map<String, Object> getComplianceDashboard(Long complianceOfficerId) {
        log.info("Getting compliance dashboard for officer ID: {}", complianceOfficerId);
        
        ComplianceOfficer officer = complianceOfficerRepository.findById(complianceOfficerId)
                .orElseThrow(() -> new RuntimeException("Compliance officer not found with ID: " + complianceOfficerId));
        
        List<ComplianceOfficerApplicationAssignment> assignments = assignmentRepository.findByComplianceOfficer_OfficerId(complianceOfficerId);
        
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("officerId", complianceOfficerId);
        dashboard.put("officerName", officer.getFirstName() + " " + officer.getLastName());
        
        // Assignment statistics
        Map<String, Long> assignmentStats = new HashMap<>();
        assignmentStats.put("total", (long) assignments.size());
        assignmentStats.put("pending", assignments.stream().filter(a -> "PENDING".equals(a.getStatus())).count());
        assignmentStats.put("inProgress", assignments.stream().filter(a -> "IN_PROGRESS".equals(a.getStatus())).count());
        assignmentStats.put("approved", assignments.stream().filter(a -> "APPROVED".equals(a.getStatus())).count());
        assignmentStats.put("rejected", assignments.stream().filter(a -> "REJECTED".equals(a.getStatus())).count());
        assignmentStats.put("documentResubmissionRequested", assignments.stream().filter(a -> "DOCUMENT_RESUBMISSION_REQUESTED".equals(a.getStatus())).count());
        
        dashboard.put("assignmentStats", assignmentStats);
        
        // Document resubmission statistics
        List<DocumentResubmission> docResubmissions = documentResubmissionRepository.findByRequestedByOfficerOfficerIdOrderByRequestedAtDesc(complianceOfficerId);
        Map<String, Long> docStats = new HashMap<>();
        docStats.put("totalRequested", (long) docResubmissions.size());
        docStats.put("pending", docResubmissions.stream().filter(d -> "REQUESTED".equals(d.getStatus())).count());
        docStats.put("submitted", docResubmissions.stream().filter(d -> "SUBMITTED".equals(d.getStatus())).count());
        docStats.put("reviewed", docResubmissions.stream().filter(d -> "REVIEWED".equals(d.getStatus())).count());
        
        dashboard.put("documentStats", docStats);
        
        // Recent assignments
        List<LoanScreeningResponse> recentAssignments = assignments.stream()
                .sorted((a1, a2) -> a2.getAssignedAt().compareTo(a1.getAssignedAt()))
                .limit(5)
                .map(this::mapToLoanScreeningResponse)
                .collect(Collectors.toList());
        
        dashboard.put("recentAssignments", recentAssignments);
        
        return dashboard;
    }
    
    @Override
    public List<DocumentResubmissionResponse> getPendingDocumentRequests() {
        log.info("Getting all pending document resubmission requests");
        
        List<DocumentResubmission> pendingRequests = documentResubmissionRepository.findByStatusOrderByPriorityLevelDescRequestedAtDesc("REQUESTED");
        
        return pendingRequests.stream()
                .map(this::mapToDocumentResubmissionResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanScreeningResponse> getProcessingHistory(Long complianceOfficerId, int page, int size) {
        log.info("Getting processing history for compliance officer ID: {}, page: {}, size: {}", complianceOfficerId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        List<ComplianceOfficerApplicationAssignment> assignments = assignmentRepository.findByComplianceOfficer_OfficerId(complianceOfficerId);
        
        return assignments.stream()
                .filter(a -> "APPROVED".equals(a.getStatus()) || "REJECTED".equals(a.getStatus()))
                .sorted((a1, a2) -> {
                    LocalDateTime date1 = a1.getCompletedAt() != null ? a1.getCompletedAt() : a1.getUpdatedAt();
                    LocalDateTime date2 = a2.getCompletedAt() != null ? a2.getCompletedAt() : a2.getUpdatedAt();
                    return date2.compareTo(date1);
                })
                .skip((long) page * size)
                .limit(size)
                .map(this::mapToLoanScreeningResponse)
                .collect(Collectors.toList());
    }
    
    // Helper methods
    private LoanScreeningResponse mapToLoanScreeningResponse(ComplianceOfficerApplicationAssignment assignment) {
        LoanScreeningResponse response = new LoanScreeningResponse();
        response.setAssignmentId(assignment.getAssignmentId());
        response.setApplicantId(assignment.getApplicant().getApplicantId());
        response.setApplicantName(assignment.getApplicant().getFirstName() + " " + 
                                 (assignment.getApplicant().getLastName() != null ? assignment.getApplicant().getLastName() : ""));
        response.setStatus(assignment.getStatus());
        response.setRemarks(assignment.getRemarks());
        response.setAssignedAt(assignment.getAssignedAt());
        response.setProcessedAt(assignment.getCompletedAt());
        response.setOfficerId(assignment.getComplianceOfficer().getOfficerId());
        response.setOfficerName(assignment.getComplianceOfficer().getFirstName() + " " + 
                               (assignment.getComplianceOfficer().getLastName() != null ? assignment.getComplianceOfficer().getLastName() : ""));
        response.setOfficerType("COMPLIANCE_OFFICER");
        
        // Set loan details if available
        if (assignment.getApplicant().getLoanDetails() != null && !assignment.getApplicant().getLoanDetails().isEmpty()) {
            ApplicantLoanDetails loanDetails = assignment.getApplicant().getLoanDetails().get(0);
            response.setLoanId(loanDetails.getLoanId());
            response.setLoanType(loanDetails.getLoanType());
            response.setLoanAmount(loanDetails.getLoanAmount());
        }
        
        // Set risk assessment if available
        if (assignment.getApplicant().getAuditScores() != null && !assignment.getApplicant().getAuditScores().isEmpty()) {
            AuditScore auditScore = assignment.getApplicant().getAuditScores().get(0);
            response.setRiskScore(auditScore.getCalculatedScore());
            response.setRiskLevel(determineRiskLevel(auditScore.getCalculatedScore()));
        }
        
        response.setCanApproveReject(true); // Compliance officers can always approve/reject
        
        return response;
    }
    
    private DocumentResubmissionResponse mapToDocumentResubmissionResponse(DocumentResubmission resubmission) {
        DocumentResubmissionResponse response = new DocumentResubmissionResponse();
        response.setResubmissionId(resubmission.getResubmissionId());
        response.setAssignmentId(resubmission.getAssignment().getAssignmentId());
        response.setApplicantId(resubmission.getApplicant().getApplicantId());
        response.setApplicantName(resubmission.getApplicant().getFirstName() + " " + 
                                 (resubmission.getApplicant().getLastName() != null ? resubmission.getApplicant().getLastName() : ""));
        response.setApplicantEmail(resubmission.getApplicant().getEmail());
        
        // Parse requested documents from JSON
        try {
            List<String> documents = objectMapper.readValue(resubmission.getRequestedDocuments(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            response.setRequestedDocuments(documents);
        } catch (JsonProcessingException e) {
            log.error("Error parsing requested documents JSON", e);
            response.setRequestedDocuments(Collections.emptyList());
        }
        
        response.setReason(resubmission.getReason());
        response.setAdditionalComments(resubmission.getAdditionalComments());
        response.setPriorityLevel(resubmission.getPriorityLevel());
        response.setStatus(resubmission.getStatus());
        response.setRequestedAt(resubmission.getRequestedAt());
        response.setSubmittedAt(resubmission.getSubmittedAt());
        response.setReviewedAt(resubmission.getReviewedAt());
        response.setRequestedByOfficerId(resubmission.getRequestedByOfficer().getOfficerId());
        response.setRequestedByOfficerName(resubmission.getRequestedByOfficer().getFirstName() + " " + 
                                          (resubmission.getRequestedByOfficer().getLastName() != null ? resubmission.getRequestedByOfficer().getLastName() : ""));
        
        return response;
    }
    
    private String determineRiskLevel(Integer score) {
        if (score == null) return "UNKNOWN";
        if (score >= 80) return "LOW";
        if (score >= 60) return "MEDIUM";
        return "HIGH";
    }
}
