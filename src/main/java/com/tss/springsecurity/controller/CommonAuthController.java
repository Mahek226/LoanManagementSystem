package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.CommonLoginRequest;
import com.tss.springsecurity.dto.CommonAuthResponse;
import com.tss.springsecurity.dto.ForgotPasswordRequest;
import com.tss.springsecurity.dto.ResetPasswordRequest;
import com.tss.springsecurity.service.CommonAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequiredArgsConstructor
public class CommonAuthController {

    private final CommonAuthService commonAuthService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody CommonLoginRequest loginRequest) {
        try {
            CommonAuthResponse response = commonAuthService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(new SuccessResponse("Logged out successfully"));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            String message = commonAuthService.forgotPassword(request);
            return ResponseEntity.ok(new SuccessResponse(message));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/reset-password/{token}")
    public ResponseEntity<?> getResetPasswordForm(@PathVariable String token) {
        try {
            String email = commonAuthService.getEmailFromResetToken(token);
            return ResponseEntity.ok(new ResetPasswordFormResponse(email, token));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            String message = commonAuthService.resetPassword(request);
            return ResponseEntity.ok(new SuccessResponse(message));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // Inner classes for responses
    private record ErrorResponse(String message) {}
    private record SuccessResponse(String message) {}
    private record ResetPasswordFormResponse(String email, String token) {}
}
