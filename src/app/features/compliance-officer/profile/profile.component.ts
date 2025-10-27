import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

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

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    const user = this.authService.currentUserValue;
    if (user) {
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

    // Simulate API call - replace with actual service call
    setTimeout(() => {
      this.loading = false;
      this.successMessage = 'Profile updated successfully!';
      this.editMode = false;

      // Update local user data
      if (this.officer) {
        this.officer.firstName = this.profileData.firstName;
        this.officer.lastName = this.profileData.lastName;
      }

      // Auto-dismiss success message
      setTimeout(() => {
        this.successMessage = '';
      }, 3000);
    }, 1000);
  }

  getMemberSince(): string {
    // Simulate member since date
    return 'January 2024';
  }

  getOfficerId(): string {
    return this.officer?.id?.toString() || 'N/A';
  }
}
