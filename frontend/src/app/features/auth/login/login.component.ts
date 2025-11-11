import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { APP_CONSTANTS } from '@core/constants/app.constants';
import { EnhancedNotificationService } from '../../../core/services/enhanced-notification.service';
import { LoadingService } from '../../../core/services/loading.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  submitted = false;
  errorMessage = '';
  returnUrl = '';
  showFallbackLogo = false;
  
  // Math CAPTCHA properties
  mathQuestion = '';
  mathAnswer = 0;
  captchaToken: string | null = null;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: EnhancedNotificationService,
    private loadingService: LoadingService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '';
  }

  private initializeForm(): void {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
      captchaAnswer: ['', [Validators.required]]
    });
    this.generateMathCaptcha();
  }

  get f() {
    return this.loginForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';

    if (this.loginForm.invalid) {
      this.errorMessage = 'Please fill in all required fields.';
      return;
    }

    // Verify math CAPTCHA
    const userAnswer = parseInt(this.loginForm.get('captchaAnswer')?.value);
    if (userAnswer !== this.mathAnswer) {
      this.errorMessage = 'Incorrect CAPTCHA answer. Please try again.';
      this.notificationService.warning(
        'CAPTCHA Verification Failed',
        'Please solve the math problem correctly to continue.',
        {
          action: {
            label: 'New Question',
            callback: () => this.refreshCaptcha()
          }
        }
      );
      this.generateMathCaptcha();
      this.loginForm.patchValue({ captchaAnswer: '' });
      return;
    }

    this.loading = true;
    this.loadingService.startLoading('login', 'Signing you in...', 'spinner');

    // Generate a simple token for backend verification
    this.captchaToken = `math_captcha_${Date.now()}_verified`;

    const loginData = {
      ...this.loginForm.value,
      captchaToken: this.captchaToken
    };

    this.authService.login(loginData).subscribe({
      next: () => {
        this.notificationService.success(
          'Login Successful!', 
          'Welcome back! Redirecting to your dashboard...'
        );
        
        // Redirect to return URL or default dashboard based on role
        const redirectUrl = this.returnUrl || this.authService.getDefaultRoute();
        
        // Add a small delay to show the success notification
        setTimeout(() => {
          this.router.navigate([redirectUrl]);
        }, 1000);
      },
      error: (error) => {
        this.errorMessage = error.message || 'Login failed. Please check your credentials.';
        this.loading = false;
        this.loadingService.stopLoading('login');
        
        this.notificationService.error(
          'Login Failed',
          this.errorMessage,
          {
            action: {
              label: 'Try Again',
              callback: () => {
                this.errorMessage = '';
                this.generateMathCaptcha();
                this.loginForm.patchValue({ captchaAnswer: '' });
              }
            }
          }
        );
        
        // Reset CAPTCHA on error
        this.generateMathCaptcha();
        this.loginForm.patchValue({ captchaAnswer: '' });
      },
      complete: () => {
        this.loading = false;
        this.loadingService.stopLoading('login');
      }
    });
  }

  navigateToForgotPassword(): void {
    this.router.navigate([APP_CONSTANTS.ROUTES.FORGOT_PASSWORD]);
  }

  onImageError(): void {
    this.showFallbackLogo = true;
  }

  generateMathCaptcha(): void {
    const num1 = Math.floor(Math.random() * 10) + 1;
    const num2 = Math.floor(Math.random() * 10) + 1;
    const operations = ['+', '-', '*'];
    const operation = operations[Math.floor(Math.random() * operations.length)];
    
    switch (operation) {
      case '+':
        this.mathAnswer = num1 + num2;
        break;
      case '-':
        this.mathAnswer = num1 - num2;
        break;
      case '*':
        this.mathAnswer = num1 * num2;
        break;
    }
    
    this.mathQuestion = `${num1} ${operation} ${num2} = ?`;
  }

  refreshCaptcha(): void {
    this.generateMathCaptcha();
    this.loginForm.patchValue({ captchaAnswer: '' });
    this.errorMessage = '';
  }
}
