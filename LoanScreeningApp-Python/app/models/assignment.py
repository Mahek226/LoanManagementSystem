from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class OfficerApplicationAssignment(Base):
    __tablename__ = "officer_application_assignment"
    
    assignment_id = Column(Integer, primary_key=True, index=True)
    officer_id = Column(Integer, ForeignKey("loan_officer.officer_id"), nullable=False)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    loan_id = Column(Integer, ForeignKey("applicant_loan_details.loan_id"), nullable=True)
    status = Column(String(50), default="PENDING")
    assigned_at = Column(DateTime(timezone=True), server_default=func.now())
    completed_at = Column(DateTime(timezone=True))
    processed_at = Column(DateTime(timezone=True))
    priority = Column(String(20), default="MEDIUM")
    remarks = Column(Text)
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    officer = relationship("LoanOfficer", back_populates="assignments")
    applicant = relationship("Applicant")
    loan = relationship("ApplicantLoanDetails", back_populates="assignments")


class ComplianceOfficerApplicationAssignment(Base):
    __tablename__ = "compliance_officer_application_assignment"
    
    assignment_id = Column(Integer, primary_key=True, index=True)
    compliance_officer_id = Column(Integer, ForeignKey("compliance_officer.officer_id"), nullable=False)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    loan_id = Column(Integer, ForeignKey("applicant_loan_details.loan_id"), nullable=True)
    status = Column(String(50), default="PENDING")
    assigned_at = Column(DateTime(timezone=True), server_default=func.now())
    completed_at = Column(DateTime(timezone=True))
    processed_at = Column(DateTime(timezone=True))
    priority = Column(String(20), default="HIGH")
    remarks = Column(Text)
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    compliance_officer = relationship("ComplianceOfficer", back_populates="assignments")
    applicant = relationship("Applicant")
    loan = relationship("ApplicantLoanDetails", back_populates="compliance_assignments")


