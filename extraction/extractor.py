# extractor.py
import os
import re
from agentic_doc.parse import parse
from agentic_doc.config import get_settings

# set API key via environment variable externally, do not hardcode in code
settings = get_settings()
settings.vision_agent_api_key = os.getenv("VISION_AGENT_API_KEY")  # must be set before running

def _first_text(doc):
    if not doc.chunks:
        return ""
    return "\n".join(chunk.text for chunk in doc.chunks if chunk.text)

def extract_field(patterns, text, multiline=False):
    flags = re.IGNORECASE | (re.DOTALL if multiline else 0)
    for pattern in patterns:
        m = re.search(pattern, text, flags)
        if m:
            return m.group(1).strip()
    return None

def extract_aadhaar(doc):
    text = _first_text(doc)
    res = {}
    res['name'] = extract_field([r"Name\s*[:\-]?\s*([A-Z][A-Za-z\s]+)", r"Cardholder name\s*[:\-]?\s*([A-Za-z\s]+)"], text)
    res['dob'] = extract_field([r"Date of Birth\s*[:\-]?\s*([\d/ -]+)", r"DOB\s*[:\-]?\s*([\d/ -]+)"], text)
    res['gender'] = extract_field([r"Gender\s*[:\-]?\s*(Male|Female|M|F|Other)"], text)
    res['aadhaar_number'] = extract_field([r"(\d{4}\s*\d{4}\s*\d{4})", r"Aadhaar\s*No\.?\s*[:\-]?\s*([\d\s]+)"], text)
    res['address'] = extract_field([r"Address\s*[:\-]?\s*(.*?)(?:\n\n|\n\s*[A-Z][a-z]+:|\Z)"], text, multiline=True)
    res['enrollment_number'] = extract_field([r"Enrollment\s*(?:No|Number)\s*[:\-]?\s*([\d/]+)"], text)
    # optional: qr detection via doc.metadata or chunk info (agentic doc may contain qr info)
    res['qr_present'] = hasattr(doc, "qr") or ("QR" in text[:200].upper())
    return res

def extract_pan(doc):
    text = _first_text(doc)
    res = {}
    res['name'] = extract_field([r"Name\s*[:\-]?\s*([A-Z][A-Za-z\s]+)", r"([A-Z][A-Za-z\s]+)\n?PAN"], text)
    res['pan_number'] = extract_field([r"([A-Z]{5}\d{4}[A-Z])"], text)
    res['father_name'] = extract_field([r"Father's Name\s*[:\-]?\s*([A-Za-z\s]+)", r"Father Name\s*[:\-]?\s*([A-Za-z\s]+)"], text)
    res['dob'] = extract_field([r"DOB\s*[:\-]?\s*([\d/ -]+)"], text)
    return res

def extract_passport(doc):
    text = _first_text(doc)
    res = {}
    res['name'] = extract_field([r"Name\s*[:\-]?\s*([A-Za-z\s]+)"], text)
    res['passport_no'] = extract_field([r"([A-Z0-9]{6,9})"], text)  # refine if needed
    res['nationality'] = extract_field([r"Nationality\s*[:\-]?\s*([A-Za-z]+)"], text)
    res['dob'] = extract_field([r"Date of Birth\s*[:\-]?\s*([\d/ -]+)"], text)
    res['gender'] = extract_field([r"Sex\s*[:\-]?\s*(Male|Female|M|F)"], text)
    res['issue_date'] = extract_field([r"Issue Date\s*[:\-]?\s*([\d/ -]+)"], text)
    res['expiry_date'] = extract_field([r"Expiry Date\s*[:\-]?\s*([\d/ -]+)"], text)
    res['issue_authority'] = extract_field([r"Issuing Authority\s*[:\-]?\s*(.*)"], text)
    res['address'] = extract_field([r"Address\s*[:\-]?\s*(.*)"], text, multiline=True)
    return res

def extract_dl(doc):
    text = _first_text(doc)
    res={}
    res['name'] = extract_field([r"Name\s*[:\-]?\s*([A-Za-z\s]+)"], text)
    res['dl_number'] = extract_field([r"([A-Z0-9]{5,20}-?\d+)"], text)
    res['dob'] = extract_field([r"Date of Birth\s*[:\-]?\s*([\d/ -]+)", r"DOB\s*[:\-]?\s*([\d/ -]+)"], text)
    res['issue_date'] = extract_field([r"Issue Date\s*[:\-]?\s*([\d/ -]+)"], text)
    res['expiry_date'] = extract_field([r"Valid Till\s*[:\-]?\s*([\d/ -]+)|Expiry Date\s*[:\-]?\s*([\d/ -]+)"], text)
    res['address'] = extract_field([r"Address\s*[:\-]?\s*(.*)"], text, multiline=True)
    res['issuing_rto'] = extract_field([r"RTO\s*[:\-]?\s*(.*)"], text)
    return res

