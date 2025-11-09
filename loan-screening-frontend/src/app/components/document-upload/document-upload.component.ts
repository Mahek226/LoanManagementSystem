import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="document-upload">
      <h2>Upload Documents</h2>
      <div class="upload-section">
        <div class="form-group">
          <label>Document Type</label>
          <select [(ngModel)]="documentType">
            <option value="">Select Document Type</option>
            <option value="AADHAAR">Aadhaar Card</option>
            <option value="PAN">PAN Card</option>
            <option value="PASSPORT">Passport</option>
            <option value="SALARY_SLIP">Salary Slip</option>
            <option value="BANK_STATEMENT">Bank Statement</option>
          </select>
        </div>
        <div class="form-group">
          <label>Select File</label>
          <input type="file" (change)="onFileSelected($event)" accept="image/*,.pdf">
        </div>
        <button (click)="uploadFile()" [disabled]="!selectedFile || !documentType || uploading">
          {{ uploading ? 'Uploading...' : 'Upload' }}
        </button>
        <div *ngIf="extractionResult" class="extraction-result">
          <h3>Extracted Data:</h3>
          <pre>{{ extractionResult | json }}</pre>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .document-upload {
      max-width: 800px;
      margin: 0 auto;
      background: white;
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }
    .form-group {
      margin-bottom: 1rem;
    }
    .form-group label {
      display: block;
      margin-bottom: 0.5rem;
      font-weight: 500;
    }
    .form-group select,
    .form-group input[type="file"] {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
    }
    button {
      width: 100%;
      padding: 0.75rem;
      background: #1976d2;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 1rem;
    }
    .extraction-result {
      margin-top: 2rem;
      padding: 1rem;
      background: #f5f5f5;
      border-radius: 4px;
    }
  `]
})
export class DocumentUploadComponent {
  documentType = '';
  selectedFile: File | null = null;
  uploading = false;
  extractionResult: any = null;
  applicantId = 1; // Get from auth service

  constructor(private http: HttpClient) {}

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
  }

  uploadFile() {
    if (!this.selectedFile || !this.documentType) return;

    const token = localStorage.getItem('token');
    const formData = new FormData();
    formData.append('file', this.selectedFile);
    formData.append('document_type', this.documentType);
    formData.append('applicant_id', this.applicantId.toString());

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    this.uploading = true;
    this.http.post(`${environment.apiUrl}/document/upload`, formData, { headers })
      .subscribe({
        next: (response: any) => {
          this.extractionResult = response.extraction;
          this.uploading = false;
        },
        error: (error) => {
          console.error('Upload error:', error);
          this.uploading = false;
        }
      });
  }
}

