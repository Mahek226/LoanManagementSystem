-- Data cleanup script to fix invalid loan_id references
-- This script should run after the loan_id columns are added but before foreign key constraints

-- First, let's see what we're dealing with - identify invalid loan_id references
-- Update officer_application_assignment with valid loan_id where possible
UPDATE officer_application_assignment oa
SET loan_id = (
    SELECT ald.loan_id 
    FROM applicant_loan_details ald 
    WHERE ald.applicant_id = oa.applicant_id 
    ORDER BY ald.loan_id DESC 
    LIMIT 1
)
WHERE (oa.loan_id IS NULL OR oa.loan_id = 0 OR oa.loan_id NOT IN (SELECT loan_id FROM applicant_loan_details))
AND EXISTS (
    SELECT 1 FROM applicant_loan_details ald 
    WHERE ald.applicant_id = oa.applicant_id
);

-- Update compliance_officer_application_assignment with valid loan_id where possible
UPDATE compliance_officer_application_assignment coa
SET loan_id = (
    SELECT ald.loan_id 
    FROM applicant_loan_details ald 
    WHERE ald.applicant_id = coa.applicant_id 
    ORDER BY ald.loan_id DESC 
    LIMIT 1
)
WHERE (coa.loan_id IS NULL OR coa.loan_id = 0 OR coa.loan_id NOT IN (SELECT loan_id FROM applicant_loan_details))
AND EXISTS (
    SELECT 1 FROM applicant_loan_details ald 
    WHERE ald.applicant_id = coa.applicant_id
);

-- Set loan_id to NULL for assignments that can't be properly linked
-- This allows the application to use fallback logic
UPDATE officer_application_assignment 
SET loan_id = NULL
WHERE (loan_id = 0 OR loan_id NOT IN (SELECT loan_id FROM applicant_loan_details))
AND NOT EXISTS (
    SELECT 1 FROM applicant_loan_details ald 
    WHERE ald.applicant_id = officer_application_assignment.applicant_id
);

UPDATE compliance_officer_application_assignment 
SET loan_id = NULL
WHERE (loan_id = 0 OR loan_id NOT IN (SELECT loan_id FROM applicant_loan_details))
AND NOT EXISTS (
    SELECT 1 FROM applicant_loan_details ald 
    WHERE ald.applicant_id = compliance_officer_application_assignment.applicant_id
);

-- Optional: Remove orphaned assignments that have no valid applicant or loan reference
-- Uncomment these lines if you want to clean up completely orphaned records
-- DELETE FROM officer_application_assignment 
-- WHERE applicant_id NOT IN (SELECT applicant_id FROM applicant WHERE applicant_id IS NOT NULL);

-- DELETE FROM compliance_officer_application_assignment 
-- WHERE applicant_id NOT IN (SELECT applicant_id FROM applicant WHERE applicant_id IS NOT NULL);
