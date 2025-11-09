from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey, Numeric
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class ApplicantPropertyDetails(Base):
    __tablename__ = "applicant_property_details"
    
    property_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False, unique=True)
    property_type = Column(String(100))
    property_address = Column(Text)
    property_value = Column(Numeric(15, 2))
    ownership_type = Column(String(50))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    applicant = relationship("Applicant", back_populates="property_details")


