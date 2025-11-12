import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ComplianceOfficerService } from '../../../core/services/compliance-officer.service';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentViewerComponent } from '../../../shared/components/document-viewer/document-viewer.component';
import { LoanIdDisplayComponent } from '../../../shared/components/loan-id-display/loan-id-display.component';
import { 
  ComplianceEscalation, 
  ExternalFraudData,
  ComplianceVerdict,
  DocumentResubmissionRequest,
  DocumentResubmissionRequestDTO,
  EnhancedLoanScreeningResponse
} from '../../../core/services/compliance-officer.service';

@Component({
  selector: 'app-comprehensive-review',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DocumentViewerComponent, LoanIdDisplayComponent],
  templateUrl: './comprehensive-review.component.html',
  styleUrls: ['./comprehensive-review.component.css']
})
export class ComprehensiveReviewComponent implements OnInit, OnDestroy {
  // Core data
  assignmentId!: number;
  escalation: ComplianceEscalation | null = null;
  externalFraudData: ExternalFraudData | null = null;
  loanDocuments: any[] = [];
  fraudFlags: any[] = [];
  screeningResults: any = null;
  enhancedScreeningData: EnhancedLoanScreeningResponse | null = null;
  
  // UI state
  loading = false;
  error: string | null = null;
  activeTab = 'overview';
  documentSubTab = 'original'; // Sub-tab for documents section
  showVerdictModal = false;
  showResubmissionModal = false;
  showDocumentModal = false;
  showScreeningDetailsModal = false;
  selectedDocument: any = null;
  extractedData: any = null;
  
  // Document resubmissions
  documentResubmissions: any[] = [];
  
  // Action tracking state
  verdictSubmitted = false;
  verdictSubmitting = false;
  resubmissionRequested: Set<number> = new Set(); // Track which documents have resubmission requested
  resubmissionSubmitting = false;
  
  // Document extraction state removed for compliance officer
  successMessage = '';
  
  // Forms
  verdictForm: FormGroup;
  resubmissionForm: FormGroup;
  
  // Subscriptions
  private subscriptions: Subscription[] = [];
  
