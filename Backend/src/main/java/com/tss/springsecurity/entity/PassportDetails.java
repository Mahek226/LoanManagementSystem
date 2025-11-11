package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "passport_details", indexes = {
    @Index(name = "idx_passport_number", columnList = "passport_number"),
    @Index(name = "idx_passport_applicant_id", columnList = "applicant_id"),
    @Index(name = "idx_passport_expiry_date", columnList = "expiry_date"),
    @Index(name = "idx_passport_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassportDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @NotBlank(message = "Passport number is required")
    @Pattern(regexp = "^[A-Z]{1}[0-9]{7}$", message = "Passport number must be in format A1234567")
    @Column(name = "passport_number", length = 20)
    private String passportNumber;
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.]+$", message = "Name can only contain letters, spaces, and dots")
    @Column(name = "name", length = 150)
    private String name;
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(name = "dob")
    private LocalDate dob;
    
    @NotBlank(message = "Nationality is required")
    @Size(min = 2, max = 50, message = "Nationality must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Nationality can only contain letters and spaces")
    @Column(name = "nationality", length = 50)
    private String nationality;
    
    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    @Size(max = 500, message = "Cloudinary URL must not exceed 500 characters")
    @Column(name = "cloudinary_url", columnDefinition = "TEXT")
    private String cloudinaryUrl;
    
    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;
    
    @Column(name = "is_tampered")
    private Boolean isTampered = false;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
