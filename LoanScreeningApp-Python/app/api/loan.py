from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import Optional
from decimal import Decimal
from datetime import datetime
from app.database import get_db
from app.core.dependencies import require_role
from app.models.applicant import Applicant
from app.models.loan import ApplicantLoanDetails
from app.models.assignment import OfficerApplicationAssignment
from app.services.loan_assignment import LoanAssignmentService
from app.services.enhanced_screening import EnhancedLoanScreeningService

router = APIRouter()


class LoanApplicationRequest(BaseModel):
    loan_type: str
    loan_amount: float
    tenure_months: int
    loan_purpose: Optional[str] = None


@router.post("/applications/submit")
async def submit_loan_application(
    request: LoanApplicationRequest,
    current_user: dict = Depends(require_role(["APPLICANT"])),
    db: Session = Depends(get_db)
):
    """Submit a new loan application"""
    applicant = current_user["user"]
    
    # Create loan details
    loan = ApplicantLoanDetails(
        applicant_id=applicant.applicant_id,
        loan_type=request.loan_type,
        loan_amount=Decimal(str(request.loan_amount)),
        tenure_months=request.tenure_months,
        loan_purpose=request.loan_purpose,
        status="pending",
        risk_score=0,
        application_date=datetime.utcnow()
    )
    db.add(loan)
    db.commit()
    db.refresh(loan)
    
    # Auto-assign to loan officer
    assignment_service = LoanAssignmentService(db)
    try:
        assignment = assignment_service.assign_loan_to_officer(loan.loan_id)
        return {
            "success": True,
            "message": "Loan application submitted successfully",
            "loan_id": loan.loan_id,
            "assignment_id": assignment.assignment_id
        }
    except Exception as e:
        # Loan created but assignment failed
        return {
            "success": True,
            "message": "Loan application submitted, but assignment pending",
            "loan_id": loan.loan_id,
            "warning": str(e)
        }


@router.get("/applications/{loan_id}")
async def get_loan_application(
    loan_id: int,
    current_user: dict = Depends(require_role(["APPLICANT", "ADMIN", "LOAN_OFFICER", "COMPLIANCE_OFFICER"])),
    db: Session = Depends(get_db)
):
    """Get loan application details"""
    loan = db.query(ApplicantLoanDetails).filter(ApplicantLoanDetails.loan_id == loan_id).first()
    if not loan:
        raise HTTPException(status_code=404, detail="Loan not found")
    
    # Check access permissions
    user = current_user["user"]
    role = current_user["role"]
    
    if role == "APPLICANT" and loan.applicant_id != user.applicant_id:
        raise HTTPException(status_code=403, detail="Access denied")
    
    return {
        "loan_id": loan.loan_id,
        "loan_type": loan.loan_type,
        "loan_amount": float(loan.loan_amount),
        "tenure_months": loan.tenure_months,
        "status": loan.status,
        "risk_score": loan.risk_score,
        "risk_level": loan.risk_level,
        "submitted_at": loan.submitted_at.isoformat() if loan.submitted_at else None
    }


@router.post("/applications/{loan_id}/screening")
async def run_loan_screening(
    loan_id: int,
    current_user: dict = Depends(require_role(["ADMIN", "LOAN_OFFICER", "COMPLIANCE_OFFICER"])),
    db: Session = Depends(get_db)
):
    """Run enhanced loan screening for a loan application"""
    loan = db.query(ApplicantLoanDetails).filter(ApplicantLoanDetails.loan_id == loan_id).first()
    if not loan:
        raise HTTPException(status_code=404, detail="Loan not found")
    
    screening_service = EnhancedLoanScreeningService(db)
    result = screening_service.perform_enhanced_screening(loan.applicant_id)
    
    return result