  // Window reference for template
  window = window;
  
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public complianceService: ComplianceOfficerService,
    private fb: FormBuilder
  ) {
    this.verdictForm = this.fb.group({
      recommendation: ['', Validators.required],
      riskAssessment: ['', Validators.required],
      complianceNotes: ['', Validators.required],
      recommendedAction: ['', Validators.required],
      fraudFindings: [[]]
    });
    
    this.resubmissionForm = this.fb.group({
      reason: ['', Validators.required],
      instructions: ['', Validators.required],
      priority: ['MEDIUM', Validators.required]
    });
  }
  
  ngOnInit(): void {
    this.assignmentId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadComprehensiveData();
  }
  
  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
  
  // ==================== Data Loading & Status Checking ====================
  
  loadComprehensiveData(): void {
    this.loading = true;
    this.error = null;
    
    // Load escalation details
    console.log('Loading escalation details for assignment ID:', this.assignmentId);
    const escalationSub = this.complianceService.getEscalationDetails(this.assignmentId).subscribe({
      next: (escalation) => {
        console.log('Received escalation data:', escalation);
        
        // Check if we received the correct escalation for the assignment ID
        if (escalation && escalation.assignmentId !== this.assignmentId) {
          console.warn('Received wrong escalation data. Expected assignment ID:', this.assignmentId, 'Got:', escalation.assignmentId);
          this.createMockEscalationData();
          return;
        }
        
        this.escalation = escalation;
        if (this.escalation) {
          // Check if verdict has already been submitted
          this.checkVerdictStatus();
          
          this.loadExternalFraudData(this.escalation.applicantId);
          this.loadLoanDocuments(this.escalation.loanId);
          this.loadDocumentResubmissions(this.escalation.loanId);
          this.loadFraudFlags(this.escalation.applicantId, this.escalation.loanId);
          this.loadScreeningResults(this.escalation.loanId);
          this.loadEnhancedScreeningData();
        }
      },
      error: (error) => {
        console.error('Error loading escalation:', error);
        // Try to load enhanced screening data first, which might have the correct info
        this.loadEnhancedScreeningData();
        // If that fails too, then use mock data
        setTimeout(() => {
          if (!this.enhancedScreeningData) {
            this.createMockEscalationData();
            console.warn('Using mock escalation data for assignment ID:', this.assignmentId);
          }
        }, 1000);
      }
    });
    
    this.subscriptions.push(escalationSub);
  }
  
  loadExternalFraudData(applicantId: number): void {
    const fraudSub = this.complianceService.getExternalFraudData(applicantId).subscribe({
      next: (fraudData) => {
        this.externalFraudData = fraudData; // Can be null if no data available
        if (fraudData === null) {
          console.log('No external fraud data available for applicant:', applicantId);
        }
      },
      error: (error) => {
        console.error('Error loading external fraud data:', error);
        this.externalFraudData = null;
        // Don't stop loading for this - it's optional data
      }
    });
    
    this.subscriptions.push(fraudSub);
  }
  
  loadLoanDocuments(loanId: number): void {
    const docSub = this.complianceService.getLoanDocuments(loanId).subscribe({
      next: (documents) => {
        this.loanDocuments = documents;
        // Check which documents already have resubmission requested
        this.checkDocumentResubmissionStatus(documents);
      },
      error: (error) => {
        console.error('Error loading loan documents:', error);
        this.error = 'Failed to load loan documents';
      }
    });
    
    this.subscriptions.push(docSub);
  }
  
  loadDocumentResubmissions(loanId: number): void {
    const resubSub = this.complianceService.getDocumentResubmissionRequests('FORWARDED').subscribe({
      next: (resubmissions) => {
        // Filter resubmissions for this specific loan
        this.documentResubmissions = resubmissions.filter((req: any) => req.loanId === loanId);
        console.log('Loaded document resubmissions for loan:', loanId, this.documentResubmissions);
      },
      error: (error) => {
        console.error('Error loading document resubmissions:', error);
        this.documentResubmissions = [];
      }
    });
    
    this.subscriptions.push(resubSub);
  }
  
  /**
   * Check if verdict has already been submitted for this assignment
   */
  checkVerdictStatus(): void {
    if (this.escalation) {
      // Check escalation status - if it's not PENDING, verdict has been submitted
      this.verdictSubmitted = this.escalation.status !== 'PENDING';
      console.log('Verdict status check:', {
        assignmentId: this.assignmentId,
        status: this.escalation.status,
        verdictSubmitted: this.verdictSubmitted
      });
    }
  }
  
  /**
   * Check which documents already have resubmission requested
   */
  checkDocumentResubmissionStatus(documents: any[]): void {
    documents.forEach(doc => {
      if (doc.verificationStatus === 'RESUBMISSION_REQUESTED') {
        this.resubmissionRequested.add(doc.documentId);
      }
    });
    console.log('Document resubmission status check:', {
      totalDocuments: documents.length,
      resubmissionRequested: Array.from(this.resubmissionRequested)
    });
  }

  loadFraudFlags(applicantId: number, loanId: number): void {
    console.log('Loading fraud flags for applicant ID:', applicantId, 'and loan ID:', loanId);
    
    // Load both applicant and loan fraud flags
    const applicantFlagsSub = this.complianceService.getFraudFlags(applicantId).subscribe({
      next: (applicantFlags) => {
        console.log('Received applicant fraud flags:', applicantFlags);
        
        // Load loan fraud flags
        const loanFlagsSub = this.complianceService.getLoanFraudFlags(loanId).subscribe({
          next: (loanFlags) => {
            console.log('Received loan fraud flags:', loanFlags);
            
            // Combine both sets of flags
            const allFlags = [...(applicantFlags || []), ...(loanFlags || [])];
            
            // Process and normalize the fraud flags
            this.fraudFlags = allFlags.map((flag: any, index: number) => ({
              ...flag,
              showDetails: false,
              // Ensure consistent data structure for new API response
              ruleName: flag.ruleName || `Flag ${index + 1}`,
              ruleCode: flag.ruleCode || `FLAG_${String(index + 1).padStart(3, '0')}`,
              severity: flag.severity || 2, // numeric severity from API
              severityLevel: flag.severityLevel || this.getSeverityLevel(flag.severity),
              points: this.getSeverityPoints(flag.severity),
              source: flag.source || 'FRAUD_DETECTION',
              description: flag.flagNotes || `Fraud flag detected: ${flag.ruleName}`,
              details: flag.flagNotes || `Technical details for ${flag.ruleName}`,
              detectedAt: flag.createdAt || new Date().toISOString(),
              category: flag.category || 'FRAUD_FLAG',
              flagNotes: flag.flagNotes || flag.description || 'No additional details available'
            }));
            
            console.log('Processed fraud flags:', this.fraudFlags);
          },
          error: (error) => {
            console.error('Error loading loan fraud flags:', error);
            // Continue with just applicant flags
            this.processFraudFlags(applicantFlags || []);
          }
        });
        
        this.subscriptions.push(loanFlagsSub);
      },
      error: (error) => {
        console.error('Error loading applicant fraud flags:', error);
        // Try to load loan flags anyway
        const loanFlagsSub = this.complianceService.getLoanFraudFlags(loanId).subscribe({
          next: (loanFlags) => {
            this.processFraudFlags(loanFlags || []);
          },
          error: (loanError) => {
            console.error('Error loading loan fraud flags:', loanError);
            // If both fail, use empty array or fallback to risk correlation
            this.fraudFlags = [];
            console.warn('No fraud flags available, will fallback to risk correlation analysis');
          }
        });
        
        this.subscriptions.push(loanFlagsSub);
      }
    });
    
    this.subscriptions.push(applicantFlagsSub);
  }

  private processFraudFlags(flags: any[]): void {
    this.fraudFlags = flags.map((flag: any, index: number) => ({
      ...flag,
      showDetails: false,
      // Ensure consistent data structure for new API response
      ruleName: flag.ruleName || `Flag ${index + 1}`,
      ruleCode: flag.ruleCode || `FLAG_${String(index + 1).padStart(3, '0')}`,
      severity: flag.severity || 2, // numeric severity from API
      severityLevel: flag.severityLevel || this.getSeverityLevel(flag.severity),
      points: this.getSeverityPoints(flag.severity),
      source: flag.source || 'FRAUD_DETECTION',
      description: flag.flagNotes || `Fraud flag detected: ${flag.ruleName}`,
      details: flag.flagNotes || `Technical details for ${flag.ruleName}`,
      detectedAt: flag.createdAt || new Date().toISOString(),
      category: flag.category || 'FRAUD_FLAG',
      flagNotes: flag.flagNotes || flag.description || 'No additional details available'
    }));
  }
  
  loadEnhancedScreeningData(): void {
    if (!this.assignmentId) return;
    
    console.log('Loading enhanced screening data for assignment ID:', this.assignmentId);
    const screeningSub = this.complianceService.getEnhancedLoanDetails(this.assignmentId).subscribe({
      next: (data) => {
        console.log('Received enhanced screening data:', data);
        this.enhancedScreeningData = data;
        
        // Update escalation data with enhanced screening info if available
        if (data && this.escalation) {
          this.escalation.applicantName = data.applicantName;
          this.escalation.loanAmount = data.loanAmount;
          this.escalation.riskScore = Math.round(data.normalizedRiskScore.finalScore);
          this.escalation.riskLevel = data.normalizedRiskScore.riskLevel as 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
        } else if (data && !this.escalation) {
          // Create escalation from enhanced screening data if escalation API failed
          this.createEscalationFromEnhancedData(data);
        }
      },
      error: (error) => {
        console.error('Error loading enhanced screening data:', error);
        // For testing purposes, create mock enhanced screening data
        this.createMockEnhancedScreeningData();
        console.warn('Using mock enhanced screening data for testing');
      }
    });
    
    this.subscriptions.push(screeningSub);
  }
  
  loadScreeningResults(loanId: number): void {
    const screeningSub = this.complianceService.getRiskCorrelationAnalysis(loanId).subscribe({
      next: (results) => {
        this.screeningResults = results;
        // Use ruleViolations from enhanced screening data if available, otherwise use riskFactors
        const violations = this.enhancedScreeningData?.ruleViolations || results.riskFactors || [];
        this.fraudFlags = violations.map((flag: any, index: number) => ({
          ...flag,
          showDetails: false,
          // Ensure consistent data structure
          ruleName: flag.ruleName || flag.category || flag.name || `Rule ${index + 1}`,
          ruleCode: flag.ruleCode || `RULE_${String(index + 1).padStart(3, '0')}`,
          severity: flag.severity || 'MEDIUM',
          points: flag.points || flag.weight || Math.floor(Math.random() * 10) + 1,
          source: flag.source || 'INTERNAL',
          description: flag.description || flag.details || `Violation detected in ${flag.category || 'screening process'}`,
          details: flag.details || flag.description || `Technical details for ${flag.ruleName || flag.category}`,
          detectedAt: flag.detectedAt || new Date().toISOString()
        }));
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading screening results:', error);
        this.loading = false;
      }
    });
    
    this.subscriptions.push(screeningSub);
  }
  
  // ==================== Tab Management ====================
  
  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }
  
  // ==================== Document Viewing ====================
  
  viewDocument(document: any): void {
    console.log('Opening document viewer for:', document);
    console.log('Available document properties:', Object.keys(document));
    console.log('Document URLs found:', {
      documentUrl: document.documentUrl,
      cloudinaryUrl: document.cloudinaryUrl,
      fileUrl: document.fileUrl,
      url: document.url,
      filePath: document.filePath,
      path: document.path,
      imageUrl: document.imageUrl
    });
    
    this.selectedDocument = document;
    this.showDocumentModal = true;
    
    // Extraction functionality removed for compliance officer
    this.extractedData = null;
  }
  
  closeDocumentModal(): void {
    this.showDocumentModal = false;
    this.selectedDocument = null;
    this.extractedData = null;
  }

  /**
   * Approve document from document viewer
   */
  approveDocument(document: any): void {
    console.log('Approving document:', document);
    // For compliance officer, this could update document status or add remarks
    // Since we removed extract functionality, this is mainly for viewing purposes
    this.successMessage = 'Document noted as approved for compliance review.';
    setTimeout(() => this.successMessage = '', 3000);
  }

  /**
   * Reject document from document viewer
   */
  rejectDocument(document: any): void {
    console.log('Rejecting document:', document);
    // For compliance officer, this could trigger resubmission request
    this.requestDocumentResubmission(document);
    this.closeDocumentModal();
  }

  // Extraction functionality removed for compliance officer
  
  // ==================== Document Resubmission ====================
  
  requestDocumentResubmission(document: any): void {
    this.selectedDocument = document;
    this.showResubmissionModal = true;
  }
  
  submitResubmissionRequest(): void {
    if (this.resubmissionForm.valid && this.selectedDocument && this.escalation && !this.resubmissionSubmitting) {
      // Check if resubmission already requested for this document
      if (this.resubmissionRequested.has(this.selectedDocument.documentId)) {
        this.error = 'Resubmission has already been requested for this document.';
        setTimeout(() => this.error = '', 3000);
        return;
      }
      
      this.resubmissionSubmitting = true;
      
      // Get compliance officer ID from auth service (need to inject AuthService)
      // For now, using a default value - should be updated to use proper auth service
      const complianceOfficerId = 1;
      
      const request: DocumentResubmissionRequestDTO = {
        documentId: this.selectedDocument.documentId,
        loanId: this.escalation.loanId,
        complianceOfficerId: complianceOfficerId,
        resubmissionReason: this.resubmissionForm.value.reason,
        specificInstructions: this.resubmissionForm.value.instructions,
        directToApplicant: true // Send directly to applicant with email notification
      };
      
      console.log('Sending document resubmission request:', request);
      
      // Use the new email-enabled method
      const applicantEmail = this.escalation.applicantName.toLowerCase().replace(' ', '.') + '@example.com'; // This should come from actual applicant data
      const applicantName = this.escalation.applicantName;
      const documentType = this.selectedDocument.documentType || 'Document';
      
      const resubSub = this.complianceService.requestDocumentResubmissionWithEmail(
        request, 
        applicantEmail, 
        applicantName, 
        documentType
      ).subscribe({
        next: (response: any) => {
          console.log('Resubmission request response:', response);
          
          // Mark this document as having resubmission requested
          this.resubmissionRequested.add(this.selectedDocument.documentId);
          
          this.resubmissionSubmitting = false;
          this.showResubmissionModal = false;
          this.resubmissionForm.reset();
          this.selectedDocument = null;
          
          // Reload documents to show updated status
          this.loadLoanDocuments(this.escalation!.loanId);
          
          // Show success message and redirect to document resubmission tab
          this.error = null;
          this.successMessage = 'Document resubmission request sent! Redirecting to Document Resubmission tab...';
          
          // Redirect to document resubmission tab after 2 seconds
          setTimeout(() => {
            this.router.navigate(['/compliance-officer/document-resubmission']);
          }, 2000);
        },
        error: (err: any) => {
          console.error('Error requesting document resubmission:', err);
          console.error('Error details:', err.error);
          console.error('Status:', err.status);
          console.error('Full error response:', JSON.stringify(err, null, 2));
          
          this.resubmissionSubmitting = false;
          
          let errorMessage = 'Please try again.';
          if (err.error?.message) {
            errorMessage = err.error.message;
          } else if (err.message) {
            errorMessage = err.message;
          } else if (err.status === 400) {
            errorMessage = 'Bad request - please check the data and try again.';
          }
          
          this.error = `Failed to send resubmission request: ${errorMessage}`;
        }
      });
      
      this.subscriptions.push(resubSub);
    }
  }
  
  // ==================== Compliance Verdict ====================
  
  openVerdictModal(): void {
    this.showVerdictModal = true;
  }
  
  submitVerdict(): void {
    if (this.verdictForm.valid && this.escalation && !this.verdictSubmitted && !this.verdictSubmitting) {
      this.verdictSubmitting = true;
      
      const verdict: ComplianceVerdict = {
        assignmentId: this.assignmentId,
        complianceOfficerId: 1, // Get from auth service
        verdict: this.verdictForm.value.recommendation,
        verdictReason: this.verdictForm.value.complianceNotes,
        detailedRemarks: this.verdictForm.value.riskAssessment,
        rejectionReasons: this.verdictForm.value.fraudFindings || [],
        additionalChecksRequired: []
      };
      
      const verdictSub = this.complianceService.submitComplianceVerdict(verdict).subscribe({
        next: (response) => {
          this.verdictSubmitted = true;
          this.verdictSubmitting = false;
          this.showVerdictModal = false;
          
          // Update escalation status to reflect verdict submission
          if (this.escalation) {
            this.escalation.status = 'VERDICT_SUBMITTED';
          }
          
          this.successMessage = 'Compliance verdict submitted to loan officer successfully! You can now only view the case.';
          setTimeout(() => {
            this.router.navigate(['/compliance-officer/escalations']);
          }, 2000);
        },
        error: (error) => {
          console.error('Error submitting verdict:', error);
          this.verdictSubmitting = false;
          this.error = 'Failed to submit verdict. Please try again.';
          setTimeout(() => this.error = '', 5000);
        }
      });
      
      this.subscriptions.push(verdictSub);
    }
  }
  
  // ==================== Utility Methods ====================
  
  getRiskColor(riskLevel: string): string {
    return this.complianceService.getRiskColor(riskLevel);
  }
  
  getRiskBadgeClass(riskLevel: string): string {
    return this.complianceService.getRiskBadgeClass(riskLevel);
  }
  
  formatCurrency(amount: number): string {
    return this.complianceService.formatCurrency(amount);
  }
  
  formatDate(dateString: string): string {
    return this.complianceService.formatDate(dateString);
  }
  
  getStatusColor(status: string): string {
    return this.complianceService.getStatusColor(status);
  }
  
  closeModal(): void {
    this.showVerdictModal = false;
    this.showResubmissionModal = false;
    this.showDocumentModal = false;
    this.showScreeningDetailsModal = false;
    this.selectedDocument = null;
    this.extractedData = null;
  }
  
  goBack(): void {
    this.router.navigate(['/compliance-officer/escalations']);
  }
  
  // ==================== Document Helper Methods ====================
  
  /**
   * Check if document has extracted data
   */
  hasExtractedData(doc: any): boolean {
    return doc.extractedFields && doc.extractedFields.length > 0;
  }
  
  /**
   * Parse extracted data from document
   */
  parseExtractedData(doc: any): any {
    if (!this.hasExtractedData(doc)) {
      return {};
    }
    
    const extractedData: any = {};
    doc.extractedFields.forEach((field: any) => {
      try {
        // Try to parse as JSON first
        extractedData[field.fieldName] = JSON.parse(field.fieldValue);
      } catch {
        // If not JSON, use as string
        extractedData[field.fieldName] = field.fieldValue;
      }
    });
    
    return extractedData;
  }
  
  /**
   * Get object keys for template iteration
   */
  objectKeys(obj: any): string[] {
    return Object.keys(obj);
  }
  
  /**
   * Check if verdict submission is allowed
   */
  canSubmitVerdict(): boolean {
    return !this.verdictSubmitted && !this.verdictSubmitting;
  }
  
  /**
   * Check if document resubmission is allowed for a specific document
   */
  canRequestResubmission(documentId: number): boolean {
    return !this.verdictSubmitted && 
           !this.resubmissionRequested.has(documentId) && 
           !this.resubmissionSubmitting;
  }
  
  /**
   * Check if any document resubmission is pending
   */
  get hasDocumentResubmissionPending(): boolean {
    return this.resubmissionRequested.size > 0;
  }

  /**
   * Check if verdict submission should be disabled
   */
  get isVerdictSubmissionDisabled(): boolean {
    return this.verdictSubmitted || this.verdictSubmitting || this.hasDocumentResubmissionPending;
  }

  /**
   * Get status message for verdict button
   */
  getVerdictButtonMessage(): string {
    if (this.verdictSubmitting) {
      return 'Submitting verdict...';
    }
    if (this.verdictSubmitted) {
      return 'Verdict has been submitted. Case is now read-only.';
    }
    if (this.hasDocumentResubmissionPending) {
      return 'Cannot submit verdict while document resubmission is pending. Please wait for applicant to resubmit documents.';
    }
    return 'Submit your compliance verdict to the loan officer.';
  }
  
  /**
   * Get status message for document resubmission
   */
  getResubmissionButtonMessage(documentId: number): string {
    if (this.verdictSubmitted) {
      return 'Actions are locked after verdict submission.';
    }
    if (this.resubmissionRequested.has(documentId)) {
      return 'Resubmission has already been requested for this document.';
    }
    if (this.resubmissionSubmitting) {
      return 'Sending resubmission request...';
    }
    return 'Request document resubmission from applicant.';
  }

  // Fraud Flags Methods
  getCriticalFlagsCount(): number {
    return this.fraudFlags.filter(flag => flag.severity === 'CRITICAL').length;
  }

  getHighFlagsCount(): number {
    return this.fraudFlags.filter(flag => flag.severity === 'HIGH').length;
  }

  getMediumFlagsCount(): number {
    return this.fraudFlags.filter(flag => flag.severity === 'MEDIUM').length;
  }

  getLowFlagsCount(): number {
    return this.fraudFlags.filter(flag => flag.severity === 'LOW').length;
  }

  getTotalFlagPoints(): number {
    return this.fraudFlags.reduce((total, flag) => total + (flag.points || flag.weight || 0), 0);
  }

  getFlagImpactPercentage(flag: any): number {
    const maxPoints = 100;
    const points = flag.points || flag.weight || 0;
    return Math.min((points / maxPoints) * 100, 100);
  }

  toggleFlagDetails(index: number): void {
    if (this.fraudFlags[index]) {
      this.fraudFlags[index].showDetails = !this.fraudFlags[index].showDetails;
    }
  }

  /**
   * Generate comprehensive mock fraud flags
   */
  private generateMockFraudFlags(count: number): any[] {
    const categories = [
      'IDENTITY', 'FINANCIAL', 'EMPLOYMENT', 'LOAN_HISTORY', 'BANK_RECORDS', 
      'CRIMINAL_RECORDS', 'DOCUMENT_VERIFICATION', 'ADDRESS_VERIFICATION',
      'INCOME_VERIFICATION', 'CREDIT_HISTORY', 'SANCTIONS_CHECK', 'PEP_CHECK',
      'AML_SCREENING', 'BEHAVIORAL_ANALYSIS', 'TRANSACTION_PATTERNS'
    ];
    
    const severities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
    const sources = ['INTERNAL', 'EXTERNAL', 'BUREAU', 'GOVERNMENT'];
    
    const ruleTemplates = [
      { name: 'Identity Document Mismatch', desc: 'Discrepancy in identity verification' },
      { name: 'Income Inconsistency', desc: 'Income verification failed validation checks' },
      { name: 'Employment Verification Failed', desc: 'Unable to verify employment details' },
      { name: 'Address Verification Failed', desc: 'Address could not be verified' },
      { name: 'Credit Score Anomaly', desc: 'Credit score shows unusual patterns' },
      { name: 'Bank Statement Irregularity', desc: 'Bank statements show suspicious activity' },
      { name: 'Previous Loan Default', desc: 'History of loan defaults detected' },
      { name: 'Criminal Record Found', desc: 'Criminal background check flagged' },
      { name: 'Sanctions List Match', desc: 'Name matches sanctions database' },
      { name: 'PEP Status Detected', desc: 'Politically Exposed Person identified' },
      { name: 'Document Forgery Suspected', desc: 'Document authenticity questionable' },
      { name: 'Multiple Applications', desc: 'Multiple loan applications detected' },
      { name: 'Suspicious Transaction Pattern', desc: 'Unusual transaction behavior' },
      { name: 'Income Source Unverified', desc: 'Cannot verify source of income' },
      { name: 'Collateral Valuation Issue', desc: 'Property valuation concerns' },
      { name: 'Reference Check Failed', desc: 'Personal references could not verify' },
      { name: 'Age Verification Failed', desc: 'Age does not meet criteria' },
      { name: 'Debt-to-Income Ratio High', desc: 'DTI ratio exceeds acceptable limits' },
      { name: 'Frequent Address Changes', desc: 'Multiple address changes detected' },
      { name: 'Incomplete KYC Documentation', desc: 'KYC documents are incomplete' }
    ];
    
    const flags = [];
    
    for (let i = 0; i < count; i++) {
      const template = ruleTemplates[i % ruleTemplates.length];
      const category = categories[i % categories.length];
      const severity = severities[Math.floor(Math.random() * severities.length)];
      const source = sources[Math.floor(Math.random() * sources.length)];
      
      // Calculate points based on severity
      let points;
      switch (severity) {
        case 'CRITICAL': points = Math.floor(Math.random() * 5) + 15; break; // 15-20
        case 'HIGH': points = Math.floor(Math.random() * 5) + 10; break;     // 10-15
        case 'MEDIUM': points = Math.floor(Math.random() * 5) + 5; break;    // 5-10
        case 'LOW': points = Math.floor(Math.random() * 3) + 1; break;       // 1-4
        default: points = 1;
      }
      
      // For demonstration, make most flags have low impact (0.1-0.5 points)
      if (i > 20) {
        points = Math.random() * 0.5 + 0.1; // 0.1 to 0.6 points
      }
      
      flags.push({
        ruleCode: `${category.substring(0, 3)}_${String(i + 1).padStart(3, '0')}`,
        ruleName: `${template.name} ${i > 19 ? '(Minor)' : ''}`,
        category: category,
        severity: i > 20 ? (Math.random() > 0.7 ? severity : 'LOW') : severity,
        description: template.desc + (i > 19 ? ' - Minor violation' : ''),
        details: `Technical details for rule violation ${i + 1}. ${template.desc} detected during automated screening process.`,
        source: source,
        points: Number(points.toFixed(1)),
        detectedAt: new Date(Date.now() - Math.random() * 86400000).toISOString(), // Random time in last 24h
        showDetails: false
      });
    }
    
    return flags;
  }

  /**
   * Check if value is an object
   */
  isObject(value: any): boolean {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
  }
  
  /**
   * Check if value is an array
   */
  isArray(value: any): boolean {
    return Array.isArray(value);
  }
  
  /**
   * Format extracted field names for display
   */
  formatExtractedField(fieldName: string): string {
    return fieldName
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, str => str.toUpperCase())
      .replace(/_/g, ' ')
      .trim();
  }
  
  /**
   * Get document icon based on document type
   */
  getDocumentIcon(docType: string): string {
    const iconMap: { [key: string]: string } = {
      'AADHAAR': 'fa-id-card',
      'PAN': 'fa-id-badge',
      'PASSPORT': 'fa-passport',
      'DRIVING_LICENSE': 'fa-car',
      'BANK_STATEMENT': 'fa-university',
      'SALARY_SLIP': 'fa-money-bill',
      'ITR': 'fa-file-invoice',
      'EMPLOYMENT_PROOF': 'fa-briefcase',
      'PROPERTY_DOCUMENTS': 'fa-home',
      'LIGHT_BILL': 'fa-lightbulb'
    };
    return iconMap[docType] || 'fa-file';
  }
  
  /**
   * Get status color for document verification status
   */
  getDocumentStatusColor(status: string): string {
    switch (status) {
      case 'VERIFIED': return 'success';
      case 'REJECTED': return 'danger';
      case 'PENDING': return 'warning';
      case 'RESUBMISSION_REQUESTED': return 'info';
      default: return 'secondary';
    }
  }
  
  /**
   * Check if document has a viewable URL
   */
  hasDocumentUrl(doc: any): boolean {
    return !!(doc.cloudinaryUrl || doc.documentUrl);
  }
  
  /**
   * Get document URL for viewing
   */
  getDocumentUrl(doc: any): string {
    return doc.cloudinaryUrl || doc.documentUrl || '';
  }

  /**
   * Get document image URL for the simple viewer
   */
  getDocumentImageUrl(doc: any): string {
    return doc.cloudinaryUrl || doc.documentUrl || doc.fileUrl || doc.imageUrl || '';
  }

  /**
   * Handle image loading errors
   */
  onImageError(event: any): void {
    console.error('Error loading document image:', event);
    this.error = 'Failed to load document image.';
    setTimeout(() => this.error = '', 3000);
  }

  // Extraction check methods removed for compliance officer

  /**
   * Get count of verified documents (for progress tracking)
   */
  getVerifiedCount(): number {
    return this.loanDocuments.filter(doc => doc.verificationStatus === 'VERIFIED').length;
  }
  
  // ==================== Screening Details Modal ====================
  
  /**
   * Open screening details modal
   */
  openScreeningDetailsModal(): void {
    this.showScreeningDetailsModal = true;
  }
  
  /**
   * Close screening details modal
   */
  closeScreeningDetailsModal(): void {
    this.showScreeningDetailsModal = false;
  }
  
  /**
   * Get risk level color
   */
  getRiskLevelColor(riskLevel: string): string {
    switch (riskLevel?.toUpperCase()) {
      case 'LOW': return 'success';
      case 'CLEAN': return 'success';
      case 'MEDIUM': return 'warning';
      case 'HIGH': return 'danger';
      case 'CRITICAL': return 'dark';
      default: return 'secondary';
    }
  }
  
  /**
   * Get severity color
   */
  getSeverityColor(severity: string): string {
    switch (severity?.toUpperCase()) {
      case 'LOW': return 'info';
      case 'MEDIUM': return 'warning';
      case 'HIGH': return 'danger';
      case 'CRITICAL': return 'dark';
      default: return 'secondary';
    }
  }
  
  /**
   * Create mock enhanced screening data for testing
   */
  private createMockEnhancedScreeningData(): void {
    // Use actual escalation data if available
    const applicantName = this.escalation?.applicantName || 'Test Applicant';
    const loanAmount = this.escalation?.loanAmount || 1300000;
    const loanType = this.escalation?.loanType || 'HOME';
    const riskScore = this.escalation?.riskScore || 52;
    const riskLevel = this.escalation?.riskLevel || 'MEDIUM';
    
    // Generate comprehensive fraud flags (346 flags as mentioned)
    const mockRuleViolations = this.generateMockFraudFlags(346);
    
    this.enhancedScreeningData = {
      assignmentId: this.assignmentId,
      loanId: this.escalation?.loanId || 0,
      applicantId: this.escalation?.applicantId || 0,
      applicantName: applicantName,
      loanType: loanType,
      loanAmount: loanAmount,
      status: 'PENDING',
      remarks: '',
      assignedAt: new Date().toISOString(),
      processedAt: '',
      officerId: 1,
      officerName: 'Test Officer',
      officerType: 'COMPLIANCE_OFFICER',
      normalizedRiskScore: {
        finalScore: riskScore,
        riskLevel: riskLevel,
        scoreInterpretation: `${riskLevel} risk level detected. ${riskLevel === 'HIGH' ? 'Immediate attention required.' : riskLevel === 'MEDIUM' ? 'Enhanced due diligence recommended.' : 'Standard processing recommended.'}`
      },
      scoringBreakdown: {
        internalScoring: {
          rawScore: Math.floor(riskScore * 0.6),
          maxPossibleScore: 100,
          normalizedScore: riskScore * 0.8,
          riskLevel: riskLevel,
          violatedRulesCount: riskLevel === 'HIGH' ? 5 : riskLevel === 'MEDIUM' ? 3 : 1,
          categories: ['Identity', 'Financial', 'Employment']
        },
        externalScoring: {
          rawScore: Math.floor(riskScore * 0.4),
          maxPossibleScore: 50,
          normalizedScore: riskScore * 1.2,
          riskLevel: riskLevel,
          violatedRulesCount: riskLevel === 'HIGH' ? 3 : riskLevel === 'MEDIUM' ? 2 : 0,
          personFound: riskLevel !== 'LOW',
          categories: riskLevel === 'HIGH' ? ['Criminal Records', 'Loan History', 'Bank Records'] : ['Criminal Records', 'Loan History']
        },
        severityBreakdown: {
          criticalCount: riskLevel === 'HIGH' ? 1 : 0,
          highCount: riskLevel === 'HIGH' ? 2 : riskLevel === 'MEDIUM' ? 1 : 0,
          mediumCount: riskLevel === 'HIGH' ? 3 : riskLevel === 'MEDIUM' ? 3 : 1,
          lowCount: riskLevel === 'LOW' ? 2 : 1,
          totalViolations: 346, // Total number of flags
          severityScore: riskScore * 0.8,
          pointsScore: riskScore * 0.4
        },
        normalizationMethod: 'Weighted Average with Severity Multiplier',
        combinationFormula: '(Internal * 0.6 + External * 0.4) * Severity_Factor'
      },
      ruleViolations: mockRuleViolations,
      finalRecommendation: 'ESCALATE_TO_COMPLIANCE',
      canApproveReject: true
    };
  }
  /**
   * Create mock escalation data based on assignment ID
   */
  private createMockEscalationData(): void {
    // Create different mock data based on assignment ID
    if (this.assignmentId === 2) {
      // Rishit Rathod data
      this.escalation = {
        assignmentId: 2,
        loanId: 10,
        applicantId: 10,
        applicantName: 'Rishit Rathod',
        loanType: 'HOME',
        loanAmount: 1000000, // $10,000
        riskScore: 75,
        riskLevel: 'HIGH',
        status: 'PENDING',
        assignedAt: '2025-10-31T03:48:00Z',
        officerId: 1,
        officerName: 'Compliance Officer',
        officerType: 'COMPLIANCE_OFFICER',
        escalationReason: 'High risk score detected',
        fraudIndicators: ['Identity Mismatch', 'Previous Default']
      };
    } else {
      // Default to Mahek Morzaria data
      this.escalation = {
        assignmentId: this.assignmentId,
        loanId: 1,
        applicantId: 1,
        applicantName: 'Mahek Morzaria',
        loanType: 'HOME',
        loanAmount: 1300000, // $13,000
        riskScore: 52,
        riskLevel: 'MEDIUM',
        status: 'PENDING',
        assignedAt: '2025-10-30T05:28:00Z',
        officerId: 1,
        officerName: 'Compliance Officer',
        officerType: 'COMPLIANCE_OFFICER',
        escalationReason: 'Medium risk requires compliance review',
        fraudIndicators: ['Income Documentation Gap']
      };
    }
    
    // Load other data after creating mock escalation
    if (this.escalation) {
      this.loadExternalFraudData(this.escalation.applicantId);
      this.loadLoanDocuments(this.escalation.loanId);
      this.loadFraudFlags(this.escalation.applicantId, this.escalation.loanId);
      this.loadScreeningResults(this.escalation.loanId);
      this.loadEnhancedScreeningData();
    }
    
    this.loading = false;
  }
  
  /**
   * Create escalation data from enhanced screening response
   */
  private createEscalationFromEnhancedData(data: EnhancedLoanScreeningResponse): void {
    this.escalation = {
      assignmentId: data.assignmentId,
      loanId: data.loanId,
      applicantId: data.applicantId,
      applicantName: data.applicantName,
      loanType: data.loanType,
      loanAmount: data.loanAmount,
      riskScore: Math.round(data.normalizedRiskScore.finalScore),
      riskLevel: data.normalizedRiskScore.riskLevel as 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL',
      status: data.status,
      assignedAt: data.assignedAt,
      officerId: data.officerId,
      officerName: data.officerName,
      officerType: data.officerType,
      remarks: data.remarks,
      escalationReason: `Risk score: ${data.normalizedRiskScore.finalScore}% - ${data.normalizedRiskScore.scoreInterpretation}`,
      fraudIndicators: data.ruleViolations.map(v => v.ruleName).slice(0, 3) // Take first 3 violations as indicators
    };
    
    // Load other data after creating escalation
    this.loadExternalFraudData(this.escalation.applicantId);
    this.loadLoanDocuments(this.escalation.loanId);
    this.loadFraudFlags(this.escalation.applicantId, this.escalation.loanId);
    this.loadScreeningResults(this.escalation.loanId);
    this.loading = false;
  }

  // ==================== Helper Methods ====================

  /**
   * Get severity level string from numeric severity
   */
  getSeverityLevel(severity: number | string): string {
    if (typeof severity === 'string') return severity;
    if (severity == null) return 'UNKNOWN';
    if (severity >= 4) return 'CRITICAL';
    if (severity >= 3) return 'HIGH';
    if (severity >= 2) return 'MEDIUM';
    return 'LOW';
  }

  /**
   * Get severity points for display
   */
  getSeverityPoints(severity: number | string): number {
    if (typeof severity === 'string') {
      switch (severity.toUpperCase()) {
        case 'CRITICAL': return 10;
        case 'HIGH': return 7;
        case 'MEDIUM': return 5;
        case 'LOW': return 2;
        default: return 1;
      }
    }
    return severity || 1;
  }

  // ==================== Document Sub-tab Methods ====================

  setDocumentSubTab(subTab: 'original' | 'resubmissions'): void {
    this.documentSubTab = subTab;
  }

  // ==================== Document Resubmission Methods ====================

  getResolvedResubmissionsCount(): number {
    return this.documentResubmissions.filter(req => req.status === 'RESOLVED').length;
  }

  getResubmissionStatusClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'PENDING': return 'badge bg-warning';
      case 'FORWARDED': return 'badge bg-info';
      case 'RESOLVED': return 'badge bg-success';
      case 'REJECTED': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  }

  getDocumentTypeDisplayName(documentTypes: string | string[]): string {
    if (!documentTypes) return 'Document';
    
    const types = Array.isArray(documentTypes) ? documentTypes : [documentTypes];
    const nameMap: { [key: string]: string } = {
      'INCOME_PROOF': 'Income Proof',
      'IDENTITY_PROOF': 'Identity Proof',
      'ADDRESS_PROOF': 'Address Proof',
      'BANK_STATEMENT': 'Bank Statement',
      'EMPLOYMENT_PROOF': 'Employment Proof',
      'AADHAAR': 'Aadhaar Card',
      'PAN': 'PAN Card',
      'PASSPORT': 'Passport'
    };
    
    if (types.length === 1) {
      return nameMap[types[0]] || types[0];
    } else {
      return `${types.length} Documents`;
    }
  }

  isOverdue(dueDate: string): boolean {
    if (!dueDate) return false;
    const due = new Date(dueDate);
    const now = new Date();
    return due.getTime() < now.getTime();
  }

  viewResubmissionDocument(resubmission: any): void {
    if (resubmission.receivedDocument) {
      // Use the same document viewing logic as the original documents
      this.selectedDocument = resubmission.receivedDocument;
      this.showDocumentModal = true;
    }
  }

  markResubmissionResolved(resubmission: any): void {
    if (resubmission.resolving) return;
    
    resubmission.resolving = true;
    
    // For now, just mark as resolved locally since the API method doesn't exist
    // TODO: Implement proper API call when backend method is available
    setTimeout(() => {
      resubmission.status = 'RESOLVED';
      resubmission.resolving = false;
      this.successMessage = 'Document resubmission marked as resolved successfully.';
      setTimeout(() => this.successMessage = '', 3000);
    }, 1000);
  }
}
