-- =====================================================
-- Fraud Rule Management Database Schema
-- =====================================================

-- Table 1: Master Rule Definition
CREATE TABLE fraud_rule_definition (
    rule_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(100) UNIQUE NOT NULL COMMENT 'Unique rule identifier (e.g., DUPLICATE_AADHAAR)',
    rule_name VARCHAR(255) NOT NULL COMMENT 'Human-readable rule name',
    rule_description TEXT COMMENT 'Detailed description of what the rule checks',
    rule_category VARCHAR(50) NOT NULL COMMENT 'IDENTITY, FINANCIAL, EMPLOYMENT, CROSS_VERIFICATION',
    severity VARCHAR(20) NOT NULL COMMENT 'LOW, MEDIUM, HIGH, CRITICAL',
    fraud_points INT NOT NULL DEFAULT 0 COMMENT 'Points assigned when rule is triggered',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Whether rule is currently active',
    rule_type VARCHAR(50) COMMENT 'THRESHOLD, PATTERN_MATCH, DUPLICATE_CHECK, CROSS_CHECK',
    execution_order INT DEFAULT 100 COMMENT 'Order in which rules should be executed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    INDEX idx_category_active (rule_category, is_active),
    INDEX idx_severity (severity),
    INDEX idx_execution_order (execution_order)
) COMMENT='Master table storing all fraud detection rule definitions';

-- Table 2: Rule Parameters (Configurable Thresholds)
CREATE TABLE fraud_rule_parameter (
    param_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    param_name VARCHAR(100) NOT NULL COMMENT 'Parameter name (e.g., MIN_AGE, MAX_DTI_RATIO)',
    param_value VARCHAR(500) NOT NULL COMMENT 'Parameter value',
    param_type VARCHAR(50) NOT NULL COMMENT 'INTEGER, DECIMAL, STRING, BOOLEAN, PERCENTAGE',
    param_description TEXT COMMENT 'What this parameter controls',
    is_configurable BOOLEAN DEFAULT TRUE COMMENT 'Can admin change this?',
    min_value VARCHAR(50) COMMENT 'Minimum allowed value',
    max_value VARCHAR(50) COMMENT 'Maximum allowed value',
    default_value VARCHAR(500) COMMENT 'Default value',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rule_id) REFERENCES fraud_rule_definition(rule_id) ON DELETE CASCADE,
    UNIQUE KEY uk_rule_param (rule_id, param_name),
    INDEX idx_rule_id (rule_id)
) COMMENT='Configurable parameters for each rule (thresholds, limits, percentages)';

-- Table 3: Rule Patterns (Regex, Keywords)
CREATE TABLE fraud_rule_pattern (
    pattern_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    pattern_type VARCHAR(50) NOT NULL COMMENT 'REGEX, KEYWORD, DOMAIN, COMPANY_NAME',
    pattern_value TEXT NOT NULL COMMENT 'The actual pattern/keyword',
    pattern_description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rule_id) REFERENCES fraud_rule_definition(rule_id) ON DELETE CASCADE,
    INDEX idx_rule_pattern (rule_id, pattern_type)
) COMMENT='Patterns and keywords used by rules (regex, shell company keywords, etc.)';

-- Table 4: Rule Lists (Whitelists/Blacklists)
CREATE TABLE fraud_rule_list (
    list_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    list_type VARCHAR(50) NOT NULL COMMENT 'WHITELIST, BLACKLIST, VERIFIED_COMPANIES, INVALID_DOMAINS',
    list_value VARCHAR(500) NOT NULL COMMENT 'The actual list item',
    list_description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rule_id) REFERENCES fraud_rule_definition(rule_id) ON DELETE CASCADE,
    INDEX idx_rule_list (rule_id, list_type),
    INDEX idx_list_value (list_value(100))
) COMMENT='List-based data for rules (verified companies, invalid email domains, etc.)';

