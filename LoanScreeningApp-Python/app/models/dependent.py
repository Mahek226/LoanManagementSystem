from sqlalchemy import Column, Integer, String, Date, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class ApplicantDependent(Base):
    __tablename__ = "applicant_dependent"
    
    dependent_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    name = Column(String(200))
    relationship = Column(String(50))
    dob = Column(Date)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    applicant = relationship("Applicant", back_populates="dependents")


