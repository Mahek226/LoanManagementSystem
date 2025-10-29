package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResubmissionResponse {
    
    private Long resubmissionId;
    private Long assignmentId;
    private Long applicantId;
    private String applicantName;
    private String applicantEmail;
    private List<String> requestedDocuments;
    private String reason;
    private String additionalComments;
    private Integer priorityLevel;
    private String status; // REQUESTED, SUBMITTED, REVIEWED
    private LocalDateTime requestedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long requestedByOfficerId;
    private String requestedByOfficerName;
}
