package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.Admin;
import com.tss.springsecurity.entity.LoanOfficer;
import com.tss.springsecurity.entity.ComplianceOfficer;
import com.tss.springsecurity.payload.request.SignupRequest;
import com.tss.springsecurity.payload.request.ApplicantRegisterRequest;
import com.tss.springsecurity.payload.response.MessageResponse;
import com.tss.springsecurity.payload.response.UserInfoResponse;
import com.tss.springsecurity.service.AuthService;
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
public class AuthController {
    @Autowired
    private AuthService authService;

    // Removed getCurrentUser() - will be implemented with JWT-based authentication

    @PostMapping("/api/auth/register")
    public ResponseEntity<?> registerApplicant(@Valid @RequestBody ApplicantRegisterRequest request) {
        try {
            authService.registerApplicant(request);
            return ResponseEntity.ok(new MessageResponse("Applicant registered successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/api/auth/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            MessageResponse response = authService.registerUser(signUpRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
