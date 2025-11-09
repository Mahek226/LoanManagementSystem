from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class UploadedDocument(Base):
    __tablename__ = "uploaded_document"
    
    document_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    loan_id = Column(Integer, ForeignKey("applicant_loan_details.loan_id"), nullable=True)
    document_type = Column(String(100), nullable=False)
    document_name = Column(String(255))
    cloudinary_url = Column(Text, nullable=True)
    file_path = Column(String(500), nullable=True)
    verification_status = Column(String(50), default="PENDING")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    applicant = relationship("Applicant")


class AadhaarDetails(Base):
    __tablename__ = "aadhaar_details"
    
    aadhaar_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    aadhaar_number = Column(String(20))
    name = Column(String(200))
    dob = Column(String(50))
    gender = Column(String(10))
    address = Column(Text)
    qr_code_data = Column(Text, nullable=True)
    cloudinary_url = Column(Text, nullable=True)
    ocr_text = Column(Text, nullable=True)
    file_path = Column(String(500), nullable=True)
    is_tampered = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    applicant = relationship("Applicant")


class PanDetails(Base):
    __tablename__ = "pan_details"
    
    pan_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    pan_number = Column(String(20))
    name = Column(String(200))
    father_name = Column(String(200), nullable=True)
    dob = Column(String(50), nullable=True)
    cloudinary_url = Column(Text, nullable=True)
    ocr_text = Column(Text, nullable=True)
    file_path = Column(String(500), nullable=True)
    is_tampered = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    applicant = relationship("Applicant")


class PassportDetails(Base):
    __tablename__ = "passport_details"
    
    passport_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    passport_number = Column(String(50))
    name = Column(String(200))
    dob = Column(String(50))
    nationality = Column(String(100))
    cloudinary_url = Column(Text)
    ocr_text = Column(Text)
    is_tampered = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    applicant = relationship("Applicant")


class OtherDocument(Base):
    __tablename__ = "other_document"
    
    document_id = Column(Integer, primary_key=True, index=True)
    applicant_id = Column(Integer, ForeignKey("applicant.applicant_id"), nullable=False)
    loan_id = Column(Integer, ForeignKey("applicant_loan_details.loan_id"), nullable=True)
    document_type = Column(String(100))
    document_name = Column(String(255))
    cloudinary_url = Column(Text)
    description = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    applicant = relationship("Applicant")

