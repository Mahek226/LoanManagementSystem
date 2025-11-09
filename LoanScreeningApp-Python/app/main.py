from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.database import engine, Base
from app.api import auth, admin, applicant, loan, officer, compliance, document

# Create database tables
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Loan Screening Automation System",
    description="A comprehensive loan management system with automated screening and fraud detection",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(auth.router, prefix="/api/auth", tags=["Authentication"])
app.include_router(applicant.router, prefix="/api/applicant", tags=["Applicant"])
app.include_router(admin.router, prefix="/api/admin", tags=["Admin"])
app.include_router(loan.router, prefix="/api/loan", tags=["Loan"])
app.include_router(officer.router, prefix="/api/officer", tags=["Loan Officer"])
app.include_router(compliance.router, prefix="/api/compliance", tags=["Compliance Officer"])
app.include_router(document.router, prefix="/api/document", tags=["Document"])


@app.get("/")
async def root():
    return {
        "message": "Loan Screening Automation System API",
        "version": "1.0.0",
        "docs": "/docs"
    }


@app.get("/health")
async def health_check():
    return {"status": "healthy"}

