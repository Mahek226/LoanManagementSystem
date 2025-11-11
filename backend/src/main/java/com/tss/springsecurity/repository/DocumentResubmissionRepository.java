package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.DocumentResubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentResubmissionRepository extends JpaRepository<DocumentResubmission, Long> {
    
    List<DocumentResubmission> findByApplicantApplicantIdOrderByRequestedAtDesc(Long applicantId);
    
    List<DocumentResubmission> findByRequestedByOfficerOfficerIdOrderByRequestedAtDesc(Long officerId);
    
    List<DocumentResubmission> findByStatusOrderByPriorityLevelDescRequestedAtDesc(String status);
    
    @Query("SELECT dr FROM DocumentResubmission dr WHERE dr.assignment.assignmentId = :assignmentId ORDER BY dr.requestedAt DESC")
    List<DocumentResubmission> findByAssignmentIdOrderByRequestedAtDesc(@Param("assignmentId") Long assignmentId);
    
    @Query("SELECT COUNT(dr) FROM DocumentResubmission dr WHERE dr.status = :status")
    Long countByStatus(@Param("status") String status);
    
    // Additional methods for document resubmission functionality
    List<DocumentResubmission> findByApplicant_ApplicantIdAndStatus(Long applicantId, String status);
}
