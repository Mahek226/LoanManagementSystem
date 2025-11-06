import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormBuilder, Validators, AbstractControl } from '@angular/forms';

export interface FormField {
  key: string;
  label: string;
  type: 'text' | 'email' | 'password' | 'number' | 'tel' | 'select' | 'textarea' | 'date' | 'checkbox' | 'radio';
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  options?: {label: string, value: any}[];
  validation?: any[];
  icon?: string;
  help?: string;
  grid?: string; // Bootstrap grid classes
}

export interface FormSection {
  title: string;
  subtitle?: string;
  fields: FormField[];
  collapsible?: boolean;
  collapsed?: boolean;
}

@Component({
  selector: 'app-professional-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="onSubmit()" class="professional-form" [class.loading]="loading">
      
      <!-- Form Header -->
      <div class="form-header" *ngIf="title || subtitle">
        <h2 class="form-title" *ngIf="title">{{ title }}</h2>
        <p class="form-subtitle" *ngIf="subtitle">{{ subtitle }}</p>
      </div>

      <!-- Progress Indicator -->
      <div class="form-progress" *ngIf="showProgress && sections.length > 1">
        <div class="progress-steps">
          <div 
            *ngFor="let section of sections; let i = index"
            class="progress-step"
            [class.active]="i === currentSection"
            [class.completed]="i < currentSection">
            <div class="step-number">{{ i + 1 }}</div>
            <span class="step-label">{{ section.title }}</span>
          </div>
        </div>
        <div class="progress-bar">
          <div 
            class="progress-fill" 
            [style.width.%]="progressPercentage"></div>
        </div>
      </div>

      <!-- Form Sections -->
      <div class="form-sections">
        <div 
          *ngFor="let section of sections; let sectionIndex = index"
          class="form-section"
          [class.active]="!showProgress || sectionIndex === currentSection"
          [class.hidden]="showProgress && sectionIndex !== currentSection">
          
          <!-- Section Header -->
          <div class="section-header" *ngIf="section.title">
            <div class="section-title-wrapper" 
                 [class.collapsible]="section.collapsible"
                 (click)="toggleSection(sectionIndex)">
              <h3 class="section-title">{{ section.title }}</h3>
              <p class="section-subtitle" *ngIf="section.subtitle">{{ section.subtitle }}</p>
              <i 
                *ngIf="section.collapsible"
                class="collapse-icon"
                [class]="section.collapsed ? 'fas fa-chevron-down' : 'fas fa-chevron-up'"></i>
            </div>
          </div>

          <!-- Section Fields -->
          <div class="section-fields" 
               [class.collapsed]="section.collapsed"
               [@slideToggle]="section.collapsed ? 'collapsed' : 'expanded'">
            <div class="fields-grid">
              <div 
                *ngFor="let field of section.fields"
                class="form-group"
                [ngClass]="field.grid || 'col-12'">
                
                <!-- Field Label -->
                <label class="form-label" [for]="field.key">
                  {{ field.label }}
                  <span class="required" *ngIf="field.required">*</span>
                </label>

                <!-- Field Input -->
                <div class="input-wrapper" [ngSwitch]="field.type">
                  
                  <!-- Text, Email, Password, Tel, Number -->
                  <div *ngSwitchCase="'text'" class="input-container">
                    <i *ngIf="field.icon" class="input-icon" [class]="field.icon"></i>
                    <input 
                      [id]="field.key"
                      [formControlName]="field.key"
                      type="text"
                      class="form-input"
                      [class.has-icon]="field.icon"
                      [class.error]="hasError(field.key)"
                      [class.success]="isValid(field.key)"
                      [placeholder]="field.placeholder || ''"
                      [disabled]="field.disabled">
                    <div class="input-status" *ngIf="isValid(field.key) && !hasError(field.key)">
                      <i class="fas fa-check-circle text-success"></i>
                    </div>
                  </div>

                  <div *ngSwitchCase="'email'" class="input-container">
                    <i class="input-icon fas fa-envelope"></i>
                    <input 
                      [id]="field.key"
                      [formControlName]="field.key"
                      type="email"
                      class="form-input has-icon"
                      [class.error]="hasError(field.key)"
                      [class.success]="isValid(field.key)"
                      [placeholder]="field.placeholder || 'Enter email address'"
                      [disabled]="field.disabled">
                    <div class="input-status" *ngIf="isValid(field.key) && !hasError(field.key)">
                      <i class="fas fa-check-circle text-success"></i>
                    </div>
                  </div>

                  <div *ngSwitchCase="'password'" class="input-container">
                    <i class="input-icon fas fa-lock"></i>
                    <input 
                      [id]="field.key"
                      [formControlName]="field.key"
                      [type]="showPassword[field.key] ? 'text' : 'password'"
                      class="form-input has-icon"
                      [class.error]="hasError(field.key)"
                      [class.success]="isValid(field.key)"
                      [placeholder]="field.placeholder || 'Enter password'"
                      [disabled]="field.disabled">
                    <button 
                      type="button"
                      class="password-toggle"
                      (click)="togglePassword(field.key)">
                      <i [class]="showPassword[field.key] ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                    </button>
                  </div>

                  <div *ngSwitchCase="'number'" class="input-container">
                    <i *ngIf="field.icon" class="input-icon" [class]="field.icon"></i>
                    <input 
                      [id]="field.key"
                      [formControlName]="field.key"
                      type="number"
                      class="form-input"
                      [class.has-icon]="field.icon"
                      [class.error]="hasError(field.key)"
                      [class.success]="isValid(field.key)"
                      [placeholder]="field.placeholder || ''"
                      [disabled]="field.disabled">
                  </div>

                  <div *ngSwitchCase="'tel'" class="input-container">
                    <i class="input-icon fas fa-phone"></i>
                    <input 
                      [id]="field.key"
                      [formControlName]="field.key"
                      type="tel"
                      class="form-input has-icon"
                      [class.error]="hasError(field.key)"
                      [class.success]="isValid(field.key)"
                      [placeholder]="field.placeholder || 'Enter phone number'"
                      [disabled]="field.disabled">
                  </div>

                  <!-- Select -->
                  <div *ngSwitchCase="'select'" class="input-container">
                    <i *ngIf="field.icon" class="input-icon" [class]="field.icon"></i>
                    <select 
                      [id]="field.key"
                      [formControlName]="field.key"
                      class="form-select"
                      [class.has-icon]="field.icon"
                      [class.error]="hasError(field.key)"
                      [class.success]="isValid(field.key)"
                      [disabled]="field.disabled">
                      <option value="" disabled>{{ field.placeholder || 'Select an option' }}</option>
                      <option 
                        *ngFor="let option of field.options"
                        [value]="option.value">
                        {{ option.label }}
                      </option>
                    </select>
                    <i class="select-arrow fas fa-chevron-down"></i>
                  </div>

                  <!-- Textarea -->
                  <div *ngSwitchCase="'textarea'" class="input-container">
                    <textarea 
                      [id]="field.key"
                      [formControlName]="field.key"
                      class="form-textarea"
                      [class.error]="hasError(field.key)"
                      [class.success]="isValid(field.key)"
                      [placeholder]="field.placeholder || ''"
                      [disabled]="field.disabled"
                      rows="4"></textarea>
                  </div>

                  <!-- Date -->
                  <div *ngSwitchCase="'date'" class="input-container">
                    <i class="input-icon fas fa-calendar"></i>
                    <input 
                      [id]="field.key"
                      [formControlName]="field.key"
                      type="date"
                      class="form-input has-icon"
                      [class.error]="hasError(field.key)"
                      [class.success]="isValid(field.key)"
                      [disabled]="field.disabled">
                  </div>

                  <!-- Checkbox -->
                  <div *ngSwitchCase="'checkbox'" class="checkbox-container">
                    <label class="checkbox-label">
                      <input 
                        [id]="field.key"
                        [formControlName]="field.key"
                        type="checkbox"
                        class="checkbox-input"
                        [disabled]="field.disabled">
                      <span class="checkbox-custom"></span>
                      <span class="checkbox-text">{{ field.placeholder || field.label }}</span>
                    </label>
                  </div>

                  <!-- Radio -->
                  <div *ngSwitchCase="'radio'" class="radio-container">
                    <div 
                      *ngFor="let option of field.options"
                      class="radio-option">
                      <label class="radio-label">
                        <input 
                          [formControlName]="field.key"
                          type="radio"
                          class="radio-input"
                          [value]="option.value"
                          [disabled]="field.disabled">
                        <span class="radio-custom"></span>
                        <span class="radio-text">{{ option.label }}</span>
                      </label>
                    </div>
                  </div>
                </div>

                <!-- Field Help Text -->
                <div class="form-help" *ngIf="field.help && !hasError(field.key)">
                  <i class="fas fa-info-circle"></i>
                  {{ field.help }}
                </div>

                <!-- Field Error -->
                <div class="form-error" *ngIf="hasError(field.key)">
                  <i class="fas fa-exclamation-circle"></i>
                  {{ getErrorMessage(field.key, field.label) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Form Actions -->
      <div class="form-actions">
        <div class="actions-left">
          <button 
            *ngIf="showProgress && currentSection > 0"
            type="button"
            class="btn btn-outline"
            (click)="previousSection()">
            <i class="fas fa-arrow-left"></i>
            Previous
          </button>
        </div>

        <div class="actions-right">
          <button 
            *ngIf="showCancel"
            type="button"
            class="btn btn-ghost"
            (click)="onCancel()">
            Cancel
          </button>

          <button 
            *ngIf="showProgress && currentSection < sections.length - 1"
            type="button"
            class="btn btn-primary"
            [disabled]="!isSectionValid(currentSection)"
            (click)="nextSection()">
            Next
            <i class="fas fa-arrow-right"></i>
          </button>

          <button 
            *ngIf="!showProgress || currentSection === sections.length - 1"
            type="submit"
            class="btn btn-primary"
            [disabled]="form.invalid || loading">
            <i *ngIf="loading" class="fas fa-spinner fa-spin"></i>
            <i *ngIf="!loading" [class]="submitIcon"></i>
            {{ submitText }}
          </button>
        </div>
      </div>

      <!-- Loading Overlay -->
      <div class="loading-overlay" *ngIf="loading">
        <div class="loading-content">
          <div class="loading-spinner">
            <i class="fas fa-spinner fa-spin"></i>
          </div>
          <p>{{ loadingText }}</p>
        </div>
      </div>
    </form>
  `,
  styles: [`
    /* Form styles will be added here */
    .professional-form {
      background: var(--white);
      border-radius: var(--radius-xl);
      box-shadow: var(--shadow-lg);
      padding: var(--space-8);
      position: relative;
      overflow: hidden;
    }

    .form-header {
      text-align: center;
      margin-bottom: var(--space-8);
    }

    .form-title {
      font-size: var(--text-3xl);
      font-weight: var(--font-bold);
      color: var(--gray-800);
      margin-bottom: var(--space-2);
    }

    .form-subtitle {
      font-size: var(--text-lg);
      color: var(--gray-600);
      margin: 0;
    }

    .form-progress {
      margin-bottom: var(--space-8);
    }

    .progress-steps {
      display: flex;
      justify-content: space-between;
      margin-bottom: var(--space-4);
    }

    .progress-step {
      display: flex;
      flex-direction: column;
      align-items: center;
      flex: 1;
    }

    .step-number {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: var(--gray-200);
      color: var(--gray-500);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: var(--font-semibold);
      margin-bottom: var(--space-2);
      transition: all var(--transition-normal);
    }

    .progress-step.active .step-number {
      background: var(--primary-blue);
      color: var(--white);
    }

    .progress-step.completed .step-number {
      background: var(--success-green);
      color: var(--white);
    }

    .step-label {
      font-size: var(--text-sm);
      color: var(--gray-600);
      text-align: center;
    }

    .progress-bar {
      height: 4px;
      background: var(--gray-200);
      border-radius: var(--radius-full);
      overflow: hidden;
    }

    .progress-fill {
      height: 100%;
      background: var(--gradient-primary);
      transition: width 0.3s ease;
    }

    .form-section {
      margin-bottom: var(--space-8);
    }

    .form-section.hidden {
      display: none;
    }

    .section-header {
      margin-bottom: var(--space-6);
    }

    .section-title-wrapper {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .section-title-wrapper.collapsible {
      cursor: pointer;
      padding: var(--space-3);
      border-radius: var(--radius-lg);
      transition: background-color var(--transition-normal);
    }

    .section-title-wrapper.collapsible:hover {
      background: var(--gray-50);
    }

    .section-title {
      font-size: var(--text-xl);
      font-weight: var(--font-semibold);
      color: var(--gray-800);
      margin: 0;
    }

    .section-subtitle {
      font-size: var(--text-sm);
      color: var(--gray-600);
      margin: var(--space-1) 0 0 0;
    }

    .collapse-icon {
      color: var(--gray-400);
      transition: transform var(--transition-normal);
    }

    .fields-grid {
      display: grid;
      grid-template-columns: repeat(12, 1fr);
      gap: var(--space-4);
    }

    .form-group {
      display: flex;
      flex-direction: column;
    }

    .form-label {
      font-size: var(--text-sm);
      font-weight: var(--font-medium);
      color: var(--gray-700);
      margin-bottom: var(--space-2);
    }

    .required {
      color: var(--danger-red);
      margin-left: var(--space-1);
    }

    .input-container {
      position: relative;
      display: flex;
      align-items: center;
    }

    .form-input,
    .form-select,
    .form-textarea {
      width: 100%;
      padding: var(--space-3) var(--space-4);
      border: 2px solid var(--gray-200);
      border-radius: var(--radius-lg);
      font-family: var(--font-family-primary);
      font-size: var(--text-base);
      background: var(--white);
      transition: all var(--transition-normal);
    }

    .form-input.has-icon {
      padding-left: var(--space-10);
    }

    .form-input:focus,
    .form-select:focus,
    .form-textarea:focus {
      outline: none;
      border-color: var(--primary-blue);
      box-shadow: 0 0 0 3px rgba(30, 64, 175, 0.1);
    }

    .form-input.error,
    .form-select.error,
    .form-textarea.error {
      border-color: var(--danger-red);
      background: rgba(220, 38, 38, 0.05);
    }

    .form-input.success,
    .form-select.success,
    .form-textarea.success {
      border-color: var(--success-green);
      background: rgba(5, 150, 105, 0.05);
    }

    .input-icon {
      position: absolute;
      left: var(--space-3);
      color: var(--gray-400);
      font-size: var(--text-base);
      z-index: 2;
    }

    .input-status {
      position: absolute;
      right: var(--space-3);
      font-size: var(--text-base);
    }

    .password-toggle {
      position: absolute;
      right: var(--space-3);
      background: none;
      border: none;
      color: var(--gray-400);
      cursor: pointer;
      padding: var(--space-1);
    }

    .select-arrow {
      position: absolute;
      right: var(--space-3);
      color: var(--gray-400);
      pointer-events: none;
    }

    .checkbox-container,
    .radio-container {
      margin-top: var(--space-2);
    }

    .checkbox-label,
    .radio-label {
      display: flex;
      align-items: center;
      cursor: pointer;
      font-size: var(--text-sm);
    }

    .checkbox-input,
    .radio-input {
      display: none;
    }

    .checkbox-custom,
    .radio-custom {
      width: 20px;
      height: 20px;
      border: 2px solid var(--gray-300);
      margin-right: var(--space-3);
      transition: all var(--transition-normal);
    }

    .checkbox-custom {
      border-radius: var(--radius-md);
    }

    .radio-custom {
      border-radius: 50%;
    }

    .checkbox-input:checked + .checkbox-custom {
      background: var(--primary-blue);
      border-color: var(--primary-blue);
    }

    .radio-input:checked + .radio-custom {
      background: var(--primary-blue);
      border-color: var(--primary-blue);
    }

    .form-help {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      margin-top: var(--space-2);
      font-size: var(--text-sm);
      color: var(--gray-500);
    }

    .form-error {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      margin-top: var(--space-2);
      font-size: var(--text-sm);
      color: var(--danger-red);
    }

    .form-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: var(--space-8);
      padding-top: var(--space-6);
      border-top: 1px solid var(--gray-100);
    }

    .actions-left,
    .actions-right {
      display: flex;
      gap: var(--space-3);
    }

    .loading-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.9);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10;
      backdrop-filter: blur(2px);
    }

    .loading-content {
      text-align: center;
    }

    .loading-spinner {
      font-size: 2rem;
      color: var(--primary-blue);
      margin-bottom: var(--space-4);
    }

    /* Responsive Design */
    @media (max-width: 768px) {
      .professional-form {
        padding: var(--space-6);
      }

      .fields-grid {
        grid-template-columns: 1fr;
      }

      .form-actions {
        flex-direction: column;
        gap: var(--space-4);
      }

      .actions-left,
      .actions-right {
        width: 100%;
        justify-content: center;
      }
    }
  `],
  animations: [
    // Add slide toggle animation here if needed
  ]
})
export class ProfessionalFormComponent implements OnInit {
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() sections: FormSection[] = [];
  @Input() showProgress = false;
  @Input() showCancel = true;
  @Input() submitText = 'Submit';
  @Input() submitIcon = 'fas fa-check';
  @Input() loading = false;
  @Input() loadingText = 'Processing...';

  @Output() formSubmit = new EventEmitter<any>();
  @Output() formCancel = new EventEmitter<void>();
  @Output() sectionChange = new EventEmitter<number>();

  form!: FormGroup;
  currentSection = 0;
  showPassword: {[key: string]: boolean} = {};

  get progressPercentage(): number {
    return ((this.currentSection + 1) / this.sections.length) * 100;
  }

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.buildForm();
  }

  private buildForm(): void {
    const formControls: {[key: string]: any} = {};

    this.sections.forEach(section => {
      section.fields.forEach(field => {
        const validators = [];
        
        if (field.required) {
          validators.push(Validators.required);
        }
        
        if (field.type === 'email') {
          validators.push(Validators.email);
        }
        
        if (field.validation) {
          validators.push(...field.validation);
        }

        formControls[field.key] = [
          field.type === 'checkbox' ? false : '',
          validators
        ];
      });
    });

    this.form = this.fb.group(formControls);
  }

  hasError(fieldKey: string): boolean {
    const control = this.form.get(fieldKey);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  isValid(fieldKey: string): boolean {
    const control = this.form.get(fieldKey);
    return !!(control && control.valid && (control.dirty || control.touched));
  }

  getErrorMessage(fieldKey: string, fieldLabel: string): string {
    const control = this.form.get(fieldKey);
    if (!control || !control.errors) return '';

    const errors = control.errors;
    
    if (errors['required']) {
      return `${fieldLabel} is required`;
    }
    
    if (errors['email']) {
      return 'Please enter a valid email address';
    }
    
    if (errors['minlength']) {
      return `${fieldLabel} must be at least ${errors['minlength'].requiredLength} characters`;
    }
    
    if (errors['maxlength']) {
      return `${fieldLabel} cannot exceed ${errors['maxlength'].requiredLength} characters`;
    }
    
    if (errors['pattern']) {
      return `${fieldLabel} format is invalid`;
    }

    return 'This field is invalid';
  }

  togglePassword(fieldKey: string): void {
    this.showPassword[fieldKey] = !this.showPassword[fieldKey];
  }

  toggleSection(sectionIndex: number): void {
    this.sections[sectionIndex].collapsed = !this.sections[sectionIndex].collapsed;
  }

  isSectionValid(sectionIndex: number): boolean {
    const section = this.sections[sectionIndex];
    return section.fields.every(field => {
      const control = this.form.get(field.key);
      return control ? control.valid : true;
    });
  }

  nextSection(): void {
    if (this.currentSection < this.sections.length - 1) {
      this.currentSection++;
      this.sectionChange.emit(this.currentSection);
    }
  }

  previousSection(): void {
    if (this.currentSection > 0) {
      this.currentSection--;
      this.sectionChange.emit(this.currentSection);
    }
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.formSubmit.emit(this.form.value);
    } else {
      this.markAllFieldsAsTouched();
    }
  }

  onCancel(): void {
    this.formCancel.emit();
  }

  private markAllFieldsAsTouched(): void {
    Object.keys(this.form.controls).forEach(key => {
      this.form.get(key)?.markAsTouched();
    });
  }
}
