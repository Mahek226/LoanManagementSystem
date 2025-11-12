import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { 
  LoanOfficerService, 
  ComplianceDocumentResubmissionRequest, 
  ProcessDocumentResubmissionRequest 
} from '../../../core/services/loan-officer.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-document-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './document-requests.component.html',
  styleUrls: ['./document-requests.component.css']
})
export class DocumentRequestsComponent implements OnInit, OnDestroy {
  // Data
  documentRequests: ComplianceDocumentResubmissionRequest[] = [];
  filteredRequests: ComplianceDocumentResubmissionRequest[] = [];
  resubmittedDocuments: any[] = [];
  filteredResubmittedDocuments: any[] = [];
  
  // UI State
  loading = false;
  error: string | null = null;
  success: string | null = null;
  
  // Tab management
  activeTab: 'requests' | 'resubmitted' = 'requests';
  
  // Filters
  searchTerm = '';
  statusFilter = '';
  priorityFilter = '';
  sortBy = 'requestedAt';
  sortOrder: 'asc' | 'desc' = 'desc';
  
  // Modal state
  showProcessModal = false;
  selectedRequest: ComplianceDocumentResubmissionRequest | null = null;
  processAction: 'APPROVE' | 'REJECT' = 'APPROVE';
  processRemarks = '';
  rejectionReason = '';
  processing = false;
  
  // Pagination
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 0;
  
  private subscriptions: Subscription[] = [];
  
  // Make Math available in template
  Math = Math;
  
  constructor(
    private loanOfficerService: LoanOfficerService,
    private authService: AuthService
  ) {}
  
  ngOnInit(): void {
    this.loadDocumentRequests();
  }
  
  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
  
  // ==================== Data Loading ====================
  
  loadDocumentRequests(): void {
    this.loading = true;
    this.error = null;
    
    // Get officer ID from localStorage or auth service
    const officerId = this.getOfficerId();
    console.log('Loading document requests for officer ID:', officerId);
    
    // Load both document resubmission requests and resubmitted documents
    const requestsSub = this.loanOfficerService.getDocumentResubmissionRequests(officerId).subscribe({
      next: (requests) => {
        console.log('Received document requests:', requests);
        console.log('Number of requests:', requests.length);
        this.documentRequests = requests || [];
        this.applyFilters();
        this.checkLoadingComplete();
      },
      error: (error) => {
        console.error('Error loading document requests:', error);
        this.error = `Failed to load document requests. ${error.error?.message || 'Please try again.'}`;
        this.documentRequests = [];
        this.applyFilters();
        this.checkLoadingComplete();
      }
    });
    
    // Load resubmitted documents
    const resubmittedSub = this.loanOfficerService.getResubmittedDocuments(officerId).subscribe({
      next: (documents) => {
        console.log('Received resubmitted documents:', documents);
        console.log('Number of resubmitted documents:', documents.length);
        this.resubmittedDocuments = documents || [];
        this.applyResubmittedFilters();
        this.checkLoadingComplete();
      },
      error: (error) => {
        console.error('Error loading resubmitted documents:', error);
        this.resubmittedDocuments = [];
        this.applyResubmittedFilters();
        this.checkLoadingComplete();
      }
    });
    
    this.subscriptions.push(requestsSub, resubmittedSub);
  }
  
  private checkLoadingComplete(): void {
    // Check if both requests have completed (success or error)
    if (this.documentRequests !== undefined && this.resubmittedDocuments !== undefined) {
      this.loading = false;
      
      const totalItems = this.documentRequests.length + this.resubmittedDocuments.length;
      if (totalItems > 0) {
        this.success = `Loaded ${this.documentRequests.length} request(s) and ${this.resubmittedDocuments.length} resubmitted document(s)`;
        setTimeout(() => this.success = null, 3000);
      }
    }
  }
  
  private getOfficerId(): number {
    const user = this.authService.currentUserValue;
    if (user && user.officerId) {
      return user.officerId;
    }
    // Fallback to user ID if officerId is not available
    return user?.userId || 1;
  }
  
  // ==================== Filtering & Sorting ====================
  
  applyFilters(): void {
    let filtered = [...this.documentRequests];
    
    // Search filter
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(req => 
        req.applicantName.toLowerCase().includes(term) ||
        req.loanType.toLowerCase().includes(term) ||
        req.reason.toLowerCase().includes(term) ||
        req.requestedBy.toLowerCase().includes(term)
      );
    }
    
