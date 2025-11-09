from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text
from sqlalchemy.sql import func
from app.database import Base


class EmailOtp(Base):
    __tablename__ = "email_otp"
    
    otp_id = Column(Integer, primary_key=True, index=True)
    email = Column(String(150), nullable=False, index=True)
    otp_code = Column(String(10), nullable=False)
    is_verified = Column(Boolean, default=False, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    expires_at = Column(DateTime(timezone=True), nullable=False)
    verified_at = Column(DateTime(timezone=True))


class PasswordResetToken(Base):
    __tablename__ = "password_reset_token"
    
    token_id = Column(Integer, primary_key=True, index=True)
    email = Column(String(150), nullable=False, index=True)
    token = Column(String(255), nullable=False, unique=True)
    is_used = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    expires_at = Column(DateTime(timezone=True), nullable=False)
    used_at = Column(DateTime(timezone=True))


