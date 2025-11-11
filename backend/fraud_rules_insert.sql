-- Insert Fraud Rule Definitions for all categories
-- This replaces hardcoded fraud rules with database-driven configuration

-- ============================================
-- IDENTITY FRAUD RULES
-- ============================================

INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, severity, fraud_points, is_active, rule_type, execution_order, created_by) VALUES
('DUPLICATE_AADHAAR', 'Duplicate Aadhaar', 'Aadhaar number is already used by another applicant', 'IDENTITY', 'CRITICAL', 50, true, 'DUPLICATE_CHECK', 1, 'SYSTEM'),
('DUPLICATE_PAN', 'Duplicate PAN', 'PAN number is already used by another applicant', 'IDENTITY', 'CRITICAL', 50, true, 'DUPLICATE_CHECK', 2, 'SYSTEM'),
('INVALID_PAN_FORMAT', 'Invalid PAN Format', 'PAN number does not match valid format', 'IDENTITY', 'HIGH', 30, true, 'PATTERN_MATCH', 3, 'SYSTEM'),
('INVALID_AADHAAR_FORMAT', 'Invalid Aadhaar Format', 'Aadhaar number must be exactly 12 digits', 'IDENTITY', 'HIGH', 30, true, 'PATTERN_MATCH', 4, 'SYSTEM'),
('INVALID_AADHAAR_CHECKSUM', 'Invalid Aadhaar Checksum', 'Aadhaar number failed Verhoeff checksum validation', 'IDENTITY', 'HIGH', 35, true, 'PATTERN_MATCH', 5, 'SYSTEM'),
('DOB_MISMATCH', 'DOB Mismatch', 'Date of Birth mismatch across documents', 'IDENTITY', 'HIGH', 40, true, 'CROSS_CHECK', 6, 'SYSTEM'),
('NAME_MISMATCH', 'Name Mismatch', 'Name mismatch across identity documents', 'IDENTITY', 'HIGH', 35, true, 'CROSS_CHECK', 7, 'SYSTEM'),
('GENDER_MISMATCH', 'Gender Mismatch', 'Gender mismatch between applicant and Aadhaar', 'IDENTITY', 'MEDIUM', 25, true, 'CROSS_CHECK', 8, 'SYSTEM'),
('EXPIRED_PASSPORT', 'Expired Passport', 'Using expired passport as identity proof', 'IDENTITY', 'MEDIUM', 20, true, 'THRESHOLD', 9, 'SYSTEM'),
('DUPLICATE_PHONE', 'Duplicate Phone', 'Phone number is used by multiple applicants', 'IDENTITY', 'HIGH', 45, true, 'DUPLICATE_CHECK', 10, 'SYSTEM'),
('DUPLICATE_EMAIL', 'Duplicate Email', 'Email is used by multiple applicants', 'IDENTITY', 'HIGH', 45, true, 'DUPLICATE_CHECK', 11, 'SYSTEM'),
('MINOR_APPLICANT', 'Minor Applicant', 'Applicant is below 18 years', 'IDENTITY', 'CRITICAL', 50, true, 'THRESHOLD', 12, 'SYSTEM'),
('SUSPICIOUS_AGE_HIGH', 'Suspicious Age High', 'Applicant age is above 80', 'IDENTITY', 'LOW', 15, true, 'THRESHOLD', 13, 'SYSTEM'),
('SUSPICIOUS_AGE_LOW', 'Suspicious Age Low', 'Applicant age is 18-20 range', 'IDENTITY', 'LOW', 10, true, 'THRESHOLD', 14, 'SYSTEM'),
('MISSING_AADHAAR', 'Missing Aadhaar', 'Aadhaar document not provided', 'IDENTITY', 'MEDIUM', 25, true, 'THRESHOLD', 15, 'SYSTEM'),
('MISSING_PAN', 'Missing PAN', 'PAN document not provided', 'IDENTITY', 'MEDIUM', 25, true, 'THRESHOLD', 16, 'SYSTEM'),
('AADHAAR_TAMPERED', 'Aadhaar Tampered', 'Aadhaar document detected as tampered/forged', 'IDENTITY', 'CRITICAL', 50, true, 'PATTERN_MATCH', 17, 'SYSTEM'),
('PAN_TAMPERED', 'PAN Tampered', 'PAN document detected as tampered/forged', 'IDENTITY', 'CRITICAL', 50, true, 'PATTERN_MATCH', 18, 'SYSTEM'),
('PASSPORT_TAMPERED', 'Passport Tampered', 'Passport document detected as tampered/forged', 'IDENTITY', 'CRITICAL', 50, true, 'PATTERN_MATCH', 19, 'SYSTEM'),
('ADDRESS_MISMATCH', 'Address Mismatch', 'Address mismatch between applicant and Aadhaar', 'IDENTITY', 'MEDIUM', 20, true, 'CROSS_CHECK', 20, 'SYSTEM');

