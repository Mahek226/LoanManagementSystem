package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.LoanApplicationResponse;
import com.tss.springsecurity.dto.LoanProgressTimelineResponse;
import com.tss.springsecurity.entity.ApplicantLoanDetails;
import com.tss.springsecurity.entity.OfficerApplicationAssignment;
import com.tss.springsecurity.entity.ComplianceOfficerApplicationAssignment;
import com.tss.springsecurity.repository.ApplicantLoanDetailsRepository;
import com.tss.springsecurity.repository.OfficerApplicationAssignmentRepository;
import com.tss.springsecurity.repository.ComplianceOfficerApplicationAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/loans")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
public class AdminLoanManagementController {

    private final ApplicantLoanDetailsRepository loanRepository;
    private final OfficerApplicationAssignmentRepository officerAssignmentRepository;
    private final ComplianceOfficerApplicationAssignmentRepository complianceAssignmentRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("Getting all loans - page: {}, size: {}", page, size);
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
            Page<ApplicantLoanDetails> loanPage = loanRepository.findAll(pageable);
            
            List<LoanApplicationResponse> loanResponses = loanPage.getContent().stream()
                    .map(this::mapToLoanApplicationResponse)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", loanResponses);
            response.put("totalElements", loanPage.getTotalElements());
            response.put("totalPages", loanPage.getTotalPages());
            response.put("currentPage", loanPage.getNumber());
            
            log.info("Found {} loans", loanPage.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting loans", e);
            throw new RuntimeException("Failed to retrieve loans: " + e.getMessage());
        }
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanApplicationResponse> getLoanById(@PathVariable Long loanId) {
        try {
            log.info("Getting loan by ID: {}", loanId);
            
            ApplicantLoanDetails loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
            
            return ResponseEntity.ok(mapToLoanApplicationResponse(loan));
        } catch (Exception e) {
            log.error("Error getting loan by ID: {}", loanId, e);
            throw new RuntimeException("Failed to retrieve loan: " + e.getMessage());
        }
    }

