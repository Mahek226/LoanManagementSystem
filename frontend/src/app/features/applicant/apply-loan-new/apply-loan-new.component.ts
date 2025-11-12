import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { ApplicantService, ApplicantProfile } from '@core/services/applicant.service';
import { DraftService, DraftApplication } from '@core/services/draft.service';
import { ToastService } from '@core/services/toast.service';
import { FormValidators } from '@core/validators/form-validators';
import { 
  LoanApplicationService, 
  LoanApplicationForm, 
  BasicDetails,
  ApplicantDetails,
  FinancialDetails,
  DocumentUpload,
  Declaration,
  EMICalculation,
  DocumentRequirement,
  UploadProgress
} from '@core/services/loan-application.service';

@Component({
  selector: 'app-apply-loan-new',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './apply-loan-new.component.html',
  styleUrls: ['./apply-loan-new.component.css']
})
export class ApplyLoanNewComponent implements OnInit, OnDestroy {
  // Current step
  currentStep: number = 1;
  totalSteps: number = 6;
  
  // Forms
  basicDetailsForm!: FormGroup;
  applicantDetailsForm!: FormGroup;
  financialDetailsForm!: FormGroup;
  declarationsForm!: FormGroup;
  
  // Data
  application: LoanApplicationForm | null = null;
  profile: ApplicantProfile | null = null;
  selectedLoanType: string = '';
  
  // EMI Calculation
  emiCalculation: EMICalculation | null = null;
  showEMIDetails: boolean = false;
  
  // Documents
  requiredDocuments: DocumentRequirement[] = [];
  uploadedDocuments: DocumentUpload[] = [];
  uploadProgress: { [key: string]: UploadProgress } = {};
  dragOver: boolean = false;
  
  // Loading & Error states
  loading: boolean = false;
  submitting: boolean = false;
  success: string = '';
  
  // Draft functionality
  isDraftSaving: boolean = false;
  draftSaved: boolean = false;
  currentDraftId: string = '';
  autoSaveInterval: any;
  
  // Subscriptions
  private subscriptions: Subscription[] = [];
  
  // Applicant ID
  applicantId: number = 0;

