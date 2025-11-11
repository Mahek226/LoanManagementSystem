import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { FormValidators } from '@core/validators/form-validators';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  loading = false;
  submitted = false;
  errorMessage = '';
  successMessage = '';
  showFallbackLogo = false;
  showTermsModal = false;
  showPrivacyModal = false;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.registerForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
      email: ['', [Validators.required, FormValidators.emailValidator()]],
      password: ['', [Validators.required, FormValidators.strongPasswordValidator()]],
      confirmPassword: ['', [Validators.required]],
      firstName: ['', [Validators.required, FormValidators.nameValidator(), Validators.minLength(2), Validators.maxLength(50)]],
      lastName: ['', [Validators.required, FormValidators.nameValidator(), Validators.minLength(2), Validators.maxLength(50)]],
      phone: ['', [Validators.required, FormValidators.mobileNumberValidator()]],
      dob: ['', [Validators.required, FormValidators.birthDateValidator()]],
      address: ['', [Validators.required, FormValidators.addressValidator(), Validators.minLength(10), Validators.maxLength(200)]],
      city: ['', [Validators.required, FormValidators.nameValidator(), Validators.minLength(2), Validators.maxLength(50)]],
      state: ['', [Validators.required, FormValidators.nameValidator(), Validators.minLength(2), Validators.maxLength(50)]],
      country: ['India', [Validators.required, FormValidators.nameValidator()]],
      gender: ['', [Validators.required]],
      termsAccepted: [false, [Validators.requiredTrue]]
    }, { validators: this.passwordMatchValidator });
  }

  private passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    
    if (confirmPassword?.hasError('passwordMismatch')) {
      delete confirmPassword.errors!['passwordMismatch'];
      if (Object.keys(confirmPassword.errors!).length === 0) {
        confirmPassword.setErrors(null);
      }
    }
    
    return null;
  }

  get f() {
    return this.registerForm.controls;
  }

  // Get today's date for max date validation
  get maxDate(): string {
    return FormValidators.getTodayDate();
  }

  // Get max birth date (18 years ago)
  get maxBirthDate(): string {
    return FormValidators.getMaxBirthDate();
  }

  // Get validation error message for a field
  getFieldError(fieldName: string): string {
    const field = this.registerForm.get(fieldName);
    if (field && field.errors && (field.dirty || field.touched || this.submitted)) {
      if (field.errors['required']) return `${this.getFieldDisplayName(fieldName)} is required`;
      if (field.errors['minlength']) return `${this.getFieldDisplayName(fieldName)} must be at least ${field.errors['minlength'].requiredLength} characters`;
      if (field.errors['maxlength']) return `${this.getFieldDisplayName(fieldName)} must not exceed ${field.errors['maxlength'].requiredLength} characters`;
      if (field.errors['invalidName']) return field.errors['invalidName'].message;
      if (field.errors['invalidFullName']) return field.errors['invalidFullName'].message;
      if (field.errors['invalidEmail']) return field.errors['invalidEmail'].message;
      if (field.errors['invalidMobile']) return field.errors['invalidMobile'].message;
      if (field.errors['invalidAddress']) return field.errors['invalidAddress'].message;
      if (field.errors['futureDate']) return field.errors['futureDate'].message;
      if (field.errors['underAge']) return field.errors['underAge'].message;
      if (field.errors['tooOld']) return field.errors['tooOld'].message;
      if (field.errors['weakPassword']) {
        const errors = field.errors['weakPassword'];
        return Object.values(errors).join(', ');
      }
      if (field.errors['passwordMismatch']) return 'Passwords do not match';
    }
    return '';
  }

  private getFieldDisplayName(fieldName: string): string {
    const displayNames: { [key: string]: string } = {
      username: 'Username',
      email: 'Email',
      password: 'Password',
      confirmPassword: 'Confirm Password',
      firstName: 'First Name',
      lastName: 'Last Name',
      phone: 'Phone Number',
      dob: 'Date of Birth',
      address: 'Address',
      city: 'City',
      state: 'State',
      country: 'Country',
      gender: 'Gender'
    };
    return displayNames[fieldName] || fieldName;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.registerForm.invalid) {
      return;
    }

    this.loading = true;

    // Prepare registration data - remove fields not needed by backend
    const registrationData = {
      ...this.registerForm.value
    };

    // Remove fields not needed by backend
    delete registrationData.confirmPassword;
    delete registrationData.termsAccepted;

    this.authService.register(registrationData).subscribe({
      next: (response) => {
        this.successMessage = 'Registration successful! Please check your email for verification code.';
        setTimeout(() => {
          this.router.navigate(['/auth/verify-email'], { 
            queryParams: { email: registrationData.email } 
          });
        }, 2000);
      },
      error: (error) => {
        // Extract clean error message from backend response
        if (error.error && error.error.message) {
          this.errorMessage = error.error.message;
        } else if (error.message) {
          this.errorMessage = error.message;
        } else if (error.error && typeof error.error === 'string') {
          this.errorMessage = error.error;
        } else {
          this.errorMessage = 'Registration failed. Please try again.';
        }
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  navigateToLogin(): void {
    this.router.navigate(['/auth/login']);
  }

  onImageError(): void {
    this.showFallbackLogo = true;
  }

  openTermsModal(event: Event): void {
    event.preventDefault();
    this.showTermsModal = true;
    setTimeout(() => {
      const modalElement = document.getElementById('termsModal');
      if (modalElement) {
        const modal = new (window as any).bootstrap.Modal(modalElement);
        modal.show();
      }
    }, 100);
  }

  closeTermsModal(): void {
    const modalElement = document.getElementById('termsModal');
    if (modalElement) {
      const modal = (window as any).bootstrap.Modal.getInstance(modalElement);
      if (modal) {
        modal.hide();
      }
    }
    setTimeout(() => {
      this.showTermsModal = false;
    }, 300);
  }

  acceptTerms(): void {
    this.registerForm.patchValue({ termsAccepted: true });
    this.closeTermsModal();
  }

  openPrivacyModal(event: Event): void {
    event.preventDefault();
    this.showPrivacyModal = true;
    setTimeout(() => {
      const modalElement = document.getElementById('privacyModal');
      if (modalElement) {
        const modal = new (window as any).bootstrap.Modal(modalElement);
        modal.show();
      }
    }, 100);
  }

  closePrivacyModal(): void {
    const modalElement = document.getElementById('privacyModal');
    if (modalElement) {
      const modal = (window as any).bootstrap.Modal.getInstance(modalElement);
      if (modal) {
        modal.hide();
      }
    }
    setTimeout(() => {
      this.showPrivacyModal = false;
    }, 300);
  }
}
