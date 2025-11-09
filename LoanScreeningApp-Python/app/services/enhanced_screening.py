from sqlalchemy.orm import Session
from typing import Dict
from app.services.fraud_detection import FraudDetectionService
from app.services.external_fraud import ExternalFraudScreeningService
from app.models.applicant import Applicant, ApplicantBasicDetails
from app.config import settings
import logging

logger = logging.getLogger(__name__)


class EnhancedLoanScreeningService:
    def __init__(self, db: Session):
        self.db = db
        self.internal_fraud_service = FraudDetectionService(db)
        self.external_fraud_service = ExternalFraudScreeningService(db)
    
    def perform_enhanced_screening(self, applicant_id: int) -> Dict:
        """Perform enhanced loan screening with normalized scoring"""
        applicant = self.db.query(Applicant).filter(Applicant.applicant_id == applicant_id).first()
        if not applicant:
            raise ValueError(f"Applicant not found with ID: {applicant_id}")
        
        # Get basic details for external screening
        basic_details = self.db.query(ApplicantBasicDetails).filter(
            ApplicantBasicDetails.applicant_id == applicant_id
        ).first()
        
        pan = basic_details.pan_number if basic_details else None
        aadhaar = basic_details.aadhaar_number if basic_details else None
        
        # Step 1: Run internal fraud detection
        internal_result = self.internal_fraud_service.run_fraud_detection(applicant_id)
        internal_score = internal_result["total_fraud_score"]
        
        # Step 2: Run external fraud screening
        external_result = self.external_fraud_service.screen_applicant(
            applicant_id,
            f"{applicant.first_name} {applicant.last_name}",
            pan,
            aadhaar
        )
        external_score = external_result["total_fraud_score"]
        
        # Step 3: Calculate normalized scores (0-100 scale)
        # Internal max: 200, External max: 150
        internal_normalized = min(100, (internal_score / 200) * 100)
        external_normalized = min(100, (external_score / 150) * 100)
        
        # Step 4: Weighted combination (60% internal, 40% external)
        normalized_score = (internal_normalized * 0.6) + (external_normalized * 0.4)
        
        # Step 5: Determine final recommendation
        recommendation = self.determine_recommendation(normalized_score)
        risk_level = self.calculate_risk_level(normalized_score)
        
        return {
            "applicant_id": applicant_id,
            "internal_score": internal_score,
            "external_score": external_score,
            "internal_normalized": round(internal_normalized, 2),
            "external_normalized": round(external_normalized, 2),
            "normalized_score": round(normalized_score, 2),
            "risk_level": risk_level,
            "recommendation": recommendation,
            "internal_result": {
                "total_fraud_score": internal_score,
                "risk_level": internal_result["risk_level"],
                "triggered_rules": len(internal_result["triggered_rules"])
            },
            "external_result": {
                "total_fraud_score": external_score,
                "risk_level": external_result["risk_level"],
                "fraud_flags_count": len(external_result["fraud_flags"])
            }
        }
    
    def determine_recommendation(self, normalized_score: float) -> str:
        """Determine final recommendation based on normalized score"""
        if normalized_score >= 60:
            return "REJECT"
        elif normalized_score >= 35:
            return "REVIEW"
        else:
            return "APPROVE"
    
    def calculate_risk_level(self, normalized_score: float) -> str:
        """Calculate risk level based on normalized score"""
        if normalized_score >= settings.CRITICAL_RISK_THRESHOLD:
            return "CRITICAL"
        elif normalized_score >= settings.HIGH_RISK_THRESHOLD:
            return "HIGH"
        elif normalized_score >= settings.MEDIUM_RISK_THRESHOLD:
            return "MEDIUM"
        elif normalized_score >= settings.LOW_RISK_THRESHOLD:
            return "LOW"
        else:
            return "CLEAN"


