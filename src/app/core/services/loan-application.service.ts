import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@environments/environment';

// Application Form Interfaces
export interface BasicDetails {
  loanType: string;
  loanAmount: number;
  tenure: number;
  purpose: string;
  hasCoApplicant: boolean;
  coApplicantName?: string;
  coApplicantRelation?: string;
  hasCollateral: boolean;
  collateralType?: string;
  collateralValue?: number;
}

export interface ApplicantDetails {
  firstName: string;
  middleName?: string;
  lastName: string;
  dateOfBirth: string;
  gender: string;
  maritalStatus: string;
  emailAddress: string;
  mobileNumber: string;
  alternateNumber?: string;
  currentAddress: string;
  currentCity: string;
  currentState: string;
  currentPincode: string;
  residenceType: string;
  yearsAtCurrentAddress: number;
  permanentAddressSame: boolean;
  permanentAddress?: string;
  permanentCity?: string;
  permanentState?: string;
  permanentPincode?: string;
  panNumber: string;
  aadharNumber: string;
  hasCoApplicant: boolean;
  coApplicantName?: string;
  coApplicantRelation?: string;
  coApplicantPan?: string;
  coApplicantAadhar?: string;
}

export interface FinancialDetails {
  employmentType: 'SALARIED' | 'SELF_EMPLOYED' | 'BUSINESS' | 'PROFESSIONAL' | 'RETIRED';
  employerName?: string;
  designation?: string;
  workExperience?: number;
  monthlyGrossSalary?: number;
  monthlyNetSalary?: number;
  businessName?: string;
  businessType?: string;
  annualTurnover?: number;
  otherIncomeSources?: string;
  otherIncomeAmount?: number;
  existingLoanEMI?: number;
  creditCardPayment?: number;
  otherObligations?: number;
  bankName: string;
  accountNumber: string;
  ifscCode: string;
  accountType: 'SAVINGS' | 'CURRENT';
  bankStatementConsent: boolean;
}

export interface DocumentUpload {
  documentType: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  uploadedAt: string;
  status: 'PENDING' | 'VERIFIED' | 'REJECTED';
  rejectionReason?: string;
  ocrData?: any;
  verificationStatus?: {
    faceMatch?: boolean;
    tamperDetection?: boolean;
    dataExtracted?: boolean;
  };
}

export interface Declaration {
  kycConsent: boolean;
  creditBureauConsent: boolean;
  bankStatementConsent: boolean;
  termsAccepted: boolean;
  privacyPolicyAccepted: boolean;
  eSignConsent: boolean;
  declarationDate: string;
  ipAddress?: string;
  eSignatureUrl?: string;
}

export interface LoanApplicationForm {
  applicationId?: string;
  applicantId: number;
  basicDetails: BasicDetails;
  applicantDetails?: ApplicantDetails;
  financialDetails: FinancialDetails;
  documents: DocumentUpload[];
  declarations: Declaration;
  currentStep: number;
  status: 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
  submittedAt?: string;
  lastUpdatedAt: string;
}

export interface EMICalculation {
  principal: number;
  annualInterestRate: number;
  tenureMonths: number;
  emi: number;
  totalInterest: number;
  totalAmount: number;
  monthlyRate: number;
  amortizationSchedule: AmortizationEntry[];
  dti: number;
  affordabilityStatus: 'EXCELLENT' | 'GOOD' | 'MODERATE' | 'RISKY';
}

export interface AmortizationEntry {
  month: number;
  emi: number;
  principal: number;
  interest: number;
  balance: number;
  cumulativeInterest: number;
}

export interface DocumentRequirement {
  documentType: string;
  displayName: string;
  required: boolean;
  description: string;
  acceptedFormats: string[];
  maxSize: number;
  icon: string;
}

