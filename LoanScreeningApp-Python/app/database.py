from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from app.config import settings

# Primary database engine
engine = create_engine(
    settings.DATABASE_URL,
    pool_pre_ping=True,
    pool_recycle=3600,
    echo=False
)

# External database engine (for fraud screening)
external_engine = create_engine(
    settings.EXTERNAL_DATABASE_URL,
    pool_pre_ping=True,
    pool_recycle=3600,
    echo=False
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
ExternalSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=external_engine)

Base = declarative_base()


def get_db():
    """Dependency for getting database session"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_external_db():
    """Dependency for getting external database session"""
    db = ExternalSessionLocal()
    try:
        yield db
    finally:
        db.close()