-- ============================================
-- FINANCIAL FRAUD RULES
-- ============================================

INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, severity, fraud_points, is_active, rule_type, execution_order, created_by) VALUES
('HIGH_LOAN_TO_INCOME_RATIO', 'High Loan-to-Income Ratio', 'Loan-to-Income ratio exceeds 20x annual income', 'FINANCIAL', 'HIGH', 60, true, 'THRESHOLD', 21, 'SYSTEM'),
('ELEVATED_LOAN_TO_INCOME_RATIO', 'Elevated Loan-to-Income Ratio', 'Loan-to-Income ratio exceeds 10x annual income', 'FINANCIAL', 'MEDIUM', 30, true, 'THRESHOLD', 22, 'SYSTEM'),
('HIGH_DEBT_TO_INCOME_RATIO', 'High Debt-to-Income Ratio', 'Debt-to-Income ratio exceeds 50%', 'FINANCIAL', 'HIGH', 50, true, 'THRESHOLD', 23, 'SYSTEM'),
('ELEVATED_DEBT_TO_INCOME_RATIO', 'Elevated Debt-to-Income Ratio', 'Debt-to-Income ratio exceeds 40%', 'FINANCIAL', 'MEDIUM', 30, true, 'THRESHOLD', 24, 'SYSTEM'),
('SALARY_MISMATCH', 'Salary Mismatch', 'Declared salary does not match bank credit', 'FINANCIAL', 'HIGH', 55, true, 'CROSS_CHECK', 25, 'SYSTEM'),
('LOW_BALANCE_HIGH_LOAN', 'Low Balance High Loan', 'Very low average balance for loan request', 'FINANCIAL', 'HIGH', 45, true, 'THRESHOLD', 26, 'SYSTEM'),
('FREQUENT_CHEQUE_BOUNCES', 'Frequent Cheque Bounces', 'Bank statement shows cheque bounces or payment failures', 'FINANCIAL', 'HIGH', 40, true, 'PATTERN_MATCH', 27, 'SYSTEM'),
('CASH_SALARY_DECLARATION', 'Cash Salary Declaration', 'Applicant declares cash-based salary or unverifiable income source', 'FINANCIAL', 'MEDIUM', 35, true, 'PATTERN_MATCH', 28, 'SYSTEM'),
('UNFILED_ITR', 'Unfiled ITR', 'Self-employed claiming high income but no ITR filed', 'FINANCIAL', 'HIGH', 50, true, 'CROSS_CHECK', 29, 'SYSTEM'),
('ITR_SALARY_MISMATCH', 'ITR Salary Mismatch', 'Declared annual income does not match ITR income', 'FINANCIAL', 'HIGH', 55, true, 'CROSS_CHECK', 30, 'SYSTEM'),
('EXCESSIVE_CREDIT_UTILIZATION', 'Excessive Credit Utilization', 'Credit card utilization exceeds 80%', 'FINANCIAL', 'MEDIUM', 35, true, 'THRESHOLD', 31, 'SYSTEM'),
('CRITICAL_CREDIT_UTILIZATION', 'Critical Credit Utilization', 'Credit card utilization exceeds 90%', 'FINANCIAL', 'HIGH', 45, true, 'THRESHOLD', 32, 'SYSTEM'),
('EXCESSIVE_ACTIVE_LOANS', 'Excessive Active Loans', 'Applicant has 5+ active loans (loan stacking fraud)', 'FINANCIAL', 'HIGH', 40, true, 'THRESHOLD', 33, 'SYSTEM'),
('MULTIPLE_ACTIVE_LOANS', 'Multiple Active Loans', 'Applicant has 3+ active loans', 'FINANCIAL', 'MEDIUM', 25, true, 'THRESHOLD', 34, 'SYSTEM'),
('SHORT_CREDIT_HISTORY', 'Short Credit History', 'Applicant has no credit history', 'FINANCIAL', 'MEDIUM', 30, true, 'THRESHOLD', 35, 'SYSTEM'),
('NEW_TO_CREDIT', 'New to Credit', 'Young applicant with no credit history', 'FINANCIAL', 'LOW', 20, true, 'THRESHOLD', 36, 'SYSTEM');

