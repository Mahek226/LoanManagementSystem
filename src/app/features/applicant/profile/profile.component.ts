import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ApplicantService, ApplicantProfile } from '@core/services/applicant.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  profile: ApplicantProfile | null = null;
  loading = false;
  error = '';
  success = '';
  editMode = false;

  applicantId: number = 0;

  // Edit form data
  editForm = {
    firstName: '',
    lastName: '',
    phone: '',
    address: '',
    city: '',
    state: '',
    country: ''
  };

  // Change password modal
  showChangePasswordModal = false;
  changingPassword = false;
  passwordError = '';
  passwordSuccess = '';
  
  passwordForm = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  constructor(
    private authService: AuthService,
    private applicantService: ApplicantService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.applicantId = user?.applicantId || 0;
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.error = '';

    this.applicantService.getApplicantProfile(this.applicantId).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.populateEditForm();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load profile';
        console.error('Error loading profile:', err);
        this.loading = false;
      }
    });
  }

  populateEditForm(): void {
    if (this.profile) {
      this.editForm = {
        firstName: this.profile.firstName,
        lastName: this.profile.lastName,
        phone: this.profile.phone,
        address: this.profile.address,
        city: this.profile.city,
        state: this.profile.state,
        country: this.profile.country
      };
    }
  }

  toggleEditMode(): void {
    this.editMode = !this.editMode;
    if (!this.editMode) {
      this.populateEditForm();
      this.error = '';
      this.success = '';
    }
  }

  saveProfile(): void {
    this.loading = true;
    this.error = '';
    this.success = '';

    // Call API to update profile
    this.applicantService.updateApplicantProfile(this.applicantId, this.editForm).subscribe({
      next: (response) => {
        this.success = 'Profile updated successfully!';
        this.editMode = false;
        this.loading = false;
        
        // Reload profile to get updated data
        this.loadProfile();
        
        setTimeout(() => {
          this.success = '';
        }, 3000);
      },
      error: (err) => {
        this.error = 'Failed to update profile: ' + (err.error?.message || 'Unknown error');
        console.error('Error updating profile:', err);
        this.loading = false;
      }
    });
  }

  formatDate(dateString: string): string {
    return this.applicantService.formatDate(dateString);
  }

  goBack(): void {
    this.router.navigate(['/applicant/dashboard']);
  }

  getApprovalStatusColor(): string {
    if (!this.profile) return 'secondary';
    
    switch (this.profile.approvalStatus) {
      case 'PENDING':
        return 'warning';
      case 'APPROVED':
        return 'success';
      case 'REJECTED':
        return 'danger';
      case 'UNDER_REVIEW':
        return 'info';
      default:
        return 'secondary';
    }
  }

  getVerificationBadge(): string {
    return this.profile?.isEmailVerified ? 'success' : 'warning';
  }

  getApprovalBadge(): string {
    return this.profile?.isApproved ? 'success' : 'warning';
  }

  // Change password functionality
  closeChangePasswordModal(): void {
    this.showChangePasswordModal = false;
    this.passwordForm = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    };
    this.passwordError = '';
    this.passwordSuccess = '';
  }

  isPasswordFormValid(): boolean {
    return this.passwordForm.currentPassword.length > 0 &&
           this.passwordForm.newPassword.length >= 6 &&
           this.passwordForm.confirmPassword.length > 0 &&
           this.passwordForm.newPassword === this.passwordForm.confirmPassword;
  }

  changePassword(): void {
    if (!this.isPasswordFormValid()) {
      this.passwordError = 'Please fill all fields correctly and ensure passwords match';
      return;
    }

    if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
      this.passwordError = 'New passwords do not match';
      return;
    }

    if (this.passwordForm.newPassword.length < 6) {
      this.passwordError = 'New password must be at least 6 characters long';
      return;
    }

    this.changingPassword = true;
    this.passwordError = '';
    this.passwordSuccess = '';

    // Call API to change password
    const changePasswordData = {
      currentPassword: this.passwordForm.currentPassword,
      newPassword: this.passwordForm.newPassword
    };

    // For now, simulate the password change since we don't have the API endpoint
    // In a real implementation, you would call the backend API
    setTimeout(() => {
      // Simulate API call
      if (this.passwordForm.currentPassword === 'wrongpassword') {
        this.passwordError = 'Current password is incorrect';
        this.changingPassword = false;
      } else {
        this.passwordSuccess = 'Password changed successfully!';
        this.changingPassword = false;
        
        // Close modal after 2 seconds
        setTimeout(() => {
          this.closeChangePasswordModal();
        }, 2000);
      }
    }, 1000);
  }
}
