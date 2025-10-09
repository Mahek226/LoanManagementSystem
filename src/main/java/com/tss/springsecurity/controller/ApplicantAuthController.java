package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.ApplicantAuthResponse;
import com.tss.springsecurity.dto.ApplicantLoginRequest;
import com.tss.springsecurity.dto.ApplicantRegisterRequest;
import com.tss.springsecurity.dto.VerifyOtpRequest;
import com.tss.springsecurity.service.ApplicantAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applicant/auth")
@RequiredArgsConstructor
public class ApplicantAuthController {

    private final ApplicantAuthService applicantAuthService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody ApplicantRegisterRequest request) {
        try {
            String message = applicantAuthService.registerApplicant(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SuccessResponse(message));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            String message = applicantAuthService.verifyOtp(request);
            return ResponseEntity.ok(new SuccessResponse(message));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String email) {
        try {
            String message = applicantAuthService.resendOtp(email);
            return ResponseEntity.ok(new SuccessResponse(message));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody ApplicantLoginRequest request) {
        try {
            ApplicantAuthResponse response = applicantAuthService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // Response classes
    private record SuccessResponse(String message) {}
    private record ErrorResponse(String message) {}
}
