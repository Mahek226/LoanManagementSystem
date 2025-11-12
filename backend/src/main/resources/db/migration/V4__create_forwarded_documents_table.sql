-- Create forwarded_documents table for tracking documents forwarded from loan officers to compliance officers
CREATE TABLE forwarded_documents (
    forwarded_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    document_name VARCHAR(255),
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(255) NOT NULL,
    loan_id BIGINT NOT NULL,
    loan_type VARCHAR(50),
    loan_amount DECIMAL(15,2),
    forwarded_by_officer_id BIGINT NOT NULL,
    forwarded_to_compliance_officer_id BIGINT,
    forwarded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'FORWARDED',
    reason TEXT,
    priority_level INT DEFAULT 3,
    
    INDEX idx_forwarded_documents_status (status),
    INDEX idx_forwarded_documents_applicant (applicant_id),
    INDEX idx_forwarded_documents_loan (loan_id),
    INDEX idx_forwarded_documents_forwarded_by (forwarded_by_officer_id),
    INDEX idx_forwarded_documents_forwarded_to (forwarded_to_compliance_officer_id),
    INDEX idx_forwarded_documents_forwarded_at (forwarded_at)
);
