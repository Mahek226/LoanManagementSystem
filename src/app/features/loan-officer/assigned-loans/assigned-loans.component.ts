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

  // Tab management
  activeTab: 'assigned' | 'past' = 'assigned';

  // Data for both tabs
  assignedLoans: LoanScreeningResponse[] = [];
  pastLoans: LoanScreeningResponse[] = [];
  filteredLoans: LoanScreeningResponse[] = [];

  // Pagination
  currentPage = 1;
  itemsPerPage = 5;
  totalItems = 0;
  totalPages = 0;
  paginatedLoans: LoanScreeningResponse[] = [];

  // Filters
  searchQuery = '';
  statusFilter = '';
  riskFilter = '';
  loanTypeFilter = '';

  // Status options for different tabs
  assignedStatusOptions = ['PENDING', 'ASSIGNED', 'IN_PROGRESS', 'UNDER_REVIEW', 'ESCALATED', 'ESCALATED_TO_COMPLIANCE'];
  pastStatusOptions = ['APPROVED', 'REJECTED'];
  riskOptions = ['LOW', 'MEDIUM', 'HIGH'];
  loanTypes = ['Personal Loan', 'Home Loan', 'Car Loan', 'Education Loan', 'Business Loan'];
  
  // View mode
  viewMode: 'table' | 'card' = 'table';
  
  // Modal state
  showLoanDetailsModal = false;
  selectedLoan: LoanScreeningResponse | null = null;
  detailedLoanData: any = null; // Comprehensive loan details from backend
  loadingDetails = false;
  
  // Document verification modal state
  showDocVerificationModal = false;
  documentVerificationStatus: any = null;
  selectedLoanForVerification: any = null;

  // Make Math available in template
  Math = Math;

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
        console.log('Loaded loan data:', data.length, 'loans');
        
        // Separate loans into assigned and past categories
        this.assignedLoans = data.filter(loan => 
          !['APPROVED', 'REJECTED'].includes(loan.status)
        );
        this.pastLoans = data.filter(loan => 
          ['APPROVED', 'REJECTED'].includes(loan.status)
        );
        
        console.log('Data separated:', {
          total: data.length,
          assigned: this.assignedLoans.length,
          past: this.pastLoans.length
        });
        
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
    // Get the appropriate data source based on active tab
    const sourceLoans = this.activeTab === 'assigned' ? this.assignedLoans : this.pastLoans;
    
    console.log('Applying filters:', {
      activeTab: this.activeTab,
      sourceLoansCount: sourceLoans.length,
      assignedLoansCount: this.assignedLoans.length,
      pastLoansCount: this.pastLoans.length
    });
    
    this.filteredLoans = sourceLoans.filter((loan: LoanScreeningResponse) => {
      const matchesSearch = !this.searchQuery || 
        loan.applicantName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        loan.loanId.toString().includes(this.searchQuery) ||
        loan.applicantId.toString().includes(this.searchQuery);

      const matchesStatus = !this.statusFilter || loan.status === this.statusFilter;
      const matchesRisk = !this.riskFilter || loan.riskLevel === this.riskFilter;
      const matchesLoanType = !this.loanTypeFilter || loan.loanType === this.loanTypeFilter;

      return matchesSearch && matchesStatus && matchesRisk && matchesLoanType;
    });
    
    console.log('Filtered loans:', this.filteredLoans.length);
    
    // Apply pagination
    this.applyPagination();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.statusFilter = '';
    this.riskFilter = '';
    this.loanTypeFilter = '';
    this.currentPage = 1;
    this.applyFilters();
  }

  // Tab management
  switchTab(tab: 'assigned' | 'past'): void {
    this.activeTab = tab;
    this.currentPage = 1;
    this.clearFilters();
  }

  // Pagination methods
  applyPagination(): void {
    this.totalItems = this.filteredLoans.length;
    this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);
    
    // Ensure current page is valid
    if (this.currentPage > this.totalPages && this.totalPages > 0) {
      this.currentPage = this.totalPages;
    }
    if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.paginatedLoans = this.filteredLoans.slice(startIndex, endIndex);
    
    console.log('Pagination applied:', {
      totalItems: this.totalItems,
      totalPages: this.totalPages,
      currentPage: this.currentPage,
      itemsPerPage: this.itemsPerPage,
      paginatedLoans: this.paginatedLoans.length,
      startIndex,
      endIndex
    });
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.applyPagination();
    }
  }

  nextPage(): void {
    this.goToPage(this.currentPage + 1);
  }

  prevPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  changeItemsPerPage(items: number): void {
    this.itemsPerPage = items;
    this.currentPage = 1;
    this.applyPagination();
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(1, this.currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxVisiblePages - 1);
    
    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }

  // Get status options based on active tab
  getStatusOptions(): string[] {
    return this.activeTab === 'assigned' ? this.assignedStatusOptions : this.pastStatusOptions;
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


  // Start screening process or view results if already screened
  startScreening(assignmentId: number): void {
    // Search in both assigned and past loans
    const allLoans = [...this.assignedLoans, ...this.pastLoans];
    const loan = allLoans.find((l: LoanScreeningResponse) => l.assignmentId === assignmentId);
    
    if (!loan) {
      this.error = 'Loan not found';
      return;
    }
    
    // If loan is already processed/screened, navigate to results view
    if (loan.status === 'APPROVED' || loan.status === 'REJECTED' || loan.status === 'ESCALATED_TO_COMPLIANCE') {
      this.router.navigate(['/loan-officer/review', assignmentId], { 
        queryParams: { viewMode: 'results' } 
      });
      return;
    }
    
    // Check document verification status first
    this.checkDocumentVerificationStatus(loan);
  }

  // Check document verification status before proceeding to screening
  checkDocumentVerificationStatus(loan: any): void {
    this.loading = true;
    this.error = '';
    
    console.log('Checking document verification status for loan:', loan.loanId);
    
    this.loanOfficerService.getDocumentVerificationStatus(loan.loanId).subscribe({
      next: (status) => {
        this.loading = false;
        console.log('Document verification status response:', status);
        
        if (status && status.documentsVerified === true) {
          // All documents are verified, proceed directly to screening
          console.log('Documents verified, navigating to screening tab');
          this.router.navigate(['/loan-officer/review', loan.assignmentId], {
            queryParams: { tab: 'verification' }
          });
        } else {
          // Documents not verified, show document verification modal
          console.log('Documents not verified, showing modal');
          this.showDocumentVerificationModal(loan, status);
        }
      },
      error: (err) => {
        console.error('Error checking document verification status:', err);
        console.log('API call failed, checking if we should bypass verification...');
        this.loading = false;
        
        // For now, let's bypass the document verification check and go directly to screening
        // This is a temporary solution until the backend API is properly implemented
        console.log('Bypassing document verification, going directly to screening');
        this.router.navigate(['/loan-officer/review', loan.assignmentId], {
          queryParams: { tab: 'verification' }
        });
      }
    });
  }

  // Show document verification modal
  showDocumentVerificationModal(loan: any, status: any): void {
    this.selectedLoanForVerification = loan;
    this.documentVerificationStatus = status;
    this.showDocVerificationModal = true;
  }

  // Close document verification modal
  closeDocumentVerificationModal(): void {
    this.showDocVerificationModal = false;
    this.selectedLoanForVerification = null;
    this.documentVerificationStatus = null;
  }

  // Proceed to document verification page
  proceedToDocumentVerification(): void {
    if (this.selectedLoanForVerification) {
      this.router.navigate(['/loan-officer/verify-documents', this.selectedLoanForVerification.assignmentId], {
        queryParams: { loanId: this.selectedLoanForVerification.loanId }
      });
      this.closeDocumentVerificationModal();
    }
  }
  
  // Check if loan is already screened
  isLoanScreened(loan: any): boolean {
    return loan.status === 'APPROVED' || loan.status === 'REJECTED' || loan.status === 'ESCALATED_TO_COMPLIANCE';
  }

  // Legacy method - kept for compatibility
  reviewLoan(assignmentId: number): void {
    this.router.navigate(['/loan-officer/review', assignmentId]);
  }

  exportToCSV(): void {
    const headers = ['Assignment ID', 'Loan ID', 'Applicant Name', 'Loan Type', 'Amount', 'Risk Level', 'Status', 'Assigned Date'];
    const csvData = this.filteredLoans.map((loan: LoanScreeningResponse) => [
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
