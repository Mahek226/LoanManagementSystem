import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<Toast[]>([]);
  public toasts$ = this.toastsSubject.asObservable();

  private toasts: Toast[] = [];

  constructor() {}

  /**
   * Show a success toast
   */
  showSuccess(title: string, message: string, duration: number = 5000): void {
    this.addToast('success', title, message, Math.max(duration, 3000));
  }

  /**
   * Show an error toast
   */
  showError(title: string, message: string, duration: number = 6000): void {
    this.addToast('error', title, message, Math.max(duration, 3000));
  }

  /**
   * Show a warning toast
   */
  showWarning(title: string, message: string, duration: number = 5000): void {
    this.addToast('warning', title, message, Math.max(duration, 3000));
  }

  /**
   * Show an info toast
   */
  showInfo(title: string, message: string, duration: number = 5000): void {
    this.addToast('info', title, message, Math.max(duration, 3000));
  }

  /**
   * Remove a specific toast
   */
  removeToast(id: string): void {
    this.toasts = this.toasts.filter(toast => toast.id !== id);
    this.toastsSubject.next([...this.toasts]);
  }

  /**
   * Clear all toasts
   */
  clearAll(): void {
    this.toasts = [];
    this.toastsSubject.next([]);
  }

  /**
   * Add a toast to the list
   */
  private addToast(type: Toast['type'], title: string, message: string, duration?: number): void {
    const toast: Toast = {
      id: this.generateId(),
      type,
      title,
      message,
      duration,
      timestamp: new Date()
    };

    this.toasts.push(toast);
    this.toastsSubject.next([...this.toasts]);

    // Auto-remove toast after duration
    if (duration && duration > 0) {
      setTimeout(() => {
        this.removeToast(toast.id);
      }, duration);
    }
  }

  /**
   * Generate a unique ID for toasts
   */
  private generateId(): string {
    return Math.random().toString(36).substr(2, 9) + Date.now().toString(36);
  }

  /**
   * Show toast based on HTTP response or error
   */
  showResponseToast(response: any, successTitle: string = 'Success', errorTitle: string = 'Error'): void {
    if (response && response.success !== false) {
      // Success response
      const message = response.message || response.data?.message || 'Operation completed successfully';
      this.showSuccess(successTitle, message);
    } else {
      // Error response
      const message = response?.message || response?.error?.message || 'An error occurred';
      this.showError(errorTitle, message);
    }
  }

  /**
   * Show toast for HTTP errors
   */
  showHttpError(error: any, title: string = 'Error'): void {
    let message = 'An unexpected error occurred';
    
    if (error?.error?.message) {
      message = error.error.message;
    } else if (error?.message) {
      message = error.message;
    } else if (typeof error === 'string') {
      message = error;
    } else if (error?.status) {
      switch (error.status) {
        case 400:
          message = 'Bad request. Please check your input.';
          break;
        case 401:
          message = 'Unauthorized. Please log in again.';
          break;
        case 403:
          message = 'Access denied. You don\'t have permission.';
          break;
        case 404:
          message = 'Resource not found.';
          break;
        case 500:
          message = 'Server error. Please try again later.';
          break;
        default:
          message = `Error ${error.status}: ${error.statusText || 'Unknown error'}`;
      }
    }

    this.showError(title, message);
  }

  /**
   * Show loan-specific toasts
   */
  showLoanToast(type: 'approved' | 'rejected' | 'submitted' | 'escalated', loanId: string, additionalInfo?: string): void {
    switch (type) {
      case 'approved':
        this.showSuccess(
          '🎉 Loan Approved!', 
          `Your loan application #${loanId} has been approved. ${additionalInfo || 'You will receive further instructions via email.'}`
        );
        break;
      case 'rejected':
        this.showError(
          'Loan Application Declined', 
          `Your loan application #${loanId} has been declined. ${additionalInfo || 'Please check your email for more details.'}`
        );
        break;
      case 'submitted':
        this.showSuccess(
          'Application Submitted!', 
          `Your loan application #${loanId} has been submitted successfully. ${additionalInfo || 'You will receive updates via email.'}`
        );
        break;
      case 'escalated':
        this.showInfo(
          'Application Under Review', 
          `Your loan application #${loanId} has been escalated for compliance review. ${additionalInfo || 'This may take additional time.'}`
        );
        break;
    }
  }

  /**
   * Show document-related toasts
   */
  showDocumentToast(type: 'uploaded' | 'verified' | 'rejected' | 'resubmission', documentType: string, additionalInfo?: string): void {
    switch (type) {
      case 'uploaded':
        this.showSuccess(
          'Document Uploaded', 
          `Your ${documentType} has been uploaded successfully. ${additionalInfo || 'It will be reviewed shortly.'}`
        );
        break;
      case 'verified':
        this.showSuccess(
          '✅ Document Verified', 
          `Your ${documentType} has been verified successfully. ${additionalInfo || ''}`
        );
        break;
      case 'rejected':
        this.showWarning(
          'Document Needs Attention', 
          `Your ${documentType} requires resubmission. ${additionalInfo || 'Please check the requirements and upload again.'}`
        );
        break;
      case 'resubmission':
        this.showInfo(
          'Document Resubmission Required', 
          `Please resubmit your ${documentType}. ${additionalInfo || 'Check your email for specific instructions.'}`
        );
        break;
    }
  }

  /**
   * Show system maintenance or security toasts
   */
  showSystemToast(type: 'maintenance' | 'security' | 'update', message: string): void {
    switch (type) {
      case 'maintenance':
        this.showWarning('🔧 System Maintenance', message, 25000);
        break;
      case 'security':
        this.showError('🔒 Security Alert', message, 30000);
        break;
      case 'update':
        this.showInfo('📢 System Update', message, 20000);
        break;
    }
  }
}
