import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-document-viewer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './document-viewer.component.html',
  styleUrls: ['./document-viewer.component.css']
})
export class DocumentViewerComponent implements OnInit {
  @Input() document: any = null;
  @Input() show = false;
  @Input() extractedData: any = null;
  @Input() annotatedImageUrl: string | null = null;
  @Input() extracting = false;
  @Input() canExtract = true;
  @Input() canVerify = true;
  @Input() themeColor = 'primary'; // primary, danger, warning, success
  
  @Output() close = new EventEmitter<void>();
  @Output() extract = new EventEmitter<any>();
  @Output() approve = new EventEmitter<any>();
  @Output() reject = new EventEmitter<any>();
  
  activeTab = 'parse';
  showAnnotatedImage = false;
  
  ngOnInit(): void {
    // Reset tab when modal opens
    if (this.show) {
      this.activeTab = 'parse';
    }
  }
  
  closeModal(): void {
    this.close.emit();
  }
  
  extractDocument(): void {
    if (this.document) {
      this.extract.emit(this.document);
    }
  }
  
  approveDocument(): void {
    if (this.document) {
      this.approve.emit(this.document);
    }
  }
  
  rejectDocument(): void {
    if (this.document) {
      this.reject.emit(this.document);
    }
  }
  
  // ==================== Helper Methods ====================
  
  getDocumentIcon(documentType: string): string {
    const iconMap: { [key: string]: string } = {
      'AADHAAR_CARD': 'fa-id-card',
      'PAN_CARD': 'fa-credit-card',
      'VOTER_ID': 'fa-vote-yea',
      'DRIVING_LICENSE': 'fa-car',
      'PASSPORT': 'fa-passport',
      'BANK_STATEMENT': 'fa-university',
      'SALARY_SLIP': 'fa-money-bill',
      'INCOME_CERTIFICATE': 'fa-certificate',
      'PROPERTY_DOCUMENTS': 'fa-home',
      'BUSINESS_REGISTRATION': 'fa-building',
      'GST_CERTIFICATE': 'fa-receipt',
      'ITR': 'fa-file-invoice'
    };
    return iconMap[documentType] || 'fa-file-alt';
  }
  
  getStatusColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'VERIFIED': case 'APPROVED': return 'success';
      case 'REJECTED': case 'FAILED': return 'danger';
      case 'PENDING': case 'UPLOADED': return 'warning';
      case 'PROCESSING': return 'info';
      default: return 'secondary';
    }
  }
  
  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  getThemeClass(): string {
    const themeMap: { [key: string]: string } = {
      'primary': 'text-primary',
      'danger': 'text-danger',
      'warning': 'text-warning',
      'success': 'text-success',
      'info': 'text-info'
    };
    return themeMap[this.themeColor] || 'text-primary';
  }
  
  getThemeGradient(): string {
    const gradientMap: { [key: string]: string } = {
      'primary': 'background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);',
      'danger': 'background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);',
      'warning': 'background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);',
      'success': 'background: linear-gradient(135deg, #10b981 0%, #059669 100%);',
      'info': 'background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);'
    };
    return gradientMap[this.themeColor] || gradientMap['primary'];
  }
  
  // ==================== Data Categorization Methods ====================
  
  formatExtractedField(fieldName: string): string {
    return fieldName
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, str => str.toUpperCase())
      .replace(/_/g, ' ')
      .trim();
  }
  
  isObject(value: any): boolean {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
  }
  
  isArray(value: any): boolean {
    return Array.isArray(value);
  }
  
  objectKeys(obj: any): string[] {
    return obj ? Object.keys(obj) : [];
  }
  
  getPersonalInfo(data: any): {key: string, value: any}[] {
    if (!data) return [];
    
    const personalFields = ['name', 'firstName', 'lastName', 'fullName', 'fatherName', 'motherName', 
                           'dob', 'dateOfBirth', 'gender', 'age', 'phone', 'mobile', 'email'];
    
    return Object.keys(data)
      .filter(key => personalFields.some(field => key.toLowerCase().includes(field.toLowerCase())))
      .map(key => ({ key, value: data[key] }));
  }
  
  getDocumentInfo(data: any): {key: string, value: any}[] {
    if (!data) return [];
    
    const documentFields = ['documentNumber', 'documentId', 'id', 'number', 'issueDate', 'expiryDate', 
                           'issuedBy', 'authority', 'validUpto', 'qrCode', 'serialNumber'];
    
    return Object.keys(data)
      .filter(key => documentFields.some(field => key.toLowerCase().includes(field.toLowerCase())))
      .map(key => ({ key, value: data[key] }));
  }
  
  getAddressInfo(data: any): {key: string, value: any}[] {
    if (!data) return [];
    
    const addressFields = ['address', 'street', 'city', 'state', 'pincode', 'pin', 'district', 
                          'block', 'village', 'tehsil', 'country', 'location'];
    
    return Object.keys(data)
      .filter(key => addressFields.some(field => key.toLowerCase().includes(field.toLowerCase())))
      .map(key => ({ key, value: data[key] }));
  }
  
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

  // ==================== Enhanced Data Methods for Confidence & Bounding Boxes ====================
  
  /**
   * Check if the extracted data has confidence scores (new format)
   */
  hasConfidenceData(data: any): boolean {
    if (!data) return false;
    return Object.values(data).some((value: any) => 
      value && typeof value === 'object' && 'confidence' in value
    );
  }

  /**
   * Get field value from either old format (direct value) or new format (with confidence)
   */
  getFieldValue(fieldData: any): string {
    if (!fieldData) return 'Not found';
    
    // Handle object responses (like [object Object])
    if (typeof fieldData === 'object' && fieldData.toString() === '[object Object]') {
      // Try to extract meaningful data from object
      if (fieldData.value) return fieldData.value;
      if (fieldData.text) return fieldData.text;
      if (fieldData.content) return fieldData.content;
      return JSON.stringify(fieldData);
    }
    
    // New format with confidence
    if (typeof fieldData === 'object' && 'value' in fieldData) {
      return fieldData.value || 'Not found';
    }
    
    // Old format (direct value)
    return fieldData || 'Not found';
  }

  /**
   * Get confidence score for a field (0-1 range)
   */
  getFieldConfidence(fieldData: any): number {
    if (!fieldData || typeof fieldData !== 'object') return 0;
    return fieldData.confidence || 0;
  }

  /**
   * Get confidence percentage as string
   */
  getConfidencePercentage(fieldData: any): string {
    const confidence = this.getFieldConfidence(fieldData);
    return `${Math.round(confidence * 100)}%`;
  }

  /**
   * Get confidence color class based on confidence level
   */
  getConfidenceColor(fieldData: any): string {
    const confidence = this.getFieldConfidence(fieldData);
    if (confidence >= 0.8) return 'text-success';
    if (confidence >= 0.6) return 'text-warning';
    return 'text-danger';
  }

  /**
   * Get confidence badge class
   */
  getConfidenceBadgeClass(fieldData: any): string {
    const confidence = this.getFieldConfidence(fieldData);
    if (confidence >= 0.8) return 'bg-success';
    if (confidence >= 0.6) return 'bg-warning';
    return 'bg-danger';
  }

  /**
   * Check if field has bounding box data
   */
  hasBoundingBox(fieldData: any): boolean {
    return fieldData && typeof fieldData === 'object' && fieldData.bbox && Array.isArray(fieldData.bbox);
  }

  /**
   * Toggle between original and annotated image
   */
  toggleAnnotatedImage(): void {
    this.showAnnotatedImage = !this.showAnnotatedImage;
  }

  /**
   * Get all extracted fields with their metadata
   */
  getAllFieldsWithMetadata(data: any): {key: string, fieldData: any, category: string}[] {
    if (!data) return [];
    
    // Handle case where data might be wrapped in a response object
    let actualData = data;
    if (data.extracted && typeof data.extracted === 'object') {
      actualData = data.extracted;
    }
    
    // Filter out non-field data (like metadata)
    const excludeKeys = ['message', 'success', 'totalDocuments', 'successCount', 'failureCount', 'extractionResults'];
    
    return Object.keys(actualData)
      .filter(key => !excludeKeys.includes(key))
      .map(key => {
        const fieldData = actualData[key];
        let category = 'other';
        
        // Categorize fields
        const personalFields = ['name', 'firstName', 'lastName', 'fullName', 'fatherName', 'motherName', 
                               'dob', 'dateOfBirth', 'gender', 'age', 'phone', 'mobile', 'email'];
        const documentFields = ['documentNumber', 'documentId', 'id', 'number', 'issueDate', 'expiryDate', 
                               'issuedBy', 'authority', 'validUpto', 'qrCode', 'serialNumber'];
        const addressFields = ['address', 'street', 'city', 'state', 'pincode', 'pin', 'district', 
                              'block', 'village', 'tehsil', 'country', 'location'];
        
        if (personalFields.some(field => key.toLowerCase().includes(field.toLowerCase()))) {
          category = 'personal';
        } else if (documentFields.some(field => key.toLowerCase().includes(field.toLowerCase()))) {
          category = 'document';
        } else if (addressFields.some(field => key.toLowerCase().includes(field.toLowerCase()))) {
          category = 'address';
        }
        
        return { key, fieldData, category };
      });
  }

  /**
   * Get the actual extracted data from the response
   */
  getActualExtractedData(data: any): any {
    if (!data) return null;
    
    // If data has an 'extracted' property, use that
    if (data.extracted && typeof data.extracted === 'object') {
      return data.extracted;
    }
    
    // Otherwise, filter out metadata and return actual field data
    const excludeKeys = ['message', 'success', 'totalDocuments', 'successCount', 'failureCount', 'extractionResults', 'document_id', 'applicant_id', 'document_type', 'annotated_image'];
    const actualData: any = {};
    
    Object.keys(data).forEach(key => {
      if (!excludeKeys.includes(key)) {
        actualData[key] = data[key];
      }
    });
    
    return Object.keys(actualData).length > 0 ? actualData : null;
  }

  /**
   * Get raw text length for terminal display
   */
  getRawTextLength(data: any): number {
    // Get the actual raw text length from the backend response
    return data?.raw_text_length || 0;
  }

  /**
   * Get extracted fields as a string (like Python dict)
   */
  getExtractedFieldsString(data: any): string {
    const actualData = this.getActualExtractedData(data);
    if (!actualData) return '{}';
    
    const fields: any = {};
    Object.keys(actualData).forEach(key => {
      const value = actualData[key];
      if (typeof value === 'object' && 'value' in value) {
        fields[key] = value.value;
      } else {
        fields[key] = value;
      }
    });
    
    return JSON.stringify(fields).replace(/"/g, "'");
  }

  /**
   * Get fields formatted for terminal-style display
   */
  getTerminalStyleFields(data: any): Array<{name: string, value: string, confidence: string}> {
    const actualData = this.getActualExtractedData(data);
    if (!actualData) return [];
    
    return Object.keys(actualData).map(key => {
      const fieldData = actualData[key];
      let value = 'None';
      let confidence = '0.0%';
      
      if (typeof fieldData === 'object' && 'value' in fieldData) {
        value = fieldData.value === null ? 'None' : 
                fieldData.value === '' ? '' : 
                fieldData.value === false ? 'False' :
                fieldData.value === true ? 'True' :
                String(fieldData.value);
        confidence = `${(fieldData.confidence * 100).toFixed(1)}%`;
      } else {
        value = fieldData === null ? 'None' : 
                fieldData === '' ? '' : 
                fieldData === false ? 'False' :
                fieldData === true ? 'True' :
                String(fieldData);
      }
      
      return {
        name: key,
        value: value,
        confidence: confidence
      };
    });
  }

  /**
   * Get document image URL from various possible properties
   */
  getDocumentImageUrl(): string {
    if (!this.document) return 'assets/images/document-placeholder.png';
    
    // Try different possible URL properties
    const possibleUrls = [
      this.document.documentUrl,
      this.document.cloudinaryUrl,
      this.document.fileUrl,
      this.document.url,
      this.document.filePath,
      this.document.path,
      this.document.imageUrl
    ];
    
    for (const url of possibleUrls) {
      if (url && typeof url === 'string' && url.trim()) {
        return url;
      }
    }
    
    return 'assets/images/document-placeholder.svg';
  }

  /**
   * Handle image loading errors
   */
  onImageError(event: any): void {
    console.warn('Failed to load document image:', this.document);
    event.target.src = 'assets/images/document-placeholder.svg';
  }
}
