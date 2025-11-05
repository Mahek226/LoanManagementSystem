# models.py
from sqlalchemy import Column, Integer, String, Text, ForeignKey, DateTime, Boolean, BigInteger
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from db import Base

class Applicant(Base):
    __tablename__ = "applicant"
    applicant_id = Column(BigInteger, primary_key=True, index=True)
    first_name = Column(String(150))
    last_name = Column(String(150))
    email = Column(String(255))
    phone = Column(String(50))
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    documents = relationship("Document", back_populates="applicant", cascade="all, delete-orphan")

class Document(Base):
    __tablename__ = "uploaded_documents"
    document_id = Column(BigInteger, primary_key=True, index=True)
    applicant_id = Column(BigInteger, ForeignKey("applicant.applicant_id"))
    document_type = Column(String(50))
    document_name = Column(String(255))  # Changed from 'filename' to match database schema
    cloudinary_url = Column(String(500), nullable=True, default="")  # Add this field
    uploaded_at = Column(DateTime(timezone=True), server_default=func.now())

    applicant = relationship("Applicant", back_populates="documents")
    fields = relationship("ExtractedField", back_populates="document", cascade="all, delete-orphan")

class ExtractedField(Base):
    __tablename__ = "extracted_fields"
    id = Column(BigInteger, primary_key=True, index=True)
    document_id = Column(BigInteger, ForeignKey("uploaded_documents.document_id"))
    field_name = Column(String(150))
    field_value = Column(Text)
    extraction_method = Column(String(100))  # 👈 added
    confidence_score = Column(
        # DECIMAL(5,4) means total 5 digits, 4 after decimal
        # Example: 0.9876
        # Use Numeric if you want SQLAlchemy’s type match
        # but String is fine if your DB stores as string
        # here we use Float for convenience
        # Float
    )  # 👈 added
    verified = Column(Boolean, default=False, nullable=False)
    verified_by = Column(String(255))  # 👈 added
    verified_at = Column(DateTime(timezone=True))  # 👈 added
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    document = relationship("Document", back_populates="fields")