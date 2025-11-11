import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KpiCardComponent } from '../shared/components/kpi-card/kpi-card.component';
import { ProfessionalTableComponent, TableColumn, TableAction } from '../shared/components/professional-table/professional-table.component';
import { ProfessionalHeaderComponent } from '../shared/components/professional-header/professional-header.component';

@Component({
  selector: 'app-professional-demo',
  standalone: true,
  imports: [CommonModule, KpiCardComponent, ProfessionalTableComponent, ProfessionalHeaderComponent],
  template: `
    <div class="demo-container">
      <!-- Professional Header -->
      <app-professional-header
        [showSearch]="true"
        [showNotifications]="true">
      </app-professional-header>

      <div class="container-fluid p-4">
        <!-- Page Title -->
        <div class="mb-4">
          <h1 class="heading-xl gradient-text mb-2">Professional Components Demo</h1>
          <p class="text-body text-muted">Showcase of the new professional design system components</p>
        </div>

        <!-- KPI Cards Section -->
        <div class="mb-6">
          <h2 class="heading-lg mb-4">KPI Cards</h2>
          <div class="row g-4">
            <div class="col-xl-3 col-md-6">
              <app-kpi-card
                label="Total Loans"
                [value]="2500000"
                prefix="₹"
                icon="fas fa-chart-line"
                color="primary"
                [trend]="{value: 12.5, direction: 'up', period: 'vs last month'}"
                description="Total loan amount processed"
                [showProgress]="true"
                [progressCurrent]="75"
                [progressTarget]="100"
                animationDelay="0.1s">
              </app-kpi-card>
            </div>

            <div class="col-xl-3 col-md-6">
              <app-kpi-card
                label="Applications"
                [value]="1250"
                icon="fas fa-file-contract"
                color="success"
                [trend]="{value: 8.3, direction: 'up', period: 'vs last month'}"
                description="New applications this month"
                [showMenu]="true"
                animationDelay="0.2s">
              </app-kpi-card>
            </div>

            <div class="col-xl-3 col-md-6">
              <app-kpi-card
                label="Approval Rate"
                [value]="87.5"
                suffix="%"
                icon="fas fa-check-circle"
                color="warning"
                [trend]="{value: 2.1, direction: 'down', period: 'vs last month'}"
                description="Current approval percentage"
                animationDelay="0.3s">
              </app-kpi-card>
            </div>

            <div class="col-xl-3 col-md-6">
              <app-kpi-card
                label="Risk Score"
                [value]="42"
                icon="fas fa-shield-alt"
                color="danger"
                [trend]="{value: 5.2, direction: 'up', period: 'vs last month'}"
                description="Average risk assessment"
                [loading]="false"
                animationDelay="0.4s">
              </app-kpi-card>
            </div>
          </div>
        </div>

        <!-- Professional Table Section -->
        <div class="mb-6">
          <h2 class="heading-lg mb-4">Professional Data Table</h2>
          <app-professional-table
            title="Loan Applications"
            subtitle="Manage and review all loan applications"
            [columns]="tableColumns"
            [data]="tableData"
            [actions]="tableActions"
            [rowActions]="rowActions"
            [loading]="tableLoading"
            [showSearch]="true"
            [showFilters]="true"
            [showPagination]="true"
            searchPlaceholder="Search applications..."
            emptyTitle="No Applications Found"
            emptyMessage="There are no loan applications to display."
            emptyAction="New Application"
            (search)="onTableSearch($event)"
            (sort)="onTableSort($event)"
            (rowClick)="onRowClick($event)"
            (rowActionClick)="onRowAction($event)"
            (actionClick)="onTableAction($event)">
          </app-professional-table>
        </div>

        <!-- Design System Elements -->
        <div class="mb-6">
          <h2 class="heading-lg mb-4">Design System Elements</h2>
          
          <!-- Buttons -->
          <div class="mb-4">
            <h3 class="heading-md mb-3">Buttons</h3>
            <div class="d-flex gap-3 flex-wrap">
              <button class="btn btn-primary">Primary Button</button>
              <button class="btn btn-secondary">Secondary Button</button>
              <button class="btn btn-success">Success Button</button>
              <button class="btn btn-danger">Danger Button</button>
              <button class="btn btn-warning">Warning Button</button>
              <button class="btn btn-outline">Outline Button</button>
              <button class="btn btn-ghost">Ghost Button</button>
            </div>
          </div>

          <!-- Badges -->
          <div class="mb-4">
            <h3 class="heading-md mb-3">Status Badges</h3>
            <div class="d-flex gap-3 flex-wrap">
              <span class="badge badge-primary">Primary</span>
              <span class="badge badge-success">Approved</span>
              <span class="badge badge-warning">Pending</span>
              <span class="badge badge-danger">Rejected</span>
              <span class="badge badge-info">In Review</span>
              <span class="badge badge-gradient">Premium</span>
            </div>
          </div>

          <!-- Cards -->
          <div class="mb-4">
            <h3 class="heading-md mb-3">Professional Cards</h3>
            <div class="row g-4">
              <div class="col-md-6">
                <div class="card hover-lift">
                  <div class="card-header">
                    <h4 class="heading-sm mb-0">Standard Card</h4>
                  </div>
                  <div class="card-body">
                    <p class="text-body mb-3">This is a standard professional card with hover effects and proper spacing.</p>
                    <button class="btn btn-primary btn-sm">Action</button>
                  </div>
                </div>
              </div>
              <div class="col-md-6">
                <div class="card glass-effect hover-lift">
                  <div class="card-header">
                    <h4 class="heading-sm mb-0">Glass Effect Card</h4>
                  </div>
                  <div class="card-body">
                    <p class="text-body mb-3">This card uses the glass effect for a modern, translucent appearance.</p>
                    <button class="btn btn-outline btn-sm">Action</button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Typography -->
          <div class="mb-4">
            <h3 class="heading-md mb-3">Typography Scale</h3>
            <div class="typography-demo">
              <h1 class="heading-xl">Heading XL - Main Page Titles</h1>
              <h2 class="heading-lg">Heading LG - Section Titles</h2>
              <h3 class="heading-md">Heading MD - Subsection Titles</h3>
              <h4 class="heading-sm">Heading SM - Card Titles</h4>
              <p class="text-body">Body Text - Regular paragraph content with proper line height and spacing.</p>
              <p class="text-small">Small Text - Secondary information and captions.</p>
              <p class="text-caption">CAPTION TEXT - Labels and metadata.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .demo-container {
      min-height: 100vh;
      background: var(--gray-50);
    }

    .typography-demo > * {
      margin-bottom: var(--space-3);
    }

    .mb-6 {
      margin-bottom: var(--space-16);
    }
  `]
})
export class ProfessionalComponentsDemoComponent {
  tableLoading = false;
  
