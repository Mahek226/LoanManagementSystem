package com.tss.springsecurity.service;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.Admin;
import com.tss.springsecurity.entity.LoanOfficer;
import com.tss.springsecurity.entity.ComplianceOfficer;
import com.tss.springsecurity.payload.request.SignupRequest;
import com.tss.springsecurity.payload.request.ApplicantRegisterRequest;
import com.tss.springsecurity.payload.response.MessageResponse;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.AdminRepository;
import com.tss.springsecurity.repository.LoanOfficerRepository;
import com.tss.springsecurity.repository.ComplianceOfficerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ApplicantRepository applicantRepository;
    private final AdminRepository adminRepository;
    private final LoanOfficerRepository loanOfficerRepository;
    private final ComplianceOfficerRepository complianceOfficerRepository;
    private final PasswordEncoder passwordEncoder;

    public void registerApplicant(ApplicantRegisterRequest request) {
        // Check if username already exists in applicant table
        if (applicantRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        // Check if email already exists in applicant table
        if (applicantRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        // Check if phone already exists
        if (applicantRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number is already in use!");
        }

        // Create new applicant with all details
        Applicant applicant = new Applicant();
        applicant.setUsername(request.getUsername());
        applicant.setEmail(request.getEmail());
        applicant.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        applicant.setFirstName(request.getFirstName());
        applicant.setLastName(request.getLastName());
        applicant.setDob(request.getDob());
        applicant.setGender(request.getGender());
        applicant.setPhone(request.getPhone());
        applicant.setAddress(request.getAddress());
        applicant.setCity(request.getCity());
        applicant.setState(request.getState());
        applicant.setCountry(request.getCountry());
        
        // Set default values
        applicant.setIsApproved(false);
        applicant.setIsEmailVerified(false);
        applicant.setApprovalStatus("PENDING");
        
        // Save the applicant
        applicantRepository.save(applicant);
    }

    public MessageResponse registerUser(SignupRequest signUpRequest) {
        // Get the role as string
        String role = signUpRequest.getRole();
        
        // Validate role
        if (role == null || role.trim().isEmpty()) {
            throw new RuntimeException("Role is required!");
        }

        // If role is APPLICANT, redirect to specific endpoint
        if ("ROLE_APPLICANT".equals(role)) {
            throw new RuntimeException("Please use /api/auth/signup/applicant endpoint for applicant registration!");
        } else if ("ROLE_ADMIN".equals(role)) {
            return registerAdmin(signUpRequest);
        } else if ("ROLE_LOAN_OFFICER".equals(role)) {
            return registerLoanOfficer(signUpRequest);
        } else if ("ROLE_COMPLIANCE_OFFICER".equals(role)) {
            return registerComplianceOfficer(signUpRequest);
        } else {
            throw new RuntimeException("Invalid role specified!");
        }
    }

    private MessageResponse registerAdmin(SignupRequest signUpRequest) {
        // Handle Admin registration
        if (adminRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        if (adminRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        // Create new admin
        Admin admin = new Admin();
        admin.setUsername(signUpRequest.getUsername());
        admin.setEmail(signUpRequest.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
        admin.setFirstName(signUpRequest.getFirstName());
        admin.setLastName(signUpRequest.getLastName());
        
        adminRepository.save(admin);
        return new MessageResponse("Admin registered successfully!");
    }

    private MessageResponse registerLoanOfficer(SignupRequest signUpRequest) {
        // Handle Loan Officer registration
        if (loanOfficerRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        if (loanOfficerRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        // Create new loan officer
        LoanOfficer loanOfficer = new LoanOfficer();
        loanOfficer.setUsername(signUpRequest.getUsername());
        loanOfficer.setEmail(signUpRequest.getEmail());
        loanOfficer.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
        loanOfficer.setFirstName(signUpRequest.getFirstName());
        loanOfficer.setLastName(signUpRequest.getLastName());
        
        loanOfficerRepository.save(loanOfficer);
        return new MessageResponse("Loan Officer registered successfully!");
    }

    private MessageResponse registerComplianceOfficer(SignupRequest signUpRequest) {
        // Handle Compliance Officer registration
        if (complianceOfficerRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        if (complianceOfficerRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        // Create new compliance officer
        ComplianceOfficer complianceOfficer = new ComplianceOfficer();
        complianceOfficer.setUsername(signUpRequest.getUsername());
        complianceOfficer.setEmail(signUpRequest.getEmail());
        complianceOfficer.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
        complianceOfficer.setFirstName(signUpRequest.getFirstName());
        complianceOfficer.setLastName(signUpRequest.getLastName());
        
        complianceOfficerRepository.save(complianceOfficer);
        return new MessageResponse("Compliance Officer registered successfully!");
    }
}
