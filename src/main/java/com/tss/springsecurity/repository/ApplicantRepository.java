package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
    Optional<Applicant> findByEmail(String email);
    Optional<Applicant> findByPhone(String phone);
    Optional<Applicant> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);
    long countByApprovalStatus(String approvalStatus);
    
    // Add explicit query for debugging
    @Query("SELECT COUNT(a) FROM Applicant a WHERE a.approvalStatus = ?1")
    long countByApprovalStatusExplicit(String approvalStatus);
    
    // Get monthly application counts for the last 12 months
    @Query("SELECT MONTH(a.createdAt) as month, COUNT(a) as count " +
           "FROM Applicant a " +
           "WHERE YEAR(a.createdAt) = YEAR(CURRENT_DATE) " +
           "GROUP BY MONTH(a.createdAt) " +
           "ORDER BY MONTH(a.createdAt)")
    List<Object[]> getMonthlyApplicationCounts();
    
    List<Applicant> findByApprovalStatus(String status);
    
    @Query("SELECT a FROM Applicant a LEFT JOIN FETCH a.basicDetails WHERE a.applicantId = :id")
    Optional<Applicant> findByIdWithBasicDetails(@Param("id") Long id);
    
    // Count applicants by month for dashboard statistics
    @Query("SELECT COUNT(a) FROM Applicant a WHERE MONTH(a.createdAt) = :month AND YEAR(a.createdAt) = YEAR(CURRENT_DATE)")
    Long countApplicantsByMonth(@Param("month") int month);
}