-- Table 5: Rule Audit Trail
CREATE TABLE fraud_rule_audit (
    audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    change_type VARCHAR(50) NOT NULL COMMENT 'CREATED, UPDATED, DELETED, ACTIVATED, DEACTIVATED, POINTS_CHANGED',
    field_name VARCHAR(100) COMMENT 'Which field was changed',
    old_value TEXT COMMENT 'Previous value',
    new_value TEXT COMMENT 'New value',
    changed_by VARCHAR(100) NOT NULL COMMENT 'Username/ID of person who made change',
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    change_reason TEXT COMMENT 'Why was this change made',
    ip_address VARCHAR(50),
    FOREIGN KEY (rule_id) REFERENCES fraud_rule_definition(rule_id) ON DELETE CASCADE,
    INDEX idx_rule_audit (rule_id, changed_at),
    INDEX idx_changed_by (changed_by)
) COMMENT='Audit trail of all changes to fraud rules';

-- Table 6: Rule Execution Log
CREATE TABLE fraud_rule_execution_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    loan_id BIGINT,
    was_triggered BOOLEAN NOT NULL COMMENT 'Did the rule trigger?',
    fraud_points_assigned INT DEFAULT 0,
    execution_time_ms INT COMMENT 'How long did rule take to execute',
    rule_details TEXT COMMENT 'Additional details about why rule triggered',
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rule_id) REFERENCES fraud_rule_definition(rule_id) ON DELETE CASCADE,
    INDEX idx_applicant (applicant_id),
    INDEX idx_loan (loan_id),
    INDEX idx_triggered (was_triggered, executed_at),
    INDEX idx_executed_at (executed_at)
) COMMENT='Log of every rule execution for analytics and debugging';

-- Table 7: Rule Performance Metrics
CREATE TABLE fraud_rule_metrics (
    metric_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    date DATE NOT NULL,
    total_executions INT DEFAULT 0,
    times_triggered INT DEFAULT 0,
    avg_execution_time_ms DECIMAL(10,2),
    false_positive_count INT DEFAULT 0 COMMENT 'Manually marked as false positive',
    true_positive_count INT DEFAULT 0 COMMENT 'Confirmed fraud',
    trigger_rate DECIMAL(5,2) COMMENT 'Percentage of times rule triggered',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rule_id) REFERENCES fraud_rule_definition(rule_id) ON DELETE CASCADE,
    UNIQUE KEY uk_rule_date (rule_id, date),
    INDEX idx_date (date)
) COMMENT='Daily aggregated metrics for rule performance monitoring';

-- =====================================================
-- Insert Sample Rules
-- =====================================================

-- IDENTITY RULES
INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, severity, fraud_points, is_active, rule_type, execution_order) VALUES
('DUPLICATE_AADHAAR', 'Duplicate Aadhaar Number', 'Aadhaar number is already used by another applicant', 'IDENTITY', 'CRITICAL', 50, TRUE, 'DUPLICATE_CHECK', 10),
('DUPLICATE_PAN', 'Duplicate PAN Number', 'PAN number is already used by another applicant', 'IDENTITY', 'CRITICAL', 50, TRUE, 'DUPLICATE_CHECK', 11),
('INVALID_PAN_FORMAT', 'Invalid PAN Format', 'PAN number does not match valid format', 'IDENTITY', 'HIGH', 30, TRUE, 'PATTERN_MATCH', 12),
('INVALID_AADHAAR_FORMAT', 'Invalid Aadhaar Format', 'Aadhaar must be exactly 12 digits', 'IDENTITY', 'HIGH', 30, TRUE, 'PATTERN_MATCH', 13),
('DOB_MISMATCH', 'Date of Birth Mismatch', 'DOB mismatch across documents', 'IDENTITY', 'HIGH', 40, TRUE, 'CROSS_CHECK', 20),
('NAME_MISMATCH', 'Name Mismatch', 'Name mismatch across identity documents', 'IDENTITY', 'HIGH', 35, TRUE, 'CROSS_CHECK', 21),
('GENDER_MISMATCH', 'Gender Mismatch', 'Gender mismatch between applicant and Aadhaar', 'IDENTITY', 'MEDIUM', 25, TRUE, 'CROSS_CHECK', 22),
('MINOR_APPLICANT', 'Minor Applicant', 'Applicant age is below minimum required age', 'IDENTITY', 'CRITICAL', 50, TRUE, 'THRESHOLD', 5),
('SUSPICIOUS_AGE_HIGH', 'Suspicious Age (High)', 'Applicant age is unusually high', 'IDENTITY', 'LOW', 15, TRUE, 'THRESHOLD', 30),
('DUPLICATE_PHONE', 'Duplicate Phone Number', 'Phone number used by multiple applicants', 'IDENTITY', 'HIGH', 45, TRUE, 'DUPLICATE_CHECK', 14),
('DUPLICATE_EMAIL', 'Duplicate Email', 'Email used by multiple applicants', 'IDENTITY', 'HIGH', 45, TRUE, 'DUPLICATE_CHECK', 15);

