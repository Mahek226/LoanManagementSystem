# db.py
from sqlalchemy import create_engine, MetaData
from sqlalchemy.orm import sessionmaker, declarative_base
import os
from dotenv import load_dotenv
import urllib.parse  # <-- for encoding special chars in password

load_dotenv()

DB_USER = os.getenv("DB_USER")
DB_PASS = urllib.parse.quote_plus(os.getenv("DB_PASS"))  # encode special chars
DB_HOST = os.getenv("DB_HOST")
DB_NAME = os.getenv("DB_NAME")
DB_PORT = os.getenv("DB_PORT")

# Build the connection string safely
DATABASE_URL = f"mysql+pymysql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"

# Create SQLAlchemy engine
engine = create_engine(DATABASE_URL, pool_pre_ping=True)

# Session factory
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base class for models
Base = declarative_base()
metadata = MetaData()
