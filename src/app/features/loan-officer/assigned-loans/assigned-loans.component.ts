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
  
  // Modal state
  showLoanDetailsModal = false;
  selectedLoan: LoanScreeningResponse | null = null;
  detailedLoanData: any = null; // Comprehensive loan details from backend
  loadingDetails = false;

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

  // View loan details in modal
  viewLoanDetails(loan: LoanScreeningResponse): void {
    this.selectedLoan = loan;
    this.showLoanDetailsModal = true;
    this.loadComprehensiveLoanDetails(loan.assignmentId);
  }
  
  // Load comprehensive loan details from backend (all tables)
  loadComprehensiveLoanDetails(assignmentId: number): void {
    this.loadingDetails = true;
    this.error = '';
    
    // Get loanId from selectedLoan
    const loanId = this.selectedLoan?.loanId;
    if (!loanId) {
      this.error = 'Loan ID not found';
      this.loadingDetails = false;
      return;
    }
    
    // Use comprehensive view API to fetch data from all related tables
    this.loanOfficerService.getComprehensiveLoanView(loanId).subscribe({
      next: (data) => {
        this.detailedLoanData = data;
        this.loadingDetails = false;
      },
      error: (err) => {
        console.error('Error loading comprehensive loan details:', err);
        this.error = 'Failed to load detailed information';
        this.loadingDetails = false;
        // Keep modal open with basic data
      }
    });
  }

  // Verify documents - navigate to document verification
  verifyDocuments(assignmentId: number, loanId: number): void {
    this.router.navigate(['/loan-officer/verify-documents', assignmentId], {
      queryParams: { loanId: loanId }
    });
  }

  // Start screening process
  startScreening(assignmentId: number): void {
    this.router.navigate(['/loan-officer/review', assignmentId]);
  }

  // Legacy method - kept for compatibility
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

  closeLoanDetailsModal(): void {
    this.showLoanDetailsModal = false;
    this.selectedLoan = null;
    this.detailedLoanData = null;
  }
  
  // Helper methods for displaying real data
  getApplicantEmail(): string {
    return this.detailedLoanData?.applicantEmail || this.detailedLoanData?.email || 'N/A';
  }
  
  getApplicantPhone(): string {
    return this.detailedLoanData?.applicantPhone || this.detailedLoanData?.phone || 'N/A';
  }
  
  getCreditScore(): number {
    return this.detailedLoanData?.creditScore || this.detailedLoanData?.cibilScore || 0;
  }
  
  getMonthlyIncome(): number {
    return this.detailedLoanData?.monthlyIncome || this.detailedLoanData?.salary || 0;
  }
  
  getDTIRatio(): number {
    if (this.detailedLoanData?.dtiRatio) return this.detailedLoanData.dtiRatio;
    if (this.detailedLoanData?.existingDebt && this.detailedLoanData?.monthlyIncome) {
      return (this.detailedLoanData.existingDebt / this.detailedLoanData.monthlyIncome) * 100;
    }
    return 0;
  }
  
  getEmploymentStatus(): string {
    return this.detailedLoanData?.employmentStatus || this.detailedLoanData?.employmentType || 'N/A';
  }
}
