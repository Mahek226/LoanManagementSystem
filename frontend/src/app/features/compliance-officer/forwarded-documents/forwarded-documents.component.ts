import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ComplianceOfficerService } from '@core/services/compliance-officer.service';
import { AuthService } from '@core/services/auth.service';

interface ForwardedDocument {
  documentId: number;
  loanId: number;
  applicantId: number;
  applicantName: string;
  documentType: string;
  documentName: string;
  documentUrl: string;
  forwardedAt: string;
  forwardedBy: string; // Loan Officer name
  forwardReason: string;
  loanOfficerRemarks: string;
  status: string; // PENDING_COMPLIANCE_REVIEW, APPROVED_BY_COMPLIANCE, REJECTED_BY_COMPLIANCE
  loanType: string;
  loanAmount: number;
  assignmentId: number;
  loanOfficerId: number;
}

@Component({
  selector: 'app-forwarded-documents',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './forwarded-documents.component.html',
  styleUrls: ['./forwarded-documents.component.css']
})
export class ForwardedDocumentsComponent implements OnInit {
  forwardedDocuments: ForwardedDocument[] = [];
  filteredDocuments: ForwardedDocument[] = [];
  selectedDocument: ForwardedDocument | null = null;
  
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
  complianceOfficerId: number = 0;
  
  // Stats
  stats = {
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0
  };

  constructor(
    private complianceService: ComplianceOfficerService,
    private authService: AuthService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    const currentUser = this.authService.currentUserValue;
    if (currentUser?.id) {
      this.complianceOfficerId = currentUser.id;
      this.loadForwardedDocuments();
    }
  }

  initializeForm(): void {
    this.processForm = this.fb.group({
      action: ['', Validators.required],
      remarks: ['', [Validators.required, Validators.minLength(20)]],
      sendBackToLoanOfficer: [true],
      recommendedAction: ['']
    });

    // Add conditional validation
    this.processForm.get('action')?.valueChanges.subscribe(action => {
      const recommendedActionControl = this.processForm.get('recommendedAction');
      if (action === 'APPROVE' || action === 'REJECT') {
        recommendedActionControl?.setValidators([Validators.required]);
      } else {
        recommendedActionControl?.clearValidators();
      }
      recommendedActionControl?.updateValueAndValidity();
    });
  }

  loadForwardedDocuments(): void {
    this.loading = true;
    this.complianceService.getForwardedDocuments(this.complianceOfficerId).subscribe({
      next: (documents) => {
        this.forwardedDocuments = documents;
        this.applyFilters();
        this.calculateStats();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading forwarded documents:', error);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredDocuments = this.forwardedDocuments.filter(doc => {
      const matchesStatus = this.filterStatus === 'ALL' || doc.status === this.filterStatus;
      const matchesSearch = !this.searchQuery || 
        doc.applicantName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        doc.documentType.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        doc.loanId.toString().includes(this.searchQuery) ||
        doc.forwardedBy.toLowerCase().includes(this.searchQuery.toLowerCase());
      
      return matchesStatus && matchesSearch;
    });
  }

  calculateStats(): void {
    this.stats.total = this.forwardedDocuments.length;
    this.stats.pending = this.forwardedDocuments.filter(d => d.status === 'PENDING_COMPLIANCE_REVIEW').length;
    this.stats.approved = this.forwardedDocuments.filter(d => d.status === 'APPROVED_BY_COMPLIANCE').length;
    this.stats.rejected = this.forwardedDocuments.filter(d => d.status === 'REJECTED_BY_COMPLIANCE').length;
  }

  openProcessModal(document: ForwardedDocument): void {
    this.selectedDocument = document;
    this.processForm.reset({
      sendBackToLoanOfficer: true
    });
    this.showProcessModal = true;
  }

  closeProcessModal(): void {
    this.showProcessModal = false;
    this.selectedDocument = null;
    this.processForm.reset();
  }

  viewDocument(document: ForwardedDocument): void {
    this.selectedDocument = document;
    this.showDocumentModal = true;
  }

  closeDocumentModal(): void {
    this.showDocumentModal = false;
  }

  processDocument(): void {
    if (this.processForm.invalid || !this.selectedDocument) {
      return;
    }

    this.processing = true;
    const formValue = this.processForm.value;
    
    const request = {
      documentId: this.selectedDocument.documentId,
      complianceOfficerId: this.complianceOfficerId,
      action: formValue.action,
      remarks: formValue.remarks,
      sendBackToLoanOfficer: formValue.sendBackToLoanOfficer
    };

    this.complianceService.processForwardedDocument(request).subscribe({
      next: (response) => {
        console.log('Document processed successfully:', response);
        
        // If sending back to loan officer, send additional feedback
        if (formValue.sendBackToLoanOfficer && formValue.recommendedAction) {
          const feedbackRequest = {
            documentId: this.selectedDocument!.documentId,
            complianceOfficerId: this.complianceOfficerId,
            feedback: formValue.remarks,
            requiresAction: formValue.recommendedAction
          };
          
          this.complianceService.sendDocumentBackToLoanOfficer(feedbackRequest).subscribe({
            next: () => {
              console.log('Feedback sent to loan officer');
            },
            error: (error) => {
              console.error('Error sending feedback:', error);
            }
          });
        }
        
        // Update local document status
        const docIndex = this.forwardedDocuments.findIndex(d => d.documentId === this.selectedDocument!.documentId);
        if (docIndex !== -1) {
          this.forwardedDocuments[docIndex].status = this.getNewStatus(formValue.action);
        }
        
        this.applyFilters();
        this.calculateStats();
        this.closeProcessModal();
        this.processing = false;
        
        alert(`Document ${formValue.action === 'APPROVE' ? 'approved' : 'rejected'} and sent back to loan officer successfully!`);
      },
      error: (error) => {
        console.error('Error processing document:', error);
        this.processing = false;
        alert('Error processing document. Please try again.');
      }
    });
  }

  getNewStatus(action: string): string {
    return action === 'APPROVE' ? 'APPROVED_BY_COMPLIANCE' : 'REJECTED_BY_COMPLIANCE';
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDING_COMPLIANCE_REVIEW':
        return 'badge bg-warning';
      case 'APPROVED_BY_COMPLIANCE':
        return 'badge bg-success';
      case 'REJECTED_BY_COMPLIANCE':
        return 'badge bg-danger';
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

  goToLoanReview(assignmentId: number): void {
    this.router.navigate(['/compliance-officer/review', assignmentId]);
  }
}
