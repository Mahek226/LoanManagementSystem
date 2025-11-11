-- =====================================================
-- SIMPLIFIED: Single Table for Fraud Rules
-- =====================================================

CREATE TABLE fraud_rule_definition (
    rule_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(100) UNIQUE NOT NULL COMMENT 'e.g., DUPLICATE_AADHAAR',
    rule_name VARCHAR(255) NOT NULL,
    rule_description TEXT,
    rule_category VARCHAR(50) NOT NULL COMMENT 'IDENTITY, FINANCIAL, EMPLOYMENT, CROSS_VERIFICATION',
    
    -- CONFIGURABLE BY ADMIN
    fraud_points INT NOT NULL DEFAULT 0 COMMENT 'Points when rule triggers - ADMIN CAN CHANGE',
    severity VARCHAR(20) NOT NULL COMMENT 'LOW, MEDIUM, HIGH, CRITICAL - ADMIN CAN CHANGE',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Enable/disable rule - ADMIN CAN CHANGE',
    
    -- PARAMETERS (stored as JSON for flexibility)
    parameters JSON COMMENT 'Configurable parameters like {"MIN_AGE": 18, "MAX_DTI_RATIO": 50}',
    
    -- METADATA
    rule_type VARCHAR(50) COMMENT 'THRESHOLD, PATTERN_MATCH, DUPLICATE_CHECK',
    execution_order INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) COMMENT 'Last admin who updated',
    
    INDEX idx_category_active (rule_category, is_active),
    INDEX idx_active (is_active)
) COMMENT='Single table storing all fraud detection rules with configurable points';

-- =====================================================
-- Insert All Rules with Default Values
-- =====================================================

-- IDENTITY RULES
INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, fraud_points, severity, is_active, rule_type, parameters, execution_order) VALUES
('DUPLICATE_AADHAAR', 'Duplicate Aadhaar Number', 'Aadhaar number already used by another applicant', 'IDENTITY', 50, 'CRITICAL', TRUE, 'DUPLICATE_CHECK', NULL, 10),
('DUPLICATE_PAN', 'Duplicate PAN Number', 'PAN number already used by another applicant', 'IDENTITY', 50, 'CRITICAL', TRUE, 'DUPLICATE_CHECK', NULL, 11),
('INVALID_PAN_FORMAT', 'Invalid PAN Format', 'PAN does not match valid format', 'IDENTITY', 30, 'HIGH', TRUE, 'PATTERN_MATCH', '{"regex": "[A-Z]{5}[0-9]{4}[A-Z]{1}"}', 12),
('INVALID_AADHAAR_FORMAT', 'Invalid Aadhaar Format', 'Aadhaar must be exactly 12 digits', 'IDENTITY', 30, 'HIGH', TRUE, 'PATTERN_MATCH', '{"regex": "^[0-9]{12}$"}', 13),
('DOB_MISMATCH', 'Date of Birth Mismatch', 'DOB mismatch across documents', 'IDENTITY', 40, 'HIGH', TRUE, 'CROSS_CHECK', NULL, 20),
('NAME_MISMATCH', 'Name Mismatch', 'Name mismatch across identity documents', 'IDENTITY', 35, 'HIGH', TRUE, 'CROSS_CHECK', NULL, 21),
('GENDER_MISMATCH', 'Gender Mismatch', 'Gender mismatch between applicant and Aadhaar', 'IDENTITY', 25, 'MEDIUM', TRUE, 'CROSS_CHECK', NULL, 22),
('MINOR_APPLICANT', 'Minor Applicant', 'Applicant age below minimum required', 'IDENTITY', 50, 'CRITICAL', TRUE, 'THRESHOLD', '{"MIN_AGE": 18}', 5),
('SUSPICIOUS_AGE_HIGH', 'Suspicious Age (High)', 'Applicant age unusually high', 'IDENTITY', 15, 'LOW', TRUE, 'THRESHOLD', '{"MAX_AGE": 80}', 30),
('SUSPICIOUS_AGE_LOW', 'Suspicious Age (Low)', 'Applicant age 18-20 years', 'IDENTITY', 10, 'LOW', TRUE, 'THRESHOLD', '{"MIN_SAFE_AGE": 21}', 31),
('DUPLICATE_PHONE', 'Duplicate Phone Number', 'Phone used by multiple applicants', 'IDENTITY', 45, 'HIGH', TRUE, 'DUPLICATE_CHECK', NULL, 14),
('DUPLICATE_EMAIL', 'Duplicate Email', 'Email used by multiple applicants', 'IDENTITY', 45, 'HIGH', TRUE, 'DUPLICATE_CHECK', NULL, 15),
('MISSING_AADHAAR', 'Missing Aadhaar', 'Aadhaar document not provided', 'IDENTITY', 25, 'MEDIUM', TRUE, 'CROSS_CHECK', NULL, 16),
('MISSING_PAN', 'Missing PAN', 'PAN document not provided', 'IDENTITY', 25, 'MEDIUM', TRUE, 'CROSS_CHECK', NULL, 17),
('ADDRESS_MISMATCH', 'Address Mismatch', 'Address mismatch between applicant and Aadhaar', 'IDENTITY', 20, 'MEDIUM', TRUE, 'CROSS_CHECK', NULL, 23);

