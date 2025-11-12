import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ComplianceOfficerService } from '../../../core/services/compliance-officer.service';
import { AuthService } from '../../../core/services/auth.service';

interface DocumentResubmissionRequestData {
  resubmissionId: number;
  assignmentId: number;
  loanId: number;
  applicantId: number;
  applicantName: string;
  loanType: string;
  loanAmount: number;
  requestedDocuments: string[];
  reason: string;
  additionalComments?: string;
  priorityLevel: number;
  status: string;
  requestedAt: string;
  submittedAt?: string;
  reviewedAt?: string;
  processedAt?: string;
  requestedByOfficerId: number;
}

interface DocumentData {
  documentId: number;
  documentType: string;
  fileName: string;
  fileUrl: string;
  uploadedAt: string;
  applicantId: number;
  loanId: number;
}

@Component({
  selector: 'app-document-resubmission',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './document-resubmission.component.html',
  styleUrls: ['./document-resubmission.component.css']
})
export class DocumentResubmissionComponent implements OnInit, OnDestroy {
  // Data
  pendingRequests: DocumentResubmissionRequestData[] = [];
  resolvedRequests: DocumentResubmissionRequestData[] = [];
  filteredPendingRequests: DocumentResubmissionRequestData[] = [];
  filteredResolvedRequests: DocumentResubmissionRequestData[] = [];
  
  // UI State
  loading = false;
  error: string | null = null;
  success: string | null = null;
  
  // Tab management
  activeTab: 'pending' | 'resolved' = 'pending';
  
  // Filters
  searchTerm = '';
  statusFilter = '';
  priorityFilter = '';
  sortBy = 'requestedAt';
  sortOrder: 'asc' | 'desc' = 'desc';
  
  // Pagination
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 1;
  
  // Document viewing
  selectedDocument: DocumentData | null = null;
  documentViewerVisible = false;
  documentsForRequest: DocumentData[] = [];
  loadingDocuments = false;
  
  private subscriptions: Subscription[] = [];

  constructor(
    private complianceService: ComplianceOfficerService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadDocumentResubmissionData();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  // ==================== Data Loading ====================

  loadDocumentResubmissionData(): void {
    this.loading = true;
    this.error = null;

    const officerId = this.getOfficerId();
    console.log('Loading document resubmission data for officer:', officerId);

    // Load pending requests
    const pendingSub = this.complianceService.getDocumentResubmissionRequests('REQUESTED').subscribe({
      next: (requests: any[]) => {
        console.log('Received pending requests:', requests);
        // Temporarily show all requests to debug the issue
        this.pendingRequests = requests || [];
        console.log('All pending requests (unfiltered):', requests);
        this.applyPendingFilters();
        this.checkLoadingComplete();
      },
      error: (error: any) => {
        console.error('Error loading pending requests:', error);
        this.error = 'Failed to load pending requests. Please try again.';
        this.pendingRequests = [];
        this.applyPendingFilters();
        this.checkLoadingComplete();
      }
    });

    // Load forwarded documents from loan officers
    const forwardedSub = this.complianceService.getForwardedDocuments('FORWARDED').subscribe({
      next: (documents: any[]) => {
        console.log('Received forwarded documents:', documents);
        this.resolvedRequests = documents || [];
        console.log('All forwarded documents:', documents);
        this.applyResolvedFilters();
        this.checkLoadingComplete();
      },
      error: (error: any) => {
        console.error('Error loading forwarded documents:', error);
        this.resolvedRequests = [];
        this.applyResolvedFilters();
        this.checkLoadingComplete();
      }
    });

    this.subscriptions.push(pendingSub, forwardedSub);
  }

  private checkLoadingComplete(): void {
    if (this.pendingRequests !== undefined && this.resolvedRequests !== undefined) {
      this.loading = false;
      
      const totalItems = this.pendingRequests.length + this.resolvedRequests.length;
      if (totalItems > 0) {
        this.success = `Loaded ${this.pendingRequests.length} pending and ${this.resolvedRequests.length} resolved requests`;
        setTimeout(() => this.success = null, 3000);
      }
    }
  }

  private getOfficerId(): number {
    const user = this.authService.currentUserValue;
    return user?.officerId || user?.userId || 1;
  }

  // ==================== Tab Management ====================

  switchTab(tab: 'pending' | 'resolved'): void {
    this.activeTab = tab;
    this.currentPage = 1;
    this.applyFilters();
  }

  // ==================== Filtering and Sorting ====================

  applyFilters(): void {
    if (this.activeTab === 'pending') {
      this.applyPendingFilters();
    } else {
      this.applyResolvedFilters();
    }
  }

  applyPendingFilters(): void {
    let filtered = [...this.pendingRequests];

    // Search filter
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(req => 
        req.applicantName.toLowerCase().includes(term) ||
        req.loanType.toLowerCase().includes(term) ||
        req.reason.toLowerCase().includes(term)
      );
    }

    // Priority filter
    if (this.priorityFilter) {
      filtered = filtered.filter(req => req.priorityLevel.toString() === this.priorityFilter);
    }

    // Sorting
    this.sortRequests(filtered);
    this.filteredPendingRequests = filtered;
    this.calculatePagination();
  }

