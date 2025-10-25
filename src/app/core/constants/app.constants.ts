export const APP_CONSTANTS = {
  STORAGE_KEYS: {
    TOKEN: 'lms_auth_token',
    USER: 'lms_user',
    REFRESH_TOKEN: 'lms_refresh_token'
  },
  
  API_ENDPOINTS: {
    AUTH: {
      LOGIN: '/auth/login',
      REGISTER: '/applicant/auth/register',
      LOGOUT: '/auth/logout',
      FORGOT_PASSWORD: '/auth/forgot-password',
      RESET_PASSWORD: '/auth/reset-password',
      VERIFY_EMAIL: '/auth/verify-email',
      REFRESH_TOKEN: '/auth/refresh'
    },
    ADMIN: '/admin',
    LOAN_OFFICER: '/loan-officer',
    COMPLIANCE: '/compliance',
    APPLICANT: '/applicant'
  },
  
  ROUTES: {
    LOGIN: '/auth/login',
    FORGOT_PASSWORD: '/auth/forgot-password',
    RESET_PASSWORD: '/auth/reset-password',
    ADMIN_DASHBOARD: '/admin/dashboard',
    LOAN_OFFICER_DASHBOARD: '/loan-officer/dashboard',
    COMPLIANCE_DASHBOARD: '/compliance/dashboard',
    APPLICANT_DASHBOARD: '/applicant/dashboard'
  },
  
  ROLES: {
    ADMIN: 'ADMIN',
    LOAN_OFFICER: 'LOAN_OFFICER',
    COMPLIANCE_OFFICER: 'COMPLIANCE_OFFICER',
    APPLICANT: 'APPLICANT'
  }
};
