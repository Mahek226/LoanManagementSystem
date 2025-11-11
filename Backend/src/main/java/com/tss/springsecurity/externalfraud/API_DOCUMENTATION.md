# External Fraud Rule-Based Engine - API Documentation

## Base URL
```
http://localhost:8080/api
```

## Authentication
All APIs require valid JWT token in Authorization header:
```
Authorization: Bearer <jwt_token>
```

---

## 1. External Fraud Screening APIs

### 1.1 Screen Applicant by ID
**Endpoint:** `POST /external-fraud/screen/{applicantId}`

**Description:** Perform external fraud screening for an existing applicant

**Parameters:**
- `applicantId` (path) - Internal applicant ID

**Response:**
```json
{
  "success": true,
  "applicantId": 1505,
  "externalScreeningResult": {
    "personFound": true,
    "externalPersonId": 3,
    "matchedBy": "PAN",
    "totalFraudScore": 180,
    "riskLevel": "CRITICAL",
    "isFraudulent": true,
    "recommendation": "REJECT",
    "fraudFlags": [
      {
        "ruleCode": "CRIMINAL_CONVICTION",
        "ruleName": "Criminal Conviction Found",
        "category": "CRIMINAL",
        "severity": "CRITICAL",
        "points": 100,
        "description": "Person has 1 criminal conviction(s)",
        "details": "Convicted cases: 1, Case types: FINANCIAL_FRAUD"
      }
    ],
    "hasCriminalRecord": true,
    "totalCriminalCases": 2,
    "convictedCases": 1,
    "openCases": 1,
    "hasLoanHistory": true,
    "totalLoans": 4,
    "activeLoans": 1,
    "defaultedLoans": 3,
    "totalOutstandingAmount": 2050000.00,
    "screeningTimestamp": "2024-10-10T16:30:00",
    "screeningDurationMs": 1250
  }
}
```

### 1.2 Deep Screening
**Endpoint:** `POST /external-fraud/screen/{applicantId}/deep`

**Description:** Perform comprehensive deep screening with additional checks

**Parameters:**
- `applicantId` (path) - Internal applicant ID

**Response:** Same as 1.1 but with additional deep analysis

### 1.3 Screen by Identifiers
**Endpoint:** `POST /external-fraud/screen/identifiers`

**Description:** Screen using specific identifiers without internal applicant

**Query Parameters:**
- `panNumber` (optional) - PAN card number
- `aadhaarNumber` (optional) - Aadhaar card number  
- `phoneNumber` (optional) - Phone number
- `email` (optional) - Email address

**Example:**
```bash
curl -X POST "http://localhost:8080/api/external-fraud/screen/identifiers?panNumber=KLMNO9012P&aadhaarNumber=345678901234"
```

### 1.4 Get Screening Summary
**Endpoint:** `GET /external-fraud/summary/{applicantId}`

**Description:** Get quick summary of external fraud screening

**Response:**
```json
{
  "success": true,
  "summary": {
    "applicantId": 1505,
    "personFoundInExternalDB": true,
    "riskLevel": "CRITICAL",
    "recommendation": "REJECT",
    "totalFraudScore": 180,
    "fraudFlagsCount": 2,
    "hasCriminalRecord": true,
    "hasLoanHistory": true,
    "screeningTimestamp": "2024-10-10T16:30:00"
  }
}
```

---

## 2. Combined Fraud Screening APIs

### 2.1 Combined Screening
**Endpoint:** `POST /combined-fraud/screen/{applicantId}`

**Description:** Perform both internal and external fraud screening

