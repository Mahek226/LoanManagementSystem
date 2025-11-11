package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalApplicants;
    private Long pendingApplications;
    private Long approvedLoans;
    private Long rejectedApplications;
    private Double totalLoanAmount;
    private Double averageLoanAmount;
    private List<Integer> monthlyApplications;
    private List<LoanStatusDistribution> loanStatusDistribution;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanStatusDistribution {
        private String status;
        private Long count;
    }
}
