import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface DraftApplication {
  id: string;
  applicantId: number;
  currentStep: number;
  totalSteps: number;
  completionPercentage: number;
  selectedLoanType: any;
  formData: {
    loanTypeForm: any;
    personalDetailsForm: any;
    loanDetailsForm: any;
    financialDetailsForm: any;
    documentsForm: any;
  };
  lastSaved: string;
  createdAt: string;
  title: string;
  description: string;
}

@Injectable({
  providedIn: 'root'
})
export class DraftService {
  private readonly STORAGE_KEY = 'loanApplicationDrafts';
  private draftsSubject = new BehaviorSubject<DraftApplication[]>([]);
  public drafts$ = this.draftsSubject.asObservable();

  constructor() {
    this.loadDrafts();
  }

  /**
   * Save or update a draft application
   */
  saveDraft(draftData: Partial<DraftApplication>): string {
    const drafts = this.getDrafts();
    const draftId = draftData.id || this.generateDraftId();
    
    const existingIndex = drafts.findIndex(d => d.id === draftId);
    const now = new Date().toISOString();
    
    const draft: DraftApplication = {
      id: draftId,
      applicantId: draftData.applicantId || 0,
      currentStep: draftData.currentStep || 1,
      totalSteps: draftData.totalSteps || 6,
      completionPercentage: draftData.completionPercentage || 0,
      selectedLoanType: draftData.selectedLoanType || null,
      formData: draftData.formData || {
        loanTypeForm: {},
        personalDetailsForm: {},
        loanDetailsForm: {},
        financialDetailsForm: {},
        documentsForm: {}
      },
      lastSaved: now,
      createdAt: draftData.createdAt || now,
      title: draftData.title || this.generateDraftTitle(draftData.selectedLoanType),
      description: draftData.description || this.generateDraftDescription(draftData.currentStep || 1, draftData.totalSteps || 6)
    };

    if (existingIndex >= 0) {
      drafts[existingIndex] = draft;
    } else {
      drafts.unshift(draft); // Add to beginning
    }

    // Keep only the latest 10 drafts
    if (drafts.length > 10) {
      drafts.splice(10);
    }

    this.saveDrafts(drafts);
    this.draftsSubject.next(drafts);
    
    return draftId;
  }

  /**
   * Get all draft applications
   */
  getDrafts(): DraftApplication[] {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch (error) {
      console.error('Error loading drafts:', error);
      return [];
    }
  }

  /**
   * Get a specific draft by ID
   */
  getDraft(id: string): DraftApplication | null {
    const drafts = this.getDrafts();
    return drafts.find(d => d.id === id) || null;
  }

  /**
   * Delete a draft application
   */
  deleteDraft(id: string): void {
    const drafts = this.getDrafts().filter(d => d.id !== id);
    this.saveDrafts(drafts);
    this.draftsSubject.next(drafts);
  }

  /**
   * Clear all drafts
   */
  clearAllDrafts(): void {
    localStorage.removeItem(this.STORAGE_KEY);
    this.draftsSubject.next([]);
  }

  /**
   * Get drafts for a specific applicant
   */
  getDraftsByApplicant(applicantId: number): DraftApplication[] {
    return this.getDrafts().filter(d => d.applicantId === applicantId);
  }

  /**
   * Check if there are any drafts for an applicant
   */
  hasDrafts(applicantId: number): boolean {
    return this.getDraftsByApplicant(applicantId).length > 0;
  }

  /**
   * Get the most recent draft for an applicant
   */
  getLatestDraft(applicantId: number): DraftApplication | null {
    const drafts = this.getDraftsByApplicant(applicantId);
    return drafts.length > 0 ? drafts[0] : null;
  }

  /**
   * Calculate completion percentage based on form data
   */
  calculateCompletionPercentage(formData: any, currentStep: number): number {
    let totalFields = 0;
    let filledFields = 0;

    // Count fields in each form
    Object.keys(formData).forEach(formKey => {
      const form = formData[formKey];
      if (form && typeof form === 'object') {
        Object.keys(form).forEach(fieldKey => {
          totalFields++;
          const value = form[fieldKey];
          if (value !== null && value !== undefined && value !== '') {
            filledFields++;
          }
        });
      }
    });

    // Add step completion bonus
    const stepBonus = (currentStep - 1) * 5; // 5% bonus per completed step
    const fieldPercentage = totalFields > 0 ? (filledFields / totalFields) * 85 : 0; // Max 85% from fields
    
    return Math.min(100, Math.round(fieldPercentage + stepBonus));
  }

  /**
   * Format time since last saved
   */
  getTimeSinceLastSaved(lastSaved: string): string {
    const now = new Date();
    const saved = new Date(lastSaved);
    const diffMs = now.getTime() - saved.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays > 0) {
      return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    } else if (diffHours > 0) {
      return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else if (diffMins > 0) {
      return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
    } else {
      return 'Just now';
    }
  }

  /**
   * Get step name by number
   */
  getStepName(step: number): string {
    const stepNames = {
      1: 'Loan Type',
      2: 'Personal Details',
      3: 'Loan Details', 
      4: 'Financial Details',
      5: 'Documents',
      6: 'Review'
    };
    return stepNames[step as keyof typeof stepNames] || `Step ${step}`;
  }

  // Private helper methods
  private loadDrafts(): void {
    const drafts = this.getDrafts();
    this.draftsSubject.next(drafts);
  }

  private saveDrafts(drafts: DraftApplication[]): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(drafts));
    } catch (error) {
      console.error('Error saving drafts:', error);
    }
  }

  private generateDraftId(): string {
    return 'draft_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  private generateDraftTitle(selectedLoanType: any): string {
    if (selectedLoanType && selectedLoanType.name) {
      return `${selectedLoanType.name} Application`;
    }
    return 'Loan Application Draft';
  }

  private generateDraftDescription(currentStep: number, totalSteps: number): string {
    const stepName = this.getStepName(currentStep);
    return `Stopped at ${stepName} (Step ${currentStep} of ${totalSteps})`;
  }
}
