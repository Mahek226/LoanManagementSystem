import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface TableColumn {
  key: string;
  label: string;
  sortable?: boolean;
  type?: 'text' | 'number' | 'date' | 'currency' | 'badge' | 'actions';
  width?: string;
  align?: 'left' | 'center' | 'right';
}

export interface TableAction {
  label: string;
  icon: string;
  color: 'primary' | 'success' | 'danger' | 'warning' | 'info';
  action: string;
}

@Component({
  selector: 'app-professional-table',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="professional-table-container">
      <!-- Table Header -->
      <div class="table-header" *ngIf="title || showSearch || showFilters || actions.length > 0">
        <div class="table-title-section">
          <h3 class="table-title" *ngIf="title">{{ title }}</h3>
          <p class="table-subtitle" *ngIf="subtitle">{{ subtitle }}</p>
        </div>
        
        <div class="table-controls">
          <!-- Search -->
          <div class="search-wrapper" *ngIf="showSearch">
            <i class="fas fa-search search-icon"></i>
            <input 
              type="text" 
              class="search-input" 
              [placeholder]="searchPlaceholder"
              [(ngModel)]="searchQuery"
              (input)="onSearch()">
          </div>
          
          <!-- Filters -->
          <div class="filters-wrapper" *ngIf="showFilters">
            <button class="filter-btn" (click)="toggleFilters()">
              <i class="fas fa-filter"></i>
              <span>Filters</span>
              <span class="filter-count" *ngIf="activeFiltersCount > 0">{{ activeFiltersCount }}</span>
            </button>
          </div>
          
          <!-- Actions -->
          <div class="actions-wrapper" *ngIf="actions.length > 0">
            <button 
              *ngFor="let action of actions"
              class="action-btn"
              [ngClass]="'btn-' + action.color"
              (click)="onAction(action.action)">
              <i [class]="action.icon"></i>
              <span>{{ action.label }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Filters Panel -->
      <div class="filters-panel" *ngIf="showFilters && filtersVisible">
        <div class="filters-content">
          <ng-content select="[slot=filters]"></ng-content>
        </div>
        <div class="filters-actions">
          <button class="btn btn-outline btn-sm" (click)="clearFilters()">
            Clear All
          </button>
          <button class="btn btn-primary btn-sm" (click)="applyFilters()">
            Apply Filters
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading-state" *ngIf="loading">
        <div class="loading-spinner">
          <i class="fas fa-spinner fa-spin"></i>
        </div>
        <p>Loading data...</p>
      </div>

      <!-- Table -->
      <div class="table-wrapper" *ngIf="!loading">
        <table class="professional-table">
          <thead>
            <tr>
              <th 
                *ngFor="let column of columns" 
                [style.width]="column.width"
                [class]="'text-' + (column.align || 'left')"
                [class.sortable]="column.sortable"
                (click)="onSort(column)">
                <div class="th-content">
                  <span>{{ column.label }}</span>
                  <i 
                    *ngIf="column.sortable"
                    class="sort-icon"
                    [class]="getSortIcon(column.key)"></i>
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr 
              *ngFor="let row of paginatedData; let i = index; trackBy: trackByFn"
              class="table-row"
              [class.selected]="isSelected(row)"
              (click)="onRowClick(row)">
              <td 
                *ngFor="let column of columns"
                [class]="'text-' + (column.align || 'left')">
                <div class="cell-content" [ngSwitch]="column.type">
                  <!-- Text -->
                  <span *ngSwitchCase="'text'">{{ getValue(row, column.key) }}</span>
                  
                  <!-- Number -->
                  <span *ngSwitchCase="'number'" class="number-cell">
                    {{ getValue(row, column.key) | number }}
                  </span>
                  
                  <!-- Currency -->
                  <span *ngSwitchCase="'currency'" class="currency-cell">
                    {{ formatCurrency(getValue(row, column.key)) }}
                  </span>
                  
                  <!-- Date -->
                  <span *ngSwitchCase="'date'" class="date-cell">
                    {{ formatDate(getValue(row, column.key)) }}
                  </span>
                  
                  <!-- Badge -->
                  <span 
                    *ngSwitchCase="'badge'" 
                    class="badge"
                    [ngClass]="getBadgeClass(getValue(row, column.key))">
                    {{ getValue(row, column.key) }}
                  </span>
                  
                  <!-- Actions -->
                  <div *ngSwitchCase="'actions'" class="actions-cell">
                    <button 
                      *ngFor="let action of rowActions"
                      class="action-btn-sm"
                      [ngClass]="'btn-' + action.color"
                      [title]="action.label"
                      (click)="onRowAction(action.action, row, $event)">
                      <i [class]="action.icon"></i>
                    </button>
                  </div>
                  
                  <!-- Default -->
                  <span *ngSwitchDefault>{{ getValue(row, column.key) }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        
        <!-- Empty State -->
        <div class="empty-state" *ngIf="data.length === 0">
          <div class="empty-icon">
            <i [class]="emptyIcon"></i>
          </div>
          <h3>{{ emptyTitle }}</h3>
          <p>{{ emptyMessage }}</p>
          <button 
            *ngIf="emptyAction"
            class="btn btn-primary"
            (click)="onEmptyAction()">
            <i [class]="emptyActionIcon"></i>
            <span>{{ emptyAction }}</span>
          </button>
        </div>
      </div>

      <!-- Pagination -->
      <div class="pagination-wrapper" *ngIf="showPagination && data.length > 0">
        <div class="pagination-info">
          <span>Showing {{ startIndex + 1 }} to {{ endIndex }} of {{ totalItems }} entries</span>
        </div>
        
        <div class="pagination-controls">
          <button 
            class="pagination-btn"
            [disabled]="currentPage === 1"
            (click)="goToPage(currentPage - 1)">
            <i class="fas fa-chevron-left"></i>
          </button>
          
          <button 
            *ngFor="let page of visiblePages"
            class="pagination-btn"
            [class.active]="page === currentPage"
            (click)="goToPage(page)">
            {{ page }}
          </button>
          
          <button 
            class="pagination-btn"
            [disabled]="currentPage === totalPages"
            (click)="goToPage(currentPage + 1)">
            <i class="fas fa-chevron-right"></i>
          </button>
        </div>
        
        <div class="page-size-selector">
          <select [(ngModel)]="pageSize" (change)="onPageSizeChange()">
            <option value="10">10 per page</option>
            <option value="25">25 per page</option>
            <option value="50">50 per page</option>
            <option value="100">100 per page</option>
          </select>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .professional-table-container {
      background: var(--white);
      border-radius: var(--radius-xl);
      box-shadow: var(--shadow-md);
      overflow: hidden;
    }

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--space-6);
      border-bottom: 1px solid var(--gray-100);
      background: var(--gray-50);
    }

    .table-title {
      font-size: var(--text-xl);
      font-weight: var(--font-semibold);
      color: var(--gray-800);
      margin: 0;
    }

    .table-subtitle {
      font-size: var(--text-sm);
      color: var(--gray-500);
      margin: var(--space-1) 0 0 0;
    }

    .table-controls {
      display: flex;
      align-items: center;
      gap: var(--space-3);
    }

    .search-wrapper {
      position: relative;
    }

    .search-input {
      padding: var(--space-2) var(--space-3) var(--space-2) var(--space-8);
      border: 2px solid var(--gray-200);
      border-radius: var(--radius-lg);
      font-size: var(--text-sm);
      min-width: 250px;
      transition: all var(--transition-normal);
    }

    .search-input:focus {
      outline: none;
      border-color: var(--primary-blue);
      box-shadow: 0 0 0 3px rgba(30, 64, 175, 0.1);
    }

    .search-icon {
      position: absolute;
      left: var(--space-3);
      top: 50%;
      transform: translateY(-50%);
      color: var(--gray-400);
      font-size: var(--text-sm);
    }

    .filter-btn {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      padding: var(--space-2) var(--space-4);
      border: 2px solid var(--gray-200);
      border-radius: var(--radius-lg);
      background: var(--white);
      color: var(--gray-600);
      font-size: var(--text-sm);
      cursor: pointer;
      transition: all var(--transition-normal);
    }

    .filter-btn:hover {
      border-color: var(--primary-blue);
      color: var(--primary-blue);
    }

    .filter-count {
      background: var(--primary-blue);
      color: var(--white);
      font-size: var(--text-xs);
      padding: 2px 6px;
      border-radius: var(--radius-full);
      min-width: 18px;
      text-align: center;
    }

    .action-btn {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      padding: var(--space-2) var(--space-4);
      border: none;
      border-radius: var(--radius-lg);
      font-size: var(--text-sm);
      font-weight: var(--font-medium);
      cursor: pointer;
      transition: all var(--transition-normal);
    }

    .btn-primary {
      background: var(--gradient-primary);
      color: var(--white);
    }

    .btn-success {
      background: var(--gradient-success);
      color: var(--white);
    }

    .btn-danger {
      background: var(--gradient-danger);
      color: var(--white);
    }

    .filters-panel {
      padding: var(--space-4);
      border-bottom: 1px solid var(--gray-100);
      background: var(--gray-50);
    }

    .filters-actions {
      display: flex;
      justify-content: flex-end;
      gap: var(--space-2);
      margin-top: var(--space-4);
    }

    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--space-12);
      color: var(--gray-500);
    }

    .loading-spinner {
      font-size: 2rem;
      margin-bottom: var(--space-4);
      color: var(--primary-blue);
    }

    .table-wrapper {
      overflow-x: auto;
    }

    .professional-table {
      width: 100%;
      border-collapse: collapse;
    }

    .professional-table th {
      padding: var(--space-4);
      background: var(--gray-50);
      border-bottom: 1px solid var(--gray-200);
      font-size: var(--text-sm);
      font-weight: var(--font-semibold);
      color: var(--gray-700);
    }

    .professional-table th.sortable {
      cursor: pointer;
      user-select: none;
    }

    .professional-table th.sortable:hover {
      background: var(--gray-100);
    }

    .th-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .sort-icon {
      font-size: var(--text-xs);
      color: var(--gray-400);
      margin-left: var(--space-2);
    }

    .professional-table td {
      padding: var(--space-4);
      border-bottom: 1px solid var(--gray-100);
      font-size: var(--text-sm);
    }

    .table-row {
      transition: background-color var(--transition-fast);
      cursor: pointer;
    }

    .table-row:hover {
      background: var(--gray-50);
    }

    .table-row.selected {
      background: rgba(30, 64, 175, 0.05);
    }

    .badge {
      display: inline-flex;
      align-items: center;
      padding: var(--space-1) var(--space-3);
      border-radius: var(--radius-full);
      font-size: var(--text-xs);
      font-weight: var(--font-semibold);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .actions-cell {
      display: flex;
      gap: var(--space-1);
    }

    .action-btn-sm {
      padding: var(--space-1);
      border: none;
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all var(--transition-normal);
      font-size: var(--text-sm);
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--space-16);
      text-align: center;
    }

    .empty-icon {
      font-size: 3rem;
      color: var(--gray-300);
      margin-bottom: var(--space-4);
    }

    .empty-state h3 {
      font-size: var(--text-xl);
      font-weight: var(--font-semibold);
      color: var(--gray-700);
      margin-bottom: var(--space-2);
    }

    .empty-state p {
      color: var(--gray-500);
      margin-bottom: var(--space-6);
    }

    .pagination-wrapper {
      display: flex;
      justify-content: between;
      align-items: center;
      padding: var(--space-4) var(--space-6);
      border-top: 1px solid var(--gray-100);
      background: var(--gray-50);
    }

    .pagination-controls {
      display: flex;
      gap: var(--space-1);
    }

    .pagination-btn {
      padding: var(--space-2) var(--space-3);
      border: 1px solid var(--gray-200);
      background: var(--white);
      color: var(--gray-600);
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all var(--transition-normal);
    }

    .pagination-btn:hover:not(:disabled) {
      background: var(--gray-50);
      border-color: var(--gray-300);
    }

    .pagination-btn.active {
      background: var(--primary-blue);
      color: var(--white);
      border-color: var(--primary-blue);
    }

    .pagination-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    @media (max-width: 768px) {
      .table-header {
        flex-direction: column;
        gap: var(--space-4);
        align-items: stretch;
      }

      .table-controls {
        flex-wrap: wrap;
      }

      .search-input {
        min-width: 200px;
      }

      .pagination-wrapper {
        flex-direction: column;
        gap: var(--space-3);
      }
    }
  `]
})
export class ProfessionalTableComponent implements OnInit {
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() columns: TableColumn[] = [];
  @Input() data: any[] = [];
  @Input() actions: TableAction[] = [];
  @Input() rowActions: TableAction[] = [];
  @Input() loading = false;
  @Input() showSearch = true;
  @Input() showFilters = false;
  @Input() showPagination = true;
  @Input() pageSize = 25;
  @Input() searchPlaceholder = 'Search...';
  @Input() emptyTitle = 'No Data Found';
  @Input() emptyMessage = 'There are no items to display.';
  @Input() emptyIcon = 'fas fa-inbox';
  @Input() emptyAction?: string;
  @Input() emptyActionIcon = 'fas fa-plus';

  @Output() search = new EventEmitter<string>();
  @Output() sort = new EventEmitter<{column: string, direction: 'asc' | 'desc'}>();
  @Output() rowClick = new EventEmitter<any>();
  @Output() rowActionClick = new EventEmitter<{action: string, row: any}>();
  @Output() actionClick = new EventEmitter<string>();
  @Output() filtersChange = new EventEmitter<any>();

  searchQuery = '';
  sortColumn = '';
  sortDirection: 'asc' | 'desc' = 'asc';
  currentPage = 1;
  filtersVisible = false;
  activeFiltersCount = 0;
  selectedRows: any[] = [];

  get paginatedData(): any[] {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    return this.data.slice(start, end);
  }

  get totalPages(): number {
    return Math.ceil(this.data.length / this.pageSize);
  }

  get totalItems(): number {
    return this.data.length;
  }

  get startIndex(): number {
    return (this.currentPage - 1) * this.pageSize;
  }

  get endIndex(): number {
    return Math.min(this.startIndex + this.pageSize, this.totalItems);
  }

  get visiblePages(): number[] {
    const pages = [];
    const start = Math.max(1, this.currentPage - 2);
    const end = Math.min(this.totalPages, this.currentPage + 2);
    
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    
    return pages;
  }

  ngOnInit(): void {
    // Component initialization
  }

  onSearch(): void {
    this.search.emit(this.searchQuery);
  }

  onSort(column: TableColumn): void {
    if (!column.sortable) return;
    
    if (this.sortColumn === column.key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column.key;
      this.sortDirection = 'asc';
    }
    
    this.sort.emit({column: column.key, direction: this.sortDirection});
  }

  getSortIcon(columnKey: string): string {
    if (this.sortColumn !== columnKey) return 'fas fa-sort';
    return this.sortDirection === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  onRowClick(row: any): void {
    this.rowClick.emit(row);
  }

  onRowAction(action: string, row: any, event: Event): void {
    event.stopPropagation();
    this.rowActionClick.emit({action, row});
  }

  onAction(action: string): void {
    this.actionClick.emit(action);
  }

  onEmptyAction(): void {
    if (this.emptyAction) {
      this.actionClick.emit('empty-action');
    }
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  clearFilters(): void {
    this.activeFiltersCount = 0;
    this.filtersChange.emit({});
  }

  applyFilters(): void {
    // Emit filter changes
    this.filtersChange.emit({});
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  onPageSizeChange(): void {
    this.currentPage = 1;
  }

  isSelected(row: any): boolean {
    return this.selectedRows.includes(row);
  }

  getValue(row: any, key: string): any {
    return key.split('.').reduce((obj, k) => obj?.[k], row);
  }

  formatCurrency(value: number): string {
    if (value == null) return '';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR'
    }).format(value);
  }

  formatDate(value: string | Date): string {
    if (!value) return '';
    const date = new Date(value);
    return date.toLocaleDateString('en-IN');
  }

  getBadgeClass(value: string): string {
    const statusClasses: {[key: string]: string} = {
      'APPROVED': 'badge-success',
      'REJECTED': 'badge-danger',
      'PENDING': 'badge-warning',
      'IN_PROGRESS': 'badge-info',
      'ESCALATED': 'badge-warning',
      'HIGH': 'badge-danger',
      'MEDIUM': 'badge-warning',
      'LOW': 'badge-success'
    };
    
    return statusClasses[value?.toUpperCase()] || 'badge-secondary';
  }

  trackByFn(index: number, item: any): any {
    return item.id || index;
  }
}
