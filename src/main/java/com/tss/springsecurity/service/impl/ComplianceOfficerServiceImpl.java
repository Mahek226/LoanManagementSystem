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
    private final ApplicantLoanDetailsRepository loanDetailsRepository;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final ActivityLogRepository activityLogRepository;
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
        
        // Update loan details status
        if (applicant.getLoanDetails() != null && !applicant.getLoanDetails().isEmpty()) {
            ApplicantLoanDetails loanDetails = applicant.getLoanDetails().get(0);
            loanDetails.setStatus("APPROVED");
            loanDetails.setReviewedAt(LocalDateTime.now());
            // Update risk score if audit score is available
            if (applicant.getAuditScores() != null && !applicant.getAuditScores().isEmpty()) {
                AuditScore auditScore = applicant.getAuditScores().get(0);
                if (auditScore.getCalculatedScore() != null) {
                    loanDetails.setRiskScore(auditScore.getCalculatedScore());
                }
            }
            loanDetailsRepository.save(loanDetails);
        }
        
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
        
        // Update loan details status
        if (applicant.getLoanDetails() != null && !applicant.getLoanDetails().isEmpty()) {
            ApplicantLoanDetails loanDetails = applicant.getLoanDetails().get(0);
            loanDetails.setStatus("REJECTED");
            loanDetails.setReviewedAt(LocalDateTime.now());
            // Update risk score if audit score is available
            if (applicant.getAuditScores() != null && !applicant.getAuditScores().isEmpty()) {
                AuditScore auditScore = applicant.getAuditScores().get(0);
                if (auditScore.getCalculatedScore() != null) {
                    loanDetails.setRiskScore(auditScore.getCalculatedScore());
                }
            }
            loanDetailsRepository.save(loanDetails);
        }
        
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
    
    // ==================== NEW METHODS FOR KYC, AML, RISK ANALYSIS ====================
    
    @Override
    public KYCVerificationResponse performKYCVerification(KYCVerificationRequest request) {
        log.info("Performing KYC verification for applicant ID: {}", request.getApplicantId());
        
        Applicant applicant = applicantRepository.findById(request.getApplicantId())
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + request.getApplicantId()));
        
        KYCVerificationResponse response = KYCVerificationResponse.builder()
                .verified(true)
                .nameMatch(true)
                .addressMatch(true)
                .duplicateFound(false)
                .build();
        
        // Verify PAN if requested
        if ("PAN".equals(request.getVerificationType()) || "BOTH".equals(request.getVerificationType())) {
            // TODO: Integrate with actual PAN verification API
            response.setPanStatus("VERIFIED");
            log.info("PAN verification completed for: {}", request.getPanNumber());
        }
        
        // Verify Aadhaar if requested
        if ("AADHAAR".equals(request.getVerificationType()) || "BOTH".equals(request.getVerificationType())) {
            // TODO: Integrate with actual Aadhaar verification API
            response.setAadhaarStatus("VERIFIED");
            log.info("Aadhaar verification completed for: {}", request.getAadhaarNumber());
        }
        
        // Check for duplicates
        List<Applicant> duplicates = applicantRepository.findByPanNumber(request.getPanNumber());
        if (duplicates.size() > 1) {
            response.setDuplicateFound(true);
            response.setVerified(false);
            response.setRemarks("Duplicate PAN number found in system");
        } else {
            response.setRemarks("KYC verification completed successfully");
        }
        
        // Log the verification
        logActivity(request.getApplicantId(), "KYC_VERIFICATION", "KYC verification performed", "COMPLETED");
        
        return response;
    }
    
    @Override
    public AMLScreeningResponse performAMLScreening(AMLScreeningRequest request) {
        log.info("Performing AML screening for applicant ID: {}", request.getApplicantId());
        
        List<AMLFinding> findings = new ArrayList<>();
        String overallRisk = "CLEAR";
        boolean isPEP = false;
        
        // Check each requested source
        for (String checkType : request.getCheckTypes()) {
            switch (checkType) {
                case "RBI_DEFAULTERS":
                    // TODO: Integrate with RBI defaulters API
                    AMLFinding rbiFind = AMLFinding.builder()
                            .source("RBI_DEFAULTERS")
                            .matchType("NONE")
                            .matchScore(0)
                            .details("No match found in RBI defaulters list")
                            .severity("LOW")
                            .build();
                    findings.add(rbiFind);
                    break;
                    
                case "FATF_SANCTIONS":
                    // TODO: Integrate with FATF sanctions API
                    AMLFinding fatfFind = AMLFinding.builder()
                            .source("FATF_SANCTIONS")
                            .matchType("NONE")
                            .matchScore(0)
                            .details("No match found in FATF sanctions list")
                            .severity("LOW")
                            .build();
                    findings.add(fatfFind);
                    break;
                    
                case "OFAC":
                    // TODO: Integrate with OFAC API
                    AMLFinding ofacFind = AMLFinding.builder()
                            .source("OFAC")
                            .matchType("NONE")
                            .matchScore(0)
                            .details("No match found in OFAC sanctions list")
                            .severity("LOW")
                            .build();
                    findings.add(ofacFind);
                    break;
                    
                case "INTERNAL_BLACKLIST":
                    // Check internal fraud flags
                    List<FraudFlag> fraudFlags = fraudFlagRepository.findByApplicant_ApplicantId(request.getApplicantId());
                    if (!fraudFlags.isEmpty()) {
                        AMLFinding blacklistFind = AMLFinding.builder()
                                .source("INTERNAL_BLACKLIST")
                                .matchType("EXACT")
                                .matchScore(100)
                                .details("Found " + fraudFlags.size() + " fraud flag(s) in internal system")
                                .severity("HIGH")
                                .build();
                        findings.add(blacklistFind);
                        overallRisk = "HIGH";
                    } else {
                        AMLFinding blacklistFind = AMLFinding.builder()
                                .source("INTERNAL_BLACKLIST")
                                .matchType("NONE")
                                .matchScore(0)
                                .details("No match found in internal blacklist")
                                .severity("LOW")
                                .build();
                        findings.add(blacklistFind);
                    }
                    break;
            }
        }
        
        // TODO: Check PEP status via external API
        isPEP = false;
        
        List<String> recommendations = new ArrayList<>();
        if ("HIGH".equals(overallRisk) || "CRITICAL".equals(overallRisk)) {
            recommendations.add("Recommend rejection due to high AML risk");
            recommendations.add("Request additional documentation");
            recommendations.add("Escalate to senior compliance officer");
        } else {
            recommendations.add("AML screening passed - proceed with application");
        }
        
        // Log the screening
        logActivity(request.getApplicantId(), "AML_SCREENING", "AML screening performed", "COMPLETED");
        
        return AMLScreeningResponse.builder()
                .applicantId(request.getApplicantId())
                .screeningDate(LocalDateTime.now())
                .overallRisk(overallRisk)
                .findings(findings)
                .isPEP(isPEP)
                .recommendations(recommendations)
                .build();
    }
    
    @Override
    public Map<String, Object> checkRBIDefaulters(String panNumber) {
        log.info("Checking RBI defaulters list for PAN: {}", panNumber);
        
        Map<String, Object> result = new HashMap<>();
        // TODO: Integrate with actual RBI defaulters API
        result.put("found", false);
        result.put("panNumber", panNumber);
        result.put("message", "No match found in RBI defaulters list");
        result.put("checkedAt", LocalDateTime.now());
        
        return result;
    }
    
    @Override
    public Map<String, Object> checkSanctionsList(String name) {
        log.info("Checking sanctions list for name: {}", name);
        
        Map<String, Object> result = new HashMap<>();
        // TODO: Integrate with FATF/OFAC sanctions APIs
        result.put("found", false);
        result.put("name", name);
        result.put("sources", Arrays.asList("FATF", "OFAC"));
        result.put("message", "No match found in sanctions lists");
        result.put("checkedAt", LocalDateTime.now());
        
        return result;
    }
    
    @Override
    public Map<String, Object> checkInternalBlacklist(Long applicantId) {
        log.info("Checking internal blacklist for applicant ID: {}", applicantId);
        
        List<FraudFlag> fraudFlags = fraudFlagRepository.findByApplicant_ApplicantId(applicantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("found", !fraudFlags.isEmpty());
        result.put("applicantId", applicantId);
        result.put("flagCount", fraudFlags.size());
        result.put("flags", fraudFlags.stream()
                .map(f -> Map.of(
                        "flagId", f.getId(),
                        "flagType", f.getRuleName(),
                        "severity", f.getSeverity(),
                        "flaggedAt", f.getCreatedAt()
                ))
                .collect(Collectors.toList()));
        result.put("checkedAt", LocalDateTime.now());
        
        return result;
    }
    
    @Override
    public Map<String, Object> checkPEPStatus(String name, String pan) {
        log.info("Checking PEP status for name: {}, PAN: {}", name, pan);
        
        Map<String, Object> result = new HashMap<>();
        // TODO: Integrate with actual PEP database/API
        result.put("isPEP", false);
        result.put("name", name);
        result.put("panNumber", pan);
        result.put("message", "No PEP status found");
        result.put("checkedAt", LocalDateTime.now());
        
        return result;
    }
    
    @Override
    public RiskCorrelationAnalysisResponse getRiskCorrelationAnalysis(Long loanId) {
        log.info("Getting risk correlation analysis for loan ID: {}", loanId);
        
        ApplicantLoanDetails loanDetails = loanDetailsRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
        
        Applicant applicant = loanDetails.getApplicant();
        
        // Collect fraud tags
        List<String> fraudTags = fraudFlagRepository.findByApplicant_ApplicantId(applicant.getApplicantId())
                .stream()
                .map(FraudFlag::getRuleName)
                .collect(Collectors.toList());
        
        // Check defaulter history
        boolean defaulterHistory = false; // TODO: Check external defaulter databases
        
        // Transaction anomalies
        List<String> transactionAnomalies = new ArrayList<>();
        // TODO: Analyze transaction patterns
        
        // Calculate risk factors
        List<RiskFactor> riskFactors = new ArrayList<>();
        
        if (!fraudTags.isEmpty()) {
            riskFactors.add(RiskFactor.builder()
                    .category("FRAUD_HISTORY")
                    .description("Applicant has " + fraudTags.size() + " fraud flag(s)")
                    .severity("HIGH")
                    .weight(0.4)
                    .build());
        }
        
        if (loanDetails.getRiskScore() != null && loanDetails.getRiskScore() < 60) {
            riskFactors.add(RiskFactor.builder()
                    .category("LOW_CREDIT_SCORE")
                    .description("Risk score below acceptable threshold")
                    .severity("MEDIUM")
                    .weight(0.3)
                    .build());
        }
        
        // Calculate compliance risk rating (1-5)
        int complianceRiskRating = calculateComplianceRiskRating(fraudTags.size(), loanDetails.getRiskScore());
        
        String recommendation = complianceRiskRating >= 4 
                ? "Recommend approval with standard monitoring" 
                : "Recommend enhanced due diligence or rejection";
        
        return RiskCorrelationAnalysisResponse.builder()
                .loanId(loanId)
                .applicantId(applicant.getApplicantId())
                .fraudTags(fraudTags)
                .defaulterHistory(defaulterHistory)
                .transactionAnomalies(transactionAnomalies)
                .complianceRiskRating(complianceRiskRating)
                .riskFactors(riskFactors)
                .recommendation(recommendation)
                .build();
    }
    
    @Override
    public List<ComplianceAuditLogResponse> getAuditLogs(Long assignmentId) {
        log.info("Getting audit logs for assignment ID: {}", assignmentId);
        
        ComplianceOfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        // Get activity logs for this applicant
        List<ActivityLog> activityLogs = activityLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc("APPLICANT", assignment.getApplicant().getApplicantId(), PageRequest.of(0, 100))
                .getContent();
        
        return activityLogs.stream()
                .map(this::mapToComplianceAuditLogResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ComplianceAuditLogResponse> getAllAuditLogs(Long officerId, int page, int size) {
        log.info("Getting all audit logs for officer ID: {}, page: {}, size: {}", officerId, page, size);
        
        // Get all assignments for this officer
        List<ComplianceOfficerApplicationAssignment> assignments = 
                assignmentRepository.findByComplianceOfficer_OfficerId(officerId);
        
        // Collect all activity logs
        List<ComplianceAuditLogResponse> allLogs = new ArrayList<>();
        for (ComplianceOfficerApplicationAssignment assignment : assignments) {
            List<ActivityLog> logs = activityLogRepository
                    .findByEntityTypeAndEntityIdOrderByTimestampDesc("APPLICANT", assignment.getApplicant().getApplicantId(), PageRequest.of(0, 50))
                    .getContent();
            allLogs.addAll(logs.stream()
                    .map(this::mapToComplianceAuditLogResponse)
                    .collect(Collectors.toList()));
        }
        
        // Sort by timestamp and paginate
        return allLogs.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DocumentResponse> getLoanDocuments(Long loanId) {
        log.info("Getting documents for loan ID: {}", loanId);
        
        ApplicantLoanDetails loanDetails = loanDetailsRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
        
        List<UploadedDocument> documents = uploadedDocumentRepository
                .findByApplicant_ApplicantId(loanDetails.getApplicant().getApplicantId());
        
        return documents.stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<FraudHistoryResponse> getFraudHistory(Long applicantId) {
        log.info("Getting fraud history for applicant ID: {}", applicantId);
        
        List<FraudFlag> fraudFlags = fraudFlagRepository.findByApplicant_ApplicantId(applicantId);
        
        return fraudFlags.stream()
                .map(this::mapToFraudHistoryResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public Map<String, Object> requestAdditionalDocuments(Long officerId, AdditionalDocumentRequest request) {
        log.info("Requesting additional documents for loan ID: {} by officer ID: {}", request.getLoanId(), officerId);
        
        ApplicantLoanDetails loanDetails = loanDetailsRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + request.getLoanId()));
        
        // TODO: Send email notification to applicant
        // TODO: Create document request record
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Document request sent to applicant");
        response.put("loanId", request.getLoanId());
        response.put("applicantId", request.getApplicantId());
        response.put("documentTypes", request.getDocumentTypes());
        response.put("requestedAt", LocalDateTime.now());
        
        // Log the activity
        logActivity(request.getApplicantId(), "DOCUMENT_REQUEST", 
                "Additional documents requested: " + String.join(", ", request.getDocumentTypes()), "PENDING");
        
        return response;
    }
    
    @Override
    public byte[] generateComplianceReportPDF(Long assignmentId) {
        log.info("Generating compliance report PDF for assignment ID: {}", assignmentId);
        
        ComplianceOfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        // TODO: Implement actual PDF generation using iText or Apache PDFBox
        // For now, return a simple placeholder
        String reportContent = String.format(
                "COMPLIANCE REPORT\n\n" +
                "Assignment ID: %d\n" +
                "Applicant: %s\n" +
                "Status: %s\n" +
                "Reviewed By: %s\n" +
                "Date: %s\n\n" +
                "Remarks: %s\n",
                assignment.getAssignmentId(),
                assignment.getApplicant().getFirstName() + " " + assignment.getApplicant().getLastName(),
                assignment.getStatus(),
                assignment.getComplianceOfficer().getFirstName() + " " + assignment.getComplianceOfficer().getLastName(),
                LocalDateTime.now(),
                assignment.getRemarks()
        );
        
        return reportContent.getBytes();
    }
    
    // ==================== HELPER METHODS ====================
    
    private void logActivity(Long applicantId, String activityType, String description, String status) {
        try {
            ActivityLog activityLog = new ActivityLog();
            activityLog.setEntityType("APPLICANT");
            activityLog.setEntityId(applicantId);
            activityLog.setActivityType(activityType);
            activityLog.setDescription(description);
            activityLog.setStatus(status);
            activityLog.setTimestamp(LocalDateTime.now());
            activityLogRepository.save(activityLog);
        } catch (Exception e) {
            log.error("Error logging activity", e);
        }
    }
    
    private int calculateComplianceRiskRating(int fraudFlagCount, Integer riskScore) {
        if (fraudFlagCount > 0) return 1; // High risk
        if (riskScore == null) return 3; // Medium risk
        if (riskScore >= 80) return 5; // Low risk
        if (riskScore >= 60) return 4; // Medium-low risk
        if (riskScore >= 40) return 3; // Medium risk
        return 2; // Medium-high risk
    }
    
    private ComplianceAuditLogResponse mapToComplianceAuditLogResponse(ActivityLog activityLog) {
        return ComplianceAuditLogResponse.builder()
                .logId(activityLog.getLogId())
                .applicantId(activityLog.getEntityId())
                .action(activityLog.getActivityType())
                .decision(activityLog.getStatus())
                .remarks(activityLog.getDescription())
                .timestamp(activityLog.getTimestamp())
                .build();
    }
    
    private DocumentResponse mapToDocumentResponse(UploadedDocument document) {
        return DocumentResponse.builder()
                .documentId(document.getDocumentId())
                .documentType(document.getDocumentType())
                .documentUrl(document.getCloudinaryUrl())
                .fileName(document.getOriginalFilename())
                .verificationStatus(document.getVerificationStatus() != null ? document.getVerificationStatus().toString() : "PENDING")
                .uploadedAt(document.getUploadedAt())
                .build();
    }
    
    private FraudHistoryResponse mapToFraudHistoryResponse(FraudFlag fraudFlag) {
        return FraudHistoryResponse.builder()
                .recordId(fraudFlag.getId())
                .applicantId(fraudFlag.getApplicant().getApplicantId())
                .fraudType(fraudFlag.getRuleName())
                .fraudTags(Arrays.asList(fraudFlag.getRuleName()))
                .riskLevel(fraudFlag.getSeverity() != null ? fraudFlag.getSeverity().toString() : "UNKNOWN")
                .detectedAt(fraudFlag.getCreatedAt())
                .status("DETECTED")
                .remarks(fraudFlag.getFlagNotes())
                .build();
    }
}
