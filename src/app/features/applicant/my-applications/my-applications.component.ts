import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ApplicantService, LoanApplication } from '@core/services/applicant.service';

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

    this.applicantService.getMyApplications(this.applicantId).subscribe({
      next: (applications) => {
        this.applications = applications;
        this.filteredApplications = applications;
        this.loading = false;
        this.applyFilters();
      },
      error: (err) => {
        this.error = 'Failed to load applications';
        console.error('Error loading applications:', err);
        this.loading = false;
      }
    });
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
    
    this.applicantService.getLoanDetails(loanId).subscribe({
      next: (details) => {
        console.log('Loan Details:', details);
        this.selectedLoan = details;
        this.loadingDetails = false;
      },
      error: (err) => {
        console.error('Error loading details:', err);
        this.error = 'Failed to load loan details';
        this.loadingDetails = false;
        this.showDetailModal = false;
      }
    });
  }
  
  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedLoan = null;
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
    const statusProgress: any = {
      'PENDING': 25,
      'UNDER_REVIEW': 50,
      'APPROVED': 100,
      'REJECTED': 100,
      'DISBURSED': 100,
      'CLOSED': 100
    };
    return statusProgress[status] || 0;
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
}
