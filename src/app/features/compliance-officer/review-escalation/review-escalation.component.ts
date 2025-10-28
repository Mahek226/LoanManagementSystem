import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { 
  ComplianceOfficerService, 
  ComplianceEscalation, 
  ComplianceDecisionRequest,
  FraudHistoryRecord,
  AdditionalDocumentRequest
} from '../../../core/services/compliance-officer.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-review-escalation',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './review-escalation.component.html',
  styleUrls: ['./review-escalation.component.css']
})
export class ReviewEscalationComponent implements OnInit {
  escalation: ComplianceEscalation | null = null;
  fraudHistory: FraudHistoryRecord[] = [];
  documents: any[] = [];
  loading = false;
  processing = false;
  errorMessage = '';
  successMessage = '';

  // Decision form
  selectedAction: 'APPROVE' | 'REJECT' | 'REQUEST_MORE_INFO' | null = null;
  remarks = '';
  rejectionReason = '';
  additionalChecks: string[] = [];

  // Modal states
  showApproveModal = false;
  showRejectModal = false;
  showRequestInfoModal = false;
  showFraudHistoryModal = false;
  showDocumentsModal = false;
  showRequestDocumentsModal = false;

  // Additional document request
  selectedDocumentTypes: string[] = [];
  documentRequestReason = '';
  availableDocumentTypes = ['PAN_CARD', 'AADHAAR_CARD', 'INCOME_PROOF', 'BANK_STATEMENT', 'PROPERTY_DOCUMENTS', 'EMPLOYMENT_PROOF', 'CREDIT_REPORT'];

  constructor(
    private complianceService: ComplianceOfficerService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const assignmentId = this.route.snapshot.paramMap.get('id');
    if (assignmentId) {
      this.loadEscalationDetails(+assignmentId);
    } else {
      this.errorMessage = 'Invalid assignment ID';
      this.loading = false;
    }
  }

  loadFraudHistory(): void {
    if (!this.escalation) return;
    
    this.complianceService.getFraudHistory(this.escalation.applicantId).subscribe({
      next: (history) => {
        this.fraudHistory = history;
      },
      error: (err) => {
        console.error('Error loading fraud history:', err);
      }
    });
  }

  loadDocuments(): void {
    if (!this.escalation) return;
    
    this.complianceService.getLoanDocuments(this.escalation.loanId).subscribe({
      next: (docs) => {
        this.documents = docs;
      },
      error: (err) => {
        console.error('Error loading documents:', err);
      }
    });
  }

  loadEscalationDetails(assignmentId: number): void {
    this.loading = true;
    this.complianceService.getEscalationDetails(assignmentId).subscribe({
      next: (escalation) => {
        this.escalation = escalation;
        this.loading = false;
        // Load additional data
        this.loadFraudHistory();
        this.loadDocuments();
      },
      error: (error) => {
        console.error('Error loading escalation details:', error);
        this.errorMessage = 'Failed to load escalation details. Please try again.';
        this.loading = false;
      }
    });
  }

  openApproveModal(): void {
    this.selectedAction = 'APPROVE';
    this.showApproveModal = true;
    setTimeout(() => {
      const modalElement = document.getElementById('approveModal');
      if (modalElement) {
        const modal = new (window as any).bootstrap.Modal(modalElement);
        modal.show();
      }
    }, 100);
  }

  openRejectModal(): void {
    this.selectedAction = 'REJECT';
    this.showRejectModal = true;
    setTimeout(() => {
      const modalElement = document.getElementById('rejectModal');
      if (modalElement) {
        const modal = new (window as any).bootstrap.Modal(modalElement);
        modal.show();
      }
    }, 100);
  }

  openRequestInfoModal(): void {
    this.selectedAction = 'REQUEST_MORE_INFO';
    this.showRequestInfoModal = true;
    setTimeout(() => {
      const modalElement = document.getElementById('requestInfoModal');
      if (modalElement) {
        const modal = new (window as any).bootstrap.Modal(modalElement);
        modal.show();
      }
    }, 100);
  }

  closeModals(): void {
    const modals = ['approveModal', 'rejectModal', 'requestInfoModal'];
    modals.forEach(modalId => {
      const modalElement = document.getElementById(modalId);
      if (modalElement) {
        const modal = (window as any).bootstrap.Modal.getInstance(modalElement);
        if (modal) {
          modal.hide();
        }
      }
    });

    setTimeout(() => {
      this.showApproveModal = false;
      this.showRejectModal = false;
      this.showRequestInfoModal = false;
      this.remarks = '';
      this.rejectionReason = '';
      this.selectedAction = null;
    }, 300);
  }

