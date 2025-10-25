package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.service.ApplicantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {

    private final ApplicantRepository applicantRepository;

    @Override
    public Page<Applicant> getAllApplicants(Pageable pageable) {
        return applicantRepository.findAll(pageable);
    }

    @Override
    public Applicant getApplicantById(Long id) {
        return applicantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Applicant not found with id: " + id));
    }

    @Override
    @Transactional
    public void approveApplicant(Long id, String comments) {
        Applicant applicant = getApplicantById(id);
        applicant.setApprovalStatus("APPROVED");
        applicant.setIsApproved(true);
        // You can add a comments field to the Applicant entity if needed
        applicantRepository.save(applicant);
    }

    @Override
    @Transactional
    public void rejectApplicant(Long id, String comments) {
        Applicant applicant = getApplicantById(id);
        applicant.setApprovalStatus("REJECTED");
        applicant.setIsApproved(false);
        // You can add a comments field to the Applicant entity if needed
        applicantRepository.save(applicant);
    }

    @Override
    public List<Applicant> searchApplicants(String query) {
        // Simple search implementation - you can enhance this with more sophisticated search
        return applicantRepository.findAll().stream()
                .filter(applicant -> 
                    applicant.getFirstName().toLowerCase().contains(query.toLowerCase()) ||
                    applicant.getLastName().toLowerCase().contains(query.toLowerCase()) ||
                    applicant.getEmail().toLowerCase().contains(query.toLowerCase()) ||
                    applicant.getUsername().toLowerCase().contains(query.toLowerCase())
                )
                .toList();
    }

    @Override
    public List<Applicant> filterApplicantsByStatus(String status) {
        return applicantRepository.findAll().stream()
                .filter(applicant -> applicant.getApprovalStatus().equals(status))
                .toList();
    }
}
