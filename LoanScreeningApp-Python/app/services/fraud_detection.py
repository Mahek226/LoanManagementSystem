from sqlalchemy.orm import Session
from typing import Dict, List
from app.models.applicant import Applicant, ApplicantBasicDetails, ApplicantEmployment, ApplicantFinancials, ApplicantCreditHistory
from app.models.loan import ApplicantLoanDetails
from app.models.fraud import FraudFlag
from app.models.document import AadhaarDetails, PanDetails
from app.config import settings
import logging

logger = logging.getLogger(__name__)


class FraudDetectionService:
    def __init__(self, db: Session):
        self.db = db
    
    def run_fraud_detection(self, applicant_id: int) -> Dict:
        """Run complete fraud detection for an applicant"""
        applicant = self.db.query(Applicant).filter(Applicant.applicant_id == applicant_id).first()
        if not applicant:
            raise ValueError(f"Applicant not found with ID: {applicant_id}")
        
        # Get loan details
        loan = self.db.query(ApplicantLoanDetails).filter(
            ApplicantLoanDetails.applicant_id == applicant_id
        ).order_by(ApplicantLoanDetails.loan_id.desc()).first()
        
        total_score = 0
        triggered_rules = []
        
        # Identity fraud detection
        identity_result = self.detect_identity_fraud(applicant_id)
        total_score += identity_result["score"]
        triggered_rules.extend(identity_result["rules"])
        
        # Financial fraud detection
        financial_result = self.detect_financial_fraud(applicant_id)
        total_score += financial_result["score"]
        triggered_rules.extend(financial_result["rules"])
        
        # Employment fraud detection
        employment_result = self.detect_employment_fraud(applicant_id)
        total_score += employment_result["score"]
        triggered_rules.extend(employment_result["rules"])
        
        # Cross-verification fraud detection
        cross_result = self.detect_cross_verification_fraud(applicant_id)
        total_score += cross_result["score"]
        triggered_rules.extend(cross_result["rules"])
        
        # Determine risk level
        risk_level = self.calculate_risk_level(total_score)
        
        # Save fraud flags
        if loan:
            for rule in triggered_rules:
                fraud_flag = FraudFlag(
                    applicant_id=applicant_id,
                    loan_id=loan.loan_id,
                    rule_name=rule["name"],
                    rule_category=rule["category"],
                    fraud_points=rule["points"],
                    flag_notes=rule["description"]
                )
                self.db.add(fraud_flag)
            
            # Update loan risk score
            loan.risk_score = total_score
            loan.risk_level = risk_level
            self.db.commit()
        
        return {
            "applicant_id": applicant_id,
            "total_fraud_score": total_score,
            "risk_level": risk_level,
            "is_fraudulent": total_score >= 60,
            "triggered_rules": triggered_rules,
            "recommendation": "REJECT" if total_score >= 60 else ("REVIEW" if total_score >= 30 else "APPROVE")
        }
    
    def detect_identity_fraud(self, applicant_id: int) -> Dict:
        """Detect identity-related fraud"""
        score = 0
        rules = []
        
        applicant = self.db.query(Applicant).filter(Applicant.applicant_id == applicant_id).first()
        basic_details = self.db.query(ApplicantBasicDetails).filter(
            ApplicantBasicDetails.applicant_id == applicant_id
        ).first()
        
        # Check for duplicate email
        duplicate_email = self.db.query(Applicant).filter(
            Applicant.email == applicant.email,
            Applicant.applicant_id != applicant_id
        ).count()
        if duplicate_email > 0:
            score += 20
            rules.append({
                "name": "Duplicate Email",
                "category": "IDENTITY",
                "points": 20,
                "description": "Email address already registered"
            })
        
        # Check for duplicate phone
        duplicate_phone = self.db.query(Applicant).filter(
            Applicant.phone == applicant.phone,
            Applicant.applicant_id != applicant_id
        ).count()
        if duplicate_phone > 0:
            score += 20
            rules.append({
                "name": "Duplicate Phone",
                "category": "IDENTITY",
                "points": 20,
                "description": "Phone number already registered"
            })
        
        # Check PAN-Aadhaar mismatch
        if basic_details:
            aadhaar = self.db.query(AadhaarDetails).filter(
                AadhaarDetails.applicant_id == applicant_id
            ).first()
            pan = self.db.query(PanDetails).filter(
                PanDetails.applicant_id == applicant_id
            ).first()
            
            if aadhaar and pan and aadhaar.name and pan.name:
                # Simple name matching (can be enhanced)
                if aadhaar.name.upper().replace(" ", "") != pan.name.upper().replace(" ", ""):
                    score += 15
                    rules.append({
                        "name": "Name Mismatch",
                        "category": "IDENTITY",
                        "points": 15,
                        "description": "Name mismatch between Aadhaar and PAN"
                    })
        
        return {"score": score, "rules": rules}
    
    def detect_financial_fraud(self, applicant_id: int) -> Dict:
        """Detect financial-related fraud"""
        score = 0
        rules = []
        
        financials = self.db.query(ApplicantFinancials).filter(
            ApplicantFinancials.applicant_id == applicant_id
        ).first()
        credit_history = self.db.query(ApplicantCreditHistory).filter(
            ApplicantCreditHistory.applicant_id == applicant_id
        ).first()
        loan = self.db.query(ApplicantLoanDetails).filter(
            ApplicantLoanDetails.applicant_id == applicant_id
        ).order_by(ApplicantLoanDetails.loan_id.desc()).first()
        
        if loan and financials:
            # Check if loan amount exceeds income
            if financials.annual_income and loan.loan_amount:
                if float(loan.loan_amount) > float(financials.annual_income) * 5:
                    score += 25
                    rules.append({
                        "name": "Excessive Loan Amount",
                        "category": "FINANCIAL",
                        "points": 25,
                        "description": "Loan amount exceeds 5x annual income"
                    })
        
        if credit_history:
            # Check credit score
            if credit_history.credit_score and credit_history.credit_score < 300:
                score += 30
                rules.append({
                    "name": "Low Credit Score",
                    "category": "FINANCIAL",
                    "points": 30,
                    "description": f"Credit score is very low: {credit_history.credit_score}"
                })
            
            # Check defaults
            if credit_history.defaults_count and credit_history.defaults_count > 0:
                score += 20 * min(credit_history.defaults_count, 3)
                rules.append({
                    "name": "Loan Defaults",
                    "category": "FINANCIAL",
                    "points": 20 * min(credit_history.defaults_count, 3),
                    "description": f"Applicant has {credit_history.defaults_count} loan defaults"
                })
            
            # Check bankruptcy
            if credit_history.bankruptcy_filed:
                score += 50
                rules.append({
                    "name": "Bankruptcy Filed",
                    "category": "FINANCIAL",
                    "points": 50,
                    "description": "Applicant has filed for bankruptcy"
                })
        
        return {"score": score, "rules": rules}
    
    def detect_employment_fraud(self, applicant_id: int) -> Dict:
        """Detect employment-related fraud"""
        score = 0
        rules = []
        
        employment = self.db.query(ApplicantEmployment).filter(
            ApplicantEmployment.applicant_id == applicant_id
        ).first()
        
        if employment:
            # Check if employment details are missing
            if not employment.employer_name or not employment.monthly_income:
                score += 15
                rules.append({
                    "name": "Incomplete Employment Details",
                    "category": "EMPLOYMENT",
                    "points": 15,
                    "description": "Missing employer name or income information"
                })
            
            # Check for unrealistic income
            if employment.monthly_income and float(employment.monthly_income) < 10000:
                score += 10
                rules.append({
                    "name": "Low Income",
                    "category": "EMPLOYMENT",
                    "points": 10,
                    "description": "Monthly income is below minimum threshold"
                })
        
        return {"score": score, "rules": rules}
    
    def detect_cross_verification_fraud(self, applicant_id: int) -> Dict:
        """Detect cross-verification fraud"""
        score = 0
        rules = []
        
        applicant = self.db.query(Applicant).filter(Applicant.applicant_id == applicant_id).first()
        aadhaar = self.db.query(AadhaarDetails).filter(
            AadhaarDetails.applicant_id == applicant_id
        ).first()
        pan = self.db.query(PanDetails).filter(
            PanDetails.applicant_id == applicant_id
        ).first()
        
        # Check DOB mismatch
        if aadhaar and aadhaar.dob and applicant.dob:
            if str(aadhaar.dob) != str(applicant.dob):
                score += 20
                rules.append({
                    "name": "DOB Mismatch",
                    "category": "CROSS_VERIFICATION",
                    "points": 20,
                    "description": "Date of birth mismatch between application and Aadhaar"
                })
        
        # Check address mismatch
        if aadhaar and aadhaar.address and applicant.address:
            # Simple address matching (can be enhanced)
            if aadhaar.address.upper().replace(" ", "")[:20] != applicant.address.upper().replace(" ", "")[:20]:
                score += 10
                rules.append({
                    "name": "Address Mismatch",
                    "category": "CROSS_VERIFICATION",
                    "points": 10,
                    "description": "Address mismatch between application and documents"
                })
        
        return {"score": score, "rules": rules}
    
    def calculate_risk_level(self, score: int) -> str:
        """Calculate risk level based on fraud score"""
        if score >= settings.CRITICAL_RISK_THRESHOLD:
            return "CRITICAL"
        elif score >= settings.HIGH_RISK_THRESHOLD:
            return "HIGH"
        elif score >= settings.MEDIUM_RISK_THRESHOLD:
            return "MEDIUM"
        elif score >= settings.LOW_RISK_THRESHOLD:
            return "LOW"
        else:
            return "CLEAN"


