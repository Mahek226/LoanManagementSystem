import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, TemplateRef, ContentChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

export interface TableColumn {
  key: string;
  label: string;
  type: 'text' | 'number' | 'date' | 'currency' | 'status' | 'actions' | 'custom';
  sortable?: boolean;
  filterable?: boolean;
  width?: string;
  align?: 'left' | 'center' | 'right';
  format?: string; // For date/number formatting
  statusColors?: { [key: string]: string };
  customTemplate?: TemplateRef<any>;
}

export interface TableAction {
  id: string;
  label: string;
  icon: string;
  color?: string;
  condition?: (row: any) => boolean;
  action: (row: any) => void;
}

export interface TableConfig {
  columns: TableColumn[];
  actions?: TableAction[];
  selectable?: boolean;
  expandable?: boolean;
  exportable?: boolean;
  searchable?: boolean;
  filterable?: boolean;
  sortable?: boolean;
  pagination?: {
    enabled: boolean;
    pageSize: number;
    pageSizeOptions: number[];
  };
  styling?: {
    striped?: boolean;
    bordered?: boolean;
    hover?: boolean;
    compact?: boolean;
  };
}

export interface TableState {
  searchQuery: string;
  sortColumn: string;
  sortDirection: 'asc' | 'desc';
  filters: { [key: string]: any };
  currentPage: number;
  pageSize: number;
  selectedRows: any[];
  expandedRows: any[];
}

@Component({
  selector: 'app-enhanced-data-table',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './enhanced-data-table.component.html',
  styleUrls: ['./enhanced-data-table.component.css']
})
export class EnhancedDataTableComponent implements OnInit, OnDestroy {
  @Input() data: any[] = [];
  @Input() config: TableConfig = { columns: [] };
  @Input() loading = false;
  @Input() error: string | null = null;
  @Input() emptyMessage = 'No data available';
  @Input() rowIdField = 'id';
  @Input() autoRefresh = false;
  @Input() refreshInterval = 30000;

  @Output() rowClick = new EventEmitter<any>();
  @Output() rowSelect = new EventEmitter<any[]>();
  @Output() actionClick = new EventEmitter<{ action: string; row: any }>();
  @Output() sortChange = new EventEmitter<{ column: string; direction: 'asc' | 'desc' }>();
  @Output() filterChange = new EventEmitter<{ [key: string]: any }>();
  @Output() searchChange = new EventEmitter<string>();
  @Output() pageChange = new EventEmitter<{ page: number; pageSize: number }>();
  @Output() exportRequested = new EventEmitter<{ format: string; data: any[] }>();
  @Output() refreshRequested = new EventEmitter<void>();

  @ContentChild('customCell') customCellTemplate!: TemplateRef<any>;
  @ContentChild('expandedRow') expandedRowTemplate!: TemplateRef<any>;

  // Table state
  tableState: TableState = {
    searchQuery: '',
    sortColumn: '',
    sortDirection: 'asc',
    filters: {},
    currentPage: 1,
    pageSize: 10,
    selectedRows: [],
    expandedRows: []
  };

  // Processed data
  filteredData: any[] = [];
  paginatedData: any[] = [];
  totalRecords = 0;
  totalPages = 0;

  // UI state
  showFilters = false;
  selectAll = false;
  
  // Search debouncing
  private searchSubject = new Subject<string>();
  private subscriptions: Subscription[] = [];

  // Export formats
  exportFormats = [
    { value: 'csv', label: 'CSV', icon: 'fas fa-file-csv' },
    { value: 'excel', label: 'Excel', icon: 'fas fa-file-excel' },
    { value: 'pdf', label: 'PDF', icon: 'fas fa-file-pdf' }
  ];

