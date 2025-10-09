package com.tss.springsecurity.entity;

import jakarta.persistence.*;
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
    
    @Column(name = "residence_type", length = 50)
    private String residenceType; // owned, rented, parental, company_provided
    
    @Column(name = "property_ownership", length = 50)
    private String propertyOwnership; // self, spouse, parent, joint
    
    @Column(name = "monthly_rent", precision = 12, scale = 2)
    private BigDecimal monthlyRent;
    
    @Column(name = "years_at_current_address")
    private Integer yearsAtCurrentAddress;
    
    @Column(name = "property_value", precision = 15, scale = 2)
    private BigDecimal propertyValue;
    
    @Column(name = "property_type", length = 50)
    private String propertyType; // apartment, independent_house, villa, farmhouse
    
    @Column(name = "total_area_sqft")
    private Integer totalAreaSqft;
    
    @Column(name = "has_home_loan")
    private Boolean hasHomeLoan = false;
    
    @Column(name = "outstanding_home_loan", precision = 15, scale = 2)
    private BigDecimal outstandingHomeLoan;
    
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
