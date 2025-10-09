package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.ComplianceOfficerRequest;
import com.tss.springsecurity.dto.LoanOfficerRequest;
import com.tss.springsecurity.dto.OfficerResponse;

import java.util.List;

public interface OfficerManagementService {
    
    OfficerResponse addLoanOfficer(LoanOfficerRequest request);
    
    OfficerResponse addComplianceOfficer(ComplianceOfficerRequest request);
    
    List<OfficerResponse> getAllLoanOfficers();
    
    List<OfficerResponse> getAllComplianceOfficers();
    
    OfficerResponse getLoanOfficerById(Long id);
    
    OfficerResponse getComplianceOfficerById(Long id);
    
    void deleteLoanOfficer(Long id);
    
    void deleteComplianceOfficer(Long id);
}