-- ============================================
-- EMPLOYMENT FRAUD RULES
-- ============================================

INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, severity, fraud_points, is_active, rule_type, execution_order, created_by) VALUES
('MISSING_EMPLOYMENT_DETAILS', 'Missing Employment Details', 'Employment details not provided', 'EMPLOYMENT', 'MEDIUM', 30, true, 'THRESHOLD', 37, 'SYSTEM'),
('SHELL_COMPANY_EMPLOYER', 'Shell Company Employer', 'Employer shows shell company characteristics', 'EMPLOYMENT', 'HIGH', 50, true, 'PATTERN_MATCH', 38, 'SYSTEM'),
('UNVERIFIED_EMPLOYER', 'Unverified Employer', 'Employer not found in verified companies database', 'EMPLOYMENT', 'MEDIUM', 35, true, 'PATTERN_MATCH', 39, 'SYSTEM'),
('FAKE_EMPLOYER_EMAIL', 'Fake Employer Email', 'Employer uses personal email domain instead of corporate domain', 'EMPLOYMENT', 'HIGH', 45, true, 'PATTERN_MATCH', 40, 'SYSTEM'),
('PERSONAL_EMAIL_IN_EMPLOYER', 'Personal Email in Employer', 'Employer name contains personal email domain', 'EMPLOYMENT', 'HIGH', 40, true, 'PATTERN_MATCH', 41, 'SYSTEM'),
('MISSING_PAYSLIP', 'Missing Payslip', 'No payslip document provided for verification', 'EMPLOYMENT', 'MEDIUM', 25, true, 'THRESHOLD', 42, 'SYSTEM'),
('FAKE_PAYSLIP_TEMPLATE', 'Fake Payslip Template', 'Payslip contains template/sample text - likely forged', 'EMPLOYMENT', 'HIGH', 50, true, 'PATTERN_MATCH', 43, 'SYSTEM'),
('PAYSLIP_EMPLOYER_MISMATCH', 'Payslip Employer Mismatch', 'Payslip does not contain employer name', 'EMPLOYMENT', 'HIGH', 40, true, 'CROSS_CHECK', 44, 'SYSTEM'),
('INCOMPLETE_PAYSLIP', 'Incomplete Payslip', 'Payslip missing standard fields', 'EMPLOYMENT', 'MEDIUM', 30, true, 'PATTERN_MATCH', 45, 'SYSTEM'),
('RESIDENTIAL_EMPLOYER_ADDRESS', 'Residential Employer Address', 'Employer address appears to be residential, not commercial', 'EMPLOYMENT', 'MEDIUM', 35, true, 'PATTERN_MATCH', 46, 'SYSTEM'),
('VAGUE_EMPLOYER_NAME', 'Vague Employer Name', 'Employer name is too generic/vague', 'EMPLOYMENT', 'MEDIUM', 25, true, 'PATTERN_MATCH', 47, 'SYSTEM'),
('EMPLOYMENT_DURATION_MISMATCH', 'Employment Duration Mismatch', 'Employment duration mismatch between application and payslip', 'EMPLOYMENT', 'HIGH', 45, true, 'CROSS_CHECK', 48, 'SYSTEM'),
('UNREALISTIC_EMPLOYMENT_DURATION', 'Unrealistic Employment Duration', 'Employment duration exceeds reasonable working years', 'EMPLOYMENT', 'MEDIUM', 30, true, 'THRESHOLD', 49, 'SYSTEM'),
('FUTURE_EMPLOYMENT_DATE', 'Future Employment Date', 'Employment start date is in the future', 'EMPLOYMENT', 'CRITICAL', 50, true, 'THRESHOLD', 50, 'SYSTEM'),
('UNVERIFIABLE_SELF_EMPLOYED', 'Unverifiable Self-Employed', 'Self-employed business not registered (No GST/Business Registration)', 'EMPLOYMENT', 'HIGH', 40, true, 'CROSS_CHECK', 51, 'SYSTEM'),
('SELF_EMPLOYED_NO_ITR', 'Self-Employed No ITR', 'Self-employed with high income but no ITR filed', 'EMPLOYMENT', 'MEDIUM', 35, true, 'CROSS_CHECK', 52, 'SYSTEM'),
('SELF_EMPLOYED_NO_PAN', 'Self-Employed No PAN', 'Self-employed applicant without PAN number', 'EMPLOYMENT', 'MEDIUM', 30, true, 'CROSS_CHECK', 53, 'SYSTEM'),
('GHOST_COMPANY', 'Ghost Company', 'Employer shows ghost company characteristics', 'EMPLOYMENT', 'HIGH', 50, true, 'PATTERN_MATCH', 54, 'SYSTEM'),
('SUSPICIOUS_EMPLOYER', 'Suspicious Employer', 'Employer shows suspicious characteristics', 'EMPLOYMENT', 'MEDIUM', 30, true, 'PATTERN_MATCH', 55, 'SYSTEM');

