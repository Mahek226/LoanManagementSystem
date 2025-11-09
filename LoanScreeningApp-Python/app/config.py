from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):
    # Database
    DATABASE_URL: str
    EXTERNAL_DATABASE_URL: str
    
    # JWT
    JWT_SECRET_KEY: str
    JWT_ALGORITHM: str = "HS256"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 1440
    
    # Email
    SMTP_HOST: str
    SMTP_PORT: int = 587
    SMTP_USER: str
    SMTP_PASSWORD: str
    EMAIL_FROM: str
    
    # Server
    SERVER_HOST: str = "0.0.0.0"
    SERVER_PORT: int = 8000
    CORS_ORIGINS: str = "http://localhost:4200,http://127.0.0.1:4200"
    
    # OCR
    TESSERACT_CMD: str = "/usr/bin/tesseract"
    TESSERACT_DATA_DIR: str = "/usr/share/tesseract-ocr/4.00/tessdata"
    
    # File Upload
    UPLOAD_DIR: str = "uploads"
    MAX_UPLOAD_SIZE: int = 15728640  # 15MB
    
    # External Fraud
    EXTERNAL_FRAUD_ENABLED: bool = True
    EXTERNAL_FRAUD_TIMEOUT_MS: int = 10000
    EXTERNAL_FRAUD_RETRY_ATTEMPTS: int = 3
    
    # Risk Score Thresholds
    RISK_SCORE_THRESHOLD: int = 70
    CRITICAL_RISK_THRESHOLD: int = 80
    HIGH_RISK_THRESHOLD: int = 60
    MEDIUM_RISK_THRESHOLD: int = 35
    LOW_RISK_THRESHOLD: int = 15
    
    # Document Extraction
    DOCUMENT_EXTRACTION_URL: str = "http://127.0.0.1:8000"
    
    @property
    def cors_origins_list(self) -> List[str]:
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",")]
    
    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()


