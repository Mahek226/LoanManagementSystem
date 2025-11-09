from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from sqlalchemy.orm import Session
from typing import Optional
import os
import shutil
from datetime import datetime
from app.database import get_db
from app.core.dependencies import require_role
from app.models.applicant import Applicant
from app.models.document import UploadedDocument, AadhaarDetails, PanDetails
from app.services.document_extraction import DocumentExtractionService
from app.config import settings
import logging

logger = logging.getLogger(__name__)

router = APIRouter()
extraction_service = DocumentExtractionService()

# Ensure upload directory exists
os.makedirs(settings.UPLOAD_DIR, exist_ok=True)


@router.post("/upload")
async def upload_document(
    applicant_id: int = Form(...),
    document_type: str = Form(...),
    file: UploadFile = File(...),
    current_user: dict = Depends(require_role(["APPLICANT", "ADMIN"])),
    db: Session = Depends(get_db)
):
    """Upload and extract document"""
    # Validate applicant
    applicant = db.query(Applicant).filter(Applicant.applicant_id == applicant_id).first()
    if not applicant:
        raise HTTPException(status_code=404, detail="Applicant not found")
    
    # Check access
    user = current_user["user"]
    if current_user["role"] == "APPLICANT" and user.applicant_id != applicant_id:
        raise HTTPException(status_code=403, detail="Access denied")
    
    # Save file
    timestamp = int(datetime.now().timestamp() * 1000)
    file_extension = os.path.splitext(file.filename)[1]
    filename = f"{document_type}_{applicant_id}_{timestamp}{file_extension}"
    file_path = os.path.join(settings.UPLOAD_DIR, filename)
    
    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"File upload failed: {str(e)}")
    
    # Create document record
    doc_record = UploadedDocument(
        applicant_id=applicant_id,
        document_type=document_type,
        document_name=file.filename,
        file_path=file_path,
        verification_status="PENDING"
    )
    db.add(doc_record)
    db.commit()
    db.refresh(doc_record)
    
    # Extract document data
    extraction_result = await extraction_service.extract_document(file_path, document_type)
    
    # Save extracted data based on document type
    if document_type.upper() in ("AADHAAR", "AADHAAR_CARD", "AADHAR"):
        extracted = extraction_result.get("extracted", {})
        aadhaar = AadhaarDetails(
            applicant_id=applicant_id,
            aadhaar_number=extracted.get("aadhaar_number", {}).get("value"),
            name=extracted.get("name", {}).get("value"),
            dob=extracted.get("dob", {}).get("value"),
            gender=extracted.get("gender", {}).get("value"),
            address=extracted.get("address", {}).get("value"),
            file_path=file_path
        )
        db.add(aadhaar)
        db.commit()
    
    elif document_type.upper() in ("PAN", "PAN_CARD"):
        extracted = extraction_result.get("extracted", {})
        pan = PanDetails(
            applicant_id=applicant_id,
            pan_number=extracted.get("pan_number", {}).get("value"),
            name=extracted.get("name", {}).get("value"),
            father_name=extracted.get("father_name", {}).get("value"),
            dob=extracted.get("dob", {}).get("value"),
            file_path=file_path
        )
        db.add(pan)
        db.commit()
    
    return {
        "document_id": doc_record.document_id,
        "applicant_id": applicant_id,
        "document_type": document_type,
        "filename": file.filename,
        "extraction": extraction_result
    }


@router.get("/applicant/{applicant_id}/documents")
async def get_applicant_documents(
    applicant_id: int,
    current_user: dict = Depends(require_role(["APPLICANT", "ADMIN", "LOAN_OFFICER", "COMPLIANCE_OFFICER"])),
    db: Session = Depends(get_db)
):
    """Get all documents for an applicant"""
    # Check access
    user = current_user["user"]
    if current_user["role"] == "APPLICANT" and user.applicant_id != applicant_id:
        raise HTTPException(status_code=403, detail="Access denied")
    
    documents = db.query(UploadedDocument).filter(
        UploadedDocument.applicant_id == applicant_id
    ).all()
    
    return [
        {
            "document_id": doc.document_id,
            "document_type": doc.document_type,
            "document_name": doc.document_name,
            "verification_status": doc.verification_status,
            "created_at": doc.created_at.isoformat() if doc.created_at else None
        }
        for doc in documents
    ]


