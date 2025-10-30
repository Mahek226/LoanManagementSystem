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
      this.officer = user;
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
    // Simulate member since date
    return 'January 2024';
  }

  getOfficerId(): string {
    return this.officer?.id?.toString() || 'N/A';
  }
}
