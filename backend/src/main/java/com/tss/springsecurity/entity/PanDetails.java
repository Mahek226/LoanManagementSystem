package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pan_details", indexes = {
    @Index(name = "idx_pan_number", columnList = "pan_number"),
    @Index(name = "idx_pan_applicant_id", columnList = "applicant_id"),
    @Index(name = "idx_pan_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PanDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "Invalid PAN format. Must be in format ABCDE1234F")
    @Column(name = "pan_number", length = 20)
    private String panNumber;
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.]+$", message = "Name can only contain letters, spaces, and dots")
    @Column(name = "name", length = 150)
    private String name;
    
    @Size(min = 2, max = 150, message = "Father's name must be between 2 and 150 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.]*$", message = "Father's name can only contain letters, spaces, and dots")
    @Column(name = "father_name", length = 150)
    private String fatherName;
    
    @Past(message = "Date of birth must be in the past")
    @Column(name = "dob")
    private LocalDate dob;
    
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
