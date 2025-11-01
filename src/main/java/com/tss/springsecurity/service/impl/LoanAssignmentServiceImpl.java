package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.LoanAssignmentRequest;
import com.tss.springsecurity.dto.LoanAssignmentResponse;
import com.tss.springsecurity.dto.OfficerSummary;
import com.tss.springsecurity.entity.ApplicantLoanDetails;
import com.tss.springsecurity.entity.LoanOfficer;
import com.tss.springsecurity.entity.OfficerApplicationAssignment;
import com.tss.springsecurity.repository.ApplicantLoanDetailsRepository;
import com.tss.springsecurity.repository.LoanOfficerRepository;
import com.tss.springsecurity.repository.OfficerApplicationAssignmentRepository;
import com.tss.springsecurity.service.LoanAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanAssignmentServiceImpl implements LoanAssignmentService {
    
    private final ApplicantLoanDetailsRepository loanRepository;
    private final LoanOfficerRepository loanOfficerRepository;
    private final OfficerApplicationAssignmentRepository assignmentRepository;
    
    @Override
    public LoanAssignmentResponse assignLoanToOfficer(LoanAssignmentRequest request) {
        log.info("Assigning loan {} to officer", request.getLoanId());
        
        // Get the loan details
        ApplicantLoanDetails loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + request.getLoanId()));
        
        // Check if loan is already assigned
        Optional<OfficerApplicationAssignment> existingAssignment = assignmentRepository
                .findByApplicant_ApplicantIdAndStatus(loan.getApplicant().getApplicantId(), "PENDING");
        
        if (existingAssignment.isPresent()) {
            throw new RuntimeException("Loan is already assigned to an officer");
        }
        
        // Auto-select officer if not provided
        Long officerId = request.getOfficerId();
        if (officerId == null) {
            log.info("Officer ID not provided, auto-selecting based on loan type: {}", loan.getLoanType());
            officerId = autoSelectBestOfficer(loan.getLoanType());
            log.info("Auto-selected officer ID: {}", officerId);
        }
        
        // Create final variable for lambda usage
        final Long finalOfficerId = officerId;
        
        // Get the officer to assign to
        LoanOfficer assignedOfficer = loanOfficerRepository.findById(finalOfficerId)
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + finalOfficerId));
        
        // Validate that officer handles this loan type
        if (!assignedOfficer.getLoanType().equalsIgnoreCase(loan.getLoanType())) {
            throw new RuntimeException("Officer category (" + assignedOfficer.getLoanType() + 
                    ") does not match loan type (" + loan.getLoanType() + ")");
        }
        
        // Create assignment
        OfficerApplicationAssignment assignment = new OfficerApplicationAssignment();
        assignment.setOfficer(assignedOfficer);
        assignment.setApplicant(loan.getApplicant());
        assignment.setStatus("PENDING");
        assignment.setPriority(request.getPriority());
        assignment.setRemarks(request.getRemarks());
        
        assignment = assignmentRepository.save(assignment);
        
        log.info("Loan {} assigned to officer {} successfully", request.getLoanId(), assignedOfficer.getOfficerId());
        
        return mapToResponse(assignment, loan);
    }
    
    @Override
    public LoanAssignmentResponse reassignLoan(Long assignmentId, Long newOfficerId, String remarks) {
        log.info("Reassigning assignment {} to officer {}", assignmentId, newOfficerId);
        
        OfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        LoanOfficer newOfficer = loanOfficerRepository.findById(newOfficerId)
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + newOfficerId));
        
        // Get loan details to validate officer category
        ApplicantLoanDetails loan = loanRepository.findByApplicant_ApplicantId(assignment.getApplicant().getApplicantId())
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Loan not found for applicant"));
        
        // Validate that new officer handles this loan type
        if (!newOfficer.getLoanType().equalsIgnoreCase(loan.getLoanType())) {
            throw new RuntimeException("Officer category (" + newOfficer.getLoanType() + 
                    ") does not match loan type (" + loan.getLoanType() + ")");
        }
        
        assignment.setOfficer(newOfficer);
        assignment.setRemarks(remarks);
        assignment.setStatus("PENDING"); // Reset status for new officer
        
        assignment = assignmentRepository.save(assignment);
        
        log.info("Assignment {} reassigned to officer {} successfully", assignmentId, newOfficerId);
        
        return mapToResponse(assignment, loan);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LoanAssignmentResponse> getAssignmentsByOfficer(Long officerId) {
        List<OfficerApplicationAssignment> assignments = assignmentRepository.findByOfficer_OfficerId(officerId);
        return assignments.stream()
                .map(this::mapToResponseWithLoan)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LoanAssignmentResponse> getAssignmentsByStatus(String status) {
        List<OfficerApplicationAssignment> assignments = assignmentRepository.findByStatus(status);
        return assignments.stream()
                .map(this::mapToResponseWithLoan)
                .collect(Collectors.toList());
    }
    
    @Override
    public LoanAssignmentResponse updateAssignmentStatus(Long assignmentId, String status, String remarks) {
        log.info("Updating assignment {} status to {}", assignmentId, status);
        
        OfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        assignment.setStatus(status);
        assignment.setRemarks(remarks);
        
        if ("COMPLETED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
            assignment.setCompletedAt(LocalDateTime.now());
        }
        
        assignment = assignmentRepository.save(assignment);
        
        ApplicantLoanDetails loan = loanRepository.findByApplicant_ApplicantId(assignment.getApplicant().getApplicantId())
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Loan not found for applicant"));
        
        log.info("Assignment {} status updated to {} successfully", assignmentId, status);
        
        return mapToResponse(assignment, loan);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LoanAssignmentResponse> getAllAssignments() {
        List<OfficerApplicationAssignment> assignments = assignmentRepository.findAll();
        return assignments.stream()
                .map(this::mapToResponseWithLoan)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public LoanAssignmentResponse getAssignmentById(Long assignmentId) {
        OfficerApplicationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));
        
        return mapToResponseWithLoan(assignment);
    }
    
    private LoanAssignmentResponse mapToResponse(OfficerApplicationAssignment assignment, ApplicantLoanDetails loan) {
        LoanAssignmentResponse response = new LoanAssignmentResponse();
        response.setAssignmentId(assignment.getAssignmentId());
        response.setLoanId(loan.getLoanId());
        response.setApplicantId(assignment.getApplicant().getApplicantId());
        response.setApplicantName(assignment.getApplicant().getFirstName() + " " + assignment.getApplicant().getLastName());
        response.setLoanType(loan.getLoanType());
        response.setOfficerId(assignment.getOfficer().getOfficerId());
        response.setOfficerName(assignment.getOfficer().getFirstName() + " " + assignment.getOfficer().getLastName());
        response.setOfficerEmail(assignment.getOfficer().getEmail());
        response.setStatus(assignment.getStatus());
        response.setPriority(assignment.getPriority());
        response.setRemarks(assignment.getRemarks());
        response.setAssignedAt(assignment.getAssignedAt());
        response.setUpdatedAt(assignment.getUpdatedAt());
        return response;
    }
    
    private LoanAssignmentResponse mapToResponseWithLoan(OfficerApplicationAssignment assignment) {
        ApplicantLoanDetails loan = loanRepository.findByApplicant_ApplicantId(assignment.getApplicant().getApplicantId())
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Loan not found for applicant"));
        
        return mapToResponse(assignment, loan);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OfficerSummary> getAvailableOfficersByLoanType(String loanType) {
        List<LoanOfficer> officers = loanOfficerRepository.findByLoanTypeOrderByWorkload(loanType);
        
        return officers.stream()
                .map(officer -> {
                    Long workload = assignmentRepository.countActiveAssignmentsByOfficer(officer.getOfficerId());
                    return new OfficerSummary(
                            officer.getOfficerId(),
                            officer.getFirstName(),
                            officer.getLastName(),
                            officer.getEmail(),
                            officer.getLoanType(),
                            workload
                    );
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long autoSelectBestOfficer(String loanType) {
        log.info("Auto-selecting best officer for loan type: {}", loanType);
        
        // Get officers by loan type, ordered by workload (ascending)
        List<LoanOfficer> officers = loanOfficerRepository.findByLoanTypeOrderByWorkload(loanType);
        
        if (officers.isEmpty()) {
            throw new RuntimeException("No officers available for loan type: " + loanType);
        }
        
        // Select the officer with the least workload (first in the list)
        LoanOfficer selectedOfficer = officers.get(0);
        Long workload = assignmentRepository.countActiveAssignmentsByOfficer(selectedOfficer.getOfficerId());
        
        log.info("Selected officer: {} {} (ID: {}) with workload: {}",
                selectedOfficer.getFirstName(),
                selectedOfficer.getLastName(),
                selectedOfficer.getOfficerId(),
                workload);
        
        return selectedOfficer.getOfficerId();
    }
}