    // Status filter
    if (this.statusFilter) {
      filtered = filtered.filter(req => req.status === this.statusFilter);
    }
    
    // Priority filter
    if (this.priorityFilter) {
      filtered = filtered.filter(req => req.priorityLevel.toString() === this.priorityFilter);
    }
    
    // Sorting
    filtered.sort((a, b) => {
      let aValue: any = a[this.sortBy as keyof ComplianceDocumentResubmissionRequest];
      let bValue: any = b[this.sortBy as keyof ComplianceDocumentResubmissionRequest];
      
      if (this.sortBy === 'requestedAt') {
        aValue = new Date(aValue).getTime();
        bValue = new Date(bValue).getTime();
      }
      
      if (this.sortOrder === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });
    
    this.filteredRequests = filtered;
    this.calculatePagination();
  }
  
  calculatePagination(): void {
    this.totalPages = Math.ceil(this.filteredRequests.length / this.itemsPerPage);
    if (this.currentPage > this.totalPages) {
      this.currentPage = 1;
    }
  }
  
  applyResubmittedFilters(): void {
    let filtered = [...this.resubmittedDocuments];
    
    // Search filter
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(doc => 
        doc.applicantName.toLowerCase().includes(term) ||
        doc.documentType.toLowerCase().includes(term) ||
        doc.loanType.toLowerCase().includes(term)
      );
    }
    
    // Status filter
    if (this.statusFilter) {
      filtered = filtered.filter(doc => doc.status === this.statusFilter);
    }
    
    // Sorting
    filtered.sort((a, b) => {
      let aValue: any = a.resubmittedAt;
      let bValue: any = b.resubmittedAt;
      
      if (this.sortBy === 'resubmittedAt') {
        aValue = new Date(aValue).getTime();
        bValue = new Date(bValue).getTime();
      }
      
      if (this.sortOrder === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });
    
    this.filteredResubmittedDocuments = filtered;
  }
  
  get paginatedRequests(): ComplianceDocumentResubmissionRequest[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredRequests.slice(startIndex, endIndex);
  }
  
  // ==================== Sorting ====================
  
  setSortBy(field: string): void {
    if (this.sortBy === field) {
      this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortOrder = 'desc';
    }
    this.applyFilters();
  }
  
  // ==================== Processing Requests ====================
  
  openProcessModal(request: ComplianceDocumentResubmissionRequest, action: 'APPROVE' | 'REJECT'): void {
    this.selectedRequest = request;
    this.processAction = action;
    this.processRemarks = '';
    this.rejectionReason = '';
    this.showProcessModal = true;
  }
  
  closeProcessModal(): void {
    this.showProcessModal = false;
    this.selectedRequest = null;
    this.processRemarks = '';
    this.rejectionReason = '';
  }
  
  processRequest(): void {
    if (!this.selectedRequest) return;
    
    if (this.processAction === 'REJECT' && !this.rejectionReason.trim()) {
      this.error = 'Rejection reason is required';
      return;
    }
    
    this.processing = true;
    this.error = null;
    
    const request: ProcessDocumentResubmissionRequest = {
      resubmissionId: this.selectedRequest.resubmissionId,
      action: this.processAction,
      remarks: this.processRemarks.trim() || undefined,
      rejectionReason: this.processAction === 'REJECT' ? this.rejectionReason.trim() : undefined
    };
    
    const officerId = this.getOfficerId();
    
    const sub = this.loanOfficerService.processDocumentResubmissionRequest(officerId, request).subscribe({
      next: (response) => {
        this.success = response.message || `Request ${this.processAction.toLowerCase()}d successfully!`;
        this.closeProcessModal();
        this.loadDocumentRequests(); // Reload data
        this.processing = false;
        
        // Clear success message after 3 seconds
        setTimeout(() => this.success = null, 3000);
      },
      error: (error) => {
        console.error('Error processing request:', error);
        this.error = error.error?.message || 'Failed to process request. Please try again.';
        this.processing = false;
      }
    });
    
    this.subscriptions.push(sub);
  }
  
  // ==================== Tab Management ====================
  
  switchTab(tab: 'requests' | 'resubmitted'): void {
    this.activeTab = tab;
    this.currentPage = 1; // Reset pagination when switching tabs
  }
  
  get paginatedResubmittedDocuments(): any[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredResubmittedDocuments.slice(startIndex, endIndex);
  }
  
