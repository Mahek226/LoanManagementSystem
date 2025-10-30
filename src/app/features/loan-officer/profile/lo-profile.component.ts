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

  officerId: number = 0;

  // Edit form data
  editForm = {
    firstName: '',
    lastName: '',
    email: ''
  };

  constructor(
    private authService: AuthService,
    private loanOfficerService: LoanOfficerService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.officerId = user?.officerId || user?.userId || 0;
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.error = '';

    const user = this.authService.currentUserValue;
    
    if (user && user.officerId) {
      // Fetch profile from API
      this.loanOfficerService.getOfficerProfile(user.officerId)
        .subscribe({
          next: (profile: OfficerProfile) => {
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
            console.error('Error loading profile:', err);
            this.error = 'Failed to load profile';
            this.loading = false;
          }
        });
    } else {
      this.error = 'Unable to load profile. Please login again.';
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

    const user = this.authService.currentUserValue;
    
    if (user && user.officerId) {
      // Call API to update profile
      this.loanOfficerService.updateOfficerProfile(user.officerId, this.editForm)
        .subscribe({
          next: (response: any) => {
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
            this.error = 'Failed to update profile: ' + (err.error?.message || 'Unknown error');
            console.error('Error updating profile:', err);
            this.loading = false;
          }
        });
    } else {
      this.error = 'Unable to update profile. Please login again.';
      this.loading = false;
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
