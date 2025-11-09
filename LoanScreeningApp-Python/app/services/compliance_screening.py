from sqlalchemy.orm import Session
from datetime import datetime
from app.models.assignment import ComplianceOfficerApplicationAssignment
from app.models.loan import ApplicantLoanDetails
from app.core.email import send_loan_status_email
import logging

logger = logging.getLogger(__name__)


class ComplianceScreeningService:
    def __init__(self, db: Session):
        self.db = db
    
    def process_compliance_review(
        self,
        officer_id: int,
        assignment_id: int,
        action: str,
        remarks: str = None,
        recommendation: str = None
    ) -> dict:
        """Process compliance review"""
        assignment = self.db.query(ComplianceOfficerApplicationAssignment).filter(
            ComplianceOfficerApplicationAssignment.assignment_id == assignment_id,
            ComplianceOfficerApplicationAssignment.compliance_officer_id == officer_id
        ).first()
        
        if not assignment:
            raise ValueError("Compliance assignment not found")
        
        loan = self.db.query(ApplicantLoanDetails).filter(
            ApplicantLoanDetails.loan_id == assignment.loan_id
        ).first()
        
        if not loan:
            raise ValueError("Loan not found")
        
        action_upper = action.upper()
        
        if action_upper == "APPROVE":
            loan.status = "approved"
            loan.approved_by = f"Compliance Officer {officer_id}"
            loan.approval_date = datetime.utcnow()
            assignment.status = "COMPLETED"
            assignment.processed_at = datetime.utcnow()
            assignment.completed_at = datetime.utcnow()
            
            # Send email notification
            applicant = assignment.applicant
            send_loan_status_email(
                applicant.email,
                f"{applicant.first_name} {applicant.last_name}",
                "APPROVED",
                loan.loan_type,
                remarks
            )
            
        elif action_upper == "REJECT":
            loan.status = "rejected"
            loan.rejected_by = f"Compliance Officer {officer_id}"
            loan.rejection_date = datetime.utcnow()
            loan.rejection_reason = remarks
            assignment.status = "COMPLETED"
            assignment.processed_at = datetime.utcnow()
            assignment.completed_at = datetime.utcnow()
            
            # Send email notification
            applicant = assignment.applicant
            send_loan_status_email(
                applicant.email,
                f"{applicant.first_name} {applicant.last_name}",
                "REJECTED",
                loan.loan_type,
                remarks
            )
            
        elif action_upper == "REQUEST_RESUBMISSION":
            loan.status = "under_review"
            assignment.status = "IN_PROGRESS"
            assignment.remarks = remarks
            
        else:
            raise ValueError(f"Invalid action: {action}")
        
        self.db.commit()
        
        return {
            "success": True,
            "message": f"Compliance review {action.lower()}d",
            "loan_id": loan.loan_id,
            "status": loan.status
        }


