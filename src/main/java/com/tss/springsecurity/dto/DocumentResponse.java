package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    
    private Long documentId;
    private String documentType;
    private String documentUrl;
    private String fileName;
    private String verificationStatus; // PENDING, VERIFIED, REJECTED
    private String remarks;
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;
}
