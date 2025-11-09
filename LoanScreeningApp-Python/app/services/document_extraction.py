import os
import re
from typing import Dict, Optional
from fastapi import UploadFile
import pytesseract
from PIL import Image
from pdf2image import convert_from_path
import logging

logger = logging.getLogger(__name__)


class DocumentExtractionService:
    """Service for extracting data from documents using OCR"""
    
    def __init__(self):
        # Configure Tesseract path if needed
        # pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'  # Windows
        # For Linux/Mac, ensure tesseract is in PATH
        pass
    
    def extract_text_from_file(self, file_path: str) -> str:
        """Extract text from image or PDF file"""
        try:
            if file_path.lower().endswith('.pdf'):
                # Convert PDF to images
                images = convert_from_path(file_path)
                text = ""
                for image in images:
                    text += pytesseract.image_to_string(image) + "\n"
                return text
            else:
                # Extract from image
                image = Image.open(file_path)
                text = pytesseract.image_to_string(image)
                return text
        except Exception as e:
            logger.error(f"Error extracting text: {e}")
            return ""
    
    def extract_field(self, patterns: list, text: str, multiline: bool = False) -> Optional[str]:
        """Extract field using regex patterns"""
        flags = re.IGNORECASE | (re.DOTALL if multiline else 0)
        for pattern in patterns:
            match = re.search(pattern, text, flags)
            if match:
                return match.group(1).strip() if match.groups() else match.group(0).strip()
        return None
    
    def extract_aadhaar(self, text: str) -> Dict:
        """Extract Aadhaar card information"""
        result = {}
        result['name'] = self.extract_field([
            r"Name\s*[:\-]?\s*([A-Z][A-Za-z\s]+)",
            r"Cardholder name\s*[:\-]?\s*([A-Za-z\s]+)"
        ], text)
        
        result['dob'] = self.extract_field([
            r"Date of Birth\s*[:\-]?\s*([\d/ -]+)",
            r"DOB\s*[:\-]?\s*([\d/ -]+)",
            r"(\d{2}[/-]\d{2}[/-]\d{4})"
        ], text)
        
        result['gender'] = self.extract_field([
            r"Gender\s*[:\-]?\s*(Male|Female|M|F|Other)"
        ], text)
        
        result['aadhaar_number'] = self.extract_field([
            r"(\d{4}\s*\d{4}\s*\d{4})",
            r"Aadhaar\s*No\.?\s*[:\-]?\s*([\d\s]+)"
        ], text)
        
        result['address'] = self.extract_field([
            r"Address\s*[:\-]?\s*(.*?)(?:\n\n|\n\s*[A-Z][a-z]+:|\Z)"
        ], text, multiline=True)
        
        result['enrollment_number'] = self.extract_field([
            r"Enrollment\s*(?:No|Number)\s*[:\-]?\s*([\d/]+)"
        ], text)
        
        result['qr_present'] = "QR" in text[:200].upper()
        
        return result
    
    def extract_pan(self, text: str) -> Dict:
        """Extract PAN card information"""
        result = {}
        result['name'] = self.extract_field([
            r"Name\s*[:\-]?\s*([A-Z][A-Za-z\s]+)",
            r"([A-Z][A-Za-z\s]+)\n?PAN"
        ], text)
        
        result['pan_number'] = self.extract_field([
            r"([A-Z]{5}\d{4}[A-Z])"
        ], text)
        
        result['father_name'] = self.extract_field([
            r"Father's Name\s*[:\-]?\s*([A-Za-z\s]+)",
            r"Father Name\s*[:\-]?\s*([A-Za-z\s]+)"
        ], text)
        
        result['dob'] = self.extract_field([
            r"DOB\s*[:\-]?\s*([\d/ -]+)",
            r"(\d{2}[/-]\d{2}[/-]\d{4})"
        ], text)
        
        return result
    
    def extract_passport(self, text: str) -> Dict:
        """Extract Passport information"""
        result = {}
        result['name'] = self.extract_field([r"Name\s*[:\-]?\s*([A-Za-z\s]+)"], text)
        result['passport_no'] = self.extract_field([r"([A-Z0-9]{6,9})"], text)
        result['nationality'] = self.extract_field([r"Nationality\s*[:\-]?\s*([A-Za-z]+)"], text)
        result['dob'] = self.extract_field([r"Date of Birth\s*[:\-]?\s*([\d/ -]+)"], text)
        result['gender'] = self.extract_field([r"Sex\s*[:\-]?\s*(Male|Female|M|F)"], text)
        result['issue_date'] = self.extract_field([r"Issue Date\s*[:\-]?\s*([\d/ -]+)"], text)
        result['expiry_date'] = self.extract_field([r"Expiry Date\s*[:\-]?\s*([\d/ -]+)"], text)
        return result
    
    def extract_salary_slip(self, text: str) -> Dict:
        """Extract Salary Slip information"""
        result = {}
        result['employee_name'] = self.extract_field([r"Employee Name\s*[:\-]?\s*(.*)"], text)
        result['employer_name'] = self.extract_field([r"Employer\s*[:\-]?\s*(.*)"], text)
        result['gross_salary'] = self.extract_field([r"Gross\s+Pay\s*[:\-]?\s*([\d,\.]+)"], text)
        result['net_salary'] = self.extract_field([
            r"Net\s+Pay\s*[:\-]?\s*([\d,\.]+)",
            r"Net\s+Salary\s*[:\-]?\s*([\d,\.]+)"
        ], text)
        return result
    
    def extract_bank_statement(self, text: str) -> Dict:
        """Extract Bank Statement information"""
        result = {}
        result['bank_name'] = self.extract_field([
            r"Bank\s*[:\-]?\s*(.*)",
            r"([A-Z][A-Za-z\s]+ Bank)"
        ], text)
        result['account_holder'] = self.extract_field([
            r"Account Holder\s*[:\-]?\s*(.*)",
            r"A/c Name\s*[:\-]?\s*(.*)"
        ], text)
        result['account_number'] = self.extract_field([
            r"Account\s*No\.?\s*[:\-]?\s*(\d{6,20})",
            r"A/c No\.?\s*[:\-]?\s*(\d{6,20})"
        ], text)
        result['ifsc'] = self.extract_field([
            r"IFSC\s*[:\-]?\s*([A-Z0-9]{4}0[0-9A-Z]{6})",
            r"IFSC Code\s*[:\-]?\s*([A-Z0-9]+)"
        ], text)
        return result
    
    def compute_confidence(self, field_name: str, field_value: any) -> float:
        """Compute confidence score based on pattern matching"""
        if not field_value:
            return 0.0
        
        value_str = str(field_value).strip()
        
        patterns = {
            "PAN": (r"^[A-Z]{5}[0-9]{4}[A-Z]$", 0.95),
            "AADHAAR": (r"^\d{4}\s\d{4}\s\d{4}$", 0.9),
            "ACCOUNT": (r"^\d{9,18}$", 0.9),
            "NAME": (r"^[A-Za-z\s]+$", 0.85),
            "DATE": (r"\d{2}[/-]\d{2}[/-]\d{4}", 0.8)
        }
        
        for key, (pattern, conf) in patterns.items():
            if key in field_name.upper() and re.match(pattern, value_str):
                return conf
        
        return 0.7  # default medium confidence
    
    def extract_by_type(self, text: str, document_type: str) -> Dict:
        """Dispatch extraction based on document type"""
        doc_type = document_type.upper()
        
        if doc_type in ("AADHAAR", "AADHAAR_CARD", "AADHAR"):
            return self.extract_aadhaar(text)
        elif doc_type in ("PAN", "PAN_CARD"):
            return self.extract_pan(text)
        elif doc_type == "PASSPORT":
            return self.extract_passport(text)
        elif doc_type in ("SALARY_SLIP", "PAYSLIP"):
            return self.extract_salary_slip(text)
        elif doc_type == "BANK_STATEMENT":
            return self.extract_bank_statement(text)
        else:
            return {"raw_text": text[:1000]}
    
    async def extract_document(self, file_path: str, document_type: str) -> Dict:
        """Main extraction method"""
        try:
            # Extract text from file
            text = self.extract_text_from_file(file_path)
            
            if not text:
                return {
                    "extracted": {},
                    "raw_text": "",
                    "error": "No text extracted from document"
                }
            
            # Extract fields based on document type
            extracted = self.extract_by_type(text, document_type)
            
            # Add confidence scores
            extracted_with_confidence = {}
            for field, value in extracted.items():
                confidence = self.compute_confidence(field, value)
                extracted_with_confidence[field] = {
                    "value": value,
                    "confidence": round(confidence, 4)
                }
            
            return {
                "extracted": extracted_with_confidence,
                "raw_text": text[:500],  # First 500 chars
                "raw_text_length": len(text)
            }
        except Exception as e:
            logger.error(f"Error in document extraction: {e}")
            return {
                "extracted": {},
                "raw_text": "",
                "error": str(e)
            }


