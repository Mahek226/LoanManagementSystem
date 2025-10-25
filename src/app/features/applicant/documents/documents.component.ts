import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="documents-container">
      <div class="page-header">
        <button class="back-btn" (click)="goBack()">
          <span class="back-icon">←</span>
        </button>
        <div class="header-content">
          <h1>Document Management</h1>
          <p>Upload and manage your required documents for loan application.</p>
        </div>
      </div>

      <div class="documents-content">
        <div class="upload-section">
          <h2>📄 Required Documents</h2>
          <div class="document-grid">
            <div class="document-card">
              <div class="document-icon">🆔</div>
              <h3>Identity Proof</h3>
              <p>Aadhaar Card, PAN Card, Passport</p>
              <button class="upload-btn">Upload</button>
            </div>
            
            <div class="document-card">
              <div class="document-icon">💰</div>
              <h3>Income Proof</h3>
              <p>Salary Slips, ITR, Bank Statements</p>
              <button class="upload-btn">Upload</button>
            </div>
            
            <div class="document-card">
              <div class="document-icon">🏠</div>
              <h3>Property Documents</h3>
              <p>Sale Agreement, Property Papers</p>
              <button class="upload-btn">Upload</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .documents-container {
      padding: 20px;
      max-width: 1200px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      align-items: center;
      margin-bottom: 30px;
      gap: 15px;
    }

    .back-btn {
      background: #f8f9fa;
      border: 1px solid #dee2e6;
      border-radius: 8px;
      padding: 10px 15px;
      cursor: pointer;
      font-size: 18px;
      transition: all 0.2s;
    }

    .back-btn:hover {
      background: #e9ecef;
    }

    .header-content h1 {
      margin: 0 0 5px 0;
      color: #2c3e50;
      font-size: 28px;
    }

    .header-content p {
      margin: 0;
      color: #6c757d;
      font-size: 16px;
    }

    .documents-content {
      background: white;
      border-radius: 12px;
      padding: 30px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }

    .upload-section h2 {
      margin-bottom: 20px;
      color: #2c3e50;
    }

    .document-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 20px;
    }

    .document-card {
      border: 2px dashed #dee2e6;
      border-radius: 12px;
      padding: 30px;
      text-align: center;
      transition: all 0.3s;
    }

    .document-card:hover {
      border-color: #007bff;
      background: #f8f9ff;
    }

    .document-icon {
      font-size: 48px;
      margin-bottom: 15px;
    }

    .document-card h3 {
      margin: 0 0 10px 0;
      color: #2c3e50;
    }

    .document-card p {
      margin: 0 0 20px 0;
      color: #6c757d;
      font-size: 14px;
    }

    .upload-btn {
      background: #007bff;
      color: white;
      border: none;
      border-radius: 8px;
      padding: 12px 24px;
      cursor: pointer;
      font-weight: 500;
      transition: all 0.2s;
    }

    .upload-btn:hover {
      background: #0056b3;
    }
  `]
})
export class DocumentsComponent {
  constructor(private router: Router) {}

  goBack(): void {
    this.router.navigate(['/applicant/dashboard']);
  }
}