  tableColumns: TableColumn[] = [
    { key: 'id', label: 'Application ID', sortable: true, type: 'text' },
    { key: 'applicantName', label: 'Applicant', sortable: true, type: 'text' },
    { key: 'loanAmount', label: 'Amount', sortable: true, type: 'currency' },
    { key: 'loanType', label: 'Type', type: 'text' },
    { key: 'status', label: 'Status', type: 'badge' },
    { key: 'submittedAt', label: 'Submitted', sortable: true, type: 'date' },
    { key: 'actions', label: 'Actions', type: 'actions', width: '120px' }
  ];

  tableData = [
    {
      id: 'LA001',
      applicantName: 'John Doe',
      loanAmount: 500000,
      loanType: 'Home Loan',
      status: 'PENDING',
      submittedAt: '2024-11-01T10:30:00Z'
    },
    {
      id: 'LA002',
      applicantName: 'Jane Smith',
      loanAmount: 250000,
      loanType: 'Personal Loan',
      status: 'APPROVED',
      submittedAt: '2024-11-02T14:15:00Z'
    },
    {
      id: 'LA003',
      applicantName: 'Mike Johnson',
      loanAmount: 750000,
      loanType: 'Business Loan',
      status: 'REJECTED',
      submittedAt: '2024-11-03T09:45:00Z'
    }
  ];

  tableActions: TableAction[] = [
    { label: 'New Application', icon: 'fas fa-plus', color: 'primary', action: 'new' },
    { label: 'Export CSV', icon: 'fas fa-download', color: 'success', action: 'export' }
  ];

  rowActions: TableAction[] = [
    { label: 'View', icon: 'fas fa-eye', color: 'info', action: 'view' },
    { label: 'Edit', icon: 'fas fa-edit', color: 'warning', action: 'edit' },
    { label: 'Delete', icon: 'fas fa-trash', color: 'danger', action: 'delete' }
  ];

  onTableSearch(query: string): void {
    console.log('Search:', query);
  }

  onTableSort(event: {column: string, direction: 'asc' | 'desc'}): void {
    console.log('Sort:', event);
  }

  onRowClick(row: any): void {
    console.log('Row clicked:', row);
  }

  onRowAction(event: {action: string, row: any}): void {
    console.log('Row action:', event);
  }

  onTableAction(action: string): void {
    console.log('Table action:', action);
  }
}
