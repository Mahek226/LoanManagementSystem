import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, LoanApplication, LoanProgressTimeline } from '@core/services/admin.service';

@Component({
  selector: 'app-loans',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loans.component.html',
  styleUrl: './loans.component.css'
})
export class LoansComponent implements OnInit {
  loans: LoanApplication[] = [];
  filteredLoans: LoanApplication[] = [];
  loading = false;
  error = '';

  // Filters
  searchQuery = '';
  statusFilter = '';
  loanTypeFilter = '';

  statusOptions = ['pending', 'approved', 'rejected', 'under_review', 'escalated'];
  loanTypes = ['Personal Loan', 'Home Loan', 'Car Loan', 'Education Loan', 'Business Loan'];

  // Detail view
  selectedLoan: LoanApplication | null = null;
  loanProgress: LoanProgressTimeline | null = null;
  showDetailModal = false;
  loadingProgress = false;

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadLoans();
  }

  loadLoans(): void {
    this.loading = true;
    this.error = '';

    this.adminService.getAllLoanApplications(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.loans = response.content;
        this.totalElements = response.totalElements;
        this.filteredLoans = [...this.loans];
        this.loading = false;
        this.applyFilters();
      },
      error: (err) => {
        console.error('Error loading loans:', err);
        this.error = 'Failed to load loan applications. Please try again.';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredLoans = this.loans.filter(loan => {
      const matchesSearch = !this.searchQuery || 
        loan.applicantName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        loan.id.toString().includes(this.searchQuery);
      
      const matchesStatus = !this.statusFilter || 
        loan.status.toLowerCase() === this.statusFilter.toLowerCase();
      
      const matchesLoanType = !this.loanTypeFilter || 
        loan.loanType === this.loanTypeFilter;

      return matchesSearch && matchesStatus && matchesLoanType;
    });
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.statusFilter = '';
    this.loanTypeFilter = '';
    this.applyFilters();
  }

  viewLoanDetails(loan: LoanApplication): void {
    this.selectedLoan = loan;
    this.showDetailModal = true;
    this.loadLoanProgress(loan.id);
  }

  loadLoanProgress(loanId: number): void {
    this.loadingProgress = true;
    this.loanProgress = null;

    this.adminService.getLoanProgressTimeline(loanId).subscribe({
      next: (progress) => {
        this.loanProgress = progress;
        this.loadingProgress = false;
      },
      error: (err) => {
        console.error('Error loading loan progress:', err);
        this.loadingProgress = false;
        // Create mock progress for demonstration
        this.createMockProgress(loanId);
      }
    });
  }

  createMockProgress(loanId: number): void {
    // Fallback mock data if backend endpoint doesn't exist yet
    if (this.selectedLoan) {
      this.loanProgress = {
        loanId: loanId,
        applicantId: this.selectedLoan.applicantId,
        applicantName: this.selectedLoan.applicantName,
        loanType: this.selectedLoan.loanType,
        loanAmount: this.selectedLoan.requestedAmount,
        currentStatus: this.selectedLoan.status,
        appliedAt: this.selectedLoan.appliedDate,
        events: [
          {
            eventId: 1,
            eventType: 'APPLICATION_SUBMITTED',
            eventStatus: 'COMPLETED',
            performedBy: this.selectedLoan.applicantName,
            performedByRole: 'APPLICANT',
            action: 'Submitted loan application',
            remarks: `Applied for ${this.selectedLoan.loanType}`,
            timestamp: this.selectedLoan.appliedDate
          }
        ]
      };
    }
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedLoan = null;
    this.loanProgress = null;
  }

  getStatusBadgeClass(status: string): string {
    const statusMap: any = {
      'pending': 'bg-warning',
      'approved': 'bg-success',
      'rejected': 'bg-danger',
      'under_review': 'bg-info',
      'escalated': 'bg-secondary'
    };
    return statusMap[status.toLowerCase()] || 'bg-secondary';
  }

  getEventIcon(eventType: string): string {
    const iconMap: any = {
      'APPLICATION_SUBMITTED': 'fa-file-alt',
      'ASSIGNED_TO_OFFICER': 'fa-user-tie',
      'OFFICER_REVIEW': 'fa-search',
      'ESCALATED': 'fa-arrow-up',
      'COMPLIANCE_REVIEW': 'fa-user-shield',
      'APPROVED': 'fa-check-circle',
      'REJECTED': 'fa-times-circle'
    };
    return iconMap[eventType] || 'fa-circle';
  }

  getEventColor(eventType: string): string {
    const colorMap: any = {
      'APPLICATION_SUBMITTED': 'primary',
      'ASSIGNED_TO_OFFICER': 'info',
      'OFFICER_REVIEW': 'warning',
      'ESCALATED': 'secondary',
      'COMPLIANCE_REVIEW': 'info',
      'APPROVED': 'success',
      'REJECTED': 'danger'
    };
    return colorMap[eventType] || 'secondary';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR'
    }).format(amount);
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  nextPage(): void {
    if ((this.currentPage + 1) * this.pageSize < this.totalElements) {
      this.currentPage++;
      this.loadLoans();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadLoans();
    }
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadLoans();
  }

  onPageSizeChange(): void {
    this.currentPage = 0; // Reset to first page when page size changes
    this.loadLoans();
  }

  getPageNumbers(): number[] {
    const totalPages = Math.ceil(this.totalElements / this.pageSize);
    const maxPagesToShow = 5;
    const pages: number[] = [];
    
    if (totalPages <= maxPagesToShow) {
      // Show all pages if total is less than max
      for (let i = 0; i < totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Show pages around current page
      let startPage = Math.max(0, this.currentPage - 2);
      let endPage = Math.min(totalPages - 1, this.currentPage + 2);
      
      // Adjust if we're near the start or end
      if (this.currentPage < 2) {
        endPage = Math.min(totalPages - 1, maxPagesToShow - 1);
      } else if (this.currentPage > totalPages - 3) {
        startPage = Math.max(0, totalPages - maxPagesToShow);
      }
      
      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }
    }
    
    return pages;
  }

  // Make Math available in template
  Math = Math;
}