**Response:**
```json
{
  "success": true,
  "applicantId": 1505,
  "screeningType": "COMBINED",
  "combinedResult": {
    "applicantId": 1505,
    "screeningTimestamp": "2024-10-10T16:30:00",
    "totalScreeningTimeMs": 2500,
    "combinedFraudScore": 156,
    "finalRiskLevel": "CRITICAL",
    "finalRecommendation": "REJECT",
    "allFraudFlags": [
      {
        "source": "INTERNAL",
        "ruleCode": "DUPLICATE_PAN",
        "ruleName": "Duplicate PAN",
        "category": "IDENTITY",
        "severity": "CRITICAL",
        "points": 50
      },
      {
        "source": "EXTERNAL", 
        "ruleCode": "CRIMINAL_CONVICTION",
        "ruleName": "Criminal Conviction Found",
        "category": "CRIMINAL",
        "severity": "CRITICAL",
        "points": 100
      }
    ],
    "insights": [
      "Criminal record found - high risk applicant",
      "Multiple high-risk fraud indicators detected across internal and external sources"
    ]
  },
  "summary": {
    "finalRiskLevel": "CRITICAL",
    "finalRecommendation": "REJECT",
    "combinedFraudScore": 156,
    "totalFraudFlags": 2,
    "screeningTimeMs": 2500,
    "internalRiskLevel": "HIGH",
    "internalScore": 60,
    "externalRiskLevel": "CRITICAL",
    "externalScore": 180,
    "personFoundInExternalDB": true
  }
}
```

### 2.2 Combined Summary
**Endpoint:** `GET /combined-fraud/summary/{applicantId}`

**Description:** Get summary of combined screening results

### 2.3 Fraud Flags Breakdown
**Endpoint:** `GET /combined-fraud/flags/{applicantId}`

**Description:** Get detailed breakdown of fraud flags by source

**Response:**
```json
{
  "success": true,
  "breakdown": {
    "applicantId": 1505,
    "totalFlags": 2,
    "internal": {
      "count": 1,
      "totalPoints": 50,
      "flags": [...]
    },
    "external": {
      "count": 1,
      "totalPoints": 100,
      "flags": [...]
    }
  }
}
```

---

## 3. Test & Monitoring APIs

### 3.1 Connectivity Test
**Endpoint:** `GET /external-fraud/test/connectivity`

**Description:** Test external database connectivity

**Response:**
```json
{
  "success": true,
  "message": "External database connectivity successful",
  "totalPersonsInDB": 5,
  "timestamp": 1697123456789
}
```

### 3.2 Risk Profile Testing
**Endpoint:** `GET /external-fraud/test/risk-profiles`

**Description:** Test all predefined risk profiles

**Response:**
```json
{
  "success": true,
  "message": "Risk profile testing completed",
  "riskProfiles": {
    "CLEAN_PROFILE": {
      "name": "Amit Patel",
      "pan": "ABCDE1234F",
      "riskLevel": "CLEAN",
      "fraudScore": 0,
      "recommendation": "APPROVE",
      "personFound": true
    },
    "HIGH_RISK_PROFILE": {
      "name": "Rajesh Kumar", 
      "pan": "KLMNO9012P",
      "riskLevel": "CRITICAL",
      "fraudScore": 180,
      "recommendation": "REJECT",
      "personFound": true,
      "hasCriminalRecord": true,
      "defaultedLoans": 3,
      "convictedCases": 1
    }
  }
}
```

### 3.3 Test by PAN
**Endpoint:** `GET /external-fraud/test/test-pan/{panNumber}`

**Description:** Test fraud screening for specific PAN

### 3.4 Performance Test
**Endpoint:** `POST /external-fraud/test/performance-test`

**Description:** Test screening performance with multiple PANs

**Request Body:**
```json
{
  "panNumbers": ["ABCDE1234F", "KLMNO9012P", "PQRST3456U"]
}
```

### 3.5 Health Check
**Endpoint:** `GET /external-fraud/test/health`

**Description:** System health check

**Response:**
```json
{
  "database": {
    "status": "UP",
    "personCount": 5
  },
  "fraudEngine": {
    "status": "UP", 
    "testScreeningTime": "245ms"
  },
  "overall": "UP",
  "timestamp": 1697123456789
}
```

---

## 4. Risk Levels & Scoring

### Risk Level Mapping
| Score Range | Risk Level | Recommendation | Action |
|-------------|------------|----------------|---------|
| 0-24        | CLEAN      | APPROVE        | Auto-approve |
| 25-49       | LOW        | REVIEW         | Manual review |
| 50-79       | MEDIUM     | REVIEW         | Detailed review |
| 80-119      | HIGH       | REJECT         | Reject with review |
| 120+        | CRITICAL   | REJECT         | Auto-reject |