-- FINANCIAL RULES
INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, severity, fraud_points, is_active, rule_type, execution_order) VALUES
('HIGH_LOAN_TO_INCOME_RATIO', 'High Loan-to-Income Ratio', 'Loan amount exceeds acceptable multiple of annual income', 'FINANCIAL', 'HIGH', 60, TRUE, 'THRESHOLD', 40),
('HIGH_DEBT_TO_INCOME_RATIO', 'High Debt-to-Income Ratio', 'Monthly debt obligations exceed acceptable percentage of income', 'FINANCIAL', 'HIGH', 50, TRUE, 'THRESHOLD', 41),
('SALARY_MISMATCH', 'Salary Mismatch', 'Declared salary does not match bank statement credits', 'FINANCIAL', 'HIGH', 55, TRUE, 'CROSS_CHECK', 42),
('LOW_BALANCE_HIGH_LOAN', 'Low Balance High Loan', 'Very low average balance for high loan request', 'FINANCIAL', 'HIGH', 45, TRUE, 'THRESHOLD', 43),
('EXCESSIVE_CREDIT_UTILIZATION', 'Excessive Credit Utilization', 'Credit card utilization exceeds safe threshold', 'FINANCIAL', 'MEDIUM', 35, TRUE, 'THRESHOLD', 44),
('EXCESSIVE_ACTIVE_LOANS', 'Excessive Active Loans', 'Too many active loans indicating loan stacking', 'FINANCIAL', 'HIGH', 40, TRUE, 'THRESHOLD', 45),
('UNFILED_ITR', 'Unfiled ITR', 'Self-employed with high income but no ITR filed', 'FINANCIAL', 'HIGH', 50, TRUE, 'CROSS_CHECK', 46);

-- EMPLOYMENT RULES
INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, severity, fraud_points, is_active, rule_type, execution_order) VALUES
('SHELL_COMPANY_EMPLOYER', 'Shell Company Employer', 'Employer shows shell company characteristics', 'EMPLOYMENT', 'HIGH', 50, TRUE, 'PATTERN_MATCH', 50),
('UNVERIFIED_EMPLOYER', 'Unverified Employer', 'Employer not found in verified companies database', 'EMPLOYMENT', 'MEDIUM', 35, TRUE, 'CROSS_CHECK', 51),
('FAKE_EMPLOYER_EMAIL', 'Fake Employer Email', 'Employer uses personal email domain instead of corporate', 'EMPLOYMENT', 'HIGH', 45, TRUE, 'PATTERN_MATCH', 52),
('FAKE_PAYSLIP_TEMPLATE', 'Fake Payslip Template', 'Payslip contains template or sample text', 'EMPLOYMENT', 'HIGH', 50, TRUE, 'PATTERN_MATCH', 53),
('GHOST_COMPANY', 'Ghost Company', 'Employer shows multiple ghost company indicators', 'EMPLOYMENT', 'HIGH', 50, TRUE, 'PATTERN_MATCH', 54),
('UNVERIFIABLE_SELF_EMPLOYED', 'Unverifiable Self-Employed', 'Self-employed without proper business registration', 'EMPLOYMENT', 'HIGH', 40, TRUE, 'CROSS_CHECK', 55);

