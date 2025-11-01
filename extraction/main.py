# main.py
import os
import shutil
from fastapi import FastAPI, File, UploadFile, Form, HTTPException, Depends
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from db import SessionLocal, engine
import models
from extractor import parse, extract_by_type  # careful: you might name parse from agentic doc differently; we'll call parse directly below
from extractor import extract_by_type as extractor_dispatch
from agentic_doc.parse import parse as agentic_parse
from models import Applicant, Document, ExtractedField
from db import Base

Base.metadata.create_all(bind=engine)

app = FastAPI(title="Document Extraction API")

UPLOAD_DIR = os.getenv("UPLOAD_DIR", "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.post("/extract")
async def extract_document(
    applicant_id: int = Form(...),
    document_type: str = Form(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    # basic validation
    applicant = db.query(Applicant).filter(Applicant.applicant_id == applicant_id).first()
    if not applicant:
        raise HTTPException(status_code=404, detail="Applicant not found")

    # save file locally
    fname = file.filename
    save_path = os.path.join(UPLOAD_DIR, fname)
    with open(save_path, "wb") as f:
        shutil.copyfileobj(file.file, f)

    # Insert document record
    doc_record = Document(applicant_id=applicant_id, document_type=document_type, document_name=fname)
    db.add(doc_record)
    db.commit()
    db.refresh(doc_record)

    # Parse with agentic-doc
    try:
        # agentic_parse returns a list of parsed documents
        results = agentic_parse(save_path)
        if not results:
            raise ValueError("No parse result")
        doc = results[0]
    except Exception as e:
        # rollback optional: remove doc record or mark error
        db.delete(doc_record)
        db.commit()
        raise HTTPException(status_code=500, detail=f"Parsing failed: {e}")

    # Extract fields by doc type
    extracted = extractor_dispatch(doc, document_type)

    # Save extracted fields to db (extracted_fields table)
    for k, v in (extracted or {}).items():
        # convert dicts/lists to JSON string if needed
        if isinstance(v, (dict, list)):
            val = str(v)
        else:
            val = v
        if val is None:
            continue
        ef = ExtractedField(document_id=doc_record.document_id, field_name=k, field_value=val)
        db.add(ef)
    db.commit()

    # optional: copy photo or qr if present and agentic_doc returned them - skip here
    return JSONResponse({
        "document_id": doc_record.document_id,
        "applicant_id": applicant_id,
        "document_type": document_type,
        "extracted": extracted
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

@app.get("/health")
def health_check():
    """Health check endpoint"""
    return {"status": "ok", "service": "Document Extraction API"}

# Run the server
if __name__ == "__main__":
    import uvicorn
    print("=" * 60)
    print("Starting Document Extraction Service...")
    print("API will be available at: http://127.0.0.1:8000")
    print("API Documentation at: http://127.0.0.1:8000/docs")
    print("=" * 60)
    uvicorn.run(app, host="127.0.0.1", port=8000, log_level="info")
