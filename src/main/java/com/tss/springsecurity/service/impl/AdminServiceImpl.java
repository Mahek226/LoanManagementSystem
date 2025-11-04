package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.AdminAuthResponse;
import com.tss.springsecurity.dto.AdminLoginRequest;
import com.tss.springsecurity.dto.AdminRegisterRequest;
import com.tss.springsecurity.dto.DashboardStatsResponse;
import com.tss.springsecurity.entity.Admin;
import com.tss.springsecurity.repository.AdminRepository;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.ApplicantLoanDetailsRepository;
import com.tss.springsecurity.service.AdminService;
import com.tss.springsecurity.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicantLoanDetailsRepository loanDetailsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AdminAuthResponse register(AdminRegisterRequest registerRequest) {
        // Check if username already exists
        if (adminRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Check if email already exists
        if (adminRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Create new admin
        Admin admin = new Admin();
        admin.setUsername(registerRequest.getUsername());
        admin.setEmail(registerRequest.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));

        // Save admin
        Admin savedAdmin = adminRepository.save(admin);

        // Generate JWT token
        String token = jwtUtil.generateToken(savedAdmin.getUsername());

        // Return response
        return new AdminAuthResponse(
                savedAdmin.getAdminId(),
                savedAdmin.getUsername(),
                savedAdmin.getEmail(),
                token
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAuthResponse login(AdminLoginRequest loginRequest) {
        // Find admin by username or email
        Admin admin = adminRepository.findByUsernameOrEmail(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getUsernameOrEmail())
                .orElseThrow(() -> new RuntimeException("Invalid username/email or password"));

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), admin.getPasswordHash())) {
            throw new RuntimeException("Invalid username/email or password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(admin.getUsername());

        // Return response
        return new AdminAuthResponse(
                admin.getAdminId(),
                admin.getUsername(),
                admin.getEmail(),
                token
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        // Get total counts with debugging
        long totalApplicants = applicantRepository.count();
        System.out.println("Total applicants from DB: " + totalApplicants);
        
        long pendingApplications = applicantRepository.countByApprovalStatus("PENDING");
        System.out.println("Pending applications from DB: " + pendingApplications);
        
        long approvedLoans = applicantRepository.countByApprovalStatus("APPROVED");
        System.out.println("Approved applications from DB: " + approvedLoans);
        
        long rejectedApplications = applicantRepository.countByApprovalStatus("REJECTED");
        System.out.println("Rejected applications from DB: " + rejectedApplications);

        // Calculate actual loan amounts from database
        Double totalLoanAmount = loanDetailsRepository.sumAllLoanAmounts();
        if (totalLoanAmount == null) {
            totalLoanAmount = 0.0;
        }
        double averageLoanAmount = totalApplicants > 0 ? totalLoanAmount / totalApplicants : 0.0;

        // Get real monthly applications data from database
        List<Integer> monthlyApplications = calculateMonthlyApplications();
        System.out.println("Monthly applications from DB: " + monthlyApplications);

        // Create loan status distribution
        List<DashboardStatsResponse.LoanStatusDistribution> statusDistribution = Arrays.asList(
                new DashboardStatsResponse.LoanStatusDistribution("Approved", approvedLoans),
                new DashboardStatsResponse.LoanStatusDistribution("Pending", pendingApplications),
                new DashboardStatsResponse.LoanStatusDistribution("Rejected", rejectedApplications),
                new DashboardStatsResponse.LoanStatusDistribution("Under Review", 
                    totalApplicants - approvedLoans - pendingApplications - rejectedApplications)
        );

        return new DashboardStatsResponse(
                totalApplicants,
                pendingApplications,
                approvedLoans,
                rejectedApplications,
                totalLoanAmount,
                averageLoanAmount,
                monthlyApplications,
                statusDistribution
        );
    }
    
    /**
     * Calculate monthly application counts for all 12 months of the current year
     * Returns an array of 12 integers representing counts for Jan through Dec
     */
    private List<Integer> calculateMonthlyApplications() {
        // Initialize array with zeros for all 12 months
        Integer[] monthCounts = new Integer[12];
        Arrays.fill(monthCounts, 0);
        
        // Get actual counts from database
        List<Object[]> results = applicantRepository.getMonthlyApplicationCounts();
        
        // Fill in the actual counts
        for (Object[] result : results) {
            Integer month = (Integer) result[0]; // Month (1-12)
            Long count = (Long) result[1]; // Count
            
            if (month != null && month >= 1 && month <= 12) {
                monthCounts[month - 1] = count.intValue(); // Convert to 0-indexed
            }
        }
        
        return Arrays.asList(monthCounts);
    }
}
