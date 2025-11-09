from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel, EmailStr
from typing import List, Optional
from app.database import get_db
from app.core.dependencies import require_role
from app.core.email import send_approval_email
from app.models.applicant import Applicant
from app.models.officer import LoanOfficer, ComplianceOfficer
from app.core.security import get_password_hash

router = APIRouter()


class ApprovalResponse(BaseModel):
    applicant_id: int
    username: str
    email: str
    first_name: str
    last_name: str
    approval_status: str


class AddOfficerRequest(BaseModel):
    username: str
    email: EmailStr
    password: str
    first_name: str
    last_name: str
    loan_type: Optional[str] = None


@router.get("/applicants/pending")
async def get_pending_applicants(
    current_user: dict = Depends(require_role(["ADMIN"])),
    db: Session = Depends(get_db)
):
    """Get all pending applicant approvals"""
    applicants = db.query(Applicant).filter(
        Applicant.is_email_verified == True,
        Applicant.is_approved == False,
        Applicant.approval_status == "PENDING"
    ).all()
    
    return [
        ApprovalResponse(
            applicant_id=a.applicant_id,
            username=a.username,
            email=a.email,
            first_name=a.first_name,
            last_name=a.last_name,
            approval_status=a.approval_status
        )
        for a in applicants
    ]


@router.put("/applicants/{applicant_id}/approve")
async def approve_applicant(
    applicant_id: int,
    current_user: dict = Depends(require_role(["ADMIN"])),
    db: Session = Depends(get_db)
):
    """Approve an applicant registration"""
    applicant = db.query(Applicant).filter(Applicant.applicant_id == applicant_id).first()
    if not applicant:
        raise HTTPException(status_code=404, detail="Applicant not found")
    
    if not applicant.is_email_verified:
        raise HTTPException(status_code=400, detail="Cannot approve applicant with unverified email")
    
    applicant.is_approved = True
    applicant.approval_status = "APPROVED"
    db.commit()
    
    # Send approval email
    await send_approval_email(
        applicant.email,
        f"{applicant.first_name} {applicant.last_name}",
        True
    )
    
    return {"message": "Applicant approved successfully"}


@router.put("/applicants/{applicant_id}/reject")
async def reject_applicant(
    applicant_id: int,
    current_user: dict = Depends(require_role(["ADMIN"])),
    db: Session = Depends(get_db)
):
    """Reject an applicant registration"""
    applicant = db.query(Applicant).filter(Applicant.applicant_id == applicant_id).first()
    if not applicant:
        raise HTTPException(status_code=404, detail="Applicant not found")
    
    applicant.is_approved = False
    applicant.approval_status = "REJECTED"
    db.commit()
    
    # Send rejection email
    await send_approval_email(
        applicant.email,
        f"{applicant.first_name} {applicant.last_name}",
        False
    )
    
    return {"message": "Applicant rejected successfully"}


@router.post("/loan-officers")
async def add_loan_officer(
    request: AddOfficerRequest,
    current_user: dict = Depends(require_role(["ADMIN"])),
    db: Session = Depends(get_db)
):
    """Add a new loan officer"""
    if db.query(LoanOfficer).filter(LoanOfficer.username == request.username).first():
        raise HTTPException(status_code=400, detail="Username already taken")
    
    if db.query(LoanOfficer).filter(LoanOfficer.email == request.email).first():
        raise HTTPException(status_code=400, detail="Email already registered")
    
    officer = LoanOfficer(
        username=request.username,
        email=request.email,
        password_hash=get_password_hash(request.password),
        first_name=request.first_name,
        last_name=request.last_name,
        loan_type=request.loan_type
    )
    db.add(officer)
    db.commit()
    db.refresh(officer)
    
    return {"message": "Loan officer added successfully", "officer_id": officer.officer_id}


@router.post("/compliance-officers")
async def add_compliance_officer(
    request: AddOfficerRequest,
    current_user: dict = Depends(require_role(["ADMIN"])),
    db: Session = Depends(get_db)
):
    """Add a new compliance officer"""
    if db.query(ComplianceOfficer).filter(ComplianceOfficer.username == request.username).first():
        raise HTTPException(status_code=400, detail="Username already taken")
    
    if db.query(ComplianceOfficer).filter(ComplianceOfficer.email == request.email).first():
        raise HTTPException(status_code=400, detail="Email already registered")
    
    officer = ComplianceOfficer(
        username=request.username,
        email=request.email,
        password_hash=get_password_hash(request.password),
        first_name=request.first_name,
        last_name=request.last_name,
        loan_type=request.loan_type
    )
    db.add(officer)
    db.commit()
    db.refresh(officer)
    
    return {"message": "Compliance officer added successfully", "officer_id": officer.officer_id}


@router.get("/dashboard/stats")
async def get_dashboard_stats(
    current_user: dict = Depends(require_role(["ADMIN"])),
    db: Session = Depends(get_db)
):
    """Get dashboard statistics"""
    total_applicants = db.query(Applicant).count()
    pending_applicants = db.query(Applicant).filter(Applicant.approval_status == "PENDING").count()
    approved_applicants = db.query(Applicant).filter(Applicant.approval_status == "APPROVED").count()
    total_loans = db.query(Applicant).join(Applicant.loan_details).count()
    
    return {
        "total_applicants": total_applicants,
        "pending_applicants": pending_applicants,
        "approved_applicants": approved_applicants,
        "total_loans": total_loans
    }


