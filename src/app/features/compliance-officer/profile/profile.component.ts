import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { ComplianceOfficerService } from '../../../core/services/compliance-officer.service';

@Component({
  selector: 'app-compliance-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ComplianceProfileComponent implements OnInit {
  officer: any = null;
  editMode: boolean = false;
  loading: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  // Password change properties
  changingPassword: boolean = false;
  passwordChangeSuccess: string = '';
  passwordChangeError: string = '';
  passwordData = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  // Form data
  profileData = {
    firstName: '',
    lastName: '',
    email: '',
    username: ''
  };

  constructor(
    private authService: AuthService,
    private complianceOfficerService: ComplianceOfficerService
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    const user = this.authService.currentUserValue;
    if (user && user.officerId) {
      this.loading = true;
      this.complianceOfficerService.getOfficerProfile(user.officerId)
        .subscribe({
          next: (profile: any) => {
            this.officer = profile;
            // Ensure officerId is set in the profile object
            if (!this.officer.officerId && user.officerId) {
              this.officer.officerId = user.officerId;
            }
            this.profileData = {
              firstName: profile.firstName || '',
              lastName: profile.lastName || '',
              email: profile.email || '',
              username: profile.username || ''
            };
            this.loading = false;
          },
          error: (err: any) => {
            console.error('Error loading profile:', err);
            this.errorMessage = 'Failed to load profile';
            this.loading = false;
          }
        });
    } else if (user) {
      // Fallback to cached user data
      this.officer = {
        ...user,
        officerId: user.officerId,
        id: user.officerId || user.id
      };
      this.profileData = {
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        email: user.email || '',
        username: user.username || ''
      };
    }
  }

  toggleEditMode(): void {
    if (this.editMode) {
      // Cancel edit - reload original data
      this.loadProfile();
    }
    this.editMode = !this.editMode;
    this.successMessage = '';
    this.errorMessage = '';
  }

  saveProfile(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const user = this.authService.currentUserValue;
    
    if (user && user.officerId) {
      // Call API to update profile
      this.complianceOfficerService.updateOfficerProfile(user.officerId, this.profileData)
        .subscribe({
          next: (response: any) => {
            this.loading = false;
            this.successMessage = 'Profile updated successfully!';
            this.editMode = false;

            // Update local user data
            if (this.officer) {
              this.officer.firstName = this.profileData.firstName;
              this.officer.lastName = this.profileData.lastName;
            }

            // Reload profile to get updated data
            this.loadProfile();

            // Auto-dismiss success message
            setTimeout(() => {
              this.successMessage = '';
            }, 3000);
          },
          error: (err: any) => {
            this.loading = false;
            this.errorMessage = 'Failed to update profile: ' + (err.error?.message || 'Unknown error');
            console.error('Error updating profile:', err);
          }
        });
    } else {
      this.loading = false;
      this.errorMessage = 'Unable to update profile. Please login again.';
    }
  }

  getMemberSince(): string {
    if (this.officer?.createdAt) {
      const date = new Date(this.officer.createdAt);
      return new Intl.DateTimeFormat('en-US', {
        year: 'numeric',
        month: 'long'
      }).format(date);
    }
    if (this.officer?.joinedDate) {
      const date = new Date(this.officer.joinedDate);
      return new Intl.DateTimeFormat('en-US', {
        year: 'numeric',
        month: 'long'
      }).format(date);
    }
    // Fallback to current user data if available
    const user = this.authService.currentUserValue;
    if (user?.officerId) {
      // For now, use a default since we don't have join date in user object
      return 'January 2024';
    }
    return 'N/A';
  }

  getOfficerId(): string {
    // First try to get from loaded officer profile
    if (this.officer?.officerId) {
      return this.officer.officerId.toString();
    }
    if (this.officer?.id) {
      return this.officer.id.toString();
    }
    // Fallback to current user data
    const user = this.authService.currentUserValue;
    if (user?.officerId) {
      return user.officerId.toString();
    }
    return 'N/A';
  }

  openChangePasswordModal(): void {
    // Reset password form data and messages
    this.passwordData = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    };
    this.passwordChangeSuccess = '';
    this.passwordChangeError = '';
    this.changingPassword = false;

    // Open the modal using Bootstrap's modal API
    const modalElement = document.getElementById('changePasswordModal');
    if (modalElement) {
      const modal = new (window as any).bootstrap.Modal(modalElement);
      modal.show();
    }
  }

  changePassword(): void {
    if (this.passwordData.newPassword !== this.passwordData.confirmPassword) {
      this.passwordChangeError = 'Passwords do not match';
      return;
    }

    this.changingPassword = true;
    this.passwordChangeError = '';
    this.passwordChangeSuccess = '';

    const user = this.authService.currentUserValue;
    if (!user || !user.officerId) {
      this.passwordChangeError = 'Unable to change password. Please login again.';
      this.changingPassword = false;
      return;
    }

    const changePasswordRequest = {
      currentPassword: this.passwordData.currentPassword,
      newPassword: this.passwordData.newPassword
    };

    // Call the change password API
    this.complianceOfficerService.changePassword(user.officerId, changePasswordRequest)
      .subscribe({
        next: (response: any) => {
          this.changingPassword = false;
          this.passwordChangeSuccess = 'Password changed successfully!';
          
          // Clear the form
          this.passwordData = {
            currentPassword: '',
            newPassword: '',
            confirmPassword: ''
          };

          // Auto-close modal after 2 seconds
          setTimeout(() => {
            const modalElement = document.getElementById('changePasswordModal');
            if (modalElement) {
              const modal = (window as any).bootstrap.Modal.getInstance(modalElement);
              if (modal) {
                modal.hide();
              }
            }
          }, 2000);
        },
        error: (err: any) => {
          this.changingPassword = false;
          this.passwordChangeError = err.error?.message || 'Failed to change password. Please try again.';
          console.error('Error changing password:', err);
        }
      });
  }
}
