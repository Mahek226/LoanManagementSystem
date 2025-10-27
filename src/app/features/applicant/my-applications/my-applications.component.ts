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
    this.router.navigate(['/applicant/applications', loanId]);
  }

  applyForNew(): void {
    this.router.navigate(['/applicant/apply-loan']);
  }

  formatCurrency(amount: number): string {
    return this.applicantService.formatCurrency(amount);
  }

  formatDate(dateString: string): string {
    return this.applicantService.formatDate(dateString);
  }

  getStatusColor(status: string): string {
    return this.applicantService.getStatusColor(status);
  }

  getFraudStatusColor(status: string): string {
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
      app.loanTenure + ' months',
      app.loanStatus,
      app.fraudScore,
      this.formatDate(app.applicationDate)
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
