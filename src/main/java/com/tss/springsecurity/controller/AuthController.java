package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.User;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.UserRole;
import com.tss.springsecurity.payload.request.SignupRequest;
import com.tss.springsecurity.payload.response.MessageResponse;
import com.tss.springsecurity.payload.response.UserInfoResponse;
import com.tss.springsecurity.repository.UserRepository;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.security.services.UserDetailsImpl;
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

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new MessageResponse("User not authenticated"));
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new UserInfoResponse(
            userDetails.getId(),
            userDetails.getUsername(),
            userDetails.getEmail(),
            userDetails.getFirstName(),
            userDetails.getLastName(),
            roles
        ));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        // Validate and get the role
        UserRole role;
        try {
            role = UserRole.valueOf(signUpRequest.getRole());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Invalid role specified!"));
        }

        // If role is APPLICANT, save to applicant table
        if (role == UserRole.ROLE_APPLICANT) {
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
            applicant.setPhone(signUpRequest.getPhone());
            applicant.setAddress(signUpRequest.getAddress());
            applicant.setCity(signUpRequest.getCity());
            applicant.setState(signUpRequest.getState());
            applicant.setCountry(signUpRequest.getCountry());
            applicant.setGender(signUpRequest.getGender());
            
            // Set default values
            applicant.setIsApproved(false);
            applicant.setIsEmailVerified(false);
            applicant.setApprovalStatus("PENDING");
            
            // Save the applicant
            applicantRepository.save(applicant);

            return ResponseEntity.ok(new MessageResponse("Applicant registered successfully!"));
        } else {
            // For other roles, save to user table as before
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Username is already taken!"));
            }

            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Email is already in use!"));
            }

            // Create new user's account with all fields
            User user = new User(
                    signUpRequest.getUsername(),
                    signUpRequest.getEmail(),
                    passwordEncoder.encode(signUpRequest.getPassword()),
                    signUpRequest.getFirstName(),
                    signUpRequest.getLastName(),
                    role
            );
            
            // Save the user
            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        }
    }
}
