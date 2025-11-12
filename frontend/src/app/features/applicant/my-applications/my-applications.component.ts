import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ApplicantService, LoanApplication } from '@core/services/applicant.service';

export interface TimelineStage {
  id: string;
  title: string;
  description: string;
  icon: string;
  status: 'completed' | 'current' | 'pending' | 'rejected';
  date?: string;
  details: string;
}

@Component({
  selector: 'app-my-applications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-applications.component.html',
  styleUrl: './my-applications.component.css'
})
export class MyApplicationsComponent implements OnInit {
  applications: LoanApplication[] = [];
  filteredApplications: LoanApplication[] = [];
  loading = false;
  error = '';
  
  // Detail Modal
  showDetailModal = false;
  selectedLoan: any = null;
  loadingDetails = false;
  
  // Draft functionality
  draftGenerated = false;
  
  // Debug functionality
  showDebugInfo = false;
  
  applicantId: number = 0;
  searchQuery = '';
  selectedStatus = '';
  selectedType = '';
  
  statusOptions = ['PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'DISBURSED', 'CLOSED'];
  loanTypes = ['Personal Loan', 'Home Loan', 'Car Loan', 'Education Loan', 'Business Loan'];

  constructor(
    private authService: AuthService,
    private applicantService: ApplicantService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.applicantId = user?.applicantId || 0;
  }

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    this.loading = true;
    this.error = '';

    console.log('Loading applications for applicant ID:', this.applicantId);
    
    this.applicantService.getMyApplications(this.applicantId).subscribe({
      next: (applications) => {
        console.log('Loaded applications:', applications);
        
        // Find and log loan ID 10 specifically
        const loan10 = applications.find(app => app.loanId === 10);
        if (loan10) {
          console.log('🔍 LOAN ID 10 STATUS:', {
            loanId: loan10.loanId,
            loanStatus: loan10.loanStatus,
            status: loan10.status,
            applicationStatus: loan10.applicationStatus,
            reviewedAt: loan10.reviewedAt,
            lastUpdated: loan10.lastUpdated,
            remarks: loan10.remarks,
            interestRate: loan10.interestRate,
            assignedOfficerName: loan10.assignedOfficerName
          });
        } else {
          console.log('❌ Loan ID 10 not found in applications');
        }
        
        this.applications = applications;
        this.filteredApplications = applications;
        this.loading = false;
        this.applyFilters();
        
        // Show success message for manual refresh
        if (!this.isInitialLoad) {
          this.showSuccessMessage('Applications refreshed successfully!');
        }
        this.isInitialLoad = false;
      },
      error: (err) => {
        this.error = 'Failed to load applications';
        console.error('Error loading applications:', err);
        this.loading = false;
      }
    });
  }

  private isInitialLoad = true;
  successMessage = '';
  private showSuccessTimeout: any;

  private showSuccessMessage(message: string): void {
    this.successMessage = message;
    if (this.showSuccessTimeout) {
      clearTimeout(this.showSuccessTimeout);
    }
    this.showSuccessTimeout = setTimeout(() => {
      this.successMessage = '';
    }, 3000);
  }

  applyFilters(): void {
    let filtered = [...this.applications];

    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(app =>
        app.loanType.toLowerCase().includes(query) ||
        app.loanId.toString().includes(query) ||
        app.loanStatus.toLowerCase().includes(query)
      );
    }

    if (this.selectedStatus) {
      filtered = filtered.filter(app => app.loanStatus === this.selectedStatus);
    }

    if (this.selectedType) {
      filtered = filtered.filter(app => app.loanType === this.selectedType);
    }

