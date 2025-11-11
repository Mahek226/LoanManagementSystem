import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, FraudRule, FraudRuleRequest, FraudRuleUpdateRequest } from '../../../core/services/admin.service';

@Component({
  selector: 'app-fraud-rules',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fraud-rules.component.html',
  styleUrl: './fraud-rules.component.css'
})
export class FraudRulesComponent implements OnInit {
  rules: FraudRule[] = [];
  filteredRules: FraudRule[] = [];
  loading = false;
  error = '';
  success = '';
  
  showAddForm = false;
  editingRule: FraudRule | null = null;
  newRule: FraudRuleRequest = this.getEmptyRule();
  updateRule: FraudRuleUpdateRequest = {};
  
  searchQuery = '';
  selectedCategory = '';
  selectedSeverity = '';
  selectedStatus = '';
  
  categories = ['IDENTITY', 'FINANCIAL', 'EMPLOYMENT', 'CROSS_VERIFICATION'];
  severities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  ruleTypes = ['THRESHOLD', 'PATTERN_MATCH', 'DUPLICATE_CHECK', 'CROSS_CHECK'];

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadRules();
  }

  getEmptyRule(): FraudRuleRequest {
    return {
      ruleCode: '',
      ruleName: '',
      ruleDescription: '',
      ruleCategory: 'IDENTITY',
      severity: 'MEDIUM',
      fraudPoints: 10,
      isActive: true,
      ruleType: 'THRESHOLD',
      executionOrder: 100
    };
  }

  loadRules(): void {
    this.loading = true;
    this.error = '';
    
    this.adminService.getAllFraudRules().subscribe({
      next: (rules) => {
        this.rules = rules;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load fraud rules';
        console.error('Error loading rules:', err);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.rules];
    
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(rule => 
        rule.ruleName.toLowerCase().includes(query) ||
        rule.ruleCode.toLowerCase().includes(query) ||
        (rule.ruleDescription && rule.ruleDescription.toLowerCase().includes(query))
      );
    }
    
    if (this.selectedCategory) {
      filtered = filtered.filter(rule => rule.ruleCategory === this.selectedCategory);
    }
    
    if (this.selectedSeverity) {
      filtered = filtered.filter(rule => rule.severity === this.selectedSeverity);
    }
    
    if (this.selectedStatus === 'active') {
      filtered = filtered.filter(rule => rule.isActive);
    } else if (this.selectedStatus === 'inactive') {
      filtered = filtered.filter(rule => !rule.isActive);
    }
    
    this.filteredRules = filtered;
  }

  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    if (this.showAddForm) {
      this.newRule = this.getEmptyRule();
      this.editingRule = null;
      this.error = '';
      this.success = '';
    }
  }

  addRule(): void {
    if (!this.validateRule(this.newRule)) {
      return;
    }
    
    this.loading = true;
    this.error = '';
    
    this.adminService.createFraudRule(this.newRule).subscribe({
      next: (response) => {
        this.success = 'Fraud rule created successfully';
        this.showAddForm = false;
        this.loadRules();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to create rule';
        this.loading = false;
      }
    });
  }

  editRule(rule: FraudRule): void {
    this.editingRule = rule;
    this.updateRule = {
      ruleName: rule.ruleName,
      ruleDescription: rule.ruleDescription,
      ruleCategory: rule.ruleCategory,
      severity: rule.severity,
      fraudPoints: rule.fraudPoints,
      isActive: rule.isActive,
      ruleType: rule.ruleType,
      executionOrder: rule.executionOrder
    };
    this.showAddForm = false;
  }

  saveUpdate(): void {
    if (!this.editingRule) return;
    
    this.loading = true;
    this.error = '';
    
    this.adminService.updateFraudRule(this.editingRule.ruleId, this.updateRule).subscribe({
      next: (response) => {
        this.success = 'Fraud rule updated successfully';
        this.editingRule = null;
        this.loadRules();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to update rule';
        this.loading = false;
      }
    });
  }

  cancelEdit(): void {
    this.editingRule = null;
    this.updateRule = {};
  }

  toggleRule(rule: FraudRule): void {
    this.loading = true;
    this.error = '';
    
    this.adminService.toggleFraudRule(rule.ruleId).subscribe({
      next: (response) => {
        this.success = `Rule ${response.isActive ? 'activated' : 'deactivated'} successfully`;
        this.loadRules();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to toggle rule';
        this.loading = false;
      }
    });
  }

  deleteRule(rule: FraudRule): void {
    if (!confirm(`Are you sure you want to delete rule: ${rule.ruleName}?`)) {
      return;
    }
    
    this.loading = true;
    this.error = '';
    
    this.adminService.deleteFraudRule(rule.ruleId).subscribe({
      next: () => {
        this.success = 'Fraud rule deleted successfully';
        this.loadRules();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to delete rule';
        this.loading = false;
      }
    });
  }

  validateRule(rule: FraudRuleRequest): boolean {
    if (!rule.ruleCode || rule.ruleCode.length < 3) {
      this.error = 'Rule code must be at least 3 characters';
      return false;
    }
    
    if (!rule.ruleName || rule.ruleName.length < 3) {
      this.error = 'Rule name must be at least 3 characters';
      return false;
    }
    
    if (!rule.fraudPoints || rule.fraudPoints < 0) {
      this.error = 'Fraud points must be a positive number';
      return false;
    }
    
    return true;
  }

  getSeverityClass(severity: string): string {
    const classes: any = {
      'CRITICAL': 'severity-critical',
      'HIGH': 'severity-high',
      'MEDIUM': 'severity-medium',
      'LOW': 'severity-low'
    };
    return classes[severity] || 'severity-medium';
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString();
  }

  exportToCsv(): void {
    const headers = ['Rule ID', 'Code', 'Name', 'Category', 'Severity', 'Fraud Points', 'Active', 'Created At'];
    const rows = this.filteredRules.map(rule => [
      rule.ruleId,
      rule.ruleCode,
      rule.ruleName,
      rule.ruleCategory,
      rule.severity,
      rule.fraudPoints,
      rule.isActive ? 'Yes' : 'No',
      this.formatDate(rule.createdAt)
    ]);
    
    let csvContent = headers.join(',') + '\n';
    rows.forEach(row => {
      csvContent += row.map(field => `"${field}"`).join(',') + '\n';
    });
    
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `fraud-rules-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
