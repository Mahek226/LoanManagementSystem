# main.py
import os
import shutil
from fastapi import FastAPI, File, UploadFile, Form, HTTPException, Depends
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
import models
from extractor import parse, extract_by_type  # careful: you might name parse from agentic doc differently; we'll call parse directly below
from extractor import extract_by_type as extractor_dispatch
from agentic_doc.parse import parse as agentic_parse
from models import Applicant, Document, ExtractedField
from db import SessionLocal, engine, Base
import cv2
from PIL import ImageFont, ImageDraw, Image
import numpy as np
import re


Base.metadata.create_all(bind=engine)

app = FastAPI(title="Document Extraction API")

# Add CORS middleware to allow frontend calls
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow all origins for development
    allow_credentials=False,  # Set to False when using wildcard origins
    allow_methods=["*"],  # Allow all methods
    allow_headers=["*"],  # Allow all headers
    expose_headers=["*"]  # Expose all headers
)

UPLOAD_DIR = os.getenv("UPLOAD_DIR", "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# from fastapi import FastAPI, File, Form, UploadFile, Depends, HTTPException
# from fastapi.responses import JSONResponse
# from sqlalchemy.orm import Session
# import os, shutil, re
# from models import Applicant, Document, ExtractedField
# from db import get_db
# from extractor import extractor_dispatch
# from agentic_doc import agentic_parse


# UPLOAD_DIR = "uploads"
# os.makedirs(UPLOAD_DIR, exist_ok=True)


# ---------------- CONFIDENCE CALCULATION FUNCTION ----------------
def compute_confidence(field_name, field_value):
    """
    Compute a simple confidence score based on regex pattern matching.
    You can later replace this with a proper ML-based or LandingAI output.
    """
    if not field_value:
        return 0.0

    # strong patterns
    patterns = {
        "PAN": (r"^[A-Z]{5}[0-9]{4}[A-Z]$", 0.95),
        "AADHAAR": (r"^\d{4}\s\d{4}\s\d{4}$", 0.9),
        "ACCOUNT": (r"^\d{9,18}$", 0.9),
        "NAME": (r"^[A-Za-z\s]+$", 0.85),
        "DATE": (r"\d{2}[/-]\d{2}[/-]\d{4}", 0.8)
    }

    for key, (pattern, conf) in patterns.items():
        if key in field_name.upper() and re.match(pattern, str(field_value).strip()):
            return conf

    return 0.7  # default medium confidence


# ---------------- MAIN EXTRACT ENDPOINT ----------------
# @app.post("/extract")
# async def extract_document(
#     applicant_id: int = Form(...),
#     document_type: str = Form(...),
#     file: UploadFile = File(...),
#     db: Session = Depends(get_db)
# ):
#     # Step 1: Validate applicant
#     applicant = db.query(Applicant).filter(Applicant.applicant_id == applicant_id).first()
#     if not applicant:
#         raise HTTPException(status_code=404, detail="Applicant not found")

#     # Step 2: Save file locally
#     fname = file.filename
#     save_path = os.path.join(UPLOAD_DIR, fname)
#     with open(save_path, "wb") as f:
#         shutil.copyfileobj(file.file, f)

#     # Step 3: Insert document record
#     doc_record = Document(
#         applicant_id=applicant_id, 
#         document_type=document_type, 
#         document_name=fname,
#         cloudinary_url=""  # Provide empty string or actual Cloudinary URL if you have one
#     )
#     db.add(doc_record)
#     db.commit()
#     db.refresh(doc_record)

#     # Step 4: Parse document using agentic-doc
#     try:
#         results = agentic_parse(save_path)
#         if not results:
#             raise ValueError("No parse result")
#         doc = results[0]
#     except Exception as e:
#         db.delete(doc_record)
#         db.commit()
#         raise HTTPException(status_code=500, detail=f"Parsing failed: {e}")

#     # Step 5: Extract fields using dispatcher
#     extracted = extractor_dispatch(doc, document_type)
    
#     # Debug: Print raw text to see what agentic-doc extracted
#     raw_text = "\n".join(chunk.text for chunk in doc.chunks if chunk.text) if doc.chunks else "No text chunks found"
#     print(f"\n🔍 Raw extracted text from agentic-doc:")
#     print(f"Text length: {len(raw_text)} characters")
#     print(f"First 500 characters: {raw_text[:500]}...")
#     print(f"Extracted fields: {extracted}")
    
#     # If extraction failed, try to extract basic info from raw text
#     if not extracted or all(v is None or v == "" for v in extracted.values()):
#         print("⚠️ Primary extraction failed, trying fallback extraction...")
#         extracted = {
#             "raw_text": raw_text[:1000],  # First 1000 chars
#             "document_type": document_type,
#             "extraction_method": "fallback"
#         }

#     # Step 6: Add confidence scores
#     extracted_with_conf = {}
#     for field, value in (extracted or {}).items():
#         conf = compute_confidence(field, value)
#         bbox = value.get("bbox") if isinstance(value, dict) and "bbox" in value else None
#         extracted_with_conf[field] = {
#             "value": value["value"] if isinstance(value, dict) and "value" in value else value,
#             "confidence": conf,
#             "bbox": bbox
#         }

#     # Step 7: Log extracted info
#     print(f"\n📄 Document Type: {document_type}")
#     print("🔍 Extracted Fields with Confidence:")
#     for k, v in extracted_with_conf.items():
#         print(f"  • {k}: {v['value']}  (Confidence: {v['confidence']*100:.1f}%)")

#     # Step 8: Draw bounding boxes (if bbox data available)
#     annotated_path = None
#     try:
#         image = cv2.imread(save_path)
#         if image is not None:
#             for field, details in extracted_with_conf.items():
#                 bbox = details.get("bbox")
#                 conf = details.get("confidence", 0.7)
#                 if bbox:
#                     x, y, w, h = map(int, bbox)
#                     # color by confidence (green→red)
#                     color = (
#                         int(255 * (1 - conf)),  # Red decreases with confidence
#                         int(255 * conf),        # Green increases with confidence
#                         0
#                     )
#                     cv2.rectangle(image, (x, y), (x + w, y + h), color, 2)
#                     label = f"{field}: {conf*100:.1f}%"
#                     cv2.putText(image, label, (x, y - 10),
#                                 cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)
#             # Ensure proper file extension for OpenCV
#             base_name = os.path.splitext(fname)[0]
#             annotated_path = os.path.join(UPLOAD_DIR, f"annotated_{base_name}.jpg")
#             cv2.imwrite(annotated_path, image)
#     except Exception as e:
#         print(f"⚠️ Skipped visualization: {e}")

#     # Step 9: Save extracted fields to DB (TEMPORARILY DISABLED FOR TESTING)
#     print("⚠️ Skipping database save for testing - will return extraction results only")
#     # for field_name, details in extracted_with_conf.items():
#     #     val = details["value"]
#     #     if isinstance(val, (dict, list)):
#     #         val = str(val)
#     #     if val is None or val == "":
#     #         val = "Not found"  # Provide default value instead of None/empty
#     #     ef = ExtractedField(
#     #         document_id=doc_record.document_id, 
#     #         field_name=field_name, 
#     #         field_value=val,
#     #         verified=False  # Add the verified field with default value
#     #     )
#     #     db.add(ef)
#     # db.commit()

#     # Step 10: Return JSON with visualization info
#     return JSONResponse({
#         "document_id": doc_record.document_id,
#         "applicant_id": applicant_id,
#         "document_type": document_type,
#         "extracted": extracted_with_conf,
#         "annotated_image": annotated_path if annotated_path else "Bounding boxes not available"
#     })

# @app.post("/extract")
# async def extract_document(
#     applicant_id: int = Form(...),
#     document_type: str = Form(...),
#     file: UploadFile = File(...)
# ):
#     try:
#         # Step 1: Skip applicant validation for now
#         print(f"🔍 Processing extraction for applicant {applicant_id}")

#         # Step 2: Save file locally
#         fname = file.filename
#         save_path = os.path.join(UPLOAD_DIR, fname)
#         with open(save_path, "wb") as f:
#             shutil.copyfileobj(file.file, f)

#         # Step 3: Create mock document record (no database)
#         class MockDocument:
#             def __init__(self):
#                 self.document_id = 999
        
#         doc_record = MockDocument()
#         print("📄 Using mock document ID - no database save")

#         # Step 4: Parse document using agentic-doc
#         try:
#             results = agentic_parse(save_path)
#             if not results:
#                 raise ValueError("No parse result")
#             doc = results[0]
#         except Exception as e:
#             print(f"⚠️ Parsing failed: {e}")
#             # Return a basic response even if parsing fails
#             return JSONResponse({
#                 "document_id": 999,
#                 "applicant_id": applicant_id,
#                 "document_type": document_type,
#                 "extracted": {},
#                 "raw_text_length": 0,
#                 "annotated_image": "Parsing failed",
#                 "error": str(e)
#             })

#         # Step 5: Extract fields using dispatcher
#         extracted = extractor_dispatch(doc, document_type)
        
#         # Debug: Print raw text and extracted summary
#         raw_text = "\n".join(chunk.text for chunk in doc.chunks if chunk.text) if doc.chunks else "No text chunks found"
#         print(f"\n🔍 Raw extracted text length: {len(raw_text)}")
#         print(f"Extracted fields: {extracted}")

#         # Step 6: Add confidence scores
#         extracted_with_conf = {}
#         for field, value in (extracted or {}).items():
#             conf = compute_confidence(field, value)
#             bbox = value.get("bbox") if isinstance(value, dict) and "bbox" in value else None
#             extracted_with_conf[field] = {
#                 "value": value["value"] if isinstance(value, dict) and "value" in value else value,
#                 "confidence": conf,
#                 "bbox": bbox
#             }

#         # Step 7: Log extracted info
#         print(f"\n📄 Document Type: {document_type}")
#         print("🔍 Extracted Fields with Confidence:")
#         for k, v in extracted_with_conf.items():
#             print(f"  • {k}: {v['value']}  (Confidence: {v['confidence']*100:.1f}%)")

#         # Step 8: Skip bounding boxes to avoid any issues
#         annotated_path = None
#         print("⚠️ Skipping bounding box visualization to avoid errors")

#         # Step 9: Database save completely disabled
#         print("💾 Database save completely disabled - showing data in UI only")

#         # Step 10: Return JSON with visualization path
#         return JSONResponse({
#             "document_id": doc_record.document_id,
#             "applicant_id": applicant_id,
#             "document_type": document_type,
#             "extracted": extracted_with_conf,
#             "raw_text_length": len(raw_text),
#             "annotated_image": annotated_path if annotated_path else "Bounding boxes not available"
#         })
        
#     except Exception as e:
#         # Catch any remaining errors and return a safe response
#         print(f"🚨 Unexpected error in extraction: {e}")
#         return JSONResponse({
#             "document_id": 999,
#             "applicant_id": applicant_id,
#             "document_type": document_type,
#             "extracted": {
#                 "name": {"value": None, "confidence": 0.0},
#                 "dob": {"value": "", "confidence": 0.0},
#                 "gender": {"value": None, "confidence": 0.0},
#                 "aadhaar_number": {"value": None, "confidence": 0.0},
#                 "address": {"value": None, "confidence": 0.0},
#                 "enrollment_number": {"value": None, "confidence": 0.0},
#                 "qr_present": {"value": False, "confidence": 0.0}
#             },
#             "raw_text_length": 4141,
#             "annotated_image": "Error occurred",
#             "error": str(e)
#         })

@app.post("/extract")
async def extract_document(
    applicant_id: int = Form(...),
    document_type: str = Form(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    # Step 1: Validate applicant
    applicant = db.query(Applicant).filter(Applicant.applicant_id == applicant_id).first()
    if not applicant:
        raise HTTPException(status_code=404, detail="Applicant not found")

    # Step 2: Save uploaded file
    fname = file.filename
    save_path = os.path.join(UPLOAD_DIR, fname)
    with open(save_path, "wb") as f:
        shutil.copyfileobj(file.file, f)

    # Step 3: Create document record
    doc_record = Document(
        applicant_id=applicant_id,
        document_type=document_type,
        document_name=fname,
        cloudinary_url=""
    )
    db.add(doc_record)
    db.commit()
    db.refresh(doc_record)

    # Step 4: Parse document using agentic-doc
    try:
        results = agentic_parse(save_path)
        if not results:
            raise ValueError("No parse result")
        doc = results[0]
    except Exception as e:
        db.delete(doc_record)
        db.commit()
        raise HTTPException(status_code=500, detail=f"Parsing failed: {e}")

    # Step 5: Extract fields using dispatcher
    extracted = extractor_dispatch(doc, document_type)
    # raw_text = "\n".join(chunk.text for chunk in doc.chunks if chunk.text) if doc.chunks else "No text"
    # print(f"\n🔍 Raw extracted text length: {len(raw_text)}")
    # print(f"Extracted fields: {extracted}")

    # Fallback if nothing extracted
    if not extracted or all(v in (None, "") for v in extracted.values()):
        extracted = {"raw_text": raw_text[:1000]}


    # Step 6: Compute confidence
    extracted_with_confidence = {}
    for field, value in extracted.items():
        confidence = compute_confidence(field, value)
        extracted_with_confidence[field] = {
            "value": value,
            "confidence": round(confidence, 4)

        }

    # Step 7: Print result summary
    print(f"\n📄 Document Type: {document_type}")
    for k, v in extracted_with_confidence.items():
        print(f"  • {k}: {v['value']}  (Confidence: {v['confidence']*100:.1f}%)")

    # Step 8: Skip visualization to avoid image errors
    annotated_path = None

    # Step 9: ❌ Skip DB save of extracted fields for now
    print("⚠️ Skipping extracted field database save — returning JSON only")

    # Step 10: Return JSON
    return JSONResponse({
        "document_id": doc_record.document_id,
        "applicant_id": applicant_id,
        "document_type": document_type,
        "extracted": extracted_with_confidence,
        "annotated_image": annotated_path or "Bounding boxes not available"
    })


@app.post("/applicant")
def create_applicant(first_name: str = Form(...), last_name: str = Form(None), email: str = Form(None), phone: str = Form(None), db: Session = Depends(get_db)):
    app_obj = Applicant(first_name=first_name, last_name=last_name, email=email, phone=phone)
    db.add(app_obj)
    db.commit()
    db.refresh(app_obj)
    return {"id": app_obj.applicant_id, "first_name": app_obj.first_name}

@app.get("/applicants/{id}/documents")
def get_docs(id: int, db: Session = Depends(get_db)):
    applicant = db.query(Applicant).filter(Applicant.applicant_id == id).first()
    if not applicant:
        raise HTTPException(status_code=404, detail="Applicant not found")
    out = []
    for doc in applicant.documents:
        fields = {f.field_name: f.field_value for f in doc.fields}
        out.append({
            "document_id": doc.document_id,
            "document_type": doc.document_type,
            "document_name": doc.document_name,
            "uploaded_at": doc.uploaded_at.isoformat(),
            "fields": fields
        })
    return {"applicant_id": id, "documents": out}

@app.get("/")
def root():
    """Root endpoint"""
    return {"message": "Document Extraction API", "status": "running", "endpoints": ["/extract", "/health", "/docs"]}

@app.get("/health")
def health_check():
    """Health check endpoint"""
    return {"status": "ok", "service": "Document Extraction API"}

@app.get("/test-cors")
def test_cors():
    """Test CORS endpoint"""
    return {"message": "CORS is working", "timestamp": "2025-11-03T11:38:00"}

# Run the server
if __name__ == "__main__":
    import uvicorn
    print("=" * 60)
    print("Starting Document Extraction Service...")
    print("API will be available at: http://127.0.0.1:8000")
    print("API Documentation at: http://127.0.0.1:8000/docs")
    print("=" * 60)
    uvicorn.run(app, host="127.0.0.1", port=8000, log_level="info")
