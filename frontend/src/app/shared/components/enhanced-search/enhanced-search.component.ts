import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';
import { Subscription } from 'rxjs';

export interface SearchResult {
  id: string;
  title: string;
  subtitle?: string;
  type: string;
  icon?: string;
  url?: string;
  data?: any;
}

export interface SearchFilter {
  key: string;
  label: string;
  options: { value: string; label: string; count?: number }[];
}

@Component({
  selector: 'app-enhanced-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="enhanced-search" [class.expanded]="isExpanded">
      
      <!-- Search Input -->
      <div class="search-input-container">
        <div class="search-input-wrapper">
          <div class="search-icon">
            <i class="fas fa-search"></i>
          </div>
          
          <input
            type="text"
            [formControl]="searchControl"
            class="search-input"
            [placeholder]="placeholder"
            (focus)="onFocus()"
            (blur)="onBlur()"
            (keydown)="onKeyDown($event)"
          />
          
          <div class="search-actions">
            <button
              *ngIf="searchControl.value"
              type="button"
              class="clear-btn"
              (click)="clearSearch()"
              title="Clear search"
            >
              <i class="fas fa-times"></i>
            </button>
            
            <button
              *ngIf="showFilters && filters.length > 0"
              type="button"
              class="filter-btn"
              [class.active]="showFilterPanel"
              (click)="toggleFilters()"
              title="Filters"
            >
              <i class="fas fa-filter"></i>
              <span *ngIf="activeFiltersCount > 0" class="filter-count">{{ activeFiltersCount }}</span>
            </button>
          </div>
        </div>
        
        <!-- Loading Indicator -->
        <div *ngIf="loading" class="search-loading">
          <div class="loading-spinner"></div>
        </div>
      </div>

      <!-- Filter Panel -->
      <div *ngIf="showFilterPanel && filters.length > 0" class="filter-panel slide-in-up">
        <div class="filter-header">
          <h6>Filters</h6>
          <button 
            *ngIf="activeFiltersCount > 0" 
            class="clear-filters-btn"
            (click)="clearAllFilters()"
          >
            Clear All
          </button>
        </div>
        
        <div class="filter-groups">
          <div *ngFor="let filter of filters" class="filter-group">
            <label class="filter-label">{{ filter.label }}</label>
            <div class="filter-options">
              <label 
                *ngFor="let option of filter.options" 
                class="filter-option"
                [class.active]="isFilterActive(filter.key, option.value)"
              >
                <input
                  type="checkbox"
                  [checked]="isFilterActive(filter.key, option.value)"
                  (change)="toggleFilter(filter.key, option.value)"
                />
                <span class="option-label">{{ option.label }}</span>
                <span *ngIf="option.count !== undefined" class="option-count">({{ option.count }})</span>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- Search Results -->
      <div *ngIf="showResults && (results.length > 0 || noResults)" class="search-results slide-in-up">
        
        <!-- Results Header -->
        <div *ngIf="results.length > 0" class="results-header">
          <span class="results-count">{{ results.length }} result{{ results.length !== 1 ? 's' : '' }}</span>
          <button 
            *ngIf="showViewAll && results.length >= maxResults"
            class="view-all-btn"
            (click)="viewAllResults()"
          >
            View All
          </button>
        </div>

        <!-- Results List -->
        <div class="results-list">
          <div
            *ngFor="let result of results; let i = index"
            class="result-item"
            [class.highlighted]="i === highlightedIndex"
            (click)="selectResult(result)"
            (mouseenter)="highlightedIndex = i"
          >
            <div class="result-icon" *ngIf="result.icon">
              <i class="fas" [ngClass]="result.icon"></i>
            </div>
            
            <div class="result-content">
              <div class="result-title" [innerHTML]="highlightSearchTerm(result.title)"></div>
              <div *ngIf="result.subtitle" class="result-subtitle" [innerHTML]="highlightSearchTerm(result.subtitle)"></div>
            </div>
            
            <div class="result-type">
              <span class="type-badge">{{ result.type }}</span>
            </div>
          </div>
        </div>

        <!-- No Results -->
        <div *ngIf="noResults" class="no-results">
          <div class="no-results-icon">
            <i class="fas fa-search"></i>
          </div>
          <p class="no-results-text">No results found for "{{ searchControl.value }}"</p>
          <p class="no-results-suggestion">Try adjusting your search terms or filters</p>
        </div>

        <!-- Quick Actions -->
        <div *ngIf="quickActions.length > 0" class="quick-actions">
          <div class="quick-actions-header">Quick Actions</div>
          <div
            *ngFor="let action of quickActions"
            class="quick-action-item"
            (click)="executeQuickAction(action)"
          >
            <i class="fas" [ngClass]="action.icon"></i>
            <span>{{ action.label }}</span>
          </div>
        </div>
      </div>

      <!-- Search Overlay -->
      <div 
        *ngIf="showResults" 
        class="search-overlay"
        (click)="closeSearch()"
      ></div>
    </div>
  `,
  styles: [`
    .enhanced-search {
      position: relative;
      width: 100%;
      max-width: 600px;
    }

    .search-input-container {
      position: relative;
    }

    .search-input-wrapper {
      display: flex;
      align-items: center;
      background: var(--bg-primary);
      border: 2px solid var(--border-color);
      border-radius: 12px;
      padding: 0;
      transition: all 0.3s ease;
      position: relative;
    }

    .search-input-wrapper:focus-within {
      border-color: var(--primary);
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }

    .search-icon {
      padding: 12px 16px;
      color: var(--text-muted);
      font-size: 16px;
    }

    .search-input {
      flex: 1;
      border: none;
      outline: none;
      padding: 12px 8px;
      font-size: 16px;
      background: transparent;
      color: var(--text-primary);
    }

    .search-input::placeholder {
      color: var(--text-muted);
    }

    .search-actions {
      display: flex;
      align-items: center;
      padding: 8px;
      gap: 4px;
    }

    .clear-btn,
    .filter-btn {
      background: none;
      border: none;
      color: var(--text-muted);
      padding: 6px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s ease;
      position: relative;
    }

    .clear-btn:hover,
    .filter-btn:hover {
      background: var(--bg-secondary);
      color: var(--text-primary);
    }

    .filter-btn.active {
      background: var(--primary);
      color: white;
    }

    .filter-count {
      position: absolute;
      top: -4px;
      right: -4px;
      background: var(--danger);
      color: white;
      font-size: 10px;
      padding: 2px 4px;
      border-radius: 8px;
      min-width: 16px;
      text-align: center;
    }

    .search-loading {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
    }

    .loading-spinner {
      width: 16px;
      height: 16px;
      border: 2px solid var(--border-color);
      border-top: 2px solid var(--primary);
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    /* Filter Panel */
    .filter-panel {
      position: absolute;
      top: 100%;
      left: 0;
      right: 0;
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      box-shadow: var(--shadow-lg);
      padding: 16px;
      margin-top: 8px;
      z-index: 1000;
    }

    .filter-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .filter-header h6 {
      margin: 0;
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
    }

    .clear-filters-btn {
      background: none;
      border: none;
      color: var(--primary);
      font-size: 12px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;
      transition: all 0.2s ease;
    }

    .clear-filters-btn:hover {
      background: rgba(59, 130, 246, 0.1);
    }

    .filter-groups {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
    }

    .filter-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .filter-label {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .filter-options {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .filter-option {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 8px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s ease;
      font-size: 14px;
    }

    .filter-option:hover {
      background: var(--bg-secondary);
    }

    .filter-option.active {
      background: rgba(59, 130, 246, 0.1);
      color: var(--primary);
    }

    .filter-option input[type="checkbox"] {
      margin: 0;
    }

    .option-label {
      flex: 1;
    }

    .option-count {
      color: var(--text-muted);
      font-size: 12px;
    }

    /* Search Results */
    .search-results {
      position: absolute;
      top: 100%;
      left: 0;
      right: 0;
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      box-shadow: var(--shadow-lg);
      margin-top: 8px;
      z-index: 1000;
      max-height: 400px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .results-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color);
      background: var(--bg-secondary);
    }

    .results-count {
      font-size: 12px;
      color: var(--text-secondary);
      font-weight: 500;
    }

    .view-all-btn {
      background: none;
      border: none;
      color: var(--primary);
      font-size: 12px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;
      transition: all 0.2s ease;
    }

    .view-all-btn:hover {
      background: rgba(59, 130, 246, 0.1);
    }

    .results-list {
      flex: 1;
      overflow-y: auto;
    }

    .result-item {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      cursor: pointer;
      transition: all 0.2s ease;
      border-bottom: 1px solid var(--border-color);
    }

    .result-item:last-child {
      border-bottom: none;
    }

    .result-item:hover,
    .result-item.highlighted {
      background: var(--bg-secondary);
    }

    .result-icon {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg-secondary);
      border-radius: 8px;
      color: var(--primary);
      margin-right: 12px;
      flex-shrink: 0;
    }

    .result-content {
      flex: 1;
      min-width: 0;
    }

    .result-title {
      font-size: 14px;
      font-weight: 500;
      color: var(--text-primary);
      margin-bottom: 2px;
    }

    .result-subtitle {
      font-size: 12px;
      color: var(--text-secondary);
      line-height: 1.4;
    }

    .result-type {
      margin-left: 12px;
    }

    .type-badge {
      background: var(--bg-secondary);
      color: var(--text-secondary);
      font-size: 10px;
      padding: 4px 8px;
      border-radius: 12px;
      text-transform: uppercase;
      font-weight: 500;
      letter-spacing: 0.5px;
    }

    /* No Results */
    .no-results {
      padding: 32px 16px;
      text-align: center;
    }

    .no-results-icon {
      font-size: 32px;
      color: var(--text-muted);
      margin-bottom: 16px;
      opacity: 0.5;
    }

    .no-results-text {
      font-size: 16px;
      color: var(--text-primary);
      margin: 0 0 8px 0;
      font-weight: 500;
    }

    .no-results-suggestion {
      font-size: 14px;
      color: var(--text-secondary);
      margin: 0;
    }

    /* Quick Actions */
    .quick-actions {
      border-top: 1px solid var(--border-color);
      padding: 12px 16px;
      background: var(--bg-secondary);
    }

    .quick-actions-header {
      font-size: 12px;
      color: var(--text-secondary);
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 8px;
    }

    .quick-action-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s ease;
      font-size: 14px;
      color: var(--text-primary);
    }

    .quick-action-item:hover {
      background: var(--bg-primary);
    }

    /* Search Overlay */
    .search-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 999;
    }

    /* Highlight Search Terms */
    .search-highlight {
      background: rgba(59, 130, 246, 0.2);
      color: var(--primary);
      font-weight: 600;
      padding: 1px 2px;
      border-radius: 2px;
    }

    /* Animations */
    .slide-in-up {
      animation: slideInUp 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }

    @keyframes slideInUp {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .search-results {
        max-height: 300px;
      }

      .filter-groups {
        grid-template-columns: 1fr;
      }

      .results-header {
        padding: 8px 12px;
      }

      .result-item {
        padding: 10px 12px;
      }
    }
  `]
})
export class EnhancedSearchComponent implements OnInit, OnDestroy {
  @Input() placeholder: string = 'Search...';
  @Input() maxResults: number = 10;
  @Input() showFilters: boolean = true;
  @Input() showViewAll: boolean = true;
  @Input() filters: SearchFilter[] = [];
  @Input() quickActions: { label: string; icon: string; action: string }[] = [];
  @Input() searchFunction?: (query: string, filters: any) => Promise<SearchResult[]>;

  @Output() search = new EventEmitter<{ query: string; filters: any }>();
  @Output() resultSelect = new EventEmitter<SearchResult>();
  @Output() viewAll = new EventEmitter<{ query: string; filters: any }>();
  @Output() quickAction = new EventEmitter<string>();

  searchControl = new FormControl('');
  results: SearchResult[] = [];
  loading = false;
  showResults = false;
  showFilterPanel = false;
  noResults = false;
  isExpanded = false;
  highlightedIndex = -1;
  activeFilters: { [key: string]: string[] } = {};

  private subscription?: Subscription;

  constructor() {}

  ngOnInit(): void {
    this.subscription = this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query => {
          if (!query || query.length < 2) {
            this.results = [];
            this.showResults = false;
            this.noResults = false;
            return of([]);
          }

          this.loading = true;
          this.showResults = true;
          
          if (this.searchFunction) {
            return this.searchFunction(query, this.activeFilters);
          } else {
            this.search.emit({ query, filters: this.activeFilters });
            return of([]);
          }
        })
      )
      .subscribe(results => {
        this.results = results.slice(0, this.maxResults);
        this.loading = false;
        this.noResults = results.length === 0 && !!this.searchControl.value;
        this.highlightedIndex = -1;
      });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  onFocus(): void {
    this.isExpanded = true;
    if (this.searchControl.value && this.searchControl.value.length >= 2) {
      this.showResults = true;
    }
  }

  onBlur(): void {
    // Delay to allow click events on results
    setTimeout(() => {
      this.isExpanded = false;
      this.showResults = false;
      this.showFilterPanel = false;
    }, 200);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (!this.showResults || this.results.length === 0) return;

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.highlightedIndex = Math.min(this.highlightedIndex + 1, this.results.length - 1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.highlightedIndex = Math.max(this.highlightedIndex - 1, -1);
        break;
      case 'Enter':
        event.preventDefault();
        if (this.highlightedIndex >= 0) {
          this.selectResult(this.results[this.highlightedIndex]);
        }
        break;
      case 'Escape':
        this.closeSearch();
        break;
    }
  }

  clearSearch(): void {
    this.searchControl.setValue('');
    this.results = [];
    this.showResults = false;
    this.noResults = false;
  }

  toggleFilters(): void {
    this.showFilterPanel = !this.showFilterPanel;
  }

  toggleFilter(filterKey: string, value: string): void {
    if (!this.activeFilters[filterKey]) {
      this.activeFilters[filterKey] = [];
    }

    const index = this.activeFilters[filterKey].indexOf(value);
    if (index > -1) {
      this.activeFilters[filterKey].splice(index, 1);
      if (this.activeFilters[filterKey].length === 0) {
        delete this.activeFilters[filterKey];
      }
    } else {
      this.activeFilters[filterKey].push(value);
    }

    // Trigger new search with updated filters
    if (this.searchControl.value && this.searchControl.value.length >= 2) {
      this.search.emit({ query: this.searchControl.value, filters: this.activeFilters });
    }
  }

  isFilterActive(filterKey: string, value: string): boolean {
    return this.activeFilters[filterKey]?.includes(value) || false;
  }

  clearAllFilters(): void {
    this.activeFilters = {};
    if (this.searchControl.value && this.searchControl.value.length >= 2) {
      this.search.emit({ query: this.searchControl.value, filters: this.activeFilters });
    }
  }

  get activeFiltersCount(): number {
    return Object.values(this.activeFilters).reduce((count, filters) => count + filters.length, 0);
  }

  selectResult(result: SearchResult): void {
    this.resultSelect.emit(result);
    this.closeSearch();
  }

  viewAllResults(): void {
    this.viewAll.emit({ query: this.searchControl.value || '', filters: this.activeFilters });
    this.closeSearch();
  }

  executeQuickAction(action: { label: string; icon: string; action: string }): void {
    this.quickAction.emit(action.action);
    this.closeSearch();
  }

  closeSearch(): void {
    this.showResults = false;
    this.showFilterPanel = false;
    this.isExpanded = false;
  }

  highlightSearchTerm(text: string): string {
    const query = this.searchControl.value;
    if (!query || !text) return text;

    const regex = new RegExp(`(${query})`, 'gi');
    return text.replace(regex, '<span class="search-highlight">$1</span>');
  }

  // Method to update results externally
  updateResults(results: SearchResult[]): void {
    this.results = results.slice(0, this.maxResults);
    this.loading = false;
    this.noResults = results.length === 0 && !!this.searchControl.value;
    this.showResults = true;
  }
}
