import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

export interface SearchFilter {
  key: string;
  label: string;
  type: 'text' | 'select' | 'date' | 'number' | 'range' | 'multiselect';
  options?: { value: any; label: string }[];
  placeholder?: string;
  icon?: string;
  validation?: {
    min?: number;
    max?: number;
    pattern?: string;
  };
}

export interface SearchCriteria {
  query: string;
  filters: { [key: string]: any };
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
  dateRange?: {
    start?: Date;
    end?: Date;
  };
}

@Component({
  selector: 'app-advanced-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './advanced-search.component.html',
  styleUrls: ['./advanced-search.component.css']
})
export class AdvancedSearchComponent implements OnInit, OnDestroy {
  @Input() placeholder = 'Search...';
  @Input() filters: SearchFilter[] = [];
  @Input() showSortOptions = true;
  @Input() showDateRange = true;
  @Input() showExportOptions = false;
  @Input() debounceTime = 300;
  @Input() context: 'applicants' | 'loans' | 'officers' | 'general' = 'general';
  
  @Output() searchChange = new EventEmitter<SearchCriteria>();
  @Output() exportRequested = new EventEmitter<{ format: string; criteria: SearchCriteria }>();
  @Output() filtersCleared = new EventEmitter<void>();

  // Search state
  searchQuery = '';
  activeFilters: { [key: string]: any } = {};
  sortBy = '';
  sortOrder: 'asc' | 'desc' = 'asc';
  dateRange = { start: '', end: '' };
  
  // UI state
  isExpanded = false;
  showAdvancedFilters = false;
  activeFilterCount = 0;
  
  // Search subject for debouncing
  private searchSubject = new Subject<string>();
  private subscriptions: Subscription[] = [];

  // Sort options based on context
  sortOptions: { value: string; label: string }[] = [];
  
  // Export formats
  exportFormats = [
    { value: 'csv', label: 'CSV', icon: 'fas fa-file-csv' },
    { value: 'excel', label: 'Excel', icon: 'fas fa-file-excel' },
    { value: 'pdf', label: 'PDF', icon: 'fas fa-file-pdf' }
  ];

  ngOnInit() {
    this.initializeSortOptions();
    this.setupSearchDebounce();
    this.loadSavedFilters();
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.saveFiltersToStorage();
  }

  private initializeSortOptions() {
    switch (this.context) {
      case 'applicants':
        this.sortOptions = [
          { value: 'name', label: 'Name' },
          { value: 'email', label: 'Email' },
          { value: 'appliedDate', label: 'Applied Date' },
          { value: 'status', label: 'Status' },
          { value: 'loanAmount', label: 'Loan Amount' }
        ];
        break;
      case 'loans':
        this.sortOptions = [
          { value: 'loanId', label: 'Loan ID' },
          { value: 'applicantName', label: 'Applicant Name' },
          { value: 'loanType', label: 'Loan Type' },
          { value: 'requestedAmount', label: 'Amount' },
          { value: 'appliedDate', label: 'Applied Date' },
          { value: 'status', label: 'Status' }
        ];
        break;
      case 'officers':
        this.sortOptions = [
          { value: 'name', label: 'Name' },
          { value: 'email', label: 'Email' },
          { value: 'assignedLoans', label: 'Assigned Loans' },
          { value: 'completedLoans', label: 'Completed Loans' },
          { value: 'joinDate', label: 'Join Date' }
        ];
        break;
      default:
        this.sortOptions = [
          { value: 'name', label: 'Name' },
          { value: 'date', label: 'Date' },
          { value: 'status', label: 'Status' }
        ];
    }
  }

  private setupSearchDebounce() {
    const searchSub = this.searchSubject.pipe(
      debounceTime(this.debounceTime),
      distinctUntilChanged()
    ).subscribe(query => {
      this.searchQuery = query;
      this.emitSearchChange();
    });
    
    this.subscriptions.push(searchSub);
  }

  private loadSavedFilters() {
    try {
      const saved = localStorage.getItem(`advanced_search_${this.context}`);
      if (saved) {
        const data = JSON.parse(saved);
        this.activeFilters = data.filters || {};
        this.sortBy = data.sortBy || '';
        this.sortOrder = data.sortOrder || 'asc';
        this.updateActiveFilterCount();
      }
    } catch (error) {
      console.error('Failed to load saved filters:', error);
    }
  }

  private saveFiltersToStorage() {
    try {
      const data = {
        filters: this.activeFilters,
        sortBy: this.sortBy,
        sortOrder: this.sortOrder
      };
      localStorage.setItem(`advanced_search_${this.context}`, JSON.stringify(data));
    } catch (error) {
      console.error('Failed to save filters:', error);
    }
  }

