from .applicant import Applicant, ApplicantBasicDetails, ApplicantEmployment, ApplicantFinancials
from .loan import ApplicantLoanDetails, LoanCollateral
from .admin import Admin, AdminLog
from .officer import LoanOfficer, ComplianceOfficer
from .assignment import OfficerApplicationAssignment, ComplianceOfficerApplicationAssignment
from .document import UploadedDocument, AadhaarDetails, PanDetails, PassportDetails, OtherDocument
from .fraud import FraudFlag, FraudRuleDefinition, FraudRuleAudit
from .external_fraud import ExternalFraudRecord, ExternalCriminalRecord, ExternalLoanHistory
from .auth import EmailOtp, PasswordResetToken
from .notification import ApplicantNotification
from .audit import AuditScore, VerificationLog, ActivityLog
from .credit import ApplicantCreditHistory
from .property import ApplicantPropertyDetails
from .dependent import ApplicantDependent
from .document_resubmission import DocumentResubmission

__all__ = [
    "Applicant",
    "ApplicantBasicDetails",
    "ApplicantEmployment",
    "ApplicantFinancials",
    "ApplicantLoanDetails",
    "LoanCollateral",
    "Admin",
    "AdminLog",
    "LoanOfficer",
    "ComplianceOfficer",
    "OfficerApplicationAssignment",
    "ComplianceOfficerApplicationAssignment",
    "UploadedDocument",
    "AadhaarDetails",
    "PanDetails",
    "PassportDetails",
    "OtherDocument",
    "FraudFlag",
    "FraudRuleDefinition",
    "FraudRuleAudit",
    "ExternalFraudRecord",
    "ExternalCriminalRecord",
    "ExternalLoanHistory",
    "EmailOtp",
    "PasswordResetToken",
    "ApplicantNotification",
    "AuditScore",
    "VerificationLog",
    "ActivityLog",
    "ApplicantCreditHistory",
    "ApplicantPropertyDetails",
    "ApplicantDependent",
    "DocumentResubmission",
]


