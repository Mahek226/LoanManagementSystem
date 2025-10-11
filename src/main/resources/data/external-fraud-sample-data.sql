-- External Fraud Database Sample Data
-- Use this script to populate the external fraud database with test data

USE lms;

-- Sample Persons Data
INSERT INTO persons (first_name, last_name, dob, gender, phone_number, email, marital_status, nationality, pan_number, aadhaar_number, created_at, updated_at) VALUES
('Amit', 'Patel', '1985-06-15', 'MALE', '9876543210', 'amit.patel@email.com', 'MARRIED', 'INDIAN', 'ABCDE1234F', '123456789012', NOW(), NOW()),
('Priya', 'Sharma', '1990-03-22', 'FEMALE', '9876543211', 'priya.sharma@email.com', 'SINGLE', 'INDIAN', 'FGHIJ5678K', '234567890123', NOW(), NOW()),
('Rajesh', 'Kumar', '1982-11-08', 'MALE', '9876543212', 'rajesh.kumar@email.com', 'MARRIED', 'INDIAN', 'KLMNO9012P', '345678901234', NOW(), NOW()),
('Sunita', 'Singh', '1988-09-14', 'FEMALE', '9876543213', 'sunita.singh@email.com', 'DIVORCED', 'INDIAN', 'PQRST3456U', '456789012345', NOW(), NOW()),
('Vikram', 'Gupta', '1975-12-03', 'MALE', '9876543214', 'vikram.gupta@email.com', 'MARRIED', 'INDIAN', 'UVWXY7890Z', '567890123456', NOW(), NOW());

-- Sample Government Issued Documents
INSERT INTO government_issued_documents (person_id, document_type, document_number, issued_date, expiry_date, issuing_authority, verification_status, created_at) VALUES
(1, 'PAN', 'ABCDE1234F', '2010-01-15', NULL, 'INCOME TAX DEPARTMENT', 'VERIFIED', NOW()),
(1, 'AADHAAR', '123456789012', '2012-03-20', NULL, 'UIDAI', 'VERIFIED', NOW()),
(1, 'PASSPORT', 'A1234567', '2018-05-10', '2028-05-09', 'PASSPORT OFFICE', 'VERIFIED', NOW()),
(2, 'PAN', 'FGHIJ5678K', '2012-06-20', NULL, 'INCOME TAX DEPARTMENT', 'VERIFIED', NOW()),
(2, 'AADHAAR', '234567890123', '2013-04-15', NULL, 'UIDAI', 'UNVERIFIED', NOW()),
(3, 'PAN', 'KLMNO9012P', '2008-03-10', NULL, 'INCOME TAX DEPARTMENT', 'VERIFIED', NOW()),
(3, 'AADHAAR', '345678901234', '2011-07-25', NULL, 'UIDAI', 'VERIFIED', NOW()),
(3, 'DRIVING_LICENSE', 'DL1234567890', '2015-01-20', '2025-01-19', 'RTO', 'EXPIRED', NOW()),
(4, 'PAN', 'PQRST3456U', '2015-08-12', NULL, 'INCOME TAX DEPARTMENT', 'VERIFIED', NOW()),
(4, 'AADHAAR', '456789012345', '2014-02-28', NULL, 'UIDAI', 'VERIFIED', NOW()),
(5, 'PAN', 'UVWXY7890Z', '2005-11-30', NULL, 'INCOME TAX DEPARTMENT', 'VERIFIED', NOW()),
(5, 'AADHAAR', '567890123456', '2010-09-18', NULL, 'UIDAI', 'VERIFIED', NOW());

-- Sample Historical and Current Loans (Including Defaults)
INSERT INTO historical_and_current_loans (person_id, loan_type, institution_name, loan_amount, outstanding_balance, start_date, end_date, status, default_flag, created_at) VALUES
-- Amit Patel (Person ID: 1) - Clean record
(1, 'HOME', 'HDFC Bank', 2500000.00, 1800000.00, '2020-01-15', NULL, 'ACTIVE', FALSE, NOW()),
(1, 'PERSONAL', 'ICICI Bank', 500000.00, 0.00, '2018-06-10', '2021-06-10', 'CLOSED', FALSE, NOW()),

-- Priya Sharma (Person ID: 2) - One default
(2, 'PERSONAL', 'SBI', 300000.00, 250000.00, '2021-03-20', NULL, 'ACTIVE', FALSE, NOW()),
(2, 'AUTO', 'Bajaj Finserv', 800000.00, 600000.00, '2019-08-15', NULL, 'DEFAULTED', TRUE, NOW()),

