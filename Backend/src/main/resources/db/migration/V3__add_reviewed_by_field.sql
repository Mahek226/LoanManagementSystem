-- Add reviewed_by field to applicant_loan_details table
ALTER TABLE applicant_loan_details 
ADD COLUMN reviewed_by VARCHAR(200) NULL COMMENT 'Name of the officer who reviewed the loan application';

-- Create index for better query performance
CREATE INDEX idx_applicant_loan_details_reviewed_by ON applicant_loan_details(reviewed_by);