  applyResolvedFilters(): void {
    let filtered = [...this.resolvedRequests];

    // Search filter
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(req => 
        req.applicantName.toLowerCase().includes(term) ||
        req.loanType.toLowerCase().includes(term) ||
        req.reason.toLowerCase().includes(term)
      );
    }

    // Status filter
    if (this.statusFilter) {
      filtered = filtered.filter(req => req.status === this.statusFilter);
    }

    // Sorting
    this.sortRequests(filtered);
    this.filteredResolvedRequests = filtered;
    this.calculatePagination();
  }

  private sortRequests(requests: DocumentResubmissionRequestData[]): void {
    requests.sort((a, b) => {
      let aValue: any = a[this.sortBy as keyof DocumentResubmissionRequestData];
      let bValue: any = b[this.sortBy as keyof DocumentResubmissionRequestData];

      if (this.sortBy === 'requestedAt' || this.sortBy === 'submittedAt' || this.sortBy === 'reviewedAt') {
        aValue = new Date(aValue).getTime();
        bValue = new Date(bValue).getTime();
      }

      if (this.sortOrder === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });
  }

  calculatePagination(): void {
    const totalItems = this.activeTab === 'pending' ? 
      this.filteredPendingRequests.length : 
      this.filteredResolvedRequests.length;
    
    this.totalPages = Math.ceil(totalItems / this.itemsPerPage);
    if (this.currentPage > this.totalPages) {
      this.currentPage = 1;
    }
  }

  // ==================== Pagination ====================

  get paginatedRequests(): DocumentResubmissionRequestData[] {
    const requests = this.activeTab === 'pending' ? 
      this.filteredPendingRequests : 
      this.filteredResolvedRequests;
    
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return requests.slice(startIndex, endIndex);
  }

  get pageNumbers(): number[] {
    const pages = [];
    for (let i = 1; i <= this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
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

  getPriorityClass(priority: number): string {
    switch (priority) {
      case 5: return 'badge bg-danger';
      case 4: return 'badge bg-warning';
      case 3: return 'badge bg-info';
      case 2: return 'badge bg-secondary';
      case 1: return 'badge bg-light text-dark';
      default: return 'badge bg-info';
    }
  }

  getPriorityLabel(priority: number): string {
    switch (priority) {
      case 5: return 'URGENT';
      case 4: return 'HIGH';
      case 3: return 'MEDIUM';
      case 2: return 'LOW';
      case 1: return 'VERY LOW';
      default: return 'MEDIUM';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'REQUESTED': return 'badge bg-warning';
      case 'SUBMITTED': return 'badge bg-info';
      case 'REVIEWED': return 'badge bg-success';
      case 'REJECTED': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  }

  parseDocumentTypes(documents: string | string[]): string[] {
    if (Array.isArray(documents)) {
      return documents;
    }
    try {
      return JSON.parse(documents);
    } catch (e) {
      return [documents];
    }
  }

  viewRequestDetails(request: DocumentResubmissionRequestData): void {
    console.log('Viewing documents for request:', request);
    this.loadingDocuments = true;
    this.documentsForRequest = [];
    
    // Fetch documents for this loan
    this.complianceService.getLoanDocuments(request.loanId).subscribe({
      next: (documents: any[]) => {
        console.log('Received documents for loan:', documents);
        this.documentsForRequest = documents.map(doc => ({
          documentId: doc.documentId,
          documentType: doc.documentType,
          fileName: doc.fileName || doc.originalFileName,
          fileUrl: doc.fileUrl || doc.filePath,
          uploadedAt: doc.uploadedAt,
          applicantId: doc.applicantId,
          loanId: doc.loanId
        }));
        
        // If documents exist, show the first one
        if (this.documentsForRequest.length > 0) {
          this.selectedDocument = this.documentsForRequest[0];
          this.documentViewerVisible = true;
        } else {
          this.error = 'No documents found for this request.';
          setTimeout(() => this.error = null, 3000);
        }
        this.loadingDocuments = false;
      },
      error: (error) => {
        console.error('Error loading documents:', error);
        this.error = 'Failed to load documents. Please try again.';
        setTimeout(() => this.error = null, 3000);
        this.loadingDocuments = false;
      }
    });
  }

  continueWithApplication(request: any): void {
    console.log('Continuing with application for request:', request);
    // Navigate to the screening page for this loan
    if (request.loanId) {
      this.router.navigate(['/compliance-officer/screening', request.loanId]);
    } else {
      console.error('No loan ID found in request:', request);
    }
  }

  proceedToReview(request: any): void {
    console.log('Proceeding to review for forwarded document:', request);
    // Navigate to the comprehensive review page using assignment ID
    if (request.assignmentId) {
      this.router.navigate(['/compliance-officer/comprehensive-review', request.assignmentId]);
    } else if (request.loanId) {
      // Fallback to loan ID if assignment ID is not available
      this.router.navigate(['/compliance-officer/comprehensive-review', request.loanId]);
    } else {
      console.error('No assignment ID or loan ID found in forwarded document:', request);
      this.error = 'Unable to navigate to application review. Missing required identifiers.';
      setTimeout(() => this.error = null, 3000);
    }
  }

  refreshData(): void {
    this.loadDocumentResubmissionData();
  }

  // ==================== Document Viewer Methods ====================

  closeDocumentViewer(): void {
    this.documentViewerVisible = false;
    this.selectedDocument = null;
    this.documentsForRequest = [];
  }

  selectDocument(document: DocumentData): void {
    this.selectedDocument = document;
  }

  getDocumentImageUrl(document: DocumentData): string {
    if (!document) return '';
    
    // Handle different possible URL formats
    if (document.fileUrl) {
      // If it's already a full URL, return as is
      if (document.fileUrl.startsWith('http')) {
        return document.fileUrl;
      }
      // If it's a relative path, prepend the API base URL
      return `http://localhost:8080${document.fileUrl}`;
    }
    
    // Fallback: construct URL from document ID
    return `http://localhost:8080/api/documents/${document.documentId}/view`;
  }

  onImageError(event: any): void {
    console.error('Error loading document image:', event);
    // You could set a placeholder image here
    event.target.src = 'assets/images/document-error.png';
  }

  getDocumentTypeIcon(documentType: string): string {
    const iconMap: { [key: string]: string } = {
      'INCOME_PROOF': 'fa-file-invoice-dollar',
      'IDENTITY_PROOF': 'fa-id-card',
      'ADDRESS_PROOF': 'fa-home',
      'BANK_STATEMENT': 'fa-university',
      'EMPLOYMENT_PROOF': 'fa-briefcase',
      'AADHAAR': 'fa-id-card',
      'PAN': 'fa-id-card',
      'PASSPORT': 'fa-passport',
      'OTHER': 'fa-file-alt'
    };
    return iconMap[documentType] || 'fa-file-alt';
  }
}