-- Rajesh Kumar (Person ID: 3) - Multiple defaults (High Risk)
(3, 'PERSONAL', 'HDFC Bank', 400000.00, 400000.00, '2020-05-10', NULL, 'DEFAULTED', TRUE, NOW()),
(3, 'BUSINESS', 'ICICI Bank', 1500000.00, 1200000.00, '2019-02-28', NULL, 'DEFAULTED', TRUE, NOW()),
(3, 'AUTO', 'SBI', 600000.00, 450000.00, '2021-07-12', NULL, 'DEFAULTED', TRUE, NOW()),
(3, 'PERSONAL', 'Axis Bank', 200000.00, 180000.00, '2022-01-05', NULL, 'ACTIVE', FALSE, NOW()),

-- Sunita Singh (Person ID: 4) - Multiple active loans
(4, 'PERSONAL', 'HDFC Bank', 350000.00, 200000.00, '2021-09-15', NULL, 'ACTIVE', FALSE, NOW()),
(4, 'HOME', 'SBI', 2000000.00, 1600000.00, '2020-11-20', NULL, 'ACTIVE', FALSE, NOW()),
(4, 'PERSONAL', 'ICICI Bank', 250000.00, 150000.00, '2022-03-10', NULL, 'ACTIVE', FALSE, NOW()),
(4, 'AUTO', 'Bajaj Finserv', 700000.00, 500000.00, '2021-12-05', NULL, 'ACTIVE', FALSE, NOW()),
(4, 'EDUCATION', 'SBI', 800000.00, 600000.00, '2019-06-30', NULL, 'ACTIVE', FALSE, NOW()),

-- Vikram Gupta (Person ID: 5) - Clean record with closed loans
(5, 'HOME', 'HDFC Bank', 3000000.00, 0.00, '2010-04-20', '2020-04-20', 'CLOSED', FALSE, NOW()),
(5, 'AUTO', 'ICICI Bank', 500000.00, 0.00, '2015-08-15', '2020-08-15', 'CLOSED', FALSE, NOW());

-- Sample Bank Records
INSERT INTO bank_records (person_id, bank_name, account_number, account_type, balance_amount, last_transaction_date, is_active, created_at) VALUES
-- Amit Patel
(1, 'HDFC Bank', '12345678901', 'SAVINGS', 150000.00, '2024-10-08', TRUE, NOW()),
(1, 'ICICI Bank', '23456789012', 'CURRENT', 75000.00, '2024-10-09', TRUE, NOW()),

-- Priya Sharma
(2, 'SBI', '34567890123', 'SAVINGS', 25000.00, '2024-10-07', TRUE, NOW()),
(2, 'Axis Bank', '45678901234', 'SAVINGS', 12000.00, '2024-09-25', FALSE, NOW()),

-- Rajesh Kumar (Multiple accounts - suspicious)
(3, 'HDFC Bank', '56789012345', 'SAVINGS', 5000.00, '2024-08-15', TRUE, NOW()),
(3, 'ICICI Bank', '67890123456', 'CURRENT', 2000.00, '2024-07-20', FALSE, NOW()),
(3, 'SBI', '78901234567', 'SAVINGS', 1500.00, '2024-06-10', FALSE, NOW()),
(3, 'Axis Bank', '89012345678', 'SAVINGS', 800.00, '2024-05-05', FALSE, NOW()),
(3, 'Kotak Bank', '90123456789', 'CURRENT', 500.00, '2024-04-01', FALSE, NOW()),

-- Sunita Singh
(4, 'SBI', '01234567890', 'SAVINGS', 85000.00, '2024-10-09', TRUE, NOW()),
(4, 'HDFC Bank', '12345098765', 'CURRENT', 45000.00, '2024-10-08', TRUE, NOW()),

-- Vikram Gupta
(5, 'HDFC Bank', '23456109876', 'SAVINGS', 250000.00, '2024-10-09', TRUE, NOW()),
(5, 'ICICI Bank', '34567210987', 'FIXED_DEPOSIT', 500000.00, '2024-09-30', TRUE, NOW());

-- Sample Criminal Records (High Risk Cases)
INSERT INTO criminal_records (person_id, case_number, case_type, description, court_name, status, verdict_date, created_at) VALUES
-- Rajesh Kumar - Criminal history
(3, 'CR/2019/1234', 'FINANCIAL_FRAUD', 'Cheque bounce case under Section 138 NI Act', 'Metropolitan Magistrate Court', 'CONVICTED', '2020-03-15', NOW()),
(3, 'CR/2021/5678', 'FORGERY', 'Document forgery case under IPC Section 420', 'Sessions Court', 'OPEN', NULL, NOW()),

-- Vikram Gupta - Old resolved case
(5, 'CR/2010/9876', 'TRAFFIC_VIOLATION', 'Rash driving case', 'Traffic Court', 'CLOSED', '2010-08-20', NOW());

