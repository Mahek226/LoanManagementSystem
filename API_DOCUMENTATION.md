# Loan Management System - Complete API Documentation

## Overview
This document provides comprehensive documentation for all API endpoints in the Loan Management System.

**Base URL:** `http://localhost:8080`  
**Total Endpoints:** 34  
**API Categories:** 8

---

## Table of Contents
1. [Authentication & Authorization](#authentication--authorization)
2. [Admin Authentication](#1-admin-authentication)
3. [Applicant Authentication](#2-applicant-authentication)
4. [Officer Management - Loan Officers](#3-officer-management---loan-officers)
5. [Officer Management - Compliance Officers](#4-officer-management---compliance-officers)
6. [Applicant Approvals](#5-applicant-approvals)
7. [Loan Applications](#6-loan-applications)
8. [Fraud Detection](#7-fraud-detection)
9. [OCR Document Processing](#8-ocr---document-processing)
10. [Error Handling](#error-handling)

---

## Authentication & Authorization

### Token Format
All protected endpoints require a JWT token in the Authorization header:
```
Authorization: Bearer {your_jwt_token}
```

### Token Expiration
- JWT tokens expire after **24 hours**
- Email OTP expires after **10 minutes**

### User Roles
- **Admin**: Full system access
- **Applicant**: Access to own loan applications
- **Loan Officer**: Manage loan applications by type
- **Compliance Officer**: Fraud detection and compliance

---

## 1. Admin Authentication

### 1.1 Register Admin
**Endpoint:** `POST /api/admin/auth/register`  
**Authentication:** None

**Request Body:**
```json
{
  "username": "admin123",
  "email": "admin@example.com",
  "password": "admin123456"
}
```

**Response (201 Created):**
```json
{
  "adminId": 1,
  "username": "admin123",
  "email": "admin@example.com",
  "message": "Admin registered successfully"
}
```

---

### 1.2 Admin Login
**Endpoint:** `POST /api/admin/auth/login`  
**Authentication:** None

**Request Body:**
```json
{
  "usernameOrEmail": "admin123",
  "password": "admin123456"
}
```

**Response (200 OK):**
```json
{
  "adminId": 1,
  "username": "admin123",
  "email": "admin@example.com",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

---

## 2. Applicant Authentication

### 2.1 Register Applicant
**Endpoint:** `POST /api/applicant/auth/register`  
**Authentication:** None

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "dob": "1990-05-15",
  "gender": "Male",
  "email": "john.doe@example.com",
  "phone": "9876543210",
  "password": "password123",
  "address": "123 Main Street, Apt 4B",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India"
}
```

**Response (201 Created):**
```json
{
  "message": "Registration initiated. Please verify your email with the OTP sent to john.doe@example.com"
}
```

---

### 2.2 Verify OTP
**Endpoint:** `POST /api/applicant/auth/verify-otp`  
**Authentication:** None

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "otp": "123456"
}
```

**Response (200 OK):**
```json
{
  "message": "Email verified successfully. Your registration is pending admin approval."
}
```

---

### 2.3 Resend OTP
**Endpoint:** `POST /api/applicant/auth/resend-otp`  
**Authentication:** None

**Query Parameters:**
- `email` (required): Email address

**Example:** `POST /api/applicant/auth/resend-otp?email=john.doe@example.com`

**Response (200 OK):**
```json
{
  "message": "OTP resent successfully to john.doe@example.com"
}
```

---

### 2.4 Applicant Login
**Endpoint:** `POST /api/applicant/auth/login`  
**Authentication:** None  
**Prerequisites:** Email verified AND admin approved

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "applicantId": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "isApproved": true,
  "approvalStatus": "APPROVED",
  "message": "Login successful"
}
```

---

## 3. Officer Management - Loan Officers

### 3.1 Add Loan Officer
**Endpoint:** `POST /api/admin/officers/loan-officers`  
**Authentication:** Bearer Token (Admin)

**Request Body:**
```json
{
  "username": "loanofficer1",
  "email": "loanofficer1@example.com",
  "password": "officer123",
  "loanType": "Home Loan"
}
```

**Response (201 Created):**
```json
{
  "officerId": 1,
  "username": "loanofficer1",
  "email": "loanofficer1@example.com",
  "loanType": "Home Loan",
  "createdAt": "2025-10-04T12:00:00",
  "message": "Loan Officer created successfully"
}
```

---

### 3.2 Get All Loan Officers
**Endpoint:** `GET /api/admin/officers/loan-officers`  
**Authentication:** Bearer Token (Admin)

**Response (200 OK):**
```json
[
  {
    "officerId": 1,
    "username": "loanofficer1",
    "email": "loanofficer1@example.com",
    "loanType": "Home Loan",
    "createdAt": "2025-10-04T12:00:00"
  }
]
```

---

### 3.3 Get Loan Officer by ID
**Endpoint:** `GET /api/admin/officers/loan-officers/{id}`  
**Authentication:** Bearer Token (Admin)

**Path Parameters:**
- `id`: Officer ID

**Response (200 OK):**
```json
{
  "officerId": 1,
  "username": "loanofficer1",
  "email": "loanofficer1@example.com",
  "loanType": "Home Loan",
  "createdAt": "2025-10-04T12:00:00"
}
```

---

### 3.4 Delete Loan Officer
**Endpoint:** `DELETE /api/admin/officers/loan-officers/{id}`  
**Authentication:** Bearer Token (Admin)

**Path Parameters:**
- `id`: Officer ID

**Response (200 OK):**
```json
{
  "message": "Loan Officer deleted successfully"
}
```

---

## 4. Officer Management - Compliance Officers

### 4.1 Add Compliance Officer
**Endpoint:** `POST /api/admin/officers/compliance-officers`  
**Authentication:** Bearer Token (Admin)

**Request Body:**
```json
{
  "username": "complianceofficer1",
  "email": "complianceofficer1@example.com",
  "password": "compliance123",
  "loanType": "All Loans"
}
```

**Response (201 Created):**
```json
{
  "officerId": 1,
  "username": "complianceofficer1",
  "email": "complianceofficer1@example.com",
  "loanType": "All Loans",
  "createdAt": "2025-10-04T12:00:00",
  "message": "Compliance Officer created successfully"
}
```

---

### 4.2 Get All Compliance Officers
**Endpoint:** `GET /api/admin/officers/compliance-officers`  
**Authentication:** Bearer Token (Admin)

**Response (200 OK):**
```json
[
  {
    "officerId": 1,
    "username": "complianceofficer1",
    "email": "complianceofficer1@example.com",
    "loanType": "All Loans",
    "createdAt": "2025-10-04T12:00:00"
  }
]
```

---

### 4.3 Get Compliance Officer by ID
**Endpoint:** `GET /api/admin/officers/compliance-officers/{id}`  
**Authentication:** Bearer Token (Admin)

**Response (200 OK):**
```json
{
  "officerId": 1,
  "username": "complianceofficer1",
  "email": "complianceofficer1@example.com",
  "loanType": "All Loans",
  "createdAt": "2025-10-04T12:00:00"
}
```

---

### 4.4 Delete Compliance Officer
**Endpoint:** `DELETE /api/admin/officers/compliance-officers/{id}`  
**Authentication:** Bearer Token (Admin)

**Response (200 OK):**
```json
{
  "message": "Compliance Officer deleted successfully"
}
```

---

## 5. Applicant Approvals

### 5.1 Get Pending Approvals
**Endpoint:** `GET /api/admin/applicant-approvals/pending`  
**Authentication:** Bearer Token (Admin)

**Response (200 OK):**
```json
[
  {
    "applicantId": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "9876543210",
    "approvalStatus": "PENDING",
    "isApproved": false,
    "isEmailVerified": true,
    "createdAt": "2025-10-04T12:00:00"
  }
]
```

---

### 5.2 Approve Applicant
**Endpoint:** `PUT /api/admin/applicant-approvals/{applicantId}/approve`  
**Authentication:** Bearer Token (Admin)

**Path Parameters:**
- `applicantId`: Applicant ID

**Response (200 OK):**
```json
{
  "applicantId": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "9876543210",
  "approvalStatus": "APPROVED",
  "isApproved": true,
  "isEmailVerified": true,
  "createdAt": "2025-10-04T12:00:00"
}
```

---

### 5.3 Reject Applicant
**Endpoint:** `PUT /api/admin/applicant-approvals/{applicantId}/reject`  
**Authentication:** Bearer Token (Admin)

**Response (200 OK):**
```json
{
  "applicantId": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "approvalStatus": "REJECTED",
  "isApproved": false
}
```

---

## 6. Loan Applications

### 6.1 Submit Basic Loan Application
**Endpoint:** `POST /api/loan-applications/submit`  
**Authentication:** Bearer Token (Applicant)

**Request Body:**
```json
{
  "applicantId": 1,
  "loanType": "Home Loan",
  "requestedAmount": 5000000,
  "loanTerm": 240,
  "purpose": "Purchase residential property"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Loan application submitted successfully",
  "applicantId": 1,
  "applicantName": "John Doe",
  "email": "john.doe@example.com"
}
```

---

### 6.2 Submit Complete Loan Application
**Endpoint:** `POST /api/loan-applications/submit-complete`  
**Authentication:** Bearer Token (Applicant)

**Request Body:**
```json
{
  "applicant": {
    "firstName": "John",
    "lastName": "Doe",
    "dob": "1990-05-15",
    "gender": "Male",
    "email": "john.doe@example.com",
    "phone": "9876543210",
    "password": "password123",
    "address": "123 Main Street",
    "city": "Mumbai",
    "state": "Maharashtra",
    "country": "India"
  },
  "employmentDetails": {
    "employerName": "Tech Corp",
    "employmentType": "Full-time",
    "jobTitle": "Software Engineer",
    "yearsEmployed": 5,
    "monthlyIncome": 100000
  },
  "financialInformation": {
    "bankName": "HDFC Bank",
    "accountNumber": "1234567890",
    "ifscCode": "HDFC0001234",
    "accountType": "Savings",
    "creditScore": 750
  },
  "loanDetails": {
    "loanType": "Home Loan",
    "requestedAmount": 5000000,
    "loanTerm": 240,
    "purpose": "Purchase residential property",
    "interestRate": 8.5
  }
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Complete loan application submitted successfully with all details",
  "applicantId": 1,
  "applicantName": "John Doe",
  "email": "john.doe@example.com",
  "phone": "9876543210"
}
```

---

### 6.3 Bulk Upload Loan Applications
**Endpoint:** `POST /api/loan-applications/submit-complete-bulk`  
**Authentication:** Bearer Token (Admin)  
**Content-Type:** `multipart/form-data`

**Form Data:**
- `file`: JSON or JSONL file containing array of CompleteLoanApplicationDTO objects

**Supported Formats:**
1. **JSON Array:**
```json
[
  {
    "applicant": {...},
    "employmentDetails": {...},
    "financialInformation": {...},
    "loanDetails": {...}
  }
]
```

2. **JSONL (one object per line):**
```
{"applicant": {...}, "employmentDetails": {...}, "financialInformation": {...}, "loanDetails": {...}}
{"applicant": {...}, "employmentDetails": {...}, "financialInformation": {...}, "loanDetails": {...}}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Bulk processing completed",
  "totalRecords": 10,
  "successCount": 8,
  "failureCount": 2,
  "successfulApplications": [
    {
      "recordNumber": 1,
      "applicantId": 1,
      "applicantName": "John Doe",
      "email": "john.doe@example.com",
      "phone": "9876543210"
    }
  ],
  "failedApplications": [
    {
      "recordNumber": 3,
      "email": "invalid@example.com",
      "error": "Email already exists"
    }
  ]
}
```

---

### 6.4 Get Applicant by ID
**Endpoint:** `GET /api/loan-applications/applicant/{applicantId}`  
**Authentication:** Bearer Token

**Response (200 OK):**
```json
{
  "applicantId": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "9876543210",
  "dob": "1990-05-15",
  "gender": "Male",
  "address": "123 Main Street",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India"
}
```

---

### 6.5 Get Applicant Loans
**Endpoint:** `GET /api/loan-applications/applicant/{applicantId}/loans`  
**Authentication:** Bearer Token

**Response (200 OK):**
```json
[
  {
    "loanId": 1,
    "loanType": "Home Loan",
    "requestedAmount": 5000000,
    "loanTerm": 240,
    "purpose": "Purchase residential property",
    "status": "PENDING"
  }
]
```

---

### 6.6 Get All Applicants
**Endpoint:** `GET /api/loan-applications/all`  
**Authentication:** Bearer Token (Admin)

**Response (200 OK):**
```json
[
  {
    "applicantId": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "9876543210"
  }
]
```

---

### 6.7 Get Loan Details by ID
**Endpoint:** `GET /api/loan-applications/loan/{loanId}`  
**Authentication:** Bearer Token

**Response (200 OK):**
```json
{
  "loanId": 1,
  "loanType": "Home Loan",
  "requestedAmount": 5000000,
  "loanTerm": 240,
  "purpose": "Purchase residential property",
  "interestRate": 8.5,
  "status": "PENDING"
}
```

---

## 7. Fraud Detection

### 7.1 Complete Fraud Detection Check
**Endpoint:** `GET /api/fraud-detection/check/{applicantId}`  
**Authentication:** Bearer Token (Admin/Officer)  
**Description:** Runs comprehensive fraud detection (Identity + Financial + Employment + Cross-Verification)

**Response (200 OK):**
```json
{
  "success": true,
  "applicantId": 1,
  "applicantName": "John Doe",
  "totalFraudScore": 45,
  "riskLevel": "MEDIUM",
  "isFraudulent": false,
  "recommendation": "Manual review recommended",
  "triggeredRulesCount": 3,
  "triggeredRules": [
    {
      "ruleName": "High DTI Ratio",
      "category": "FINANCIAL",
      "severity": "MEDIUM",
      "score": 15
    }
  ]
}
```

**Risk Levels:**
- **LOW**: 0-30 points
- **MEDIUM**: 31-60 points
- **HIGH**: 61-80 points
- **CRITICAL**: 81+ points

---

### 7.2 Identity Fraud Check
**Endpoint:** `POST /api/fraud-detection/check-identity/{applicantId}`  
**Authentication:** Bearer Token (Admin/Officer)

**Response (200 OK):**
```json
{
  "success": true,
  "category": "IDENTITY",
  "applicantId": 1,
  "applicantName": "John Doe",
  "totalFraudScore": 10,
  "riskLevel": "LOW",
  "isFraudulent": false,
  "recommendation": "Approve",
  "triggeredRulesCount": 0,
  "triggeredRules": []
}
```

---

### 7.3 Financial Fraud Check
**Endpoint:** `POST /api/fraud-detection/check-financial/{applicantId}`  
**Authentication:** Bearer Token (Admin/Officer)

**Response (200 OK):**
```json
{
  "success": true,
  "category": "FINANCIAL",
  "applicantId": 1,
  "applicantName": "John Doe",
  "totalFraudScore": 20,
  "riskLevel": "LOW",
  "isFraudulent": false,
  "recommendation": "Approve",
  "triggeredRulesCount": 0,
  "triggeredRules": []
}
```

---

### 7.4 Employment Fraud Check
**Endpoint:** `POST /api/fraud-detection/check-employment/{applicantId}`  
**Authentication:** Bearer Token (Admin/Officer)

**Response (200 OK):**
```json
{
  "success": true,
  "category": "EMPLOYMENT",
  "applicantId": 1,
  "applicantName": "John Doe",
  "totalFraudScore": 5,
  "riskLevel": "LOW",
  "isFraudulent": false,
  "recommendation": "Approve",
  "triggeredRulesCount": 0,
  "triggeredRules": []
}
```

---

### 7.5 Cross-Verification Fraud Check
**Endpoint:** `POST /api/fraud-detection/check-cross-verification/{applicantId}`  
**Authentication:** Bearer Token (Admin/Officer)

**Response (200 OK):**
```json
{
  "success": true,
  "category": "CROSS_VERIFICATION",
  "applicantId": 1,
  "applicantName": "John Doe",
  "totalFraudScore": 10,
  "riskLevel": "LOW",
  "isFraudulent": false,
  "recommendation": "Approve",
  "triggeredRulesCount": 0,
  "triggeredRules": []
}
```

---

### 7.6 Get Applicant Fraud Flags
**Endpoint:** `GET /api/fraud-detection/flags/applicant/{applicantId}`  
**Authentication:** Bearer Token (Admin/Officer)

**Response (200 OK):**
```json
[
  {
    "flagId": 1,
    "applicantId": 1,
    "ruleName": "High DTI Ratio",
    "category": "FINANCIAL",
    "severity": "MEDIUM",
    "description": "Debt-to-income ratio exceeds threshold",
    "flaggedAt": "2025-10-06T09:00:00"
  }
]
```

---

### 7.7 Get Loan Fraud Flags
**Endpoint:** `GET /api/fraud-detection/flags/loan/{loanId}`  
**Authentication:** Bearer Token (Admin/Officer)

**Response (200 OK):**
```json
[
  {
    "flagId": 1,
    "loanId": 1,
    "ruleName": "Unusual Loan Amount",
    "category": "FINANCIAL",
    "severity": "HIGH",
    "description": "Loan amount significantly higher than income",
    "flaggedAt": "2025-10-06T09:00:00"
  }
]
```

---

### 7.8 Get High Severity Fraud Flags
**Endpoint:** `GET /api/fraud-detection/flags/high-severity`  
**Authentication:** Bearer Token (Admin/Officer)

**Response (200 OK):**
```json
{
  "count": 5,
  "flags": [
    {
      "flagId": 1,
      "applicantId": 2,
      "ruleName": "Duplicate Identity",
      "category": "IDENTITY",
      "severity": "HIGH",
      "flaggedAt": "2025-10-06T08:00:00"
    }
  ]
}
```

---

### 7.9 Get Critical Fraud Flags
**Endpoint:** `GET /api/fraud-detection/flags/critical`  
**Authentication:** Bearer Token (Admin/Officer)

**Response (200 OK):**
```json
{
  "count": 2,
  "flags": [
    {
      "flagId": 3,
      "applicantId": 5,
      "ruleName": "Known Fraudster",
      "category": "IDENTITY",
      "severity": "CRITICAL",
      "flaggedAt": "2025-10-06T07:00:00"
    }
  ]
}
```

---

## 8. OCR - Document Processing

### 8.1 Extract Text from Document
**Endpoint:** `POST /api/ocr/extract`  
**Authentication:** Bearer Token  
**Content-Type:** `multipart/form-data`

**Form Data:**
- `docType`: Document type (PAN, AADHAAR, PASSPORT, DRIVING_LICENSE, VOTER_ID)
- `file`: Image file (JPG, PNG, PDF)

**Example using cURL:**
```bash
curl -X POST http://localhost:8080/api/ocr/extract \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "docType=PAN" \
  -F "file=@/path/to/pan_card.jpg"
```

**Response (200 OK):**
```json
{
  "documentType": "PAN",
  "extractedText": "PERMANENT ACCOUNT NUMBER\nName: JOHN DOE\nPAN: ABCDE1234F\nDOB: 15/05/1990"
}
```

**Supported Document Types:**
- **PAN**: Permanent Account Number Card
- **AADHAAR**: Aadhaar Card
- **PASSPORT**: Passport
- **DRIVING_LICENSE**: Driving License
- **VOTER_ID**: Voter ID Card

---

## Error Handling

### Standard Error Responses

#### 400 Bad Request
```json
{
  "message": "Invalid request data or business logic error"
}
```

#### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Authentication required. Please provide a valid JWT token.",
  "path": "/api/admin/officers/loan-officers"
}
```

#### 404 Not Found
```json
{
  "message": "Resource not found"
}
```

#### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Error processing request: {error details}"
}
```

---

## Quick Start Guide

### 1. Admin Workflow
```
1. POST /api/admin/auth/register     → Register admin
2. POST /api/admin/auth/login        → Get JWT token
3. Use token for all admin operations
```

### 2. Applicant Workflow
```
1. POST /api/applicant/auth/register      → Register applicant
2. Check email for OTP
3. POST /api/applicant/auth/verify-otp    → Verify email
4. Wait for admin approval
5. POST /api/applicant/auth/login         → Login and get JWT token
6. POST /api/loan-applications/submit     → Submit loan application
```

### 3. Fraud Detection Workflow
```
1. POST /api/loan-applications/submit-complete  → Submit complete application
2. GET /api/fraud-detection/check/{id}          → Run fraud detection
3. GET /api/fraud-detection/flags/applicant/{id} → View fraud flags
```

---

## Important Notes

1. **Content-Type**: Use `application/json` for all requests except file uploads (use `multipart/form-data`)
2. **Date Format**: All dates use ISO 8601 format (YYYY-MM-DD)
3. **Token Security**: Never expose JWT tokens in client-side code
4. **Rate Limiting**: Not currently implemented (consider adding for production)
5. **CORS**: Configure CORS settings for frontend integration
6. **HTTPS**: Always use HTTPS in production environments

---

## Postman Collection

Import the `API_COLLECTION.json` file into Postman for easy testing of all endpoints.

---

## Support & Contact

For API support or questions, please contact the development team.

**Last Updated:** October 6, 2025  
**Version:** 1.0.0
