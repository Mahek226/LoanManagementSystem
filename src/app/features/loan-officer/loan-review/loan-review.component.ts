import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { LoanOfficerService, LoanScreeningResponse, LoanScreeningRequest } from '@core/services/loan-officer.service';

@Component({
  selector: 'app-loan-review',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loan-review.component.html',
  styleUrl: './loan-review.component.css'
})
export class LoanReviewComponent implements OnInit {
  assignmentId: number = 0;
  officerId: number = 0;
  loading = false;
  processing = false;
  error = '';
  success = '';

  loan: LoanScreeningResponse | null = null;

  // Action form
  selectedAction: string = '';
  remarks: string = '';
  rejectionReason: string = '';

  // Modal state
  showActionModal = false;
  actionType: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private loanOfficerService: LoanOfficerService
  ) {
    const user = this.authService.currentUserValue;
    this.officerId = user?.officerId || user?.userId || 0;
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.assignmentId = +params['id'];
      if (this.assignmentId) {
        this.loadLoanDetails();
      }
    });
  }

  loadLoanDetails(): void {
    this.loading = true;
    this.error = '';

    this.loanOfficerService.getLoanDetailsForScreening(this.assignmentId).subscribe({
      next: (data) => {
        this.loan = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading loan details:', err);
        this.error = 'Failed to load loan details. Please try again.';
        this.loading = false;
      }
    });
  }

  openActionModal(action: string): void {
    this.actionType = action;
    this.selectedAction = action;
    this.showActionModal = true;
    this.remarks = '';
    this.rejectionReason = '';
  }

  closeActionModal(): void {
    this.showActionModal = false;
    this.actionType = '';
    this.selectedAction = '';
    this.remarks = '';
    this.rejectionReason = '';
  }

  confirmAction(): void {
    if (this.actionType === 'ESCALATE_TO_COMPLIANCE') {
      this.escalateToCompliance();
    } else {
      this.processScreening();
    }
  }

  processScreening(): void {
    if (!this.selectedAction) {
      this.error = 'Please select an action';
      return;
    }

    if (this.selectedAction === 'REJECT' && !this.rejectionReason) {
      this.error = 'Please provide a rejection reason';
      return;
    }

    this.processing = true;
    this.error = '';
    this.success = '';

    const request: LoanScreeningRequest = {
      assignmentId: this.assignmentId,
      action: this.selectedAction,
      remarks: this.remarks,
      rejectionReason: this.selectedAction === 'REJECT' ? this.rejectionReason : undefined
    };

    this.loanOfficerService.processLoanScreening(this.officerId, request).subscribe({
      next: (response) => {
        this.processing = false;
        this.success = `Loan ${this.selectedAction.toLowerCase()} successfully!`;
        this.closeActionModal();
        
        // Reload loan details
        setTimeout(() => {
          this.loadLoanDetails();
        }, 1000);

        // Navigate back after 2 seconds
        setTimeout(() => {
          this.router.navigate(['/loan-officer/assigned-loans']);
        }, 2000);
      },
      error: (err) => {
        console.error('Error processing loan:', err);
        this.error = err.error?.message || 'Failed to process loan. Please try again.';
        this.processing = false;
      }
    });
  }

  escalateToCompliance(): void {
    this.processing = true;
    this.error = '';
    this.success = '';

    this.loanOfficerService.escalateToCompliance(this.assignmentId, this.remarks).subscribe({
      next: (response) => {
        this.processing = false;
        this.success = 'Loan escalated to compliance officer successfully!';
        this.closeActionModal();
        
        // Navigate back after 2 seconds
        setTimeout(() => {
          this.router.navigate(['/loan-officer/assigned-loans']);
        }, 2000);
      },
      error: (err) => {
        console.error('Error escalating loan:', err);
        this.error = err.error?.message || 'Failed to escalate loan. Please try again.';
        this.processing = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/loan-officer/assigned-loans']);
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

  getRiskPercentage(riskScore: number): number {
    return riskScore;
  }

  getRiskProgressColor(riskScore: number): string {
    if (riskScore < 30) return 'bg-success';
    if (riskScore < 70) return 'bg-warning';
    return 'bg-danger';
  }
}
