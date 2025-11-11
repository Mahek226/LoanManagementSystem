import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LoanIdService {
  
  constructor() {}

  /**
   * Generate a display-friendly loan ID from the internal loan ID
   * Format: LNID + 6 random digits
   * Example: LNID123456
   */
  generateDisplayLoanId(internalLoanId: number): string {
    // Use the internal loan ID as seed for consistent generation
    const seed = internalLoanId;
    
    // Generate 6-digit number based on the seed
    // This ensures the same internal ID always generates the same display ID
    const randomPart = this.generateConsistentRandom(seed, 6);
    
    return `LNID${randomPart}`;
  }

  /**
   * Generate a consistent random number based on a seed
   */
  private generateConsistentRandom(seed: number, digits: number): string {
    // Simple seeded random number generator
    let hash = seed;
    hash = ((hash << 5) - hash + seed) & 0xffffffff;
    hash = Math.abs(hash);
    
    // Generate the required number of digits
    const max = Math.pow(10, digits) - 1;
    const min = Math.pow(10, digits - 1);
    
    const randomNum = min + (hash % (max - min + 1));
    
    return randomNum.toString().padStart(digits, '0');
  }

  /**
   * Validate if a loan ID follows the correct format
   */
  isValidLoanIdFormat(loanId: string): boolean {
    const pattern = /^LNID\d{6}$/;
    return pattern.test(loanId);
  }

  /**
   * Extract the numeric part from a display loan ID
   */
  extractNumericPart(displayLoanId: string): string {
    if (!this.isValidLoanIdFormat(displayLoanId)) {
      return '';
    }
    return displayLoanId.substring(4); // Remove 'LNID' prefix
  }

  /**
   * Generate multiple display IDs for testing
   */
  generateTestIds(count: number = 10): { internal: number; display: string }[] {
    const testIds = [];
    for (let i = 1; i <= count; i++) {
      testIds.push({
        internal: i,
        display: this.generateDisplayLoanId(i)
      });
    }
    return testIds;
  }

  /**
   * Format loan ID for display with styling
   */
  formatLoanIdForDisplay(internalLoanId: number): {
    displayId: string;
    formatted: string;
    parts: { prefix: string; number: string };
  } {
    const displayId = this.generateDisplayLoanId(internalLoanId);
    const prefix = 'LNID';
    const number = displayId.substring(4);
    
    return {
      displayId,
      formatted: `${prefix}-${number}`,
      parts: { prefix, number }
    };
  }

  /**
   * Generate QR code data for loan ID
   */
  generateQRCodeData(internalLoanId: number): string {
    const displayId = this.generateDisplayLoanId(internalLoanId);
    return JSON.stringify({
      loanId: displayId,
      type: 'LOAN_APPLICATION',
      timestamp: new Date().toISOString()
    });
  }
}
