import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

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
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      phone: ['', [Validators.required]], // Changed from phoneNumber to phone
      dob: ['', [Validators.required]], // Added date of birth
      address: ['', [Validators.required]],
      city: ['', [Validators.required]],
      state: ['', [Validators.required]],
      country: ['', [Validators.required]],
      gender: ['', [Validators.required]],
      termsAccepted: [false, [Validators.requiredTrue]] // Terms and conditions checkbox
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

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.registerForm.invalid) {
      return;
    }

    this.loading = true;

    // Prepare registration data with default role as APPLICANT
    const registrationData = {
      ...this.registerForm.value,
      role: 'ROLE_APPLICANT'
    };

    // Remove confirmPassword from the data sent to backend
    delete registrationData.confirmPassword;

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
