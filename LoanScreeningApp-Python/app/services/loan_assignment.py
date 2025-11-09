from sqlalchemy.orm import Session
from sqlalchemy import func
from datetime import datetime
from app.models.loan import ApplicantLoanDetails
from app.models.officer import LoanOfficer
from app.models.assignment import OfficerApplicationAssignment
from app.core.email import send_loan_assignment_email
import logging

logger = logging.getLogger(__name__)


class LoanAssignmentService:
    def __init__(self, db: Session):
        self.db = db
    
    def assign_loan_to_officer(self, loan_id: int, officer_id: int = None) -> OfficerApplicationAssignment:
        """Assign a loan to a loan officer"""
        loan = self.db.query(ApplicantLoanDetails).filter(ApplicantLoanDetails.loan_id == loan_id).first()
        if not loan:
            raise ValueError(f"Loan not found with ID: {loan_id}")
        
        # Check if already assigned
        existing = self.db.query(OfficerApplicationAssignment).filter(
            OfficerApplicationAssignment.loan_id == loan_id,
            OfficerApplicationAssignment.status == "PENDING"
        ).first()
        
        if existing:
            raise ValueError("Loan is already assigned to an officer")
        
        # Auto-select officer if not provided
        if not officer_id:
            officer_id = self.auto_select_best_officer(loan.loan_type)
        
        officer = self.db.query(LoanOfficer).filter(LoanOfficer.officer_id == officer_id).first()
        if not officer:
            raise ValueError(f"Officer not found with ID: {officer_id}")
        
        # Validate officer handles this loan type
        if officer.loan_type and officer.loan_type.upper() != loan.loan_type.upper():
            raise ValueError(f"Officer category ({officer.loan_type}) does not match loan type ({loan.loan_type})")
        
        # Create assignment
        assignment = OfficerApplicationAssignment(
            officer_id=officer_id,
            applicant_id=loan.applicant_id,
            loan_id=loan_id,
            status="PENDING",
            priority="MEDIUM"
        )
        self.db.add(assignment)
        self.db.commit()
        self.db.refresh(assignment)
        
        # Send email notification
        applicant = loan.applicant
        try:
            send_loan_assignment_email(
                officer.email,
                f"{officer.first_name} {officer.last_name}",
                f"{applicant.first_name} {applicant.last_name}",
                loan.loan_type,
                float(loan.loan_amount)
            )
        except Exception as e:
            logger.error(f"Failed to send assignment email: {e}")
        
        return assignment
    
    def auto_select_best_officer(self, loan_type: str) -> int:
        """Auto-select the best available officer based on loan type and workload"""
        # Get officers by loan type, ordered by workload
        officers = self.db.query(LoanOfficer).filter(
            LoanOfficer.loan_type == loan_type
        ).all()
        
        if not officers:
            # Try to find any officer if none match the loan type exactly
            officers = self.db.query(LoanOfficer).all()
        
        if not officers:
            raise ValueError(f"No officers available for loan type: {loan_type}")
        
        # Calculate workload for each officer
        officer_workloads = []
        for officer in officers:
            workload = self.db.query(OfficerApplicationAssignment).filter(
                OfficerApplicationAssignment.officer_id == officer.officer_id,
                OfficerApplicationAssignment.status.in_(["PENDING", "IN_PROGRESS"])
            ).count()
            officer_workloads.append((officer.officer_id, workload))
        
        # Select officer with least workload
        officer_workloads.sort(key=lambda x: x[1])
        return officer_workloads[0][0]


