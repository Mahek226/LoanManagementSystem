import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { LoanOfficerService } from '../../../core/services/loan-officer.service';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentViewerComponent } from '../../../shared/components/document-viewer/document-viewer.component';
import { LoanIdDisplayComponent } from '../../../shared/components/loan-id-display/loan-id-display.component';

interface ExtractedData {
  [key: string]: any;
}

interface LoanDocument {
  documentId: number;
  documentType: string;
  documentName: string;
  originalFilename?: string;
  documentUrl?: string;
  cloudinaryUrl?: string;
  verificationStatus: string;
  uploadedAt: string;
  verifiedBy?: string;
  verifiedAt?: string;
  remarks?: string;
  [key: string]: any;
}

@Component({
  selector: 'app-verify-documents',
  standalone: true,
  imports: [CommonModule, FormsModule, DocumentViewerComponent, LoanIdDisplayComponent],
  templateUrl: './verify-documents.component.html',
  styleUrl: './verify-documents.component.css'
})
export class VerifyDocumentsComponent implements OnInit {
  assignmentId: number = 0;
  loanId: number = 0;
  officerId: number = 0;
  
  loading = false;
  extracting = false;
  error = '';
  successMessage = '';
  
  documents: LoanDocument[] = [];
  selectedDocument: LoanDocument | null = null;
  
  // Track extraction status per document
  extractionCache: Map<number, ExtractedData> = new Map();
  extractedDocumentIds: Set<number> = new Set();
  
  // Modal states
  showDocumentModal = false;
  showVerifyModal = false;
  verificationStatus = '';
  verificationRemarks = '';
  
  // Extraction state
  extractedData: any = null;
  
