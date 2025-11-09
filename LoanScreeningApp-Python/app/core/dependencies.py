from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from app.database import get_db
from app.core.security import decode_access_token
from app.models.applicant import Applicant
from app.models.admin import Admin
from app.models.officer import LoanOfficer, ComplianceOfficer

security = HTTPBearer()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
):
    """Get current authenticated user from JWT token"""
    token = credentials.credentials
    payload = decode_access_token(token)
    
    if payload is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    username = payload.get("sub")
    role = payload.get("role")
    
    if not username or not role:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token payload",
        )
    
    # Get user based on role
    if role == "ADMIN":
        user = db.query(Admin).filter(Admin.username == username).first()
    elif role == "APPLICANT":
        user = db.query(Applicant).filter(Applicant.username == username).first()
    elif role == "LOAN_OFFICER":
        user = db.query(LoanOfficer).filter(LoanOfficer.username == username).first()
    elif role == "COMPLIANCE_OFFICER":
        user = db.query(ComplianceOfficer).filter(ComplianceOfficer.username == username).first()
    else:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid user role",
        )
    
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
        )
    
    return {"user": user, "role": role}


def require_role(allowed_roles: list):
    """Dependency to require specific roles"""
    def role_checker(current_user: dict = Depends(get_current_user)):
        if current_user["role"] not in allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Insufficient permissions"
            )
        return current_user
    return role_checker


