import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

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

    // Get profile from current user for now
    const user = this.authService.currentUserValue;
    
    if (user) {
      this.profile = {
        officerId: user.officerId || user.userId || 0,
        username: user.username || '',
        email: user.email,
        firstName: user.firstName,
        lastName: user.lastName,
        loanType: 'All Types', // Default
        createdAt: new Date().toISOString()
      };
      
      // Populate edit form
      this.editForm = {
        firstName: this.profile.firstName || '',
        lastName: this.profile.lastName || '',
        email: this.profile.email
      };
      
      this.loading = false;
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
    this.success = 'Profile update feature will be available soon!';
    this.editMode = false;
    
    setTimeout(() => {
      this.success = '';
    }, 3000);
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
