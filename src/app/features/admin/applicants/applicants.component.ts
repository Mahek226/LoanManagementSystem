import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, Applicant } from '../../../core/services/admin.service';

declare var bootstrap: any;

@Component({
  selector: 'app-applicants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './applicants.component.html',
  styleUrl: './applicants.component.css'
})
export class ApplicantsComponent implements OnInit {
  applicants: Applicant[] = [];
  filteredApplicants: Applicant[] = [];
  paginatedApplicants: Applicant[] = [];
  selectedApplicant: Applicant | null = null;

  // Filters and Search
  searchQuery = '';
  statusFilter = '';
  verificationFilter = '';
  viewMode: 'table' | 'grid' = 'table';

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;

  // Statistics
  totalApplicants = 0;
  pendingCount = 0;
  approvedCount = 0;
  rejectedCount = 0;

  // Math reference for template
  Math = Math;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadApplicants();
  }

  private loadApplicants(): void {
    // Load real data from backend
    this.adminService.getAllApplicants(0, 100).subscribe({
      next: (response) => {
        this.applicants = response.content;
        this.updateStatistics();
        this.applyFilters();
      },
      error: (error) => {
        console.error('Error loading applicants:', error);
        // Fallback to empty array if backend fails
        this.applicants = [];
        this.updateStatistics();
        this.applyFilters();
      }
    });
  }

  private updateStatistics(): void {
    this.totalApplicants = this.applicants.length;
    this.pendingCount = this.applicants.filter(a => a.approvalStatus === 'PENDING').length;
    this.approvedCount = this.applicants.filter(a => a.approvalStatus === 'APPROVED').length;
    this.rejectedCount = this.applicants.filter(a => a.approvalStatus === 'REJECTED').length;
  }

  onSearch(): void {
    this.applyFilters();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.statusFilter = '';
    this.verificationFilter = '';
    this.applyFilters();
  }

  private applyFilters(): void {
    this.filteredApplicants = this.applicants.filter(applicant => {
      const matchesSearch = !this.searchQuery || 
        applicant.firstName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        applicant.lastName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        applicant.email.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        applicant.username.toLowerCase().includes(this.searchQuery.toLowerCase());

      const matchesStatus = !this.statusFilter || applicant.approvalStatus === this.statusFilter;
      
      const matchesVerification = !this.verificationFilter || 
        applicant.isEmailVerified.toString() === this.verificationFilter;

      return matchesSearch && matchesStatus && matchesVerification;
    });

    this.currentPage = 1;
    this.updatePagination();
  }

  private updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredApplicants.length / this.pageSize);
    const startIndex = (this.currentPage - 1) * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.paginatedApplicants = this.filteredApplicants.slice(startIndex, endIndex);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePagination();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = 5;
    let startPage = Math.max(1, this.currentPage - Math.floor(maxPages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxPages - 1);

    if (endPage - startPage < maxPages - 1) {
      startPage = Math.max(1, endPage - maxPages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  }

  getInitials(firstName: string, lastName: string): string {
    return (firstName.charAt(0) + lastName.charAt(0)).toUpperCase();
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'APPROVED': return 'bg-success';
      case 'PENDING': return 'bg-warning';
      case 'REJECTED': return 'bg-danger';
      case 'UNDER_REVIEW': return 'bg-info';
      default: return 'bg-secondary';
    }
  }

  viewApplicant(applicant: Applicant): void {
    this.selectedApplicant = applicant;
    const modal = new bootstrap.Modal(document.getElementById('applicantModal'));
    modal.show();
  }

  approveApplicant(applicant: Applicant): void {
    if (confirm(`Are you sure you want to approve ${applicant.firstName} ${applicant.lastName}?`)) {
      this.adminService.approveApplicant(applicant.id).subscribe({
        next: () => {
          applicant.approvalStatus = 'APPROVED';
          applicant.isApproved = true;
          this.updateStatistics();
          this.applyFilters();
          alert('Applicant approved successfully!');
        },
        error: (error) => {
          console.error('Error approving applicant:', error);
          alert('Error approving applicant. Please try again.');
        }
      });
    }
  }

  rejectApplicant(applicant: Applicant): void {
    const reason = prompt(`Please provide a reason for rejecting ${applicant.firstName} ${applicant.lastName}:`);
    if (reason) {
      this.adminService.rejectApplicant(applicant.id, reason).subscribe({
        next: () => {
          applicant.approvalStatus = 'REJECTED';
          applicant.isApproved = false;
          this.updateStatistics();
          this.applyFilters();
          alert('Applicant rejected successfully!');
        },
        error: (error) => {
          console.error('Error rejecting applicant:', error);
          alert('Error rejecting applicant. Please try again.');
        }
      });
    }
  }

  refreshData(): void {
    this.loadApplicants();
  }

  exportData(): void {
    // Create CSV content
    const headers = ['Name', 'Username', 'Email', 'Phone', 'Status', 'Verification', 'Applied Date'];
    const csvContent = [
      headers.join(','),
      ...this.filteredApplicants.map(applicant => [
        `"${applicant.firstName} ${applicant.lastName}"`,
        applicant.username,
        applicant.email,
        applicant.phone,
        applicant.approvalStatus,
        applicant.isEmailVerified ? 'Verified' : 'Unverified',
        new Date(applicant.createdAt).toLocaleDateString()
      ].join(','))
    ].join('\n');

    // Download CSV
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `applicants-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
