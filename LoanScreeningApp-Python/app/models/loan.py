from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, ForeignKey, Numeric
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class ApplicantLoanDetails(Base):
    __tablename__ = "applicant_loan_details"
    
    loan_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    loan_type = Column(String(50), nullable=False)
    loan_amount = Column(Numeric(15, 2), nullable=False)
    interest_rate = Column(Numeric(5, 2))
    tenure_months = Column(Integer, nullable=False)
    status = Column(String(50), default="pending")
    loan_purpose = Column(String(500))
    risk_score = Column(Integer, default=0)
    risk_level = Column(String(20))
    submitted_at = Column(DateTime(timezone=True), server_default=func.now())
    application_date = Column(DateTime(timezone=True))
    approval_date = Column(DateTime(timezone=True))
    reviewed_at = Column(DateTime(timezone=True))
    monthly_emi = Column(Numeric(15, 2))
    approved_by = Column(String(200))
    rejected_by = Column(String(200))
    reviewed_by = Column(String(200))
    rejection_date = Column(DateTime(timezone=True))
    rejection_reason = Column(String(1000))
    
    # Relationships
    applicant = relationship("Applicant", back_populates="loan_details")
    fraud_flags = relationship("FraudFlag", back_populates="loan")
    assignments = relationship("OfficerApplicationAssignment", back_populates="loan")
    compliance_assignments = relationship("ComplianceOfficerApplicationAssignment", back_populates="loan")
    collaterals = relationship("LoanCollateral", back_populates="loan")


class LoanCollateral(Base):
    __tablename__ = "loan_collateral"
    
    collateral_id = Column(Integer, primary_key=True, index=True)
    loan_id = Column(Integer, ForeignKey("applicant_loan_details.loan_id"), nullable=False)
    collateral_type = Column(String(100))
    collateral_value = Column(Numeric(15, 2))
    description = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    loan = relationship("ApplicantLoanDetails", back_populates="collaterals")


