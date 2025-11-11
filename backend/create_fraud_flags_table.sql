-- =====================================================
-- Create Fraud Flags Table
-- Run this SQL script in your MySQL database
-- =====================================================

-- Drop table if exists (optional - use with caution)
-- DROP TABLE IF EXISTS fraud_flags;

-- Create fraud_flags table
CREATE TABLE IF NOT EXISTS fraud_flags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    applicant_id BIGINT NOT NULL,
    loan_id BIGINT NULL COMMENT 'Nullable - fraud can be detected without a loan',
    rule_name VARCHAR(100),
    severity INT COMMENT '1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL',
    flag_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_fraud_flag_applicant 
        FOREIGN KEY (applicant_id) 
        REFERENCES applicants(applicant_id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_fraud_flag_loan 
        FOREIGN KEY (loan_id) 
        REFERENCES applicant_loan_details(loan_id) 
        ON DELETE SET NULL,
    
    -- Indexes for performance
    INDEX idx_fraud_flag_applicant_id (applicant_id),
    INDEX idx_fraud_flag_loan_id (loan_id),
    INDEX idx_fraud_flag_severity (severity),
    INDEX idx_fraud_flag_created_at (created_at),
    INDEX idx_fraud_flag_rule_name (rule_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores fraud detection flags for applicants and loans';

-- Verify table creation
SELECT 'fraud_flags table created successfully' AS status;
