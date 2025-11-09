from sqlalchemy.orm import Session
from typing import Dict, List
from app.database import get_external_db
from app.models.external_fraud import ExternalFraudRecord, ExternalCriminalRecord, ExternalLoanHistory
from app.config import settings
import logging

logger = logging.getLogger(__name__)


class ExternalFraudScreeningService:
    def __init__(self, db: Session):
        self.db = db
    
    def screen_applicant(self, applicant_id: int, applicant_name: str, pan: str = None, aadhaar: str = None) -> Dict:
        """Screen applicant against external fraud databases"""
        if not settings.EXTERNAL_FRAUD_ENABLED:
            return {
                "total_fraud_score": 0,
                "risk_level": "CLEAN",
                "is_fraudulent": False,
                "fraud_flags": []
            }
        
        total_score = 0
        fraud_flags = []
        
        # Check criminal records
        if pan or aadhaar:
            criminal_score, criminal_flags = self.check_criminal_records(applicant_name, pan, aadhaar)
            total_score += criminal_score
            fraud_flags.extend(criminal_flags)
        
        # Check loan history
        if pan or aadhaar:
            loan_score, loan_flags = self.check_loan_history(applicant_name, pan, aadhaar)
            total_score += loan_score
            fraud_flags.extend(loan_flags)
        
        # Check fraud records
        if pan or aadhaar:
            fraud_score, fraud_flags_list = self.check_fraud_records(applicant_name, pan, aadhaar)
            total_score += fraud_score
            fraud_flags.extend(fraud_flags_list)
        
        # Calculate risk level
        risk_level = self.calculate_risk_level(total_score)
        
        return {
            "total_fraud_score": total_score,
            "risk_level": risk_level,
            "is_fraudulent": total_score >= 100,
            "fraud_flags": fraud_flags
        }
    
    def check_criminal_records(self, name: str, pan: str = None, aadhaar: str = None) -> tuple:
        """Check criminal records database"""
        score = 0
        flags = []
        
        try:
            external_db = next(get_external_db())
            query = external_db.query(ExternalCriminalRecord).filter(
                ExternalCriminalRecord.is_active == True
            )
            
            if pan:
                query = query.filter(ExternalCriminalRecord.applicant_pan == pan)
            elif aadhaar:
                query = query.filter(ExternalCriminalRecord.applicant_aadhaar == aadhaar)
            else:
                query = query.filter(ExternalCriminalRecord.applicant_name.ilike(f"%{name}%"))
            
            records = query.all()
            
            if records:
                score = 100  # High score for criminal records
                flags.append({
                    "type": "CRIMINAL_RECORD",
                    "points": 100,
                    "description": f"Found {len(records)} criminal record(s)",
                    "details": [{"case_number": r.case_number, "case_type": r.case_type} for r in records]
                })
        except Exception as e:
            logger.error(f"Error checking criminal records: {e}")
        
        return score, flags
    
    def check_loan_history(self, name: str, pan: str = None, aadhaar: str = None) -> tuple:
        """Check loan history database"""
        score = 0
        flags = []
        
        try:
            external_db = next(get_external_db())
            query = external_db.query(ExternalLoanHistory)
            
            if pan:
                query = query.filter(ExternalLoanHistory.applicant_pan == pan)
            elif aadhaar:
                query = query.filter(ExternalLoanHistory.applicant_aadhaar == aadhaar)
            else:
                query = query.filter(ExternalLoanHistory.applicant_name.ilike(f"%{name}%"))
            
            records = query.all()
            
            # Check for defaults
            defaulted_loans = [r for r in records if r.defaulted]
            if defaulted_loans:
                score = 80
                flags.append({
                    "type": "LOAN_DEFAULT",
                    "points": 80,
                    "description": f"Found {len(defaulted_loans)} defaulted loan(s)",
                    "details": [{"loan_type": r.loan_type, "default_amount": float(r.default_amount)} for r in defaulted_loans]
                })
            
            # Check for multiple active loans
            active_loans = [r for r in records if r.loan_status == "ACTIVE"]
            if len(active_loans) > 5:
                score += 20
                flags.append({
                    "type": "MULTIPLE_LOANS",
                    "points": 20,
                    "description": f"Applicant has {len(active_loans)} active loans"
                })
        except Exception as e:
            logger.error(f"Error checking loan history: {e}")
        
        return score, flags
    
    def check_fraud_records(self, name: str, pan: str = None, aadhaar: str = None) -> tuple:
        """Check fraud records database"""
        score = 0
        flags = []
        
        try:
            external_db = next(get_external_db())
            query = external_db.query(ExternalFraudRecord).filter(
                ExternalFraudRecord.is_active == True
            )
            
            if pan:
                query = query.filter(ExternalFraudRecord.applicant_pan == pan)
            elif aadhaar:
                query = query.filter(ExternalFraudRecord.applicant_aadhaar == aadhaar)
            else:
                query = query.filter(ExternalFraudRecord.applicant_name.ilike(f"%{name}%"))
            
            records = query.all()
            
            if records:
                total_fraud_score = sum(r.fraud_score for r in records)
                score = min(total_fraud_score, 150)  # Cap at 150
                flags.append({
                    "type": "FRAUD_RECORD",
                    "points": score,
                    "description": f"Found {len(records)} fraud record(s)",
                    "details": [{"fraud_type": r.fraud_type, "description": r.fraud_description} for r in records]
                })
        except Exception as e:
            logger.error(f"Error checking fraud records: {e}")
        
        return score, flags
    
    def calculate_risk_level(self, score: int) -> str:
        """Calculate risk level based on external fraud score"""
        if score >= 150:
            return "CRITICAL"
        elif score >= 100:
            return "HIGH"
        elif score >= 50:
            return "MEDIUM"
        elif score >= 20:
            return "LOW"
        else:
            return "CLEAN"


