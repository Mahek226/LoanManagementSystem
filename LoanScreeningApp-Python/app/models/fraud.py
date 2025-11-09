from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, ForeignKey, Numeric
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class FraudFlag(Base):
    __tablename__ = "fraud_flag"
    
    flag_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    loan_id = Column(Integer, ForeignKey("applicant_loan_details.loan_id"), nullable=True)
    rule_name = Column(String(200))
    rule_category = Column(String(100))
    fraud_points = Column(Integer, default=0)
    flag_notes = Column(Text)
    is_resolved = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    applicant = relationship("Applicant")
    loan = relationship("ApplicantLoanDetails", back_populates="fraud_flags")


class FraudRuleDefinition(Base):
    __tablename__ = "fraud_rule_definition"
    
    rule_id = Column(Integer, primary_key=True, index=True)
    rule_name = Column(String(200), nullable=False, unique=True)
    rule_category = Column(String(100))
    rule_description = Column(Text)
    fraud_points = Column(Integer, default=0)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())


class FraudRuleAudit(Base):
    __tablename__ = "fraud_rule_audit"
    
    audit_id = Column(Integer, primary_key=True, index=True)
    rule_id = Column(Integer, ForeignKey("fraud_rule_definition.rule_id"), nullable=False)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    triggered_at = Column(DateTime(timezone=True), server_default=func.now())
    fraud_score = Column(Integer)
    
    rule = relationship("FraudRuleDefinition")
    applicant = relationship("Applicant")


