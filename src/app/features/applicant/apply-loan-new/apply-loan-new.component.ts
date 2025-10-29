import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { ApplicantService, ApplicantProfile } from '@core/services/applicant.service';
import { 
  LoanApplicationService, 
  LoanApplicationForm, 
  BasicDetails,
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
  error: string = '';
  success: string = '';
  
  // Subscriptions
  private subscriptions: Subscription[] = [];
  
  // Applicant ID
  applicantId: number = 0;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
    private applicantService: ApplicantService,
    private loanApplicationService: LoanApplicationService
  ) {
    const user = this.authService.currentUserValue;
    this.applicantId = user?.id || 0;
  }

  ngOnInit(): void {
    this.initializeForms();
    this.loadProfile();
    this.loadLoanType();
    this.subscribeToApplicationState();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  // ==================== Initialization ====================

  initializeForms(): void {
    // Step 1: Basic Details Form
    this.basicDetailsForm = this.fb.group({
      loanType: ['', Validators.required],
      loanAmount: [0, [Validators.required, Validators.min(1000)]],
      tenure: [12, [Validators.required, Validators.min(6)]],
      purpose: ['', [Validators.required, Validators.minLength(10)]],
      hasCoApplicant: [false],
      coApplicantName: [''],
      coApplicantRelation: [''],
      hasCollateral: [false],
      collateralType: [''],
      collateralValue: [0]
    });

    // Step 2: Applicant Details Form
    this.applicantDetailsForm = this.fb.group({
      // Applicant Information
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      middleName: [''],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      dateOfBirth: ['', Validators.required],
      gender: ['', Validators.required],
      maritalStatus: ['', Validators.required],
      
      // Contact Information
      emailAddress: ['', [Validators.required, Validators.email]],
      mobileNumber: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
      alternateNumber: ['', [Validators.pattern(/^[6-9]\d{9}$/)]],
      
      // Current Address
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
      panNumber: ['', [Validators.required, Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/)]],
      aadharNumber: ['', [Validators.required, Validators.pattern(/^\d{12}$/)]],
      
      // Co-applicant Details
      hasCoApplicant: [false],
      coApplicantName: [''],
      coApplicantRelation: [''],
      coApplicantPan: [''],
      coApplicantAadhar: ['']
    });

    // Step 3: Financial Details Form
    this.financialDetailsForm = this.fb.group({
      employmentType: ['SALARIED', Validators.required],
      employerName: [''],
      designation: [''],
      workExperience: [0],
      monthlyGrossSalary: [0],
      monthlyNetSalary: [0],
      businessName: [''],
      businessType: [''],
      annualTurnover: [0],
      otherIncomeSources: [''],
      otherIncomeAmount: [0],
      existingLoanEMI: [0],
      creditCardPayment: [0],
      otherObligations: [0],
      bankName: ['', Validators.required],
      accountNumber: ['', Validators.required],
      ifscCode: ['', [Validators.required, Validators.pattern(/^[A-Z]{4}0[A-Z0-9]{6}$/)]],
      accountType: ['SAVINGS', Validators.required],
      bankStatementConsent: [false, Validators.requiredTrue]
    });

    // Step 4: Declarations Form
    this.declarationsForm = this.fb.group({
      kycConsent: [false, Validators.requiredTrue],
      creditBureauConsent: [false, Validators.requiredTrue],
      bankStatementConsent: [false, Validators.requiredTrue],
      termsAccepted: [false, Validators.requiredTrue],
      privacyPolicyAccepted: [false, Validators.requiredTrue],
      eSignConsent: [false]
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
        this.error = 'Failed to load profile data';
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
        this.loanApplicationService.initializeApplication(this.applicantId, loanType.id);
        
        // Load required documents
        this.requiredDocuments = this.loanApplicationService.getRequiredDocuments(loanType.id);
        
        localStorage.removeItem('selectedLoanType');
      } catch (error) {
        console.error('Error parsing loan type:', error);
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
    this.uploadedDocuments = app.documents;
  }

  // ==================== Form Validators ====================

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
      if (this.validateCurrentStep()) {
        this.saveCurrentStep();
        this.currentStep++;
        this.loanApplicationService.setCurrentStep(this.currentStep);
        
        // Load required documents when entering step 3
        if (this.currentStep === 3) {
          this.loadRequiredDocuments();
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
    } else {
      this.error = 'Please select a loan type first';
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
    this.error = '';
    
    switch (this.currentStep) {
      case 1:
        if (!this.basicDetailsForm.valid) {
          this.error = 'Please fill all required fields in Basic Details';
          return false;
        }
        return true;
      case 2:
        if (!this.financialDetailsForm.valid) {
          this.error = 'Please fill all required fields in Financial Details';
          return false;
        }
        if (!this.emiCalculation || this.emiCalculation.dti > 60) {
          this.error = 'Your debt-to-income ratio is too high. Please adjust loan amount or tenure.';
          return false;
        }
        return true;
      case 3:
        const requiredTypes = this.requiredDocuments.filter(d => d.required).map(d => d.documentType);
        const uploadedTypes = this.uploadedDocuments.map(d => d.documentType);
        const missingDocs = requiredTypes.filter(type => !uploadedTypes.includes(type));
        if (missingDocs.length > 0) {
          this.error = `Please upload all required documents: ${missingDocs.join(', ')}`;
          return false;
        }
        return true;
      case 4:
        if (!this.declarationsForm.valid) {
          this.error = 'Please accept all required declarations and consents';
          return false;
        }
        return true;
      default:
        return true;
    }
  }

  saveCurrentStep(): void {
    switch (this.currentStep) {
      case 1:
        this.loanApplicationService.updateBasicDetails(this.basicDetailsForm.value);
        this.calculateEMI();
        break;
      case 2:
        this.loanApplicationService.updateFinancialDetails(this.financialDetailsForm.value);
        break;
      case 4:
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

      if (!docReq.acceptedFormats.includes(file.type)) {
        this.error = `Invalid file format for ${docReq.displayName}. Accepted formats: ${docReq.acceptedFormats.join(', ')}`;
        return;
      }

      if (file.size > docReq.maxSize) {
        this.error = `File size exceeds maximum limit of ${(docReq.maxSize / (1024 * 1024)).toFixed(0)}MB`;
        return;
      }

      // Upload file to Cloudinary via backend
      this.loanApplicationService.uploadDocument(file, documentType, this.applicantId).subscribe({
        next: (response) => {
          // Update progress
          this.uploadProgress[file.name] = {
            fileName: file.name,
            progress: response.progress || 0,
            status: response.status || 'uploading'
          };
          
          if (response.status === 'success' && response.data) {
            const uploadData = response.data;
            
            // Add to uploaded documents with Cloudinary URL
            const document: DocumentUpload = {
              documentType,
              fileName: uploadData.document?.originalFilename || file.name,
              fileUrl: uploadData.document?.cloudinaryUrl || '',
              fileSize: file.size,
              uploadedAt: uploadData.document?.uploadedAt || new Date().toISOString(),
              status: 'PENDING'
            };
            
            // Add document via service (which updates both service state and local array)
            this.loanApplicationService.addDocument(document);
            
            // Sync local array with service state
            const app = this.loanApplicationService.getCurrentApplication();
            if (app) {
              this.uploadedDocuments = app.documents;
            }
            
            this.success = `${file.name} uploaded successfully!`;
            
            setTimeout(() => {
              delete this.uploadProgress[file.name];
              this.success = '';
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
          this.error = error.error?.message || `Failed to upload ${file.name}`;
          
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
    
    // Sync local array with service state
    const app = this.loanApplicationService.getCurrentApplication();
    if (app) {
      this.uploadedDocuments = app.documents;
    }
  }

  getDocumentIcon(documentType: string): string {
    const doc = this.requiredDocuments.find(d => d.documentType === documentType);
    return doc?.icon || 'fa-file';
  }

  // ==================== Submit Application ====================

  submitApplication(): void {
    if (!this.application) {
      this.error = 'No application data found';
      return;
    }

    this.submitting = true;
    this.error = '';

    this.loanApplicationService.submitApplication(this.application).subscribe({
      next: (response) => {
        this.success = 'Application submitted successfully!';
        this.submitting = false;
        
        // Clear draft
        this.loanApplicationService.clearDraft();
        
        // Redirect to dashboard after 2 seconds
        setTimeout(() => {
          this.router.navigate(['/applicant/dashboard']);
        }, 2000);
      },
      error: (error) => {
        console.error('Submission error:', error);
        this.error = error.error?.message || 'Failed to submit application. Please try again.';
        this.submitting = false;
      }
    });
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
      this.loanApplicationService.clearDraft();
      this.router.navigate(['/applicant/dashboard']);
    }
  }

  clearError(): void {
    this.error = '';
  }

  clearSuccess(): void {
    this.success = '';
  }

  // Helper method to check if document is uploaded
  isDocumentUploaded(documentType: string): boolean {
    return this.uploadedDocuments.some(d => d.documentType === documentType);
  }
}