  ngOnInit() {
    this.initializeTable();
    this.setupSearchDebounce();
    this.processData();

    if (this.autoRefresh) {
      this.startAutoRefresh();
    }
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private initializeTable() {
    // Set default pagination if enabled
    if (this.config.pagination?.enabled) {
      this.tableState.pageSize = this.config.pagination.pageSize || 10;
    }

    // Initialize filters for filterable columns
    this.config.columns.forEach(column => {
      if (column.filterable) {
        this.tableState.filters[column.key] = '';
      }
    });
  }

  private setupSearchDebounce() {
    const searchSub = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(query => {
      this.tableState.searchQuery = query;
      this.searchChange.emit(query);
      this.processData();
    });
    
    this.subscriptions.push(searchSub);
  }

  private startAutoRefresh() {
    const refreshSub = setInterval(() => {
      this.refreshRequested.emit();
    }, this.refreshInterval);

    this.subscriptions.push({
      unsubscribe: () => clearInterval(refreshSub)
    } as Subscription);
  }

  private processData() {
    let processedData = [...this.data];

    // Apply search
    if (this.tableState.searchQuery) {
      processedData = this.applySearch(processedData, this.tableState.searchQuery);
    }

    // Apply filters
    processedData = this.applyFilters(processedData, this.tableState.filters);

    // Apply sorting
    if (this.tableState.sortColumn) {
      processedData = this.applySorting(processedData, this.tableState.sortColumn, this.tableState.sortDirection);
    }

    this.filteredData = processedData;
    this.totalRecords = processedData.length;

    // Apply pagination
    if (this.config.pagination?.enabled) {
      this.totalPages = Math.ceil(this.totalRecords / this.tableState.pageSize);
      this.paginatedData = this.applyPagination(processedData, this.tableState.currentPage, this.tableState.pageSize);
    } else {
      this.paginatedData = processedData;
    }
  }

  private applySearch(data: any[], query: string): any[] {
    if (!query.trim()) return data;

    const searchTerm = query.toLowerCase();
    return data.filter(row => {
      return this.config.columns.some(column => {
        const value = this.getCellValue(row, column.key);
        return value && value.toString().toLowerCase().includes(searchTerm);
      });
    });
  }

  private applyFilters(data: any[], filters: { [key: string]: any }): any[] {
    return data.filter(row => {
      return Object.keys(filters).every(key => {
        const filterValue = filters[key];
        if (!filterValue) return true;

        const cellValue = this.getCellValue(row, key);
        
        if (typeof filterValue === 'string') {
          return cellValue && cellValue.toString().toLowerCase().includes(filterValue.toLowerCase());
        }
        
        return cellValue === filterValue;
      });
    });
  }

  private applySorting(data: any[], column: string, direction: 'asc' | 'desc'): any[] {
    return data.sort((a, b) => {
      const aValue = this.getCellValue(a, column);
      const bValue = this.getCellValue(b, column);

      let comparison = 0;
      
      if (aValue < bValue) comparison = -1;
      if (aValue > bValue) comparison = 1;

      return direction === 'desc' ? -comparison : comparison;
    });
  }

  private applyPagination(data: any[], page: number, pageSize: number): any[] {
    const startIndex = (page - 1) * pageSize;
    return data.slice(startIndex, startIndex + pageSize);
  }

  // Public methods
  onSearchInput(event: any) {
    this.searchSubject.next(event.target.value);
  }

  onSort(column: TableColumn) {
    if (!column.sortable) return;

    if (this.tableState.sortColumn === column.key) {
      this.tableState.sortDirection = this.tableState.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.tableState.sortColumn = column.key;
      this.tableState.sortDirection = 'asc';
    }

    this.sortChange.emit({ column: column.key, direction: this.tableState.sortDirection });
    this.processData();
  }

  onFilterChange(column: string, value: any) {
    this.tableState.filters[column] = value;
    this.tableState.currentPage = 1; // Reset to first page
    this.filterChange.emit(this.tableState.filters);
    this.processData();
  }

  onPageChange(page: number) {
    this.tableState.currentPage = page;
    this.pageChange.emit({ page, pageSize: this.tableState.pageSize });
    this.processData();
  }

  onPageSizeChange(pageSize: number) {
    this.tableState.pageSize = pageSize;
    this.tableState.currentPage = 1; // Reset to first page
    this.pageChange.emit({ page: 1, pageSize });
    this.processData();
  }

  onRowClick(row: any) {
    this.rowClick.emit(row);
  }

  onRowSelect(row: any, selected: boolean) {
    if (selected) {
      this.tableState.selectedRows.push(row);
    } else {
      const index = this.tableState.selectedRows.findIndex(r => 
        this.getCellValue(r, this.rowIdField) === this.getCellValue(row, this.rowIdField)
      );
      if (index > -1) {
        this.tableState.selectedRows.splice(index, 1);
      }
    }
    
    this.updateSelectAllState();
    this.rowSelect.emit(this.tableState.selectedRows);
  }

  onSelectAll(selected: boolean) {
    if (selected) {
      this.tableState.selectedRows = [...this.paginatedData];
    } else {
      this.tableState.selectedRows = [];
    }
    
    this.selectAll = selected;
    this.rowSelect.emit(this.tableState.selectedRows);
  }

  onRowExpand(row: any) {
    const rowId = this.getCellValue(row, this.rowIdField);
    const index = this.tableState.expandedRows.findIndex(r => 
      this.getCellValue(r, this.rowIdField) === rowId
    );
    
    if (index > -1) {
      this.tableState.expandedRows.splice(index, 1);
    } else {
      this.tableState.expandedRows.push(row);
    }
  }

  onActionClick(action: TableAction, row: any) {
    this.actionClick.emit({ action: action.id, row });
    action.action(row);
  }

  exportData(format: string) {
    this.exportRequested.emit({ format, data: this.filteredData });
  }

  refreshData() {
    this.refreshRequested.emit();
  }

  clearFilters() {
    this.tableState.filters = {};
    this.config.columns.forEach(column => {
      if (column.filterable) {
        this.tableState.filters[column.key] = '';
      }
    });
    this.tableState.searchQuery = '';
    this.tableState.currentPage = 1;
    
    // Clear search input
    const searchInput = document.querySelector('.table-search-input') as HTMLInputElement;
    if (searchInput) {
      searchInput.value = '';
    }
    
    this.processData();
  }

  // Utility methods
  getCellValue(row: any, key: string): any {
    return key.split('.').reduce((obj, prop) => obj?.[prop], row);
  }

  formatCellValue(row: any, column: TableColumn): string {
    const value = this.getCellValue(row, column.key);
    
    if (value === null || value === undefined) return '';

    switch (column.type) {
      case 'currency':
        return this.formatCurrency(value);
      case 'date':
        return this.formatDate(value, column.format);
      case 'number':
        return this.formatNumber(value, column.format);
      default:
        return value.toString();
    }
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }

  private formatDate(value: string | Date, format?: string): string {
    const date = new Date(value);
    if (isNaN(date.getTime())) return value.toString();

    if (format === 'relative') {
      return this.getRelativeTime(date);
    }

    return date.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  private formatNumber(value: number, format?: string): string {
    if (format === 'percentage') {
      return `${value}%`;
    }
    return value.toLocaleString('en-IN');
  }

  private getRelativeTime(date: Date): string {
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return date.toLocaleDateString();
  }

  private updateSelectAllState() {
    const selectedCount = this.tableState.selectedRows.length;
    const totalCount = this.paginatedData.length;
    
    this.selectAll = selectedCount > 0 && selectedCount === totalCount;
  }

  // Template helper methods
  isRowSelected(row: any): boolean {
    return this.tableState.selectedRows.some(r => 
      this.getCellValue(r, this.rowIdField) === this.getCellValue(row, this.rowIdField)
    );
  }

  isRowExpanded(row: any): boolean {
    return this.tableState.expandedRows.some(r => 
      this.getCellValue(r, this.rowIdField) === this.getCellValue(row, this.rowIdField)
    );
  }

  shouldShowAction(action: TableAction, row: any): boolean {
    return !action.condition || action.condition(row);
  }

  getSortIcon(column: TableColumn): string {
    if (!column.sortable) return '';
    if (this.tableState.sortColumn !== column.key) return 'fas fa-sort';
    return this.tableState.sortDirection === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  getStatusColor(value: string, column: TableColumn): string {
    return column.statusColors?.[value] || '#6b7280';
  }

  getPaginationPages(): number[] {
    const pages = [];
    const maxPages = 5;
    const current = this.tableState.currentPage;
    const total = this.totalPages;

    let start = Math.max(1, current - Math.floor(maxPages / 2));
    let end = Math.min(total, start + maxPages - 1);

    if (end - start + 1 < maxPages) {
      start = Math.max(1, end - maxPages + 1);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    return pages;
  }

  trackByRowId(index: number, row: any): any {
    return this.getCellValue(row, this.rowIdField);
  }

  trackByColumnKey(index: number, column: TableColumn): string {
    return column.key;
  }

  getColumnCount(): number {
    let count = this.config.columns.length;
    if (this.config.selectable) count++;
    if (this.config.expandable) count++;
    if (this.config.actions && this.config.actions.length > 0) count++;
    return count;
  }

  // Expose Math for template
  Math = Math;
}