export interface UploadProgress {
  fileName: string;
  progress: number;
  status: 'uploading' | 'success' | 'error';
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class LoanApplicationService {
  private readonly API_URL = environment.apiUrl;
  private readonly STORAGE_KEY = 'loan_application_draft';
  
  // Application state
  private applicationState = new BehaviorSubject<LoanApplicationForm | null>(null);
  public applicationState$ = this.applicationState.asObservable();

  constructor(private http: HttpClient) {
    this.loadDraft();
  }

  // Get current application state value
  getCurrentApplication(): LoanApplicationForm | null {
    return this.applicationState.value;
  }

  // ==================== Application Management ====================

  initializeApplication(applicantId: number, loanType: string): LoanApplicationForm {
    const application: LoanApplicationForm = {
      applicantId,
      basicDetails: {
        loanType,
        loanAmount: 0,
        tenure: 12,
        purpose: '',
        hasCoApplicant: false,
        hasCollateral: false
      },
      financialDetails: {
        employmentType: 'SALARIED',
        monthlyGrossSalary: 0,
        monthlyNetSalary: 0,
        existingLoanEMI: 0,
        creditCardPayment: 0,
        otherObligations: 0,
        bankName: '',
        accountNumber: '',
        ifscCode: '',
        accountType: 'SAVINGS',
        bankStatementConsent: false
      },
      documents: [],
      declarations: {
        kycConsent: false,
        creditBureauConsent: false,
        bankStatementConsent: false,
        termsAccepted: false,
        privacyPolicyAccepted: false,
        eSignConsent: false,
        declarationDate: new Date().toISOString()
      },
      currentStep: 1,
      status: 'DRAFT',
      lastUpdatedAt: new Date().toISOString()
    };
    
    this.applicationState.next(application);
    this.saveDraft(application);
    return application;
  }

  updateBasicDetails(details: BasicDetails): void {
    const current = this.applicationState.value;
    if (current) {
      current.basicDetails = details;
      current.lastUpdatedAt = new Date().toISOString();
      this.applicationState.next(current);
      this.saveDraft(current);
    }
  }

  updateApplicantDetails(details: ApplicantDetails): void {
    const current = this.applicationState.value;
    if (current) {
      current.applicantDetails = details;
      current.lastUpdatedAt = new Date().toISOString();
      this.applicationState.next(current);
      this.saveDraft(current);
    }
  }

  updateFinancialDetails(details: FinancialDetails): void {
    const current = this.applicationState.value;
    if (current) {
      current.financialDetails = details;
      current.lastUpdatedAt = new Date().toISOString();
      this.applicationState.next(current);
      this.saveDraft(current);
    }
  }

  addDocument(document: DocumentUpload): void {
    const current = this.applicationState.value;
    if (current) {
      current.documents.push(document);
      current.lastUpdatedAt = new Date().toISOString();
      this.applicationState.next(current);
      this.saveDraft(current);
    }
  }

  removeDocument(fileName: string): void {
    const current = this.applicationState.value;
    if (current) {
      current.documents = current.documents.filter(doc => doc.fileName !== fileName);
      current.lastUpdatedAt = new Date().toISOString();
      this.applicationState.next(current);
      this.saveDraft(current);
    }
  }

  updateDeclarations(declarations: Declaration): void {
    const current = this.applicationState.value;
    if (current) {
      current.declarations = declarations;
      current.lastUpdatedAt = new Date().toISOString();
      this.applicationState.next(current);
      this.saveDraft(current);
    }
  }

  setCurrentStep(step: number): void {
    const current = this.applicationState.value;
    if (current) {
      current.currentStep = step;
      this.applicationState.next(current);
      this.saveDraft(current);
    }
  }

  // ==================== EMI Calculation ====================

  calculateEMI(
    principal: number,
    annualInterestRate: number,
    tenureMonths: number,
    monthlyIncome: number,
    existingObligations: number
  ): EMICalculation {
    const monthlyRate = annualInterestRate / 12 / 100;
    const n = tenureMonths;
    
    // EMI Formula: P × r × (1 + r)^n ÷ ((1 + r)^n − 1)
    const onePlusR = 1 + monthlyRate;
    const onePlusRPowerN = Math.pow(onePlusR, n);
    const emi = (principal * monthlyRate * onePlusRPowerN) / (onePlusRPowerN - 1);
    
    const totalAmount = emi * n;
    const totalInterest = totalAmount - principal;
    
    // Calculate amortization schedule
    const amortizationSchedule: AmortizationEntry[] = [];
    let balance = principal;
    let cumulativeInterest = 0;
    
    for (let month = 1; month <= n; month++) {
      const interestPayment = balance * monthlyRate;
      const principalPayment = emi - interestPayment;
      balance -= principalPayment;
      cumulativeInterest += interestPayment;
      
      amortizationSchedule.push({
        month,
        emi: Math.round(emi * 100) / 100,
        principal: Math.round(principalPayment * 100) / 100,
        interest: Math.round(interestPayment * 100) / 100,
        balance: Math.round(Math.max(0, balance) * 100) / 100,
        cumulativeInterest: Math.round(cumulativeInterest * 100) / 100
      });
    }
    
    // Calculate DTI (Debt-to-Income ratio)
    const totalMonthlyObligation = emi + existingObligations;
    const dti = (totalMonthlyObligation / monthlyIncome) * 100;
    
    // Determine affordability status
    let affordabilityStatus: 'EXCELLENT' | 'GOOD' | 'MODERATE' | 'RISKY';
    if (dti <= 30) affordabilityStatus = 'EXCELLENT';
    else if (dti <= 40) affordabilityStatus = 'GOOD';
    else if (dti <= 50) affordabilityStatus = 'MODERATE';
    else affordabilityStatus = 'RISKY';
    
    return {
      principal,
      annualInterestRate,
      tenureMonths,
      emi: Math.round(emi * 100) / 100,
      totalInterest: Math.round(totalInterest * 100) / 100,
      totalAmount: Math.round(totalAmount * 100) / 100,
      monthlyRate,
      amortizationSchedule,
      dti: Math.round(dti * 100) / 100,
      affordabilityStatus
    };
  }

  // ==================== Document Management ====================

  getRequiredDocuments(loanType: string): DocumentRequirement[] {
    const commonDocs: DocumentRequirement[] = [
      {
        documentType: 'AADHAAR',
        displayName: 'Aadhaar Card',
        required: true,
        description: 'Government issued identity proof',
        acceptedFormats: ['image/jpeg', 'image/png', 'application/pdf'],
        maxSize: 5 * 1024 * 1024, // 5MB
        icon: 'fa-id-card'
      },
      {
        documentType: 'PAN',
        displayName: 'PAN Card',
        required: true,
        description: 'Permanent Account Number',
        acceptedFormats: ['image/jpeg', 'image/png', 'application/pdf'],
        maxSize: 5 * 1024 * 1024,
        icon: 'fa-credit-card'
      },
      {
        documentType: 'PHOTO',
        displayName: 'Passport Size Photo',
        required: true,
        description: 'Recent photograph for face verification',
        acceptedFormats: ['image/jpeg', 'image/png'],
        maxSize: 2 * 1024 * 1024,
        icon: 'fa-camera'
      },
      {
        documentType: 'BANK_STATEMENT',
        displayName: 'Bank Statements (Last 6 months)',
        required: true,
        description: 'Bank account statements',
        acceptedFormats: ['application/pdf'],
        maxSize: 10 * 1024 * 1024,
        icon: 'fa-file-invoice-dollar'
      }
    ];

    const employmentDocs: DocumentRequirement[] = [
      {
        documentType: 'SALARY_SLIP',
        displayName: 'Salary Slips (Last 3 months)',
        required: true,
        description: 'Recent salary slips',
        acceptedFormats: ['image/jpeg', 'image/png', 'application/pdf'],
        maxSize: 5 * 1024 * 1024,
        icon: 'fa-file-invoice'
      }
    ];

    const selfEmployedDocs: DocumentRequirement[] = [
      {
        documentType: 'ITR',
        displayName: 'Income Tax Returns (Last 2 years)',
        required: true,
        description: 'ITR acknowledgment',
        acceptedFormats: ['application/pdf'],
        maxSize: 10 * 1024 * 1024,
        icon: 'fa-file-alt'
      },
      {
        documentType: 'BUSINESS_PROOF',
        displayName: 'Business Registration',
        required: true,
        description: 'GST/Shop Act/Registration certificate',
        acceptedFormats: ['image/jpeg', 'image/png', 'application/pdf'],
        maxSize: 5 * 1024 * 1024,
        icon: 'fa-briefcase'
      }
    ];

    const propertyDocs: DocumentRequirement[] = [
      {
        documentType: 'PROPERTY_PAPERS',
        displayName: 'Property Documents',
        required: true,
        description: 'Sale deed, title deed, or allotment letter',
        acceptedFormats: ['application/pdf'],
        maxSize: 20 * 1024 * 1024,
        icon: 'fa-home'
      },
      {
        documentType: 'PROPERTY_VALUATION',
        displayName: 'Property Valuation Report',
        required: false,
        description: 'Approved valuer report',
        acceptedFormats: ['application/pdf'],
        maxSize: 10 * 1024 * 1024,
        icon: 'fa-chart-line'
      }
    ];

    const vehicleDocs: DocumentRequirement[] = [
      {
        documentType: 'VEHICLE_QUOTATION',
        displayName: 'Vehicle Quotation',
        required: true,
        description: 'Proforma invoice from dealer',
        acceptedFormats: ['image/jpeg', 'image/png', 'application/pdf'],
        maxSize: 5 * 1024 * 1024,
        icon: 'fa-car'
      },
      {
        documentType: 'DRIVING_LICENSE',
        displayName: 'Driving License',
        required: true,
        description: 'Valid driving license',
        acceptedFormats: ['image/jpeg', 'image/png', 'application/pdf'],
        maxSize: 5 * 1024 * 1024,
        icon: 'fa-id-card-alt'
      }
    ];

    let docs = [...commonDocs];

    switch (loanType) {
      case 'PERSONAL':
        docs.push(...employmentDocs);
        break;
      case 'HOME':
        docs.push(...employmentDocs, ...propertyDocs);
        break;
      case 'VEHICLE':
        docs.push(...employmentDocs, ...vehicleDocs);
        break;
      case 'EDUCATION':
        docs.push(...employmentDocs);
        docs.push({
          documentType: 'ADMISSION_LETTER',
          displayName: 'Admission Letter',
          required: true,
          description: 'University/college admission letter',
          acceptedFormats: ['application/pdf'],
          maxSize: 5 * 1024 * 1024,
          icon: 'fa-graduation-cap'
        });
        break;
      case 'BUSINESS':
        docs.push(...selfEmployedDocs);
        docs.push({
          documentType: 'BUSINESS_PLAN',
          displayName: 'Business Plan',
          required: false,
          description: 'Detailed business plan',
          acceptedFormats: ['application/pdf'],
          maxSize: 10 * 1024 * 1024,
          icon: 'fa-file-powerpoint'
        });
        break;
    }

    return docs;
  }

  uploadDocument(file: File, documentType: string, applicantId: number, loanId?: number): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    formData.append('applicantId', applicantId.toString());
    
    if (loanId) {
      formData.append('loanId', loanId.toString());
    }

    return this.http.post(`${this.API_URL}/documents/upload-single`, formData, {
      reportProgress: true,
      observe: 'events'
    }).pipe(
      map((event: HttpEvent<any>) => {
        switch (event.type) {
          case HttpEventType.UploadProgress:
            const progress = event.total ? Math.round((100 * event.loaded) / event.total) : 0;
            return {
              fileName: file.name,
              progress,
              status: 'uploading' as const,
              event
            };
          case HttpEventType.Response:
            return {
              fileName: file.name,
              progress: 100,
              status: 'success' as const,
              data: event.body
            };
          default:
            return {
              fileName: file.name,
              progress: 0,
              status: 'uploading' as const
            };
        }
      })
    );
  }

