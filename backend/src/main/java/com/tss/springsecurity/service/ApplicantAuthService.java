package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.*;

import java.util.List;

public interface ApplicantAuthService {
    
    // Registration workflow
    String registerApplicant(ApplicantRegisterRequest request);
    
    String verifyOtp(VerifyOtpRequest request);
    
    String resendOtp(String email);
    
    // Login
    ApplicantAuthResponse login(ApplicantLoginRequest request);
    
    // Admin operations
    List<ApprovalResponse> getPendingApprovals();
    
    ApprovalResponse approveApplicant(Long applicantId);
    
    ApprovalResponse rejectApplicant(Long applicantId);
}
