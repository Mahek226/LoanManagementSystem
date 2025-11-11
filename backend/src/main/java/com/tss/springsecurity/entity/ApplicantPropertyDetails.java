package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "applicant_property_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantPropertyDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @NotBlank(message = "Residence type is required")
    @Pattern(regexp = "owned|rented|parental|company_provided", message = "Residence type must be owned, rented, parental, or company_provided")
    @Column(name = "residence_type", length = 50)
    private String residenceType; // owned, rented, parental, company_provided
    
    @Pattern(regexp = "self|spouse|parent|joint|other", message = "Property ownership must be self, spouse, parent, joint, or other")
    @Column(name = "property_ownership", length = 50)
    private String propertyOwnership; // self, spouse, parent, joint
    
    @DecimalMin(value = "0.0", message = "Monthly rent cannot be negative")
    @DecimalMax(value = "1000000.0", message = "Monthly rent cannot exceed ₹10 lakhs")
    @Digits(integer = 10, fraction = 2, message = "Monthly rent must have at most 10 digits before decimal and 2 after")
    @Column(name = "monthly_rent", precision = 12, scale = 2)
    private BigDecimal monthlyRent;
    
    @Min(value = 0, message = "Years at current address cannot be negative")
    @Max(value = 100, message = "Years at current address cannot exceed 100")
    @Column(name = "years_at_current_address")
    private Integer yearsAtCurrentAddress;
    
    @DecimalMin(value = "0.0", message = "Property value cannot be negative")
    @DecimalMax(value = "1000000000.0", message = "Property value cannot exceed ₹100 crores")
    @Digits(integer = 13, fraction = 2, message = "Property value must have at most 13 digits before decimal and 2 after")
    @Column(name = "property_value", precision = 15, scale = 2)
    private BigDecimal propertyValue;
    
    @Pattern(regexp = "apartment|independent_house|villa|farmhouse|plot|commercial", message = "Property type must be apartment, independent_house, villa, farmhouse, plot, or commercial")
    @Column(name = "property_type", length = 50)
    private String propertyType; // apartment, independent_house, villa, farmhouse
    
    @Min(value = 1, message = "Total area must be at least 1 sq ft")
    @Max(value = 100000, message = "Total area cannot exceed 100,000 sq ft")
    @Column(name = "total_area_sqft")
    private Integer totalAreaSqft;
    
    @Column(name = "has_home_loan")
    private Boolean hasHomeLoan = false;
    
    @DecimalMin(value = "0.0", message = "Outstanding home loan cannot be negative")
    @DecimalMax(value = "100000000.0", message = "Outstanding home loan cannot exceed ₹10 crores")
    @Digits(integer = 13, fraction = 2, message = "Outstanding home loan must have at most 13 digits before decimal and 2 after")
    @Column(name = "outstanding_home_loan", precision = 15, scale = 2)
    private BigDecimal outstandingHomeLoan;
    
    @DecimalMin(value = "0.0", message = "Home loan EMI cannot be negative")
    @DecimalMax(value = "1000000.0", message = "Home loan EMI cannot exceed ₹10 lakhs")
    @Digits(integer = 10, fraction = 2, message = "Home loan EMI must have at most 10 digits before decimal and 2 after")
    @Column(name = "home_loan_emi", precision = 12, scale = 2)
    private BigDecimal homeLoanEmi;
    
    @Column(name = "property_documents_verified")
    private Boolean propertyDocumentsVerified = false;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
