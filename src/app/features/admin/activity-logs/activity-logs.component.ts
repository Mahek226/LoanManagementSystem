import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, ActivityLog } from '../../../core/services/admin.service';

@Component({
  selector: 'app-activity-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './activity-logs.component.html',
  styleUrl: './activity-logs.component.css'
})
export class ActivityLogsComponent implements OnInit {
  activities: ActivityLog[] = [];
  filteredActivities: ActivityLog[] = [];
  loading = false;
  error = '';
  
  searchQuery = '';
  selectedType = '';
  selectedStatus = '';
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  pageSize = 20;
  
  activityTypes = ['LOGIN', 'CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT', 'LOGOUT'];
  statusTypes = ['SUCCESS', 'FAILED'];

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadActivities();
  }

  loadActivities(): void {
    this.loading = true;
    this.error = '';
    
    this.adminService.getAllActivityLogs(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.activities = response.content || [];
        this.filteredActivities = this.activities;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || 0;
        this.loading = false;
        this.applyFilters();
      },
      error: (err) => {
        this.error = 'Failed to load activity logs';
        console.error('Error loading activities:', err);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.activities];
    
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(activity => 
        activity.performedBy.toLowerCase().includes(query) ||
        activity.activityType.toLowerCase().includes(query) ||
        activity.description.toLowerCase().includes(query)
      );
    }
    
    if (this.selectedType) {
      filtered = filtered.filter(activity => activity.activityType === this.selectedType);
    }
    
    if (this.selectedStatus) {
      filtered = filtered.filter(activity => activity.status === this.selectedStatus);
    }
    
    this.filteredActivities = filtered;
  }

  onSearchChange(): void {
    if (this.searchQuery.trim()) {
      this.loading = true;
      this.adminService.searchActivityLogs(this.searchQuery, this.currentPage, this.pageSize).subscribe({
        next: (response) => {
          this.activities = response.content || [];
          this.filteredActivities = this.activities;
          this.totalPages = response.totalPages || 0;
          this.totalElements = response.totalElements || 0;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Failed to search activities';
          this.loading = false;
        }
      });
    } else {
      this.loadActivities();
    }
  }

  onTypeChange(): void {
    if (this.selectedType) {
      this.loading = true;
      this.adminService.getActivityLogsByType(this.selectedType, this.currentPage, this.pageSize).subscribe({
        next: (response) => {
          this.activities = response.content || [];
          this.filteredActivities = this.activities;
          this.totalPages = response.totalPages || 0;
          this.totalElements = response.totalElements || 0;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Failed to filter activities';
          this.loading = false;
        }
      });
    } else {
      this.loadActivities();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadActivities();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadActivities();
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString();
  }

  getActivityIcon(type: string): string {
    const icons: any = {
      'LOGIN': 'fa-sign-in-alt',
      'LOGOUT': 'fa-sign-out-alt',
      'CREATE': 'fa-plus-circle',
      'UPDATE': 'fa-edit',
      'DELETE': 'fa-trash',
      'APPROVE': 'fa-check-circle',
      'REJECT': 'fa-times-circle'
    };
    return icons[type] || 'fa-info-circle';
  }

  getActivityColor(type: string): string {
    const colors: any = {
      'LOGIN': 'primary',
      'LOGOUT': 'secondary',
      'CREATE': 'success',
      'UPDATE': 'info',
      'DELETE': 'danger',
      'APPROVE': 'success',
      'REJECT': 'danger'
    };
    return colors[type] || 'secondary';
  }

  exportToCsv(): void {
    const headers = ['Log ID', 'User', 'Role', 'Activity', 'Entity', 'Description', 'Status', 'Timestamp'];
    const rows = this.filteredActivities.map(log => [
      log.logId,
      log.performedBy,
      log.userRole,
      log.activityType,
      log.entityType || 'N/A',
      log.description,
      log.status,
      this.formatDate(log.timestamp)
    ]);
    
    let csvContent = headers.join(',') + '\n';
    rows.forEach(row => {
      csvContent += row.map(field => `"${field}"`).join(',') + '\n';
    });
    
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `activity-logs-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
