import { Component, Input, Output, EventEmitter, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-enhanced-form-field',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EnhancedFormFieldComponent),
      multi: true
    }
  ],
  template: `
    <div class="enhanced-form-field" [class.has-error]="hasError" [class.focused]="isFocused">
      
      <!-- Label -->
      <label *ngIf="label" class="field-label" [for]="fieldId">
        {{ label }}
        <span *ngIf="required" class="required-asterisk">*</span>
        <span *ngIf="optional" class="optional-text">(Optional)</span>
      </label>

      <!-- Input Container -->
      <div class="input-container" [class.has-icon]="icon || suffixIcon">
        
        <!-- Prefix Icon -->
        <div *ngIf="icon" class="input-icon prefix-icon">
          <i class="fas" [ngClass]="icon"></i>
        </div>

        <!-- Input Field -->
        <input
          *ngIf="type !== 'textarea' && type !== 'select'"
          [id]="fieldId"
          [type]="getInputType()"
          [placeholder]="placeholder"
          [disabled]="disabled"
          [readonly]="readonly"
          [autocomplete]="autocomplete"
          [value]="value"
          (input)="onInput($event)"
          (focus)="onFocus()"
          (blur)="onBlur()"
          (keyup)="onKeyup($event)"
          class="form-input"
        />

        <!-- Textarea -->
        <textarea
          *ngIf="type === 'textarea'"
          [id]="fieldId"
          [placeholder]="placeholder"
          [disabled]="disabled"
          [readonly]="readonly"
          [rows]="rows"
          [value]="value"
          (input)="onInput($event)"
          (focus)="onFocus()"
          (blur)="onBlur()"
          class="form-input form-textarea"
        ></textarea>

        <!-- Select -->
        <select
          *ngIf="type === 'select'"
          [id]="fieldId"
          [disabled]="disabled"
          [value]="value"
          (change)="onInput($event)"
          (focus)="onFocus()"
          (blur)="onBlur()"
          class="form-input form-select"
        >
          <option value="" disabled>{{ placeholder || 'Select an option' }}</option>
          <option *ngFor="let option of options" [value]="option.value">
            {{ option.label }}
          </option>
        </select>

        <!-- Suffix Icon / Toggle -->
        <div *ngIf="suffixIcon || type === 'password'" class="input-icon suffix-icon">
          <button
            *ngIf="type === 'password'"
            type="button"
            class="icon-button"
            (click)="togglePasswordVisibility()"
            [title]="showPassword ? 'Hide password' : 'Show password'"
          >
            <i class="fas" [ngClass]="showPassword ? 'fa-eye-slash' : 'fa-eye'"></i>
          </button>
          <i *ngIf="suffixIcon && type !== 'password'" class="fas" [ngClass]="suffixIcon"></i>
        </div>

        <!-- Loading Indicator -->
        <div *ngIf="loading" class="input-loading">
          <div class="loading-spinner"></div>
        </div>
      </div>

      <!-- Helper Text / Error Messages -->
      <div class="field-feedback">
        <div *ngIf="!hasError && helperText" class="helper-text">
          <i class="fas fa-info-circle"></i>
          {{ helperText }}
        </div>
        
        <div *ngIf="hasError && errorMessage" class="error-message">
          <i class="fas fa-exclamation-triangle"></i>
          {{ errorMessage }}
        </div>

        <!-- Character Count -->
        <div *ngIf="maxLength && value" class="character-count">
          {{ value.length }}/{{ maxLength }}
        </div>
      </div>

      <!-- Suggestions -->
      <div *ngIf="suggestions.length > 0 && isFocused" class="suggestions-dropdown">
        <div
          *ngFor="let suggestion of suggestions"
          class="suggestion-item"
          (click)="selectSuggestion(suggestion)"
        >
          {{ suggestion }}
        </div>
      </div>
    </div>
  `,
  styles: [`
    .enhanced-form-field {
      position: relative;
      margin-bottom: 20px;
    }

    .field-label {
      display: block;
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
      margin-bottom: 8px;
      transition: color 0.3s ease;
    }

    .required-asterisk {
      color: var(--danger);
      margin-left: 2px;
    }

    .optional-text {
      color: var(--text-muted);
      font-weight: 400;
      font-size: 12px;
      margin-left: 4px;
    }

    .input-container {
      position: relative;
      display: flex;
      align-items: center;
    }

    .form-input {
      width: 100%;
      padding: 12px 16px;
      border: 2px solid var(--border-color);
      border-radius: 8px;
      font-size: 14px;
      background: var(--bg-primary);
      color: var(--text-primary);
      transition: all 0.3s ease;
      outline: none;
    }

    .input-container.has-icon .form-input {
      padding-left: 48px;
    }

    .input-container .form-input:has(+ .suffix-icon) {
      padding-right: 48px;
    }

    .form-input:focus {
      border-color: var(--primary);
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }

    .enhanced-form-field.has-error .form-input {
      border-color: var(--danger);
    }

    .enhanced-form-field.has-error .form-input:focus {
      border-color: var(--danger);
      box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
    }

    .form-textarea {
      resize: vertical;
      min-height: 100px;
    }

    .form-select {
      cursor: pointer;
    }

    .input-icon {
      position: absolute;
      top: 50%;
      transform: translateY(-50%);
      color: var(--text-muted);
      font-size: 16px;
      z-index: 2;
    }

    .prefix-icon {
      left: 16px;
    }

    .suffix-icon {
      right: 16px;
    }

    .icon-button {
      background: none;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      padding: 4px;
      border-radius: 4px;
      transition: all 0.2s ease;
    }

    .icon-button:hover {
      color: var(--text-primary);
      background: rgba(0, 0, 0, 0.05);
    }

    .input-loading {
      position: absolute;
      right: 16px;
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

    .field-feedback {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-top: 6px;
      min-height: 20px;
    }

    .helper-text,
    .error-message {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      line-height: 1.4;
    }

    .helper-text {
      color: var(--text-secondary);
    }

    .error-message {
      color: var(--danger);
    }

    .character-count {
      font-size: 11px;
      color: var(--text-muted);
      margin-left: auto;
    }

    .suggestions-dropdown {
      position: absolute;
      top: 100%;
      left: 0;
      right: 0;
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      box-shadow: var(--shadow-lg);
      z-index: 1000;
      max-height: 200px;
      overflow-y: auto;
    }

    .suggestion-item {
      padding: 12px 16px;
      cursor: pointer;
      transition: background-color 0.2s ease;
      border-bottom: 1px solid var(--border-color);
    }

    .suggestion-item:last-child {
      border-bottom: none;
    }

    .suggestion-item:hover {
      background: var(--bg-secondary);
    }

    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    /* Focus animation */
    .enhanced-form-field.focused .field-label {
      color: var(--primary);
    }

    /* Disabled state */
    .form-input:disabled {
      background: var(--bg-secondary);
      color: var(--text-muted);
      cursor: not-allowed;
    }

    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .form-input {
        padding: 10px 14px;
        font-size: 16px; /* Prevents zoom on iOS */
      }

      .input-container.has-icon .form-input {
        padding-left: 44px;
      }
    }
  `]
})
export class EnhancedFormFieldComponent implements ControlValueAccessor {
  @Input() label: string = '';
  @Input() placeholder: string = '';
  @Input() type: string = 'text';
  @Input() icon: string = '';
  @Input() suffixIcon: string = '';
  @Input() required: boolean = false;
  @Input() optional: boolean = false;
  @Input() disabled: boolean = false;
  @Input() readonly: boolean = false;
  @Input() loading: boolean = false;
  @Input() helperText: string = '';
  @Input() errorMessage: string = '';
  @Input() hasError: boolean = false;
  @Input() autocomplete: string = '';
  @Input() maxLength: number = 0;
  @Input() rows: number = 3;
  @Input() options: { value: any; label: string }[] = [];
  @Input() suggestions: string[] = [];

  @Output() inputChange = new EventEmitter<string>();
  @Output() keyup = new EventEmitter<KeyboardEvent>();

  value: string = '';
  isFocused: boolean = false;
  showPassword: boolean = false;
  fieldId: string = '';

  private onChange = (value: any) => {};
  private onTouched = () => {};

  constructor() {
    this.fieldId = 'field_' + Math.random().toString(36).substr(2, 9);
  }

  // ControlValueAccessor implementation
  writeValue(value: any): void {
    this.value = value || '';
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  // Event handlers
  onInput(event: any): void {
    const value = event.target.value;
    this.value = value;
    this.onChange(value);
    this.inputChange.emit(value);
  }

  onFocus(): void {
    this.isFocused = true;
  }

  onBlur(): void {
    this.isFocused = false;
    this.onTouched();
  }

  onKeyup(event: KeyboardEvent): void {
    this.keyup.emit(event);
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  getInputType(): string {
    if (this.type === 'password') {
      return this.showPassword ? 'text' : 'password';
    }
    return this.type;
  }

  selectSuggestion(suggestion: string): void {
    this.value = suggestion;
    this.onChange(suggestion);
    this.inputChange.emit(suggestion);
    this.isFocused = false;
  }
}
