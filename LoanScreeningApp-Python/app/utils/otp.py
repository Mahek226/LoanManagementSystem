import random
from datetime import datetime, timedelta
from app.models.auth import EmailOtp
from app.database import SessionLocal


def generate_otp(length: int = 6) -> str:
    """Generate a random OTP"""
    return ''.join([str(random.randint(0, 9)) for _ in range(length)])


def create_otp_record(email: str, db) -> str:
    """Create and save an OTP record"""
    otp = generate_otp()
    expires_at = datetime.utcnow() + timedelta(minutes=10)
    
    # Invalidate previous OTPs for this email
    db.query(EmailOtp).filter(
        EmailOtp.email == email,
        EmailOtp.is_verified == False
    ).update({"is_verified": True})
    
    # Create new OTP record
    otp_record = EmailOtp(
        email=email,
        otp_code=otp,
        is_verified=False,
        expires_at=expires_at
    )
    db.add(otp_record)
    db.commit()
    db.refresh(otp_record)
    
    return otp


def verify_otp(email: str, otp: str, db) -> bool:
    """Verify an OTP"""
    otp_record = db.query(EmailOtp).filter(
        EmailOtp.email == email,
        EmailOtp.otp_code == otp,
        EmailOtp.is_verified == False,
        EmailOtp.expires_at > datetime.utcnow()
    ).first()
    
    if otp_record:
        otp_record.is_verified = True
        otp_record.verified_at = datetime.utcnow()
        db.commit()
        return True
    
    return False


