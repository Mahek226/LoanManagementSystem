package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.*;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.EmailOtp;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.EmailOtpRepository;
import com.tss.springsecurity.service.ApplicantAuthService;
import com.tss.springsecurity.service.EmailService;
import com.tss.springsecurity.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicantAuthServiceImpl implements ApplicantAuthService {

    private final ApplicantRepository applicantRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public String registerApplicant(ApplicantRegisterRequest request) {
        // Check if username already exists
        if (applicantRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Check if email already exists
        if (applicantRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Check if phone already exists
        if (applicantRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number is already registered");
        }

        // Create applicant with pending status
        Applicant applicant = new Applicant();
        applicant.setUsername(request.getUsername());
        applicant.setFirstName(request.getFirstName());
        applicant.setLastName(request.getLastName());
        applicant.setDob(request.getDob());
        applicant.setGender(request.getGender());
        applicant.setEmail(request.getEmail());
        applicant.setPhone(request.getPhone());
        applicant.setAddress(request.getAddress());
        applicant.setCity(request.getCity());
        applicant.setState(request.getState());
        applicant.setCountry(request.getCountry());
        applicant.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        applicant.setIsApproved(false);
        applicant.setIsEmailVerified(false);
        applicant.setApprovalStatus("PENDING");

        // Save applicant
        applicantRepository.save(applicant);

        // Generate and send OTP
        String otp = generateOtp();
        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setEmail(request.getEmail());
        emailOtp.setOtpCode(otp);
        emailOtp.setIsVerified(false);
        emailOtpRepository.save(emailOtp);

        // Send OTP email
        emailService.sendOtpEmail(request.getEmail(), otp);

        return "Registration initiated. Please verify your email with the OTP sent to " + request.getEmail();
    }

    @Override
    @Transactional
    public String verifyOtp(VerifyOtpRequest request) {
        // Find OTP record
        EmailOtp emailOtp = emailOtpRepository
                .findByEmailAndOtpCodeAndIsVerifiedFalseAndExpiresAtAfter(
                        request.getEmail(), request.getOtp(), LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));

        // Mark OTP as verified
        emailOtp.setIsVerified(true);
        emailOtp.setVerifiedAt(LocalDateTime.now());
        emailOtpRepository.save(emailOtp);

        // Update applicant email verification status
        Applicant applicant = applicantRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Applicant not found"));
        
        applicant.setIsEmailVerified(true);
        applicantRepository.save(applicant);

        return "Email verified successfully. Your registration is pending admin approval.";
    }

    @Override
    @Transactional
    public String resendOtp(String email) {
        // Check if applicant exists
        Applicant applicant = applicantRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Applicant not found with email: " + email));

        // Check if already verified
        if (applicant.getIsEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        // Generate new OTP
        String otp = generateOtp();
        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setEmail(email);
        emailOtp.setOtpCode(otp);
        emailOtp.setIsVerified(false);
        emailOtpRepository.save(emailOtp);

        // Send OTP email
        emailService.sendOtpEmail(email, otp);

        return "OTP resent successfully to " + email;
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicantAuthResponse login(ApplicantLoginRequest request) {
        // Find applicant by email
        Applicant applicant = applicantRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), applicant.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Check if email is verified
        if (!applicant.getIsEmailVerified()) {
            throw new RuntimeException("Please verify your email before logging in");
        }

        // Check if approved by admin
        if (!applicant.getIsApproved()) {
            throw new RuntimeException("Your account is pending admin approval. Status: " + applicant.getApprovalStatus());
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(applicant.getEmail());

        // Return response
        ApplicantAuthResponse response = new ApplicantAuthResponse(
                applicant.getApplicantId(),
                applicant.getFirstName(),
                applicant.getLastName(),
                applicant.getEmail(),
                token,
                applicant.getIsApproved(),
                applicant.getApprovalStatus()
        );
        response.setMessage("Login successful");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalResponse> getPendingApprovals() {
        List<Applicant> pendingApplicants = applicantRepository.findAll().stream()
                .filter(a -> a.getIsEmailVerified() && !a.getIsApproved() && "PENDING".equals(a.getApprovalStatus()))
                .collect(Collectors.toList());

        return pendingApplicants.stream()
                .map(this::convertToApprovalResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApprovalResponse approveApplicant(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with id: " + applicantId));

        if (!applicant.getIsEmailVerified()) {
            throw new RuntimeException("Cannot approve applicant with unverified email");
        }

        applicant.setIsApproved(true);
        applicant.setApprovalStatus("APPROVED");
        applicantRepository.save(applicant);

        // Send approval email
        emailService.sendApprovalEmail(applicant.getEmail(), 
                applicant.getFirstName() + " " + applicant.getLastName(), true);

        return convertToApprovalResponse(applicant);
    }

    @Override
    @Transactional
    public ApprovalResponse rejectApplicant(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with id: " + applicantId));

        applicant.setIsApproved(false);
        applicant.setApprovalStatus("REJECTED");
        applicantRepository.save(applicant);

        // Send rejection email
        emailService.sendApprovalEmail(applicant.getEmail(), 
                applicant.getFirstName() + " " + applicant.getLastName(), false);

        return convertToApprovalResponse(applicant);
    }

    // Helper methods
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

    private ApprovalResponse convertToApprovalResponse(Applicant applicant) {
        return new ApprovalResponse(
                applicant.getApplicantId(),
                applicant.getFirstName(),
                applicant.getLastName(),
                applicant.getEmail(),
                applicant.getPhone(),
                applicant.getApprovalStatus(),
                applicant.getIsApproved(),
                applicant.getIsEmailVerified(),
                applicant.getCreatedAt()
        );
    }
}
