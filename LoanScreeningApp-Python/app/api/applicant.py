from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import List, Optional
from app.database import get_db
from app.core.dependencies import require_role
from app.models.applicant import Applicant
from app.models.loan import ApplicantLoanDetails
from app.models.notification import ApplicantNotification

router = APIRouter()


class LoanApplicationRequest(BaseModel):
    loan_type: str
    loan_amount: float
    tenure_months: int
    loan_purpose: Optional[str] = None


@router.get("/profile")
async def get_profile(
    current_user: dict = Depends(require_role(["APPLICANT"])),
    db: Session = Depends(get_db)
):
    """Get applicant profile"""
    applicant = current_user["user"]
    return {
        "applicant_id": applicant.applicant_id,
        "username": applicant.username,
        "email": applicant.email,
        "first_name": applicant.first_name,
        "last_name": applicant.last_name,
        "phone": applicant.phone,
        "approval_status": applicant.approval_status
    }


@router.get("/loans")
async def get_my_loans(
    current_user: dict = Depends(require_role(["APPLICANT"])),
    db: Session = Depends(get_db)
):
    """Get all loans for the current applicant"""
    applicant = current_user["user"]
    loans = db.query(ApplicantLoanDetails).filter(
        ApplicantLoanDetails.applicant_id == applicant.applicant_id
    ).all()
    
    return [
        {
            "loan_id": loan.loan_id,
            "loan_type": loan.loan_type,
            "loan_amount": float(loan.loan_amount),
            "status": loan.status,
            "risk_score": loan.risk_score,
            "submitted_at": loan.submitted_at.isoformat() if loan.submitted_at else None
        }
        for loan in loans
    ]


@router.get("/notifications")
async def get_notifications(
    current_user: dict = Depends(require_role(["APPLICANT"])),
    db: Session = Depends(get_db)
):
    """Get all notifications for the current applicant"""
    applicant = current_user["user"]
    notifications = db.query(ApplicantNotification).filter(
        ApplicantNotification.applicant_id == applicant.applicant_id
    ).order_by(ApplicantNotification.created_at.desc()).limit(50).all()
    
    return [
        {
            "notification_id": n.notification_id,
            "title": n.title,
            "message": n.message,
            "type": n.notification_type,
            "is_read": n.is_read,
            "created_at": n.created_at.isoformat() if n.created_at else None
        }
        for n in notifications
    ]


