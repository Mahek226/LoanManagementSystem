import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ComplianceOfficerService, ComplianceEscalation } from '../../../core/services/compliance-officer.service';

@Component({
  selector: 'app-escalations',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './escalations.component.html',
  styleUrls: ['./escalations.component.css']
})
export class EscalationsComponent implements OnInit {
  escalations: ComplianceEscalation[] = [];
  filteredEscalations: ComplianceEscalation[] = [];
  loading: boolean = true;
  errorMessage: string = '';

  // View mode
  viewMode: 'grid' | 'list' = 'grid';

  // Filters
  searchQuery: string = '';
  selectedPriority: string = 'all';
  selectedRiskLevel: string = 'all';
  selectedStatus: string = 'all';
  selectedLoanType: string = 'all';

  // Sort
  sortBy: string = 'assignedAt';
  sortOrder: 'asc' | 'desc' = 'desc';

  constructor(private complianceService: ComplianceOfficerService) {}

  ngOnInit(): void {
    this.loadEscalations();
  }

  loadEscalations(): void {
    this.loading = true;
    this.complianceService.getEscalations().subscribe({
      next: (escalations) => {
        this.escalations = escalations;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading escalations:', error);
        this.errorMessage = 'Failed to load escalations. Please try again.';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.escalations];

    // Search filter
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(e =>
        e.applicantName.toLowerCase().includes(query) ||
        e.loanType.toLowerCase().includes(query) ||
        e.applicantId.toString().includes(query) ||
        e.loanId.toString().includes(query)
      );
    }

    // Priority filter
    if (this.selectedPriority !== 'all') {
      filtered = filtered.filter(e =>
        this.complianceService.getPriorityLevel(e) === this.selectedPriority
      );
    }

    // Risk level filter
    if (this.selectedRiskLevel !== 'all') {
      filtered = filtered.filter(e => e.riskLevel === this.selectedRiskLevel);
    }

    // Status filter
    if (this.selectedStatus !== 'all') {
      filtered = filtered.filter(e => e.status === this.selectedStatus);
    }

    // Loan type filter
    if (this.selectedLoanType !== 'all') {
      filtered = filtered.filter(e => e.loanType === this.selectedLoanType);
    }

    // Sort
    filtered.sort((a, b) => {
      let aValue: any;
      let bValue: any;

      switch (this.sortBy) {
        case 'assignedAt':
          aValue = new Date(a.assignedAt).getTime();
          bValue = new Date(b.assignedAt).getTime();
          break;
        case 'loanAmount':
          aValue = a.loanAmount;
          bValue = b.loanAmount;
          break;
        case 'riskScore':
          aValue = a.riskScore;
          bValue = b.riskScore;
          break;
        case 'priority':
          const priorities = { 'URGENT': 4, 'HIGH': 3, 'MEDIUM': 2, 'LOW': 1 };
          aValue = priorities[this.complianceService.getPriorityLevel(a) as keyof typeof priorities];
          bValue = priorities[this.complianceService.getPriorityLevel(b) as keyof typeof priorities];
          break;
        default:
          aValue = a.applicantName;
          bValue = b.applicantName;
      }

      if (this.sortOrder === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });

    this.filteredEscalations = filtered;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  onSortChange(): void {
    this.applyFilters();
  }

  toggleSortOrder(): void {
    this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
    this.applyFilters();
  }

  setViewMode(mode: 'grid' | 'list'): void {
    this.viewMode = mode;
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.selectedPriority = 'all';
    this.selectedRiskLevel = 'all';
    this.selectedStatus = 'all';
    this.selectedLoanType = 'all';
    this.sortBy = 'assignedAt';
    this.sortOrder = 'desc';
    this.applyFilters();
  }

  exportToCSV(): void {
    this.complianceService.exportToCSV(this.filteredEscalations, 'compliance-escalations.csv');
  }

  formatCurrency(amount: number): string {
    return this.complianceService.formatCurrency(amount);
  }

  formatDate(dateString: string): string {
    return this.complianceService.formatDate(dateString);
  }

  getRiskBadgeClass(riskLevel: string): string {
    return this.complianceService.getRiskBadgeClass(riskLevel);
  }

  getStatusBadgeClass(status: string): string {
    return this.complianceService.getStatusBadgeClass(status);
  }

  getPriorityLevel(escalation: ComplianceEscalation): string {
    return this.complianceService.getPriorityLevel(escalation);
  }

  getPriorityColor(priority: string): string {
    return this.complianceService.getPriorityColor(priority);
  }

  getUniqueLoanTypes(): string[] {
    return [...new Set(this.escalations.map(e => e.loanType))];
  }
}
