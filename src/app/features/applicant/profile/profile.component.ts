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
}
