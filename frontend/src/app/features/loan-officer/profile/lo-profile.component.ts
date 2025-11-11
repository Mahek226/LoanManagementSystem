import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { LoanOfficerService } from '@core/services/loan-officer.service';

interface OfficerProfile {
  officerId: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  loanType?: string;
  createdAt?: string;
}

@Component({
  selector: 'app-lo-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lo-profile.component.html',
  styleUrl: './lo-profile.component.css'
})
export class LoProfileComponent implements OnInit {
  profile: OfficerProfile | null = null;
  loading = false;
  error = '';
  success = '';
  editMode = false;
  passwordChangeMode = false;
  passwordChanging = false;

  officerId: number = 0;

  // Edit form data
  editForm = {
    firstName: '',
    lastName: '',
    email: ''
  };

  // Password change form data
  passwordChangeForm = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  constructor(
    private authService: AuthService,
    private loanOfficerService: LoanOfficerService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    console.log('Current user in profile component:', user);
    this.officerId = user?.officerId || user?.userId || 0;
    console.log('Extracted officer ID:', this.officerId);
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.error = '';

    console.log('Loading profile for officer ID:', this.officerId);
    
    if (this.officerId && this.officerId > 0) {
      // Try the main profile method first
      this.loanOfficerService.getOfficerProfile(this.officerId)
        .subscribe({
          next: (profile: OfficerProfile) => {
            console.log('Profile loaded successfully:', profile);
            this.profile = profile;
            
            // Populate edit form
            this.editForm = {
              firstName: this.profile.firstName || '',
              lastName: this.profile.lastName || '',
              email: this.profile.email
            };
            
            this.loading = false;
          },
          error: (err: any) => {
            console.error('Primary method failed, trying alternative:', err);
            
            // Try alternative method
            this.loanOfficerService.getOfficerProfileAlternative(this.officerId)
              .subscribe({
                next: (profile: OfficerProfile) => {
                  console.log('Alternative method succeeded:', profile);
                  this.profile = profile;
                  
                  // Populate edit form
                  this.editForm = {
                    firstName: this.profile.firstName || '',
                    lastName: this.profile.lastName || '',
                    email: this.profile.email
                  };
                  
                  this.loading = false;
                },
                error: (altErr: any) => {
                  console.error('Both methods failed');
                  console.error('Primary error:', err);
                  console.error('Alternative error:', altErr);
                  this.error = `Failed to load profile: ${err.message || 'Unknown error'}. Please check browser console for details.`;
                  this.loading = false;
                }
              });
          }
        });
    } else {
      console.error('Invalid officer ID:', this.officerId);
      this.error = 'Unable to load profile. Officer ID not found. Please login again.';
      this.loading = false;
    }
  }

  toggleEditMode(): void {
    this.editMode = !this.editMode;
    if (!this.editMode) {
      // Reset form if canceling
      this.editForm = {
        firstName: this.profile?.firstName || '',
        lastName: this.profile?.lastName || '',
        email: this.profile?.email || ''
      };
    }
  }

  saveProfile(): void {
    this.loading = true;
    this.error = '';
    this.success = '';

    console.log('Saving profile for officer ID:', this.officerId);
    console.log('Profile data:', this.editForm);
    
    if (this.officerId && this.officerId > 0) {
      // Call API to update profile
      this.loanOfficerService.updateOfficerProfile(this.officerId, this.editForm)
        .subscribe({
          next: (response: any) => {
            console.log('Profile updated successfully:', response);
            this.success = 'Profile updated successfully!';
            this.editMode = false;
            this.loading = false;
            
            // Reload profile to get updated data
            this.loadProfile();
            
            setTimeout(() => {
              this.success = '';
            }, 3000);
          },
          error: (err: any) => {
            console.error('Error updating profile:', err);
            console.error('Error details:', err.error);
            console.error('Status:', err.status);
            this.error = `Failed to update profile: ${err.error?.message || err.message || 'Unknown error'}`;
            this.loading = false;
          }
        });
    } else {
      console.error('Invalid officer ID for update:', this.officerId);
      this.error = 'Unable to update profile. Officer ID not found. Please login again.';
      this.loading = false;
    }
  }

  togglePasswordChangeMode(): void {
    this.passwordChangeMode = !this.passwordChangeMode;
    if (!this.passwordChangeMode) {
      // Reset form when closing
      this.resetPasswordForm();
    }
    // Clear any existing messages
    this.error = '';
    this.success = '';
  }

  cancelPasswordChange(): void {
    this.passwordChangeMode = false;
    this.resetPasswordForm();
    this.error = '';
    this.success = '';
  }

  resetPasswordForm(): void {
    this.passwordChangeForm = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    };
  }

  changePassword(): void {
    // Validate passwords match
    if (this.passwordChangeForm.newPassword !== this.passwordChangeForm.confirmPassword) {
      this.error = 'New passwords do not match';
      return;
    }

    // Validate password strength
    if (this.passwordChangeForm.newPassword.length < 6) {
      this.error = 'New password must be at least 6 characters long';
      return;
    }

    this.passwordChanging = true;
    this.error = '';
    this.success = '';

    console.log('Changing password for officer ID:', this.officerId);

    if (this.officerId && this.officerId > 0) {
      // Call API to change password
      this.loanOfficerService.changePassword(this.officerId, {
        currentPassword: this.passwordChangeForm.currentPassword,
        newPassword: this.passwordChangeForm.newPassword
      }).subscribe({
        next: (response: any) => {
          console.log('Password changed successfully:', response);
          this.success = 'Password changed successfully!';
          this.passwordChangeMode = false;
          this.passwordChanging = false;
          this.resetPasswordForm();
          
          setTimeout(() => {
            this.success = '';
          }, 5000);
        },
        error: (err: any) => {
          console.error('Error changing password:', err);
          console.error('Error details:', err.error);
          console.error('Status:', err.status);
          
          if (err.status === 400) {
            this.error = err.error?.message || 'Current password is incorrect';
          } else if (err.status === 401) {
            this.error = 'Authentication failed. Please login again.';
          } else {
            this.error = `Failed to change password: ${err.error?.message || err.message || 'Unknown error'}`;
          }
          
          this.passwordChanging = false;
        }
      });
    } else {
      console.error('Invalid officer ID for password change:', this.officerId);
      this.error = 'Unable to change password. Officer ID not found. Please login again.';
      this.passwordChanging = false;
    }
  }

  goBack(): void {
    this.router.navigate(['/loan-officer/dashboard']);
  }

  formatDate(dateString?: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric'
    });
  }
}
