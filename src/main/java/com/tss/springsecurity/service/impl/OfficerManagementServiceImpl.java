package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.ComplianceOfficerRequest;
import com.tss.springsecurity.dto.LoanOfficerRequest;
import com.tss.springsecurity.dto.OfficerResponse;
import com.tss.springsecurity.entity.ComplianceOfficer;
import com.tss.springsecurity.entity.LoanOfficer;
import com.tss.springsecurity.repository.ComplianceOfficerRepository;
import com.tss.springsecurity.repository.LoanOfficerRepository;
import com.tss.springsecurity.service.OfficerManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfficerManagementServiceImpl implements OfficerManagementService {

    private final LoanOfficerRepository loanOfficerRepository;
    private final ComplianceOfficerRepository complianceOfficerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OfficerResponse addLoanOfficer(LoanOfficerRequest request) {
        // Check if username already exists
        if (loanOfficerRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Check if email already exists
        if (loanOfficerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Create new loan officer
        LoanOfficer officer = new LoanOfficer();
        officer.setUsername(request.getUsername());
        officer.setEmail(request.getEmail());
        officer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        officer.setFirstName(request.getFirstName());
        officer.setLastName(request.getLastName());
        officer.setLoanType(request.getLoanType());

        // Save loan officer
        LoanOfficer savedOfficer = loanOfficerRepository.save(officer);

        // Return response
        OfficerResponse response = new OfficerResponse(
                savedOfficer.getOfficerId(),
                savedOfficer.getUsername(),
                savedOfficer.getEmail(),
                savedOfficer.getLoanType(),
                savedOfficer.getCreatedAt()
        );
        response.setMessage("Loan Officer created successfully");
        return response;
    }

    @Override
    @Transactional
    public OfficerResponse addComplianceOfficer(ComplianceOfficerRequest request) {
        // Check if username already exists
        if (complianceOfficerRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Check if email already exists
        if (complianceOfficerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Create new compliance officer
        ComplianceOfficer officer = new ComplianceOfficer();
        officer.setUsername(request.getUsername());
        officer.setEmail(request.getEmail());
        officer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        officer.setFirstName(request.getFirstName());
        officer.setLastName(request.getLastName());
        officer.setLoanType(request.getLoanType());

        // Save compliance officer
        ComplianceOfficer savedOfficer = complianceOfficerRepository.save(officer);

        // Return response
        OfficerResponse response = new OfficerResponse(
                savedOfficer.getOfficerId(),
                savedOfficer.getUsername(),
                savedOfficer.getEmail(),
                savedOfficer.getLoanType(),
                savedOfficer.getCreatedAt()
        );
        response.setMessage("Compliance Officer created successfully");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficerResponse> getAllLoanOfficers() {
        return loanOfficerRepository.findAll().stream()
                .map(officer -> new OfficerResponse(
                        officer.getOfficerId(),
                        officer.getUsername(),
                        officer.getEmail(),
                        officer.getLoanType(),
                        officer.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficerResponse> getAllComplianceOfficers() {
        return complianceOfficerRepository.findAll().stream()
                .map(officer -> new OfficerResponse(
                        officer.getOfficerId(),
                        officer.getUsername(),
                        officer.getEmail(),
                        officer.getLoanType(),
                        officer.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OfficerResponse getLoanOfficerById(Long id) {
        LoanOfficer officer = loanOfficerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan Officer not found with id: " + id));
        
        return new OfficerResponse(
                officer.getOfficerId(),
                officer.getUsername(),
                officer.getEmail(),
                officer.getLoanType(),
                officer.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OfficerResponse getComplianceOfficerById(Long id) {
        ComplianceOfficer officer = complianceOfficerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compliance Officer not found with id: " + id));
        
        return new OfficerResponse(
                officer.getOfficerId(),
                officer.getUsername(),
                officer.getEmail(),
                officer.getLoanType(),
                officer.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public void deleteLoanOfficer(Long id) {
        if (!loanOfficerRepository.existsById(id)) {
            throw new RuntimeException("Loan Officer not found with id: " + id);
        }
        loanOfficerRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteComplianceOfficer(Long id) {
        if (!complianceOfficerRepository.existsById(id)) {
            throw new RuntimeException("Compliance Officer not found with id: " + id);
        }
        complianceOfficerRepository.deleteById(id);
    }
}
