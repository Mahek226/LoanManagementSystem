import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { DraftService, DraftApplication } from '@core/services/draft.service';
import { AuthService } from '@core/services/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-apply-loan',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './apply-loan.component.html',
  styleUrls: ['./apply-loan.component.scss']
})
export class ApplyLoanComponent implements OnInit, OnDestroy {
  currentStep!: number;
  totalSteps!: number;
  
  // Form groups for each step
  loanTypeForm!: FormGroup;
  personalDetailsForm!: FormGroup;
  loanDetailsForm!: FormGroup;
  financialDetailsForm!: FormGroup;
  documentsForm!: FormGroup;
  reviewForm!: FormGroup;

  // Step configuration
  steps = [
    { id: 1, title: 'Loan Type', icon: '🏠' },
    { id: 2, title: 'Personal Details', icon: '👤' },
    { id: 3, title: 'Loan Details', icon: '💰' },
    { id: 4, title: 'Financial Details', icon: '📊' },
    { id: 5, title: 'Documents', icon: '📄' },
    { id: 6, title: 'Review', icon: '✅' }
  ];

  // Loan types with their specific requirements
  loanTypes = [
    {
      id: 'home_loan',
      name: 'Home Loan',
      description: 'For purchasing or constructing a new home',
      icon: '🏠',
      maxAmount: 10000000,
      minAmount: 500000,
      maxTenure: 300, // months
      interestRate: '8.5% - 12%'
    },
    {
      id: 'home_improvement',
      name: 'Home Improvement Loan',
      description: 'For renovating or improving existing property',
      icon: '🔨',
      maxAmount: 2000000,
      minAmount: 100000,
      maxTenure: 84,
      interestRate: '9% - 14%'
    },
    {
      id: 'plot_loan',
      name: 'Plot/Land Loan',
      description: 'For purchasing residential plot or land',
      icon: '🌍',
      maxAmount: 5000000,
      minAmount: 300000,
      maxTenure: 180,
      interestRate: '9.5% - 13%'
    },
    {
      id: 'balance_transfer',
      name: 'Balance Transfer',
      description: 'Transfer existing home loan to get better rates',
      icon: '🔄',
      maxAmount: 15000000,
      minAmount: 500000,
      maxTenure: 300,
      interestRate: '8% - 11.5%'
    },
    {
      id: 'construction_loan',
      name: 'Construction Loan',
      description: 'For constructing house on owned land',
      icon: '🏗️',
      maxAmount: 8000000,
      minAmount: 500000,
      maxTenure: 240,
      interestRate: '9% - 13%'
    }
  ];

  selectedLoanType: any = null;
  
