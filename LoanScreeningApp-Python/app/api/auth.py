from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPBearer
from sqlalchemy.orm import Session
from pydantic import BaseModel, EmailStr
from datetime import timedelta
from app.database import get_db
from app.core.security import verify_password, get_password_hash, create_access_token
from app.core.email import send_otp_email, send_approval_email
from app.utils.otp import create_otp_record, verify_otp
from app.models.applicant import Applicant
from app.models.admin import Admin
from app.models.officer import LoanOfficer, ComplianceOfficer
from app.config import settings

router = APIRouter()
security = HTTPBearer()


class RegisterRequest(BaseModel):
    username: str
    email: EmailStr
    password: str
    first_name: str
    last_name: str
    dob: str
    gender: str
    phone: str
    address: str
    city: str
    state: str
    country: str


class VerifyOtpRequest(BaseModel):
    email: EmailStr
    otp: str


class LoginRequest(BaseModel):
    username: str
    password: str


class AuthResponse(BaseModel):
    user_id: int
    username: str
    email: str
    role: str
    token: str


@router.post("/register")
async def register(request: RegisterRequest, db: Session = Depends(get_db)):
    """Register a new applicant"""
    # Check if username exists
    if db.query(Applicant).filter(Applicant.username == request.username).first():
        raise HTTPException(status_code=400, detail="Username already taken")
    
    # Check if email exists
    if db.query(Applicant).filter(Applicant.email == request.email).first():
        raise HTTPException(status_code=400, detail="Email already registered")
    
    # Check if phone exists
    if db.query(Applicant).filter(Applicant.email == request.email).first():
        raise HTTPException(status_code=400, detail="Phone number already registered")
    
    # Create applicant
    applicant = Applicant(
        username=request.username,
        email=request.email,
        password_hash=get_password_hash(request.password),
        first_name=request.first_name,
        last_name=request.last_name,
        dob=request.dob,
        gender=request.gender,
        phone=request.phone,
        address=request.address,
        city=request.city,
        state=request.state,
        country=request.country,
        is_approved=False,
        is_email_verified=False,
        approval_status="PENDING"
    )
    db.add(applicant)
    db.commit()
    db.refresh(applicant)
    
    # Generate and send OTP
    otp = create_otp_record(request.email, db)
    await send_otp_email(request.email, otp, request.first_name)
    
    return {"message": f"Registration initiated. Please verify your email with the OTP sent to {request.email}"}


@router.post("/verify-otp")
async def verify_otp_endpoint(request: VerifyOtpRequest, db: Session = Depends(get_db)):
    """Verify OTP and mark email as verified"""
    if verify_otp(request.email, request.otp, db):
        applicant = db.query(Applicant).filter(Applicant.email == request.email).first()
        if applicant:
            applicant.is_email_verified = True
            db.commit()
            return {"message": "Email verified successfully. Your registration is pending admin approval."}
    
    raise HTTPException(status_code=400, detail="Invalid or expired OTP")


@router.post("/login")
async def login(request: LoginRequest, db: Session = Depends(get_db)):
    """Common login endpoint for all user types"""
    # Try Admin
    admin = db.query(Admin).filter(Admin.username == request.username).first()
    if admin and verify_password(request.password, admin.password_hash):
        token = create_access_token({"sub": admin.username, "role": "ADMIN"})
        return AuthResponse(
            user_id=admin.admin_id,
            username=admin.username,
            email=admin.email,
            role="ADMIN",
            token=token
        )
    
    # Try Applicant
    applicant = db.query(Applicant).filter(Applicant.username == request.username).first()
    if applicant and verify_password(request.password, applicant.password_hash):
        if not applicant.is_email_verified:
            raise HTTPException(status_code=400, detail="Email not verified")
        if not applicant.is_approved:
            raise HTTPException(status_code=400, detail="Registration pending admin approval")
        token = create_access_token({"sub": applicant.username, "role": "APPLICANT"})
        return AuthResponse(
            user_id=applicant.applicant_id,
            username=applicant.username,
            email=applicant.email,
            role="APPLICANT",
            token=token
        )
    
    # Try Loan Officer
    officer = db.query(LoanOfficer).filter(LoanOfficer.username == request.username).first()
    if officer and verify_password(request.password, officer.password_hash):
        token = create_access_token({"sub": officer.username, "role": "LOAN_OFFICER"})
        return AuthResponse(
            user_id=officer.officer_id,
            username=officer.username,
            email=officer.email,
            role="LOAN_OFFICER",
            token=token
        )
    
    # Try Compliance Officer
    compliance = db.query(ComplianceOfficer).filter(ComplianceOfficer.username == request.username).first()
    if compliance and verify_password(request.password, compliance.password_hash):
        token = create_access_token({"sub": compliance.username, "role": "COMPLIANCE_OFFICER"})
        return AuthResponse(
            user_id=compliance.officer_id,
            username=compliance.username,
            email=compliance.email,
            role="COMPLIANCE_OFFICER",
            token=token
        )
    
    raise HTTPException(status_code=401, detail="Invalid credentials")


