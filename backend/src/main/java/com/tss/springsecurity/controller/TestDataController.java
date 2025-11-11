package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.Admin;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.repository.AdminRepository;
import com.tss.springsecurity.repository.ApplicantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
@RequiredArgsConstructor
public class TestDataController {

    private final ApplicantRepository applicantRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/create-sample-data")
    public ResponseEntity<String> createSampleData() {
        try {
            // Always create more sample data for testing
            createSampleApplicant("john_doe", "John", "Doe", "john.doe@example.com", "+1234567890", "PENDING");
            createSampleApplicant("jane_smith", "Jane", "Smith", "jane.smith@example.com", "+1234567891", "APPROVED");
            createSampleApplicant("mike_wilson", "Michael", "Wilson", "mike.wilson@example.com", "+1234567892", "REJECTED");
            createSampleApplicant("sarah_johnson", "Sarah", "Johnson", "sarah.johnson@example.com", "+1234567893", "PENDING");
            createSampleApplicant("david_brown", "David", "Brown", "david.brown@example.com", "+1234567894", "PENDING");
            createSampleApplicant("emma_davis", "Emma", "Davis", "emma.davis@example.com", "+1234567895", "APPROVED");
            createSampleApplicant("alex_miller", "Alex", "Miller", "alex.miller@example.com", "+1234567896", "PENDING");
            createSampleApplicant("lisa_garcia", "Lisa", "Garcia", "lisa.garcia@example.com", "+1234567897", "REJECTED");
            
            return ResponseEntity.ok("Sample data created successfully! Total applicants: " + applicantRepository.count());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating sample data: " + e.getMessage());
        }
    }

    @PostMapping("/create-admin")
    public ResponseEntity<String> createAdmin() {
        try {
            // Create admin user for testing
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setEmail("admin@lms.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            
            if (!adminRepository.existsByUsername("admin")) {
                adminRepository.save(admin);
                return ResponseEntity.ok("Admin user created successfully! Username: admin, Password: admin123");
            } else {
                return ResponseEntity.ok("Admin user already exists! Username: admin, Password: admin123");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating admin: " + e.getMessage());
        }
    }

    private void createSampleApplicant(String username, String firstName, String lastName, 
                                     String email, String phone, String status) {
        if (!applicantRepository.existsByUsername(username)) {
            Applicant applicant = new Applicant();
            applicant.setUsername(username);
            applicant.setFirstName(firstName);
            applicant.setLastName(lastName);
            applicant.setEmail(email);
            applicant.setPhone(phone);
            applicant.setPasswordHash(passwordEncoder.encode("password123"));
            applicant.setDob(LocalDate.of(1990, 1, 1));
            applicant.setGender("Male");
            applicant.setAddress("123 Sample Street");
            applicant.setCity("Sample City");
            applicant.setState("Sample State");
            applicant.setCountry("USA");
            applicant.setApprovalStatus(status);
            applicant.setIsApproved("APPROVED".equals(status));
            applicant.setIsEmailVerified(true);
            applicant.setCreatedAt(LocalDateTime.now());
            applicant.setUpdatedAt(LocalDateTime.now());
            
            applicantRepository.save(applicant);
        }
    }

    @GetMapping("/check-database")
    public ResponseEntity<String> checkDatabase() {
        try {
            long totalCount = applicantRepository.count();
            long pendingCount = applicantRepository.countByApprovalStatus("PENDING");
            long approvedCount = applicantRepository.countByApprovalStatus("APPROVED");
            long rejectedCount = applicantRepository.countByApprovalStatus("REJECTED");
            
            String result = String.format(
                "Database Check:\nTotal: %d\nPending: %d\nApproved: %d\nRejected: %d", 
                totalCount, pendingCount, approvedCount, rejectedCount
            );
            
            System.out.println(result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking database: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear-data")
    public ResponseEntity<String> clearData() {
        try {
            long count = applicantRepository.count();
            applicantRepository.deleteAll();
            return ResponseEntity.ok("Cleared " + count + " applicants from database");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error clearing data: " + e.getMessage());
        }
    }
}