  // Draft management
  currentDraftId: string | null = null;
  applicantId: number = 0;
  autoSaveInterval: any;
  private subscriptions: Subscription[] = [];
  
  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private draftService: DraftService,
    private authService: AuthService
  ) {
    this.currentStep = 1;
    this.totalSteps = 6;
    
    // Get applicant ID from auth service
    const user = this.authService.currentUserValue;
    this.applicantId = user?.applicantId || user?.id || 0;
  }

  ngOnInit(): void {
    this.initializeForms();
    this.checkForDraftToResume();
    this.setupAutoSave();
  }
  
  ngOnDestroy(): void {
    this.clearAutoSave();
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initializeForms(): void {
    // Loan Type Form
    this.loanTypeForm = this.fb.group({
      loanType: ['', Validators.required]
    });

    // Personal Details Form
    this.personalDetailsForm = this.fb.group({
      // Applicant Details
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      middleName: [''],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      dateOfBirth: ['', Validators.required],
      gender: ['', Validators.required],
      maritalStatus: ['', Validators.required],
      nationality: ['Indian', Validators.required],
      
      // Contact Details
      emailAddress: ['', [Validators.required, Validators.email]],
      mobileNumber: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
      alternateNumber: ['', [Validators.pattern(/^[6-9]\d{9}$/)]],
      
      // Address Details
      currentAddress: ['', Validators.required],
      currentCity: ['', Validators.required],
      currentState: ['', Validators.required],
      currentPincode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
      residenceType: ['', Validators.required],
      yearsAtCurrentAddress: ['', [Validators.required, Validators.min(0)]],
      
      // Permanent Address
      permanentAddressSame: [false],
      permanentAddress: [''],
      permanentCity: [''],
      permanentState: [''],
      permanentPincode: [''],
      
      // Identity Details
      panNumber: ['', [Validators.required]],
      aadharNumber: ['', [Validators.required, Validators.pattern(/^\d{12}$/)]],
      
      // Co-applicant Details
      hasCoApplicant: [false],
      coApplicantName: [''],
      coApplicantRelation: [''],
      coApplicantPan: [''],
      coApplicantAadhar: ['']
    });

    // Loan Details Form
    this.loanDetailsForm = this.fb.group({
      loanAmount: ['', [Validators.required, Validators.min(100000)]],
      loanTenure: ['', [Validators.required, Validators.min(12)]],
      
      // Property Details
      propertyType: ['', Validators.required],
      propertyValue: ['', [Validators.required, Validators.min(0)]],
      propertyAddress: ['', Validators.required],
      propertyCity: ['', Validators.required],
      propertyState: ['', Validators.required],
      propertyPincode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
      
      // Construction/Purchase Details
      constructionStage: [''],
      builderName: [''],
      agreementValue: [''],
      stampDutyRegistration: [''],
      
      // Existing Loan Details (for balance transfer)
      existingLoanAmount: [''],
      existingBankName: [''],
      existingEmi: [''],
      existingInterestRate: ['']
    });

    // Financial Details Form
    this.financialDetailsForm = this.fb.group({
      // Employment Details
      employmentType: ['', Validators.required],
      companyName: ['', Validators.required],
      designation: ['', Validators.required],
      workExperience: ['', [Validators.required, Validators.min(0)]],
      currentJobExperience: ['', [Validators.required, Validators.min(0)]],
      officeAddress: ['', Validators.required],
      officeCity: ['', Validators.required],
      officeState: ['', Validators.required],
      officePincode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
      
      // Income Details
      grossMonthlyIncome: ['', [Validators.required, Validators.min(0)]],
      netMonthlyIncome: ['', [Validators.required, Validators.min(0)]],
      otherIncome: ['', [Validators.min(0)]],
      otherIncomeSource: [''],
      
      // Existing EMIs and Liabilities
      existingHomeLoanEmi: ['', [Validators.min(0)]],
      existingPersonalLoanEmi: ['', [Validators.min(0)]],
      existingCarLoanEmi: ['', [Validators.min(0)]],
      existingCreditCardEmi: ['', [Validators.min(0)]],
      otherEmi: ['', [Validators.min(0)]],
      
      // Bank Details
      bankName: ['', Validators.required],
      accountNumber: ['', [Validators.required, Validators.pattern(/^\d{9,18}$/)]],
      ifscCode: ['', [Validators.required]],
      accountType: ['', Validators.required],
      
      // Co-applicant Financial Details
      coApplicantIncome: [''],
      coApplicantEmployment: [''],
      coApplicantCompany: ['']
    });

    // Documents Form
    this.documentsForm = this.fb.group({
      // Identity Documents
      panCard: [null, Validators.required],
      aadharCard: [null, Validators.required],
      passport: [null],
      
      // Address Proof
      addressProof: [null, Validators.required],
      
      // Income Documents
      salarySlips: [null, Validators.required],
      bankStatements: [null, Validators.required],
      itr: [null, Validators.required],
      form16: [null],
      
      // Property Documents
      propertyDocuments: [null],
      agreementCopy: [null],
      approvedPlan: [null],
      
      // Additional Documents
      photograph: [null, Validators.required],
      signature: [null, Validators.required],
      
      // Co-applicant Documents
      coApplicantPan: [null],
      coApplicantAadhar: [null],
      coApplicantIncomeDocs: [null]
    });

    // Review Form
    this.reviewForm = this.fb.group({
      termsAccepted: [false, Validators.requiredTrue],
      declarationAccepted: [false, Validators.requiredTrue]
    });
  }

  getCurrentForm(): FormGroup {
    switch (this.currentStep) {
      case 1: return this.loanTypeForm;
      case 2: return this.personalDetailsForm;
      case 3: return this.loanDetailsForm;
      case 4: return this.financialDetailsForm;
      case 5: return this.documentsForm;
      case 6: return this.reviewForm;
      default: return this.loanTypeForm;
    }
  }

  nextStep(): void {
    const currentForm = this.getCurrentForm();
    if (currentForm.valid && this.currentStep < this.totalSteps) {
      if (this.currentStep === 1) {
        this.selectedLoanType = this.loanTypes.find(type => type.id === this.loanTypeForm.get('loanType')?.value);
        this.updateLoanDetailsValidators();
      }
      this.currentStep++;
      this.saveDraft(); // Auto-save on step change
    } else {
      this.markFormGroupTouched(currentForm);
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      this.saveDraft(); // Auto-save on step change
    }
  }

  updateLoanDetailsValidators(): void {
    if (this.selectedLoanType) {
      const loanAmountControl = this.loanDetailsForm.get('loanAmount');
      const loanTenureControl = this.loanDetailsForm.get('loanTenure');
      
      loanAmountControl?.setValidators([
        Validators.required,
        Validators.min(this.selectedLoanType.minAmount),
        Validators.max(this.selectedLoanType.maxAmount)
      ]);
      
      loanTenureControl?.setValidators([
        Validators.required,
        Validators.min(12),
        Validators.max(this.selectedLoanType.maxTenure)
      ]);
      
      loanAmountControl?.updateValueAndValidity();
      loanTenureControl?.updateValueAndValidity();
    }
  }

  onLoanTypeSelect(loanTypeId: string): void {
    this.loanTypeForm.patchValue({ loanType: loanTypeId });
    this.selectedLoanType = this.loanTypes.find(type => type.id === loanTypeId);
  }

  onPermanentAddressChange(isSame: boolean): void {
    const permanentFields = ['permanentAddress', 'permanentCity', 'permanentState', 'permanentPincode'];
    
    if (isSame) {
      // Copy current address to permanent address
      this.personalDetailsForm.patchValue({
        permanentAddress: this.personalDetailsForm.get('currentAddress')?.value,
        permanentCity: this.personalDetailsForm.get('currentCity')?.value,
        permanentState: this.personalDetailsForm.get('currentState')?.value,
        permanentPincode: this.personalDetailsForm.get('currentPincode')?.value
      });
      
      // Remove validators for permanent address fields
      permanentFields.forEach(field => {
        this.personalDetailsForm.get(field)?.clearValidators();
        this.personalDetailsForm.get(field)?.updateValueAndValidity();
      });
    } else {
      // Add validators for permanent address fields
      permanentFields.forEach(field => {
        this.personalDetailsForm.get(field)?.setValidators([Validators.required]);
        this.personalDetailsForm.get(field)?.updateValueAndValidity();
      });
    }
  }

  onCoApplicantChange(hasCoApplicant: boolean): void {
    const coApplicantFields = ['coApplicantName', 'coApplicantRelation', 'coApplicantPan', 'coApplicantAadhar'];
    
    if (hasCoApplicant) {
      coApplicantFields.forEach(field => {
        this.personalDetailsForm.get(field)?.setValidators([Validators.required]);
        this.personalDetailsForm.get(field)?.updateValueAndValidity();
      });
    } else {
      coApplicantFields.forEach(field => {
        this.personalDetailsForm.get(field)?.clearValidators();
        this.personalDetailsForm.get(field)?.updateValueAndValidity();
      });
    }
  }

  submitApplication(): void {
    if (this.isAllFormsValid()) {
      const applicationData = {
        loanType: this.loanTypeForm.value,
        personalDetails: this.personalDetailsForm.value,
        loanDetails: this.loanDetailsForm.value,
        financialDetails: this.financialDetailsForm.value,
        documents: this.documentsForm.value
      };
      
      console.log('Application Data:', applicationData);
      // TODO: Submit to backend
      
      // Clear draft after successful submission
      this.clearSavedData();
      
      alert('Loan application submitted successfully! You will receive a confirmation email shortly.');
      this.router.navigate(['/applicant/dashboard']);
    }
  }

  isAllFormsValid(): boolean {
    return this.loanTypeForm.valid &&
           this.personalDetailsForm.valid &&
           this.loanDetailsForm.valid &&
           this.financialDetailsForm.valid &&
           this.documentsForm.valid &&
           this.reviewForm.valid;
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  onFileSelected(event: any, controlName: string): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert('File size should not exceed 5MB');
        return;
      }
      
      this.documentsForm.patchValue({
        [controlName]: file
      });
    }
  }

  calculateEmi(): number {
    const loanAmount = this.loanDetailsForm.get('loanAmount')?.value || 0;
    const tenure = this.loanDetailsForm.get('loanTenure')?.value || 0;
    const rate = 0.09; // 9% annual interest rate (example)
    
    if (loanAmount && tenure) {
      const monthlyRate = rate / 12;
      const emi = (loanAmount * monthlyRate * Math.pow(1 + monthlyRate, tenure)) / 
                  (Math.pow(1 + monthlyRate, tenure) - 1);
      return Math.round(emi);
    }
    return 0;
  }

  goBack(): void {
    this.router.navigate(['/applicant/dashboard']);
  }

  // Draft Management Methods
  checkForDraftToResume(): void {
    // Check if we're resuming a specific draft
    const draftId = this.route.snapshot.queryParams['draftId'];
    if (draftId) {
      this.loadDraft(draftId);
      return;
    }
    
    // Check for existing draft for this applicant
    const latestDraft = this.draftService.getLatestDraft(this.applicantId);
    if (latestDraft) {
      // Ask user if they want to resume
      const resume = confirm(`You have an incomplete application (${latestDraft.completionPercentage}% complete). Would you like to resume it?`);
      if (resume) {
        this.loadDraft(latestDraft.id);
      } else {
        // Start fresh
        this.clearSavedData();
      }
    }
  }
  
  loadDraft(draftId: string): void {
    const draft = this.draftService.getDraft(draftId);
    if (draft) {
      this.currentDraftId = draft.id;
      this.currentStep = draft.currentStep;
      this.selectedLoanType = draft.selectedLoanType;
      
      // Load form data
      if (draft.formData.loanTypeForm) {
        this.loanTypeForm.patchValue(draft.formData.loanTypeForm);
      }
      if (draft.formData.personalDetailsForm) {
        this.personalDetailsForm.patchValue(draft.formData.personalDetailsForm);
      }
      if (draft.formData.loanDetailsForm) {
        this.loanDetailsForm.patchValue(draft.formData.loanDetailsForm);
      }
      if (draft.formData.financialDetailsForm) {
        this.financialDetailsForm.patchValue(draft.formData.financialDetailsForm);
      }
      if (draft.formData.documentsForm) {
        this.documentsForm.patchValue(draft.formData.documentsForm);
      }
      
      // Update validators if loan type is selected
      if (this.selectedLoanType) {
        this.updateLoanDetailsValidators();
      }
    }
  }
  
  saveDraft(): void {
    const formData = {
      loanTypeForm: this.loanTypeForm.value,
      personalDetailsForm: this.personalDetailsForm.value,
      loanDetailsForm: this.loanDetailsForm.value,
      financialDetailsForm: this.financialDetailsForm.value,
      documentsForm: this.documentsForm.value
    };
    
    const completionPercentage = this.draftService.calculateCompletionPercentage(formData, this.currentStep);
    
    const draftData: Partial<DraftApplication> = {
      id: this.currentDraftId || undefined,
      applicantId: this.applicantId,
      currentStep: this.currentStep,
      totalSteps: this.totalSteps,
      completionPercentage: completionPercentage,
      selectedLoanType: this.selectedLoanType,
      formData: formData
    };
    
    this.currentDraftId = this.draftService.saveDraft(draftData);
  }
  
  setupAutoSave(): void {
    // Auto-save every 30 seconds
    this.autoSaveInterval = setInterval(() => {
      this.saveDraft();
    }, 30000);
  }
  
  clearAutoSave(): void {
    if (this.autoSaveInterval) {
      clearInterval(this.autoSaveInterval);
    }
  }

  getFormCompletionPercentage(): number {
    let totalFields = 0;
    let filledFields = 0;
    
    // Count loan type
    totalFields += 1;
    if (this.selectedLoanType) filledFields += 1;
    
    // Count personal details fields
    const personalFields = Object.keys(this.personalDetailsForm.controls);
    totalFields += personalFields.length;
    personalFields.forEach(field => {
      const value = this.personalDetailsForm.get(field)?.value;
      if (value && value !== '') filledFields += 1;
    });
    
    // Count loan details fields
    const loanFields = Object.keys(this.loanDetailsForm.controls);
    totalFields += loanFields.length;
    loanFields.forEach(field => {
      const value = this.loanDetailsForm.get(field)?.value;
      if (value && value !== '') filledFields += 1;
    });
    
    // Count financial fields
    const financialFields = Object.keys(this.financialDetailsForm.controls);
    totalFields += financialFields.length;
    financialFields.forEach(field => {
      const value = this.financialDetailsForm.get(field)?.value;
      if (value && value !== '') filledFields += 1;
    });
    
    return Math.round((filledFields / totalFields) * 100);
  }

  clearSavedData(): void {
    if (this.currentDraftId) {
      this.draftService.deleteDraft(this.currentDraftId);
      this.currentDraftId = null;
    }
  }
  
  saveAsDraft(): void {
    this.saveDraft();
    alert('Application saved as draft! You can resume it later from your dashboard.');
    this.router.navigate(['/applicant/dashboard']);
  }

  // Helper method to get current step as number (for template)
  getCurrentStep(): number {
    return this.currentStep;
  }

  // Helper method to check if current step matches
  isCurrentStep(step: number): boolean {
    return this.currentStep === step;
  }
}
