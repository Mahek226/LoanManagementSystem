package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "aadhaar_details", indexes = {
    @Index(name = "idx_aadhaar_number", columnList = "aadhaar_number"),
    @Index(name = "idx_aadhaar_applicant_id", columnList = "applicant_id"),
    @Index(name = "idx_aadhaar_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AadhaarDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar number must be exactly 12 digits")
    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.]+$", message = "Name can only contain letters, spaces, and dots")
    @Column(name = "name", length = 150)
    private String name;
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(name = "dob")
    private LocalDate dob;
    
    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "Male|Female|Other", message = "Gender must be Male, Female, or Other")
    @Column(name = "gender", length = 10)
    private String gender;
    
    @NotBlank(message = "Address is required")
    @Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;
    
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
