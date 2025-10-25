package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.Admin;
import com.tss.springsecurity.entity.LoanOfficer;
import com.tss.springsecurity.entity.ComplianceOfficer;
import com.tss.springsecurity.payload.request.SignupRequest;
import com.tss.springsecurity.payload.response.MessageResponse;
import com.tss.springsecurity.payload.response.UserInfoResponse;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.AdminRepository;
import com.tss.springsecurity.repository.LoanOfficerRepository;
import com.tss.springsecurity.repository.ComplianceOfficerRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private ApplicantRepository applicantRepository;
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private LoanOfficerRepository loanOfficerRepository;
    
    @Autowired
    private ComplianceOfficerRepository complianceOfficerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Removed getCurrentUser() - will be implemented with JWT-based authentication

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        // Get the role as string
        String role = signUpRequest.getRole();
        
        // Validate role
        if (role == null || role.trim().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Role is required!"));
        }

        // If role is APPLICANT, save to applicant table
        if ("ROLE_APPLICANT".equals(role)) {
            // Check if username already exists in applicant table
            if (applicantRepository.existsByUsername(signUpRequest.getUsername())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Username is already taken!"));
            }

            // Check if email already exists in applicant table
            if (applicantRepository.existsByEmail(signUpRequest.getEmail())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Email is already in use!"));
            }

            // Check if phone already exists (if provided)
            if (signUpRequest.getPhone() != null && !signUpRequest.getPhone().isEmpty() 
                && applicantRepository.existsByPhone(signUpRequest.getPhone())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Phone number is already in use!"));
            }

            // Create new applicant with all details
            Applicant applicant = new Applicant();
            applicant.setUsername(signUpRequest.getUsername());
            applicant.setEmail(signUpRequest.getEmail());
            applicant.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
            applicant.setFirstName(signUpRequest.getFirstName());
            applicant.setLastName(signUpRequest.getLastName());
            applicant.setDob(signUpRequest.getDob());
            applicant.setGender(signUpRequest.getGender());
            applicant.setPhone(signUpRequest.getPhone());
            applicant.setAddress(signUpRequest.getAddress());
            applicant.setCity(signUpRequest.getCity());
            applicant.setState(signUpRequest.getState());
            applicant.setCountry(signUpRequest.getCountry());
            
            // Set default values
            applicant.setIsApproved(false);
            applicant.setIsEmailVerified(false);
            applicant.setApprovalStatus("PENDING");
            
            // Save the applicant
            applicantRepository.save(applicant);

            return ResponseEntity.ok(new MessageResponse("Applicant registered successfully!"));
        } else if ("ROLE_ADMIN".equals(role)) {
            // Handle Admin registration
            if (adminRepository.existsByUsername(signUpRequest.getUsername())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Username is already taken!"));
            }

            if (adminRepository.existsByEmail(signUpRequest.getEmail())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Email is already in use!"));
            }

            // Create new admin
            Admin admin = new Admin();
            admin.setUsername(signUpRequest.getUsername());
            admin.setEmail(signUpRequest.getEmail());
            admin.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
            admin.setFirstName(signUpRequest.getFirstName());
            admin.setLastName(signUpRequest.getLastName());
            
            adminRepository.save(admin);
            return ResponseEntity.ok(new MessageResponse("Admin registered successfully!"));
            
        } else if ("ROLE_LOAN_OFFICER".equals(role)) {
            // Handle Loan Officer registration
            if (loanOfficerRepository.existsByUsername(signUpRequest.getUsername())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Username is already taken!"));
            }

            if (loanOfficerRepository.existsByEmail(signUpRequest.getEmail())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Email is already in use!"));
            }

            // Create new loan officer
            LoanOfficer loanOfficer = new LoanOfficer();
            loanOfficer.setUsername(signUpRequest.getUsername());
            loanOfficer.setEmail(signUpRequest.getEmail());
            loanOfficer.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
            loanOfficer.setFirstName(signUpRequest.getFirstName());
            loanOfficer.setLastName(signUpRequest.getLastName());
            
            loanOfficerRepository.save(loanOfficer);
            return ResponseEntity.ok(new MessageResponse("Loan Officer registered successfully!"));
            
        } else if ("ROLE_COMPLIANCE_OFFICER".equals(role)) {
            // Handle Compliance Officer registration
            if (complianceOfficerRepository.existsByUsername(signUpRequest.getUsername())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Username is already taken!"));
            }

            if (complianceOfficerRepository.existsByEmail(signUpRequest.getEmail())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Email is already in use!"));
            }

            // Create new compliance officer
            ComplianceOfficer complianceOfficer = new ComplianceOfficer();
            complianceOfficer.setUsername(signUpRequest.getUsername());
            complianceOfficer.setEmail(signUpRequest.getEmail());
            complianceOfficer.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
            complianceOfficer.setFirstName(signUpRequest.getFirstName());
            complianceOfficer.setLastName(signUpRequest.getLastName());
            
            complianceOfficerRepository.save(complianceOfficer);
            return ResponseEntity.ok(new MessageResponse("Compliance Officer registered successfully!"));
            
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Invalid role specified!"));
        }
    }
}
