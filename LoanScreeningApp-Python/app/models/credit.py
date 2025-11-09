from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, ForeignKey, Numeric
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class ApplicantCreditHistory(Base):
    __tablename__ = "applicant_credit_history"
    
    credit_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False, unique=True)
    credit_score = Column(Integer)
    credit_bureau = Column(String(100))
    total_active_loans = Column(Integer, default=0)
    total_outstanding_debt = Column(Numeric(15, 2), default=0)
    credit_card_count = Column(Integer, default=0)
    defaults_count = Column(Integer, default=0)
    bankruptcy_filed = Column(Boolean, default=False)
    payment_history = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    applicant = relationship("Applicant", back_populates="credit_history")