-- CROSS-VERIFICATION RULES
INSERT INTO fraud_rule_definition (rule_code, rule_name, rule_description, rule_category, severity, fraud_points, is_active, rule_type, execution_order) VALUES
('NAME_CROSS_VERIFICATION_FAILED', 'Name Cross-Verification Failed', 'Name mismatch across Form, Aadhaar, PAN, Bank', 'CROSS_VERIFICATION', 'HIGH', 50, TRUE, 'CROSS_CHECK', 60),
('INCOME_CROSS_VERIFICATION_FAILED', 'Income Cross-Verification Failed', 'Income mismatch across Form, Payslip, Bank, ITR', 'CROSS_VERIFICATION', 'HIGH', 55, TRUE, 'CROSS_CHECK', 61),
('HIDDEN_LOANS_DETECTED', 'Hidden Loans Detected', 'Declared no loans but EMI transactions found in bank statement', 'CROSS_VERIFICATION', 'HIGH', 50, TRUE, 'CROSS_CHECK', 62),
('EMPLOYER_CROSS_VERIFICATION_FAILED', 'Employer Cross-Verification Failed', 'Employer name mismatch across sources', 'CROSS_VERIFICATION', 'HIGH', 45, TRUE, 'CROSS_CHECK', 63);

-- =====================================================
-- Insert Rule Parameters
-- =====================================================

-- MINOR_APPLICANT parameters
INSERT INTO fraud_rule_parameter (rule_id, param_name, param_value, param_type, param_description, is_configurable, min_value, max_value, default_value)
SELECT rule_id, 'MIN_AGE', '18', 'INTEGER', 'Minimum age requirement for loan applicant', TRUE, '18', '25', '18'
FROM fraud_rule_definition WHERE rule_code = 'MINOR_APPLICANT';

-- SUSPICIOUS_AGE_HIGH parameters
INSERT INTO fraud_rule_parameter (rule_id, param_name, param_value, param_type, param_description, is_configurable, min_value, max_value, default_value)
SELECT rule_id, 'MAX_AGE', '80', 'INTEGER', 'Maximum age threshold for flagging', TRUE, '70', '90', '80'
FROM fraud_rule_definition WHERE rule_code = 'SUSPICIOUS_AGE_HIGH';

-- HIGH_LOAN_TO_INCOME_RATIO parameters
INSERT INTO fraud_rule_parameter (rule_id, param_name, param_value, param_type, param_description, is_configurable, min_value, max_value, default_value)
SELECT rule_id, 'MAX_LTI_RATIO', '20', 'DECIMAL', 'Maximum loan-to-income ratio (loan amount / annual income)', TRUE, '10', '30', '20'
FROM fraud_rule_definition WHERE rule_code = 'HIGH_LOAN_TO_INCOME_RATIO';

-- HIGH_DEBT_TO_INCOME_RATIO parameters
INSERT INTO fraud_rule_parameter (rule_id, param_name, param_value, param_type, param_description, is_configurable, min_value, max_value, default_value)
SELECT rule_id, 'MAX_DTI_RATIO', '50', 'PERCENTAGE', 'Maximum debt-to-income ratio percentage', TRUE, '40', '70', '50'
FROM fraud_rule_definition WHERE rule_code = 'HIGH_DEBT_TO_INCOME_RATIO';

-- SALARY_MISMATCH parameters
INSERT INTO fraud_rule_parameter (rule_id, param_name, param_value, param_type, param_description, is_configurable, min_value, max_value, default_value)
SELECT rule_id, 'VARIANCE_THRESHOLD', '30', 'PERCENTAGE', 'Acceptable variance percentage between declared and actual salary', TRUE, '20', '50', '30'
FROM fraud_rule_definition WHERE rule_code = 'SALARY_MISMATCH';

