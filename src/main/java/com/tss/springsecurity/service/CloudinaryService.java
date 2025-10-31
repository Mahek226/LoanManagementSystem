package com.tss.springsecurity.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file, String folder, String applicantId) throws IOException {
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFilename = applicantId + "_" + UUID.randomUUID().toString() + fileExtension;

        // Upload parameters - PUBLIC ACCESS for document viewing
        Map<String, Object> uploadParams = ObjectUtils.asMap(
            "folder", "loan_documents/" + folder,
            "public_id", uniqueFilename,
            "resource_type", "auto",
            "type", "upload",
            "access_mode", "public",
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

    public Map<String, Object> uploadDocumentWithDetails(MultipartFile file, String documentType, String applicantId) throws IOException {
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFilename = applicantId + "_" + UUID.randomUUID().toString() + fileExtension;

        // Upload parameters - PUBLIC ACCESS for document viewing
        Map<String, Object> uploadParams = ObjectUtils.asMap(
            "folder", "loan_documents/" + documentType.toLowerCase(),
            "public_id", uniqueFilename,
            "resource_type", "auto",
            "type", "upload",
            "access_mode", "public",
            "quality", "auto:good",
            "fetch_format", "auto"
        );

        // Upload to Cloudinary
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        
        return uploadResult;
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

    /**
     * Make an existing Cloudinary resource public
     * This is useful for fixing 401 errors on previously uploaded private documents
     */
    public boolean makeResourcePublic(String publicId) {
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                "access_mode", "public",
                "type", "upload",
                "invalidate", true
            );
            Map<String, Object> result = cloudinary.uploader().explicit(publicId, params);
            return result != null && result.containsKey("secure_url");
        } catch (IOException e) {
            System.err.println("Failed to make resource public: " + publicId + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Batch update existing documents to public access
     * Use this to fix 401 errors on old uploads
     */
    public int makeAllDocumentsPublic(java.util.List<String> cloudinaryUrls) {
        int successCount = 0;
        for (String url : cloudinaryUrls) {
            String publicId = extractPublicIdFromUrl(url);
            if (publicId != null && makeResourcePublic(publicId)) {
                successCount++;
            }
        }
        return successCount;
    }
}
