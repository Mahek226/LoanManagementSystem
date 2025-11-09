from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, Numeric
from sqlalchemy.sql import func
from app.database import Base


class ExternalFraudRecord(Base):
    __tablename__ = "external_fraud_record"
    
    record_id = Column(Integer, primary_key=True, index=True)
    applicant_name = Column(String(200))
    applicant_email = Column(String(150))
    applicant_phone = Column(String(20))
    applicant_pan = Column(String(20))
    applicant_aadhaar = Column(String(20))
    fraud_type = Column(String(100))
    fraud_description = Column(Text)
    fraud_score = Column(Integer, default=0)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())


class ExternalCriminalRecord(Base):
    __tablename__ = "external_criminal_record"
    
    record_id = Column(Integer, primary_key=True, index=True)
    applicant_name = Column(String(200))
    applicant_pan = Column(String(20))
    applicant_aadhaar = Column(String(20))
    case_number = Column(String(100))
    case_type = Column(String(100))
    case_description = Column(Text)
    case_status = Column(String(50))
    case_date = Column(DateTime(timezone=True))
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class ExternalLoanHistory(Base):
    __tablename__ = "external_loan_history"
    
    history_id = Column(Integer, primary_key=True, index=True)
    applicant_name = Column(String(200))
    applicant_pan = Column(String(20))
    applicant_aadhaar = Column(String(20))
    loan_amount = Column(Numeric(15, 2))
    loan_type = Column(String(50))
    loan_status = Column(String(50))
    defaulted = Column(Boolean, default=False)
    default_amount = Column(Numeric(15, 2))
    default_date = Column(DateTime(timezone=True))
    lender_name = Column(String(200))
    created_at = Column(DateTime(timezone=True), server_default=func.now())