-- EXCESSIVE_CREDIT_UTILIZATION parameters
INSERT INTO fraud_rule_parameter (rule_id, param_name, param_value, param_type, param_description, is_configurable, min_value, max_value, default_value)
SELECT rule_id, 'MAX_UTILIZATION', '80', 'PERCENTAGE', 'Maximum credit card utilization percentage', TRUE, '70', '95', '80'
FROM fraud_rule_definition WHERE rule_code = 'EXCESSIVE_CREDIT_UTILIZATION';

-- EXCESSIVE_ACTIVE_LOANS parameters
INSERT INTO fraud_rule_parameter (rule_id, param_name, param_value, param_type, param_description, is_configurable, min_value, max_value, default_value)
SELECT rule_id, 'MAX_ACTIVE_LOANS', '5', 'INTEGER', 'Maximum number of active loans before flagging', TRUE, '3', '10', '5'
FROM fraud_rule_definition WHERE rule_code = 'EXCESSIVE_ACTIVE_LOANS';

-- =====================================================
-- Insert Rule Patterns
-- =====================================================

-- PAN format pattern
INSERT INTO fraud_rule_pattern (rule_id, pattern_type, pattern_value, pattern_description, is_active)
SELECT rule_id, 'REGEX', '[A-Z]{5}[0-9]{4}[A-Z]{1}', 'Valid PAN format pattern', TRUE
FROM fraud_rule_definition WHERE rule_code = 'INVALID_PAN_FORMAT';

-- Aadhaar format pattern
INSERT INTO fraud_rule_pattern (rule_id, pattern_type, pattern_value, pattern_description, is_active)
SELECT rule_id, 'REGEX', '^[0-9]{12}$', 'Valid Aadhaar format pattern', TRUE
FROM fraud_rule_definition WHERE rule_code = 'INVALID_AADHAAR_FORMAT';

-- Shell company keywords
INSERT INTO fraud_rule_pattern (rule_id, pattern_type, pattern_value, pattern_description, is_active)
SELECT rule_id, 'KEYWORD', keyword, 'Shell company indicator keyword', TRUE
FROM fraud_rule_definition, 
(SELECT 'consultancy' AS keyword UNION ALL
 SELECT 'services pvt ltd' UNION ALL
 SELECT 'solutions pvt ltd' UNION ALL
 SELECT 'enterprises' UNION ALL
 SELECT 'trading' UNION ALL
 SELECT 'exports' UNION ALL
 SELECT 'imports' UNION ALL
 SELECT 'ventures' UNION ALL
 SELECT 'holdings') AS keywords
WHERE rule_code = 'SHELL_COMPANY_EMPLOYER';

-- Fake payslip template keywords
INSERT INTO fraud_rule_pattern (rule_id, pattern_type, pattern_value, pattern_description, is_active)
SELECT rule_id, 'KEYWORD', keyword, 'Fake payslip indicator', TRUE
FROM fraud_rule_definition,
(SELECT 'template' AS keyword UNION ALL
 SELECT 'sample' UNION ALL
 SELECT 'dummy' UNION ALL
 SELECT 'example') AS keywords
WHERE rule_code = 'FAKE_PAYSLIP_TEMPLATE';

-- =====================================================
-- Insert Rule Lists
-- =====================================================

-- Invalid email domains (personal email providers)
INSERT INTO fraud_rule_list (rule_id, list_type, list_value, list_description, is_active)
SELECT rule_id, 'BLACKLIST', domain, 'Personal email domain - not corporate', TRUE
FROM fraud_rule_definition,
(SELECT 'gmail.com' AS domain UNION ALL
 SELECT 'yahoo.com' UNION ALL
 SELECT 'hotmail.com' UNION ALL
 SELECT 'outlook.com' UNION ALL
 SELECT 'rediffmail.com' UNION ALL
 SELECT 'ymail.com' UNION ALL
 SELECT 'aol.com' UNION ALL
 SELECT 'mail.com' UNION ALL
 SELECT 'protonmail.com' UNION ALL
 SELECT 'zoho.com' UNION ALL
 SELECT 'icloud.com') AS domains
WHERE rule_code = 'FAKE_EMPLOYER_EMAIL';

