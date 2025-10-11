# External Fraud Rule-Based Engine

## Overview
This module provides comprehensive fraud screening capabilities by integrating with an external database containing historical records of persons, their criminal history, loan defaults, bank records, and other relevant information for fraud detection.

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Your Loan     │    │  External Fraud  │    │ External Fraud  │
│   Application   │    │  Rule Engine     │    │   Database      │
│                 │    │                  │    │   (LMS)         │
│ ┌─────────────┐ │    │ ┌──────────────┐ │    │ ┌─────────────┐ │
│ │Internal     │ │    │ │Fraud Rules   │ │    │ │persons      │ │
│ │Fraud Rules  │ │────┼─│Engine        │ │────┼─│loans        │ │
│ │             │ │    │ │              │ │    │ │criminal_rec │ │
│ │- Duplicates │ │    │ │- Criminal    │ │    │ │bank_records │ │
│ │- Patterns   │ │    │ │- Loan History│ │    │ │documents    │ │
│ │- Validation │ │    │ │- Bank Records│ │    │ │             │ │
│ └─────────────┘ │    │ │- Documents   │ │    │ └─────────────┘ │
└─────────────────┘    │ └──────────────┘ │    └─────────────────┘
                       └──────────────────┘
```

## Key Components

### 1. Entities (`entity/`)
- **Person**: Core person information (PAN, Aadhaar, Phone, Email)
- **CriminalRecord**: Criminal history and court cases
- **HistoricalAndCurrentLoan**: Loan history with defaults
- **BankRecord**: Bank account information
- **GovernmentIssuedDocument**: Document verification status

### 2. Repositories (`repository/`)
- Data access layer for external database
- Optimized queries for fraud detection
- Support for complex searches and aggregations

### 3. Fraud Rule Engine (`engine/`)
- **ExternalFraudRuleEngine**: Core fraud detection logic
- Rule-based scoring system
- Configurable fraud thresholds
- Comprehensive risk assessment

### 4. Services (`service/`)
- **ExternalFraudScreeningService**: Main screening service
- **CombinedFraudScreeningService**: Combines internal + external results
- Caching and performance optimization

### 5. Controllers (`controller/`)
- REST APIs for fraud screening
- Combined screening endpoints
- Summary and detailed reporting

## Fraud Rules Implemented

### Criminal Record Rules
- **CRIMINAL_CONVICTION**: 100 points (CRITICAL)
- **CRIMINAL_OPEN_CASE**: 60 points (HIGH)

### Loan History Rules
- **LOAN_DEFAULT_HISTORY**: 40-80 points (MEDIUM-CRITICAL)
- **MULTIPLE_ACTIVE_LOANS**: 50 points (HIGH)
- **HIGH_OUTSTANDING_DEBT**: 45 points (HIGH)

### Bank Record Rules
- **EXCESSIVE_BANK_ACCOUNTS**: 30 points (MEDIUM)
- **MULTIPLE_INACTIVE_ACCOUNTS**: 25 points (MEDIUM)

### Document Verification Rules
- **EXPIRED_DOCUMENT**: 20 points (MEDIUM)
- **UNVERIFIED_DOCUMENT**: 15 points (MEDIUM)

## Risk Scoring System

| Score Range | Risk Level | Recommendation | Action |
|-------------|------------|----------------|---------|
| 0-24        | CLEAN      | APPROVE        | Auto-approve |
| 25-49       | LOW        | REVIEW         | Manual review |
| 50-79       | MEDIUM     | REVIEW         | Detailed review |
| 80-119      | HIGH       | REJECT         | Reject with review |
| 120+        | CRITICAL   | REJECT         | Auto-reject |

## API Endpoints

### External Fraud Screening
```
POST /api/external-fraud/screen/{applicantId}
POST /api/external-fraud/screen/{applicantId}/deep
POST /api/external-fraud/screen/identifiers
GET  /api/external-fraud/summary/{applicantId}
```

### Combined Screening (Internal + External)
```
POST /api/combined-fraud/screen/{applicantId}
GET  /api/combined-fraud/summary/{applicantId}
GET  /api/combined-fraud/flags/{applicantId}
```

## Configuration

### Database Configuration (`application-external.yml`)
```yaml
spring:
  datasource:
    external:
      url: jdbc:mysql://localhost:3306/lms
      username: ${EXTERNAL_DB_USERNAME:root}
      password: ${EXTERNAL_DB_PASSWORD:password}