  // Document viewer state
  activeTab = 'parse';
  
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private loanOfficerService: LoanOfficerService,
    private authService: AuthService
  ) {
    const user = this.authService.currentUserValue;
    this.officerId = user?.officerId || user?.userId || 0;
  }

  ngOnInit(): void {
    this.assignmentId = Number(this.route.snapshot.paramMap.get('assignmentId'));
    this.loanId = Number(this.route.snapshot.queryParamMap.get('loanId'));
    
    if (this.loanId) {
      this.loadDocuments();
    } else {
      this.error = 'Loan ID not found';
    }
  }

  loadDocuments(): void {
    this.loading = true;
    this.error = '';
    
    this.loanOfficerService.getLoanDocuments(this.loanId).subscribe({
      next: (data) => {
        // Filter to show only documents that were actually uploaded
        // A document is considered uploaded if it has a URL and upload timestamp
        this.documents = data.filter(doc => 
          (doc.documentUrl || doc.cloudinaryUrl) && 
          doc.uploadedAt && 
          doc.uploadedAt !== null
        );
        
        console.log(`Filtered documents: ${this.documents.length} uploaded out of ${data.length} total document types`);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading documents:', err);
        this.error = 'Failed to load documents. Please try again.';
        this.loading = false;
      }
    });
  }

  viewDocument(document: LoanDocument): void {
    this.selectedDocument = document;
    this.showDocumentModal = true;
    
    // Load cached extraction data if available
    if (this.extractionCache.has(document.documentId)) {
      this.extractedData = this.extractionCache.get(document.documentId) || null;
    } else {
      this.extractedData = null;
    }
  }

  closeDocumentModal(): void {
    this.showDocumentModal = false;
    this.selectedDocument = null;
    this.extractedData = null;
  }

  extractDocumentData(document: LoanDocument): void {
    // Check if already extracted
    if (this.extractedDocumentIds.has(document.documentId)) {
      this.error = 'This document has already been extracted. Using cached data.';
      setTimeout(() => this.error = '', 3000);
      return;
    }
    
    this.extracting = true;
    this.error = '';
    this.extractedData = null;
    
    // Check if we have the document file
    if (!document['fileUrl'] && !document['cloudinaryUrl'] && !document['documentUrl']) {
      this.error = 'Document file not available for extraction.';
      this.extracting = false;
      setTimeout(() => this.error = '', 5000);
      return;
    }

    // For now, we need to get the file from the document URL
    // This is a simplified approach - you might need to adjust based on your document storage
    const documentUrl = document['fileUrl'] || document['cloudinaryUrl'] || document['documentUrl'];
    
    // Convert URL to File object (this is a workaround - ideally you'd have direct file access)
    fetch(documentUrl)
      .then(response => response.blob())
      .then(blob => {
        const file = new File([blob], document.documentName || 'document.jpg', { type: blob.type });
        
        // Call FastAPI extraction service
        this.loanOfficerService.extractSingleDocument(
          document['applicantId'] || 1, // Use actual applicant ID
          document.documentType,
          file
        ).subscribe({
          next: (response) => {
            console.log('FastAPI Response:', response);
            this.extractedData = response;
            
            // Cache the extracted data (only if not null)
            if (this.extractedData) {
              this.extractionCache.set(document.documentId, this.extractedData);
              this.extractedDocumentIds.add(document.documentId);
            }
            
            this.extracting = false;
            this.successMessage = 'Document data extracted successfully using AI!';
            setTimeout(() => this.successMessage = '', 3000);
          },
          error: (err) => {
            console.error('Error extracting document with FastAPI:', err);
            this.extracting = false;
            this.error = 'Failed to extract document data. Please try again.';
            setTimeout(() => this.error = '', 5000);
          }
        });
      })
      .catch(err => {
        console.error('Error fetching document file:', err);
        this.extracting = false;
        this.error = 'Failed to load document file for extraction.';
        setTimeout(() => this.error = '', 5000);
      });
  }

  openVerifyModal(document: LoanDocument, status: 'VERIFIED' | 'REJECTED'): void {
    // Prevent re-verification of already verified/rejected documents
    if (document.verificationStatus === 'VERIFIED' || document.verificationStatus === 'REJECTED') {
      this.error = `This document is already ${document.verificationStatus.toLowerCase()}. Cannot modify status.`;
      setTimeout(() => this.error = '', 3000);
      return;
    }
    
    this.selectedDocument = document;
    this.verificationStatus = status;
    this.verificationRemarks = '';
    this.showVerifyModal = true;
  }

  closeVerifyModal(): void {
    this.showVerifyModal = false;
    this.selectedDocument = null;
    this.verificationRemarks = '';
  }

  submitVerification(): void {
    if (!this.selectedDocument) return;
    
    if (this.verificationStatus === 'REJECTED' && !this.verificationRemarks.trim()) {
      this.error = 'Please provide remarks for rejection';
      return;
    }
    
    this.loading = true;
    this.error = '';
    
    const request = {
      documentId: this.selectedDocument.documentId,
      verificationStatus: this.verificationStatus,
      remarks: this.verificationRemarks
    };
    
    this.loanOfficerService.verifyDocument(this.officerId, request).subscribe({
      next: (response) => {
        this.successMessage = `Document ${this.verificationStatus.toLowerCase()} successfully!`;
        this.closeVerifyModal();
        this.loadDocuments(); // Reload documents
        this.loading = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        console.error('Error verifying document:', err);
        this.error = 'Failed to verify document. Please try again.';
        this.loading = false;
      }
    });
  }

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

  getStatusColor(status: string): string {
    switch (status) {
      case 'VERIFIED': return 'success';
      case 'REJECTED': return 'danger';
      case 'PENDING': return 'warning';
      default: return 'secondary';
    }
  }

  goBack(): void {
    this.router.navigate(['/loan-officer/assigned-loans']);
  }

  proceedToScreening(): void {
    this.router.navigate(['/loan-officer/review', this.assignmentId]);
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric'
    });
  }
  
  // Check if document has been extracted
  isExtracted(document: LoanDocument): boolean {
    return this.extractedDocumentIds.has(document.documentId);
  }
  
  // Check if document can be verified (not already verified/rejected)
  canVerify(document: LoanDocument): boolean {
    return document.verificationStatus === 'PENDING';
  }
  
  // Check if all documents are verified
  allDocumentsVerified(): boolean {
    return this.documents.length > 0 && this.documents.every(doc => doc.verificationStatus === 'VERIFIED');
  }
  
  // Get count of verified documents
  getVerifiedCount(): number {
    return this.documents.filter(doc => doc.verificationStatus === 'VERIFIED').length;
  }
  
  // ==================== Enhanced Modal Helper Methods ====================
  
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
   * Get object keys for template iteration
   */
  objectKeys(obj: any): string[] {
    return obj ? Object.keys(obj) : [];
  }

  /**
   * Get personal information fields from extracted data
   */
  getPersonalInfo(data: any): {key: string, value: any}[] {
    if (!data) return [];
    
    const personalFields = ['name', 'firstName', 'lastName', 'fullName', 'fatherName', 'motherName', 
                           'dob', 'dateOfBirth', 'gender', 'age', 'phone', 'mobile', 'email'];
    
    return Object.keys(data)
      .filter(key => personalFields.some(field => key.toLowerCase().includes(field.toLowerCase())))
      .map(key => ({ key, value: data[key] }));
  }

  /**
   * Get document information fields from extracted data
   */
  getDocumentInfo(data: any): {key: string, value: any}[] {
    if (!data) return [];
    
    const documentFields = ['documentNumber', 'documentId', 'id', 'number', 'issueDate', 'expiryDate', 
                           'issuedBy', 'authority', 'validUpto', 'qrCode', 'serialNumber'];
    
    return Object.keys(data)
      .filter(key => documentFields.some(field => key.toLowerCase().includes(field.toLowerCase())))
      .map(key => ({ key, value: data[key] }));
  }

  /**
   * Get address information fields from extracted data
   */
  getAddressInfo(data: any): {key: string, value: any}[] {
    if (!data) return [];
    
    const addressFields = ['address', 'street', 'city', 'state', 'pincode', 'pin', 'district', 
                          'block', 'village', 'tehsil', 'country', 'location'];
    
    return Object.keys(data)
      .filter(key => addressFields.some(field => key.toLowerCase().includes(field.toLowerCase())))
      .map(key => ({ key, value: data[key] }));
  }

  /**
   * Get other information fields from extracted data
   */
  getOtherInfo(data: any): {key: string, value: any}[] {
    if (!data) return [];
    
    const excludeFields = ['name', 'firstName', 'lastName', 'fullName', 'fatherName', 'motherName', 
                          'dob', 'dateOfBirth', 'gender', 'age', 'phone', 'mobile', 'email',
                          'documentNumber', 'documentId', 'id', 'number', 'issueDate', 'expiryDate', 
                          'issuedBy', 'authority', 'validUpto', 'qrCode', 'serialNumber',
                          'address', 'street', 'city', 'state', 'pincode', 'pin', 'district', 
                          'block', 'village', 'tehsil', 'country', 'location'];
    
    return Object.keys(data)
      .filter(key => !excludeFields.some(field => key.toLowerCase().includes(field.toLowerCase())))
      .map(key => ({ key, value: data[key] }));
  }
}
