import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@environments/environment';

export interface LoanApplication {
  loanId: number;
  applicantId?: number;
  loanType: string;
  loanAmount: number;
  tenureMonths?: number;
  loanTenure?: number;
  interestRate?: number;
  monthlyIncome?: number;
  employmentType?: string;
  employerName?: string;
  status?: string;
  loanStatus: string;
  applicationStatus?: string;
  submittedAt?: string;
  reviewedAt?: string;
  applicationDate?: string;
  lastUpdated?: string;
  riskScore?: number;
  fraudScore?: number;
  fraudStatus?: string;
  assignedOfficerId?: number;
  assignedOfficerName?: string;
  remarks?: string;
  // Additional fields from backend
  applicantFirstName?: string;
  applicantLastName?: string;
  applicantEmail?: string;
  applicantMobile?: string;
}

export interface ApplicantProfile {
  applicantId: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string;
  dob: string;
  gender: string;
  address: string;
  city: string;
  state: string;
  country: string;
  isApproved: boolean;
  isEmailVerified: boolean;
  approvalStatus: string;
  createdAt: string;
}

export interface DashboardStats {
  totalApplications: number;
  pendingApplications: number;
  approvedApplications: number;
  rejectedApplications: number;
  totalLoanAmount: number;
  averageFraudScore: number;
  recentApplications: LoanApplication[];
}

export interface PreQualificationStatus {
  isEligible: boolean;
  status: 'ELIGIBLE' | 'NEEDS_DOCUMENTS' | 'NOT_ELIGIBLE' | 'PENDING_VERIFICATION';
  message: string;
  requiredDocuments: string[];
  estimatedMaxLoan: number;
  creditScore?: number;
  verifiedFields: {
    email: boolean;
    phone: boolean;
    identity: boolean;
    income: boolean;
    employment: boolean;
  };
}

export interface LoanType {
  id: string;
  name: string;
  description: string;
  icon: string;
  minAmount: number;
  maxAmount: number;
  minTenure: number;
  maxTenure: number;
  interestRateMin: number;
  interestRateMax: number;
  requiredDocuments: string[];
  features: string[];
  eligibilityCriteria: string[];
  processingTime: string;
  color: string;
}

export interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  priority: string;
  isRead: boolean;
  createdAt: string;
  actionUrl?: string;
}

export interface DocumentResubmissionNotification {
  notificationId: number;
  loanId: number;
  assignmentId: number;
  title: string;
  message: string;
  type: string; // 'DOCUMENT_REQUEST'
  priority: string; // 'HIGH', 'MEDIUM', 'LOW'
  status: string; // 'PENDING', 'READ', 'RESOLVED'
  requestedBy: string;
  requestedAt: string;
  dueDate: string;
  readAt?: string;
  resolvedAt?: string;
  requestedDocuments: string[]; // Array of document types
}

export interface ApplicationProgress {
  loanId: number;
  currentStep: number;
  totalSteps: number;
  steps: {
    stepNumber: number;
    stepName: string;
    status: 'COMPLETED' | 'IN_PROGRESS' | 'PENDING' | 'FAILED';
    completedAt?: string;
    description: string;
  }[];
  overallProgress: number;
  estimatedCompletion?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApplicantService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Get applicant profile
  getApplicantProfile(applicantId: number): Observable<ApplicantProfile> {
    return this.http.get<ApplicantProfile>(`${this.API_URL}/loan-applications/applicant/${applicantId}`);
  }

  // Update applicant profile
  updateApplicantProfile(applicantId: number, profileData: any): Observable<any> {
    return this.http.put(`${this.API_URL}/profile/applicant/${applicantId}`, profileData);
  }