  private updateActiveFilterCount() {
    this.activeFilterCount = Object.keys(this.activeFilters).filter(key => {
      const value = this.activeFilters[key];
      return value !== null && value !== undefined && value !== '' && 
             (Array.isArray(value) ? value.length > 0 : true);
    }).length;
  }

  private emitSearchChange() {
    const criteria: SearchCriteria = {
      query: this.searchQuery,
      filters: { ...this.activeFilters },
      sortBy: this.sortBy,
      sortOrder: this.sortOrder
    };

    // Add date range if provided
    if (this.dateRange.start || this.dateRange.end) {
      criteria.dateRange = {
        start: this.dateRange.start ? new Date(this.dateRange.start) : undefined,
        end: this.dateRange.end ? new Date(this.dateRange.end) : undefined
      };
    }

    this.searchChange.emit(criteria);
  }

  // Public methods
  onSearchInput(event: any) {
    this.searchSubject.next(event.target.value);
  }

  onFilterChange(filterKey: string, value: any) {
    if (value === null || value === undefined || value === '' || 
        (Array.isArray(value) && value.length === 0)) {
      delete this.activeFilters[filterKey];
    } else {
      this.activeFilters[filterKey] = value;
    }
    
    this.updateActiveFilterCount();
    this.emitSearchChange();
  }

  onSortChange() {
    this.emitSearchChange();
  }

  onDateRangeChange() {
    this.emitSearchChange();
  }

  toggleAdvancedFilters() {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  clearAllFilters() {
    this.searchQuery = '';
    this.activeFilters = {};
    this.dateRange = { start: '', end: '' };
    this.sortBy = '';
    this.sortOrder = 'asc';
    this.activeFilterCount = 0;
    
    // Clear search input
    const searchInput = document.querySelector('.search-input') as HTMLInputElement;
    if (searchInput) {
      searchInput.value = '';
    }
    
    this.filtersCleared.emit();
    this.emitSearchChange();
  }

  exportData(format: string) {
    const criteria: SearchCriteria = {
      query: this.searchQuery,
      filters: { ...this.activeFilters },
      sortBy: this.sortBy,
      sortOrder: this.sortOrder
    };

    if (this.dateRange.start || this.dateRange.end) {
      criteria.dateRange = {
        start: this.dateRange.start ? new Date(this.dateRange.start) : undefined,
        end: this.dateRange.end ? new Date(this.dateRange.end) : undefined
      };
    }

    this.exportRequested.emit({ format, criteria });
  }

  getFilterValue(filter: SearchFilter): any {
    return this.activeFilters[filter.key] || (filter.type === 'multiselect' ? [] : '');
  }

  isFilterActive(filterKey: string): boolean {
    const value = this.activeFilters[filterKey];
    return value !== null && value !== undefined && value !== '' && 
           (Array.isArray(value) ? value.length > 0 : true);
  }

  getActiveFilterLabel(filterKey: string): string {
    const filter = this.filters.find(f => f.key === filterKey);
    const value = this.activeFilters[filterKey];
    
    if (!filter || !value) return '';
    
    if (filter.type === 'select' && filter.options) {
      const option = filter.options.find(opt => opt.value === value);
      return option ? option.label : value;
    }
    
    if (filter.type === 'multiselect' && filter.options && Array.isArray(value)) {
      return value.map(v => {
        const option = filter.options!.find(opt => opt.value === v);
        return option ? option.label : v;
      }).join(', ');
    }
    
    return value.toString();
  }

  removeFilter(filterKey: string) {
    delete this.activeFilters[filterKey];
    this.updateActiveFilterCount();
    this.emitSearchChange();
  }

  // Utility methods for templates
  trackByFilterKey(index: number, filter: SearchFilter): string {
    return filter.key;
  }

  trackByOptionValue(index: number, option: { value: any; label: string }): any {
    return option.value;
  }

  isMultiselectOptionSelected(filter: SearchFilter, optionValue: any): boolean {
    const selectedValues = this.activeFilters[filter.key] || [];
    return Array.isArray(selectedValues) && selectedValues.includes(optionValue);
  }

  toggleMultiselectOption(filter: SearchFilter, optionValue: any) {
    let selectedValues = this.activeFilters[filter.key] || [];
    
    if (!Array.isArray(selectedValues)) {
      selectedValues = [];
    }
    
    const index = selectedValues.indexOf(optionValue);
    if (index > -1) {
      selectedValues.splice(index, 1);
    } else {
      selectedValues.push(optionValue);
    }
    
    this.onFilterChange(filter.key, selectedValues);
  }
}
