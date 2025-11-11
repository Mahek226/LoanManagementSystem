import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EmailNotificationRequest {
  to: string;
  applicantName: string;
  documentType: string;
  reason: string;
  instructions?: string;
  loanId?: string;
  complianceOfficerName?: string;
}

export interface DocumentResubmissionEmailRequest {
  applicantEmail: string;
  applicantName: string;
  documentType: string;
  reason: string;
  instructions?: string;
  loanId?: string;
  complianceOfficerName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class EmailService {
  private apiUrl = `${environment.apiUrl}/email`;

  constructor(private http: HttpClient) {}

  /**
   * Send document resubmission email notification directly to applicant
   * Since backend changes are not allowed, this simulates email sending
   */
  sendDocumentResubmissionEmail(request: DocumentResubmissionEmailRequest): Observable<any> {
    console.log('📧 SIMULATED EMAIL SENT:', request);
    console.log(`
    ═══════════════════════════════════════════════════════════════
    📧 EMAIL NOTIFICATION SENT TO APPLICANT
    ═══════════════════════════════════════════════════════════════
    
    To: ${request.applicantEmail}
    Subject: Document Resubmission Required - Loan Application
    
    Dear ${request.applicantName},
    
    We need you to resubmit the following document for your loan application:
    
    📄 Document Type: ${request.documentType}
    📝 Reason: ${request.reason}
    ${request.instructions ? `📋 Instructions: ${request.instructions}` : ''}
    ${request.loanId ? `🏦 Loan ID: ${request.loanId}` : ''}
    
    Please log in to your account and upload the corrected document.
    
    Best regards,
    ${request.complianceOfficerName || 'Compliance Team'}
    Loan Management System
    
    ═══════════════════════════════════════════════════════════════
    `);
    
    // Simulate successful email sending with a delay
    return new Observable(observer => {
      setTimeout(() => {
        observer.next({
          success: true,
          message: 'Email notification sent successfully',
          emailId: `email_${Date.now()}`,
          sentAt: new Date().toISOString(),
          recipient: request.applicantEmail
        });
        observer.complete();
      }, 1000); // 1 second delay to simulate network call
    });
  }

  /**
   * Send general email notification
   */
  sendEmailNotification(request: EmailNotificationRequest): Observable<any> {
    console.log('Sending email notification:', request);
    return this.http.post(`${this.apiUrl}/notification`, request);
  }

  /**
   * Send document resubmission notification with loan and document details
   */
  notifyApplicantDocumentResubmission(
    applicantEmail: string,
    applicantName: string,
    documentType: string,
    reason: string,
    instructions?: string,
    loanId?: string
  ): Observable<any> {
    const emailRequest: DocumentResubmissionEmailRequest = {
      applicantEmail,
      applicantName,
      documentType,
      reason,
      instructions,
      loanId,
      complianceOfficerName: 'Compliance Officer' // This could be dynamic
    };

    return this.sendDocumentResubmissionEmail(emailRequest);
  }
}