    this.filteredApplications = filtered;
  }

  viewDetails(loanId: number): void {
    this.loadingDetails = true;
    this.showDetailModal = true;
    this.selectedLoan = null;
    this.draftGenerated = false; // Reset draft state
    
    console.log('🔍 Loading detailed information for Loan ID:', loanId);
    
    this.applicantService.getLoanDetails(loanId).subscribe({
      next: (details) => {
        console.log('📋 Complete Loan Details from DB:', details);
        console.log('📋 Available Fields:', Object.keys(details));
        
        // Log specific sections for debugging (using safe property access)
        console.log('👤 Applicant Info:', {
          name: (details as any).applicantFirstName + ' ' + ((details as any).applicantLastName || ''),
          email: (details as any).applicantEmail,
          mobile: (details as any).applicantMobile,
          pan: (details as any).applicantPan,
          aadhar: (details as any).applicantAadhar
        });
        
        console.log('💼 Employment Info:', {
          type: (details as any).employmentType,
          employer: (details as any).employerName,
          designation: (details as any).designation,
          income: (details as any).monthlyIncome
        });
        
        console.log('🏦 Banking Info:', {
          bank: (details as any).bankName,
          account: (details as any).accountNumber,
          ifsc: (details as any).ifscCode,
          type: (details as any).accountType
        });
        
        this.selectedLoan = details;
        this.loadingDetails = false;
      },
      error: (err) => {
        console.error('❌ Error loading loan details:', err);
        this.error = 'Failed to load loan details from database';
        this.loadingDetails = false;
        this.showDetailModal = false;
      }
    });
  }
  
  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedLoan = null;
    this.draftGenerated = false; // Reset draft state when closing modal
    this.showDebugInfo = false; // Reset debug state when closing modal
  }

  downloadPDF(app: LoanApplication): void {
    this.loading = true;
    this.applicantService.downloadLoanApplicationPDF(app.loanId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `Loan_Application_${app.loanId}_${app.loanType.replace(/\s+/g, '_')}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error downloading PDF:', err);
        this.error = 'Failed to download PDF. Please try again.';
        this.loading = false;
        // Fallback: Generate PDF on frontend
        this.generatePDFOnFrontend(app);
      }
    });
  }

  generatePDFOnFrontend(app: LoanApplication): void {
    // Import jsPDF dynamically or use a service
    // For now, create a simple HTML print version
    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
          <title>Loan Application #${app.loanId}</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 40px; }
            .header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 20px; margin-bottom: 30px; }
            .section { margin-bottom: 20px; }
            .label { font-weight: bold; display: inline-block; width: 200px; }
            .value { display: inline-block; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            th { background-color: #f2f2f2; }
          </style>
        </head>
        <body>
          <div class="header">
            <h1>Loan Application</h1>
            <p>Application ID: #${app.loanId}</p>
            <p>Date: ${this.formatDate(app.submittedAt || app.applicationDate)}</p>
          </div>
          
          <div class="section">
            <h2>Loan Information</h2>
            <p><span class="label">Loan Type:</span> <span class="value">${app.loanType}</span></p>
            <p><span class="label">Loan Amount:</span> <span class="value">₹${app.loanAmount.toLocaleString('en-IN')}</span></p>
            <p><span class="label">Tenure:</span> <span class="value">${app.tenureMonths || app.loanTenure || 'N/A'} months</span></p>
            <p><span class="label">Interest Rate:</span> <span class="value">${app.interestRate || 'N/A'}% p.a.</span></p>
            <p><span class="label">Status:</span> <span class="value">${app.loanStatus}</span></p>
          </div>

          ${app.applicantFirstName ? `
          <div class="section">
            <h2>Applicant Information</h2>
            <p><span class="label">Name:</span> <span class="value">${app.applicantFirstName} ${app.applicantLastName || ''}</span></p>
            <p><span class="label">Email:</span> <span class="value">${app.applicantEmail || 'N/A'}</span></p>
            <p><span class="label">Mobile:</span> <span class="value">${app.applicantMobile || 'N/A'}</span></p>
          </div>
          ` : ''}

          ${app.employmentType ? `
          <div class="section">
            <h2>Employment Details</h2>
            <p><span class="label">Employment Type:</span> <span class="value">${app.employmentType}</span></p>
            <p><span class="label">Employer:</span> <span class="value">${app.employerName || 'N/A'}</span></p>
            <p><span class="label">Monthly Income:</span> <span class="value">₹${(app.monthlyIncome || 0).toLocaleString('en-IN')}</span></p>
          </div>
          ` : ''}

          <div class="section">
            <h2>Application Status</h2>
            <table>
              <tr>
                <th>Status</th>
                <th>Date</th>
              </tr>
              <tr>
                <td>Submitted</td>
                <td>${this.formatDate(app.submittedAt || app.applicationDate)}</td>
              </tr>
              ${app.reviewedAt ? `
              <tr>
                <td>Reviewed</td>
                <td>${this.formatDate(app.reviewedAt)}</td>
              </tr>
              ` : ''}
            </table>
          </div>

          ${app.remarks ? `
          <div class="section">
            <h2>Remarks</h2>
            <p>${app.remarks}</p>
          </div>
          ` : ''}

          <div class="section" style="margin-top: 50px; text-align: center; color: #666; font-size: 12px;">
            <p>This is a computer-generated document. No signature is required.</p>
            <p>Generated on: ${new Date().toLocaleString('en-IN')}</p>
          </div>

          <script>
            window.onload = function() {
              window.print();
            }
          </script>
        </body>
        </html>
      `);
      printWindow.document.close();
    }
  }

  applyForNew(): void {
    this.router.navigate(['/applicant/apply-loan']);
  }

  formatCurrency(amount: number): string {
    return this.applicantService.formatCurrency(amount);
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    return this.applicantService.formatDate(dateString);
  }

  getStatusColor(status: string): string {
    return this.applicantService.getStatusColor(status);
  }

  getFraudStatusColor(status: string | undefined): string {
    if (!status) return 'secondary';
    return this.applicantService.getFraudStatusColor(status);
  }

  getProgressPercentage(status: string): number {
    // Updated to match real backend statuses
    const statusProgress: any = {
      'PENDING': 20,
      'UNDER_REVIEW': 60,
      'APPROVED': 90,
      'DISBURSED': 100,
      'REJECTED': 100,
      'CLOSED': 100
    };
    return statusProgress[status] || 15;
  }

  // Get progress percentage using actual status
  getProgressPercentageForApp(application: LoanApplication): number {
    const actualStatus = this.getActualStatus(application);
    return this.getProgressPercentage(actualStatus);
  }

  // Timeline stages for progress tracking - customer-facing view
  getTimelineStages(application: LoanApplication): TimelineStage[] {
    const actualStatus = this.getActualStatus(application);
    
    const stages: TimelineStage[] = [
      {
        id: 'submitted',
        title: 'Application Submitted',
        description: 'Your loan application has been received',
        icon: 'fa-file-alt',
        status: 'completed',
        date: application.submittedAt || application.applicationDate,
        details: `${application.loanType} application for ${this.formatCurrency(application.loanAmount)} submitted successfully. Application ID: #${application.loanId}`
      },
      {
        id: 'document_verification',
        title: 'Document Verification',
        description: 'Verifying your submitted documents',
        icon: 'fa-file-check',
        status: actualStatus === 'PENDING' ? 'current' : 
                (actualStatus === 'REJECTED' ? 'completed' : 'completed'),
        date: this.getEstimatedDate(application.submittedAt || application.applicationDate, 1),
        details: actualStatus === 'REJECTED' ? 
          'Document verification was completed before application review.' :
          'We are verifying your identity, income, and supporting documents to ensure all requirements are met.'
      },
      {
        id: 'review_process',
        title: 'Loan Review Process',
        description: actualStatus === 'REJECTED' ? 'Application review completed' : 'Comprehensive evaluation in progress',
        icon: 'fa-search',
        status: actualStatus === 'UNDER_REVIEW' ? 'current' : 
                (actualStatus === 'PENDING' ? 'pending' : 'completed'),
        date: actualStatus === 'UNDER_REVIEW' || actualStatus === 'APPROVED' || actualStatus === 'REJECTED' || actualStatus === 'DISBURSED' ? 
              this.getEstimatedDate(application.submittedAt || application.applicationDate, 3) : undefined,
        details: actualStatus === 'UNDER_REVIEW' ? 
          'Your application is being thoroughly reviewed by our loan processing team. This includes eligibility verification, creditworthiness assessment, and policy compliance checks.' :
          actualStatus === 'PENDING' ? 
          'Your application will undergo comprehensive review including eligibility verification and policy compliance.' :
          actualStatus === 'REJECTED' ?
          'Your application underwent comprehensive review including eligibility verification, creditworthiness assessment, and policy compliance checks.' :
          'Loan review process completed successfully.'
      },
      {
        id: 'decision',
        title: this.getDecisionTitle(actualStatus),
        description: this.getDecisionDescription(actualStatus),
        icon: this.getDecisionIcon(actualStatus),
        status: this.getDecisionStatus(actualStatus),
        date: this.getDecisionDate(application, actualStatus),
        details: this.getLoanDecisionDetailsWithActualStatus(application, actualStatus)
      }
    ];

    // Add disbursement stage if approved or disbursed
    if (actualStatus === 'APPROVED' || actualStatus === 'DISBURSED') {
      stages.push({
        id: 'disbursement',
        title: 'Loan Disbursement',
        description: actualStatus === 'DISBURSED' ? 'Funds successfully transferred' : 'Preparing for fund transfer',
        icon: 'fa-university',
        status: actualStatus === 'DISBURSED' ? 'completed' : 'pending',
        date: actualStatus === 'DISBURSED' ? application.lastUpdated : undefined,
        details: this.getDisbursementDetailsWithActualStatus(application, actualStatus)
      });
    }

    return stages;
  }

  // New method to determine stage status based on real backend data
  private getStageStatusFromReal(currentStatus: string, pendingStatuses: string[], completedStatuses: string[]): 'completed' | 'current' | 'pending' | 'rejected' {
    if (currentStatus === 'REJECTED') {
      return 'rejected';
    }
    
    if (completedStatuses.includes(currentStatus)) {
      return 'completed';
    }
    
    if (pendingStatuses.includes(currentStatus)) {
      return 'current';
    }
    
    return 'pending';
  }

  // Get the actual status from the application data
  getActualStatus(application: LoanApplication): string {
    // Debug log to see what status fields are available
    console.log('🔍 Getting actual status for application:', {
      loanId: application.loanId,
      loanStatus: application.loanStatus,
      status: (application as any).status,
      applicationStatus: (application as any).applicationStatus,
      currentStatus: (application as any).currentStatus,
      allFields: Object.keys(application)
    });

    // Check multiple possible status fields from database
    const statusFields = [
      application.loanStatus,
      (application as any).status,
      (application as any).applicationStatus,
      (application as any).currentStatus,
      (application as any).loanApplicationStatus
    ];

    // Look for rejected status first
    for (const statusField of statusFields) {
      if (statusField) {
        const normalizedStatus = statusField.toString().toUpperCase();
        if (normalizedStatus === 'REJECTED' || normalizedStatus === 'DECLINE' || normalizedStatus === 'DECLINED') {
          console.log('✅ Found REJECTED status:', statusField);
          return 'REJECTED';
        }
      }
    }

    // Look for approved status
    for (const statusField of statusFields) {
      if (statusField) {
        const normalizedStatus = statusField.toString().toUpperCase();
        if (normalizedStatus === 'APPROVED' || normalizedStatus === 'APPROVE') {
          console.log('✅ Found APPROVED status:', statusField);
          return 'APPROVED';
        }
      }
    }

    // Look for disbursed status
    for (const statusField of statusFields) {
      if (statusField) {
        const normalizedStatus = statusField.toString().toUpperCase();
        if (normalizedStatus === 'DISBURSED' || normalizedStatus === 'DISBURSEMENT_COMPLETED') {
          console.log('✅ Found DISBURSED status:', statusField);
          return 'DISBURSED';
        }
      }
    }

    // Look for under review status
    for (const statusField of statusFields) {
      if (statusField) {
        const normalizedStatus = statusField.toString().toUpperCase();
        if (normalizedStatus === 'UNDER_REVIEW' || normalizedStatus === 'IN_REVIEW' || normalizedStatus === 'REVIEWING') {
          console.log('✅ Found UNDER_REVIEW status:', statusField);
          return 'UNDER_REVIEW';
        }
      }
    }
    
    // Default fallback
    const defaultStatus = application.loanStatus || 'PENDING';
    console.log('⚠️ Using default status:', defaultStatus);
    return defaultStatus;
  }

  // Get decision date from multiple possible fields
  private getDecisionDate(application: LoanApplication, actualStatus: string): string | undefined {
    // Try multiple possible date fields for decision
    const dateFields = [
      (application as any).reviewedAt,
      (application as any).approvedAt,
      (application as any).rejectedAt,
      (application as any).decisionDate,
      (application as any).processedAt,
      application.lastUpdated,
      (application as any).updatedAt
    ];

    // For rejected applications, specifically look for rejection date
    if (actualStatus === 'REJECTED') {
      const rejectionDate = (application as any).rejectedAt || (application as any).decisionDate;
      if (rejectionDate) {
        console.log('📅 Found rejection date:', rejectionDate);
        return rejectionDate;
      }
    }

    // For approved applications, look for approval date
    if (actualStatus === 'APPROVED') {
      const approvalDate = (application as any).approvedAt || (application as any).decisionDate;
      if (approvalDate) {
        console.log('📅 Found approval date:', approvalDate);
        return approvalDate;
      }
    }

    // Find the first available date
    for (const dateField of dateFields) {
      if (dateField) {
        console.log('📅 Using decision date:', dateField);
        return dateField;
      }
    }

    // If no specific date found and status is not pending/under review, use estimated date
    if (actualStatus !== 'PENDING' && actualStatus !== 'UNDER_REVIEW') {
      return this.getEstimatedDate(application.submittedAt || application.applicationDate, 5);
    }

    return undefined;
  }

  // Generate estimated dates based on submission date
  private getEstimatedDate(submissionDate: string | undefined, daysToAdd: number): string | undefined {
    if (!submissionDate) return undefined;
    
    const date = new Date(submissionDate);
    date.setDate(date.getDate() + daysToAdd);
    return date.toISOString();
  }

  // Get decision title based on status
  private getDecisionTitle(status: string): string {
    switch (status) {
      case 'APPROVED': return 'Loan Approved';
      case 'REJECTED': return 'Application Rejected';
      case 'DISBURSED': return 'Loan Approved & Disbursed';
      default: return 'Decision Pending';
    }
  }

  // Get decision icon based on status
  private getDecisionIcon(status: string): string {
    switch (status) {
      case 'APPROVED': return 'fa-check-circle';
      case 'REJECTED': return 'fa-times-circle';
      case 'DISBURSED': return 'fa-check-double';
      default: return 'fa-clock';
    }
  }

  // Get decision status
  private getDecisionStatus(status: string): 'completed' | 'current' | 'pending' | 'rejected' {
    switch (status) {
      case 'APPROVED':
      case 'DISBURSED':
        return 'completed';
      case 'REJECTED':
        return 'rejected';
      case 'UNDER_REVIEW':
        return 'current';
      default:
        return 'pending';
    }
  }

  // Get decision details from status and application data
  private getDecisionDetailsFromStatus(status: string, application: LoanApplication): string {
    switch (status) {
      case 'APPROVED':
        return `Congratulations! Your ${application.loanType} for ${this.formatCurrency(application.loanAmount)} has passed all screening checks and been approved. You will receive disbursement details shortly.`;
      case 'REJECTED':
        return `Your ${application.loanType} application did not meet our screening criteria at this time. Please contact support for detailed feedback.`;
      case 'DISBURSED':
        return `Your loan amount ${this.formatCurrency(application.loanAmount)} has been successfully disbursed to your account after completing all screening requirements.`;
      case 'UNDER_REVIEW':
        return `Your ${application.loanType} application is currently undergoing comprehensive screening including fraud detection, risk assessment, and eligibility verification.`;
      default:
        return `Your ${application.loanType} application is being processed through our screening system. We'll notify you of any updates.`;
    }
  }

  private getStageStatus(stage: string, currentStatus: string): 'completed' | 'current' | 'pending' | 'rejected' {
    const statusOrder = ['PENDING', 'DOCUMENT_VERIFICATION', 'UNDER_REVIEW', 'CREDIT_EVALUATION', 'UNDERWRITING', 'APPROVED', 'DISBURSED'];
    const stageOrder = ['PENDING', 'DOCUMENT_VERIFICATION', 'CREDIT_EVALUATION', 'UNDERWRITING', 'DECISION', 'DISBURSEMENT'];
    
    if (currentStatus === 'REJECTED') {
      return stage === 'DECISION' ? 'rejected' : statusOrder.indexOf(currentStatus) >= statusOrder.indexOf(stage) ? 'completed' : 'pending';
    }
    
    const currentIndex = statusOrder.indexOf(currentStatus);
    const stageIndex = stageOrder.indexOf(stage.toUpperCase());
    
    if (stageIndex < currentIndex) return 'completed';
    if (stageIndex === currentIndex) return 'current';
    return 'pending';
  }

  private getStageDate(stage: string, application: LoanApplication): string | undefined {
    // This would ideally come from backend with actual stage dates
    // For now, we'll use available dates and estimate others
    switch (stage) {
      case 'DOCUMENT_VERIFICATION':
        return application.submittedAt || application.applicationDate;
      case 'CREDIT_EVALUATION':
        return application.reviewedAt;
      case 'UNDERWRITING':
        return application.reviewedAt;
      default:
        return undefined;
    }
  }

  private getDecisionDescription(status: string): string {
    switch (status) {
      case 'APPROVED': return 'Congratulations! Your loan has been approved';
      case 'REJECTED': return 'Application could not be approved at this time';
      default: return 'Awaiting final decision';
    }
  }

  private getDecisionDetails(status: string): string {
    switch (status) {
      case 'APPROVED': return 'You will receive disbursement details shortly';
      case 'REJECTED': return 'Please contact support for more information';
      default: return 'Our team is reviewing your application';
    }
  }

  // Show timeline modal
  showTimelineModal = false;
  selectedApplicationForTimeline: LoanApplication | null = null;

  viewTimeline(application: LoanApplication): void {
    // Refresh the specific application data before showing timeline
    this.refreshApplicationData(application.loanId).then((updatedApp) => {
      this.selectedApplicationForTimeline = updatedApp || application;
      this.showTimelineModal = true;
    });
  }

  // Refresh specific application data
  private refreshApplicationData(loanId: number): Promise<LoanApplication | null> {
    return new Promise((resolve) => {
      console.log('🔄 Refreshing data for loan ID:', loanId);
      
      this.applicantService.getLoanDetails(loanId).subscribe({
        next: (details) => {
          console.log('✅ Refreshed data for loan ID', loanId, ':', details);
          
          if (loanId === 10) {
            console.log('🎯 LOAN ID 10 DETAILED STATUS:', {
              loanId: details.loanId,
              loanStatus: details.loanStatus,
              status: details.status,
              applicationStatus: details.applicationStatus,
              reviewedAt: details.reviewedAt,
              lastUpdated: details.lastUpdated,
              remarks: details.remarks,
              interestRate: details.interestRate,
              assignedOfficerName: details.assignedOfficerName,
              fullObject: details
            });
          }
          
          // Update the application in the list with fresh data
          const index = this.applications.findIndex(app => app.loanId === loanId);
          if (index !== -1) {
            this.applications[index] = { ...this.applications[index], ...details };
            this.applyFilters();
            console.log('📝 Updated application in list at index:', index);
          }
          resolve(details);
        },
        error: (err) => {
          console.error('❌ Error refreshing application data:', err);
          resolve(null);
        }
      });
    });
  }

  closeTimelineModal(): void {
    this.showTimelineModal = false;
    this.selectedApplicationForTimeline = null;
  }

  getTimelineStageClass(status: string): string {
    switch (status) {
      case 'completed': return 'timeline-completed';
      case 'current': return 'timeline-current';
      case 'rejected': return 'timeline-rejected';
      default: return 'timeline-pending';
    }
  }

  getTimelineIconClass(status: string): string {
    switch (status) {
      case 'completed': return 'text-success';
      case 'current': return 'text-primary';
      case 'rejected': return 'text-danger';
      default: return 'text-muted';
    }
  }

  getCurrentStageText(status: string): string {
    // Customer-friendly stage descriptions
    switch (status) {
      case 'PENDING': return 'Document verification in progress';
      case 'UNDER_REVIEW': return 'Loan review and evaluation in progress';
      case 'APPROVED': return 'Congratulations! Your loan has been approved';
      case 'REJECTED': return 'Application decision communicated';
      case 'DISBURSED': return 'Loan amount successfully disbursed';
      case 'CLOSED': return 'Application process completed';
      default: return 'Processing your application';
    }
  }

  // Get current stage text using actual status
  getCurrentStageTextForApp(application: LoanApplication): string {
    const actualStatus = this.getActualStatus(application);
    return this.getCurrentStageText(actualStatus);
  }

  // Get actual status text for display
  getActualStatusText(application: LoanApplication): string {
    const actualStatus = this.getActualStatus(application);
    switch (actualStatus) {
      case 'APPROVED': return 'APPROVED';
      case 'REJECTED': return 'REJECTED';
      case 'DISBURSED': return 'COMPLETED';
      case 'PENDING': return 'PENDING';
      case 'UNDER_REVIEW': return 'UNDER REVIEW';
      default: return 'PROCESSING';
    }
  }

  // Get actual status color
  getActualStatusColor(application: LoanApplication): string {
    const actualStatus = this.getActualStatus(application);
    switch (actualStatus) {
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'danger';
      case 'DISBURSED': return 'primary';
      case 'PENDING': return 'warning';
      case 'UNDER_REVIEW': return 'info';
      default: return 'secondary';
    }
  }

  // Get loan decision details with EMI calculations and final terms
  getLoanDecisionDetails(application: LoanApplication): string {
    const actualStatus = this.getActualStatus(application);
    return this.getLoanDecisionDetailsWithActualStatus(application, actualStatus);
  }

  // Get loan decision details using actual status
  getLoanDecisionDetailsWithActualStatus(application: LoanApplication, actualStatus: string): string {
    switch (actualStatus) {
      case 'APPROVED':
        return this.getApprovedLoanDetails(application);
      case 'REJECTED':
        return this.getRejectedLoanDetails(application);
      case 'DISBURSED':
        return this.getDisbursedLoanDetails(application);
      case 'UNDER_REVIEW':
        return 'Your loan application is currently under review. Our team is carefully evaluating your eligibility and will notify you of the decision soon.';
      default:
        return 'Your loan application is being processed. You will be notified once the review is complete.';
    }
  }

  // Get disbursement details using actual status
  getDisbursementDetailsWithActualStatus(application: LoanApplication, actualStatus: string): string {
    if (actualStatus === 'DISBURSED') {
      return `Loan amount ${this.formatCurrency(application.loanAmount)} has been successfully credited to your registered bank account. You can start using the funds immediately.`;
    } else {
      return `Your loan documents are being prepared. Once you complete the documentation process, the loan amount ${this.formatCurrency(application.loanAmount)} will be disbursed to your account within 2-3 working days.`;
    }
  }

  // Get detailed approved loan information with EMI calculation
  private getApprovedLoanDetails(application: LoanApplication): string {
    const loanAmount = application.loanAmount;
    const interestRate = application.interestRate || this.getEstimatedInterestRate(application.loanType);
    const tenure = application.tenureMonths || application.loanTenure || this.getDefaultTenure(application.loanType);
    const emi = this.calculateEMI(loanAmount, interestRate, tenure);
    const totalAmount = emi * tenure;
    const totalInterest = totalAmount - loanAmount;

    return `🎉 Congratulations! Your ${application.loanType} has been APPROVED!

📋 LOAN DETAILS:
• Loan Amount: ${this.formatCurrency(loanAmount)}
• Interest Rate: ${interestRate}% per annum
• Loan Tenure: ${tenure} months (${Math.round(tenure/12)} years)
• Monthly EMI: ${this.formatCurrency(emi)}
• Total Amount Payable: ${this.formatCurrency(totalAmount)}
• Total Interest: ${this.formatCurrency(totalInterest)}

📅 NEXT STEPS:
• You will receive loan agreement documents within 24 hours
• Please review and digitally sign the documents
• Funds will be disbursed within 2-3 working days after document completion
• EMI deduction will start from next month

For any queries, please contact our customer support.`;
  }

  // Get rejection details
  private getRejectedLoanDetails(application: LoanApplication): string {
    const rejectionReason = application.remarks || 'Based on our assessment criteria';
    
    return `❌ We regret to inform you that your ${application.loanType} application has been REJECTED.

📋 REASON:
${rejectionReason}

📅 NEXT STEPS:
• You can reapply after 3 months
• Consider improving your credit profile
• Ensure all documents are complete and accurate
• Contact our support team for detailed feedback

We appreciate your interest and encourage you to apply again in the future.`;
  }

  // Get disbursed loan details
  private getDisbursedLoanDetails(application: LoanApplication): string {
    const loanAmount = application.loanAmount;
    const interestRate = application.interestRate || this.getEstimatedInterestRate(application.loanType);
    const tenure = application.tenureMonths || application.loanTenure || this.getDefaultTenure(application.loanType);
    const emi = this.calculateEMI(loanAmount, interestRate, tenure);

    return `✅ Your loan has been successfully DISBURSED!

💰 DISBURSEMENT DETAILS:
• Amount Credited: ${this.formatCurrency(loanAmount)}
• Date of Disbursement: ${this.formatDate(application.lastUpdated || '')}
• Monthly EMI: ${this.formatCurrency(emi)}
• First EMI Date: ${this.getFirstEMIDate()}

📱 LOAN MANAGEMENT:
• Track your EMIs through our mobile app
• Set up auto-debit for hassle-free payments
• Access loan statements and certificates online

Thank you for choosing our services!`;
  }

  // Get disbursement stage details
  getDisbursementDetails(application: LoanApplication): string {
    if (application.loanStatus === 'DISBURSED') {
      return `Loan amount ${this.formatCurrency(application.loanAmount)} has been successfully credited to your registered bank account. You can start using the funds immediately.`;
    } else {
      return `Your loan documents are being prepared. Once you complete the documentation process, the loan amount ${this.formatCurrency(application.loanAmount)} will be disbursed to your account within 2-3 working days.`;
    }
  }

  // Calculate EMI using standard formula
  private calculateEMI(principal: number, annualRate: number, tenureMonths: number): number {
    const monthlyRate = annualRate / (12 * 100);
    const emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)) / 
                (Math.pow(1 + monthlyRate, tenureMonths) - 1);
    return Math.round(emi);
  }

  // Get estimated interest rate based on loan type
  private getEstimatedInterestRate(loanType: string): number {
    const rates: any = {
      'Personal Loan': 12.5,
      'Home Loan': 8.5,
      'Car Loan': 9.5,
      'Education Loan': 10.5,
      'Business Loan': 14.0
    };
    return rates[loanType] || 12.0;
  }

  // Get default tenure based on loan type
  private getDefaultTenure(loanType: string): number {
    const tenures: any = {
      'Personal Loan': 36,
      'Home Loan': 240,
      'Car Loan': 60,
      'Education Loan': 84,
      'Business Loan': 48
    };
    return tenures[loanType] || 36;
  }

  // Get first EMI date (next month)
  private getFirstEMIDate(): string {
    const date = new Date();
    date.setMonth(date.getMonth() + 1);
    date.setDate(5); // EMI typically on 5th of every month
    return this.formatDate(date.toISOString());
  }

  // Generate rejection letter document for rejected loans
  generateRejectionLetterPDF(application: LoanApplication): void {
    const actualStatus = this.getActualStatus(application);
    
    if (actualStatus !== 'REJECTED') {
      alert('Rejection letter is only available for rejected loans.');
      return;
    }

    const rejectionReasons = this.getRejectionReasons(application);
    const nextSteps = this.getNextStepsForRejection(application);

    const letterWindow = window.open('', '_blank');
    if (letterWindow) {
      letterWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
          <title>Loan Application Rejection Letter - ${application.loanId}</title>
          <style>
            body { 
              font-family: 'Times New Roman', serif; 
              padding: 40px; 
              line-height: 1.8;
              color: #333;
            }
            .header { 
              text-align: center; 
              border-bottom: 3px solid #dc2626; 
              padding-bottom: 20px; 
              margin-bottom: 30px; 
            }
            .company-name {
              font-size: 24px;
              font-weight: bold;
              color: #dc2626;
              margin-bottom: 5px;
            }
            .document-title {
              font-size: 18px;
              font-weight: bold;
              margin: 20px 0;
              color: #dc2626;
            }
            .date-ref {
              text-align: right;
              margin: 20px 0;
              font-size: 14px;
            }
            .applicant-details {
              margin: 30px 0;
              padding: 15px;
              background-color: #f9fafb;
              border-left: 4px solid #dc2626;
            }
            .section { 
              margin-bottom: 25px; 
              page-break-inside: avoid;
            }
            .section-title {
              font-size: 16px;
              font-weight: bold;
              color: #dc2626;
              border-bottom: 1px solid #e5e7eb;
              padding-bottom: 5px;
              margin-bottom: 15px;
            }
            .reason-box {
              background-color: #fef2f2;
              border: 2px solid #dc2626;
              border-radius: 8px;
              padding: 20px;
              margin: 20px 0;
            }
            .next-steps-box {
              background-color: #f0f9ff;
              border: 2px solid #2563eb;
              border-radius: 8px;
              padding: 20px;
              margin: 20px 0;
            }
            .info-grid {
              display: grid;
              grid-template-columns: 1fr 1fr;
              gap: 20px;
              margin: 15px 0;
            }
            .info-item {
              display: flex;
              justify-content: space-between;
              padding: 8px 0;
              border-bottom: 1px dotted #ccc;
            }
            .label { 
              font-weight: bold; 
              color: #374151;
            }
            .value { 
              color: #111827;
              font-weight: 500;
            }
            .reasons-list {
              list-style-type: decimal;
              padding-left: 20px;
            }
            .reasons-list li {
              margin-bottom: 10px;
              color: #dc2626;
              font-weight: 500;
            }
            .steps-list {
              list-style-type: disc;
              padding-left: 20px;
            }
            .steps-list li {
              margin-bottom: 8px;
              color: #2563eb;
            }
            .signature-section {
              margin-top: 50px;
              text-align: right;
            }
            .signature-box {
              display: inline-block;
              text-align: center;
              border-top: 2px solid #333;
              padding-top: 10px;
              min-width: 200px;
            }
            .footer {
              margin-top: 40px;
              text-align: center;
              font-size: 12px;
              color: #6b7280;
              border-top: 1px solid #e5e7eb;
              padding-top: 20px;
            }
            .contact-box {
              background-color: #f9fafb;
              border: 1px solid #d1d5db;
              border-radius: 8px;
              padding: 15px;
              margin: 20px 0;
            }
            @media print {
              body { margin: 0; padding: 20px; }
              .no-print { display: none; }
            }
          </style>
        </head>
        <body>
          <div class="header">
            <div class="company-name">LoanMS Financial Services</div>
            <div>Loan Management System</div>
            <div class="document-title">LOAN APPLICATION REJECTION LETTER</div>
          </div>

          <div class="date-ref">
            <div><strong>Date:</strong> ${new Date().toLocaleDateString('en-IN', { 
              year: 'numeric', 
              month: 'long', 
              day: 'numeric' 
            })}</div>
            <div><strong>Reference No:</strong> REJ-${application.loanId}-${new Date().getFullYear()}</div>
          </div>

          <div class="applicant-details">
            <div><strong>To:</strong></div>
            <div>${application.applicantFirstName || 'Dear'} ${application.applicantLastName || 'Applicant'}</div>
            <div>${application.applicantEmail || 'Email on file'}</div>
            <div>${application.applicantMobile || 'Phone on file'}</div>
          </div>

          <div class="section">
            <p><strong>Subject: Loan Application Rejection - Application ID #${application.loanId}</strong></p>
            
            <p>Dear ${application.applicantFirstName || 'Applicant'},</p>
            
            <p>Thank you for your interest in LoanMS Financial Services and for taking the time to apply for a ${application.loanType} with us. We have carefully reviewed your loan application submitted on ${this.formatDate(application.submittedAt || application.applicationDate)}.</p>
            
            <p>After thorough evaluation of your application, we regret to inform you that we are unable to approve your loan request for <strong>${this.formatCurrency(application.loanAmount)}</strong> at this time.</p>
          </div>

          <div class="reason-box">
            <div class="section-title">REASONS FOR REJECTION</div>
            <ol class="reasons-list">
              ${rejectionReasons.map(reason => `<li>${reason}</li>`).join('')}
            </ol>
          </div>

          <div class="section">
            <div class="section-title">APPLICATION DETAILS REVIEWED</div>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">Loan Type:</span>
                <span class="value">${application.loanType}</span>
              </div>
              <div class="info-item">
                <span class="label">Requested Amount:</span>
                <span class="value">${this.formatCurrency(application.loanAmount)}</span>
              </div>
              <div class="info-item">
                <span class="label">Tenure Requested:</span>
                <span class="value">${application.tenureMonths || application.loanTenure || 'N/A'} months</span>
              </div>
              <div class="info-item">
                <span class="label">Application Date:</span>
                <span class="value">${this.formatDate(application.submittedAt || application.applicationDate)}</span>
              </div>
              <div class="info-item">
                <span class="label">Review Date:</span>
                <span class="value">${this.formatDate(application.reviewedAt || new Date().toISOString())}</span>
              </div>
              <div class="info-item">
                <span class="label">Employment Type:</span>
                <span class="value">${application.employmentType || 'Not specified'}</span>
              </div>
            </div>
          </div>

          <div class="next-steps-box">
            <div class="section-title">NEXT STEPS & RECOMMENDATIONS</div>
            <ul class="steps-list">
              ${nextSteps.map(step => `<li>${step}</li>`).join('')}
            </ul>
          </div>

          <div class="section">
            <p>We understand that this news may be disappointing. However, we encourage you to take the recommended steps above and consider reapplying in the future when your financial profile better aligns with our lending criteria.</p>
            
            <p>This decision is based on our current lending policies and risk assessment criteria. It does not reflect your personal character or future creditworthiness.</p>
          </div>

          <div class="contact-box">
            <div class="section-title">NEED ASSISTANCE?</div>
            <p><strong>For detailed feedback or clarification on this decision, please contact:</strong></p>
            <div class="info-grid">
              <div>
                <div><strong>Customer Support:</strong> 1800-123-4567</div>
                <div><strong>Email:</strong> support&#64;loanms.com</div>
              </div>
              <div>
                <div><strong>Loan Advisory:</strong> loans&#64;loanms.com</div>
                <div><strong>Office Hours:</strong> Mon-Fri, 9 AM - 6 PM</div>
              </div>
            </div>
          </div>

          <div class="signature-section">
            <div class="signature-box">
              <div>_________________________</div>
              <div><strong>Loan Review Committee</strong></div>
              <div>LoanMS Financial Services</div>
              <div>Date: ${new Date().toLocaleDateString('en-IN')}</div>
            </div>
          </div>

          <div class="footer">
            <p><strong>LoanMS Financial Services Pvt. Ltd.</strong></p>
            <p>Registered Office: 123 Business District, Financial Center, Mumbai - 400001</p>
            <p>Phone: 1800-123-4567 | Email: support&#64;loanms.com | Website: www.loanms.com</p>
            <p><strong>IMPORTANT:</strong> This is an official communication. Please retain this letter for your records.</p>
            <p>Generated on: ${new Date().toLocaleString('en-IN')}</p>
          </div>

          <div class="no-print" style="position: fixed; top: 20px; right: 20px;">
            <button onclick="window.print()" style="padding: 10px 20px; background: #dc2626; color: white; border: none; border-radius: 5px; cursor: pointer;">
              Print Letter
            </button>
          </div>

          <script>
            // Auto-print when document loads
            window.onload = function() {
              setTimeout(() => {
                window.print();
              }, 1000);
            }
          </script>
        </body>
        </html>
      `);
      letterWindow.document.close();
    }
  }

  // Get rejection reasons based on application data
  private getRejectionReasons(application: LoanApplication): string[] {
    const reasons = [];
    
    // Use actual remarks if available
    if (application.remarks) {
      reasons.push(application.remarks);
    } else {
      // Generate realistic rejection reasons based on loan type and amount
      const loanAmount = application.loanAmount;
      
      if (loanAmount > 1000000) { // Above 10 lakhs
        reasons.push("Requested loan amount exceeds our current lending limits for your profile");
      }
      
      if (!application.monthlyIncome || application.monthlyIncome < loanAmount * 0.02) {
        reasons.push("Monthly income does not meet the minimum requirement for the requested loan amount");
      }
      
      if (application.employmentType === 'UNEMPLOYED' || !application.employmentType) {
        reasons.push("Insufficient employment verification or unstable employment history");
      }
      
      // Default reasons if none above apply
      if (reasons.length === 0) {
        reasons.push("Credit history does not meet our current lending criteria");
        reasons.push("Debt-to-income ratio exceeds acceptable limits");
        reasons.push("Insufficient documentation provided for verification");
      }
    }
    
    return reasons;
  }

  // Get next steps for rejected applications
  private getNextStepsForRejection(application: LoanApplication): string[] {
    return [
      "Improve your credit score by paying all existing debts on time",
      "Increase your monthly income or provide additional income sources",
      "Reduce existing debt obligations to improve debt-to-income ratio",
      "Consider applying for a lower loan amount that better matches your profile",
      "Provide additional documentation to strengthen your application",
      "Wait for 3-6 months before reapplying to allow for financial improvements",
      "Consider adding a co-applicant with strong financial credentials",
      "Consult with our financial advisors for personalized guidance"
    ];
  }

  // Generate loan agreement document for approved loans
  generateLoanAgreementPDF(application: LoanApplication): void {
    const actualStatus = this.getActualStatus(application);
    
    if (actualStatus !== 'APPROVED' && actualStatus !== 'DISBURSED') {
      alert('Loan agreement is only available for approved loans.');
      return;
    }

    const loanAmount = application.loanAmount;
    const interestRate = application.interestRate || this.getEstimatedInterestRate(application.loanType);
    const tenure = application.tenureMonths || application.loanTenure || this.getDefaultTenure(application.loanType);
    const emi = this.calculateEMI(loanAmount, interestRate, tenure);
    const totalAmount = emi * tenure;
    const totalInterest = totalAmount - loanAmount;
    const processingFee = Math.round(loanAmount * 0.01); // 1% processing fee
    const firstEMIDate = this.getFirstEMIDate();

    const agreementWindow = window.open('', '_blank');
    if (agreementWindow) {
      agreementWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
          <title>Loan Agreement - ${application.loanId}</title>
          <style>
            body { 
              font-family: 'Times New Roman', serif; 
              padding: 40px; 
              line-height: 1.6;
              color: #333;
            }
            .header { 
              text-align: center; 
              border-bottom: 3px solid #2563eb; 
              padding-bottom: 20px; 
              margin-bottom: 30px; 
            }
            .company-name {
              font-size: 24px;
              font-weight: bold;
              color: #2563eb;
              margin-bottom: 5px;
            }
            .document-title {
              font-size: 20px;
              font-weight: bold;
              margin: 20px 0;
            }
            .section { 
              margin-bottom: 25px; 
              page-break-inside: avoid;
            }
            .section-title {
              font-size: 16px;
              font-weight: bold;
              color: #2563eb;
              border-bottom: 1px solid #e5e7eb;
              padding-bottom: 5px;
              margin-bottom: 15px;
            }
            .info-grid {
              display: grid;
              grid-template-columns: 1fr 1fr;
              gap: 20px;
              margin: 15px 0;
            }
            .info-item {
              display: flex;
              justify-content: space-between;
              padding: 8px 0;
              border-bottom: 1px dotted #ccc;
            }
            .label { 
              font-weight: bold; 
              color: #374151;
            }
            .value { 
              color: #111827;
              font-weight: 500;
            }
            .highlight-box {
              background-color: #f0f9ff;
              border: 2px solid #2563eb;
              border-radius: 8px;
              padding: 20px;
              margin: 20px 0;
            }
            .terms-list {
              list-style-type: decimal;
              padding-left: 20px;
            }
            .terms-list li {
              margin-bottom: 10px;
            }
            .signature-section {
              margin-top: 50px;
              display: grid;
              grid-template-columns: 1fr 1fr;
              gap: 50px;
            }
            .signature-box {
              border-top: 2px solid #333;
              padding-top: 10px;
              text-align: center;
            }
            .footer {
              margin-top: 40px;
              text-align: center;
              font-size: 12px;
              color: #6b7280;
              border-top: 1px solid #e5e7eb;
              padding-top: 20px;
            }
            @media print {
              body { margin: 0; padding: 20px; }
              .no-print { display: none; }
            }
          </style>
        </head>
        <body>
          <div class="header">
            <div class="company-name">LoanMS Financial Services</div>
            <div>Loan Management System</div>
            <div class="document-title">LOAN AGREEMENT</div>
            <div>Agreement No: LMS-${application.loanId}-${new Date().getFullYear()}</div>
          </div>

          <div class="section">
            <div class="section-title">LOAN DETAILS</div>
            <div class="info-grid">
              <div>
                <div class="info-item">
                  <span class="label">Loan ID:</span>
                  <span class="value">#${application.loanId}</span>
                </div>
                <div class="info-item">
                  <span class="label">Loan Type:</span>
                  <span class="value">${application.loanType}</span>
                </div>
                <div class="info-item">
                  <span class="label">Principal Amount:</span>
                  <span class="value">${this.formatCurrency(loanAmount)}</span>
                </div>
                <div class="info-item">
                  <span class="label">Interest Rate:</span>
                  <span class="value">${interestRate}% per annum</span>
                </div>
              </div>
              <div>
                <div class="info-item">
                  <span class="label">Loan Tenure:</span>
                  <span class="value">${tenure} months (${Math.round(tenure/12)} years)</span>
                </div>
                <div class="info-item">
                  <span class="label">Monthly EMI:</span>
                  <span class="value">${this.formatCurrency(emi)}</span>
                </div>
                <div class="info-item">
                  <span class="label">Total Interest:</span>
                  <span class="value">${this.formatCurrency(totalInterest)}</span>
                </div>
                <div class="info-item">
                  <span class="label">Total Payable:</span>
                  <span class="value">${this.formatCurrency(totalAmount)}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="highlight-box">
            <div class="section-title">EMI SCHEDULE</div>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">First EMI Date:</span>
                <span class="value">${firstEMIDate}</span>
              </div>
              <div class="info-item">
                <span class="label">EMI Deduction:</span>
                <span class="value">5th of every month</span>
              </div>
              <div class="info-item">
                <span class="label">Processing Fee:</span>
                <span class="value">${this.formatCurrency(processingFee)}</span>
              </div>
              <div class="info-item">
                <span class="label">Late Payment Fee:</span>
                <span class="value">₹500 + 2% of EMI</span>
              </div>
            </div>
          </div>

          <div class="section">
            <div class="section-title">BORROWER DETAILS</div>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">Name:</span>
                <span class="value">${application.applicantFirstName || 'N/A'} ${application.applicantLastName || ''}</span>
              </div>
              <div class="info-item">
                <span class="label">Email:</span>
                <span class="value">${application.applicantEmail || 'N/A'}</span>
              </div>
              <div class="info-item">
                <span class="label">Mobile:</span>
                <span class="value">${application.applicantMobile || 'N/A'}</span>
              </div>
              <div class="info-item">
                <span class="label">Employment:</span>
                <span class="value">${application.employmentType || 'N/A'}</span>
              </div>
            </div>
          </div>

          <div class="section">
            <div class="section-title">TERMS AND CONDITIONS</div>
            <ol class="terms-list">
              <li>The borrower agrees to pay the EMI amount of ${this.formatCurrency(emi)} on or before the 5th of every month.</li>
              <li>The loan carries an interest rate of ${interestRate}% per annum on reducing balance method.</li>
              <li>Late payment will attract a penalty of ₹500 plus 2% of the EMI amount.</li>
              <li>The borrower can prepay the loan partially or fully at any time with 30 days notice.</li>
              <li>Prepayment charges: 2% of the outstanding principal amount for prepayment within 12 months.</li>
              <li>The loan is secured/unsecured as per the loan type and documentation provided.</li>
              <li>Default in payment for 3 consecutive months will make the entire loan amount due immediately.</li>
              <li>The lender reserves the right to recover the loan amount through legal means in case of default.</li>
              <li>This agreement is governed by the laws of India and subject to local jurisdiction.</li>
              <li>Any changes to this agreement must be made in writing and signed by both parties.</li>
            </ol>
          </div>

          <div class="signature-section">
            <div class="signature-box">
              <div>BORROWER</div>
              <div style="margin-top: 30px;">_________________________</div>
              <div>${application.applicantFirstName || 'Borrower Name'} ${application.applicantLastName || ''}</div>
              <div>Date: ${new Date().toLocaleDateString('en-IN')}</div>
            </div>
            <div class="signature-box">
              <div>LENDER</div>
              <div style="margin-top: 30px;">_________________________</div>
              <div>Authorized Signatory</div>
              <div>LoanMS Financial Services</div>
              <div>Date: ${new Date().toLocaleDateString('en-IN')}</div>
            </div>
          </div>

          <div class="footer">
            <p><strong>LoanMS Financial Services Pvt. Ltd.</strong></p>
            <p>Registered Office: 123 Business District, Financial Center, Mumbai - 400001</p>
            <p>Phone: 1800-123-4567 | Email: support@loanms.com | Website: www.loanms.com</p>
            <p>This is a computer-generated document. Digital signature applied.</p>
            <p>Generated on: ${new Date().toLocaleString('en-IN')}</p>
          </div>

          <div class="no-print" style="position: fixed; top: 20px; right: 20px;">
            <button onclick="window.print()" style="padding: 10px 20px; background: #2563eb; color: white; border: none; border-radius: 5px; cursor: pointer;">
              Print Document
            </button>
          </div>

          <script>
            // Auto-print when document loads
            window.onload = function() {
              setTimeout(() => {
                window.print();
              }, 1000);
            }
          </script>
        </body>
        </html>
      `);
      agreementWindow.document.close();
    }
  }

  exportToCsv(): void {
    const headers = ['Loan ID', 'Type', 'Amount', 'Tenure', 'Status', 'Fraud Score', 'Applied Date'];
    const rows = this.filteredApplications.map(app => [
      app.loanId,
      app.loanType,
      app.loanAmount,
      (app.tenureMonths || app.loanTenure || 0) + ' months',
      app.loanStatus,
      app.fraudScore || app.riskScore || 0,
      this.formatDate(app.submittedAt || app.applicationDate)
    ]);

    let csvContent = headers.join(',') + '\n';
    rows.forEach(row => {
      csvContent += row.map(field => `"${field}"`).join(',') + '\n';
    });

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `my-applications-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  goBack(): void {
    this.router.navigate(['/applicant/dashboard']);
  }

  // Document handling methods
  getAdditionalDocuments(loan: any): any[] {
    if (!loan.additionalDocuments) return [];
    
    // If additionalDocuments is a string, parse it or return default documents
    if (typeof loan.additionalDocuments === 'string') {
      try {
        return JSON.parse(loan.additionalDocuments);
      } catch {
        // Return some default additional documents based on loan type
        return this.getDefaultAdditionalDocuments(loan.loanType);
      }
    }
    
    // If it's already an array, return it
    if (Array.isArray(loan.additionalDocuments)) {
      return loan.additionalDocuments;
    }
    
    return this.getDefaultAdditionalDocuments(loan.loanType);
  }

  private getDefaultAdditionalDocuments(loanType: string): any[] {
    const commonDocs = [
      { name: 'Photograph', description: 'Passport size photograph' },
      { name: 'Signature Verification', description: 'Signature specimen' }
    ];

    switch (loanType) {
      case 'Home Loan':
        return [
          ...commonDocs,
          { name: 'Property Valuation Report', description: 'Professional valuation' },
          { name: 'NOC from Builder', description: 'No objection certificate' }
        ];
      case 'Car Loan':
        return [
          ...commonDocs,
          { name: 'Vehicle Quotation', description: 'Price quotation from dealer' },
          { name: 'Insurance Documents', description: 'Vehicle insurance papers' }
        ];
      case 'Business Loan':
        return [
          ...commonDocs,
          { name: 'Business Registration', description: 'Business license/registration' },
          { name: 'GST Registration', description: 'GST certificate' },
          { name: 'Financial Statements', description: 'Audited financial statements' }
        ];
      case 'Education Loan':
        return [
          ...commonDocs,
          { name: 'Admission Letter', description: 'College admission confirmation' },
          { name: 'Fee Structure', description: 'Course fee details' },
          { name: 'Academic Records', description: 'Previous academic certificates' }
        ];
      default:
        return commonDocs;
    }
  }

  getDocumentVerificationStatus(loan: any): { text: string, color: string } {
    // Check if loan has document verification status
    if (loan.documentVerificationStatus) {
      switch (loan.documentVerificationStatus.toLowerCase()) {
        case 'verified':
          return { text: 'All Documents Verified', color: 'success' };
        case 'pending':
          return { text: 'Verification Pending', color: 'warning' };
        case 'rejected':
          return { text: 'Documents Rejected', color: 'danger' };
        case 'partial':
          return { text: 'Partially Verified', color: 'info' };
        default:
          return { text: 'Under Review', color: 'primary' };
      }
    }

    // Determine status based on loan status
    switch (loan.loanStatus?.toLowerCase()) {
      case 'approved':
      case 'disbursed':
        return { text: 'All Documents Verified', color: 'success' };
      case 'rejected':
        return { text: 'Documents Rejected', color: 'danger' };
      case 'under_review':
        return { text: 'Under Verification', color: 'warning' };
      default:
        return { text: 'Verification Pending', color: 'info' };
    }
  }

  // Draft functionality methods
  generateApplicationDraft(loan: any): void {
    // Debug: Log the loan object to see available fields
    console.log('🔍 LOAN OBJECT FOR DRAFT:', loan);
    console.log('🔍 AVAILABLE FIELDS:', Object.keys(loan));
    
    // Try to fetch additional applicant data if needed
    this.enrichLoanDataForDraft(loan);
    
    // Simulate draft generation process
    setTimeout(() => {
      this.draftGenerated = true;
    }, 1000);
  }

  private enrichLoanDataForDraft(loan: any): void {
    // If we have minimal data, try to get more from current user session
    const currentUser = this.authService.currentUserValue;
    
    if (currentUser) {
      // Enrich with current user data if loan data is missing
      if (!loan.applicantEmail && currentUser.email) {
        loan.applicantEmail = currentUser.email;
      }
      if (!loan.applicantFirstName && currentUser.firstName) {
        loan.applicantFirstName = currentUser.firstName;
      }
      if (!loan.applicantLastName && currentUser.lastName) {
        loan.applicantLastName = currentUser.lastName;
      }
    }
    
    console.log('🔄 ENRICHED LOAN DATA:', loan);
  }

  downloadApplicationDraft(loan: any): void {
    // Debug: Log the loan object to see available fields
    console.log('🔍 DOWNLOADING DRAFT FOR LOAN:', loan);
    
    // Generate comprehensive application draft PDF
    const draftWindow = window.open('', '_blank');
    if (draftWindow) {
      draftWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
          <title>Loan Application Draft - ${loan.loanId}</title>
          <style>
            body { 
              font-family: 'Times New Roman', serif; 
              line-height: 1.6; 
              margin: 0; 
              padding: 20px; 
              color: #333;
            }
            .header { 
              text-align: center; 
              border-bottom: 3px solid #2563eb; 
              padding-bottom: 20px; 
              margin-bottom: 30px; 
            }
            .company-logo { 
              font-size: 28px; 
              font-weight: bold; 
              color: #2563eb; 
              margin-bottom: 5px; 
            }
            .company-tagline { 
              font-size: 14px; 
              color: #666; 
              font-style: italic; 
            }
            .draft-title { 
              font-size: 24px; 
              font-weight: bold; 
              color: #1f2937; 
              margin: 20px 0; 
              text-align: center;
            }
            .section { 
              margin-bottom: 25px; 
              padding: 15px; 
              border: 1px solid #e5e7eb; 
              border-radius: 8px; 
            }
            .section-title { 
              font-size: 18px; 
              font-weight: bold; 
              color: #2563eb; 
              margin-bottom: 15px; 
              border-bottom: 2px solid #2563eb; 
              padding-bottom: 5px; 
            }
            .info-row { 
              display: flex; 
              margin-bottom: 8px; 
            }
            .info-label { 
              font-weight: bold; 
              width: 200px; 
              color: #374151; 
            }
            .info-value { 
              flex: 1; 
              color: #1f2937; 
            }
            .footer { 
              margin-top: 40px; 
              text-align: center; 
              font-size: 12px; 
              color: #666; 
              border-top: 1px solid #e5e7eb; 
              padding-top: 20px; 
            }
            .draft-watermark {
              position: fixed;
              top: 50%;
              left: 50%;
              transform: translate(-50%, -50%) rotate(-45deg);
              font-size: 72px;
              color: rgba(37, 99, 235, 0.1);
              font-weight: bold;
              z-index: -1;
              pointer-events: none;
            }
            @media print {
              body { margin: 0; }
              .no-print { display: none; }
            }
          </style>
        </head>
        <body>
          <div class="draft-watermark">DRAFT</div>
          
          <div class="header">
            <div class="company-logo">LoanMS Financial Services</div>
            <div class="company-tagline">Your Trusted Financial Partner</div>
          </div>

          <div class="draft-title">LOAN APPLICATION DRAFT</div>

          <div class="section">
            <div class="section-title">Application Summary</div>
            <div class="info-row">
              <div class="info-label">Application ID:</div>
              <div class="info-value">DRAFT-${loan.loanId}-${new Date().getFullYear()}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Loan Type:</div>
              <div class="info-value">${loan.loanType}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Loan Amount:</div>
              <div class="info-value">₹${loan.loanAmount?.toLocaleString()}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Tenure:</div>
              <div class="info-value">${loan.tenureMonths || loan.loanTenure} months</div>
            </div>
            <div class="info-row">
              <div class="info-label">Interest Rate:</div>
              <div class="info-value">${loan.interestRate}% per annum</div>
            </div>
            <div class="info-row">
              <div class="info-label">Draft Generated:</div>
              <div class="info-value">${new Date().toLocaleDateString()}</div>
            </div>
          </div>

          <div class="section">
            <div class="section-title">Personal Information</div>
            <div class="info-row">
              <div class="info-label">Full Name:</div>
              <div class="info-value">${this.getApplicantName(loan)}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Email:</div>
              <div class="info-value">${this.getApplicantEmail(loan)}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Mobile:</div>
              <div class="info-value">${this.getApplicantMobile(loan)}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Date of Birth:</div>
              <div class="info-value">${this.getApplicantDOB(loan)}</div>
            </div>
            <div class="info-row">
              <div class="info-label">PAN Number:</div>
              <div class="info-value">${this.getApplicantPAN(loan)}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Aadhar Number:</div>
              <div class="info-value">${this.getApplicantAadhar(loan)}</div>
            </div>
          </div>

          <div class="section">
            <div class="section-title">Employment Details</div>
            <div class="info-row">
              <div class="info-label">Employment Type:</div>
              <div class="info-value">${this.getEmploymentDetails(loan).type}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Employer Name:</div>
              <div class="info-value">${this.getEmploymentDetails(loan).employer}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Designation:</div>
              <div class="info-value">${this.getEmploymentDetails(loan).designation}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Monthly Income:</div>
              <div class="info-value">₹${this.getEmploymentDetails(loan).income > 0 ? this.getEmploymentDetails(loan).income.toLocaleString() : 'Not Provided'}</div>
            </div>
          </div>

          <div class="section">
            <div class="section-title">Banking Information</div>
            <div class="info-row">
              <div class="info-label">Bank Name:</div>
              <div class="info-value">${this.getBankingDetails(loan).bankName}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Account Number:</div>
              <div class="info-value">${this.getBankingDetails(loan).accountNumber}</div>
            </div>
            <div class="info-row">
              <div class="info-label">IFSC Code:</div>
              <div class="info-value">${this.getBankingDetails(loan).ifscCode}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Account Type:</div>
              <div class="info-value">${this.getBankingDetails(loan).accountType}</div>
            </div>
          </div>

          <div class="section">
            <div class="section-title">Loan Calculations</div>
            <div class="info-row">
              <div class="info-label">Principal Amount:</div>
              <div class="info-value">₹${loan.loanAmount?.toLocaleString()}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Interest Rate:</div>
              <div class="info-value">${loan.interestRate}% per annum</div>
            </div>
            <div class="info-row">
              <div class="info-label">Loan Tenure:</div>
              <div class="info-value">${loan.tenureMonths || loan.loanTenure} months</div>
            </div>
            <div class="info-row">
              <div class="info-label">Estimated EMI:</div>
              <div class="info-value">₹${this.calculateEMI(loan.loanAmount, loan.interestRate, loan.tenureMonths || loan.loanTenure).toLocaleString()}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Total Payable:</div>
              <div class="info-value">₹${(this.calculateEMI(loan.loanAmount, loan.interestRate, loan.tenureMonths || loan.loanTenure) * (loan.tenureMonths || loan.loanTenure)).toLocaleString()}</div>
            </div>
          </div>

          <div class="section">
            <div class="section-title">Documents Checklist</div>
            <div style="margin-bottom: 10px;"><strong>Identity Documents:</strong></div>
            <div style="margin-left: 20px; margin-bottom: 5px;">✓ Aadhar Card</div>
            <div style="margin-left: 20px; margin-bottom: 5px;">✓ PAN Card</div>
            <div style="margin-left: 20px; margin-bottom: 10px;">✓ Passport Size Photographs</div>
            
            <div style="margin-bottom: 10px;"><strong>Income Documents:</strong></div>
            <div style="margin-left: 20px; margin-bottom: 5px;">✓ Salary Slips (Last 3 months)</div>
            <div style="margin-left: 20px; margin-bottom: 5px;">✓ Bank Statements (Last 6 months)</div>
            <div style="margin-left: 20px; margin-bottom: 10px;">✓ Form 16 / ITR</div>
            
            <div style="margin-bottom: 10px;"><strong>Employment Documents:</strong></div>
            <div style="margin-left: 20px; margin-bottom: 5px;">✓ Employment Letter</div>
            <div style="margin-left: 20px; margin-bottom: 5px;">✓ Experience Certificate</div>
          </div>

          <div class="section">
            <div class="section-title">Important Notes</div>
            <div style="margin-bottom: 10px;">• This is a draft document for your reference only</div>
            <div style="margin-bottom: 10px;">• Final loan terms may vary based on credit assessment</div>
            <div style="margin-bottom: 10px;">• All documents must be original and verified</div>
            <div style="margin-bottom: 10px;">• Processing fee and other charges apply as per bank policy</div>
            <div style="margin-bottom: 10px;">• Loan approval is subject to credit verification and bank policies</div>
          </div>

          <div class="footer">
            <p><strong>LoanMS Financial Services</strong></p>
            <p>Email: support@loanms.com | Phone: 1800-123-4567</p>
            <p>This is a system-generated draft document. Please contact us for any clarifications.</p>
            <p>Generated on: ${new Date().toLocaleString()}</p>
          </div>

          <script>
            window.onload = function() {
              setTimeout(() => {
                window.print();
              }, 1000);
            }
          </script>
        </body>
        </html>
      `);
      draftWindow.document.close();
    }
  }

  previewApplicationDraft(loan: any): void {
    // Open draft in new window for preview (without auto-print)
    const previewWindow = window.open('', '_blank');
    if (previewWindow) {
      previewWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
          <title>Loan Application Draft Preview - ${loan.loanId}</title>
          <style>
            body { 
              font-family: 'Times New Roman', serif; 
              line-height: 1.6; 
              margin: 0; 
              padding: 20px; 
              color: #333;
              background-color: #f9fafb;
            }
            .container {
              max-width: 800px;
              margin: 0 auto;
              background: white;
              padding: 30px;
              border-radius: 8px;
              box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            }
            .header { 
              text-align: center; 
              border-bottom: 3px solid #2563eb; 
              padding-bottom: 20px; 
              margin-bottom: 30px; 
            }
            .company-logo { 
              font-size: 28px; 
              font-weight: bold; 
              color: #2563eb; 
              margin-bottom: 5px; 
            }
            .draft-title { 
              font-size: 24px; 
              font-weight: bold; 
              color: #1f2937; 
              margin: 20px 0; 
              text-align: center;
            }
            .preview-note {
              background: #fef3c7;
              border: 1px solid #f59e0b;
              padding: 15px;
              border-radius: 8px;
              margin-bottom: 20px;
              text-align: center;
            }
            .section { 
              margin-bottom: 25px; 
              padding: 15px; 
              border: 1px solid #e5e7eb; 
              border-radius: 8px; 
            }
            .section-title { 
              font-size: 18px; 
              font-weight: bold; 
              color: #2563eb; 
              margin-bottom: 15px; 
              border-bottom: 2px solid #2563eb; 
              padding-bottom: 5px; 
            }
            .info-row { 
              display: flex; 
              margin-bottom: 8px; 
            }
            .info-label { 
              font-weight: bold; 
              width: 200px; 
              color: #374151; 
            }
            .info-value { 
              flex: 1; 
              color: #1f2937; 
            }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="preview-note">
              <strong>📋 DRAFT PREVIEW</strong><br>
              This is a preview of your loan application draft. Use the print function to save as PDF.
            </div>
            
            <div class="header">
              <div class="company-logo">LoanMS Financial Services</div>
            </div>

            <div class="draft-title">LOAN APPLICATION DRAFT</div>

            <div class="section">
              <div class="section-title">Application Summary</div>
              <div class="info-row">
                <div class="info-label">Loan Type:</div>
                <div class="info-value">${loan.loanType}</div>
              </div>
              <div class="info-row">
                <div class="info-label">Loan Amount:</div>
                <div class="info-value">₹${loan.loanAmount?.toLocaleString()}</div>
              </div>
              <div class="info-row">
                <div class="info-label">Estimated EMI:</div>
                <div class="info-value">₹${this.calculateEMI(loan.loanAmount, loan.interestRate, loan.tenureMonths || loan.loanTenure).toLocaleString()}</div>
              </div>
            </div>
          </div>
        </body>
        </html>
      `);
      previewWindow.document.close();
    }
  }

  emailApplicationDraft(loan: any): void {
    // Simulate email functionality
    alert(`Draft for Loan Application ${loan.loanId} will be emailed to ${loan.applicantEmail || 'your registered email address'}.`);
  }

  // Helper methods to get applicant data from different possible field names
  private getApplicantName(loan: any): string {
    // Try different field name combinations
    const firstName = loan.applicantFirstName || loan.firstName || loan.applicant?.firstName || '';
    const middleName = loan.applicantMiddleName || loan.middleName || loan.applicant?.middleName || '';
    const lastName = loan.applicantLastName || loan.lastName || loan.applicant?.lastName || '';
    
    const fullName = `${firstName} ${middleName} ${lastName}`.trim();
    
    // Fallback to current user info if available
    const currentUser = this.authService.currentUserValue;
    const fallbackName = currentUser ? `${currentUser.firstName || ''} ${currentUser.lastName || ''}`.trim() : '';
    
    // Also try common field variations
    const nameVariations = loan.name || loan.fullName || loan.applicantName || loan.customerName;
    
    return fullName || nameVariations || fallbackName || 'Information Available in Database';
  }

  private getApplicantEmail(loan: any): string {
    const currentUser = this.authService.currentUserValue;
    const emailVariations = loan.applicantEmail || loan.email || loan.emailAddress || loan.applicant?.email || loan.contactEmail;
    return emailVariations || currentUser?.email || 'Available in Database';
  }

  private getApplicantMobile(loan: any): string {
    const mobileVariations = loan.applicantMobile || loan.mobile || loan.phone || loan.phoneNumber || 
                           loan.contactNumber || loan.applicant?.mobile || loan.applicant?.phone;
    return mobileVariations || 'Available in Database';
  }

  private getApplicantDOB(loan: any): string {
    const dobVariations = loan.applicantDateOfBirth || loan.dateOfBirth || loan.dob || 
                         loan.birthDate || loan.applicant?.dateOfBirth;
    return dobVariations ? this.formatDate(dobVariations) : 'Available in Database';
  }

  private getApplicantPAN(loan: any): string {
    const panVariations = loan.applicantPan || loan.pan || loan.panNumber || loan.panCard || loan.applicant?.pan;
    return panVariations || 'Available in Database';
  }

  private getApplicantAadhar(loan: any): string {
    const aadharVariations = loan.applicantAadhar || loan.aadhar || loan.aadharNumber || 
                           loan.aadhaarNumber || loan.applicant?.aadhar;
    return aadharVariations || 'Available in Database';
  }

  private getEmploymentDetails(loan: any): any {
    return {
      type: loan.employmentType || loan.employment?.type || loan.jobType || 'Available in Database',
      employer: loan.employerName || loan.employer || loan.companyName || loan.employment?.employer || 'Available in Database',
      designation: loan.designation || loan.position || loan.jobTitle || loan.employment?.designation || 'Available in Database',
      income: loan.monthlyIncome || loan.income || loan.salary || loan.employment?.monthlyIncome || 0
    };
  }

  private getBankingDetails(loan: any): any {
    return {
      bankName: loan.bankName || loan.bank?.name || loan.bankDetails?.name || 'Available in Database',
      accountNumber: loan.accountNumber || loan.bank?.accountNumber || loan.bankDetails?.accountNumber || 'Available in Database',
      ifscCode: loan.ifscCode || loan.bank?.ifscCode || loan.bankDetails?.ifscCode || 'Available in Database',
      accountType: loan.accountType || loan.bank?.accountType || loan.bankDetails?.accountType || 'Available in Database'
    };
  }

  // Debug methods
  toggleDebugInfo(): void {
    this.showDebugInfo = !this.showDebugInfo;
  }

  getFormattedLoanData(loan: any): string {
    if (!loan) return 'No data available';
    
    try {
      return JSON.stringify(loan, null, 2);
    } catch (error) {
      return 'Error formatting data: ' + error;
    }
  }

  // Generate dummy loan ID in LNID format
  generateDummyLoanId(loanId: number): string {
    // Generate a consistent dummy ID based on the actual loan ID
    const baseNumber = 100000 + (loanId % 900000); // Ensures 6-digit number
    return `LNID${baseNumber.toString().padStart(6, '0')}`;
  }

  // Get tenure display with fallback
  getTenureDisplay(app: LoanApplication): string {
    const tenure = app.tenureMonths || app.loanTenure || 12; // Default to 12 months if not available
    return tenure.toString();
  }

  // Get loan type icon
  getLoanTypeIcon(loanType: string): string {
    const iconMap: { [key: string]: string } = {
      'Personal Loan': 'fa-user',
      'PERSONAL': 'fa-user',
      'Home Loan': 'fa-home',
      'HOME': 'fa-home',
      'Car Loan': 'fa-car',
      'VEHICLE': 'fa-car',
      'Business Loan': 'fa-briefcase',
      'BUSINESS': 'fa-briefcase',
      'Education Loan': 'fa-graduation-cap',
      'EDUCATION': 'fa-graduation-cap',
      'Gold Loan': 'fa-coins',
      'GOLD': 'fa-coins'
    };
    return iconMap[loanType] || 'fa-money-bill-wave';
  }
}
