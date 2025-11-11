import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-email-verification',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './email-verification.component.html',
  styleUrls: ['./email-verification.component.css']
})
export class EmailVerificationComponent implements OnInit {
  verificationForm: FormGroup;
  email: string = '';
  loading = false;
  submitted = false;
  successMessage = '';
  errorMessage = '';
  resendLoading = false;
  resendCooldown = 0;
  cooldownInterval: any;
  showFallbackLogo = false;
  
  // OTP timing controls
  otpValidityTime = 300; // 5 minutes in seconds
  otpTimeRemaining = 300;
  otpTimerInterval: any;
  canResend = false;
  resendEnabledAfter = 60; // 1 minute in seconds
  resendTimeRemaining = 60;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.verificationForm = this.formBuilder.group({
      otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });
  }

  ngOnInit(): void {
    // Get email from query parameters
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
      if (!this.email) {
        // If no email provided, redirect to registration
        this.router.navigate(['/auth/register']);
      } else {
        // Start OTP validity timer
        this.startOtpTimer();
        // Start resend enablement timer
        this.startResendTimer();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.cooldownInterval) {
      clearInterval(this.cooldownInterval);
    }
    if (this.otpTimerInterval) {
      clearInterval(this.otpTimerInterval);
    }
  }

  get f() {
    return this.verificationForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.verificationForm.invalid) {
      return;
    }

    // Check if OTP has expired
    if (this.otpTimeRemaining <= 0) {
      this.errorMessage = 'OTP has expired. Please request a new one.';
      return;
    }

    this.loading = true;

    const verificationData = {
      email: this.email,
      otp: this.verificationForm.value.otp
    };

    this.authService.verifyEmail(verificationData).subscribe({
      next: (response) => {
        this.successMessage = 'Email verified successfully! Redirecting to login...';
        // Clear all timers on successful verification
        this.clearAllTimers();
        setTimeout(() => {
          this.router.navigate(['/auth/login'], { 
            queryParams: { verified: 'true', email: this.email } 
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
          this.errorMessage = 'Verification failed. Please check your OTP and try again.';
        }
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  resendOTP(): void {
    if (!this.canResend || this.resendLoading) {
      return;
    }

    this.resendLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.resendOTP(this.email).subscribe({
      next: (response) => {
        this.successMessage = 'New OTP sent successfully! Please check your email.';
        // Reset timers for new OTP
        this.resetTimers();
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
          this.errorMessage = 'Failed to resend OTP. Please try again.';
        }
      },
      complete: () => {
        this.resendLoading = false;
      }
    });
  }

  private startCooldown(): void {
    this.resendCooldown = 60; // 60 seconds cooldown
    this.cooldownInterval = setInterval(() => {
      this.resendCooldown--;
      if (this.resendCooldown <= 0) {
        clearInterval(this.cooldownInterval);
      }
    }, 1000);
  }

  navigateToRegister(): void {
    this.router.navigate(['/auth/register']);
  }

  navigateToLogin(): void {
    this.router.navigate(['/auth/login']);
  }

  onImageError(): void {
    this.showFallbackLogo = true;
  }

  // Timer management methods
  private startOtpTimer(): void {
    this.otpTimeRemaining = this.otpValidityTime;
    this.otpTimerInterval = setInterval(() => {
      this.otpTimeRemaining--;
      if (this.otpTimeRemaining <= 0) {
        clearInterval(this.otpTimerInterval);
        this.errorMessage = 'OTP has expired. Please request a new one.';
      }
    }, 1000);
  }

  private startResendTimer(): void {
    this.resendTimeRemaining = this.resendEnabledAfter;
    this.canResend = false;
    
    const resendInterval = setInterval(() => {
      this.resendTimeRemaining--;
      if (this.resendTimeRemaining <= 0) {
        this.canResend = true;
        clearInterval(resendInterval);
      }
    }, 1000);
  }

  private resetTimers(): void {
    // Clear existing timers
    this.clearAllTimers();
    
    // Reset OTP validity timer
    this.startOtpTimer();
    
    // Reset resend timer
    this.startResendTimer();
    
    // Clear form
    this.verificationForm.patchValue({ otp: '' });
    this.submitted = false;
  }

  private clearAllTimers(): void {
    if (this.otpTimerInterval) {
      clearInterval(this.otpTimerInterval);
    }
    if (this.cooldownInterval) {
      clearInterval(this.cooldownInterval);
    }
  }

  // Utility methods for time formatting
  formatTime(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  }

  get otpExpiryDisplay(): string {
    return this.formatTime(this.otpTimeRemaining);
  }

  get resendTimerDisplay(): string {
    return this.formatTime(this.resendTimeRemaining);
  }
}