  // Get applicant's loan applications
  getMyApplications(applicantId: number): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(`${this.API_URL}/loan-applications/applicant/${applicantId}/loans`);
  }

  // Get specific loan details
  getLoanDetails(loanId: number): Observable<LoanApplication> {
    return this.http.get<LoanApplication>(`${this.API_URL}/loan-applications/loan/${loanId}`);
  }

  // Download loan application as PDF
  downloadLoanApplicationPDF(loanId: number): Observable<Blob> {
    return this.http.get(`${this.API_URL}/loan-applications/loan/${loanId}/download-pdf`, {
      responseType: 'blob'
    });
  }

  // Submit loan application
  submitLoanApplication(applicationData: any): Observable<any> {
    return this.http.post(`${this.API_URL}/loan-applications/submit-complete`, applicationData);
  }

  // Get dashboard statistics (calculated from applications)
  getDashboardStats(applicantId: number): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.API_URL}/loan-applications/applicant/${applicantId}/stats`);
  }

  // Calculate stats locally from applications
  calculateStats(applications: LoanApplication[]): DashboardStats {
    const totalApplications = applications.length;
    const pendingApplications = applications.filter(app => 
      app.loanStatus === 'PENDING' || app.loanStatus === 'UNDER_REVIEW'
    ).length;
    const approvedApplications = applications.filter(app => 
      app.loanStatus === 'APPROVED'
    ).length;
    const rejectedApplications = applications.filter(app => 
      app.loanStatus === 'REJECTED'
    ).length;
    const totalLoanAmount = applications
      .filter(app => app.loanStatus === 'APPROVED')
      .reduce((sum, app) => sum + app.loanAmount, 0);
    const averageFraudScore = applications.length > 0
      ? applications.reduce((sum, app) => sum + (app.fraudScore || 0), 0) / applications.length
      : 0;
    const recentApplications = applications
      .sort((a, b) => {
        const dateA = new Date(b.submittedAt || b.applicationDate || 0).getTime();
        const dateB = new Date(a.submittedAt || a.applicationDate || 0).getTime();
        return dateA - dateB;
      })
      .slice(0, 5);

    return {
      totalApplications,
      pendingApplications,
      approvedApplications,
      rejectedApplications,
      totalLoanAmount,
      averageFraudScore,
      recentApplications
    };
  }

  // Format currency
  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  // Format date
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  // Get status color class
  getStatusColor(status: string): string {
    const colors: any = {
      'PENDING': 'warning',
      'UNDER_REVIEW': 'info',
      'APPROVED': 'success',
      'REJECTED': 'danger',
      'DISBURSED': 'success',
      'CLOSED': 'secondary'
    };
    return colors[status] || 'secondary';
  }

  // Get fraud status color
  getFraudStatusColor(status: string): string {
    const colors: any = {
      'LOW_RISK': 'success',
      'MEDIUM_RISK': 'warning',
      'HIGH_RISK': 'danger',
      'FLAGGED': 'danger',
      'CLEARED': 'success'
    };
    return colors[status] || 'secondary';
  }

  // Get pre-qualification status
  getPreQualificationStatus(applicantId: number): Observable<PreQualificationStatus> {
    return this.http.get<PreQualificationStatus>(`${this.API_URL}/applicant/${applicantId}/pre-qualification`);
  }

  // Get all loan types
  getLoanTypes(): LoanType[] {
    return [
      {
        id: 'PERSONAL',
        name: 'Personal Loan',
        description: 'Flexible personal loans for any purpose - medical, education, travel, or emergencies',
        icon: 'fa-user',
        minAmount: 50000,
        maxAmount: 2000000,
        minTenure: 12,
        maxTenure: 60,
        interestRateMin: 10.5,
        interestRateMax: 18.0,
        requiredDocuments: ['Identity Proof', 'Address Proof', 'Income Proof', 'Bank Statements (6 months)'],
        features: ['Quick approval', 'Minimal documentation', 'Flexible repayment', 'No collateral required'],
        eligibilityCriteria: ['Age: 21-60 years', 'Minimum income: ₹25,000/month', 'Credit score: 650+', 'Employment: 2+ years'],
        processingTime: '2-3 business days',
        color: '#3b82f6'
      },
      {
        id: 'HOME',
        name: 'Home Loan',
        description: 'Realize your dream of owning a home with our competitive home loan rates',
        icon: 'fa-home',
        minAmount: 500000,
        maxAmount: 50000000,
        minTenure: 60,
        maxTenure: 300,
        interestRateMin: 8.5,
        interestRateMax: 11.0,
        requiredDocuments: ['Property documents', 'Identity Proof', 'Income Proof', 'Bank Statements (12 months)', 'Property valuation report'],
        features: ['Low interest rates', 'Long tenure options', 'Tax benefits', 'Balance transfer facility'],
        eligibilityCriteria: ['Age: 23-65 years', 'Minimum income: ₹50,000/month', 'Credit score: 700+', 'Property value verification'],
        processingTime: '7-10 business days',
        color: '#10b981'
      },
      {
        id: 'VEHICLE',
        name: 'Vehicle Loan',
        description: 'Drive your dream car or bike with our easy vehicle financing options',
        icon: 'fa-car',
        minAmount: 100000,
        maxAmount: 5000000,
        minTenure: 12,
        maxTenure: 84,
        interestRateMin: 9.0,
        interestRateMax: 14.0,
        requiredDocuments: ['Identity Proof', 'Address Proof', 'Income Proof', 'Vehicle quotation', 'Bank Statements (6 months)'],
        features: ['Up to 90% financing', 'Quick processing', 'Flexible EMI options', 'Insurance included'],
        eligibilityCriteria: ['Age: 21-65 years', 'Minimum income: ₹30,000/month', 'Credit score: 650+', 'Valid driving license'],
        processingTime: '3-5 business days',
        color: '#f59e0b'
      },
      {
        id: 'EDUCATION',
        name: 'Education Loan',
        description: 'Invest in your future with our comprehensive education loan solutions',
        icon: 'fa-graduation-cap',
        minAmount: 100000,
        maxAmount: 10000000,
        minTenure: 60,
        maxTenure: 180,
        interestRateMin: 9.5,
        interestRateMax: 13.5,
        requiredDocuments: ['Admission letter', 'Fee structure', 'Identity Proof', 'Income Proof', 'Academic records', 'Co-applicant documents'],
        features: ['Moratorium period', 'Tax benefits', 'Covers tuition & living expenses', 'Flexible repayment'],
        eligibilityCriteria: ['Age: 18-35 years', 'Admission to recognized institution', 'Co-applicant required', 'Academic performance'],
        processingTime: '5-7 business days',
        color: '#8b5cf6'
      },
      {
        id: 'BUSINESS',
        name: 'Business Loan',
        description: 'Grow your business with our tailored business financing solutions',
        icon: 'fa-briefcase',
        minAmount: 200000,
        maxAmount: 20000000,
        minTenure: 12,
        maxTenure: 120,
        interestRateMin: 11.0,
        interestRateMax: 16.0,
        requiredDocuments: ['Business registration', 'GST returns', 'ITR (2 years)', 'Bank Statements (12 months)', 'Business plan', 'Financial statements'],
        features: ['Working capital support', 'Equipment financing', 'Business expansion', 'Overdraft facility'],
        eligibilityCriteria: ['Business vintage: 2+ years', 'Annual turnover: ₹10 lakhs+', 'Credit score: 700+', 'Profitable operations'],
        processingTime: '7-14 business days',
        color: '#ef4444'
      }
    ];
  }

  // Get notifications from API
  getNotifications(applicantId: number): Observable<DocumentResubmissionNotification[]> {
    const url = `${this.API_URL}/applicant/${applicantId}/notifications`;
    console.log('Making API call to get all notifications:', url);
    return this.http.get<DocumentResubmissionNotification[]>(url);
  }

  // Get document resubmission requests (filter notifications by type)
  getDocumentResubmissionRequests(applicantId: number): Observable<DocumentResubmissionNotification[]> {
    // First try to get all notifications and filter on frontend
    const url = `${this.API_URL}/applicant/${applicantId}/notifications`;
    console.log('Making API call to get all notifications for filtering:', url);
    return this.http.get<DocumentResubmissionNotification[]>(url).pipe(
      map((notifications: DocumentResubmissionNotification[]) => {
        console.log('All notifications received:', notifications);
        const documentRequests = notifications.filter(notif => 
          notif.type === 'DOCUMENT_REQUEST' && 
          notif.resolvedAt == null && 
          notif.status !== 'RESOLVED'
        );
        console.log('Filtered document resubmission requests:', documentRequests);
        return documentRequests;
      })
    );
  }

  // Mark notification as read
  markNotificationAsRead(notificationId: number): Observable<any> {
    const url = `${this.API_URL}/applicant/notification/${notificationId}/read`;
    console.log('Making API call to mark notification as read:', url);
    return this.http.put(url, {});
  }

  // Mark notification as resolved (dismiss)
  markNotificationAsResolved(notificationId: number): Observable<any> {
    const url = `${this.API_URL}/applicant/notification/${notificationId}/resolve`;
    console.log('Making API call to mark notification as resolved:', url);
    return this.http.put(url, {});
  }


  // Get application progress
  getApplicationProgress(loanId: number): Observable<ApplicationProgress> {
    return this.http.get<ApplicationProgress>(`${this.API_URL}/loan-applications/${loanId}/progress`);
  }

  // Calculate pre-qualification locally (mock for now)
  calculatePreQualification(profile: ApplicantProfile, applications: LoanApplication[]): PreQualificationStatus {
    const verifiedFields = {
      email: profile.isEmailVerified,
      phone: !!profile.phone,
      identity: profile.isApproved,
      income: applications.some(app => (app.monthlyIncome || 0) > 0),
      employment: applications.some(app => app.employmentType && app.employerName)
    };

    const verifiedCount = Object.values(verifiedFields).filter(v => v).length;
    const totalFields = Object.keys(verifiedFields).length;
    
    let status: 'ELIGIBLE' | 'NEEDS_DOCUMENTS' | 'NOT_ELIGIBLE' | 'PENDING_VERIFICATION';
    let message: string;
    let requiredDocuments: string[] = [];
    let estimatedMaxLoan = 0;

    if (verifiedCount === totalFields) {
      status = 'ELIGIBLE';
      message = 'Congratulations! You are pre-qualified for a loan.';
      estimatedMaxLoan = 2000000;
    } else if (verifiedCount >= 3) {
      status = 'NEEDS_DOCUMENTS';
      message = 'Please complete your profile and upload required documents to proceed.';
      if (!verifiedFields.email) requiredDocuments.push('Email Verification');
      if (!verifiedFields.phone) requiredDocuments.push('Phone Verification');
      if (!verifiedFields.identity) requiredDocuments.push('Identity Proof');
      if (!verifiedFields.income) requiredDocuments.push('Income Proof');
      if (!verifiedFields.employment) requiredDocuments.push('Employment Details');
      estimatedMaxLoan = 500000;
    } else if (verifiedCount >= 1) {
      status = 'PENDING_VERIFICATION';
      message = 'Your profile is under verification. Please complete all required fields.';
      requiredDocuments = ['Identity Proof', 'Address Proof', 'Income Proof'];
      estimatedMaxLoan = 0;
    } else {
      status = 'NOT_ELIGIBLE';
      message = 'Please complete your profile to check eligibility.';
      requiredDocuments = ['Identity Proof', 'Address Proof', 'Income Proof', 'Employment Details'];
      estimatedMaxLoan = 0;
    }

    return {
      isEligible: status === 'ELIGIBLE',
      status,
      message,
      requiredDocuments,
      estimatedMaxLoan,
      verifiedFields
    };
  }
}
