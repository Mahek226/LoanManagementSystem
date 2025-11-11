import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-applicant-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './applicant-dashboard.component.html',
  styleUrls: ['./applicant-dashboard.component.scss']
})
export class ApplicantDashboardComponent {
  userName: string;
  savedApplicationData: any = null;
  formProgress: number = 0;

  constructor(
    private authService: AuthService,
    public router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.userName = user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : 'Applicant';
    this.loadSavedApplicationData();
  }

  loadSavedApplicationData(): void {
    const savedData = localStorage.getItem('loanApplicationData');
    if (savedData) {
      this.savedApplicationData = JSON.parse(savedData);
      this.formProgress = this.calculateFormProgress();
    }
  }

  calculateFormProgress(): number {
    if (!this.savedApplicationData) return 0;
    
    let totalFields = 0;
    let filledFields = 0;
    
    // Count loan type
    totalFields += 1;
    if (this.savedApplicationData.selectedLoanType) filledFields += 1;
    
    // Count personal details fields
    if (this.savedApplicationData.personalDetailsForm) {
      const personalFields = Object.keys(this.savedApplicationData.personalDetailsForm);
      totalFields += personalFields.length;
      personalFields.forEach(field => {
        const value = this.savedApplicationData.personalDetailsForm[field];
        if (value && value !== '') filledFields += 1;
      });
    }
    
    // Count loan details fields
    if (this.savedApplicationData.loanDetailsForm) {
      const loanFields = Object.keys(this.savedApplicationData.loanDetailsForm);
      totalFields += loanFields.length;
      loanFields.forEach(field => {
        const value = this.savedApplicationData.loanDetailsForm[field];
        if (value && value !== '') filledFields += 1;
      });
    }
    
    // Count financial fields
    if (this.savedApplicationData.financialDetailsForm) {
      const financialFields = Object.keys(this.savedApplicationData.financialDetailsForm);
      totalFields += financialFields.length;
      financialFields.forEach(field => {
        const value = this.savedApplicationData.financialDetailsForm[field];
        if (value && value !== '') filledFields += 1;
      });
    }
    
    return Math.round((filledFields / totalFields) * 100);
  }

  continueApplication(): void {
    this.router.navigate(['/applicant/apply-loan']);
  }

  startNewApplication(): void {
    localStorage.removeItem('loanApplicationData');
    this.savedApplicationData = null;
    this.formProgress = 0;
    this.router.navigate(['/applicant/apply-loan']);
  }

  logout(): void {
    this.authService.logout();
  }
}
