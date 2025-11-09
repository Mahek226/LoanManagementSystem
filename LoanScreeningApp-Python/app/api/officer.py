from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
from app.database import get_db
from app.core.dependencies import require_role
from app.models.assignment import OfficerApplicationAssignment
from app.models.loan import ApplicantLoanDetails
from app.models.assignment import ComplianceOfficerApplicationAssignment
from app.services.loan_screening import LoanScreeningService
from app.config import settings

router = APIRouter()


class ScreeningRequest(BaseModel):
    assignment_id: int
    action: str  # APPROVE, REJECT, ESCALATE_TO_COMPLIANCE
    remarks: Optional[str] = None
    rejection_reason: Optional[str] = None


@router.get("/assignments")
async def get_assignments(
    current_user: dict = Depends(require_role(["LOAN_OFFICER"])),
    db: Session = Depends(get_db)
):
    """Get all assignments for the current loan officer"""
    officer = current_user["user"]
    assignments = db.query(OfficerApplicationAssignment).filter(
        OfficerApplicationAssignment.officer_id == officer.officer_id
    ).all()
    
    return [
        {
            "assignment_id": a.assignment_id,
            "loan_id": a.loan_id,
            "applicant_id": a.applicant_id,
            "status": a.status,
            "priority": a.priority,
            "assigned_at": a.assigned_at.isoformat() if a.assigned_at else None
        }
        for a in assignments
    ]


@router.post("/screening/process")
async def process_screening(
    request: ScreeningRequest,
    current_user: dict = Depends(require_role(["LOAN_OFFICER"])),
    db: Session = Depends(get_db)
):
    """Process loan screening and make decision"""
    officer = current_user["user"]
    screening_service = LoanScreeningService(db)
    
    try:
        result = screening_service.process_screening(
            officer_id=officer.officer_id,
            assignment_id=request.assignment_id,
            action=request.action,
            remarks=request.remarks,
            rejection_reason=request.rejection_reason
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/assignments/{assignment_id}")
async def get_assignment_details(
    assignment_id: int,
    current_user: dict = Depends(require_role(["LOAN_OFFICER"])),
    db: Session = Depends(get_db)
):
    """Get detailed assignment information"""
    officer = current_user["user"]
    assignment = db.query(OfficerApplicationAssignment).filter(
        OfficerApplicationAssignment.assignment_id == assignment_id,
        OfficerApplicationAssignment.officer_id == officer.officer_id
    ).first()
    
    if not assignment:
        raise HTTPException(status_code=404, detail="Assignment not found")
    
    loan = db.query(ApplicantLoanDetails).filter(
        ApplicantLoanDetails.loan_id == assignment.loan_id
    ).first()
    
    return {
        "assignment_id": assignment.assignment_id,
        "loan": {
            "loan_id": loan.loan_id,
            "loan_type": loan.loan_type,
            "loan_amount": float(loan.loan_amount),
            "status": loan.status,
            "risk_score": loan.risk_score,
            "risk_level": loan.risk_level
        },
        "status": assignment.status,
        "priority": assignment.priority,
        "remarks": assignment.remarks
    }