  processDecision(): void {
    if (!this.escalation || !this.selectedAction) return;

    const user = this.authService.currentUserValue;
    if (!user || !user.id) {
      this.errorMessage = 'User not authenticated';
      return;
    }

    const request: ComplianceDecisionRequest = {
      assignmentId: this.escalation.assignmentId,
      action: this.selectedAction,
      remarks: this.remarks || undefined,
      rejectionReason: this.selectedAction === 'REJECT' ? this.rejectionReason : undefined,
      additionalChecks: this.additionalChecks.length > 0 ? this.additionalChecks : undefined
    };

    this.processing = true;
    this.errorMessage = '';

    this.complianceService.processDecision(user.id, request).subscribe({
      next: (response) => {
        this.processing = false;
        this.successMessage = `Application ${this.selectedAction?.toLowerCase() || 'processed'} successfully!`;
        this.closeModals();

        // Redirect after 2 seconds
        setTimeout(() => {
          this.router.navigate(['/compliance-officer/escalations']);
        }, 2000);
      },
      error: (error) => {
        console.error('Error processing decision:', error);
        this.errorMessage = error.error?.message || 'Failed to process decision. Please try again.';
        this.processing = false;
      }
    });
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

  getRiskColor(riskLevel: string): string {
    return this.complianceService.getRiskColor(riskLevel);
  }

  goBack(): void {
    this.router.navigate(['/compliance-officer/escalations']);
  }

  // Fraud History
  openFraudHistoryModal(): void {
    this.showFraudHistoryModal = true;
    this.loadFraudHistory();
  }

  closeFraudHistoryModal(): void {
    this.showFraudHistoryModal = false;
  }

  // Documents
  openDocumentsModal(): void {
    this.showDocumentsModal = true;
    this.loadDocuments();
  }

  closeDocumentsModal(): void {
    this.showDocumentsModal = false;
  }

  // Request Additional Documents
  openRequestDocumentsModal(): void {
    this.showRequestDocumentsModal = true;
    this.selectedDocumentTypes = [];
    this.documentRequestReason = '';
  }

  closeRequestDocumentsModal(): void {
    this.showRequestDocumentsModal = false;
    this.selectedDocumentTypes = [];
    this.documentRequestReason = '';
  }

  toggleDocumentType(docType: string): void {
    const index = this.selectedDocumentTypes.indexOf(docType);
    if (index > -1) {
      this.selectedDocumentTypes.splice(index, 1);
    } else {
      this.selectedDocumentTypes.push(docType);
    }
  }

  requestAdditionalDocuments(): void {
    if (!this.escalation || this.selectedDocumentTypes.length === 0 || !this.documentRequestReason) {
      this.errorMessage = 'Please select documents and provide a reason';
      return;
    }

    const user = this.authService.currentUserValue;
    if (!user || !user.id) {
      this.errorMessage = 'User not authenticated';
      return;
    }

    const request: AdditionalDocumentRequest = {
      loanId: this.escalation.loanId,
      applicantId: this.escalation.applicantId,
      documentTypes: this.selectedDocumentTypes,
      reason: this.documentRequestReason,
      remarks: this.remarks
    };

    this.processing = true;
    this.complianceService.requestAdditionalDocuments(user.id, request).subscribe({
      next: () => {
        this.successMessage = 'Document request sent to applicant via Loan Officer!';
        this.closeRequestDocumentsModal();
        setTimeout(() => this.successMessage = '', 3000);
        this.processing = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to request documents';
        this.processing = false;
      }
    });
  }

  formatDocumentType(type: string): string {
    return type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
  }

  getDocumentStatusClass(status: string): string {
    const classes: any = {
      'PENDING': 'badge bg-warning',
      'VERIFIED': 'badge bg-success',
      'REJECTED': 'badge bg-danger'
    };
    return classes[status] || 'badge bg-secondary';
  }

  getFraudStatusClass(status: string): string {
    const classes: any = {
      'DETECTED': 'badge bg-danger',
      'RESOLVED': 'badge bg-success',
      'UNDER_INVESTIGATION': 'badge bg-warning',
      'CLEARED': 'badge bg-info'
    };
    return classes[status] || 'badge bg-secondary';
  }
}
