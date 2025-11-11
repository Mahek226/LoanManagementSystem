import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ApplicantService, LoanType } from '@core/services/applicant.service';

@Component({
  selector: 'app-loan-types',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './loan-types.component.html',
  styleUrls: ['./loan-types.component.css']
})
export class LoanTypesComponent implements OnInit {
  loanTypes: LoanType[] = [];
  selectedLoanType: LoanType | null = null;

  constructor(
    private applicantService: ApplicantService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loanTypes = this.applicantService.getLoanTypes();
  }

  selectLoanType(loanType: LoanType): void {
    this.selectedLoanType = loanType;
  }

  applyForLoan(loanType: LoanType): void {
    // Store selected loan type in localStorage
    localStorage.setItem('selectedLoanType', JSON.stringify(loanType));
    // Navigate to apply loan page
    this.router.navigate(['/applicant/apply-loan']);
  }

  formatCurrency(amount: number): string {
    return this.applicantService.formatCurrency(amount);
  }

  getInterestRateRange(loanType: LoanType): string {
    return `${loanType.interestRateMin}% - ${loanType.interestRateMax}%`;
  }

  getTenureRange(loanType: LoanType): string {
    const minYears = Math.floor(loanType.minTenure / 12);
    const maxYears = Math.floor(loanType.maxTenure / 12);
    
    if (maxYears < 1) {
      return `${loanType.minTenure} - ${loanType.maxTenure} months`;
    }
    
    return `${minYears} - ${maxYears} years`;
  }

  closeModal(): void {
    this.selectedLoanType = null;
  }
}