-- FINANCIAL RULES
INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, fraud_points, severity, is_active, rule_type, parameters, execution_order) VALUES
('HIGH_LOAN_TO_INCOME_RATIO', 'High Loan-to-Income Ratio', 'Loan amount exceeds acceptable multiple of annual income', 'FINANCIAL', 60, 'HIGH', TRUE, 'THRESHOLD', '{"MAX_LTI_RATIO": 20}', 40),
('ELEVATED_LOAN_TO_INCOME_RATIO', 'Elevated Loan-to-Income Ratio', 'Loan amount exceeds 10x annual income', 'FINANCIAL', 30, 'MEDIUM', TRUE, 'THRESHOLD', '{"WARNING_LTI_RATIO": 10}', 41),
('HIGH_DEBT_TO_INCOME_RATIO', 'High Debt-to-Income Ratio', 'Monthly debt obligations exceed acceptable percentage', 'FINANCIAL', 50, 'HIGH', TRUE, 'THRESHOLD', '{"MAX_DTI_RATIO": 50}', 42),
('ELEVATED_DEBT_TO_INCOME_RATIO', 'Elevated Debt-to-Income Ratio', 'DTI ratio exceeds 40%', 'FINANCIAL', 30, 'MEDIUM', TRUE, 'THRESHOLD', '{"WARNING_DTI_RATIO": 40}', 43),
('SALARY_MISMATCH', 'Salary Mismatch', 'Declared salary does not match bank credits', 'FINANCIAL', 55, 'HIGH', TRUE, 'CROSS_CHECK', '{"VARIANCE_THRESHOLD": 30}', 44),
('LOW_BALANCE_HIGH_LOAN', 'Low Balance High Loan', 'Very low average balance for high loan request', 'FINANCIAL', 45, 'HIGH', TRUE, 'THRESHOLD', '{"MIN_BALANCE_PERCENT": 1}', 45),
('FREQUENT_CHEQUE_BOUNCES', 'Frequent Cheque Bounces', 'Bank statement shows cheque bounces', 'FINANCIAL', 40, 'HIGH', TRUE, 'PATTERN_MATCH', NULL, 46),
('CASH_SALARY_DECLARATION', 'Cash Salary Declaration', 'Applicant declares cash-based salary', 'FINANCIAL', 35, 'MEDIUM', TRUE, 'PATTERN_MATCH', NULL, 47),
('UNFILED_ITR', 'Unfiled ITR', 'Self-employed with high income but no ITR', 'FINANCIAL', 50, 'HIGH', TRUE, 'CROSS_CHECK', '{"MIN_INCOME_FOR_ITR": 50000}', 48),
('ITR_SALARY_MISMATCH', 'ITR Salary Mismatch', 'Declared income does not match ITR', 'FINANCIAL', 55, 'HIGH', TRUE, 'CROSS_CHECK', '{"VARIANCE_THRESHOLD": 30}', 49),
('EXCESSIVE_CREDIT_UTILIZATION', 'Excessive Credit Utilization', 'Credit card utilization exceeds safe threshold', 'FINANCIAL', 35, 'MEDIUM', TRUE, 'THRESHOLD', '{"MAX_UTILIZATION": 80}', 50),
('CRITICAL_CREDIT_UTILIZATION', 'Critical Credit Utilization', 'Credit card utilization exceeds 90%', 'FINANCIAL', 45, 'HIGH', TRUE, 'THRESHOLD', '{"CRITICAL_UTILIZATION": 90}', 51),
('EXCESSIVE_ACTIVE_LOANS', 'Excessive Active Loans', 'Too many active loans (loan stacking)', 'FINANCIAL', 40, 'HIGH', TRUE, 'THRESHOLD', '{"MAX_ACTIVE_LOANS": 5}', 52),
('MULTIPLE_ACTIVE_LOANS', 'Multiple Active Loans', 'Applicant has 3+ active loans', 'FINANCIAL', 25, 'MEDIUM', TRUE, 'THRESHOLD', '{"WARNING_ACTIVE_LOANS": 3}', 53),
('SHORT_CREDIT_HISTORY', 'Short Credit History', 'Applicant has no credit history', 'FINANCIAL', 30, 'MEDIUM', TRUE, 'THRESHOLD', '{"MIN_AGE_FOR_CREDIT": 25}', 54);

