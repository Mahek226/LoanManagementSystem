from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class DocumentResubmission(Base):
    __tablename__ = "document_resubmission"
    
    resubmission_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    loan_id = Column(Integer, ForeignKey("applicant_loan_details.loan_id"), nullable=True)
    document_type = Column(String(100))
    reason = Column(Text)
    requested_by = Column(String(200))
    is_resubmitted = Column(Boolean, default=False)
    requested_at = Column(DateTime(timezone=True), server_default=func.now())
    resubmitted_at = Column(DateTime(timezone=True))
    
    applicant = relationship("Applicant")
    loan = relationship("ApplicantLoanDetails")


