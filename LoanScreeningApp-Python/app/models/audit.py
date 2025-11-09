from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey, Numeric
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class AuditScore(Base):
    __tablename__ = "audit_score"
    
    audit_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    loan_id = Column(Integer, ForeignKey("applicant_loan_details.loan_id"), nullable=True)
    score = Column(Numeric(5, 2))
    score_type = Column(String(50))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    applicant = relationship("Applicant")
    loan = relationship("ApplicantLoanDetails")


class VerificationLog(Base):
    __tablename__ = "verification_log"
    
    log_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    loan_id = Column(Integer, ForeignKey("applicant_loan_details.loan_id"), nullable=True)
    verification_type = Column(String(100))
    verification_status = Column(String(50))
    verified_by = Column(String(200))
    notes = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    applicant = relationship("Applicant")
    loan = relationship("ApplicantLoanDetails")


class ActivityLog(Base):
    __tablename__ = "activity_log"
    
    log_id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer)
    user_type = Column(String(50))
    action = Column(String(100))
    description = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


