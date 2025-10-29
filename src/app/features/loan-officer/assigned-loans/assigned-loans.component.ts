import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { LoanOfficerService, LoanScreeningResponse } from '@core/services/loan-officer.service';

@Component({
  selector: 'app-assigned-loans',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assigned-loans.component.html',
  styleUrl: './assigned-loans.component.css'
})
export class AssignedLoansComponent implements OnInit {
  officerId: number = 0;
  loading = false;
  error = '';

  loans: LoanScreeningResponse[] = [];
  filteredLoans: LoanScreeningResponse[] = [];

  // Filters
  searchQuery = '';
  statusFilter = '';
  riskFilter = '';
  loanTypeFilter = '';

  statusOptions = ['PENDING', 'ASSIGNED', 'IN_PROGRESS', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'ESCALATED', 'ESCALATED_TO_COMPLIANCE'];
  riskOptions = ['LOW', 'MEDIUM', 'HIGH'];
  loanTypes = ['Personal Loan', 'Home Loan', 'Car Loan', 'Education Loan', 'Business Loan'];
  
  // View mode
  viewMode: 'table' | 'card' = 'table';

  constructor(
    private authService: AuthService,
    private loanOfficerService: LoanOfficerService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.officerId = user?.officerId || user?.userId || 0;
  }

  ngOnInit(): void {
    this.loadAssignedLoans();
  }

  loadAssignedLoans(): void {
    this.loading = true;
    this.error = '';

    this.loanOfficerService.getAssignedLoans(this.officerId).subscribe({
      next: (data) => {
        this.loans = data;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading assigned loans:', err);
        this.error = 'Failed to load assigned loans. Please try again.';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredLoans = this.loans.filter(loan => {
      const matchesSearch = !this.searchQuery || 
        loan.applicantName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        loan.loanId.toString().includes(this.searchQuery) ||
        loan.applicantId.toString().includes(this.searchQuery);

      const matchesStatus = !this.statusFilter || loan.status === this.statusFilter;
      const matchesRisk = !this.riskFilter || loan.riskLevel === this.riskFilter;
      const matchesLoanType = !this.loanTypeFilter || loan.loanType === this.loanTypeFilter;

      return matchesSearch && matchesStatus && matchesRisk && matchesLoanType;
    });
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.statusFilter = '';
    this.riskFilter = '';
    this.loanTypeFilter = '';
    this.applyFilters();
  }

  reviewLoan(assignmentId: number): void {
    this.router.navigate(['/loan-officer/review', assignmentId]);
  }

  exportToCSV(): void {
    const headers = ['Assignment ID', 'Loan ID', 'Applicant Name', 'Loan Type', 'Amount', 'Risk Level', 'Status', 'Assigned Date'];
    const csvData = this.filteredLoans.map(loan => [
      loan.assignmentId,
      loan.loanId,
      loan.applicantName,
      loan.loanType,
      loan.loanAmount,
      loan.riskLevel,
      loan.status,
      loan.assignedAt
    ]);

    const csv = [headers, ...csvData].map(row => row.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `assigned-loans-${new Date().getTime()}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  formatCurrency(amount: number): string {
    return this.loanOfficerService.formatCurrency(amount);
  }

  formatDate(dateString: string): string {
    return this.loanOfficerService.formatDate(dateString);
  }

  getRiskColor(riskLevel: string): string {
    return this.loanOfficerService.getRiskColor(riskLevel);
  }

  getStatusColor(status: string): string {
    return this.loanOfficerService.getStatusColor(status);
  }

  toggleView(mode: 'table' | 'card'): void {
    this.viewMode = mode;
  }
}
