package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfficerSummary {
    
    private Long officerId;
    private String firstName;
    private String lastName;
    private String email;
    private String loanType;
    private Long currentWorkload; // Number of active assignments
}
