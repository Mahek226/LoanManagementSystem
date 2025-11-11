import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface TourStep {
  id: string;
  title: string;
  content: string;
  target: string; // CSS selector
  placement: 'top' | 'bottom' | 'left' | 'right';
  showSkip?: boolean;
  showPrevious?: boolean;
  showNext?: boolean;
  action?: {
    label: string;
    callback: () => void;
  };
  beforeShow?: () => void;
  afterShow?: () => void;
}

export interface Tour {
  id: string;
  name: string;
  description: string;
  steps: TourStep[];
  autoStart?: boolean;
  showProgress?: boolean;
  allowSkip?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class GuidedTourService {
  private currentTour$ = new BehaviorSubject<Tour | null>(null);
  private currentStepIndex$ = new BehaviorSubject<number>(-1);
  private isActive$ = new BehaviorSubject<boolean>(false);
  private overlay?: HTMLElement;

  constructor() {}

  getCurrentTour(): Observable<Tour | null> {
    return this.currentTour$.asObservable();
  }

  getCurrentStepIndex(): Observable<number> {
    return this.currentStepIndex$.asObservable();
  }

  isActive(): Observable<boolean> {
    return this.isActive$.asObservable();
  }

  startTour(tour: Tour): void {
    this.currentTour$.next(tour);
    this.currentStepIndex$.next(0);
    this.isActive$.next(true);
    this.createOverlay();
    this.showCurrentStep();
  }

  nextStep(): void {
    const tour = this.currentTour$.value;
    const currentIndex = this.currentStepIndex$.value;
    
    if (!tour || currentIndex >= tour.steps.length - 1) {
      this.endTour();
      return;
    }

    this.currentStepIndex$.next(currentIndex + 1);
    this.showCurrentStep();
  }

  previousStep(): void {
    const currentIndex = this.currentStepIndex$.value;
    
    if (currentIndex <= 0) return;

    this.currentStepIndex$.next(currentIndex - 1);
    this.showCurrentStep();
  }

  skipTour(): void {
    this.endTour();
  }

  endTour(): void {
    this.currentTour$.next(null);
    this.currentStepIndex$.next(-1);
    this.isActive$.next(false);
    this.removeOverlay();
    this.removeHighlight();
  }

  goToStep(stepIndex: number): void {
    const tour = this.currentTour$.value;
    
    if (!tour || stepIndex < 0 || stepIndex >= tour.steps.length) return;

    this.currentStepIndex$.next(stepIndex);
    this.showCurrentStep();
  }

  private showCurrentStep(): void {
    const tour = this.currentTour$.value;
    const stepIndex = this.currentStepIndex$.value;
    
    if (!tour || stepIndex < 0 || stepIndex >= tour.steps.length) return;

    const step = tour.steps[stepIndex];
    
    // Execute beforeShow callback
    if (step.beforeShow) {
      step.beforeShow();
    }

    // Wait for DOM updates
    setTimeout(() => {
      this.highlightElement(step.target);
      this.showStepPopover(step, stepIndex, tour.steps.length);
      
      // Execute afterShow callback
      if (step.afterShow) {
        step.afterShow();
      }
    }, 100);
  }

  private highlightElement(selector: string): void {
    this.removeHighlight();
    
    const element = document.querySelector(selector) as HTMLElement;
    if (!element) {
      console.warn(`Tour target element not found: ${selector}`);
      return;
    }

    // Add highlight class
    element.classList.add('tour-highlight');
    
    // Scroll element into view
    element.scrollIntoView({
      behavior: 'smooth',
      block: 'center',
      inline: 'center'
    });

    // Update overlay to show cutout around element
    this.updateOverlay(element);
  }

  private removeHighlight(): void {
    const highlighted = document.querySelectorAll('.tour-highlight');
    highlighted.forEach(el => el.classList.remove('tour-highlight'));
  }

  private createOverlay(): void {
    if (this.overlay) return;

    this.overlay = document.createElement('div');
    this.overlay.className = 'tour-overlay';
    this.overlay.innerHTML = `
      <style>
        .tour-overlay {
          position: fixed;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          background: rgba(0, 0, 0, 0.5);
          z-index: 9998;
          pointer-events: none;
        }
        
        .tour-highlight {
          position: relative;
          z-index: 9999 !important;
          box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.5), 0 0 0 9999px rgba(0, 0, 0, 0.5) !important;
          border-radius: 4px;
        }
        
        .tour-popover {
          position: absolute;
          background: white;
          border: 1px solid #e2e8f0;
          border-radius: 12px;
          box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
          z-index: 10000;
          max-width: 320px;
          min-width: 280px;
          pointer-events: auto;
        }
        
        .tour-popover-header {
          padding: 16px 20px 12px 20px;
          border-bottom: 1px solid #f1f5f9;
        }
        
        .tour-popover-title {
          font-size: 16px;
          font-weight: 600;
          color: #1e293b;
          margin: 0 0 4px 0;
        }
        
        .tour-popover-progress {
          font-size: 12px;
          color: #64748b;
          margin: 0;
        }
        
        .tour-popover-content {
          padding: 16px 20px;
          color: #475569;
          line-height: 1.6;
        }
        
        .tour-popover-actions {
          padding: 12px 20px 16px 20px;
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 12px;
        }
        
        .tour-btn {
          padding: 8px 16px;
          border: none;
          border-radius: 6px;
          font-size: 14px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s ease;
        }
        
        .tour-btn-primary {
          background: #3b82f6;
          color: white;
        }
        
        .tour-btn-primary:hover {
          background: #2563eb;
        }
        
        .tour-btn-secondary {
          background: #f8fafc;
          color: #475569;
          border: 1px solid #e2e8f0;
        }
        
        .tour-btn-secondary:hover {
          background: #f1f5f9;
        }
        
        .tour-btn-skip {
          background: none;
          color: #64748b;
          text-decoration: underline;
          padding: 4px 8px;
        }
        
        .tour-btn-skip:hover {
          color: #475569;
        }
        
        .tour-popover-arrow {
          position: absolute;
          width: 0;
          height: 0;
          border: 8px solid transparent;
        }
        
        .tour-popover.placement-top .tour-popover-arrow {
          bottom: -16px;
          left: 50%;
          transform: translateX(-50%);
          border-top-color: white;
        }
        
        .tour-popover.placement-bottom .tour-popover-arrow {
          top: -16px;
          left: 50%;
          transform: translateX(-50%);
          border-bottom-color: white;
        }
        
        .tour-popover.placement-left .tour-popover-arrow {
          right: -16px;
          top: 50%;
          transform: translateY(-50%);
          border-left-color: white;
        }
        
        .tour-popover.placement-right .tour-popover-arrow {
          left: -16px;
          top: 50%;
          transform: translateY(-50%);
          border-right-color: white;
        }
        
        @media (max-width: 768px) {
          .tour-popover {
            max-width: 280px;
            min-width: 240px;
          }
          
          .tour-popover-header,
          .tour-popover-content,
          .tour-popover-actions {
            padding-left: 16px;
            padding-right: 16px;
          }
        }
      </style>
    `;
    
    document.body.appendChild(this.overlay);
  }

  private updateOverlay(targetElement: HTMLElement): void {
    if (!this.overlay) return;

    const rect = targetElement.getBoundingClientRect();
    
    // Create cutout effect by updating the box-shadow
    const cutoutShadow = `
      0 0 0 4px rgba(59, 130, 246, 0.5),
      0 0 0 ${rect.top}px rgba(0, 0, 0, 0.5),
      ${rect.right}px 0 0 9999px rgba(0, 0, 0, 0.5),
      0 ${rect.bottom}px 0 9999px rgba(0, 0, 0, 0.5),
      ${-rect.left}px 0 0 9999px rgba(0, 0, 0, 0.5)
    `;
    
    targetElement.style.boxShadow = cutoutShadow;
  }

  private removeOverlay(): void {
    if (this.overlay) {
      document.body.removeChild(this.overlay);
      this.overlay = undefined;
    }
  }

  private showStepPopover(step: TourStep, stepIndex: number, totalSteps: number): void {
    // Remove existing popover
    const existingPopover = document.querySelector('.tour-popover');
    if (existingPopover) {
      existingPopover.remove();
    }

    const targetElement = document.querySelector(step.target) as HTMLElement;
    if (!targetElement) return;

    const popover = document.createElement('div');
    popover.className = `tour-popover placement-${step.placement}`;
    
    const showSkip = step.showSkip !== false;
    const showPrevious = step.showPrevious !== false && stepIndex > 0;
    const showNext = step.showNext !== false;
    const isLastStep = stepIndex === totalSteps - 1;

    popover.innerHTML = `
      <div class="tour-popover-header">
        <h6 class="tour-popover-title">${step.title}</h6>
        <p class="tour-popover-progress">Step ${stepIndex + 1} of ${totalSteps}</p>
      </div>
      <div class="tour-popover-content">
        ${step.content}
      </div>
      <div class="tour-popover-actions">
        <div>
          ${showSkip ? '<button class="tour-btn tour-btn-skip" data-action="skip">Skip Tour</button>' : ''}
        </div>
        <div style="display: flex; gap: 8px;">
          ${showPrevious ? '<button class="tour-btn tour-btn-secondary" data-action="previous">Previous</button>' : ''}
          ${step.action ? `<button class="tour-btn tour-btn-primary" data-action="custom">${step.action.label}</button>` : ''}
          ${showNext ? `<button class="tour-btn tour-btn-primary" data-action="next">${isLastStep ? 'Finish' : 'Next'}</button>` : ''}
        </div>
      </div>
      <div class="tour-popover-arrow"></div>
    `;

    // Add event listeners
    popover.addEventListener('click', (e) => {
      const target = e.target as HTMLElement;
      const action = target.getAttribute('data-action');
      
      switch (action) {
        case 'skip':
          this.skipTour();
          break;
        case 'previous':
          this.previousStep();
          break;
        case 'next':
          this.nextStep();
          break;
        case 'custom':
          if (step.action?.callback) {
            step.action.callback();
          }
          break;
      }
    });

    document.body.appendChild(popover);
    this.positionPopover(popover, targetElement, step.placement);
  }

  private positionPopover(popover: HTMLElement, target: HTMLElement, placement: string): void {
    const targetRect = target.getBoundingClientRect();
    const popoverRect = popover.getBoundingClientRect();
    const viewport = {
      width: window.innerWidth,
      height: window.innerHeight
    };

    let top = 0;
    let left = 0;

    switch (placement) {
      case 'top':
        top = targetRect.top - popoverRect.height - 16;
        left = targetRect.left + (targetRect.width - popoverRect.width) / 2;
        break;
      case 'bottom':
        top = targetRect.bottom + 16;
        left = targetRect.left + (targetRect.width - popoverRect.width) / 2;
        break;
      case 'left':
        top = targetRect.top + (targetRect.height - popoverRect.height) / 2;
        left = targetRect.left - popoverRect.width - 16;
        break;
      case 'right':
        top = targetRect.top + (targetRect.height - popoverRect.height) / 2;
        left = targetRect.right + 16;
        break;
    }

    // Adjust for viewport boundaries
    if (left < 16) left = 16;
    if (left + popoverRect.width > viewport.width - 16) {
      left = viewport.width - popoverRect.width - 16;
    }
    if (top < 16) top = 16;
    if (top + popoverRect.height > viewport.height - 16) {
      top = viewport.height - popoverRect.height - 16;
    }

    popover.style.position = 'fixed';
    popover.style.top = `${top}px`;
    popover.style.left = `${left}px`;
  }

  // Predefined tours
  getApplicantOnboardingTour(): Tour {
    return {
      id: 'applicant-onboarding',
      name: 'Welcome to FraudShield',
      description: 'Learn how to navigate your loan application journey',
      showProgress: true,
      allowSkip: true,
      steps: [
        {
          id: 'welcome',
          title: 'Welcome to FraudShield!',
          content: 'Let\'s take a quick tour to help you get started with your loan application process.',
          target: '.welcome-banner',
          placement: 'bottom'
        },
        {
          id: 'navigation',
          title: 'Navigation Menu',
          content: 'Use this menu to navigate between different sections of your dashboard.',
          target: '.nav-tabs',
          placement: 'bottom'
        },
        {
          id: 'apply-loan',
          title: 'Apply for a Loan',
          content: 'Click here to start a new loan application. We\'ll guide you through each step.',
          target: '[href="/applicant/apply-loan"]',
          placement: 'bottom',
          action: {
            label: 'Start Application',
            callback: () => {
              // Navigate to loan application
              window.location.href = '/applicant/apply-loan';
            }
          }
        },
        {
          id: 'profile',
          title: 'Your Profile',
          content: 'Keep your profile information up to date for faster loan processing.',
          target: '[href="/applicant/profile"]',
          placement: 'left'
        },
        {
          id: 'applications',
          title: 'Track Applications',
          content: 'Monitor the status of your loan applications and view their progress here.',
          target: '[href="/applicant/applications"]',
          placement: 'left'
        }
      ]
    };
  }

  getLoanApplicationTour(): Tour {
    return {
      id: 'loan-application',
      name: 'Loan Application Process',
      description: 'Step-by-step guide through the loan application',
      showProgress: true,
      allowSkip: true,
      steps: [
        {
          id: 'loan-type',
          title: 'Choose Loan Type',
          content: 'Select the type of loan that best fits your needs. Each type has different requirements and terms.',
          target: '.loan-type-selection',
          placement: 'right'
        },
        {
          id: 'personal-details',
          title: 'Personal Information',
          content: 'Fill in your personal details accurately. This information will be verified during processing.',
          target: '.personal-details-form',
          placement: 'top'
        },
        {
          id: 'documents',
          title: 'Upload Documents',
          content: 'Upload clear, readable copies of all required documents. This speeds up the verification process.',
          target: '.document-upload-section',
          placement: 'left'
        },
        {
          id: 'review',
          title: 'Review & Submit',
          content: 'Review all your information carefully before submitting. You can make changes until you submit.',
          target: '.review-section',
          placement: 'top'
        }
      ]
    };
  }
}
