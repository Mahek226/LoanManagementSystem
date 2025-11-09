from sqlalchemy import Column, Integer, String, Boolean, Date, DateTime, Text, ForeignKey, Numeric
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class Applicant(Base):
    __tablename__ = "applicant"
    
    applicant_id = Column(Integer, primary_key=True, index=True)
    first_name = Column(String(100), nullable=False)
    last_name = Column(String(100))
    dob = Column(Date, nullable=False)
    gender = Column(String(10))
    username = Column(String(50), unique=True, nullable=False, index=True)
    email = Column(String(100), unique=True, nullable=False, index=True)
    phone = Column(String(20), unique=True, nullable=False, index=True)
    address = Column(Text, nullable=False)
    city = Column(String(100), nullable=False)
    state = Column(String(100), nullable=False)
    country = Column(String(100), nullable=False)
    password_hash = Column(Text, nullable=False)
    is_approved = Column(Boolean, default=False, nullable=False)
    is_email_verified = Column(Boolean, default=False, nullable=False)
    approval_status = Column(String(50), default="PENDING")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    # Relationships
    basic_details = relationship("ApplicantBasicDetails", back_populates="applicant", uselist=False)
    employment = relationship("ApplicantEmployment", back_populates="applicant", uselist=False)
    financials = relationship("ApplicantFinancials", back_populates="applicant", uselist=False)
    loan_details = relationship("ApplicantLoanDetails", back_populates="applicant")
    credit_history = relationship("ApplicantCreditHistory", back_populates="applicant", uselist=False)
    property_details = relationship("ApplicantPropertyDetails", back_populates="applicant", uselist=False)
    dependents = relationship("ApplicantDependent", back_populates="applicant")
    notifications = relationship("ApplicantNotification", back_populates="applicant")


class ApplicantBasicDetails(Base):
    __tablename__ = "applicant_basic_details"
    
    basic_details_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False, unique=True)
    marital_status = Column(String(50))
    education = Column(String(100))
    nationality = Column(String(100))
    pan_number = Column(String(20))
    aadhaar_number = Column(String(20))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    applicant = relationship("Applicant", back_populates="basic_details")


class ApplicantEmployment(Base):
    __tablename__ = "applicant_employment"
    
    employment_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False, unique=True)
    employment_type = Column(String(50))
    employer_name = Column(String(200))
    designation = Column(String(100))
    monthly_income = Column(Numeric(15, 2))
    years_of_experience = Column(Integer)
    employment_status = Column(String(50))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    applicant = relationship("Applicant", back_populates="employment")


class ApplicantFinancials(Base):
    __tablename__ = "applicant_financials"
    
    financial_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False, unique=True)
    bank_name = Column(String(200))
    account_number = Column(String(50))
    ifsc_code = Column(String(20))
    account_type = Column(String(50))
    annual_income = Column(Numeric(15, 2))
    existing_loans = Column(Numeric(15, 2))
    monthly_expenses = Column(Numeric(15, 2))
    savings = Column(Numeric(15, 2))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    applicant = relationship("Applicant", back_populates="financials")


