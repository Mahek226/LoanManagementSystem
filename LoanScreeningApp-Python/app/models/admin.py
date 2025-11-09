from sqlalchemy import Column, Integer, String, DateTime, Text
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class Admin(Base):
    __tablename__ = "admin"
    
    admin_id = Column(Integer, primary_key=True, index=True)
    username = Column(String(100), unique=True, nullable=False)
    email = Column(String(150), unique=True, nullable=False)
    password_hash = Column(Text, nullable=False)
    first_name = Column(String(100))
    last_name = Column(String(100))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    admin_logs = relationship("AdminLog", back_populates="admin")


class AdminLog(Base):
    __tablename__ = "admin_log"
    
    log_id = Column(Integer, primary_key=True, index=True)
    admin_id = Column(Integer, ForeignKey("admin.admin_id"), nullable=False)
    action = Column(String(100))
    description = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    admin = relationship("Admin", back_populates="admin_logs")


