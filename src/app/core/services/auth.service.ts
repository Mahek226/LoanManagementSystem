import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '@environments/environment';
import { 
  LoginRequest, 
  CommonLoginResponse, 
  RegisterRequest, 
  ForgotPasswordRequest,
  ResetPasswordRequest,
  MessageResponse,
  User 
} from '@core/models/user.model';
import { StorageService } from './storage.service';
import { APP_CONSTANTS } from '@core/constants/app.constants';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = environment.apiUrl;
  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser$: Observable<User | null>;
  private isAuthenticatedSubject: BehaviorSubject<boolean>;
  public isAuthenticated$: Observable<boolean>;

  constructor(
    private http: HttpClient,
    private storageService: StorageService,
    private router: Router
  ) {
    const user = this.storageService.getItem<User>(APP_CONSTANTS.STORAGE_KEYS.USER);
    this.currentUserSubject = new BehaviorSubject<User | null>(user);
    this.currentUser$ = this.currentUserSubject.asObservable();
    
    this.isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
    this.isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  }

  /**
   * Get current user value
   */
  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check if user is authenticated
   */
  public get isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  /**
   * Login user (Common endpoint for all user types)
   */
  login(credentials: LoginRequest): Observable<CommonLoginResponse> {
    // Backend expects { username, password } while our form provides { usernameOrEmail, password }
    const payload = {
      username: credentials.usernameOrEmail,
      password: credentials.password
    };

    return this.http.post<CommonLoginResponse>(
      `${this.API_URL}${APP_CONSTANTS.API_ENDPOINTS.AUTH.LOGIN}`,
      payload
    ).pipe(
      tap(response => this.handleAuthSuccess(response))
    );
  }

  /**
   * Register new user
   */
  register(userData: RegisterRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${this.API_URL}${APP_CONSTANTS.API_ENDPOINTS.AUTH.REGISTER}`,
      userData
    );
  }

  /**
   * Logout user
   */
  logout(): void {
    this.storageService.removeItem(APP_CONSTANTS.STORAGE_KEYS.TOKEN);
    this.storageService.removeItem(APP_CONSTANTS.STORAGE_KEYS.USER);
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.router.navigate([APP_CONSTANTS.ROUTES.LOGIN]);
  }

  /**
   * Forgot password
   */
  forgotPassword(request: ForgotPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${this.API_URL}${APP_CONSTANTS.API_ENDPOINTS.AUTH.FORGOT_PASSWORD}`,
      request
    );
  }

  /**
   * Reset password
   */
  resetPassword(request: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${this.API_URL}${APP_CONSTANTS.API_ENDPOINTS.AUTH.RESET_PASSWORD}`,
      request
    );
  }

  /**
   * Verify email with OTP
   */
  verifyEmail(request: { email: string; otp: string }): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${this.API_URL}/applicant/auth/verify-otp`,
      request
    );
  }

  /**
   * Resend OTP for email verification
   */
  resendOTP(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${this.API_URL}/applicant/auth/resend-otp?email=${encodeURIComponent(email)}`,
      {}
    );
  }

  /**
   * Get authentication token
   */
  getToken(): string | null {
    return this.storageService.getItem<string>(APP_CONSTANTS.STORAGE_KEYS.TOKEN);
  }

  /**
   * Check if user has token
   */
  hasToken(): boolean {
    return this.storageService.hasItem(APP_CONSTANTS.STORAGE_KEYS.TOKEN);
  }

  /**
   * Check if user has specific role
   */
  hasRole(role: string): boolean {
    const user = this.currentUserValue;
    return user ? user.role === role : false;
  }

  /**
   * Get user's primary role
   */
  getPrimaryRole(): string | null {
    const user = this.currentUserValue;
    return user ? user.role : null;
  }

  /**
   * Get default route based on user role
   */
  getDefaultRoute(): string {
    const role = this.getPrimaryRole();
    
    switch (role) {
      case APP_CONSTANTS.ROLES.ADMIN:
        return APP_CONSTANTS.ROUTES.ADMIN_DASHBOARD;
      case APP_CONSTANTS.ROLES.LOAN_OFFICER:
        return APP_CONSTANTS.ROUTES.LOAN_OFFICER_DASHBOARD;
      case APP_CONSTANTS.ROLES.COMPLIANCE_OFFICER:
        return APP_CONSTANTS.ROUTES.COMPLIANCE_DASHBOARD;
      case APP_CONSTANTS.ROLES.APPLICANT:
        return APP_CONSTANTS.ROUTES.APPLICANT_DASHBOARD;
      default:
        return APP_CONSTANTS.ROUTES.LOGIN;
    }
  }

  /**
   * Handle successful authentication
   */
  private handleAuthSuccess(response: CommonLoginResponse): void {
    // Store token
    this.storageService.setItem(APP_CONSTANTS.STORAGE_KEYS.TOKEN, response.accessToken);
    
    // Create user object
    const user: User = {
      id: response.userId,
      userId: response.userId,
      applicantId: response.applicantId,
      officerId: response.officerId,
      username: response.username,
      firstName: response.firstName,
      lastName: response.lastName,
      email: response.email,
      role: response.userType
    };
    
    // Store user
    this.storageService.setItem(APP_CONSTANTS.STORAGE_KEYS.USER, user);
    
    // Update subjects
    this.currentUserSubject.next(user);
    this.isAuthenticatedSubject.next(true);
  }
}
