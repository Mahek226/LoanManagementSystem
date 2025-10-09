package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.CompleteLoanApplicationDTO;
import com.tss.springsecurity.dto.LoanApplicationDTO;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ApplicantLoanDetails;

import java.util.List;

public interface LoanApplicationService {
    
    /**
     * Submit a new loan application
     * @param loanApplicationDTO Complete loan application data
     * @return Created applicant with loan details
     */
    Applicant submitLoanApplication(LoanApplicationDTO loanApplicationDTO);
    
    /**
     * Submit a complete loan application with all details
     * @param completeLoanApplicationDTO Complete loan application with documents, references, etc.
     * @return Created applicant with all details
     */
    Applicant submitCompleteLoanApplication(CompleteLoanApplicationDTO completeLoanApplicationDTO);
    
    /**
     * Get applicant by ID
     * @param applicantId Applicant ID
     * @return Applicant details
     */
    Applicant getApplicantById(Long applicantId);
    
    /**
     * Get all loan applications for an applicant
     * @param applicantId Applicant ID
     * @return List of loan applications
     */
    List<ApplicantLoanDetails> getApplicantLoans(Long applicantId);
    
    /**
     * Get all applicants
     * @return List of all applicants
     */
    List<Applicant> getAllApplicants();
    
    /**
     * Get loan application by loan ID
     * @param loanId Loan ID
     * @return Loan details
     */
    ApplicantLoanDetails getLoanById(Long loanId);
}