-- Sample Addresses
INSERT INTO addresses (person_id, address_type, line1, line2, city, state, country, postal_code, verified, created_at) VALUES
(1, 'CURRENT', '123 MG Road', 'Near City Mall', 'Mumbai', 'Maharashtra', 'India', '400001', TRUE, NOW()),
(1, 'PERMANENT', '456 Gandhi Street', 'Sector 15', 'Ahmedabad', 'Gujarat', 'India', '380015', TRUE, NOW()),
(2, 'CURRENT', '789 Park Avenue', 'Block A', 'Delhi', 'Delhi', 'India', '110001', TRUE, NOW()),
(3, 'CURRENT', '321 Ring Road', 'Phase 2', 'Bangalore', 'Karnataka', 'India', '560001', FALSE, NOW()),
(4, 'CURRENT', '654 Lake View', 'Apartment 5B', 'Pune', 'Maharashtra', 'India', '411001', TRUE, NOW()),
(5, 'CURRENT', '987 Hill Station', 'Villa 12', 'Chennai', 'Tamil Nadu', 'India', '600001', TRUE, NOW());

-- Sample Education Records
INSERT INTO education (person_id, institution_name, degree, field_of_study, start_date, end_date, grade, verified, created_at) VALUES
(1, 'Mumbai University', 'Bachelor of Engineering', 'Computer Science', '2003-06-01', '2007-05-31', 'First Class', TRUE, NOW()),
(2, 'Delhi University', 'Master of Business Administration', 'Finance', '2012-07-01', '2014-06-30', 'Distinction', TRUE, NOW()),
(3, 'Bangalore University', 'Bachelor of Commerce', 'Accounting', '2000-06-01', '2003-05-31', 'Second Class', FALSE, NOW()),
(4, 'Pune University', 'Master of Computer Applications', 'Information Technology', '2008-07-01', '2011-06-30', 'First Class', TRUE, NOW()),
(5, 'Anna University', 'Bachelor of Technology', 'Mechanical Engineering', '1993-07-01', '1997-06-30', 'First Class', TRUE, NOW());

-- Sample Employment History
INSERT INTO employment_history (person_id, employer_name, job_title, employment_type, start_date, end_date, annual_income, verified, created_at) VALUES
(1, 'TCS', 'Senior Software Engineer', 'FULL_TIME', '2007-07-01', '2015-12-31', 800000.00, TRUE, NOW()),
(1, 'Infosys', 'Technical Lead', 'FULL_TIME', '2016-01-01', NULL, 1200000.00, TRUE, NOW()),
(2, 'HDFC Bank', 'Assistant Manager', 'FULL_TIME', '2014-08-01', '2020-03-31', 600000.00, TRUE, NOW()),
(2, 'ICICI Bank', 'Manager', 'FULL_TIME', '2020-04-01', NULL, 900000.00, TRUE, NOW()),
(3, 'Self Employed', 'Business Owner', 'SELF_EMPLOYED', '2010-01-01', NULL, 500000.00, FALSE, NOW()),
(4, 'Wipro', 'Project Manager', 'FULL_TIME', '2011-08-01', '2019-12-31', 1000000.00, TRUE, NOW()),
(4, 'Accenture', 'Senior Manager', 'FULL_TIME', '2020-01-01', NULL, 1400000.00, TRUE, NOW()),
(5, 'L&T', 'General Manager', 'FULL_TIME', '1997-08-01', '2020-12-31', 1800000.00, TRUE, NOW()),
(5, 'Consultant', 'Independent Consultant', 'CONTRACT', '2021-01-01', NULL, 2000000.00, TRUE, NOW());

-- Sample Assets
INSERT INTO assets (person_id, asset_type, description, estimated_value, ownership_type, acquired_date, verified, created_at) VALUES
(1, 'PROPERTY', '2BHK Apartment in Mumbai', 8000000.00, 'SOLE', '2020-01-15', TRUE, NOW()),
(1, 'VEHICLE', 'Honda City 2019', 800000.00, 'SOLE', '2019-03-20', TRUE, NOW()),
(2, 'PROPERTY', '1BHK Apartment in Delhi', 4500000.00, 'SOLE', '2021-06-10', TRUE, NOW()),
(3, 'VEHICLE', 'Maruti Swift 2018', 500000.00, 'SOLE', '2018-08-15', FALSE, NOW()),
(4, 'PROPERTY', '3BHK House in Pune', 12000000.00, 'JOINT', '2020-11-20', TRUE, NOW()),
(4, 'VEHICLE', 'Toyota Innova 2021', 1500000.00, 'SOLE', '2021-12-05', TRUE, NOW()),
(4, 'GOLD', 'Gold Jewelry 500 grams', 2500000.00, 'JOINT', '2019-06-30', TRUE, NOW()),
(5, 'PROPERTY', 'Villa in Chennai', 25000000.00, 'SOLE', '2010-04-20', TRUE, NOW()),
(5, 'VEHICLE', 'BMW X5 2022', 8000000.00, 'SOLE', '2022-01-15', TRUE, NOW()),
(5, 'INVESTMENT', 'Mutual Funds Portfolio', 5000000.00, 'SOLE', '2015-08-15', TRUE, NOW());

COMMIT;