### Fraud Rule Points
| Rule Category | Rule Name | Points | Severity |
|---------------|-----------|--------|----------|
| Criminal | Criminal Conviction | 100 | CRITICAL |
| Criminal | Open Criminal Case | 60 | HIGH |
| Loan History | Loan Default (3+) | 80 | CRITICAL |
| Loan History | Loan Default (2) | 60 | HIGH |
| Loan History | Loan Default (1) | 40 | MEDIUM |
| Loan History | Multiple Active Loans (5+) | 50 | HIGH |
| Loan History | High Outstanding Debt | 45 | HIGH |
| Bank Records | Excessive Bank Accounts (10+) | 30 | MEDIUM |
| Bank Records | Multiple Inactive Accounts | 25 | MEDIUM |
| Documents | Expired Document | 20 | MEDIUM |
| Documents | Unverified Document | 15 | MEDIUM |

---

## 5. Error Codes

| HTTP Code | Error Type | Description |
|-----------|------------|-------------|
| 400 | Bad Request | Invalid parameters or missing required fields |
| 401 | Unauthorized | Invalid or missing JWT token |
| 404 | Not Found | Applicant or resource not found |
| 500 | Internal Server Error | System error during processing |
| 503 | Service Unavailable | External database connectivity issues |

### Error Response Format
```json
{
  "success": false,
  "applicantId": 1505,
  "message": "External fraud screening failed: Database connection timeout",
  "timestamp": 1697123456789,
  "errorCode": "EXT_DB_TIMEOUT"
}
```

---

## 6. Rate Limits

- **Standard APIs**: 100 requests per minute per user
- **Test APIs**: 50 requests per minute per user
- **Performance Tests**: 10 requests per minute per user

---

## 7. Sample Test Data

### Test PANs Available
- `ABCDE1234F` - Clean profile (Amit Patel)
- `FGHIJ5678K` - Medium risk (Priya Sharma) 
- `KLMNO9012P` - High risk (Rajesh Kumar)
- `PQRST3456U` - Over-leveraged (Sunita Singh)
- `UVWXY7890Z` - Clean with history (Vikram Gupta)

### Test Applicant IDs
- Use existing applicant IDs from your internal database
- Test with applicants having matching PAN/Aadhaar in external DB

---

## 8. Integration Examples

### Java/Spring Boot
```java
@Autowired
private ExternalFraudScreeningService externalFraudService;

public void screenApplicant(Long applicantId) {
    ExternalFraudCheckResult result = externalFraudService.screenApplicant(applicantId);
    
    if ("REJECT".equals(result.getRecommendation())) {
        // Handle rejection
        rejectApplication(applicantId, result.getFraudFlags());
    }
}
```

### cURL Examples
```bash
# Screen applicant
curl -X POST http://localhost:8080/api/external-fraud/screen/1505 \
  -H "Authorization: Bearer <token>"

# Screen by PAN
curl -X POST "http://localhost:8080/api/external-fraud/screen/identifiers?panNumber=KLMNO9012P" \
  -H "Authorization: Bearer <token>"

# Combined screening
curl -X POST http://localhost:8080/api/combined-fraud/screen/1505 \
  -H "Authorization: Bearer <token>"

# Health check
curl -X GET http://localhost:8080/api/external-fraud/test/health \
  -H "Authorization: Bearer <token>"
```

---

## 9. Best Practices

1. **Always use combined screening** for comprehensive fraud detection
2. **Cache results** for 30 minutes to improve performance
3. **Handle timeouts gracefully** with fallback to internal screening
4. **Log all screening activities** for audit purposes
5. **Monitor false positive rates** and adjust thresholds accordingly
6. **Use test endpoints** for development and debugging
7. **Implement circuit breakers** for external database calls
8. **Mask sensitive data** in logs and responses

---

## 10. Support & Troubleshooting

### Common Issues
1. **Database Connection Failed**: Check external DB credentials and connectivity
2. **Person Not Found**: Verify PAN/Aadhaar exists in external database
3. **High Response Times**: Check database performance and query optimization
4. **False Positives**: Review fraud rule thresholds and data quality

### Monitoring
- Check application logs: `logs/external-fraud.log`
- Monitor API response times and success rates
- Track fraud detection accuracy and false positive rates
- Set up alerts for system health and performance issues
