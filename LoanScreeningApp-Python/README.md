# Loan Screening Automation System - Python FastAPI Backend

A comprehensive loan management system built with FastAPI, featuring automated screening, fraud detection, and multi-role workflow management.

## Features

- **Multi-Role Authentication**: Admin, Applicant, Loan Officer, Compliance Officer
- **OTP-based Email Verification**: Secure registration workflow
- **Loan Application Management**: Complete loan submission and tracking
- **Document Processing**: OCR extraction for Aadhaar, PAN, and other documents
- **Automated Assignment**: Smart loan officer assignment based on loan type
- **Fraud Detection**: Internal and external fraud screening with AI-based scoring
- **Risk Assessment**: Comprehensive risk scoring and evaluation
- **Compliance Escalation**: Automatic escalation for high-risk cases
- **Admin Dashboard**: Real-time analytics and monitoring
- **Email Notifications**: Automated email notifications for all workflows

## Setup

### Prerequisites
- Python 3.8 or higher
- MySQL database
- Gmail account (for email service)

### Installation Steps

1. **Clone and navigate to the project:**
```bash
cd LoanScreeningApp-Python
```

2. **Create virtual environment (recommended):**
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. **Install dependencies:**
```bash
pip install -r requirements.txt
```

4. **Configure environment variables:**
```bash
# Copy the example file
cp .env.example .env

# Edit .env with your configuration:
# - Database credentials (MySQL)
# - JWT secret key
# - Email SMTP settings
# - Risk score thresholds
```

5. **Create database:**
```sql
CREATE DATABASE lms_python;
```

6. **Run the application:**
```bash
# Development mode
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Production mode
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

The application will automatically create all database tables on first run.

## API Documentation

Once the server is running, access:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## Project Structure

```
LoanScreeningApp-Python/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── config.py
│   ├── database.py
│   ├── models/
│   ├── schemas/
│   ├── api/
│   │   ├── auth/
│   │   ├── admin/
│   │   ├── applicant/
│   │   ├── loan/
│   │   ├── officer/
│   │   └── compliance/
│   ├── services/
│   ├── utils/
│   └── core/
├── alembic/
├── uploads/
├── requirements.txt
└── README.md
```

## License

MIT