def extract_light_bill(doc):
    text = _first_text(doc)
    res={}
    res['name'] = extract_field([r"Consumer Name\s*[:\-]?\s*(.*)", r"Name\s*[:\-]?\s*(.*)"], text)
    res['address'] = extract_field([r"Address\s*[:\-]?\s*(.*)"], text, multiline=True)
    res['doc_type'] = "LIGHT_BILL"
    res['issue_date'] = extract_field([r"Bill Date\s*[:\-]?\s*([\d/ -]+)", r"Issue Date\s*[:\-]?\s*([\d/ -]+)"], text)
    res['authority'] = extract_field([r"(Electricity Board|DISCOM|Electricity)\s*(.*)"], text)
    return res

def extract_salary_slip(doc):
    text = _first_text(doc)
    res={}
    res['employee_name'] = extract_field([r"Employee Name\s*[:\-]?\s*(.*)"], text)
    res['employer_name'] = extract_field([r"Employer\s*[:\-]?\s*(.*)"], text)
    res['gross_salary'] = extract_field([r"Gross\s+Pay\s*[:\-]?\s*([\d,\.]+)"], text)
    res['net_salary'] = extract_field([r"Net\s+Pay\s*[:\-]?\s*([\d,\.]+)", r"Net\s+Salary\s*[:\-]?\s*([\d,\.]+)"], text)
    # earnings and deductions will need more advanced parsing (tables)
    return res

def extract_bank_statement(doc):
    text = _first_text(doc)
    res={}
    res['bank_name'] = extract_field([r"Bank\s*[:\-]?\s*(.*)", r"([A-Z][A-Za-z\s]+ Bank)"], text)
    res['account_holder'] = extract_field([r"Account Holder\s*[:\-]?\s*(.*)", r"A/c Name\s*[:\-]?\s*(.*)"], text)
    res['account_number'] = extract_field([r"Account\s*No\.?\s*[:\-]?\s*(\d{6,20})", r"A/c No\.?\s*[:\-]?\s*(\d{6,20})"], text)
    res['ifsc'] = extract_field([r"IFSC\s*[:\-]?\s*([A-Z0-9]{4}0[0-9A-Z]{6})", r"IFSC Code\s*[:\-]?\s*([A-Z0-9]+)"], text)
    # transaction lists require table extraction - agentic_doc may return structured table chunks
    return res

def extract_itr(doc):
    text = _first_text(doc)
    res={}
    res['name'] = extract_field([r"Name\s*[:\-]?\s*(.*)"], text)
    res['pan'] = extract_field([r"PAN\s*[:\-]?\s*([A-Z]{5}\d{4}[A-Z])"], text)
    res['assessment_year'] = extract_field([r"Assessment Year\s*[:\-]?\s*([0-9\/-]+)"], text)
    res['total_gross_income'] = extract_field([r"Total Gross Income\s*[:\-]?\s*([\d,\.]+)"], text)
    res['taxable_income'] = extract_field([r"Taxable Income\s*[:\-]?\s*([\d,\.]+)"], text)
    res['refund_or_tax_paid'] = extract_field([r"Refund\s*[:\-]?\s*(.*)|Tax Paid\s*[:\-]?\s*(.*)"], text)
    return res

def extract_employment_proof(doc):
    text = _first_text(doc)
    res={}
    res['name'] = extract_field([r"Employee Name\s*[:\-]?\s*(.*)", r"Name\s*[:\-]?\s*(.*)"], text)
    res['employment_type'] = extract_field([r"Type of Employment\s*[:\-]?\s*(.*)", r"Employment Type\s*[:\-]?\s*(.*)"], text)
    res['designation'] = extract_field([r"Designation\s*[:\-]?\s*(.*)"], text)
    res['join_date'] = extract_field([r"Date of Joining\s*[:\-]?\s*(.*)"], text)
    return res

# dispatcher
def extract_by_type(doc, doc_type):
    # doc_type should be normalized (upper-case)
    dt = doc_type.upper()
    if dt in ("AADHAAR","AADHAAR_CARD","AADHAR"):
        return extract_aadhaar(doc)
    if dt in ("PAN","PAN_CARD"):
        return extract_pan(doc)
    if dt in ("PASSPORT",):
        return extract_passport(doc)
    if dt in ("DRIVING_LICENSE","DL","DRIVING LICENCE"):
        return extract_dl(doc)
    if dt in ("LIGHT_BILL","UTILITY_BILL","ELECTRICITY_BILL"):
        return extract_light_bill(doc)
    if dt in ("SALARY_SLIP","PAYSLIP"):
        return extract_salary_slip(doc)
    if dt in ("BANK_STATEMENT","BANK_STATEMENT"):
        return extract_bank_statement(doc)
    if dt in ("ITR","INCOME_TAX_RETURN"):
        return extract_itr(doc)
    if dt in ("EMPLOYMENT_PROOF","EMPLOYMENT"):
        return extract_employment_proof(doc)

    # fallback: return raw text
    return {"raw_text": _first_text(doc)}
