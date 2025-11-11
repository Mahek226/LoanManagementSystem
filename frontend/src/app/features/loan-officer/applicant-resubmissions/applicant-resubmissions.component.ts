import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { LoanOfficerService } from '@core/services/loan-officer.service';
import { AuthService } from '@core/services/auth.service';

interface ResubmittedDocument {
  documentId: number;
  loanId: number;
  applicantId: number;
  applicantName: string;
  documentType: string;
  documentName: string;
  documentUrl: string;
  resubmittedAt: string;
  originalRequestReason: string;
  applicantComments?: string;
  status: string;
  loanType: string;
  loanAmount: number;
  notificationId: number;
  assignmentId: number;
}

@Component({
  selector: 'app-applicant-resubmissions',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './applicant-resubmissions.component.html',
  styleUrls: ['./applicant-resubmissions.component.css']
})
export class ApplicantResubmissionsComponent implements OnInit {
  resubmittedDocuments: ResubmittedDocument[] = [];
  filteredDocuments: ResubmittedDocument[] = [];
  selectedDocument: ResubmittedDocument | null = null;
  
  // Filter options
  filterStatus: string = 'ALL';
  searchQuery: string = '';
  
  // Modal states
  showProcessModal: boolean = false;
  showDocumentModal: boolean = false;
  
  // Form
  processForm!: FormGroup;
  
  // Loading states
  loading: boolean = false;
  processing: boolean = false;
  
  // Officer info
  officerId: number = 0;
  
  // Stats
  stats = {
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0,
    forwarded: 0
  };

  constructor(
    private loanOfficerService: LoanOfficerService,
    private authService: AuthService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    const currentUser = this.authService.currentUserValue;
    if (currentUser?.id) {
      this.officerId = currentUser.id;
      this.loadResubmittedDocuments();
    }
  }

  initializeForm(): void {
    this.processForm = this.fb.group({
      action: ['', Validators.required],
      remarks: ['', [Validators.required, Validators.minLength(10)]],
      forwardReason: ['']
    });

    // Add conditional validation for forwardReason
    this.processForm.get('action')?.valueChanges.subscribe(action => {
      const forwardReasonControl = this.processForm.get('forwardReason');
      if (action === 'FORWARD_TO_COMPLIANCE') {
        forwardReasonControl?.setValidators([Validators.required, Validators.minLength(20)]);
      } else {
        forwardReasonControl?.clearValidators();
      }
      forwardReasonControl?.updateValueAndValidity();
    });
  }

  loadResubmittedDocuments(): void {
    this.loading = true;
    this.loanOfficerService.getResubmittedDocuments(this.officerId).subscribe({
      next: (documents: any) => {
        this.resubmittedDocuments = documents;
        this.applyFilters();
        this.calculateStats();
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Error loading resubmitted documents:', error);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredDocuments = this.resubmittedDocuments.filter(doc => {
      const matchesStatus = this.filterStatus === 'ALL' || doc.status === this.filterStatus;
      const matchesSearch = !this.searchQuery || 
        doc.applicantName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        doc.documentType.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        doc.loanId.toString().includes(this.searchQuery);
      
      return matchesStatus && matchesSearch;
    });
  }

  calculateStats(): void {
    this.stats.total = this.resubmittedDocuments.length;
    this.stats.pending = this.resubmittedDocuments.filter(d => d.status === 'PENDING_REVIEW').length;
    this.stats.approved = this.resubmittedDocuments.filter(d => d.status === 'APPROVED').length;
    this.stats.rejected = this.resubmittedDocuments.filter(d => d.status === 'REJECTED').length;
    this.stats.forwarded = this.resubmittedDocuments.filter(d => d.status === 'FORWARDED_TO_COMPLIANCE').length;
  }

  openProcessModal(document: ResubmittedDocument): void {
    this.selectedDocument = document;
    this.processForm.reset();
    this.showProcessModal = true;
  }

  closeProcessModal(): void {
    this.showProcessModal = false;
    this.selectedDocument = null;
    this.processForm.reset();
  }

  viewDocument(document: ResubmittedDocument): void {
    this.selectedDocument = document;
    this.showDocumentModal = true;
  }

  closeDocumentModal(): void {
    this.showDocumentModal = false;
  }

  processResubmission(): void {
    if (this.processForm.invalid || !this.selectedDocument) {
      return;
    }

    this.processing = true;
    const formValue = this.processForm.value;
    
    const request = {
      documentId: this.selectedDocument.documentId,
      action: formValue.action,
      remarks: formValue.remarks,
      forwardReason: formValue.forwardReason || null,
      officerId: this.officerId,
      assignmentId: this.selectedDocument.assignmentId
    };

    this.loanOfficerService.processResubmittedDocument(request).subscribe({
      next: (response: any) => {
        console.log('Document processed successfully:', response);
        
        // Update local document status
        const docIndex = this.resubmittedDocuments.findIndex(d => d.documentId === this.selectedDocument!.documentId);
        if (docIndex !== -1) {
          this.resubmittedDocuments[docIndex].status = this.getNewStatus(formValue.action);
        }
        
        this.applyFilters();
        this.calculateStats();
        this.closeProcessModal();
        this.processing = false;
        
        alert(`Document ${formValue.action === 'APPROVE' ? 'approved' : formValue.action === 'REJECT' ? 'rejected' : 'forwarded to compliance'} successfully!`);
      },
      error: (error: any) => {
        console.error('Error processing document:', error);
        this.processing = false;
        alert('Error processing document. Please try again.');
      }
    });
  }

  getNewStatus(action: string): string {
    switch (action) {
      case 'APPROVE':
        return 'APPROVED';
      case 'REJECT':
        return 'REJECTED';
      case 'FORWARD_TO_COMPLIANCE':
        return 'FORWARDED_TO_COMPLIANCE';
      default:
        return 'PENDING_REVIEW';
    }
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDING_REVIEW':
        return 'badge bg-warning';
      case 'APPROVED':
        return 'badge bg-success';
      case 'REJECTED':
        return 'badge bg-danger';
      case 'FORWARDED_TO_COMPLIANCE':
        return 'badge bg-info';
      default:
        return 'badge bg-secondary';
    }
  }

  getDocumentIcon(documentType: string): string {
    const icons: { [key: string]: string } = {
      'AADHAAR': 'fa-id-card',
      'PAN': 'fa-credit-card',
      'PASSPORT': 'fa-passport',
      'PHOTO': 'fa-camera',
      'INCOME_PROOF': 'fa-file-invoice-dollar',
      'BANK_STATEMENT': 'fa-university',
      'EMPLOYMENT_PROOF': 'fa-briefcase',
      'ADDRESS_PROOF': 'fa-home'
    };
    return icons[documentType] || 'fa-file';
  }

  formatDate(dateString: string): string {
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
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  formatLoanId(loanId: number): string {
    return `LNID${(100000 + loanId).toString().padStart(6, '0')}`;
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  goToLoanReview(loanId: number): void {
    this.router.navigate(['/loan-officer/review', loanId]);
  }
}