-- Verified companies (whitelist)
INSERT INTO fraud_rule_list (rule_id, list_type, list_value, list_description, is_active)
SELECT rule_id, 'WHITELIST', company, 'Verified legitimate company', TRUE
FROM fraud_rule_definition,
(SELECT 'tcs' AS company UNION ALL
 SELECT 'infosys' UNION ALL
 SELECT 'wipro' UNION ALL
 SELECT 'cognizant' UNION ALL
 SELECT 'hcl' UNION ALL
 SELECT 'tech mahindra' UNION ALL
 SELECT 'accenture' UNION ALL
 SELECT 'ibm' UNION ALL
 SELECT 'microsoft' UNION ALL
 SELECT 'google' UNION ALL
 SELECT 'amazon' UNION ALL
 SELECT 'flipkart' UNION ALL
 SELECT 'hdfc' UNION ALL
 SELECT 'icici' UNION ALL
 SELECT 'sbi' UNION ALL
 SELECT 'axis' UNION ALL
 SELECT 'kotak' UNION ALL
 SELECT 'reliance' UNION ALL
 SELECT 'tata') AS companies
WHERE rule_code = 'UNVERIFIED_EMPLOYER';

-- =====================================================
-- Create Views for Easy Querying
-- =====================================================

CREATE OR REPLACE VIEW vw_active_rules AS
SELECT 
    r.rule_id,
    r.rule_code,
    r.rule_name,
    r.rule_category,
    r.severity,
    r.fraud_points,
    r.rule_type,
    r.execution_order,
    COUNT(DISTINCT p.param_id) AS parameter_count,
    COUNT(DISTINCT pt.pattern_id) AS pattern_count,
    COUNT(DISTINCT l.list_id) AS list_item_count
FROM fraud_rule_definition r
LEFT JOIN fraud_rule_parameter p ON r.rule_id = p.rule_id
LEFT JOIN fraud_rule_pattern pt ON r.rule_id = pt.rule_id AND pt.is_active = TRUE
LEFT JOIN fraud_rule_list l ON r.rule_id = l.rule_id AND l.is_active = TRUE
WHERE r.is_active = TRUE
GROUP BY r.rule_id, r.rule_code, r.rule_name, r.rule_category, r.severity, r.fraud_points, r.rule_type, r.execution_order
ORDER BY r.execution_order;

CREATE OR REPLACE VIEW vw_rule_performance AS
SELECT 
    r.rule_code,
    r.rule_name,
    r.rule_category,
    r.severity,
    r.fraud_points,
    m.date,
    m.total_executions,
    m.times_triggered,
    m.trigger_rate,
    m.avg_execution_time_ms,
    m.false_positive_count,
    m.true_positive_count
FROM fraud_rule_definition r
INNER JOIN fraud_rule_metrics m ON r.rule_id = m.rule_id
WHERE r.is_active = TRUE
ORDER BY m.date DESC, m.times_triggered DESC;

-- =====================================================
-- Indexes for Performance
-- =====================================================

CREATE INDEX idx_rule_execution_log_date ON fraud_rule_execution_log(executed_at);
CREATE INDEX idx_rule_execution_log_applicant_date ON fraud_rule_execution_log(applicant_id, executed_at);
CREATE INDEX idx_fraud_rule_audit_date ON fraud_rule_audit(changed_at);

-- =====================================================
-- Comments
-- =====================================================

COMMENT ON TABLE fraud_rule_definition IS 'Master table for all fraud detection rules';
COMMENT ON TABLE fraud_rule_parameter IS 'Configurable parameters for rules (thresholds, limits)';
COMMENT ON TABLE fraud_rule_pattern IS 'Regex patterns and keywords used by rules';
COMMENT ON TABLE fraud_rule_list IS 'Whitelist/blacklist data for rules';
COMMENT ON TABLE fraud_rule_audit IS 'Audit trail of all rule changes';
COMMENT ON TABLE fraud_rule_execution_log IS 'Log of every rule execution';
COMMENT ON TABLE fraud_rule_metrics IS 'Daily aggregated performance metrics';
