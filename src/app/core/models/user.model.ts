export interface User {
  id: number;
  username?: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: string; // 'ADMIN', 'APPLICANT', 'LOAN_OFFICER', 'COMPLIANCE_OFFICER'
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface CommonLoginResponse {
  userId: number;
  username?: string;
  firstName?: string;
  lastName?: string;
  email: string;
  userType: string; // "ADMIN", "APPLICANT", "LOAN_OFFICER", "COMPLIANCE_OFFICER"
  accessToken: string;
  tokenType: string;
  message?: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone: string;
  dob: string;
  gender: string;
  address: string;
  city: string;
  state: string;
  country: string;
  role: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

export interface MessageResponse {
  message: string;
}

export enum UserRole {
  ADMIN = 'ADMIN',
  LOAN_OFFICER = 'LOAN_OFFICER',
  COMPLIANCE_OFFICER = 'COMPLIANCE_OFFICER',
  APPLICANT = 'APPLICANT'
}
