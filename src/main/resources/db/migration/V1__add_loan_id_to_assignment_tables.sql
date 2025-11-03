-- Migration script to add loan_id columns to assignment tables
-- This script adds foreign key relationships to ApplicantLoanDetails table

-- Add loan_id column to officer_application_assignment table
ALTER TABLE officer_application_assignment 
ADD COLUMN loan_id BIGINT;

-- Add foreign key constraint for officer_application_assignment
ALTER TABLE officer_application_assignment 
ADD CONSTRAINT fk_officer_assignment_loan 
FOREIGN KEY (loan_id) REFERENCES applicant_loan_details(loan_id);

-- Add loan_id column to compliance_officer_application_assignment table
ALTER TABLE compliance_officer_application_assignment 
ADD COLUMN loan_id BIGINT;

-- Add foreign key constraint for compliance_officer_application_assignment
ALTER TABLE compliance_officer_application_assignment 
ADD CONSTRAINT fk_compliance_assignment_loan 
FOREIGN KEY (loan_id) REFERENCES applicant_loan_details(loan_id);

-- Create indexes for better query performance
CREATE INDEX idx_officer_assignment_loan_id ON officer_application_assignment(loan_id);
CREATE INDEX idx_compliance_assignment_loan_id ON compliance_officer_application_assignment(loan_id);

-- Update existing records to populate loan_id based on applicant_id
-- This assumes one active loan per applicant (most recent loan)
UPDATE officer_application_assignment oa
SET loan_id = (
    SELECT ald.loan_id 
    FROM applicant_loan_details ald 
    WHERE ald.applicant_id = oa.applicant_id 
    ORDER BY ald.loan_id DESC 
    LIMIT 1
)
WHERE oa.loan_id IS NULL 
AND EXISTS (
    SELECT 1 FROM applicant_loan_details ald 
    WHERE ald.applicant_id = oa.applicant_id
);

UPDATE compliance_officer_application_assignment coa
SET loan_id = (
    SELECT ald.loan_id 
    FROM applicant_loan_details ald 
    WHERE ald.applicant_id = coa.applicant_id 
    ORDER BY ald.loan_id DESC 
    LIMIT 1
)
WHERE coa.loan_id IS NULL 
AND EXISTS (
    SELECT 1 FROM applicant_loan_details ald 
    WHERE ald.applicant_id = coa.applicant_id
);

-- Clean up assignments that don't have valid loan associations
-- These are orphaned records that should be removed or handled separately
DELETE FROM officer_application_assignment 
WHERE loan_id IS NULL OR loan_id = 0;

DELETE FROM compliance_officer_application_assignment 
WHERE loan_id IS NULL OR loan_id = 0;

-- Make loan_id NOT NULL after populating existing records
ALTER TABLE officer_application_assignment 
MODIFY COLUMN loan_id BIGINT NOT NULL;

ALTER TABLE compliance_officer_application_assignment 
MODIFY COLUMN loan_id BIGINT NOT NULL;
