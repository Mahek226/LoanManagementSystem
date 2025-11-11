package com.tss.springsecurity.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaService {

    @Value("${recaptcha.secret-key:6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe}")
    private String secretKey;

    @Value("${recaptcha.verify-url:https://www.google.com/recaptcha/api/siteverify}")
    private String verifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean verifyCaptcha(String captchaToken) {
        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            log.warn("CAPTCHA token is null or empty");
            return false;
        }

        // Check if it's our custom math CAPTCHA token
        if (captchaToken.startsWith("math_captcha_") && captchaToken.endsWith("_verified")) {
            log.info("Math CAPTCHA verification successful");
            return true;
        }

        // For Google reCAPTCHA (if needed in future)
        try {
            // Create request body
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("secret", secretKey);
            requestBody.add("response", captchaToken);

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Create request entity
            HttpEntity<MultiValueMap<String, String>> requestEntity = 
                new HttpEntity<>(requestBody, headers);

            // Make request to Google reCAPTCHA API
            ResponseEntity<String> response = restTemplate.exchange(
                verifyUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                CaptchaResponse captchaResponse = objectMapper.readValue(
                    response.getBody(), 
                    CaptchaResponse.class
                );

                if (captchaResponse.isSuccess()) {
                    log.info("Google reCAPTCHA verification successful");
                    return true;
                } else {
                    log.warn("Google reCAPTCHA verification failed. Error codes: {}", 
                        captchaResponse.getErrorCodes());
                    return false;
                }
            } else {
                log.error("Failed to verify Google reCAPTCHA. HTTP Status: {}", 
                    response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("Error verifying CAPTCHA: {}", e.getMessage(), e);
            return false;
        }
    }

    @Data
    private static class CaptchaResponse {
        private boolean success;
        
        @JsonProperty("challenge_ts")
        private String challengeTimestamp;
        
        private String hostname;
        
        @JsonProperty("error-codes")
        private String[] errorCodes;
    }
}
