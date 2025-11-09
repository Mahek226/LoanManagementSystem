import aiosmtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from jinja2 import Template
from app.config import settings
import logging

logger = logging.getLogger(__name__)


async def send_email(to_email: str, subject: str, body: str, is_html: bool = False):
    """Send an email using SMTP"""
    try:
        message = MIMEMultipart("alternative")
        message["From"] = settings.EMAIL_FROM
        message["To"] = to_email
        message["Subject"] = subject
        
        if is_html:
            message.attach(MIMEText(body, "html"))
        else:
            message.attach(MIMEText(body, "plain"))
        
        await aiosmtplib.send(
            message,
            hostname=settings.SMTP_HOST,
            port=settings.SMTP_PORT,
            username=settings.SMTP_USER,
            password=settings.SMTP_PASSWORD,
            use_tls=True,
        )
        logger.info(f"Email sent successfully to {to_email}")
        return True
    except Exception as e:
        logger.error(f"Failed to send email to {to_email}: {str(e)}")
        return False


async def send_otp_email(email: str, otp: str, name: str = "User"):
    """Send OTP verification email"""
    subject = "Email Verification OTP - Loan Screening System"
    body = f"""
    Dear {name},
    
    Your OTP for email verification is: {otp}
    
    This OTP will expire in 10 minutes.
    
    If you did not request this OTP, please ignore this email.
    
    Best regards,
    Loan Screening System
    """
    return await send_email(email, subject, body)


async def send_approval_email(email: str, name: str, approved: bool):
    """Send approval/rejection email to applicant"""
    if approved:
        subject = "Registration Approved - Loan Screening System"
        body = f"""
        Dear {name},
        
        Congratulations! Your registration has been approved by the administrator.
        
        You can now log in to the system and submit loan applications.
        
        Best regards,
        Loan Screening System
        """
    else:
        subject = "Registration Rejected - Loan Screening System"
        body = f"""
        Dear {name},
        
        We regret to inform you that your registration has been rejected.
        
        If you have any questions, please contact our support team.
        
        Best regards,
        Loan Screening System
        """
    return await send_email(email, subject, body)


async def send_loan_assignment_email(officer_email: str, officer_name: str, applicant_name: str, loan_type: str, loan_amount: float):
    """Send email notification to loan officer about new assignment"""
    subject = f"New Loan Application Assigned - {loan_type}"
    body = f"""
    Dear {officer_name},
    
    A new loan application has been assigned to you:
    
    - Applicant: {applicant_name}
    - Loan Type: {loan_type}
    - Loan Amount: ₹{loan_amount:,.2f}
    
    Please review the application in your dashboard.
    
    Best regards,
    Loan Screening System
    """
    return await send_email(officer_email, subject, body)


async def send_loan_status_email(applicant_email: str, applicant_name: str, status: str, loan_type: str, remarks: str = ""):
    """Send loan status update email to applicant"""
    subject = f"Loan Application Status Update - {status}"
    body = f"""
    Dear {applicant_name},
    
    Your loan application status has been updated:
    
    - Loan Type: {loan_type}
    - Status: {status}
    {f"- Remarks: {remarks}" if remarks else ""}
    
    Please check your dashboard for more details.
    
    Best regards,
    Loan Screening System
    """
    return await send_email(applicant_email, subject, body)


