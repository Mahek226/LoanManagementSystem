import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '@environments/environment';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent implements OnInit {
  resetPasswordForm!: FormGroup;
  loading = false;
  message = '';
  error = '';
  token = '';
  email = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.params['token'];
    this.initializeForm();
    this.loadResetPasswordData();
  }

  initializeForm(): void {
    this.resetPasswordForm = this.fb.group({
      email: [{ value: '', disabled: true }],
      newPassword: ['', [
        Validators.required,
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/)
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: AbstractControl): { [key: string]: boolean } | null {
    const newPassword = control.get('newPassword');
    const confirmPassword = control.get('confirmPassword');
    
    if (newPassword && confirmPassword && newPassword.value !== confirmPassword.value) {
      return { passwordMismatch: true };
    }
    return null;
  }

  loadResetPasswordData(): void {
    this.loading = true;
    this.http.get<any>(`${environment.apiUrl}/auth/reset-password/${this.token}`)
      .subscribe({
        next: (response) => {
          this.email = response.email;
          this.resetPasswordForm.patchValue({ email: this.email });
          this.loading = false;
        },
        error: (error) => {
          this.error = error.error?.message || 'Invalid or expired reset token';
          this.loading = false;
        }
      });
  }

  onSubmit(): void {
    if (this.resetPasswordForm.valid) {
      this.loading = true;
      this.error = '';
      this.message = '';

      const formValue = this.resetPasswordForm.getRawValue();
      const resetData = {
        email: this.email,
        resetToken: this.token,
        newPassword: formValue.newPassword,
        confirmPassword: formValue.confirmPassword
      };

      this.http.post<any>(`${environment.apiUrl}/auth/reset-password`, resetData)
        .subscribe({
          next: (response) => {
            this.message = response.message;
            this.loading = false;
            // Redirect to login after 3 seconds
            setTimeout(() => {
              this.router.navigate(['/auth/login']);
            }, 3000);
          },
          error: (error) => {
            this.error = error.error?.message || 'Failed to reset password';
            this.loading = false;
          }
        });
    } else {
      this.markFormGroupTouched();
    }
  }

  markFormGroupTouched(): void {
    Object.keys(this.resetPasswordForm.controls).forEach(key => {
      const control = this.resetPasswordForm.get(key);
      control?.markAsTouched();
    });
  }

  getPasswordErrorMessage(): string {
    const passwordControl = this.resetPasswordForm.get('newPassword');
    if (passwordControl?.hasError('required')) {
      return 'Password is required';
    }
    if (passwordControl?.hasError('pattern')) {
      return 'Password must be at least 8 characters long and contain at least 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special character';
    }
    return '';
  }

  getConfirmPasswordErrorMessage(): string {
    const confirmPasswordControl = this.resetPasswordForm.get('confirmPassword');
    if (confirmPasswordControl?.hasError('required')) {
      return 'Confirm password is required';
    }
    if (this.resetPasswordForm.hasError('passwordMismatch')) {
      return 'Passwords do not match';
    }
    return '';
  }
}
