import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface EligibilityReport {
  reportId: number;
  loanId: number;
  applicantId: number;
  applicantName: string;
  loanAmount: number;
  loanType: string;
  
  // Eligibility Assessment
  isEligible: boolean;
  eligibilityScore: number;
  recommendedAction: string; // APPROVE, REJECT, MANUAL_REVIEW
  
  // Risk Analysis
  overallRiskLevel: string;
  riskScore: number;
  fraudRiskLevel: string;
  creditRiskLevel: string;
  
  // Document Verification
  documentsVerified: boolean;
  totalDocuments: number;
  verifiedDocuments: number;
  rejectedDocuments: number;
  
  // Financial Assessment
  monthlyIncome: number;
  existingLoans: number;
  creditScore: number;
  debtToIncomeRatio: number;
  
  // Fraud Screening
  fraudCheckCompleted: boolean;
  fraudTags: string[];
  
  // Officer Details
  reviewedBy: string;
  reviewedAt: string;
  officerRemarks: string;
  
  // Recommendations
  recommendations: string[];
  conditions: string[];
  
  generatedAt: string;
}

@Component({
  selector: 'app-loan-eligibility-report',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './loan-eligibility-report.component.html',
  styleUrl: './loan-eligibility-report.component.css'
})
export class LoanEligibilityReportComponent {
  @Input() loanId!: number;
  @Input() assignmentId!: number;
  
  report: EligibilityReport | null = null;
  loading = false;
  error = '';
  showReport = false;

  constructor(private http: HttpClient) {}

  generateReport(): void {
    this.loading = true;
    this.error = '';

    this.http.post<EligibilityReport>(
      `${environment.apiUrl}/loan-officer/loan/${this.loanId}/generate-eligibility-report`,
      { assignmentId: this.assignmentId }
    ).subscribe({
      next: (report) => {
        this.report = report;
        this.showReport = true;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error generating report:', err);
        this.error = err.error?.message || 'Failed to generate eligibility report';
        this.loading = false;
      }
    });
  }

  downloadReport(): void {
    if (!this.report) return;

    const reportContent = this.generateReportHTML();
    const blob = new Blob([reportContent], { type: 'text/html' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `Loan_Eligibility_Report_${this.loanId}_${new Date().getTime()}.html`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  printReport(): void {
    window.print();
  }

  private generateReportHTML(): string {
    if (!this.report) return '';

    return `
<!DOCTYPE html>
<html>
<head>
  <title>Loan Eligibility Report - ${this.report.loanId}</title>
  <style>
    body { font-family: Arial, sans-serif; padding: 20px; }
    .header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 10px; }
    .section { margin: 20px 0; }
    .section-title { font-weight: bold; font-size: 18px; color: #0d6efd; margin-bottom: 10px; }
    table { width: 100%; border-collapse: collapse; }
    td { padding: 8px; border: 1px solid #ddd; }
    .label { font-weight: bold; width: 40%; background-color: #f8f9fa; }
    .badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; }
    .badge-success { background-color: #d4edda; color: #155724; }
    .badge-danger { background-color: #f8d7da; color: #721c24; }
    .badge-warning { background-color: #fff3cd; color: #856404; }
  </style>
</head>
<body>
  <div class="header">
    <h1>Loan Eligibility Report</h1>
    <p>Report ID: ${this.report.reportId} | Generated: ${new Date(this.report.generatedAt).toLocaleString()}</p>
  </div>

  <div class="section">
    <div class="section-title">Applicant Information</div>
    <table>
      <tr><td class="label">Applicant Name</td><td>${this.report.applicantName}</td></tr>
      <tr><td class="label">Loan ID</td><td>${this.report.loanId}</td></tr>
      <tr><td class="label">Loan Type</td><td>${this.report.loanType}</td></tr>
      <tr><td class="label">Loan Amount</td><td>₹${this.report.loanAmount.toLocaleString('en-IN')}</td></tr>
    </table>
  </div>

  <div class="section">
    <div class="section-title">Eligibility Assessment</div>
    <table>
      <tr><td class="label">Eligible</td><td>${this.report.isEligible ? 'Yes' : 'No'}</td></tr>
      <tr><td class="label">Eligibility Score</td><td>${this.report.eligibilityScore}/100</td></tr>
      <tr><td class="label">Recommended Action</td><td>${this.report.recommendedAction}</td></tr>
      <tr><td class="label">Overall Risk Level</td><td>${this.report.overallRiskLevel}</td></tr>
    </table>
  </div>

  <div class="section">
    <div class="section-title">Financial Assessment</div>
    <table>
      <tr><td class="label">Monthly Income</td><td>₹${this.report.monthlyIncome.toLocaleString('en-IN')}</td></tr>
      <tr><td class="label">Credit Score</td><td>${this.report.creditScore}</td></tr>
      <tr><td class="label">Debt-to-Income Ratio</td><td>${this.report.debtToIncomeRatio}%</td></tr>
      <tr><td class="label">Existing Loans</td><td>${this.report.existingLoans}</td></tr>
    </table>
  </div>

  <div class="section">
    <div class="section-title">Document Verification</div>
    <table>
      <tr><td class="label">Total Documents</td><td>${this.report.totalDocuments}</td></tr>
      <tr><td class="label">Verified</td><td>${this.report.verifiedDocuments}</td></tr>
      <tr><td class="label">Rejected</td><td>${this.report.rejectedDocuments}</td></tr>
    </table>
  </div>

  <div class="section">
    <div class="section-title">Officer Review</div>
    <table>
      <tr><td class="label">Reviewed By</td><td>${this.report.reviewedBy}</td></tr>
      <tr><td class="label">Reviewed At</td><td>${new Date(this.report.reviewedAt).toLocaleString()}</td></tr>
      <tr><td class="label">Remarks</td><td>${this.report.officerRemarks}</td></tr>
    </table>
  </div>
</body>
</html>
    `;
  }

  formatCurrency(amount: number): string {
    return '₹' + amount.toLocaleString('en-IN');
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getEligibilityColor(): string {
    if (!this.report) return 'secondary';
    return this.report.isEligible ? 'success' : 'danger';
  }

  getRiskColor(riskLevel: string): string {
    switch (riskLevel) {
      case 'LOW': return 'success';
      case 'MEDIUM': return 'warning';
      case 'HIGH': return 'danger';
      default: return 'secondary';
    }
  }

  getScoreColor(score: number): string {
    if (score >= 70) return 'success';
    if (score >= 40) return 'warning';
    return 'danger';
  }
}
