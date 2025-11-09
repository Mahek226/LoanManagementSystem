from sqlalchemy.orm import Session
from datetime import datetime
from app.models.assignment import OfficerApplicationAssignment, ComplianceOfficerApplicationAssignment
from app.models.loan import ApplicantLoanDetails
from app.models.officer import ComplianceOfficer
from app.core.email import send_loan_status_email
from app.config import settings
import logging

logger = logging.getLogger(__name__)


class LoanScreeningService:
    def __init__(self, db: Session):
        self.db = db
    
    def process_screening(
        self,
        officer_id: int,
        assignment_id: int,
        action: str,
        remarks: str = None,
        rejection_reason: str = None
    ) -> dict:
        """Process loan screening decision"""
        assignment = self.db.query(OfficerApplicationAssignment).filter(
            OfficerApplicationAssignment.assignment_id == assignment_id,
            OfficerApplicationAssignment.officer_id == officer_id
        ).first()
        
        if not assignment:
            raise ValueError("Assignment not found")
        
        loan = self.db.query(ApplicantLoanDetails).filter(
            ApplicantLoanDetails.loan_id == assignment.loan_id
        ).first()
        
        if not loan:
            raise ValueError("Loan not found")
        
        # Check if officer can approve/reject based on risk score
        can_approve_reject = loan.risk_score < settings.RISK_SCORE_THRESHOLD
        
        action_upper = action.upper()
        
        if action_upper == "APPROVE":
            if not can_approve_reject:
                raise ValueError(f"Cannot approve loan with high risk score ({loan.risk_score}). Must escalate to compliance.")
            
            loan.status = "approved"
            loan.approved_by = f"Officer {officer_id}"
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
            if not can_approve_reject:
                raise ValueError(f"Cannot reject loan with high risk score ({loan.risk_score}). Must escalate to compliance.")
            
            loan.status = "rejected"
            loan.rejected_by = f"Officer {officer_id}"
            loan.rejection_date = datetime.utcnow()
            loan.rejection_reason = rejection_reason
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
                rejection_reason
            )
            
        elif action_upper == "ESCALATE_TO_COMPLIANCE":
            # Escalate to compliance officer
            compliance_officer = self.db.query(ComplianceOfficer).first()
            if not compliance_officer:
                raise ValueError("No compliance officers available")
            
            # Create compliance assignment
            compliance_assignment = ComplianceOfficerApplicationAssignment(
                compliance_officer_id=compliance_officer.officer_id,
                applicant_id=assignment.applicant_id,
                loan_id=loan.loan_id,
                status="PENDING",
                priority="HIGH",
                remarks=f"Escalated from loan officer: {remarks or 'High risk score'}"
            )
            self.db.add(compliance_assignment)
            
            assignment.status = "ESCALATED_TO_COMPLIANCE"
            assignment.remarks = remarks
            
        else:
            raise ValueError(f"Invalid action: {action}")
        
        self.db.commit()
        
        return {
            "success": True,
            "message": f"Loan {action.lower()}d successfully",
            "loan_id": loan.loan_id,
            "status": loan.status
        }

