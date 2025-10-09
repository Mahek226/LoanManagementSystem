package com.tss.springsecurity.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

//@Service  // Disabled until Cloudinary dependency is added
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file, String folder, String applicantId) throws IOException {
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFilename = applicantId + "_" + UUID.randomUUID().toString() + fileExtension;

        // Upload parameters
        Map<String, Object> uploadParams = ObjectUtils.asMap(
            "folder", "loan_documents/" + folder,
            "public_id", uniqueFilename,
            "resource_type", "auto",
            "quality", "auto:good",
            "fetch_format", "auto"
        );

        // Upload to Cloudinary
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        
        // Return the secure URL
        return (String) uploadResult.get("secure_url");
    }

    public String uploadDocument(MultipartFile file, String documentType, String applicantId) throws IOException {
        return uploadFile(file, documentType.toLowerCase(), applicantId);
    }

    public boolean deleteFile(String publicId) {
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (IOException e) {
            return false;
        }
    }

    public String extractPublicIdFromUrl(String cloudinaryUrl) {
        // Extract public_id from Cloudinary URL for deletion
        if (cloudinaryUrl != null && cloudinaryUrl.contains("/")) {
            String[] parts = cloudinaryUrl.split("/");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                return lastPart.substring(0, lastPart.lastIndexOf("."));
            }
        }
        return null;
    }
}