  uploadMultipleDocuments(files: File[], documentTypes: string[], applicantId: number, loanId?: number): Observable<any> {
    const formData = new FormData();
    
    files.forEach((file, index) => {
      formData.append('files', file);
    });
    
    documentTypes.forEach(type => {
      formData.append('documentTypes', type);
    });
    
    formData.append('applicantId', applicantId.toString());
    
    if (loanId) {
      formData.append('loanId', loanId.toString());
    }

    return this.http.post(`${this.API_URL}/documents/upload`, formData);
  }

  getDocumentsByApplicant(applicantId: number): Observable<any> {
    return this.http.get(`${this.API_URL}/documents/applicant/${applicantId}`);
  }

  getDocumentsByLoan(loanId: number): Observable<any> {
    return this.http.get(`${this.API_URL}/documents/loan/${loanId}`);
  }

  deleteDocument(documentId: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/documents/${documentId}`);
  }

  // ==================== API Calls ====================

  submitApplication(application: LoanApplicationForm): Observable<any> {
    application.status = 'SUBMITTED';
    application.submittedAt = new Date().toISOString();
    
    // Map to comprehensive DTO format for existing applicant
    const comprehensiveDTO = this.mapToComprehensiveDTO(application);
    
    console.log('=== SUBMITTING TO COMPREHENSIVE API ===');
    console.log('Original Application:', application);
    console.log('Mapped DTO:', comprehensiveDTO);
    console.log('API Endpoint:', `${this.API_URL}/loan-applications/submit-for-existing-applicant`);
    
    return this.http.post(`${this.API_URL}/loan-applications/submit-for-existing-applicant`, comprehensiveDTO);
  }

  private mapToComprehensiveDTO(application: LoanApplicationForm): any {
    const applicantDetails = application.applicantDetails;
    const basicDetails = application.basicDetails;
    const financialDetails = application.financialDetails;
    
    console.log('=== MAPPING DATA ===');
    console.log('Financial Details:', financialDetails);
    console.log('Employer Name:', financialDetails?.employerName);
    console.log('Designation:', financialDetails?.designation);
    
    return {
      // Applicant ID
      applicantId: application.applicantId,
      
      // Basic Details (from applicantDetails)
      maritalStatus: applicantDetails?.maritalStatus || null,
      education: null, // Not collected in current form
      nationality: 'Indian', // Default
      panNumber: applicantDetails?.panNumber || '',
      aadhaarNumber: applicantDetails?.aadharNumber || '',
      
      // Employment Details (from financialDetails)
      employerName: financialDetails?.employerName || null,
      designation: financialDetails?.designation || null,
      employmentType: financialDetails?.employmentType?.toLowerCase().replace('_', '-') || 'salaried',
      employmentStartDate: null, // Not collected
      monthlyIncome: financialDetails?.monthlyGrossSalary || financialDetails?.monthlyNetSalary || 0,
      
      // Financial Details
      bankName: financialDetails.bankName,
      accountNumber: financialDetails.accountNumber,
      accountType: financialDetails.accountType?.toLowerCase() || 'savings',
      ifscCode: financialDetails.ifscCode,
      totalCreditLastMonth: null, // Not collected
      totalDebitLastMonth: null, // Not collected
      
      // Property Details (from applicantDetails)
      residenceType: applicantDetails?.residenceType?.toLowerCase().replace(' ', '_') || 'rented',
      propertyOwnership: null, // Not collected
      monthlyRent: null, // Not collected
      yearsAtCurrentAddress: applicantDetails?.yearsAtCurrentAddress || 0,
      propertyValue: null, // Not collected
      propertyType: null, // Not collected
      totalAreaSqft: null, // Not collected
      hasHomeLoan: false, // Default
      
      // Credit History (not collected - using defaults)
      creditScore: null,
      creditBureau: null,
      totalActiveLoans: 0,
      totalOutstandingDebt: financialDetails.existingLoanEMI || 0,
      totalMonthlyEmi: financialDetails.existingLoanEMI || 0,
      creditCardCount: 0,
      paymentHistory: null,
      defaultsCount: 0,
      bankruptcyFiled: false,
      
      // Loan Details (from basicDetails)
      loanType: basicDetails.loanType?.toLowerCase() || 'personal',
      loanAmount: basicDetails.loanAmount,
      tenureMonths: basicDetails.tenure,
      
      // Documents (map to comprehensive format)
      documents: application.documents.map(doc => ({
        docType: this.mapDocumentType(doc.documentType),
        docNumber: null,
        cloudinaryUrl: doc.fileUrl,
        ocrText: null,
        isTampered: false,
        name: applicantDetails ? `${applicantDetails.firstName} ${applicantDetails.lastName}` : null,
        dob: applicantDetails?.dateOfBirth || null,
        gender: applicantDetails?.gender || null,
        address: applicantDetails?.currentAddress || null,
        fatherName: null
      })),
      
      // References (from applicantDetails co-applicant if available)
      references: applicantDetails?.hasCoApplicant ? [{
        referenceName: applicantDetails.coApplicantName || '',
        relationship: applicantDetails.coApplicantRelation?.toLowerCase() || 'family',
        phone: '0000000000', // Placeholder
        email: null,
        address: null,
        occupation: null,
        yearsKnown: 0
      }] : [],
      
      // Dependents (not collected)
      dependents: [],
      
      // Collaterals (from basicDetails if has collateral)
      collaterals: basicDetails.hasCollateral ? [{
        collateralType: this.mapCollateralType(basicDetails.collateralType),
        collateralDescription: basicDetails.collateralType || 'Collateral',
        estimatedValue: basicDetails.collateralValue || 0,
        valuationBy: null,
        ownershipProofUrl: null,
        valuationReportUrl: null
      }] : []
    };
  }

  private mapDocumentType(docType: string): string {
    const mapping: { [key: string]: string } = {
      'AADHAAR': 'aadhaar',
      'PAN': 'pan',
      'PHOTO': 'other',
      'BANK_STATEMENT': 'bank_statement',
      'SALARY_SLIP': 'payslip',
      'PROPERTY_PAPERS': 'other',
      'INCOME_PROOF': 'itr',
      'ADDRESS_PROOF': 'other'
    };
    return mapping[docType] || 'other';
  }

  private mapCollateralType(collateralType?: string): string {
    if (!collateralType) return 'property';
    const type = collateralType.toLowerCase();
    if (type.includes('property') || type.includes('real estate')) return 'property';
    if (type.includes('gold')) return 'gold';
    if (type.includes('vehicle') || type.includes('car')) return 'vehicle';
    if (type.includes('fd') || type.includes('deposit')) return 'fixed_deposit';
    if (type.includes('securities') || type.includes('shares')) return 'securities';
    return 'property';
  }

  saveDraftToServer(application: LoanApplicationForm): Observable<any> {
    return this.http.post(`${this.API_URL}/loan-applications/save-draft`, application);
  }

  getApplication(applicationId: string): Observable<LoanApplicationForm> {
    return this.http.get<LoanApplicationForm>(`${this.API_URL}/loan-applications/${applicationId}`);
  }

  // ==================== Local Storage ====================

  private saveDraft(application: LoanApplicationForm): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(application));
  }

  private loadDraft(): void {
    const draft = localStorage.getItem(this.STORAGE_KEY);
    if (draft) {
      try {
        const application = JSON.parse(draft);
        this.applicationState.next(application);
      } catch (error) {
        console.error('Error loading draft:', error);
      }
    }
  }

  clearDraft(): void {
    localStorage.removeItem(this.STORAGE_KEY);
    this.applicationState.next(null);
  }

  // ==================== Utility Methods ====================

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  validateStep(step: number): boolean {
    const application = this.applicationState.value;
    if (!application) return false;

    switch (step) {
      case 1: // Basic Details
        return !!(
          application.basicDetails.loanAmount > 0 &&
          application.basicDetails.tenure > 0 &&
          application.basicDetails.purpose.trim()
        );
      case 2: // Financial Details
        return !!(
          application.financialDetails.employmentType &&
          application.financialDetails.monthlyGrossSalary &&
          application.financialDetails.bankName &&
          application.financialDetails.accountNumber &&
          application.financialDetails.ifscCode
        );
      case 3: // Documents
        const requiredDocs = this.getRequiredDocuments(application.basicDetails.loanType);
        const requiredTypes = requiredDocs.filter(d => d.required).map(d => d.documentType);
        const uploadedTypes = application.documents.map(d => d.documentType);
        return requiredTypes.every(type => uploadedTypes.includes(type));
      case 4: // Declarations
        return !!(
          application.declarations.kycConsent &&
          application.declarations.creditBureauConsent &&
          application.declarations.termsAccepted &&
          application.declarations.privacyPolicyAccepted
        );
      default:
        return false;
    }
  }

  getStepCompletion(): { [key: number]: boolean } {
    return {
      1: this.validateStep(1),
      2: this.validateStep(2),
      3: this.validateStep(3),
      4: this.validateStep(4)
    };
  }
}
