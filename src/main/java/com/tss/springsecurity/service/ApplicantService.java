package com.tss.springsecurity.service;

import com.tss.springsecurity.entity.Applicant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ApplicantService {
    Page<Applicant> getAllApplicants(Pageable pageable);
    Applicant getApplicantById(Long id);
    void approveApplicant(Long id, String comments);
    void rejectApplicant(Long id, String comments);
    List<Applicant> searchApplicants(String query);
    List<Applicant> filterApplicantsByStatus(String status);
}
