package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ApplicantBasicDetails;
import com.tss.springsecurity.externalfraud.entity.*;
import com.tss.springsecurity.externalfraud.repository.*;
import com.tss.springsecurity.repository.ApplicantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class TestController {

    @Autowired
    private ApplicantRepository applicantRepository;
    
    @Autowired
    private PersonRepository personRepository;
    
    @Autowired
    private BankRecordRepository bankRecordRepository;
    
    @Autowired
    private CriminalRecordRepository criminalRecordRepository;
    
//    @Autowired
//    private LoanHistoryRepository loanHistoryRepository;

    @GetMapping("/cors")
    public ResponseEntity<?> testCors() {
        return ResponseEntity.ok(Map.of(
            "message", "CORS is working!",
            "timestamp", System.currentTimeMillis(),
            "status", "success"
        ));
    }

    @PostMapping("/cors")
    public ResponseEntity<?> testCorsPost(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
            "message", "CORS POST is working!",
            "receivedData", body != null ? body : "No data",
            "timestamp", System.currentTimeMillis(),
            "status", "success"
        ));
    }

    @GetMapping("/verify-matching/{applicantId}")
    public ResponseEntity<?> verifyMatching(@PathVariable Long applicantId) {
        try {
            log.info("Testing PAN/Aadhaar extraction and matching for applicant ID: {}", applicantId);

            // Get applicant with basic details
            Applicant applicant = applicantRepository.findByIdWithBasicDetails(applicantId)
                    .orElse(null);

            if (applicant == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Applicant not found with ID: " + applicantId));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("applicantId", applicantId);
            result.put("applicantName", applicant.getFirstName() + " " + applicant.getLastName());

            // Check basic details
            if (applicant.getBasicDetails() != null) {
                ApplicantBasicDetails basicDetails = applicant.getBasicDetails();
                String panNumber = basicDetails.getPanNumber();
                String aadhaarNumber = basicDetails.getAadhaarNumber();

                result.put("panNumber", panNumber != null ? panNumber.substring(0, 3) + "****" : "null");
                result.put("aadhaarNumber", aadhaarNumber != null ? "****" + aadhaarNumber.substring(8) : "null");

                // Try to find person in external DB
                Optional<Person> personOpt = Optional.empty();
                if (panNumber != null) {
                    personOpt = personRepository.findByPanNumber(panNumber);
                    result.put("foundByPAN", personOpt.isPresent());
                }

                if (!personOpt.isPresent() && aadhaarNumber != null) {
                    personOpt = personRepository.findByAadhaarNumber(aadhaarNumber);
                    result.put("foundByAadhaar", personOpt.isPresent());
                }

                if (personOpt.isPresent()) {
                    Person person = personOpt.get();
                    result.put("externalPersonId", person.getId());
                    result.put("externalPersonName", person.getFirstName() + " " + person.getLastName());
                    result.put("externalPersonPAN", person.getPanNumber() != null ? person.getPanNumber().substring(0, 3) + "****" : "null");
                    result.put("externalPersonAadhaar", person.getAadhaarNumber() != null ? "****" + person.getAadhaarNumber().substring(8) : "null");

                    // Special check for person ID 3
                    if (person.getId().equals(3L)) {
                        result.put("specialNote", "This is person ID 3 - matches with our test data");

                        // Get some sample data
                        List<BankRecord> bankRecords = bankRecordRepository.findByPersonId(3L);
                        List<CriminalRecord> criminalRecords = criminalRecordRepository.findByPersonId(3L);
//                        List<HistoricalAndCurrentLoan> loanHistory = loanHistoryRepository.findByPersonId(3L);

                        result.put("bankRecordsCount", bankRecords.size());
                        result.put("criminalRecordsCount", criminalRecords.size());
//                        result.put("loanHistoryCount", loanHistory.size());
                    }
                } else {
                    result.put("externalPersonFound", false);
                    result.put("message", "No matching person found in external database");
                }
                
            } else {
                result.put("error", "Basic details not found for applicant");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error in verify matching test", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Test failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/external-persons")
    public ResponseEntity<?> listExternalPersons() {
        try {
            log.info("Listing all persons in external database");
            
            List<Person> allPersons = personRepository.findAll();
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalPersons", allPersons.size());
            result.put("persons", allPersons.stream()
                    .map(person -> Map.of(
                            "id", person.getId(),
                            "name", (person.getFirstName() != null ? person.getFirstName() : "") + " " + 
                                   (person.getLastName() != null ? person.getLastName() : ""),
                            "panNumber", person.getPanNumber() != null ? person.getPanNumber().substring(0, 3) + "****" : "null",
                            "aadhaarNumber", person.getAadhaarNumber() != null ? "****" + person.getAadhaarNumber().substring(8) : "null",
                            "phoneNumber", person.getPhoneNumber(),
                            "email", person.getEmail()
                    ))
                    .collect(Collectors.toList()));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error listing external persons", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list persons: " + e.getMessage()));
        }
    }
    
    @GetMapping("/external-person-direct/{personId}")
    public ResponseEntity<?> getExternalPersonDirect(@PathVariable Long personId) {
        try {
            log.info("Testing direct access to person ID: {}", personId);
            
            Optional<Person> personOpt = personRepository.findById(personId);
            
            if (personOpt.isPresent()) {
                Person person = personOpt.get();
                Map<String, Object> result = new HashMap<>();
                result.put("found", true);
                result.put("personId", person.getId());
                result.put("firstName", person.getFirstName());
                result.put("lastName", person.getLastName());
                result.put("panNumber", person.getPanNumber());
                result.put("aadhaarNumber", person.getAadhaarNumber());
                result.put("email", person.getEmail());
                result.put("phoneNumber", person.getPhoneNumber());
                result.put("dob", person.getDob());
                result.put("gender", person.getGender());
                
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.ok(Map.of("found", false, "message", "Person not found with ID: " + personId));
            }
            
        } catch (Exception e) {
            log.error("Error in direct person access test for ID: {}", personId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Direct access failed: " + e.getMessage()));
        }
    }
}