-- ============================================
-- CROSS-VERIFICATION FRAUD RULES
-- ============================================

INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, severity, fraud_points, is_active, rule_type, execution_order, created_by) VALUES
('IDENTITY_CROSS_VERIFY_FAIL', 'Identity Cross-Verification Failed', 'Multiple identity mismatches detected across documents', 'CROSS_VERIFICATION', 'CRITICAL', 60, true, 'CROSS_CHECK', 56, 'SYSTEM'),
('ADDRESS_CROSS_VERIFY_FAIL', 'Address Cross-Verification Failed', 'Address mismatch across multiple documents', 'CROSS_VERIFICATION', 'HIGH', 45, true, 'CROSS_CHECK', 57, 'SYSTEM'),
('PAN_AADHAAR_LINK_FAIL', 'PAN-Aadhaar Link Failed', 'PAN and Aadhaar details do not match', 'CROSS_VERIFICATION', 'HIGH', 50, true, 'CROSS_CHECK', 58, 'SYSTEM'),
('INCOME_CROSS_VERIFY_FAIL', 'Income Cross-Verification Failed', 'Income mismatch across employment and financial documents', 'CROSS_VERIFICATION', 'HIGH', 55, true, 'CROSS_CHECK', 59, 'SYSTEM'),
('EMPLOYMENT_CROSS_VERIFY_FAIL', 'Employment Cross-Verification Failed', 'Employment details do not match with financial records', 'CROSS_VERIFICATION', 'HIGH', 50, true, 'CROSS_CHECK', 60, 'SYSTEM'),
('BANKING_CROSS_VERIFY_FAIL', 'Banking Cross-Verification Failed', 'Bank account details mismatch', 'CROSS_VERIFICATION', 'MEDIUM', 40, true, 'CROSS_CHECK', 61, 'SYSTEM'),
('PROPERTY_VALUATION_MISMATCH', 'Property Valuation Mismatch', 'Declared property value does not match market value', 'CROSS_VERIFICATION', 'HIGH', 45, true, 'THRESHOLD', 62, 'SYSTEM'),
('COLLATERAL_FRAUD', 'Collateral Fraud', 'Collateral already pledged or fraudulent', 'CROSS_VERIFICATION', 'CRITICAL', 70, true, 'DUPLICATE_CHECK', 63, 'SYSTEM'),
('SYNTHETIC_IDENTITY', 'Synthetic Identity', 'Possible synthetic identity fraud detected', 'CROSS_VERIFICATION', 'CRITICAL', 80, true, 'PATTERN_MATCH', 64, 'SYSTEM'),
('RAPID_APPLICATION_PATTERN', 'Rapid Application Pattern', 'Multiple loan applications in short time', 'CROSS_VERIFICATION', 'HIGH', 50, true, 'PATTERN_MATCH', 65, 'SYSTEM');

-- Verify the insert
SELECT rule_category, COUNT(*) as rule_count 
FROM fraud_rule_definition 
GROUP BY rule_category 
ORDER BY rule_category;
