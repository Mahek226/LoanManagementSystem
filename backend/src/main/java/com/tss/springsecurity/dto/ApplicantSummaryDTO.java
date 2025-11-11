package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantSummaryDTO {
    private Long applicantId;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String gender;
    private String username;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String country;
    private Boolean isApproved;
    private Boolean isEmailVerified;
    private String approvalStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalLoans;
}
