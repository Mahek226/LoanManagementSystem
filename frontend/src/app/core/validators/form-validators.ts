import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export class FormValidators {
  
  // ==================== NAME VALIDATIONS ====================
  
  /**
   * Validates name fields - only alphabets and spaces, first letter capital
   * Pattern: First letter capital, followed by lowercase letters and spaces
   */
  static nameValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const namePattern = /^[A-Z][a-zA-Z\s]*$/;
      const valid = namePattern.test(control.value.trim());
      
      if (!valid) {
        return { 
          invalidName: { 
            message: 'Name must start with a capital letter and contain only alphabets and spaces' 
          } 
        };
      }
      
      // Check for multiple consecutive spaces
      if (/\s{2,}/.test(control.value)) {
        return { 
          invalidName: { 
            message: 'Name cannot contain multiple consecutive spaces' 
          } 
        };
      }
      
      return null;
    };
  }

  /**
   * Validates full name - allows multiple words with proper capitalization
   */
  static fullNameValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const trimmedValue = control.value.trim();
      
      // Check if each word starts with capital letter
      const words = trimmedValue.split(/\s+/);
      const validWords = words.every((word: string) => /^[A-Z][a-z]*$/.test(word));
      
      if (!validWords) {
        return { 
          invalidFullName: { 
            message: 'Each word must start with a capital letter and contain only alphabets' 
          } 
        };
      }
      
      return null;
    };
  }

  // ==================== DATE VALIDATIONS ====================
  
  /**
   * Validates date - only allows dates up to today
   */
  static dateNotFutureValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const inputDate = new Date(control.value);
      const today = new Date();
      today.setHours(23, 59, 59, 999); // End of today
      
      if (inputDate > today) {
        return { 
          futureDate: { 
            message: 'Date cannot be in the future' 
          } 
        };
      }
      
      return null;
    };
  }

  /**
   * Validates birth date - must be at least 18 years ago and not future
   */
  static birthDateValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const inputDate = new Date(control.value);
      const today = new Date();
      const eighteenYearsAgo = new Date();
      eighteenYearsAgo.setFullYear(today.getFullYear() - 18);
      
      if (inputDate > today) {
        return { 
          futureDate: { 
            message: 'Birth date cannot be in the future' 
          } 
        };
      }
      
      if (inputDate > eighteenYearsAgo) {
        return { 
          underAge: { 
            message: 'You must be at least 18 years old' 
          } 
        };
      }
      
      // Check if date is too old (more than 100 years)
      const hundredYearsAgo = new Date();
      hundredYearsAgo.setFullYear(today.getFullYear() - 100);
      
      if (inputDate < hundredYearsAgo) {
        return { 
          tooOld: { 
            message: 'Please enter a valid birth date' 
          } 
        };
      }
      
      return null;
    };
  }

  // ==================== CONTACT VALIDATIONS ====================
  
  /**
   * Validates Indian mobile number - 10 digits starting with 6-9
   */
  static mobileNumberValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const mobilePattern = /^[6-9]\d{9}$/;
      const valid = mobilePattern.test(control.value);
      
      if (!valid) {
        return { 
          invalidMobile: { 
            message: 'Mobile number must be 10 digits starting with 6, 7, 8, or 9' 
          } 
        };
      }
      
      return null;
    };
  }

  /**
   * Validates email with comprehensive pattern
   */
  static emailValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
      const valid = emailPattern.test(control.value);
      
      if (!valid) {
        return { 
          invalidEmail: { 
            message: 'Please enter a valid email address' 
          } 
        };
      }
      
      return null;
    };
  }

  // ==================== INDIAN DOCUMENT VALIDATIONS ====================
  
  /**
   * Validates PAN number - AAAAA9999A format
   */
  static panValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const panPattern = /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/;
      const valid = panPattern.test(control.value.toUpperCase());
      
      if (!valid) {
        return { 
          invalidPan: { 
            message: 'PAN must be in format AAAAA9999A (5 letters, 4 digits, 1 letter)' 
          } 
        };
      }
      
      return null;
    };
  }

  /**
   * Validates Aadhar number - 12 digits
   */
  static aadharValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const aadharPattern = /^\d{12}$/;
      const valid = aadharPattern.test(control.value);
      
      if (!valid) {
        return { 
          invalidAadhar: { 
            message: 'Aadhar number must be exactly 12 digits' 
          } 
        };
      }
      
      return null;
    };
  }

  // ==================== FINANCIAL VALIDATIONS ====================
  
  /**
   * Validates positive numbers only
   */
  static positiveNumberValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const value = parseFloat(control.value);
      
      if (isNaN(value) || value <= 0) {
        return { 
          invalidAmount: { 
            message: 'Amount must be a positive number' 
          } 
        };
      }
      
      return null;
    };
  }

  /**
   * Validates IFSC code - AAAA0999999 format
   */
  static ifscValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const ifscPattern = /^[A-Z]{4}0[A-Z0-9]{6}$/;
      const valid = ifscPattern.test(control.value.toUpperCase());
      
      if (!valid) {
        return { 
          invalidIfsc: { 
            message: 'IFSC code must be in format AAAA0999999 (4 letters, 0, 6 alphanumeric)' 
          } 
        };
      }
      
      return null;
    };
  }

  // ==================== ADDRESS VALIDATIONS ====================
  
  /**
   * Validates pincode - 6 digits
   */
  static pincodeValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const pincodePattern = /^\d{6}$/;
      const valid = pincodePattern.test(control.value);
      
      if (!valid) {
        return { 
          invalidPincode: { 
            message: 'Pincode must be exactly 6 digits' 
          } 
        };
      }
      
      return null;
    };
  }

  /**
   * Validates address - allows alphanumeric, spaces, commas, periods, hyphens
   */
  static addressValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const addressPattern = /^[a-zA-Z0-9\s,.-]+$/;
      const valid = addressPattern.test(control.value.trim());
      
      if (!valid) {
        return { 
          invalidAddress: { 
            message: 'Address can only contain letters, numbers, spaces, commas, periods, and hyphens' 
          } 
        };
      }
      
      return null;
    };
  }

  // ==================== PASSWORD VALIDATIONS ====================
  
  /**
   * Validates strong password
   */
  static strongPasswordValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const password = control.value;
      const errors: any = {};
      
      // At least 8 characters
      if (password.length < 8) {
        errors.minLength = 'Password must be at least 8 characters long';
      }
      
      // At least one uppercase letter
      if (!/[A-Z]/.test(password)) {
        errors.uppercase = 'Password must contain at least one uppercase letter';
      }
      
      // At least one lowercase letter
      if (!/[a-z]/.test(password)) {
        errors.lowercase = 'Password must contain at least one lowercase letter';
      }
      
      // At least one digit
      if (!/\d/.test(password)) {
        errors.digit = 'Password must contain at least one digit';
      }
      
      // At least one special character
      if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
        errors.special = 'Password must contain at least one special character';
      }
      
      return Object.keys(errors).length > 0 ? { weakPassword: errors } : null;
    };
  }

  // ==================== UTILITY METHODS ====================
  
  /**
   * Get today's date in YYYY-MM-DD format for max date attribute
   */
  static getTodayDate(): string {
    const today = new Date();
    return today.toISOString().split('T')[0];
  }

  /**
   * Get date 18 years ago for birth date validation
   */
  static getMaxBirthDate(): string {
    const today = new Date();
    const eighteenYearsAgo = new Date();
    eighteenYearsAgo.setFullYear(today.getFullYear() - 18);
    return eighteenYearsAgo.toISOString().split('T')[0];
  }

  /**
   * Capitalize first letter of each word
   */
  static capitalizeWords(value: string): string {
    return value.replace(/\b\w/g, l => l.toUpperCase());
  }
}
