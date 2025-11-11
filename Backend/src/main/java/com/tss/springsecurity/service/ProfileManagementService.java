package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.ProfileUpdateRequest;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ComplianceOfficer;
import com.tss.springsecurity.entity.LoanOfficer;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.ComplianceOfficerRepository;
import com.tss.springsecurity.repository.LoanOfficerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileManagementService {

    private final ApplicantRepository applicantRepository;
    private final LoanOfficerRepository loanOfficerRepository;
    private final ComplianceOfficerRepository complianceOfficerRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== APPLICANT PROFILE ====================

    public Applicant getApplicantById(Long applicantId) {
        return applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));
    }

    public void updateApplicantProfile(Long applicantId, ProfileUpdateRequest request) {
        Applicant applicant = getApplicantById(applicantId);

        // Update allowed fields
        if (request.getFirstName() != null) {
            applicant.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            applicant.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            applicant.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            applicant.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            applicant.setCity(request.getCity());
        }
        if (request.getState() != null) {
            applicant.setState(request.getState());
        }
        if (request.getCountry() != null) {
            applicant.setCountry(request.getCountry());
        }

        applicantRepository.save(applicant);
    }

    // ==================== LOAN OFFICER PROFILE ====================

    public LoanOfficer getLoanOfficerById(Long officerId) {
        return loanOfficerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Loan Officer not found"));
    }

    public void updateLoanOfficerProfile(Long officerId, ProfileUpdateRequest request) {
        LoanOfficer officer = getLoanOfficerById(officerId);

        // Update allowed fields
        if (request.getFirstName() != null) {
            officer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            officer.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(officer.getEmail())) {
            // Check if email already exists
            if (loanOfficerRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            officer.setEmail(request.getEmail());
        }

        loanOfficerRepository.save(officer);
    }

    // ==================== COMPLIANCE OFFICER PROFILE ====================

    public ComplianceOfficer getComplianceOfficerById(Long officerId) {
        return complianceOfficerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Compliance Officer not found"));
    }

    public void updateComplianceOfficerProfile(Long officerId, ProfileUpdateRequest request) {
        ComplianceOfficer officer = getComplianceOfficerById(officerId);

        // Update allowed fields
        if (request.getFirstName() != null) {
            officer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            officer.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(officer.getEmail())) {
            // Check if email already exists
            if (complianceOfficerRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            officer.setEmail(request.getEmail());
        }

        complianceOfficerRepository.save(officer);
    }

    // ==================== PASSWORD CHANGE (ALL USERS) ====================

    public void changePassword(String userType, Long userId, String currentPassword, String newPassword) {
        switch (userType.toUpperCase()) {
            case "APPLICANT":
                changeApplicantPassword(userId, currentPassword, newPassword);
                break;
            case "LOAN_OFFICER":
                changeLoanOfficerPassword(userId, currentPassword, newPassword);
                break;
            case "COMPLIANCE_OFFICER":
                changeComplianceOfficerPassword(userId, currentPassword, newPassword);
                break;
            default:
                throw new RuntimeException("Invalid user type");
        }
    }

    private void changeApplicantPassword(Long userId, String currentPassword, String newPassword) {
        Applicant applicant = getApplicantById(userId);
        
        if (!passwordEncoder.matches(currentPassword, applicant.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        applicant.setPasswordHash(passwordEncoder.encode(newPassword));
        applicantRepository.save(applicant);
    }

    private void changeLoanOfficerPassword(Long userId, String currentPassword, String newPassword) {
        LoanOfficer officer = getLoanOfficerById(userId);
        
        if (!passwordEncoder.matches(currentPassword, officer.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        officer.setPasswordHash(passwordEncoder.encode(newPassword));
        loanOfficerRepository.save(officer);
    }

    private void changeComplianceOfficerPassword(Long userId, String currentPassword, String newPassword) {
        ComplianceOfficer officer = getComplianceOfficerById(userId);
        
        if (!passwordEncoder.matches(currentPassword, officer.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        officer.setPasswordHash(passwordEncoder.encode(newPassword));
        complianceOfficerRepository.save(officer);
    }
}
