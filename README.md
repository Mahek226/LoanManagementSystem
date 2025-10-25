# LMS Frontend Application

This is the Angular frontend for the Loan Management System (LMS).

## Features

- **Common Login Page**: Single login interface for all user roles (Admin, Loan Officer, Compliance Officer, Applicant)
- **Forgot Password**: Password recovery functionality
- **Role-Based Access Control**: Automatic routing based on user role after login
- **JWT Authentication**: Secure token-based authentication
- **Modern UI**: Clean and responsive design

## Project Structure

```
src/
├── app/
│   ├── core/                    # Core module (singleton services, guards, interceptors)
│   │   ├── guards/             # Route guards
│   │   ├── interceptors/       # HTTP interceptors
│   │   ├── services/           # Core services (auth, storage)
│   │   └── models/             # Core data models
│   ├── shared/                  # Shared module (common components, directives, pipes)
│   │   ├── components/         # Reusable components
│   │   ├── directives/         # Custom directives
│   │   └── pipes/              # Custom pipes
│   ├── features/                # Feature modules
│   │   ├── auth/               # Authentication feature
│   │   │   ├── login/          # Login component
│   │   │   ├── forgot-password/ # Forgot password component
│   │   │   └── reset-password/ # Reset password component
│   │   ├── admin/              # Admin dashboard
│   │   ├── loan-officer/       # Loan officer dashboard
│   │   ├── compliance-officer/ # Compliance officer dashboard
│   │   └── applicant/          # Applicant dashboard
│   └── app.component.ts        # Root component
├── assets/                      # Static assets
└── environments/                # Environment configurations
```

## User Roles

- **ADMIN**: System administrator with full access
- **LOAN_OFFICER**: Manages loan applications
- **COMPLIANCE_OFFICER**: Reviews and approves loans
- **APPLICANT**: Applies for loans

## Getting Started

### Prerequisites

- Node.js (v18 or higher)
- npm (v9 or higher)
- Angular CLI (v17 or higher)

### Installation

1. Install dependencies:
   ```bash
   npm install
   ```

2. Install Angular CLI globally (if not already installed):
   ```bash
   npm install -g @angular/cli
   ```

### Development Server

Run the development server:

```bash
npm start
```

Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

### Build

Build the project for production:

```bash
npm run build
```

The build artifacts will be stored in the `dist/` directory.

### Configuration

Update the API endpoint in `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

## API Integration

The frontend integrates with the Spring Boot backend API:

- **Login**: `POST /api/admin/auth/login`
- **Register**: `POST /api/auth/signup`
- **Forgot Password**: `POST /api/auth/forgot-password`
- **Reset Password**: `POST /api/auth/reset-password`

## Security

- JWT tokens are stored in sessionStorage
- HTTP interceptor automatically adds Authorization header
- Route guards protect authenticated routes
- Auto-logout on token expiration

## Best Practices Implemented

- ✅ Feature-based folder structure
- ✅ Lazy loading for feature modules
- ✅ Reactive forms with validation
- ✅ HTTP interceptor for authentication
- ✅ Route guards for authorization
- ✅ Environment-based configuration
- ✅ TypeScript path aliases
- ✅ Separation of concerns (Core, Shared, Features)
- ✅ Reusable components and services
