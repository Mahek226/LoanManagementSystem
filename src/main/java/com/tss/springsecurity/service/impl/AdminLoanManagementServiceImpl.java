package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.LoanApplicationResponse;
import com.tss.springsecurity.dto.LoanProgressTimelineResponse;
import com.tss.springsecurity.entity.ApplicantLoanDetails;
import com.tss.springsecurity.entity.OfficerApplicationAssignment;
import com.tss.springsecurity.entity.ComplianceOfficerApplicationAssignment;
import com.tss.springsecurity.repository.ApplicantLoanDetailsRepository;
import com.tss.springsecurity.repository.OfficerApplicationAssignmentRepository;
import com.tss.springsecurity.repository.ComplianceOfficerApplicationAssignmentRepository;
import com.tss.springsecurity.service.AdminLoanManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminLoanManagementServiceImpl implements AdminLoanManagementService {

    private final ApplicantLoanDetailsRepository loanDetailsRepository;
    private final OfficerApplicationAssignmentRepository officerAssignmentRepository;
    private final ComplianceOfficerApplicationAssignmentRepository complianceAssignmentRepository;

    @Override
    public Map<String, Object> getAllLoans(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
            Page<ApplicantLoanDetails> loanPage = loanDetailsRepository.findAll(pageable);
            
            List<LoanApplicationResponse> loanResponses = loanPage.getContent().stream()
                    .map(this::convertToLoanApplicationResponse)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("loans", loanResponses);
            response.put("currentPage", loanPage.getNumber());
            response.put("totalElements", loanPage.getTotalElements());
            response.put("totalPages", loanPage.getTotalPages());
            response.put("size", loanPage.getSize());
            
            return response;
        } catch (Exception e) {
            log.error("Error getting all loans", e);
            throw new RuntimeException("Failed to retrieve loans: " + e.getMessage());
        }
    }

    @Override
    public LoanApplicationResponse getLoanById(Long loanId) {
        try {
            ApplicantLoanDetails loan = loanDetailsRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
            
            return convertToLoanApplicationResponse(loan);
        } catch (Exception e) {
            log.error("Error getting loan by ID: {}", loanId, e);
            throw new RuntimeException("Failed to retrieve loan: " + e.getMessage());
        }
    }

    @Override
    public LoanProgressTimelineResponse getLoanProgress(Long loanId) {
        try {
            ApplicantLoanDetails loan = loanDetailsRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
            
            LoanProgressTimelineResponse timeline = new LoanProgressTimelineResponse();
            timeline.setLoanId(loanId);
            timeline.setCurrentStatus(loan.getLoanStatus());
            timeline.setLoanType(loan.getLoanType());
            timeline.setLoanAmount(loan.getLoanAmount());
            
            if (loan.getApplicant() != null) {
                timeline.setApplicantId(loan.getApplicant().getApplicantId());
                timeline.setApplicantName(loan.getApplicant().getFirstName() + " " + loan.getApplicant().getLastName());
            }
            
            if (loan.getSubmittedAt() != null) {
                timeline.setAppliedAt(loan.getSubmittedAt().toString());
            }
            
            // Build timeline events
            List<LoanProgressTimelineResponse.LoanProgressEvent> events = new ArrayList<>();
            
            // Application submitted
            if (loan.getSubmittedAt() != null) {
                events.add(createProgressEvent(
                    1,
                    "APPLICATION_SUBMITTED",
                    "COMPLETED",
                    "Applicant",
                    "APPLICANT",
                    null,
                    null,
                    "Application Submitted",
                    "Loan application submitted by applicant",
                    loan.getSubmittedAt().toString()
                ));
            }
            
            // Check for officer assignment
            List<OfficerApplicationAssignment> officerAssignments = 
                officerAssignmentRepository.findByLoan_LoanId(loanId);
            
            int eventCounter = 2;
            for (OfficerApplicationAssignment assignment : officerAssignments) {
                if (assignment.getAssignedAt() != null) {
                    events.add(createProgressEvent(
                        eventCounter++,
                        "ASSIGNED_TO_OFFICER",
                        "COMPLETED",
                        "System",
                        "SYSTEM",
                        assignment.getOfficer().getOfficerId(),
                        "Loan Officer #" + assignment.getOfficer().getOfficerId(),
                        "Assigned to Loan Officer",
                        "Application assigned to loan officer for review",
                        assignment.getAssignedAt().toString()
                    ));
                }
                
                if (assignment.getCompletedAt() != null) {
                    String status = assignment.getStatus();
                    String eventType = status.contains("APPROVED") ? "OFFICER_APPROVED" : 
                                     status.contains("REJECTED") ? "OFFICER_REJECTED" : 
                                     "OFFICER_REVIEW";
                    
                    events.add(createProgressEvent(
                        eventCounter++,
                        eventType,
                        "COMPLETED",
                        "Loan Officer #" + assignment.getOfficer().getOfficerId(),
                        "LOAN_OFFICER",
                        assignment.getOfficer().getOfficerId(),
                        "Loan Officer #" + assignment.getOfficer().getOfficerId(),
                        "Officer Decision",
                        "Loan officer completed review: " + status,
                        assignment.getCompletedAt().toString()
                    ));
                }
            }
            
            // Check for compliance assignment
            List<ComplianceOfficerApplicationAssignment> complianceAssignments = 
                complianceAssignmentRepository.findByLoan_LoanId(loanId);
            
            for (ComplianceOfficerApplicationAssignment assignment : complianceAssignments) {
                if (assignment.getAssignedAt() != null) {
                    events.add(createProgressEvent(
                        eventCounter++,
                        "ESCALATED",
                        "COMPLETED",
                        "System",
                        "SYSTEM",
                        null,
                        null,
                        "Escalated to Compliance",
                        "Application escalated to compliance officer",
                        assignment.getAssignedAt().toString()
                    ));
                }
                
                if (assignment.getCompletedAt() != null) {
                    events.add(createProgressEvent(
                        eventCounter++,
                        "COMPLIANCE_REVIEW",
                        "COMPLETED",
                        "Compliance Officer #" + assignment.getComplianceOfficer().getOfficerId(),
                        "COMPLIANCE_OFFICER",
                        assignment.getComplianceOfficer().getOfficerId(),
                        "Compliance Officer #" + assignment.getComplianceOfficer().getOfficerId(),
                        "Compliance Decision",
                        "Compliance officer completed review: " + assignment.getStatus(),
                        assignment.getCompletedAt().toString()
                    ));
                }
            }
            
            // Final decision
            if (loan.getReviewedAt() != null) {
                String finalStatus = loan.getLoanStatus();
                String eventType = "APPROVED".equals(finalStatus) ? "APPROVED" : 
                                 "REJECTED".equals(finalStatus) ? "REJECTED" : 
                                 "FINAL_DECISION";
                
                events.add(createProgressEvent(
                    eventCounter++,
                    eventType,
                    "COMPLETED",
                    loan.getReviewedBy() != null ? loan.getReviewedBy() : "Unknown Officer",
                    "LOAN_OFFICER",
                    null,
                    loan.getReviewedBy() != null ? loan.getReviewedBy() : "Unknown Officer",
                    "Final Decision",
                    "Final loan decision: " + finalStatus,
                    loan.getReviewedAt().toString()
                ));
            }
            
            timeline.setEvents(events);
            
            return timeline;
        } catch (Exception e) {
            log.error("Error getting loan progress for loan ID: {}", loanId, e);
            throw new RuntimeException("Failed to retrieve loan progress: " + e.getMessage());
        }
    }

    @Override
    public List<LoanApplicationResponse> getLoansByStatus(String status) {
        try {
            List<ApplicantLoanDetails> loans;
            
            if ("ALL".equalsIgnoreCase(status)) {
                loans = loanDetailsRepository.findAll(Sort.by("submittedAt").descending());
            } else {
                loans = loanDetailsRepository.findByLoanStatusOrderBySubmittedAtDesc(status);
            }
            
            return loans.stream()
                    .map(this::convertToLoanApplicationResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting loans by status: {}", status, e);
            throw new RuntimeException("Failed to retrieve loans: " + e.getMessage());
        }
    }

    private LoanApplicationResponse convertToLoanApplicationResponse(ApplicantLoanDetails loan) {
        LoanApplicationResponse response = new LoanApplicationResponse();
        
        // Map to the actual DTO fields
        response.setId(loan.getLoanId());
        response.setLoanType(loan.getLoanType());
        response.setRequestedAmount(loan.getLoanAmount());
        response.setPurpose(loan.getLoanPurpose());
        response.setStatus(loan.getLoanStatus());
        response.setLoanTenure(loan.getTenureMonths());
        
        // Set applicant information
        if (loan.getApplicant() != null) {
            response.setApplicantId(loan.getApplicant().getApplicantId());
            response.setApplicantName(loan.getApplicant().getFirstName() + " " + loan.getApplicant().getLastName());
            response.setEmail(loan.getApplicant().getEmail());
            response.setPhone(loan.getApplicant().getPhone());
        }
        
        // Set dates
        if (loan.getSubmittedAt() != null) {
            response.setAppliedDate(loan.getSubmittedAt().toString());
        }
        if (loan.getReviewedAt() != null) {
            response.setReviewedDate(loan.getReviewedAt().toString());
        }
        response.setReviewedBy(loan.getReviewedBy());
        
        // Calculate EMI (simple calculation)
        if (loan.getLoanAmount() != null && loan.getTenureMonths() != null && loan.getInterestRate() != null) {
            double monthlyRate = loan.getInterestRate().doubleValue() / 100 / 12;
            double emi = loan.getLoanAmount().doubleValue() * monthlyRate * 
                        Math.pow(1 + monthlyRate, loan.getTenureMonths()) / 
                        (Math.pow(1 + monthlyRate, loan.getTenureMonths()) - 1);
            response.setEmiAmount(BigDecimal.valueOf(emi));
        }
        
        return response;
    }

    private LoanProgressTimelineResponse.LoanProgressEvent createProgressEvent(
            Integer eventId, String eventType, String eventStatus, String performedBy,
            String performedByRole, Long officerId, String officerName, String action,
            String remarks, String timestamp) {
        
        LoanProgressTimelineResponse.LoanProgressEvent event = new LoanProgressTimelineResponse.LoanProgressEvent();
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setEventStatus(eventStatus);
        event.setPerformedBy(performedBy);
        event.setPerformedByRole(performedByRole);
        event.setOfficerId(officerId);
        event.setOfficerName(officerName);
        event.setAction(action);
        event.setRemarks(remarks);
        event.setTimestamp(timestamp);
        return event;
    }
}