  // ==================== Utility Methods ====================
  
  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  formatCurrency(amount: number): string {
    return '₹' + amount.toLocaleString('en-IN');
  }
  
  getPriorityLabel(level: number): string {
    switch (level) {
      case 5: return 'URGENT';
      case 4: return 'HIGH';
      case 3: return 'MEDIUM';
      case 2: return 'LOW';
      case 1: return 'VERY LOW';
      default: return 'MEDIUM';
    }
  }
  
  getPriorityClass(level: number): string {
    switch (level) {
      case 5: return 'badge bg-danger';
      case 4: return 'badge bg-warning';
      case 3: return 'badge bg-info';
      case 2: return 'badge bg-secondary';
      case 1: return 'badge bg-light text-dark';
      default: return 'badge bg-info';
    }
  }
  
  getStatusClass(status: string): string {
    switch (status) {
      case 'REQUESTED': return 'badge bg-warning';
      case 'FORWARDED_TO_APPLICANT': return 'badge bg-success';
      case 'REJECTED_BY_LOAN_OFFICER': return 'badge bg-danger';
      case 'PENDING': return 'badge bg-warning';
      case 'VERIFIED': return 'badge bg-success';
      case 'REJECTED': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  }
  
  // ==================== Resubmitted Documents Methods ====================
  
  viewDocument(document: any): void {
    // Open document in new tab
    if (document.documentUrl) {
      window.open(document.documentUrl, '_blank');
    }
  }
  
  processDocument(document: any, action: 'APPROVE' | 'REJECT'): void {
    const officerId = this.getOfficerId();
    const remarks = action === 'APPROVE' ? 'Document approved after resubmission' : 'Document rejected after resubmission';
    
    const request = {
      documentId: document.documentId,
      action: action,
      remarks: remarks,
      officerId: officerId,
      assignmentId: document.assignmentId
    };
    
    this.loanOfficerService.processResubmittedDocument(request).subscribe({
      next: (response) => {
        this.success = `Document ${action.toLowerCase()}d successfully!`;
        
        // Update local document status
        const docIndex = this.resubmittedDocuments.findIndex(d => d.documentId === document.documentId);
        if (docIndex !== -1) {
          this.resubmittedDocuments[docIndex].status = action === 'APPROVE' ? 'VERIFIED' : 'REJECTED';
        }
        
        this.applyResubmittedFilters();
        setTimeout(() => this.success = null, 3000);
      },
      error: (error) => {
        console.error('Error processing document:', error);
        this.error = error.error?.message || 'Failed to process document. Please try again.';
        setTimeout(() => this.error = null, 5000);
      }
    });
  }
  
  getStatusLabel(status: string): string {
    switch (status) {
      case 'REQUESTED': return 'Pending Review';
      case 'FORWARDED_TO_APPLICANT': return 'Forwarded to Applicant';
      case 'REJECTED_BY_LOAN_OFFICER': return 'Rejected';
      default: return status;
    }
  }
  
  parseDocumentTypes(documentsJson: string): string[] {
    try {
      return JSON.parse(documentsJson);
    } catch {
      return [];
    }
  }
  
  // ==================== Pagination ====================
  
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }
  
  get pageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    const half = Math.floor(maxVisible / 2);
    
    let start = Math.max(1, this.currentPage - half);
    let end = Math.min(this.totalPages, start + maxVisible - 1);
    
    if (end - start + 1 < maxVisible) {
      start = Math.max(1, end - maxVisible + 1);
    }
    
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    
    return pages;
  }
  
  // ==================== Export ====================
  
  exportToCSV(): void {
    const headers = [
      'Applicant Name', 'Loan Type', 'Loan Amount', 'Requested Documents', 
      'Reason', 'Priority', 'Status', 'Requested By', 'Requested At'
    ];
    
    const rows = this.filteredRequests.map(req => [
      req.applicantName,
      req.loanType,
      req.loanAmount.toString(),
      this.parseDocumentTypes(req.requestedDocuments).join('; '),
      req.reason,
      this.getPriorityLabel(req.priorityLevel),
      this.getStatusLabel(req.status),
      req.requestedBy,
      this.formatDate(req.requestedAt)
    ]);
    
    const csvContent = [headers, ...rows]
      .map(row => row.map(field => `"${field}"`).join(','))
      .join('\n');
    
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `document-resubmission-requests-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