-- EMPLOYMENT RULES
INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, fraud_points, severity, is_active, rule_type, parameters, execution_order) VALUES
('MISSING_EMPLOYMENT_DETAILS', 'Missing Employment Details', 'Employment details not provided', 'EMPLOYMENT', 30, 'MEDIUM', TRUE, 'CROSS_CHECK', NULL, 60),
('SHELL_COMPANY_EMPLOYER', 'Shell Company Employer', 'Employer shows shell company characteristics', 'EMPLOYMENT', 50, 'HIGH', TRUE, 'PATTERN_MATCH', NULL, 61),
('UNVERIFIED_EMPLOYER', 'Unverified Employer', 'Employer not in verified companies database', 'EMPLOYMENT', 35, 'MEDIUM', TRUE, 'CROSS_CHECK', NULL, 62),
('FAKE_EMPLOYER_EMAIL', 'Fake Employer Email', 'Employer uses personal email domain', 'EMPLOYMENT', 45, 'HIGH', TRUE, 'PATTERN_MATCH', NULL, 63),
('FAKE_PAYSLIP_TEMPLATE', 'Fake Payslip Template', 'Payslip contains template/sample text', 'EMPLOYMENT', 50, 'HIGH', TRUE, 'PATTERN_MATCH', NULL, 64),
('MISSING_PAYSLIP', 'Missing Payslip', 'No payslip document provided', 'EMPLOYMENT', 25, 'MEDIUM', TRUE, 'CROSS_CHECK', NULL, 65),
('GHOST_COMPANY', 'Ghost Company', 'Employer shows ghost company indicators', 'EMPLOYMENT', 50, 'HIGH', TRUE, 'PATTERN_MATCH', NULL, 66),
('UNVERIFIABLE_SELF_EMPLOYED', 'Unverifiable Self-Employed', 'Self-employed without proper registration', 'EMPLOYMENT', 40, 'HIGH', TRUE, 'CROSS_CHECK', NULL, 67),
('FUTURE_EMPLOYMENT_DATE', 'Future Employment Date', 'Employment start date is in future', 'EMPLOYMENT', 50, 'CRITICAL', TRUE, 'THRESHOLD', NULL, 68);

-- CROSS-VERIFICATION RULES
INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, fraud_points, severity, is_active, rule_type, parameters, execution_order) VALUES
('NAME_CROSS_VERIFICATION_FAILED', 'Name Cross-Verification Failed', 'Name mismatch across Form, Aadhaar, PAN, Bank', 'CROSS_VERIFICATION', 50, 'HIGH', TRUE, 'CROSS_CHECK', NULL, 70),
('DOB_CROSS_VERIFICATION_FAILED', 'DOB Cross-Verification Failed', 'DOB mismatch across documents', 'CROSS_VERIFICATION', 45, 'HIGH', TRUE, 'CROSS_CHECK', NULL, 71),
('INCOME_CROSS_VERIFICATION_FAILED', 'Income Cross-Verification Failed', 'Income mismatch across sources', 'CROSS_VERIFICATION', 55, 'HIGH', TRUE, 'CROSS_CHECK', '{"VARIANCE_THRESHOLD": 30}', 72),
('EMPLOYER_CROSS_VERIFICATION_FAILED', 'Employer Cross-Verification Failed', 'Employer name mismatch', 'CROSS_VERIFICATION', 45, 'HIGH', TRUE, 'CROSS_CHECK', NULL, 73),
('HIDDEN_LOANS_DETECTED', 'Hidden Loans Detected', 'Declared no loans but EMI transactions found', 'CROSS_VERIFICATION', 50, 'HIGH', TRUE, 'CROSS_CHECK', NULL, 74),
('ADDRESS_CROSS_VERIFICATION_FAILED', 'Address Cross-Verification Failed', 'Address mismatch across sources', 'CROSS_VERIFICATION', 40, 'HIGH', TRUE, 'CROSS_CHECK', NULL, 75);

-- =====================================================
-- View for Easy Querying
-- =====================================================

CREATE OR REPLACE VIEW vw_active_fraud_rules AS
SELECT 
    rule_id,
    rule_code,
    rule_name,
    rule_category,
    fraud_points,
    severity,
    is_active,
    parameters,
    execution_order
FROM fraud_rule_definition
WHERE is_active = TRUE
ORDER BY execution_order;

-- =====================================================
-- Sample Queries
-- =====================================================

-- Get all active rules
-- SELECT * FROM fraud_rule_definition WHERE is_active = TRUE;

-- Get rules by category
-- SELECT * FROM fraud_rule_definition WHERE rule_category = 'IDENTITY' AND is_active = TRUE;

-- Update fraud points (ADMIN CAN DO THIS)
-- UPDATE fraud_rule_definition SET fraud_points = 60, updated_by = 'admin@example.com' WHERE rule_code = 'DUPLICATE_AADHAAR';

-- Update parameter (ADMIN CAN DO THIS)
-- UPDATE fraud_rule_definition SET parameters = JSON_SET(parameters, '$.MIN_AGE', 21) WHERE rule_code = 'MINOR_APPLICANT';

-- Deactivate a rule (ADMIN CAN DO THIS)
-- UPDATE fraud_rule_definition SET is_active = FALSE, updated_by = 'admin@example.com' WHERE rule_code = 'SUSPICIOUS_AGE_LOW';

-- Get parameter value
-- SELECT JSON_EXTRACT(parameters, '$.MIN_AGE') as min_age FROM fraud_rule_definition WHERE rule_code = 'MINOR_APPLICANT';
