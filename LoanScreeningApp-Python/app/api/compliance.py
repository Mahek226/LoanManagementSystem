from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
from app.database import get_db
from app.core.dependencies import require_role
from app.models.assignment import ComplianceOfficerApplicationAssignment
from app.models.loan import ApplicantLoanDetails
from app.services.compliance_screening import ComplianceScreeningService

router = APIRouter()


class ComplianceReviewRequest(BaseModel):
    assignment_id: int
    action: str  # APPROVE, REJECT, REQUEST_RESUBMISSION
    remarks: Optional[str] = None
    recommendation: Optional[str] = None


@router.get("/assignments")
async def get_compliance_assignments(
    current_user: dict = Depends(require_role(["COMPLIANCE_OFFICER"])),
    db: Session = Depends(get_db)
):
    """Get all compliance assignments"""
    officer = current_user["user"]
    assignments = db.query(ComplianceOfficerApplicationAssignment).filter(
        ComplianceOfficerApplicationAssignment.compliance_officer_id == officer.officer_id
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


@router.post("/review")
async def review_compliance(
    request: ComplianceReviewRequest,
    current_user: dict = Depends(require_role(["COMPLIANCE_OFFICER"])),
    db: Session = Depends(get_db)
):
    """Review and process compliance assignment"""
    officer = current_user["user"]
    screening_service = ComplianceScreeningService(db)
    
    try:
        result = screening_service.process_compliance_review(
            officer_id=officer.officer_id,
            assignment_id=request.assignment_id,
            action=request.action,
            remarks=request.remarks,
            recommendation=request.recommendation
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


