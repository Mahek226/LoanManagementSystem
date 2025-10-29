package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.ApplicantSummaryDTO;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.service.ApplicantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {

    private final ApplicantRepository applicantRepository;

    @Override
    public Page<Applicant> getAllApplicants(Pageable pageable) {
        return applicantRepository.findAll(pageable);
    }

    @Override
    public Page<ApplicantSummaryDTO> getAllApplicantsSummary(Pageable pageable) {
        Page<Applicant> applicants = applicantRepository.findAll(pageable);
        List<ApplicantSummaryDTO> dtos = applicants.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, applicants.getTotalElements());
    }

    private ApplicantSummaryDTO convertToDTO(Applicant applicant) {
        ApplicantSummaryDTO dto = new ApplicantSummaryDTO();
        dto.setApplicantId(applicant.getApplicantId());
        dto.setFirstName(applicant.getFirstName());
        dto.setLastName(applicant.getLastName());
        dto.setDob(applicant.getDob());
        dto.setGender(applicant.getGender());
        dto.setUsername(applicant.getUsername());
        dto.setEmail(applicant.getEmail());
        dto.setPhone(applicant.getPhone());
        dto.setAddress(applicant.getAddress());
        dto.setCity(applicant.getCity());
        dto.setState(applicant.getState());
        dto.setCountry(applicant.getCountry());
        dto.setIsApproved(applicant.getIsApproved());
        dto.setIsEmailVerified(applicant.getIsEmailVerified());
        dto.setApprovalStatus(applicant.getApprovalStatus());
        dto.setCreatedAt(applicant.getCreatedAt());
        dto.setUpdatedAt(applicant.getUpdatedAt());
        dto.setTotalLoans(applicant.getLoanDetails() != null ? applicant.getLoanDetails().size() : 0);
        return dto;
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