  // Select All functionality for declarations
  selectAllDeclarations: boolean = false;
  private isTogglingAll: boolean = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
    private applicantService: ApplicantService,
    private loanApplicationService: LoanApplicationService,
    private draftService: DraftService,
    private toastService: ToastService
  ) {
    const user = this.authService.currentUserValue;
    this.applicantId = user?.id || 0;
  }

  ngOnInit(): void {
    this.initializeForms();
    this.loadProfile();
    this.loadLoanType();
    this.subscribeToApplicationState();
    this.initializeDraftFunctionality();
    
    // Make debug method accessible from browser console
    (window as any).debugDeclarationForm = () => this.debugDeclarationForm();
    (window as any).testSelectAll = () => {
      this.selectAllDeclarations = true;
      this.toggleAllDeclarations();
    };
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.clearAutoSave();
  }

  // ==================== Initialization ====================

  initializeForms(): void {
    // Step 1: Basic Details Form
    this.basicDetailsForm = this.fb.group({
      loanType: ['', Validators.required],
      loanAmount: [0, [Validators.required, Validators.min(1000), Validators.max(10000000), FormValidators.positiveNumberValidator()]],
      tenure: [12, [Validators.required, Validators.min(6), Validators.max(360)]],
      hasCoApplicant: [false],
      coApplicantName: ['', [FormValidators.fullNameValidator()]],
      coApplicantRelation: ['', [FormValidators.nameValidator()]],
      hasCollateral: [false],
      collateralType: ['', [FormValidators.nameValidator()]],
      collateralValue: [0, [FormValidators.positiveNumberValidator()]]
    });

    // Step 2: Applicant Details Form
    this.applicantDetailsForm = this.fb.group({
      // Applicant Information
      firstName: ['', [Validators.required, FormValidators.nameValidator(), Validators.minLength(2), Validators.maxLength(50)]],
      middleName: ['', [FormValidators.nameValidator(), Validators.maxLength(50)]],
      lastName: ['', [Validators.required, FormValidators.nameValidator(), Validators.minLength(2), Validators.maxLength(50)]],
      dateOfBirth: ['', [Validators.required, FormValidators.birthDateValidator()]],
      gender: ['', Validators.required],
      maritalStatus: ['', Validators.required],
      
      // Contact Information
      emailAddress: ['', [Validators.required, FormValidators.emailValidator()]],
      mobileNumber: ['', [Validators.required, FormValidators.mobileNumberValidator()]],
      alternateNumber: ['', [FormValidators.mobileNumberValidator()]],
      
      // Current Address
      currentAddress: ['', [Validators.required, FormValidators.addressValidator(), Validators.minLength(10), Validators.maxLength(200)]],
      currentCity: ['', [Validators.required, FormValidators.nameValidator(), Validators.minLength(2), Validators.maxLength(50)]],
      currentState: ['', [Validators.required, FormValidators.nameValidator(), Validators.minLength(2), Validators.maxLength(50)]],
      currentPincode: ['', [Validators.required, FormValidators.pincodeValidator()]],
      residenceType: ['', Validators.required],
      yearsAtCurrentAddress: ['', [Validators.required, Validators.min(0), Validators.max(100)]],
      
      // Permanent Address
      permanentAddressSame: [false],
      permanentAddress: ['', [FormValidators.addressValidator(), Validators.maxLength(200)]],
      permanentCity: ['', [FormValidators.nameValidator(), Validators.maxLength(50)]],
      permanentState: ['', [FormValidators.nameValidator(), Validators.maxLength(50)]],
      permanentPincode: ['', [FormValidators.pincodeValidator()]],
      
      // Identity Details
      panNumber: ['', [Validators.required, FormValidators.panValidator()]],
      aadharNumber: ['', [Validators.required, FormValidators.aadharValidator()]],
      
      // Co-applicant Details
      hasCoApplicant: [false],
      coApplicantName: ['', [FormValidators.fullNameValidator()]],
      coApplicantRelation: ['', [FormValidators.nameValidator()]],
      coApplicantPan: ['', [FormValidators.panValidator()]],
      coApplicantAadhar: ['', [FormValidators.aadharValidator()]]
    });

    // Step 3: Financial Details Form
    this.financialDetailsForm = this.fb.group({
      employmentType: ['SALARIED', Validators.required],
      employerName: ['', [FormValidators.nameValidator(), Validators.maxLength(100)]],
      designation: ['', [FormValidators.nameValidator(), Validators.maxLength(50)]],
      workExperience: [0, [Validators.min(0), Validators.max(50)]],
      monthlyGrossSalary: [0, [FormValidators.positiveNumberValidator(), Validators.max(10000000)]],
      monthlyNetSalary: [0, [FormValidators.positiveNumberValidator(), Validators.max(10000000)]],
      businessName: ['', [FormValidators.nameValidator(), Validators.maxLength(100)]],
      businessType: ['', [FormValidators.nameValidator(), Validators.maxLength(50)]],
      annualTurnover: [0, [FormValidators.positiveNumberValidator(), Validators.max(1000000000)]],
      otherIncomeSources: ['', [FormValidators.addressValidator(), Validators.maxLength(200)]],
      otherIncomeAmount: [0, [FormValidators.positiveNumberValidator(), Validators.max(10000000)]],
      existingLoanEMI: [0, [Validators.min(0), Validators.max(1000000)]],
      creditCardPayment: [0, [Validators.min(0), Validators.max(1000000)]],
      otherObligations: [0, [Validators.min(0), Validators.max(1000000)]],
      bankName: ['', [Validators.required, FormValidators.nameValidator(), Validators.maxLength(100)]],
      accountNumber: ['', [Validators.required, Validators.minLength(9), Validators.maxLength(18), Validators.pattern(/^\d+$/)]],
      ifscCode: ['', [Validators.required, FormValidators.ifscValidator()]],
      accountType: ['SAVINGS', Validators.required],
      bankStatementConsent: [false, Validators.requiredTrue]
    });

    // Step 4: Declarations Form
    this.declarationsForm = this.fb.group({
      kycConsent: [false, this.requiredTrueValidator],
      creditBureauConsent: [false, this.requiredTrueValidator],
      bankStatementConsent: [false, this.requiredTrueValidator],
      termsAccepted: [false, this.requiredTrueValidator],
      privacyPolicyAccepted: [false, this.requiredTrueValidator],
      eSignConsent: [false]
    });

    // Watch for changes in declaration checkboxes to update select all state
    const declarationFields = ['kycConsent', 'creditBureauConsent', 'bankStatementConsent', 'termsAccepted', 'privacyPolicyAccepted'];
    declarationFields.forEach(field => {
      const subscription = this.declarationsForm.get(field)?.valueChanges.subscribe(() => {
        this.updateSelectAllState();
      });
      if (subscription) {
        this.subscriptions.push(subscription);
      }
    });

    // Watch for loan type changes - reinitialize application if needed
    this.basicDetailsForm.get('loanType')?.valueChanges.subscribe(loanType => {
      if (loanType) {
        console.log('Loan type changed to:', loanType);
        const currentApp = this.loanApplicationService.getCurrentApplication();
        if (!currentApp) {
          console.log('No application exists, initializing with loan type:', loanType);
          this.loanApplicationService.initializeApplication(this.applicantId, loanType);
        } else if (currentApp.basicDetails.loanType !== loanType) {
          console.log('Loan type changed, updating application');
          currentApp.basicDetails.loanType = loanType;
          this.loanApplicationService.updateBasicDetails(currentApp.basicDetails);
        }
        this.requiredDocuments = this.loanApplicationService.getRequiredDocuments(loanType);
        // Update loan amount validators based on selected loan type
        this.updateLoanAmountValidators(loanType);
      }
    });

    // Watch for employment type changes
    this.financialDetailsForm.get('employmentType')?.valueChanges.subscribe(type => {
      this.updateFinancialValidators(type);
    });

    // Watch for co-applicant changes
    this.basicDetailsForm.get('hasCoApplicant')?.valueChanges.subscribe(has => {
      this.updateCoApplicantValidators(has);
    });

    // Watch for collateral changes
    this.basicDetailsForm.get('hasCollateral')?.valueChanges.subscribe(has => {
      this.updateCollateralValidators(has);
    });

    // Watch for loan amount/tenure changes for EMI calculation
    this.basicDetailsForm.get('loanAmount')?.valueChanges.subscribe(() => this.calculateEMI());
    this.basicDetailsForm.get('tenure')?.valueChanges.subscribe(() => this.calculateEMI());
    
    // Watch for financial details changes to recalculate EMI/DTI
    this.financialDetailsForm.get('monthlyGrossSalary')?.valueChanges.subscribe(() => this.calculateEMI());
    this.financialDetailsForm.get('monthlyNetSalary')?.valueChanges.subscribe(() => this.calculateEMI());
    this.financialDetailsForm.get('existingLoanEMI')?.valueChanges.subscribe(() => this.calculateEMI());
    this.financialDetailsForm.get('creditCardPayment')?.valueChanges.subscribe(() => this.calculateEMI());
    this.financialDetailsForm.get('otherObligations')?.valueChanges.subscribe(() => this.calculateEMI());
  }

  subscribeToApplicationState(): void {
    const sub = this.loanApplicationService.applicationState$.subscribe(app => {
      if (app) {
        this.application = app;
        this.currentStep = app.currentStep;
        this.populateFormsFromApplication(app);
      }
    });
    this.subscriptions.push(sub);
  }

  loadProfile(): void {
    this.loading = true;
    this.applicantService.getApplicantProfile(this.applicantId).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.autofillFromProfile(profile);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading profile:', error);
        this.toastService.showError(
          '👤 Profile Loading Failed',
          'Failed to load your profile data. Please refresh the page and try again.',
          10000
        );
        this.loading = false;
      }
    });
  }

  loadLoanType(): void {
    // Check if loan type was selected from loan-types page
    const savedLoanType = localStorage.getItem('selectedLoanType');
    if (savedLoanType) {
      try {
        const loanType = JSON.parse(savedLoanType);
        this.selectedLoanType = loanType.id;
        this.basicDetailsForm.patchValue({ loanType: loanType.id });
        
        // Initialize application
        console.log('Initializing application with loan type:', loanType.id);
        this.loanApplicationService.initializeApplication(this.applicantId, loanType.id);
        
        // Load required documents
        this.requiredDocuments = this.loanApplicationService.getRequiredDocuments(loanType.id);
        
        localStorage.removeItem('selectedLoanType');
      } catch (error) {
        console.error('Error parsing loan type:', error);
      }
    } else {
      // If no saved loan type, check if there's an existing application or initialize with default
      const existingApp = this.loanApplicationService.getCurrentApplication();
      if (!existingApp) {
        console.log('No saved loan type and no existing application. Initializing with PERSONAL loan type.');
        // Initialize with a default loan type (PERSONAL) so the form can work
        this.loanApplicationService.initializeApplication(this.applicantId, 'PERSONAL');
        this.basicDetailsForm.patchValue({ loanType: 'PERSONAL' });
        this.requiredDocuments = this.loanApplicationService.getRequiredDocuments('PERSONAL');
      } else {
        console.log('Found existing application:', existingApp);
        this.selectedLoanType = existingApp.basicDetails.loanType;
        this.requiredDocuments = this.loanApplicationService.getRequiredDocuments(existingApp.basicDetails.loanType);
      }
    }
  }

  autofillFromProfile(profile: ApplicantProfile): void {
    // Autofill financial details from profile
    this.financialDetailsForm.patchValue({
      // Will be filled from existing applications if available
    });
  }

  populateFormsFromApplication(app: LoanApplicationForm): void {
    this.basicDetailsForm.patchValue(app.basicDetails);
    this.financialDetailsForm.patchValue(app.financialDetails);
    this.declarationsForm.patchValue(app.declarations);
    this.uploadedDocuments = [...app.documents];
  }

  // ==================== Form Validators ====================

  updateLoanAmountValidators(loanType: string): void {
    const loanAmountControl = this.basicDetailsForm.get('loanAmount');
    const tenureControl = this.basicDetailsForm.get('tenure');
    
    // Get loan type configuration from ApplicantService
    const loanTypes = this.applicantService.getLoanTypes();
    const selectedLoanType = loanTypes.find(type => type.id === loanType);
    
    if (selectedLoanType && loanAmountControl && tenureControl) {
      // Update loan amount validators with min/max from loan type
      loanAmountControl.setValidators([
        Validators.required,
        Validators.min(selectedLoanType.minAmount),
        Validators.max(selectedLoanType.maxAmount),
        FormValidators.positiveNumberValidator()
      ]);
      
      // Update tenure validators with min/max from loan type
      tenureControl.setValidators([
        Validators.required,
        Validators.min(selectedLoanType.minTenure),
        Validators.max(selectedLoanType.maxTenure)
      ]);
      
      // Update validity
      loanAmountControl.updateValueAndValidity();
      tenureControl.updateValueAndValidity();
      
      console.log(`Updated validators for ${loanType}: Amount ${selectedLoanType.minAmount}-${selectedLoanType.maxAmount}, Tenure ${selectedLoanType.minTenure}-${selectedLoanType.maxTenure} months`);
    }
  }

  getSelectedLoanType() {
    const loanType = this.basicDetailsForm.get('loanType')?.value;
    if (loanType) {
      const loanTypes = this.applicantService.getLoanTypes();
      return loanTypes.find(type => type.id === loanType);
    }
    return null;
  }

  updateFinancialValidators(employmentType: string): void {
    const employerName = this.financialDetailsForm.get('employerName');
    const designation = this.financialDetailsForm.get('designation');
    const monthlyGrossSalary = this.financialDetailsForm.get('monthlyGrossSalary');
    const businessName = this.financialDetailsForm.get('businessName');
    const annualTurnover = this.financialDetailsForm.get('annualTurnover');

    if (employmentType === 'SALARIED') {
      employerName?.setValidators([Validators.required]);
      designation?.setValidators([Validators.required]);
      monthlyGrossSalary?.setValidators([Validators.required, Validators.min(10000)]);
      businessName?.clearValidators();
      annualTurnover?.clearValidators();
    } else if (employmentType === 'SELF_EMPLOYED' || employmentType === 'BUSINESS') {
      businessName?.setValidators([Validators.required]);
      annualTurnover?.setValidators([Validators.required, Validators.min(100000)]);
      employerName?.clearValidators();
      designation?.clearValidators();
    }

    employerName?.updateValueAndValidity();
    designation?.updateValueAndValidity();
    monthlyGrossSalary?.updateValueAndValidity();
    businessName?.updateValueAndValidity();
    annualTurnover?.updateValueAndValidity();
  }

  updateCoApplicantValidators(hasCoApplicant: boolean): void {
    const coApplicantName = this.basicDetailsForm.get('coApplicantName');
    const coApplicantRelation = this.basicDetailsForm.get('coApplicantRelation');

    if (hasCoApplicant) {
      coApplicantName?.setValidators([Validators.required]);
      coApplicantRelation?.setValidators([Validators.required]);
    } else {
      coApplicantName?.clearValidators();
      coApplicantRelation?.clearValidators();
    }

    coApplicantName?.updateValueAndValidity();
    coApplicantRelation?.updateValueAndValidity();
  }

  updateCollateralValidators(hasCollateral: boolean): void {
    const collateralType = this.basicDetailsForm.get('collateralType');
    const collateralValue = this.basicDetailsForm.get('collateralValue');

    if (hasCollateral) {
      collateralType?.setValidators([Validators.required]);
      collateralValue?.setValidators([Validators.required, Validators.min(1000)]);
    } else {
      collateralType?.clearValidators();
      collateralValue?.clearValidators();
    }

    collateralType?.updateValueAndValidity();
    collateralValue?.updateValueAndValidity();
  }

  // ==================== EMI Calculation ====================

  calculateEMI(): void {
    const loanAmount = this.basicDetailsForm.get('loanAmount')?.value || 0;
    const tenure = this.basicDetailsForm.get('tenure')?.value || 12;
    const monthlyIncome = this.financialDetailsForm.get('monthlyGrossSalary')?.value || 0;
    const existingEMI = this.financialDetailsForm.get('existingLoanEMI')?.value || 0;
    const creditCard = this.financialDetailsForm.get('creditCardPayment')?.value || 0;
    const otherObligations = this.financialDetailsForm.get('otherObligations')?.value || 0;
    
    const totalObligations = existingEMI + creditCard + otherObligations;
    
    // Get interest rate based on loan type (mock - should come from backend)
    const interestRate = this.getInterestRateForLoanType(this.selectedLoanType);
    
    if (loanAmount > 0 && tenure > 0 && monthlyIncome > 0) {
      this.emiCalculation = this.loanApplicationService.calculateEMI(
        loanAmount,
        interestRate,
        tenure,
        monthlyIncome,
        totalObligations
      );
    }
  }

  getInterestRateForLoanType(loanType: string): number {
    const rates: { [key: string]: number } = {
      'PERSONAL': 14.0,
      'HOME': 9.5,
      'VEHICLE': 11.5,
      'EDUCATION': 11.0,
      'BUSINESS': 13.5
    };
    return rates[loanType] || 12.0;
  }

  toggleEMIDetails(): void {
    this.showEMIDetails = !this.showEMIDetails;
  }

  getDTIClass(): string {
    if (!this.emiCalculation) return '';
    const dti = this.emiCalculation.dti;
    if (dti <= 30) return 'text-success';
    if (dti <= 40) return 'text-info';
    if (dti <= 50) return 'text-warning';
    return 'text-danger';
  }

  // ==================== Navigation ====================

  nextStep(): void {
    if (this.currentStep < this.totalSteps) {
      // Sync uploaded documents before validation
      if (this.currentStep === 4) {
        const app = this.loanApplicationService.getCurrentApplication();
        if (app) {
          this.uploadedDocuments = [...app.documents];
        }
      }
      
      if (this.validateCurrentStep()) {
        this.saveCurrentStep();
        this.currentStep++;
        this.loanApplicationService.setCurrentStep(this.currentStep);
        
        // Load required documents when entering step 4
        if (this.currentStep === 4) {
          this.loadRequiredDocuments();
        }
        
        // Load application data when entering step 6 (Review & Submit)
        if (this.currentStep === 6) {
          // Force save current step before loading
          this.saveCurrentStep();
          
          // Get application from service
          this.application = this.loanApplicationService.getCurrentApplication();
          console.log('=== Review Step Debug ===');
          console.log('Application loaded:', this.application);
          console.log('Basic Details:', this.application?.basicDetails);
          console.log('Applicant Details:', this.application?.applicantDetails);
          console.log('Financial Details:', this.application?.financialDetails);
          console.log('Documents:', this.application?.documents);
          console.log('Declarations:', this.application?.declarations);
          console.log('========================');
          
          if (!this.application) {
            this.toastService.showError(
              '📋 Application Data Missing',
              'Failed to load application data. Please go back and check all steps.',
              10000
            );
          } else if (!this.application.basicDetails || !this.application.financialDetails) {
            this.toastService.showWarning(
              '⚠️ Incomplete Application',
              'Some required data is missing. Please review all previous steps and ensure all forms are completed.',
              12000
            );
            console.error('Missing required data in application');
          }
        }
        
        window.scrollTo(0, 0);
      }
    }
  }

  loadRequiredDocuments(): void {
    const loanType = this.basicDetailsForm.get('loanType')?.value;
    if (loanType) {
      this.requiredDocuments = this.loanApplicationService.getRequiredDocuments(loanType);
      console.log('Loaded required documents for loan type:', loanType, this.requiredDocuments);
      
      // Sync uploaded documents when entering step 4
      const app = this.loanApplicationService.getCurrentApplication();
      if (app) {
        this.uploadedDocuments = [...app.documents];
        console.log('Synced uploaded documents on step 4 entry:', this.uploadedDocuments);
      } else {
        this.toastService.showWarning(
          '📋 Loan Type Required',
          'Please select a loan type first before proceeding to document upload.',
          8000
        );
      }
    } else {
      this.toastService.showWarning(
        '📋 Loan Type Required',
        'Please select a loan type first before proceeding to document upload.',
        8000
      );
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      this.loanApplicationService.setCurrentStep(this.currentStep);
      window.scrollTo(0, 0);
    }
  }

  goToStep(step: number): void {
    if (step <= this.currentStep || this.loanApplicationService.validateStep(step - 1)) {
      this.currentStep = step;
      this.loanApplicationService.setCurrentStep(step);
      
      // Load required documents when entering step 3
      if (step === 3) {
        this.loadRequiredDocuments();
      }
      
      window.scrollTo(0, 0);
    }
  }

  validateCurrentStep(): boolean {
    switch (this.currentStep) {
      case 1:
        // Step 1: Basic Loan Details
        if (!this.basicDetailsForm.valid) {
          const missingFields = this.getMissingFields(this.basicDetailsForm);
          this.showValidationToast('Basic Loan Details', missingFields);
          this.markFormGroupTouched(this.basicDetailsForm);
          this.scrollToFirstError();
          return false;
        }
        return true;
      
      case 2:
        // Step 2: Applicant Information
        if (!this.applicantDetailsForm.valid) {
          const missingFields = this.getMissingFields(this.applicantDetailsForm);
          this.showValidationToast('Applicant Information', missingFields);
          this.markFormGroupTouched(this.applicantDetailsForm);
          this.scrollToFirstError();
          return false;
        }
        return true;
      
      case 3:
        // Step 3: Financial Details
        if (!this.financialDetailsForm.valid) {
          const missingFields = this.getMissingFields(this.financialDetailsForm);
          this.showValidationToast('Financial Details', missingFields);
          this.markFormGroupTouched(this.financialDetailsForm);
          this.scrollToFirstError();
          return false;
        }
        if (!this.emiCalculation || this.emiCalculation.dti > 60) {
          this.toastService.showWarning(
            '⚠️ High Debt-to-Income Ratio',
            'Your debt-to-income ratio is too high. Please adjust your loan amount or tenure to improve eligibility.',
            10000
          );
          return false;
        }
        return true;
      
      case 4:
        // Step 4: Upload Documents
        const requiredTypes = this.requiredDocuments.filter(d => d.required).map(d => d.documentType);
        const uploadedTypes = this.uploadedDocuments.map(d => d.documentType);
        const missingDocs = requiredTypes.filter(type => !uploadedTypes.includes(type));
        
        // Debug logging
        console.log('Required document types:', requiredTypes);
        console.log('Uploaded document types:', uploadedTypes);
        console.log('Missing documents:', missingDocs);
        console.log('Uploaded documents array:', this.uploadedDocuments);
        
        if (missingDocs.length > 0) {
          this.showDocumentValidationToast(missingDocs);
          return false;
        }
        return true;
      
      case 5:
        // Step 5: Declarations
        if (!this.declarationsForm.valid) {
          this.showDeclarationValidationToast();
          return false;
        }
        return true;
      
      default:
        return true;
    }
  }

  getMissingFields(formGroup: any): string[] {
    const missingFields: string[] = [];
    const fieldLabels: { [key: string]: string } = {
      // Basic Details
      loanType: 'Loan Type',
      loanAmount: 'Loan Amount',
      tenure: 'Tenure',
      
      // Applicant Information
      firstName: 'First Name',
      lastName: 'Last Name',
      dateOfBirth: 'Date of Birth',
      gender: 'Gender',
      maritalStatus: 'Marital Status',
      emailAddress: 'Email Address',
      mobileNumber: 'Mobile Number',
      currentAddress: 'Current Address',
      currentCity: 'Current City',
      currentState: 'Current State',
      currentPincode: 'Current Pincode',
      residenceType: 'Residence Type',
      yearsAtCurrentAddress: 'Years at Current Address',
      panNumber: 'PAN Number',
      aadharNumber: 'Aadhar Number',
      permanentAddress: 'Permanent Address',
      permanentCity: 'Permanent City',
      permanentState: 'Permanent State',
      permanentPincode: 'Permanent Pincode',
      
      // Financial Details
      employmentType: 'Employment Type',
      employerName: 'Employer Name',
      designation: 'Designation',
      monthlyGrossSalary: 'Monthly Gross Salary',
      businessName: 'Business Name',
      annualTurnover: 'Annual Turnover',
      bankName: 'Bank Name',
      accountNumber: 'Account Number',
      ifscCode: 'IFSC Code',
      accountType: 'Account Type',
      bankStatementConsent: 'Bank Statement Consent'
    };

    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      if (control && control.invalid && control.errors) {
        const fieldName = fieldLabels[key] || key;
        missingFields.push(fieldName);
      }
    });

    return missingFields;
  }

  saveCurrentStep(): void {
    switch (this.currentStep) {
      case 1:
        // Save Basic Loan Details
        this.loanApplicationService.updateBasicDetails(this.basicDetailsForm.value);
        this.calculateEMI();
        break;
      
      case 2:
        // Save Applicant Information
        this.loanApplicationService.updateApplicantDetails(this.applicantDetailsForm.value);
        break;
      
      case 3:
        // Save Financial Details
        this.loanApplicationService.updateFinancialDetails(this.financialDetailsForm.value);
        break;
      
      case 5:
        // Save Declarations
        const declarations: Declaration = {
          ...this.declarationsForm.value,
          declarationDate: new Date().toISOString()
        };
        this.loanApplicationService.updateDeclarations(declarations);
        break;
    }
  }

  // ==================== Document Upload ====================

  onFileSelected(event: any, documentType: string): void {
    const files: FileList = event.target.files;
    if (files && files.length > 0) {
      this.uploadFiles(Array.from(files), documentType);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;
  }

  onDrop(event: DragEvent, documentType: string): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.uploadFiles(Array.from(files), documentType);
    }
  }

  uploadFiles(files: File[], documentType: string): void {
    files.forEach(file => {
      // Validate file
      const docReq = this.requiredDocuments.find(d => d.documentType === documentType);
      if (!docReq) return;

      // Enhanced file type validation - check both MIME type and file extension
      const isValidMimeType = this.isValidFileType(file.type, docReq.acceptedFormats);
      const isValidExtension = this.isValidFileExtension(file.name);
      
      if (!isValidMimeType && !isValidExtension) {
        this.toastService.showError(
          'Invalid File Format', 
          `Please upload PDF, JPG, JPEG, or PNG files only for ${docReq.displayName}. File: ${file.name}`,
          8000
        );
        console.error('File validation failed:', {
          fileName: file.name,
          mimeType: file.type,
          acceptedFormats: docReq.acceptedFormats
        });
        return;
      }

      if (file.size > docReq.maxSize) {
        this.toastService.showError(
          'File Too Large', 
          `File size exceeds maximum limit of ${(docReq.maxSize / (1024 * 1024)).toFixed(0)}MB for ${docReq.displayName}`,
          8000
        );
        return;
      }

      // Upload file to Cloudinary via backend
      this.loanApplicationService.uploadDocument(file, documentType, this.applicantId).subscribe({
        next: (response) => {
          console.log('Upload response received:', response);
          
          // Update progress
          this.uploadProgress[file.name] = {
            fileName: file.name,
            progress: response.progress || 0,
            status: response.status || 'uploading'
          };
          
          // Check if upload is complete (status === 'success' from HTTP event)
          if (response.status === 'success') {
            console.log('Upload complete! Response data:', response.data);
            
            const uploadData = response.data;
            
            // Create document object - handle both possible response structures
            const document: DocumentUpload = {
              documentType,
              fileName: uploadData?.document?.originalFilename || uploadData?.originalFilename || file.name,
              fileUrl: uploadData?.document?.cloudinaryUrl || uploadData?.cloudinaryUrl || '',
              fileSize: file.size,
              uploadedAt: uploadData?.document?.uploadedAt || uploadData?.uploadedAt || new Date().toISOString(),
              status: 'PENDING'
            };
            
            console.log('Adding document to service:', document);
            console.log('Current uploadedDocuments before add:', this.uploadedDocuments);
            
            // Add document via service (which updates both service state and local array)
            this.loanApplicationService.addDocument(document);
            
            // Sync local array with service state - create new array reference for change detection
            const app = this.loanApplicationService.getCurrentApplication();
            if (app) {
              this.uploadedDocuments = [...app.documents];
              console.log('Updated uploadedDocuments array after sync:', this.uploadedDocuments);
            } else {
              console.error('No current application found!');
            }
            
            // Also add directly to local array as backup
            if (!this.uploadedDocuments.some(d => d.documentType === documentType)) {
              this.uploadedDocuments.push(document);
              console.log('Added directly to local array:', this.uploadedDocuments);
            }
            
            // Show document upload success toast
            this.toastService.showSuccess(
              '📄 Document Uploaded', 
              `Your ${docReq.displayName} has been uploaded successfully. File: ${file.name}`,
              6000
            );
            
            setTimeout(() => {
              delete this.uploadProgress[file.name];
            }, 3000);
          }
        },
        error: (error) => {
          console.error('Upload error:', error);
          this.uploadProgress[file.name] = {
            fileName: file.name,
            progress: 0,
            status: 'error',
            error: error.error?.message || 'Upload failed'
          };
          // Show document upload error toast
          this.toastService.showError(
            'Upload Failed', 
            error.error?.message || `Failed to upload ${file.name}. Please try again.`,
            8000
          );
          
          setTimeout(() => {
            delete this.uploadProgress[file.name];
          }, 3000);
        }
      });
    });
  }

  removeDocument(fileName: string): void {
    // Remove via service
    this.loanApplicationService.removeDocument(fileName);
    
    // Sync local array with service state - create new array reference for change detection
    const app = this.loanApplicationService.getCurrentApplication();
    if (app) {
      this.uploadedDocuments = [...app.documents];
    }
  }

  getDocumentIcon(documentType: string): string {
    const doc = this.requiredDocuments.find(d => d.documentType === documentType);
    return doc?.icon || 'fa-file';
  }

  // ==================== Submit Application ====================

  submitApplication(): void {
    if (!this.application) {
      this.toastService.showError('❌ Submission Error', 'No application data found. Please complete all steps and try again.');
      return;
    }

    this.submitting = true;

    this.loanApplicationService.submitApplication(this.application).subscribe({
      next: (response) => {
        console.log('Loan submission response:', response);
        
        // Show loan submission success toast
        const loanId = response.loanId || response.applicationId || 'N/A';
        this.toastService.showSuccess(
          '🎉 Application Submitted!', 
          `Your loan application #${loanId} has been submitted successfully. You will receive updates via email.`,
          8000
        );
        
        // Check if loan was assigned to an officer and show additional toast
        if (response.assignmentId && response.assignedOfficerName) {
          this.toastService.showSuccess(
            '👨‍💼 Loan Officer Assigned!', 
            `Your application has been assigned to ${response.assignedOfficerName} for review.`,
            8000
          );
        } else if (response.assignmentError) {
          this.toastService.showWarning(
            'Assignment Pending', 
            'Your application was submitted successfully but assignment to a loan officer is pending.',
            10000
          );
        }
        
        this.submitting = false;
        
        // Clear all form data and drafts
        this.clearApplicationData();
        
        // Redirect to dashboard after 4 seconds to allow users to see the toasts
        setTimeout(() => {
          this.router.navigate(['/applicant/dashboard']);
        }, 4000);
      },
      error: (error) => {
        console.error('Submission error:', error);
        this.toastService.showHttpError(error, 'Submission Failed');
        this.submitting = false;
      }
    });
  }

  // ==================== Validation Toast Methods ====================

  /**
   * Show validation toast for missing form fields
   */
  private showValidationToast(stepName: string, missingFields: string[]): void {
    if (missingFields.length === 0) return;
    
    const title = `📝 ${stepName} - Required Fields Missing`;
    let message: string;
    
    if (missingFields.length === 1) {
      message = `Please fill the required field: ${missingFields[0]}`;
    } else if (missingFields.length <= 3) {
      message = `Please fill the following required fields:\n• ${missingFields.join('\n• ')}`;
    } else {
      message = `Please fill ${missingFields.length} required fields:\n• ${missingFields.slice(0, 3).join('\n• ')}\n• ... and ${missingFields.length - 3} more`;
    }
    
    this.toastService.showWarning(title, message, 10000);
  }

  /**
   * Show validation toast for missing documents
   */
  private showDocumentValidationToast(missingDocs: string[]): void {
    const title = '📄 Document Upload Required';
    let message: string;
    
    if (missingDocs.length === 1) {
      message = `Please upload the required document: ${missingDocs[0]}`;
    } else if (missingDocs.length <= 3) {
      message = `Please upload the following required documents:\n• ${missingDocs.join('\n• ')}`;
    } else {
      message = `Please upload ${missingDocs.length} required documents:\n• ${missingDocs.slice(0, 3).join('\n• ')}\n• ... and ${missingDocs.length - 3} more`;
    }
    
    this.toastService.showWarning(title, message, 12000);
  }

  /**
   * Show validation toast for missing declarations
   */
  private showDeclarationValidationToast(): void {
    const invalidFields: string[] = [];
    const declarationFields = [
      { key: 'kycConsent', label: 'KYC Consent' },
      { key: 'creditBureauConsent', label: 'Credit Bureau Consent' },
      { key: 'bankStatementConsent', label: 'Bank Statement Consent' },
      { key: 'termsAccepted', label: 'Terms & Conditions' },
      { key: 'privacyPolicyAccepted', label: 'Privacy Policy' }
    ];
    
    declarationFields.forEach(field => {
      const control = this.declarationsForm.get(field.key);
      if (control && control.invalid) {
        invalidFields.push(field.label);
      }
    });
    
    const title = '✅ Declarations & Consents Required';
    let message: string;
    
    if (invalidFields.length === 0) {
      message = 'Please accept all required declarations and consents to proceed.';
    } else if (invalidFields.length === 1) {
      message = `Please accept the ${invalidFields[0]} to proceed.`;
    } else {
      message = `Please accept the following declarations:\n• ${invalidFields.join('\n• ')}`;
    }
    
    this.toastService.showWarning(title, message, 10000);
  }

  /**
   * Show field-specific validation toast for individual field errors
   */
  showFieldValidationToast(fieldName: string, errorType: string, fieldDisplayName?: string): void {
    const displayName = fieldDisplayName || this.getFieldDisplayName(fieldName);
    let title = '⚠️ Validation Error';
    let message = '';
    
    switch (errorType) {
      case 'required':
        title = '📝 Required Field';
        message = `${displayName} is required. Please fill this field to continue.`;
        break;
      case 'min':
        title = '📊 Minimum Value';
        message = `${displayName} value is too low. Please enter a higher value.`;
        break;
      case 'max':
        title = '📊 Maximum Value';
        message = `${displayName} value is too high. Please enter a lower value.`;
        break;
      case 'minlength':
        title = '📏 Minimum Length';
        message = `${displayName} is too short. Please enter more characters.`;
        break;
      case 'maxlength':
        title = '📏 Maximum Length';
        message = `${displayName} is too long. Please enter fewer characters.`;
        break;
      case 'pattern':
      case 'invalidFormat':
        title = '🔤 Invalid Format';
        message = `${displayName} format is invalid. Please check the format and try again.`;
        break;
      case 'invalidEmail':
        title = '📧 Invalid Email';
        message = 'Please enter a valid email address (e.g., user@example.com).';
        break;
      case 'invalidMobile':
        title = '📱 Invalid Mobile Number';
        message = 'Please enter a valid 10-digit mobile number starting with 6, 7, 8, or 9.';
        break;
      case 'invalidPan':
        title = '🆔 Invalid PAN';
        message = 'Please enter a valid PAN number (e.g., ABCDE1234F).';
        break;
      case 'invalidAadhar':
        title = '🆔 Invalid Aadhar';
        message = 'Please enter a valid 12-digit Aadhar number.';
        break;
      case 'futureDate':
        title = '📅 Invalid Date';
        message = 'Date cannot be in the future. Please select a valid date.';
        break;
      case 'underAge':
        title = '🎂 Age Requirement';
        message = 'You must be at least 18 years old to apply for a loan.';
        break;
      default:
        message = `Please check the ${displayName} field and correct any errors.`;
    }
    
    this.toastService.showWarning(title, message, 8000);
  }

  // ==================== Utility Methods ====================

  onPermanentAddressChange(isSame: boolean): void {
    const permanentFields = ['permanentAddress', 'permanentCity', 'permanentState', 'permanentPincode'];
    
    if (isSame) {
      // Copy current address to permanent address
      this.applicantDetailsForm.patchValue({
        permanentAddress: this.applicantDetailsForm.get('currentAddress')?.value,
        permanentCity: this.applicantDetailsForm.get('currentCity')?.value,
        permanentState: this.applicantDetailsForm.get('currentState')?.value,
        permanentPincode: this.applicantDetailsForm.get('currentPincode')?.value
      });
      
      // Remove validators for permanent address fields
      permanentFields.forEach(field => {
        this.applicantDetailsForm.get(field)?.clearValidators();
        this.applicantDetailsForm.get(field)?.updateValueAndValidity();
      });
    } else {
      // Add validators for permanent address fields
      permanentFields.forEach(field => {
        this.applicantDetailsForm.get(field)?.setValidators([Validators.required]);
        this.applicantDetailsForm.get(field)?.updateValueAndValidity();
      });
    }
  }

  onCoApplicantChange(hasCoApplicant: boolean): void {
    const coApplicantFields = ['coApplicantName', 'coApplicantRelation', 'coApplicantPan', 'coApplicantAadhar'];
    
    if (hasCoApplicant) {
      coApplicantFields.forEach(field => {
        this.applicantDetailsForm.get(field)?.setValidators([Validators.required]);
        this.applicantDetailsForm.get(field)?.updateValueAndValidity();
      });
    } else {
      coApplicantFields.forEach(field => {
        this.applicantDetailsForm.get(field)?.clearValidators();
        this.applicantDetailsForm.get(field)?.updateValueAndValidity();
      });
    }
  }

  formatCurrency(amount: number): string {
    return this.loanApplicationService.formatCurrency(amount);
  }

  getStepIcon(step: number): string {
    const icons = ['', 'fa-info-circle', 'fa-user', 'fa-money-bill-wave', 'fa-file-upload', 'fa-check-square', 'fa-eye'];
    return icons[step] || 'fa-circle';
  }

  getStepTitle(step: number): string {
    const titles = ['', 'Basic Details', 'Applicant Information', 'Financial Details', 'Upload Documents', 'Declarations', 'Review & Submit'];
    return titles[step] || '';
  }

  isStepCompleted(step: number): boolean {
    return this.loanApplicationService.validateStep(step);
  }

  cancelApplication(): void {
    if (confirm('Are you sure you want to cancel? All progress will be lost.')) {
      this.clearSavedData();
      this.toastService.showInfo(
        '🚪 Application Cancelled',
        'Your loan application has been cancelled. You can start a new application anytime.',
        6000
      );
      this.router.navigate(['/applicant/dashboard']);
    }
  }

  clearSuccess(): void {
    this.success = '';
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  scrollToFirstError(): void {
    setTimeout(() => {
      const firstInvalidControl = document.querySelector('.is-invalid, .ng-invalid.ng-touched');
      if (firstInvalidControl) {
        firstInvalidControl.scrollIntoView({ behavior: 'smooth', block: 'center' });
        (firstInvalidControl as HTMLElement).focus();
      }
    }, 100);
  }

  // Helper method to check if document is uploaded
  isDocumentUploaded(documentType: string): boolean {
    return this.uploadedDocuments.some(d => d.documentType === documentType);
  }

  // Enhanced file type validation - checks MIME type flexibly
  private isValidFileType(mimeType: string, acceptedFormats: string[]): boolean {
    if (!mimeType) return false;
    
    // Normalize MIME type (lowercase, remove parameters)
    const normalizedType = mimeType.toLowerCase().split(';')[0].trim();
    
    // Check if it matches accepted formats
    const isAccepted = acceptedFormats.some(format => {
      const normalizedFormat = format.toLowerCase().trim();
      return normalizedType === normalizedFormat || 
             normalizedType.startsWith(normalizedFormat) ||
             normalizedType.includes(normalizedFormat.split('/')[1]);
    });
    
    // Additional checks for common image variations
    const isImageVariant = normalizedType.includes('jpeg') || 
                          normalizedType.includes('jpg') || 
                          normalizedType.includes('png') ||
                          normalizedType === 'image/pjpeg' ||
                          normalizedType === 'image/x-png';
    
    return isAccepted || isImageVariant;
  }

  // File extension validation as fallback
  private isValidFileExtension(filename: string): boolean {
    if (!filename) return false;
    
    const lowerFilename = filename.toLowerCase();
    const validExtensions = ['.pdf', '.jpg', '.jpeg', '.png', '.jpe'];
    
    return validExtensions.some(ext => lowerFilename.endsWith(ext));
  }

  // ==================== Draft Functionality ====================

  initializeDraftFunctionality(): void {
    // Check for draft to resume from query params
    this.checkForDraftToResume();
    
    // Set up auto-save every 30 seconds
    this.setupAutoSave();

    // Subscribe to form changes for auto-save
    this.subscribeToFormChanges();
  }

  checkForDraftToResume(): void {
    const draftId = this.route.snapshot.queryParams['draftId'];
    if (draftId) {
      this.loadDraft(draftId);
    } else {
      // Check if user has existing drafts and prompt
      const existingDrafts = this.draftService.getDraftsByApplicant(this.applicantId);
      if (existingDrafts.length > 0) {
        const latestDraft = existingDrafts[0];
        const resume = confirm(`You have an incomplete application (${latestDraft.completionPercentage}% complete). Would you like to resume it?`);
        if (resume) {
          this.loadDraft(latestDraft.id);
        }
      }
    }
  }

  setupAutoSave(): void {
    this.autoSaveInterval = setInterval(() => {
      if (this.hasFormData() && !this.isDraftSaving && !this.submitting) {
        this.saveDraft();
      }
    }, 30000); // Auto-save every 30 seconds
  }

  clearAutoSave(): void {
    if (this.autoSaveInterval) {
      clearInterval(this.autoSaveInterval);
      this.autoSaveInterval = null;
    }
  }

  subscribeToFormChanges(): void {
    // Subscribe to all form changes
    const forms = [
      this.basicDetailsForm,
      this.applicantDetailsForm,
      this.financialDetailsForm,
      this.declarationsForm
    ];

    forms.forEach(form => {
      if (form) {
        const subscription = form.valueChanges.subscribe(() => {
          // Reset draft saved status when form changes
          this.draftSaved = false;
        });
        this.subscriptions.push(subscription);
      }
    });
  }

  saveDraft(): void {
    if (this.isDraftSaving || !this.hasFormData()) return;
    
    this.isDraftSaving = true;
    
    try {
      const formData = this.collectFormData();
      const completionPercentage = this.calculateCompletionPercentage();
      
      const draftData = {
        id: this.currentDraftId || undefined,
        applicantId: this.applicantId,
        currentStep: this.currentStep,
        totalSteps: this.totalSteps,
        completionPercentage: completionPercentage,
        selectedLoanType: this.selectedLoanType,
        formData: formData
      };
      
      this.currentDraftId = this.draftService.saveDraft(draftData);
      
      this.draftSaved = true;
      this.success = 'Draft saved successfully!';
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        this.success = '';
      }, 3000);
      
    } catch (error) {
      console.error('Error saving draft:', error);
      this.toastService.showError(
        '💾 Draft Save Failed',
        'Failed to save draft. Please try again or continue without saving.',
        8000
      );
    } finally {
      this.isDraftSaving = false;
    }
  }

  saveAsDraft(): void {
    this.saveDraft();
    // Use success message instead of toast service
    this.success = 'Draft saved successfully! You can resume this application later from your dashboard.';
    setTimeout(() => {
      this.success = '';
    }, 5000);
  }

  loadDraft(draftId: string): void {
    try {
      const draft = this.draftService.getDraft(draftId);
      if (draft) {
        this.currentDraftId = draftId;
        this.restoreDraftData(draft);
        this.success = 'Draft loaded successfully!';
        setTimeout(() => {
          this.success = '';
        }, 3000);
      }
    } catch (error) {
      console.error('Error loading draft:', error);
      this.toastService.showError(
        '📂 Draft Loading Failed',
        'Failed to load saved draft. Starting with a fresh application.',
        8000
      );
    }
  }

  collectFormData(): any {
    return {
      loanTypeForm: { loanType: this.selectedLoanType },
      personalDetailsForm: this.applicantDetailsForm?.value || {},
      loanDetailsForm: this.basicDetailsForm?.value || {},
      financialDetailsForm: this.financialDetailsForm?.value || {},
      documentsForm: { uploadedDocuments: this.uploadedDocuments || [] }
    };
  }

  restoreDraftData(draft: DraftApplication): void {
    if (!draft || !draft.formData) return;

    try {
      // Restore current step
      this.currentStep = draft.currentStep;
      
      // Restore selected loan type
      if (draft.selectedLoanType) {
        this.selectedLoanType = draft.selectedLoanType;
      }

      // Restore form data
      const formData = draft.formData;
      
      if (formData.loanDetailsForm && this.basicDetailsForm) {
        this.basicDetailsForm.patchValue(formData.loanDetailsForm);
      }

      if (formData.personalDetailsForm && this.applicantDetailsForm) {
        this.applicantDetailsForm.patchValue(formData.personalDetailsForm);
      }

      if (formData.financialDetailsForm && this.financialDetailsForm) {
        this.financialDetailsForm.patchValue(formData.financialDetailsForm);
      }

      if (formData.documentsForm && formData.documentsForm.uploadedDocuments) {
        this.uploadedDocuments = formData.documentsForm.uploadedDocuments;
      }

    } catch (error) {
      console.error('Error restoring draft data:', error);
    }
  }

  hasFormData(): boolean {
    // Check if any form has meaningful data
    const basicData = this.basicDetailsForm?.value || {};
    const applicantData = this.applicantDetailsForm?.value || {};
    const financialData = this.financialDetailsForm?.value || {};

    return (
      basicData.loanAmount > 0 ||
      basicData.loanType ||
      applicantData.firstName ||
      applicantData.emailAddress ||
      financialData.monthlyIncome > 0 ||
      this.uploadedDocuments.length > 0
    );
  }

  clearSavedData(): void {
    if (this.currentDraftId) {
      this.draftService.deleteDraft(this.currentDraftId);
      this.currentDraftId = '';
    }
    this.draftSaved = false;
  }

  getDraftInfo(): string {
    if (this.currentDraftId) {
      const draft = this.draftService.getDraft(this.currentDraftId);
      if (draft) {
        return `Last saved: ${this.draftService.getTimeSinceLastSaved(draft.lastSaved)}`;
      }
    }
    return '';
  }

  calculateCompletionPercentage(): number {
    let filledFields = 0;
    let totalFields = 0;

    // Count basic details form fields
    if (this.basicDetailsForm) {
      const basicValues = this.basicDetailsForm.value;
      Object.keys(basicValues).forEach(key => {
        totalFields++;
        if (basicValues[key] && basicValues[key] !== '' && basicValues[key] !== 0) {
          filledFields++;
        }
      });
    }

    // Count applicant details form fields
    if (this.applicantDetailsForm) {
      const applicantValues = this.applicantDetailsForm.value;
      Object.keys(applicantValues).forEach(key => {
        totalFields++;
        if (applicantValues[key] && applicantValues[key] !== '' && applicantValues[key] !== 0) {
          filledFields++;
        }
      });
    }

    // Count financial details form fields
    if (this.financialDetailsForm) {
      const financialValues = this.financialDetailsForm.value;
      Object.keys(financialValues).forEach(key => {
        totalFields++;
        if (financialValues[key] && financialValues[key] !== '' && financialValues[key] !== 0) {
          filledFields++;
        }
      });
    }

    // Add step completion bonus
    const stepBonus = (this.currentStep - 1) * 10;
    const fieldPercentage = totalFields > 0 ? (filledFields / totalFields) * 70 : 0;
    
    return Math.min(100, Math.round(fieldPercentage + stepBonus));
  }

  clearApplicationData(): void {
    // Clear all form data
    this.basicDetailsForm.reset({
      loanType: '',
      loanAmount: 0,
      tenure: 12,
      hasCoApplicant: false,
      coApplicantName: '',
      coApplicantRelation: '',
      hasCollateral: false,
      collateralType: '',
      collateralValue: 0
    });

    this.applicantDetailsForm.reset({
      firstName: '',
      middleName: '',
      lastName: '',
      dateOfBirth: '',
      gender: '',
      maritalStatus: '',
      emailAddress: '',
      mobileNumber: '',
      alternateNumber: '',
      currentAddress: '',
      currentCity: '',
      currentState: '',
      currentPincode: '',
      residenceType: '',
      yearsAtCurrentAddress: '',
      permanentAddressSame: false,
      permanentAddress: '',
      permanentCity: '',
      permanentState: '',
      permanentPincode: '',
      panNumber: '',
      aadharNumber: '',
      hasCoApplicant: false,
      coApplicantName: '',
      coApplicantRelation: '',
      coApplicantPan: '',
      coApplicantAadhar: ''
    });

    this.financialDetailsForm.reset({
      employmentType: 'SALARIED',
      employerName: '',
      designation: '',
      workExperience: 0,
      monthlyGrossSalary: 0,
      monthlyNetSalary: 0,
      businessName: '',
      businessType: '',
      annualTurnover: 0,
      otherIncomeSources: '',
      otherIncomeAmount: 0,
      existingLoanEMI: 0,
      creditCardPayment: 0,
      otherObligations: 0,
      bankName: '',
      accountNumber: '',
      ifscCode: '',
      accountType: 'SAVINGS',
      bankStatementConsent: false
    });

    this.declarationsForm.reset({
      kycConsent: false,
      creditBureauConsent: false,
      bankStatementConsent: false,
      termsAccepted: false,
      privacyPolicyAccepted: false,
      eSignConsent: false
    });

    // Clear component state
    this.currentStep = 1;
    this.application = null;
    this.selectedLoanType = '';
    this.emiCalculation = null;
    this.uploadedDocuments = [];
    this.uploadProgress = {};
    this.selectAllDeclarations = false;
    this.success = '';

    // Clear drafts and service state
    this.clearSavedData();
    
    // Clear localStorage drafts for this user
    this.clearAllUserDrafts();
  }

  clearAllUserDrafts(): void {
    try {
      const allKeys = Object.keys(localStorage);
      const userDraftKeys = allKeys.filter(key => 
        key.startsWith(`loan_draft_${this.applicantId}_`)
      );
      
      userDraftKeys.forEach(key => {
        localStorage.removeItem(key);
      });
      
      console.log(`Cleared ${userDraftKeys.length} draft(s) for user ${this.applicantId}`);
    } catch (error) {
      console.error('Error clearing user drafts:', error);
    }
  }

  // ==================== Select All Declarations ====================

  onSelectAllChange(event: any): void {
    this.selectAllDeclarations = event.target.checked;
    this.toggleAllDeclarations();
  }

  toggleAllDeclarations(): void {
    // Set flag to prevent interference from updateSelectAllState
    this.isTogglingAll = true;
    
    try {
      // Update all required declaration fields
      const updateValues: any = {
        kycConsent: this.selectAllDeclarations,
        creditBureauConsent: this.selectAllDeclarations,
        bankStatementConsent: this.selectAllDeclarations,
        termsAccepted: this.selectAllDeclarations,
        privacyPolicyAccepted: this.selectAllDeclarations
      };
      
      // Optional field - only set if selectAll is true
      if (this.selectAllDeclarations) {
        updateValues.eSignConsent = true;
      }
      
      this.declarationsForm.patchValue(updateValues);
      
      // Mark all controls as touched for validation
      Object.keys(updateValues).forEach(key => {
        this.declarationsForm.get(key)?.markAsTouched();
      });
      
    } finally {
      // Reset flag after a short delay to allow change detection
      setTimeout(() => {
        this.isTogglingAll = false;
      }, 100);
    }
  }

  updateSelectAllState(): void {
    // Don't update during toggle operation to prevent interference
    if (this.isTogglingAll) {
      return;
    }
    
    const requiredFields = ['kycConsent', 'creditBureauConsent', 'bankStatementConsent', 'termsAccepted', 'privacyPolicyAccepted'];
    const allSelected = requiredFields.every(field => this.declarationsForm.get(field)?.value === true);
    this.selectAllDeclarations = allSelected;
  }

  // Custom validator for required true checkboxes
  requiredTrueValidator(control: any) {
    return control.value === true ? null : { requiredTrue: true };
  }

  // ==================== Validation Utilities ====================

  // Get today's date for max date validation
  get maxDate(): string {
    return FormValidators.getTodayDate();
  }

  // Get max birth date (18 years ago)
  get maxBirthDate(): string {
    return FormValidators.getMaxBirthDate();
  }

  // Get validation error message for a field
  getFieldError(formName: string, fieldName: string): string {
    let form: FormGroup;
    
    switch (formName) {
      case 'basic':
        form = this.basicDetailsForm;
        break;
      case 'applicant':
        form = this.applicantDetailsForm;
        break;
      case 'financial':
        form = this.financialDetailsForm;
        break;
      case 'declarations':
        form = this.declarationsForm;
        break;
      default:
        return '';
    }

    const field = form.get(fieldName);
    if (field && field.errors && (field.dirty || field.touched)) {
      if (field.errors['required']) return `${this.getFieldDisplayName(fieldName)} is required`;
      if (field.errors['min']) return `${this.getFieldDisplayName(fieldName)} must be at least ${field.errors['min'].min}`;
      if (field.errors['max']) return `${this.getFieldDisplayName(fieldName)} must not exceed ${field.errors['max'].max}`;
      if (field.errors['minlength']) return `${this.getFieldDisplayName(fieldName)} must be at least ${field.errors['minlength'].requiredLength} characters`;
      if (field.errors['maxlength']) return `${this.getFieldDisplayName(fieldName)} must not exceed ${field.errors['maxlength'].requiredLength} characters`;
      if (field.errors['pattern']) return `${this.getFieldDisplayName(fieldName)} format is invalid`;
      if (field.errors['invalidName']) return field.errors['invalidName'].message;
      if (field.errors['invalidFullName']) return field.errors['invalidFullName'].message;
      if (field.errors['invalidEmail']) return field.errors['invalidEmail'].message;
      if (field.errors['invalidMobile']) return field.errors['invalidMobile'].message;
      if (field.errors['invalidAddress']) return field.errors['invalidAddress'].message;
      if (field.errors['invalidPan']) return field.errors['invalidPan'].message;
      if (field.errors['invalidAadhar']) return field.errors['invalidAadhar'].message;
      if (field.errors['invalidIfsc']) return field.errors['invalidIfsc'].message;
      if (field.errors['invalidPincode']) return field.errors['invalidPincode'].message;
      if (field.errors['invalidAmount']) return field.errors['invalidAmount'].message;
      if (field.errors['futureDate']) return field.errors['futureDate'].message;
      if (field.errors['underAge']) return field.errors['underAge'].message;
      if (field.errors['tooOld']) return field.errors['tooOld'].message;
    }
    return '';
  }

  private getFieldDisplayName(fieldName: string): string {
    const displayNames: { [key: string]: string } = {
      loanType: 'Loan Type',
      loanAmount: 'Loan Amount',
      tenure: 'Tenure',
      coApplicantName: 'Co-applicant Name',
      coApplicantRelation: 'Co-applicant Relation',
      collateralType: 'Collateral Type',
      collateralValue: 'Collateral Value',
      firstName: 'First Name',
      middleName: 'Middle Name',
      lastName: 'Last Name',
      dateOfBirth: 'Date of Birth',
      gender: 'Gender',
      maritalStatus: 'Marital Status',
      emailAddress: 'Email Address',
      mobileNumber: 'Mobile Number',
      alternateNumber: 'Alternate Number',
      currentAddress: 'Current Address',
      currentCity: 'Current City',
      currentState: 'Current State',
      currentPincode: 'Current Pincode',
      residenceType: 'Residence Type',
      yearsAtCurrentAddress: 'Years at Current Address',
      permanentAddress: 'Permanent Address',
      permanentCity: 'Permanent City',
      permanentState: 'Permanent State',
      permanentPincode: 'Permanent Pincode',
      panNumber: 'PAN Number',
      aadharNumber: 'Aadhar Number',
      coApplicantPan: 'Co-applicant PAN',
      coApplicantAadhar: 'Co-applicant Aadhar',
      employmentType: 'Employment Type',
      employerName: 'Employer Name',
      designation: 'Designation',
      workExperience: 'Work Experience',
      monthlyGrossSalary: 'Monthly Gross Salary',
      monthlyNetSalary: 'Monthly Net Salary',
      businessName: 'Business Name',
      businessType: 'Business Type',
      annualTurnover: 'Annual Turnover',
      otherIncomeSources: 'Other Income Sources',
      otherIncomeAmount: 'Other Income Amount',
      existingLoanEMI: 'Existing Loan EMI',
      creditCardPayment: 'Credit Card Payment',
      otherObligations: 'Other Obligations',
      bankName: 'Bank Name',
      accountNumber: 'Account Number',
      ifscCode: 'IFSC Code',
      accountType: 'Account Type'
    };
    return displayNames[fieldName] || fieldName;
  }

  // Debug method - can be called from browser console
  debugDeclarationForm(): void {
    console.log('=== Declaration Form Debug ===');
    console.log('Form exists:', !!this.declarationsForm);
    console.log('Form value:', this.declarationsForm?.value);
    console.log('Form valid:', this.declarationsForm?.valid);
    console.log('Select all state:', this.selectAllDeclarations);
    
    const fields = ['kycConsent', 'creditBureauConsent', 'bankStatementConsent', 'termsAccepted', 'privacyPolicyAccepted', 'eSignConsent'];
    fields.forEach(field => {
      const control = this.declarationsForm.get(field);
      console.log(`${field}:`, {
        exists: !!control,
        value: control?.value,
        valid: control?.valid,
        errors: control?.errors
      });
    });
  }
}