external-fraud:
  screening:
    enabled: true
    timeout-ms: 10000
    cache-results: true
```

## Sample Usage

### 1. Screen Single Applicant
```bash
curl -X POST http://localhost:8080/api/external-fraud/screen/1505
```

### 2. Combined Screening
```bash
curl -X POST http://localhost:8080/api/combined-fraud/screen/1505
```

### 3. Screen by Identifiers
```bash
curl -X POST "http://localhost:8080/api/external-fraud/screen/identifiers?panNumber=KLMNO9012P&aadhaarNumber=345678901234"
```

## Sample Response

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
      },
      {
        "ruleCode": "LOAN_DEFAULT_HISTORY",
        "ruleName": "Loan Default History",
        "category": "LOAN_HISTORY",
        "severity": "CRITICAL",
        "points": 80,
        "description": "Person has 3 defaulted loan(s)",
        "details": "Total defaulted loans: 3, Total outstanding: ₹2050000"
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
    "totalOutstandingAmount": 2050000.00
  }
}
```

## Test Data

The system includes comprehensive test data with different risk profiles:

### Clean Profile (Person ID: 1 - Amit Patel)
- No criminal record
- Clean loan history
- Verified documents
- **Expected Result**: CLEAN/LOW risk

### Medium Risk Profile (Person ID: 2 - Priya Sharma)
- One loan default
- Unverified documents
- **Expected Result**: MEDIUM risk

### High Risk Profile (Person ID: 3 - Rajesh Kumar)
- Criminal conviction + open case
- Multiple loan defaults
- Multiple inactive bank accounts
- **Expected Result**: CRITICAL risk

### Over-leveraged Profile (Person ID: 4 - Sunita Singh)
- 5 active loans
- High debt burden
- **Expected Result**: HIGH risk

## Performance Considerations

1. **Caching**: Results cached for 30 minutes
2. **Async Processing**: External calls run asynchronously
3. **Timeout Handling**: 10-second timeout with fallback
4. **Database Indexing**: Optimized queries on PAN, Aadhaar, Phone
5. **Connection Pooling**: Separate connection pool for external DB

## Security Features

1. **Data Masking**: PII data masked in logs
2. **Encrypted Storage**: Sensitive data encrypted at rest
3. **Access Control**: Role-based access to fraud data
4. **Audit Trail**: All screening activities logged

## Monitoring & Alerts

1. **Performance Metrics**: Response times, success rates
2. **Fraud Alerts**: High-risk cases flagged immediately
3. **System Health**: Database connectivity, API availability
4. **Business Metrics**: Fraud detection rates, false positives

## Integration Steps

1. **Setup External Database**: Run the LMS database setup
2. **Load Sample Data**: Execute `external-fraud-sample-data.sql`
3. **Configure Application**: Update `application-external.yml`
4. **Test Integration**: Use provided test endpoints
5. **Monitor Results**: Check logs and metrics

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check database credentials
   - Verify database is running
   - Check network connectivity

2. **Person Not Found**
   - Verify PAN/Aadhaar format
   - Check if data exists in external DB
   - Review matching logic

3. **High Response Times**
   - Check database performance
   - Review query optimization
   - Verify caching configuration

4. **False Positives**
   - Review fraud rule thresholds
   - Check data quality
   - Adjust scoring weights

## Future Enhancements

1. **Machine Learning Integration**: ML-based fraud scoring
2. **Real-time Data Sync**: Live updates from external sources
3. **Advanced Analytics**: Fraud pattern detection
4. **API Rate Limiting**: Prevent abuse of screening APIs
5. **Multi-tenant Support**: Support multiple external databases

## Support

For technical support or questions:
- Check application logs: `logs/external-fraud.log`
- Review configuration: `application-external.yml`
- Test connectivity: Use health check endpoints
- Contact: Development Team
