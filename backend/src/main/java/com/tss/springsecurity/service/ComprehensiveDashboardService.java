package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.ComprehensiveDashboardDTO;
import com.tss.springsecurity.dto.ComprehensiveDashboardDTO.*;
import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveDashboardService {
    
    private final ApplicantLoanDetailsRepository loanRepository;
    private final OfficerApplicationAssignmentRepository assignmentRepository;
    private final ApplicantRepository applicantRepository;
    private final LoanOfficerRepository loanOfficerRepository;
    private final FraudFlagRepository fraudFlagRepository;
    
    @Transactional(readOnly = true)
    public ComprehensiveDashboardDTO getComprehensiveDashboard(Long officerId, String filterType, LocalDate startDate, LocalDate endDate) {
        log.info("Generating comprehensive dashboard for officer: {}, filter: {}", officerId, filterType);
        
        ComprehensiveDashboardDTO dashboard = new ComprehensiveDashboardDTO();
        
        // Set date range
        if (startDate == null || endDate == null) {
            endDate = LocalDate.now();
            startDate = switch (filterType != null ? filterType : "MONTH") {
                case "YEAR" -> endDate.minusYears(1);
                case "QUARTER" -> endDate.minusMonths(3);
                case "MONTH" -> endDate.minusMonths(1);
                default -> endDate.minusMonths(1);
            };
        }
        dashboard.setStartDate(startDate);
        dashboard.setEndDate(endDate);
        dashboard.setFilterType(filterType);
        
        // Get all assignments for the officer within date range
        List<OfficerApplicationAssignment> assignments = officerId != null 
            ? assignmentRepository.findByOfficer_OfficerId(officerId)
            : assignmentRepository.findAll();
        
        // Filter by date range
        LocalDate finalStartDate = startDate;
        LocalDate finalEndDate = endDate;
        assignments = assignments.stream()
            .filter(a -> a.getAssignedAt() != null)
            .filter(a -> {
                LocalDate assignedDate = a.getAssignedAt().toLocalDate();
                return !assignedDate.isBefore(finalStartDate) && !assignedDate.isAfter(finalEndDate);
            })
            .collect(Collectors.toList());
        
        // Calculate all metrics
        calculateApplicationVolume(dashboard, assignments);
        calculateFinancialPerformance(dashboard, assignments);
        calculateApprovalPerformance(dashboard, assignments);
        calculateLoanStatus(dashboard, assignments);
        calculateLoanQuality(dashboard, assignments);
        calculateRiskDistribution(dashboard, assignments);
        calculateMonthlyTrends(dashboard, assignments);
        calculateLoanBreakdowns(dashboard, assignments);
        calculateGeographicData(dashboard, assignments);
        calculateEmployeePerformance(dashboard, assignments);
        identifyRiskyLoans(dashboard, assignments);
        
        log.info("Dashboard generation complete. Total applications: {}", dashboard.getTotalApplications());
        return dashboard;
    }
    
    private void calculateApplicationVolume(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        dashboard.setTotalApplications(assignments.size());
        dashboard.setTotalApproved((int) assignments.stream().filter(a -> "APPROVED".equals(a.getStatus())).count());
        dashboard.setTotalRejected((int) assignments.stream().filter(a -> "REJECTED".equals(a.getStatus())).count());
        dashboard.setTotalPending((int) assignments.stream().filter(a -> "PENDING".equals(a.getStatus()) || "ASSIGNED".equals(a.getStatus()) || "IN_PROGRESS".equals(a.getStatus())).count());
        dashboard.setTotalEscalated((int) assignments.stream().filter(a -> "ESCALATED_TO_COMPLIANCE".equals(a.getStatus())).count());
    }
    
    private void calculateFinancialPerformance(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        List<ApplicantLoanDetails> approvedLoans = assignments.stream()
            .filter(a -> "APPROVED".equals(a.getStatus()))
            .map(OfficerApplicationAssignment::getLoan)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        dashboard.setTotalFundedAmount(approvedLoans.stream()
            .mapToDouble(l -> l.getLoanAmount() != null ? l.getLoanAmount().doubleValue() : 0.0)
            .sum());
        
        dashboard.setAverageInterestRate(approvedLoans.stream()
            .filter(l -> l.getInterestRate() != null)
            .mapToDouble(l -> l.getInterestRate().doubleValue())
            .average()
            .orElse(0.0));
        
        // Calculate average DTI from applicant financials
        dashboard.setAverageDTI(35.0); // Placeholder - would need to calculate from actual financial data
        
        dashboard.setAverageLoanAmount(approvedLoans.stream()
            .mapToDouble(l -> l.getLoanAmount() != null ? l.getLoanAmount().doubleValue() : 0.0)
            .average()
            .orElse(0.0));
        
        dashboard.setTotalPendingAmount(assignments.stream()
            .filter(a -> "PENDING".equals(a.getStatus()) || "ASSIGNED".equals(a.getStatus()) || "IN_PROGRESS".equals(a.getStatus()))
            .map(OfficerApplicationAssignment::getLoan)
            .filter(Objects::nonNull)
            .mapToDouble(l -> l.getLoanAmount() != null ? l.getLoanAmount().doubleValue() : 0.0)
            .sum());
        
        dashboard.setTotalRejectedAmount(assignments.stream()
            .filter(a -> "REJECTED".equals(a.getStatus()))
            .map(OfficerApplicationAssignment::getLoan)
            .filter(Objects::nonNull)
            .mapToDouble(l -> l.getLoanAmount() != null ? l.getLoanAmount().doubleValue() : 0.0)
            .sum());
    }
    
    private void calculateApprovalPerformance(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        int total = assignments.size();
        if (total > 0) {
            dashboard.setApprovalRate((dashboard.getTotalApproved() * 100.0) / total);
            dashboard.setRejectionRate((dashboard.getTotalRejected() * 100.0) / total);
            dashboard.setEscalationRate((dashboard.getTotalEscalated() * 100.0) / total);
        } else {
            dashboard.setApprovalRate(0.0);
            dashboard.setRejectionRate(0.0);
            dashboard.setEscalationRate(0.0);
        }
        
        // Pull-through rate: approved loans that were actually funded (assume all approved are funded for now)
        dashboard.setPullThroughRate(dashboard.getApprovalRate());
        
        // Default rate: would need historical data on loan defaults
        dashboard.setDefaultRate(2.5); // Placeholder
    }
    
    private void calculateLoanStatus(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        List<ApplicantLoanDetails> loans = assignments.stream()
            .map(OfficerApplicationAssignment::getLoan)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        // Good loans: Low risk and approved
        dashboard.setGoodLoans((int) loans.stream()
            .filter(l -> "LOW".equals(l.getRiskLevel()) && "APPROVED".equals(l.getStatus()))
            .count());
        
        // Bad loans: High risk, rejected, or defaulted
        dashboard.setBadLoans((int) loans.stream()
            .filter(l -> "HIGH".equals(l.getRiskLevel()) || "REJECTED".equals(l.getStatus()) || "DEFAULTED".equals(l.getStatus()))
            .count());
        
        // Under review
        dashboard.setUnderReviewLoans((int) loans.stream()
            .filter(l -> "PENDING".equals(l.getStatus()) || "IN_PROGRESS".equals(l.getStatus()) || "UNDER_REVIEW".equals(l.getStatus()))
            .count());
    }
    
    private void calculateLoanQuality(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        // Underwriting accuracy: correctly assessed loans (would need outcome data)
        dashboard.setUnderwritingAccuracy(92.5); // Placeholder
        
        // Portfolio yield: average return (would calculate from interest rates and amounts)
        dashboard.setPortfolioYield(dashboard.getAverageInterestRate());
        
        // Loan quality index: composite score based on multiple factors
        double qualityIndex = (dashboard.getUnderwritingAccuracy() * 0.4) + 
                             ((100 - dashboard.getDefaultRate()) * 0.3) +
                             (dashboard.getApprovalRate() * 0.3);
        dashboard.setLoanQualityIndex(qualityIndex);
    }
    
    private void calculateRiskDistribution(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        List<ApplicantLoanDetails> loans = assignments.stream()
            .map(OfficerApplicationAssignment::getLoan)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        dashboard.setLowRiskCount((int) loans.stream().filter(l -> "LOW".equals(l.getRiskLevel())).count());
        dashboard.setMediumRiskCount((int) loans.stream().filter(l -> "MEDIUM".equals(l.getRiskLevel())).count());
        dashboard.setHighRiskCount((int) loans.stream().filter(l -> "HIGH".equals(l.getRiskLevel())).count());
        dashboard.setCriticalRiskCount((int) loans.stream().filter(l -> "CRITICAL".equals(l.getRiskLevel())).count());
    }
    
    private void calculateMonthlyTrends(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        Map<String, MonthlyTrend> applicationsByMonth = new TreeMap<>();
        Map<String, MonthlyTrend> approvalsByMonth = new TreeMap<>();
        Map<String, MonthlyTrend> rejectionsByMonth = new TreeMap<>();
        
        for (OfficerApplicationAssignment assignment : assignments) {
            if (assignment.getAssignedAt() == null) continue;
            
            LocalDateTime assignedDate = assignment.getAssignedAt();
            String monthKey = String.format("%d-%02d", assignedDate.getYear(), assignedDate.getMonthValue());
            String monthLabel = assignedDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + assignedDate.getYear();
            
            // Applications trend
            applicationsByMonth.computeIfAbsent(monthKey, k -> new MonthlyTrend(monthLabel, assignedDate.getYear(), 0, 0.0, 0.0));
            MonthlyTrend appTrend = applicationsByMonth.get(monthKey);
            appTrend.setCount(appTrend.getCount() + 1);
            if (assignment.getLoan() != null && assignment.getLoan().getLoanAmount() != null) {
                appTrend.setAmount(appTrend.getAmount() + assignment.getLoan().getLoanAmount().doubleValue());
            }
            
            // Approvals trend
            if ("APPROVED".equals(assignment.getStatus())) {
                approvalsByMonth.computeIfAbsent(monthKey, k -> new MonthlyTrend(monthLabel, assignedDate.getYear(), 0, 0.0, 0.0));
                MonthlyTrend approvTrend = approvalsByMonth.get(monthKey);
                approvTrend.setCount(approvTrend.getCount() + 1);
                if (assignment.getLoan() != null && assignment.getLoan().getLoanAmount() != null) {
                    approvTrend.setAmount(approvTrend.getAmount() + assignment.getLoan().getLoanAmount().doubleValue());
                }
            }
            
            // Rejections trend
            if ("REJECTED".equals(assignment.getStatus())) {
                rejectionsByMonth.computeIfAbsent(monthKey, k -> new MonthlyTrend(monthLabel, assignedDate.getYear(), 0, 0.0, 0.0));
                MonthlyTrend rejectTrend = rejectionsByMonth.get(monthKey);
                rejectTrend.setCount(rejectTrend.getCount() + 1);
                if (assignment.getLoan() != null && assignment.getLoan().getLoanAmount() != null) {
                    rejectTrend.setAmount(rejectTrend.getAmount() + assignment.getLoan().getLoanAmount().doubleValue());
                }
            }
        }
        
        // Calculate averages
        applicationsByMonth.values().forEach(t -> {
            if (t.getCount() > 0) {
                t.setAverageAmount(t.getAmount() / t.getCount());
            }
        });
        approvalsByMonth.values().forEach(t -> {
            if (t.getCount() > 0) {
                t.setAverageAmount(t.getAmount() / t.getCount());
            }
        });
        rejectionsByMonth.values().forEach(t -> {
            if (t.getCount() > 0) {
                t.setAverageAmount(t.getAmount() / t.getCount());
            }
        });
        
        dashboard.setMonthlyApplications(new ArrayList<>(applicationsByMonth.values()));
        dashboard.setMonthlyApprovals(new ArrayList<>(approvalsByMonth.values()));
        dashboard.setMonthlyRejections(new ArrayList<>(rejectionsByMonth.values()));
        dashboard.setMonthlyDefaults(new ArrayList<>()); // Placeholder
    }
    
    private void calculateLoanBreakdowns(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        List<ApplicantLoanDetails> loans = assignments.stream()
            .map(OfficerApplicationAssignment::getLoan)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        // By purpose
        Map<String, Integer> byPurpose = loans.stream()
            .filter(l -> l.getLoanPurpose() != null)
            .collect(Collectors.groupingBy(
                ApplicantLoanDetails::getLoanPurpose,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        dashboard.setLoansByPurpose(byPurpose);
        
        Map<String, Double> amountsByPurpose = loans.stream()
            .filter(l -> l.getLoanPurpose() != null && l.getLoanAmount() != null)
            .collect(Collectors.groupingBy(
                ApplicantLoanDetails::getLoanPurpose,
                Collectors.summingDouble(l -> l.getLoanAmount().doubleValue())
            ));
        dashboard.setAmountsByPurpose(amountsByPurpose);
        
        // By type
        Map<String, Integer> byType = loans.stream()
            .filter(l -> l.getLoanType() != null)
            .collect(Collectors.groupingBy(
                ApplicantLoanDetails::getLoanType,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        dashboard.setLoansByType(byType);
        
        // By term (group into ranges)
        Map<String, Integer> byTerm = loans.stream()
            .filter(l -> l.getTenureMonths() != null)
            .collect(Collectors.groupingBy(
                l -> getTermRange(l.getTenureMonths()),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        dashboard.setLoansByTerm(byTerm);
    }
    
    private String getTermRange(Integer months) {
        if (months == null) return "Unknown";
        if (months <= 12) return "0-1 years";
        if (months <= 36) return "1-3 years";
        if (months <= 60) return "3-5 years";
        if (months <= 120) return "5-10 years";
        return "10+ years";
    }
    
    private void calculateGeographicData(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        Map<String, GeographicData> byState = new HashMap<>();
        
        for (OfficerApplicationAssignment assignment : assignments) {
            Applicant applicant = assignment.getApplicant();
            if (applicant != null && applicant.getState() != null) {
                String state = applicant.getState();
                GeographicData geoData = byState.computeIfAbsent(state, k -> 
                    new GeographicData(state, state, 0, 0, 0, 0.0, 0.0, 0.0)
                );
                
                geoData.setApplicationCount(geoData.getApplicationCount() + 1);
                
                if ("APPROVED".equals(assignment.getStatus())) {
                    geoData.setApprovedCount(geoData.getApprovedCount() + 1);
                } else if ("REJECTED".equals(assignment.getStatus())) {
                    geoData.setRejectedCount(geoData.getRejectedCount() + 1);
                }
                
                if (assignment.getLoan() != null && assignment.getLoan().getLoanAmount() != null) {
                    geoData.setTotalAmount(geoData.getTotalAmount() + assignment.getLoan().getLoanAmount().doubleValue());
                }
            }
        }
        
        // Calculate averages and approval rates
        byState.values().forEach(gd -> {
            if (gd.getApplicationCount() > 0) {
                gd.setAverageAmount(gd.getTotalAmount() / gd.getApplicationCount());
                gd.setApprovalRate((gd.getApprovedCount() * 100.0) / gd.getApplicationCount());
            }
        });
        
        dashboard.setLoansByState(new ArrayList<>(byState.values()));
        dashboard.setLoansByCity(new ArrayList<>()); // Would calculate similarly for cities
    }
    
    private void calculateEmployeePerformance(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        Map<Long, EmployeePerformance> performanceMap = new HashMap<>();
        
        for (OfficerApplicationAssignment assignment : assignments) {
            if (assignment.getOfficer() == null) continue;
            
            Long officerId = assignment.getOfficer().getOfficerId();
            EmployeePerformance perf = performanceMap.computeIfAbsent(officerId, k -> {
                LoanOfficer officer = assignment.getOfficer();
                return new EmployeePerformance(
                    officerId,
                    officer.getFirstName() + " " + officer.getLastName(),
                    0, // yearsExperience - would calculate from employment start date
                    0, // totalAssigned
                    0, // processed
                    0, // approved
                    0, // rejected
                    0, // escalated
                    0.0, // approvalRate
                    0.0, // averageProcessingDays
                    0.0  // underwritingAccuracy
                );
            });
            
            perf.setTotalAssigned(perf.getTotalAssigned() + 1);
            
            if ("APPROVED".equals(assignment.getStatus()) || "REJECTED".equals(assignment.getStatus())) {
                perf.setProcessed(perf.getProcessed() + 1);
                
                if ("APPROVED".equals(assignment.getStatus())) {
                    perf.setApproved(perf.getApproved() + 1);
                } else {
                    perf.setRejected(perf.getRejected() + 1);
                }
                
                // Calculate processing time
                if (assignment.getAssignedAt() != null && assignment.getProcessedAt() != null) {
                    long days = ChronoUnit.DAYS.between(assignment.getAssignedAt(), assignment.getProcessedAt());
                    perf.setAverageProcessingDays((perf.getAverageProcessingDays() * (perf.getProcessed() - 1) + days) / perf.getProcessed());
                }
            } else if ("ESCALATED_TO_COMPLIANCE".equals(assignment.getStatus())) {
                perf.setEscalated(perf.getEscalated() + 1);
            }
        }
        
        // Calculate approval rates and accuracy
        performanceMap.values().forEach(perf -> {
            if (perf.getProcessed() > 0) {
                perf.setApprovalRate((perf.getApproved() * 100.0) / perf.getProcessed());
            }
            perf.setUnderwritingAccuracy(92.0); // Placeholder - would need historical accuracy data
        });
        
        dashboard.setOfficerPerformance(new ArrayList<>(performanceMap.values()));
    }
    
    private void identifyRiskyLoans(ComprehensiveDashboardDTO dashboard, List<OfficerApplicationAssignment> assignments) {
        List<RiskyLoan> riskyLoans = new ArrayList<>();
        int highRiskCount = 0;
        int overdueCount = 0;
        int fraudAlertCount = 0;
        
        for (OfficerApplicationAssignment assignment : assignments) {
            ApplicantLoanDetails loan = assignment.getLoan();
            if (loan == null) continue;
            
            boolean isRisky = false;
            String alert = null;
            List<String> fraudIndicators = new ArrayList<>();
            
            // High risk score
            if (loan.getRiskScore() != null && loan.getRiskScore() >= 70) {
                isRisky = true;
                alert = "HIGH_RISK";
                highRiskCount++;
            }
            
            // Overdue processing
            if (assignment.getAssignedAt() != null && assignment.getProcessedAt() == null) {
                long daysPending = ChronoUnit.DAYS.between(assignment.getAssignedAt(), LocalDateTime.now());
                if (daysPending > 7) {
                    isRisky = true;
                    alert = "OVERDUE";
                    overdueCount++;
                }
            }
            
            // Check for fraud flags
            List<FraudFlag> fraudFlags = fraudFlagRepository.findByLoan_LoanId(loan.getLoanId());
            if (!fraudFlags.isEmpty()) {
                isRisky = true;
                alert = "FRAUD_DETECTED";
                fraudAlertCount++;
                fraudIndicators = fraudFlags.stream()
                    .map(f -> f.getRuleName() + (f.getFlagNotes() != null ? ": " + f.getFlagNotes() : ""))
                    .collect(Collectors.toList());
            }
            
            if (isRisky) {
                RiskyLoan riskyLoan = new RiskyLoan();
                riskyLoan.setLoanId(loan.getLoanId());
                riskyLoan.setApplicantId(assignment.getApplicant().getApplicantId());
                riskyLoan.setApplicantName(assignment.getApplicant().getFirstName() + " " + assignment.getApplicant().getLastName());
                riskyLoan.setLoanType(loan.getLoanType());
                riskyLoan.setLoanAmount(loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : 0.0);
                riskyLoan.setRiskScore(loan.getRiskScore());
                riskyLoan.setRiskLevel(loan.getRiskLevel());
                riskyLoan.setRiskReason("High risk indicators detected");
                riskyLoan.setFraudIndicators(fraudIndicators);
                riskyLoan.setAssignedOfficer(assignment.getOfficer() != null ? 
                    assignment.getOfficer().getFirstName() + " " + assignment.getOfficer().getLastName() : "Unassigned");
                riskyLoan.setAlert(alert);
                
                if (assignment.getAssignedAt() != null) {
                    riskyLoan.setDaysPending((int) ChronoUnit.DAYS.between(assignment.getAssignedAt(), LocalDateTime.now()));
                }
                
                riskyLoans.add(riskyLoan);
            }
        }
        
        dashboard.setRiskyLoans(riskyLoans);
        dashboard.setHighRiskLoansCount(highRiskCount);
        dashboard.setOverdueLoansCount(overdueCount);
        dashboard.setFraudAlertsCount(fraudAlertCount);
    }
}
