import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApplicantService, ApplicationProgress } from '@core/services/applicant.service';

@Component({
  selector: 'app-progress-tracker',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './progress-tracker.component.html',
  styleUrls: ['./progress-tracker.component.css']
})
export class ProgressTrackerComponent implements OnInit {
  @Input() loanId?: number;
  
  progress: ApplicationProgress | null = null;
  loading: boolean = true;

  // Default progress steps for new applications
  defaultSteps = [
    {
      stepNumber: 1,
      stepName: 'Application Submitted',
      status: 'COMPLETED' as const,
      description: 'Your loan application has been successfully submitted',
      completedAt: new Date().toISOString()
    },
    {
      stepNumber: 2,
      stepName: 'Document Verification',
      status: 'IN_PROGRESS' as const,
      description: 'We are verifying your submitted documents'
    },
    {
      stepNumber: 3,
      stepName: 'Credit Assessment',
      status: 'PENDING' as const,
      description: 'Credit score and financial assessment'
    },
    {
      stepNumber: 4,
      stepName: 'Loan Officer Review',
      status: 'PENDING' as const,
      description: 'Application review by loan officer'
    },
    {
      stepNumber: 5,
      stepName: 'Final Approval',
      status: 'PENDING' as const,
      description: 'Final approval and disbursement'
    }
  ];

  constructor(private applicantService: ApplicantService) {}

  ngOnInit(): void {
    if (this.loanId) {
      this.loadProgress();
    } else {
      this.setDefaultProgress();
    }
  }

  loadProgress(): void {
    if (!this.loanId) return;
    
    this.applicantService.getApplicationProgress(this.loanId).subscribe({
      next: (progress) => {
        this.progress = progress;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading progress:', error);
        this.setDefaultProgress();
      }
    });
  }

  setDefaultProgress(): void {
    this.progress = {
      loanId: this.loanId || 0,
      currentStep: 2,
      totalSteps: 5,
      steps: this.defaultSteps,
      overallProgress: 20,
      estimatedCompletion: this.getEstimatedCompletion()
    };
    this.loading = false;
  }

  getEstimatedCompletion(): string {
    const date = new Date();
    date.setDate(date.getDate() + 7); // 7 days from now
    return date.toISOString();
  }

  getStepIcon(status: string): string {
    switch(status) {
      case 'COMPLETED': return 'fa-check-circle';
      case 'IN_PROGRESS': return 'fa-spinner fa-spin';
      case 'FAILED': return 'fa-times-circle';
      default: return 'fa-circle';
    }
  }

  getStepClass(status: string): string {
    switch(status) {
      case 'COMPLETED': return 'step-completed';
      case 'IN_PROGRESS': return 'step-in-progress';
      case 'FAILED': return 'step-failed';
      default: return 'step-pending';
    }
  }

  formatDate(dateString: string): string {
    return this.applicantService.formatDate(dateString);
  }
}
