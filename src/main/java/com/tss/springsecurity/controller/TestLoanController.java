package com.tss.springsecurity.controller;

import com.tss.springsecurity.service.CloudinaryService;
import com.tss.springsecurity.payload.response.MessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@CrossOrigin(origins = "*", maxAge = 3600)
//@RestController  // Disabled until Cloudinary dependency is added
@RequestMapping("/api/test")
public class TestLoanController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload-document")
    public ResponseEntity<?> testDocumentUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam("applicantId") String applicantId) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Please select a file to upload"));
            }

            String cloudinaryUrl = cloudinaryService.uploadDocument(file, documentType, applicantId);
            
            return ResponseEntity.ok(new MessageResponse("File uploaded successfully! URL: " + cloudinaryUrl));
            
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(new MessageResponse("Error uploading file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new MessageResponse("Unexpected error: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(new MessageResponse("Loan Application Service is running!"));
    }
}