    @GetMapping("/{loanId}/progress")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanProgressTimelineResponse> getLoanProgress(@PathVariable Long loanId) {
        try {
            log.info("Getting loan progress for loan ID: {}", loanId);
            
            ApplicantLoanDetails loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
            
            LoanProgressTimelineResponse timeline = buildProgressTimeline(loan);
            
            return ResponseEntity.ok(timeline);
        } catch (Exception e) {
            log.error("Error getting loan progress for loan ID: {}", loanId, e);
            throw new RuntimeException("Failed to retrieve loan progress: " + e.getMessage());
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanApplicationResponse>> getLoansByStatus(@PathVariable String status) {
        try {
            log.info("Getting loans by status: {}", status);
            
            List<ApplicantLoanDetails> loans = loanRepository.findByStatus(status);
            List<LoanApplicationResponse> responses = loans.stream()
                    .map(this::mapToLoanApplicationResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting loans by status: {}", status, e);
            throw new RuntimeException("Failed to retrieve loans: " + e.getMessage());
        }
    }

    private LoanApplicationResponse mapToLoanApplicationResponse(ApplicantLoanDetails loan) {
        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setId(loan.getLoanId());
        response.setApplicantId(loan.getApplicant().getApplicantId());
        response.setApplicantName(loan.getApplicant().getFirstName() + " " + loan.getApplicant().getLastName());
        response.setLoanType(formatLoanType(loan.getLoanType()));
        response.setRequestedAmount(loan.getLoanAmount());
        response.setPurpose(loan.getLoanType());
        response.setEmploymentStatus("N/A"); // Not available in current schema
        response.setMonthlyIncome(0.0); // Not available in current schema
        response.setStatus(loan.getStatus());
        response.setAppliedDate(loan.getSubmittedAt() != null ? loan.getSubmittedAt().toString() : null);
        response.setReviewedDate(loan.getReviewedAt() != null ? loan.getReviewedAt().toString() : null);
        
        // Try to find who reviewed it
        List<OfficerApplicationAssignment> officerAssignments = 
                officerAssignmentRepository.findByApplicant_ApplicantId(loan.getApplicant().getApplicantId());
        if (!officerAssignments.isEmpty()) {
            OfficerApplicationAssignment assignment = officerAssignments.get(0);
            if (assignment.getOfficer() != null) {
                response.setReviewedBy(assignment.getOfficer().getFirstName() + " " + assignment.getOfficer().getLastName());
            }
            response.setComments(assignment.getRemarks());
        }
        
        return response;
    }

    private LoanProgressTimelineResponse buildProgressTimeline(ApplicantLoanDetails loan) {
        LoanProgressTimelineResponse timeline = new LoanProgressTimelineResponse();
        timeline.setLoanId(loan.getLoanId());
        timeline.setApplicantId(loan.getApplicant().getApplicantId());
        timeline.setApplicantName(loan.getApplicant().getFirstName() + " " + loan.getApplicant().getLastName());
        timeline.setLoanType(formatLoanType(loan.getLoanType()));
        timeline.setLoanAmount(loan.getLoanAmount());
        timeline.setCurrentStatus(loan.getStatus());
        timeline.setAppliedAt(loan.getSubmittedAt() != null ? loan.getSubmittedAt().toString() : null);
        
        List<LoanProgressTimelineResponse.LoanProgressEvent> events = new ArrayList<>();
        int eventId = 1;
        
        // Event 1: Application Submitted
        LoanProgressTimelineResponse.LoanProgressEvent submissionEvent = new LoanProgressTimelineResponse.LoanProgressEvent();
        submissionEvent.setEventId(eventId++);
        submissionEvent.setEventType("APPLICATION_SUBMITTED");
        submissionEvent.setEventStatus("COMPLETED");
        submissionEvent.setPerformedBy(loan.getApplicant().getFirstName() + " " + loan.getApplicant().getLastName());
        submissionEvent.setPerformedByRole("APPLICANT");
        submissionEvent.setAction("Submitted loan application");
        submissionEvent.setRemarks("Applied for " + formatLoanType(loan.getLoanType()) + " of amount " + loan.getLoanAmount());
        submissionEvent.setTimestamp(loan.getSubmittedAt() != null ? loan.getSubmittedAt().toString() : null);
        events.add(submissionEvent);
        
        // Check for loan officer assignments
        List<OfficerApplicationAssignment> officerAssignments = 
                officerAssignmentRepository.findByApplicant_ApplicantId(loan.getApplicant().getApplicantId());
        
        for (OfficerApplicationAssignment assignment : officerAssignments) {
            // Event: Assigned to Loan Officer
            LoanProgressTimelineResponse.LoanProgressEvent assignmentEvent = new LoanProgressTimelineResponse.LoanProgressEvent();
            assignmentEvent.setEventId(eventId++);
            assignmentEvent.setEventType("ASSIGNED_TO_OFFICER");
            assignmentEvent.setEventStatus("COMPLETED");
            assignmentEvent.setPerformedBy("System");
            assignmentEvent.setPerformedByRole("SYSTEM");
            assignmentEvent.setOfficerId(assignment.getOfficer().getOfficerId());
            String officerName = formatOfficerName(
                    assignment.getOfficer().getFirstName(),
                    assignment.getOfficer().getLastName(),
                    assignment.getOfficer().getUsername());
            assignmentEvent.setOfficerName(officerName);
            assignmentEvent.setAction("Assigned to Loan Officer");
            assignmentEvent.setRemarks("Loan assigned to " + officerName);
            assignmentEvent.setTimestamp(assignment.getAssignedAt() != null ? assignment.getAssignedAt().toString() : null);
            events.add(assignmentEvent);
            
            // Event: Officer Review (if processed)
            if (assignment.getCompletedAt() != null) {
                LoanProgressTimelineResponse.LoanProgressEvent reviewEvent = new LoanProgressTimelineResponse.LoanProgressEvent();
                reviewEvent.setEventId(eventId++);
                reviewEvent.setEventType("OFFICER_REVIEW");
                reviewEvent.setEventStatus(assignment.getStatus());
                String reviewOfficerName = formatOfficerName(
                        assignment.getOfficer().getFirstName(),
                        assignment.getOfficer().getLastName(),
                        assignment.getOfficer().getUsername());
                reviewEvent.setPerformedBy(reviewOfficerName);
                reviewEvent.setPerformedByRole("LOAN_OFFICER");
                reviewEvent.setOfficerId(assignment.getOfficer().getOfficerId());
                reviewEvent.setOfficerName(reviewOfficerName);
                reviewEvent.setAction("Loan Officer Review - " + assignment.getStatus());
                reviewEvent.setRemarks(assignment.getRemarks());
                reviewEvent.setTimestamp(assignment.getCompletedAt().toString());
                events.add(reviewEvent);
            }
        }
        
        // Check for compliance officer assignments (escalated cases)
        List<ComplianceOfficerApplicationAssignment> complianceAssignments = 
                complianceAssignmentRepository.findByApplicant_ApplicantId(loan.getApplicant().getApplicantId());
        
        for (ComplianceOfficerApplicationAssignment compAssignment : complianceAssignments) {
            // Event: Escalated to Compliance
            LoanProgressTimelineResponse.LoanProgressEvent escalationEvent = new LoanProgressTimelineResponse.LoanProgressEvent();
            escalationEvent.setEventId(eventId++);
            escalationEvent.setEventType("ESCALATED");
            escalationEvent.setEventStatus("COMPLETED");
            escalationEvent.setPerformedBy("System");
            escalationEvent.setPerformedByRole("SYSTEM");
            escalationEvent.setAction("Escalated to Compliance");
            escalationEvent.setRemarks("High risk loan escalated to compliance officer");
            escalationEvent.setTimestamp(compAssignment.getAssignedAt() != null ? compAssignment.getAssignedAt().toString() : null);
            events.add(escalationEvent);
            
            // Event: Compliance Review (if processed)
            if (compAssignment.getCompletedAt() != null) {
                LoanProgressTimelineResponse.LoanProgressEvent compReviewEvent = new LoanProgressTimelineResponse.LoanProgressEvent();
                compReviewEvent.setEventId(eventId++);
                compReviewEvent.setEventType("COMPLIANCE_REVIEW");
                compReviewEvent.setEventStatus(compAssignment.getStatus());
                String compOfficerName = formatOfficerName(
                        compAssignment.getComplianceOfficer().getFirstName(),
                        compAssignment.getComplianceOfficer().getLastName(),
                        compAssignment.getComplianceOfficer().getUsername());
                compReviewEvent.setPerformedBy(compOfficerName);
                compReviewEvent.setPerformedByRole("COMPLIANCE_OFFICER");
                compReviewEvent.setOfficerId(compAssignment.getComplianceOfficer().getOfficerId());
                compReviewEvent.setOfficerName(compOfficerName);
                compReviewEvent.setAction("Compliance Officer Review - " + compAssignment.getStatus());
                compReviewEvent.setRemarks(compAssignment.getRemarks());
                compReviewEvent.setTimestamp(compAssignment.getCompletedAt().toString());
                events.add(compReviewEvent);
            }
        }
        
        // Final Event: Approved or Rejected (handled by loan officer, not system)
        if ("approved".equalsIgnoreCase(loan.getStatus()) || "rejected".equalsIgnoreCase(loan.getStatus())) {
            // Find the loan officer who made the final decision
            String finalDecisionOfficer = "Unknown Officer";
            String finalDecisionRole = "LOAN_OFFICER";
            Long finalOfficerId = null;
            
            // Check if there's a reviewed by officer in the loan record
            if (loan.getReviewedBy() != null && !loan.getReviewedBy().trim().isEmpty()) {
                finalDecisionOfficer = loan.getReviewedBy();
            } else {
                // Find the last officer assignment that was completed
                for (OfficerApplicationAssignment assignment : officerAssignments) {
                    if (assignment.getCompletedAt() != null) {
                        finalDecisionOfficer = formatOfficerName(
                                assignment.getOfficer().getFirstName(),
                                assignment.getOfficer().getLastName(),
                                assignment.getOfficer().getUsername());
                        finalOfficerId = assignment.getOfficer().getOfficerId();
                        break;
                    }
                }
                
                // If no loan officer found, check compliance officers
                if ("Unknown Officer".equals(finalDecisionOfficer)) {
                    for (ComplianceOfficerApplicationAssignment compAssignment : complianceAssignments) {
                        if (compAssignment.getCompletedAt() != null) {
                            finalDecisionOfficer = formatOfficerName(
                                    compAssignment.getComplianceOfficer().getFirstName(),
                                    compAssignment.getComplianceOfficer().getLastName(),
                                    compAssignment.getComplianceOfficer().getUsername());
                            finalDecisionRole = "COMPLIANCE_OFFICER";
                            finalOfficerId = compAssignment.getComplianceOfficer().getOfficerId();
                            break;
                        }
                    }
                }
            }
            
            LoanProgressTimelineResponse.LoanProgressEvent finalEvent = new LoanProgressTimelineResponse.LoanProgressEvent();
            finalEvent.setEventId(eventId++);
            finalEvent.setEventType("approved".equalsIgnoreCase(loan.getStatus()) ? "APPROVED" : "REJECTED");
            finalEvent.setEventStatus("COMPLETED");
            finalEvent.setPerformedBy(finalDecisionOfficer);
            finalEvent.setPerformedByRole(finalDecisionRole);
            finalEvent.setOfficerId(finalOfficerId);
            finalEvent.setOfficerName(finalDecisionOfficer);
            finalEvent.setAction("approved".equalsIgnoreCase(loan.getStatus()) ? "Loan Approved" : "Loan Rejected");
            finalEvent.setRemarks("approved".equalsIgnoreCase(loan.getStatus()) ? 
                    "Loan application has been approved by " + finalDecisionOfficer : 
                    "Loan application has been rejected by " + finalDecisionOfficer);
            finalEvent.setTimestamp(loan.getReviewedAt() != null ? loan.getReviewedAt().toString() : null);
            events.add(finalEvent);
        }
        
        // Sort events by timestamp to ensure chronological order
        events.sort((e1, e2) -> {
            if (e1.getTimestamp() == null && e2.getTimestamp() == null) return 0;
            if (e1.getTimestamp() == null) return 1;
            if (e2.getTimestamp() == null) return -1;
            
            try {
                // Parse timestamps and compare
                java.time.LocalDateTime time1 = java.time.LocalDateTime.parse(e1.getTimestamp());
                java.time.LocalDateTime time2 = java.time.LocalDateTime.parse(e2.getTimestamp());
                return time1.compareTo(time2);
            } catch (Exception ex) {
                // If parsing fails, maintain original order
                return 0;
            }
        });
        
        // Reassign event IDs after sorting to maintain sequential order
        for (int i = 0; i < events.size(); i++) {
            events.get(i).setEventId(i + 1);
        }
        
        timeline.setEvents(events);
        return timeline;
    }

    private String formatLoanType(String loanType) {
        if (loanType == null) return "Unknown";
        return loanType.substring(0, 1).toUpperCase() + loanType.substring(1) + " Loan";
    }
    
    private String formatOfficerName(String firstName, String lastName, String username) {
        // If both firstName and lastName are available, use them
        if (firstName != null && !firstName.trim().isEmpty() && 
            lastName != null && !lastName.trim().isEmpty()) {
            return firstName + " " + lastName;
        }
        
        // If only firstName is available
        if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName;
        }
        
        // If only lastName is available
        if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName;
        }
        
        // Fallback to username
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        
        // Last resort
        return "Unknown Officer";
    }
}