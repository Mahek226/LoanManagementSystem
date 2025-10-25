import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, OfficerResponse, LoanOfficerRequest } from '../../../core/services/admin.service';

@Component({
  selector: 'app-loan-officers',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loan-officers.component.html',
  styleUrl: './loan-officers.component.css'
})
export class LoanOfficersComponent implements OnInit {
  officers: OfficerResponse[] = [];
  filteredOfficers: OfficerResponse[] = [];
  loading = false;
  error = '';
  success = '';
  
  // Form data
  showAddForm = false;
  newOfficer: LoanOfficerRequest = {
    username: '',
    email: '',
    password: '',
    loanType: ''
  };
  
  // Search and filter
  searchQuery = '';
  selectedLoanType = '';
  loanTypes = ['Personal Loan', 'Home Loan', 'Car Loan', 'Business Loan', 'Education Loan'];
  
  // View mode
  viewMode: 'table' | 'grid' = 'table';
  
  // Selected officer for details
  selectedOfficer: OfficerResponse | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadOfficers();
  }

  loadOfficers(): void {
    this.loading = true;
    this.error = '';
    
    this.adminService.getAllLoanOfficers().subscribe({
      next: (officers) => {
        this.officers = officers;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load loan officers';
        console.error('Error loading loan officers:', err);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.officers];
    
    // Search filter
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(officer => 
        officer.username.toLowerCase().includes(query) ||
        officer.email.toLowerCase().includes(query) ||
        (officer.loanType && officer.loanType.toLowerCase().includes(query))
      );
    }
    
    // Loan type filter
    if (this.selectedLoanType) {
      filtered = filtered.filter(officer => officer.loanType === this.selectedLoanType);
    }
    
    this.filteredOfficers = filtered;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onLoanTypeChange(): void {
    this.applyFilters();
  }

  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    if (this.showAddForm) {
      this.resetForm();
      this.error = '';
      this.success = '';
    }
  }

  resetForm(): void {
    this.newOfficer = {
      username: '',
      email: '',
      password: '',
      loanType: ''
    };
  }

  addOfficer(): void {
    if (!this.validateForm()) {
      return;
    }
    
    this.loading = true;
    this.error = '';
    this.success = '';
    
    this.adminService.addLoanOfficer(this.newOfficer).subscribe({
      next: (response) => {
        this.success = response.message || 'Loan Officer added successfully';
        this.showAddForm = false;
        this.resetForm();
        this.loadOfficers();
        
        setTimeout(() => {
          this.success = '';
        }, 3000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to add loan officer';
        this.loading = false;
      }
    });
  }

  validateForm(): boolean {
    if (!this.newOfficer.username || this.newOfficer.username.length < 3) {
      this.error = 'Username must be at least 3 characters';
      return false;
    }
    
    if (!this.newOfficer.email || !this.isValidEmail(this.newOfficer.email)) {
      this.error = 'Please enter a valid email address';
      return false;
    }
    
    if (!this.newOfficer.password || this.newOfficer.password.length < 6) {
      this.error = 'Password must be at least 6 characters';
      return false;
    }
    
    return true;
  }

  isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  deleteOfficer(id: number, username: string): void {
    if (!confirm(`Are you sure you want to delete Loan Officer: ${username}?`)) {
      return;
    }
    
    this.loading = true;
    this.error = '';
    
    this.adminService.deleteLoanOfficer(id).subscribe({
      next: () => {
        this.success = 'Loan Officer deleted successfully';
        this.loadOfficers();
        
        setTimeout(() => {
          this.success = '';
        }, 3000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to delete loan officer';
        this.loading = false;
      }
    });
  }

  viewDetails(officer: OfficerResponse): void {
    this.selectedOfficer = officer;
  }

  closeDetails(): void {
    this.selectedOfficer = null;
  }

  toggleViewMode(): void {
    this.viewMode = this.viewMode === 'table' ? 'grid' : 'table';
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
  }

  exportToCsv(): void {
    const headers = ['Officer ID', 'Username', 'Email', 'Loan Type', 'Created At'];
    const rows = this.filteredOfficers.map(officer => [
      officer.officerId,
      officer.username,
      officer.email,
      officer.loanType || 'N/A',
      this.formatDate(officer.createdAt)
    ]);
    
    let csvContent = headers.join(',') + '\n';
    rows.forEach(row => {
      csvContent += row.join(',') + '\n';
    });
    
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `loan-officers-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
