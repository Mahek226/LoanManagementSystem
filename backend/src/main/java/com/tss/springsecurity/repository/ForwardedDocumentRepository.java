package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ForwardedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForwardedDocumentRepository extends JpaRepository<ForwardedDocument, Long> {
    
    List<ForwardedDocument> findByStatus(String status);
    
    List<ForwardedDocument> findByForwardedToComplianceOfficerId(Long complianceOfficerId);
    
    List<ForwardedDocument> findByStatusOrderByForwardedAtDesc(String status);
    
    List<ForwardedDocument> findAllByOrderByForwardedAtDesc();
    
    List<ForwardedDocument> findByApplicantId(Long applicantId);
    
    List<ForwardedDocument> findByLoanId(Long loanId);
}
