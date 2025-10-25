# LMS Frontend Setup Guide

## Prerequisites

Before you begin, ensure you have the following installed:

- **Node.js** (v18 or higher) - [Download](https://nodejs.org/)
- **npm** (v9 or higher) - Comes with Node.js
- **Angular CLI** (v17 or higher)

## Installation Steps

### 1. Install Node.js and npm

Check if you have Node.js and npm installed:

```bash
node --version
npm --version
```

If not installed, download and install from [nodejs.org](https://nodejs.org/).

### 2. Install Angular CLI

Install Angular CLI globally:

```bash
npm install -g @angular/cli@17
```

Verify installation:

```bash
ng version
```

### 3. Install Project Dependencies

Navigate to the Frontend directory and install dependencies:

```bash
cd Frontend
npm install
```

### 4. Configure Backend API URL

Update the API URL in the environment files if your backend is running on a different port:

- **Development**: `src/environments/environment.ts`
- **Production**: `src/environments/environment.prod.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'  // Update this if needed
};
```

## Running the Application

### Development Server

Start the development server:

```bash
npm start
```

Or with the proxy configuration (recommended):

```bash
ng serve --proxy-config proxy.conf.json
```

The application will be available at `http://localhost:4200/`.

### Production Build

Build the application for production:

```bash
npm run build
```

The build artifacts will be stored in the `dist/` directory.

## Project Structure

```
Frontend/
├── src/
│   ├── app/
│   │   ├── core/               # Core module (singleton services)
│   │   │   ├── guards/        # Route guards
│   │   │   ├── interceptors/  # HTTP interceptors
│   │   │   ├── services/      # Core services
│   │   │   ├── models/        # Data models
│   │   │   └── constants/     # App constants
│   │   ├── features/          # Feature modules
│   │   │   ├── auth/          # Authentication
│   │   │   ├── admin/         # Admin features
│   │   │   ├── loan-officer/  # Loan officer features
│   │   │   ├── compliance-officer/  # Compliance features
│   │   │   └── applicant/     # Applicant features
│   │   ├── app.component.ts   # Root component
│   │   ├── app.config.ts      # App configuration
│   │   └── app.routes.ts      # Route definitions
│   ├── assets/                # Static assets
│   ├── environments/          # Environment configs
│   ├── styles.css            # Global styles
│   └── index.html            # Main HTML file
├── angular.json              # Angular configuration
├── package.json             # Dependencies
└── tsconfig.json           # TypeScript configuration
```

## User Roles and Access

The application supports four user roles:

1. **Admin** (`ROLE_ADMIN`)
   - Route: `/admin/dashboard`
   - Full system access

2. **Loan Officer** (`ROLE_LOAN_OFFICER`)
   - Route: `/loan-officer/dashboard`
   - Manages loan applications

3. **Compliance Officer** (`ROLE_COMPLIANCE_OFFICER`)
   - Route: `/compliance/dashboard`
   - Reviews and approves loans

4. **Applicant** (`ROLE_APPLICANT`)
   - Route: `/applicant/dashboard`
   - Applies for loans

## Features

### Authentication

- **Login**: Common login page for all user roles
- **Forgot Password**: Password recovery via email
- **JWT Token**: Secure token-based authentication
- **Auto-logout**: On token expiration or 401 errors
- **Role-based Routing**: Automatic redirection based on user role

### Security

- HTTP interceptor adds JWT token to all requests
- Route guards protect authenticated routes
- Role-based access control
- Session storage for token management

## Development Tips

### Using Proxy Configuration

To avoid CORS issues during development, use the proxy configuration:

```bash
ng serve --proxy-config proxy.conf.json
```

This will proxy all `/api/*` requests to `http://localhost:8080`.

### Hot Reload

Angular CLI supports hot reload. Any changes to the code will automatically reload the browser.

### Debugging

1. Open browser DevTools (F12)
2. Check the Console for errors
3. Use the Network tab to inspect API calls
4. Angular DevTools extension is recommended

## Testing

### Unit Tests

Run unit tests:

```bash
npm test
```

### End-to-End Tests

```bash
ng e2e
```

## Common Issues

### Port Already in Use

If port 4200 is already in use, specify a different port:

```bash
ng serve --port 4300
```

### Module Not Found

Clear node_modules and reinstall:

```bash
rm -rf node_modules package-lock.json
npm install
```

### CORS Errors

Use the proxy configuration:

```bash
ng serve --proxy-config proxy.conf.json
```

## Backend Integration

Ensure your Spring Boot backend is running on `http://localhost:8080` with the following endpoints available:

- `POST /api/admin/auth/login` - Login
- `POST /api/auth/signup` - Register
- `POST /api/auth/forgot-password` - Forgot password
- `POST /api/auth/reset-password` - Reset password

## Next Steps

1. Start the backend Spring Boot application
2. Start the Angular frontend
3. Navigate to `http://localhost:4200`
4. Login with your credentials
5. You'll be redirected to the appropriate dashboard based on your role

## Additional Resources

- [Angular Documentation](https://angular.io/docs)
- [Angular CLI](https://angular.io/cli)
- [TypeScript Documentation](https://www.typescriptlang.org/docs/)
- [RxJS Documentation](https://rxjs.dev/)
